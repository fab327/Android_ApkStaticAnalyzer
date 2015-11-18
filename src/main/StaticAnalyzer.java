package main;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

/**
 * Created by fab on 11/10/2015
 */
public class StaticAnalyzer {

    public enum OS {Windows, OSX}

    private static OS currentOs;
    private static String javaHome, javaHomeAlternative, apkName, apkLocation, dexDirectory, decompiledDirectory, manifestPath;

    public static void main(String[] args) {
        StaticAnalyzer staticAnalyzer = new StaticAnalyzer();
        staticAnalyzer.determineOS();
        staticAnalyzer.determineJavaHome();

        //Choose the file to analyze
        staticAnalyzer.chooseApk();

        //Decompile the apk
        ApkToolHelper.doApkTool(javaHome, javaHomeAlternative, apkLocation, apkName, currentOs);
        Dex2JarHelper dex2JarHelper = Dex2JarHelper.getInstance(currentOs, apkLocation, apkName);
        dex2JarHelper.doDex2Jar();
        dexDirectory = dex2JarHelper.getDexDirectory();

        //Decompile the classes so they can be analyzed
        staticAnalyzer.dexToJava();

        //Analyze the data
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
                javaHomeAlternative = javaHome + "\\bin\\java";
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

    /**
     * Decompile classes to Java source code
     */
    private synchronized void dexToJava() {
        File classPath = new File (getClassPath());
        createFolderForDecompiled();
        File outputDir = new File(decompiledDirectory);
        getAndDecompileClasses(outputDir, classPath.listFiles());
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

        return classpath;
    }

    /**
     * Returns the package name contained in the manifest file
     */
    private synchronized String getPackageName() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document document;
        String packageName = null;
        manifestPath = getManifestPath();

        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(new File(manifestPath));
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
    private String getManifestPath() {
        switch (currentOs) {
            case Windows:
                manifestPath = ".\\output\\" + apkName + "\\AndroidManifest.xml";
                break;
            case OSX:
                manifestPath = "./output/" + apkName + "/AndroidManifest.xml";
                break;
        }

        return manifestPath;
    }

    /**
     * Helper method for classToJava
     */
    private synchronized void createFolderForDecompiled() {
        switch (currentOs) {
            case Windows:
                decompiledDirectory = ".\\output\\" + apkName + "\\" + "javaSource";
                new File(decompiledDirectory).mkdir();
                break;
            case OSX:
                decompiledDirectory = "./output/" + apkName + "/" + "javaSource";
                new File(decompiledDirectory).mkdir();
                break;
        }
    }

    private void getAndDecompileClasses(File outputDir, File[] files) {
        for (File file: files) {
            if (file.isDirectory()) {
                getAndDecompileClasses(outputDir, file.listFiles());
            } else if (file.isFile()) {
                if (!file.getName().contains("$")) {
                    decompileClass(outputDir, file.getAbsolutePath(), file.getName());
                }
            }
        }
    }

    /**
     * Helper method
     */
    private synchronized void decompileClass(File outputDir, String classPath, String className) {
        PrintWriter writer = null;
        String fileOut = null;

        switch (currentOs) {
            case Windows:
                fileOut = outputDir + "\\" + className.replace(".class", ".java");
                break;
            case OSX:
                fileOut = outputDir + "/" + className.replace(".class", ".java");
                break;
            }

        try {
            writer = new PrintWriter(fileOut);


            com.strobel.decompiler.Decompiler.decompile(
                    classPath,
                    new com.strobel.decompiler.PlainTextOutput(writer)
            );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Decompiled: " + className + " to .java");
            writer.flush();
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
