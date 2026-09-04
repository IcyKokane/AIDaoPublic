package dev.thefoolish.aidao;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * V1 regression gate for platform capabilities that the deterministic local
 * generator must not pretend to implement. A request can still generate the
 * locally supported parts of a product, but APK-ready verification must fail
 * when a required external/platform integration has no executable evidence.
 */
public final class UnsupportedPlatformCapabilityAcceptance {
    public static void main(String[] args) {
        requireRejected(
                "BLE Sensor Log",
                "Build an offline sensor journal that connects to a Bluetooth LE thermometer, reads measurements, and stores them locally.",
                Arrays.asList("Connect to a Bluetooth LE sensor", "Read measurements", "Persist readings locally"),
                "Bluetooth/Nearby Devices");

        requireRejected(
                "Background Sync Notes",
                "Build a notes app that persists notes locally and performs periodic background sync while the app is closed.",
                Arrays.asList("Persist notes locally", "Run periodic background sync"),
                "scheduled/background work");

        requireRejected(
                "Account Journal",
                "Build a private journal with sign in and local offline entries.",
                Arrays.asList("Require user sign in", "Persist journal entries locally"),
                "authentication");

        requireRejected(
                "Remote Team Board",
                "Build a task board that stores local state but also syncs tasks to a remote backend API.",
                Arrays.asList("Persist task state locally", "Sync tasks with a remote backend API"),
                "network/backend data");

        System.out.println("Unsupported platform capability acceptance passed: Bluetooth, background work, authentication, and remote backend requests cannot report fake completion without executable integration evidence.");
    }

    private static void requireRejected(String name, String brief, List<String> requirements, String capabilityLabel) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                name,
                brief,
                requirements,
                Collections.singletonList("Generate supported behavior and reject unsupported required capabilities rather than faking completion"));

        boolean rejected = false;
        for (String note : project.verificationNotes) {
            if (note != null && note.startsWith("FAIL ") && note.toLowerCase().contains(capabilityLabel.toLowerCase())) {
                rejected = true;
                break;
            }
        }
        if (!rejected) {
            throw new IllegalStateException(name + " falsely passed required capability " + capabilityLabel + ". Notes: " + project.verificationNotes);
        }
    }
}
