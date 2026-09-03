package dev.thefoolish.aidao;

import java.util.Arrays;

/** Guards repository safety plus the real-device Mihon phone-native/provider-discovery baseline. */
public final class MihonExtensionCompatibilityAcceptance {
    public static void main(String[] args) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Anime Shelf",
                "Create an anime app like Mihon with repository-based providers, search, favorites, history, playback and resume progress.",
                Arrays.asList(
                        "Use a Mihon-like Library / Updates / History / Browse / More information architecture.",
                        "Allow user-managed HTTPS extension repositories.",
                        "Ship useful compatible authorized built-in discovery providers when available.",
                        "Show actionable provider/network failures instead of misleading no matches.",
                        "Use phone-native top app bar, fixed bottom navigation, cards, touch targets, scrolling, and system insets.",
                        "Do not present incompatible repository metadata as a working executable extension.",
                        "Only enable repository providers when AIDao has a declared compatible search contract."
                ),
                Arrays.asList("Generate media app", "Integrate repositories", "Validate extension capability safety")
        );
        for (String note : project.verificationNotes) {
            if (note.startsWith("FAIL ")) throw new IllegalStateException(note);
        }

        String providers = content(project, "/ProvidersActivity.java");
        String extension = content(project, "/ExtensionRecord.java");
        String repositoryProvider = content(project, "/RepositoryMediaProvider.java");
        String main = content(project, "/MainActivity.java");
        String appScreen = content(project, "/AppScreen.java");
        String builtIns = content(project, "/BuiltInProviderCatalog.java");
        String allExecutable = executableSource(project);

        require(extension, "public boolean searchable(){return searchUrl.length()>0;}", "declared search-contract compatibility predicate");
        require(repositoryProvider, "if(!ext.searchable())throw new IllegalStateException", "repository search refuses incompatible metadata");

        // Final phone UX renders a Select provider action only inside the compatible/playable branch.
        require(providers, "boolean ok=enabled&&x.playable()", "repository playback compatibility predicate");
        require(providers, "if(ok){compatible++;Button choose=button", "provider selection action gated behind compatibility");
        require(providers, "Select provider", "reachable compatible-provider selection action");
        require(providers, "authorized configuration must be verified by the user", "visible authorization/provenance boundary");
        if (!providers.contains("Unsupported/incompatible") && !providers.contains("Search: unavailable"))
            throw new IllegalStateException("Provider screen does not visibly explain incompatible repository metadata");
        if (providers.contains("toggle.setEnabled(true)"))
            throw new IllegalStateException("Provider screen contains an unconditional extension enable path");
        if (providers.contains("choose.setEnabled(true)"))
            throw new IllegalStateException("Provider screen contains an unconditional repository provider selection path");

        // Phone-native behavior cannot pass by merely having the right activity names.
        require(appScreen, "Library\",\"Updates\",\"History\",\"Browse\",\"More", "Mihon-style primary information architecture");
        require(appScreen, "boolean selected=getClass()==target", "selected bottom-navigation state");
        require(appScreen, "WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()", "system bar and cutout inset handling");
        require(appScreen, "setMinHeight(dp(56))", "phone-sized primary navigation touch target");
        require(appScreen, "card(String heading,String supporting)", "card-based phone information hierarchy");

        // Built-in discovery must be demonstrably usable on-device and failures must not masquerade as zero matches.
        require(builtIns, "new JikanCatalogProvider()", "reviewed Jikan built-in discovery provider");
        require(builtIns, "new AniListCatalogProvider()", "reviewed AniList built-in discovery provider");
        require(main, "Verify built-in sources", "reachable on-device provider diagnostic");
        require(main, "p.search(\"Naruto\")", "diagnostic requires provider to return real catalog data");
        require(main, "reachable but returned no usable catalog data", "empty provider response is reported as a provider diagnostic failure");
        require(main, "one or more sources failed", "network/provider failure is distinguished from genuine no-match state");
        require(main, "Set<String> seen=new LinkedHashSet<>()", "cross-provider result de-duplication");

        // Repository integration is deliberately declarative-only. It may consume reviewed HTTPS
        // search/playback contracts, but generated code must never load or install arbitrary APK code.
        forbid(allExecutable, "DexClassLoader", "dynamic APK/class loading");
        forbid(allExecutable, "PathClassLoader", "dynamic APK/class loading");
        forbid(allExecutable, "installPackage", "package installation API");
        forbid(allExecutable, "REQUEST_INSTALL_PACKAGES", "unknown-package installation permission");
        forbid(allExecutable, "ACTION_INSTALL_PACKAGE", "package installer intent");

        System.out.println("PASS Mihon repository safety, phone-native UX, and built-in discovery diagnostics");
    }

    private static String content(GeneratedProject project, String suffix) {
        for (GeneratedProject.FileEntry f : project.files)
            if (f != null && f.path != null && f.path.endsWith(suffix)) return f.content == null ? "" : f.content;
        throw new IllegalStateException("Missing generated file " + suffix);
    }

    private static String executableSource(GeneratedProject project) {
        StringBuilder out = new StringBuilder();
        for (GeneratedProject.FileEntry f : project.files) {
            if (f != null && f.path != null && f.path.startsWith("app/src/main/java/") && f.path.endsWith(".java") && f.content != null)
                out.append('\n').append(f.content);
        }
        return out.toString();
    }

    private static void require(String source, String token, String label) {
        if (source == null || !source.contains(token))
            throw new IllegalStateException("Missing " + label + ": " + token);
    }

    private static void forbid(String source, String token, String label) {
        if (source != null && source.contains(token))
            throw new IllegalStateException("Forbidden " + label + " present: " + token);
    }
}
