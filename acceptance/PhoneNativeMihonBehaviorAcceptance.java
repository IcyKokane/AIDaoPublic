package dev.thefoolish.aidao;

import java.util.Arrays;
import java.util.List;

/**
 * Hard regression for the latest real-phone feedback.
 * A generated Mihon-reference media app must be behaviorally useful and phone-native,
 * not merely a vertical form with the right class names.
 */
public final class PhoneNativeMihonBehaviorAcceptance {
    public static void main(String[] args) {
        String brief = "Create an Android anime library app like Mihon. Use Mihon-style information architecture with Library, Updates, History, Browse, and More. It must feel native on a phone, ship with useful safe built-in catalog providers, keep repository management for additions, and show actionable provider/network errors.";
        List<String> requirements = Arrays.asList(
                "Mihon-style Library Updates History Browse More navigation",
                "phone-native Android layout with top app bar bottom navigation cards responsive spacing scrolling and system insets",
                "built-in authorized compatible providers available immediately",
                "repository management remains available for additional compatible providers",
                "provider failures are distinct from genuine no-match searches"
        );
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Create an app like Mihon",
                brief,
                requirements,
                Arrays.asList("Generate Android source", "Validate native UI", "Validate built-in provider discovery")
        );

        String root = "app/src/main/java/" + project.packageName.replace('.', '/') + "/";
        String shell = require(project, root + "AppScreen.java");
        String browse = require(project, root + "MainActivity.java");
        String providers = require(project, root + "ProvidersActivity.java");
        String more = require(project, root + "MoreActivity.java");
        String catalog = require(project, root + "BuiltInProviderCatalog.java");
        String jikan = require(project, root + "JikanCatalogProvider.java");
        String anilist = require(project, root + "AniListCatalogProvider.java");

        must(shell, "Library", "primary Library destination");
        must(shell, "Updates", "primary Updates destination");
        must(shell, "History", "primary History destination");
        must(shell, "Browse", "primary Browse destination");
        must(shell, "More", "primary More destination");
        mustNot(shell, "Downloads\",\"Extensions", "desktop-like primary navigation that exposes implementation surfaces");

        must(shell, "ScrollView", "scrollable phone body");
        must(shell, "bottomNav", "persistent bottom navigation outside content scroll");
        must(shell, "setOnApplyWindowInsetsListener", "system-bar inset handling");
        must(shell, "setMinHeight(dp(52))", "phone-sized navigation touch targets");
        must(shell, "card(String heading,String supporting)", "reusable card hierarchy");
        must(shell, "topTitle", "screen-specific top app bar title");

        // A phone-native browse surface must not be only EditText -> Button -> vertical result list.
        boolean richerBrowse = browse.contains("HorizontalScrollView") || browse.contains("GridLayout") || browse.contains("sourceChips") || browse.contains("sourceRail");
        if (!richerBrowse) fail("Browse screen is still a desktop/web-like vertical form; require a horizontal source rail/chips or responsive grid/list composition");
        must(browse, "BuiltInProviderCatalog.providers()", "built-in providers used without repository setup");
        must(browse, "Some sources could not be reached", "actionable provider/network failure surface");
        must(browse, "No matches found", "true zero-result state kept distinct from failures");
        must(browse, "new Thread", "network/provider work off the Android UI thread");

        must(providers, "Built in", "built-in provider section");
        must(providers, "Repository extensions", "optional repository-added provider section");
        must(more, "Extension repositories", "repository management remains reachable under More");

        must(catalog, "JikanCatalogProvider", "reviewed Jikan catalog is bundled");
        must(catalog, "AniListCatalogProvider", "reviewed AniList catalog is bundled");
        must(jikan, "https://api.jikan.moe/v4/anime", "Jikan live catalog endpoint");
        must(jikan, "HttpURLConnection", "Jikan on-device network implementation");
        must(anilist, "https://graphql.anilist.co", "AniList live catalog endpoint");
        must(anilist, "HttpURLConnection", "AniList on-device network implementation");

        boolean hasProviderPass = false;
        boolean hasNativePass = false;
        boolean hasMihonPass = false;
        for (String note : project.verificationNotes) {
            if (note == null) continue;
            if (note.contains("reviewed provider")) hasProviderPass = true;
            if (note.contains("native-fidelity") || note.contains("native Android fidelity") || note.contains("fixed bottom navigation")) hasNativePass = true;
            if (note.contains("Mihon reference intent")) hasMihonPass = true;
            if (note.startsWith("FAIL ")) fail("Generated verification still contains failure: " + note);
        }
        if (!hasProviderPass) fail("Provider capability research did not prove reviewed built-in provider selection");
        if (!hasNativePass) fail("Native Android fidelity did not contribute a positive verification signal");
        if (!hasMihonPass) fail("Mihon reference intent was not recognized as a behavior profile");

        System.out.println("Phone-native Mihon behavior acceptance passed for " + project.projectName + " / " + project.packageName);
    }

    private static String require(GeneratedProject p, String path) {
        GeneratedProject.FileEntry f = p.find(path);
        if (f == null) fail("Missing generated file: " + path);
        return f.content == null ? "" : f.content;
    }

    private static void must(String source, String token, String behavior) {
        if (!source.contains(token)) fail("Missing " + behavior + " marker: " + token);
    }

    private static void mustNot(String source, String token, String behavior) {
        if (source.contains(token)) fail("Found forbidden " + behavior + " marker: " + token);
    }

    private static void fail(String message) {
        throw new IllegalStateException(message);
    }

    private PhoneNativeMihonBehaviorAcceptance() {}
}
