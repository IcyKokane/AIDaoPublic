package dev.thefoolish.aidao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** CI-only acceptance harness for the production generated-source pipeline. */
public final class GenerateSampleProject {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected output directory");
        Path output = Paths.get(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(output);

        List<String> requirements = Arrays.asList(
                "Provide a searchable anime catalog and separate detail pages with episode navigation.",
                "Persist favorites, watch history, and per-episode resume progress locally.",
                "Keep media providers behind replaceable interfaces with visible health/failure states.",
                "Provide separate Browse, Detail, Library, History, Downloads, Extensions, Repositories, and Player screens.",
                "Use reviewed catalog providers plus compatible repository providers and never substitute fabricated sample entries.",
                "Generate reusable Android UI/data architecture, resources, and manifest navigation."
        );
        List<String> tasks = Arrays.asList(
                "Create the Android application shell and reusable screen architecture.",
                "Define anime, provider, library, extension, and watch-progress models.",
                "Build browse, detail, library, history, download, extension, repository, and player flows.",
                "Persist favorites and watch progress locally.",
                "Run semantic fidelity validation and Android CI."
        );

        GeneratedProject project = new LocalSourceGenerator().generate(
                "AIDao V1 Acceptance Anime App",
                "Build a nontrivial Android anime browsing app with search, detail pages, episode buttons, favorites, history, provider isolation and resumable playback. Use repository based providers where compatible. This proves AIDao-generated multi-screen source compiles into a real APK.",
                requirements,
                tasks
        );

        for (GeneratedProject.FileEntry entry : project.files) {
            Path target = output.resolve(entry.path).normalize();
            if (!target.startsWith(output)) throw new SecurityException("Generated path escaped output root: " + entry.path);
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            Files.write(target, entry.content.getBytes(StandardCharsets.UTF_8));
        }

        requireVerified(project, "media acceptance");
        if (project.files.size() < 20) throw new IllegalStateException("Expected a nontrivial production generated source tree, got " + project.files.size());
        String root = "app/src/main/java/" + project.packageName.replace('.', '/') + "/";
        String[] requiredFiles = {
                root + "AppScreen.java", root + "LocalStore.java", root + "AppNavigator.java",
                root + "MainActivity.java", root + "DetailActivity.java", root + "LibraryActivity.java",
                root + "HistoryActivity.java", root + "DownloadsActivity.java", root + "ProvidersActivity.java",
                root + "RepositoriesActivity.java", root + "PlayerActivity.java", root + "MediaProvider.java",
                root + "ExtensionRecord.java", root + "ExtensionRepositoryClient.java", root + "ExtensionManager.java",
                root + "RepositoryMediaProvider.java", root + "BuiltInProviderCatalog.java",
                root + "JikanCatalogProvider.java", root + "AniListCatalogProvider.java",
                "app/src/main/res/values/colors.xml", "app/src/main/res/values/strings.xml"
        };
        for (String path : requiredFiles) if (!project.hasPath(path)) throw new IllegalStateException("Missing v1 acceptance file: " + path);
        if (project.hasPath(root + "DemoProvider.java")) throw new IllegalStateException("Production media output must not retain DemoProvider.java");
        String manifest = project.find("app/src/main/AndroidManifest.xml").content;
        for (String activity : new String[]{"MainActivity","DetailActivity","LibraryActivity","HistoryActivity","DownloadsActivity","ProvidersActivity","RepositoriesActivity","PlayerActivity"})
            if (!manifest.contains("." + activity)) throw new IllegalStateException("Manifest missing activity " + activity);

        verifyDomainMatrix();
        System.out.println("Generated " + project.files.size() + " production files for " + project.packageName + " with provider-backed media navigation and local persistence");
        System.out.println("Cross-domain source-generation acceptance matrix passed");
    }

    private static void verifyDomainMatrix() {
        verifyDomain(
                "Budget Companion",
                "Create a personal finance budget and expense tracker with transactions, monthly reports, and spending categories.",
                new String[]{"TransactionsActivity.java", "BudgetsActivity.java", "ReportsActivity.java"}
        );
        verifyDomain(
                "Habit Activity Tracker",
                "Create an activity and habit tracker with a timeline, weekly analytics reports, and data controls.",
                new String[]{"TimelineActivity.java", "ReportsActivity.java", "DataControlsActivity.java"}
        );
        verifyDomain(
                "Community Chat",
                "Create a social community app with chat messages, profiles, inbox conversations, and settings.",
                new String[]{"InboxActivity.java", "ProfileActivity.java", "SettingsActivity.java"}
        );
        verifyDomain(
                "Simple Marketplace",
                "Create a shopping marketplace with products, product details, a cart, orders, and checkout preparation.",
                new String[]{"ProductActivity.java", "CartActivity.java", "OrdersActivity.java"}
        );
        verifyDomain(
                "Writing Library",
                "Create a note and document writing app with an editor, search, and a local content library.",
                new String[]{"EditorActivity.java", "SearchActivity.java", "LibraryActivity.java"}
        );
        verifyDomain(
                "Inventory Utility",
                "Create a small Android inventory utility with connected explore, detail, and settings screens.",
                new String[]{"ExploreActivity.java", "DetailActivity.java", "SettingsActivity.java"}
        );
    }

    private static void verifyDomain(String name, String brief, String[] expectedScreens) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                name,
                brief,
                Collections.singletonList("Generate multiple connected Android screens with persistent local state."),
                Collections.singletonList("Generate and verify the Android source tree.")
        );
        requireVerified(project, name);
        String root = "app/src/main/java/" + project.packageName.replace('.', '/') + "/";
        if (!project.hasPath(root + "MainActivity.java")) throw new IllegalStateException(name + " missing MainActivity.java");
        if (!project.hasPath(root + "AppScreen.java") && !project.hasPath(root + "GeneratedScreen.java"))
            throw new IllegalStateException(name + " missing reusable screen architecture");
        if (!project.hasPath(root + "LocalStore.java")) throw new IllegalStateException(name + " missing LocalStore.java");
        if (!project.hasPath(root + "AppNavigator.java")) throw new IllegalStateException(name + " missing AppNavigator.java");
        for (String screen : expectedScreens) {
            if (!project.hasPath(root + screen)) throw new IllegalStateException(name + " missing expected domain screen " + screen);
        }
        String manifest = project.find("app/src/main/AndroidManifest.xml").content;
        for (String screen : expectedScreens) {
            String activity = screen.substring(0, screen.length() - ".java".length());
            if (!manifest.contains("." + activity)) throw new IllegalStateException(name + " manifest missing " + activity);
        }
    }

    private static void requireVerified(GeneratedProject project, String label) {
        for (String note : project.verificationNotes) {
            System.out.println(label + ": " + note);
            if (note.startsWith("FAIL ")) throw new IllegalStateException(label + " generator verification failed: " + note);
        }
    }
}
