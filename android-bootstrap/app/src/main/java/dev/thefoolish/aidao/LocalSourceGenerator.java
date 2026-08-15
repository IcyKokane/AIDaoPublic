package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic, local-only Android source generator.
 *
 * This stage deliberately creates source text only. It never installs APKs,
 * publishes repositories, uses credentials, spends money, or performs
 * destructive actions. Those actions remain explicit user-controlled gates.
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
        files.add(file("app/src/main/AndroidManifest.xml", manifest(safeName), "Create Android project shell"));
        files.add(file("app/src/main/res/values/styles.xml", styles(), "Create Android project shell"));

        if (media) {
            files.add(file("app/src/main/java/" + packagePath + "/AnimeItem.java", animeItem(packageName), "Define anime and watch-progress models"));
            files.add(file("app/src/main/java/" + packagePath + "/MediaProvider.java", mediaProvider(packageName), "Implement provider contracts"));
            files.add(file("app/src/main/java/" + packagePath + "/LocalLibraryStore.java", localLibraryStore(packageName), "Persist favorites and watch progress"));
            files.add(file("app/src/main/java/" + packagePath + "/MainActivity.java", mediaActivity(packageName, safeName, brief), "Build catalog, library, history, and provider user flows"));
        } else {
            files.add(file("app/src/main/java/" + packagePath + "/MainActivity.java", genericActivity(packageName, safeName, brief, requirements), "Build primary user flow"));
        }

        files.add(file("README.md", readme(safeName, brief, requirements, tasks, media), "Document generated project"));
        files.add(file(".github/workflows/android.yml", workflow(), "Run Android CI"));

        List<String> verification = verify(packageName, files, media);
        return new GeneratedProject(safeName, packageName, files, verification);
    }

    private GeneratedProject.FileEntry file(String path, String content, String taskHint) {
        return new GeneratedProject.FileEntry(path, content, taskHint);
    }

    private boolean isMediaProject(String brief, List<String> requirements) {
        StringBuilder all = new StringBuilder(brief == null ? "" : brief.toLowerCase(Locale.US));
        if (requirements != null) for (String item : requirements) all.append(' ').append(item == null ? "" : item.toLowerCase(Locale.US));
        String s = all.toString();
        return s.contains("anime") || s.contains("episode") || s.contains("stream") || s.contains("video") || s.contains("media provider");
    }

    private List<String> verify(String packageName, List<GeneratedProject.FileEntry> files, boolean media) {
        List<String> notes = new ArrayList<>();
        String[] required = {
                "settings.gradle.kts", "build.gradle.kts", "app/build.gradle.kts",
                "app/src/main/AndroidManifest.xml", "app/src/main/res/values/styles.xml",
                ".github/workflows/android.yml"
        };
        for (String path : required) notes.add((has(files, path) ? "PASS " : "FAIL ") + path);
        String sourceRoot = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        notes.add((has(files, sourceRoot + "MainActivity.java") ? "PASS " : "FAIL ") + "launcher source");
        if (media) {
            notes.add((has(files, sourceRoot + "AnimeItem.java") ? "PASS " : "FAIL ") + "media model");
            notes.add((has(files, sourceRoot + "MediaProvider.java") ? "PASS " : "FAIL ") + "provider contract");
            notes.add((has(files, sourceRoot + "LocalLibraryStore.java") ? "PASS " : "FAIL ") + "local library persistence");
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
                "    defaultConfig { applicationId = \"" + packageName + "\"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = \"0.1.0\" }\n" +
                "    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }\n" +
                "}\n";
    }

    private String manifest(String name) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "  <application android:theme=\"@style/AppTheme\" android:label=\"" + xml(name) + "\" android:allowBackup=\"true\" android:supportsRtl=\"true\">\n" +
                "    <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
                "      <intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter>\n" +
                "    </activity>\n" +
                "  </application>\n" +
                "</manifest>\n";
    }

    private String styles() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
                "  <style name=\"AppTheme\" parent=\"@android:style/Theme.Material.NoActionBar\">\n" +
                "    <item name=\"android:fontFamily\">sans-serif-condensed</item>\n" +
                "    <item name=\"android:windowActionModeOverlay\">true</item>\n" +
                "    <item name=\"android:colorAccent\">#5094FF</item>\n" +
                "    <item name=\"android:navigationBarColor\">#0C0D11</item>\n" +
                "    <item name=\"android:statusBarColor\">#121318</item>\n" +
                "    <item name=\"android:windowLightStatusBar\">false</item>\n" +
                "    <item name=\"android:windowLightNavigationBar\">false</item>\n" +
                "  </style>\n</resources>\n";
    }

    private String animeItem(String packageName) {
        return "package " + packageName + ";\n\n" +
                "public final class AnimeItem {\n" +
                "  public final String id,title,provider; public final int episodeCount;\n" +
                "  public AnimeItem(String id,String title,String provider,int episodeCount){this.id=id;this.title=title;this.provider=provider;this.episodeCount=episodeCount;}\n" +
                "}\n";
    }

    private String mediaProvider(String packageName) {
        return "package " + packageName + ";\n\nimport java.util.List;\n\n" +
                "/** Replaceable source boundary. Generated code does not assume an unverified content source. */\n" +
                "public interface MediaProvider {\n" +
                "  String id(); String displayName(); boolean isEnabled(); String health();\n" +
                "  List<AnimeItem> search(String query) throws Exception;\n" +
                "}\n";
    }

    private String localLibraryStore(String packageName) {
        return "package " + packageName + ";\n\nimport android.content.Context;\nimport android.content.SharedPreferences;\n\n" +
                "public final class LocalLibraryStore {\n" +
                "  private final SharedPreferences prefs; public LocalLibraryStore(Context c){prefs=c.getSharedPreferences(\"library\",Context.MODE_PRIVATE);}\n" +
                "  public boolean isFavorite(String id){return prefs.getBoolean(\"fav_\"+id,false);}\n" +
                "  public void setFavorite(String id,boolean value){prefs.edit().putBoolean(\"fav_\"+id,value).apply();}\n" +
                "  public int progressSeconds(String episodeId){return prefs.getInt(\"progress_\"+episodeId,0);}\n" +
                "  public void saveProgress(String episodeId,int seconds){prefs.edit().putInt(\"progress_\"+episodeId,Math.max(0,seconds)).apply();}\n" +
                "}\n";
    }

    private String mediaActivity(String packageName, String name, String brief) {
        return "package " + packageName + ";\n\n" +
                "import android.app.Activity;\nimport android.graphics.Color;\nimport android.os.Bundle;\nimport android.view.Gravity;\nimport android.view.View;\nimport android.widget.Button;\nimport android.widget.LinearLayout;\nimport android.widget.ScrollView;\nimport android.widget.TextView;\n\n" +
                "public class MainActivity extends Activity {\n" +
                "  private LinearLayout body; private LocalLibraryStore store;\n" +
                "  @Override public void onCreate(Bundle state){super.onCreate(state);store=new LocalLibraryStore(this);render(\"Catalog\");}\n" +
                "  private void render(String section){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(32,48,32,32);root.setBackgroundColor(Color.rgb(18,19,24));root.addView(text(\"" + java(name) + "\",26,true));root.addView(text(\"" + java(brief) + "\",14,false));LinearLayout nav=new LinearLayout(this);String[] tabs={\"Catalog\",\"Library\",\"History\",\"Providers\"};for(String tab:tabs){Button b=new Button(this);b.setText(tab);b.setAllCaps(false);b.setOnClickListener(v->render(tab));nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));}root.addView(nav);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);showSection(section);root.addView(body);ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);}\n" +
                "  private void showSection(String section){body.removeAllViews();body.addView(text(section,21,true));if(section.equals(\"Catalog\")){body.addView(text(\"Search and browse results will be supplied through enabled MediaProvider implementations.\",15,false));sampleCard(\"Example Anime\",\"provider-demo\");}else if(section.equals(\"Library\")){body.addView(text(\"Favorites are persisted locally on-device.\",15,false));sampleCard(\"Example Anime\",\"provider-demo\");}else if(section.equals(\"History\")){body.addView(text(\"Episode resume positions are stored locally and can be restored after restart.\",15,false));}else{body.addView(text(\"Providers are isolated behind MediaProvider so one failing source cannot break healthy sources.\",15,false));}}\n" +
                "  private void sampleCard(String title,String id){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(24,20,24,20);card.setBackgroundColor(Color.rgb(29,31,39));card.addView(text(title,18,true));Button fav=new Button(this);fav.setAllCaps(false);fav.setText(store.isFavorite(id)?\"Remove favorite\":\"Add favorite\");fav.setOnClickListener(v->{store.setFavorite(id,!store.isFavorite(id));fav.setText(store.isFavorite(id)?\"Remove favorite\":\"Add favorite\");});card.addView(fav);body.addView(card);}\n" +
                "  private TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setPadding(0,10,0,10);t.setGravity(Gravity.START);t.setTypeface(android.graphics.Typeface.create(\"sans-serif-condensed\",bold?1:0));return t;}\n" +
                "}\n";
    }

    private String genericActivity(String packageName, String name, String brief, List<String> requirements) {
        StringBuilder body = new StringBuilder();
        body.append("package ").append(packageName).append(";\n\n")
            .append("import android.app.Activity;\nimport android.graphics.Color;\nimport android.os.Bundle;\nimport android.view.Gravity;\nimport android.widget.LinearLayout;\nimport android.widget.ScrollView;\nimport android.widget.TextView;\n\n")
            .append("public class MainActivity extends Activity {\n")
            .append("  @Override public void onCreate(Bundle state) { super.onCreate(state);\n")
            .append("    LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(36,56,36,36); root.setBackgroundColor(Color.rgb(18,19,24));\n")
            .append("    root.addView(text(\"").append(java(name)).append("\", 26, true));\n")
            .append("    root.addView(text(\"").append(java(brief)).append("\", 16, false));\n");
        int count = Math.min(requirements == null ? 0 : requirements.size(), 10);
        for (int i = 0; i < count; i++) body.append("    root.addView(text(\"• ").append(java(requirements.get(i))).append("\", 14, false));\n");
        body.append("    ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll); }\n")
            .append("  private TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setPadding(0,10,0,10);t.setGravity(Gravity.START);t.setTypeface(android.graphics.Typeface.create(\"sans-serif-condensed\",bold?1:0));return t;}\n")
            .append("}\n");
        return body.toString();
    }

    private String readme(String name, String brief, List<String> requirements, List<String> tasks, boolean media) {
        StringBuilder out = new StringBuilder("# ").append(name).append("\n\n").append(brief == null ? "" : brief).append("\n\n## Requirements\n");
        if (requirements != null) for (String item : requirements) out.append("- ").append(item).append('\n');
        out.append("\n## Implementation tasks\n");
        if (tasks != null) for (String item : tasks) out.append("- [ ] ").append(item).append('\n');
        if (media) out.append("\n## Generated architecture\n- MediaProvider boundary for replaceable/failing sources\n- LocalLibraryStore for favorites and episode progress\n- Catalog, Library, History, and Providers navigation surfaces\n");
        out.append("\nGenerated locally by AIDao. Installation, publication, credential use, spending, and destructive actions require explicit user control.\n");
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

    private String cleanDisplayName(String value) {
        String v = value == null ? "Generated Android App" : value.trim();
        return v.isEmpty() ? "Generated Android App" : v.substring(0, Math.min(v.length(), 80));
    }

    private String slug(String value) {
        String s = value == null ? "app" : value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
        if (s.isEmpty()) s = "app";
        if (Character.isDigit(s.charAt(0))) s = "app" + s;
        return s.substring(0, Math.min(s.length(), 32));
    }

    private String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private String java(String s) { return escape((s == null ? "" : s).replace("\n", " ").replace("\r", " ")); }
    private String xml(String s) { return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;"); }
}
