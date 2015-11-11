import java.io.IOException;

/**
 * Created by fab on 11/10/2015.
 */
public class StaticAnalyzer {

    private enum OS { Windows, OSX }
    private OS currentOs;
    private String javaHome;

    public static void main(String[] args) {
        StaticAnalyzer staticAnalyzer = new StaticAnalyzer();
        staticAnalyzer.determineOS();
        staticAnalyzer.determineJavaHome();

        //Test calling out the tools
        staticAnalyzer.doApkTool();
        staticAnalyzer.doDex2Jar();
    }

    private synchronized void determineOS() {
        String os = System.getProperty("os.name");
        if (os.contains("Windows"))
            currentOs = OS.Windows;
        else
            currentOs = OS.OSX;
    }

    private synchronized void determineJavaHome() {
        javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            String path = System.getenv("PATH");
            String[] pathArray = path.split(";");
            for (String item: pathArray) {
                if (item.contains("javapath"))
                    javaHome = item;
            }
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

    private synchronized void doApkTool() {
        switch (currentOs) {
            case Windows:
                ProcessBuilder pb = new ProcessBuilder(javaHome, "-jar", "./tools/apktool.jar", "d", "./samples/FindAndCallSpyware.apk");
//                Map<String, String> env = pb.environment();
//                env.put("VAR1", "myValue");
//                env.remove("OTHERVAR");
//                env.put("VAR2", env.get("VAR1") + "suffix");
//                pb.directory(new File("./output"));
                try {
                    Process p = pb.start();
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case OSX:

                break;
        }
    }

    private synchronized void doDex2Jar() {
        switch (currentOs) {
            case Windows:
                ProcessBuilder pb = new ProcessBuilder("./tools/dex2jar/d2j-dex2jar.bat", "./samples/TGLoader.apk");
                try {
                    Process p = pb.start();
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case OSX:

                break;
        }
    }

}
