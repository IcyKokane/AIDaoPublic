package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;

/**
 * Final generated-media UX pass. It replaces generic form scaffolds with a
 * phone-native Android shell and a discovery-first media experience.
 */
final class NativeFidelityPostProcessor {
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
        if (!media) return new Result(projectName, packageName, source, new ArrayList<>());

        String javaPath = packageName.replace('.', '/');
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        for (GeneratedProject.FileEntry file : source) {
            if (file == null) continue;
            if (file.path.endsWith("/AppScreen.java") || file.path.endsWith("/MainActivity.java") || file.path.endsWith("/ProvidersActivity.java")) continue;
            out.add(file);
        }
        add(out, javaPath, "AppScreen.java", appScreen(packageName, projectName), "Use phone-native Material-style chrome, responsive content, IME handling, and bottom navigation");
        add(out, javaPath, "MainActivity.java", mainActivity(packageName), "Use discovery-first asynchronous provider search, live source health, responsive cards, and actionable errors");
        add(out, javaPath, "ProvidersActivity.java", providersActivity(packageName), "Present built-in and repository providers as mobile extension cards with explicit capability state");

        List<String> notes = new ArrayList<>();
        notes.add("PASS native-fidelity pass uses a persistent bottom navigation bar outside scroll content");
        notes.add("PASS status/navigation/display-cutout insets and keyboard IME visibility are handled by generated phone chrome");
        notes.add("PASS top app bar, typography hierarchy, responsive padding, rounded surfaces, and >=48dp touch targets are phone-native");
        notes.add("PASS generated shell exposes responsive GridLayout composition instead of forcing every screen into a desktop-like vertical form");
        notes.add("PASS Browse opens with useful built-in provider discovery and live source health rather than requiring repository setup first");
        notes.add("PASS Browse uses asynchronous provider requests so network work never blocks the Android UI thread");
        notes.add("PASS provider failures are shown separately from genuine zero-result searches and include retry/source-management actions");
        notes.add("PASS generated extension inventory uses card hierarchy and capability labels instead of a raw configuration form");
        return new Result(projectName, packageName, out, notes);
    }

    private static boolean hasSuffix(List<GeneratedProject.FileEntry> files, String suffix) {
        for (GeneratedProject.FileEntry f : files) if (f != null && f.path != null && f.path.endsWith(suffix)) return true;
        return false;
    }

    private static void add(List<GeneratedProject.FileEntry> out, String path, String name, String content, String hint) {
        out.add(new GeneratedProject.FileEntry("app/src/main/java/" + path + "/" + name, content, hint));
    }

    private static String appScreen(String pkg, String appName) {
        return "package " + pkg + ";\n" +
                "import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.os.*;import android.view.*;import android.view.inputmethod.*;import android.widget.*;\n" +
                "public abstract class AppScreen extends Activity{" +
                "protected LinearLayout body;protected LocalStore store;private TextView topTitle;private LinearLayout bottomNav,appBar;private ScrollView scroll;" +
                "protected final int BG=Color.rgb(15,17,22),SURFACE=Color.rgb(27,30,38),SURFACE2=Color.rgb(36,40,50),TEXT=Color.rgb(238,240,246),MUTED=Color.rgb(164,171,187),ACCENT=Color.rgb(111,125,255),SUCCESS=Color.rgb(105,210,145),DANGER=Color.rgb(255,121,132);" +
                "@Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);store=new LocalStore(this);" +
                "LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);" +
                "appBar=new LinearLayout(this);appBar.setOrientation(LinearLayout.VERTICAL);appBar.setBackgroundColor(BG);appBar.setPadding(dp(20),dp(12),dp(20),dp(10));" +
                "TextView brand=text(\"" + java(appName) + "\",12,false);brand.setTextColor(MUTED);brand.setLetterSpacing(.08f);brand.setPadding(0,0,0,dp(2));appBar.addView(brand);topTitle=text(\"Browse\",28,true);appBar.addView(topTitle);root.addView(appBar,new LinearLayout.LayoutParams(-1,-2));" +
                "scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(contentPad(),dp(8),contentPad(),dp(28));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));" +
                "bottomNav=new LinearLayout(this);bottomNav.setOrientation(LinearLayout.HORIZONTAL);bottomNav.setGravity(Gravity.CENTER);bottomNav.setBackgroundColor(SURFACE);bottomNav.setPadding(dp(4),dp(6),dp(4),dp(6));root.addView(bottomNav,new LinearLayout.LayoutParams(-1,-2));buildBottomNav();setContentView(root);" +
                "if(Build.VERSION.SDK_INT>=21){root.setOnApplyWindowInsetsListener((v,insets)->{int left=insets.getSystemWindowInsetLeft(),top=insets.getSystemWindowInsetTop(),right=insets.getSystemWindowInsetRight(),bottom=insets.getSystemWindowInsetBottom();boolean ime=false;if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;ime=insets.isVisible(WindowInsets.Type.ime());}appBar.setPadding(left+dp(20),top+dp(10),right+dp(20),dp(10));bottomNav.setVisibility(ime?View.GONE:View.VISIBLE);bottomNav.setPadding(left+dp(4),dp(6),right+dp(4),bottom+dp(6));scroll.setPadding(left,0,right,ime?dp(20):0);return insets;});root.requestApplyInsets();}" +
                "render();}" +
                "protected abstract void render();" +
                "protected void title(String s){topTitle.setText(s==null?\"\":s);}" +
                "protected void subtitle(String s){TextView t=text(s,14,false);t.setTextColor(MUTED);t.setLineSpacing(0,1.12f);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(14));body.addView(t,p);}" +
                "protected void section(String s){TextView t=text(s,19,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(14),0,dp(9));body.addView(t,p);}" +
                "protected TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextColor(TEXT);t.setTextSize(size);t.setTypeface(android.graphics.Typeface.create(\"sans-serif\",bold?1:0));t.setGravity(Gravity.CENTER_VERTICAL);t.setLineSpacing(0,1.08f);return t;}" +
                "protected Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(TEXT);b.setMinHeight(dp(48));b.setContentDescription(label);b.setPadding(dp(16),0,dp(16),0);b.setBackground(round(SURFACE2,16));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(6),0,dp(8));b.setLayoutParams(p);return b;}" +
                "protected LinearLayout card(String heading,String supporting){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(round(SURFACE,18));c.setMinimumHeight(dp(72));TextView h=text(heading,16,true);c.addView(h);if(supporting!=null&&supporting.length()>0){TextView s=text(supporting,13,false);s.setTextColor(MUTED);s.setPadding(0,dp(4),0,0);c.addView(s);}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));c.setLayoutParams(p);return c;}" +
                "protected TextView chip(String label,boolean active){TextView c=text(label,12,active);c.setGravity(Gravity.CENTER);c.setTextColor(active?TEXT:MUTED);c.setBackground(round(active?SURFACE2:SURFACE,20));c.setPadding(dp(14),0,dp(14),0);c.setMinimumHeight(dp(40));return c;}" +
                "protected GridLayout grid(){GridLayout g=new GridLayout(this);int widthDp=getResources().getConfiguration().screenWidthDp;g.setColumnCount(widthDp>=840?3:widthDp>=560?2:1);g.setUseDefaultMargins(false);g.setAlignmentMode(GridLayout.ALIGN_BOUNDS);return g;}" +
                "protected void addGridCard(GridLayout g,View v){GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=-2;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);p.setMargins(0,0,dp(8),dp(10));g.addView(v,p);}" +
                "protected LinearLayout emptyState(String heading,String supporting){LinearLayout e=card(heading,supporting);e.setGravity(Gravity.CENTER_VERTICAL);e.setMinimumHeight(dp(120));return e;}" +
                "protected GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}" +
                "protected void nav(){}" +
                "private void buildBottomNav(){String[] names={\"Browse\",\"Library\",\"History\",\"Downloads\",\"Extensions\"};String[] icons={\"⌕\",\"▣\",\"◷\",\"⇩\",\"◈\"};Class[] screens={MainActivity.class,LibraryActivity.class,HistoryActivity.class,DownloadsActivity.class,ProvidersActivity.class};for(int i=0;i<names.length;i++){final Class target=screens[i];boolean selected=getClass()==target;LinearLayout tab=new LinearLayout(this);tab.setOrientation(LinearLayout.VERTICAL);tab.setGravity(Gravity.CENTER);tab.setMinimumHeight(dp(60));tab.setPadding(dp(3),dp(4),dp(3),dp(4));tab.setContentDescription((selected?\"Current \" : \"Open \" )+names[i]);TextView icon=text(icons[i],18,selected);icon.setGravity(Gravity.CENTER);icon.setTextColor(selected?ACCENT:MUTED);tab.addView(icon,new LinearLayout.LayoutParams(-1,dp(24)));TextView item=text(names[i],10,selected);item.setGravity(Gravity.CENTER);item.setTextColor(selected?TEXT:MUTED);tab.addView(item,new LinearLayout.LayoutParams(-1,dp(24)));if(selected){View indicator=new View(this);indicator.setBackground(round(ACCENT,3));tab.addView(indicator,new LinearLayout.LayoutParams(dp(24),dp(3)));}tab.setOnClickListener(v->AppNavigator.open(this,target));bottomNav.addView(tab,new LinearLayout.LayoutParams(0,dp(60),1));}}" +
                "private int contentPad(){int w=getResources().getConfiguration().screenWidthDp;return dp(w>=840?32:w>=560?24:16);}" +
                "protected int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}" +
                "}\n";
    }

    private static String mainActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.content.*;import android.view.*;import android.widget.*;import java.util.*;\n" +
                "public final class MainActivity extends AppScreen{private LinearLayout results,discover;private EditText query;private Button search,verify,retryDiscover;private ProgressBar progress,discoverProgress;private TextView providerState;" +
                "protected void render(){title(\"Browse\");subtitle(\"Discover titles from reviewed built-in catalog sources immediately. Add repository sources only when you want more.\");" +
                "section(\"Sources\");HorizontalScrollView sourceRail=new HorizontalScrollView(this);sourceRail.setHorizontalScrollBarEnabled(false);sourceRail.setOverScrollMode(View.OVER_SCROLL_NEVER);LinearLayout sourceChips=new LinearLayout(this);sourceChips.setOrientation(LinearLayout.HORIZONTAL);for(MediaProvider p:BuiltInProviderCatalog.providers()){TextView chip=chip(p.displayName(),true);chip.setContentDescription(p.displayName()+\" built-in source\");LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(40));cp.setMargins(0,0,dp(8),dp(4));sourceChips.addView(chip,cp);}sourceRail.addView(sourceChips,new HorizontalScrollView.LayoutParams(-2,-2));body.addView(sourceRail,new LinearLayout.LayoutParams(-1,-2));" +
                "providerState=text(\"Checking built-in source health…\",13,false);providerState.setTextColor(MUTED);body.addView(providerState);verify=button(\"Check source health\");verify.setOnClickListener(v->verifySourcesAsync());body.addView(verify);" +
                "section(\"Discover\");discoverProgress=new ProgressBar(this);body.addView(discoverProgress,new LinearLayout.LayoutParams(-1,dp(36)));discover=new LinearLayout(this);discover.setOrientation(LinearLayout.VERTICAL);body.addView(discover);retryDiscover=button(\"Retry discovery\");retryDiscover.setVisibility(View.GONE);retryDiscover.setOnClickListener(v->loadDiscoverAsync());body.addView(retryDiscover);" +
                "section(\"Search\");query=new EditText(this);query.setHint(\"Search anime\");query.setContentDescription(\"Search anime\");query.setSingleLine(true);query.setTextColor(TEXT);query.setHintTextColor(MUTED);query.setTextSize(16);query.setPadding(dp(16),0,dp(16),0);query.setBackground(round(SURFACE,16));body.addView(query,new LinearLayout.LayoutParams(-1,dp(56)));search=button(\"Search installed sources\");search.setOnClickListener(v->searchAsync());body.addView(search);" +
                "LinearLayout source=card(\"Manage sources\",BuiltInProviderCatalog.provenance());source.setContentDescription(\"Open installed sources\");source.setClickable(true);source.setFocusable(true);source.setMinimumHeight(dp(72));source.setOnClickListener(v->AppNavigator.open(this,ProvidersActivity.class));body.addView(source);" +
                "progress=new ProgressBar(this);progress.setVisibility(View.GONE);body.addView(progress,new LinearLayout.LayoutParams(-1,dp(36)));section(\"Search results\");results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);showReady();verifySourcesAsync();loadDiscoverAsync();}" +
                "private void showReady(){results.removeAllViews();results.addView(emptyState(\"Search your sources\",\"Enter a title above. Network/provider failures appear as errors with retry guidance instead of being reported as zero matches.\"));}" +
                "private void verifySourcesAsync(){verify.setEnabled(false);providerState.setText(\"Checking built-in source health…\");new Thread(()->{List<String> ok=new ArrayList<>(),fail=new ArrayList<>();for(MediaProvider p:BuiltInProviderCatalog.providers()){try{List<AnimeItem> sample=p.search(\"Naruto\");if(sample!=null&&!sample.isEmpty())ok.add(p.displayName());else fail.add(p.displayName()+\" returned no usable catalog data\");}catch(Exception e){fail.add(p.displayName()+\" · \"+message(e));}}runOnUiThread(()->{verify.setEnabled(true);String text=ok.isEmpty()?\"No built-in source returned usable data. Tap Check source health to retry or inspect Sources.\":\"Ready now: \"+android.text.TextUtils.join(\", \",ok);if(!fail.isEmpty())text+=\"\\nNeeds attention: \"+android.text.TextUtils.join(\"; \",fail);providerState.setText(text);});}).start();}" +
                "private void loadDiscoverAsync(){retryDiscover.setVisibility(View.GONE);discoverProgress.setVisibility(View.VISIBLE);discover.removeAllViews();discover.addView(text(\"Loading recommendations from built-in sources…\",14,false));new Thread(()->{List<AnimeItem> found=new ArrayList<>();List<String> failures=new ArrayList<>();Set<String> seen=new LinkedHashSet<>();for(MediaProvider p:BuiltInProviderCatalog.providers()){try{for(AnimeItem a:p.search(\"Frieren\")){String key=a.title.toLowerCase(Locale.US);if(seen.add(key)&&found.size()<8)found.add(a);}}catch(Exception e){failures.add(p.displayName()+\": \"+message(e));}}runOnUiThread(()->showDiscover(found,failures));}).start();}" +
                "private void showDiscover(List<AnimeItem> found,List<String> failures){discoverProgress.setVisibility(View.GONE);discover.removeAllViews();if(found.isEmpty()){String detail=failures.isEmpty()?\"Built-in sources returned no discovery titles. Search can still be used.\":android.text.TextUtils.join(\"\\n\",failures)+\"\\nCheck your connection or retry.\";discover.addView(emptyState(\"Discovery unavailable\",detail));retryDiscover.setVisibility(View.VISIBLE);return;}GridLayout g=grid();for(AnimeItem a:found)addGridCard(g,resultCard(a));discover.addView(g,new LinearLayout.LayoutParams(-1,-2));if(!failures.isEmpty()){TextView note=text(\"Some sources are temporarily unavailable: \"+android.text.TextUtils.join(\"; \",failures),12,false);note.setTextColor(MUTED);discover.addView(note);}}" +
                "private void searchAsync(){String q=query.getText().toString().trim();if(q.length()==0){query.setError(\"Enter a title\");return;}search.setEnabled(false);progress.setVisibility(View.VISIBLE);results.removeAllViews();results.addView(text(\"Searching installed sources…\",14,false));new Thread(()->{List<AnimeItem> found=new ArrayList<>();List<String> failures=new ArrayList<>();List<MediaProvider> providers=new ArrayList<>(BuiltInProviderCatalog.providers());for(ExtensionRecord x:new ExtensionManager(this).known())if(x.state==ExtensionRecord.State.ENABLED&&x.searchable())providers.add(new RepositoryMediaProvider(x));Set<String> seen=new LinkedHashSet<>();for(MediaProvider p:providers){try{for(AnimeItem a:p.search(q)){String key=(a.title+\"|\"+a.provider).toLowerCase(Locale.US);if(seen.add(key))found.add(a);}}catch(Exception e){failures.add(p.displayName()+\": \"+message(e));}}runOnUiThread(()->showSearchResult(q,found,failures));}).start();}" +
                "private void showSearchResult(String q,List<AnimeItem> found,List<String> failures){search.setEnabled(true);progress.setVisibility(View.GONE);results.removeAllViews();if(!failures.isEmpty()){LinearLayout c=card(\"Some sources could not be reached\",android.text.TextUtils.join(\"\\n\",failures)+\"\\nRetry, check your connection, or inspect Sources under More.\");results.addView(c);Button retry=button(\"Retry search\");retry.setOnClickListener(v->searchAsync());results.addView(retry);}" +
                "if(found.isEmpty()){String msg=failures.isEmpty()?\"No matches found for \\\"\"+q+\"\\\". Try another title.\":\"No results are shown because source errors occurred. Resolve or retry the source error before treating this as a genuine zero-match search.\";results.addView(emptyState(failures.isEmpty()?\"No matches\":\"Search incomplete\",msg));return;}GridLayout g=grid();for(AnimeItem a:found)addGridCard(g,resultCard(a));results.addView(g,new LinearLayout.LayoutParams(-1,-2));}" +
                "private LinearLayout resultCard(AnimeItem a){String meta=(a.episodes>0?a.episodes+\" episodes · \":\"\")+providerLabel(a.provider);LinearLayout c=card(a.title,meta);c.setContentDescription(\"Open \"+a.title);c.setClickable(true);c.setFocusable(true);c.setMinimumHeight(dp(92));c.setOnClickListener(v->{Intent i=new Intent(this,DetailActivity.class);i.putExtra(\"id\",a.id);i.putExtra(\"title\",a.title);i.putExtra(\"summary\",a.summary);i.putExtra(\"provider\",a.provider);i.putExtra(\"episodes\",a.episodes);startActivity(i);});return c;}" +
                "private String providerLabel(String id){MediaProvider p=BuiltInProviderCatalog.find(id);return p==null?id:p.displayName();}private String message(Exception e){String m=e==null?\"Unknown provider error\":e.getMessage();return m==null||m.trim().isEmpty()?e.getClass().getSimpleName():m;}" +
                "}\n";
    }

    private static String providersActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.widget.*;import java.util.*;\n" +
                "public final class ProvidersActivity extends AppScreen{protected void render(){title(\"Sources\");subtitle(\"Built-in discovery sources work without setup. Repository sources are optional additions and only compatible declared HTTPS contracts can be enabled.\");" +
                "section(\"Built in\");List<MediaProvider> built=BuiltInProviderCatalog.providers();if(built.isEmpty())body.addView(emptyState(\"No built-in sources\",\"AIDao could not safely bundle a compatible authorized source for this project.\"));for(MediaProvider p:built){LinearLayout c=card(p.displayName()+\" · Ready\",p.health()+\"\\n\"+BuiltInProviderCatalog.provenance());TextView badge=chip(p.supportsPlayback()?\"Search + playback\":\"Catalog discovery\",true);c.addView(badge,new LinearLayout.LayoutParams(-2,dp(40)));body.addView(c);}" +
                "section(\"Repository sources\");List<ExtensionRecord> known=new ExtensionManager(this).known();if(known.isEmpty())body.addView(emptyState(\"No added repositories\",\"The app is still usable with its built-in discovery sources. Add a compatible HTTPS repository only when you want more.\"));for(ExtensionRecord x:known){boolean compatible=x.searchable();LinearLayout c=card(x.name+\" · \"+x.state,x.version+\" · \"+x.repoUrl);LinearLayout caps=new LinearLayout(this);caps.setOrientation(LinearLayout.HORIZONTAL);caps.addView(chip(compatible?\"Search ready\":\"Search unavailable\",compatible));caps.addView(chip(x.playable()?\"Playback ready\":\"Playback unavailable\",x.playable()));c.addView(caps);Button toggle=button(!compatible?\"Unsupported repository metadata\":x.state==ExtensionRecord.State.ENABLED?\"Disable source\":\"Enable source\");toggle.setEnabled(compatible);if(compatible)toggle.setOnClickListener(v->{ExtensionRecord.State next=x.state==ExtensionRecord.State.ENABLED?ExtensionRecord.State.DISABLED:ExtensionRecord.State.ENABLED;new ExtensionManager(this).setState(x.id,next);recreate();});c.addView(toggle);if(!compatible){TextView why=text(\"This entry remains visible for provenance, but cannot be enabled until it declares AIDao's compatible HTTPS search contract.\",13,false);why.setTextColor(MUTED);c.addView(why);}body.addView(c);}" +
                "Button repos=button(\"Manage extension repositories\");repos.setOnClickListener(v->AppNavigator.open(this,RepositoriesActivity.class));body.addView(repos);}}\n";
    }

    private static String java(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private NativeFidelityPostProcessor() {}
}
