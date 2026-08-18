package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic release-oriented validation for generated Android source trees.
 *
 * This validator inspects source text only. It never executes generated content.
 * The goal is to reject malformed, unexpectedly privileged, semantically fake,
 * or structurally inconsistent source before a user can explicitly authorize a remote build.
 */
final class GeneratedProjectValidator {
    private static final int MAX_FILE_COUNT = 400;
    private static final int MAX_FILE_CHARS = 1_000_000;
    private static final long MAX_TOTAL_CHARS = 8_000_000L;

    private static final String[] UNSUPPORTED_PRIVILEGED_PERMISSIONS = {
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.WRITE_SECURE_SETTINGS",
            "android.permission.BIND_DEVICE_ADMIN",
            "android.permission.BIND_ACCESSIBILITY_SERVICE"
    };

    private static final String[] FORBIDDEN_GENERATED_JAVA_MARKERS = {
            "android.widget.android.widget.",
            "android.graphics.android.graphics.",
            "android.content.android.content.",
            "android.app.android.app.",
            "Save local sample state",
            "placeholder data"
    };

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
        } else if (!packageName.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")) {
            notes.add("FAIL generated package name is not a valid dotted Java package: " + packageName);
            ok = false;
        } else {
            notes.add("PASS generated package name syntax");
        }

        if (files == null) {
            notes.add("FAIL generated file list is null");
            return new Result(notes, false);
        }
        if (files.size() > MAX_FILE_COUNT) {
            notes.add("FAIL generated tree exceeds bounded file count: " + files.size());
            ok = false;
        }

        Set<String> seen = new HashSet<>();
        long totalChars = 0L;
        for (GeneratedProject.FileEntry file : files) {
            if (file == null || file.path == null || file.path.trim().isEmpty()) {
                notes.add("FAIL generated file has an empty path");
                ok = false;
                continue;
            }
            if (unsafePath(file.path)) {
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
                continue;
            }
            if (file.content.indexOf('\u0000') >= 0) {
                notes.add("FAIL NUL byte marker in generated text: " + file.path);
                ok = false;
            }
            if (file.content.length() > MAX_FILE_CHARS) {
                notes.add("FAIL generated file exceeds bounded text size: " + file.path);
                ok = false;
            }
            if (file.path.endsWith(".java")) {
                for (String marker : FORBIDDEN_GENERATED_JAVA_MARKERS) {
                    if (file.content.contains(marker)) {
                        notes.add("FAIL corrupted/placeholder Java marker '" + marker + "' in " + file.path);
                        ok = false;
                    }
                }
            }
            totalChars += file.content.length();
        }
        if (totalChars > MAX_TOTAL_CHARS) {
            notes.add("FAIL generated tree exceeds bounded total text size: " + totalChars + " characters");
            ok = false;
        } else {
            notes.add("PASS bounded generated source size");
        }
        if (ok || !containsForbiddenJavaMarker(files)) notes.add("PASS generated Java source hygiene markers");

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

        for (GeneratedProject.FileEntry file : files) {
            if (file == null || file.path == null || file.content == null) continue;
            if (file.path.startsWith(packageRoot) && file.path.endsWith(".java")) {
                if (!declaresPackage(file.content, packageName)) {
                    notes.add("FAIL Java source package does not match generated package: " + file.path);
                    ok = false;
                }
            }
        }

        GeneratedProject.FileEntry appGradle = find(files, "app/build.gradle.kts");
        if (appGradle != null) {
            String body = appGradle.content == null ? "" : appGradle.content;
            String namespace = quotedAssignment(body, "namespace");
            String applicationId = quotedAssignment(body, "applicationId");
            if (!packageName.equals(namespace)) {
                notes.add("FAIL Gradle namespace does not match generated package");
                ok = false;
            } else {
                notes.add("PASS Gradle namespace matches generated package");
            }
            if (!packageName.equals(applicationId)) {
                notes.add("FAIL Gradle applicationId does not match generated package");
                ok = false;
            } else {
                notes.add("PASS Gradle applicationId matches generated package");
            }
            int compileSdk = numericAssignment(body, "compileSdk");
            int targetSdk = numericAssignment(body, "targetSdk");
            int minSdk = numericAssignment(body, "minSdk");
            if (compileSdk < 33 || targetSdk < 33 || minSdk < 23 || minSdk > targetSdk || targetSdk > compileSdk) {
                notes.add("FAIL generated Android SDK bounds are inconsistent or below the v1 baseline");
                ok = false;
            } else {
                notes.add("PASS generated Android SDK bounds");
            }
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
            if (!body.contains("android:name=\".MainActivity\"")) {
                notes.add("FAIL manifest launcher source is not .MainActivity");
                ok = false;
            }
            if (body.contains("android:debuggable=\"true\"")) {
                notes.add("FAIL generated manifest hard-codes debuggable=true");
                ok = false;
            }
            for (String permission : UNSUPPORTED_PRIVILEGED_PERMISSIONS) {
                if (body.contains(permission)) {
                    notes.add("FAIL generated manifest requests unsupported privileged permission: " + permission);
                    ok = false;
                }
            }
            if (!containsUnsupportedPrivilege(body)) {
                notes.add("PASS no unsupported privileged Android permissions");
            }
        }

        GeneratedProject.FileEntry workflow = find(files, ".github/workflows/android.yml");
        if (workflow != null) {
            String body = workflow.content == null ? "" : workflow.content;
            String lower = body.toLowerCase();
            if (lower.contains("pull_request_target") || lower.contains("permissions: write-all") || lower.contains("secrets.")) {
                notes.add("FAIL generated workflow contains a privileged trigger/permission/secret reference");
                ok = false;
            } else {
                notes.add("PASS generated workflow contains no privileged trigger/secret reference");
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

    private static boolean containsForbiddenJavaMarker(List<GeneratedProject.FileEntry> files) {
        for (GeneratedProject.FileEntry file : files) {
            if (file == null || file.path == null || file.content == null || !file.path.endsWith(".java")) continue;
            for (String marker : FORBIDDEN_GENERATED_JAVA_MARKERS) if (file.content.contains(marker)) return true;
        }
        return false;
    }

    private static boolean unsafePath(String path) {
        return path.startsWith("/") || path.startsWith("\\") || path.contains("../") || path.contains("..\\")
                || path.equals("..") || path.indexOf('\u0000') >= 0 || path.contains(":") || path.contains("\\");
    }

    private static boolean declaresPackage(String source, String packageName) {
        Pattern p = Pattern.compile("(?m)^\\s*package\\s+" + Pattern.quote(packageName) + "\\s*;");
        return p.matcher(source).find();
    }

    private static String quotedAssignment(String source, String name) {
        Matcher m = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*=\\s*\"([^\"]+)\"").matcher(source);
        return m.find() ? m.group(1) : null;
    }

    private static int numericAssignment(String source, String name) {
        Matcher m = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*=\\s*(\\d+)").matcher(source);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static boolean containsUnsupportedPrivilege(String body) {
        for (String permission : UNSUPPORTED_PRIVILEGED_PERMISSIONS) if (body.contains(permission)) return true;
        return false;
    }

    private static boolean hasPath(List<GeneratedProject.FileEntry> files, String path) {
        return find(files, path) != null;
    }

    private static GeneratedProject.FileEntry find(List<GeneratedProject.FileEntry> files, String path) {
        for (GeneratedProject.FileEntry file : files) if (file != null && path.equals(file.path)) return file;
        return null;
    }
}
