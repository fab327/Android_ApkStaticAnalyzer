package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fab on 11/17/15
 */
public class ApkToolHelper {

    public static void doApkTool(String javaHome, String javaHomeAlternative, String apkLocation, String apkName, StaticAnalyzer.OS currentOs) {
        System.out.println("--------------------------------------------------------------------------------------");
        System.out.println("Starting apkTool decompilation" + "\n");
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
                        arguments.set(0, javaHomeAlternative);
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

}
