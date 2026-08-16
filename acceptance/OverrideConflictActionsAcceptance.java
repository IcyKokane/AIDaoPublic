package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CI acceptance for explicit stale/orphaned generated-source conflict actions. */
public final class OverrideConflictActionsAcceptance {
    public static void main(String[] args) {
        GeneratedProject first = generate();
        String target = "app/src/main/res/values/strings.xml";
        GeneratedProject.FileEntry original = first.find(target);
        require(original != null, "missing strings.xml");

        String userEdit = original.content.replace("</resources>", "  <string name=\"manual_label\">Manual user value</string>\n</resources>");
        Map<String,String> overrides = new LinkedHashMap<>();
        Map<String,String> bases = new LinkedHashMap<>();
        overrides.put(target, userEdit);
        bases.put(target, ProjectRevisionLedger.hash(original.content));

        // Simulate a deterministic generator evolution that changes the same file.
        // The acceptance must not depend on a particular planner phrase affecting
        // strings.xml, only on the revision-safety contract itself.
        String evolved = original.content.replace("</resources>", "  <string name=\"generated_revision\">Generator vNext</string>\n</resources>");
        GeneratedProject changed = replace(first, target, evolved);
        GeneratedProjectOverrideResolver.Resolution stale =
                new GeneratedProjectOverrideResolver().resolve(changed, overrides, bases);
        require(!stale.canBuild(), "changed generation baseline must create an explicit stale conflict");

        GeneratedOverrideConflictActions actions = new GeneratedOverrideConflictActions();

        GeneratedOverrideConflictActions.Result keepUser = actions.resolve(
                changed, overrides, bases, target,
                GeneratedOverrideConflictActions.Decision.KEEP_USER_OVERRIDE);
        require(keepUser.resolution.canBuild(), "explicit rebase should make stale override buildable");
        require(userEdit.equals(keepUser.resolution.project.find(target).content), "rebased user override was not applied");
        require(ProjectRevisionLedger.hash(changed.find(target).content).equals(keepUser.overrideBaseHashes.get(target)),
                "rebase did not capture the current generated baseline hash");

        GeneratedOverrideConflictActions.Result keepGenerated = actions.resolve(
                changed, overrides, bases, target,
                GeneratedOverrideConflictActions.Decision.KEEP_GENERATED);
        require(keepGenerated.resolution.canBuild(), "discarding stale override should make generated project buildable");
        require(!keepGenerated.overrides.containsKey(target), "discarded override remained stored");
        require(changed.find(target).content.equals(keepGenerated.resolution.project.find(target).content),
                "generated source changed after KEEP_GENERATED");

        String orphan = "app/src/main/java/dev/thefoolish/generated/conflict/Removed.java";
        Map<String,String> orphanOverrides = new LinkedHashMap<>(overrides);
        Map<String,String> orphanBases = new LinkedHashMap<>(bases);
        orphanOverrides.put(orphan, "package dev.thefoolish.generated.conflict; final class Removed {}\n");
        orphanBases.put(orphan, ProjectRevisionLedger.hash("old generated file"));

        GeneratedOverrideConflictActions.Result dropOrphan = actions.resolve(
                changed, orphanOverrides, orphanBases, orphan,
                GeneratedOverrideConflictActions.Decision.KEEP_GENERATED);
        require(!dropOrphan.overrides.containsKey(orphan), "orphan override was not removed");

        boolean blockedReintroduction = false;
        try {
            actions.resolve(changed, orphanOverrides, orphanBases, orphan,
                    GeneratedOverrideConflictActions.Decision.KEEP_USER_OVERRIDE);
        } catch (IllegalStateException expected) {
            blockedReintroduction = expected.getMessage().contains("Orphaned override");
        }
        require(blockedReintroduction, "orphaned source was allowed to reappear silently");

        System.out.println("Override conflict actions acceptance passed: stale edits require explicit rebase/discard and orphaned files cannot silently return.");
    }

    private static GeneratedProject generate() {
        return new LocalSourceGenerator().generate(
                "Conflict Safety App",
                "Create a small Android inventory utility with explore, detail, and settings screens.",
                Arrays.asList(
                        "Generate connected Android screens with persistent local state.",
                        "Keep generated source deterministic across identical inputs."),
                Arrays.asList(
                        "Generate the Android source tree.",
                        "Verify the generated project."));
    }

    private static GeneratedProject replace(GeneratedProject project, String path, String content) {
        List<GeneratedProject.FileEntry> files = new ArrayList<>();
        for (GeneratedProject.FileEntry file : project.files) {
            files.add(path.equals(file.path)
                    ? new GeneratedProject.FileEntry(file.path, content, file.taskHint)
                    : file);
        }
        return new GeneratedProject(project.projectName, project.packageName, files, project.verificationNotes);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
