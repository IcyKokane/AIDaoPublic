package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies user-controlled generated-source overrides without silently overwriting
 * a newer deterministic generation baseline.
 *
 * The resolver never executes source text. A clean override is applied only when
 * its recorded baseline hash still matches the freshly generated file. Stale or
 * orphaned overrides are preserved as explicit conflicts for the UI/user to
 * resolve before a GitHub build can proceed.
 */
final class GeneratedProjectOverrideResolver {
    static final class Conflict {
        final String path;
        final ProjectRevisionLedger.OverrideState state;
        final String generatedHash;
        final String overrideBaseHash;
        final String overrideHash;

        Conflict(ProjectRevisionLedger.Entry entry) {
            this.path = entry.path;
            this.state = entry.overrideState;
            this.generatedHash = entry.generatedHash;
            this.overrideBaseHash = entry.overrideBaseHash;
            this.overrideHash = entry.overrideHash;
        }
    }

    static final class Resolution {
        final GeneratedProject project;
        final List<Conflict> conflicts;
        final int appliedOverrides;

        Resolution(GeneratedProject project, List<Conflict> conflicts, int appliedOverrides) {
            this.project = project;
            this.conflicts = Collections.unmodifiableList(new ArrayList<>(conflicts));
            this.appliedOverrides = appliedOverrides;
        }

        boolean canBuild() { return conflicts.isEmpty(); }
    }

    Resolution resolve(GeneratedProject generated,
                       Map<String,String> overrides,
                       Map<String,String> overrideBaseHashes) {
        if (generated == null) throw new IllegalArgumentException("generated project required");
        Map<String,String> safeOverrides = overrides == null ? Collections.emptyMap() : overrides;
        Map<String,String> safeBases = overrideBaseHashes == null ? Collections.emptyMap() : overrideBaseHashes;

        ProjectRevisionLedger.Snapshot snapshot = new ProjectRevisionLedger().inspect(generated, safeOverrides, safeBases);
        Map<String,ProjectRevisionLedger.Entry> byPath = new LinkedHashMap<>();
        List<Conflict> conflicts = new ArrayList<>();
        for (ProjectRevisionLedger.Entry entry : snapshot.entries) {
            byPath.put(entry.path, entry);
            if (entry.overrideState == ProjectRevisionLedger.OverrideState.STALE_BASELINE
                    || entry.overrideState == ProjectRevisionLedger.OverrideState.ORPHANED) {
                conflicts.add(new Conflict(entry));
            }
        }

        List<GeneratedProject.FileEntry> resolved = new ArrayList<>();
        int applied = 0;
        for (GeneratedProject.FileEntry file : generated.files) {
            ProjectRevisionLedger.Entry entry = byPath.get(file.path);
            String content = file.content;
            if (entry != null && entry.overrideState == ProjectRevisionLedger.OverrideState.APPLIES_CLEANLY) {
                String override = safeOverrides.get(file.path);
                if (override != null) {
                    content = override;
                    applied++;
                }
            }
            resolved.add(new GeneratedProject.FileEntry(file.path, content, file.taskHint));
        }

        List<String> notes = new ArrayList<>(generated.verificationNotes);
        if (applied > 0) {
            notes.add("PASS " + applied + " user override(s) matched their generation baseline and were applied");
        }
        if (conflicts.isEmpty()) {
            notes.add("PASS no stale or orphaned user source overrides");
        } else {
            for (Conflict conflict : conflicts) {
                notes.add("FAIL source override requires user resolution: " + conflict.path + " (" + conflict.state + ")");
            }
        }

        GeneratedProject project = GeneratedProject.resolved(
                generated.projectName,
                generated.packageName,
                resolved,
                notes);
        return new Resolution(project, conflicts, applied);
    }

    static Map<String,String> captureBaselineHashes(GeneratedProject generated, Map<String,String> overrides) {
        if (generated == null) throw new IllegalArgumentException("generated project required");
        Map<String,String> result = new LinkedHashMap<>();
        if (overrides == null || overrides.isEmpty()) return result;
        for (GeneratedProject.FileEntry file : generated.files) {
            if (overrides.containsKey(file.path)) {
                result.put(file.path, ProjectRevisionLedger.hash(file.content));
            }
        }
        return result;
    }
}
