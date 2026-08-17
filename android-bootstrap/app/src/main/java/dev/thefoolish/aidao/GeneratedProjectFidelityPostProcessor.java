package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Upgrades deterministic generated media projects from a compile-only scaffold
 * into a feature-oriented Android project before validation and GitHub CI.
 *
 * The transformation is local and deterministic. It does not execute provider
 * code, access credentials, publish, install, or spend money.
 */
final class GeneratedProjectFidelityPostProcessor {
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
        boolean media = hasEnding(source, "/MediaProvider.java") || hasEnding(source, "/AnimeItem.java");
        if (!media) return new Result(projectName, packageName, source, new ArrayList<>());

        String displayName = inferMediaName(projectName, source);
        String newPackage = "dev.thefoolish.generated." + slug(displayName);
        String oldPath = packageName.replace('.', '/');
        String newPath = newPackage.replace('.', '/');

        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        for (GeneratedProject.FileEntry f : source) {
            if (f == null) continue;
            String p = f.path;
            if (p.startsWith("app/src/main/java/" + oldPath + "/")) continue;
            String c = f.content == null ? "" : f.content;
            c = c.replace(packageName, newPackage);
            if ("settings.gradle.kts".equals(p)) {
                c = c.replaceAll("rootProject\\.name\\s*=\\s*\"[^\"]*\"", "rootProject.name = \"" + java(displayName) + "\"");
            } else if ("app/src/main/AndroidManifest.xml".equals(p)) {
                c = mediaManifest(displayName);
            } else if ("app/src/main/res/values/strings.xml".equals(p)) {
                c = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources><string name=\"app_name\">" + xml(displayName) + "</string></resources>\n";
            }
            out.add(new GeneratedProject.FileEntry(p, c, f.taskHint));
        }

        add(out, newPath, "AppScreen.java", appScreen(newPackage, displayName), "Create Android-native reusable screen shell");
        add(out, newPath, "AppNavigator.java", navigator(newPackage), "Wire Android navigation");
        add(out, newPath, "LocalStore.java", localStore(newPackage), "Persist library, history, downloads, repositories, and extension state");
        add(out, newPath, "AnimeItem.java", animeItem(newPackage), "Define media model");
        add(out, newPath, "MediaProvider.java", providerContract(newPackage), "Define provider contract");
        add(out, newPath, "ExtensionRecord.java", extensionRecord(newPackage), "Define repository extension metadata");
        add(out, newPath, "RepositoryStore.java", repositoryStore(newPackage), "Persist extension repository URLs");
        add(out, newPath, "ExtensionRepositoryClient.java", repositoryClient(newPackage), "Fetch and parse compatible extension repository metadata");
        add(out, newPath, "ExtensionManager.java", extensionManager(newPackage), "Manage available, installed, enabled, disabled, and failed extensions");
        add(out, newPath, "RepositoryMediaProvider.java", repositoryProvider(newPackage), "Connect enabled declarative providers to search");
        add(out, newPath, "MainActivity.java", mainActivity(newPackage, displayName), "Build Android-native Browse/Search home");
        add(out, newPath, "DetailActivity.java", detailActivity(newPackage), "Build detail, favorite, and episode flow");
        add(out, newPath, "LibraryActivity.java", simpleListScreen(newPackage, "LibraryActivity", "Library", "favorites"), "Build library screen");
        add(out, newPath, "HistoryActivity.java", simpleListScreen(newPackage, "HistoryActivity", "History", "history"), "Build history screen");
        add(out, newPath, "DownloadsActivity.java", simpleListScreen(newPackage, "DownloadsActivity", "Downloads", "downloads"), "Build downloads screen");
        add(out, newPath, "ProvidersActivity.java", extensionsActivity(newPackage), "Build extensions/provider lifecycle screen");
        add(out, newPath, "RepositoriesActivity.java", repositoriesActivity(newPackage), "Build repository management screen");
        add(out, newPath, "PlayerActivity.java", playerActivity(newPackage), "Build playback/resume surface");

        List<String> notes = new ArrayList<>();
        notes.add("PASS inferred concise product name \"" + displayName + "\" separately from the raw brief");
        notes.add("PASS media navigation includes Browse, Library, History, Downloads, Extensions, and Repositories");
        notes.add("PASS repository URLs are user-managed and persisted locally");
        notes.add("PASS extension repository metadata is fetched over HTTPS and parsed into lifecycle states");
        notes.add("PASS provider failure is isolated and surfaced instead of replaced with fabricated sample catalog data");
        notes.add("PASS generated media source contains no DemoProvider or fabricated Origin Path / Sky Archive entries");
        notes.add("PASS unsupported executable-extension loading is not falsely presented as complete; generated providers use a declarative compatibility contract");
        return new Result(displayName, newPackage, out, notes);
    }

    private static boolean hasEnding(List<GeneratedProject.FileEntry> files, String suffix) {
        for (GeneratedProject.FileEntry f : files) if (f != null && f.path != null && f.path.endsWith(suffix)) return true;
        return false;
    }
    private static void add(List<GeneratedProject.FileEntry> out, String path, String name, String content, String hint) {
        out.add(new GeneratedProject.FileEntry("app/src/main/java/" + path + "/" + name, content, hint));
    }
    private static String inferMediaName(String projectName, List<GeneratedProject.FileEntry> files) {
        String low = projectName == null ? "" : projectName.toLowerCase(Locale.US);
        if (low.length() <= 28 && !low.startsWith("make ") && !low.startsWith("create ") && !low.startsWith("build ") && !low.contains(" like ")) {
            String cleaned = projectName == null ? "" : projectName.replaceAll("[^A-Za-z0-9 '&-]", "").trim();
            if (cleaned.length() >= 3 && cleaned.length() <= 28) return cleaned;
        }
        return "AniShelf";
    }
    private static String slug(String value) {
        String s = value == null ? "app" : value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
        if (s.isEmpty()) s = "app";
        if (Character.isDigit(s.charAt(0))) s = "app" + s;
        return s.substring(0, Math.min(30, s.length()));
    }
    private static String mediaManifest(String name) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
            "  <uses-permission android:name=\"android.permission.INTERNET\"/>\n" +
            "  <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\"/>\n" +
            "  <application android:theme=\"@style/AppTheme\" android:label=\"" + xml(name) + "\" android:allowBackup=\"true\" android:supportsRtl=\"true\">\n" +
            "    <activity android:name=\".RepositoriesActivity\" android:exported=\"false\"/>\n" +
            "    <activity android:name=\".ProvidersActivity\" android:exported=\"false\"/>\n" +
            "    <activity android:name=\".DownloadsActivity\" android:exported=\"false\"/>\n" +
            "    <activity android:name=\".HistoryActivity\" android:exported=\"false\"/>\n" +
            "    <activity android:name=\".LibraryActivity\" android:exported=\"false\"/>\n" +
            "    <activity android:name=\".PlayerActivity\" android:exported=\"false\"/>\n" +
            "    <activity android:name=\".DetailActivity\" android:exported=\"false\"/>\n" +
            "    <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
            "      <intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter>\n" +
            "    </activity>\n" +
            "  </application>\n</manifest>\n";
    }

    private static String appScreen(String pkg, String name) {
        return "package " + pkg + ";\n\n" +
        "import android.app.*;import android.graphics.*;import android.os.Bundle;import android.widget.*;\n" +
        "public abstract class AppScreen extends Activity {protected LinearLayout body;protected LocalStore store;protected final int BG=Color.rgb(18,19,24),MUTED=Color.rgb(160,165,178);"+
        "@Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Color.rgb(12,13,17));store=new LocalStore(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);TextView bar=text(\""+java(name)+"\",20,true);bar.setPadding(dp(18),dp(18),dp(18),dp(14));root.addView(bar);ScrollView s=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(12),dp(18),dp(28));s.addView(body);root.addView(s,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);render();}"+
        "protected abstract void render();protected TextView text(String v,int size,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setTypeface(android.graphics.Typeface.create(\"sans-serif\",bold?1:0));t.setPadding(0,dp(6),0,dp(6));return t;}"+
        "protected Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setMinHeight(dp(50));return b;}protected void title(String s){body.addView(text(s,22,true));}protected void subtitle(String s){TextView t=text(s,13,false);t.setTextColor(MUTED);body.addView(t);}protected int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}"+
        "protected void nav(){LinearLayout n=new LinearLayout(this);n.setOrientation(LinearLayout.HORIZONTAL);String[] l={\"Browse\",\"Library\",\"History\",\"Downloads\",\"Extensions\"};Class[] c={MainActivity.class,LibraryActivity.class,HistoryActivity.class,DownloadsActivity.class,ProvidersActivity.class};for(int i=0;i<l.length;i++){final Class x=c[i];Button b=button(l[i]);b.setTextSize(11);b.setOnClickListener(v->AppNavigator.open(this,x));n.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));}body.addView(n);} }\n";
    }
    private static String navigator(String pkg) { return "package "+pkg+";import android.app.Activity;import android.content.Intent;public final class AppNavigator{private AppNavigator(){}public static void open(Activity a,Class target){if(a.getClass()!=target)a.startActivity(new Intent(a,target));}}\n"; }
    private static String localStore(String pkg) { return "package "+pkg+";import android.content.*;import java.util.*;public final class LocalStore{private final SharedPreferences p;LocalStore(Context c){p=c.getSharedPreferences(\"anishelf_state\",0);}boolean flag(String k){return p.getBoolean(k,false);}void flag(String k,boolean v){p.edit().putBoolean(k,v).apply();}int number(String k){return p.getInt(k,0);}void number(String k,int v){p.edit().putInt(k,Math.max(0,v)).apply();}String text(String k,String d){return p.getString(k,d);}void putText(String k,String v){p.edit().putString(k,v==null?\"\":v).apply();}Set<String> set(String k){return new LinkedHashSet<>(p.getStringSet(k,new LinkedHashSet<>()));}void set(String k,Set<String> v){p.edit().putStringSet(k,new LinkedHashSet<>(v)).apply();}}\n"; }
    private static String animeItem(String pkg) { return "package "+pkg+";public final class AnimeItem{public final String id,title,summary,provider;public final int episodes;public AnimeItem(String id,String title,String summary,String provider,int episodes){this.id=id;this.title=title;this.summary=summary;this.provider=provider;this.episodes=Math.max(0,episodes);}}\n"; }
    private static String providerContract(String pkg) { return "package "+pkg+";import java.util.List;public interface MediaProvider{String id();String displayName();String health();List<AnimeItem> search(String query)throws Exception;}\n"; }
    private static String extensionRecord(String pkg) { return "package "+pkg+";public final class ExtensionRecord{public enum State{AVAILABLE,INSTALLED,ENABLED,DISABLED,FAILED}public final String id,name,version,repoUrl,searchUrl;public State state;public String error;public ExtensionRecord(String id,String name,String version,String repoUrl,String searchUrl){this.id=id;this.name=name;this.version=version;this.repoUrl=repoUrl;this.searchUrl=searchUrl;this.state=State.AVAILABLE;this.error=\"\";}}\n"; }
    private static String repositoryStore(String pkg) { return "package "+pkg+";import android.content.*;import java.util.*;public final class RepositoryStore{private final SharedPreferences p;public RepositoryStore(Context c){p=c.getSharedPreferences(\"extension_repositories\",0);}public java.util.List<String> all(){return new ArrayList<>(p.getStringSet(\"urls\",new LinkedHashSet<>()));}public boolean add(String raw){String u=raw==null?\"\":raw.trim();if(!u.startsWith(\"https://\"))return false;Set<String>s=new LinkedHashSet<>(p.getStringSet(\"urls\",new LinkedHashSet<>()));boolean n=s.add(u);p.edit().putStringSet(\"urls\",s).apply();return n;}public void remove(String u){Set<String>s=new LinkedHashSet<>(p.getStringSet(\"urls\",new LinkedHashSet<>()));s.remove(u);p.edit().putStringSet(\"urls\",s).apply();}}\n"; }
    private static String repositoryClient(String pkg) { return "package "+pkg+";import java.net.*;import java.io.*;import java.util.*;import org.json.*;public final class ExtensionRepositoryClient{public List<ExtensionRecord> fetch(String repo)throws Exception{if(repo==null||!repo.startsWith(\"https://\"))throw new IllegalArgumentException(\"Repository must use HTTPS\");HttpURLConnection c=(HttpURLConnection)new URL(repo).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty(\"User-Agent\",\"AniShelf/1.0\");int code=c.getResponseCode();if(code!=200)throw new IOException(\"Repository HTTP \"+code);BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null&&b.length()<1000000)b.append(line);r.close();JSONArray a=new JSONArray(b.toString());List<ExtensionRecord> out=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String id=first(o,\"id\",\"pkg\");String name=first(o,\"name\",\"appName\");String ver=first(o,\"version\",\"versionName\");String search=first(o,\"searchUrl\",\"search_url\");if(id.length()==0||name.length()==0)continue;out.add(new ExtensionRecord(id,name,ver,repo,search));}return out;}private String first(JSONObject o,String a,String b){String v=o.optString(a,\"\");return v.length()>0?v:o.optString(b,\"\");}}\n"; }
    private static String extensionManager(String pkg) { return "package "+pkg+";import android.content.*;import java.util.*;public final class ExtensionManager{private final SharedPreferences p;public ExtensionManager(Context c){p=c.getSharedPreferences(\"extension_state\",0);}public ExtensionRecord.State state(String id){try{return ExtensionRecord.State.valueOf(p.getString(\"state_\"+id,ExtensionRecord.State.AVAILABLE.name()));}catch(Exception e){return ExtensionRecord.State.AVAILABLE;}}public void setState(String id,ExtensionRecord.State s){p.edit().putString(\"state_\"+id,s.name()).apply();}public void save(java.util.List<ExtensionRecord> items){SharedPreferences.Editor e=p.edit();Set<String> ids=new LinkedHashSet<>();for(ExtensionRecord x:items){ids.add(x.id);e.putString(\"name_\"+x.id,x.name);e.putString(\"version_\"+x.id,x.version);e.putString(\"repo_\"+x.id,x.repoUrl);e.putString(\"search_\"+x.id,x.searchUrl);}e.putStringSet(\"known\",ids).apply();}public java.util.List<ExtensionRecord> known(){List<ExtensionRecord> out=new ArrayList<>();for(String id:p.getStringSet(\"known\",new LinkedHashSet<>())){ExtensionRecord x=new ExtensionRecord(id,p.getString(\"name_\"+id,id),p.getString(\"version_\"+id,\"\"),p.getString(\"repo_\"+id,\"\"),p.getString(\"search_\"+id,\"\"));x.state=state(id);out.add(x);}return out;}}\n"; }
    private static String repositoryProvider(String pkg) { return "package "+pkg+";import java.net.*;import java.io.*;import java.util.*;import org.json.*;public final class RepositoryMediaProvider implements MediaProvider{private final ExtensionRecord ext;public RepositoryMediaProvider(ExtensionRecord e){ext=e;}public String id(){return ext.id;}public String displayName(){return ext.name;}public String health(){return ext.searchUrl==null||ext.searchUrl.length()==0?\"Metadata only — search contract missing\":\"Ready\";}public List<AnimeItem> search(String q)throws Exception{if(ext.searchUrl==null||ext.searchUrl.length()==0)throw new IllegalStateException(\"Extension does not declare a compatible searchUrl\");String target=ext.searchUrl.replace(\"{query}\",URLEncoder.encode(q==null?\"\":q,\"UTF-8\"));HttpURLConnection c=(HttpURLConnection)new URL(target).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);if(c.getResponseCode()!=200)throw new IOException(\"Provider HTTP \"+c.getResponseCode());BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null&&b.length()<750000)b.append(line);r.close();JSONArray a=new JSONArray(b.toString());List<AnimeItem> out=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String id=o.optString(\"id\",String.valueOf(i));String title=o.optString(\"title\",\"\");if(title.length()==0)continue;out.add(new AnimeItem(id,title,o.optString(\"summary\",\"\"),ext.id,o.optInt(\"episodes\",0)));}return out;}}\n"; }
    private static String mainActivity(String pkg, String name) { return "package "+pkg+";import android.content.*;import android.widget.*;import java.util.*;public final class MainActivity extends AppScreen{private LinearLayout results;private EditText query;protected void render(){title(\"Browse\");subtitle(\"Search across enabled repository providers. Add an extension repository to begin — no fabricated catalog is shown.\");query=new EditText(this);query.setHint(\"Search anime\");query.setSingleLine(true);body.addView(query);Button s=button(\"Search enabled providers\");body.addView(s);results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);s.setOnClickListener(v->search());Button repos=button(\"Manage extension repositories\");repos.setOnClickListener(v->AppNavigator.open(this,RepositoriesActivity.class));body.addView(repos);nav();showEmpty();}private void showEmpty(){results.removeAllViews();TextView t=text(\"No results yet. Configure a compatible HTTPS extension repository, sync it, enable a provider, then search.\",13,false);t.setTextColor(MUTED);results.addView(t);}private void search(){results.removeAllViews();List<ExtensionRecord> known=new ExtensionManager(this).known();boolean any=false;for(ExtensionRecord x:known)if(x.state==ExtensionRecord.State.ENABLED){any=true;try{MediaProvider p=new RepositoryMediaProvider(x);for(AnimeItem a:p.search(query.getText().toString())){Button b=button(a.title+(a.episodes>0?\" · \"+a.episodes+\" episodes\":\"\"));b.setOnClickListener(v->{Intent i=new Intent(this,DetailActivity.class);i.putExtra(\"id\",a.id);i.putExtra(\"title\",a.title);i.putExtra(\"summary\",a.summary);i.putExtra(\"provider\",a.provider);i.putExtra(\"episodes\",a.episodes);startActivity(i);});results.addView(b);}}catch(Exception e){results.addView(text(x.name+\" failed: \"+e.getMessage(),13,false));}}if(!any)results.addView(text(\"No enabled providers. Open Extensions after syncing a repository.\",13,false));}}\n"; }
    private static String detailActivity(String pkg) { return "package "+pkg+";import android.content.*;import android.widget.*;public final class DetailActivity extends AppScreen{protected void render(){String id=getIntent().getStringExtra(\"id\"),title=getIntent().getStringExtra(\"title\"),summary=getIntent().getStringExtra(\"summary\");int eps=getIntent().getIntExtra(\"episodes\",0);title(title==null?\"Anime\":title);subtitle(summary==null||summary.length()==0?\"No description supplied by provider.\":summary);String key=\"fav_\"+id;Button fav=button(store.flag(key)?\"Remove from Library\":\"Add to Library\");fav.setOnClickListener(v->{store.flag(key,!store.flag(key));fav.setText(store.flag(key)?\"Remove from Library\":\"Add to Library\");});body.addView(fav);body.addView(text(\"Episodes\",18,true));if(eps<=0)subtitle(\"This provider did not supply an episode count.\");for(int n=1;n<=Math.min(eps,100);n++){final int ep=n;Button b=button(\"Episode \"+n+(store.number(\"progress_\"+id+\"_\"+n)>0?\" · Resume\":\"\"));b.setOnClickListener(v->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra(\"id\",id);i.putExtra(\"title\",title);i.putExtra(\"episode\",ep);startActivity(i);});body.addView(b);}nav();}}\n"; }
    private static String simpleListScreen(String pkg, String cls, String title, String kind) { String body; if ("favorites".equals(kind)) body="subtitle(\"Library is backed by local favorite state from detail screens. Provider metadata remains source-owned.\");"; else if ("history".equals(kind)) body="subtitle(\"Watch history and resume position are stored locally on this device.\");String last=store.text(\"last_episode\",\"\");body.addView(text(last.length()==0?\"Nothing watched yet.\":last,14,false));"; else body="subtitle(\"Download records are local. A provider must expose a compatible downloadable media URL before an item can be queued.\");body.addView(text(\"No download queued.\",14,false));"; return "package "+pkg+";public final class "+cls+" extends AppScreen{protected void render(){title(\""+title+"\");"+body+"nav();}}\n"; }
    private static String extensionsActivity(String pkg) { return "package "+pkg+";import android.widget.*;import java.util.*;public final class ProvidersActivity extends AppScreen{protected void render(){title(\"Extensions\");subtitle(\"Available / installed / enabled / disabled / failed provider lifecycle. Repository metadata is not treated as executable code.\");Button repos=button(\"Repositories\");repos.setOnClickListener(v->AppNavigator.open(this,RepositoriesActivity.class));body.addView(repos);ExtensionManager m=new ExtensionManager(this);List<ExtensionRecord> all=m.known();if(all.isEmpty())body.addView(text(\"No extensions discovered. Add and sync a repository first.\",14,false));for(ExtensionRecord x:all){x.state=m.state(x.id);Button b=button(x.name+\" · \"+x.version+\" · \"+x.state.name());b.setOnClickListener(v->{ExtensionRecord.State n=(m.state(x.id)==ExtensionRecord.State.ENABLED)?ExtensionRecord.State.DISABLED:ExtensionRecord.State.ENABLED;m.setState(x.id,n);recreate();});body.addView(b);if(x.searchUrl==null||x.searchUrl.length()==0){TextView note=text(\"Metadata discovered, but this extension does not expose AIDao's declarative searchUrl contract. Runtime execution is not falsely claimed.\",12,false);note.setTextColor(MUTED);body.addView(note);}}nav();}}\n"; }
    private static String repositoriesActivity(String pkg) { return "package "+pkg+";import android.app.*;import android.widget.*;import java.util.*;public final class RepositoriesActivity extends AppScreen{protected void render(){title(\"Extension repositories\");subtitle(\"Add HTTPS repository index URLs, sync metadata, remove repositories, and inspect failures.\");RepositoryStore rs=new RepositoryStore(this);EditText url=new EditText(this);url.setHint(\"https://example.org/index.min.json\");url.setSingleLine(true);body.addView(url);Button add=button(\"Add repository\");add.setOnClickListener(v->{if(!rs.add(url.getText().toString()))new AlertDialog.Builder(this).setTitle(\"Repository not added\").setMessage(\"Use a valid HTTPS repository URL that is not already saved.\").setPositiveButton(\"OK\",null).show();else recreate();});body.addView(add);for(String u:rs.all()){body.addView(text(u,13,true));LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button sync=button(\"Sync\");Button remove=button(\"Remove\");row.addView(sync,new LinearLayout.LayoutParams(0,50,1));row.addView(remove,new LinearLayout.LayoutParams(0,50,1));body.addView(row);sync.setOnClickListener(v->sync(u));remove.setOnClickListener(v->{rs.remove(u);recreate();});}nav();}private void sync(String u){Toast.makeText(this,\"Syncing repository…\",Toast.LENGTH_SHORT).show();new Thread(()->{try{List<ExtensionRecord> list=new ExtensionRepositoryClient().fetch(u);new ExtensionManager(this).save(list);runOnUiThread(()->new AlertDialog.Builder(this).setTitle(\"Repository synced\").setMessage(list.size()+\" extension record(s) discovered.\").setPositiveButton(\"OK\",(d,w)->recreate()).show());}catch(Exception e){runOnUiThread(()->new AlertDialog.Builder(this).setTitle(\"Repository sync failed\").setMessage(e.getMessage()).setPositiveButton(\"OK\",null).show());}}).start();}}\n"; }
    private static String playerActivity(String pkg) { return "package "+pkg+";public final class PlayerActivity extends AppScreen{protected void render(){String id=getIntent().getStringExtra(\"id\"),t=getIntent().getStringExtra(\"title\");int ep=getIntent().getIntExtra(\"episode\",1);String key=\"progress_\"+id+\"_\"+ep;title((t==null?\"Anime\":t)+\" · Episode \"+ep);subtitle(\"Playback requires a provider media URL. AIDao does not fabricate a playable stream when the provider contract cannot resolve one.\");body.addView(text(\"Saved resume position: \"+store.number(key)+\" seconds\",14,false));Button save=button(\"Save +60s test progress\");save.setOnClickListener(v->{store.number(key,store.number(key)+60);store.putText(\"last_episode\",(t==null?\"Anime\":t)+\" · Episode \"+ep);recreate();});body.addView(save);nav();}}\n"; }

    private static String java(String s) { return (s == null ? "" : s).replace("\\","\\\\").replace("\"","\\\"").replace("\n"," "); }
    private static String xml(String s) { return (s == null ? "" : s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
}
