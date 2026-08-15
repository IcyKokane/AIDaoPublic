package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic release-oriented validation for generated Android source trees.
 * This performs structural checks only; it never executes generated content.
 */
final class GeneratedProjectValidator {
    static final class Result {
        final List<String> notes;
        final boolean valid;
        Result(List<String> notes, boolean valid) {
            this.notes = notes;
            this.valid = valid;
        }
    }

    Result validate(GeneratedProject project) {
        if (project == null) {
            List<String> notes = new ArrayList<>();
            notes.add("FAIL generated project is null");
            return new Result(notes, false);
        }
        return validateRaw(project.packageName, project.files);
    }

    static Result validateRaw(String packageName, List<GeneratedProject.FileEntry> files) {
        List<String> notes = new ArrayList<>();
        boolean ok = true;
        if (packageName == null || packageName.trim().isEmpty()) {
            notes.add("FAIL generated package name is empty");
            ok = false;
            packageName = "invalid";
        }
        if (files == null) {
            notes.add("FAIL generated file list is null");
            return new Result(notes, false);
        }

        Set<String> seen = new HashSet<>();
        for (GeneratedProject.FileEntry file : files) {
            if (file == null || file.path == null || file.path.trim().isEmpty()) {
                notes.add("FAIL generated file has an empty path");
                ok = false;
                continue;
            }
            if (file.path.startsWith("/") || file.path.contains("../") || file.path.equals("..")) {
                notes.add("FAIL unsafe generated path: " + file.path);
                ok = false;
            }
            if (!seen.add(file.path)) {
                notes.add("FAIL duplicate generated path: " + file.path);
                ok = false;
            }
            if (file.content == null) {
                notes.add("FAIL null generated content: " + file.path);
                ok = false;
            }
        }

        String[] required = {
                "settings.gradle.kts",
                "build.gradle.kts",
                "app/build.gradle.kts",
                "app/src/main/AndroidManifest.xml",
                "app/src/main/res/values/strings.xml",
                "app/src/main/res/values/styles.xml"
        };
        for (String path : required) {
            if (!hasPath(files, path)) {
                notes.add("FAIL missing required Android file: " + path);
                ok = false;
            } else {
                notes.add("PASS required Android file: " + path);
            }
        }

        String packageRoot = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        if (!hasPath(files, packageRoot + "MainActivity.java")) {
            notes.add("FAIL missing launcher source under package root");
            ok = false;
        } else {
            notes.add("PASS launcher source under package root");
        }

        GeneratedProject.FileEntry manifest = find(files, "app/src/main/AndroidManifest.xml");
        if (manifest != null) {
            String body = manifest.content == null ? "" : manifest.content;
            if (!body.contains("android.intent.action.MAIN") || !body.contains("android.intent.category.LAUNCHER")) {
                notes.add("FAIL manifest does not declare a launcher activity");
                ok = false;
            } else {
                notes.add("PASS manifest launcher declaration");
            }
            if (body.contains("android:debuggable=\"true\"")) {
                notes.add("FAIL generated manifest hard-codes debuggable=true");
                ok = false;
            }
        }

        if (files.size() < 12) {
            notes.add("FAIL generated tree is too small for v1 acceptance: " + files.size() + " files");
            ok = false;
        } else {
            notes.add("PASS nontrivial generated tree: " + files.size() + " files");
        }

        return new Result(notes, ok);
    }

    private static boolean hasPath(List<GeneratedProject.FileEntry> files, String path) {
        return find(files, path) != null;
    }

    private static GeneratedProject.FileEntry find(List<GeneratedProject.FileEntry> files, String path) {
        for (GeneratedProject.FileEntry file : files) if (file != null && path.equals(file.path)) return file;
        return null;
    }
}
