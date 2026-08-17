package dev.thefoolish.aidao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/** CI harness for the generated-app fidelity milestone. */
public final class GenerateFidelitySampleProject {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected output directory");
        Path output = Paths.get(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(output);

        List<String> requirements = Arrays.asList(
                "Search anime through repository-backed providers rather than fabricated sample entries.",
                "Allow users to add and remove HTTPS extension repository URLs and sync extension metadata.",
                "Show extension states including available, installed, enabled, disabled, and failed.",
                "Provide Browse, Detail, Library, History, Downloads, Extensions, Repositories, and Player surfaces.",
                "Persist favorites, history, downloads metadata, repository configuration, extension state, and resume progress locally.",
                "Use Android-native navigation and typography, and clearly identify unsupported provider capabilities instead of faking them."
        );
        List<String> tasks = Arrays.asList(
                "Infer a concise product identity independently of the raw request.",
                "Generate Android-native media navigation and persistence.",
                "Generate repository and extension lifecycle components.",
                "Generate provider failure isolation and honest incomplete states.",
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
                root + "ExtensionManager.java", root + "RepositoryMediaProvider.java"
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
        for (String forbidden : new String[]{"DemoProvider", "Origin Path", "Sky Archive", "sample data only"})
            if (source.contains(forbidden)) throw new IllegalStateException("Placeholder/fabricated executable content survived fidelity pass: " + forbidden);
        for (String marker : new String[]{"ExtensionRepositoryClient", "RepositoryStore", "RepositoriesActivity", "ExtensionRecord.State.ENABLED", "No enabled providers"})
            if (!source.contains(marker)) throw new IllegalStateException("Missing semantic fidelity marker: " + marker);

        System.out.println("Generated fidelity acceptance project: " + project.projectName + " / " + project.packageName + " / " + project.files.size() + " files");
    }
}
