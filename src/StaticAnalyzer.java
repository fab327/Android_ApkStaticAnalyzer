import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fab on 11/10/2015
 */
public class StaticAnalyzer {

    private enum OS {Windows, OSX}

    private OS currentOs;
    private String javaHome, apkName, apkLocation, dexDirectory;

    public static void main(String[] args) {
        StaticAnalyzer staticAnalyzer = new StaticAnalyzer();
        staticAnalyzer.determineOS();
        staticAnalyzer.determineJavaHome();

        //Choose the file to analyze
        staticAnalyzer.chooseApk();

        //Decompile the apk
        staticAnalyzer.doApkTool();
        staticAnalyzer.doDex2Jar();

        //Analyze
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
                    e.printStackTrace();
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
                dexDirectory = ".\\output\\" + apkName + "Classes";
                new File(dexDirectory).mkdir();
                break;
            case OSX:
                dexDirectory = "./output/" + apkName + "Classes";
                break;
        }
//        List<String> arguments = new ArrayList<>();
//        arguments.add("mkdir");
//        arguments.add(dexDirectory);
//
//
//        ProcessBuilder pb = new ProcessBuilder(arguments);
//        try {
//            Process p = pb.start();
//            p.waitFor();
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
    }

}
