package main;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fab on 11/10/2015
 */
public class StaticAnalyzer {

    private enum OS {Windows, OSX}

    private OS currentOs;
    private String javaHome, javaHome2, apkName, apkLocation, dexDirectory, decompiledDirectory, pathManifest;

    public static void main(String[] args) {
        StaticAnalyzer staticAnalyzer = new StaticAnalyzer();
        staticAnalyzer.determineOS();
        staticAnalyzer.determineJavaHome();

        //Choose the file to analyze
        staticAnalyzer.chooseApk();

        //Decompile the apk
        staticAnalyzer.doApkTool();
        staticAnalyzer.doDex2Jar();
        staticAnalyzer.toJavaSourceCode();
        //Analyze
        staticAnalyzer.analyze();

    }

    private synchronized void determineOS() {
        String os = System.getProperty("os.name");
        if (os.contains("Windows"))
            currentOs = OS.Windows;
        else if (os.contains("Mac"))
            currentOs = OS.OSX;
    }

    private synchronized void determineJavaHome() {
        javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            String path = System.getenv("PATH");
            String[] pathArray = path.split(";");
            for (String item : pathArray) {
                if (item.contains("javapath"))
                    javaHome = item;
            }
        }
        if (javaHome == null && currentOs == OS.OSX) {
            javaHome = "/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin";
        }
        switch (currentOs) {
            case Windows:
                javaHome2 = javaHome + "\\bin\\java";
                javaHome = javaHome + "\\java";
                break;
            case OSX:
                javaHome = javaHome + "/java";
                break;
        }
    }

    private synchronized void chooseApk() {
        JFrame frame = new JFrame();
        frame.setVisible(true);
        frame.setExtendedState(JFrame.ICONIFIED);
        frame.setExtendedState(JFrame.NORMAL);

        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
        if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(null)) {
            frame.setVisible(false);
            apkName = fc.getSelectedFile().getName().replace(".apk", "");
            apkLocation = fc.getSelectedFile().getAbsolutePath();
            frame.dispose();
        } else {
            System.out.println("Next time select a file.");
            System.exit(1);
        }
    }

    private synchronized void doApkTool() {
        List<String> arguments = new ArrayList<>();
        arguments.add(javaHome);
        arguments.add("-jar");
        arguments.add("./tools/apktool.jar");
        arguments.add("d");
        arguments.add(apkLocation);
        arguments.add("-f");
        arguments.add("-o");
        arguments.add("output/" + apkName);

        switch (currentOs) {
            case Windows:
                ProcessBuilder pb = new ProcessBuilder(arguments);
                try {
                    Process p = pb.start();
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    try {
                        arguments.set(0, javaHome2);
                        pb = new ProcessBuilder(arguments);
                        Process p = pb.start();
                        p.waitFor();
                    } catch (IOException | InterruptedException e2){
                        e2.printStackTrace();
                    }
                }
                break;
            case OSX:
                pb = new ProcessBuilder(arguments);
                try {
                    Process p = pb.start();
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private synchronized void doDex2Jar() {
        createFolderForDex2Jar();

        List<String> arguments = new ArrayList<>();
        switch (currentOs) {
            case Windows:
                arguments.add("./tools/dex2jar/d2j-dex2jar.bat");
                arguments.add(apkLocation);
                arguments.add("-f");
                arguments.add("-o");
                arguments.add(dexDirectory);

                ProcessBuilder pb = new ProcessBuilder(arguments);
                try {
                    Process p = pb.start();
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case OSX:
                arguments.add("sh");
                arguments.add("./tools/dex2jar/d2j-dex2jar.sh");
                arguments.add(apkLocation);
                arguments.add("-f");
                arguments.add("-o");
                arguments.add(dexDirectory);

                ProcessBuilder permissionProcess = new ProcessBuilder("chmod", "+x", "./tools/dex2jar/d2j_invoke.sh");
                pb = new ProcessBuilder(arguments);
                try {
                    Process permP = permissionProcess.start();
                    permP.waitFor();
                    Process p = pb.start();
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    /**
     * Helper method for doDex2Jar
     */
    private synchronized void createFolderForDex2Jar() {
        switch (currentOs) {
            case Windows:
                dexDirectory = ".\\output\\" + apkName + "\\" + "sources";
                new File(dexDirectory).mkdir();
                break;
            case OSX:
                dexDirectory = "./output/" + apkName + "/" + "sources";
                new File(dexDirectory).mkdir();
                break;
        }
    }

    /**
     * Returns the package name contained in the manifest file
     */
    private synchronized String getPackageName() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document document;
        String packageName = null;
        pathManifest = getPathManifest();

        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(new File(pathManifest));
            Element root  = document.getDocumentElement();

            packageName = root.getAttributes().getNamedItem("package").getNodeValue();
        }
        catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return packageName;
    }

    /**
     * Helper method for getPackageName
     */
    private String getPathManifest() {
        switch (currentOs) {
            case Windows:
                pathManifest = ".\\output\\" + apkName + "\\AndroidManifest.xml";
                break;
            case OSX:
                pathManifest = "./output/" + apkName + "/AndroidManifest.xml";
                break;
        }

        return pathManifest;
    }

    /**
     * Decompile classes to Java source code
     */
    private synchronized void toJavaSourceCode() {
        File classPath = new File (getClassPath());
        createFolderForDecompiled();
        List<String> classes = getClassNames(classPath);

        //output directory
        File outputDir = new File(decompiledDirectory);

        for (String className : classes) {
            decompileClass(outputDir, classPath, className);
        }
    }

    private synchronized void decompileClass(File outputDir, File classPath, String className) {
        PrintWriter writer = null;
        String fileOut = null;
        String classPathAndName = null;

        switch (currentOs) {
            case Windows:
                fileOut = outputDir + "\\" + className.replace(".class", ".java");
                classPathAndName = classPath + "\\" + className;
                break;
            case OSX:
                fileOut = outputDir + "/" + className.replace(".class", ".java");
                classPathAndName = classPath + "/" + className;
                break;
            }

        try {
            writer = new PrintWriter(fileOut);


            com.strobel.decompiler.Decompiler.decompile(
                    classPathAndName,
                    new com.strobel.decompiler.PlainTextOutput(writer)
            );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            writer.flush();
        }
    }

    public List<String> getClassNames(final File folder) {
        List<String> classNames = new ArrayList<>();

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isFile()) {
                String className = fileEntry.getName();
                //ignores classes with $ notation
                if (!className.contains("$")) {
                    classNames.add(className);
                }
            }
        }

        return classNames;
    }

    private synchronized String getClassPath() {
        String packageName = getPackageName();
        String classpath = null;
        switch (currentOs) {
            case Windows:
                classpath = dexDirectory + "\\" + packageName.replace(".","\\");
                break;
            case OSX:
                classpath = dexDirectory + "/" + packageName.replace(".","/");
                break;
        }
        System.out.println("classpath: " + classpath);

        return classpath;
    }

    /**
     * Helper method for classToJava
     */
    private synchronized void createFolderForDecompiled() {
        switch (currentOs) {
            case Windows:
                decompiledDirectory = ".\\output\\" + apkName + "JavaSourceCode";
                new File(decompiledDirectory).mkdir();
                break;
            case OSX:
                decompiledDirectory = "./output/" + apkName + "JavaSourceCode";
                new File(decompiledDirectory).mkdir();
                break;
        }
    }

    /**
     * Reads the java files, line by line
     */
    private synchronized void analyze() {
        File folder = new File(decompiledDirectory);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isFile()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(fileEntry.getAbsolutePath()));
                    String line;

                    System.out.println("Analysing Java File: " + fileEntry.getName());
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                        /**
                         * Code analysis
                         *
                         *
                         *
                         *
                         *
                         *
                         */
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
