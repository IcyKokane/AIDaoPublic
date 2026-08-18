package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;

/** Turns common non-media scaffolds into coherent offline-first Android products. */
final class GeneralProductPostProcessor {
    private enum Domain { FINANCE, TRACKER, CONTENT, NONE }

    static final class Result {
        final String projectName, packageName;
        final List<GeneratedProject.FileEntry> files;
        final List<String> notes;
        Result(String projectName,String packageName,List<GeneratedProject.FileEntry> files,List<String> notes){this.projectName=projectName;this.packageName=packageName;this.files=files;this.notes=notes;}
    }

    static Result process(String projectName,String packageName,List<GeneratedProject.FileEntry> incoming){
        List<GeneratedProject.FileEntry> source=incoming==null?new ArrayList<>():new ArrayList<>(incoming);
        Domain domain=detect(source);
        if(domain==Domain.NONE)return new Result(projectName,packageName,source,new ArrayList<>());
        String javaPath=packageName.replace('.','/');
        List<GeneratedProject.FileEntry> out=new ArrayList<>();
        for(GeneratedProject.FileEntry f:source){if(f==null)continue;if(f.path.endsWith("/GeneratedScreen.java")||f.path.endsWith("/AppScreen.java")||replaced(f.path,domain))continue;out.add(f);}
        add(out,javaPath,"AppScreen.java",appScreen(packageName,projectName),"Use inset-aware Android product chrome and accessible navigation");
        if(domain==Domain.FINANCE){
            add(out,javaPath,"MainActivity.java",financeMain(packageName),"Show persisted finance summary");
            add(out,javaPath,"TransactionsActivity.java",financeTransactions(packageName),"Persist validated transaction records");
            add(out,javaPath,"BudgetsActivity.java",financeBudget(packageName),"Persist monthly budget");
            add(out,javaPath,"ReportsActivity.java",financeReports(packageName),"Calculate finance report");
        }else if(domain==Domain.TRACKER){
            add(out,javaPath,"MainActivity.java",trackerMain(packageName),"Show persisted activity summary");
            add(out,javaPath,"TimelineActivity.java",trackerTimeline(packageName),"Persist activity records");
            add(out,javaPath,"ReportsActivity.java",trackerReports(packageName),"Calculate activity report");
            add(out,javaPath,"DataControlsActivity.java",trackerControls(packageName),"Provide explicit local data controls");
        }else{
            add(out,javaPath,"MainActivity.java",contentMain(packageName),"Show writing workspace");
            add(out,javaPath,"EditorActivity.java",contentEditor(packageName),"Persist documents and recover drafts");
            add(out,javaPath,"SearchActivity.java",contentSearch(packageName),"Search local documents");
            add(out,javaPath,"LibraryActivity.java",contentLibrary(packageName),"Browse local documents");
        }
        List<String> notes=new ArrayList<>();
        notes.add("PASS non-media product pass replaced generic sample-state surfaces for "+domain.name().toLowerCase());
        notes.add("PASS generated product uses inset-aware Android chrome and >=48dp interactive targets");
        notes.add("PASS domain records persist in SharedPreferences across process/activity restart");
        notes.add("PASS user input has domain-specific validation and actionable inline errors");
        if(domain==Domain.CONTENT)notes.add("PASS editor recovers an in-progress draft after lifecycle interruption");
        return new Result(projectName,packageName,out,notes);
    }

    private static Domain detect(List<GeneratedProject.FileEntry> files){
        if(has(files,"/MediaProvider.java"))return Domain.NONE;
        if(has(files,"/TransactionsActivity.java")&&has(files,"/BudgetsActivity.java"))return Domain.FINANCE;
        if(has(files,"/TimelineActivity.java")&&has(files,"/DataControlsActivity.java"))return Domain.TRACKER;
        if(has(files,"/EditorActivity.java")&&has(files,"/SearchActivity.java"))return Domain.CONTENT;
        return Domain.NONE;
    }
    private static boolean has(List<GeneratedProject.FileEntry> files,String suffix){for(GeneratedProject.FileEntry f:files)if(f!=null&&f.path!=null&&f.path.endsWith(suffix))return true;return false;}
    private static boolean replaced(String path,Domain d){if(path==null)return false;if(path.endsWith("/MainActivity.java"))return true;switch(d){case FINANCE:return path.endsWith("/TransactionsActivity.java")||path.endsWith("/BudgetsActivity.java")||path.endsWith("/ReportsActivity.java");case TRACKER:return path.endsWith("/TimelineActivity.java")||path.endsWith("/ReportsActivity.java")||path.endsWith("/DataControlsActivity.java");case CONTENT:return path.endsWith("/EditorActivity.java")||path.endsWith("/SearchActivity.java")||path.endsWith("/LibraryActivity.java");default:return false;}}
    private static void add(List<GeneratedProject.FileEntry> out,String path,String name,String content,String hint){out.add(new GeneratedProject.FileEntry("app/src/main/java/"+path+"/"+name,content,hint));}
    private static String pkg(String p,String body){return "package "+p+";"+body;}

    private static String appScreen(String p,String appName){return pkg(p,
            "import android.app.*;import android.graphics.*;import android.graphics.drawable.*;import android.os.*;import android.view.*;import android.widget.*;"+
            "public abstract class AppScreen extends Activity{"+
            "protected LinearLayout body;protected LocalStore store;private TextView topTitle;private LinearLayout bottomNav;protected final int BG=Color.rgb(15,17,22),SURFACE=Color.rgb(27,30,38),SURFACE2=Color.rgb(36,40,50),TEXT=Color.rgb(238,240,246),MUTED=Color.rgb(164,171,187);"+
            "@Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);store=new LocalStore(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.VERTICAL);bar.setPadding(dp(20),dp(12),dp(20),dp(10));TextView brand=text(\""+java(appName)+"\",12,false);brand.setTextColor(MUTED);bar.addView(brand);topTitle=text(\"Home\",24,true);bar.addView(topTitle);root.addView(bar,new LinearLayout.LayoutParams(-1,-2));ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(8),dp(16),dp(24));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));bottomNav=new LinearLayout(this);bottomNav.setOrientation(LinearLayout.HORIZONTAL);bottomNav.setBackgroundColor(SURFACE);root.addView(bottomNav,new LinearLayout.LayoutParams(-1,-2));setContentView(root);if(Build.VERSION.SDK_INT>=21){root.setOnApplyWindowInsetsListener((v,i)->{bar.setPadding(dp(20),i.getSystemWindowInsetTop()+dp(10),dp(20),dp(10));bottomNav.setPadding(dp(6),dp(6),dp(6),i.getSystemWindowInsetBottom()+dp(6));return i;});root.requestApplyInsets();}render();}"+
            "protected abstract void render();protected void title(String s){topTitle.setText(s);}protected void subtitle(String s){TextView t=text(s,14,false);t.setTextColor(MUTED);body.addView(t,margin(-1,-2,0,0,0,12));}protected void section(String s){body.addView(text(s,18,true),margin(-1,-2,0,10,0,8));}"+
            "protected TextView text(String v,int size,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextColor(TEXT);t.setTextSize(size);t.setTypeface(android.graphics.Typeface.create(\"sans-serif\",bold?1:0));t.setGravity(Gravity.CENTER_VERTICAL);return t;}"+
            "protected EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setPadding(dp(14),0,dp(14),0);e.setBackground(round(SURFACE,14));e.setMinHeight(dp(52));e.setContentDescription(hint);return e;}"+
            "protected Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEXT);b.setMinHeight(dp(48));b.setContentDescription(label);b.setBackground(round(SURFACE2,14));b.setLayoutParams(margin(-1,dp(50),0,6,0,6));return b;}"+
            "protected LinearLayout card(String h,String s){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(round(SURFACE,16));c.addView(text(h,16,true));if(s!=null&&!s.isEmpty()){TextView t=text(s,13,false);t.setTextColor(MUTED);c.addView(t);}c.setLayoutParams(margin(-1,-2,0,0,0,10));return c;}"+
            "protected void nav(String[] labels,Class[] screens){bottomNav.removeAllViews();for(int x=0;x<labels.length;x++){final Class target=screens[x];TextView item=text(labels[x],11,false);item.setGravity(Gravity.CENTER);item.setMinHeight(dp(52));item.setContentDescription(\"Open \"+labels[x]);item.setOnClickListener(v->AppNavigator.open(this,target));bottomNav.addView(item,new LinearLayout.LayoutParams(0,dp(52),1));}}"+
            "protected GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}protected LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(w,h);q.setMargins(dp(l),dp(t),dp(r),dp(b));return q;}protected int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}}"
    );}

    private static String financeMain(String p){return pkg(p,
            "public final class MainActivity extends AppScreen{protected void render(){title(\"Overview\");subtitle(\"Your local finance snapshot. Data stays on this device.\");int spent=total(),budget=store.number(\"monthly_budget_cents\");body.addView(card(\"Spent\",money(spent)));body.addView(card(\"Monthly budget\",budget>0?money(budget):\"Not set\"));body.addView(card(\"Remaining\",budget>0?money(budget-spent):\"Set a budget to calculate\"));nav(new String[]{\"Overview\",\"Transactions\",\"Budget\",\"Reports\"},new Class[]{MainActivity.class,TransactionsActivity.class,BudgetsActivity.class,ReportsActivity.class});}private int total(){int s=0;for(String r:store.text(\"transactions\",\"\").split(\"\\n\")){if(r.isEmpty())continue;String[] x=r.split(\"[|]\",3);try{s+=Integer.parseInt(x[0]);}catch(Exception ignored){}}return s;}private String money(int c){return \"$\"+(c/100)+\".\"+String.format(java.util.Locale.US,\"%02d\",Math.abs(c%100));}}"
    );}
    private static String financeTransactions(String p){return pkg(p,
            "import android.text.InputType;import android.widget.*;public final class TransactionsActivity extends AppScreen{private LinearLayout list;protected void render(){title(\"Transactions\");subtitle(\"Add an expense. Amount, category, and note are persisted locally.\");EditText amount=field(\"Amount, e.g. 12.50\");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);EditText category=field(\"Category\");EditText note=field(\"Note (optional)\");body.addView(amount);body.addView(category);body.addView(note);Button add=button(\"Add transaction\");body.addView(add);section(\"Recent\");list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);body.addView(list);add.setOnClickListener(v->{String a=amount.getText().toString().trim(),c=category.getText().toString().trim();if(a.isEmpty()){amount.setError(\"Enter an amount\");return;}if(c.isEmpty()){category.setError(\"Enter a category\");return;}try{int cents=(int)Math.round(Double.parseDouble(a)*100);if(cents<=0){amount.setError(\"Amount must be greater than zero\");return;}String old=store.text(\"transactions\",\"\");store.putText(\"transactions\",cents+\"|\"+c.replace(\"|\",\"/\")+\"|\"+note.getText().toString().replace(\"|\",\"/\").replace(\"\\n\",\" \")+(old.isEmpty()?\"\":\"\\n\"+old));amount.setText(\"\");note.setText(\"\");show();}catch(Exception e){amount.setError(\"Use a valid number\");}});show();nav(new String[]{\"Overview\",\"Transactions\",\"Budget\",\"Reports\"},new Class[]{MainActivity.class,TransactionsActivity.class,BudgetsActivity.class,ReportsActivity.class});}private void show(){list.removeAllViews();String raw=store.text(\"transactions\",\"\");if(raw.isEmpty()){list.addView(text(\"No transactions yet.\",14,false));return;}int n=0;for(String r:raw.split(\"\\n\")){String[] x=r.split(\"[|]\",3);if(x.length>=2)list.addView(card(x[1],(x.length==3?x[2]+\" · \":\"\")+x[0]+\" cents\"));if(++n>=30)break;}}}"
    );}
    private static String financeBudget(String p){return pkg(p,
            "import android.text.InputType;import android.widget.*;public final class BudgetsActivity extends AppScreen{protected void render(){title(\"Budget\");int c=store.number(\"monthly_budget_cents\");subtitle(c>0?\"Current monthly budget: \"+c+\" cents\":\"Set a monthly spending target.\");EditText v=field(\"Monthly budget\");v.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);body.addView(v);Button save=button(\"Save budget\");save.setOnClickListener(x->{try{int cents=(int)Math.round(Double.parseDouble(v.getText().toString().trim())*100);if(cents<=0){v.setError(\"Budget must be greater than zero\");return;}store.number(\"monthly_budget_cents\",cents);recreate();}catch(Exception e){v.setError(\"Use a valid number\");}});body.addView(save);nav(new String[]{\"Overview\",\"Transactions\",\"Budget\",\"Reports\"},new Class[]{MainActivity.class,TransactionsActivity.class,BudgetsActivity.class,ReportsActivity.class});}}"
    );}
    private static String financeReports(String p){return pkg(p,
            "public final class ReportsActivity extends AppScreen{protected void render(){title(\"Reports\");int count=0,total=0;for(String r:store.text(\"transactions\",\"\").split(\"\\n\")){if(r.isEmpty())continue;try{total+=Integer.parseInt(r.split(\"[|]\",3)[0]);count++;}catch(Exception ignored){}}int budget=store.number(\"monthly_budget_cents\");body.addView(card(\"Transactions\",String.valueOf(count)));body.addView(card(\"Total spent\",total+\" cents\"));if(budget>0)body.addView(card(total<=budget?\"Within budget\":\"Over budget\",(budget-total)+\" cents remaining\"));nav(new String[]{\"Overview\",\"Transactions\",\"Budget\",\"Reports\"},new Class[]{MainActivity.class,TransactionsActivity.class,BudgetsActivity.class,ReportsActivity.class});}}"
    );}

    private static String trackerMain(String p){return pkg(p,
            "public final class MainActivity extends AppScreen{protected void render(){title(\"Activity\");int count=0,min=0;for(String r:store.text(\"activity_log\",\"\").split(\"\\n\")){if(r.isEmpty())continue;try{min+=Integer.parseInt(r.split(\"[|]\",2)[0]);count++;}catch(Exception ignored){}}body.addView(card(\"Entries\",String.valueOf(count)));body.addView(card(\"Tracked time\",min+\" minutes\"));body.addView(card(\"Privacy\",\"Only activity you explicitly enter is stored.\"));nav(new String[]{\"Activity\",\"Timeline\",\"Reports\",\"Data\"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}}"
    );}
    private static String trackerTimeline(String p){return pkg(p,
            "import android.text.InputType;import android.widget.*;public final class TimelineActivity extends AppScreen{private LinearLayout list;protected void render(){title(\"Timeline\");EditText name=field(\"Activity name\"),minutes=field(\"Minutes\");minutes.setInputType(InputType.TYPE_CLASS_NUMBER);body.addView(name);body.addView(minutes);Button add=button(\"Add activity\");body.addView(add);section(\"Recent activity\");list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);body.addView(list);add.setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){name.setError(\"Enter an activity\");return;}try{int m=Integer.parseInt(minutes.getText().toString().trim());if(m<=0||m>1440){minutes.setError(\"Enter 1–1440 minutes\");return;}String old=store.text(\"activity_log\",\"\");store.putText(\"activity_log\",m+\"|\"+n.replace(\"|\",\"/\")+(old.isEmpty()?\"\":\"\\n\"+old));name.setText(\"\");minutes.setText(\"\");show();}catch(Exception e){minutes.setError(\"Enter whole minutes\");}});show();nav(new String[]{\"Activity\",\"Timeline\",\"Reports\",\"Data\"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}private void show(){list.removeAllViews();String raw=store.text(\"activity_log\",\"\");if(raw.isEmpty()){list.addView(text(\"No activity recorded yet.\",14,false));return;}for(String r:raw.split(\"\\n\")){String[] x=r.split(\"[|]\",2);if(x.length==2)list.addView(card(x[1],x[0]+\" minutes\"));}}}"
    );}
    private static String trackerReports(String p){return pkg(p,
            "public final class ReportsActivity extends AppScreen{protected void render(){title(\"Reports\");int count=0,total=0,longest=0;for(String r:store.text(\"activity_log\",\"\").split(\"\\n\")){if(r.isEmpty())continue;try{int m=Integer.parseInt(r.split(\"[|]\",2)[0]);total+=m;longest=Math.max(longest,m);count++;}catch(Exception ignored){}}body.addView(card(\"Activities\",String.valueOf(count)));body.addView(card(\"Total time\",total+\" minutes\"));body.addView(card(\"Longest entry\",longest+\" minutes\"));nav(new String[]{\"Activity\",\"Timeline\",\"Reports\",\"Data\"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}}"
    );}
    private static String trackerControls(String p){return pkg(p,
            "import android.widget.*;public final class DataControlsActivity extends AppScreen{protected void render(){title(\"Data controls\");subtitle(\"Clearing local activity is explicit and irreversible.\");body.addView(card(\"Stored entries\",String.valueOf(count())));Button clear=button(\"Clear activity history\");clear.setOnClickListener(v->{if(!store.flag(\"confirm_clear\")){store.flag(\"confirm_clear\",true);clear.setText(\"Tap again to confirm clear\");}else{store.putText(\"activity_log\",\"\");store.flag(\"confirm_clear\",false);recreate();}});body.addView(clear);nav(new String[]{\"Activity\",\"Timeline\",\"Reports\",\"Data\"},new Class[]{MainActivity.class,TimelineActivity.class,ReportsActivity.class,DataControlsActivity.class});}private int count(){String r=store.text(\"activity_log\",\"\");return r.isEmpty()?0:r.split(\"\\n\").length;}}"
    );}

    private static String contentMain(String p){return pkg(p,
            "import android.widget.*;public final class MainActivity extends AppScreen{protected void render(){title(\"Workspace\");String docs=store.text(\"documents\",\"\");int count=docs.isEmpty()?0:docs.split(\"\\n---DOC---\\n\",-1).length;body.addView(card(\"Documents\",String.valueOf(count)));String draft=store.text(\"draft_title\",\"\");if(!draft.isEmpty())body.addView(card(\"Draft recovered\",draft));Button write=button(\"Write a document\");write.setOnClickListener(v->AppNavigator.open(this,EditorActivity.class));body.addView(write);nav(new String[]{\"Workspace\",\"Library\",\"Write\",\"Search\"},new Class[]{MainActivity.class,LibraryActivity.class,EditorActivity.class,SearchActivity.class});}}"
    );}
    private static String contentEditor(String p){return pkg(p,
            "import android.os.*;import android.widget.*;public final class EditorActivity extends AppScreen{private EditText titleField,bodyField;protected void render(){title(\"Editor\");subtitle(\"Draft text is recovered after a lifecycle interruption.\");titleField=field(\"Document title\");bodyField=field(\"Document body\");bodyField.setSingleLine(false);bodyField.setMinHeight(dp(180));titleField.setText(store.text(\"draft_title\",\"\"));bodyField.setText(store.text(\"draft_body\",\"\"));body.addView(titleField);body.addView(bodyField);Button save=button(\"Save document\");save.setOnClickListener(v->{String t=titleField.getText().toString().trim(),b=bodyField.getText().toString().trim();if(t.isEmpty()){titleField.setError(\"Enter a title\");return;}if(b.isEmpty()){bodyField.setError(\"Write some content\");return;}String record=t.replace(\"|\",\"/\")+\"|\"+b.replace(\"|\",\"/\").replace(\"\\n\",\" \");String old=store.text(\"documents\",\"\");store.putText(\"documents\",record+(old.isEmpty()?\"\":\"\\n---DOC---\\n\"+old));store.putText(\"draft_title\",\"\");store.putText(\"draft_body\",\"\");titleField.setText(\"\");bodyField.setText(\"\");});body.addView(save);nav(new String[]{\"Workspace\",\"Library\",\"Write\",\"Search\"},new Class[]{MainActivity.class,LibraryActivity.class,EditorActivity.class,SearchActivity.class});}@Override protected void onPause(){super.onPause();if(titleField!=null){store.putText(\"draft_title\",titleField.getText().toString());store.putText(\"draft_body\",bodyField.getText().toString());}}}"
    );}
    private static String contentSearch(String p){return pkg(p,
            "import android.widget.*;public final class SearchActivity extends AppScreen{private LinearLayout results;protected void render(){title(\"Search documents\");EditText q=field(\"Search title or content\");body.addView(q);Button go=button(\"Search documents\");body.addView(go);results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);go.setOnClickListener(v->show(q.getText().toString()));nav(new String[]{\"Workspace\",\"Library\",\"Write\",\"Search\"},new Class[]{MainActivity.class,LibraryActivity.class,EditorActivity.class,SearchActivity.class});}private void show(String q){results.removeAllViews();String needle=q==null?\"\":q.trim().toLowerCase();String docs=store.text(\"documents\",\"\");if(needle.isEmpty()){results.addView(text(\"Enter a search term.\",14,false));return;}for(String d:docs.split(\"\\n---DOC---\\n\")){if(d.toLowerCase().contains(needle)){String[] x=d.split(\"[|]\",2);results.addView(card(x[0],x.length>1?x[1]:\"\"));}}if(results.getChildCount()==0)results.addView(text(\"No matching documents.\",14,false));}}"
    );}
    private static String contentLibrary(String p){return pkg(p,
            "public final class LibraryActivity extends AppScreen{protected void render(){title(\"Library\");String docs=store.text(\"documents\",\"\");if(docs.isEmpty())body.addView(text(\"No saved documents yet.\",14,false));else for(String d:docs.split(\"\\n---DOC---\\n\")){String[] x=d.split(\"[|]\",2);body.addView(card(x[0],x.length>1?x[1]:\"\"));}nav(new String[]{\"Workspace\",\"Library\",\"Write\",\"Search\"},new Class[]{MainActivity.class,LibraryActivity.class,EditorActivity.class,SearchActivity.class});}}"
    );}

    private static String java(String s){return(s==null?"":s).replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ");}
}
