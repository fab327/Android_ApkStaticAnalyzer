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
        getAndDecompileClasses(outputDir, classPath.listFiles());
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

    private void getAndDecompileClasses(File outputDir, File[] files) {
        for (File file : files) {
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

    private void giveDiagnosis(int analyzisScore) {
        switch (analyzisScore/10){
            case 0:
                System.out.println("This app seems beningn, nothing to worry about");
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            case 7:
                break;
            case 8:
                break;
            case 9:
                System.out.println("This app requires a lot of invasive permissions, it is recommended not to install it");
                break;
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                System.out.println("This app is extremely dangerous!");
                break;
        }
    }

}
