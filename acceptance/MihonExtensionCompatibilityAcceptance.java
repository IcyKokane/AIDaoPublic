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
                        "Use phone-native top app bar, fixed bottom navigation, cards, responsive grids, touch targets, scrolling, system insets and IME handling.",
                        "Do not present incompatible repository metadata as a working executable extension.",
                        "Only enable repository providers when AIDao has a declared compatible search contract."
                ),
                Arrays.asList("Generate media app", "Integrate repositories", "Validate extension capability safety")
        );
        for (String note : project.verificationNotes) if (note.startsWith("FAIL ")) throw new IllegalStateException(note);

        String providers = content(project, "/ProvidersActivity.java");
        String extension = content(project, "/ExtensionRecord.java");
        String repositoryProvider = content(project, "/RepositoryMediaProvider.java");
        String main = content(project, "/MainActivity.java");
        String appScreen = content(project, "/AppScreen.java");
        String builtIns = content(project, "/BuiltInProviderCatalog.java");
        String library = content(project, "/LibraryActivity.java");
        String updates = content(project, "/UpdatesActivity.java");
        String history = content(project, "/HistoryActivity.java");
        String more = content(project, "/MoreActivity.java");
        String allExecutable = executableSource(project);

        require(extension, "public boolean searchable(){return searchUrl.length()>0;}", "declared search-contract compatibility predicate");
        require(repositoryProvider, "if(!ext.searchable())throw new IllegalStateException", "repository search refuses incompatible metadata");

        // Final phone UX renders provider controls only inside compatible branches.
        if (!providers.contains("boolean ok=enabled&&x.playable()") && !providers.contains("boolean compatible=x.searchable()"))
            throw new IllegalStateException("Provider screen is missing a repository compatibility predicate");
        if (!providers.contains("Select provider") && !providers.contains("Enable source") && !providers.contains("Enable extension"))
            throw new IllegalStateException("Provider screen is missing a reachable compatible-provider action");
        if (!providers.contains("Unsupported/incompatible") && !providers.contains("Search unavailable") && !providers.contains("Search: unavailable") && !providers.contains("Unsupported repository metadata"))
            throw new IllegalStateException("Provider screen does not visibly explain incompatible repository metadata");
        if (providers.contains("toggle.setEnabled(true)") || providers.contains("choose.setEnabled(true)"))
            throw new IllegalStateException("Provider screen contains an unconditional repository enable/select path");

        // Phone-native behavior cannot pass by merely having the right activity names.
        require(appScreen, "Library\",\"Updates\",\"History\",\"Browse\",\"More", "Mihon-style primary information architecture");
        require(appScreen, "boolean selected=getClass()==target", "selected bottom-navigation state");
        require(appScreen, "WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()", "system bar and cutout inset handling");
        require(appScreen, "WindowInsets.Type.ime()", "keyboard IME handling");
        require(appScreen, "setMinimumHeight(dp(60))", "phone-sized primary navigation touch target");
        require(appScreen, "card(String heading,String supporting)", "card-based phone information hierarchy");
        require(appScreen, "GridLayout grid()", "responsive grid composition");
        require(appScreen, "chip(String label,boolean active)", "compact phone-native capability/source chips");
        require(appScreen, "emptyState(String heading,String supporting)", "screen-specific empty-state component");

        // Built-in discovery must be useful without repository setup and failures must not masquerade as zero matches.
        require(builtIns, "new JikanCatalogProvider()", "reviewed Jikan built-in discovery provider");
        require(builtIns, "new AniListCatalogProvider()", "reviewed AniList built-in discovery provider");
        require(main, "Check source health", "reachable on-device provider diagnostic");
        require(main, "p.search(\"Naruto\")", "diagnostic requires provider to return real catalog data");
        require(main, "returned no usable catalog data", "empty provider response is reported as a provider diagnostic failure");
        require(main, "Some sources could not be reached", "network/provider failure is visibly distinguished from genuine no-match state");
        require(main, "source errors occurred", "zero-result copy does not hide source errors");
        require(main, "loadDiscoverAsync()", "automatic built-in discovery on Browse");
        require(main, "p.search(\"Frieren\")", "discovery performs real built-in catalog work");
        require(main, "Retry discovery", "actionable discovery retry");
        require(main, "Set<String> seen=new LinkedHashSet<>()", "cross-provider result de-duplication");
        require(main, "GridLayout g=grid()", "responsive discovery/search result grid");

        // Mihon reference intent must create meaningful product behavior, not only matching labels.
        require(library, "store.set(\"favorites\")", "persisted local library data");
        require(library, "GridLayout g=grid()", "responsive library card layout");
        require(updates, "Refresh library updates", "reachable library update refresh action");
        require(updates, "new Thread", "provider update work off the UI thread");
        require(updates, "p.search(title)", "updates query the saved title's provider");
        require(updates, "Update available", "meaningful update result state");
        require(history, "store.text(\"last_episode\"", "persisted playback history state");
        require(history, "Continue watching", "resume-oriented history surface");
        require(more, "Extension repositories", "repository management under More");
        require(more, "card(\"Sources\"", "card-based secondary source management");

        // Repository integration is deliberately declarative-only. It may consume reviewed HTTPS
        // search/playback contracts, but generated code must never load or install arbitrary APK code.
        forbid(allExecutable, "DexClassLoader", "dynamic APK/class loading");
        forbid(allExecutable, "PathClassLoader", "dynamic APK/class loading");
        forbid(allExecutable, "installPackage", "package installation API");
        forbid(allExecutable, "REQUEST_INSTALL_PACKAGES", "unknown-package installation permission");
        forbid(allExecutable, "ACTION_INSTALL_PACKAGE", "package installer intent");

        System.out.println("PASS Mihon repository safety, phone-native UX, behavioral fidelity, and built-in discovery diagnostics");
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
        if (source == null || !source.contains(token)) throw new IllegalStateException("Missing " + label + ": " + token);
    }

    private static void forbid(String source, String token, String label) {
        if (source != null && source.contains(token)) throw new IllegalStateException("Forbidden " + label + " present: " + token);
    }
}
