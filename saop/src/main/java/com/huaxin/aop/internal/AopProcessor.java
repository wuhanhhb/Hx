package com.huaxin.aop.internal;


import static javax.tools.Diagnostic.Kind.ERROR;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.huaxin.aop.InjectMethod;
import com.huaxin.aop.internal.Aop.AopClass;

/**
 * Created by hebing on 2016/4/12.
 */
public class AopProcessor extends AbstractProcessor {

	private Elements elementUtils;
	private Types typeUtils;
	private Filer filer;

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);
		elementUtils = env.getElementUtils();
		typeUtils = env.getTypeUtils();
		filer = env.getFiler();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment env) {
		System.out.println("process =================================> begin");
		Map<TypeElement, AopClass> targetClassMap = new LinkedHashMap<>();
		for (Element element : env.getElementsAnnotatedWith(InjectMethod.class)) {
			if (element.getKind() != ElementKind.METHOD) {
				error(element, "Only method can be annotated with @%s",
						InjectMethod.class.getSimpleName());
				return true;
			}

			String[] params = element.getAnnotation(InjectMethod.class)
					.params();

			String duplicate = findDuplicate(params);
			if (duplicate != null) {
				error(element,
						"@%s annotation contains duplicate ID %d. (%s.%s)",
						InjectMethod.class.getSimpleName(), duplicate,
						((TypeElement) element.getEnclosingElement())
								.getQualifiedName(), element.getSimpleName());
			}

			AopClass aopClass = getOrCreateTargetClass(targetClassMap,
					(TypeElement) element.getEnclosingElement());

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < params.length; i++) {
				sb.append(params[i]);
				if (i < params.length - 1)
					sb.append(", ");
			}
			aopClass.addParams(sb.toString());
		}

		for (Map.Entry<TypeElement, AopClass> entry : targetClassMap.entrySet()) {
			TypeElement typeElement = entry.getKey();
			AopClass bindingClass = entry.getValue();
			try {
				bindingClass.brewJava(filer);
			} catch (Exception e) {
				error(typeElement,
						"Unable to write view binder for type %s: %s",
						typeElement, e.getMessage());
			}
		}

		return true;
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> types = new LinkedHashSet<>();
		types.add(InjectMethod.class.getCanonicalName());
		return types;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	private void error(Element element, String message, Object... args) {
		if (args.length > 0) {
			message = String.format(message, args);
		}
		processingEnv.getMessager().printMessage(ERROR, message, element);
	}

	/**
	 * Returns the first duplicate element inside an array, null if there are no
	 * duplicates.
	 */
	private static String findDuplicate(String[] array) {
		Set<String> seenElements = new LinkedHashSet<>();
		for (String element : array) {
			if (!seenElements.add(element)) {
				return element;
			}
		}
		return null;
	}

	private AopClass getOrCreateTargetClass(
			Map<TypeElement, AopClass> targetClassMap,
			TypeElement enclosingElement) {
		AopClass aopClass = targetClassMap.get(enclosingElement);
		if (aopClass == null) {
			String classPackage = getPackageName(enclosingElement);
			String className = getClassName(enclosingElement, classPackage);
			String classFqcn = getFqcn(enclosingElement);

			aopClass = new AopClass(classPackage, className, classFqcn);
			targetClassMap.put(enclosingElement, aopClass);
		}
		return aopClass;
	}

	private String getPackageName(TypeElement type) {
		return elementUtils.getPackageOf(type).getQualifiedName().toString();
	}

	private static String getClassName(TypeElement type, String packageName) {
		int packageLen = packageName.length() + 1;
		return type.getQualifiedName().toString().substring(packageLen)
				.replace('.', '$');
	}

	/**
	 * Get full-qualified class name of a {@linkplain TypeElement typeElement}
	 */
	private String getFqcn(TypeElement typeElement) {
		String packageName = getPackageName(typeElement);
		return packageName + "." + getClassName(typeElement, packageName);
	}

}
