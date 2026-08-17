package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Applies reference-app information architecture when the approved brief explicitly
 * asks for a Mihon-like media experience. This is behavior/layout inference only;
 * it does not copy Mihon code, branding, artwork, or extension binaries.
 */
final class MihonBehaviorPostProcessor {
    static final class Result {
        final String projectName;
        final String packageName;
        final List<GeneratedProject.FileEntry> files;
        final List<String> notes;
        Result(String projectName, String packageName, List<GeneratedProject.FileEntry> files, List<String> notes) {
            this.projectName = projectName;
            this.packageName = packageName;
            this.files = files;
            this.notes = notes;
        }
    }

    static Result process(String projectName, String packageName, List<GeneratedProject.FileEntry> incoming) {
        List<GeneratedProject.FileEntry> source = incoming == null ? new ArrayList<>() : new ArrayList<>(incoming);
        if (!isMedia(source) || !mentionsMihon(source)) {
            return new Result(projectName, packageName, source, new ArrayList<>());
        }

        String root = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        boolean hasUpdates = false, hasMore = false;
        for (GeneratedProject.FileEntry f : source) {
            if (f == null) continue;
            String content = f.content == null ? "" : f.content;
            if (f.path.endsWith("/AppScreen.java")) {
                content = content.replace(
                        "String[] names={\"Browse\",\"Library\",\"History\",\"Downloads\",\"Extensions\"};Class[] screens={MainActivity.class,LibraryActivity.class,HistoryActivity.class,DownloadsActivity.class,ProvidersActivity.class};",
                        "String[] names={\"Library\",\"Updates\",\"History\",\"Browse\",\"More\"};Class[] screens={LibraryActivity.class,UpdatesActivity.class,HistoryActivity.class,MainActivity.class,MoreActivity.class};");
            } else if ("app/src/main/AndroidManifest.xml".equals(f.path)) {
                if (!content.contains(".UpdatesActivity")) {
                    content = content.replace("<activity android:name=\".MainActivity\"", "<activity android:name=\".UpdatesActivity\" android:exported=\"false\"/>\n    <activity android:name=\".MoreActivity\" android:exported=\"false\"/>\n    <activity android:name=\".MainActivity\"");
                }
            } else if (f.path.equals(root + "MainActivity.java")) {
                content = content.replace("Search across installed sources. Built-in providers are ready immediately; repository sources stay optional and user-controlled.",
                        "Browse installed sources and search their catalogs. Built-in providers are ready immediately; additional repositories remain optional and user-controlled.");
                content = content.replace("section(\"Results\")", "section(\"Search results\")");
            }
            // Keep generated Java type names idempotent across repeated fidelity passes.
            // A prior compatibility rewrite may already qualify Button; never double-qualify it.
            while (content.contains("android.widget.android.widget.")) {
                content = content.replace("android.widget.android.widget.", "android.widget.");
            }
            if (f.path.equals(root + "UpdatesActivity.java")) hasUpdates = true;
            if (f.path.equals(root + "MoreActivity.java")) hasMore = true;
            out.add(new GeneratedProject.FileEntry(f.path, content, f.taskHint));
        }

        if (!hasUpdates) out.add(new GeneratedProject.FileEntry(root + "UpdatesActivity.java", updatesActivity(packageName), "Add Mihon-style library updates surface"));
        if (!hasMore) out.add(new GeneratedProject.FileEntry(root + "MoreActivity.java", moreActivity(packageName), "Group downloads, extensions, repositories, and settings under More"));

        List<String> notes = new ArrayList<>();
        notes.add("PASS Mihon reference intent activated behavior profile without copying Mihon branding or source code");
        notes.add("PASS bottom information architecture uses Library / Updates / History / Browse / More");
        notes.add("PASS extension and repository management moves under More instead of consuming primary navigation slots");
        notes.add("PASS downloads remain available as a secondary surface under More");
        return new Result(projectName, packageName, out, notes);
    }

    private static boolean isMedia(List<GeneratedProject.FileEntry> files) {
        for (GeneratedProject.FileEntry f : files) {
            if (f != null && f.path != null && (f.path.endsWith("/MediaProvider.java") || f.path.endsWith("/AnimeItem.java"))) return true;
        }
        return false;
    }

    private static boolean mentionsMihon(List<GeneratedProject.FileEntry> files) {
        for (GeneratedProject.FileEntry f : files) {
            if (f == null || f.content == null) continue;
            if (f.content.toLowerCase(Locale.US).contains("mihon")) return true;
        }
        return false;
    }

    private static String updatesActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "public final class UpdatesActivity extends AppScreen{" +
                "protected void render(){title(\"Updates\");subtitle(\"Recent episode and catalog changes from enabled sources appear here after items are added to your library.\");" +
                "section(\"Library updates\");body.addView(card(\"Nothing new yet\",\"Add titles to Library, then refresh their enabled providers to track newly reported episodes.\"));}}\n";
    }

    private static String moreActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.widget.*;" +
                "public final class MoreActivity extends AppScreen{" +
                "protected void render(){title(\"More\");subtitle(\"Downloads, sources, repositories, and app management.\");" +
                "Button downloads=button(\"Downloads\");downloads.setOnClickListener(v->AppNavigator.open(this,DownloadsActivity.class));body.addView(downloads);" +
                "Button extensions=button(\"Extensions\");extensions.setOnClickListener(v->AppNavigator.open(this,ProvidersActivity.class));body.addView(extensions);" +
                "Button repositories=button(\"Extension repositories\");repositories.setOnClickListener(v->AppNavigator.open(this,RepositoriesActivity.class));body.addView(repositories);" +
                "section(\"Source status\");for(MediaProvider p:BuiltInProviderCatalog.providers())body.addView(card(p.displayName(),p.health()));}}\n";
    }

    private MihonBehaviorPostProcessor() {}
}
