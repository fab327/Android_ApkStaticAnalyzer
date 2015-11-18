package main;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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

        //Match it to its closest category
        AnalyzerHelper analyzerHelper = AnalyzerHelper.getInstance();
//        staticAnalyzer.matchApkToClosestCategory();
        analyzerHelper.setApplicationType(AnalyzerHelper.ApplicationType.Others);

        //Decompile the apk
        ApkToolHelper.doApkTool(javaHome, javaHomeAlternative, apkLocation, apkName, currentOs);
        Dex2JarHelper dex2JarHelper = Dex2JarHelper.getInstance(currentOs, apkLocation, apkName);
        dex2JarHelper.doDex2Jar();
        dexDirectory = dex2JarHelper.getDexDirectory();

        //Decompile the classes so they can be analyzed
        staticAnalyzer.dexToJava();

        //Analyze the data
        analyzerHelper.analyzeManifestPermissions(manifestPath);
        analyzerHelper.analyzeJavaSources(decompiledDirectory);

        staticAnalyzer.giveDiagnosis(analyzerHelper.getMalwareLikelihood());
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

    private synchronized void matchApkToClosestCategory() {
        //If time permits add a selection dropdown for the user to choose the closest category matching the apk
    }

    /**
     * Decompile classes to Java source code
     */
    private synchronized void dexToJava() {
        System.out.println("--------------------------------------------------------------------------------------");
        System.out.println("Starting decompiling classes to java" + "\n");
        File classPath = new File(getClassPath());
        createFolderForDecompiled();
        File outputDir = new File(decompiledDirectory);
        getAndDecompileClasses(outputDir, classPath);
    }

    private synchronized String getClassPath() {
        String packageName = getPackageName();
        String classpath = null;
        switch (currentOs) {
            case Windows:
                classpath = dexDirectory + "\\" + packageName.replace(".", "\\");
                break;
            case OSX:
                classpath = dexDirectory + "/" + packageName.replace(".", "/");
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
            Element root = document.getDocumentElement();

            packageName = root.getAttributes().getNamedItem("package").getNodeValue();
        } catch (ParserConfigurationException | SAXException | IOException e) {
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

    private void getAndDecompileClasses(File outputDir, File folder) {
        File [] files = folder.listFiles();
        if (files == null) {
            System.out.println("Error: There are no .class files to decompile");
        } else {
            int classCount = 1;
            int totalClasses = countRelevantClasses(files);

            System.out.println("Decompiling " + totalClasses + " classes at folder: " + folder.getName());
            for (File file : files) {
                if (file.isDirectory()) {
                    getAndDecompileClasses(outputDir, file);
                } else if (file.isFile()) {
                    String className = file.getName();
                    if (!className.contains("$")) {
                        System.out.println("Decompiling " + className + " (" + classCount + " out of " + totalClasses + ")");
                        decompileClass(outputDir, file.getAbsolutePath(), file.getName());
                        classCount++;
                    }
                }
            }
        }
    }

    /**
     * Counts relevant classes
     */
    private synchronized int countRelevantClasses(File[] files) {
        int count = 0;
        for (File file : files) {
            if (file.isFile() && !file.getName().contains("$")) {
                count++;
            }
        }
        return count;
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

    private void giveDiagnosis(int analyzisScore) {
        switch (analyzisScore/10){
            case 0:
                System.out.println("This app seems benign, nothing to worry about.");
                break;
            case 1:
                System.out.println("This app requires a few benign permissions.");
                break;
            case 2:
            case 3:
                System.out.println("This app requires a few invasive permissions, it is recommended not to install it.");
                break;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
                System.out.println("This app requires invasive permissions, it is recommended not to install it.");
                break;
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
                System.out.println("This app is extremely dangerous!");
                break;
        }
    }

}
