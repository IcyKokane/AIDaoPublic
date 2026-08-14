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

        requirements.add("Provide an Android-native application with a clear primary user flow.");
        tasks.add("Create the Android application shell and primary navigation.");
        tasks.add("Implement the core feature flow described by the project brief.");
        tasks.add("Add build verification and produce an installable debug APK.");

        feature(source, requirements, tasks, new String[]{"login", "account", "sign in", "oauth"},
                "Support user authentication and account state.", "Implement authentication screens, session state, and secure credential handling.");
        feature(source, requirements, tasks, new String[]{"github", "repository", "repo"},
                "Integrate with a user-controlled GitHub repository.", "Add repository connection, project synchronization, and visible GitHub status.");
        feature(source, requirements, tasks, new String[]{"download", "file", "upload", "import"},
                "Allow user-controlled file import or export where required.", "Implement Android document-picker based file access with scoped-storage-safe handling.");
        feature(source, requirements, tasks, new String[]{"video", "media", "anime", "stream"},
                "Provide media-oriented browsing and presentation where required.", "Create media list/detail surfaces and isolate provider/data-source interfaces from the UI.");
        feature(source, requirements, tasks, new String[]{"plugin", "extension", "provider"},
                "Support replaceable provider or plugin-style data sources.", "Define a provider interface and repository-driven provider discovery boundary.");
        feature(source, requirements, tasks, new String[]{"offline", "local", "device"},
                "Preserve useful project data locally on the device.", "Add local persistence with explicit ownership and clearing controls.");
        feature(source, requirements, tasks, new String[]{"notification", "notify", "alert"},
                "Surface relevant user-visible notifications without hidden background behavior.", "Add notification permission handling, channels, and user-controlled notification settings.");
        feature(source, requirements, tasks, new String[]{"location", "route", "map", "gps"},
                "Use location only with explicit Android permission and visible user control.", "Implement permission-gated location access and a separable route/location service.");
        feature(source, requirements, tasks, new String[]{"ai", "model", "assistant", "generate"},
                "Expose AI-assisted behavior through a visible, user-controlled workflow.", "Define a model-provider boundary, request state, errors, and approval points before destructive actions.");

        if (source.isEmpty()) assumptions.add("The project brief is incomplete; planning remains a safe Android baseline until more context is supplied.");
        else assumptions.add("Requirements are inferred from ordinary-language project context and should remain editable before implementation.");
        assumptions.add("Installation, external publishing, spending, and destructive actions remain user-controlled.");

        return new Plan(new ArrayList<>(requirements), new ArrayList<>(tasks), assumptions);
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
