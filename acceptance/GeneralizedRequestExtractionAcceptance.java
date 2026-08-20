package dev.thefoolish.aidao;

import java.util.Arrays;

/**
 * Broad request-fidelity regression outside the notepad/workout special profiles.
 * The generator must translate explicit product identity, navigation, theme, data fields,
 * and persistence requirements into source rather than falling back to a fixed generic shell.
 */
public final class GeneralizedRequestExtractionAcceptance {
    public static void main(String[] args) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Create A Pantry Inventory App Called PantryQuest",
                "Create a pantry inventory app called PantryQuest. Use top tab navigation. Use a modern teal and orange theme. Let me add pantry items with a quantity, change the quantity later, and keep the inventory after restarting the app.",
                Arrays.asList(
                        "App name is PantryQuest.",
                        "Use top tab navigation.",
                        "Use a teal and orange modern theme.",
                        "Add pantry items with quantity.",
                        "Allow quantity edits.",
                        "Persist inventory across restart."
                ),
                Arrays.asList("Generate identity", "Generate inventory UI", "Persist inventory", "Validate explicit request fidelity")
        );

        assertNoFail(project);

        if (!"PantryQuest".equals(project.projectName)) {
            throw new IllegalStateException("explicit app name was not preserved: " + project.projectName);
        }

        String strings = content(project, "app/src/main/res/values/strings.xml");
        String manifest = content(project, "app/src/main/AndroidManifest.xml");
        String all = allContent(project);
        String lower = all.toLowerCase();

        if (!strings.contains(">PantryQuest</string>")) {
            throw new IllegalStateException("app_name is not synchronized with explicit PantryQuest identity");
        }
        if (!manifest.contains("android:label=\"PantryQuest\"")) {
            throw new IllegalStateException("manifest label is not synchronized with explicit PantryQuest identity");
        }

        boolean tabNavigation = lower.contains("tablayout") || lower.contains("tabhost") ||
                lower.contains("topnav") || lower.contains("top_tabs") || lower.contains("top tabs") ||
                (lower.contains("linearlayout.horizontal") && lower.contains("tab"));
        if (!tabNavigation) {
            throw new IllegalStateException("explicit top-tab navigation request fell back to generic navigation");
        }

        boolean teal = lower.contains("#0d9488") || lower.contains("#14b8a6") ||
                lower.contains("teal") || lower.contains("0xff0d9488") || lower.contains("0xff14b8a6");
        boolean orange = lower.contains("#f97316") || lower.contains("#fb923c") ||
                lower.contains("orange") || lower.contains("0xfff97316") || lower.contains("0xfffb923c");
        if (!teal || !orange) {
            throw new IllegalStateException("explicit teal/orange theme request was not materially represented");
        }

        if (!lower.contains("quantity")) {
            throw new IllegalStateException("inventory quantity field/behavior is missing");
        }
        boolean quantityMutation = lower.contains("setquantity") || lower.contains("quantity=") ||
                lower.contains("quantity +") || lower.contains("quantity-") ||
                lower.contains("puttext") || lower.contains("putint") || lower.contains("number(");
        if (!quantityMutation) {
            throw new IllegalStateException("quantity exists only as display text and cannot be changed/persisted");
        }

        boolean persistedInventory = lower.contains("sharedpreferences") || lower.contains("localstore") ||
                lower.contains("puttext") || lower.contains("putint") || lower.contains("store.text(") || lower.contains("store.number(");
        if (!persistedInventory) {
            throw new IllegalStateException("inventory does not have a persisted restart-safe storage path");
        }

        System.out.println("PASS generalized request extraction regression");
    }

    private static void assertNoFail(GeneratedProject project) {
        for (String note : project.verificationNotes) {
            if (note.startsWith("FAIL ")) {
                throw new IllegalStateException("generalized request became source blocked: " + note);
            }
        }
    }

    private static String content(GeneratedProject project, String path) {
        for (GeneratedProject.FileEntry f : project.files) {
            if (f != null && path.equals(f.path)) return f.content == null ? "" : f.content;
        }
        throw new IllegalStateException("Missing generated file " + path);
    }

    private static String allContent(GeneratedProject project) {
        StringBuilder b = new StringBuilder();
        for (GeneratedProject.FileEntry f : project.files) {
            if (f != null && f.content != null) b.append('\n').append(f.content);
        }
        return b.toString();
    }
}
