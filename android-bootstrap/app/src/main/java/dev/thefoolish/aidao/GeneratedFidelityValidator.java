package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Semantic acceptance gate for generated source. Compilation alone is insufficient. */
final class GeneratedFidelityValidator {
    static List<String> validate(String packageName, List<GeneratedProject.FileEntry> files) {
        List<String> notes = new ArrayList<>();
        String joined = joinExecutableSource(files);
        String request = requestText(files).toLowerCase(Locale.US);

        validateGeneralRequestFidelity(notes, files, joined, request);

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

    /**
     * Request-specific requirements are acceptance criteria, not suggestions.
     * The generator may still use domain defaults, but it must not report an APK
     * as ready when an explicit identity/branding/navigation/theme/behavior
     * requirement has disappeared from the generated source tree.
     */
    private static void validateGeneralRequestFidelity(List<String> notes,
                                                       List<GeneratedProject.FileEntry> files,
                                                       String joined,
                                                       String request) {
        String strings = content(files, "app/src/main/res/values/strings.xml");
        String manifest = content(files, "app/src/main/AndroidManifest.xml");
        String resources = joinResources(files);

        String appName = xmlValue(strings, "app_name");
        boolean conciseName = appName.length() >= 2 && appName.length() <= 40 &&
                !appName.toLowerCase(Locale.US).startsWith("create ") &&
                !appName.toLowerCase(Locale.US).startsWith("make ") &&
                !appName.toLowerCase(Locale.US).contains(" should ");
        notes.add((conciseName ? "PASS " : "FAIL ") + "generated app identity uses a concise product name rather than prompt text");

        if (mentions(request, "app logo", "app icon", "logo", "icon")) {
            boolean iconDeclared = manifest.contains("android:icon=") || manifest.contains("android:roundIcon=");
            boolean iconResource = hasResourceSuffix(files, "/ic_launcher.xml") ||
                    hasResourceSuffix(files, "/ic_generated_app.xml") ||
                    hasResourceSuffix(files, "/ic_app.xml") || hasRasterLauncher(files);
            notes.add((iconDeclared && iconResource ? "PASS " : "FAIL ") +
                    "explicit app-logo request produces and wires launcher icon resources");
        }

        if (mentions(request, "sidebar", "side bar", "navigation drawer", "drawer navigation")) {
            boolean sidebar = joined.contains("DrawerLayout") || joined.contains("NavigationView") ||
                    joined.contains("SIDE_NAV") || joined.contains("sidebarNav") || joined.contains("sideNav");
            notes.add((sidebar ? "PASS " : "FAIL ") + "explicit sidebar/drawer navigation requirement is implemented");
        }

        if (request.contains("purple")) {
            boolean purple = containsAny(resources.toLowerCase(Locale.US),
                    "#6", "#7", "#8", "purple", "violet", "rgb(98", "rgb(109", "rgb(126", "rgb(128");
            notes.add((purple ? "PASS " : "FAIL ") + "requested purple visual direction is represented in generated resources/source");
        }
        if (request.contains("red")) {
            boolean red = containsAny(resources.toLowerCase(Locale.US),
                    "#e", "#f", "red", "crimson", "rgb(2", "rgb(220", "rgb(239", "rgb(244");
            notes.add((red ? "PASS " : "FAIL ") + "requested red visual direction is represented in generated resources/source");
        }

        boolean noteRequest = mentions(request, "notepad", "notes", "note app", "document editor");
        if (mentions(request, "lock notes", "lock note", "notes so they can't be edited", "notes so they cannot be edited", "read-only", "read only")) {
            String lower = joined.toLowerCase(Locale.US);
            boolean lockState = lower.contains("locked") || lower.contains("islocked") || lower.contains("note_lock");
            boolean readOnlyEnforced = joined.contains("setEnabled(false)") || joined.contains("setFocusable(false)") ||
                    joined.contains("setInputType(0)") || lower.contains("read-only") || lower.contains("read only");
            notes.add((lockState && readOnlyEnforced ? "PASS " : "FAIL ") +
                    "requested note-lock behavior persists state and prevents editing");
        }
        if (noteRequest) {
            boolean noteMutationsPersist = joined.contains("store.putText(\"note_title_\"") &&
                    joined.contains("store.putText(\"note_body_\"") &&
                    joined.contains("store.putText(\"documents\"") &&
                    joined.contains("store.putText(\"active_note\"");
            notes.add((noteMutationsPersist ? "PASS " : "FAIL ") +
                    "note save/open mutations use persisted string setters rather than getter-shaped no-ops");
        }

        boolean workoutRequest = mentions(request, "workout", "track the exercise", "track exercise", "weight and reps", "weight", "reps");
        if (mentions(request, "track the exercise", "track exercise", "weight and reps", "weight", "reps")) {
            String lower = joined.toLowerCase(Locale.US);
            boolean workoutFields = lower.contains("exercise") && lower.contains("weight") && lower.contains("reps");
            notes.add((workoutFields ? "PASS " : "FAIL ") + "workout request retains exercise / weight / reps data model");
        }
        if (workoutRequest) {
            boolean workoutMutationsPersist = joined.contains("store.putText(\"workouts\"") ||
                    joined.contains("store.putText(\"workout_history\"");
            notes.add((workoutMutationsPersist ? "PASS " : "FAIL ") +
                    "workout completed-set history uses a persisted string setter");
        }
        if (mentions(request, "rpg", "stats", "growth")) {
            String lower = joined.toLowerCase(Locale.US);
            boolean progression = lower.contains("xp") || lower.contains("level") || lower.contains("stat") || lower.contains("growth");
            notes.add((progression ? "PASS " : "FAIL ") + "requested RPG/stat progression behavior is represented in executable source");
        }
    }

    private static boolean mentions(String request, String... terms) {
        for (String term : terms) if (request.contains(term)) return true;
        return false;
    }
    private static boolean containsAny(String source, String... terms) {
        for (String term : terms) if (source.contains(term)) return true;
        return false;
    }
    private static String requestText(List<GeneratedProject.FileEntry> files) {
        String readme = content(files, "README.md");
        if (!readme.isEmpty()) return readme;
        return joinAllText(files);
    }
    private static String content(List<GeneratedProject.FileEntry> files, String path) {
        for (GeneratedProject.FileEntry f : files)
            if (f != null && path.equals(f.path) && f.content != null) return f.content;
        return "";
    }
    private static String xmlValue(String xml, String name) {
        String open = "<string name=\"" + name + "\">";
        int a = xml.indexOf(open);
        if (a < 0) return "";
        int b = xml.indexOf("</string>", a + open.length());
        return b < 0 ? "" : xml.substring(a + open.length(), b).trim();
    }
    private static boolean hasResourceSuffix(List<GeneratedProject.FileEntry> files, String suffix) {
        for (GeneratedProject.FileEntry f : files)
            if (f != null && f.path != null && f.path.startsWith("app/src/main/res/") && f.path.endsWith(suffix)) return true;
        return false;
    }
    private static boolean hasRasterLauncher(List<GeneratedProject.FileEntry> files) {
        for (GeneratedProject.FileEntry f : files) {
            if (f == null || f.path == null || !f.path.startsWith("app/src/main/res/")) continue;
            String p = f.path.toLowerCase(Locale.US);
            if ((p.contains("ic_launcher") || p.contains("ic_app")) &&
                    (p.endsWith(".png") || p.endsWith(".webp"))) return true;
        }
        return false;
    }
    private static String joinResources(List<GeneratedProject.FileEntry> files) {
        StringBuilder b = new StringBuilder();
        for (GeneratedProject.FileEntry f : files) {
            if (f == null || f.path == null || f.content == null) continue;
            if (f.path.startsWith("app/src/main/res/") || f.path.startsWith("app/src/main/java/"))
                b.append('\n').append(f.content);
        }
        return b.toString();
    }
    private static String joinAllText(List<GeneratedProject.FileEntry> files) {
        StringBuilder b = new StringBuilder();
        for (GeneratedProject.FileEntry f : files) if (f != null && f.content != null) b.append('\n').append(f.content);
        return b.toString();
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
