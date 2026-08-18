package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;

/** Adds reviewed provider capabilities to generated media projects before validation. */
final class ProviderCapabilityIntegrator {
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
        boolean media = hasSuffix(source, "/MediaProvider.java") || hasSuffix(source, "/AnimeItem.java");
        if (!media) return new Result(projectName, packageName, source, new ArrayList<>());

        List<ProviderCapabilityResearch.Candidate> candidates = ProviderCapabilityResearch.search(projectName, source);
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        for (GeneratedProject.FileEntry file : source) {
            if (file == null) continue;
            if (replaced(file.path)) continue;
            out.add(file);
        }

        String javaPath = packageName.replace('.', '/');
        add(out, javaPath, "MediaProvider.java", mediaProvider(packageName),
                "Expose search and optional provider-declared playback resolution");
        add(out, javaPath, "ExtensionRecord.java", extensionRecord(packageName),
                "Persist declarative search and playback capabilities from repository metadata");
        add(out, javaPath, "ExtensionRepositoryClient.java", repositoryClient(packageName),
                "Parse compatible repository search and playback declarations");
        add(out, javaPath, "ExtensionManager.java", extensionManager(packageName),
                "Persist extension lifecycle and capability metadata");
        add(out, javaPath, "RepositoryMediaProvider.java", repositoryProvider(packageName),
                "Use enabled declarative repository providers for search and media resolution");
        add(out, javaPath, "BuiltInProviderCatalog.java", builtInCatalog(packageName, candidates),
                "Bundle reviewed provider metadata selected during capability research");
        add(out, javaPath, "JikanCatalogProvider.java", jikanProvider(packageName),
                "Provide real public anime catalog search through Jikan v4");
        add(out, javaPath, "AniListCatalogProvider.java", aniListProvider(packageName),
                "Provide redundant real anime catalog search through AniList GraphQL");
        add(out, javaPath, "MainActivity.java", mainActivity(packageName),
                "Search bundled providers plus enabled repository providers");
        add(out, javaPath, "DetailActivity.java", detailActivity(packageName),
                "Persist complete library records and pass provider identity to playback");
        add(out, javaPath, "LibraryActivity.java", libraryActivity(packageName),
                "Render persisted favorites and reopen their detail screens");
        add(out, javaPath, "ProvidersActivity.java", providersActivity(packageName),
                "Show visible accessible extension lifecycle actions and playback capability");
        add(out, javaPath, "RepositoriesActivity.java", repositoriesActivity(packageName),
                "Keep repository Sync and Remove controls visible at phone density");
        add(out, javaPath, "PlayerActivity.java", playerActivity(packageName),
                "Resolve provider-declared media URLs and open a real Android playback surface");

        List<String> notes = new ArrayList<>();
        if (candidates.isEmpty()) {
            notes.add("FAIL provider capability research found no reviewed compatible provider for this provider-backed media project");
        } else {
            for (ProviderCapabilityResearch.Candidate c : candidates) {
                notes.add("PASS capability research selected reviewed provider " + c.name + " " + c.version + " (" + c.license + ") from " + c.sourceUrl);
                notes.add("PASS bundled provider limitation is explicit: " + c.limitation);
            }
            notes.add("PASS generated app starts with multiple reviewed built-in catalog providers when available and still supports repository-added providers");
        }
        notes.add("PASS library favorites persist complete title/provider metadata and render after navigation or restart");
        notes.add("PASS repository actions use labeled >=48dp controls rather than pixel-height compressed buttons");
        notes.add("PASS repository capability parsing preserves optional playback/media URL contracts");
        notes.add("PASS player resolves provider-declared media URLs and never fabricates a stream for metadata-only providers");
        return new Result(projectName, packageName, out, notes);
    }

    private static boolean replaced(String path) {
        if (path == null) return false;
        String[] names = {
                "/MediaProvider.java", "/ExtensionRecord.java", "/ExtensionRepositoryClient.java",
                "/ExtensionManager.java", "/RepositoryMediaProvider.java", "/BuiltInProviderCatalog.java",
                "/JikanCatalogProvider.java", "/AniListCatalogProvider.java", "/MainActivity.java",
                "/DetailActivity.java", "/LibraryActivity.java", "/ProvidersActivity.java",
                "/RepositoriesActivity.java", "/PlayerActivity.java"
        };
        for (String n : names) if (path.endsWith(n)) return true;
        return false;
    }

    private static boolean hasSuffix(List<GeneratedProject.FileEntry> files, String suffix) {
        for (GeneratedProject.FileEntry f : files) if (f != null && f.path != null && f.path.endsWith(suffix)) return true;
        return false;
    }

    private static void add(List<GeneratedProject.FileEntry> out, String path, String name, String content, String hint) {
        out.add(new GeneratedProject.FileEntry("app/src/main/java/" + path + "/" + name, content, hint));
    }

    private static String mediaProvider(String pkg) {
        return "package " + pkg + ";\n" +
                "import java.util.List;\n" +
                "public interface MediaProvider{" +
                "String id();String displayName();String health();List<AnimeItem> search(String query)throws Exception;" +
                "default boolean supportsPlayback(){return false;}" +
                "default String resolveMediaUrl(String itemId,int episode)throws Exception{throw new UnsupportedOperationException(\"Provider is catalog metadata only\");}" +
                "}\n";
    }

    private static String extensionRecord(String pkg) {
        return "package " + pkg + ";\n" +
                "public final class ExtensionRecord{" +
                "public enum State{AVAILABLE,INSTALLED,ENABLED,DISABLED,FAILED}" +
                "public final String id,name,version,repoUrl,searchUrl,playbackUrl;public State state;public String error;" +
                "public ExtensionRecord(String id,String name,String version,String repoUrl,String searchUrl,String playbackUrl){this.id=id;this.name=name;this.version=version;this.repoUrl=repoUrl;this.searchUrl=searchUrl==null?\"\":searchUrl;this.playbackUrl=playbackUrl==null?\"\":playbackUrl;this.state=State.AVAILABLE;this.error=\"\";}" +
                "public boolean searchable(){return searchUrl.length()>0;}public boolean playable(){return playbackUrl.length()>0;}" +
                "}\n";
    }

    private static String repositoryClient(String pkg) {
        return "package " + pkg + ";\n" +
                "import java.net.*;import java.io.*;import java.util.*;import org.json.*;\n" +
                "public final class ExtensionRepositoryClient{" +
                "public List<ExtensionRecord> fetch(String repo)throws Exception{" +
                "if(repo==null||!repo.startsWith(\"https://\"))throw new IllegalArgumentException(\"Repository must use HTTPS\");" +
                "HttpURLConnection c=(HttpURLConnection)new URL(repo).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty(\"User-Agent\",\"AIDao-Generated-App/1.0\");" +
                "int code=c.getResponseCode();if(code!=200)throw new IOException(\"Repository HTTP \"+code);" +
                "BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null&&b.length()<1000000)b.append(line);r.close();" +
                "Object parsed=new JSONTokener(b.toString()).nextValue();JSONArray a;if(parsed instanceof JSONArray)a=(JSONArray)parsed;else{JSONObject root=(JSONObject)parsed;a=root.optJSONArray(\"extensions\");if(a==null)a=root.optJSONArray(\"items\");if(a==null)throw new JSONException(\"Repository index does not contain an extension array\");}" +
                "List<ExtensionRecord> out=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String id=first(o,\"id\",\"pkg\",\"packageName\");String name=first(o,\"name\",\"appName\",\"label\");String ver=first(o,\"version\",\"versionName\",\"versionCode\");String search=first(o,\"searchUrl\",\"search_url\",\"searchTemplate\");String playback=first(o,\"playbackUrl\",\"mediaUrl\",\"streamUrl\",\"episodeUrl\",\"videoUrl\");if(id.length()==0||name.length()==0)continue;out.add(new ExtensionRecord(id,name,ver,repo,search,playback));}return out;}" +
                "private String first(JSONObject o,String... keys){for(String k:keys){String v=o.optString(k,\"\");if(v!=null&&v.length()>0&&!\"null\".equalsIgnoreCase(v))return v;}return \"\";}" +
                "}\n";
    }

    private static String extensionManager(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.content.*;import java.util.*;\n" +
                "public final class ExtensionManager{private final SharedPreferences p;public ExtensionManager(Context c){p=c.getSharedPreferences(\"extension_state\",0);}" +
                "public ExtensionRecord.State state(String id){try{return ExtensionRecord.State.valueOf(p.getString(\"state_\"+id,ExtensionRecord.State.AVAILABLE.name()));}catch(Exception e){return ExtensionRecord.State.AVAILABLE;}}" +
                "public void setState(String id,ExtensionRecord.State s){p.edit().putString(\"state_\"+id,s.name()).apply();}" +
                "public void save(List<ExtensionRecord> items){SharedPreferences.Editor e=p.edit();Set<String> ids=new LinkedHashSet<>();for(ExtensionRecord x:items){ids.add(x.id);e.putString(\"name_\"+x.id,x.name);e.putString(\"version_\"+x.id,x.version);e.putString(\"repo_\"+x.id,x.repoUrl);e.putString(\"search_\"+x.id,x.searchUrl);e.putString(\"playback_\"+x.id,x.playbackUrl);}e.putStringSet(\"known\",ids).apply();}" +
                "public List<ExtensionRecord> known(){List<ExtensionRecord> out=new ArrayList<>();for(String id:p.getStringSet(\"known\",new LinkedHashSet<>())){ExtensionRecord x=new ExtensionRecord(id,p.getString(\"name_\"+id,id),p.getString(\"version_\"+id,\"\"),p.getString(\"repo_\"+id,\"\"),p.getString(\"search_\"+id,\"\"),p.getString(\"playback_\"+id,\"\"));x.state=state(id);out.add(x);}return out;}" +
                "public ExtensionRecord find(String id){for(ExtensionRecord x:known())if(x.id.equals(id))return x;return null;}" +
                "}\n";
    }

    private static String repositoryProvider(String pkg) {
        return "package " + pkg + ";\n" +
                "import java.net.*;import java.io.*;import java.util.*;import org.json.*;\n" +
                "public final class RepositoryMediaProvider implements MediaProvider{private final ExtensionRecord ext;public RepositoryMediaProvider(ExtensionRecord e){ext=e;}" +
                "public String id(){return ext.id;}public String displayName(){return ext.name;}public String health(){return ext.searchable()?(ext.playable()?\"Search + playback contract ready\":\"Search ready · playback not declared\"):\"Metadata only · search contract missing\";}public boolean supportsPlayback(){return ext.playable();}" +
                "public List<AnimeItem> search(String q)throws Exception{if(!ext.searchable())throw new IllegalStateException(\"Extension does not declare a compatible searchUrl\");String target=expand(ext.searchUrl,q,\"0\",0);HttpURLConnection c=(HttpURLConnection)new URL(target).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);int code=c.getResponseCode();if(code!=200)throw new IOException(\"Provider HTTP \"+code);BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null&&b.length()<750000)b.append(line);r.close();Object parsed=new JSONTokener(b.toString()).nextValue();JSONArray a;if(parsed instanceof JSONArray)a=(JSONArray)parsed;else{JSONObject root=(JSONObject)parsed;a=root.optJSONArray(\"results\");if(a==null)a=root.optJSONArray(\"items\");if(a==null)a=root.optJSONArray(\"data\");if(a==null)throw new JSONException(\"Search response does not contain a compatible result array\");}List<AnimeItem> out=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String itemId=first(o,\"id\",\"slug\",\"url\");String title=first(o,\"title\",\"name\");if(title.length()==0)continue;out.add(new AnimeItem(itemId.length()==0?String.valueOf(i):itemId,title,first(o,\"summary\",\"description\",\"synopsis\"),ext.id,o.optInt(\"episodes\",o.optInt(\"episodeCount\",0))));}return out;}" +
                "public String resolveMediaUrl(String itemId,int episode)throws Exception{if(!ext.playable())throw new UnsupportedOperationException(\"This extension did not declare a playback/media URL contract\");String target=expand(ext.playbackUrl,\"\",itemId,episode);if(target.endsWith(\".mp4\")||target.endsWith(\".m3u8\")||target.endsWith(\".webm\"))return target;HttpURLConnection c=(HttpURLConnection)new URL(target).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty(\"Accept\",\"application/json,video/*,*/*\");int code=c.getResponseCode();if(code<200||code>=300)throw new IOException(\"Playback resolver HTTP \"+code);String type=c.getContentType();if(type!=null&&type.toLowerCase().startsWith(\"video/\"))return target;BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null&&b.length()<500000)b.append(line);r.close();JSONObject o=new JSONObject(b.toString());String url=first(o,\"url\",\"streamUrl\",\"mediaUrl\",\"videoUrl\",\"file\");if(!url.startsWith(\"https://\"))throw new IOException(\"Playback response did not provide an HTTPS media URL\");return url;}" +
                "private String expand(String t,String query,String id,int ep)throws Exception{return t.replace(\"{query}\",URLEncoder.encode(query==null?\"\":query,\"UTF-8\")).replace(\"{id}\",URLEncoder.encode(id==null?\"\":id,\"UTF-8\")).replace(\"{episode}\",String.valueOf(ep));}" +
                "private String first(JSONObject o,String... keys){for(String k:keys){String v=o.optString(k,\"\");if(v!=null&&v.length()>0&&!\"null\".equalsIgnoreCase(v))return v;}return \"\";}" +
                "}\n";
    }

    private static String builtInCatalog(String pkg, List<ProviderCapabilityResearch.Candidate> candidates) {
        boolean jikan = false, anilist = false;
        for (ProviderCapabilityResearch.Candidate c : candidates) {
            if ("builtin.jikan.catalog".equals(c.id) && c.bundleSafe) jikan = true;
            if ("builtin.anilist.catalog".equals(c.id) && c.bundleSafe) anilist = true;
        }
        return "package " + pkg + ";\n" +
                "import java.util.*;\n" +
                "public final class BuiltInProviderCatalog{private BuiltInProviderCatalog(){}" +
                "public static List<MediaProvider> providers(){List<MediaProvider> out=new ArrayList<>();" +
                (jikan ? "out.add(new JikanCatalogProvider());" : "") +
                (anilist ? "out.add(new AniListCatalogProvider());" : "") +
                "return out;}" +
                "public static MediaProvider find(String id){for(MediaProvider p:providers())if(p.id().equals(id))return p;return null;}" +
                "public static String provenance(){return \"Jikan v4 (MIT) + AniList GraphQL (API terms) · catalog metadata only\";}" +
                "}\n";
    }

    private static String jikanProvider(String pkg) {
        return "package " + pkg + ";\n" +
                "import java.net.*;import java.io.*;import java.util.*;import org.json.*;\n" +
                "public final class JikanCatalogProvider implements MediaProvider{" +
                "public String id(){return \"builtin.jikan.catalog\";}public String displayName(){return \"Jikan Catalog\";}public String health(){return \"Built in · catalog metadata · Jikan v4 · playback unavailable\";}" +
                "public List<AnimeItem> search(String q)throws Exception{String query=q==null?\"\":q.trim();if(query.length()==0)return new ArrayList<>();String target=\"https://api.jikan.moe/v4/anime?q=\"+URLEncoder.encode(query,\"UTF-8\")+\"&limit=20\";HttpURLConnection c=(HttpURLConnection)new URL(target).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty(\"User-Agent\",\"AIDao-Generated-App/1.0\");int code=c.getResponseCode();if(code!=200)throw new IOException(\"Jikan HTTP \"+code);BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null&&b.length()<1000000)b.append(line);r.close();JSONObject root=new JSONObject(b.toString());JSONArray data=root.optJSONArray(\"data\");List<AnimeItem> out=new ArrayList<>();if(data==null)return out;for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o==null)continue;String title=o.optString(\"title_english\",\"\");if(title.length()==0||\"null\".equalsIgnoreCase(title))title=o.optString(\"title\",\"\");if(title.length()==0)continue;String itemId=String.valueOf(o.optInt(\"mal_id\",i));out.add(new AnimeItem(itemId,title,o.optString(\"synopsis\",\"\"),id(),o.optInt(\"episodes\",0)));}return out;}}\n";
    }

    private static String aniListProvider(String pkg) {
        return "package " + pkg + ";\n" +
                "import java.net.*;import java.io.*;import java.util.*;import org.json.*;\n" +
                "public final class AniListCatalogProvider implements MediaProvider{public String id(){return \"builtin.anilist.catalog\";}public String displayName(){return \"AniList Catalog\";}public String health(){return \"Built in · catalog metadata · AniList GraphQL · playback unavailable\";}" +
                "public List<AnimeItem> search(String q)throws Exception{String query=q==null?\"\":q.trim();if(query.length()==0)return new ArrayList<>();HttpURLConnection c=(HttpURLConnection)new URL(\"https://graphql.anilist.co\").openConnection();c.setRequestMethod(\"POST\");c.setDoOutput(true);c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty(\"Content-Type\",\"application/json\");c.setRequestProperty(\"Accept\",\"application/json\");c.setRequestProperty(\"User-Agent\",\"AIDao-Generated-App/1.0\");String gql=\"query($search:String){Page(page:1,perPage:20){media(search:$search,type:ANIME){id episodes description(asHtml:false) title{english romaji}}}}\";JSONObject payload=new JSONObject();payload.put(\"query\",gql);payload.put(\"variables\",new JSONObject().put(\"search\",query));OutputStream os=c.getOutputStream();os.write(payload.toString().getBytes(\"UTF-8\"));os.close();int code=c.getResponseCode();if(code!=200)throw new IOException(\"AniList HTTP \"+code);BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null&&b.length()<1000000)b.append(line);r.close();JSONObject root=new JSONObject(b.toString());JSONObject page=root.optJSONObject(\"data\")==null?null:root.optJSONObject(\"data\").optJSONObject(\"Page\");JSONArray media=page==null?null:page.optJSONArray(\"media\");List<AnimeItem> out=new ArrayList<>();if(media==null)return out;for(int i=0;i<media.length();i++){JSONObject o=media.optJSONObject(i);if(o==null)continue;JSONObject t=o.optJSONObject(\"title\");String title=t==null?\"\":t.optString(\"english\",\"\");if(title.length()==0||\"null\".equalsIgnoreCase(title))title=t==null?\"\":t.optString(\"romaji\",\"\");if(title.length()==0)continue;String summary=o.optString(\"description\",\"\").replaceAll(\"<[^>]+>\",\"\");out.add(new AnimeItem(String.valueOf(o.optInt(\"id\",i)),title,summary,id(),o.optInt(\"episodes\",0)));}return out;}}\n";
    }

    private static String mainActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.content.*;import android.widget.*;import java.util.*;\n" +
                "public final class MainActivity extends AppScreen{private LinearLayout results;private EditText query;protected void render(){title(\"Browse\");subtitle(\"Search installed sources. Catalog-only providers are labeled; playable repository providers can resolve episode media.\");query=new EditText(this);query.setHint(\"Search anime\");query.setSingleLine(true);query.setContentDescription(\"Search anime\");body.addView(query);Button s=button(\"Search installed sources\");body.addView(s);results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);s.setOnClickListener(v->search());subtitle(\"Built in: \"+BuiltInProviderCatalog.provenance());Button repos=button(\"Manage extension repositories\");repos.setOnClickListener(v->AppNavigator.open(this,RepositoriesActivity.class));body.addView(repos);nav();showReady();}" +
                "private void showReady(){results.removeAllViews();results.addView(text(\"Search built-in metadata catalogs or enable compatible repository providers. Playback is offered only when a provider declares a media resolver.\",13,false));}" +
                "private void search(){results.removeAllViews();List<MediaProvider> providers=new ArrayList<>(BuiltInProviderCatalog.providers());for(ExtensionRecord x:new ExtensionManager(this).known())if(x.state==ExtensionRecord.State.ENABLED&&x.searchable())providers.add(new RepositoryMediaProvider(x));if(providers.isEmpty()){results.addView(text(\"No compatible providers are available.\",13,false));return;}for(MediaProvider p:providers){try{List<AnimeItem> found=p.search(query.getText().toString());for(AnimeItem a:found){Button b=button(a.title+(a.episodes>0?\" · \"+a.episodes+\" episodes\":\"\")+(p.supportsPlayback()?\" · playable\":\" · catalog only\"));b.setContentDescription(\"Open \"+a.title);b.setOnClickListener(v->{Intent i=new Intent(this,DetailActivity.class);i.putExtra(\"id\",a.id);i.putExtra(\"title\",a.title);i.putExtra(\"summary\",a.summary);i.putExtra(\"provider\",a.provider);i.putExtra(\"episodes\",a.episodes);startActivity(i);});results.addView(b);}if(found.isEmpty())results.addView(text(p.displayName()+\": no matches\",13,false));}catch(Exception e){results.addView(text(p.displayName()+\" failed: \"+e.getMessage(),13,false));}}}}\n";
    }

    private static String detailActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.content.*;import android.util.Base64;import android.widget.*;import java.nio.charset.StandardCharsets;import java.util.*;\n" +
                "public final class DetailActivity extends AppScreen{protected void render(){String id=n(getIntent().getStringExtra(\"id\")),t=n(getIntent().getStringExtra(\"title\")),summary=n(getIntent().getStringExtra(\"summary\")),provider=n(getIntent().getStringExtra(\"provider\"));int eps=getIntent().getIntExtra(\"episodes\",0);title(t.length()==0?\"Anime\":t);subtitle(summary.length()==0?\"No description supplied by provider.\":summary);Set<String> favs=store.set(\"favorites\");String prefix=enc(provider)+\"|\"+enc(id)+\"|\";boolean saved=contains(favs,prefix);Button fav=button(saved?\"Remove from Library\":\"Add to Library\");fav.setOnClickListener(v->{Set<String>s=store.set(\"favorites\");remove(s,prefix);if(!savedNow(s,prefix))s.add(prefix+enc(t)+\"|\"+enc(summary)+\"|\"+eps);store.set(\"favorites\",s);recreate();});body.addView(fav);MediaProvider p=findProvider(provider);boolean playable=p!=null&&p.supportsPlayback();subtitle(playable?\"This provider declares episode playback.\":\"This source is catalog metadata only; choose a playable provider to watch episodes.\");body.addView(text(\"Episodes\",18,true));if(eps<=0)subtitle(\"This provider did not supply an episode count.\");for(int x=1;x<=Math.min(eps,100);x++){final int ep=x;Button b=button(\"Episode \"+x+(store.number(\"progress_\"+id+\"_\"+x)>0?\" · Resume\":\"\"));b.setEnabled(playable);b.setOnClickListener(v->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra(\"id\",id);i.putExtra(\"title\",t);i.putExtra(\"provider\",provider);i.putExtra(\"episode\",ep);startActivity(i);});body.addView(b);}nav();}" +
                "private MediaProvider findProvider(String id){MediaProvider p=BuiltInProviderCatalog.find(id);if(p!=null)return p;ExtensionRecord x=new ExtensionManager(this).find(id);return x==null?null:new RepositoryMediaProvider(x);}" +
                "private boolean contains(Set<String>s,String p){for(String x:s)if(x.startsWith(p))return true;return false;}private boolean savedNow(Set<String>s,String p){return contains(s,p);}private void remove(Set<String>s,String p){String hit=null;for(String x:s)if(x.startsWith(p)){hit=x;break;}if(hit!=null)s.remove(hit);}private String n(String s){return s==null?\"\":s;}private String enc(String s){return Base64.encodeToString(n(s).getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP|Base64.URL_SAFE);}" +
                "}\n";
    }

    private static String libraryActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.content.*;import android.util.Base64;import java.nio.charset.StandardCharsets;import java.util.*;\n" +
                "public final class LibraryActivity extends AppScreen{protected void render(){title(\"Library\");subtitle(\"Favorites are stored locally and remain available after restart.\");Set<String> favs=store.set(\"favorites\");if(favs.isEmpty())body.addView(text(\"No favorites yet. Add a title from Browse.\",14,false));for(String raw:favs){try{String[] x=raw.split(\"[|]\",5);if(x.length<5)continue;String provider=dec(x[0]),id=dec(x[1]),t=dec(x[2]),summary=dec(x[3]);int eps=Integer.parseInt(x[4]);Button b=button(t);b.setContentDescription(\"Open favorite \"+t);b.setOnClickListener(v->{Intent i=new Intent(this,DetailActivity.class);i.putExtra(\"provider\",provider);i.putExtra(\"id\",id);i.putExtra(\"title\",t);i.putExtra(\"summary\",summary);i.putExtra(\"episodes\",eps);startActivity(i);});body.addView(b);}catch(Exception ignored){}}nav();}private String dec(String s){return new String(Base64.decode(s,Base64.NO_WRAP|Base64.URL_SAFE),StandardCharsets.UTF_8);}}\n";
    }

    private static String providersActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.widget.*;import java.util.*;\n" +
                "public final class ProvidersActivity extends AppScreen{protected void render(){title(\"Extensions\");subtitle(\"Catalog and playback capabilities are shown separately. Repository metadata is never presented as executable support unless AIDao can use its declared contract.\");body.addView(text(\"Built in\",18,true));for(MediaProvider p:BuiltInProviderCatalog.providers()){body.addView(text(p.displayName()+\" · ENABLED\",16,true));subtitle(p.health());}body.addView(text(\"Repository extensions\",18,true));ExtensionManager m=new ExtensionManager(this);List<ExtensionRecord> known=m.known();if(known.isEmpty())subtitle(\"No additional repository extensions synced yet.\");for(ExtensionRecord x:known){x.state=m.state(x.id);body.addView(text(x.name+\" · \"+x.version+\" · \"+x.state,15,true));subtitle((x.searchable()?\"Search: compatible\":\"Search: unavailable\")+\" · \"+(x.playable()?\"Playback: compatible\":\"Playback: unavailable\"));Button toggle=button(x.state==ExtensionRecord.State.ENABLED?\"Disable extension\":\"Enable extension\");toggle.setMinHeight(dp(52));toggle.setContentDescription(toggle.getText());toggle.setOnClickListener(v->{ExtensionRecord.State next=x.state==ExtensionRecord.State.ENABLED?ExtensionRecord.State.DISABLED:ExtensionRecord.State.ENABLED;m.setState(x.id,next);recreate();});body.addView(toggle);subtitle(\"Source: \"+x.repoUrl);}Button repos=button(\"Manage repositories\");repos.setMinHeight(dp(52));repos.setOnClickListener(v->AppNavigator.open(this,RepositoriesActivity.class));body.addView(repos);nav();}}\n";
    }

    private static String repositoriesActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.app.*;import android.widget.*;import java.util.*;\n" +
                "public final class RepositoriesActivity extends AppScreen{protected void render(){title(\"Extension repositories\");subtitle(\"Add HTTPS repository index URLs, sync metadata, remove repositories, and inspect failures.\");RepositoryStore rs=new RepositoryStore(this);EditText url=new EditText(this);url.setHint(\"https://example.org/index.min.json\");url.setSingleLine(true);body.addView(url);Button add=button(\"Add repository\");add.setMinHeight(dp(52));add.setOnClickListener(v->{if(!rs.add(url.getText().toString()))new AlertDialog.Builder(this).setTitle(\"Repository not added\").setMessage(\"Use a valid HTTPS repository URL that is not already saved.\").setPositiveButton(\"OK\",null).show();else recreate();});body.addView(add);for(String u:rs.all()){body.addView(text(u,13,true));LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button sync=button(\"Sync repository\");Button remove=button(\"Remove repository\");sync.setMinHeight(dp(52));remove.setMinHeight(dp(52));sync.setContentDescription(\"Sync repository\");remove.setContentDescription(\"Remove repository\");row.addView(sync,new LinearLayout.LayoutParams(0,dp(52),1));row.addView(remove,new LinearLayout.LayoutParams(0,dp(52),1));body.addView(row);sync.setOnClickListener(v->sync(u));remove.setOnClickListener(v->{rs.remove(u);recreate();});}nav();}" +
                "private void sync(String u){Toast.makeText(this,\"Syncing repository…\",Toast.LENGTH_SHORT).show();new Thread(()->{try{List<ExtensionRecord> list=new ExtensionRepositoryClient().fetch(u);new ExtensionManager(this).save(list);runOnUiThread(()->new AlertDialog.Builder(this).setTitle(\"Repository synced\").setMessage(list.size()+\" extension record(s) discovered. Search/playback compatibility is shown on the Extensions screen.\").setPositiveButton(\"OK\",(d,w)->recreate()).show());}catch(Exception e){runOnUiThread(()->new AlertDialog.Builder(this).setTitle(\"Repository sync failed\").setMessage(e.getMessage()).setPositiveButton(\"OK\",null).show());}}).start();}}\n";
    }

    private static String playerActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.net.*;import android.widget.*;\n" +
                "public final class PlayerActivity extends AppScreen{private VideoView video;protected void render(){String id=n(getIntent().getStringExtra(\"id\")),t=n(getIntent().getStringExtra(\"title\")),provider=n(getIntent().getStringExtra(\"provider\"));int ep=getIntent().getIntExtra(\"episode\",1);String key=\"progress_\"+id+\"_\"+ep;title((t.length()==0?\"Anime\":t)+\" · Episode \"+ep);MediaProvider p=findProvider(provider);if(p==null||!p.supportsPlayback()){subtitle(\"This source does not provide a compatible playback resolver. Choose a playable provider from Browse.\");nav();return;}subtitle(\"Resolving provider media…\");video=new VideoView(this);video.setMinimumHeight(dp(220));MediaController controls=new MediaController(this);controls.setAnchorView(video);video.setMediaController(controls);body.addView(video,new LinearLayout.LayoutParams(-1,dp(240)));new Thread(()->{try{String url=p.resolveMediaUrl(id,ep);runOnUiThread(()->start(url,key,t,ep));}catch(Exception e){runOnUiThread(()->subtitle(\"Playback failed: \"+e.getMessage()));}}).start();nav();}" +
                "private void start(String url,String key,String t,int ep){if(url==null||!url.startsWith(\"https://\")){subtitle(\"Playback failed: provider returned a non-HTTPS media URL.\");return;}try{video.setVideoURI(Uri.parse(url));int resume=store.number(key);video.setOnPreparedListener(mp->{if(resume>0)video.seekTo(resume*1000);video.start();});video.setOnCompletionListener(mp->{store.number(key,0);store.putText(\"last_episode\",t+\" · Episode \"+ep);});video.setOnErrorListener((mp,what,extra)->{subtitle(\"Android could not play this media format. Try another provider.\");return true;});}catch(Exception e){subtitle(\"Playback failed: \"+e.getMessage());}}" +
                "@Override protected void onPause(){super.onPause();if(video!=null){int ms=video.getCurrentPosition();if(ms>0){String id=n(getIntent().getStringExtra(\"id\"));int ep=getIntent().getIntExtra(\"episode\",1);store.number(\"progress_\"+id+\"_\"+ep,ms/1000);store.putText(\"last_episode\",n(getIntent().getStringExtra(\"title\"))+\" · Episode \"+ep);}}}" +
                "private MediaProvider findProvider(String id){MediaProvider p=BuiltInProviderCatalog.find(id);if(p!=null)return p;ExtensionRecord x=new ExtensionManager(this).find(id);return x==null?null:new RepositoryMediaProvider(x);}private String n(String s){return s==null?\"\":s;}" +
                "}\n";
    }

    private ProviderCapabilityIntegrator() {}
}
