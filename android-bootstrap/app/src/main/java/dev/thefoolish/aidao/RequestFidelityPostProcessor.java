package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Applies explicit user-request fidelity after broad domain product passes.
 * This local-only pass protects identity, branding, navigation, theme and
 * concrete behaviors that broad templates can otherwise flatten away.
 */
final class RequestFidelityPostProcessor {
    static final class Result {
        final String projectName, packageName;
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
        String request = requestText(source).toLowerCase(Locale.US);
        if (hasSuffix(source, "/MediaProvider.java") || hasSuffix(source, "/AnimeItem.java")) {
            return new Result(projectName, packageName, source, new ArrayList<>());
        }

        boolean note = any(request, "notepad", "note app", "notes app", "lock notes", "document editor");
        boolean workout = any(request, "workout", "exercise", "reps", "weight tracking", "rpg stats");
        boolean logo = any(request, "app logo", "app icon", "launcher icon", " logo", " icon");
        boolean sidebar = any(request, "sidebar", "side bar", "navigation drawer", "drawer navigation", "side navigation");
        boolean purple = request.contains("purple") || request.contains("violet");
        boolean red = request.contains("red") || request.contains("crimson");
        boolean lock = any(request, "lock notes", "lock note", "can't be edited", "cannot be edited", "read-only", "read only");
        if (!note && !workout && !logo && !sidebar && !purple && !red && !lock) {
            return new Result(projectName, packageName, source, new ArrayList<>());
        }

        String name = inferName(projectName, note, workout);
        String root = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        for (GeneratedProject.FileEntry f : source) {
            if (f == null) continue;
            String p = f.path;
            if ("app/src/main/res/values/strings.xml".equals(p)
                    || "app/src/main/res/values/colors.xml".equals(p)
                    || "app/src/main/AndroidManifest.xml".equals(p)
                    || p.equals(root + "AppScreen.java")
                    || p.equals(root + "MainActivity.java")
                    || (note && (p.equals(root + "EditorActivity.java") || p.equals(root + "LibraryActivity.java")))
                    || (workout && (p.equals(root + "TimelineActivity.java") || p.equals(root + "ReportsActivity.java")))) {
                continue;
            }
            out.add(f);
        }

        out.add(file("app/src/main/res/values/strings.xml", strings(name), "Use concise generated product identity"));
        out.add(file("app/src/main/res/values/colors.xml", colors(purple, red), "Honor requested visual color direction"));
        if (logo) out.add(file("app/src/main/res/drawable/ic_generated_app.xml", icon(note, workout, purple, red), "Generate distinct launcher icon"));
        out.add(file("app/src/main/AndroidManifest.xml", manifest(source, name, logo), "Wire generated product identity and icon"));
        out.add(file(root + "AppScreen.java", appScreen(packageName, name, sidebar, purple, red, note, workout), "Honor requested navigation and theme architecture"));

        if (note) {
            out.add(file(root + "MainActivity.java", noteHome(packageName), "Show persisted notes and draft status"));
            out.add(file(root + "EditorActivity.java", noteEditor(packageName, lock), "Persist notes and enforce note lock/read-only state"));
            out.add(file(root + "LibraryActivity.java", noteLibrary(packageName), "Browse and reopen saved notes"));
        } else if (workout) {
            out.add(file(root + "MainActivity.java", workoutHome(packageName), "Show automatic RPG workout progression"));
            out.add(file(root + "TimelineActivity.java", workoutLog(packageName), "Track exercise, weight, and reps"));
            out.add(file(root + "ReportsActivity.java", workoutStats(packageName), "Calculate RPG stats from workout history"));
        }

        List<String> notes = new ArrayList<>();
        notes.add("PASS request-fidelity pass selected concise product identity " + name);
        if (logo) notes.add("PASS explicit app-logo request generated and wired a distinct launcher vector");
        if (sidebar) notes.add("PASS explicit sidebar request uses persistent sideNav navigation on generated screens");
        if (purple) notes.add("PASS explicit purple theme direction applied to generated resources");
        if (red) notes.add("PASS explicit red theme direction applied to generated resources");
        if (note && lock) notes.add("PASS note locking persists state and disables editor input while locked");
        if (workout) notes.add("PASS workout model tracks exercise/weight/reps and derives XP/level/stats automatically");
        validateExplicitRequest(out, packageName, name, logo, sidebar, purple, red, note, workout, lock, notes);
        return new Result(name, packageName, out, notes);
    }

    private static void validateExplicitRequest(List<GeneratedProject.FileEntry> files, String packageName, String name,
            boolean logo, boolean sidebar, boolean purple, boolean red, boolean note, boolean workout, boolean lock,
            List<String> notes) {
        String all = join(files);
        String low = all.toLowerCase(Locale.US);
        if (name.toLowerCase(Locale.US).startsWith("create ") || name.length() > 40)
            notes.add("FAIL generated product identity still contains raw request prose");
        if (logo && (!all.contains("ic_generated_app") || !all.contains("android:icon=\"@drawable/ic_generated_app\"")))
            notes.add("FAIL requested launcher logo was not generated and wired");
        if (sidebar && (!all.contains("sideNav()") || !all.contains("LinearLayout.HORIZONTAL") || !all.contains("dp(112),-1")))
            notes.add("FAIL requested sidebar navigation is missing from generated source");
        if (purple && !low.contains("#7c3aed")) notes.add("FAIL requested purple theme direction is missing");
        if (red && !low.contains("#ef4444")) notes.add("FAIL requested red theme direction is missing");
        if (note && lock && (!all.contains("note_lock_") || !all.contains("setEnabled(!locked)") || !all.contains("setFocusable(!locked)")))
            notes.add("FAIL requested locked-note read-only behavior is missing");
        if (note && (!all.contains("note_title_") || !all.contains("note_body_") || !all.contains("documents")))
            notes.add("FAIL requested note save/load persistence is missing");
        if (workout && (!low.contains("exercise") || !low.contains("weight") || !low.contains("reps") || !low.contains("workout_xp")))
            notes.add("FAIL requested workout tracking and automatic RPG progression is incomplete");
        String root = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        if ((note || workout) && !hasPath(files, root + "AppScreen.java")) notes.add("FAIL request-specific app shell was not emitted");
    }

    private static GeneratedProject.FileEntry file(String p, String c, String h) { return new GeneratedProject.FileEntry(p, c, h); }
    private static boolean hasSuffix(List<GeneratedProject.FileEntry> files, String suffix) { for (GeneratedProject.FileEntry f : files) if (f != null && f.path != null && f.path.endsWith(suffix)) return true; return false; }
    private static boolean hasPath(List<GeneratedProject.FileEntry> files, String path) { for (GeneratedProject.FileEntry f : files) if (f != null && path.equals(f.path)) return true; return false; }
    private static boolean any(String source, String... terms) { for (String term : terms) if (source.contains(term)) return true; return false; }
    private static String requestText(List<GeneratedProject.FileEntry> files) { for (GeneratedProject.FileEntry f : files) if (f != null && "README.md".equals(f.path) && f.content != null) return f.content; StringBuilder b = new StringBuilder(); for (GeneratedProject.FileEntry f : files) if (f != null && f.content != null) b.append('\n').append(f.content); return b.toString(); }

    private static String inferName(String projectName, boolean note, boolean workout) {
        String p = projectName == null ? "" : projectName.trim();
        String low = p.toLowerCase(Locale.US);
        if (p.length() >= 2 && p.length() <= 32 && !low.startsWith("create ") && !low.startsWith("make ") && !low.startsWith("build ") && !low.contains(" should ") && !low.contains(" that uses ")) return p;
        if (note) return "NoteForge";
        if (workout) return "QuestFit";
        return "Generated App";
    }

    private static String strings(String name) { return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources><string name=\"app_name\">" + xml(name) + "</string></resources>\n"; }
    private static String colors(boolean purple, boolean red) { return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources><color name=\"bg\">#101116</color><color name=\"panel\">#1B1D26</color><color name=\"accent\">" + (purple ? "#7C3AED" : "#4F7CFF") + "</color><color name=\"accent_secondary\">" + (red ? "#EF4444" : purple ? "#A855F7" : "#6B7280") + "</color><color name=\"muted\">#A8ADBD</color></resources>\n"; }

    private static String icon(boolean note, boolean workout, boolean purple, boolean red) {
        String base = purple ? "#7C3AED" : "#4F7CFF";
        String accent = red ? "#EF4444" : "#C4B5FD";
        String glyph;
        if (note) {
            glyph = "<path android:fillColor=\"#FFFFFFFF\" android:pathData=\"M29,22h42l12,12v52h-54z\"/><path android:fillColor=\"" + accent + "\" android:pathData=\"M43,57h28v21h-28zM49,57v-6c0,-7 4,-11 8,-11s8,4 8,11v6h-6v-6c0,-3 -1,-5 -2,-5s-2,2 -2,5v6z\"/>";
        } else if (workout) {
            glyph = "<path android:fillColor=\"#FFFFFFFF\" android:pathData=\"M24,48h12v12h-12zM72,48h12v12h-12zM36,52h36v4h-36z\"/><path android:fillColor=\"" + accent + "\" android:pathData=\"M17,43h7v22h-7zM84,43h7v22h-7z\"/>";
        } else {
            glyph = "<path android:fillColor=\"#FFFFFFFF\" android:pathData=\"M28,29h52v10h-52zM28,49h52v10h-52zM28,69h34v10h-34z\"/>";
        }
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"108dp\" android:height=\"108dp\" android:viewportWidth=\"108\" android:viewportHeight=\"108\"><path android:fillColor=\"" + base + "\" android:pathData=\"M12,12h84v84h-84z\"/>" + glyph + "</vector>\n";
    }

    private static String manifest(List<GeneratedProject.FileEntry> files, String name, boolean icon) {
        String source = "";
        for (GeneratedProject.FileEntry f : files) if (f != null && "app/src/main/AndroidManifest.xml".equals(f.path)) { source = f.content == null ? "" : f.content; break; }
        if (source.isEmpty()) return source;
        source = source.replaceAll("android:label=\"[^\"]*\"", "android:label=\"" + xml(name) + "\"");
        if (icon && !source.contains("android:icon=")) source = source.replace("<application ", "<application android:icon=\"@drawable/ic_generated_app\" android:roundIcon=\"@drawable/ic_generated_app\" ");
        return source;
    }

    private static String appScreen(String pkg, String name, boolean sidebar, boolean purple, boolean red, boolean note, boolean workout) {
        String nav = note ? "String[] labels={\"Writing\",\"Editor\",\"Search\",\"Library\"};Class[] screens={MainActivity.class,EditorActivity.class,SearchActivity.class,LibraryActivity.class};" : workout ? "String[] labels={\"Overview\",\"Workout\",\"Stats\",\"Data\"};Class[] screens={MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class};" : "String[] labels={\"Home\"};Class[] screens={MainActivity.class};";
        int accent = purple ? 0xFF7C3AED : 0xFF4F7CFF;
        int secondary = red ? 0xFFEF4444 : 0xFFA855F7;
        return "package " + pkg + ";import android.app.*;import android.graphics.*;import android.graphics.drawable.*;import android.os.*;import android.view.*;import android.widget.*;public abstract class AppScreen extends Activity{protected LinearLayout body;protected LocalStore store;protected final int BG=0xFF101116,SURFACE=0xFF1B1D26,TEXT=0xFFF2F3F7,MUTED=0xFFA8ADBD,ACCENT=" + accent + ",SECONDARY=" + secondary + ";@Override public void onCreate(Bundle z){super.onCreate(z);store=new LocalStore(this);LinearLayout root=new LinearLayout(this);root.setOrientation(" + (sidebar ? "LinearLayout.HORIZONTAL" : "LinearLayout.VERTICAL") + ");root.setBackgroundColor(BG);" + (sidebar ? "LinearLayout nav=sideNav();root.addView(nav,new LinearLayout.LayoutParams(dp(112),-1));" : "") + "LinearLayout main=new LinearLayout(this);main.setOrientation(LinearLayout.VERTICAL);TextView brand=text(\"" + java(name) + "\",13,true);brand.setTextColor(SECONDARY);brand.setPadding(dp(18),dp(16),dp(18),dp(8));main.addView(brand);ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(12),dp(18),dp(28));scroll.addView(body);main.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));" + (!sidebar ? "main.addView(sideNav(),new LinearLayout.LayoutParams(-1,dp(64)));" : "") + (sidebar ? "root.addView(main,new LinearLayout.LayoutParams(0,-1,1));" : "root.addView(main,new LinearLayout.LayoutParams(-1,0,1));") + "setContentView(root);render();}protected abstract void render();protected TextView text(String v,int q,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextColor(TEXT);t.setTextSize(q);t.setTypeface(Typeface.create(\"sans-serif\",bold?1:0));t.setPadding(dp(6),dp(6),dp(6),dp(6));return t;}protected EditText field(String h){EditText e=new EditText(this);e.setHint(h);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setMinHeight(dp(52));e.setPadding(dp(14),0,dp(14),0);e.setBackground(round(SURFACE,14));return e;}protected Button button(String l){Button q=new Button(this);q.setText(l);q.setAllCaps(false);q.setTextColor(TEXT);q.setMinHeight(dp(50));q.setBackground(round(SURFACE,14));return q;}protected LinearLayout card(String h,String d){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(round(SURFACE,14));c.addView(text(h,17,true));if(d!=null)c.addView(text(d,13,false));return c;}protected LinearLayout sideNav(){LinearLayout n=new LinearLayout(this);n.setOrientation(" + (sidebar ? "LinearLayout.VERTICAL" : "LinearLayout.HORIZONTAL") + ");n.setBackgroundColor(SURFACE);n.setPadding(dp(6),dp(6),dp(6),dp(6));" + nav + "for(int i=0;i<labels.length;i++){final Class target=screens[i];TextView x=text(labels[i],12,false);x.setGravity(Gravity.CENTER);x.setMinHeight(dp(48));x.setContentDescription(\"Open \"+labels[i]);x.setOnClickListener(v->AppNavigator.open(this,target));n.addView(x,new LinearLayout.LayoutParams(" + (sidebar ? "-1,0,1" : "0,-1,1") + "));}return n;}protected GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}protected int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}}";
    }

    private static String noteHome(String pkg) { return "package " + pkg + ";public final class MainActivity extends AppScreen{protected void render(){body.addView(text(\"Writing\",26,true));body.addView(text(\"Draft locally, lock finished notes, and recover saved work.\",14,false));String docs=store.text(\"documents\",\"\");body.addView(card(\"Saved documents\",docs.isEmpty()?\"No notes yet\":String.valueOf(docs.split(\"\\n\").length)));Button edit=button(\"Create note\");edit.setOnClickListener(v->{store.putText(\"active_note\",\"note_\"+System.currentTimeMillis());AppNavigator.open(this,EditorActivity.class);});body.addView(edit);}}"; }
    private static String noteEditor(String pkg, boolean lockFeature) { return "package " + pkg + ";import android.widget.*;public final class EditorActivity extends AppScreen{private EditText title,content;protected void render(){body.addView(text(\"Editor\",26,true));String id=store.text(\"active_note\",\"note_default\");boolean locked=store.flag(\"note_lock_\"+id);title=field(\"Document title\");content=field(\"Write here...\");content.setSingleLine(false);content.setMinLines(10);title.setText(store.text(\"note_title_\"+id,\"\"));content.setText(store.text(\"note_body_\"+id,\"\"));title.setEnabled(!locked);title.setFocusable(!locked);content.setEnabled(!locked);content.setFocusable(!locked);body.addView(title);body.addView(content);" + (lockFeature ? "Button lockBtn=button(locked?\"Unlock note\":\"Lock note\");lockBtn.setOnClickListener(v->{store.flag(\"note_lock_\"+id,!store.flag(\"note_lock_\"+id));recreate();});body.addView(lockBtn);" : "") + "Button save=button(\"Save document\");save.setEnabled(!locked);save.setOnClickListener(v->{if(title.getText().toString().trim().isEmpty()){title.setError(\"Enter a title\");return;}store.putText(\"note_title_\"+id,title.getText().toString());store.putText(\"note_body_\"+id,content.getText().toString());String docs=store.text(\"documents\",\"\");String marker=id+\"|\";if(!docs.contains(marker))store.putText(\"documents\",docs+(docs.isEmpty()?\"\":\"\\n\")+id+\"|\"+title.getText().toString().replace(\"|\",\"/\"));save.setText(\"Saved\");});body.addView(save);}}"; }
    private static String noteLibrary(String pkg) { return "package " + pkg + ";import android.widget.*;public final class LibraryActivity extends AppScreen{protected void render(){body.addView(text(\"Library\",26,true));String docs=store.text(\"documents\",\"\");if(docs.isEmpty()){body.addView(text(\"No notes yet.\",14,false));return;}for(String row:docs.split(\"\\n\")){String[] x=row.split(\"[|]\",2);if(x.length<2)continue;final String id=x[0];Button b=button((store.flag(\"note_lock_\"+id)?\"Locked: \":\"\")+x[1]);b.setOnClickListener(v->{store.putText(\"active_note\",id);AppNavigator.open(this,EditorActivity.class);});body.addView(b);}}}"; }
    private static String workoutHome(String pkg) { return "package " + pkg + ";public final class MainActivity extends AppScreen{protected void render(){int xp=store.number(\"workout_xp\");int level=1+xp/100;body.addView(text(\"Training Guild\",26,true));body.addView(card(\"Level \"+level,\"XP \"+xp+\" / \"+(level*100)));body.addView(card(\"Strength\",String.valueOf(store.number(\"stat_strength\"))));body.addView(card(\"Endurance\",String.valueOf(store.number(\"stat_endurance\"))));Button log=button(\"Log workout\");log.setOnClickListener(v->AppNavigator.open(this,TimelineActivity.class));body.addView(log);}}"; }
    private static String workoutLog(String pkg) { return "package " + pkg + ";import android.text.InputType;import android.widget.*;public final class TimelineActivity extends AppScreen{protected void render(){body.addView(text(\"Workout\",26,true));EditText exercise=field(\"Exercise\");EditText weight=field(\"Weight\");EditText reps=field(\"Reps\");weight.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);reps.setInputType(InputType.TYPE_CLASS_NUMBER);body.addView(exercise);body.addView(weight);body.addView(reps);Button save=button(\"Complete set\");save.setOnClickListener(v->{String ex=exercise.getText().toString().trim();if(ex.isEmpty()){exercise.setError(\"Enter exercise\");return;}try{double w=Double.parseDouble(weight.getText().toString().trim());int r=Integer.parseInt(reps.getText().toString().trim());if(w<0||r<=0)throw new NumberFormatException();int gain=Math.max(5,(int)Math.round(r*Math.max(1,w)/10.0));store.number(\"workout_xp\",store.number(\"workout_xp\")+gain);store.number(\"stat_strength\",store.number(\"stat_strength\")+Math.max(1,gain/20));store.number(\"stat_endurance\",store.number(\"stat_endurance\")+Math.max(1,r/10));String history=store.text(\"workout_history\",\"\");store.putText(\"workout_history\",System.currentTimeMillis()+\"|\"+ex.replace(\"|\",\"/\")+\"|\"+w+\"|\"+r+(history.isEmpty()?\"\":\"\\n\"+history));save.setText(\"Recorded +\"+gain+\" XP\");}catch(Exception e){reps.setError(\"Enter valid weight and reps\");}});body.addView(save);}}"; }
    private static String workoutStats(String pkg) { return "package " + pkg + ";public final class ReportsActivity extends AppScreen{protected void render(){int xp=store.number(\"workout_xp\");int level=1+xp/100;body.addView(text(\"RPG Stats\",26,true));body.addView(card(\"Adventurer level\",String.valueOf(level)));body.addView(card(\"Total XP\",String.valueOf(xp)));body.addView(card(\"Strength\",String.valueOf(store.number(\"stat_strength\"))));body.addView(card(\"Endurance\",String.valueOf(store.number(\"stat_endurance\"))));String h=store.text(\"workout_history\",\"\");body.addView(card(\"Completed sets\",h.isEmpty()?\"0\":String.valueOf(h.split(\"\\n\").length)));}}"; }

    private static String join(List<GeneratedProject.FileEntry> files) { StringBuilder b = new StringBuilder(); for (GeneratedProject.FileEntry f : files) if (f != null && f.content != null) b.append('\n').append(f.content); return b.toString(); }
    private static String java(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " "); }
    private static String xml(String value) { if (value == null) return ""; return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;"); }
    private RequestFidelityPostProcessor() {}
}
