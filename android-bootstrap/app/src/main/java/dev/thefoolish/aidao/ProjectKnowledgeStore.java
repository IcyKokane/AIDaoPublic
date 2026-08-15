package dev.thefoolish.aidao;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores explicitly shared project reference material as non-executable knowledge.
 * This class never evaluates code, shells, scripts, APKs, macros, or embedded instructions.
 * Callers must obtain content through a user-controlled Android picker/share action.
 */
final class ProjectKnowledgeStore {
    static final int MAX_ITEM_CHARS = 12000;
    static final int MAX_ITEMS = 24;

    static final class Item {
        final String id;
        final String displayName;
        final String mimeType;
        final String excerpt;
        final long addedAt;
        final String sha256;

        Item(String id, String displayName, String mimeType, String excerpt, long addedAt, String sha256) {
            this.id = id;
            this.displayName = displayName;
            this.mimeType = mimeType;
            this.excerpt = excerpt;
            this.addedAt = addedAt;
            this.sha256 = sha256;
        }
    }

    private final SharedPreferences prefs;

    ProjectKnowledgeStore(Context context) {
        prefs = context.getSharedPreferences("aidao_project_knowledge_v1", Context.MODE_PRIVATE);
    }

    Item addText(String projectKey, String displayName, String mimeType, String text) {
        String safeProject = clean(projectKey, 120);
        String safeName = clean(displayName, 160);
        String safeMime = clean(mimeType, 120);
        String safeText = sanitizeText(text);
        String hash = sha256(safeText);
        String id = Long.toHexString(System.currentTimeMillis()) + "-" + hash.substring(0, Math.min(12, hash.length()));
        String prefix = key(safeProject) + ".item." + id + ".";
        long now = System.currentTimeMillis();

        List<String> ids = ids(safeProject);
        ids.remove(id);
        ids.add(0, id);
        while (ids.size() > MAX_ITEMS) {
            String removed = ids.remove(ids.size() - 1);
            removeItem(safeProject, removed);
        }

        prefs.edit()
                .putString(prefix + "name", safeName)
                .putString(prefix + "mime", safeMime)
                .putString(prefix + "text", safeText)
                .putString(prefix + "sha256", hash)
                .putLong(prefix + "added", now)
                .putString(key(safeProject) + ".ids", join(ids))
                .apply();
        return new Item(id, safeName, safeMime, safeText, now, hash);
    }

    List<Item> list(String projectKey) {
        String project = clean(projectKey, 120);
        List<Item> out = new ArrayList<>();
        for (String id : ids(project)) {
            String prefix = key(project) + ".item." + id + ".";
            out.add(new Item(
                    id,
                    prefs.getString(prefix + "name", "Reference"),
                    prefs.getString(prefix + "mime", "text/plain"),
                    prefs.getString(prefix + "text", ""),
                    prefs.getLong(prefix + "added", 0L),
                    prefs.getString(prefix + "sha256", "")
            ));
        }
        return Collections.unmodifiableList(out);
    }

    String planningContext(String projectKey) {
        StringBuilder out = new StringBuilder();
        for (Item item : list(projectKey)) {
            if (out.length() > 0) out.append("\n\n");
            out.append("Reference: ").append(item.displayName)
                    .append(" [").append(item.mimeType).append("]\n")
                    .append(item.excerpt);
        }
        return out.toString();
    }

    void clearProject(String projectKey) {
        String project = clean(projectKey, 120);
        for (String id : ids(project)) removeItem(project, id);
        prefs.edit().remove(key(project) + ".ids").apply();
    }

    private void removeItem(String project, String id) {
        String prefix = key(project) + ".item." + id + ".";
        prefs.edit()
                .remove(prefix + "name")
                .remove(prefix + "mime")
                .remove(prefix + "text")
                .remove(prefix + "sha256")
                .remove(prefix + "added")
                .apply();
    }

    private List<String> ids(String project) {
        String raw = prefs.getString(key(project) + ".ids", "");
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String item : raw.split("\\|")) if (!item.trim().isEmpty()) out.add(item.trim());
        return out;
    }

    private String join(List<String> ids) {
        StringBuilder b = new StringBuilder();
        for (String id : ids) { if (b.length() > 0) b.append('|'); b.append(id); }
        return b.toString();
    }

    private String key(String project) {
        return Integer.toHexString(project == null ? 0 : project.hashCode());
    }

    private String sanitizeText(String value) {
        String v = value == null ? "" : value.replace('\u0000', ' ').trim();
        if (v.length() > MAX_ITEM_CHARS) v = v.substring(0, MAX_ITEM_CHARS);
        return v;
    }

    private String clean(String value, int max) {
        String v = value == null ? "" : value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return v.substring(0, Math.min(v.length(), max));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode()) + "000000000000";
        }
    }
}
