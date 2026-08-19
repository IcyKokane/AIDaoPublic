package dev.thefoolish.aidao;

import java.util.Arrays;

/**
 * Prevents declarative playback templates from bypassing the HTTPS-only provider
 * boundary when the expanded URL already looks like a direct media file.
 */
public final class RepositoryPlaybackHttpsAcceptance {
    public static void main(String[] args) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Anime Shelf",
                "Create an anime app like Mihon with repository-based providers and secure playback.",
                Arrays.asList(
                        "Allow only HTTPS repository contracts.",
                        "Resolve provider-declared playback URLs without fabricating streams.",
                        "Reject non-HTTPS playback targets even when they directly end in a media extension."
                ),
                Arrays.asList("Generate playback resolver", "Validate HTTPS media handoff")
        );
        for (String note : project.verificationNotes) {
            if (note.startsWith("FAIL ")) throw new IllegalStateException(note);
        }

        String provider = content(project, "/RepositoryMediaProvider.java");
        require(provider,
                "if(!target.startsWith(\"https://\"))throw new IOException(\"Playback contract must resolve to HTTPS\")",
                "expanded playback template HTTPS guard");
        require(provider,
                "target.endsWith(\".mp4\")||target.endsWith(\".m3u8\")||target.endsWith(\".webm\")",
                "direct-media fast path remains available after HTTPS validation");

        int guard = provider.indexOf("Playback contract must resolve to HTTPS");
        int fastPath = provider.indexOf("target.endsWith(\".mp4\")");
        if (guard < 0 || fastPath < 0 || guard > fastPath) {
            throw new IllegalStateException("HTTPS validation must occur before direct-media return");
        }

        System.out.println("PASS repository playback direct-media HTTPS boundary");
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
