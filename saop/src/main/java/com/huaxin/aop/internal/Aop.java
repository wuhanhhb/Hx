package com.huaxin.aop.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.processing.Filer;

/**
 * Created by hebing on 2016/4/12.
 */
public class Aop {

    private final static String className = "Request";
    private static String packageName = "com.huaxin.common";
    private final static String commonpath = "common\\src\\main\\java\\com\\huaxin\\common\\Request.java";

    final static class SUFFIX {
        // all path push contain it
        final static String FLAG = "src\\main\\res\\raw\\baseconfig.xml";
        // common flag
        final static String COMMON = "common\\src\\main\\res\\raw";
        // malsite flag
        final static String MALSITE = "malsite\\src\\main\\res\\raw";
        // multsite flag
        final static String MULTSITE = "multsite\\src\\main\\res\\raw";
    }

    private static long updateTime = 0;

    public static void aopMap() {
        String xml = null;
        // step 1 find all baseconfig.xml
        ArrayList<File> files = new ArrayList<File>();
        searchAllXML(new File("."), files);
        for (File find : files)
            System.out.println("Find : " + find.getAbsolutePath());
        if (files.size() == 0) {
            throw new RuntimeException("Not find any baseconfig.xml");
        } else if (files.size() == 1) {
            xml = files.get(0).getAbsolutePath();
        } else if (files.size() == 2) {
            String path = files.get(0).getAbsolutePath();
            path = path.replace("/", "\\");
            if (path.contains(SUFFIX.COMMON)) {
                xml = files.get(1).getAbsolutePath();
            } else {
                xml = path;
            }
        } else {
            for (File find : files) {
                if (isFit(find.getAbsolutePath())) {
                    xml = find.getAbsolutePath();
                    break;
                }
            }

        }

        //get module's name
        final String[] tmp = xml.replace("/", "\\").replace(SUFFIX.FLAG, "").split("\\\\");
        final String module = tmp[tmp.length - 1];

        System.out.println("Find config xml :" + xml + ", and target module is " + module);


        // step 2
        ArrayList<String> keys = XmlHelper.parse(xml);

        File f = null;
        String path = commonpath.replace("common", module);
        //find default dir
        if (new File(path).exists()) {
            packageName = packageName.replace("common", module);
            f = new File(path);
            if (f.exists()) {
                long time = f.lastModified();
                if (time == updateTime) {
                    System.out.println("not need to re build :" + path);
                    return;
                }
                f.delete();
            } else {
                String parent = new File(path).getParent();
                if (!new File(parent).exists()) {
                    if (!new File(parent).mkdirs()) {
                        System.out.println("Faied to make dir : " + parent);
                        return;
                    }
                }
            }
        }

        System.out.println("begin to write:" + f.getAbsolutePath());

        FileWriter fileWriter = null;
        BufferedWriter writer = null;
        String line = "    public final static String PLACE = \"PLACE\";";
        try {
            fileWriter = new FileWriter(path);
            writer = new BufferedWriter(fileWriter);
            // write begin
            writeLine(writer, "/**");
            writeLine(writer, " * Automatically generated file. DO NOT MODIFY");
            writeLine(writer, " */");
            writeLine(writer, "package " + packageName + ";");
            writeLine(writer, "public class " + className + " {");
            // write content
            // all like this public final static String LOGIN = "LOGIN";
            for (String key : keys)
                writeLine(writer, line.replace("PLACE", key));
            // write end
            writeLine(writer, "}");
            writer.flush();
            writer.close();
            writer = null;
            // record it~
            updateTime = new File(path).lastModified();
        } catch (Exception e) {
            System.out.println(e == null ? "" : e.getMessage());
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception ee) {
                System.out.println(ee == null ? "" : ee.getMessage());
            }
        }
    }

    private static void searchAllXML(File root, ArrayList<File> result) {
        if (root.isFile()) {
            if (root.getAbsolutePath().replace("/", "\\").contains(SUFFIX.FLAG)) {
                result.add(root);
            }
        } else {
            for (File f : root.listFiles()) {
                searchAllXML(f, result);
            }
        }
    }

    private static boolean isFit(String path) {
        path = path.replace("/", "\\");
        return path != null && !path.contains(SUFFIX.COMMON)
                && !path.contains(SUFFIX.MALSITE)
                && !path.contains(SUFFIX.MULTSITE);
    }

    private static void writeLine(BufferedWriter writer, String line)
            throws IOException {
        writer.write(line);
        writer.newLine();
    }

    public static void aop(String path) {
        // 插入代码
    }

    public static class AopClass {

        String classFqcn;
        String className;
        String params;

        public AopClass(String classPackage, String className, String classFqcn) {
            this.className = className;
            this.classFqcn = classFqcn;
        }

        public void addParams(String params) {
            this.params = params;
        }

        public void brewJava(Filer filer) {
            aopMap();
            System.out.println("brewJava:" + classFqcn);
            // JavaFileManager.Location location =
            // StandardLocation.locationFor(StandardLocation.SOURCE_PATH);
            // JavaFileManager.Location location = new
            // JavaFileManager.Location()
            // JavaFileObject jfo = filer.createSourceFile(classFqcn);
            // Writer writer = jfo.openWriter();

        }
    }

    public static void main(String[] args) {
        System.out.println("main");
    }

}
