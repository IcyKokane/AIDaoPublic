package dev.thefoolish.aidao;

import java.util.Arrays;
import java.util.Collections;

/** Regression coverage for real nontrivial product semantics and false-completion blocking. */
public final class SemanticProductAcceptance {
    public static void main(String[] args) {
        LocalSourceGenerator g = new LocalSourceGenerator();

        GeneratedProject finance = g.generate(
                "Pocket Ledger",
                "Create an offline expense and budget tracker. Add transactions with category and amount, persist them after restart, set a monthly budget, and show totals by category.",
                Arrays.asList("offline persistence", "transaction entry", "monthly budget", "reports by category"),
                Collections.singletonList("Build coherent local finance product"));
        requireNoUnexpectedFailure(finance, "finance");
        requireContains(finance, "/TransactionsActivity.java", "store.putText(\"transactions\"", "finance transaction persistence");
        requireContains(finance, "/BudgetsActivity.java", "monthly_budget", "finance budget persistence");
        requireContains(finance, "/ReportsActivity.java", "By category", "finance computed category report");

        GeneratedProject habits = g.generate(
                "Ritual Streak",
                "Make an offline daily habit tracker. Let me create habits, check them off, see today's completion percentage and all-time check-ins, and clear local data. It must survive app restart.",
                Arrays.asList("habit creation", "daily check-in", "progress report", "restart-safe local persistence"),
                Collections.singletonList("Build functional habit tracker"));
        requireNoUnexpectedFailure(habits, "habit");
        requireContains(habits, "/TimelineActivity.java", "Complete today", "habit check-in behavior");
        requireContains(habits, "/ReportsActivity.java", "habit_checkins", "habit report behavior");
        requireContains(habits, "/DataControlsActivity.java", "Clear habit data", "habit data controls");

        GeneratedProject impossible = g.generate(
                "Cloud Lens",
                "Create a camera app that scans photos and uploads them to a remote backend API, sends notifications when processing finishes, and works in the background.",
                Arrays.asList("camera", "remote backend API", "notifications", "background processing"),
                Collections.singletonList("Generate only implemented capabilities"));
        boolean semanticFailure = false;
        for (String note : impossible.verificationNotes) {
            if (note.startsWith("FAIL requested ")) semanticFailure = true;
        }
        if (!semanticFailure) throw new IllegalStateException("complex unimplemented capabilities were falsely reported complete");

        System.out.println("Semantic product acceptance passed");
    }

    private static void requireNoUnexpectedFailure(GeneratedProject p, String label) {
        for (String note : p.verificationNotes) {
            if (note.startsWith("FAIL ")) throw new IllegalStateException(label + " fidelity failure: " + note);
        }
    }

    private static void requireContains(GeneratedProject p, String suffix, String needle, String label) {
        for (GeneratedProject.FileEntry f : p.files) {
            if (f != null && f.path != null && f.path.endsWith(suffix) && f.content != null && f.content.contains(needle)) return;
        }
        throw new IllegalStateException("missing " + label + " marker: " + needle);
    }
}
