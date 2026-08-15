package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Deterministic local planner used before an optional model-backed planner is connected. */
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

        requirements.add("Provide an Android-native application whose screens and navigation reflect the requested primary user flows.");
        requirements.add("Persist user-owned project/application state that should survive ordinary app restarts.");
        tasks.add("Create the Android application shell, theme, navigation graph, reusable UI helpers, and project-level state model.");

        boolean anime = containsAny(source, "anime", "episode", "watch anime", "stream anime", "mihon");
        boolean media = anime || containsAny(source, "video", "media", "stream", "player", "playback", "movie", "music");
        boolean providers = containsAny(source, "plugin", "extension", "provider", "repository", "repo", "source");
        boolean listHeavy = containsAny(source, "list", "tasks", "todo", "notes", "inventory", "library", "catalog", "collection", "history");
        boolean search = containsAny(source, "search", "find", "browse", "filter", "sort");
        boolean detail = containsAny(source, "detail", "profile", "item", "episode", "product", "entry", "record");
        boolean edit = containsAny(source, "create", "add", "edit", "update", "delete", "remove", "write", "enter", "input");
        boolean settings = containsAny(source, "settings", "preferences", "configure", "configuration", "options");

        if (anime) {
            requirements.add("Provide an anime catalog with search/browse, anime detail pages, episode lists, watch progress, history, and library/favorites state.");
            requirements.add("Keep anime metadata and stream discovery behind replaceable provider contracts so a single source can fail without breaking healthy providers.");
            requirements.add("Provide provider failure isolation with visible provider installation, enabled/disabled, health, loading, empty, and error states without silently executing untrusted provider content.");
            requirements.add("Persist watch history, favorites, watched state, and per-episode resume position locally on-device.");
            requirements.add("Provide explicit playback loading, source-selection, resume, fullscreen/orientation, and failure states.");

            tasks.add("Define anime, episode, provider, stream candidate, watch-progress, history, and library data models.");
            tasks.add("Implement provider contracts for catalog search, anime details, episode discovery, and stream resolution.");
            tasks.add("Build a catalog/home screen with search, browse results, loading/empty/error states, and provider attribution.");
            tasks.add("Build anime detail and episode screens with metadata, episode lists, favorite controls, watched state, and provider switching.");
            tasks.add("Implement a playback screen with explicit stream selection, resume position, playback errors, and fullscreen/orientation handling.");
            tasks.add("Persist watch history, episode progress, favorites, and recent activity locally on-device.");
            tasks.add("Add provider failure isolation so broken providers do not prevent browsing healthy providers.");
            tasks.add("Add a provider-management screen showing available, installed, enabled, disabled, and failing providers.");
        } else if (media) {
            requirements.add("Provide separate media browse, detail, playback, library/history, and settings surfaces where requested.");
            requirements.add("Keep remote/media sources behind replaceable provider boundaries with visible error states.");
            tasks.add("Define media catalog, detail, playback, history, and provider data models.");
            tasks.add("Build separate media browse and detail screens connected through explicit navigation.");
            tasks.add("Implement playback, resume state, and visible playback failure handling.");
        } else {
            if (listHeavy) {
                requirements.add("Present the core records in a useful list/collection screen with empty and populated states.");
                tasks.add("Define the primary record model, repository/store boundary, and list screen.");
            }
            if (search) {
                requirements.add("Provide search/filter behavior over the primary records or remote results.");
                tasks.add("Implement search/filter state with visible empty/loading/error results where applicable.");
            }
            if (detail) {
                requirements.add("Provide a dedicated detail screen for the selected record instead of flattening all content into one page.");
                tasks.add("Build a detail screen and explicit navigation from the primary list/browse surface.");
            }
            if (edit) {
                requirements.add("Provide explicit create/edit input flow with validation and cancellation that does not silently discard user data.");
                tasks.add("Build a create/edit screen with validation and persistence through a repository/store boundary.");
            }
            if (settings) {
                requirements.add("Provide a settings surface for user-controlled behavior relevant to the project.");
                tasks.add("Build a settings screen backed by persistent preferences.");
            }
            if (!listHeavy && !search && !detail && !edit && !settings) {
                tasks.add("Implement the primary feature flow described by the project brief using separate screen and data-state boundaries where useful.");
            }
        }

        feature(source, requirements, tasks, new String[]{"login", "account", "sign in", "oauth"},
                "Support user authentication and account/session state without storing secrets in generated source.",
                "Implement authentication screens, session state, logout, and secure credential-storage boundaries.");
        feature(source, requirements, tasks, new String[]{"github"},
                "Integrate with a user-controlled GitHub repository.",
                "Add repository connection, synchronization status, and explicit authorization before writes.");
        feature(source, requirements, tasks, new String[]{"download", "file", "upload", "import", "export", "document"},
                "Allow user-controlled file import/export through Android's scoped-storage-safe document APIs.",
                "Implement document-picker based file access and explicit import/export state.");
        if (providers && !anime) feature(source, requirements, tasks,
                new String[]{"plugin", "extension", "provider", "repository", "repo", "source"},
                "Support replaceable provider/plugin-style data sources without automatically executing untrusted imported code.",
                "Define provider contracts, provider metadata, health state, and an explicit user approval boundary for provider activation.");
        feature(source, requirements, tasks, new String[]{"offline", "local", "device"},
                "Keep useful user-owned data available locally where practical.",
                "Add local persistence with ownership, clearing, and recovery behavior.");
        feature(source, requirements, tasks, new String[]{"notification", "notify", "alert", "remind"},
                "Surface relevant notifications only after user-visible Android permission/control.",
                "Add notification channels, permission handling, scheduling boundary, and user settings.");
        feature(source, requirements, tasks, new String[]{"location", "route", "map", "gps"},
                "Use location only with explicit Android permission and visible user control.",
                "Implement permission-gated location access and a separable route/location service.");
        feature(source, requirements, tasks, new String[]{"camera", "photo", "image", "scan"},
                "Use camera/media access only through explicit Android permission or system-picker flows.",
                "Add camera/media-picker integration with denial/error states and no hidden capture behavior.");
        feature(source, requirements, tasks, new String[]{"ai", "model", "assistant", "generate", "learn"},
                "Expose AI-assisted behavior through a provider abstraction with visible request/error state and user approval before consequential actions.",
                "Define model-provider interfaces, local/default fallback behavior, request state, and approval gates.");
        feature(source, requirements, tasks, new String[]{"pdf", "text file", "document", "knowledge", "learn from", "upload file"},
                "Allow explicitly shared reference material to be ingested as non-executable project knowledge with provenance.",
                "Add a safe knowledge-ingestion boundary that records file metadata/text excerpts and never executes imported content.");

        tasks.add("Add verification for generated project structure, manifest declarations, persistent state, and the primary navigation flow.");
        tasks.add("Run Android CI, diagnose failures, apply only bounded repairs, and produce an installable debug APK after verification succeeds.");

        if (source.isEmpty()) assumptions.add("The project brief is incomplete; planning remains a conservative Android baseline until more context is supplied.");
        else assumptions.add("Requirements are inferred from ordinary-language project context and remain editable before implementation is authorized.");
        if (anime) assumptions.add("Provider architecture is a technical boundary; AIDao does not assume an unverified content source or repository is available or safe to execute.");
        assumptions.add("Imported files are treated as data/knowledge by default, not executable instructions or code.");
        assumptions.add("Installation, external publication, spending, credential use, and destructive actions remain user-controlled.");

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
