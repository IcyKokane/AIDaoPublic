package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic, local-only Android source generator.
 *
 * Source creation is deliberately side-effect free: no installation,
 * publication, credential use, provider acquisition, spending, or execution of
 * user-supplied material occurs here. External actions remain explicit gates.
 */
final class LocalSourceGenerator {
    GeneratedProject generate(String projectName, String brief, List<String> requirements, List<String> tasks) {
        String safeName = cleanDisplayName(projectName);
        String slug = slug(projectName);
        String packageName = "dev.thefoolish.generated." + slug;
        String packagePath = packageName.replace('.', '/');
        ProjectIntent intent = ProjectIntent.from(brief, requirements);

        List<GeneratedProject.FileEntry> files = new ArrayList<>();
        files.add(file("settings.gradle.kts", settings(safeName), "Create Android project shell"));
        files.add(file("build.gradle.kts", rootGradle(), "Create Android project shell"));
        files.add(file("app/build.gradle.kts", appGradle(packageName), "Create Android project shell"));
        files.add(file("app/src/main/AndroidManifest.xml", manifest(safeName, intent), "Generate manifest declarations"));
        files.add(file("app/src/main/res/values/styles.xml", styles(), "Generate accessible application theme"));
        files.add(file("app/src/main/res/values/colors.xml", colors(), "Generate reusable color resources"));
        files.add(file("app/src/main/res/values/strings.xml", strings(safeName), "Generate application string resources"));

        files.add(file("app/src/main/java/" + packagePath + "/ProjectContract.java", projectContract(packageName, safeName, brief, intent), "Represent interpreted project intent"));
        files.add(file("app/src/main/java/" + packagePath + "/AppRecord.java", appRecord(packageName), "Define reusable local data model"));
        files.add(file("app/src/main/java/" + packagePath + "/AppStateStore.java", appStateStore(packageName), "Persist generated app state"));
        files.add(file("app/src/main/java/" + packagePath + "/AppRepository.java", appRepository(packageName, intent), "Implement local project data repository"));
        if (intent.providers) files.add(file("app/src/main/java/" + packagePath + "/ProviderContract.java", providerContract(packageName), "Define safe replaceable provider boundary"));
        if (intent.media) files.add(file("app/src/main/java/" + packagePath + "/MediaProgressStore.java", mediaProgressStore(packageName), "Persist media favorites and progress"));
        files.add(file("app/src/main/java/" + packagePath + "/MainActivity.java", mainActivity(packageName, safeName, brief, intent), "Build intent-specific multi-screen user flow"));

        files.add(file("README.md", readme(safeName, brief, requirements, tasks, intent), "Document generated architecture"));
        files.add(file(".github/workflows/android.yml", workflow(), "Run Android CI"));

        List<String> verification = verify(packageName, files, intent);
        return new GeneratedProject(safeName, packageName, files, verification);
    }

    private GeneratedProject.FileEntry file(String path, String content, String taskHint) {
        return new GeneratedProject.FileEntry(path, content, taskHint);
    }

    private List<String> verify(String packageName, List<GeneratedProject.FileEntry> files, ProjectIntent intent) {
        List<String> notes = new ArrayList<>();
        String[] required = {
                "settings.gradle.kts", "build.gradle.kts", "app/build.gradle.kts",
                "app/src/main/AndroidManifest.xml", "app/src/main/res/values/styles.xml",
                "app/src/main/res/values/colors.xml", "app/src/main/res/values/strings.xml",
                ".github/workflows/android.yml"
        };
        for (String path : required) notes.add((has(files, path) ? "PASS " : "FAIL ") + path);
        String root = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        String[] core = {"MainActivity.java", "ProjectContract.java", "AppRecord.java", "AppStateStore.java", "AppRepository.java"};
        for (String source : core) notes.add((has(files, root + source) ? "PASS " : "FAIL ") + source);
        if (intent.providers) notes.add((has(files, root + "ProviderContract.java") ? "PASS " : "FAIL ") + "provider boundary");
        if (intent.media) notes.add((has(files, root + "MediaProgressStore.java") ? "PASS " : "FAIL ") + "media progress store");
        notes.add(intent.screens.size() >= 3 ? "PASS multi-screen navigation plan: " + intent.screens.size() + " screens" : "FAIL insufficient navigation surfaces");
        return notes;
    }

    private boolean has(List<GeneratedProject.FileEntry> files, String path) {
        for (GeneratedProject.FileEntry f : files) if (path.equals(f.path)) return true;
        return false;
    }

    private String settings(String name) {
        return "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\n" +
                "dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\n" +
                "rootProject.name = \"" + escape(name) + "\"\ninclude(\":app\")\n";
    }

    private String rootGradle() {
        return "plugins { id(\"com.android.application\") version \"8.7.3\" apply false }\n";
    }

    private String appGradle(String packageName) {
        return "plugins { id(\"com.android.application\") }\n\nandroid {\n" +
                "  namespace = \"" + packageName + "\"\n  compileSdk = 35\n" +
                "  defaultConfig { applicationId = \"" + packageName + "\"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = \"0.5-generated\" }\n" +
                "  compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }\n}\n";
    }

    private String manifest(String name, ProjectIntent intent) {
        StringBuilder out = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n");
        if (intent.providers || intent.media) out.append("  <uses-permission android:name=\"android.permission.INTERNET\"/>\n");
        if (intent.notifications) out.append("  <uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\"/>\n");
        if (intent.location) out.append("  <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\"/>\n");
        out.append("  <application android:theme=\"@style/AppTheme\" android:label=\"@string/app_name\" android:allowBackup=\"true\" android:supportsRtl=\"true\">\n")
           .append("    <activity android:name=\".MainActivity\" android:exported=\"true\" android:windowSoftInputMode=\"adjustResize\">\n")
           .append("      <intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter>\n")
           .append("    </activity>\n  </application>\n</manifest>\n");
        return out.toString();
    }

    private String styles() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
                "  <style name=\"AppTheme\" parent=\"@android:style/Theme.Material.NoActionBar\">\n" +
                "    <item name=\"android:fontFamily\">sans</item><item name=\"android:colorAccent\">@color/accent</item>\n" +
                "    <item name=\"android:statusBarColor\">@color/background</item><item name=\"android:navigationBarColor\">@color/navigation</item>\n" +
                "    <item name=\"android:windowLightStatusBar\">false</item><item name=\"android:windowLightNavigationBar\">false</item>\n" +
                "  </style>\n</resources>\n";
    }

    private String colors() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
                "  <color name=\"background\">#121318</color><color name=\"navigation\">#0C0D11</color>\n" +
                "  <color name=\"panel\">#1D1F27</color><color name=\"accent\">#5094FF</color>\n" +
                "  <color name=\"text_primary\">#FFFFFF</color><color name=\"text_secondary\">#A3A8B8</color>\n</resources>\n";
    }

    private String strings(String name) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources><string name=\"app_name\">" + xml(name) + "</string></resources>\n";
    }

    private String projectContract(String pkg, String name, String brief, ProjectIntent intent) {
        StringBuilder screens = new StringBuilder();
        for (int i = 0; i < intent.screens.size(); i++) { if (i > 0) screens.append(","); screens.append("\"").append(java(intent.screens.get(i))).append("\""); }
        return "package " + pkg + ";\n\npublic final class ProjectContract {\n" +
                "  public static final String NAME=\"" + java(name) + "\";\n" +
                "  public static final String BRIEF=\"" + java(brief) + "\";\n" +
                "  public static final String[] SCREENS={" + screens + "};\n" +
                "  public static final boolean MEDIA=" + intent.media + ", SEARCH=" + intent.search + ", FORMS=" + intent.forms + ", PROVIDERS=" + intent.providers + ";\n" +
                "  private ProjectContract(){}\n}\n";
    }

    private String appRecord(String pkg) {
        return "package " + pkg + ";\n\npublic final class AppRecord {\n" +
                "  public final String id,title,detail; public AppRecord(String id,String title,String detail){this.id=id;this.title=title;this.detail=detail;}\n}\n";
    }

    private String appStateStore(String pkg) {
        return "package " + pkg + ";\n\nimport android.content.Context; import android.content.SharedPreferences;\n" +
                "public final class AppStateStore {\n" +
                "  private final SharedPreferences p; public AppStateStore(Context c){p=c.getSharedPreferences(\"app_state\",Context.MODE_PRIVATE);}\n" +
                "  public String lastScreen(){return p.getString(\"last_screen\",\"Home\");} public void lastScreen(String v){p.edit().putString(\"last_screen\",v).apply();}\n" +
                "  public boolean saved(String id){return p.getBoolean(\"saved_\"+id,false);} public void saved(String id,boolean v){p.edit().putBoolean(\"saved_\"+id,v).apply();}\n" +
                "  public String text(String key,String fallback){return p.getString(key,fallback);} public void text(String key,String value){p.edit().putString(key,value).apply();}\n}\n";
    }

    private String appRepository(String pkg, ProjectIntent intent) {
        String noun = intent.media ? "Anime" : (intent.listData ? "Item" : "Project item");
        return "package " + pkg + ";\n\nimport java.util.*;\n" +
                "public final class AppRepository {\n" +
                "  private final List<AppRecord> records=new ArrayList<>(); public AppRepository(){records.add(new AppRecord(\"one\",\"" + java(noun) + " One\",\"Generated from the project intent boundary.\"));records.add(new AppRecord(\"two\",\"" + java(noun) + " Two\",\"Replace demo records through the repository without rewriting screens.\"));}\n" +
                "  public List<AppRecord> all(){return Collections.unmodifiableList(records);} public List<AppRecord> search(String q){if(q==null||q.trim().isEmpty())return all();List<AppRecord> out=new ArrayList<>();String n=q.toLowerCase(Locale.US);for(AppRecord r:records)if(r.title.toLowerCase(Locale.US).contains(n)||r.detail.toLowerCase(Locale.US).contains(n))out.add(r);return out;}\n}\n";
    }

    private String providerContract(String pkg) {
        return "package " + pkg + ";\n\nimport java.util.List;\n/** Stable boundary only; generated code never downloads or executes provider code automatically. */\n" +
                "public interface ProviderContract { String id(); String name(); boolean enabled(); String health(); List<AppRecord> search(String query) throws Exception; }\n";
    }

    private String mediaProgressStore(String pkg) {
        return "package " + pkg + ";\n\nimport android.content.Context; import android.content.SharedPreferences;\n" +
                "public final class MediaProgressStore { private final SharedPreferences p; public MediaProgressStore(Context c){p=c.getSharedPreferences(\"media_progress\",Context.MODE_PRIVATE);} public int seconds(String id){return p.getInt(\"progress_\"+id,0);} public void seconds(String id,int v){p.edit().putInt(\"progress_\"+id,Math.max(0,v)).apply();} }\n";
    }

    private String mainActivity(String pkg, String name, String brief, ProjectIntent intent) {
        StringBuilder screenArray = new StringBuilder();
        for (int i = 0; i < intent.screens.size(); i++) { if (i > 0) screenArray.append(','); screenArray.append("\"").append(java(intent.screens.get(i))).append("\""); }
        return "package " + pkg + ";\n\n" +
                "import android.app.*; import android.graphics.Color; import android.os.Bundle; import android.text.InputType; import android.view.*; import android.widget.*; import java.util.*;\n\n" +
                "public class MainActivity extends Activity {\n" +
                "  private final int BG=Color.rgb(18,19,24),PANEL=Color.rgb(29,31,39),MUTED=Color.rgb(163,168,184),ACCENT=Color.rgb(80,148,255); private LinearLayout body; private AppStateStore state; private AppRepository repo;\n" +
                "  @Override public void onCreate(Bundle b){super.onCreate(b);state=new AppStateStore(this);repo=new AppRepository();render(valid(state.lastScreen())?state.lastScreen():\"Home\");}\n" +
                "  private boolean valid(String s){for(String x:ProjectContract.SCREENS)if(x.equals(s))return true;return false;}\n" +
                "  private void render(String screen){state.lastScreen(screen);LinearLayout root=col();root.setPadding(28,32,28,24);root.setBackgroundColor(BG);root.addView(text(ProjectContract.NAME,25,true));root.addView(text(screen,18,true));HorizontalScrollView hs=new HorizontalScrollView(this);LinearLayout nav=new LinearLayout(this);for(String s:ProjectContract.SCREENS){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setContentDescription(\"Open \"+s);b.setOnClickListener(v->render(s));nav.addView(b);}hs.addView(nav);root.addView(hs);body=col();body.setPadding(0,18,0,24);show(screen);root.addView(body);ScrollView sv=new ScrollView(this);sv.addView(root);setContentView(sv);}\n" +
                "  private void show(String screen){body.removeAllViews();if(screen.equals(\"Home\")){body.addView(text(ProjectContract.BRIEF,14,false));records(repo.all());return;}if(screen.equals(\"Search\")){EditText q=new EditText(this);q.setHint(\"Search\");q.setSingleLine(true);Button go=new Button(this);go.setText(\"Search\");go.setOnClickListener(v->{body.removeViews(2,Math.max(0,body.getChildCount()-2));records(repo.search(q.getText().toString()));});body.addView(q);body.addView(go);records(repo.all());return;}if(screen.equals(\"Details\")){detail(repo.all().get(0));return;}if(screen.equals(\"Library\")||screen.equals(\"Saved\")){body.addView(text(\"Saved state is persisted locally across restarts.\",14,false));records(repo.all());return;}if(screen.equals(\"History\")){body.addView(text(\"Recent/progress state is stored locally through explicit app state boundaries.\",14,false));return;}if(screen.equals(\"Providers\")){body.addView(text(\"Provider execution is disabled by default. Providers must implement ProviderContract and surface enabled/health state.\",14,false));return;}if(screen.equals(\"Account\")){body.addView(text(\"Account credentials are not embedded or stored as plain project preferences.\",14,false));return;}if(screen.equals(\"Map\")){body.addView(text(\"Location access requires explicit Android permission before a location service is invoked.\",14,false));return;}if(screen.equals(\"Items\")){records(repo.all());if(ProjectContract.FORMS)addForm();return;}settings();}\n" +
                "  private void records(List<AppRecord> list){for(AppRecord r:list){LinearLayout c=col();c.setPadding(18,16,18,16);c.setBackgroundColor(PANEL);c.addView(text(r.title,17,true));c.addView(text(r.detail,13,false));Button d=new Button(this);d.setText(\"View details\");d.setAllCaps(false);d.setOnClickListener(v->detail(r));c.addView(d);body.addView(c,new LinearLayout.LayoutParams(-1,-2));}}\n" +
                "  private void detail(AppRecord r){body.removeAllViews();body.addView(text(r.title,22,true));body.addView(text(r.detail,15,false));Button save=new Button(this);save.setAllCaps(false);save.setText(state.saved(r.id)?\"Remove saved\":\"Save\");save.setOnClickListener(v->{state.saved(r.id,!state.saved(r.id));save.setText(state.saved(r.id)?\"Remove saved\":\"Save\");});body.addView(save);}\n" +
                "  private void addForm(){EditText title=new EditText(this);title.setHint(\"New item title\");Button save=new Button(this);save.setText(\"Save draft locally\");save.setOnClickListener(v->{String x=title.getText().toString().trim();if(!x.isEmpty()){state.text(\"draft_title\",x);Toast.makeText(this,\"Draft saved\",Toast.LENGTH_SHORT).show();}});body.addView(title);body.addView(save);}\n" +
                "  private void settings(){body.addView(text(\"Settings\",20,true));body.addView(text(\"Generated safeguards: local persistence, explicit permissions, no embedded credentials, and no automatic execution of untrusted provider material.\",14,false));}\n" +
                "  private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;} private TextView text(String v,int size,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setPadding(0,9,0,9);t.setTypeface(android.graphics.Typeface.create(\"sans\",bold?1:0));return t;}\n" +
                "}\n";
    }

    private String readme(String name, String brief, List<String> requirements, List<String> tasks, ProjectIntent intent) {
        StringBuilder out = new StringBuilder("# ").append(name).append("\n\n").append(brief == null ? "" : brief).append("\n\n## Interpreted screens\n");
        for (String screen : intent.screens) out.append("- ").append(screen).append('\n');
        out.append("\n## Requirements\n"); if (requirements != null) for (String r : requirements) out.append("- ").append(r).append('\n');
        out.append("\n## Implementation tasks\n"); if (tasks != null) for (String t : tasks) out.append("- [ ] ").append(t).append('\n');
        out.append("\n## Generated architecture\n- ProjectContract captures interpreted intent\n- AppRepository isolates project data\n- AppStateStore persists navigation and saved/draft state\n- MainActivity renders intent-specific multi-screen navigation\n");
        if (intent.providers) out.append("- ProviderContract is a non-executing provider boundary; untrusted provider code is never silently acquired or run\n");
        out.append("\nGenerated locally by AIDao. Installation, publication, credential use, spending, destructive actions, and external provider acquisition remain explicit user-controlled actions.\n");
        return out.toString();
    }

    private String workflow() {
        return "name: Generated Android CI\non: [push, pull_request, workflow_dispatch]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with: { distribution: temurin, java-version: '17' }\n      - uses: gradle/actions/setup-gradle@v4\n        with: { gradle-version: '8.10.2' }\n      - run: gradle :app:assembleDebug --stacktrace\n      - uses: actions/upload-artifact@v4\n        with: { name: generated-debug-apk, path: app/build/outputs/apk/debug/app-debug.apk }\n";
    }

    private String cleanDisplayName(String value) { String v=value==null?"Generated Android App":value.trim();return v.isEmpty()?"Generated Android App":v.substring(0,Math.min(v.length(),80)); }
    private String slug(String value) { String s=value==null?"app":value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+","");if(s.isEmpty())s="app";if(Character.isDigit(s.charAt(0)))s="app"+s;return s.substring(0,Math.min(s.length(),32)); }
    private String escape(String s){return (s==null?"":s).replace("\\","\\\\").replace("\"","\\\"");}
    private String java(String s){return escape((s==null?"":s).replace("\n"," ").replace("\r"," "));}
    private String xml(String s){return (s==null?"":s).replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;");}
}
