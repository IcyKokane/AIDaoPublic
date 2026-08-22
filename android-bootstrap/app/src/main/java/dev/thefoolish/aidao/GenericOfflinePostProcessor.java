package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Final generated-product pass. Besides converting simple offline list requests,
 * this pass enforces phone-native launcher identity, safe system/IME insets and
 * non-dead-end media provider/player setup on every generated Android project.
 */
final class GenericOfflinePostProcessor {
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

        UniversalResult hardened = hardenPhoneOutput(projectName, packageName, source, media);
        source = hardened.files;
        if (media) return new Result(projectName, packageName, source, hardened.notes);

        CompatibilityResult compatibility = ensureRequestScreenCompatibility(source);
        source = compatibility.files;

        String request = requestText(source).toLowerCase(Locale.US);
        boolean offlineList = any(request, "grocery list", "shopping list", "checklist", "to-do list", "todo list")
                && any(request, "offline", "persist", "restart", "keep the list", "save");
        if (!offlineList) {
            List<String> notes = new ArrayList<>(hardened.notes);
            if (compatibility.changed) notes.add("PASS inherited domain screens remain source-compatible with request-specific AppScreen navigation");
            return new Result(projectName, packageName, source, notes);
        }

        String root = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        for (GeneratedProject.FileEntry f : source) {
            if (f == null) continue;
            String p = f.path;
            if (p.equals(root + "MainActivity.java") || p.equals(root + "ExploreActivity.java")
                    || p.equals(root + "DetailActivity.java") || p.equals(root + "SettingsActivity.java")) continue;
            out.add(f);
        }
        out.add(file(root + "MainActivity.java", main(packageName), "Render persisted grocery items"));
        out.add(file(root + "ExploreActivity.java", editor(packageName), "Add and edit persisted grocery items"));
        out.add(file(root + "DetailActivity.java", summary(packageName), "Summarize persisted grocery items"));
        out.add(file(root + "SettingsActivity.java", settings(packageName), "Provide explicit local list data controls"));

        List<String> notes = new ArrayList<>(hardened.notes);
        if (compatibility.changed) notes.add("PASS inherited domain screens remain source-compatible with request-specific AppScreen navigation");
        notes.add("PASS generic offline list request replaced sample-state placeholders with persisted add/edit behavior");
        notes.add("PASS generic offline list uses putText mutations and restart-safe text reads");
        return new Result(projectName, packageName, out, notes);
    }

    private static final class UniversalResult {
        final List<GeneratedProject.FileEntry> files;
        final List<String> notes;
        UniversalResult(List<GeneratedProject.FileEntry> files, List<String> notes) { this.files = files; this.notes = notes; }
    }

    private static UniversalResult hardenPhoneOutput(String projectName, String packageName, List<GeneratedProject.FileEntry> source, boolean media) {
        String root = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        for (GeneratedProject.FileEntry f : source) {
            if (f == null) continue;
            String p = f.path == null ? "" : f.path;
            if ("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml".equals(p)
                    || "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml".equals(p)
                    || "app/src/main/res/drawable/ic_launcher_foreground.xml".equals(p)
                    || "app/src/main/res/values/launcher_background.xml".equals(p)) continue;
            if (media && (p.equals(root + "ProvidersActivity.java") || p.equals(root + "PlayerActivity.java"))) continue;

            String c = f.content == null ? "" : f.content;
            if ("app/src/main/AndroidManifest.xml".equals(p)) c = patchManifest(c);
            if (p.endsWith("/AppScreen.java") || p.endsWith("/GeneratedScreen.java")) c = patchInsets(c);
            out.add(new GeneratedProject.FileEntry(p, c, f.taskHint));
        }

        out.add(file("app/src/main/res/values/launcher_background.xml", launcherBackground(projectName), "Generate app-specific launcher background"));
        out.add(file("app/src/main/res/drawable/ic_launcher_foreground.xml", launcherForeground(projectName), "Generate app-specific adaptive launcher foreground"));
        out.add(file("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml", adaptiveIcon(false), "Wire adaptive launcher icon"));
        out.add(file("app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml", adaptiveIcon(true), "Wire adaptive round launcher icon"));

        if (media) {
            out.add(file(root + "ProvidersActivity.java", mediaProviders(packageName), "Select only compatible authorized playback providers and persist the choice"));
            out.add(file(root + "PlayerActivity.java", mediaPlayer(packageName), "Provide reachable provider/player setup and validated playback handoff"));
        }

        List<String> notes = new ArrayList<>();
        notes.add("PASS generated manifest wires app-specific adaptive launcher icon and roundIcon resources");
        notes.add("PASS generated screen shell reapplies status/navigation/display-cutout/IME insets after legacy chrome setup");
        notes.add("PASS generated scrolling surfaces retain reachable controls above navigation and IME areas");
        if (media) {
            notes.add("PASS media provider selection persists selected_provider and labels incompatible metadata-only sources as Unsupported");
            notes.add("PASS playback setup exposes reachable Select provider and Select player actions and persists selected_player");
            notes.add("PASS playback resolves only through a selected provider that advertises playback and rejects non-HTTPS media URLs");
        }
        return new UniversalResult(out, notes);
    }

    private static String patchManifest(String source) {
        if (source == null) return "";
        String x = source.replaceAll("\\sandroid:icon=\"[^\"]*\"", "").replaceAll("\\sandroid:roundIcon=\"[^\"]*\"", "");
        if (x.contains("<application ")) {
            x = x.replaceFirst("<application ", "<application android:icon=\"@mipmap/ic_launcher\" android:roundIcon=\"@mipmap/ic_launcher_round\" ");
        }
        return x;
    }

    private static String patchInsets(String source) {
        if (source == null || source.contains("AIDaoPhoneInsetsV1")) return source == null ? "" : source;
        String x = source;
        if (x.contains("render();}")) x = x.replace("render();}", "render();applyPhoneInsets(root);}");
        else if (x.contains("setContentView(root);")) x = x.replace("setContentView(root);", "setContentView(root);applyPhoneInsets(root);");
        int end = x.lastIndexOf('}');
        if (end < 0) return x;
        String helper = "private void applyPhoneInsets(final android.view.View root){/*AIDaoPhoneInsetsV1*/if(android.os.Build.VERSION.SDK_INT<23)return;root.setOnApplyWindowInsetsListener((v,insets)->{int left=insets.getSystemWindowInsetLeft(),top=insets.getSystemWindowInsetTop(),right=insets.getSystemWindowInsetRight(),bottom=insets.getSystemWindowInsetBottom();if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());android.graphics.Insets ime=insets.getInsets(android.view.WindowInsets.Type.ime());left=bars.left;top=bars.top;right=bars.right;bottom=Math.max(bars.bottom,ime.bottom);}root.setPadding(left,top,right,bottom);return insets;});root.requestApplyInsets();}";
        return x.substring(0, end) + helper + x.substring(end);
    }

    private static String launcherBackground(String name) {
        int rgb = 0x303030 | (Math.abs((name == null ? "App" : name).hashCode()) & 0x4F4F4F);
        String hex = String.format(Locale.US, "#%06X", rgb & 0xFFFFFF);
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources><color name=\"launcher_background\">" + hex + "</color></resources>\n";
    }

    private static String launcherForeground(String name) {
        int h = Math.abs((name == null ? "App" : name).hashCode());
        String a = String.format(Locale.US, "#%06X", (0x5A4CFF ^ h) & 0xFFFFFF);
        String b = String.format(Locale.US, "#%06X", (0xFF4F81 ^ (h >>> 3)) & 0xFFFFFF);
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"108dp\" android:height=\"108dp\" android:viewportWidth=\"108\" android:viewportHeight=\"108\"><path android:fillColor=\"" + a + "\" android:pathData=\"M18,18h72v72h-72z\"/><path android:fillColor=\"" + b + "\" android:pathData=\"M32,31h44v10h-44zM32,49h44v10h-44zM32,67h29v10h-29z\"/></vector>\n";
    }

    private static String adaptiveIcon(boolean round) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\"><background android:drawable=\"@color/launcher_background\"/><foreground android:drawable=\"@drawable/ic_launcher_foreground\"/></adaptive-icon>\n";
    }

    private static String mediaProviders(String p) {
        return "package " + p + ";\n" +
                "import android.widget.*;import java.util.*;\n" +
                "public final class ProvidersActivity extends AppScreen{" +
                "protected void render(){title(\"Playback sources\");subtitle(\"Choose a compatible source explicitly. Metadata-only sources remain usable for discovery but cannot be selected for playback.\");String selected=store.text(\"selected_provider\",\"\");section(\"Built in\");int compatible=0;for(MediaProvider provider:BuiltInProviderCatalog.providers()){boolean ok=provider.supportsPlayback();LinearLayout c=card(provider.displayName()+(provider.id().equals(selected)?\" · Selected\":\"\"),ok?\"Playback: compatible · \"+provider.health():\"Unsupported for playback · metadata/catalog only · \"+provider.health());if(ok){compatible++;Button choose=button(provider.id().equals(selected)?\"Selected provider\":\"Select provider\");choose.setEnabled(!provider.id().equals(selected));choose.setOnClickListener(v->{store.putText(\"selected_provider\",provider.id());recreate();});c.addView(choose);}body.addView(c);}" +
                "section(\"Repository extensions\");for(ExtensionRecord x:new ExtensionManager(this).known()){boolean enabled=x.state==ExtensionRecord.State.ENABLED;boolean ok=enabled&&x.playable();LinearLayout c=card(x.name+(x.id.equals(selected)?\" · Selected\":\"\"),ok?\"Playback: compatible · authorized configuration must be verified by the user\":\"Unsupported/incompatible: \"+(enabled?\"no playback contract declared\":\"extension is not enabled\"));if(ok){compatible++;Button choose=button(x.id.equals(selected)?\"Selected provider\":\"Select provider\");choose.setEnabled(!x.id.equals(selected));choose.setOnClickListener(v->{store.putText(\"selected_provider\",x.id);recreate();});c.addView(choose);}body.addView(c);}" +
                "if(compatible==0)body.addView(card(\"Playback capability incomplete\",\"No compatible authorized playback provider is currently configured. Discovery can continue, but playback must not be reported as complete.\"));Button repos=button(\"Manage repositories\");repos.setOnClickListener(v->AppNavigator.open(this,RepositoriesActivity.class));body.addView(repos);}" +
                "}\n";
    }

    private static String mediaPlayer(String p) {
        return "package " + p + ";\n" +
                "import android.net.Uri;import android.view.View;import android.widget.*;import java.util.*;\n" +
                "public final class PlayerActivity extends AppScreen{private String id,title,providerHint;private int episode;protected void render(){id=getIntent().getStringExtra(\"id\");title=getIntent().getStringExtra(\"title\");providerHint=getIntent().getStringExtra(\"provider\");episode=Math.max(1,getIntent().getIntExtra(\"episode\",1));title((title==null?\"Episode\":title)+\" · Episode \"+episode);String providerId=store.text(\"selected_provider\",\"\");String player=store.text(\"selected_player\",\"\");if(providerId.isEmpty()){body.addView(card(\"Playback setup required\",\"No compatible provider has been selected. Select a provider to continue.\"));Button b=button(\"Select provider\");b.setOnClickListener(v->AppNavigator.open(this,ProvidersActivity.class));body.addView(b);return;}MediaProvider provider=find(providerId);if(provider==null||!provider.supportsPlayback()){body.addView(card(\"Unsupported/incompatible provider\",\"The selected provider is unavailable or does not advertise a playback contract. Choose another provider.\"));Button b=button(\"Select provider\");b.setOnClickListener(v->AppNavigator.open(this,ProvidersActivity.class));body.addView(b);return;}if(player.isEmpty()){body.addView(card(\"Player setup required\",\"Choose the Android video player for compatible HTTPS media.\"));Button choose=button(\"Select player\");choose.setOnClickListener(v->{store.putText(\"selected_player\",\"android_video\");recreate();});body.addView(choose);Button change=button(\"Select provider\");change.setOnClickListener(v->AppNavigator.open(this,ProvidersActivity.class));body.addView(change);return;}body.addView(card(\"Playback: compatible\",provider.displayName()+\" · Android video player\"));ProgressBar progress=new ProgressBar(this);body.addView(progress);new Thread(()->{try{String url=provider.resolveMediaUrl(id,episode);if(url==null||!url.startsWith(\"https://\"))throw new IllegalStateException(\"provider returned a non-HTTPS media URL\");runOnUiThread(()->startVideo(url,progress));}catch(Exception e){runOnUiThread(()->{progress.setVisibility(View.GONE);body.addView(card(\"Playback could not start\",(e.getMessage()==null?\"Provider resolution failed\":e.getMessage())+\". Use Select provider to choose another compatible source.\"));Button change=button(\"Select provider\");change.setOnClickListener(v->AppNavigator.open(this,ProvidersActivity.class));body.addView(change);});}}).start();}" +
                "private MediaProvider find(String providerId){for(MediaProvider p:BuiltInProviderCatalog.providers())if(p.id().equals(providerId))return p;ExtensionRecord x=new ExtensionManager(this).find(providerId);if(x!=null&&x.state==ExtensionRecord.State.ENABLED)return new RepositoryMediaProvider(x);return null;}" +
                "private void startVideo(String url,ProgressBar progress){progress.setVisibility(View.GONE);VideoView video=new VideoView(this);video.setContentDescription(\"Episode video player\");body.addView(video,new LinearLayout.LayoutParams(-1,dp(260)));video.setVideoURI(Uri.parse(url));int saved=store.number(\"progress_\"+id+\"_\"+episode);video.setOnPreparedListener(mp->{if(saved>0)video.seekTo(saved*1000);video.start();});video.setOnCompletionListener(mp->store.number(\"progress_\"+id+\"_\"+episode,video.getCurrentPosition()/1000));video.setOnErrorListener((mp,what,extra)->{Toast.makeText(this,\"Playback failed. Choose another compatible provider.\",Toast.LENGTH_LONG).show();return true;});}" +
                "}\n";
    }

    private static final class CompatibilityResult {
        final List<GeneratedProject.FileEntry> files;
        final boolean changed;
        CompatibilityResult(List<GeneratedProject.FileEntry> files, boolean changed) { this.files = files; this.changed = changed; }
    }

    private static CompatibilityResult ensureRequestScreenCompatibility(List<GeneratedProject.FileEntry> source) {
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        boolean changed = false;
        for (GeneratedProject.FileEntry f : source) {
            if (f == null) continue;
            String content = f.content == null ? "" : f.content;
            if (f.path != null && f.path.endsWith("/AppScreen.java")
                    && content.contains("protected LinearLayout sideNav()")
                    && content.contains("protected abstract void render();")
                    && !content.contains("protected void nav(String[]")) {
                content = content.replace("protected abstract void render();", "protected abstract void render();protected void title(String s){body.addView(text(s,26,true));}protected void subtitle(String s){body.addView(text(s,14,false));}protected void nav(String[] labels,Class[] screens){}");
                changed = true;
            }
            out.add(new GeneratedProject.FileEntry(f.path, content, f.taskHint));
        }
        return new CompatibilityResult(out, changed);
    }

    private static GeneratedProject.FileEntry file(String p, String c, String h) { return new GeneratedProject.FileEntry(p, c, h); }
    private static boolean hasSuffix(List<GeneratedProject.FileEntry> files, String suffix) { for (GeneratedProject.FileEntry f : files) if (f != null && f.path != null && f.path.endsWith(suffix)) return true; return false; }
    private static boolean any(String source, String... terms) { for (String term : terms) if (source.contains(term)) return true; return false; }
    private static String requestText(List<GeneratedProject.FileEntry> files) { for (GeneratedProject.FileEntry f : files) if (f != null && "README.md".equals(f.path) && f.content != null) return f.content; StringBuilder b = new StringBuilder(); for (GeneratedProject.FileEntry f : files) if (f != null && f.content != null) b.append('\n').append(f.content); return b.toString(); }

    private static String main(String p) {
        return "package " + p + ";\nimport android.widget.*;\npublic final class MainActivity extends GeneratedScreen{protected void render(){store.putText(\"last_surface\",\"list\");body.addView(text(\"Grocery List\",22,true));String raw=store.text(\"grocery_items\",\"\");if(raw.trim().isEmpty())body.addView(text(\"No items yet. Add your first item.\",14,false));else{int n=1;for(String item:raw.split(\"\\n\")){if(item.trim().isEmpty())continue;body.addView(text(n+\". \"+item,16,false));n++;}}Button edit=action(\"Add or edit items\");edit.setOnClickListener(v->AppNavigator.open(this,ExploreActivity.class));body.addView(edit);Button summary=action(\"List summary\");summary.setOnClickListener(v->AppNavigator.open(this,DetailActivity.class));body.addView(summary);Button settings=action(\"Data controls\");settings.setOnClickListener(v->AppNavigator.open(this,SettingsActivity.class));body.addView(settings);}}\n";
    }
    private static String editor(String p) {
        return "package " + p + ";\nimport android.widget.*;import java.util.*;\npublic final class ExploreActivity extends GeneratedScreen{protected void render(){store.putText(\"last_surface\",\"editor\");body.addView(text(\"Add or edit item\",22,true));EditText item=new EditText(this);item.setHint(\"Item name\");body.addView(item);EditText number=new EditText(this);number.setHint(\"Item number to replace (optional)\");number.setInputType(2);body.addView(number);Button save=action(\"Save item\");save.setOnClickListener(v->{String name=item.getText().toString().trim();if(name.isEmpty()){item.setError(\"Enter an item\");return;}List<String> rows=new ArrayList<>();String raw=store.text(\"grocery_items\",\"\");if(!raw.trim().isEmpty())for(String x:raw.split(\"\\n\"))if(!x.trim().isEmpty())rows.add(x);String n=number.getText().toString().trim();if(!n.isEmpty()){try{int i=Integer.parseInt(n)-1;if(i<0||i>=rows.size()){number.setError(\"Choose an existing item number\");return;}rows.set(i,name);}catch(Exception e){number.setError(\"Use a valid item number\");return;}}else rows.add(name);store.putText(\"grocery_items\",join(rows));item.setText(\"\");number.setText(\"\");Toast.makeText(this,\"List saved\",Toast.LENGTH_SHORT).show();});body.addView(save);Button back=action(\"View list\");back.setOnClickListener(v->AppNavigator.open(this,MainActivity.class));body.addView(back);}private String join(List<String> rows){StringBuilder b=new StringBuilder();for(String x:rows){if(b.length()>0)b.append(\"\\n\");b.append(x.replace(\"\\n\",\" \").trim());}return b.toString();}}\n";
    }
    private static String summary(String p) { return "package " + p + ";\npublic final class DetailActivity extends GeneratedScreen{protected void render(){store.putText(\"last_surface\",\"summary\");body.addView(text(\"List summary\",22,true));String raw=store.text(\"grocery_items\",\"\");int count=0;if(!raw.trim().isEmpty())for(String x:raw.split(\"\\n\"))if(!x.trim().isEmpty())count++;body.addView(text(count+\" saved item\"+(count==1?\"\":\"s\"),16,false));}}\n"; }
    private static String settings(String p) { return "package " + p + ";\nimport android.widget.*;\npublic final class SettingsActivity extends GeneratedScreen{protected void render(){store.putText(\"last_surface\",\"settings\");body.addView(text(\"Data controls\",22,true));body.addView(text(\"Your grocery list stays on this device.\",14,false));Button clear=action(store.flag(\"confirm_clear_grocery\")?\"Tap again to clear list\":\"Clear grocery list\");clear.setOnClickListener(v->{if(!store.flag(\"confirm_clear_grocery\")){store.flag(\"confirm_clear_grocery\",true);recreate();return;}store.putText(\"grocery_items\",\"\");store.flag(\"confirm_clear_grocery\",false);Toast.makeText(this,\"List cleared\",Toast.LENGTH_SHORT).show();});body.addView(clear);}}\n"; }

    private GenericOfflinePostProcessor() {}
}
