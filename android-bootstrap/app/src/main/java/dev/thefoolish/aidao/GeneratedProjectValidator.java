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
        List<String> notes = new ArrayList<>();
        boolean ok = true;
        if (project == null) {
            notes.add("FAIL generated project is null");
            return new Result(notes, false);
        }

        Set<String> seen = new HashSet<>();
        for (GeneratedProject.FileEntry file : project.files) {
            if (file.path == null || file.path.trim().isEmpty()) {
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
            if (!project.hasPath(path)) {
                notes.add("FAIL missing required Android file: " + path);
                ok = false;
            } else {
                notes.add("PASS required Android file: " + path);
            }
        }

        String packageRoot = "app/src/main/java/" + project.packageName.replace('.', '/') + "/";
        if (!project.hasPath(packageRoot + "MainActivity.java")) {
            notes.add("FAIL missing launcher source under package root");
            ok = false;
        } else {
            notes.add("PASS launcher source under package root");
        }

        GeneratedProject.FileEntry manifest = project.find("app/src/main/AndroidManifest.xml");
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

        if (project.files.size() < 12) {
            notes.add("FAIL generated tree is too small for v1 acceptance: " + project.files.size() + " files");
            ok = false;
        } else {
            notes.add("PASS nontrivial generated tree: " + project.files.size() + " files");
        }

        return new Result(notes, ok);
    }
}
