package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Applies explicit request fidelity after broad domain generation.
 * Media projects keep their provider-specific pipeline; ordinary apps can override
 * identity, navigation, theme, persistence, and requested behavior without falling
 * back to a compile-only shell.
 */
final class RequestFidelityPostProcessor {
    static final class Result {
        final String projectName;
        final String packageName;
        final List<GeneratedProject.FileEntry> files;
        final List<String> notes;
        Result(String a, String b, List<GeneratedProject.FileEntry> c, List<String> d) {
            projectName = a; packageName = b; files = c; notes = d;
        }
    }

    static Result process(String projectName, String packageName, List<GeneratedProject.FileEntry> incoming) {
        List<GeneratedProject.FileEntry> source = incoming == null ? new ArrayList<>() : new ArrayList<>(incoming);
        if (hasSuffix(source, "/MediaProvider.java") || hasSuffix(source, "/AnimeItem.java"))
            return new Result(projectName, packageName, source, new ArrayList<>());

        String request = requestText(source).toLowerCase(Locale.US);
        boolean note = any(request, "notepad", "note app", "notes", "document editor");
        boolean workout = any(request, "workout", "exercise", "weight and reps", "rpg stats");
        boolean pantry = any(request, "pantry", "inventory") && any(request, "quantity", "stock", "items");
        boolean logo = any(request, "app logo", "app icon", " logo", " icon");
        boolean sidebar = any(request, "sidebar", "side bar", "navigation drawer", "drawer navigation");
        boolean topTabs = any(request, "top tab", "top tabs", "top-tab", "tab navigation");
        boolean purple = request.contains("purple");
        boolean red = request.contains("red");
        boolean teal = request.contains("teal");
        boolean orange = request.contains("orange");
        boolean lock = any(request, "lock notes", "lock note", "can't be edited", "cannot be edited", "read-only", "read only");

        String name = inferName(projectName, request, note, workout, pantry);
        if (!note && !workout && !pantry && !logo && !sidebar && !topTabs && !purple && !red && !teal && !orange) {
            if (name.equals(projectName)) return new Result(projectName, packageName, source, new ArrayList<>());
            List<GeneratedProject.FileEntry> out = new ArrayList<>();
            for (GeneratedProject.FileEntry f : source) {
                if (f == null) continue;
                if ("app/src/main/res/values/strings.xml".equals(f.path) || "app/src/main/AndroidManifest.xml".equals(f.path)) continue;
                out.add(f);
            }
            out.add(file("app/src/main/res/values/strings.xml", strings(name), "Normalize prompt-like product identity"));
            out.add(file("app/src/main/AndroidManifest.xml", manifest(source, name, false), "Synchronize concise launcher identity"));
            List<String> notes = new ArrayList<>();
            notes.add("PASS generic prompt identity normalized to " + name + " without changing product behavior");
            return new Result(name, packageName, out, notes);
        }

        String root = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        for (GeneratedProject.FileEntry f : source) {
            if (f == null) continue;
            String p = f.path;
            boolean replace = "app/src/main/res/values/strings.xml".equals(p)
                    || "app/src/main/res/values/colors.xml".equals(p)
                    || "app/src/main/AndroidManifest.xml".equals(p)
                    || p.equals(root + "AppScreen.java")
                    || p.equals(root + "MainActivity.java")
                    || (note && (p.equals(root + "EditorActivity.java") || p.equals(root + "LibraryActivity.java")))
                    || (workout && (p.equals(root + "TimelineActivity.java") || p.equals(root + "ReportsActivity.java")))
                    || (pantry && (p.equals(root + "ExploreActivity.java") || p.equals(root + "DetailActivity.java")));
            if (!replace) out.add(f);
        }

        out.add(file("app/src/main/res/values/strings.xml", strings(name), "Use concise generated product identity"));
        out.add(file("app/src/main/res/values/colors.xml", colors(purple, red, teal, orange), "Honor requested visual color direction"));
        if (logo) out.add(file("app/src/main/res/drawable/ic_generated_app.xml", icon(purple, red, teal, orange), "Generate distinct launcher icon"));
        out.add(file("app/src/main/AndroidManifest.xml", manifest(source, name, logo), "Wire generated product identity and icon"));
        out.add(file(root + "AppScreen.java", appScreen(packageName, name, sidebar, topTabs, purple, red, teal, orange, note, workout, pantry), "Honor requested navigation and theme architecture"));

        if (note) {
            out.add(file(root + "MainActivity.java", noteHome(packageName), "Show persisted notes and draft status"));
            out.add(file(root + "EditorActivity.java", noteEditor(packageName, lock), "Persist notes and enforce note lock/read-only state"));
            out.add(file(root + "LibraryActivity.java", noteLibrary(packageName), "Browse and reopen saved notes"));
        } else if (workout) {
            out.add(file(root + "MainActivity.java", workoutHome(packageName), "Show automatic RPG workout progression"));
            out.add(file(root + "TimelineActivity.java", workoutLog(packageName), "Track preset exercise, weight, reps, and history"));
            out.add(file(root + "ReportsActivity.java", workoutStats(packageName), "Calculate RPG stats from workout history"));
        } else if (pantry) {
            out.add(file(root + "MainActivity.java", pantryHome(packageName), "Render restart-safe pantry inventory"));
            out.add(file(root + "ExploreActivity.java", pantryEditor(packageName), "Add and edit pantry quantities"));
            out.add(file(root + "DetailActivity.java", pantrySummary(packageName), "Summarize pantry inventory"));
        }

        List<String> notes = new ArrayList<>();
        notes.add("PASS request-fidelity pass selected concise product identity " + name);
        if (logo) notes.add("PASS explicit app-logo request generated and wired a distinct launcher vector");
        if (sidebar) notes.add("PASS explicit sidebar request uses persistent sideNav navigation on generated screens");
        if (topTabs) notes.add("PASS explicit top-tab request uses generated topNav navigation rather than a bottom bar");
        if (purple) notes.add("PASS explicit purple theme direction applied to generated resources and primary actions");
        if (red) notes.add("PASS explicit red theme direction applied to generated resources and branding");
        if (teal) notes.add("PASS explicit teal theme direction applied to generated resources and primary actions");
        if (orange) notes.add("PASS explicit orange theme direction applied to generated resources and branding");
        if (note && lock) notes.add("PASS note locking persists state and disables editor input while locked");
        if (note) notes.add("PASS note creation assigns distinct persisted note identities so multiple notes can coexist");
        if (workout) notes.add("PASS workout model uses a built-in exercise catalog and derives XP/level/stats automatically");
        if (pantry) notes.add("PASS pantry inventory persists item quantities and supports later quantity mutation");
        return new Result(name, packageName, out, notes);
    }

    private static GeneratedProject.FileEntry file(String p, String c, String h) { return new GeneratedProject.FileEntry(p, c, h); }
    private static boolean hasSuffix(List<GeneratedProject.FileEntry> f, String s) { for (GeneratedProject.FileEntry x : f) if (x != null && x.path != null && x.path.endsWith(s)) return true; return false; }
    private static boolean any(String s, String... terms) { for (String x : terms) if (s.contains(x)) return true; return false; }
    private static String requestText(List<GeneratedProject.FileEntry> f) { for (GeneratedProject.FileEntry x : f) if (x != null && "README.md".equals(x.path) && x.content != null) return x.content; StringBuilder b = new StringBuilder(); for (GeneratedProject.FileEntry x : f) if (x != null && x.content != null) b.append('\n').append(x.content); return b.toString(); }

    private static String inferName(String p, String request, boolean note, boolean workout, boolean pantry) {
        String explicit = explicitName(request);
        if (!explicit.isEmpty()) return explicit;
        String x = p == null ? "" : p.trim();
        String low = x.toLowerCase(Locale.US);
        boolean promptLike = low.startsWith("create ") || low.startsWith("make ") || low.startsWith("build ") || low.startsWith("develop ") || low.contains(" should ") || low.contains(" that ") || x.endsWith(".") || wordCount(x) > 5;
        if (!promptLike && x.length() >= 2 && x.length() <= 32) return x;
        if (note) return "NoteForge";
        if (workout) return "QuestFit";
        if (pantry) return "PantryQuest";
        x = x.replaceFirst("(?i)^(create|make|build|develop)\\s+", "").replaceFirst("(?i)^(a|an|the)\\s+", "").replaceFirst("(?i)^simple\\s+", "").trim();
        String xl = x.toLowerCase(Locale.US);
        int cut = xl.indexOf(" app ");
        if (cut < 0) cut = xl.indexOf(" application ");
        if (cut < 0 && xl.endsWith(" app")) cut = x.length() - 4;
        if (cut < 0 && xl.endsWith(" application")) cut = x.length() - 12;
        if (cut > 0) x = x.substring(0, cut).trim();
        if (x.isEmpty() || x.length() > 32 || wordCount(x) > 5) return "Generated App";
        return titleCaseName(x);
    }

    private static String explicitName(String request) {
        if (request == null) return "";
        String r = request.replace('\n', ' ').replace('\r', ' ');
        String low = r.toLowerCase(Locale.US);
        String[] keys = {"called ", "named ", "name it ", "app name is ", "app name ", "application name "};
        for (String key : keys) {
            int i = low.indexOf(key); if (i < 0) continue;
            int s = i + key.length();
            while (s < r.length() && (r.charAt(s) == '\'' || r.charAt(s) == '"' || r.charAt(s) == ':' || Character.isWhitespace(r.charAt(s)))) s++;
            int e = s;
            while (e < r.length() && e - s < 32) { char c = r.charAt(e); if (c == '.' || c == ',' || c == ';') break; e++; }
            String x = r.substring(s, e).trim();
            while (x.endsWith("\"") || x.endsWith("'")) x = x.substring(0, x.length() - 1).trim();
            if (x.length() >= 2 && x.length() <= 28 && wordCount(x) <= 4) return titleCaseName(x);
        }
        return "";
    }

    private static int wordCount(String s) { String t = s == null ? "" : s.trim(); return t.isEmpty() ? 0 : t.split("\\s+").length; }
    private static String titleCaseName(String s) { StringBuilder b = new StringBuilder(); for (String w : s.trim().split("\\s+")) { if (w.isEmpty()) continue; if (b.length() > 0) b.append(' '); b.append(Character.toUpperCase(w.charAt(0))); if (w.length() > 1) b.append(w.substring(1)); } return b.toString(); }
    private static String xml(String s) { return (s == null ? "" : s).replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;"); }
    private static String j(String s) { return (s == null ? "" : s).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " "); }

    private static String strings(String n) { return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources><string name=\"app_name\">" + xml(n) + "</string></resources>\n"; }
    private static String colors(boolean p, boolean r, boolean t, boolean o) {
        String accent = t ? "#0D9488" : p ? "#7C3AED" : "#4F7CFF";
        String secondary = o ? "#F97316" : r ? "#EF4444" : p ? "#A855F7" : "#6B7280";
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources><color name=\"bg\">#101116</color><color name=\"panel\">#1B1D26</color><color name=\"accent\">" + accent + "</color><color name=\"accent_secondary\">" + secondary + "</color><color name=\"muted\">#A8ADBD</color></resources>\n";
    }
    private static String icon(boolean p, boolean r, boolean t, boolean o) {
        String a = t ? "#0D9488" : p ? "#7C3AED" : "#4F7CFF";
        String b = o ? "#F97316" : r ? "#EF4444" : "#C4B5FD";
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"108dp\" android:height=\"108dp\" android:viewportWidth=\"108\" android:viewportHeight=\"108\"><path android:fillColor=\"" + a + "\" android:pathData=\"M12,12h84v84h-84z\"/><path android:fillColor=\"" + b + "\" android:pathData=\"M29,27h50v10h-50zM29,49h50v10h-50zM29,71h33v10h-33z\"/></vector>\n";
    }
    private static String manifest(List<GeneratedProject.FileEntry> f, String n, boolean icon) {
        String x = ""; for (GeneratedProject.FileEntry e : f) if (e != null && "app/src/main/AndroidManifest.xml".equals(e.path)) x = e.content == null ? "" : e.content;
        if (x.isEmpty()) return x;
        x = x.replaceAll("android:label=\"[^\"]*\"", "android:label=\"" + xml(n) + "\"");
        if (icon && !x.contains("android:icon=")) x = x.replace("<application ", "<application android:icon=\"@drawable/ic_generated_app\" android:roundIcon=\"@drawable/ic_generated_app\" ");
        return x;
    }

    private static String appScreen(String p, String n, boolean side, boolean tabs, boolean purple, boolean red, boolean teal, boolean orange, boolean note, boolean workout, boolean pantry) {
        String nav;
        if (note) nav = "String[] labels={\"Writing\",\"Editor\",\"Search\",\"Library\"};Class[] screens={MainActivity.class,EditorActivity.class,SearchActivity.class,LibraryActivity.class};";
        else if (workout) nav = "String[] labels={\"Overview\",\"Workout\",\"Stats\",\"Data\"};Class[] screens={MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class};";
        else if (pantry) nav = "String[] labels={\"Inventory\",\"Add/Edit\",\"Summary\",\"Settings\"};Class[] screens={MainActivity.class,ExploreActivity.class,DetailActivity.class,SettingsActivity.class};";
        else nav = "String[] labels={\"Home\"};Class[] screens={MainActivity.class};";
        int a = teal ? 0xFF0D9488 : purple ? 0xFF7C3AED : 0xFF4F7CFF;
        int b = orange ? 0xFFF97316 : red ? 0xFFEF4444 : 0xFFA855F7;
        String orient = side ? "LinearLayout.HORIZONTAL" : "LinearLayout.VERTICAL";
        String navPlacement = side ? "LinearLayout nav=sideNav();root.addView(nav,new LinearLayout.LayoutParams(dp(104),-1));" : "";
        String bottom = (!side && !tabs) ? "main.addView(sideNav(),new LinearLayout.LayoutParams(-1,dp(64)));" : "";
        String mainParams = side ? "root.addView(main,new LinearLayout.LayoutParams(0,-1,1));" : "root.addView(main,new LinearLayout.LayoutParams(-1,0,1));";
        return "package " + p + ";import android.app.*;import android.graphics.*;import android.graphics.drawable.*;import android.os.*;import android.view.*;import android.widget.*;public abstract class AppScreen extends Activity{protected LinearLayout body;protected LocalStore store;protected final int BG=0xFF101116,SURFACE=0xFF1B1D26,TEXT=0xFFF2F3F7,MUTED=0xFFA8ADBD,ACCENT=" + a + ",SECONDARY=" + b + ";@Override public void onCreate(Bundle z){super.onCreate(z);store=new LocalStore(this);LinearLayout root=new LinearLayout(this);root.setOrientation(" + orient + ");root.setBackgroundColor(BG);" + navPlacement + "LinearLayout main=new LinearLayout(this);main.setOrientation(LinearLayout.VERTICAL);TextView brand=text(\"" + j(n) + "\",13,true);brand.setTextColor(SECONDARY);brand.setPadding(dp(18),dp(16),dp(18),dp(8));main.addView(brand);" + (tabs ? "main.addView(topNav(),new LinearLayout.LayoutParams(-1,dp(58)));" : "") + "ScrollView s=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(12),dp(18),dp(28));s.addView(body);main.addView(s,new LinearLayout.LayoutParams(-1,0,1));" + bottom + mainParams + "setContentView(root);render();}protected abstract void render();protected TextView text(String v,int q,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextColor(TEXT);t.setTextSize(q);t.setTypeface(Typeface.create(\"sans-serif\",bold?1:0));t.setPadding(dp(6),dp(6),dp(6),dp(6));return t;}protected EditText field(String h){EditText e=new EditText(this);e.setHint(h);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setMinHeight(dp(52));e.setPadding(dp(14),0,dp(14),0);e.setBackground(round(SURFACE,14));return e;}protected Button button(String l){Button q=new Button(this);q.setText(l);q.setAllCaps(false);q.setTextColor(TEXT);q.setMinHeight(dp(50));q.setBackground(round(ACCENT,14));return q;}protected LinearLayout card(String h,String d){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(round(SURFACE,14));c.addView(text(h,17,true));if(d!=null)c.addView(text(d,13,false));return c;}protected LinearLayout topNav(){LinearLayout q=new LinearLayout(this);q.setOrientation(LinearLayout.HORIZONTAL);q.setBackgroundColor(SURFACE);" + nav + "for(int i=0;i<labels.length;i++){final Class target=screens[i];TextView x=text(labels[i],12,false);x.setGravity(Gravity.CENTER);x.setMinHeight(dp(48));x.setContentDescription(\"Open tab \"+labels[i]);x.setOnClickListener(v->AppNavigator.open(this,target));q.addView(x,new LinearLayout.LayoutParams(0,-1,1));}return q;}protected LinearLayout sideNav(){LinearLayout q=new LinearLayout(this);q.setOrientation(" + (side ? "LinearLayout.VERTICAL" : "LinearLayout.HORIZONTAL") + ");q.setBackgroundColor(SURFACE);q.setPadding(dp(6),dp(6),dp(6),dp(6));" + nav + "for(int i=0;i<labels.length;i++){final Class target=screens[i];TextView x=text(labels[i],12,false);x.setGravity(Gravity.CENTER);x.setMinHeight(dp(48));x.setContentDescription(\"Open \"+labels[i]);x.setOnClickListener(v->AppNavigator.open(this,target));q.addView(x,new LinearLayout.LayoutParams(" + (side ? "-1,0,1" : "0,-1,1") + "));}return q;}protected GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}protected int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}}";
    }

    private static String noteHome(String p) { return "package " + p + ";public final class MainActivity extends AppScreen{protected void render(){body.addView(text(\"Writing\",26,true));body.addView(text(\"Draft locally, lock finished notes, and recover saved work.\",14,false));String docs=store.text(\"documents\",\"\");body.addView(card(\"Saved documents\",docs.isEmpty()?\"No notes yet\":String.valueOf(docs.split(\"\\n\").length)));android.widget.Button add=button(\"New note\");add.setOnClickListener(v->{String id=String.valueOf(System.currentTimeMillis());store.putText(\"active_note\",id);AppNavigator.open(this,EditorActivity.class);});body.addView(add);}}"; }
    private static String noteEditor(String p, boolean lock) { return "package " + p + ";import android.widget.*;public final class EditorActivity extends AppScreen{private EditText title,content;protected void render(){body.addView(text(\"Editor\",26,true));String id=store.text(\"active_note\",\"default\");boolean locked=store.flag(\"note_lock_\"+id);title=field(\"Document title\");content=field(\"Write here...\");content.setSingleLine(false);content.setMinLines(10);title.setText(store.text(\"note_title_\"+id,\"\"));content.setText(store.text(\"note_body_\"+id,\"\"));body.addView(title);body.addView(content);" + (lock ? "if(locked){title.setEnabled(false);content.setEnabled(false);content.setFocusable(false);}Button lockBtn=button(locked?\"Unlock note\":\"Lock note\");lockBtn.setOnClickListener(v->{store.flag(\"note_lock_\"+id,!store.flag(\"note_lock_\"+id));recreate();});body.addView(lockBtn);" : "") + "Button save=button(\"Save document\");save.setEnabled(!locked);save.setOnClickListener(v->{if(title.getText().toString().trim().isEmpty()){title.setError(\"Enter a title\");return;}store.putText(\"note_title_\"+id,title.getText().toString());store.putText(\"note_body_\"+id,content.getText().toString());String docs=store.text(\"documents\",\"\");if(!docs.contains(id+\"|\"))store.putText(\"documents\",docs+(docs.isEmpty()?\"\":\"\\n\")+id+\"|\"+title.getText().toString().replace(\"|\",\"/\"));save.setText(\"Saved\");});body.addView(save);}}"; }
    private static String noteLibrary(String p) { return "package " + p + ";import android.widget.*;public final class LibraryActivity extends AppScreen{protected void render(){body.addView(text(\"Library\",26,true));String docs=store.text(\"documents\",\"\");if(docs.isEmpty()){body.addView(text(\"No notes yet.\",14,false));return;}for(String row:docs.split(\"\\n\")){String[] x=row.split(\"[|]\",2);if(x.length<2)continue;Button b=button((store.flag(\"note_lock_\"+x[0])?\"Locked: \":\"\")+x[1]);b.setOnClickListener(v->{store.putText(\"active_note\",x[0]);AppNavigator.open(this,EditorActivity.class);});body.addView(b);}}}"; }

    private static String workoutHome(String p) { return "package " + p + ";public final class MainActivity extends AppScreen{protected void render(){int xp=store.number(\"workout_xp\");int level=1+xp/100;body.addView(text(\"Training Guild\",26,true));body.addView(card(\"Level \"+level,\"XP \"+xp+\" / \"+(level*100)));body.addView(card(\"Strength\",String.valueOf(store.number(\"stat_strength\"))));body.addView(card(\"Endurance\",String.valueOf(store.number(\"stat_endurance\"))));android.widget.Button log=button(\"Log workout\");log.setOnClickListener(v->AppNavigator.open(this,TimelineActivity.class));body.addView(log);}}"; }
    private static String workoutLog(String p) { return "package " + p + ";import android.text.InputType;import android.widget.*;public final class TimelineActivity extends AppScreen{private LinearLayout recent;protected void render(){body.addView(text(\"Workout\",26,true));String[] exercises={\"Squat\",\"Bench Press\",\"Deadlift\",\"Push Up\",\"Pull Up\",\"Overhead Press\",\"Barbell Row\",\"Lunge\",\"Plank\"};Spinner exercise=new Spinner(this);ArrayAdapter<String> exerciseAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,exercises);exerciseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);exercise.setAdapter(exerciseAdapter);EditText weight=field(\"Weight\");EditText reps=field(\"Reps\");weight.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);reps.setInputType(InputType.TYPE_CLASS_NUMBER);body.addView(exercise);body.addView(weight);body.addView(reps);Button save=button(\"Complete set\");save.setOnClickListener(v->{String ex=String.valueOf(exercise.getSelectedItem());try{double w=Double.parseDouble(weight.getText().toString());int r=Integer.parseInt(reps.getText().toString());if(w<0||r<=0)throw new Exception();int gain=Math.max(5,(int)Math.round(r+Math.min(40,w/5)));store.number(\"workout_xp\",store.number(\"workout_xp\")+gain);store.number(\"stat_strength\",store.number(\"stat_strength\")+Math.max(1,(int)(w/25)));store.number(\"stat_endurance\",store.number(\"stat_endurance\")+Math.max(1,r/5));String old=store.text(\"workout_history\",\"\");store.putText(\"workout_history\",ex+\"|\"+w+\"|\"+r+(old.isEmpty()?\"\":\"\\n\"+old));weight.setText(\"\");reps.setText(\"\");save.setText(\"+\"+gain+\" XP\");showRecent();}catch(Exception e){reps.setError(\"Enter valid weight and reps\");}});body.addView(save);body.addView(text(\"Recent workouts\",20,true));recent=new LinearLayout(this);recent.setOrientation(LinearLayout.VERTICAL);body.addView(recent);showRecent();}private void showRecent(){recent.removeAllViews();String history=store.text(\"workout_history\",\"\");if(history.isEmpty()){recent.addView(text(\"No workouts logged yet.\",14,false));return;}for(String row:history.split(\"\\n\")){String[] x=row.split(\"[|]\",3);if(x.length>=3)recent.addView(card(x[0],\"Weight: \"+x[1]+\" · Reps: \"+x[2]));}}}"; }
    private static String workoutStats(String p) { return "package " + p + ";public final class ReportsActivity extends AppScreen{protected void render(){int xp=store.number(\"workout_xp\");body.addView(text(\"RPG Stats\",26,true));body.addView(card(\"Level\",String.valueOf(1+xp/100)));body.addView(card(\"Total XP\",String.valueOf(xp)));body.addView(card(\"Strength\",String.valueOf(store.number(\"stat_strength\"))));body.addView(card(\"Endurance\",String.valueOf(store.number(\"stat_endurance\"))));}}"; }

    private static String pantryHome(String p) { return "package " + p + ";import android.widget.*;public final class MainActivity extends AppScreen{protected void render(){body.addView(text(\"Inventory\",26,true));String raw=store.text(\"pantry_inventory\",\"\");if(raw.isEmpty()){body.addView(text(\"No pantry items yet.\",14,false));return;}for(String row:raw.split(\"\\n\")){String[] x=row.split(\"[|]\",2);if(x.length<2)continue;body.addView(card(x[0],\"Quantity: \"+x[1]));}}}"; }
    private static String pantryEditor(String p) { return "package " + p + ";import android.text.InputType;import android.widget.*;public final class ExploreActivity extends AppScreen{protected void render(){body.addView(text(\"Add / Edit Item\",26,true));EditText name=field(\"Item name\");EditText quantity=field(\"Quantity\");quantity.setInputType(InputType.TYPE_CLASS_NUMBER);body.addView(name);body.addView(quantity);Button save=button(\"Save quantity\");save.setOnClickListener(v->{String item=name.getText().toString().trim();if(item.isEmpty()){name.setError(\"Enter an item\");return;}int q;try{q=Integer.parseInt(quantity.getText().toString());if(q<0)throw new Exception();}catch(Exception e){quantity.setError(\"Enter a valid quantity\");return;}String raw=store.text(\"pantry_inventory\",\"\");StringBuilder next=new StringBuilder();boolean found=false;for(String row:raw.split(\"\\n\")){if(row.trim().isEmpty())continue;String[] x=row.split(\"[|]\",2);if(x.length<2)continue;if(x[0].equalsIgnoreCase(item)){if(next.length()>0)next.append('\\n');next.append(item).append('|').append(q);found=true;}else{if(next.length()>0)next.append('\\n');next.append(row);}}if(!found){if(next.length()>0)next.append('\\n');next.append(item).append('|').append(q);}store.putText(\"pantry_inventory\",next.toString());save.setText(\"Saved quantity\");});body.addView(save);}}"; }
    private static String pantrySummary(String p) { return "package " + p + ";public final class DetailActivity extends AppScreen{protected void render(){String raw=store.text(\"pantry_inventory\",\"\");int items=0,totalQuantity=0;for(String row:raw.split(\"\\n\")){String[] x=row.split(\"[|]\",2);if(x.length<2)continue;items++;try{totalQuantity+=Integer.parseInt(x[1]);}catch(Exception ignored){}}body.addView(text(\"Pantry Summary\",26,true));body.addView(card(\"Items\",String.valueOf(items)));body.addView(card(\"Total quantity\",String.valueOf(totalQuantity)));}}"; }

    private RequestFidelityPostProcessor() {}
}
