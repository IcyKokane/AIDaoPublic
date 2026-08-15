package dev.thefoolish.aidao;

import java.util.List;

/** CI-only behavioral acceptance for v1 local natural-language planning. */
public final class PlannerBehaviorAcceptance {
    public static void main(String[] args) {
        verifyAnimeProject();
        verifyCapabilityProject();
        verifyRefinementNegation();
        verifyAnimeOptionalFeatureRemoval();
        verifyKeywordBoundaryIsolation();
        verifySafetyBoundaries();
        System.out.println("Planner behavior acceptance passed");
    }

    private static void verifyAnimeProject() {
        ProjectPlanner.Plan p = ProjectPlanner.build(
                "Create an anime streaming app with provider repositories, search, favorites, history, episode playback and resume progress.",
                "Make provider failures visible and keep sources replaceable."
        );
        requireContains(p.requirements, "anime catalog");
        requireContains(p.requirements, "provider interfaces");
        requireContains(p.tasks, "Catalog and Anime Detail");
        requireContains(p.tasks, "Persist favorites, watch history, and episode progress");
        requireNotContains(p.tasks, "activity/event, aggregation");
        requireNotContains(p.tasks, "Bluetooth abstraction");
    }

    private static void verifyCapabilityProject() {
        ProjectPlanner.Plan p = ProjectPlanner.build(
                "Build a field inspection tracker that syncs with a remote API, scans QR codes with the camera, uses GPS routes, sends reminders, performs scheduled background sync, and connects to a BLE sensor.",
                "Data must remain understandable when permissions or the network are unavailable."
        );
        requireContains(p.requirements, "remote/network data access");
        requireContains(p.requirements, "camera/media access");
        requireContains(p.requirements, "Android notifications");
        requireContains(p.requirements, "Use location only");
        requireContains(p.requirements, "scheduled/background work");
        requireContains(p.requirements, "Bluetooth/Nearby Devices");
        requireContains(p.tasks, "network/data gateway");
        requireContains(p.tasks, "camera/media capture or picker flow");
        requireContains(p.tasks, "notification channels");
        requireContains(p.tasks, "route/location service");
        requireContains(p.tasks, "WorkManager-style scheduling boundaries");
        requireContains(p.tasks, "Bluetooth abstraction");
    }

    private static void verifyRefinementNegation() {
        ProjectPlanner.Plan p = ProjectPlanner.build(
                "Create a social app with chat, notifications, location, and cloud sync.",
                "Remove location. Disable notification. Keep chat and cloud sync."
        );
        requireContains(p.requirements, "profile, conversation/list");
        requireContains(p.requirements, "remote/network data access");
        requireNotContains(p.requirements, "Use location only");
        requireNotContains(p.requirements, "Android notifications");
    }

    private static void verifyAnimeOptionalFeatureRemoval() {
        ProjectPlanner.Plan p = ProjectPlanner.build(
                "Create an anime app with provider repositories, search, favorites, watch history, playback, and resume progress.",
                "Remove favorites. Remove history. Remove search. Keep episode playback and provider management."
        );
        requireContains(p.requirements, "anime catalog with details and episode lists");
        requireContains(p.requirements, "provider interfaces");
        requireNotContains(p.requirements, "search/browse");
        requireNotContains(p.requirements, "favorites/library");
        requireNotContains(p.requirements, "watch history");
        requireNotContains(p.requirements, "visible search/filter interaction");
        requireNotContains(p.requirements, "Persist user favorites/bookmarks");
        requireNotContains(p.requirements, "history/recent activity surface");
        requireNotContains(p.tasks, "Persist favorites");
        requireContains(p.tasks, "Player");
    }

    private static void verifyKeywordBoundaryIsolation() {
        ProjectPlanner.Plan p = ProjectPlanner.build(
                "Create a document viewer with visible empty states and stable navigation.",
                "Keep the experience reliable and readable."
        );
        requireContains(p.requirements, "creating, editing, viewing, searching");
        requireNotContains(p.requirements, "Bluetooth/Nearby Devices");
        requireNotContains(p.tasks, "Bluetooth abstraction");
        requireNotContains(p.tasks, "activity/event, aggregation");
    }

    private static void verifySafetyBoundaries() {
        ProjectPlanner.Plan p = ProjectPlanner.build(
                "Create an AI assistant with login, file import, cloud API access, and local offline data.",
                ""
        );
        requireContains(p.requirements, "authentication/account state");
        requireContains(p.requirements, "user-controlled file import/export");
        requireContains(p.requirements, "AI-assisted behavior");
        requireContains(p.assumptions, "API keys");
        requireContains(p.assumptions, "Imported/shared material is treated as data/knowledge");
        requireContains(p.assumptions, "Installation, external publishing, spending, credential use, and destructive actions remain user-controlled");
    }

    private static void requireContains(List<String> values, String needle) {
        for (String value : values) if (value.contains(needle)) return;
        throw new IllegalStateException("Expected planner output containing: " + needle + "\nActual: " + values);
    }

    private static void requireNotContains(List<String> values, String needle) {
        for (String value : values) if (value.contains(needle))
            throw new IllegalStateException("Planner output unexpectedly contains: " + needle + "\nActual: " + values);
    }
}
