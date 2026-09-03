package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Final semantic pass for locally implementable non-media product behavior. */
final class SemanticProductPostProcessor {
    static final class Result {
        final String projectName;
        final String packageName;
        final List<GeneratedProject.FileEntry> files;
        final List<String> notes;
        Result(String projectName,String packageName,List<GeneratedProject.FileEntry> files,List<String> notes){
            this.projectName=projectName;this.packageName=packageName;this.files=files;this.notes=notes;
        }
    }

    static Result process(String projectName,String packageName,List<GeneratedProject.FileEntry> incoming){
        List<GeneratedProject.FileEntry> source=incoming==null?new ArrayList<>():new ArrayList<>(incoming);
        if(hasSuffix(source,"/MediaProvider.java")||hasSuffix(source,"/AnimeItem.java"))return new Result(projectName,packageName,source,new ArrayList<>());
        String request=requestText(source).toLowerCase(Locale.US);
        // Classify from the user's identity + request contract only. Generated source and
        // README boilerplate contain generic safety/navigation vocabulary (for example
        // "spending") that must never override the user's actual product intent.
        String semantic=((projectName==null?"":projectName)+"\n"+request).toLowerCase(Locale.US);
        boolean finance=any(semantic,"expense","budget","transaction","spending","finance","ledger");
        boolean habit=any(semantic,"habit","routine","streak","daily tracker","check in","check-in","check them off","completion percentage");
        List<String> notes=new ArrayList<>();
        if(finance){source=financeProduct(packageName,source);notes.add("PASS finance requests generate restart-safe transaction entry, budget mutation, and computed reports");}
        else if(habit){source=habitProduct(packageName,source);notes.add("PASS habit requests generate restart-safe habit creation, check-ins, streak/progress summaries, and clear controls");}
        String post=allText(source).toLowerCase(Locale.US);
        requireCapability(notes,request,post,new String[]{"camera","scan","photo"},new String[]{"camera","requestpermissions","activityresultcontracts","mediastore"},"camera/media capture");
        requireCapability(notes,request,post,new String[]{"notification","reminder","notify"},new String[]{"notificationmanager","notificationchannel","post_notifications"},"Android notifications");
        requireCapability(notes,request,post,new String[]{"location","gps","geofence","map"},new String[]{"access_fine_location","locationmanager","fusedlocation","geofenc"},"location");
        requireCapability(notes,request,post,new String[]{"bluetooth","ble device","ble sensor","ble tracker","wearable"},new String[]{"bluetooth_connect","bluetooth_scan","bluetoothadapter"},"Bluetooth/Nearby Devices");
        requireCapability(notes,request,post,new String[]{"background sync","periodic sync","scheduled sync","workmanager","works in the background"},new String[]{"workmanager","periodicworkrequest","jobservice"},"scheduled/background work");
        requireCapability(notes,request,post,new String[]{"login","sign in","oauth"},new String[]{"auth","oauth","credentialmanager","device flow"},"authentication");
        requireCapability(notes,request,post,new String[]{"api","server","backend","cloud sync","remote data","uploads"},new String[]{"httpurlconnection","httpsurlconnection","okhttp","retrofit","network gateway","repository_dispatch"},"network/backend data");
        if(any(request,"offline","persist","restart","save locally","local storage")){
            if(post.contains("sharedpreferences")||post.contains("localstore"))notes.add("PASS offline/restart persistence contract is backed by generated local storage");
            else notes.add("FAIL requested offline/restart persistence has no durable generated storage implementation");
        }
        if(containsPlaceholder(post))notes.add("FAIL generated source still contains placeholder/sample-only completion language");
        return new Result(projectName,packageName,source,notes);
    }

    private static List<GeneratedProject.FileEntry> financeProduct(String pkg,List<GeneratedProject.FileEntry> source){
        String root="app/src/main/java/"+pkg.replace('.','/')+"/";List<GeneratedProject.FileEntry> out=retainExcept(source,root,"MainActivity.java","TransactionsActivity.java","BudgetsActivity.java","ReportsActivity.java");
        out.add(file(root+"MainActivity.java",financeHome(pkg),"Render live local finance dashboard"));
        out.add(file(root+"TransactionsActivity.java",financeTransactions(pkg),"Create and persist transactions"));
        out.add(file(root+"BudgetsActivity.java",financeBudgets(pkg),"Create and persist monthly budget"));
        out.add(file(root+"ReportsActivity.java",financeReports(pkg),"Compute totals from persisted transactions"));return out;
    }
    private static List<GeneratedProject.FileEntry> habitProduct(String pkg,List<GeneratedProject.FileEntry> source){
        String root="app/src/main/java/"+pkg.replace('.','/')+"/";List<GeneratedProject.FileEntry> out=retainExcept(source,root,"MainActivity.java","TimelineActivity.java","ReportsActivity.java","DataControlsActivity.java");
        out.add(file(root+"MainActivity.java",habitHome(pkg),"Render persisted habits and completion state"));
        out.add(file(root+"TimelineActivity.java",habitEditor(pkg),"Create habits and record check-ins"));
        out.add(file(root+"ReportsActivity.java",habitReports(pkg),"Compute habit completion summary"));
        out.add(file(root+"DataControlsActivity.java",habitControls(pkg),"Provide explicit local habit reset controls"));return out;
    }
    private static List<GeneratedProject.FileEntry> retainExcept(List<GeneratedProject.FileEntry> source,String root,String... names){List<GeneratedProject.FileEntry> out=new ArrayList<>();for(GeneratedProject.FileEntry f:source){if(f==null)continue;boolean drop=false;for(String n:names)if((root+n).equals(f.path)){drop=true;break;}if(!drop)out.add(f);}return out;}

    private static String financeHome(String p){return "package "+p+";\npublic final class MainActivity extends AppScreen{protected void render(){title(\"Finance dashboard\");String raw=store.text(\"transactions\",\"\");double total=0;int count=0;for(String row:raw.split(\"\\n\")){if(row.trim().isEmpty())continue;String[] x=row.split(\"\\\\|\",3);if(x.length>1)try{total+=Double.parseDouble(x[1]);count++;}catch(Exception ignored){}}body.addView(card(\"This month\",\"Transactions: \"+count+\" · Spending: $\"+String.format(java.util.Locale.US,\"%.2f\",total)+\" · Budget: $\"+store.text(\"monthly_budget\",\"0\")));Button add=button(\"Add transaction\");add.setOnClickListener(v->AppNavigator.open(this,TransactionsActivity.class));body.addView(add);Button b=button(\"Set budget\");b.setOnClickListener(v->AppNavigator.open(this,BudgetsActivity.class));body.addView(b);Button r=button(\"Reports\");r.setOnClickListener(v->AppNavigator.open(this,ReportsActivity.class));body.addView(r);}}\n";}
    private static String financeTransactions(String p){return "package "+p+";\nimport android.text.InputType;import android.widget.*;public final class TransactionsActivity extends AppScreen{protected void render(){title(\"Transactions\");EditText category=field(\"Category\");EditText amount=field(\"Amount\");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);body.addView(category);body.addView(amount);Button save=button(\"Save transaction\");save.setOnClickListener(v->{String c=category.getText().toString().trim();String a=amount.getText().toString().trim();if(c.isEmpty()){category.setError(\"Enter a category\");return;}try{double value=Double.parseDouble(a);if(value<0)throw new Exception();String old=store.text(\"transactions\",\"\");store.putText(\"transactions\",old+c.replace(\"|\",\" \")+\"|\"+value+\"|\"+System.currentTimeMillis()+\"\\n\");Toast.makeText(this,\"Transaction saved\",Toast.LENGTH_SHORT).show();category.setText(\"\");amount.setText(\"\");}catch(Exception e){amount.setError(\"Enter a valid non-negative amount\");}});body.addView(save);}}\n";}
    private static String financeBudgets(String p){return "package "+p+";\nimport android.text.InputType;import android.widget.*;public final class BudgetsActivity extends AppScreen{protected void render(){title(\"Monthly budget\");EditText amount=field(\"Budget amount\");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amount.setText(store.text(\"monthly_budget\",\"\"));body.addView(amount);Button save=button(\"Save budget\");save.setOnClickListener(v->{try{double x=Double.parseDouble(amount.getText().toString());if(x<0)throw new Exception();store.putText(\"monthly_budget\",String.format(java.util.Locale.US,\"%.2f\",x));Toast.makeText(this,\"Budget saved\",Toast.LENGTH_SHORT).show();}catch(Exception e){amount.setError(\"Enter a valid budget\");}});body.addView(save);}}\n";}
    private static String financeReports(String p){return "package "+p+";\npublic final class ReportsActivity extends AppScreen{protected void render(){title(\"Reports\");String raw=store.text(\"transactions\",\"\");double total=0;int count=0;java.util.LinkedHashMap<String,Double> by=new java.util.LinkedHashMap<>();for(String row:raw.split(\"\\n\")){String[] x=row.split(\"\\\\|\",3);if(x.length<2)continue;try{double v=Double.parseDouble(x[1]);total+=v;count++;by.put(x[0],by.containsKey(x[0])?by.get(x[0])+v:v);}catch(Exception ignored){}}body.addView(card(\"Summary\",\"Transactions: \"+count+\" · Total: $\"+String.format(java.util.Locale.US,\"%.2f\",total)));section(\"By category\");for(java.util.Map.Entry<String,Double> e:by.entrySet())body.addView(card(e.getKey(),\"$\"+String.format(java.util.Locale.US,\"%.2f\",e.getValue())));}}\n";}
    private static String habitHome(String p){return "package "+p+";\npublic final class MainActivity extends AppScreen{protected void render(){title(\"Habits\");String habits=store.text(\"habits\",\"\");String done=store.text(\"habit_done_today\",\"\");if(habits.trim().isEmpty())body.addView(card(\"No habits yet\",\"Create a habit to begin tracking.\"));else for(String h:habits.split(\"\\n\")){if(h.trim().isEmpty())continue;boolean checked=(\"\\n\"+done+\"\\n\").contains(\"\\n\"+h+\"\\n\");body.addView(card(h,checked?\"Completed today\":\"Not completed today\"));}Button log=button(\"Manage & check in\");log.setOnClickListener(v->AppNavigator.open(this,TimelineActivity.class));body.addView(log);Button report=button(\"Progress\");report.setOnClickListener(v->AppNavigator.open(this,ReportsActivity.class));body.addView(report);}}\n";}
    private static String habitEditor(String p){return "package "+p+";\nimport android.widget.*;public final class TimelineActivity extends AppScreen{protected void render(){title(\"Habit check-in\");EditText name=field(\"New habit\");body.addView(name);Button add=button(\"Add habit\");add.setOnClickListener(v->{String h=name.getText().toString().trim().replace(\"\\n\",\" \" );if(h.isEmpty()){name.setError(\"Enter a habit\");return;}String old=store.text(\"habits\",\"\");if(!(\"\\n\"+old+\"\\n\").contains(\"\\n\"+h+\"\\n\"))store.putText(\"habits\",old+h+\"\\n\");recreate();});body.addView(add);String habits=store.text(\"habits\",\"\");String done=store.text(\"habit_done_today\",\"\");for(String h:habits.split(\"\\n\")){if(h.trim().isEmpty())continue;boolean checked=(\"\\n\"+done+\"\\n\").contains(\"\\n\"+h+\"\\n\");LinearLayout c=card(h,checked?\"Completed\":\"Pending\");Button toggle=button(checked?\"Undo today\":\"Complete today\");final String habit=h;toggle.setOnClickListener(v->{String d=store.text(\"habit_done_today\",\"\");if((\"\\n\"+d+\"\\n\").contains(\"\\n\"+habit+\"\\n\"))d=d.replace(habit+\"\\n\",\"\");else{d+=habit+\"\\n\";store.number(\"habit_checkins\",store.number(\"habit_checkins\")+1);}store.putText(\"habit_done_today\",d);recreate();});c.addView(toggle);body.addView(c);}}}\n";}
    private static String habitReports(String p){return "package "+p+";\npublic final class ReportsActivity extends AppScreen{protected void render(){title(\"Habit progress\");String habits=store.text(\"habits\",\"\");String done=store.text(\"habit_done_today\",\"\");int total=0,complete=0;for(String h:habits.split(\"\\n\")){if(h.trim().isEmpty())continue;total++;if((\"\\n\"+done+\"\\n\").contains(\"\\n\"+h+\"\\n\"))complete++;}int pct=total==0?0:(complete*100/total);body.addView(card(\"Today\",complete+\" / \"+total+\" complete · \"+pct+\"%\"));body.addView(card(\"All-time check-ins\",String.valueOf(store.number(\"habit_checkins\"))));}}\n";}
    private static String habitControls(String p){return "package "+p+";\npublic final class DataControlsActivity extends AppScreen{protected void render(){title(\"Data controls\");body.addView(card(\"Local ownership\",\"Habits and check-ins are stored on this device.\"));Button clear=button(\"Clear habit data\");clear.setOnClickListener(v->{store.putText(\"habits\",\"\");store.putText(\"habit_done_today\",\"\");store.number(\"habit_checkins\",0);clear.setText(\"Habit data cleared\");});body.addView(clear);}}\n";}

    private static void requireCapability(List<String> notes,String request,String post,String[] triggers,String[] markers,String label){if(!any(request,triggers))return;if(any(post,markers))notes.add("PASS requested "+label+" is represented by generated executable source");else notes.add("FAIL requested "+label+" has no generated executable implementation");}
    private static boolean containsPlaceholder(String s){return s.contains("todo: implement")||s.contains("coming soon")||s.contains("placeholder data")||s.contains("sample only");}
    private static boolean hasSuffix(List<GeneratedProject.FileEntry> files,String suffix){for(GeneratedProject.FileEntry f:files)if(f!=null&&f.path!=null&&f.path.endsWith(suffix))return true;return false;}
    private static boolean any(String source,String... terms){if(source==null)return false;for(String term:terms)if(source.contains(term))return true;return false;}
    private static String requestText(List<GeneratedProject.FileEntry> files){
        for(GeneratedProject.FileEntry f:files){
            if(f==null||!"README.md".equals(f.path)||f.content==null)continue;
            String readme=f.content;
            int architecture=readme.indexOf("\n## Generated architecture");
            StringBuilder user=new StringBuilder(architecture>=0?readme.substring(0,architecture):readme);
            int requirements=readme.indexOf("\n## Requirements");
            if(requirements>=0){
                int start=requirements+"\n## Requirements".length();
                int end=readme.indexOf("\n## Implementation tasks",start);
                if(end<0)end=readme.length();
                user.append('\n').append(readme, start, end);
            }
            return user.toString();
        }
        return allText(files);
    }
    private static String allText(List<GeneratedProject.FileEntry> files){StringBuilder b=new StringBuilder();if(files!=null)for(GeneratedProject.FileEntry f:files)if(f!=null&&f.content!=null)b.append('\n').append(f.content);return b.toString();}
    private static GeneratedProject.FileEntry file(String path,String content,String hint){return new GeneratedProject.FileEntry(path,content,hint);}
    private SemanticProductPostProcessor(){}
}