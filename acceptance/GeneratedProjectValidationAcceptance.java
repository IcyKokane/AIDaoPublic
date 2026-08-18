package dev.thefoolish.aidao;

import java.util.Arrays;
import java.util.List;

/** CI-only acceptance coverage for structural validation of generated projects. */
public final class GeneratedProjectValidationAcceptance {
    public static void main(String[] args) {
        // Keep this acceptance focused on the structural validator itself. Media
        // capability/fidelity modules have their own end-to-end acceptance workflow;
        // using a non-media product here makes missing optional reflective modules
        // impossible to mask or misdiagnose as a structural-validation failure.
        List<String> requirements = Arrays.asList(
                "Provide local transactions, monthly budgeting, and reports.",
                "Persist finance state locally and validate user-entered amounts.",
                "Generate multiple Android screens, resources, manifest declarations, and reusable navigation."
        );
        List<String> tasks = Arrays.asList(
                "Generate the Android source tree.",
                "Verify required files and navigation.",
                "Build the generated project with trusted CI."
        );

        GeneratedProject good = new LocalSourceGenerator().generate(
                "AIDao V1 Validation Sample",
                "Create a multi-screen offline expense and budget app with transactions, budget controls, reports, persistence, and input validation.",
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
