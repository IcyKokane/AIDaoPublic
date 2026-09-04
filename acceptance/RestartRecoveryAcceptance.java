package dev.thefoolish.aidao;

import java.util.Arrays;
import java.util.List;

/**
 * V1 regression gate for restart recovery and deterministic regeneration.
 * Generated products must persist user state, keep phone-native affordances,
 * and produce byte-stable source for the same approved specification.
 */
public final class RestartRecoveryAcceptance {
    public static void main(String[] args) {
        verifyProduct(
                "Ledger Restart",
                "Build an offline budget app with categorized transactions, a monthly budget, reports, and state that survives Android process restart.",
                Arrays.asList("Persist categorized transactions", "Persist a monthly budget", "Compute reports", "Recover state after restart"),
                new String[]{"transactions", "monthly_budget", "SharedPreferences"});

        verifyProduct(
                "Draft Harbor",
                "Build an offline notes workspace with saved documents, editable drafts, local search, and draft recovery after the app is killed and reopened.",
                Arrays.asList("Persist documents locally", "Recover an in-progress draft", "Search saved documents", "Keep state after restart"),
                new String[]{"documents", "draft", "SharedPreferences"});

        verifyProduct(
                "Habit Restart",
                "Build an offline habit tracker where I can create habits, complete them today, view progress, and keep every check-in after restart.",
                Arrays.asList("Persist habits", "Persist daily check-ins", "Show progress", "Recover state after restart"),
                new String[]{"habits", "habit_checkins", "SharedPreferences"});

        verifyDeterministicRegeneration();
        System.out.println("Restart/recovery acceptance passed: representative products persist state and identical approved requests regenerate byte-stable normalized source.");
    }

    private static void verifyProduct(String name, String brief, List<String> requirements, String[] markers) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                name,
                brief,
                requirements,
                Arrays.asList("Generate real Android behavior", "Persist state", "Validate source", "Build APK"));
        require(!hasFail(project), name + " failed verification: " + firstFail(project));
        String all = allText(project);
        for (String marker : markers) require(all.contains(marker), name + " is missing restart-state marker: " + marker);
        require(all.contains("setOnApplyWindowInsetsListener"), name + " is missing system inset handling");
        require(all.contains("setContentDescription"), name + " is missing accessibility descriptions");
        require(all.contains("AppNavigator.open") || all.contains("startActivity("), name + " has no executable navigation path");
        require(!all.contains("android.widget.android.widget."), name + " contains a repeated Android package qualifier");
    }

    private static void verifyDeterministicRegeneration() {
        String name = "Deterministic Pantry";
        String brief = "Build an offline pantry inventory app with named items, quantities, local persistence, browsing, and explicit clear-data controls.";
        List<String> requirements = Arrays.asList("Persist pantry items", "Validate names", "Persist quantities", "Clear local data explicitly");
        List<String> tasks = Arrays.asList("Generate real product behavior", "Normalize source", "Validate and build");
        GeneratedProject first = new LocalSourceGenerator().generate(name, brief, requirements, tasks);
        GeneratedProject second = new LocalSourceGenerator().generate(name, brief, requirements, tasks);
        require(!hasFail(first), "first deterministic generation failed: " + firstFail(first));
        require(!hasFail(second), "second deterministic generation failed: " + firstFail(second));
        require(first.files.size() == second.files.size(), "identical requests generated different file counts");
        for (int i = 0; i < first.files.size(); i++) {
            GeneratedProject.FileEntry a = first.files.get(i), b = second.files.get(i);
            require(a.path.equals(b.path), "identical requests generated different path ordering at index " + i);
            require(a.content.equals(b.content), "identical requests generated non-deterministic content for " + a.path);
            require(!a.content.contains("android.widget.android.widget."), "normalization is not idempotent for " + a.path);
        }
    }

    private static String allText(GeneratedProject project) {
        StringBuilder b = new StringBuilder();
        for (GeneratedProject.FileEntry file : project.files) if (file != null && file.content != null) b.append('\n').append(file.content);
        return b.toString();
    }

    private static boolean hasFail(GeneratedProject project) {
        for (String note : project.verificationNotes) if (note != null && note.startsWith("FAIL ")) return true;
        return false;
    }

    private static String firstFail(GeneratedProject project) {
        for (String note : project.verificationNotes) if (note != null && note.startsWith("FAIL ")) return note;
        return "unknown";
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
