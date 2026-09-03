package dev.thefoolish.aidao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/** CI harness for the exact provider-backed AniShelf real-phone failure class. */
public final class GenerateFidelitySampleProject {
    public static void main(String[] args) throws Exception {
        if (args.length!=1) throw new IllegalArgumentException("Expected output directory");
        Path output=Paths.get(args[0]).toAbsolutePath().normalize(); Files.createDirectories(output);
        List<String> requirements=Arrays.asList(
                "Search anime through repository-backed providers rather than fabricated sample entries.",
                "Bundle at least one reviewed real provider when a safe compatible public source is available.",
                "Allow users to add and remove HTTPS extension repository URLs and sync extension metadata.",
                "Show extension states including available, installed, enabled, disabled, failed, and unsupported for playback.",
                "Provide Browse, Detail, Library, History, Downloads, Extensions, Repositories, and Player surfaces.",
                "Persist favorites, history, downloads metadata, repository configuration, extension state, resume progress, selected provider, and selected player locally.",
                "Use Android-native navigation and typography, accessible labels, phone-safe status/navigation/IME insets, and scroll-safe reachable controls.",
                "A favorited title must render in Library after navigation and process restart, and removal must update Library.",
                "Repository Sync/Remove and provider compatibility/selection controls must remain visibly labeled at phone density.",
                "Episode playback must use an actual provider-declared HTTPS media resolver when supported and clearly identify metadata-only or otherwise incompatible providers.",
                "If playback has no compatible authorized provider, show capability incomplete rather than pretending playback is ready.",
                "If playback needs setup, expose reachable Select provider and Select player actions and persist both choices.",
                "Generate and wire an app-specific adaptive launcher icon and round icon instead of the generic debug launcher."
        );
        List<String> tasks=Arrays.asList("Infer concise product identity","Research reviewed provider capabilities","Integrate safe public metadata providers","Generate Android-native media navigation and persistence","Generate repository and extension lifecycle components","Generate provider compatibility and failure isolation","Generate favorites round-trip rendering and provider-aware playback handoff","Generate phone-native launcher identity and inset behavior","Run semantic fidelity validation and Android CI");
        GeneratedProject project=new LocalSourceGenerator().generate(
                "Make An Anime App Like Mihon, It Should Have Repository Based Providers",
                "Make an anime app like Mihon. It should have repository based providers, libraries based on anime websites, favorites, offline downloads, tags for genres, history, playback and resume progress. Users should be able to add extension repositories.",requirements,tasks);

        for(String note:project.verificationNotes){System.out.println(note);if(note.startsWith("FAIL "))throw new IllegalStateException("Fidelity verification failed: "+note);}
        if(project.projectName.length()>28||project.projectName.toLowerCase().startsWith("make "))throw new IllegalStateException("Raw brief leaked into product name: "+project.projectName);
        if(project.packageName.contains("makeananimeapplikemihon"))throw new IllegalStateException("Raw brief leaked into package identity: "+project.packageName);

        String root="app/src/main/java/"+project.packageName.replace('.','/')+"/";
        String[] required={root+"AppScreen.java",root+"LocalStore.java",root+"AppNavigator.java",root+"MainActivity.java",root+"DetailActivity.java",root+"LibraryActivity.java",root+"HistoryActivity.java",root+"DownloadsActivity.java",root+"ProvidersActivity.java",root+"RepositoriesActivity.java",root+"PlayerActivity.java",root+"MediaProvider.java",root+"ExtensionRecord.java",root+"RepositoryStore.java",root+"ExtensionRepositoryClient.java",root+"ExtensionManager.java",root+"RepositoryMediaProvider.java",root+"BuiltInProviderCatalog.java",root+"JikanCatalogProvider.java",root+"AniListCatalogProvider.java",root+"UpdatesActivity.java",root+"MoreActivity.java","app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml","app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml","app/src/main/res/drawable/ic_launcher_foreground.xml"};
        for(String path:required)if(!project.hasPath(path))throw new IllegalStateException("Missing fidelity file: "+path);
        String manifest=content(project,"app/src/main/AndroidManifest.xml");
        if(!manifest.contains("android:icon=\"@mipmap/ic_launcher\"")||!manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))throw new IllegalStateException("AniShelf launcher identity is not wired through adaptive mipmap resources");

        StringBuilder executable=new StringBuilder();
        for(GeneratedProject.FileEntry entry:project.files){if(entry.path.startsWith("app/src/main/java/")&&entry.path.endsWith(".java"))executable.append('\n').append(entry.content);Path target=output.resolve(entry.path).normalize();if(!target.startsWith(output))throw new SecurityException("Generated path escaped output root: "+entry.path);if(target.getParent()!=null)Files.createDirectories(target.getParent());Files.write(target,entry.content.getBytes(StandardCharsets.UTF_8));}
        String source=executable.toString();
        for(String forbidden:new String[]{"DemoProvider","Origin Path","Sky Archive","sample data only","android.widget.android.widget.","android.graphics.android.graphics.","android.content.android.content.","android.app.android.app.","Save +60s test progress","Playback surface placeholder"})if(source.contains(forbidden))throw new IllegalStateException("Forbidden/corrupted generated executable content survived fidelity pass: "+forbidden);
        for(String marker:new String[]{
                "ExtensionRepositoryClient","RepositoryStore","RepositoriesActivity","ExtensionRecord.State.ENABLED",
                "BuiltInProviderCatalog","JikanCatalogProvider","AniListCatalogProvider","https://api.jikan.moe/v4/anime","https://graphql.anilist.co",
                "new ArrayList<>(BuiltInProviderCatalog.providers())","WindowInsets.Type.systemBars","WindowInsets.Type.displayCutout","WindowInsets.Type.ime","ScrollView",
                "Some sources could not be reached","LibraryActivity.class,UpdatesActivity.class,HistoryActivity.class,MainActivity.class,MoreActivity.class",
                "store.set(\"favorites\"","Remove from Library","Add to Library","Sync repository","Remove repository","dp(52)",
                "selected_provider","selected_player","Select provider","Select player","Unsupported for playback","Playback capability incomplete",
                "playbackUrl","supportsPlayback()","resolveMediaUrl","Playback: compatible","new VideoView(this)","setVideoURI","getCurrentPosition()","provider returned a non-HTTPS media URL"
        })if(!source.contains(marker))throw new IllegalStateException("Missing AniShelf real-phone fidelity marker: "+marker);

        String providers=content(project,root+"ProvidersActivity.java"),player=content(project,root+"PlayerActivity.java");
        if(providers.contains("Select provider")&&!providers.contains("supportsPlayback"))throw new IllegalStateException("provider selection is not compatibility-gated");
        if(!providers.contains("Unsupported for playback")||!providers.contains("Select provider"))throw new IllegalStateException("provider lifecycle must expose compatibility-aware selection instead of a generic enable action");
        if(providers.contains("Enable extension")||providers.contains("Disable extension"))throw new IllegalStateException("provider lifecycle must not expose misleading generic enable/disable controls for unsupported playback providers");
        if(!player.contains("AppNavigator.open(this,ProvidersActivity.class)"))throw new IllegalStateException("playback setup cannot navigate to provider selection");
        if(!player.contains("store.putText(\"selected_player\""))throw new IllegalStateException("player selection is not persisted");
        System.out.println("Generated AniShelf phone-fidelity acceptance project: "+project.projectName+" / "+project.packageName+" / "+project.files.size()+" files");
    }

    private static String content(GeneratedProject p,String path){for(GeneratedProject.FileEntry f:p.files)if(f!=null&&path.equals(f.path))return f.content==null?"":f.content;throw new IllegalStateException("missing generated file "+path);}
}