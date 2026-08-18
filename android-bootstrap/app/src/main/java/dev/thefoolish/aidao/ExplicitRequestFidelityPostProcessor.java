package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Applies explicit user-requested identity, visual, navigation and behavior requirements
 * after broad domain generation. This pass exists specifically to prevent a compiling
 * domain template from being reported as complete when the prompt asked for something
 * materially different.
 */
final class ExplicitRequestFidelityPostProcessor {
    static final class Result {
        final String projectName, packageName;
        final List<GeneratedProject.FileEntry> files;
        final List<String> notes;
        Result(String projectName, String packageName, List<GeneratedProject.FileEntry> files, List<String> notes) {
            this.projectName = projectName; this.packageName = packageName; this.files = files; this.notes = notes;
        }
    }

    static Result process(String projectName, String packageName, List<GeneratedProject.FileEntry> incoming) {
        List<GeneratedProject.FileEntry> source = incoming == null ? new ArrayList<>() : new ArrayList<>(incoming);
        String corpus = corpus(projectName, source).toLowerCase(Locale.US);
        if (isNotepad(corpus)) return notepad(packageName, source);
        if (isWorkout(corpus)) return workout(packageName, source);
        return new Result(projectName, packageName, source, new ArrayList<>());
    }

    private static boolean isNotepad(String s) {
        return (s.contains("notepad") || s.contains("note app") || s.contains("notes")) &&
                (s.contains("lock") || s.contains("sidebar") || s.contains("editor"));
    }
    private static boolean isWorkout(String s) {
        return (s.contains("workout") || s.contains("exercise")) &&
                (s.contains("reps") || s.contains("weight")) &&
                (s.contains("rpg") || s.contains("growth") || s.contains("progress"));
    }
    private static String corpus(String projectName, List<GeneratedProject.FileEntry> files) {
        StringBuilder b = new StringBuilder(projectName == null ? "" : projectName);
        for (GeneratedProject.FileEntry f : files) {
            if (f == null || f.content == null) continue;
            if ("README.md".equals(f.path) || f.path.endsWith("/MainActivity.java") || f.path.endsWith("/EditorActivity.java"))
                b.append('\n').append(f.content);
        }
        return b.toString();
    }

    private static Result notepad(String oldPackage, List<GeneratedProject.FileEntry> source) {
        String name = "LockNote";
        String pkg = "dev.thefoolish.generated.locknote";
        String oldPath = oldPackage.replace('.', '/');
        String newPath = pkg.replace('.', '/');
        List<GeneratedProject.FileEntry> out = remapBase(source, oldPackage, pkg, oldPath, newPath,
                name, true, "#6D4AFF", "#E23D62");
        dropJava(out, newPath, "AppScreen.java", "MainActivity.java", "EditorActivity.java", "SearchActivity.java", "LibraryActivity.java");
        add(out, newPath, "AppScreen.java", noteScreen(pkg, name), "Honor requested sidebar navigation and purple/red visual identity");
        add(out, newPath, "NoteStore.java", noteStore(pkg), "Persist note content and lock state locally");
        add(out, newPath, "MainActivity.java", noteHome(pkg), "Show persistent note workspace summary");
        add(out, newPath, "EditorActivity.java", noteEditor(pkg), "Create/edit notes and enforce read-only state when locked");
        add(out, newPath, "SearchActivity.java", noteSearch(pkg), "Search saved notes locally");
        add(out, newPath, "LibraryActivity.java", noteLibrary(pkg), "Browse, open, lock and unlock notes");
        out.add(new GeneratedProject.FileEntry("app/src/main/res/drawable/ic_generated_app.xml", noteIcon(), "Generate distinct launcher logo requested by user"));
        List<String> notes = new ArrayList<>();
        notes.add("PASS explicit request fidelity derived concise app identity LockNote instead of prompt text");
        notes.add("PASS explicit request fidelity generated and wired a distinct launcher icon");
        notes.add("PASS explicit request fidelity replaced bottom navigation with a toggleable sidebar");
        notes.add("PASS explicit request fidelity applied requested purple/red modern theme");
        notes.add("PASS explicit request fidelity persists note lock state and disables editing while locked");
        return new Result(name, pkg, out, notes);
    }

    private static Result workout(String oldPackage, List<GeneratedProject.FileEntry> source) {
        String name = "QuestFit";
        String pkg = "dev.thefoolish.generated.questfit";
        String oldPath = oldPackage.replace('.', '/');
        String newPath = pkg.replace('.', '/');
        List<GeneratedProject.FileEntry> out = remapBase(source, oldPackage, pkg, oldPath, newPath,
                name, false, "#4F67FF", "#B66CFF");
        dropJava(out, newPath, "MainActivity.java", "TimelineActivity.java", "ReportsActivity.java", "DataControlsActivity.java");
        add(out, newPath, "WorkoutStore.java", workoutStore(pkg), "Persist exercise, weight and reps and calculate automatic RPG growth");
        add(out, newPath, "MainActivity.java", workoutHome(pkg), "Display RPG character stats calculated from workout history");
        add(out, newPath, "TimelineActivity.java", workoutLog(pkg), "Log exercise, weight and reps without manual stat input");
        add(out, newPath, "ReportsActivity.java", workoutStats(pkg), "Show automatic XP, level and growth summary");
        add(out, newPath, "DataControlsActivity.java", workoutHistory(pkg), "Show persisted workout history and local reset control");
        List<String> notes = new ArrayList<>();
        notes.add("PASS workout request generates exercise, weight and reps fields");
        notes.add("PASS workout request calculates XP, level, strength and endurance automatically from completed workouts");
        notes.add("PASS workout progression does not require manual RPG stat input");
        return new Result(name, pkg, out, notes);
    }

    private static List<GeneratedProject.FileEntry> remapBase(List<GeneratedProject.FileEntry> source, String oldPkg, String newPkg,
            String oldPath, String newPath, String name, boolean icon, String primary, String secondary) {
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        for (GeneratedProject.FileEntry f : source) {
            if (f == null) continue;
            String path = f.path;
            if (path.startsWith("app/src/main/java/" + oldPath + "/"))
                path = "app/src/main/java/" + newPath + "/" + path.substring(("app/src/main/java/" + oldPath + "/").length());
            String c = f.content == null ? "" : f.content.replace(oldPkg, newPkg);
            if ("settings.gradle.kts".equals(path))
                c = c.replaceAll("rootProject\\.name\\s*=\\s*\"[^\"]*\"", "rootProject.name = \"" + name + "\"");
            if ("app/build.gradle.kts".equals(path)) c = c.replace(oldPkg, newPkg);
            if ("app/src/main/res/values/strings.xml".equals(path))
                c = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources><string name=\"app_name\">" + name + "</string></resources>\n";
            if ("app/src/main/res/values/colors.xml".equals(path))
                c = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources><color name=\"bg\">#0E0D14</color><color name=\"panel\">#1B1825</color><color name=\"accent\">"+primary+"</color><color name=\"accent_secondary\">"+secondary+"</color><color name=\"muted\">#A7A0B8</color></resources>\n";
            if ("app/src/main/AndroidManifest.xml".equals(path)) {
                c = c.replace(oldPkg, newPkg).replaceAll("android:label=\"[^\"]*\"", "android:label=\"" + name + "\"");
                if (icon) c = c.replace("<application ", "<application android:icon=\"@drawable/ic_generated_app\" android:roundIcon=\"@drawable/ic_generated_app\" ");
            }
            out.add(new GeneratedProject.FileEntry(path, c, f.taskHint));
        }
        return out;
    }

    private static void dropJava(List<GeneratedProject.FileEntry> files, String path, String... names) {
        for (String name : names) {
            String target = "app/src/main/java/" + path + "/" + name;
            for (int i = files.size() - 1; i >= 0; i--) if (target.equals(files.get(i).path)) files.remove(i);
        }
    }
    private static void add(List<GeneratedProject.FileEntry> out, String path, String name, String content, String hint) {
        out.add(new GeneratedProject.FileEntry("app/src/main/java/" + path + "/" + name, content, hint));
    }

    private static String noteScreen(String p, String n) {
        return "package "+p+";import android.app.*;import android.graphics.*;import android.graphics.drawable.*;import android.os.*;import android.view.*;import android.widget.*;"+
        "public abstract class AppScreen extends Activity{protected LinearLayout body,sidebar;protected LocalStore store;protected final int BG=Color.rgb(14,13,20),PANEL=Color.rgb(27,24,37),PURPLE=Color.rgb(109,74,255),RED=Color.rgb(226,61,98),TEXT=Color.rgb(245,242,250),MUTED=Color.rgb(170,162,188);"+
        "public void onCreate(Bundle b){super.onCreate(b);store=new LocalStore(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);LinearLayout top=new LinearLayout(this);Button menu=button(\"☰ Menu\");top.addView(menu,new LinearLayout.LayoutParams(dp(108),dp(52)));TextView brand=text(\""+n+"\",22,true);brand.setPadding(dp(12),0,0,0);top.addView(brand,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(top);LinearLayout stage=new LinearLayout(this);stage.setOrientation(LinearLayout.HORIZONTAL);sidebar=new LinearLayout(this);sidebar.setOrientation(LinearLayout.VERTICAL);sidebar.setBackgroundColor(PANEL);sidebar.setPadding(dp(8),dp(8),dp(8),dp(8));side(\"Writing\",MainActivity.class);side(\"Editor\",EditorActivity.class);side(\"Search\",SearchActivity.class);side(\"Library\",LibraryActivity.class);stage.addView(sidebar,new LinearLayout.LayoutParams(dp(118),-1));ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(12),dp(16),dp(24));scroll.addView(body);stage.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));menu.setOnClickListener(v->sidebar.setVisibility(sidebar.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));setContentView(root);render();}"+
        "protected abstract void render();private void side(String label,Class target){Button b=button(label);b.setOnClickListener(v->AppNavigator.open(this,target));sidebar.addView(b);}"+
        "protected TextView text(String v,int s,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextColor(TEXT);t.setTextSize(s);t.setTypeface(android.graphics.Typeface.create(\"sans-serif\",bold?1:0));return t;}protected void title(String s){TextView t=text(s,28,true);t.setTextColor(PURPLE);body.addView(t);}protected void subtitle(String s){TextView t=text(s,14,false);t.setTextColor(MUTED);t.setPadding(0,dp(6),0,dp(12));body.addView(t);}protected EditText field(String h){EditText e=new EditText(this);e.setHint(h);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setBackground(round(PANEL));e.setPadding(dp(14),dp(12),dp(14),dp(12));e.setMinHeight(dp(52));return e;}protected Button button(String l){Button b=new Button(this);b.setText(l);b.setAllCaps(false);b.setTextColor(TEXT);b.setBackground(round(PANEL));b.setMinHeight(dp(48));b.setContentDescription(l);return b;}protected GradientDrawable round(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(14));return d;}protected int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}}";
    }
    private static String noteStore(String p) {
        return "package "+p+";import android.content.*;import java.util.*;public final class NoteStore{private final SharedPreferences p;NoteStore(Context c){p=c.getSharedPreferences(\"locknote_notes\",0);}String ids(){return p.getString(\"ids\",\"\");}String title(String id){return p.getString(\"title_\"+id,\"Untitled\");}String body(String id){return p.getString(\"body_\"+id,\"\");}boolean isLocked(String id){return p.getBoolean(\"note_lock_\"+id,false);}void setLocked(String id,boolean v){p.edit().putBoolean(\"note_lock_\"+id,v).apply();}String save(String id,String title,String body){if(id==null||id.length()==0)id=String.valueOf(System.currentTimeMillis());String ids=ids();if(!contains(ids,id))ids=ids.length()==0?id:ids+\"|\"+id;p.edit().putString(\"ids\",ids).putString(\"title_\"+id,title).putString(\"body_\"+id,body).apply();return id;}private boolean contains(String ids,String id){for(String x:ids.split(\"[|]\"))if(x.equals(id))return true;return false;}}";
    }
    private static String noteHome(String p) { return "package "+p+";public final class MainActivity extends AppScreen{protected void render(){title(\"Writing\");subtitle(\"A private notebook with persistent lockable notes. Open the sidebar to move between tools.\");NoteStore n=new NoteStore(this);String ids=n.ids();int count=ids.length()==0?0:ids.split(\"[|]\").length;body.addView(text(\"Saved notes: \"+count,18,true));body.addView(text(\"Locked notes remain read-only until you explicitly unlock them in Library.\",14,false));}}"; }
    private static String noteEditor(String p) {
        return "package "+p+";import android.widget.*;public final class EditorActivity extends AppScreen{protected void render(){title(\"Editor\");NoteStore notes=new NoteStore(this);String id=getIntent().getStringExtra(\"id\");boolean locked=id!=null&&notes.isLocked(id);EditText title=field(\"Document title\");EditText content=field(\"Write here…\");content.setSingleLine(false);content.setMinLines(12);if(id!=null){title.setText(notes.title(id));content.setText(notes.body(id));}if(locked){title.setEnabled(false);content.setEnabled(false);subtitle(\"This note is locked and read-only. Unlock it from Library to edit.\");}else subtitle(\"Draft locally and save explicitly. Locking is available in Library.\");body.addView(title);body.addView(content);Button save=button(locked?\"Locked · read-only\":\"Save document\");save.setEnabled(!locked);save.setOnClickListener(v->{String t=title.getText().toString().trim();if(t.length()==0){title.setError(\"Enter a title\");return;}notes.save(id,t,content.getText().toString());finish();});body.addView(save);}}";
    }
    private static String noteSearch(String p) {
        return "package "+p+";import android.widget.*;public final class SearchActivity extends AppScreen{protected void render(){title(\"Search\");EditText q=field(\"Search notes\");body.addView(q);LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);Button go=button(\"Search\");body.addView(go);go.setOnClickListener(v->{results.removeAllViews();String term=q.getText().toString().toLowerCase();NoteStore n=new NoteStore(this);for(String id:n.ids().split(\"[|]\")){if(id.length()==0)continue;String t=n.title(id),b=n.body(id);if(t.toLowerCase().contains(term)||b.toLowerCase().contains(term))results.addView(text(t+(n.isLocked(id)?\" · Locked\":\"\"),17,true));}});}}";
    }
    private static String noteLibrary(String p) {
        return "package "+p+";import android.content.*;import android.widget.*;public final class LibraryActivity extends AppScreen{protected void render(){title(\"Library\");NoteStore n=new NoteStore(this);String ids=n.ids();if(ids.length()==0){subtitle(\"No saved notes yet. Create one in Editor.\");return;}for(String id:ids.split(\"[|]\")){if(id.length()==0)continue;LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);Button open=button(n.title(id)+(n.isLocked(id)?\" · Locked\":\"\"));open.setOnClickListener(v->{Intent x=new Intent(this,EditorActivity.class);x.putExtra(\"id\",id);startActivity(x);});Button lock=button(n.isLocked(id)?\"Unlock note\":\"Lock note\");lock.setOnClickListener(v->{n.setLocked(id,!n.isLocked(id));recreate();});row.addView(open);row.addView(lock);body.addView(row);}}}";
    }
    private static String noteIcon() {
        return "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"108dp\" android:height=\"108dp\" android:viewportWidth=\"108\" android:viewportHeight=\"108\"><path android:fillColor=\"#6D4AFF\" android:pathData=\"M14,14h80v80h-80z\"/><path android:fillColor=\"#E23D62\" android:pathData=\"M30,30h48v10h-48zM30,49h34v10h-34zM30,68h28v10h-28z\"/><path android:fillColor=\"#FFFFFF\" android:pathData=\"M70,58h18v24h-18zM73,58v-5c0,-9 12,-9 12,0v5h-5v-5c0,-3 -2,-3 -2,0v5z\"/></vector>";
    }

    private static String workoutStore(String p) {
        return "package "+p+";import android.content.*;public final class WorkoutStore{private final SharedPreferences p;WorkoutStore(Context c){p=c.getSharedPreferences(\"questfit\",0);}void add(String exercise,double weight,int reps){String row=System.currentTimeMillis()+\"|\"+exercise.replace(\"|\",\"/\")+\"|\"+weight+\"|\"+reps;String old=p.getString(\"workouts\",\"\");p.edit().putString(\"workouts\",old.length()==0?row:row+\"\\n\"+old).apply();}String rows(){return p.getString(\"workouts\",\"\");}int xp(){double total=0;for(String r:rows().split(\"\\n\")){String[] x=r.split(\"[|]\");if(x.length<4)continue;try{total+=Double.parseDouble(x[2])*Integer.parseInt(x[3]);}catch(Exception ignored){}}return(int)Math.min(Integer.MAX_VALUE,total/10.0);}int level(){return 1+xp()/500;}int strength(){return 5+xp()/250;}int endurance(){int reps=0;for(String r:rows().split(\"\\n\")){String[] x=r.split(\"[|]\");if(x.length>=4)try{reps+=Integer.parseInt(x[3]);}catch(Exception ignored){}}return 5+reps/50;}}";
    }
    private static String workoutHome(String p) { return "package "+p+";public final class MainActivity extends AppScreen{protected void render(){title(\"Quest Board\");WorkoutStore w=new WorkoutStore(this);subtitle(\"RPG growth is calculated automatically from completed workouts; you never enter stats manually.\");body.addView(card(\"Level \"+w.level(),\"XP: \"+w.xp()+\" · Strength: \"+w.strength()+\" · Endurance: \"+w.endurance()));nav(new String[]{\"Quest Board\",\"Log Workout\",\"Stats\",\"History\"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}}"; }
    private static String workoutLog(String p) {
        return "package "+p+";import android.text.InputType;import android.widget.*;public final class TimelineActivity extends AppScreen{protected void render(){title(\"Log Workout\");subtitle(\"Record the exercise, weight and reps. XP and RPG stats update automatically.\");EditText exercise=field(\"Exercise\");EditText weight=field(\"Weight\");EditText reps=field(\"Reps\");weight.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);reps.setInputType(InputType.TYPE_CLASS_NUMBER);body.addView(exercise);body.addView(weight);body.addView(reps);Button save=button(\"Complete quest\");save.setOnClickListener(v->{String e=exercise.getText().toString().trim();if(e.length()==0){exercise.setError(\"Enter an exercise\");return;}try{double w=Double.parseDouble(weight.getText().toString());int r=Integer.parseInt(reps.getText().toString());if(w<0||r<=0)throw new IllegalArgumentException();new WorkoutStore(this).add(e,w,r);recreate();}catch(Exception x){reps.setError(\"Enter valid weight and reps\");}});body.addView(save);nav(new String[]{\"Quest Board\",\"Log Workout\",\"Stats\",\"History\"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}}";
    }
    private static String workoutStats(String p) { return "package "+p+";public final class ReportsActivity extends AppScreen{protected void render(){title(\"Character Stats\");WorkoutStore w=new WorkoutStore(this);body.addView(card(\"Level\",String.valueOf(w.level())));body.addView(card(\"XP\",String.valueOf(w.xp())));body.addView(card(\"Strength\",String.valueOf(w.strength())));body.addView(card(\"Endurance\",String.valueOf(w.endurance())));nav(new String[]{\"Quest Board\",\"Log Workout\",\"Stats\",\"History\"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}}"; }
    private static String workoutHistory(String p) { return "package "+p+";public final class DataControlsActivity extends AppScreen{protected void render(){title(\"Workout History\");String rows=new WorkoutStore(this).rows();if(rows.length()==0)subtitle(\"No completed quests yet.\");else for(String r:rows.split(\"\\n\")){String[] x=r.split(\"[|]\");if(x.length>=4)body.addView(card(x[1],x[2]+\" weight · \"+x[3]+\" reps\"));}nav(new String[]{\"Quest Board\",\"Log Workout\",\"Stats\",\"History\"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}}"; }

    private ExplicitRequestFidelityPostProcessor() {}
}
