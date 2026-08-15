package dev.thefoolish.aidao;

import java.util.Arrays;
import java.util.List;

/** CI-only acceptance coverage for structural validation of generated projects. */
public final class GeneratedProjectValidationAcceptance {
    public static void main(String[] args) {
        List<String> requirements = Arrays.asList(
                "Provide a searchable anime catalog, detail pages, favorites, history, providers, and playback.",
                "Persist watch progress locally and isolate provider failures.",
                "Generate multiple Android screens, resources, manifest declarations, and reusable navigation."
        );
        List<String> tasks = Arrays.asList(
                "Generate the Android source tree.",
                "Verify required files and navigation.",
                "Build the generated project with trusted CI."
        );

        GeneratedProject good = new LocalSourceGenerator().generate(
                "AIDao V1 Validation Sample",
                "Create a multi-screen anime app with search, detail, favorites, watch history, provider isolation and playback.",
                requirements,
                tasks
        );
        GeneratedProjectValidator.Result result = new GeneratedProjectValidator().validate(good);
        for (String note : result.notes) System.out.println(note);
        if (!result.valid) throw new IllegalStateException("Expected generated project to pass structural validation");

        GeneratedProject broken = new GeneratedProject(
                "Broken",
                "dev.thefoolish.generated.broken",
                Arrays.asList(
                        new GeneratedProject.FileEntry("../escape.txt", "bad", "test"),
                        new GeneratedProject.FileEntry("settings.gradle.kts", "", "test"),
                        new GeneratedProject.FileEntry("settings.gradle.kts", "duplicate", "test")
                ),
                Arrays.asList("test")
        );
        GeneratedProjectValidator.Result brokenResult = new GeneratedProjectValidator().validate(broken);
        if (brokenResult.valid) throw new IllegalStateException("Expected unsafe/incomplete project to fail validation");
        boolean unsafeSeen = false, duplicateSeen = false;
        for (String note : brokenResult.notes) {
            if (note.contains("unsafe generated path")) unsafeSeen = true;
            if (note.contains("duplicate generated path")) duplicateSeen = true;
        }
        if (!unsafeSeen || !duplicateSeen) throw new IllegalStateException("Validator did not report expected path failures");
        System.out.println("Generated-project structural validation acceptance passed");
    }
}
