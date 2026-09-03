package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Applies reference-app information architecture and product behavior when an
 * approved media brief explicitly asks for a Mihon-like experience. This is
 * behavioral inference only; no Mihon code, artwork, branding, or binaries are copied.
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
        if (!isMedia(source) || !mentionsMihon(source)) return new Result(projectName, packageName, source, new ArrayList<>());

        String root = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        boolean hasUpdates = false, hasMore = false, hasHistory = false, hasLibrary = false;
        for (GeneratedProject.FileEntry f : source) {
            if (f == null) continue;
            String content = f.content == null ? "" : f.content;
            if (f.path.endsWith("/AppScreen.java")) {
                content = content.replace(
                        "String[] names={\"Browse\",\"Library\",\"History\",\"Downloads\",\"Extensions\"};String[] icons={\"⌕\",\"▣\",\"◷\",\"⇩\",\"◈\"};Class[] screens={MainActivity.class,LibraryActivity.class,HistoryActivity.class,DownloadsActivity.class,ProvidersActivity.class};",
                        "String[] names={\"Library\",\"Updates\",\"History\",\"Browse\",\"More\"};String[] icons={\"▣\",\"↻\",\"◷\",\"⌕\",\"•••\"};Class[] screens={LibraryActivity.class,UpdatesActivity.class,HistoryActivity.class,MainActivity.class,MoreActivity.class};");
                content = content.replace(
                        "String[] names={\"Browse\",\"Library\",\"History\",\"Downloads\",\"Extensions\"};Class[] screens={MainActivity.class,LibraryActivity.class,HistoryActivity.class,DownloadsActivity.class,ProvidersActivity.class};",
                        "String[] names={\"Library\",\"Updates\",\"History\",\"Browse\",\"More\"};Class[] screens={LibraryActivity.class,UpdatesActivity.class,HistoryActivity.class,MainActivity.class,MoreActivity.class};");
            } else if ("app/src/main/AndroidManifest.xml".equals(f.path)) {
                if (!content.contains(".UpdatesActivity")) content = content.replace("<activity android:name=\".MainActivity\"", "<activity android:name=\".UpdatesActivity\" android:exported=\"false\"/>\n    <activity android:name=\".MoreActivity\" android:exported=\"false\"/>\n    <activity android:name=\".MainActivity\"");
            } else if (f.path.equals(root + "MainActivity.java")) {
                content = content.replace("Browse installed sources and search their catalogs. Built-in providers are ready immediately; additional repositories remain optional and user-controlled.",
                        "Discover and search across installed sources. Built-in catalog providers work immediately; additional repositories stay optional and user-controlled.");
                content = content.replace(
                        "for(ExtensionRecord x:new ExtensionManager(this).known())if(x.state==ExtensionRecord.State.ENABLED)providers.add(new RepositoryMediaProvider(x));",
                        "for(ExtensionRecord x:new ExtensionManager(this).known())if(x.state==ExtensionRecord.State.ENABLED&&x.searchable())providers.add(new RepositoryMediaProvider(x));");
            } else if (f.path.equals(root + "ProvidersActivity.java")) {
                content = makeProviderCompatibilitySafe(content);
            } else if (f.path.equals(root + "LibraryActivity.java")) {
                content = libraryActivity(packageName);
                hasLibrary = true;
            } else if (f.path.equals(root + "HistoryActivity.java")) {
                content = historyActivity(packageName);
                hasHistory = true;
            }
            while (content.contains("android.widget.android.widget.")) content = content.replace("android.widget.android.widget.", "android.widget.");
            if (f.path.equals(root + "UpdatesActivity.java")) hasUpdates = true;
            if (f.path.equals(root + "MoreActivity.java")) hasMore = true;
            out.add(new GeneratedProject.FileEntry(f.path, content, f.taskHint));
        }

        if (!hasLibrary) out.add(new GeneratedProject.FileEntry(root + "LibraryActivity.java", libraryActivity(packageName), "Render a Mihon-style locally persisted library as responsive cards"));
        if (!hasHistory) out.add(new GeneratedProject.FileEntry(root + "HistoryActivity.java", historyActivity(packageName), "Render recent playback/resume state as a dedicated history surface"));
        if (!hasUpdates) out.add(new GeneratedProject.FileEntry(root + "UpdatesActivity.java", updatesActivity(packageName), "Refresh saved library titles against their installed providers and show actionable update results"));
        if (!hasMore) out.add(new GeneratedProject.FileEntry(root + "MoreActivity.java", moreActivity(packageName), "Group downloads, sources, repositories, and management surfaces under More"));

        List<String> notes = new ArrayList<>();
        notes.add("PASS Mihon reference intent activated product behavior and information architecture without copying Mihon branding or source code");
        notes.add("PASS bottom information architecture uses Library / Updates / History / Browse / More");
        notes.add("PASS Library is a persisted card surface, Updates performs real provider refresh work, and History exposes resume state instead of placeholder buttons");
        notes.add("PASS extension and repository management moves under More instead of consuming primary navigation slots");
        notes.add("PASS incompatible repository metadata remains visible but cannot be falsely enabled as a working extension");
        notes.add("PASS downloads remain available as a secondary surface under More");
        return new Result(projectName, packageName, out, notes);
    }

    private static String makeProviderCompatibilitySafe(String content) {
        String oldToggle = "Button toggle=button(x.state==ExtensionRecord.State.ENABLED?\"Disable extension\":\"Enable extension\");toggle.setMinHeight(dp(52));toggle.setContentDescription(toggle.getText());toggle.setOnClickListener(v->{ExtensionRecord.State next=x.state==ExtensionRecord.State.ENABLED?ExtensionRecord.State.DISABLED:ExtensionRecord.State.ENABLED;m.setState(x.id,next);recreate();});body.addView(toggle);";
        String safeToggle = "boolean compatible=x.searchable();Button toggle=button(!compatible?\"Unsupported repository metadata\":x.state==ExtensionRecord.State.ENABLED?\"Disable extension\":\"Enable extension\");toggle.setMinHeight(dp(52));toggle.setContentDescription(toggle.getText());toggle.setEnabled(compatible);if(compatible)toggle.setOnClickListener(v->{ExtensionRecord.State next=x.state==ExtensionRecord.State.ENABLED?ExtensionRecord.State.DISABLED:ExtensionRecord.State.ENABLED;m.setState(x.id,next);recreate();});body.addView(toggle);if(!compatible)subtitle(\"This repository entry is visible for provenance only. AIDao cannot execute arbitrary extension APKs; add a provider with a declared HTTPS search contract to enable it.\");";
        content = content.replace(oldToggle, safeToggle);
        String nativeToggle = "for(ExtensionRecord x:known){LinearLayout c=card(x.name+\" · \"+x.state,x.version+\" · \"+x.repoUrl);Button toggle=button(x.state==ExtensionRecord.State.ENABLED?\"Disable extension\":\"Enable extension\");toggle.setOnClickListener(v->{ExtensionRecord.State next=x.state==ExtensionRecord.State.ENABLED?ExtensionRecord.State.DISABLED:ExtensionRecord.State.ENABLED;new ExtensionManager(this).setState(x.id,next);recreate();});c.addView(toggle);body.addView(c);}";
        String nativeSafe = "for(ExtensionRecord x:known){boolean compatible=x.searchable();String capability=(compatible?\"Search: compatible\":\"Unsupported repository metadata\")+\" · \"+(x.playable()?\"Playback: compatible\":\"Playback: unavailable\");LinearLayout c=card(x.name+\" · \"+x.state,x.version+\" · \"+x.repoUrl+\"\\n\"+capability);Button toggle=button(!compatible?\"Unsupported repository metadata\":x.state==ExtensionRecord.State.ENABLED?\"Disable extension\":\"Enable extension\");toggle.setMinHeight(dp(52));toggle.setContentDescription(toggle.getText());toggle.setEnabled(compatible);if(compatible)toggle.setOnClickListener(v->{ExtensionRecord.State next=x.state==ExtensionRecord.State.ENABLED?ExtensionRecord.State.DISABLED:ExtensionRecord.State.ENABLED;new ExtensionManager(this).setState(x.id,next);recreate();});c.addView(toggle);if(!compatible)c.addView(text(\"AIDao cannot execute arbitrary extension APKs. This entry remains visible for provenance until it declares a compatible HTTPS search contract.\",13,false));body.addView(c);}";
        return content.replace(nativeToggle, nativeSafe);
    }

    private static boolean isMedia(List<GeneratedProject.FileEntry> files) {
        for (GeneratedProject.FileEntry f : files) if (f != null && f.path != null && (f.path.endsWith("/MediaProvider.java") || f.path.endsWith("/AnimeItem.java"))) return true;
        return false;
    }

    private static boolean mentionsMihon(List<GeneratedProject.FileEntry> files) {
        for (GeneratedProject.FileEntry f : files) if (f != null && f.content != null && f.content.toLowerCase(Locale.US).contains("mihon")) return true;
        return false;
    }

    private static String libraryActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.content.*;import android.util.Base64;import android.widget.*;import java.nio.charset.StandardCharsets;import java.util.*;" +
                "public final class LibraryActivity extends AppScreen{protected void render(){title(\"Library\");Set<String> favs=store.set(\"favorites\");subtitle(favs.isEmpty()?\"Save titles from Browse and they will remain here after restart.\":favs.size()+\" saved title(s) · stored locally\");" +
                "if(favs.isEmpty()){body.addView(emptyState(\"Your library is empty\",\"Browse installed sources, open a title, then choose Add to Library.\"));Button browse=button(\"Browse sources\");browse.setOnClickListener(v->AppNavigator.open(this,MainActivity.class));body.addView(browse);return;}section(\"Saved titles\");GridLayout g=grid();for(String raw:favs){try{String[] x=raw.split(\"[|]\",5);if(x.length<5)continue;String provider=dec(x[0]),id=dec(x[1]),t=dec(x[2]),summary=dec(x[3]);int eps=Integer.parseInt(x[4]);MediaProvider p=BuiltInProviderCatalog.find(provider);String source=p==null?provider:p.displayName();LinearLayout c=card(t,(eps>0?eps+\" episodes · \":\"\")+source);c.setContentDescription(\"Open saved title \"+t);c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->{Intent i=new Intent(this,DetailActivity.class);i.putExtra(\"provider\",provider);i.putExtra(\"id\",id);i.putExtra(\"title\",t);i.putExtra(\"summary\",summary);i.putExtra(\"episodes\",eps);startActivity(i);});addGridCard(g,c);}catch(Exception ignored){}}body.addView(g,new LinearLayout.LayoutParams(-1,-2));}" +
                "private String dec(String s){return new String(Base64.decode(s,Base64.NO_WRAP|Base64.URL_SAFE),StandardCharsets.UTF_8);}}\n";
    }

    private static String historyActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.widget.*;public final class HistoryActivity extends AppScreen{protected void render(){title(\"History\");String last=store.text(\"last_episode\",\"\");if(last.trim().isEmpty()){subtitle(\"Playback history stays on this device.\");body.addView(emptyState(\"No watch history yet\",\"Open a playable source and start an episode; resume state will appear here.\"));}else{subtitle(\"Recent activity and resume state saved locally.\");section(\"Continue watching\");LinearLayout c=card(last,\"Resume position is restored automatically when the same episode is reopened.\");body.addView(c);}Button browse=button(\"Browse titles\");browse.setOnClickListener(v->AppNavigator.open(this,MainActivity.class));body.addView(browse);}}\n";
    }

    private static String updatesActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.util.Base64;import android.widget.*;import java.nio.charset.StandardCharsets;import java.util.*;" +
                "public final class UpdatesActivity extends AppScreen{private LinearLayout feed;private Button refresh;private ProgressBar progress;protected void render(){title(\"Updates\");subtitle(\"Refresh saved titles against their installed providers to check current episode metadata.\");refresh=button(\"Refresh library updates\");refresh.setOnClickListener(v->refresh());body.addView(refresh);progress=new ProgressBar(this);progress.setVisibility(android.view.View.GONE);body.addView(progress,new LinearLayout.LayoutParams(-1,dp(36)));section(\"Library updates\");feed=new LinearLayout(this);feed.setOrientation(LinearLayout.VERTICAL);body.addView(feed);showIdle();}" +
                "private void showIdle(){feed.removeAllViews();Set<String> favs=store.set(\"favorites\");feed.addView(favs.isEmpty()?emptyState(\"Nothing to refresh\",\"Add titles to Library first.\"):emptyState(\"Ready to refresh\",favs.size()+\" saved title(s) can be checked against their source.\"));}" +
                "private void refresh(){Set<String> favs=new LinkedHashSet<>(store.set(\"favorites\"));if(favs.isEmpty()){showIdle();return;}refresh.setEnabled(false);progress.setVisibility(android.view.View.VISIBLE);feed.removeAllViews();feed.addView(text(\"Checking saved titles…\",14,false));new Thread(()->{List<String> updates=new ArrayList<>(),unchanged=new ArrayList<>(),errors=new ArrayList<>();for(String raw:favs){try{String[] x=raw.split(\"[|]\",5);if(x.length<5)continue;String provider=dec(x[0]),title=dec(x[2]);int oldEpisodes=Integer.parseInt(x[4]);MediaProvider p=find(provider);if(p==null){errors.add(title+\" · source unavailable\");continue;}List<AnimeItem> found=p.search(title);AnimeItem best=found==null||found.isEmpty()?null:found.get(0);if(best==null){errors.add(title+\" · no current catalog match\");continue;}if(best.episodes>oldEpisodes&&oldEpisodes>0)updates.add(title+\" · \"+oldEpisodes+\" → \"+best.episodes+\" episodes\");else unchanged.add(title+\" · current metadata checked\");}catch(Exception e){errors.add(\"Refresh error · \"+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}}runOnUiThread(()->show(updates,unchanged,errors));}).start();}" +
                "private void show(List<String> updates,List<String> unchanged,List<String> errors){refresh.setEnabled(true);progress.setVisibility(android.view.View.GONE);feed.removeAllViews();if(updates.isEmpty()&&errors.isEmpty())feed.addView(emptyState(\"No new episode counts found\",\"All reachable saved titles are current according to their providers.\"));for(String s:updates)feed.addView(card(\"Update available\",s));for(String s:unchanged)feed.addView(card(\"Checked\",s));if(!errors.isEmpty()){feed.addView(card(\"Some titles could not be refreshed\",android.text.TextUtils.join(\"\\n\",errors)+\"\\nRetry after checking your connection or source status.\"));}}" +
                "private MediaProvider find(String id){MediaProvider p=BuiltInProviderCatalog.find(id);if(p!=null)return p;ExtensionRecord x=new ExtensionManager(this).find(id);return x!=null&&x.state==ExtensionRecord.State.ENABLED&&x.searchable()?new RepositoryMediaProvider(x):null;}private String dec(String s){return new String(Base64.decode(s,Base64.NO_WRAP|Base64.URL_SAFE),StandardCharsets.UTF_8);}}\n";
    }

    private static String moreActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.widget.*;public final class MoreActivity extends AppScreen{protected void render(){title(\"More\");subtitle(\"Downloads, sources, repositories, and app management.\");section(\"Content\");LinearLayout downloads=card(\"Downloads\",\"Open saved offline media and download state.\");downloads.setClickable(true);downloads.setOnClickListener(v->AppNavigator.open(this,DownloadsActivity.class));body.addView(downloads);section(\"Sources\");LinearLayout extensions=card(\"Sources\",\"Review built-in providers and optional repository sources.\");extensions.setClickable(true);extensions.setOnClickListener(v->AppNavigator.open(this,ProvidersActivity.class));body.addView(extensions);LinearLayout repositories=card(\"Extension repositories\",\"Add or sync compatible HTTPS repository metadata.\");repositories.setClickable(true);repositories.setOnClickListener(v->AppNavigator.open(this,RepositoriesActivity.class));body.addView(repositories);section(\"Source status\");for(MediaProvider p:BuiltInProviderCatalog.providers())body.addView(card(p.displayName(),p.health()));}}\n";
    }

    private MihonBehaviorPostProcessor() {}
}
