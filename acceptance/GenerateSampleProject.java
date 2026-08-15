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
                "Provide a searchable anime catalog and detail pages.",
                "Persist favorites and watch progress locally.",
                "Keep media providers behind replaceable interfaces.",
                "Expose provider loading and failure states clearly."
        );
        List<String> tasks = Arrays.asList(
                "Create the Android application shell.",
                "Define anime, episode, provider, and watch-progress models.",
                "Build the catalog and detail user flow.",
                "Persist favorites and watch progress.",
                "Run Android CI and verify the debug APK."
        );

        GeneratedProject project = new LocalSourceGenerator().generate(
                "AIDao Generated Acceptance App",
                "Build a small Android anime browsing app used to prove that AIDao-generated source compiles into a real APK.",
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
        System.out.println("Generated " + project.files.size() + " files for " + project.packageName);
    }
}
