package com.orbis.stream;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainLauncher {
    public static void main(String[] args) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isMac = os.contains("mac");
        boolean isWindows = os.contains("win");
        boolean isLinux = os.contains("nux") || os.contains("nix");
        boolean hasArgs = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("sun.awt");

        if (!hasArgs) {
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
            if (isWindows) {
                javaBin += ".exe";
            }

            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("--add-opens=java.desktop/sun.awt=ALL-UNNAMED");
            command.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED");

            if (isMac) {
                command.add("--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED");
                command.add("--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED");
                command.add("--add-exports=java.desktop/sun.lwawt=ALL-UNNAMED");
                command.add("--add-exports=java.desktop/sun.lwawt.macosx=ALL-UNNAMED");
            }

            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add("com.orbis.stream.MainLauncher");

            Collections.addAll(command, args);

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.inheritIO();
            Process process = builder.start();
            System.exit(process.waitFor());
        } else {
            com.orbis.stream.StreamApplication.main(args);
        }
    }
}