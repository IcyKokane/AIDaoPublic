package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic pre-generation capability research for provider-backed projects.
 *
 * V1 deliberately uses a small reviewed registry rather than executing arbitrary
 * extension code or scraping unreviewed repositories. Entries record provenance,
 * license posture, API scope, and whether the provider is safe to bundle.
 */
final class ProviderCapabilityResearch {
    static final class Candidate {
        final String id;
        final String name;
        final String sourceUrl;
        final String version;
        final String license;
        final String capability;
        final String endpoint;
        final boolean bundleSafe;
        final String limitation;

        Candidate(String id, String name, String sourceUrl, String version, String license,
                  String capability, String endpoint, boolean bundleSafe, String limitation) {
            this.id = id;
            this.name = name;
            this.sourceUrl = sourceUrl;
            this.version = version;
            this.license = license;
            this.capability = capability;
            this.endpoint = endpoint;
            this.bundleSafe = bundleSafe;
            this.limitation = limitation;
        }
    }

    static List<Candidate> search(String projectName, List<GeneratedProject.FileEntry> files) {
        StringBuilder signal = new StringBuilder(projectName == null ? "" : projectName);
        if (files != null) {
            for (GeneratedProject.FileEntry file : files) {
                if (file == null) continue;
                signal.append(' ').append(file.path == null ? "" : file.path);
                if (file.taskHint != null) signal.append(' ').append(file.taskHint);
            }
        }
        String low = signal.toString().toLowerCase(Locale.US);
        boolean animeMedia = low.contains("anime") || low.contains("mediaprovider") ||
                low.contains("animeitem") || low.contains("mihon");
        if (!animeMedia) return Collections.emptyList();

        List<Candidate> out = new ArrayList<>();
        // Jikan is an open-source REST API and exposes a public, unauthenticated v4 service.
        // It supplies catalog metadata only; it is never represented as a streaming source.
        out.add(new Candidate(
                "builtin.jikan.catalog",
                "Jikan Catalog",
                "https://github.com/jikan-me/jikan-rest",
                "v4",
                "MIT",
                "anime-catalog-search",
                "https://api.jikan.moe/v4/anime?q={query}&limit=20",
                true,
                "Catalog metadata only; no episode video or download URLs are provided."
        ));
        return out;
    }

    private ProviderCapabilityResearch() {}
}
