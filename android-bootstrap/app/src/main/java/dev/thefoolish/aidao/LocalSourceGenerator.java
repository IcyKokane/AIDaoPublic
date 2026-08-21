package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic, local-only Android source generator.
 *
 * It turns an approved project specification into source text only. It never
 * installs APKs, publishes repositories, uses credentials, spends money, or
 * performs destructive actions. Those remain explicit user-controlled gates.
 */
final class LocalSourceGenerator {
    private enum Domain { MEDIA, FINANCE, TRACKER, SOCIAL, COMMERCE, CONTENT, GENERIC }

    GeneratedProject generate(String projectName, String brief, List<String> requirements, List<String> tasks) {
        String safeName = cleanDisplayName(projectName);
        String slug = slug(projectName);
        String packageName = "dev.thefoolish.generated." + slug;
        String packagePath = packageName.replace('.', '/');
        Domain domain = detectDomain(brief, requirements);
        List<String> screens = screensFor(domain);

        List<GeneratedProject.FileEntry> files = new ArrayList<>();
        files.add(file("settings.gradle.kts", settings(safeName), "Create Android project shell"));
        files.add(file("build.gradle.kts", rootGradle(), "Create Android project shell"));
        files.add(file("app/build.gradle.kts", appGradle(packageName), "Create Android project shell"));
        files.add(file("app/src/main/AndroidManifest.xml", manifest(safeName, screens), "Generate manifest and navigation declarations"));
        files.add(file("app/src/main/res/values/colors.xml", colors(), "Generate Android resources"));
        files.add(file("app/src/main/res/values/strings.xml", strings(safeName), "Generate Android resources"));
        files.add(file("app/src/main/res/values/styles.xml", styles(), "Generate Android resources"));
        files.add(file("app/src/main/java/" + packagePath + "/GeneratedScreen.java", generatedScreen(packageName, safeName), "Create reusable UI architecture"));
        files.add(file("app/src/main/java/" + packagePath + "/LocalStore.java", localStore(packageName), "Create reusable local persistence"));
        files.add(file("app/src/main/java/" + packagePath + "/AppNavigator.java", navigator(packageName), "Wire multi-screen navigation"));

        if (domain == Domain.MEDIA) {
            addMediaProject(files, packageName, packagePath, safeName, brief);
        } else {
            addDomainProject(files, packageName, packagePath, safeName, brief, domain, screens, requirements);
        }

        files.add(file("README.md", readme(safeName, brief, requirements, tasks, domain, screens), "Document generated project"));
        files.add(file(".github/workflows/android.yml", workflow(), "Run Android CI"));

        List<String> verification = verify(packageName, files, domain, screens);
        return new GeneratedProject(safeName, packageName, files, verification);
    }

    private GeneratedProject.FileEntry file(String path, String content, String taskHint) {
        return new GeneratedProject.FileEntry(path, content, taskHint);
    }

    private Domain detectDomain(String brief, List<String> requirements) {
        StringBuilder all = new StringBuilder(brief == null ? "" : brief.toLowerCase(Locale.US));
        if (requirements != null) for (String item : requirements) all.append(' ').append(item == null ? "" : item.toLowerCase(Locale.US));
        String s = all.toString();
        int media=score(s,"anime","episode","stream","video","media provider","playback");
        int finance=score(s,"expense","budget","transaction","spending","finance","ledger");
        int tracker=score(s,"tracker","tracking","activity tracker","workout","exercise","weight and reps","reps","strength","gym","analytics","habit","usage tracker","timeline");
        int social=score(s,"chat","message","friend","dating","social","community","profile","inbox");
        int commerce=score(s,"shop","store","cart","product","checkout","order","marketplace","purchase","catalog");
        int content=score(s,"note","document","article","journal","editor","write","content","draft","library search");
        int max=Math.max(media,Math.max(finance,Math.max(tracker,Math.max(social,Math.max(commerce,content)))));
        if(max==0)return Domain.GENERIC;
        if(media==max)return Domain.MEDIA;
        if(commerce==max)return Domain.COMMERCE;
        if(finance==max)return Domain.FINANCE;
        if(social==max)return Domain.SOCIAL;
        if(tracker==max)return Domain.TRACKER;
        return Domain.CONTENT;
    }

    private int score(String source,String... terms){int n=0;for(String term:terms)if(source.contains(term))n++;return n;}

    private boolean containsAny(String source, String... terms) {
        for (String term : terms) if (source.contains(term)) return true;
        return false;
    }

    private List<String> screensFor(Domain domain) {
        switch (domain) {
            case MEDIA: return Arrays.asList("MainActivity","DetailActivity","LibraryActivity","HistoryActivity","ProvidersActivity","PlayerActivity");
            case FINANCE: return Arrays.asList("MainActivity","TransactionsActivity","BudgetsActivity","ReportsActivity");
            case TRACKER: return Arrays.asList("MainActivity","TimelineActivity","ReportsActivity","DataControlsActivity");
            case SOCIAL: return Arrays.asList("MainActivity","InboxActivity","ProfileActivity","SettingsActivity");
            case COMMERCE: return Arrays.asList("MainActivity","ProductActivity","CartActivity","OrdersActivity");
            case CONTENT: return Arrays.asList("MainActivity","EditorActivity","SearchActivity","LibraryActivity");
            default: return Arrays.asList("MainActivity","ExploreActivity","DetailActivity","SettingsActivity");
        }
    }

    private void addMediaProject(List<GeneratedProject.FileEntry> files,String packageName,String packagePath,String name,String brief) {
        files.add(file("app/src/main/java/"+packagePath+"/AnimeItem.java", animeItem(packageName), "Define anime and episode models"));
        files.add(file("app/src/main/java/"+packagePath+"/MediaProvider.java", mediaProvider(packageName), "Implement provider contracts"));
        files.add(file("app/src/main/java/"+packagePath+"/DemoProvider.java", demoProvider(packageName), "Provide safe provider placeholder data"));
        files.add(file("app/src/main/java/"+packagePath+"/MainActivity.java", mediaCatalog(packageName,name,brief), "Build searchable catalog screen"));
        files.add(file("app/src/main/java/"+packagePath+"/DetailActivity.java", mediaDetail(packageName), "Build anime detail and episode screen"));
        files.add(file("app/src/main/java/"+packagePath+"/LibraryActivity.java", simpleMediaScreen(packageName,"LibraryActivity","Library","Favorites are stored locally. Open an item from Catalog to add or remove it."), "Build library screen"));
        files.add(file("app/src/main/java/"+packagePath+"/HistoryActivity.java", simpleMediaScreen(packageName,"HistoryActivity","History","Playback progress and the last watched episode are stored locally on-device."), "Build history screen"));
        files.add(file("app/src/main/java/"+packagePath+"/ProvidersActivity.java", providerScreen(packageName), "Build provider management screen"));
        files.add(file("app/src/main/java/"+packagePath+"/PlayerActivity.java", playerScreen(packageName), "Build playback/resume screen"));
    }

    private void addDomainProject(List<GeneratedProject.FileEntry> files,String packageName,String packagePath,String name,String brief,Domain domain,List<String> screens,List<String> requirements) {
        files.add(file("app/src/main/java/"+packagePath+"/DomainRecord.java", domainRecord(packageName), "Define project data model"));
        files.add(file("app/src/main/java/"+packagePath+"/MainActivity.java", domainMain(packageName,name,brief,domain,screens,requirements), "Build primary dashboard and navigation"));
        for (int i=1;i<screens.size();i++) {
            String className=screens.get(i);
            String title=className.replace("Activity","").replaceAll("([a-z])([A-Z])","$1 $2");
            files.add(file("app/src/main/java/"+packagePath+"/"+className+".java", domainScreen(packageName,className,title,domainDescription(domain,title)), "Build "+title+" screen"));
        }
    }

    private List<String> verify(String packageName,List<GeneratedProject.FileEntry> files,Domain domain,List<String> screens) {
        List<String> notes=new ArrayList<>();
        String[] required={"settings.gradle.kts","build.gradle.kts","app/build.gradle.kts","app/src/main/AndroidManifest.xml","app/src/main/res/values/colors.xml","app/src/main/res/values/strings.xml","app/src/main/res/values/styles.xml",".github/workflows/android.yml"};
        for(String path:required) notes.add((has(files,path)?"PASS ":"FAIL ")+path);
        String root="app/src/main/java/"+packageName.replace('.','/')+"/";
        notes.add((has(files,root+"GeneratedScreen.java")?"PASS ":"FAIL ")+"reusable screen architecture");
        notes.add((has(files,root+"LocalStore.java")?"PASS ":"FAIL ")+"local persistence");
        notes.add((has(files,root+"AppNavigator.java")?"PASS ":"FAIL ")+"navigation helper");
        for(String screen:screens) notes.add((has(files,root+screen+".java")?"PASS ":"FAIL ")+"screen "+screen);
        if(domain==Domain.MEDIA){
            notes.add((has(files,root+"MediaProvider.java")?"PASS ":"FAIL ")+"provider boundary");
            notes.add((has(files,root+"PlayerActivity.java")?"PASS ":"FAIL ")+"player flow");
        }
        notes.add((files.size()>=16?"PASS ":"FAIL ")+"nontrivial generated source tree ("+files.size()+" files)");
        return notes;
    }

    private boolean has(List<GeneratedProject.FileEntry> files,String path){for(GeneratedProject.FileEntry f:files)if(path.equals(f.path))return true;return false;}

    private String settings(String name){return "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\n"+
            "dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\n"+
            "rootProject.name = \""+escape(name)+"\"\ninclude(\":app\")\n";}
    private String rootGradle(){return "plugins {\n    id(\"com.android.application\") version \"8.7.3\" apply false\n}\n";}
    private String appGradle(String pkg){return "plugins { id(\"com.android.application\") }\n\nandroid {\n"+
            "    namespace = \""+pkg+"\"\n    compileSdk = 35\n"+
            "    defaultConfig { applicationId = \""+pkg+"\"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = \"0.5.0\" }\n"+
            "    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }\n}\n";}

    private String manifest(String name,List<String> screens){
        StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n  <application android:theme=\"@style/AppTheme\" android:label=\"").append(xml(name)).append("\" android:allowBackup=\"true\" android:supportsRtl=\"true\">\n");
        for(String screen:screens){
            if("MainActivity".equals(screen)) b.append("    <activity android:name=\".MainActivity\" android:exported=\"true\">\n      <intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter>\n    </activity>\n");
            else b.append("    <activity android:name=\".").append(screen).append("\" android:exported=\"false\"/>\n");
        }
        return b.append("  </application>\n</manifest>\n").toString();
    }
    private String colors(){return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n  <color name=\"bg\">#121318</color>\n  <color name=\"panel\">#1D1F27</color>\n  <color name=\"accent\">#5094FF</color>\n  <color name=\"muted\">#9DA2B2</color>\n</resources>\n";}
    private String strings(String name){return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources><string name=\"app_name\">"+xml(name)+"</string></resources>\n";}
    private String styles(){return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n  <style name=\"AppTheme\" parent=\"@android:style/Theme.Material.NoActionBar\">\n    <item name=\"android:fontFamily\">sans-serif</item>\n    <item name=\"android:colorAccent\">@color/accent</item>\n    <item name=\"android:navigationBarColor\">#0C0D11</item>\n    <item name=\"android:statusBarColor\">@color/bg</item>\n    <item name=\"android:windowLightStatusBar\">false</item>\n    <item name=\"android:windowLightNavigationBar\">false</item>\n  </style>\n</resources>\n";}

    private String generatedScreen(String pkg,String appName){return "package "+pkg+";\n\n"+
            "import android.app.Activity;import android.graphics.Color;import android.os.Bundle;import android.view.Gravity;import android.widget.*;\n"+
            "public abstract class GeneratedScreen extends Activity { protected static class Button extends android.widget.Button { Button(android.content.Context c){super(c);} } protected LinearLayout body; protected LocalStore store;\n"+
            "  @Override public void onCreate(Bundle b){super.onCreate(b);store=new LocalStore(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(32,48,32,32);root.setBackgroundColor(Color.rgb(18,19,24));root.addView(text(\""+java(appName)+"\",24,true));body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);root.addView(body,new LinearLayout.LayoutParams(-1,-2));ScrollView s=new ScrollView(this);s.addView(root);setContentView(s);render();}\n"+
            "  protected abstract void render(); protected TextView text(String v,int size,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setPadding(0,10,0,10);t.setGravity(Gravity.START);t.setTypeface(android.graphics.Typeface.create(\"sans-serif\",bold?1:0));return t;}\n"+
            "  protected Button action(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setMinHeight(52);return b;} protected void gap(){Space s=new Space(this);body.addView(s,new LinearLayout.LayoutParams(1,16));}\n"+
            "}\n";}
    private String localStore(String pkg){return "package "+pkg+";\n\nimport android.content.Context;import android.content.SharedPreferences;\n"+
            "public final class LocalStore { private final SharedPreferences p; public LocalStore(Context c){p=c.getSharedPreferences(\"generated_app_state\",Context.MODE_PRIVATE);}\n"+
            " public boolean flag(String k){return p.getBoolean(k,false);} public void flag(String k,boolean v){p.edit().putBoolean(k,v).apply();}\n"+
            " public int number(String k){return p.getInt(k,0);} public void number(String k,int v){p.edit().putInt(k,Math.max(0,v)).apply();}\n"+
            " public String text(String k,String d){return p.getString(k,d);} public void putText(String k,String v){p.edit().putString(k,v==null?\"\":v).apply();} }\n";}
    private String navigator(String pkg){return "package "+pkg+";\n\nimport android.app.Activity;import android.content.Intent;\npublic final class AppNavigator { private AppNavigator(){} public static void open(Activity a,Class<? extends Activity> target){a.startActivity(new Intent(a,target));} }\n";}

    private String animeItem(String pkg){return "package "+pkg+";\n\npublic final class AnimeItem { public final String id,title,summary,provider; public final int episodes; public AnimeItem(String id,String title,String summary,String provider,int episodes){this.id=id;this.title=title;this.summary=summary;this.provider=provider;this.episodes=episodes;} }\n";}
    private String mediaProvider(String pkg){return "package "+pkg+";\n\nimport java.util.List;\n/** Replaceable provider boundary. No unverified external source is executed by generated code. */\npublic interface MediaProvider {String id();String displayName();boolean enabled();String health();List<AnimeItem> search(String query) throws Exception;}\n";}
    private String demoProvider(String pkg){return "package "+pkg+";\n\nimport java.util.*;\n/** Safe placeholder provider used until the user explicitly configures a trusted provider implementation. */\npublic final class DemoProvider implements MediaProvider { public String id(){return \"demo\";}public String displayName(){return \"Demo provider\";}public boolean enabled(){return true;}public String health(){return \"Ready (sample data only)\";} public List<AnimeItem> search(String q){List<AnimeItem> all=Arrays.asList(new AnimeItem(\"origin\",\"Origin Path\",\"Sample catalog entry proving catalog/detail/player navigation.\",id(),12),new AnimeItem(\"sky\",\"Sky Archive\",\"Second sample entry for search and library state.\",id(),24));if(q==null||q.trim().isEmpty())return all;List<AnimeItem> out=new ArrayList<>();for(AnimeItem a:all)if(a.title.toLowerCase().contains(q.toLowerCase()))out.add(a);return out;} }\n";}

    private String mediaCatalog(String pkg,String name,String brief){return "package "+pkg+";\n\nimport android.content.Intent;import android.widget.*;import java.util.*;\npublic final class MainActivity extends GeneratedScreen { private final MediaProvider provider=new DemoProvider();\n"+
            " protected void render(){body.addView(text(\"Catalog\",22,true));body.addView(text(\""+java(brief)+"\",14,false));EditText q=new EditText(this);q.setHint(\"Search anime\");body.addView(q);Button search=action(\"Search\");body.addView(search);LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);search.setOnClickListener(v->show(results,q.getText().toString()));show(results,\"\");nav();}\n"+
            " private void show(LinearLayout r,String q){r.removeAllViews();try{for(AnimeItem a:provider.search(q)){Button b=action(a.title+\" · \"+a.episodes+\" episodes\");b.setOnClickListener(v->{Intent i=new Intent(this,DetailActivity.class);i.putExtra(\"id\",a.id);i.putExtra(\"title\",a.title);i.putExtra(\"summary\",a.summary);i.putExtra(\"episodes\",a.episodes);startActivity(i);});r.addView(b);}}catch(Exception e){r.addView(text(\"Provider error: \"+e.getMessage(),14,false));}}\n"+
            " private void nav(){gap();Button lib=action(\"Library\");lib.setOnClickListener(v->AppNavigator.open(this,LibraryActivity.class));body.addView(lib);Button hist=action(\"History\");hist.setOnClickListener(v->AppNavigator.open(this,HistoryActivity.class));body.addView(hist);Button providers=action(\"Providers\");providers.setOnClickListener(v->AppNavigator.open(this,ProvidersActivity.class));body.addView(providers);}\n}\n";}
    private String mediaDetail(String pkg){return "package "+pkg+";\n\nimport android.content.Intent;import android.widget.*;\npublic final class DetailActivity extends GeneratedScreen { protected void render(){String id=getIntent().getStringExtra(\"id\");String title=getIntent().getStringExtra(\"title\");String summary=getIntent().getStringExtra(\"summary\");int episodes=Math.max(1,getIntent().getIntExtra(\"episodes\",12));body.addView(text(title==null?\"Anime detail\":title,22,true));body.addView(text(summary==null?\"\":summary,14,false));Button fav=action(store.flag(\"fav_\"+id)?\"Remove favorite\":\"Add favorite\");fav.setOnClickListener(v->{boolean n=!store.flag(\"fav_\"+id);store.flag(\"fav_\"+id,n);fav.setText(n?\"Remove favorite\":\"Add favorite\");});body.addView(fav);body.addView(text(\"Episodes\",18,true));for(int e=1;e<=Math.min(episodes,24);e++){final int ep=e;Button play=action(\"Episode \"+e+(store.number(\"progress_\"+id+\"_\"+e)>0?\" · resume\":\"\"));play.setOnClickListener(v->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra(\"id\",id);i.putExtra(\"title\",title);i.putExtra(\"episode\",ep);startActivity(i);});body.addView(play);}}}\n";}
    private String simpleMediaScreen(String pkg,String cls,String title,String detail){return "package "+pkg+";\npublic final class "+cls+" extends GeneratedScreen { protected void render(){body.addView(text(\""+java(title)+"\",22,true));body.addView(text(\""+java(detail)+"\",14,false));Button catalog=action(\"Back to Catalog\");catalog.setOnClickListener(v->AppNavigator.open(this,MainActivity.class));body.addView(catalog);} }\n";}
    private String providerScreen(String pkg){return "package "+pkg+";\npublic final class ProvidersActivity extends GeneratedScreen { protected void render(){MediaProvider p=new DemoProvider();body.addView(text(\"Providers\",22,true));body.addView(text(p.displayName()+\" · \"+p.health(),15,false));body.addView(text(\"Provider implementations are isolated. This generated sample does not download or execute untrusted extensions.\",13,false));Button catalog=action(\"Back to Catalog\");catalog.setOnClickListener(v->AppNavigator.open(this,MainActivity.class));body.addView(catalog);} }\n";}
    private String playerScreen(String pkg){return "package "+pkg+";\npublic final class PlayerActivity extends GeneratedScreen { protected void render(){String id=getIntent().getStringExtra(\"id\");String title=getIntent().getStringExtra(\"title\");int ep=getIntent().getIntExtra(\"episode\",1);String key=\"progress_\"+id+\"_\"+ep;body.addView(text((title==null?\"Anime\":title)+\" · Episode \"+ep,22,true));body.addView(text(\"Playback surface placeholder. A trusted stream resolver can be connected through the provider boundary.\",14,false));body.addView(text(\"Resume position: \"+store.number(key)+\" seconds\",14,false));Button simulate=action(\"Save +60 seconds progress\");simulate.setOnClickListener(v->{store.number(key,store.number(key)+60);store.putText(\"last_episode\",(title==null?\"Anime\":title)+\" episode \"+ep);recreate();});body.addView(simulate);} }\n";}

    private String domainRecord(String pkg){return "package "+pkg+";\npublic final class DomainRecord { public final String id,title,detail; public DomainRecord(String id,String title,String detail){this.id=id;this.title=title;this.detail=detail;} }\n";}
    private String domainMain(String pkg,String name,String brief,Domain domain,List<String> screens,List<String> requirements){
        StringBuilder b=new StringBuilder("package ").append(pkg).append(";\n\npublic final class MainActivity extends GeneratedScreen { protected void render(){body.addView(text(\"").append(java(domainTitle(domain))).append("\",22,true));body.addView(text(\"").append(java(brief)).append("\",14,false));");
        int n=Math.min(requirements==null?0:requirements.size(),4);for(int i=0;i<n;i++)b.append("body.addView(text(\"• ").append(java(requirements.get(i))).append("\",13,false));");
        b.append("gap();");
        for(int i=1;i<screens.size();i++){String cls=screens.get(i);String title=cls.replace("Activity","").replaceAll("([a-z])([A-Z])","$1 $2");b.append("Button b").append(i).append("=action(\"").append(java(title)).append("\");b").append(i).append(".setOnClickListener(v->AppNavigator.open(this,").append(cls).append(".class));body.addView(b").append(i).append(");");}
        b.append("} }\n");return b.toString();
    }
    private String domainScreen(String pkg,String cls,String title,String detail){return "package "+pkg+";\npublic final class "+cls+" extends GeneratedScreen { protected void render(){body.addView(text(\""+java(title)+"\",22,true));body.addView(text(\""+java(detail)+"\",14,false));Button save=action(\"Save local sample state\");save.setOnClickListener(v->{store.putText(\"last_surface\",\""+java(title)+"\");save.setText(\"Saved locally\");});body.addView(save);Button home=action(\"Back to Home\");home.setOnClickListener(v->AppNavigator.open(this,MainActivity.class));body.addView(home);} }\n";}
    private String domainTitle(Domain d){switch(d){case FINANCE:return "Finance Dashboard";case TRACKER:return "Activity Overview";case SOCIAL:return "Community Home";case COMMERCE:return "Product Catalog";case CONTENT:return "Content Workspace";default:return "App Home";}}
    private String domainDescription(Domain d,String title){switch(d){case FINANCE:return title+" is wired for locally persisted transactions, categories, budgets, and summaries.";case TRACKER:return title+" is wired for user-approved activity records, reports, and data controls.";case SOCIAL:return title+" is a separate navigation surface for profile, conversation, and community state.";case COMMERCE:return title+" keeps product/cart/order intent separate from any final spending action.";case CONTENT:return title+" supports locally persisted authored content and explicit editing flows.";default:return title+" is a generated project surface derived from the project requirements.";}}

    private String readme(String name,String brief,List<String> req,List<String> tasks,Domain domain,List<String> screens){StringBuilder out=new StringBuilder("# ").append(name).append("\n\n").append(brief==null?"":brief).append("\n\n## Generated architecture\n- Domain: ").append(domain.name()).append("\n- Multi-screen Android navigation: ").append(String.join(", ",screens)).append("\n- LocalStore persistence shared by generated screens\n- Explicit loading/failure/provider boundaries where applicable\n- Android resources and manifest generated from the inferred feature set\n");if(domain==Domain.MEDIA)out.append("- MediaProvider boundary with DemoProvider placeholder data; unverified extensions are not executed\n");out.append("\n## Requirements\n");if(req!=null)for(String r:req)out.append("- ").append(r).append('\n');out.append("\n## Implementation tasks\n");if(tasks!=null)for(String t:tasks)out.append("- [ ] ").append(t).append('\n');out.append("\nGenerated locally by AIDao. Imported/shared material is treated as data unless separately and explicitly authorized. Installation, publication, credentials, spending, and destructive actions remain user-controlled.\n");return out.toString();}
    private String workflow(){return "name: Generated Android CI\non: [push, pull_request, workflow_dispatch]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with: { distribution: temurin, java-version: '17' }\n      - uses: gradle/actions/setup-gradle@v4\n        with: { gradle-version: '8.10.2' }\n      - run: gradle :app:assembleDebug --stacktrace\n      - uses: actions/upload-artifact@v4\n        with: { name: generated-debug-apk, path: app/build/outputs/apk/debug/app-debug.apk }\n";}

    private String cleanDisplayName(String value){String v=value==null?"Generated Android App":value.trim();return v.isEmpty()?"Generated Android App":v.substring(0,Math.min(v.length(),80));}
    private String slug(String value){String s=value==null?"app":value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+","");if(s.isEmpty())s="app";if(Character.isDigit(s.charAt(0)))s="app"+s;return s.substring(0,Math.min(s.length(),32));}
    private String escape(String s){return s.replace("\\","\\\\").replace("\"","\\\"");}
    private String java(String s){return escape((s==null?"":s).replace("\n"," ").replace("\r"," "));}
    private String xml(String s){return s.replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;");}
}
