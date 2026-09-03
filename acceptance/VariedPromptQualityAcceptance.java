package dev.thefoolish.aidao;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * V1 quality gate for varied nontrivial requests. The generator must either
 * produce durable, Android-native behavior or explicitly fail verification for
 * capabilities it cannot genuinely implement. A compiling shell is not enough.
 */
public final class VariedPromptQualityAcceptance {
    public static void main(String[] args) {
        requireCoherentOfflineProduct(
                "Trail Pantry",
                "Build an offline pantry inventory app where I can add named pantry items, keep them after restart, browse saved items, and deliberately clear local data.",
                Arrays.asList("Persist pantry items locally", "Validate item names", "Keep state after restart", "Provide explicit local data controls"));

        requireCoherentOfflineProduct(
                "Reading Harbor",
                "Build a private offline reading log with saved book entries, notes, local search, and state that survives Android process restart.",
                Arrays.asList("Persist reading entries", "Persist notes", "Search saved entries", "Recover state after restart"));

        requireSemanticProduct(
                "Pocket Ledger",
                "Build an offline personal budget app where I can enter categorized transactions, set a monthly budget, and view computed spending reports after restart.",
                Arrays.asList("Persist categorized transactions", "Set and persist a monthly budget", "Compute spending reports from saved transactions", "Keep state after restart"),
                new String[]{"Save transaction", "monthly_budget", "Reports", "By category"});

        requireSemanticProduct(
                "Daily Forge",
                "Build an offline habit tracker where I can create habits, check them off each day, view completion percentage and all-time check-ins, and clear my local habit data.",
                Arrays.asList("Create and persist habits", "Record daily check-ins", "Show completion percentage", "Show all-time check-ins", "Provide explicit local data controls"),
                new String[]{"Add habit", "Complete today", "habit_checkins", "%", "Clear habit data"});

        requireUnsupportedCapabilityRejected(
                "Scan Shelf",
                "Build an offline inventory app that scans item barcodes with the Android camera and saves results locally.",
                Arrays.asList("Use the camera for barcode scanning", "Persist scanned inventory locally"),
                "camera/media capture");

        requireUnsupportedCapabilityRejected(
                "Geo Field Log",
                "Build a field journal that automatically captures my GPS location for every entry and keeps entries offline.",
                Arrays.asList("Capture GPS location", "Persist field entries locally"),
                "location");

        requireUnsupportedCapabilityRejected(
                "Reminder Garden",
                "Build an offline plant-care tracker that sends Android reminder notifications for watering schedules.",
                Arrays.asList("Persist plants locally", "Send Android watering reminders"),
                "Android notifications");

        System.out.println("Varied prompt quality acceptance passed: coherent offline products remain durable/native, semantic products implement requested behavior, and unsupported capabilities cannot fake completion.");
    }

    private static void requireCoherentOfflineProduct(String name, String brief, List<String> requirements) {
        GeneratedProject project = new LocalSourceGenerator().generate(name, brief, requirements,
                Arrays.asList("Generate real product behavior", "Persist state", "Validate source", "Build Android APK"));
        assertNativeProduct(name, project);
    }

    private static void requireSemanticProduct(String name, String brief, List<String> requirements, String[] executableMarkers) {
        GeneratedProject project = new LocalSourceGenerator().generate(name, brief, requirements,
                Arrays.asList("Generate requested product behavior", "Persist state", "Validate semantics", "Build Android APK"));
        assertNativeProduct(name, project);
        String all = allText(project);
        for (String marker : executableMarkers) {
            require(all.contains(marker), name + " is missing semantic executable marker: " + marker);
        }
    }

    private static void assertNativeProduct(String name, GeneratedProject project) {
        String all = allText(project);
        require(!hasFail(project), name + " produced a verification failure: " + firstFail(project));
        require(all.contains("SharedPreferences") || all.contains("LocalStore"), name + " has no durable local persistence implementation");
        require(all.contains("setOnApplyWindowInsetsListener"), name + " has no system-bar inset handling");
        require(all.contains("AppNavigator.open"), name + " has no real screen navigation path");
        require(all.contains("setContentDescription"), name + " has no accessibility content descriptions");
        require(!containsPlaceholder(all), name + " retained placeholder/fake-completion language");
        require(project.projectName != null && project.projectName.length() >= 2 && project.projectName.length() <= 40,
                name + " generated an invalid app identity");
    }

    private static void requireUnsupportedCapabilityRejected(String name, String brief, List<String> requirements, String capabilityLabel) {
        GeneratedProject project = new LocalSourceGenerator().generate(name, brief, requirements,
                Collections.singletonList("Generate and verify source without fake completion"));
        boolean rejected = false;
        for (String note : project.verificationNotes) {
            if (note != null && note.startsWith("FAIL ") && note.toLowerCase().contains(capabilityLabel.toLowerCase())) {
                rejected = true;
                break;
            }
        }
        require(rejected, name + " falsely passed despite unsupported requested capability: " + capabilityLabel + "\nNotes: " + project.verificationNotes);
    }

    private static String allText(GeneratedProject project) {
        StringBuilder b = new StringBuilder();
        for (GeneratedProject.FileEntry file : project.files) {
            if (file != null && file.content != null) b.append('\n').append(file.content);
        }
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

    private static boolean containsPlaceholder(String source) {
        String lower = source.toLowerCase();
        return lower.contains("todo: implement") || lower.contains("coming soon") || lower.contains("placeholder data") || lower.contains("sample only");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}