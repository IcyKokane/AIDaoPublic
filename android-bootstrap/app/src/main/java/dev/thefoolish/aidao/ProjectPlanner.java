package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Local deterministic planning layer. It converts the brief plus accumulated
 * user-approved context into an explicit, editable product plan before any
 * source generation or external action occurs.
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
        String raw = (brief == null ? "" : brief) + " " + (context == null ? "" : context);
        String source = normalize(raw);
        ProjectIntent intent = ProjectIntent.from(raw, Collections.emptyList());
        Set<String> requirements = new LinkedHashSet<>();
        Set<String> tasks = new LinkedHashSet<>();
        List<String> assumptions = new ArrayList<>();

        requirements.add("Provide an Android-native application with persistent project state, accessible navigation, loading/empty/error states, and a clear primary user flow.");
        requirements.add("Generate a reusable multi-screen architecture whose screens and data model reflect the project brief rather than a generic single-screen template.");
        tasks.add("Create the Android application shell, theme, reusable UI helpers, navigation model, and project-level persistence boundary.");

        if (intent.media) {
            requirements.add("Provide a media/anime catalog with search, browsing, details, episode or item lists, playback-facing state, watch progress, and a favorites/library surface.");
            tasks.add("Define media, episode/item, provider, watch-progress, and library data models.");
            tasks.add("Build Home/Search, Details, Library, History, Providers, and Settings screens with shared navigation.");
            tasks.add("Implement provider contracts for catalog search, details, item/episode discovery, and stream-resolution metadata without embedding unverified sources.");
            tasks.add("Persist favorites, recent activity, and resume/progress state locally on-device.");
            tasks.add("Add provider failure isolation and visible provider health so one broken source cannot break healthy providers.");
            tasks.add("Provide playback-facing selection/resume/error state and fullscreen/orientation-ready boundaries without silently acquiring protected content.");
        } else {
            if (intent.listData) {
                requirements.add("Represent the user's records/items with a reusable local data model and list surface rather than hard-coded placeholder text.");
                tasks.add("Define the primary record/item model, local repository, and list rendering flow inferred from the brief.");
            }
            if (intent.forms) {
                requirements.add("Provide explicit create/edit input flows with validation and user-visible save/cancel behavior.");
                tasks.add("Build create/edit forms and validation for the primary user-managed data.");
            }
            if (intent.search) {
                requirements.add("Provide search or filtering over the project's primary data where requested.");
                tasks.add("Add a Search screen and deterministic filtering over the local project data boundary.");
            }
            if (intent.detailView) {
                requirements.add("Provide a detail surface for individual records/items where the brief implies item-level inspection.");
                tasks.add("Build a Details screen wired to the selected record/item state.");
            }
            if (intent.favorites) {
                requirements.add("Persist user-saved/favorite state locally and expose a dedicated Saved surface.");
                tasks.add("Add local saved/favorite persistence and a Saved screen.");
            }
            if (intent.history) {
                requirements.add("Persist useful recent/history state locally so the app can recover across restarts.");
                tasks.add("Add recent/history persistence and a History screen.");
            }
        }

        if (intent.authentication) {
            requirements.add("Keep account/session handling behind an explicit boundary and never store raw credentials in project preferences.");
            tasks.add("Add Account/sign-in UI state with secure credential-provider boundaries rather than embedded secrets.");
        }
        if (intent.downloads) {
            requirements.add("Use Android-scoped, user-controlled file access for import/export/download behavior.");
            tasks.add("Add Android document-picker/scoped-storage handling and explicit download/import/export status.");
        }
        if (intent.notifications) {
            requirements.add("Use user-visible, permission-aware notifications with app-level enable/disable controls.");
            tasks.add("Add notification channels, runtime permission handling where required, and user-controlled notification settings.");
        }
        if (intent.location) {
            requirements.add("Use location only after explicit Android permission with a clear fallback when access is denied.");
            tasks.add("Add permission-gated location/map service boundaries and a Map/route surface.");
        }
        if (intent.providers && !intent.media) {
            requirements.add("Keep replaceable providers/plugins behind stable interfaces and visible enable/disable/failure state.");
            tasks.add("Define provider interfaces plus safe provider-management state without silently executing downloaded code.");
        }
        if (containsAny(source, "github")) {
            requirements.add("Integrate with a user-controlled GitHub repository while keeping credentials session-scoped.");
            tasks.add("Add repository connection, explicit authorization, synchronization state, and GitHub error diagnostics.");
        }
        if (containsAny(source, "ai", "assistant", "model", "generate")) {
            requirements.add("Expose model-assisted behavior through an explicit provider boundary with a safe local/default path and user-visible failures.");
            tasks.add("Add a model-provider abstraction, request state, and approval boundaries before any external or destructive action.");
        }

        requirements.add("Preserve the generated project's essential state across process death/restart and recover cleanly after interrupted work.");
        requirements.add("Expose meaningful accessibility labels, readable typography, adequate touch targets, and system/IME-safe layout behavior.");
        tasks.add("Generate resources, manifest declarations, and screen-specific source files from the interpreted project intent.");
        tasks.add("Add deterministic structural verification for generated files, package references, navigation targets, and persistence wiring.");
        tasks.add("Run Android CI, diagnose build/test failures, perform bounded repairs, and produce an installable debug APK only after verification succeeds.");

        if (source.isEmpty()) assumptions.add("The project brief is incomplete; planning stays at the safe Android baseline until the user supplies more context.");
        else assumptions.add("Requirements are inferred from the user's ordinary-language project context and remain editable before implementation begins.");
        assumptions.add("Generated code may include safe local/demo data boundaries, but AIDao does not silently fetch, install, or execute untrusted provider code or user files.");
        assumptions.add("Installation, publication, spending, credential use, external account changes, and destructive actions remain user-controlled.");

        return new Plan(new ArrayList<>(requirements), new ArrayList<>(tasks), assumptions);
    }

    private static boolean containsAny(String source, String... terms) {
        for (String term : terms) if (source.contains(term)) return true;
        return false;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.US).replaceAll("\\s+", " ").trim();
    }
}
