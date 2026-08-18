package dev.thefoolish.aidao;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * AIDao v1.0.1 GitHub App entrypoint.
 *
 * Normal Android-to-GitHub authorization uses GitHub's Device Flow with only
 * the public GitHub App client ID embedded in the APK. No GitHub App private
 * key, client secret, or long-lived PAT is bundled or persisted.
 */
public class AIDaoActivityV6 extends Activity {
    private static final int BG=Color.rgb(18,19,24), HEADER=Color.rgb(22,23,29), PANEL=Color.rgb(29,31,39), MUTED=Color.rgb(157,162,178), BLUE=Color.rgb(80,148,255), GREEN=Color.rgb(76,201,144), YELLOW=Color.rgb(235,190,80), RED=Color.rgb(234,98,108), PURPLE=Color.rgb(145,93,255);
    private static final Typeface UI=Typeface.create("sans-serif",Typeface.NORMAL), BOLD=Typeface.create("sans-serif",Typeface.BOLD), BRAND=Typeface.create("cursive",Typeface.BOLD);
    private static final String GITHUB_APP_CLIENT_ID="Iv23ltCpkWHmKdijzsLC";
    private static final String DEFAULT_REPO="IcyKokane/AIDaoPublic";

    private SharedPreferences prefs;
    private LinearLayout root,content;
    private volatile boolean busy=false;
    private volatile boolean cancelAuth=false;
    private volatile GitHubDeviceAuthClient.DeviceCode activeCode;
    private String sessionToken="";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("aidao_workspace_v4",MODE_PRIVATE);
        recoverInterruptedBuild();
        Window w=getWindow();
        w.setStatusBarColor(BG);w.setNavigationBarColor(Color.rgb(12,13,17));
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if(android.os.Build.VERSION.SDK_INT>=30)w.setDecorFitsSystemWindows(false);
        render();
    }

    @Override protected void onDestroy(){
        cancelAuth=true;
        sessionToken="";
        super.onDestroy();
    }

    private void recoverInterruptedBuild(){
        String stage=prefs.getString("stage","");
        if("BUILD RUNNING".equals(stage)){
            prefs.edit()
                    .putString("ci_state","Recovery required · the previous GitHub authorization/build session was interrupted. Reconnect GitHub App & Build to safely retry; session credentials were not stored.")
                    .putString("stage","BUILD RECOVERY")
                    .apply();
        }
    }

    private void render(){
        root=col();root.setBackgroundColor(BG);
        LinearLayout header=row();header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(18),dp(28),dp(18),dp(14));header.setBackgroundColor(HEADER);
        TextView logo=text("A",22,Color.WHITE,true);logo.setTypeface(BRAND);logo.setGravity(Gravity.CENTER);logo.setBackground(gradient());header.addView(logo,lp(dp(44),dp(44),0,0,dp(12),0));
        LinearLayout names=col();names.addView(text("AIDao",20,Color.WHITE,true));names.addView(text("GitHub App connection · v1.0.1",12,MUTED,false));header.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView dot=text("●",16,busy?PURPLE:GREEN,true);header.addView(dot);
        root.addView(header);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);content=col();content.setPadding(dp(18),dp(18),dp(18),dp(32));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);applyInsets(header);

        String project=prefs.getString("project_name",null);
        boolean generated=prefs.getBoolean("generated",false);
        String stage=prefs.getString("stage",project==null?"READY":"BRIEF READY");
        card("GitHub App authorization","AIDao now uses GitHub Device Flow for the normal build path. The APK contains only the public Client ID. Your GitHub App private key and client secret are not used by the phone app.",GREEN);
        card("Repository boundary",prefs.getString("repo",DEFAULT_REPO)+" · access remains limited by the GitHub App installation and your GitHub account permissions.",BLUE);
        card("Current project",project==null?"No project yet":project+" · "+stage,project==null?YELLOW:(generated?GREEN:BLUE));

        String ci=prefs.getString("ci_state","No GitHub App build started");
        card("Build status",friendlyStatus(ci),colorFor(ci));
        String artifact=prefs.getString("artifact_name","");
        if(!artifact.isEmpty())card("Latest APK artifact",artifact+" · produced by trusted generated-project CI",GREEN);

        Button workspace=secondary(project==null?"Create Project in AIDao":"Open AIDao Workspace");
        workspace.setOnClickListener(v->startActivity(new Intent(this,AIDaoActivityV5.class)));
        content.addView(workspace,lp(-1,dp(50),0,dp(12),0,dp(8)));

        if(project!=null&&generated){
            Button connect=primary(busy?"GitHub authorization / build in progress…":"Connect GitHub App & Build");
            connect.setEnabled(!busy);
            connect.setOnClickListener(v->beginGitHubAppBuild());
            content.addView(connect,lp(-1,dp(52),0,dp(6),0,dp(8)));
            content.addView(text("You will receive a short GitHub code, approve AIDao in your browser, then AIDao continues automatically. No fine-grained token is required.",12,MUTED,false));
        }else if(project!=null){
            card("Source generation required","Open the workspace, finish planning, and generate/locally verify the Android source before connecting GitHub.",YELLOW);
        }

        String run=prefs.getString("run_url",null);
        if(run!=null&&!run.isEmpty()){
            Button open=secondary("Open Latest CI Run / APK Artifact");
            open.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(run))));
            content.addView(open,lp(-1,dp(50),0,dp(12),0,0));
        }
    }

    private void beginGitHubAppBuild(){
        if(busy)return;
        final String repo=prefs.getString("repo",DEFAULT_REPO);
        if(!repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")){
            showError("Repository needs attention","The configured repository must use owner/name format.");return;
        }
        busy=true;cancelAuth=false;sessionToken="";
        setCi("Waiting for GitHub authorization");
        render();
        new Thread(()->{
            try{
                GitHubDeviceAuthClient auth=new GitHubDeviceAuthClient();
                activeCode=auth.begin(GITHUB_APP_CLIENT_ID);
                runOnUiThread(()->showDeviceCode(activeCode));
                int interval=activeCode.intervalSeconds;
                while(!cancelAuth&&!activeCode.expired()){
                    Thread.sleep(interval*1000L);
                    GitHubDeviceAuthClient.TokenResult result=auth.pollOnce(GITHUB_APP_CLIENT_ID,activeCode,interval);
                    interval=result.nextIntervalSeconds;
                    if(result.authorized()){
                        sessionToken=result.accessToken;
                        setCi("GitHub authorized · starting repository preflight");
                        runRemoteBuild(repo,sessionToken);
                        return;
                    }
                    if(result.state==GitHubDeviceAuthClient.TokenResult.State.DENIED)throw new IllegalStateException("AUTH_DENIED");
                    if(result.state==GitHubDeviceAuthClient.TokenResult.State.EXPIRED)throw new IllegalStateException("AUTH_EXPIRED");
                    setCi(result.state==GitHubDeviceAuthClient.TokenResult.State.SLOW_DOWN?"GitHub authorization pending · slowing checks":"Waiting for GitHub authorization");
                }
                if(cancelAuth)throw new IllegalStateException("AUTH_CANCELLED");
                throw new IllegalStateException("AUTH_EXPIRED");
            }catch(Exception e){
                setBlocked(friendlyError(e));
            }finally{
                sessionToken="";activeCode=null;busy=false;
                runOnUiThread(this::render);
            }
        },"AIDao-GitHub-App-Auth").start();
    }

    private void showDeviceCode(GitHubDeviceAuthClient.DeviceCode code){
        if(isFinishing()||code==null)return;
        ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        if(cm!=null)cm.setPrimaryClip(ClipData.newPlainText("GitHub device code",code.userCode));
        new AlertDialog.Builder(this)
            .setTitle("Approve AIDao on GitHub")
            .setMessage("GitHub code:  "+code.userCode+"\n\nThe code has been copied. Open GitHub, paste the code, and approve AIDao-TheFoolish. AIDao will continue automatically after approval.")
            .setPositiveButton("Open GitHub",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(code.verificationUri))))
            .setNegativeButton("Cancel",(d,w)->{cancelAuth=true;setCi("GitHub authorization cancelled");})
            .setCancelable(false)
            .show();
    }

    private void runRemoteBuild(String repo,String token)throws Exception{
        prefs.edit().putString("stage","BUILD RUNNING").apply();
        GitHubGeneratedBuildClient client=new GitHubGeneratedBuildClient();
        GeneratedProject generated=regenerateWithOverrides();
        GitHubGeneratedBuildClient.BuildReceipt receipt=client.sendBuildAndWait(repo,token,generated,(stage,detail)->setCi(stage+" · "+detail));
        if(!receipt.success()){
            setCi("CI failed · bounded repair starting");
            GeneratedProjectRepairer.RepairResult repair=new GeneratedProjectRepairer().repair(generated,receipt.failureSummary);
            prefs.edit().putString("repair",repair.action).apply();
            GitHubGeneratedBuildClient.BuildReceipt retry=client.sendBuildAndWait(repo,token,repair.project,(stage,detail)->setCi("Repair rebuild · "+stage+" · "+detail));
            finishReceipt(retry);
        }else finishReceipt(receipt);
    }

    private void finishReceipt(GitHubGeneratedBuildClient.BuildReceipt r){
        if(r.success()){
            prefs.edit().putString("ci_state","Successful · APK artifact: "+r.artifactName).putString("artifact_name",r.artifactName).putString("run_url",r.runUrl==null?"":r.runUrl).putString("stage","APK READY").putString("branch",r.branch).remove("repair").apply();
            List<String> tasks=decode(prefs.getString("tasks",""));SharedPreferences.Editor e=prefs.edit();for(int i=0;i<tasks.size();i++)e.putString("task_"+i,TaskExecutionState.COMPLETE.name());e.apply();
        }else{
            String summary=friendlyFailure(r.failureSummary);
            prefs.edit().putString("ci_state","Build blocked after bounded repair · "+summary).putString("run_url",r.runUrl==null?"":r.runUrl).putString("stage","BUILD BLOCKED").apply();
        }
    }

    private GeneratedProject regenerateWithOverrides(){
        String project=prefs.getString("project_name","Project");
        GeneratedProject base=new LocalSourceGenerator().generate(project,prefs.getString("brief",""),decode(prefs.getString("requirements","")),decode(prefs.getString("tasks","")));
        java.util.Map<String,String> overrides=new java.util.HashMap<>(),bases=new java.util.HashMap<>();
        for(String k:new java.util.HashSet<>(prefs.getAll().keySet())){
            if(k.startsWith("override::")){String v=prefs.getString(k,null);if(v!=null)overrides.put(k.substring("override::".length()),v);}
            else if(k.startsWith("override-base::")){String v=prefs.getString(k,null);if(v!=null)bases.put(k.substring("override-base::".length()),v);}
        }
        GeneratedProjectOverrideResolver.Resolution resolution=new GeneratedProjectOverrideResolver().resolve(base,overrides,bases);
        if(!resolution.canBuild())throw new IllegalStateException("Manual source edits conflict with regenerated source. Open Files and reset or reapply the affected edits before building.");
        return resolution.project;
    }

    private void setCi(String state){prefs.edit().putString("ci_state",state).apply();runOnUiThread(()->{if(!isFinishing())render();});}
    private void setBlocked(String message){prefs.edit().putString("ci_state","Blocked · "+message).putString("stage","BUILD BLOCKED").apply();}

    private String friendlyError(Throwable error){
        String raw=error==null||error.getMessage()==null?"":error.getMessage();
        String low=raw.toLowerCase(Locale.US);
        if(raw.contains("AUTH_DENIED"))return "GitHub authorization was denied. No repository changes were made.";
        if(raw.contains("AUTH_EXPIRED"))return "The GitHub approval code expired. Start Connect GitHub App & Build again for a fresh code.";
        if(raw.contains("AUTH_CANCELLED"))return "GitHub authorization was cancelled. No repository changes were made.";
        if(low.contains("client id")||low.contains("incorrect_client_credentials"))return "AIDao could not start GitHub authorization. The public GitHub App Client ID needs to be checked.";
        if(low.contains("401"))return "GitHub rejected the authorization session. Reconnect AIDao to GitHub and try again.";
        if(low.contains("403")||low.contains("resource not accessible"))return "AIDao is authorized, but the installed GitHub App does not have the required repository permission. AIDao-TheFoolish needs Contents read/write and Actions read access to the selected repository.";
        if(low.contains("404"))return "GitHub could not see the selected repository or trusted generated-project workflow. Confirm AIDao-TheFoolish is installed on AIDaoPublic and Actions are enabled.";
        if(low.contains("workflow"))return "The trusted GitHub Actions workflow could not be used. Open the latest CI run for details; AIDao did not write workflow files from the phone.";
        return raw.isEmpty()?"GitHub connection failed. Try again after checking network access.":friendlyFailure(raw);
    }

    private String friendlyFailure(String raw){
        if(raw==null||raw.trim().isEmpty())return "The GitHub build failed without a detailed summary.";
        String low=raw.toLowerCase(Locale.US);
        if(low.contains("permission 403")||low.contains("resource not accessible"))return "GitHub App repository permissions are insufficient for this operation.";
        if(low.contains("auth 401"))return "The GitHub authorization session is no longer valid; reconnect and retry.";
        if(low.contains("repository 404"))return "The selected repository is not available to the GitHub App installation.";
        if(low.contains("workflow 404"))return "The trusted generated-project workflow is missing or not visible to the GitHub App.";
        if(low.contains("build error"))return raw.substring(0,Math.min(raw.length(),420));
        return raw.substring(0,Math.min(raw.length(),320));
    }

    private String friendlyStatus(String raw){
        if(raw==null||raw.isEmpty())return "No build started";
        return friendlyFailure(raw);
    }

    private List<String> decode(String s){if(s==null||s.isEmpty())return new ArrayList<>();return new ArrayList<>(Arrays.asList(s.split("\u001f",-1)));}
    private void showError(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK",null).show();}

    private void applyInsets(LinearLayout header){if(android.os.Build.VERSION.SDK_INT<23)return;root.setOnApplyWindowInsetsListener((v,i)->{int top=i.getSystemWindowInsetTop();if(android.os.Build.VERSION.SDK_INT>=30)top=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()).top;header.setPadding(dp(18),top+dp(22),dp(18),dp(14));return i;});root.requestApplyInsets();}
    private View card(String title,String detail,int color){LinearLayout p=col();p.setPadding(dp(16),dp(15),dp(16),dp(15));p.setBackground(round(PANEL,14));LinearLayout r=row();TextView dot=text("◆",14,color,true);r.addView(dot,lp(dp(34),-2,0,0,dp(8),0));LinearLayout l=col();l.addView(text(title,14,Color.WHITE,true));l.addView(text(detail,12,MUTED,false));r.addView(l,new LinearLayout.LayoutParams(0,-2,1));p.addView(r);content.addView(p,lp(-1,-2,0,0,0,dp(8)));return p;}
    private Button primary(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setTypeface(BOLD);b.setBackground(round(Color.rgb(82,89,245),14));return b;}
    private Button secondary(String s){Button b=primary(s);b.setBackground(round(PANEL,14));return b;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(bold?BOLD:UI);t.setLineSpacing(0,1.08f);return t;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    private GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable gradient(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(73,139,255),Color.rgb(139,79,255)});g.setCornerRadius(dp(14));return g;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private int colorFor(String s){String v=s==null?"":s.toLowerCase(Locale.US);if(v.contains("successful")||v.contains("ready")||v.contains("authorized"))return GREEN;if(v.contains("blocked")||v.contains("fail")||v.contains("denied")||v.contains("expired"))return RED;if(v.contains("waiting")||v.contains("pending")||v.contains("not started"))return YELLOW;return PURPLE;}
}
