package dev.thefoolish.aidao;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(18, 19, 24);
    private static final int PANEL = Color.rgb(29, 31, 39);
    private static final int PANEL_ALT = Color.rgb(35, 38, 48);
    private static final int MUTED = Color.rgb(157, 162, 178);
    private static final int BLUE = Color.rgb(80, 148, 255);
    private static final int PURPLE = Color.rgb(145, 93, 255);
    private static final int GREEN = Color.rgb(76, 201, 144);
    private static final String UPDATE_SOURCE = "https://raw.githubusercontent.com/IcyKokane/AIDaoPublic/main/android-bootstrap/app/build.gradle.kts";
    private static final String DOWNLOAD_PAGE = "https://github.com/IcyKokane/AIDaoPublic/actions/workflows/android.yml";

    private LinearLayout content;
    private LinearLayout shellRoot;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.rgb(12, 13, 17));
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        showProjects();
    }

    private void buildShell(String title, String subtitle) {
        LinearLayout root = new LinearLayout(this);
        shellRoot = root;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(0, dp(8), 0, 0);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(16), dp(18), dp(13));
        header.setBackgroundColor(Color.rgb(22, 23, 29));

        TextView logo = label("A", 21, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable logoBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{BLUE, PURPLE});
        logoBg.setCornerRadius(dp(12));
        logo.setBackground(logoBg);
        header.addView(logo, box(dp(42), dp(42), 0, 0, dp(12), 0));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(label(title, 20, Color.WHITE, true));
        titles.addView(label(subtitle, 12, MUTED, false));
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));

        TextView account = label("●", 18, GREEN, true);
        account.setGravity(Gravity.CENTER);
        account.setContentDescription("Connected status");
        header.addView(account, box(dp(40), dp(40), 0, 0, 0, 0));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottomNav());
        setContentView(root);
        applyInsets(root);
    }

    private void applyInsets(View root) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets system = insets.getInsets(WindowInsets.Type.systemBars());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                top = system.top;
                bottom = Math.max(system.bottom, ime.bottom);
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0, top + dp(8), 0, bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void showProjects() {
        buildShell("AIDao", "Developer workspace");
        LinearLayout welcome = panel(PANEL);
        welcome.addView(label("BUILD WITH AIDAO", 11, BLUE, true));
        welcome.addView(label("What do you want to create?", 25, Color.WHITE, true), lp(-1, -2, 0, 5, 0, 7));
        welcome.addView(label("Describe the product. AIDao turns it into a plan, files, builds, tests, repairs, and an installable APK.", 14, MUTED, false));
        Button create = primaryButton("＋  New Project");
        create.setOnClickListener(v -> openNewProjectDialog());
        welcome.addView(create, lp(-1, dp(50), 0, 16, 0, 0));
        content.addView(welcome);

        LinearLayout section = horizontal();
        section.addView(label("Recent projects", 18, Color.WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView all = label("View all", 13, BLUE, true);
        all.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        section.addView(all);
        content.addView(section, lp(-1, -2, 0, 22, 0, 8));
        content.addView(projectCard("Anime Library", "Android", "PLANNING", BLUE, "Provider plugin architecture · Repository support", "Updated just now"));
        content.addView(projectCard("Starter Workspace", "Android", "APK READY", GREEN, "Baseline AIDao build pipeline", "Latest verified build"), lp(-1, -2, 0, 10, 0, 0));

        content.addView(label("Activity", 18, Color.WHITE, true), lp(-1, -2, 0, 24, 0, 8));
        content.addView(activityRow("✓", GREEN, "Android CI pipeline", "Build and APK artifact delivery enabled"));
        content.addView(activityRow("↗", BLUE, "GitHub connected", "IcyKokane / AIDaoPublic"), lp(-1, -2, 0, 8, 0, 0));
        View updates = activityRow("↓", PURPLE, "Check for Updates", "Installed: " + BuildConfig.VERSION_NAME + " · Tap to check GitHub");
        updates.setOnClickListener(v -> checkForUpdates());
        updates.setClickable(true);
        content.addView(updates, lp(-1, -2, 0, 8, 0, 0));
    }

    private View projectCard(String name, String platform, String status, int statusColor, String detail, String time) {
        LinearLayout card = panel(PANEL);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> showWorkspace(name, status));
        LinearLayout top = horizontal();
        TextView icon = label("◇", 20, BLUE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(PANEL_ALT, 11));
        top.addView(icon, box(dp(44), dp(44), 0, 0, dp(12), 0));
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(label(name, 17, Color.WHITE, true));
        names.addView(label(platform, 12, MUTED, false));
        top.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = label(status, 10, statusColor, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(9), dp(5), dp(9), dp(5));
        badge.setBackground(stroke(PANEL_ALT, statusColor, 10));
        top.addView(badge);
        card.addView(top);
        card.addView(label(detail, 13, Color.rgb(205, 208, 218), false), lp(-1, -2, 0, 12, 0, 8));
        card.addView(label(time, 11, MUTED, false));
        return card;
    }

    private void showWorkspace(String projectName, String status) {
        buildShell(projectName, "Project workspace · " + status);
        ScrollView tabScroll = new ScrollView(this);
        LinearLayout tabs = horizontal();
        String[] items = {"Chat", "Plan", "Files", "Builds", "GitHub"};
        for (int i = 0; i < items.length; i++) {
            TextView chip = label(items[i], 12, i == 0 ? Color.WHITE : MUTED, i == 0);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setBackground(round(i == 0 ? Color.rgb(71, 78, 104) : PANEL, 12));
            tabs.addView(chip, lp(-2, -2, 0, 0, 7, 0));
        }
        content.addView(tabs);
        content.addView(label("# ai-build", 13, MUTED, true), lp(-1, -2, 0, 20, 0, 10));
        content.addView(message("A", "AIDao", "I can take this project from description to installable Android build. Plan, source changes, CI status, and APK delivery stay visible here.", BLUE));
        content.addView(buildEvent("Understanding request", "Complete", GREEN), lp(-1, -2, 0, 12, 0, 0));
        content.addView(buildEvent("Implementation plan", "Ready for next pass", BLUE), lp(-1, -2, 0, 8, 0, 0));
        content.addView(buildEvent("Android build", status.equals("APK READY") ? "Successful" : "Waiting", status.equals("APK READY") ? GREEN : PURPLE), lp(-1, -2, 0, 8, 0, 0));
        LinearLayout composer = panel(PANEL);
        EditText prompt = new EditText(this);
        prompt.setHint("Message AIDao about this project...");
        prompt.setHintTextColor(MUTED);
        prompt.setTextColor(Color.WHITE);
        prompt.setTextSize(14);
        prompt.setMinLines(2);
        prompt.setMaxLines(5);
        prompt.setBackgroundColor(Color.TRANSPARENT);
        composer.addView(prompt);
        Button send = primaryButton("Send to AIDao");
        send.setOnClickListener(v -> { if (!prompt.getText().toString().trim().isEmpty()) { Toast.makeText(this, "Added to project conversation", Toast.LENGTH_SHORT).show(); prompt.setText(""); } });
        composer.addView(send, lp(-1, dp(46), 0, 8, 0, 0));
        content.addView(composer, lp(-1, -2, 0, 18, 0, 0));
    }

    private void showSettings() {
        buildShell("Settings", "AIDao preferences and updates");
        content.addView(label("App", 18, Color.WHITE, true));
        LinearLayout version = panel(PANEL);
        version.addView(label("AIDao " + BuildConfig.VERSION_NAME, 16, Color.WHITE, true));
        version.addView(label("Updates are checked against the authoritative AIDaoPublic GitHub source. AIDao never silently installs an APK.", 13, MUTED, false), lp(-1, -2, 0, 7, 0, 0));
        Button check = primaryButton("Check for Updates");
        check.setOnClickListener(v -> checkForUpdates());
        version.addView(check, lp(-1, dp(48), 0, 14, 0, 0));
        content.addView(version, lp(-1, -2, 0, 10, 0, 0));
    }

    private void checkForUpdates() {
        Toast.makeText(this, "Checking AIDaoPublic…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(UPDATE_SOURCE).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("User-Agent", "AIDao-Android");
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) body.append(line).append('\n');
                r.close();
                Matcher m = Pattern.compile("versionName\\s*=\\s*\"([^\"]+)\"").matcher(body.toString());
                if (!m.find()) throw new IllegalStateException("versionName missing");
                String latest = m.group(1);
                runOnUiThread(() -> showUpdateResult(latest));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Update check failed. Check your connection and try again.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showUpdateResult(String latest) {
        boolean newer = compareVersions(latest, BuildConfig.VERSION_NAME) > 0;
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout wrap = panel(PANEL);
        wrap.setPadding(dp(20), dp(20), dp(20), dp(20));
        wrap.addView(label(newer ? "Update available" : "AIDao is current", 20, Color.WHITE, true));
        wrap.addView(label("Installed: " + BuildConfig.VERSION_NAME + "\nAvailable: " + latest, 13, MUTED, false), lp(-1, -2, 0, 8, 0, 14));
        if (newer) {
            Button obtain = primaryButton("Open Verified Build Page");
            obtain.setOnClickListener(v -> {
                dialog.dismiss();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_PAGE)));
            });
            wrap.addView(obtain, new LinearLayout.LayoutParams(-1, dp(48)));
            wrap.addView(label("You choose whether to download and install the newer APK. AIDao does not install updates in the background.", 11, MUTED, false), lp(-1, -2, 0, 10, 0, 0));
        } else {
            Button done = secondaryButton("Done");
            done.setOnClickListener(v -> dialog.dismiss());
            wrap.addView(done, new LinearLayout.LayoutParams(-1, dp(48)));
        }
        dialog.setContentView(wrap);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) { w.setBackgroundDrawableResource(android.R.color.transparent); w.setLayout((int)(getResources().getDisplayMetrics().widthPixels * .90f), WindowManager.LayoutParams.WRAP_CONTENT); }
    }

    private int compareVersions(String a, String b) {
        String[] aa = a.replaceAll("[^0-9.]", "").split("\\.");
        String[] bb = b.replaceAll("[^0-9.]", "").split("\\.");
        int n = Math.max(aa.length, bb.length);
        for (int i = 0; i < n; i++) {
            int av = i < aa.length && !aa[i].isEmpty() ? Integer.parseInt(aa[i]) : 0;
            int bv = i < bb.length && !bb[i].isEmpty() ? Integer.parseInt(bb[i]) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private View message(String avatar, String author, String body, int accent) {
        LinearLayout row = horizontal(); row.setGravity(Gravity.TOP);
        TextView av = label(avatar, 17, Color.WHITE, true); av.setGravity(Gravity.CENTER); av.setBackground(round(accent, 12));
        row.addView(av, box(dp(42), dp(42), 0, 0, dp(11), 0));
        LinearLayout bubble = panel(PANEL); bubble.setPadding(dp(14), dp(12), dp(14), dp(12));
        bubble.addView(label(author + "  ·  now", 12, Color.rgb(208, 212, 226), true));
        bubble.addView(label(body, 14, Color.WHITE, false), lp(-1, -2, 0, 6, 0, 0));
        row.addView(bubble, new LinearLayout.LayoutParams(0, -2, 1)); return row;
    }

    private View buildEvent(String title, String state, int stateColor) {
        LinearLayout row = panel(PANEL_ALT); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(14), dp(11), dp(14), dp(11));
        TextView dot = label("●", 11, stateColor, true); row.addView(dot, box(dp(28), dp(28), 0, 0, dp(7), 0));
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.addView(label(title, 13, Color.WHITE, true)); info.addView(label(state, 11, MUTED, false));
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1)); return row;
    }

    private View activityRow(String mark, int color, String title, String detail) {
        LinearLayout row = panel(PANEL); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView icon = label(mark, 15, color, true); icon.setGravity(Gravity.CENTER); row.addView(icon, box(dp(36), dp(36), 0, 0, dp(10), 0));
        LinearLayout text = new LinearLayout(this); text.setOrientation(LinearLayout.VERTICAL); text.addView(label(title, 14, Color.WHITE, true)); text.addView(label(detail, 11, MUTED, false));
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1)); return row;
    }

    private void openNewProjectDialog() {
        Dialog dialog = new Dialog(this); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout wrap = panel(PANEL); wrap.setPadding(dp(20), dp(20), dp(20), dp(20));
        wrap.addView(label("Create a project", 22, Color.WHITE, true));
        wrap.addView(label("Describe the Android app in ordinary language. You can refine architecture and behavior with AIDao after creation.", 13, MUTED, false), lp(-1, -2, 0, 5, 0, 14));
        EditText request = new EditText(this); request.setHint("Example: Build an anime library app with provider plugins…"); request.setHintTextColor(MUTED); request.setTextColor(Color.WHITE); request.setTextSize(14); request.setMinLines(5); request.setGravity(Gravity.TOP); request.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); request.setPadding(dp(14), dp(12), dp(14), dp(12)); request.setBackground(stroke(Color.rgb(24,26,33), Color.rgb(63,67,82), 12));
        wrap.addView(request, new LinearLayout.LayoutParams(-1, dp(150)));
        Button create = primaryButton("Create Project Brief");
        create.setOnClickListener(v -> { String raw = request.getText().toString().trim(); if (raw.isEmpty()) { request.setError("Describe what you want to build"); return; } dialog.dismiss(); showGeneratedBrief(raw); });
        wrap.addView(create, lp(-1, dp(48), 0, 14, 0, 0)); dialog.setContentView(wrap); dialog.show();
        Window w = dialog.getWindow(); if (w != null) { w.setBackgroundDrawableResource(android.R.color.transparent); w.setLayout((int)(getResources().getDisplayMetrics().widthPixels * .92f), WindowManager.LayoutParams.WRAP_CONTENT); w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE); }
        request.requestFocus();
    }

    private void showGeneratedBrief(String raw) {
        buildShell("New project", "AIDao project brief"); content.addView(label("Project brief", 22, Color.WHITE, true)); content.addView(label(raw, 15, Color.rgb(224,226,234), false), lp(-1,-2,0,8,0,16));
        content.addView(buildEvent("1. Understand product goal", "Queued", BLUE)); content.addView(buildEvent("2. Define architecture and requirements", "Queued", PURPLE), lp(-1,-2,0,8,0,0)); content.addView(buildEvent("3. Generate Android project files", "Queued", PURPLE), lp(-1,-2,0,8,0,0)); content.addView(buildEvent("4. Build, test, and repair", "Queued", PURPLE), lp(-1,-2,0,8,0,0)); content.addView(buildEvent("5. Deliver APK", "Queued", PURPLE), lp(-1,-2,0,8,0,0));
        Button open = primaryButton("Open Project Workspace"); open.setOnClickListener(v -> showWorkspace("New project", "PLANNING")); content.addView(open, lp(-1,dp(50),0,18,0,0));
    }

    private View bottomNav() {
        LinearLayout nav = horizontal(); nav.setGravity(Gravity.CENTER); nav.setPadding(dp(8), dp(7), dp(8), dp(9)); nav.setBackgroundColor(Color.rgb(13,14,18));
        String[] names = {"Projects", "AIDao", "Activity", "Settings"}; String[] icons = {"▦", "◇", "≋", "⚙"};
        for (int i=0;i<names.length;i++) { final int index=i; LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER); item.addView(label(icons[i],17,i==0?BLUE:MUTED,true)); item.addView(label(names[i],10,i==0?Color.WHITE:MUTED,i==0)); item.setOnClickListener(v -> { if(index==0) showProjects(); else if(index==3) showSettings(); else Toast.makeText(this,names[index]+" workspace is being integrated",Toast.LENGTH_SHORT).show(); }); nav.addView(item,new LinearLayout.LayoutParams(0,dp(58),1)); }
        return nav;
    }

    private LinearLayout panel(int color) { LinearLayout v=new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); v.setPadding(dp(16),dp(15),dp(16),dp(15)); v.setBackground(round(color,15)); return v; }
    private LinearLayout horizontal() { LinearLayout v=new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private Button primaryButton(String text) { Button b=new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setAllCaps(false); b.setBackground(round(BLUE,12)); return b; }
    private Button secondaryButton(String text) { Button b=primaryButton(text); b.setBackground(round(PANEL_ALT,12)); return b; }
    private TextView label(String text,int sp,int color,boolean bold) { TextView v=new TextView(this); v.setText(text); v.setTextColor(color); v.setTextSize(sp); v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL)); v.setIncludeFontPadding(false); return v; }
    private GradientDrawable round(int color,int radiusDp) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radiusDp)); return d; }
    private GradientDrawable stroke(int color,int strokeColor,int radiusDp) { GradientDrawable d=round(color,radiusDp); d.setStroke(dp(1),strokeColor); return d; }
    private LinearLayout.LayoutParams lp(int w,int h,int left,int top,int right,int bottom) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(dp(left),dp(top),dp(right),dp(bottom)); return p; }
    private LinearLayout.LayoutParams box(int w,int h,int left,int top,int right,int bottom) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(dp(left),dp(top),dp(right),dp(bottom)); return p; }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
