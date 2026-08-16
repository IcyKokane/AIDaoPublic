package dev.thefoolish.aidao;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Explicit, user-directed conflict actions for regenerated source overrides.
 *
 * This class never guesses how to resolve a stale edit. A stale override can be
 * deliberately rebased onto the current deterministic generation baseline, or
 * discarded in favor of the regenerated file. Orphaned overrides can only be
 * dropped here because silently creating a new generated file would change the
 * project structure without a fresh planning/generation decision.
 */
final class GeneratedOverrideConflictActions {
    enum Decision {
        KEEP_GENERATED,
        KEEP_USER_OVERRIDE
    }

    static final class Result {
        final Map<String,String> overrides;
        final Map<String,String> overrideBaseHashes;
        final GeneratedProjectOverrideResolver.Resolution resolution;
        final String actionSummary;

        Result(Map<String,String> overrides,
               Map<String,String> overrideBaseHashes,
               GeneratedProjectOverrideResolver.Resolution resolution,
               String actionSummary) {
            this.overrides = Collections.unmodifiableMap(new LinkedHashMap<>(overrides));
            this.overrideBaseHashes = Collections.unmodifiableMap(new LinkedHashMap<>(overrideBaseHashes));
            this.resolution = resolution;
            this.actionSummary = actionSummary;
        }
    }

    Result resolve(GeneratedProject generated,
                   Map<String,String> overrides,
                   Map<String,String> overrideBaseHashes,
                   String path,
                   Decision decision) {
        if (generated == null) throw new IllegalArgumentException("generated project required");
        if (path == null || path.trim().isEmpty()) throw new IllegalArgumentException("conflict path required");
        if (decision == null) throw new IllegalArgumentException("explicit decision required");

        Map<String,String> nextOverrides = new LinkedHashMap<>();
        if (overrides != null) nextOverrides.putAll(overrides);
        Map<String,String> nextBases = new LinkedHashMap<>();
        if (overrideBaseHashes != null) nextBases.putAll(overrideBaseHashes);

        if (!nextOverrides.containsKey(path)) {
            throw new IllegalStateException("No user override exists for conflict path: " + path);
        }

        ProjectRevisionLedger.Snapshot snapshot = new ProjectRevisionLedger().inspect(generated, nextOverrides, nextBases);
        ProjectRevisionLedger.Entry target = null;
        for (ProjectRevisionLedger.Entry entry : snapshot.entries) {
            if (path.equals(entry.path)) {
                target = entry;
                break;
            }
        }
        if (target == null) throw new IllegalStateException("Revision ledger did not contain conflict path: " + path);
        if (target.overrideState != ProjectRevisionLedger.OverrideState.STALE_BASELINE
                && target.overrideState != ProjectRevisionLedger.OverrideState.ORPHANED) {
            throw new IllegalStateException("Override is not in a conflict state: " + target.overrideState);
        }

        String summary;
        if (decision == Decision.KEEP_GENERATED) {
            nextOverrides.remove(path);
            nextBases.remove(path);
            summary = "Kept regenerated source and removed the conflicting local override for " + path + ".";
        } else {
            if (target.overrideState == ProjectRevisionLedger.OverrideState.ORPHANED) {
                throw new IllegalStateException(
                        "Orphaned override cannot be silently reintroduced as generated source. " +
                        "Keep the regenerated project or add the file again through an explicit edit/generation step.");
            }
            GeneratedProject.FileEntry current = generated.find(path);
            if (current == null) throw new IllegalStateException("Generated file disappeared while resolving conflict: " + path);
            nextBases.put(path, ProjectRevisionLedger.hash(current.content));
            summary = "Kept the user override and explicitly rebased it onto the current generated baseline for " + path + ".";
        }

        GeneratedProjectOverrideResolver.Resolution resolution =
                new GeneratedProjectOverrideResolver().resolve(generated, nextOverrides, nextBases);
        return new Result(nextOverrides, nextBases, resolution, summary);
    }
}
