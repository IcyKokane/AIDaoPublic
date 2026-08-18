package dev.thefoolish.aidao;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** CI acceptance for non-destructive generated-source revision bookkeeping. */
public final class ProjectRevisionAcceptance {
    public static void main(String[] args) {
        GeneratedProject first = new LocalSourceGenerator().generate(
                "Revision Ledger Acceptance",
                "Create a simple Android inventory utility with explore, detail, and settings screens.",
                Collections.singletonList("Persist local inventory state."),
                Collections.singletonList("Generate and verify source."));

        String target = "app/src/main/res/values/strings.xml";
        GeneratedProject.FileEntry original = first.find(target);
        if (original == null) throw new IllegalStateException("missing acceptance target");

        Map<String,String> overrides = new HashMap<>();
        Map<String,String> bases = new HashMap<>();
        String userEdit = original.content.replace("</resources>", "  <string name=\"custom_label\">User edit</string></resources>");
        overrides.put(target, userEdit);
        bases.put(target, ProjectRevisionLedger.hash(original.content));

        ProjectRevisionLedger ledger = new ProjectRevisionLedger();
        ProjectRevisionLedger.Snapshot clean = ledger.inspect(first, overrides, bases);
        require(clean.cleanOverrides == 1, "expected one clean override");
        require(!clean.hasConflicts(), "clean override unexpectedly conflicted");

        GeneratedProjectOverrideResolver resolver = new GeneratedProjectOverrideResolver();
        GeneratedProjectOverrideResolver.Resolution cleanResolution = resolver.resolve(first, overrides, bases);
        require(cleanResolution.canBuild(), "clean override should be buildable");
        require(cleanResolution.appliedOverrides == 1, "clean override was not applied");
        require(userEdit.equals(cleanResolution.project.find(target).content), "resolved project does not contain user edit");

        Map<String,String> captured = GeneratedProjectOverrideResolver.captureBaselineHashes(first, overrides);
        require(ProjectRevisionLedger.hash(original.content).equals(captured.get(target)), "baseline capture did not fingerprint generated source");

        GeneratedProject second = new LocalSourceGenerator().generate(
                "Revision Ledger Acceptance",
                "Create a simple Android inventory utility with explore, detail, settings, and reporting screens.",
                Collections.singletonList("Persist local inventory state and include reporting."),
                Collections.singletonList("Generate and verify source."));

        // A refinement that changes unrelated generated source must not destroy a clean user edit.
        GeneratedProject.FileEntry refinedTarget = second.find(target);
        require(refinedTarget != null, "refinement removed unrelated strings resource");
        require(ProjectRevisionLedger.hash(original.content).equals(ProjectRevisionLedger.hash(refinedTarget.content)),
                "acceptance target unexpectedly changed during unrelated refinement");
        ProjectRevisionLedger.Snapshot refinedClean = ledger.inspect(second, overrides, bases);
        require(refinedClean.cleanOverrides == 1 && !refinedClean.hasConflicts(),
                "unrelated refinement incorrectly invalidated a clean user edit");
        GeneratedProjectOverrideResolver.Resolution refinedResolution = resolver.resolve(second, overrides, bases);
        require(refinedResolution.canBuild(), "unrelated refinement should remain buildable with clean edit");
        require(refinedResolution.appliedOverrides == 1, "clean user edit was lost during refinement");
        require(userEdit.equals(refinedResolution.project.find(target).content),
                "unrelated refinement did not preserve the user edit");

        Map<String,String> staleBases = new HashMap<>(bases);
        staleBases.put(target, "0000000000000000000000000000000000000000000000000000000000000000");
        ProjectRevisionLedger.Snapshot stale = ledger.inspect(second, overrides, staleBases);
        require(stale.staleOverrides == 1, "expected stale override detection");
        require(stale.hasConflicts(), "stale override must block silent application");

        GeneratedProjectOverrideResolver.Resolution staleResolution = resolver.resolve(second, overrides, staleBases);
        require(!staleResolution.canBuild(), "stale override must block a build until user resolution");
        require(staleResolution.appliedOverrides == 0, "stale override must not be silently applied");
        require(!userEdit.equals(staleResolution.project.find(target).content), "stale override leaked into regenerated source");

        String orphanPath = "app/src/main/java/dev/thefoolish/generated/revisionledgeracceptance/Removed.java";
        overrides.put(orphanPath, "package example;");
        ProjectRevisionLedger.Snapshot orphan = ledger.inspect(second, overrides, staleBases);
        require(orphan.orphanedOverrides == 1, "expected orphaned override detection");
        require(orphan.hasConflicts(), "orphaned override must block silent loss");

        GeneratedProjectOverrideResolver.Resolution orphanResolution = resolver.resolve(second, overrides, staleBases);
        require(!orphanResolution.canBuild(), "orphaned override must block a build until user resolution");
        boolean orphanSeen = false;
        for (GeneratedProjectOverrideResolver.Conflict conflict : orphanResolution.conflicts) {
            if (orphanPath.equals(conflict.path) && conflict.state == ProjectRevisionLedger.OverrideState.ORPHANED) orphanSeen = true;
        }
        require(orphanSeen, "orphaned override was not surfaced in resolver conflicts");

        System.out.println("Project revision acceptance passed: clean edits survive unrelated refinement; stale/orphaned edits block silent regeneration.");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
