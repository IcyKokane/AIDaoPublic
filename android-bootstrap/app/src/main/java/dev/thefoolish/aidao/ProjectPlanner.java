package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Local deterministic planner with a reusable structured intent model. */
public final class ProjectPlanner {
    public static final class Plan {
        public final List<String> requirements;
        public final List<String> tasks;
        public final List<String> assumptions;
        Plan(List<String> requirements,List<String> tasks,List<String> assumptions){
            this.requirements=Collections.unmodifiableList(requirements);
            this.tasks=Collections.unmodifiableList(tasks);
            this.assumptions=Collections.unmodifiableList(assumptions);
        }
    }

    private ProjectPlanner() {}

    public static Plan build(String brief,String context){
        ProjectIntent intent=ProjectIntent.parse(brief,context);
        Set<String> requirements=new LinkedHashSet<>();
        Set<String> tasks=new LinkedHashSet<>();
        List<String> assumptions=new ArrayList<>();

        requirements.add("Provide an Android-native application whose screens and data model directly reflect the project brief.");
        requirements.add("Preserve project state locally and recover safely after app restarts or interrupted work.");
        requirements.add("Use explicit navigation between generated screens: "+join(intent.screens)+".");
        tasks.add("Create the Android application shell, reusable theme, navigation host, and persistent project-level state model.");
        tasks.add("Define generated domain models for: "+join(intent.entities)+".");

        if(intent.has("search")){
            requirements.add("Provide search with visible empty/loading/error states and results appropriate to the generated domain.");
            tasks.add("Implement search input, filtering/query state, results rendering, and empty/error states.");
        }
        if(intent.has("detail")){
            requirements.add("Provide a detail screen that exposes domain-specific metadata and available actions.");
            tasks.add("Build a detail screen and navigation contract from list/search results.");
        }
        if(intent.has("forms")){
            requirements.add("Provide validated data-entry forms with clear save/cancel behavior.");
            tasks.add("Build validated create/edit forms and persist accepted values locally.");
        }
        if(intent.has("favorites")){
            requirements.add("Allow users to add/remove domain items from a persistent favorites or library collection.");
            tasks.add("Implement local favorites/library persistence and surface it in navigation.");
        }
        if(intent.has("history")){
            requirements.add("Persist recent/history state so users can resume previous activity after restart.");
            tasks.add("Implement local history/recent-state persistence and recovery.");
        }
        if(intent.has("playback")){
            requirements.add("Provide explicit media playback state, resume position, errors, orientation/fullscreen behavior, and source attribution.");
            tasks.add("Implement the player surface, resume position storage, and visible playback failure handling.");
        }
        if(intent.has("providers")){
            requirements.add("Keep external repositories/providers behind replaceable interfaces so one failing source cannot break healthy sources.");
            requirements.add("Show installed/enabled/disabled/failing provider state and never silently execute untrusted provider content.");
            tasks.add("Define provider contracts, health/error isolation, and a provider-management screen.");
        }
        if(intent.has("files")){
            requirements.add("Allow user-controlled import/export through Android document APIs without silently executing imported content.");
            tasks.add("Implement scoped-storage-safe document picking, inspection, and explicit import/export actions.");
        }
        if(intent.has("notifications")){
            requirements.add("Use notifications only with visible permission handling and user-controlled settings.");
            tasks.add("Add notification channels, permission flow, and user-visible controls.");
        }
        if(intent.has("location")){
            requirements.add("Use location only after explicit Android permission with visible current-state controls.");
            tasks.add("Implement permission-gated location access behind a separable service boundary.");
        }
        if(intent.has("authentication")){
            requirements.add("Support account/session state without embedding credentials in generated source.");
            tasks.add("Build authentication/session surfaces and secure credential-boundary placeholders.");
        }
        if(intent.has("model-provider")){
            requirements.add("Expose AI/model behavior through a provider abstraction with a safe local/default path and explicit approval before consequential actions.");
            tasks.add("Define model-provider interfaces, request/error state, and approval boundaries.");
        }

        for(String screen:intent.screens) tasks.add("Build the "+screen+" screen using reusable generated UI/data components.");
        tasks.add("Generate Android resources, manifest declarations, and navigation/data wiring required by the inferred capabilities.");
        tasks.add("Add deterministic local verification for required files, navigation targets, persistence hooks, and the primary user flow.");
        tasks.add("Run Android CI; diagnose and apply only bounded repairs; produce an installable debug APK only after verification succeeds.");

        assumptions.add("Requirements are inferred from ordinary-language project context and remain editable before implementation.");
        assumptions.add("Imported knowledge or provider material is treated as data for inspection; it is not silently executed.");
        assumptions.add("Installation, external publishing, spending, credential use, provider acquisition, and destructive actions remain user-controlled.");
        if(intent.source.isEmpty()) assumptions.add("The brief is incomplete, so AIDao generated a safe multi-screen Android baseline only.");
        return new Plan(new ArrayList<>(requirements),new ArrayList<>(tasks),assumptions);
    }

    private static String join(List<String> values){
        StringBuilder b=new StringBuilder();
        for(int i=0;i<values.size();i++){if(i>0)b.append(i==values.size()-1?" and ":", ");b.append(values.get(i));}
        return b.toString();
    }
}
