package dev.thefoolish.aidao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/** CI-only acceptance harness for the deterministic local source generator. */
public final class GenerateSampleProject {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected output directory");
        Path output = Paths.get(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(output);

        List<String> requirements = Arrays.asList(
                "Provide a searchable anime catalog and separate detail pages with episode navigation.",
                "Persist favorites, watch history, and per-episode resume progress locally.",
                "Keep media providers behind replaceable interfaces with visible health/failure states.",
                "Provide separate Catalog, Detail, Library, History, Providers, and Player screens.",
                "Generate reusable Android UI/data architecture, resources, and manifest navigation."
        );
        List<String> tasks = Arrays.asList(
                "Create the Android application shell and reusable screen architecture.",
                "Define anime, episode, provider, library, and watch-progress models.",
                "Build catalog, detail, library, history, provider, and player flows.",
                "Persist favorites and watch progress locally.",
                "Run Android CI and verify the debug APK."
        );

        GeneratedProject project = new LocalSourceGenerator().generate(
                "AIDao V0.5 Acceptance Anime App",
                "Build a nontrivial Android anime browsing app with search, detail pages, episode buttons, favorites, history, provider isolation and resumable playback state. This proves AIDao-generated multi-screen source compiles into a real APK.",
                requirements,
                tasks
        );

        for (GeneratedProject.FileEntry entry : project.files) {
            Path target = output.resolve(entry.path).normalize();
            if (!target.startsWith(output)) throw new SecurityException("Generated path escaped output root: " + entry.path);
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            Files.write(target, entry.content.getBytes(StandardCharsets.UTF_8));
        }

        boolean verificationFailed = false;
        for (String note : project.verificationNotes) {
            System.out.println(note);
            if (note.startsWith("FAIL ")) verificationFailed = true;
        }
        if (verificationFailed) throw new IllegalStateException("Generator verification failed");
        if (project.files.size() < 16) throw new IllegalStateException("Expected a nontrivial generated source tree, got " + project.files.size());
        String root = "app/src/main/java/" + project.packageName.replace('.', '/') + "/";
        String[] requiredFiles = {
                root + "GeneratedScreen.java", root + "LocalStore.java", root + "AppNavigator.java",
                root + "MainActivity.java", root + "DetailActivity.java", root + "LibraryActivity.java",
                root + "HistoryActivity.java", root + "ProvidersActivity.java", root + "PlayerActivity.java",
                root + "MediaProvider.java", root + "DemoProvider.java",
                "app/src/main/res/values/colors.xml", "app/src/main/res/values/strings.xml"
        };
        for (String path : requiredFiles) if (!project.hasPath(path)) throw new IllegalStateException("Missing v0.5 acceptance file: " + path);
        String manifest = project.find("app/src/main/AndroidManifest.xml").content;
        for (String activity : new String[]{"MainActivity","DetailActivity","LibraryActivity","HistoryActivity","ProvidersActivity","PlayerActivity"})
            if (!manifest.contains("." + activity)) throw new IllegalStateException("Manifest missing activity " + activity);
        System.out.println("Generated " + project.files.size() + " files for " + project.packageName + " with six-screen media navigation and local persistence");
    }
}
