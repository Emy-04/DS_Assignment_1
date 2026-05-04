package com.example.util;

import java.io.File;
import java.net.URISyntaxException;

public class PathResolver {

    private static File dataDir = null;

    public static String resolve(String filename) {
        String name = filename
                .replaceAll("^data[/\\\\]", "")
                .replaceAll("^.*[/\\\\]", "");
        File dir = getDataDir();
        if (dir != null) {
            File f = new File(dir, name);
            if (f.exists()) return f.getAbsolutePath();
            return f.getAbsolutePath();
        }
        return filename;
    }

    public static File getDataDir() {
        if (dataDir != null && dataDir.exists()) return dataDir;

        File wd = new File(System.getProperty("user.dir"), "data");
        if (wd.exists()) { dataDir = wd; return dataDir; }

        try {
            File classRoot = new File(
                    PathResolver.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI());
            File candidate = new File(classRoot, "data");
            if (candidate.exists()) { dataDir = candidate; return dataDir; }
            candidate = new File(classRoot.getParentFile(), "data");
            if (candidate.exists()) { dataDir = candidate; return dataDir; }
        } catch (URISyntaxException ignored) {}

        File dir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 6; i++) {
            File candidate = new File(dir, "data");
            if (candidate.exists() && new File(candidate, "questions.txt").exists()) {
                dataDir = candidate;
                return dataDir;
            }
            dir = dir.getParentFile();
            if (dir == null) break;
        }

        File fallback = new File(System.getProperty("user.dir"), "data");
        fallback.mkdirs();
        dataDir = fallback;
        return dataDir;
    }
}