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
            if (file.path.endsWith("/MainActivity.java") || file.path.endsWith("/ProvidersActivity.java")) continue;
            out.add(file);
        }

        String javaPath = packageName.replace('.', '/');
        add(out, javaPath, "BuiltInProviderCatalog.java", builtInCatalog(packageName, candidates),
                "Bundle reviewed provider metadata selected during capability research");
        add(out, javaPath, "JikanCatalogProvider.java", jikanProvider(packageName),
                "Provide real public anime catalog search through Jikan v4");
        add(out, javaPath, "AniListCatalogProvider.java", aniListProvider(packageName),
                "Provide redundant real anime catalog search through AniList GraphQL");
        add(out, javaPath, "MainActivity.java", mainActivity(packageName),
                "Search bundled providers plus enabled repository providers");
        add(out, javaPath, "ProvidersActivity.java", providersActivity(packageName),
                "Show built-in providers and repository extension lifecycle");

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
        return new Result(projectName, packageName, out, notes);
    }

    private static boolean hasSuffix(List<GeneratedProject.FileEntry> files, String suffix) {
        for (GeneratedProject.FileEntry f : files) if (f != null && f.path != null && f.path.endsWith(suffix)) return true;
        return false;
    }

    private static void add(List<GeneratedProject.FileEntry> out, String path, String name, String content, String hint) {
        out.add(new GeneratedProject.FileEntry("app/src/main/java/" + path + "/" + name, content, hint));
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
                "public static String provenance(){return \"Jikan v4 (MIT) + AniList GraphQL (API terms) · catalog metadata only\";}" +
                "}\n";
    }

    private static String jikanProvider(String pkg) {
        return "package " + pkg + ";\n" +
                "import java.net.*;import java.io.*;import java.util.*;import org.json.*;\n" +
                "public final class JikanCatalogProvider implements MediaProvider{" +
                "public String id(){return \"builtin.jikan.catalog\";}" +
                "public String displayName(){return \"Jikan Catalog\";}" +
                "public String health(){return \"Built in · catalog metadata · Jikan v4\";}" +
                "public List<AnimeItem> search(String q)throws Exception{" +
                "String query=q==null?\"\":q.trim();if(query.length()==0)return new ArrayList<>();" +
                "String target=\"https://api.jikan.moe/v4/anime?q=\"+URLEncoder.encode(query,\"UTF-8\")+\"&limit=20\";" +
                "HttpURLConnection c=(HttpURLConnection)new URL(target).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty(\"User-Agent\",\"AIDao-Generated-App/1.0\");" +
                "int code=c.getResponseCode();if(code!=200)throw new IOException(\"Jikan HTTP \"+code);" +
                "BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null&&b.length()<1000000)b.append(line);r.close();" +
                "JSONObject root=new JSONObject(b.toString());JSONArray data=root.optJSONArray(\"data\");List<AnimeItem> out=new ArrayList<>();if(data==null)return out;" +
                "for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o==null)continue;String title=o.optString(\"title_english\",\"\");if(title.length()==0||\"null\".equalsIgnoreCase(title))title=o.optString(\"title\",\"\");if(title.length()==0)continue;String id=String.valueOf(o.optInt(\"mal_id\",i));String summary=o.optString(\"synopsis\",\"\");int episodes=o.optInt(\"episodes\",0);out.add(new AnimeItem(id,title,summary,id(),episodes));}" +
                "return out;}}\n";
    }

    private static String aniListProvider(String pkg) {
        return "package " + pkg + ";\n" +
                "import java.net.*;import java.io.*;import java.util.*;import org.json.*;\n" +
                "public final class AniListCatalogProvider implements MediaProvider{" +
                "public String id(){return \"builtin.anilist.catalog\";}public String displayName(){return \"AniList Catalog\";}public String health(){return \"Built in · catalog metadata · AniList GraphQL\";}" +
                "public List<AnimeItem> search(String q)throws Exception{String query=q==null?\"\":q.trim();if(query.length()==0)return new ArrayList<>();" +
                "HttpURLConnection c=(HttpURLConnection)new URL(\"https://graphql.anilist.co\").openConnection();c.setRequestMethod(\"POST\");c.setDoOutput(true);c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty(\"Content-Type\",\"application/json\");c.setRequestProperty(\"Accept\",\"application/json\");c.setRequestProperty(\"User-Agent\",\"AIDao-Generated-App/1.0\");" +
                "String gql=\"query($search:String){Page(page:1,perPage:20){media(search:$search,type:ANIME){id episodes description(asHtml:false) title{english romaji}}}}\";JSONObject payload=new JSONObject();payload.put(\"query\",gql);payload.put(\"variables\",new JSONObject().put(\"search\",query));OutputStream os=c.getOutputStream();os.write(payload.toString().getBytes(\"UTF-8\"));os.close();int code=c.getResponseCode();if(code!=200)throw new IOException(\"AniList HTTP \"+code);" +
                "BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null&&b.length()<1000000)b.append(line);r.close();JSONObject root=new JSONObject(b.toString());JSONObject page=root.optJSONObject(\"data\")==null?null:root.optJSONObject(\"data\").optJSONObject(\"Page\");JSONArray media=page==null?null:page.optJSONArray(\"media\");List<AnimeItem> out=new ArrayList<>();if(media==null)return out;" +
                "for(int i=0;i<media.length();i++){JSONObject o=media.optJSONObject(i);if(o==null)continue;JSONObject t=o.optJSONObject(\"title\");String title=t==null?\"\":t.optString(\"english\",\"\");if(title.length()==0||\"null\".equalsIgnoreCase(title))title=t==null?\"\":t.optString(\"romaji\",\"\");if(title.length()==0)continue;String summary=o.optString(\"description\",\"\").replaceAll(\"<[^>]+>\",\"\");out.add(new AnimeItem(String.valueOf(o.optInt(\"id\",i)),title,summary,id(),o.optInt(\"episodes\",0)));}return out;}}\n";
    }

    private static String mainActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.content.*;import android.widget.*;import java.util.*;\n" +
                "public final class MainActivity extends AppScreen{private LinearLayout results;private EditText query;" +
                "protected void render(){title(\"Browse\");subtitle(\"Search reviewed built-in providers immediately. Repository extensions can be added later.\");query=new EditText(this);query.setHint(\"Search anime\");query.setSingleLine(true);body.addView(query);Button s=button(\"Search providers\");body.addView(s);results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);s.setOnClickListener(v->search());subtitle(\"Built in: \"+BuiltInProviderCatalog.provenance());Button repos=button(\"Manage extension repositories\");repos.setOnClickListener(v->AppNavigator.open(this,RepositoriesActivity.class));body.addView(repos);nav();showReady();}" +
                "private void showReady(){results.removeAllViews();results.addView(text(\"Reviewed catalog providers are already installed. Search above, or add more providers from Extensions / Repositories.\",13,false));}" +
                "private void search(){results.removeAllViews();List<MediaProvider> providers=new ArrayList<>(BuiltInProviderCatalog.providers());for(ExtensionRecord x:new ExtensionManager(this).known())if(x.state==ExtensionRecord.State.ENABLED)providers.add(new RepositoryMediaProvider(x));if(providers.isEmpty()){results.addView(text(\"No compatible providers are available.\",13,false));return;}for(MediaProvider p:providers){try{List<AnimeItem> found=p.search(query.getText().toString());for(AnimeItem a:found){Button b=button(a.title+(a.episodes>0?\" · \"+a.episodes+\" episodes\":\"\"));b.setOnClickListener(v->{Intent i=new Intent(this,DetailActivity.class);i.putExtra(\"id\",a.id);i.putExtra(\"title\",a.title);i.putExtra(\"summary\",a.summary);i.putExtra(\"provider\",a.provider);i.putExtra(\"episodes\",a.episodes);startActivity(i);});results.addView(b);}if(found.isEmpty())results.addView(text(p.displayName()+\": no matches\",13,false));}catch(Exception e){results.addView(text(p.displayName()+\" failed: \"+e.getMessage(),13,false));}}}}\n";
    }

    private static String providersActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.widget.*;import java.util.*;\n" +
                "public final class ProvidersActivity extends AppScreen{protected void render(){title(\"Extensions\");subtitle(\"Reviewed built-in providers are ready immediately. Repository providers remain user-manageable and isolated.\");body.addView(text(\"Built in\",18,true));for(MediaProvider p:BuiltInProviderCatalog.providers()){body.addView(text(p.displayName()+\" · ENABLED\",16,true));subtitle(p.health());}body.addView(text(\"Repository extensions\",18,true));List<ExtensionRecord> known=new ExtensionManager(this).known();if(known.isEmpty())subtitle(\"No additional repository extensions synced yet.\");for(ExtensionRecord x:known){body.addView(text(x.name+\" · \"+x.version+\" · \"+x.state,15,true));Button toggle=button(x.state==ExtensionRecord.State.ENABLED?\"Disable\":\"Enable\");toggle.setOnClickListener(v->{ExtensionRecord.State next=x.state==ExtensionRecord.State.ENABLED?ExtensionRecord.State.DISABLED:ExtensionRecord.State.ENABLED;new ExtensionManager(this).setState(x.id,next);recreate();});body.addView(toggle);subtitle(\"Source: \"+x.repoUrl);}Button repos=button(\"Repositories\");repos.setOnClickListener(v->AppNavigator.open(this,RepositoriesActivity.class));body.addView(repos);nav();}}\n";
    }

    private ProviderCapabilityIntegrator() {}
}
