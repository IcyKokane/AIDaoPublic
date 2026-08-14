package dev.thefoolish.aidao;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
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
    private static final int HEADER = Color.rgb(22, 23, 29);
    private static final int PANEL = Color.rgb(29, 31, 39);
    private static final int PANEL_ALT = Color.rgb(35, 38, 48);
    private static final int MUTED = Color.rgb(157, 162, 178);
    private static final int BLUE = Color.rgb(80, 148, 255);
    private static final int PURPLE = Color.rgb(145, 93, 255);
    private static final int GREEN = Color.rgb(76, 201, 144);
    private static final String UPDATE_SOURCE = "https://raw.githubusercontent.com/IcyKokane/AIDaoPublic/main/updates/latest.json";
    private static final String FALLBACK_DOWNLOAD_PAGE = "https://github.com/IcyKokane/AIDaoPublic/actions/workflows/android.yml";

    private LinearLayout content;
    private LinearLayout header;
    private LinearLayout bottomNav;
    private ScrollView contentScroll;
    private int baseContentBottom;
    private int activeNavIndex = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(Color.rgb(12, 13, 17));
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= 30) w.setDecorFitsSystemWindows(false);
        showProjects();
    }

    private void buildShell(String title, String subtitle) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(22), dp(18), dp(14));
        header.setBackgroundColor(HEADER);

        TextView logo = label("A", 20, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable logoBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{BLUE, PURPLE});
        logoBg.setCornerRadius(dp(12));
        logo.setBackground(logoBg);
        logo.setContentDescription("AIDao");
        header.addView(logo, box(dp(42), dp(42), 0, 0, dp(12), 0));

        LinearLayout titles = column();
        titles.addView(label(title, 20, Color.WHITE, true));
        titles.addView(label(subtitle, 12, MUTED, false), lp(-1, -2, 0, 3, 0, 0));
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));

        TextView status = label("●", 17, GREEN, true);
        status.setGravity(Gravity.CENTER);
        status.setContentDescription("AIDao ready");
        header.addView(status, new LinearLayout.LayoutParams(dp(40), dp(40)));
        root.addView(header);

        contentScroll = new ScrollView(this);
        contentScroll.setFillViewport(true);
        content = column();
        baseContentBottom = dp(26);
        content.setPadding(dp(18), dp(18), dp(18), baseContentBottom);
        contentScroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(contentScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        bottomNav = createBottomNav();
        root.addView(bottomNav);
        setContentView(root);
        applyInsets(root);
    }

    private void applyInsets(View root) {
        if (Build.VERSION.SDK_INT >= 23) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                int statusTop;
                int navBottom;
                int imeBottom = 0;
                boolean imeVisible = false;

                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                    android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                    statusTop = bars.top;
                    navBottom = bars.bottom;
                    imeBottom = ime.bottom;
                    imeVisible = insets.isVisible(WindowInsets.Type.ime());
                } else {
                    statusTop = insets.getSystemWindowInsetTop();
                    navBottom = insets.getSystemWindowInsetBottom();
                }

                header.setPadding(dp(18), statusTop + dp(22), dp(18), dp(14));
                int bottomSafe = Math.max(navBottom, dp(6));
                bottomNav.setPadding(dp(8), dp(7), dp(8), bottomSafe + dp(8));

                if (Build.VERSION.SDK_INT >= 30) {
                    int keyboardInset = imeVisible ? Math.max(0, imeBottom - navBottom) : 0;
                    bottomNav.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
                    bottomNav.setTranslationY(0f);
                    content.setPadding(dp(18), dp(18), dp(18), baseContentBottom + keyboardInset + (imeVisible ? dp(12) : 0));
                }
                return insets;
            });
            root.requestApplyInsets();
        }
    }

    private void showProjects() {
        activeNavIndex = 0;
        buildShell("AIDao", "Developer workspace");
        LinearLayout welcome = panel(PANEL);
        welcome.addView(label("BUILD WITH AIDAO", 11, BLUE, true));
        welcome.addView(label("What do you want to create?", 25, Color.WHITE, true), lp(-1, -2, 0, 6, 0, 8));
        welcome.addView(label("Describe the product. AIDao turns it into a visible plan, source changes, builds, tests, repairs, and an installable APK.", 14, MUTED, false));
        Button create = primaryButton("＋  New Project");
        create.setOnClickListener(v -> openNewProjectDialog());
        welcome.addView(create, lp(-1, dp(50), 0, 16, 0, 0));
        content.addView(welcome);

        LinearLayout section = row();
        section.addView(label("Recent projects", 18, Color.WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        section.addView(label("View all", 13, BLUE, true));
        content.addView(section, lp(-1, -2, 0, 22, 0, 8));
        content.addView(projectCard("Anime Library", "Android", "PLANNING", BLUE, "Provider plugin architecture · Repository support", "Updated recently"));
        content.addView(projectCard("Starter Workspace", "Android", "APK READY", GREEN, "Baseline AIDao build pipeline", "Latest verified build"), lp(-1, -2, 0, 10, 0, 0));

        content.addView(label("Activity", 18, Color.WHITE, true), lp(-1, -2, 0, 24, 0, 8));
        content.addView(activityRow("✓", GREEN, "Android CI pipeline", "Build and APK artifact delivery enabled"));
        content.addView(activityRow("↗", BLUE, "GitHub connected", "IcyKokane / AIDaoPublic"), lp(-1, -2, 0, 8, 0, 0));
        View updates = activityRow("↓", PURPLE, "Check for Updates", "Installed: " + BuildConfig.VERSION_NAME + " · Tap to check");
        updates.setOnClickListener(v -> checkForUpdates());
        updates.setClickable(true);
        updates.setFocusable(true);
        updates.setContentDescription("Check for AIDao updates. Installed version " + BuildConfig.VERSION_NAME);
        content.addView(updates, lp(-1, -2, 0, 8, 0, 0));
    }

    private View projectCard(String name, String platform, String state, int stateColor, String detail, String time) {
        LinearLayout card = panel(PANEL);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> showWorkspace(name, state));
        LinearLayout top = row();
        TextView icon = label("◇", 20, BLUE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(PANEL_ALT, 11));
        top.addView(icon, box(dp(44), dp(44), 0, 0, dp(12), 0));
        LinearLayout names = column();
        names.addView(label(name, 17, Color.WHITE, true));
        names.addView(label(platform, 12, MUTED, false));
        top.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = label(state, 10, stateColor, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(9), dp(5), dp(9), dp(5));
        badge.setBackground(stroke(PANEL_ALT, stateColor, 10));
        top.addView(badge);
        card.addView(top);
        card.addView(label(detail, 13, Color.rgb(205, 208, 218), false), lp(-1, -2, 0, 12, 0, 8));
        card.addView(label(time, 11, MUTED, false));
        return card;
    }

    private void showWorkspace(String projectName, String state) {
        activeNavIndex = 1;
        buildShell(projectName, "Project workspace · " + state);
        HorizontalScrollView tabsScroll = new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = row();
        String[] items = {"Chat", "Plan", "Files", "Builds", "GitHub"};
        for (int i = 0; i < items.length; i++) {
            TextView chip = label(items[i], 12, i == 0 ? Color.WHITE : MUTED, i == 0);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(13), dp(9), dp(13), dp(9));
            chip.setBackground(round(i == 0 ? Color.rgb(71, 78, 104) : PANEL, 12));
            tabs.addView(chip, lp(-2, -2, 0, 0, 7, 0));
        }
        tabsScroll.addView(tabs);
        content.addView(tabsScroll, new LinearLayout.LayoutParams(-1, -2));
        content.addView(label("# ai-build", 13, MUTED, true), lp(-1, -2, 0, 20, 0, 10));
        content.addView(message("A", "AIDao", "Plan, source changes, CI status, approvals, and APK delivery stay visible in this project workspace.", BLUE));
        content.addView(buildEvent("Understanding request", "Complete", GREEN), lp(-1, -2, 0, 12, 0, 0));
        content.addView(buildEvent("Implementation plan", "Ready", BLUE), lp(-1, -2, 0, 8, 0, 0));
        content.addView(buildEvent("Android build", state.equals("APK READY") ? "Successful" : "Waiting", state.equals("APK READY") ? GREEN : PURPLE), lp(-1, -2, 0, 8, 0, 0));

        LinearLayout composer = panel(PANEL);
        EditText prompt = new EditText(this);
        prompt.setHint("Message AIDao about this project…");
        prompt.setHintTextColor(MUTED);
        prompt.setTextColor(Color.WHITE);
        prompt.setTextSize(14);
        prompt.setMinLines(2);
        prompt.setMaxLines(5);
        prompt.setBackgroundColor(Color.TRANSPARENT);
        composer.addView(prompt);
        Button send = primaryButton("Send to AIDao");
        send.setOnClickListener(v -> {
            if (!prompt.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Added to project conversation", Toast.LENGTH_SHORT).show();
                prompt.setText("");
            }
        });
        composer.addView(send, lp(-1, dp(46), 0, 8, 0, 0));
        content.addView(composer, lp(-1, -2, 0, 18, 0, 0));
    }

    private void showAIDao() {
        activeNavIndex = 1;
        buildShell("AIDao", "AI build console");
        content.addView(label("Assistant", 21, Color.WHITE, true));
        content.addView(label("A focused command surface for active project work. Open a project to keep plans, source changes, builds, and approvals in context.", 14, MUTED, false), lp(-1, -2, 0, 8, 0, 16));
        content.addView(message("A", "AIDao", "I’m ready to continue the active Android project. Choose a recent project or create a new one from Projects.", BLUE));
        Button projects = primaryButton("Open Projects");
        projects.setOnClickListener(v -> showProjects());
        content.addView(projects, lp(-1, dp(48), 0, 16, 0, 0));
    }

    private void showActivity() {
        activeNavIndex = 2;
        buildShell("Activity", "Build and update status");
        content.addView(label("System activity", 21, Color.WHITE, true));
        content.addView(activityRow("✓", GREEN, "Android build pipeline", "CI and APK artifact delivery are configured"), lp(-1, -2, 0, 12, 0, 0));
        content.addView(activityRow("↗", BLUE, "Repository", "IcyKokane / AIDaoPublic"), lp(-1, -2, 0, 8, 0, 0));
        View update = activityRow("↓", PURPLE, "Check for Updates", "Current install: " + BuildConfig.VERSION_NAME);
        update.setOnClickListener(v -> checkForUpdates());
        update.setClickable(true);
        update.setFocusable(true);
        update.setContentDescription("Check for AIDao updates. Installed version " + BuildConfig.VERSION_NAME);
        content.addView(update, lp(-1, -2, 0, 8, 0, 0));
    }

    private void showSettings() {
        activeNavIndex = 3;
        buildShell("Settings", "AIDao preferences and updates");
        content.addView(label("App", 18, Color.WHITE, true));
        LinearLayout version = panel(PANEL);
        version.addView(label("AIDao " + BuildConfig.VERSION_NAME, 16, Color.WHITE, true));
        version.addView(label("AIDao checks its published update metadata on AIDaoPublic. Updates are always user-controlled; the app never silently installs an APK.", 13, MUTED, false), lp(-1, -2, 0, 7, 0, 0));
        Button check = primaryButton("Check for Updates");
        check.setOnClickListener(v -> checkForUpdates());
        check.setContentDescription("Check for AIDao updates");
        version.addView(check, lp(-1, dp(48), 0, 14, 0, 0));
        content.addView(version, lp(-1, -2, 0, 10, 0, 0));
    }

    private void checkForUpdates() {
        Toast.makeText(this, "Checking AIDaoPublic…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(UPDATE_SOURCE).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setUseCaches(false);
                c.setRequestProperty("Cache-Control", "no-cache");
                c.setRequestProperty("User-Agent", "AIDao-Android/" + BuildConfig.VERSION_NAME);
                if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) body.append(line);
                r.close();
                String latest = jsonValue(body.toString(), "version");
                String download = jsonValue(body.toString(), "downloadUrl");
                if (latest == null) throw new IllegalStateException("version missing");
                runOnUiThread(() -> showUpdateResult(latest, download));
            } catch (Exception e) {
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("Unable to check for updates")
                        .setMessage("AIDao could not reach its update source. Check your connection and try again.")
                        .setPositiveButton("OK", null)
                        .show());
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }

    private String jsonValue(String json, String key) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
        return m.find() ? m.group(1).replace("\\/", "/") : null;
    }

    private void showUpdateResult(String latest, String download) {
        boolean newer = compareVersions(latest, BuildConfig.VERSION_NAME) > 0;
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(newer ? "Update available" : "AIDao is current")
                .setMessage("Installed: " + BuildConfig.VERSION_NAME + "\nAvailable: " + latest);
        if (newer) {
            b.setPositiveButton("Open verified build page", (d, which) -> {
                String target = verifiedDownloadTarget(download);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(target)));
            });
            b.setNegativeButton("Not now", null);
        } else {
            b.setPositiveButton("Done", null);
        }
        b.show();
    }

    private String verifiedDownloadTarget(String download) {
        if (download == null || download.trim().isEmpty()) return FALLBACK_DOWNLOAD_PAGE;
        try {
            Uri uri = Uri.parse(download.trim());
            String host = uri.getHost();
            String path = uri.getPath();
            boolean trustedHost = "github.com".equalsIgnoreCase(host);
            boolean trustedRepo = path != null && path.startsWith("/IcyKokane/AIDaoPublic/");
            return trustedHost && trustedRepo ? uri.toString() : FALLBACK_DOWNLOAD_PAGE;
        } catch (Exception ignored) {
            return FALLBACK_DOWNLOAD_PAGE;
        }
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

    private void openNewProjectDialog() {
        EditText request = new EditText(this);
        request.setHint("Example: Build an anime library app with provider plugins…");
        request.setMinLines(5);
        request.setTextSize(14);
        request.setPadding(dp(14), dp(12), dp(14), dp(12));
        new AlertDialog.Builder(this)
                .setTitle("Create a project")
                .setMessage("Describe the Android app in ordinary language. You can refine the architecture and behavior after creation.")
                .setView(request)
                .setPositiveButton("Create Project Brief", (d, which) -> {
                    String raw = request.getText().toString().trim();
                    if (raw.isEmpty()) Toast.makeText(this, "Describe what you want to build", Toast.LENGTH_SHORT).show();
                    else showGeneratedBrief(raw);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGeneratedBrief(String raw) {
        activeNavIndex = 1;
        buildShell("New project", "AIDao project brief");
        content.addView(label("Project brief", 22, Color.WHITE, true));
        content.addView(label(raw, 15, Color.rgb(224, 226, 234), false), lp(-1, -2, 0, 8, 0, 16));
        content.addView(buildEvent("1. Understand product goal", "Queued", BLUE));
        content.addView(buildEvent("2. Define architecture", "Queued", PURPLE), lp(-1, -2, 0, 8, 0, 0));
        content.addView(buildEvent("3. Generate Android files", "Queued", PURPLE), lp(-1, -2, 0, 8, 0, 0));
        content.addView(buildEvent("4. Build, test, and repair", "Queued", PURPLE), lp(-1, -2, 0, 8, 0, 0));
        content.addView(buildEvent("5. Deliver APK", "Queued", PURPLE), lp(-1, -2, 0, 8, 0, 0));
        Button open = primaryButton("Open Project Workspace");
        open.setOnClickListener(v -> showWorkspace("New project", "PLANNING"));
        content.addView(open, lp(-1, dp(50), 0, 18, 0, 0));
    }

    private View message(String avatar, String author, String body, int accent) {
        LinearLayout row = row();
        row.setGravity(Gravity.TOP);
        TextView av = label(avatar, 17, Color.WHITE, true);
        av.setGravity(Gravity.CENTER);
        av.setBackground(round(accent, 12));
        row.addView(av, box(dp(42), dp(42), 0, 0, dp(11), 0));
        LinearLayout bubble = panel(PANEL);
        bubble.setPadding(dp(14), dp(12), dp(14), dp(12));
        bubble.addView(label(author + "  ·  now", 12, Color.rgb(208, 212, 226), true));
        bubble.addView(label(body, 14, Color.WHITE, false), lp(-1, -2, 0, 6, 0, 0));
        row.addView(bubble, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private View buildEvent(String title, String state, int stateColor) {
        LinearLayout row = panel(PANEL_ALT);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(11), dp(14), dp(11));
        TextView dot = label("●", 11, stateColor, true);
        row.addView(dot, box(dp(28), dp(28), 0, 0, dp(7), 0));
        LinearLayout info = column();
        info.addView(label(title, 13, Color.WHITE, true));
        info.addView(label(state, 11, MUTED, false));
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private View activityRow(String mark, int color, String title, String detail) {
        LinearLayout row = panel(PANEL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView icon = label(mark, 15, color, true);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, box(dp(36), dp(36), 0, 0, dp(10), 0));
        LinearLayout text = column();
        text.addView(label(title, 14, Color.WHITE, true));
        text.addView(label(detail, 11, MUTED, false));
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private LinearLayout createBottomNav() {
        LinearLayout nav = row();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(7), dp(8), dp(12));
        nav.setBackgroundColor(Color.rgb(13, 14, 18));
        String[] names = {"Projects", "AIDao", "Activity", "Settings"};
        String[] icons = {"▦", "◇", "≋", "⚙"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            boolean active = i == activeNavIndex;
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setMinimumHeight(dp(58));
            item.setClickable(true);
            item.setFocusable(true);
            item.setContentDescription(names[i]);
            item.addView(label(icons[i], 17, active ? BLUE : MUTED, true));
            item.addView(label(names[i], 10, active ? Color.WHITE : MUTED, active));
            item.setOnClickListener(v -> {
                if (index == 0) showProjects();
                else if (index == 1) showAIDao();
                else if (index == 2) showActivity();
                else showSettings();
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        return nav;
    }

    private LinearLayout panel(int color) {
        LinearLayout v = column();
        v.setPadding(dp(16), dp(15), dp(16), dp(15));
        v.setBackground(round(color, 15));
        return v;
    }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private Button primaryButton(String text) { Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setAllCaps(false); b.setBackground(round(BLUE, 12)); return b; }
    private TextView label(String text, int sp, int color, boolean bold) { TextView v = new TextView(this); v.setText(text); v.setTextColor(color); v.setTextSize(sp); v.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL)); v.setIncludeFontPadding(false); return v; }
    private GradientDrawable round(int color, int radiusDp) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radiusDp)); return d; }
    private GradientDrawable stroke(int color, int strokeColor, int radiusDp) { GradientDrawable d = round(color, radiusDp); d.setStroke(dp(1), strokeColor); return d; }
    private LinearLayout.LayoutParams lp(int w, int h, int left, int top, int right, int bottom) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h); p.setMargins(dp(left), dp(top), dp(right), dp(bottom)); return p; }
    private LinearLayout.LayoutParams box(int w, int h, int left, int top, int right, int bottom) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h); p.setMargins(dp(left), dp(top), dp(right), dp(bottom)); return p; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
