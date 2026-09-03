package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;

/**
 * Final generated-media UX pass. It replaces the generic vertical scaffold with
 * a phone-native shell, fixed bottom navigation, inset-aware chrome, card-based
 * browse results, asynchronous provider search, and explicit provider diagnostics.
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
        add(out, javaPath, "AppScreen.java", appScreen(packageName, projectName), "Use inset-aware phone-native app chrome and fixed bottom navigation");
        add(out, javaPath, "MainActivity.java", mainActivity(packageName), "Use asynchronous provider search, provider diagnostics, source rail, and card browse results");
        add(out, javaPath, "ProvidersActivity.java", providersActivity(packageName), "Present built-in and repository providers as mobile extension cards");

        List<String> notes = new ArrayList<>();
        notes.add("PASS native-fidelity pass uses a fixed bottom navigation bar outside scroll content");
        notes.add("PASS status/navigation/display-cutout insets are applied to generated phone chrome");
        notes.add("PASS top app bar, typography hierarchy, responsive padding, card spacing, and >=48dp touch targets are phone-native");
        notes.add("PASS Browse uses a horizontal installed-source rail plus card results instead of a desktop-style vertical form");
        notes.add("PASS Browse uses asynchronous provider requests so network work never blocks the Android UI thread");
        notes.add("PASS provider failures are shown separately from genuine zero-result searches");
        notes.add("PASS generated app exposes an on-device built-in source diagnostic that requires a provider to return usable catalog data");
        notes.add("PASS generated extension inventory uses card hierarchy instead of a desktop-style vertical control form");
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
                "import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.os.*;import android.view.*;import android.widget.*;\n" +
                "public abstract class AppScreen extends Activity{" +
                "protected LinearLayout body;protected LocalStore store;private TextView topTitle;private LinearLayout bottomNav;" +
                "protected final int BG=Color.rgb(15,17,22),SURFACE=Color.rgb(27,30,38),SURFACE2=Color.rgb(36,40,50),TEXT=Color.rgb(238,240,246),MUTED=Color.rgb(164,171,187),ACCENT=Color.rgb(111,125,255);" +
                "@Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);store=new LocalStore(this);" +
                "LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);" +
                "LinearLayout appBar=new LinearLayout(this);appBar.setOrientation(LinearLayout.VERTICAL);appBar.setBackgroundColor(BG);appBar.setPadding(dp(20),dp(12),dp(20),dp(12));" +
                "TextView brand=text(\"" + java(appName) + "\",12,false);brand.setTextColor(MUTED);brand.setPadding(0,0,0,dp(2));appBar.addView(brand);topTitle=text(\"Browse\",26,true);appBar.addView(topTitle);root.addView(appBar,new LinearLayout.LayoutParams(-1,-2));" +
                "ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(8),dp(16),dp(28));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));" +
                "bottomNav=new LinearLayout(this);bottomNav.setOrientation(LinearLayout.HORIZONTAL);bottomNav.setBackgroundColor(SURFACE);bottomNav.setPadding(dp(4),dp(6),dp(4),dp(6));root.addView(bottomNav,new LinearLayout.LayoutParams(-1,-2));buildBottomNav();setContentView(root);" +
                "if(Build.VERSION.SDK_INT>=21){root.setOnApplyWindowInsetsListener((v,insets)->{int left=insets.getSystemWindowInsetLeft(),top=insets.getSystemWindowInsetTop(),right=insets.getSystemWindowInsetRight(),bottom=insets.getSystemWindowInsetBottom();if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;}appBar.setPadding(left+dp(20),top+dp(10),right+dp(20),dp(12));bottomNav.setPadding(left+dp(4),dp(6),right+dp(4),bottom+dp(6));return insets;});root.requestApplyInsets();}" +
                "render();}" +
                "protected abstract void render();" +
                "protected void title(String s){topTitle.setText(s==null?\"\":s);}" +
                "protected void subtitle(String s){TextView t=text(s,14,false);t.setTextColor(MUTED);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(14));body.addView(t,p);}" +
                "protected void section(String s){TextView t=text(s,18,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(12),0,dp(8));body.addView(t,p);}" +
                "protected TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextColor(TEXT);t.setTextSize(size);t.setTypeface(android.graphics.Typeface.create(\"sans-serif\",bold?1:0));t.setGravity(Gravity.CENTER_VERTICAL);return t;}" +
                "protected Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(TEXT);b.setMinHeight(dp(48));b.setContentDescription(label);b.setBackground(round(SURFACE2,16));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(6),0,dp(8));b.setLayoutParams(p);return b;}" +
                "protected LinearLayout card(String heading,String supporting){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(round(SURFACE,18));TextView h=text(heading,16,true);c.addView(h);if(supporting!=null&&supporting.length()>0){TextView s=text(supporting,13,false);s.setTextColor(MUTED);s.setPadding(0,dp(4),0,0);c.addView(s);}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));c.setLayoutParams(p);return c;}" +
                "protected GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}" +
                "protected void nav(){}" +
                "private void buildBottomNav(){String[] names={\"Browse\",\"Library\",\"History\",\"Downloads\",\"Extensions\"};Class[] screens={MainActivity.class,LibraryActivity.class,HistoryActivity.class,DownloadsActivity.class,ProvidersActivity.class};for(int i=0;i<names.length;i++){final Class target=screens[i];boolean selected=getClass()==target;LinearLayout tab=new LinearLayout(this);tab.setOrientation(LinearLayout.VERTICAL);tab.setGravity(Gravity.CENTER);tab.setMinHeight(dp(56));tab.setPadding(dp(3),dp(4),dp(3),dp(4));tab.setContentDescription((selected?\"Current \" : \"Open \" )+names[i]);TextView item=text(names[i],11,selected);item.setGravity(Gravity.CENTER);item.setTextColor(selected?TEXT:MUTED);tab.addView(item,new LinearLayout.LayoutParams(-1,dp(38)));if(selected){View indicator=new View(this);indicator.setBackground(round(ACCENT,3));tab.addView(indicator,new LinearLayout.LayoutParams(dp(24),dp(3)));}tab.setOnClickListener(v->AppNavigator.open(this,target));bottomNav.addView(tab,new LinearLayout.LayoutParams(0,dp(56),1));}}" +
                "protected int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}" +
                "}\n";
    }

    private static String mainActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.content.*;import android.graphics.*;import android.view.*;import android.widget.*;import java.util.*;\n" +
                "public final class MainActivity extends AppScreen{private LinearLayout results;private EditText query;private Button search,verify;private ProgressBar progress;private TextView providerState;" +
                "protected void render(){title(\"Browse\");subtitle(\"Browse installed sources and search their catalogs. Built-in providers are ready immediately; additional repositories remain optional and user-controlled.\");" +
                "section(\"Installed sources\");HorizontalScrollView sourceRail=new HorizontalScrollView(this);sourceRail.setHorizontalScrollBarEnabled(false);sourceRail.setOverScrollMode(View.OVER_SCROLL_NEVER);LinearLayout sourceChips=new LinearLayout(this);sourceChips.setOrientation(LinearLayout.HORIZONTAL);for(MediaProvider p:BuiltInProviderCatalog.providers()){LinearLayout chip=card(p.displayName(),p.health());chip.setMinimumWidth(dp(180));chip.setContentDescription(p.displayName()+\" installed source\");LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(210),-2);cp.setMargins(0,0,dp(10),dp(2));sourceChips.addView(chip,cp);}sourceRail.addView(sourceChips,new HorizontalScrollView.LayoutParams(-2,-2));body.addView(sourceRail,new LinearLayout.LayoutParams(-1,-2));" +
                "verify=button(\"Verify built-in sources\");verify.setOnClickListener(v->verifySourcesAsync());body.addView(verify);providerState=text(\"Source verification has not run yet. Search also validates providers as you use them.\",13,false);providerState.setTextColor(MUTED);body.addView(providerState);" +
                "section(\"Search\");query=new EditText(this);query.setHint(\"Search anime\");query.setContentDescription(\"Search anime\");query.setSingleLine(true);query.setTextColor(TEXT);query.setHintTextColor(MUTED);query.setTextSize(16);query.setPadding(dp(14),0,dp(14),0);query.setBackground(round(SURFACE,16));body.addView(query,new LinearLayout.LayoutParams(-1,dp(54)));" +
                "search=button(\"Search installed sources\");search.setOnClickListener(v->searchAsync());body.addView(search);" +
                "LinearLayout source=card(\"Manage sources\",BuiltInProviderCatalog.provenance());source.setContentDescription(\"Open installed sources\");source.setClickable(true);source.setFocusable(true);source.setMinHeight(dp(64));source.setOnClickListener(v->AppNavigator.open(this,ProvidersActivity.class));body.addView(source);" +
                "progress=new ProgressBar(this);progress.setVisibility(View.GONE);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(42));pp.setMargins(0,dp(4),0,dp(4));body.addView(progress,pp);" +
                "section(\"Search results\");results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);showReady();}" +
                "private void showReady(){results.removeAllViews();TextView t=text(\"Enter a title to search the providers already installed in this app.\",14,false);t.setTextColor(MUTED);results.addView(t);}" +
                "private void verifySourcesAsync(){verify.setEnabled(false);providerState.setText(\"Verifying installed sources…\");new Thread(()->{List<String> ok=new ArrayList<>(),fail=new ArrayList<>();for(MediaProvider p:BuiltInProviderCatalog.providers()){try{List<AnimeItem> sample=p.search(\"Naruto\");if(sample!=null&&!sample.isEmpty())ok.add(p.displayName()+\" · \"+sample.size()+\" result(s)\");else fail.add(p.displayName()+\" · reachable but returned no usable catalog data\");}catch(Exception e){fail.add(p.displayName()+\" · \"+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}}runOnUiThread(()->{verify.setEnabled(true);String text=ok.isEmpty()?\"No built-in source returned usable data.\":\"Working: \"+android.text.TextUtils.join(\"; \",ok);if(!fail.isEmpty())text+=\"\\nNeeds attention: \"+android.text.TextUtils.join(\"; \",fail);providerState.setText(text);});}).start();}" +
                "private void searchAsync(){String q=query.getText().toString().trim();if(q.length()==0){query.setError(\"Enter a title\");return;}search.setEnabled(false);progress.setVisibility(View.VISIBLE);results.removeAllViews();results.addView(text(\"Searching installed sources…\",14,false));" +
                "new Thread(()->{List<AnimeItem> found=new ArrayList<>();List<String> failures=new ArrayList<>();List<MediaProvider> providers=new ArrayList<>(BuiltInProviderCatalog.providers());for(ExtensionRecord x:new ExtensionManager(this).known())if(x.state==ExtensionRecord.State.ENABLED&&x.searchable())providers.add(new RepositoryMediaProvider(x));Set<String> seen=new LinkedHashSet<>();for(MediaProvider p:providers){try{for(AnimeItem a:p.search(q)){String key=(a.title+\"|\"+a.provider).toLowerCase(Locale.US);if(seen.add(key))found.add(a);}}catch(Exception e){String m=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();failures.add(p.displayName()+\": \"+m);}}runOnUiThread(()->showSearchResult(q,found,failures));}).start();}" +
                "private void showSearchResult(String q,List<AnimeItem> found,List<String> failures){search.setEnabled(true);progress.setVisibility(View.GONE);results.removeAllViews();" +
                "if(!failures.isEmpty()){LinearLayout c=card(\"Some sources could not be reached\",android.text.TextUtils.join(\"\\n\",failures)+\"\\nCheck your connection, retry, or inspect Sources under More.\");results.addView(c);}" +
                "if(found.isEmpty()){String msg=failures.isEmpty()?\"No matches found for \\\"\"+q+\"\\\". Try another title.\":\"No results are shown because one or more sources failed. Resolve the source error above before treating this as a genuine zero-match search.\";TextView empty=text(msg,14,false);empty.setTextColor(MUTED);results.addView(empty);return;}" +
                "for(AnimeItem a:found){String meta=(a.episodes>0?a.episodes+\" episodes · \":\"\")+providerLabel(a.provider);LinearLayout c=card(a.title,meta);c.setContentDescription(\"Open \"+a.title);c.setClickable(true);c.setFocusable(true);c.setMinHeight(dp(72));c.setOnClickListener(v->{Intent i=new Intent(this,DetailActivity.class);i.putExtra(\"id\",a.id);i.putExtra(\"title\",a.title);i.putExtra(\"summary\",a.summary);i.putExtra(\"provider\",a.provider);i.putExtra(\"episodes\",a.episodes);startActivity(i);});results.addView(c);}}" +
                "private String providerLabel(String id){MediaProvider p=BuiltInProviderCatalog.find(id);return p==null?id:p.displayName();}" +
                "}\n";
    }

    private static String providersActivity(String pkg) {
        return "package " + pkg + ";\n" +
                "import android.widget.*;import java.util.*;\n" +
                "public final class ProvidersActivity extends AppScreen{protected void render(){title(\"Extensions\");subtitle(\"Installed sources are available immediately. Add repositories only when you want more compatible sources. Search and playback capability are shown separately.\");" +
                "section(\"Built in\");List<MediaProvider> built=BuiltInProviderCatalog.providers();if(built.isEmpty())body.addView(card(\"No built-in sources\",\"AIDao could not safely bundle a compatible authorized source for this project.\"));for(MediaProvider p:built){body.addView(card(p.displayName()+\" · Enabled\",p.health()+\"\\n\"+BuiltInProviderCatalog.provenance()));}" +
                "section(\"Repository extensions\");List<ExtensionRecord> known=new ExtensionManager(this).known();if(known.isEmpty()){body.addView(card(\"No additional extensions\",\"The app remains usable with its built-in discovery sources. Add a compatible HTTPS repository to discover more providers.\"));}" +
                "for(ExtensionRecord x:known){boolean compatible=x.searchable();LinearLayout c=card(x.name+\" · \"+x.state,x.version+\" · \"+x.repoUrl+\"\\n\"+(compatible?\"Search: compatible\":\"Search: unavailable\")+\" · \"+(x.playable()?\"Playback: compatible\":\"Playback: unavailable\"));Button toggle=button(!compatible?\"Unsupported repository metadata\":x.state==ExtensionRecord.State.ENABLED?\"Disable extension\":\"Enable extension\");toggle.setEnabled(compatible);if(compatible)toggle.setOnClickListener(v->{ExtensionRecord.State next=x.state==ExtensionRecord.State.ENABLED?ExtensionRecord.State.DISABLED:ExtensionRecord.State.ENABLED;new ExtensionManager(this).setState(x.id,next);recreate();});c.addView(toggle);if(!compatible){TextView why=text(\"This entry remains visible for provenance, but it cannot be enabled until it declares AIDao's compatible HTTPS search contract.\",13,false);why.setTextColor(MUTED);c.addView(why);}body.addView(c);}" +
                "Button repos=button(\"Manage repositories\");repos.setOnClickListener(v->AppNavigator.open(this,RepositoriesActivity.class));body.addView(repos);}}\n";
    }

    private static String java(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private NativeFidelityPostProcessor() {}
}
