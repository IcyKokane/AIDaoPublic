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
                "Provide Mihon-like Library, Updates, History, Browse and More primary information architecture; keep downloads, sources and repository management secondary.",
                "Persist favorites, history, downloads metadata, repository configuration, extension state, resume progress, selected provider, and selected player locally.",
                "Use Android-native navigation and typography, accessible labels, phone-safe status/navigation/IME insets, responsive cards/grids, and scroll-safe reachable controls.",
                "Do not render every screen as one long desktop-like vertical form. Use screen-specific cards, lists, grids, rails and empty states appropriate to each task.",
                "A favorited title must render in Library after navigation and process restart, and removal must update Library.",
                "Repository Sync/Remove and provider compatibility/selection controls must remain visibly labeled at phone density.",
                "Episode playback must use an actual provider-declared HTTPS media resolver when supported and clearly identify metadata-only or otherwise incompatible providers.",
                "If playback has no compatible authorized provider, show capability incomplete rather than pretending playback is ready.",
                "If playback needs setup, expose reachable Select provider and Select player actions and persist both choices.",
                "Built-in provider discovery must return usable parsed catalog records when the provider is reachable; transport or parse failures must be surfaced as provider errors, never as misleading no matches.",
                "Generate and wire an app-specific adaptive launcher icon and round icon instead of the generic debug launcher."
        );
        List<String> tasks=Arrays.asList("Infer concise product identity","Infer Mihon behavioral information architecture","Research reviewed provider capabilities","Integrate safe public metadata providers","Generate Android-native media navigation and persistence","Generate repository and extension lifecycle components","Generate provider compatibility and failure isolation","Generate favorites round-trip rendering and provider-aware playback handoff","Generate phone-native launcher identity and inset behavior","Run semantic fidelity validation and Android CI");
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
        for(String forbidden:new String[]{"DemoProvider","Origin Path","Sky Archive","sample data only","android.widget.android.widget.","android.graphics.android.graphics.","android.content.android.content.","android.app.android.app.","Save +60s test progress","Playback surface placeholder"})if(source.contains(forbidden))throw new IllegalStateException("Forbidden/corrupted/generated-fidelity content survived final pass: "+forbidden);

        for(String marker:new String[]{
                "ExtensionRepositoryClient","RepositoryStore","RepositoriesActivity","ExtensionRecord.State.ENABLED",
                "BuiltInProviderCatalog","JikanCatalogProvider","AniListCatalogProvider","https://api.jikan.moe/v4/anime","https://graphql.anilist.co",
                "HttpURLConnection","setConnectTimeout","setReadTimeout","getResponseCode",
                "new ArrayList<>(BuiltInProviderCatalog.providers())","WindowInsets.Type.systemBars","WindowInsets.Type.displayCutout","WindowInsets.Type.ime","ScrollView",
                "GridLayout","HorizontalScrollView","setMinHeight(dp(48))","card(","emptyState(",
                "Some sources could not be reached","LibraryActivity.class,UpdatesActivity.class,HistoryActivity.class,MainActivity.class,MoreActivity.class",
                "store.set(\"favorites\"","Remove from Library","Add to Library","Sync repository","Remove repository","dp(52)",
                "selected_provider","selected_player","Select provider","Select player","Unsupported for playback","Playback capability incomplete",
                "playbackUrl","supportsPlayback()","resolveMediaUrl","Playback: compatible","new VideoView(this)","setVideoURI","getCurrentPosition()","provider returned a non-HTTPS media URL"
        })if(!source.contains(marker))throw new IllegalStateException("Missing AniShelf real-phone fidelity marker: "+marker);

        String appScreen=content(project,root+"AppScreen.java");
        String browse=content(project,root+"MainActivity.java");
        String library=content(project,root+"LibraryActivity.java");
        String updates=content(project,root+"UpdatesActivity.java");
        String more=content(project,root+"MoreActivity.java");
        String providers=content(project,root+"ProvidersActivity.java");
        String player=content(project,root+"PlayerActivity.java");
        String jikan=content(project,root+"JikanCatalogProvider.java");
        String anilist=content(project,root+"AniListCatalogProvider.java");

        if(!appScreen.contains("Library\",\"Updates\",\"History\",\"Browse\",\"More"))throw new IllegalStateException("Mihon intent did not alter primary information architecture");
        if(appScreen.contains("Browse\",\"Library\",\"History\",\"Downloads\",\"Extensions"))throw new IllegalStateException("Generic media nav survived Mihon semantic pass");
        if(!browse.contains("HorizontalScrollView")||!browse.contains("GridLayout")||!browse.contains("discover"))throw new IllegalStateException("Browse is still a generic vertical form rather than discovery-oriented mobile layout");
        if(!browse.contains("Some sources could not be reached")||!browse.contains("No matches"))throw new IllegalStateException("Browse does not distinguish provider failure from a genuine zero-result state");
        if(!library.contains("grid()")||!library.contains("card("))throw new IllegalStateException("Library lacks responsive saved-title card/grid composition");
        if(!updates.contains("Refresh library updates")||!updates.contains("new Thread"))throw new IllegalStateException("Updates is a placeholder rather than provider-backed refresh behavior");
        if(!more.contains("Downloads")||!more.contains("Sources")||!more.contains("Repositories"))throw new IllegalStateException("More does not group secondary Mihon management surfaces");

        if(providers.contains("Select provider")&&!providers.contains("supportsPlayback"))throw new IllegalStateException("provider selection is not compatibility-gated");
        if(!providers.contains("Unsupported for playback")||!providers.contains("Select provider"))throw new IllegalStateException("provider lifecycle must expose compatibility-aware selection instead of a generic enable action");
        if(providers.contains("Enable extension")||providers.contains("Disable extension"))throw new IllegalStateException("provider lifecycle must not expose misleading generic enable/disable controls for unsupported playback providers");
        if(!player.contains("AppNavigator.open(this,ProvidersActivity.class)"))throw new IllegalStateException("playback setup cannot navigate to provider selection");
        if(!player.contains("store.putText(\"selected_player\""))throw new IllegalStateException("player selection is not persisted");

        for(String providerSource:new String[]{jikan,anilist}){
            if(!providerSource.contains("throw new")||!providerSource.contains("HttpURLConnection"))throw new IllegalStateException("built-in provider does not surface transport/parse failure as an actionable provider error");
            if(!providerSource.contains("AnimeItem"))throw new IllegalStateException("built-in provider does not parse network results into usable catalog records");
        }
        if(!jikan.contains("mal_id")||!jikan.contains("title"))throw new IllegalStateException("Jikan provider lacks concrete response-field parsing contract");
        if(!anilist.contains("media")||!anilist.contains("title"))throw new IllegalStateException("AniList provider lacks concrete response-field parsing contract");
        System.out.println("Generated AniShelf phone-fidelity acceptance project: "+project.projectName+" / "+project.packageName+" / "+project.files.size()+" files");
    }

    private static String content(GeneratedProject p,String path){for(GeneratedProject.FileEntry f:p.files)if(f!=null&&path.equals(f.path))return f.content==null?"":f.content;throw new IllegalStateException("missing generated file "+path);}
}
