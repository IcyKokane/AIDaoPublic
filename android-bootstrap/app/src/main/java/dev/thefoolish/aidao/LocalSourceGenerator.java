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

        List<GeneratedProject.FileEntry> files = new ArrayList<>();
        files.add(file("settings.gradle.kts", settings(safeName), "Create Android project shell"));
        files.add(file("build.gradle.kts", rootGradle(), "Create Android project shell"));
        files.add(file("app/build.gradle.kts", appGradle(packageName), "Create Android project shell"));
        files.add(file("app/src/main/AndroidManifest.xml", manifest(packageName, safeName), "Create Android project shell"));
        files.add(file("app/src/main/java/" + packagePath + "/MainActivity.java",
                activity(packageName, safeName, brief, requirements), "Build primary user flow"));
        files.add(file("README.md", readme(safeName, brief, requirements, tasks), "Document generated project"));
        files.add(file(".github/workflows/android.yml", workflow(), "Run Android CI"));

        List<String> verification = verify(packageName, files);
        return new GeneratedProject(safeName, packageName, files, verification);
    }

    private GeneratedProject.FileEntry file(String path, String content, String taskHint) {
        return new GeneratedProject.FileEntry(path, content, taskHint);
    }

    private List<String> verify(String packageName, List<GeneratedProject.FileEntry> files) {
        List<String> notes = new ArrayList<>();
        String[] required = {"settings.gradle.kts", "build.gradle.kts", "app/build.gradle.kts", "app/src/main/AndroidManifest.xml", ".github/workflows/android.yml"};
        for (String path : required) {
            boolean found = false;
            for (GeneratedProject.FileEntry file : files) if (path.equals(file.path)) { found = true; break; }
            notes.add((found ? "PASS " : "FAIL ") + path);
        }
        String mainPath = "app/src/main/java/" + packageName.replace('.', '/') + "/MainActivity.java";
        boolean main = false;
        for (GeneratedProject.FileEntry file : files) if (mainPath.equals(file.path)) { main = true; break; }
        notes.add((main ? "PASS " : "FAIL ") + "launcher source");
        return notes;
    }

    private String settings(String name) {
        return "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\n" +
                "dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\n" +
                "rootProject.name = \"" + escape(name) + "\"\ninclude(\":app\")\n";
    }

    private String rootGradle() {
        return "plugins {\n" +
                "    id(\"com.android.application\") version \"8.7.3\" apply false\n" +
                "}\n";
    }

    private String appGradle(String packageName) {
        return "plugins { id(\"com.android.application\") }\n\n" +
                "android {\n" +
                "    namespace = \"" + packageName + "\"\n" +
                "    compileSdk = 35\n" +
                "    defaultConfig { applicationId = \"" + packageName + "\"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = \"0.1.0\" }\n" +
                "}\n";
    }

    private String manifest(String packageName, String name) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "  <application android:theme=\"@style/AppTheme\" android:label=\"" + xml(name) + "\">\n" +
                "    <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
                "      <intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter>\n" +
                "    </activity>\n" +
                "  </application>\n" +
                "</manifest>\n";
    }

    private String activity(String packageName, String name, String brief, List<String> requirements) {
        StringBuilder body = new StringBuilder();
        body.append("package ").append(packageName).append(";\n\n")
            .append("import android.app.Activity;\nimport android.graphics.Color;\nimport android.os.Bundle;\nimport android.view.Gravity;\nimport android.widget.LinearLayout;\nimport android.widget.TextView;\n\n")
            .append("public class MainActivity extends Activity {\n")
            .append("  @Override public void onCreate(Bundle state) { super.onCreate(state);\n")
            .append("    LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(36,56,36,36); root.setBackgroundColor(Color.rgb(18,19,24));\n")
            .append("    TextView title = text(\"").append(java(name)).append("\", 26, true); root.addView(title);\n")
            .append("    TextView brief = text(\"").append(java(brief)).append("\", 16, false); root.addView(brief);\n");
        int count = Math.min(requirements == null ? 0 : requirements.size(), 8);
        for (int i = 0; i < count; i++) body.append("    root.addView(text(\"• ").append(java(requirements.get(i))).append("\", 14, false));\n");
        body.append("    setContentView(root); }\n")
            .append("  private TextView text(String value,int size,boolean bold){ TextView t=new TextView(this); t.setText(value); t.setTextColor(Color.WHITE); t.setTextSize(size); t.setPadding(0,10,0,10); t.setGravity(Gravity.START); if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); return t; }\n")
            .append("}\n");
        return body.toString();
    }

    private String readme(String name, String brief, List<String> requirements, List<String> tasks) {
        StringBuilder out = new StringBuilder("# ").append(name).append("\n\n").append(brief == null ? "" : brief).append("\n\n## Requirements\n");
        if (requirements != null) for (String item : requirements) out.append("- ").append(item).append('\n');
        out.append("\n## Implementation tasks\n");
        if (tasks != null) for (String item : tasks) out.append("- [ ] ").append(item).append('\n');
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
