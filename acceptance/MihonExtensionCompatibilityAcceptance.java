package dev.thefoolish.aidao;

import java.util.Arrays;

/** Guards the real-device failure where incompatible repository metadata looked installable/enabled. */
public final class MihonExtensionCompatibilityAcceptance {
    public static void main(String[] args) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Anime Shelf",
                "Create an anime app like Mihon with repository-based providers, search, favorites, history, playback and resume progress.",
                Arrays.asList(
                        "Use a Mihon-like Library / Updates / History / Browse / More information architecture.",
                        "Allow user-managed HTTPS extension repositories.",
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
        String allExecutable = executableSource(project);

        require(extension, "public boolean searchable(){return searchUrl.length()>0;}", "declared search-contract compatibility predicate");
        require(repositoryProvider, "if(!ext.searchable())throw new IllegalStateException", "repository search refuses incompatible metadata");

        // Final phone UX no longer renders a disabled toggle for incompatible repository entries.
        // Instead, it renders a Select provider action only inside the compatible/playable branch.
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

        // Repository integration is deliberately declarative-only. It may consume reviewed HTTPS
        // search/playback contracts, but generated code must never load or install arbitrary APK code.
        forbid(allExecutable, "DexClassLoader", "dynamic APK/class loading");
        forbid(allExecutable, "PathClassLoader", "dynamic APK/class loading");
        forbid(allExecutable, "installPackage", "package installation API");
        forbid(allExecutable, "REQUEST_INSTALL_PACKAGES", "unknown-package installation permission");
        forbid(allExecutable, "ACTION_INSTALL_PACKAGE", "package installer intent");

        System.out.println("PASS Mihon repository extension compatibility safety");
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
