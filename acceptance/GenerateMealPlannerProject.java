package dev.thefoolish.aidao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/** Emits the semantic meal-planner regression as a real standalone Android project. */
public final class GenerateMealPlannerProject {
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args.length > 0 ? args[0] : "build/generated-meal-planner");
        GeneratedProject project = new LocalSourceGenerator().generate(
                "A meal planning app",
                "Create an offline meal planning app where I can save recipes with ingredients, schedule recipes onto days of the week, favorite recipes, and automatically build a deduplicated shopping list from the scheduled meals. Data must survive restarts.",
                Arrays.asList("Save recipes with ingredients locally", "Schedule recipes Monday through Sunday", "Favorite recipes", "Derive and deduplicate a shopping list from scheduled meals", "Persist all state across restart"),
                Arrays.asList("Generate product", "Validate source", "Build APK"));
        for (String note : project.verificationNotes) if (note != null && note.startsWith("FAIL ")) throw new IllegalStateException(note);
        if (Files.exists(root)) deleteTree(root);
        Files.createDirectories(root);
        for (GeneratedProject.FileEntry file : project.files) {
            Path target = root.resolve(file.path);
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            Files.write(target, file.content.getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("Generated " + project.projectName + " at " + root + " with " + project.files.size() + " files.");
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            Path[] paths = stream.sorted(java.util.Comparator.reverseOrder()).toArray(Path[]::new);
            for (Path path : paths) Files.deleteIfExists(path);
        }
    }
}
