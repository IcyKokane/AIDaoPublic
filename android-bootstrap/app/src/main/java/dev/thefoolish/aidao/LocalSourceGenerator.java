package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic local-only Android source generator.
 * Source text is generated without installing, publishing, spending, using
 * credentials, or executing imported/untrusted content.
 */
final class LocalSourceGenerator {
    GeneratedProject generate(String projectName, String brief, List<String> requirements, List<String> tasks) {
        String safeName = cleanDisplayName(projectName);
        String slug = slug(projectName);
        String packageName = "dev.thefoolish.generated." + slug;
        String packagePath = packageName.replace('.', '/');
        boolean media = isMediaProject(brief, requirements);

        List<GeneratedProject.FileEntry> files = new ArrayList<>();
        files.add(file("settings.gradle.kts", settings(safeName), "Create Android project shell"));
        files.add(file("build.gradle.kts", rootGradle(), "Create Android project shell"));
        files.add(file("app/build.gradle.kts", appGradle(packageName), "Create Android project shell"));
        files.add(file("app/src/main/AndroidManifest.xml", manifest(safeName, media), "Declare generated screens and application metadata"));
        files.add(file("app/src/main/res/values/styles.xml", styles(), "Create accessible generated theme"));
        files.add(file("app/src/main/res/values/strings.xml", strings(safeName), "Create generated string resources"));

        if (media) {
            files.add(file("app/src/main/java/" + packagePath + "/AnimeItem.java", animeItem(packageName), "Define anime model"));
            files.add(file("app/src/main/java/" + packagePath + "/EpisodeItem.java", episodeItem(packageName), "Define episode/watch model"));
            files.add(file("app/src/main/java/" + packagePath + "/MediaProvider.java", mediaProvider(packageName), "Define replaceable provider contracts"));
            files.add(file("app/src/main/java/" + packagePath + "/LocalLibraryStore.java", localLibraryStore(packageName), "Persist favorites and watch progress"));
            files.add(file("app/src/main/java/" + packagePath + "/MainActivity.java", mediaMainActivity(packageName, safeName, brief), "Build catalog/library/history navigation"));
            files.add(file("app/src/main/java/" + packagePath + "/AnimeDetailActivity.java", animeDetailActivity(packageName), "Build anime detail and episode flow"));
            files.add(file("app/src/main/java/" + packagePath + "/PlayerActivity.java", playerActivity(packageName), "Build explicit playback/resume surface"));
            files.add(file("app/src/main/java/" + packagePath + "/ProvidersActivity.java", providersActivity(packageName), "Build provider management and health surface"));
        } else {
            files.add(file("app/src/main/java/" + packagePath + "/AppRepository.java", appRepository(packageName), "Persist user-owned records and settings"));
            files.add(file("app/src/main/java/" + packagePath + "/MainActivity.java", genericMainActivity(packageName, safeName, brief), "Build primary collection/home flow"));
            files.add(file("app/src/main/java/" + packagePath + "/DetailActivity.java", genericDetailActivity(packageName), "Build dedicated detail flow"));
            files.add(file("app/src/main/java/" + packagePath + "/EditActivity.java", genericEditActivity(packageName), "Build create/edit input flow"));
            files.add(file("app/src/main/java/" + packagePath + "/SettingsActivity.java", settingsActivity(packageName), "Build persistent settings flow"));
        }

        files.add(file("README.md", readme(safeName, brief, requirements, tasks, media), "Document generated architecture and safeguards"));
        files.add(file(".github/workflows/android.yml", workflow(), "Run generated Android CI"));

        return new GeneratedProject(safeName, packageName, files, verify(packageName, files, media));
    }

    private GeneratedProject.FileEntry file(String path, String content, String taskHint) {
        return new GeneratedProject.FileEntry(path, content, taskHint);
    }

    private boolean isMediaProject(String brief, List<String> requirements) {
        StringBuilder all = new StringBuilder(brief == null ? "" : brief.toLowerCase(Locale.US));
        if (requirements != null) for (String item : requirements) all.append(' ').append(item == null ? "" : item.toLowerCase(Locale.US));
        String s = all.toString();
        return s.contains("anime") || s.contains("episode") || s.contains("stream") || s.contains("video") || s.contains("media provider") || s.contains("mihon");
    }

    private List<String> verify(String packageName, List<GeneratedProject.FileEntry> files, boolean media) {
        List<String> notes = new ArrayList<>();
        String[] required = {"settings.gradle.kts", "build.gradle.kts", "app/build.gradle.kts",
                "app/src/main/AndroidManifest.xml", "app/src/main/res/values/styles.xml",
                "app/src/main/res/values/strings.xml", ".github/workflows/android.yml"};
        for (String path : required) notes.add((has(files, path) ? "PASS " : "FAIL ") + path);
        String root = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        notes.add((has(files, root + "MainActivity.java") ? "PASS " : "FAIL ") + "launcher source");
        if (media) {
            notes.add((has(files, root + "AnimeDetailActivity.java") ? "PASS " : "FAIL ") + "anime detail screen");
            notes.add((has(files, root + "PlayerActivity.java") ? "PASS " : "FAIL ") + "playback screen");
            notes.add((has(files, root + "ProvidersActivity.java") ? "PASS " : "FAIL ") + "provider management screen");
            notes.add((has(files, root + "MediaProvider.java") ? "PASS " : "FAIL ") + "provider contract");
        } else {
            notes.add((has(files, root + "DetailActivity.java") ? "PASS " : "FAIL ") + "detail screen");
            notes.add((has(files, root + "EditActivity.java") ? "PASS " : "FAIL ") + "edit screen");
            notes.add((has(files, root + "SettingsActivity.java") ? "PASS " : "FAIL ") + "settings screen");
        }
        return notes;
    }

    private boolean has(List<GeneratedProject.FileEntry> files, String path) {
        for (GeneratedProject.FileEntry file : files) if (path.equals(file.path)) return true;
        return false;
    }

    private String settings(String name) {
        return "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\n" +
                "dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\n" +
                "rootProject.name = \"" + escape(name) + "\"\ninclude(\":app\")\n";
    }

    private String rootGradle() {
        return "plugins {\n    id(\"com.android.application\") version \"8.7.3\" apply false\n}\n";
    }

    private String appGradle(String packageName) {
        return "plugins { id(\"com.android.application\") }\n\n" +
                "android {\n" +
                "    namespace = \"" + packageName + "\"\n" +
                "    compileSdk = 35\n" +
                "    defaultConfig { applicationId = \"" + packageName + "\"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = \"0.5.0\" }\n" +
                "    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }\n" +
                "}\n";
    }

    private String manifest(String name, boolean media) {
        String extra = media
                ? "    <activity android:name=\".AnimeDetailActivity\"/><activity android:name=\".PlayerActivity\" android:screenOrientation=\"unspecified\"/><activity android:name=\".ProvidersActivity\"/>\n"
                : "    <activity android:name=\".DetailActivity\"/><activity android:name=\".EditActivity\"/><activity android:name=\".SettingsActivity\"/>\n";
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "  <application android:theme=\"@style/AppTheme\" android:label=\"" + xml(name) + "\" android:allowBackup=\"true\" android:supportsRtl=\"true\">\n" +
                extra +
                "    <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
                "      <intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter>\n" +
                "    </activity>\n" +
                "  </application>\n</manifest>\n";
    }

    private String styles() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
                "  <style name=\"AppTheme\" parent=\"@android:style/Theme.Material.NoActionBar\">\n" +
                "    <item name=\"android:fontFamily\">sans-serif</item><item name=\"android:colorAccent\">#5094FF</item>\n" +
                "    <item name=\"android:navigationBarColor\">#0C0D11</item><item name=\"android:statusBarColor\">#121318</item>\n" +
                "    <item name=\"android:windowLightStatusBar\">false</item><item name=\"android:windowLightNavigationBar\">false</item>\n" +
                "  </style>\n</resources>\n";
    }

    private String strings(String name) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources><string name=\"app_name\">" + xml(name) + "</string></resources>\n";
    }

    private String animeItem(String p) {
        return "package " + p + ";\n\npublic final class AnimeItem { public final String id,title,provider; public final int episodeCount; public AnimeItem(String id,String title,String provider,int episodeCount){this.id=id;this.title=title;this.provider=provider;this.episodeCount=episodeCount;} }\n";
    }

    private String episodeItem(String p) {
        return "package " + p + ";\n\npublic final class EpisodeItem { public final String id,title; public final int number; public EpisodeItem(String id,String title,int number){this.id=id;this.title=title;this.number=number;} }\n";
    }

    private String mediaProvider(String p) {
        return "package " + p + ";\n\nimport java.util.List;\n/** Replaceable source boundary. Imported provider material is metadata/data until explicitly trusted by the user. */\npublic interface MediaProvider { String id(); String displayName(); boolean isEnabled(); String health(); List<AnimeItem> search(String query) throws Exception; List<EpisodeItem> episodes(String animeId) throws Exception; String resolveStream(String episodeId) throws Exception; }\n";
    }

    private String localLibraryStore(String p) {
        return "package " + p + ";\n\nimport android.content.Context; import android.content.SharedPreferences;\npublic final class LocalLibraryStore { private final SharedPreferences prefs; public LocalLibraryStore(Context c){prefs=c.getSharedPreferences(\"library\",Context.MODE_PRIVATE);} public boolean isFavorite(String id){return prefs.getBoolean(\"fav_\"+id,false);} public void setFavorite(String id,boolean v){prefs.edit().putBoolean(\"fav_\"+id,v).apply();} public int progressSeconds(String id){return prefs.getInt(\"progress_\"+id,0);} public void saveProgress(String id,int s){prefs.edit().putInt(\"progress_\"+id,Math.max(0,s)).apply();} public void markWatched(String id){prefs.edit().putBoolean(\"watched_\"+id,true).apply();} public boolean watched(String id){return prefs.getBoolean(\"watched_\"+id,false);} }\n";
    }

    private String uiHelpers(String p) {
        return "";
    }

    private String mediaMainActivity(String p, String name, String brief) {
        return "package " + p + ";\n\nimport android.app.Activity; import android.content.Intent; import android.graphics.Color; import android.os.Bundle; import android.view.Gravity; import android.widget.*;\n" +
                "public class MainActivity extends Activity { private LinearLayout body; private LocalLibraryStore store; public void onCreate(Bundle b){super.onCreate(b);store=new LocalLibraryStore(this);render(\"Catalog\");} " +
                "private void render(String section){LinearLayout root=base();root.addView(text(\"" + java(name) + "\",26,true));root.addView(text(\"" + java(brief) + "\",14,false));LinearLayout nav=new LinearLayout(this);for(String tab:new String[]{\"Catalog\",\"Library\",\"History\"}){Button x=button(tab);x.setOnClickListener(v->render(tab));nav.addView(x,new LinearLayout.LayoutParams(0,-2,1));}Button providers=button(\"Providers\");providers.setOnClickListener(v->startActivity(new Intent(this,ProvidersActivity.class)));nav.addView(providers,new LinearLayout.LayoutParams(0,-2,1));root.addView(nav);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);show(section);root.addView(body);ScrollView s=new ScrollView(this);s.addView(root);setContentView(s);} " +
                "private void show(String section){body.removeAllViews();body.addView(text(section,21,true));if(section.equals(\"Catalog\")){body.addView(text(\"Search/browse results are resolved through enabled providers. Provider failures stay isolated and visible.\",14,false));item(\"Example Anime\",\"demo\");}else if(section.equals(\"Library\")){body.addView(text(\"Favorites persist locally on this device.\",14,false));item(\"Example Anime\",\"demo\");}else body.addView(text(\"Resume positions and watched state persist locally.\",14,false));} " +
                "private void item(String title,String id){Button b=button(title+(store.isFavorite(id)?\" ★\":\"\"));b.setOnClickListener(v->{Intent i=new Intent(this,AnimeDetailActivity.class);i.putExtra(\"id\",id);i.putExtra(\"title\",title);startActivity(i);});body.addView(b);} " +
                commonUi() + "}\n";
    }

    private String animeDetailActivity(String p) {
        return "package " + p + ";\n\nimport android.app.Activity; import android.content.Intent; import android.graphics.Color; import android.os.Bundle; import android.widget.*;\npublic class AnimeDetailActivity extends Activity { public void onCreate(Bundle b){super.onCreate(b);String id=getIntent().getStringExtra(\"id\");String title=getIntent().getStringExtra(\"title\");LocalLibraryStore store=new LocalLibraryStore(this);LinearLayout root=base();root.addView(text(title==null?\"Anime detail\":title,26,true));Button fav=button(store.isFavorite(id)?\"Remove favorite\":\"Add favorite\");fav.setOnClickListener(v->{store.setFavorite(id,!store.isFavorite(id));fav.setText(store.isFavorite(id)?\"Remove favorite\":\"Add favorite\");});root.addView(fav);root.addView(text(\"Episodes\",20,true));for(int n=1;n<=3;n++){final int ep=n;Button e=button(\"Episode \"+n);e.setOnClickListener(v->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra(\"episode_id\",id+\"-\"+ep);i.putExtra(\"episode_title\",\"Episode \"+ep);startActivity(i);});root.addView(e);}setContentView(root);} " + commonUi() + "}\n";
    }

    private String playerActivity(String p) {
        return "package " + p + ";\n\nimport android.app.Activity; import android.os.Bundle; import android.widget.*; import android.graphics.Color;\npublic class PlayerActivity extends Activity { public void onCreate(Bundle b){super.onCreate(b);String id=getIntent().getStringExtra(\"episode_id\");String title=getIntent().getStringExtra(\"episode_title\");LocalLibraryStore store=new LocalLibraryStore(this);LinearLayout root=base();root.addView(text(title==null?\"Playback\":title,26,true));root.addView(text(\"Stream resolution belongs behind MediaProvider. This generated surface does not silently select or execute an unverified source.\",14,false));root.addView(text(\"Resume position: \"+store.progressSeconds(id)+\" seconds\",14,false));Button save=button(\"Save demo progress\");save.setOnClickListener(v->{store.saveProgress(id,60);Toast.makeText(this,\"Saved 60 second resume point\",Toast.LENGTH_SHORT).show();});root.addView(save);Button watched=button(\"Mark watched\");watched.setOnClickListener(v->store.markWatched(id));root.addView(watched);setContentView(root);} " + commonUi() + "}\n";
    }

    private String providersActivity(String p) {
        return "package " + p + ";\n\nimport android.app.Activity; import android.os.Bundle; import android.widget.*; import android.graphics.Color;\npublic class ProvidersActivity extends Activity { public void onCreate(Bundle b){super.onCreate(b);LinearLayout root=base();root.addView(text(\"Providers\",26,true));root.addView(text(\"Provider packages remain disabled until explicitly reviewed/approved. A failing provider must not break healthy providers.\",14,false));root.addView(text(\"No external providers installed\",18,true));root.addView(text(\"Status: safe local boundary only\",14,false));setContentView(root);} " + commonUi() + "}\n";
    }

    private String appRepository(String p) {
        return "package " + p + ";\n\nimport android.content.Context; import android.content.SharedPreferences; import java.util.*;\npublic final class AppRepository { private final SharedPreferences prefs; public AppRepository(Context c){prefs=c.getSharedPreferences(\"app_data\",Context.MODE_PRIVATE);} public List<String> records(){String raw=prefs.getString(\"records\",\"\");if(raw.isEmpty())return new ArrayList<>();return new ArrayList<>(Arrays.asList(raw.split(\"\\\\n\",-1)));} public void add(String value){List<String> r=records();r.add(value);prefs.edit().putString(\"records\",String.join(\"\\n\",r)).apply();} public boolean compactMode(){return prefs.getBoolean(\"compact\",false);} public void compactMode(boolean v){prefs.edit().putBoolean(\"compact\",v).apply();} }\n";
    }

    private String genericMainActivity(String p, String name, String brief) {
        return "package " + p + ";\n\nimport android.app.Activity; import android.content.Intent; import android.os.Bundle; import android.widget.*; import android.graphics.Color;\npublic class MainActivity extends Activity { private LinearLayout body; private AppRepository repo; public void onCreate(Bundle b){super.onCreate(b);repo=new AppRepository(this);render();} protected void onResume(){super.onResume();if(repo!=null)render();} private void render(){LinearLayout root=base();root.addView(text(\"" + java(name) + "\",26,true));root.addView(text(\"" + java(brief) + "\",14,false));LinearLayout actions=new LinearLayout(this);Button add=button(\"Add\");add.setOnClickListener(v->startActivity(new Intent(this,EditActivity.class)));actions.addView(add,new LinearLayout.LayoutParams(0,-2,1));Button settings=button(\"Settings\");settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));actions.addView(settings,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);List<String> records=repo.records();if(records.isEmpty())body.addView(text(\"No items yet. Add one to begin the primary flow.\",15,false));for(String value:records){Button item=button(value);item.setOnClickListener(v->{Intent i=new Intent(this,DetailActivity.class);i.putExtra(\"value\",value);startActivity(i);});body.addView(item);}root.addView(body);setContentView(root);} " + commonUi() + "}\n";
    }

    private String genericDetailActivity(String p) {
        return "package " + p + ";\n\nimport android.app.Activity; import android.os.Bundle; import android.widget.*; import android.graphics.Color;\npublic class DetailActivity extends Activity { public void onCreate(Bundle b){super.onCreate(b);LinearLayout root=base();root.addView(text(\"Detail\",26,true));root.addView(text(getIntent().getStringExtra(\"value\"),18,false));root.addView(text(\"This dedicated screen is generated from the requested record/detail flow instead of flattening all content onto the launcher.\",14,false));setContentView(root);} " + commonUi() + "}\n";
    }

    private String genericEditActivity(String p) {
        return "package " + p + ";\n\nimport android.app.Activity; import android.os.Bundle; import android.widget.*; import android.graphics.Color;\npublic class EditActivity extends Activity { public void onCreate(Bundle b){super.onCreate(b);AppRepository repo=new AppRepository(this);LinearLayout root=base();root.addView(text(\"Create item\",26,true));EditText input=new EditText(this);input.setHint(\"Enter a value\");input.setTextColor(Color.WHITE);input.setHintTextColor(Color.LTGRAY);root.addView(input);Button save=button(\"Save\");save.setOnClickListener(v->{String value=input.getText().toString().trim();if(value.isEmpty()){input.setError(\"Required\");return;}repo.add(value);finish();});root.addView(save);Button cancel=button(\"Cancel\");cancel.setOnClickListener(v->finish());root.addView(cancel);setContentView(root);} " + commonUi() + "}\n";
    }

    private String settingsActivity(String p) {
        return "package " + p + ";\n\nimport android.app.Activity; import android.os.Bundle; import android.widget.*; import android.graphics.Color;\npublic class SettingsActivity extends Activity { public void onCreate(Bundle b){super.onCreate(b);AppRepository repo=new AppRepository(this);LinearLayout root=base();root.addView(text(\"Settings\",26,true));Switch compact=new Switch(this);compact.setText(\"Compact list mode\");compact.setTextColor(Color.WHITE);compact.setChecked(repo.compactMode());compact.setOnCheckedChangeListener((v,c)->repo.compactMode(c));root.addView(compact);root.addView(text(\"Settings persist locally and remain user-controlled.\",14,false));setContentView(root);} " + commonUi() + "}\n";
    }

    private String commonUi() {
        return "private LinearLayout base(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(32,48,32,32);r.setBackgroundColor(Color.rgb(18,19,24));return r;} private TextView text(String v,int s,boolean b){TextView t=new TextView(this);t.setText(v==null?\"\":v);t.setTextColor(Color.WHITE);t.setTextSize(s);t.setPadding(0,10,0,10);t.setTypeface(android.graphics.Typeface.create(\"sans-serif\",b?1:0));return t;} private Button button(String v){Button b=new Button(this);b.setText(v);b.setAllCaps(false);return b;} ";
    }

    private String readme(String name, String brief, List<String> requirements, List<String> tasks, boolean media) {
        StringBuilder out = new StringBuilder("# ").append(name).append("\n\n").append(brief == null ? "" : brief).append("\n\n## Requirements\n");
        if (requirements != null) for (String item : requirements) out.append("- ").append(item).append('\n');
        out.append("\n## Implementation tasks\n");
        if (tasks != null) for (String item : tasks) out.append("- [ ] ").append(item).append('\n');
        out.append("\n## Generated architecture\n");
        if (media) out.append("- Catalog/library/history launcher\n- Anime detail + episode navigation\n- Explicit playback/resume surface\n- Provider management/health boundary\n- Local favorites, progress, and watched state\n");
        else out.append("- Collection/home launcher\n- Dedicated detail screen\n- Create/edit screen with validation\n- Persistent settings screen\n- SharedPreferences-backed repository boundary\n");
        out.append("\nGenerated locally by AIDao. Imported material is data by default; installation, publication, credential use, spending, destructive actions, and activation of untrusted code require explicit user control.\n");
        return out.toString();
    }

    private String workflow() {
        return "name: Generated Android CI\n" +
                "on: [push, pull_request, workflow_dispatch]\n" +
                "jobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n" +
                "      - uses: actions/checkout@v4\n" +
                "      - uses: actions/setup-java@v4\n        with: { distribution: temurin, java-version: '17' }\n" +
                "      - uses: gradle/actions/setup-gradle@v4\n        with: { gradle-version: '8.10.2' }\n" +
                "      - run: gradle :app:assembleDebug --stacktrace\n" +
                "      - uses: actions/upload-artifact@v4\n        with: { name: generated-debug-apk, path: app/build/outputs/apk/debug/app-debug.apk }\n";
    }

    private String cleanDisplayName(String value) { String v=value==null?"Generated Android App":value.trim(); return v.isEmpty()?"Generated Android App":v.substring(0,Math.min(v.length(),80)); }
    private String slug(String value) { String s=value==null?"app":value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+",""); if(s.isEmpty())s="app"; if(Character.isDigit(s.charAt(0)))s="app"+s; return s.substring(0,Math.min(s.length(),32)); }
    private String escape(String s) { return s.replace("\\","\\\\").replace("\"","\\\""); }
    private String java(String s) { return escape((s==null?"":s).replace("\n"," ").replace("\r"," ")); }
    private String xml(String s) { return s.replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;"); }
}
