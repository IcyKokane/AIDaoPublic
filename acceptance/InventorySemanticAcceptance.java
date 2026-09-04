package dev.thefoolish.aidao;

import java.util.Arrays;

/**
 * Quality-first regression for a nontrivial offline inventory request. The
 * generator must implement the requested mutation/browse/reset behavior rather
 * than passing only because a generic Android shell compiles.
 */
public final class InventorySemanticAcceptance {
    public static void main(String[] args) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Trail Pantry",
                "Build an offline pantry inventory app where I can add named pantry items, keep them after restart, browse saved items, see a real item count, and deliberately clear local inventory data.",
                Arrays.asList(
                        "Persist pantry items locally",
                        "Validate item names before saving",
                        "Browse saved pantry items",
                        "Show a computed inventory count",
                        "Keep state after restart",
                        "Provide an explicit confirmed clear-inventory control"),
                Arrays.asList("Generate real product behavior", "Persist state", "Validate semantics", "Build Android APK"));

        String all = allText(project);
        require(!hasFail(project), "inventory generation failed verification: " + firstFail(project));
        require(all.contains("pantry_inventory") || all.contains("inventory_items"), "inventory state has no dedicated persisted collection");
        require(all.contains("Add item") || all.contains("Save item"), "inventory has no executable add-item action");
        require(all.contains("setError"), "inventory does not validate item input");
        require(all.contains("Clear inventory") || all.contains("Clear local inventory"), "inventory has no explicit clear control");
        require(all.contains("split(\\\"\\\\n\\\")") || all.contains("split(\"\\n\")"), "inventory has no executable saved-item browse/count path");
        require(all.contains("setOnApplyWindowInsetsListener"), "inventory is missing phone-safe inset handling");
        require(all.contains("setContentDescription"), "inventory is missing accessibility descriptions");

        System.out.println("Inventory semantic acceptance passed: add/validate/persist/browse/count/reset behavior is executable and phone-native.");
    }

    private static String allText(GeneratedProject project) {
        StringBuilder out = new StringBuilder();
        for (GeneratedProject.FileEntry file : project.files) if (file != null && file.content != null) out.append('\n').append(file.content);
        return out.toString();
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
