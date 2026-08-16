package dev.thefoolish.aidao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic bookkeeping for generated-source revisions and user overrides.
 *
 * The ledger never executes source text. It records fingerprints so a later
 * regeneration can distinguish untouched generated files from explicit user
 * edits and can surface stale/conflicting overrides instead of silently
 * discarding them.
 */
final class ProjectRevisionLedger {
    enum OverrideState { NONE, APPLIES_CLEANLY, STALE_BASELINE, ORPHANED }

    static final class Entry {
        final String path;
        final String generatedHash;
        final String overrideBaseHash;
        final String overrideHash;
        final OverrideState overrideState;

        Entry(String path, String generatedHash, String overrideBaseHash, String overrideHash, OverrideState overrideState) {
            this.path = path;
            this.generatedHash = generatedHash;
            this.overrideBaseHash = overrideBaseHash;
            this.overrideHash = overrideHash;
            this.overrideState = overrideState;
        }
    }

    static final class Snapshot {
        final List<Entry> entries;
        final int cleanOverrides;
        final int staleOverrides;
        final int orphanedOverrides;

        Snapshot(List<Entry> entries, int cleanOverrides, int staleOverrides, int orphanedOverrides) {
            this.entries = Collections.unmodifiableList(entries);
            this.cleanOverrides = cleanOverrides;
            this.staleOverrides = staleOverrides;
            this.orphanedOverrides = orphanedOverrides;
        }

        boolean hasConflicts() { return staleOverrides > 0 || orphanedOverrides > 0; }
    }

    Snapshot inspect(GeneratedProject generated, Map<String,String> overrides, Map<String,String> overrideBaseHashes) {
        if (generated == null) throw new IllegalArgumentException("generated project required");
        Map<String,String> safeOverrides = overrides == null ? Collections.emptyMap() : overrides;
        Map<String,String> safeBases = overrideBaseHashes == null ? Collections.emptyMap() : overrideBaseHashes;
        Map<String,GeneratedProject.FileEntry> generatedByPath = new LinkedHashMap<>();
        for (GeneratedProject.FileEntry file : generated.files) {
            if (file != null && file.path != null) generatedByPath.put(file.path, file);
        }

        List<Entry> entries = new ArrayList<>();
        int clean = 0, stale = 0, orphaned = 0;
        for (GeneratedProject.FileEntry file : generated.files) {
            if (file == null || file.path == null) continue;
            String generatedHash = hash(file.content);
            if (!safeOverrides.containsKey(file.path)) {
                entries.add(new Entry(file.path, generatedHash, null, null, OverrideState.NONE));
                continue;
            }
            String override = safeOverrides.get(file.path);
            String base = safeBases.get(file.path);
            OverrideState state;
            if (base == null || base.isEmpty() || base.equals(generatedHash)) {
                state = OverrideState.APPLIES_CLEANLY;
                clean++;
            } else {
                state = OverrideState.STALE_BASELINE;
                stale++;
            }
            entries.add(new Entry(file.path, generatedHash, base, hash(override), state));
        }

        for (Map.Entry<String,String> override : safeOverrides.entrySet()) {
            if (generatedByPath.containsKey(override.getKey())) continue;
            orphaned++;
            entries.add(new Entry(override.getKey(), null, safeBases.get(override.getKey()), hash(override.getValue()), OverrideState.ORPHANED));
        }
        return new Snapshot(entries, clean, stale, orphaned);
    }

    static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) out.append(String.format("%02x", b & 0xff));
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
