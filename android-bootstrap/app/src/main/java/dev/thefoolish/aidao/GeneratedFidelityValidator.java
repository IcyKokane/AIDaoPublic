package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;

/** Semantic acceptance gate for generated source. Compilation alone is insufficient. */
final class GeneratedFidelityValidator {
    static List<String> validate(String packageName, List<GeneratedProject.FileEntry> files) {
        List<String> notes = new ArrayList<>();
        boolean media = hasSuffix(files, "/MediaProvider.java") || hasSuffix(files, "/AnimeItem.java");
        if (!media) return notes;

        require(notes, files, packageName, "MainActivity.java", "Browse/Search implementation");
        require(notes, files, packageName, "DetailActivity.java", "detail/episode implementation");
        require(notes, files, packageName, "LibraryActivity.java", "library implementation");
        require(notes, files, packageName, "HistoryActivity.java", "history implementation");
        require(notes, files, packageName, "DownloadsActivity.java", "downloads implementation");
        require(notes, files, packageName, "ProvidersActivity.java", "extensions lifecycle implementation");
        require(notes, files, packageName, "RepositoriesActivity.java", "repository management implementation");
        require(notes, files, packageName, "ExtensionRepositoryClient.java", "repository sync implementation");
        require(notes, files, packageName, "ExtensionManager.java", "extension state implementation");
        require(notes, files, packageName, "RepositoryMediaProvider.java", "provider isolation implementation");

        String joined = joinExecutableSource(files);
        failIf(notes, joined, "DemoProvider", "fabricated DemoProvider placeholder is forbidden in executable source");
        failIf(notes, joined, "Origin Path", "fabricated catalog data is forbidden in executable source");
        failIf(notes, joined, "Sky Archive", "fabricated catalog data is forbidden in executable source");
        failIf(notes, joined, "sample data only", "sample-only provider cannot satisfy a repository-provider request");

        boolean repoBehavior = joined.contains("RepositoryStore") && joined.contains("https://") &&
                joined.contains("ExtensionRepositoryClient") && joined.contains("AVAILABLE") &&
                joined.contains("ENABLED") && joined.contains("FAILED");
        notes.add((repoBehavior ? "PASS " : "FAIL ") + "semantic repository/extension lifecycle gate");

        boolean nativeUi = joined.contains("sans-serif") && joined.contains("statusBarColor") &&
                joined.contains("Library") && joined.contains("History") && joined.contains("Downloads") &&
                joined.contains("Extensions");
        notes.add((nativeUi ? "PASS " : "FAIL ") + "Android navigation and native UI gate");

        return notes;
    }

    private static void require(List<String> notes, List<GeneratedProject.FileEntry> files, String pkg, String file, String label) {
        String path = "app/src/main/java/" + pkg.replace('.', '/') + "/" + file;
        notes.add((has(files, path) ? "PASS " : "FAIL ") + label + " (" + file + ")");
    }
    private static boolean has(List<GeneratedProject.FileEntry> files, String path) {
        for (GeneratedProject.FileEntry f : files) if (f != null && path.equals(f.path)) return true;
        return false;
    }
    private static boolean hasSuffix(List<GeneratedProject.FileEntry> files, String suffix) {
        for (GeneratedProject.FileEntry f : files) if (f != null && f.path != null && f.path.endsWith(suffix)) return true;
        return false;
    }
    private static String joinExecutableSource(List<GeneratedProject.FileEntry> files) {
        StringBuilder b = new StringBuilder();
        for (GeneratedProject.FileEntry f : files) {
            if (f != null && f.path != null && f.path.startsWith("app/src/main/java/") && f.path.endsWith(".java") && f.content != null)
                b.append('\n').append(f.content);
        }
        return b.toString();
    }
    private static void failIf(List<String> notes, String joined, String token, String message) {
        notes.add((joined.contains(token) ? "FAIL " : "PASS ") + message);
    }
}
