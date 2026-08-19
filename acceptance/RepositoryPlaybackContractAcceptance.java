package dev.thefoolish.aidao;

import java.util.Arrays;

/**
 * Guards the V1 real-device playback gap by requiring a complete declarative
 * repository-provider path from metadata parsing through HTTPS media handoff.
 * This does not execute arbitrary extension APKs or contact third-party hosts.
 */
public final class RepositoryPlaybackContractAcceptance {
    public static void main(String[] args) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Anime Shelf",
                "Create an anime app like Mihon with repository-based providers, search, favorites, playback and resume progress.",
                Arrays.asList(
                        "Allow user-managed HTTPS extension repositories.",
                        "Parse compatible search and playback/media URL contracts from repository metadata.",
                        "Only expose playback when a provider declares a compatible playback contract.",
                        "Resolve provider-declared HTTPS media URLs without fabricating streams.",
                        "Open resolved media in a real Android playback surface and persist resume position."
                ),
                Arrays.asList("Generate provider contracts", "Generate repository parser", "Generate playback resolver", "Validate media handoff")
        );
        for (String note : project.verificationNotes) {
            if (note.startsWith("FAIL ")) throw new IllegalStateException(note);
        }

        String record = content(project, "/ExtensionRecord.java");
        require(record, "playbackUrl", "persisted playback URL contract");
        require(record, "boolean playable()", "playback capability predicate");

        String client = content(project, "/ExtensionRepositoryClient.java");
        require(client, "playbackUrl", "repository playbackUrl metadata alias");
        require(client, "mediaUrl", "repository mediaUrl metadata alias");
        require(client, "streamUrl", "repository streamUrl metadata alias");
        require(client, "episodeUrl", "repository episodeUrl metadata alias");
        require(client, "videoUrl", "repository videoUrl metadata alias");

        String manager = content(project, "/ExtensionManager.java");
        require(manager, "playback_", "persisted playback capability metadata");

        String provider = content(project, "/RepositoryMediaProvider.java");
        require(provider, "supportsPlayback(){return ext.playable();}", "provider playback capability handoff");
        require(provider, "resolveMediaUrl", "provider media resolver");
        require(provider, "{id}", "item-id playback template expansion");
        require(provider, "{episode}", "episode playback template expansion");
        require(provider, "Playback response did not provide an HTTPS media URL", "HTTPS fail-closed playback response");
        require(provider, "This extension did not declare a playback/media URL contract", "metadata-only provider rejection");

        String player = content(project, "/PlayerActivity.java");
        require(player, "new VideoView(this)", "real Android VideoView playback surface");
        require(player, "setMediaController", "Android playback controls");
        require(player, "setVideoURI", "resolved URL handoff to player");
        require(player, "getCurrentPosition()", "resume-position persistence");
        require(player, "resolveMediaUrl", "player provider resolver invocation");
        require(player, "supportsPlayback()", "player capability gate");

        System.out.println("PASS repository playback contract path");
    }

    private static String content(GeneratedProject project, String suffix) {
        for (GeneratedProject.FileEntry f : project.files)
            if (f != null && f.path != null && f.path.endsWith(suffix)) return f.content == null ? "" : f.content;
        throw new IllegalStateException("Missing generated file " + suffix);
    }

    private static void require(String source, String token, String label) {
        if (source == null || !source.contains(token))
            throw new IllegalStateException("Missing " + label + ": " + token);
    }
}
