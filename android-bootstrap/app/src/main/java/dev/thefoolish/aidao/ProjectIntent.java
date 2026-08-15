package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic, local interpretation of a user's project brief.
 *
 * It deliberately does not execute code, fetch providers, use credentials, or
 * make network calls. It turns ordinary-language intent into reusable feature
 * and screen hints for both planning and source generation.
 */
final class ProjectIntent {
    final String normalized;
    final boolean media;
    final boolean authentication;
    final boolean search;
    final boolean favorites;
    final boolean history;
    final boolean downloads;
    final boolean providers;
    final boolean notifications;
    final boolean location;
    final boolean forms;
    final boolean listData;
    final boolean detailView;
    final List<String> screens;

    private ProjectIntent(String normalized, boolean media, boolean authentication,
                          boolean search, boolean favorites, boolean history,
                          boolean downloads, boolean providers, boolean notifications,
                          boolean location, boolean forms, boolean listData,
                          boolean detailView, List<String> screens) {
        this.normalized = normalized;
        this.media = media;
        this.authentication = authentication;
        this.search = search;
        this.favorites = favorites;
        this.history = history;
        this.downloads = downloads;
        this.providers = providers;
        this.notifications = notifications;
        this.location = location;
        this.forms = forms;
        this.listData = listData;
        this.detailView = detailView;
        this.screens = Collections.unmodifiableList(screens);
    }

    static ProjectIntent from(String brief, List<String> requirements) {
        StringBuilder source = new StringBuilder(brief == null ? "" : brief);
        if (requirements != null) for (String r : requirements) source.append(' ').append(r == null ? "" : r);
        String s = source.toString().toLowerCase(Locale.US).replaceAll("\\s+", " ").trim();

        boolean media = any(s, "anime", "episode", "video", "stream", "playback", "movie", "show");
        boolean authentication = any(s, "login", "sign in", "account", "oauth", "profile");
        boolean search = media || any(s, "search", "find", "browse", "catalog");
        boolean favorites = media || any(s, "favorite", "favourite", "bookmark", "saved", "library");
        boolean history = media || any(s, "history", "recent", "progress", "resume", "last watched");
        boolean downloads = any(s, "download", "offline", "export", "save file");
        boolean providers = media || any(s, "provider", "extension", "plugin", "repository", "source");
        boolean notifications = any(s, "notification", "notify", "alert", "reminder");
        boolean location = any(s, "location", "map", "route", "gps", "nearby");
        boolean forms = any(s, "form", "enter", "create", "add", "edit", "expense", "task", "note", "profile");
        boolean listData = media || any(s, "list", "tasks", "notes", "expenses", "transactions", "items", "records", "entries", "catalog");
        boolean detailView = media || any(s, "detail", "details", "profile", "record", "item", "episode");

        Set<String> screens = new LinkedHashSet<>();
        if (authentication) screens.add("Account");
        if (media) {
            screens.add("Home");
            screens.add("Search");
            screens.add("Details");
            screens.add("Library");
            screens.add("History");
            if (providers) screens.add("Providers");
            screens.add("Settings");
        } else {
            screens.add("Home");
            if (search) screens.add("Search");
            if (listData) screens.add("Items");
            if (detailView) screens.add("Details");
            if (favorites) screens.add("Saved");
            if (history) screens.add("History");
            if (location) screens.add("Map");
            screens.add("Settings");
        }

        return new ProjectIntent(s, media, authentication, search, favorites, history,
                downloads, providers, notifications, location, forms, listData,
                detailView, new ArrayList<>(screens));
    }

    private static boolean any(String source, String... terms) {
        for (String term : terms) if (source.contains(term)) return true;
        return false;
    }
}
