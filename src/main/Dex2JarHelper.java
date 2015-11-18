package main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fab on 11/17/15
 * Singleton class
 */
public class Dex2JarHelper {

    private StaticAnalyzer.OS currentOs;
    private String apkLocation;
    private String dexDirectory;
    private String apkName;
    private static Dex2JarHelper dex2JarHelper;

    private Dex2JarHelper() { }

    public static Dex2JarHelper getInstance(StaticAnalyzer.OS currentOs, String apkLocation, String apkName) {
        if (dex2JarHelper == null) {
            dex2JarHelper = new Dex2JarHelper();
            dex2JarHelper.currentOs = currentOs;
            dex2JarHelper.apkLocation = apkLocation;
            dex2JarHelper.apkName = apkName;
        }
      return dex2JarHelper;
    }

    public String getDexDirectory() {
        return this.dexDirectory;
    }

    public void doDex2Jar() {
        createFolderForDex2Jar();

        System.out.println("--------------------------------------------------------------------------------------");
        System.out.println("Starting dex2jar conversion" + "\n");
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
    private void createFolderForDex2Jar() {
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

}
