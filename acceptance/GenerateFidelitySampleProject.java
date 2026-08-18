package dev.thefoolish.aidao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/** CI harness for the generated-app fidelity and provider-integration milestone. */
public final class GenerateFidelitySampleProject {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected output directory");
        Path output = Paths.get(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(output);

        List<String> requirements = Arrays.asList(
                "Search anime through repository-backed providers rather than fabricated sample entries.",
                "Bundle at least one reviewed real provider when a safe compatible public source is available.",
                "Allow users to add and remove HTTPS extension repository URLs and sync extension metadata.",
                "Show extension states including available, installed, enabled, disabled, and failed.",
                "Provide Browse, Detail, Library, History, Downloads, Extensions, Repositories, and Player surfaces.",
                "Persist favorites, history, downloads metadata, repository configuration, extension state, and resume progress locally.",
                "Use Android-native navigation and typography, expose accessible interaction labels, and clearly identify unsupported provider capabilities instead of faking them.",
                "A favorited title must render in Library after navigation and process restart, and removal must update Library.",
                "Repository Sync/Remove and extension Enable/Disable controls must remain visibly labeled at phone density.",
                "Episode playback must use an actual provider-declared HTTPS media resolver when supported and clearly disable playback for metadata-only providers."
        );
        List<String> tasks = Arrays.asList(
                "Infer a concise product identity independently of the raw request.",
                "Research reviewed provider capabilities before generation.",
                "Integrate safe built-in providers into the generated app.",
                "Generate Android-native media navigation and persistence.",
                "Generate repository and extension lifecycle components.",
                "Generate provider failure isolation and honest incomplete states.",
                "Generate favorites round-trip rendering and provider-aware playback handoff.",
                "Run semantic fidelity validation and Android CI."
        );

        GeneratedProject project = new LocalSourceGenerator().generate(
                "Make An Anime App Like Mihon, It Should Have Repository Based Providers",
                "Make an anime app like Mihon. It should have repository based providers, libraries based on anime websites, favorites, offline downloads, tags for genres, history, playback and resume progress. Users should be able to add extension repositories.",
                requirements,
                tasks
        );

        for (String note : project.verificationNotes) {
            System.out.println(note);
            if (note.startsWith("FAIL ")) throw new IllegalStateException("Fidelity verification failed: " + note);
        }

        if (project.projectName.length() > 28 || project.projectName.toLowerCase().startsWith("make "))
            throw new IllegalStateException("Raw brief leaked into product name: " + project.projectName);
        if (project.packageName.contains("makeananimeapplikemihon"))
            throw new IllegalStateException("Raw brief leaked into package identity: " + project.packageName);

        String root = "app/src/main/java/" + project.packageName.replace('.', '/') + "/";
        String[] required = {
                root + "AppScreen.java", root + "LocalStore.java", root + "AppNavigator.java",
                root + "MainActivity.java", root + "DetailActivity.java", root + "LibraryActivity.java",
                root + "HistoryActivity.java", root + "DownloadsActivity.java", root + "ProvidersActivity.java",
                root + "RepositoriesActivity.java", root + "PlayerActivity.java", root + "MediaProvider.java",
                root + "ExtensionRecord.java", root + "RepositoryStore.java", root + "ExtensionRepositoryClient.java",
                root + "ExtensionManager.java", root + "RepositoryMediaProvider.java",
                root + "BuiltInProviderCatalog.java", root + "JikanCatalogProvider.java", root + "AniListCatalogProvider.java",
                root + "UpdatesActivity.java", root + "MoreActivity.java"
        };
        for (String path : required) if (!project.hasPath(path)) throw new IllegalStateException("Missing fidelity file: " + path);

        StringBuilder executable = new StringBuilder();
        for (GeneratedProject.FileEntry entry : project.files) {
            if (entry.path.startsWith("app/src/main/java/") && entry.path.endsWith(".java")) executable.append('\n').append(entry.content);
            Path target = output.resolve(entry.path).normalize();
            if (!target.startsWith(output)) throw new SecurityException("Generated path escaped output root: " + entry.path);
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            Files.write(target, entry.content.getBytes(StandardCharsets.UTF_8));
        }
        String source = executable.toString();
        for (String forbidden : new String[]{
                "DemoProvider", "Origin Path", "Sky Archive", "sample data only",
                "android.widget.android.widget.", "android.graphics.android.graphics.",
                "android.content.android.content.", "android.app.android.app.",
                "Save +60s test progress", "Playback surface placeholder"
        }) if (source.contains(forbidden)) throw new IllegalStateException("Forbidden/corrupted generated executable content survived fidelity pass: " + forbidden);
        for (String marker : new String[]{
                "ExtensionRepositoryClient", "RepositoryStore", "RepositoriesActivity", "ExtensionRecord.State.ENABLED",
                "BuiltInProviderCatalog", "JikanCatalogProvider", "AniListCatalogProvider",
                "https://api.jikan.moe/v4/anime", "https://graphql.anilist.co",
                "new ArrayList<>(BuiltInProviderCatalog.providers())",
                "setOnApplyWindowInsetsListener", "Some sources could not be reached",
                "setContentDescription(label)", "setContentDescription(\"Open \"+names[i])",
                "query.setContentDescription(\"Search anime\")", "setContentDescription(\"Open \"+a.title)",
                "LibraryActivity.class,UpdatesActivity.class,HistoryActivity.class,MainActivity.class,MoreActivity.class",
                "store.set(\"favorites\"", "Open favorite ", "Remove from Library", "Add to Library",
                "Sync repository", "Remove repository", "Enable extension", "Disable extension", "dp(52)",
                "playbackUrl", "supportsPlayback()", "resolveMediaUrl",
                "new VideoView(this)", "setVideoURI", "getCurrentPosition()", "provider returned a non-HTTPS media URL"
        }) if (!source.contains(marker)) throw new IllegalStateException("Missing semantic fidelity marker: " + marker);

        System.out.println("Generated fidelity acceptance project: " + project.projectName + " / " + project.packageName + " / " + project.files.size() + " files");
    }
}
