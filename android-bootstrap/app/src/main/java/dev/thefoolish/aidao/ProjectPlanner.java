package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Local, deterministic planning layer used before a model-backed planner is connected.
 * It converts the user's brief and accumulated project context into explicit requirements
 * and implementation tasks without performing network calls or modifying source code.
 */
public final class ProjectPlanner {
    public static final class Plan {
        public final List<String> requirements;
        public final List<String> tasks;
        public final List<String> assumptions;

        Plan(List<String> requirements, List<String> tasks, List<String> assumptions) {
            this.requirements = Collections.unmodifiableList(requirements);
            this.tasks = Collections.unmodifiableList(tasks);
            this.assumptions = Collections.unmodifiableList(assumptions);
        }
    }

    private ProjectPlanner() {}

    public static Plan build(String brief, String context) {
        String source = normalize((brief == null ? "" : brief) + " " + (context == null ? "" : context));
        Set<String> requirements = new LinkedHashSet<>();
        Set<String> tasks = new LinkedHashSet<>();
        List<String> assumptions = new ArrayList<>();

        requirements.add("Provide an Android-native application with a clear primary user flow and persistent project state.");
        tasks.add("Create the Android application shell, navigation, theme, and project-level state model.");

        boolean anime = containsAny(source, "anime", "episode", "watch anime", "stream anime");
        boolean media = anime || containsAny(source, "video", "media", "stream", "player", "playback");
        boolean providers = containsAny(source, "plugin", "extension", "provider", "repository", "repo", "source");

        if (anime) {
            requirements.add("Provide an anime catalog with search, browsing, anime details, episode lists, watch progress, and library/favorites state.");
            requirements.add("Keep anime metadata and streaming-source discovery behind provider interfaces so individual sources can fail or change without breaking the whole app.");
            requirements.add("Provide clear provider availability, loading, empty, and failure states rather than silently hiding source errors.");
            requirements.add("Persist watch history and episode progress locally so users can resume where they stopped.");

            tasks.add("Define anime, episode, provider, watch-progress, and library data models.");
            tasks.add("Implement provider contracts for catalog search, anime details, episode discovery, and stream resolution.");
            tasks.add("Build the catalog/home screen with search, browse results, loading states, and provider attribution.");
            tasks.add("Build anime detail screens with metadata, episode lists, library/favorite controls, and provider switching where available.");
            tasks.add("Implement video playback with explicit stream selection, playback errors, resume position, and orientation/fullscreen handling.");
            tasks.add("Persist watch history, episode progress, favorites, and recent activity locally on-device.");
            tasks.add("Add provider failure isolation so a broken source does not break browsing through healthy providers.");
            tasks.add("Add an extensions/providers management surface showing installed, enabled, disabled, and failing providers.");
        } else if (media) {
            requirements.add("Provide media catalog/detail browsing with playback and visible provider/error states.");
            tasks.add("Define media catalog, detail, playback, and provider data models.");
            tasks.add("Build media browse/detail surfaces and connect them through a provider boundary.");
            tasks.add("Implement playback, resume state, and visible playback failure handling.");
        } else {
            tasks.add("Implement the primary feature flow described by the project brief using explicit screen and data-state boundaries.");
        }

        feature(source, requirements, tasks, new String[]{"login", "account", "sign in", "oauth"},
                "Support user authentication and account state.", "Implement authentication screens, session state, and secure credential handling.");
        feature(source, requirements, tasks, new String[]{"github"},
                "Integrate with a user-controlled GitHub repository.", "Add repository connection, project synchronization, and visible GitHub status.");
        feature(source, requirements, tasks, new String[]{"download", "file", "upload", "import", "export"},
                "Allow user-controlled file import or export where required.", "Implement Android document-picker based file access with scoped-storage-safe handling.");
        if (providers && !anime) feature(source, requirements, tasks, new String[]{"plugin", "extension", "provider", "repository", "repo", "source"},
                "Support replaceable provider or plugin-style data sources.", "Define a provider interface and repository-driven provider discovery boundary.");
        feature(source, requirements, tasks, new String[]{"offline", "local", "device"},
                "Preserve useful project data locally on the device.", "Add local persistence with explicit ownership and clearing controls.");
        feature(source, requirements, tasks, new String[]{"notification", "notify", "alert"},
                "Surface relevant user-visible notifications without hidden background behavior.", "Add notification permission handling, channels, and user-controlled notification settings.");
        feature(source, requirements, tasks, new String[]{"location", "route", "map", "gps"},
                "Use location only with explicit Android permission and visible user control.", "Implement permission-gated location access and a separable route/location service.");
        feature(source, requirements, tasks, new String[]{"ai", "model", "assistant", "generate"},
                "Expose AI-assisted behavior through a visible, user-controlled workflow.", "Define a model-provider boundary, request state, errors, and approval points before destructive actions.");

        tasks.add("Add automated verification for project-state persistence and the primary user flow.");
        tasks.add("Run Android CI, repair build/test failures, and produce an installable debug APK only after verification succeeds.");

        if (source.isEmpty()) assumptions.add("The project brief is incomplete; planning remains a safe Android baseline until more context is supplied.");
        else assumptions.add("Requirements are inferred from ordinary-language project context and should remain editable before implementation begins.");
        if (anime) assumptions.add("Provider architecture is treated as a technical boundary; AIDao does not assume a particular unverified content source or repository is available.");
        assumptions.add("Installation, external publishing, spending, credential use, and destructive actions remain user-controlled.");

        return new Plan(new ArrayList<>(requirements), new ArrayList<>(tasks), assumptions);
    }

    private static boolean containsAny(String source, String... terms) {
        for (String term : terms) if (source.contains(term)) return true;
        return false;
    }

    private static void feature(String source, Set<String> requirements, Set<String> tasks, String[] terms, String requirement, String task) {
        for (String term : terms) {
            if (source.contains(term)) {
                requirements.add(requirement);
                tasks.add(task);
                return;
            }
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.US).replaceAll("\\s+", " ").trim();
    }
}
