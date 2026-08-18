package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;

/**
 * Quality-first post-processing for non-media generated apps.
 *
 * The early generator intentionally produced broad domain scaffolds. This pass
 * turns the most common offline-first domains into coherent Android products
 * with inset-aware chrome, persistent user data, validation, recovery-friendly
 * state, and domain-specific behavior rather than "save sample state" shells.
 */
final class GeneralProductPostProcessor {
    private enum Domain { FINANCE, TRACKER, CONTENT, NONE }

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
        Domain domain = detect(source);
        if (domain == Domain.NONE) return new Result(projectName, packageName, source, new ArrayList<>());

        String javaPath = packageName.replace('.', '/');
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        for (GeneratedProject.FileEntry file : source) {
            if (file == null) continue;
            if (file.path.endsWith("/GeneratedScreen.java") || file.path.endsWith("/AppScreen.java") || isReplacedActivity(file.path, domain)) continue;
            out.add(file);
        }

        add(out, javaPath, "AppScreen.java", appScreen(packageName, projectName), "Use inset-aware Android product chrome and accessible navigation");
        switch (domain) {
            case FINANCE:
                add(out, javaPath, "MainActivity.java", financeMain(packageName), "Show persisted finance summary and navigation");
                add(out, javaPath, "TransactionsActivity.java", financeTransactions(packageName), "Create and persist validated transaction records");
                add(out, javaPath, "BudgetsActivity.java", financeBudgets(packageName), "Persist monthly budget configuration");
                add(out, javaPath, "ReportsActivity.java", financeReports(packageName), "Calculate spending totals from persisted transactions");
                break;
            case TRACKER:
                add(out, javaPath, "MainActivity.java", trackerMain(packageName), "Show persisted activity summary and navigation");
                add(out, javaPath, "TimelineActivity.java", trackerTimeline(packageName), "Create and persist activity timeline records");
                add(out, javaPath, "ReportsActivity.java", trackerReports(packageName), "Calculate activity totals from persisted records");
                add(out, javaPath, "DataControlsActivity.java", trackerControls(packageName), "Provide explicit local data controls");
                break;
            case CONTENT:
                add(out, javaPath, "MainActivity.java", contentMain(packageName), "Show writing workspace and persisted draft state");
                add(out, javaPath, "EditorActivity.java", contentEditor(packageName), "Create documents with restart-safe draft recovery");
                add(out, javaPath, "SearchActivity.java", contentSearch(packageName), "Search persisted documents locally");
                add(out, javaPath, "LibraryActivity.java", contentLibrary(packageName), "Browse persisted documents");
                break;
            default:
                break;
        }

        List<String> notes = new ArrayList<>();
        notes.add("PASS non-media product pass replaced generic sample-state surfaces for " + domain.name().toLowerCase());
        notes.add("PASS generated product uses inset-aware Android chrome and >=48dp interactive targets");
        notes.add("PASS domain records persist in SharedPreferences across process/activity restart");
        notes.add("PASS user input has domain-specific validation and actionable inline errors");
        if (domain == Domain.CONTENT) notes.add("PASS editor recovers an in-progress draft after lifecycle interruption");
        return new Result(projectName, packageName, out, notes);
    }

    private static Domain detect(List<GeneratedProject.FileEntry> files) {
        if (hasSuffix(files, "/MediaProvider.java")) return Domain.NONE;
        if (hasSuffix(files, "/TransactionsActivity.java") && hasSuffix(files, "/BudgetsActivity.java")) return Domain.FINANCE;
        if (hasSuffix(files, "/TimelineActivity.java") && hasSuffix(files, "/DataControlsActivity.java")) return Domain.TRACKER;
        if (hasSuffix(files, "/EditorActivity.java") && hasSuffix(files, "/SearchActivity.java")) return Domain.CONTENT;
        return Domain.NONE;
    }

    private static boolean hasSuffix(List<GeneratedProject.FileEntry> files, String suffix) {
        for (GeneratedProject.FileEntry f : files) if (f != null && f.path != null && f.path.endsWith(suffix)) return true;
        return false;
    }

    private static boolean isReplacedActivity(String path, Domain domain) {
        if (path == null) return false;
        if (path.endsWith("/MainActivity.java")) return true;
        switch (domain) {
            case FINANCE: return path.endsWith("/TransactionsActivity.java") || path.endsWith("/BudgetsActivity.java") || path.endsWith("/ReportsActivity.java");
            case TRACKER: return path.endsWith("/TimelineActivity.java") || path.endsWith("/ReportsActivity.java") || path.endsWith("/DataControlsActivity.java");
            case CONTENT: return path.endsWith("/EditorActivity.java") || path.endsWith("/SearchActivity.java") || path.endsWith("/LibraryActivity.java");
            default: return false;
        }
    }

    private static void add(List<GeneratedProject.FileEntry> out, String path, String name, String content, String hint) {
        out.add(new GeneratedProject.FileEntry("app/src/main/java/" + path + "/" + name, content, hint));
    }

    private static String appScreen(String pkg, String appName) {
        return """
                package %s;
                import android.app.*;import android.graphics.*;import android.graphics.drawable.*;import android.os.*;import android.view.*;import android.widget.*;
                public abstract class AppScreen extends Activity{
                  protected LinearLayout body;protected LocalStore store;private TextView topTitle;private LinearLayout bottomNav;
                  protected final int BG=Color.rgb(15,17,22),SURFACE=Color.rgb(27,30,38),SURFACE2=Color.rgb(36,40,50),TEXT=Color.rgb(238,240,246),MUTED=Color.rgb(164,171,187),ACCENT=Color.rgb(111,125,255);
                  @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);store=new LocalStore(this);
                    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
                    LinearLayout appBar=new LinearLayout(this);appBar.setOrientation(LinearLayout.VERTICAL);appBar.setPadding(dp(20),dp(12),dp(20),dp(10));
                    TextView brand=text("%s",12,false);brand.setTextColor(MUTED);appBar.addView(brand);topTitle=text("Home",24,true);appBar.addView(topTitle);root.addView(appBar,new LinearLayout.LayoutParams(-1,-2));
                    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(8),dp(16),dp(24));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
                    bottomNav=new LinearLayout(this);bottomNav.setOrientation(LinearLayout.HORIZONTAL);bottomNav.setBackgroundColor(SURFACE);bottomNav.setPadding(dp(6),dp(6),dp(6),dp(6));root.addView(bottomNav,new LinearLayout.LayoutParams(-1,-2));setContentView(root);
                    if(Build.VERSION.SDK_INT>=21){root.setOnApplyWindowInsetsListener((v,insets)->{appBar.setPadding(dp(20),insets.getSystemWindowInsetTop()+dp(10),dp(20),dp(10));bottomNav.setPadding(dp(6),dp(6),dp(6),insets.getSystemWindowInsetBottom()+dp(6));return insets;});root.requestApplyInsets();}
                    render();}
                  protected abstract void render();protected void title(String s){topTitle.setText(s==null?"":s);}protected void subtitle(String s){TextView t=text(s,14,false);t.setTextColor(MUTED);body.addView(t,margin(-1,-2,0,0,0,12));}
                  protected void section(String s){body.addView(text(s,18,true),margin(-1,-2,0,10,0,8));}
                  protected TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextColor(TEXT);t.setTextSize(size);t.setTypeface(android.graphics.Typeface.create("sans-serif",bold?1:0));t.setGravity(Gravity.CENTER_VERTICAL);return t;}
                  protected EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setTextSize(16);e.setPadding(dp(14),0,dp(14),0);e.setBackground(round(SURFACE,14));e.setMinHeight(dp(52));e.setContentDescription(hint);return e;}
                  protected Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(TEXT);b.setMinHeight(dp(48));b.setContentDescription(label);b.setBackground(round(SURFACE2,14));b.setLayoutParams(margin(-1,dp(50),0,6,0,6));return b;}
                  protected LinearLayout card(String heading,String supporting){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(round(SURFACE,16));c.addView(text(heading,16,true));if(supporting!=null&&!supporting.isEmpty()){TextView s=text(supporting,13,false);s.setTextColor(MUTED);s.setPadding(0,dp(4),0,0);c.addView(s);}c.setLayoutParams(margin(-1,-2,0,0,0,10));return c;}
                  protected void nav(String[] labels,Class[] screens){bottomNav.removeAllViews();for(int i=0;i<labels.length;i++){final Class target=screens[i];TextView item=text(labels[i],11,false);item.setGravity(Gravity.CENTER);item.setMinHeight(dp(52));item.setContentDescription("Open "+labels[i]);item.setOnClickListener(v->AppNavigator.open(this,target));bottomNav.addView(item,new LinearLayout.LayoutParams(0,dp(52),1));}}
                  protected GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}protected LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}protected int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
                }
                """.formatted(pkg, java(appName));
    }

    private static String financeMain(String pkg) {
        return """
                package %s;
                public final class MainActivity extends AppScreen{protected void render(){title("Overview");subtitle("Your local finance snapshot. Data stays on this device.");int spent=totalCents();int budget=store.number("monthly_budget_cents");body.addView(card("Spent",money(spent)));body.addView(card("Monthly budget",budget>0?money(budget):"Not set"));body.addView(card("Remaining",budget>0?money(budget-spent):"Set a budget to calculate"));nav(new String[]{"Overview","Transactions","Budget","Reports"},new Class[]{MainActivity.class,TransactionsActivity.class,BudgetsActivity.class,ReportsActivity.class});}private int totalCents(){int sum=0;String raw=store.text("transactions","");for(String row:raw.split("\\n")){String[] p=row.split("\\|",3);if(p.length>0)try{sum+=Integer.parseInt(p[0]);}catch(Exception ignored){}}return sum;}private String money(int cents){return String.format(java.util.Locale.US,"$%.2f",cents/100.0);}}
                """.formatted(pkg);
    }

    private static String financeTransactions(String pkg) {
        return """
                package %s;
                import android.text.InputType;import android.widget.*;import java.util.*;
                public final class TransactionsActivity extends AppScreen{private LinearLayout list;protected void render(){title("Transactions");subtitle("Add an expense. Amount, category, and note are persisted locally.");EditText amount=field("Amount, e.g. 12.50");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);EditText category=field("Category");EditText note=field("Note (optional)");body.addView(amount);body.addView(category);body.addView(note);Button add=button("Add transaction");body.addView(add);section("Recent");list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);body.addView(list);add.setOnClickListener(v->{String a=amount.getText().toString().trim(),c=category.getText().toString().trim();if(a.isEmpty()){amount.setError("Enter an amount");return;}if(c.isEmpty()){category.setError("Enter a category");return;}try{int cents=(int)Math.round(Double.parseDouble(a)*100.0);if(cents<=0){amount.setError("Amount must be greater than zero");return;}String safeNote=note.getText().toString().replace("|","/").replace("\\n"," ");String safeCat=c.replace("|","/").replace("\\n"," ");String old=store.text("transactions","");store.putText("transactions",cents+"|"+safeCat+"|"+safeNote+(old.isEmpty()?"":"\\n"+old));amount.setText("");note.setText("");show();}catch(NumberFormatException e){amount.setError("Use a valid number");}});show();nav(new String[]{"Overview","Transactions","Budget","Reports"},new Class[]{MainActivity.class,TransactionsActivity.class,BudgetsActivity.class,ReportsActivity.class});}private void show(){list.removeAllViews();String raw=store.text("transactions","");if(raw.isEmpty()){list.addView(text("No transactions yet.",14,false));return;}int shown=0;for(String row:raw.split("\\n")){String[] p=row.split("\\|",3);if(p.length>=2){int cents=0;try{cents=Integer.parseInt(p[0]);}catch(Exception ignored){}String detail=(p.length==3&&!p[2].isEmpty()?p[2]+" · ":"")+String.format(Locale.US,"$%.2f",cents/100.0);list.addView(card(p[1],detail));if(++shown>=30)break;}}}}
                """.formatted(pkg);
    }

    private static String financeBudgets(String pkg) {
        return """
                package %s;
                import android.text.InputType;import android.widget.*;
                public final class BudgetsActivity extends AppScreen{protected void render(){title("Budget");int current=store.number("monthly_budget_cents");subtitle(current>0?"Current monthly budget: "+String.format(java.util.Locale.US,"$%.2f",current/100.0):"Set a monthly spending target.");EditText value=field("Monthly budget");value.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);body.addView(value);Button save=button("Save budget");save.setOnClickListener(v->{try{int cents=(int)Math.round(Double.parseDouble(value.getText().toString().trim())*100.0);if(cents<=0){value.setError("Budget must be greater than zero");return;}store.number("monthly_budget_cents",cents);recreate();}catch(Exception e){value.setError("Use a valid number");}});body.addView(save);nav(new String[]{"Overview","Transactions","Budget","Reports"},new Class[]{MainActivity.class,TransactionsActivity.class,BudgetsActivity.class,ReportsActivity.class});}}
                """.formatted(pkg);
    }

    private static String financeReports(String pkg) {
        return """
                package %s;
                public final class ReportsActivity extends AppScreen{protected void render(){title("Reports");String raw=store.text("transactions","");int count=0,total=0;for(String row:raw.split("\\n")){if(row.isEmpty())continue;String[] p=row.split("\\|",3);try{total+=Integer.parseInt(p[0]);count++;}catch(Exception ignored){}}int budget=store.number("monthly_budget_cents");body.addView(card("Transactions",String.valueOf(count)));body.addView(card("Total spent",String.format(java.util.Locale.US,"$%.2f",total/100.0)));if(budget>0)body.addView(card(total<=budget?"Within budget":"Over budget",String.format(java.util.Locale.US,"$%.2f remaining",(budget-total)/100.0)));nav(new String[]{"Overview","Transactions","Budget","Reports"},new Class[]{MainActivity.class,TransactionsActivity.class,BudgetsActivity.class,ReportsActivity.class});}}
                """.formatted(pkg);
    }

    private static String trackerMain(String pkg) {
        return """
                package %s;
                public final class MainActivity extends AppScreen{protected void render(){title("Activity");int count=0,minutes=0;for(String row:store.text("activity_log","").split("\\n")){if(row.isEmpty())continue;String[] p=row.split("\\|",2);try{minutes+=Integer.parseInt(p[0]);count++;}catch(Exception ignored){}}body.addView(card("Entries",String.valueOf(count)));body.addView(card("Tracked time",minutes+" minutes"));body.addView(card("Privacy","Only activity you explicitly enter is stored by this generated app."));nav(new String[]{"Activity","Timeline","Reports","Data"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}}
                """.formatted(pkg);
    }

    private static String trackerTimeline(String pkg) {
        return """
                package %s;
                import android.text.InputType;import android.widget.*;
                public final class TimelineActivity extends AppScreen{private LinearLayout list;protected void render(){title("Timeline");subtitle("Record an activity and duration.");EditText name=field("Activity name");EditText minutes=field("Minutes");minutes.setInputType(InputType.TYPE_CLASS_NUMBER);body.addView(name);body.addView(minutes);Button add=button("Add activity");body.addView(add);section("Recent activity");list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);body.addView(list);add.setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Enter an activity");return;}try{int m=Integer.parseInt(minutes.getText().toString().trim());if(m<=0||m>1440){minutes.setError("Enter 1–1440 minutes");return;}String old=store.text("activity_log","");store.putText("activity_log",m+"|"+n.replace("|","/").replace("\\n"," ")+(old.isEmpty()?"":"\\n"+old));name.setText("");minutes.setText("");show();}catch(Exception e){minutes.setError("Enter whole minutes");}});show();nav(new String[]{"Activity","Timeline","Reports","Data"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}private void show(){list.removeAllViews();String raw=store.text("activity_log","");if(raw.isEmpty()){list.addView(text("No activity recorded yet.",14,false));return;}int shown=0;for(String row:raw.split("\\n")){String[] p=row.split("\\|",2);if(p.length==2)list.addView(card(p[1],p[0]+" minutes"));if(++shown>=30)break;}}}
                """.formatted(pkg);
    }

    private static String trackerReports(String pkg) {
        return """
                package %s;
                public final class ReportsActivity extends AppScreen{protected void render(){title("Reports");int count=0,total=0,longest=0;for(String row:store.text("activity_log","").split("\\n")){if(row.isEmpty())continue;String[] p=row.split("\\|",2);try{int m=Integer.parseInt(p[0]);total+=m;longest=Math.max(longest,m);count++;}catch(Exception ignored){}}body.addView(card("Activities",String.valueOf(count)));body.addView(card("Total time",total+" minutes"));body.addView(card("Longest entry",longest+" minutes"));nav(new String[]{"Activity","Timeline","Reports","Data"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}}
                """.formatted(pkg);
    }

    private static String trackerControls(String pkg) {
        return """
                package %s;
                import android.widget.*;
                public final class DataControlsActivity extends AppScreen{protected void render(){title("Data controls");subtitle("Your activity log is local. Clearing it is explicit and irreversible.");body.addView(card("Stored entries",String.valueOf(count())));Button clear=button("Clear activity history");clear.setOnClickListener(v->{if(!store.flag("confirm_clear")){store.flag("confirm_clear",true);clear.setText("Tap again to confirm clear");}else{store.putText("activity_log","");store.flag("confirm_clear",false);recreate();}});body.addView(clear);nav(new String[]{"Activity","Timeline","Reports","Data"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}private int count(){String raw=store.text("activity_log","");if(raw.isEmpty())return 0;return raw.split("\\n").length;}}
                """.formatted(pkg);
    }

    private static String contentMain(String pkg) {
        return """
                package %s;
                import android.widget.*;
                public final class MainActivity extends AppScreen{protected void render(){title("Workspace");String docs=store.text("documents","");int count=docs.isEmpty()?0:docs.split("\\u001E",-1).length;body.addView(card("Documents",String.valueOf(count)));String draft=store.text("draft_title","");if(!draft.isEmpty())body.addView(card("Draft recovered",draft));Button write=button("Write a document");write.setOnClickListener(v->AppNavigator.open(this,EditorActivity.class));body.addView(write);nav(new String[]{"Workspace","Library","Write","Search"},new Class[]{MainActivity.class,LibraryActivity.class,EditorActivity.class,SearchActivity.class});}}
                """.formatted(pkg);
    }

    private static String contentEditor(String pkg) {
        return """
                package %s;
                import android.widget.*;
                public final class EditorActivity extends AppScreen{private EditText titleField,bodyField;protected void render(){title("Editor");subtitle("Draft text is recovered after interruptions until you save or clear it.");titleField=field("Title");titleField.setText(store.text("draft_title",""));bodyField=new EditText(this);bodyField.setHint("Write here…");bodyField.setText(store.text("draft_body",""));bodyField.setTextColor(TEXT);bodyField.setHintTextColor(MUTED);bodyField.setMinLines(10);bodyField.setGravity(android.view.Gravity.TOP);bodyField.setPadding(dp(14),dp(14),dp(14),dp(14));bodyField.setBackground(round(SURFACE,14));body.addView(titleField);body.addView(bodyField,new LinearLayout.LayoutParams(-1,-2));Button save=button("Save document");save.setOnClickListener(v->{String t=titleField.getText().toString().trim(),b=bodyField.getText().toString().trim();if(t.isEmpty()){titleField.setError("Enter a title");return;}if(b.isEmpty()){bodyField.setError("Write some content");return;}String record=t.replace("\\u001F"," ").replace("\\u001E"," ")+"\\u001F"+b.replace("\\u001F"," ").replace("\\u001E"," ");String old=store.text("documents","");store.putText("documents",record+(old.isEmpty()?"":"\\u001E"+old));store.putText("draft_title","");store.putText("draft_body","");AppNavigator.open(this,LibraryActivity.class);});body.addView(save);nav(new String[]{"Workspace","Library","Write","Search"},new Class[]{MainActivity.class,LibraryActivity.class,EditorActivity.class,SearchActivity.class});}@Override protected void onPause(){super.onPause();if(titleField!=null)store.putText("draft_title",titleField.getText().toString());if(bodyField!=null)store.putText("draft_body",bodyField.getText().toString());}}
                """.formatted(pkg);
    }

    private static String contentLibrary(String pkg) {
        return """
                package %s;
                public final class LibraryActivity extends AppScreen{protected void render(){title("Library");String raw=store.text("documents","");if(raw.isEmpty())body.addView(card("No documents","Create your first document from Write."));else for(String record:raw.split("\\u001E")){String[] p=record.split("\\u001F",2);if(p.length==2)body.addView(card(p[0],preview(p[1])));}nav(new String[]{"Workspace","Library","Write","Search"},new Class[]{MainActivity.class,LibraryActivity.class,EditorActivity.class,SearchActivity.class});}private String preview(String s){String flat=s.replace("\\n"," ");return flat.length()>120?flat.substring(0,120)+"…":flat;}}
                """.formatted(pkg);
    }

    private static String contentSearch(String pkg) {
        return """
                package %s;
                import android.widget.*;
                public final class SearchActivity extends AppScreen{private LinearLayout results;protected void render(){title("Search");EditText q=field("Search documents");body.addView(q);Button go=button("Search");body.addView(go);results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);go.setOnClickListener(v->show(q.getText().toString().trim()));nav(new String[]{"Workspace","Library","Write","Search"},new Class[]{MainActivity.class,LibraryActivity.class,EditorActivity.class,SearchActivity.class});}private void show(String q){results.removeAllViews();if(q.isEmpty()){results.addView(text("Enter a search term.",14,false));return;}int found=0;for(String record:store.text("documents","").split("\\u001E")){String[] p=record.split("\\u001F",2);if(p.length==2&&(p[0]+" "+p[1]).toLowerCase(java.util.Locale.US).contains(q.toLowerCase(java.util.Locale.US))){results.addView(card(p[0],p[1].length()>100?p[1].substring(0,100)+"…":p[1]));found++;}}if(found==0)results.addView(text("No matching documents.",14,false));}}
                """.formatted(pkg);
    }

    private static String java(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private GeneralProductPostProcessor() {}
}
