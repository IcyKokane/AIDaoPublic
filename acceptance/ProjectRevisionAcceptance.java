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
        overrides.put(target, original.content.replace("</resources>", "  <string name=\"custom_label\">User edit</string></resources>"));
        bases.put(target, ProjectRevisionLedger.hash(original.content));

        ProjectRevisionLedger ledger = new ProjectRevisionLedger();
        ProjectRevisionLedger.Snapshot clean = ledger.inspect(first, overrides, bases);
        require(clean.cleanOverrides == 1, "expected one clean override");
        require(!clean.hasConflicts(), "clean override unexpectedly conflicted");

        GeneratedProject second = new LocalSourceGenerator().generate(
                "Revision Ledger Acceptance",
                "Create a simple Android inventory utility with explore, detail, settings, and reporting screens.",
                Collections.singletonList("Persist local inventory state and include reporting."),
                Collections.singletonList("Generate and verify source."));
        Map<String,String> staleBases = new HashMap<>(bases);
        staleBases.put(target, "0000000000000000000000000000000000000000000000000000000000000000");
        ProjectRevisionLedger.Snapshot stale = ledger.inspect(second, overrides, staleBases);
        require(stale.staleOverrides == 1, "expected stale override detection");
        require(stale.hasConflicts(), "stale override must block silent application");

        overrides.put("app/src/main/java/dev/thefoolish/generated/revisionledgeracceptance/Removed.java", "package example;");
        ProjectRevisionLedger.Snapshot orphan = ledger.inspect(second, overrides, staleBases);
        require(orphan.orphanedOverrides == 1, "expected orphaned override detection");
        require(orphan.hasConflicts(), "orphaned override must block silent loss");

        System.out.println("Project revision acceptance passed: clean/stale/orphaned override states are deterministic.");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
