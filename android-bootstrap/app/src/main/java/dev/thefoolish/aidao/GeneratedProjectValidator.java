package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic release-oriented validation for generated Android source trees. */
final class GeneratedProjectValidator {
    private static final int MAX_FILE_COUNT = 400;
    private static final int MAX_FILE_CHARS = 1_000_000;
    private static final long MAX_TOTAL_CHARS = 8_000_000L;
    private static final String[] UNSUPPORTED_PRIVILEGED_PERMISSIONS = {
            "android.permission.REQUEST_INSTALL_PACKAGES","android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.WRITE_SECURE_SETTINGS","android.permission.BIND_DEVICE_ADMIN",
            "android.permission.BIND_ACCESSIBILITY_SERVICE"
    };
    private static final String[] CORRUPTED_JAVA_MARKERS = {
            "android.widget.android.widget.","android.graphics.android.graphics.",
            "android.content.android.content.","android.app.android.app."
    };
    private static final String[] PLACEHOLDER_COMPLETION_MARKERS = {
            "Save local sample state","placeholder data","sample data only",
            "Playback surface placeholder","DemoProvider"
    };

    static final class Result {
        final List<String> notes; final boolean valid;
        Result(List<String> notes, boolean valid) { this.notes=notes; this.valid=valid; }
    }

    Result validate(GeneratedProject project) {
        if (project==null) { List<String> n=new ArrayList<>(); n.add("FAIL generated project is null"); return new Result(n,false); }
        return validateRaw(project.packageName, project.files);
    }

    static Result validateRaw(String packageName, List<GeneratedProject.FileEntry> files) {
        List<String> notes=new ArrayList<>(); boolean ok=true;
        if (packageName==null||packageName.trim().isEmpty()) { notes.add("FAIL generated package name is empty"); ok=false; packageName="invalid"; }
        else if (!packageName.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")) { notes.add("FAIL generated package name is not a valid dotted Java package: "+packageName); ok=false; }
        else notes.add("PASS generated package name syntax");
        if (files==null) { notes.add("FAIL generated file list is null"); return new Result(notes,false); }
        if (files.size()>MAX_FILE_COUNT) { notes.add("FAIL generated tree exceeds bounded file count: "+files.size()); ok=false; }

        Set<String> seen=new HashSet<>(); long totalChars=0; boolean corruptJava=false,placeholderCompletion=false;
        for (GeneratedProject.FileEntry file:files) {
            if (file==null||file.path==null||file.path.trim().isEmpty()) { notes.add("FAIL generated file has an empty path"); ok=false; continue; }
            if (unsafePath(file.path)) { notes.add("FAIL unsafe generated path: "+file.path); ok=false; }
            if (!seen.add(file.path)) { notes.add("FAIL duplicate generated path: "+file.path); ok=false; }
            if (file.content==null) { notes.add("FAIL null generated content: "+file.path); ok=false; continue; }
            if (file.content.indexOf('\u0000')>=0) { notes.add("FAIL NUL byte marker in generated text: "+file.path); ok=false; }
            if (file.content.length()>MAX_FILE_CHARS) { notes.add("FAIL generated file exceeds bounded text size: "+file.path); ok=false; }
            if (file.path.endsWith(".java")) {
                for (String marker:CORRUPTED_JAVA_MARKERS) if (file.content.contains(marker)) { notes.add("FAIL corrupted Java package qualifier '"+marker+"' in "+file.path); ok=false; corruptJava=true; }
                if (file.path.startsWith("app/src/main/java/")) for (String marker:PLACEHOLDER_COMPLETION_MARKERS) if (file.content.contains(marker)) { notes.add("FAIL executable generated source still contains placeholder completion marker '"+marker+"' in "+file.path); ok=false; placeholderCompletion=true; }
            }
            totalChars+=file.content.length();
        }
        if (totalChars>MAX_TOTAL_CHARS) { notes.add("FAIL generated tree exceeds bounded total text size: "+totalChars+" characters"); ok=false; } else notes.add("PASS bounded generated source size");
        if (!corruptJava) notes.add("PASS generated Java package-qualifier hygiene");
        if (!placeholderCompletion) notes.add("PASS executable source contains no fake-completion placeholders");

        String[] required={"settings.gradle.kts","build.gradle.kts","app/build.gradle.kts","app/src/main/AndroidManifest.xml","app/src/main/res/values/strings.xml","app/src/main/res/values/styles.xml"};
        for (String path:required) { if (!hasPath(files,path)) { notes.add("FAIL missing required Android file: "+path); ok=false; } else notes.add("PASS required Android file: "+path); }
        String packageRoot="app/src/main/java/"+packageName.replace('.','/')+"/";
        if (!hasPath(files,packageRoot+"MainActivity.java")) { notes.add("FAIL missing launcher source under package root"); ok=false; } else notes.add("PASS launcher source under package root");
        for (GeneratedProject.FileEntry file:files) if (file!=null&&file.path!=null&&file.content!=null&&file.path.startsWith(packageRoot)&&file.path.endsWith(".java")&&!declaresPackage(file.content,packageName)) { notes.add("FAIL Java source package does not match generated package: "+file.path); ok=false; }

        GeneratedProject.FileEntry appGradle=find(files,"app/build.gradle.kts");
        if (appGradle!=null) {
            String body=appGradle.content==null?"":appGradle.content;
            String namespace=quotedAssignment(body,"namespace"),applicationId=quotedAssignment(body,"applicationId");
            if (!packageName.equals(namespace)) { notes.add("FAIL Gradle namespace does not match generated package"); ok=false; } else notes.add("PASS Gradle namespace matches generated package");
            if (!packageName.equals(applicationId)) { notes.add("FAIL Gradle applicationId does not match generated package"); ok=false; } else notes.add("PASS Gradle applicationId matches generated package");
            int compileSdk=numericAssignment(body,"compileSdk"),targetSdk=numericAssignment(body,"targetSdk"),minSdk=numericAssignment(body,"minSdk");
            if (compileSdk<33||targetSdk<33||minSdk<23||minSdk>targetSdk||targetSdk>compileSdk) { notes.add("FAIL generated Android SDK bounds are inconsistent or below the v1 baseline"); ok=false; } else notes.add("PASS generated Android SDK bounds");
        }

        GeneratedProject.FileEntry manifest=find(files,"app/src/main/AndroidManifest.xml");
        if (manifest!=null) {
            String body=manifest.content==null?"":manifest.content;
            if (!body.contains("android.intent.action.MAIN")||!body.contains("android.intent.category.LAUNCHER")) { notes.add("FAIL manifest does not declare a launcher activity"); ok=false; } else notes.add("PASS manifest launcher declaration");
            if (!body.contains("android:name=\".MainActivity\"")) { notes.add("FAIL manifest launcher source is not .MainActivity"); ok=false; }
            if (body.contains("android:debuggable=\"true\"")) { notes.add("FAIL generated manifest hard-codes debuggable=true"); ok=false; }
            for (String permission:UNSUPPORTED_PRIVILEGED_PERMISSIONS) if (body.contains(permission)) { notes.add("FAIL generated manifest requests unsupported privileged permission: "+permission); ok=false; }
            if (!containsUnsupportedPrivilege(body)) notes.add("PASS no unsupported privileged Android permissions");
            if (!body.contains("android:icon=\"@mipmap/ic_launcher\"")) { notes.add("FAIL generated manifest does not wire an app-specific launcher icon"); ok=false; } else notes.add("PASS manifest launcher icon wiring");
            if (!body.contains("android:roundIcon=\"@mipmap/ic_launcher_round\"")) { notes.add("FAIL generated manifest does not wire a round launcher icon"); ok=false; } else notes.add("PASS manifest round icon wiring");
        }
        String[] iconPaths={"app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml","app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml","app/src/main/res/drawable/ic_launcher_foreground.xml"};
        for (String iconPath:iconPaths) { if (!hasPath(files,iconPath)) { notes.add("FAIL missing generated launcher icon resource: "+iconPath); ok=false; } else notes.add("PASS generated launcher icon resource: "+iconPath); }

        // Validate whichever reusable shell survives the postprocessing pipeline.
        GeneratedProject.FileEntry shell=find(files,packageRoot+"AppScreen.java");
        if (shell==null) shell=find(files,packageRoot+"GeneratedScreen.java");
        if (shell==null) { notes.add("FAIL generated project has no reusable phone screen shell"); ok=false; }
        else {
            String body=shell.content==null?"":shell.content;
            if (!(body.contains("WindowInsets")&&body.contains("systemBars")&&body.contains("ime"))) { notes.add("FAIL generated screen shell does not handle status/navigation/IME insets"); ok=false; } else notes.add("PASS generated screen shell handles system and IME insets");
            if (!body.contains("ScrollView")) { notes.add("FAIL generated screen shell is not scroll-safe"); ok=false; } else notes.add("PASS generated screen shell supports scrolling");
            if (!(body.contains("displayCutout")||body.contains("getSystemWindowInsetLeft"))) { notes.add("FAIL generated screen shell does not account for lateral/cutout insets"); ok=false; } else notes.add("PASS generated screen shell handles lateral/cutout insets");
        }

        boolean mediaProject=hasPath(files,packageRoot+"MediaProvider.java")||hasPath(files,packageRoot+"PlayerActivity.java");
        if (mediaProject) {
            GeneratedProject.FileEntry providers=find(files,packageRoot+"ProvidersActivity.java"),player=find(files,packageRoot+"PlayerActivity.java");
            String providersBody=providers==null||providers.content==null?"":providers.content;
            String playerBody=player==null||player.content==null?"":player.content;
            String mediaBody=providersBody+"\n"+playerBody;
            if (!(mediaBody.contains("selected_provider")&&mediaBody.contains("selected_player"))) { notes.add("FAIL media app does not persist explicit provider/player selections"); ok=false; } else notes.add("PASS media app persists provider/player selections");
            if (!(mediaBody.contains("Select provider")&&mediaBody.contains("Select player"))) { notes.add("FAIL media playback can dead-end without reachable provider/player setup actions"); ok=false; } else notes.add("PASS media playback exposes setup actions");
            if (!(mediaBody.contains("Unsupported")||mediaBody.toLowerCase().contains("incompatible"))) { notes.add("FAIL media provider UI does not surface compatibility reasons"); ok=false; } else notes.add("PASS media provider UI surfaces compatibility state");
            if (!mediaBody.contains("Playback capability incomplete")) { notes.add("FAIL media provider UI can conceal an all-incompatible provider state"); ok=false; } else notes.add("PASS media provider UI exposes capability-incomplete state");
            if (!(playerBody.contains("resolveMediaUrl")&&playerBody.contains("https://")&&playerBody.contains("VideoView")&&playerBody.contains("setVideoURI"))) { notes.add("FAIL media player lacks validated provider-to-video handoff"); ok=false; } else notes.add("PASS media player uses provider-declared HTTPS playback handoff");
        }

        GeneratedProject.FileEntry workflow=find(files,".github/workflows/android.yml");
        if (workflow!=null) {
            String lower=(workflow.content==null?"":workflow.content).toLowerCase();
            if (lower.contains("pull_request_target")||lower.contains("permissions: write-all")||lower.contains("secrets.")) { notes.add("FAIL generated workflow contains a privileged trigger/permission/secret reference"); ok=false; } else notes.add("PASS generated workflow contains no privileged trigger/secret reference");
        }
        if (files.size()<12) { notes.add("FAIL generated tree is too small for v1 acceptance: "+files.size()+" files"); ok=false; } else notes.add("PASS nontrivial generated tree: "+files.size()+" files");
        return new Result(notes,ok);
    }

    private static boolean unsafePath(String path){return path.startsWith("/")||path.startsWith("\\")||path.contains("../")||path.contains("..\\")||path.equals("..")||path.indexOf('\u0000')>=0||path.contains(":")||path.contains("\\");}
    private static boolean declaresPackage(String source,String packageName){return Pattern.compile("(?m)^\\s*package\\s+"+Pattern.quote(packageName)+"\\s*;").matcher(source).find();}
    private static String quotedAssignment(String source,String name){Matcher m=Pattern.compile("\\b"+Pattern.quote(name)+"\\s*=\\s*\"([^\"]+)\"").matcher(source);return m.find()?m.group(1):null;}
    private static int numericAssignment(String source,String name){Matcher m=Pattern.compile("\\b"+Pattern.quote(name)+"\\s*=\\s*(\\d+)").matcher(source);return m.find()?Integer.parseInt(m.group(1)):-1;}
    private static boolean containsUnsupportedPrivilege(String body){for(String permission:UNSUPPORTED_PRIVILEGED_PERMISSIONS)if(body.contains(permission))return true;return false;}
    private static boolean hasPath(List<GeneratedProject.FileEntry> files,String path){return find(files,path)!=null;}
    private static GeneratedProject.FileEntry find(List<GeneratedProject.FileEntry> files,String path){for(GeneratedProject.FileEntry file:files)if(file!=null&&path.equals(file.path))return file;return null;}
}
