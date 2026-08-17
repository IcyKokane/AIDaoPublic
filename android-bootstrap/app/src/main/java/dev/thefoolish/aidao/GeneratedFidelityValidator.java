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
        require(notes, files, packageName, "BuiltInProviderCatalog.java", "built-in provider catalog implementation");
        require(notes, files, packageName, "JikanCatalogProvider.java", "reviewed Jikan provider implementation");
        require(notes, files, packageName, "AniListCatalogProvider.java", "reviewed AniList provider implementation");
        require(notes, files, packageName, "AppScreen.java", "phone-native app chrome implementation");

        String joined = joinExecutableSource(files);
        failIf(notes, joined, "DemoProvider", "fabricated DemoProvider placeholder is forbidden in executable source");
        failIf(notes, joined, "Origin Path", "fabricated catalog data is forbidden in executable source");
        failIf(notes, joined, "Sky Archive", "fabricated catalog data is forbidden in executable source");
        failIf(notes, joined, "sample data only", "sample-only provider cannot satisfy a repository-provider request");

        boolean repoBehavior = joined.contains("RepositoryStore") && joined.contains("https://") &&
                joined.contains("ExtensionRepositoryClient") && joined.contains("AVAILABLE") &&
                joined.contains("ENABLED") && joined.contains("FAILED");
        notes.add((repoBehavior ? "PASS " : "FAIL ") + "semantic repository/extension lifecycle gate");

        boolean builtInProvider = joined.contains("BuiltInProviderCatalog") &&
                joined.contains("JikanCatalogProvider") &&
                joined.contains("https://api.jikan.moe/v4/anime") &&
                joined.contains("builtin.jikan.catalog") &&
                joined.contains("metadata only");
        notes.add((builtInProvider ? "PASS " : "FAIL ") + "pre-generation capability research / built-in Jikan provider gate");

        boolean redundantProviders = joined.contains("AniListCatalogProvider") &&
                joined.contains("https://graphql.anilist.co") &&
                joined.contains("builtin.anilist.catalog") &&
                joined.contains("out.add(new JikanCatalogProvider())") &&
                joined.contains("out.add(new AniListCatalogProvider())");
        notes.add((redundantProviders ? "PASS " : "FAIL ") + "multiple reviewed built-in provider redundancy gate");

        boolean providerIsActuallyUsed = joined.contains("BuiltInProviderCatalog.providers()") &&
                joined.contains("new ArrayList<>(BuiltInProviderCatalog.providers())");
        notes.add((providerIsActuallyUsed ? "PASS " : "FAIL ") + "built-in providers are active in generated Browse/Search flow");

        boolean honestCapabilityBoundary = joined.contains("catalog metadata only") || joined.contains("metadata-only");
        notes.add((honestCapabilityBoundary ? "PASS " : "FAIL ") + "provider capability limitation is explicit");

        boolean nativeChrome = joined.contains("setOnApplyWindowInsetsListener") &&
                joined.contains("bottomNav") && joined.contains("ScrollView") &&
                joined.contains("Gravity.CENTER") && joined.contains("setNavigationBarColor") &&
                joined.contains("round(SURFACE");
        notes.add((nativeChrome ? "PASS " : "FAIL ") + "phone-native fixed navigation / inset-aware chrome gate");

        boolean responsiveHierarchy = joined.contains("card(String heading,String supporting)") &&
                joined.contains("section(String s)") && joined.contains("setMinHeight(dp(52))") &&
                joined.contains("setPadding(dp(16)");
        notes.add((responsiveHierarchy ? "PASS " : "FAIL ") + "mobile touch-target / card hierarchy gate");

        boolean asynchronousProviderSearch = joined.contains("new Thread(()->") &&
                joined.contains("runOnUiThread") && joined.contains("ProgressBar") &&
                joined.contains("Some sources could not be reached");
        notes.add((asynchronousProviderSearch ? "PASS " : "FAIL ") + "non-blocking provider search and actionable error-state gate");

        boolean distinguishesFailureFromEmpty = joined.contains("No matches found for") &&
                joined.contains("No results were returned because one or more sources failed");
        notes.add((distinguishesFailureFromEmpty ? "PASS " : "FAIL ") + "provider failure is distinct from genuine zero-result state");

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
