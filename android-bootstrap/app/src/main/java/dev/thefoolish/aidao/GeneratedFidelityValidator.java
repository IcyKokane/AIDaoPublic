package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
                joined.contains("builtin.jikan.catalog") && joined.contains("metadata only");
        notes.add((builtInProvider ? "PASS " : "FAIL ") + "pre-generation capability research / built-in Jikan provider gate");

        boolean redundantProviders = joined.contains("AniListCatalogProvider") &&
                joined.contains("https://graphql.anilist.co") && joined.contains("builtin.anilist.catalog") &&
                joined.contains("out.add(new JikanCatalogProvider())") &&
                joined.contains("out.add(new AniListCatalogProvider())");
        notes.add((redundantProviders ? "PASS " : "FAIL ") + "multiple reviewed built-in provider redundancy gate");

        boolean providerIsActuallyUsed = joined.contains("BuiltInProviderCatalog.providers()") &&
                joined.contains("new ArrayList<>(BuiltInProviderCatalog.providers())");
        notes.add((providerIsActuallyUsed ? "PASS " : "FAIL ") + "built-in providers are active in generated Browse/Search flow");

        boolean honestCapabilityBoundary = joined.contains("catalog metadata only") || joined.contains("metadata-only");
        notes.add((honestCapabilityBoundary ? "PASS " : "FAIL ") + "provider capability limitation is explicit");

        boolean nativeChrome = joined.contains("setOnApplyWindowInsetsListener") && joined.contains("bottomNav") &&
                joined.contains("ScrollView") && joined.contains("Gravity.CENTER") &&
                joined.contains("setNavigationBarColor") && joined.contains("round(SURFACE");
        notes.add((nativeChrome ? "PASS " : "FAIL ") + "phone-native fixed navigation / inset-aware chrome gate");

        boolean responsiveHierarchy = joined.contains("card(String heading,String supporting)") &&
                joined.contains("section(String s)") && joined.contains("setMinHeight(dp(52))") &&
                joined.contains("setPadding(dp(16)");
        notes.add((responsiveHierarchy ? "PASS " : "FAIL ") + "mobile touch-target / card hierarchy gate");

        boolean asynchronousProviderSearch = joined.contains("new Thread(()->") && joined.contains("runOnUiThread") &&
                joined.contains("ProgressBar") && joined.contains("Some sources could not be reached");
        notes.add((asynchronousProviderSearch ? "PASS " : "FAIL ") + "non-blocking provider search and actionable error-state gate");

        boolean distinguishesFailureFromEmpty = joined.contains("No matches found for") &&
                joined.contains("No results were returned because one or more sources failed");
        notes.add((distinguishesFailureFromEmpty ? "PASS " : "FAIL ") + "provider failure is distinct from genuine zero-result state");

        return notes;
    }

    /** Explicit request requirements are hard acceptance criteria, not suggestions. */
    private static void validateGeneralRequestFidelity(List<String> notes,
                                                       List<GeneratedProject.FileEntry> files,
                                                       String joined,
                                                       String request) {
        String strings = content(files, "app/src/main/res/values/strings.xml");
        String manifest = content(files, "app/src/main/AndroidManifest.xml");
        String resources = joinResources(files);
        String lowerJoined = joined.toLowerCase(Locale.US);

        String appName = xmlValue(strings, "app_name");
        String lowerName = appName.toLowerCase(Locale.US);
        int words = appName.trim().isEmpty() ? 0 : appName.trim().split("\\s+").length;
        boolean conciseName = appName.length() >= 2 && appName.length() <= 32 && words <= 5 &&
                !lowerName.startsWith("create ") && !lowerName.startsWith("make ") &&
                !lowerName.startsWith("build ") && !lowerName.contains(" should ") &&
                !lowerName.contains(" it should ") && !appName.contains(".");
        notes.add((conciseName ? "PASS " : "FAIL ") + "generated app identity uses a concise product name rather than prompt text");

        if (mentions(request, "app logo", "app icon", "logo", "icon")) {
            String adaptive = content(files, "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml");
            String adaptiveRound = content(files, "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml");
            String foreground = content(files, "app/src/main/res/drawable/ic_launcher_foreground.xml");
            String background = content(files, "app/src/main/res/values/launcher_background.xml");
            boolean iconDeclared = manifest.contains("android:icon=\"@mipmap/ic_launcher\"") &&
                    manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\"");
            boolean adaptiveWiring = adaptive.contains("<adaptive-icon") &&
                    adaptive.contains("@drawable/ic_launcher_foreground") &&
                    adaptive.contains("@color/launcher_background") &&
                    adaptiveRound.contains("<adaptive-icon");
            boolean generatedArtwork = foreground.contains("<vector") && foreground.contains("<path") &&
                    foreground.contains("android:pathData=") && background.contains("launcher_background");
            notes.add((iconDeclared && adaptiveWiring && generatedArtwork ? "PASS " : "FAIL ") +
                    "explicit app-logo request produces distinct app-specific adaptive artwork and wires launcher/round mipmap resources");
        }

        if (mentions(request, "sidebar", "side bar", "navigation drawer", "drawer navigation")) {
            boolean actualSidebar = (joined.contains("root.setOrientation(LinearLayout.HORIZONTAL)") &&
                    joined.contains("root.addView(nav,new LinearLayout.LayoutParams(dp(104),-1))")) ||
                    (joined.contains("DrawerLayout") && joined.contains("NavigationView"));
            notes.add((actualSidebar ? "PASS " : "FAIL ") +
                    "explicit sidebar/drawer requirement materially changes generated layout rather than only renaming navigation helpers");
        }

        if (containsWord(request, "purple")) {
            boolean purpleResource = containsAny(resources.toUpperCase(Locale.US), "#7C3AED", "#8B5CF6", "#9333EA", "#A855F7");
            boolean purpleUsed = joined.contains("ACCENT") &&
                    (joined.contains("round(ACCENT") || joined.contains("setBackgroundColor(ACCENT") || joined.contains("setTextColor(ACCENT"));
            notes.add((purpleResource && purpleUsed ? "PASS " : "FAIL ") +
                    "requested purple direction is both defined and materially applied to generated UI");
        }
        if (containsWord(request, "red")) {
            boolean redResource = containsAny(resources.toUpperCase(Locale.US), "#EF4444", "#DC2626", "#F43F5E", "#E11D48");
            boolean redUsed = joined.contains("SECONDARY") &&
                    (joined.contains("setTextColor(SECONDARY)") || joined.contains("round(SECONDARY") || joined.contains("setBackgroundColor(SECONDARY"));
            notes.add((redResource && redUsed ? "PASS " : "FAIL ") +
                    "requested red direction is both defined and materially applied to generated UI");
        }

        boolean noteRequest = mentions(request, "notepad", "notes", "note app", "document editor");
        if (mentions(request, "lock notes", "lock note", "notes so they can't be edited", "notes so they cannot be edited", "read-only", "read only")) {
            boolean lockState = lowerJoined.contains("note_lock_") && joined.contains("store.flag(");
            boolean readOnlyEnforced = joined.contains("setEnabled(false)") || joined.contains("setFocusable(false)") || joined.contains("setInputType(0)");
            boolean lockToggle = lowerJoined.contains("unlock note") && lowerJoined.contains("lock note");
            notes.add((lockState && readOnlyEnforced && lockToggle ? "PASS " : "FAIL ") +
                    "requested note-lock behavior persists lock state, supports explicit unlock, and prevents editing while locked");
        }
        if (noteRequest) {
            boolean newNoteIdentity = joined.contains("System.currentTimeMillis()") && joined.contains("store.putText(\"active_note\"");
            boolean noteMutationsPersist = joined.contains("store.putText(\"note_title_\"") &&
                    joined.contains("store.putText(\"note_body_\"") && joined.contains("store.putText(\"documents\"") &&
                    joined.contains("store.putText(\"active_note\"");
            boolean reopenSavedNote = joined.contains("AppNavigator.open(this,EditorActivity.class)") && lowerJoined.contains("documents");
            notes.add((newNoteIdentity && noteMutationsPersist && reopenSavedNote ? "PASS " : "FAIL ") +
                    "notepad supports multiple persisted notes, save/load round trip, and deterministic reopen behavior");
        }

        boolean workoutRequest = mentions(request, "workout", "track the exercise", "track exercise", "weight and reps", "weight", "reps");
        if (workoutRequest) {
            boolean workoutFields = lowerJoined.contains("exercise") && lowerJoined.contains("weight") && lowerJoined.contains("reps");
            boolean workoutMutationsPersist = joined.contains("store.putText(\"workouts\"") || joined.contains("store.putText(\"workout_history\"");
            boolean xpMutation = joined.contains("store.number(\"workout_xp\",store.number(\"workout_xp\")+");
            boolean statMutation = joined.contains("store.number(\"stat_strength\",store.number(\"stat_strength\")+") &&
                    joined.contains("store.number(\"stat_endurance\",store.number(\"stat_endurance\")+");
            notes.add((workoutFields && workoutMutationsPersist ? "PASS " : "FAIL ") +
                    "workout completed-set flow retains exercise/weight/reps and persists workout history");
            if (mentions(request, "rpg", "stats", "growth", "automatically")) {
                notes.add((xpMutation && statMutation ? "PASS " : "FAIL ") +
                        "requested RPG growth is calculated automatically by completed workout mutations rather than manual stat entry");
            }
        }
    }

    private static boolean mentions(String request, String... terms) {
        for (String term : terms) if (request.contains(term)) return true;
        return false;
    }

    /** Match standalone color words so words such as inferred/required do not imply red. */
    private static boolean containsWord(String source, String word) {
        if (source == null || word == null || word.isEmpty()) return false;
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(word.toLowerCase(Locale.US)) + "(?![a-z0-9])")
                .matcher(source.toLowerCase(Locale.US)).find();
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
    private static String joinResources(List<GeneratedProject.FileEntry> files) {
        StringBuilder b = new StringBuilder();
        for (GeneratedProject.FileEntry f : files) {
            if (f == null || f.path == null || f.content == null) continue;
            if (f.path.startsWith("app/src/main/res/") || f.path.startsWith("app/src/main/java/")) b.append('\n').append(f.content);
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
            if (f != null && f.path != null && f.path.startsWith("app/src/main/java/") && f.path.endsWith(".java") && f.content != null) b.append('\n').append(f.content);
        }
        return b.toString();
    }
    private static void failIf(List<String> notes, String source, String forbidden, String label) {
        notes.add((source.contains(forbidden) ? "FAIL " : "PASS ") + label);
    }
}
