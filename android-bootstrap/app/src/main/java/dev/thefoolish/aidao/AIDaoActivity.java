package dev.thefoolish.aidao;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
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
import android.view.inputmethod.InputMethodManager;
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

public class AIDaoActivity extends Activity {
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

    private LinearLayout root;
    private LinearLayout header;
    private LinearLayout content;
    private LinearLayout bottomNav;
    private ScrollView scroll;
    private int activeNav = 0;
    private int horizontalPad;
    private boolean updateCheckInProgress;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(Color.rgb(12, 13, 17));
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        if (Build.VERSION.SDK_INT >= 29) window.setNavigationBarContrastEnforced(false);
        showProjects();
    }

    private void buildShell(String title, String subtitle) {
        horizontalPad = getResources().getConfiguration().smallestScreenWidthDp >= 600 ? dp(30) : dp(18);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(HEADER);
        header.setPadding(horizontalPad, dp(28), horizontalPad, dp(14));

        TextView logo = text("A", 20, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable logoBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{BLUE, PURPLE});
        logoBg.setCornerRadius(dp(13));
        logo.setBackground(logoBg);
        logo.setContentDescription("AIDao logo");
        header.addView(logo, margins(dp(44), dp(44), 0, 0, dp(12), 0));

        LinearLayout titles = column();
        titles.addView(text(title, 20, Color.WHITE, true));
        titles.addView(text(subtitle, 12, MUTED, false), margins(-1, -2, 0, dp(3), 0, 0));
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));

        TextView ready = text("●", 16, GREEN, true);
        ready.setGravity(Gravity.CENTER);
        ready.setContentDescription("AIDao ready");
        header.addView(ready, new LinearLayout.LayoutParams(dp(44), dp(44)));
        root.addView(header);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = column();
        content.setPadding(horizontalPad, dp(18), horizontalPad, dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        bottomNav = createBottomNav();
        root.addView(bottomNav);
        setContentView(root);
        installInsets();
        installLegacyImeDetector();
    }

    private void installInsets() {
        if (Build.VERSION.SDK_INT < 23) return;
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            int left;
            int right;
            int imeBottom = 0;
            boolean imeVisible = false;

            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                top = bars.top;
                bottom = bars.bottom;
                left = bars.left;
                right = bars.right;
                imeBottom = ime.bottom;
                imeVisible = insets.isVisible(WindowInsets.Type.ime());
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
                left = insets.getSystemWindowInsetLeft();
                right = insets.getSystemWindowInsetRight();
            }

            int safeLeft = Math.max(horizontalPad, left + dp(12));
            int safeRight = Math.max(horizontalPad, right + dp(12));
            header.setPadding(safeLeft, top + dp(28), safeRight, dp(14));
            bottomNav.setPadding(Math.max(dp(8), left + dp(6)), dp(7), Math.max(dp(8), right + dp(6)), Math.max(bottom, dp(6)) + dp(8));

            if (Build.VERSION.SDK_INT >= 30) {
                int keyboard = imeVisible ? Math.max(0, imeBottom - bottom) : 0;
                bottomNav.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
                content.setPadding(safeLeft, dp(18), safeRight, dp(28) + keyboard + (imeVisible ? dp(12) : 0));
            } else {
                content.setPadding(safeLeft, dp(18), safeRight, dp(28));
            }
            return insets;
        });
        root.requestApplyInsets();
    }

    private void installLegacyImeDetector() {
        if (Build.VERSION.SDK_INT >= 30) return;
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect visible = new Rect();
            root.getWindowVisibleDisplayFrame(visible);
            int hidden = root.getRootView().getHeight() - visible.bottom;
            boolean keyboard = hidden > root.getRootView().getHeight() * 0.18f;
            bottomNav.setVisibility(keyboard ? View.GONE : View.VISIBLE);
        });
    }

    private void showProjects() {
        activeNav = 0;
        buildShell("AIDao", "Developer workspace");
        LinearLayout hero = panel(PANEL);
        hero.addView(text("BUILD WITH AIDAO", 11, BLUE, true));
        hero.addView(text("What do you want to create?", 25, Color.WHITE, true), margins(-1, -2, 0, dp(6), 0, dp(8)));
        hero.addView(text("Describe the product. AIDao keeps the plan, files, builds, tests, repairs, and APK delivery visible in one workspace.", 14, MUTED, false));
        Button create = primary("New Project");
        create.setOnClickListener(v -> openNewProjectDialog());
        hero.addView(create, margins(-1, dp(50), 0, dp(16), 0, 0));
        content.addView(hero);

        content.addView(text("Recent projects", 18, Color.WHITE, true), margins(-1, -2, 0, dp(22), 0, dp(8)));
        content.addView(projectCard("Anime Library", "PLANNING", BLUE, "Provider plugin architecture · Repository support"));
        content.addView(projectCard("Starter Workspace", "APK READY", GREEN, "Baseline AIDao build pipeline"), margins(-1, -2, 0, dp(10), 0, 0));

        content.addView(text("Activity", 18, Color.WHITE, true), margins(-1, -2, 0, dp(24), 0, dp(8)));
        content.addView(activity("✓", GREEN, "Android CI pipeline", "Build and APK artifact delivery enabled"));
        content.addView(activity("↗", BLUE, "GitHub connected", "IcyKokane / AIDaoPublic"), margins(-1, -2, 0, dp(8), 0, 0));
        View updates = activity("↓", PURPLE, "Check for Updates", "Installed: " + BuildConfig.VERSION_NAME + " · Tap to check");
        updates.setOnClickListener(v -> checkForUpdates());
        updates.setClickable(true);
        updates.setFocusable(true);
        content.addView(updates, margins(-1, -2, 0, dp(8), 0, 0));
    }

    private View projectCard(String name, String state, int stateColor, String detail) {
        LinearLayout card = panel(PANEL);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> showWorkspace(name, state));
        LinearLayout top = row();
        TextView icon = text("◇", 20, BLUE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(PANEL_ALT, 11));
        top.addView(icon, margins(dp(44), dp(44), 0, 0, dp(12), 0));
        LinearLayout names = column();
        names.addView(text(name, 17, Color.WHITE, true));
        names.addView(text("Android", 12, MUTED, false));
        top.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = text(state, 10, stateColor, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(9), dp(5), dp(9), dp(5));
        badge.setBackground(stroke(PANEL_ALT, stateColor, 10));
        top.addView(badge);
        card.addView(top);
        card.addView(text(detail, 13, Color.rgb(205, 208, 218), false), margins(-1, -2, 0, dp(12), 0, 0));
        return card;
    }

    private void showWorkspace(String name, String state) {
        activeNav = 1;
        buildShell(name, "Project workspace · " + state);
        addWorkspaceTabs();
        content.addView(text("# ai-build", 13, MUTED, true), margins(-1, -2, 0, dp(18), 0, dp(10)));
        content.addView(message("AIDao", "Plan, source changes, CI status, approvals, and APK delivery stay visible here."));
        content.addView(buildEvent("Understanding request", "Complete", GREEN), margins(-1, -2, 0, dp(12), 0, 0));
        content.addView(buildEvent("Implementation plan", "Ready", BLUE), margins(-1, -2, 0, dp(8), 0, 0));
        content.addView(buildEvent("Android build", state.equals("APK READY") ? "Successful" : "Waiting", state.equals("APK READY") ? GREEN : PURPLE), margins(-1, -2, 0, dp(8), 0, 0));

        LinearLayout composer = panel(PANEL);
        EditText prompt = new EditText(this);
        prompt.setHint("Message AIDao about this project…");
        prompt.setHintTextColor(MUTED);
        prompt.setTextColor(Color.WHITE);
        prompt.setTextSize(14);
        prompt.setMinLines(2);
        prompt.setMaxLines(5);
        prompt.setBackgroundColor(Color.TRANSPARENT);
        composer.addView(prompt, new LinearLayout.LayoutParams(-1, -2));
        Button send = primary("Send to AIDao");
        send.setOnClickListener(v -> {
            String msg = prompt.getText().toString().trim();
            if (!msg.isEmpty()) {
                hideKeyboard(prompt);
                prompt.setText("");
                Toast.makeText(this, "Added to project conversation", Toast.LENGTH_SHORT).show();
            }
        });
        composer.addView(send, margins(-1, dp(48), 0, dp(8), 0, 0));
        content.addView(composer, margins(-1, -2, 0, dp(18), 0, 0));
    }

    private void addWorkspaceTabs() {
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout tabs = row();
        String[] names = {"Chat", "Plan", "Files", "Builds", "GitHub"};
        for (int i = 0; i < names.length; i++) {
            boolean selected = i == 0;
            TextView tab = text(names[i], 12, selected ? Color.WHITE : MUTED, selected);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(14), dp(10), dp(14), dp(10));
            tab.setMinHeight(dp(44));
            tab.setBackground(round(selected ? Color.rgb(71, 78, 104) : PANEL, 12));
            final String tabName = names[i];
            tab.setOnClickListener(v -> Toast.makeText(this, tabName + " workspace", Toast.LENGTH_SHORT).show());
            tabs.addView(tab, margins(-2, -2, 0, 0, dp(7), 0));
        }
        scroller.addView(tabs);
        content.addView(scroller, new LinearLayout.LayoutParams(-1, -2));
    }

    private void showAssistant() {
        activeNav = 1;
        buildShell("AIDao", "AI build console");
        content.addView(text("Assistant", 21, Color.WHITE, true));
        content.addView(text("Open a project to keep plans, source changes, builds, approvals, and installation in context.", 14, MUTED, false), margins(-1, -2, 0, dp(8), 0, dp(16)));
        content.addView(message("AIDao", "Ready to continue active Android work."));
        Button open = primary("Open Projects");
        open.setOnClickListener(v -> showProjects());
        content.addView(open, margins(-1, dp(48), 0, dp(16), 0, 0));
    }

    private void showActivity() {
        activeNav = 2;
        buildShell("Activity", "Build and update status");
        content.addView(text("System activity", 21, Color.WHITE, true));
        content.addView(activity("✓", GREEN, "Android build pipeline", "CI and APK artifact delivery are configured"), margins(-1, -2, 0, dp(12), 0, 0));
        View update = activity("↓", PURPLE, "Check for Updates", "Current install: " + BuildConfig.VERSION_NAME);
        update.setOnClickListener(v -> checkForUpdates());
        update.setClickable(true);
        content.addView(update, margins(-1, -2, 0, dp(8), 0, 0));
    }

    private void showSettings() {
        activeNav = 3;
        buildShell("Settings", "AIDao preferences and updates");
        LinearLayout version = panel(PANEL);
        version.addView(text("AIDao " + BuildConfig.VERSION_NAME, 17, Color.WHITE, true));
        version.addView(text("Updates are checked against AIDaoPublic and are always user-controlled. AIDao never silently installs a build.", 13, MUTED, false), margins(-1, -2, 0, dp(8), 0, 0));
        Button check = primary("Check for Updates");
        check.setOnClickListener(v -> checkForUpdates());
        version.addView(check, margins(-1, dp(50), 0, dp(14), 0, 0));
        content.addView(version);
    }

    private LinearLayout createBottomNav() {
        LinearLayout nav = row();
        nav.setGravity(Gravity.CENTER);
        nav.setBackgroundColor(Color.rgb(13, 14, 18));
        nav.setPadding(dp(8), dp(7), dp(8), dp(12));
        String[] names = {"Projects", "AIDao", "Activity", "Settings"};
        String[] icons = {"▦", "◇", "≋", "⚙"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            boolean selected = i == activeNav;
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setMinimumHeight(dp(58));
            item.setClickable(true);
            item.setFocusable(true);
            item.setContentDescription(names[i] + (selected ? ", selected" : ""));
            item.addView(text(icons[i], 17, selected ? BLUE : MUTED, true));
            item.addView(text(names[i], 10, selected ? Color.WHITE : MUTED, selected));
            item.setOnClickListener(v -> {
                if (index == 0) showProjects();
                else if (index == 1) showAssistant();
                else if (index == 2) showActivity();
                else showSettings();
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        }
        return nav;
    }

    private void openNewProjectDialog() {
        EditText input = new EditText(this);
        input.setHint("Example: Build an anime library app with provider plugins…");
        input.setMinLines(5);
        input.setTextSize(14);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        new AlertDialog.Builder(this)
                .setTitle("Create a project")
                .setMessage("Describe the Android app in ordinary language.")
                .setView(input)
                .setPositiveButton("Create Project Brief", (d, which) -> {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) Toast.makeText(this, "Describe what you want to build", Toast.LENGTH_SHORT).show();
                    else {
                        hideKeyboard(input);
                        showWorkspace("New project", "PLANNING");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkForUpdates() {
        if (updateCheckInProgress) {
            Toast.makeText(this, "Update check already in progress", Toast.LENGTH_SHORT).show();
            return;
        }
        updateCheckInProgress = true;
        Toast.makeText(this, "Checking AIDaoPublic…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(UPDATE_SOURCE).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Cache-Control", "no-cache, no-store");
                connection.setRequestProperty("Pragma", "no-cache");
                connection.setRequestProperty("User-Agent", "AIDao-Android/" + BuildConfig.VERSION_NAME);
                if (connection.getResponseCode() != 200) throw new IllegalStateException("HTTP " + connection.getResponseCode());
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                reader.close();
                String latest = jsonValue(body.toString(), "version");
                String download = jsonValue(body.toString(), "downloadUrl");
                String notes = jsonValue(body.toString(), "notes");
                if (latest == null || latest.trim().isEmpty()) throw new IllegalStateException("version missing");
                runOnUiThread(() -> {
                    updateCheckInProgress = false;
                    showUpdateResult(latest, download, notes);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    updateCheckInProgress = false;
                    new AlertDialog.Builder(this)
                            .setTitle("Unable to check for updates")
                            .setMessage("AIDao could not reach its official AIDaoPublic update source. Check your connection and try again.")
                            .setPositiveButton("OK", null)
                            .show();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void showUpdateResult(String latest, String download, String notes) {
        boolean newer = compareVersions(latest, BuildConfig.VERSION_NAME) > 0;
        StringBuilder message = new StringBuilder("Installed: ").append(BuildConfig.VERSION_NAME).append("\nAvailable: ").append(latest);
        if (notes != null && !notes.trim().isEmpty()) message.append("\n\nWhat changed\n").append(notes.trim());
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(newer ? "Update available" : "AIDao is current")
                .setMessage(message.toString());
        if (newer) {
            dialog.setPositiveButton("Open verified build page", (d, w) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(verifiedDownloadTarget(download)))))
                    .setNegativeButton("Not now", null);
        } else dialog.setPositiveButton("Done", null);
        dialog.show();
    }

    private String verifiedDownloadTarget(String value) {
        if (value == null || value.trim().isEmpty()) return FALLBACK_DOWNLOAD_PAGE;
        try {
            Uri uri = Uri.parse(value.trim());
            String host = uri.getHost();
            String path = uri.getPath();
            boolean secure = "https".equalsIgnoreCase(uri.getScheme());
            boolean trustedHost = "github.com".equalsIgnoreCase(host);
            boolean trustedRepo = path != null && (path.equals("/IcyKokane/AIDaoPublic") || path.startsWith("/IcyKokane/AIDaoPublic/"));
            return secure && trustedHost && trustedRepo ? uri.toString() : FALLBACK_DOWNLOAD_PAGE;
        } catch (Exception ignored) {
            return FALLBACK_DOWNLOAD_PAGE;
        }
    }

    private String jsonValue(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
        return matcher.find() ? matcher.group(1).replace("\\/", "/").replace("\\n", "\n") : null;
    }

    private int compareVersions(String a, String b) {
        try {
            String[] aa = a.replaceAll("[^0-9.]", "").split("\\.");
            String[] bb = b.replaceAll("[^0-9.]", "").split("\\.");
            int count = Math.max(aa.length, bb.length);
            for (int i = 0; i < count; i++) {
                int av = i < aa.length && !aa[i].isEmpty() ? Integer.parseInt(aa[i]) : 0;
                int bv = i < bb.length && !bb[i].isEmpty() ? Integer.parseInt(bb[i]) : 0;
                if (av != bv) return Integer.compare(av, bv);
            }
            return 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private View message(String author, String body) {
        LinearLayout row = row();
        row.setGravity(Gravity.TOP);
        TextView avatar = text("A", 17, Color.WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(BLUE, 12));
        row.addView(avatar, margins(dp(42), dp(42), 0, 0, dp(11), 0));
        LinearLayout bubble = panel(PANEL);
        bubble.setPadding(dp(14), dp(12), dp(14), dp(12));
        bubble.addView(text(author + "  ·  now", 12, Color.rgb(208, 212, 226), true));
        bubble.addView(text(body, 14, Color.WHITE, false), margins(-1, -2, 0, dp(6), 0, 0));
        row.addView(bubble, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private View buildEvent(String title, String state, int stateColor) {
        LinearLayout row = panel(PANEL_ALT);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(11), dp(14), dp(11));
        TextView dot = text("●", 11, stateColor, true);
        row.addView(dot, margins(dp(28), dp(28), 0, 0, dp(7), 0));
        LinearLayout info = column();
        info.addView(text(title, 13, Color.WHITE, true));
        info.addView(text(state, 11, MUTED, false));
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private View activity(String mark, int color, String title, String detail) {
        LinearLayout row = panel(PANEL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setMinimumHeight(dp(58));
        TextView icon = text(mark, 15, color, true);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, margins(dp(36), dp(36), 0, 0, dp(10), 0));
        LinearLayout labels = column();
        labels.addView(text(title, 14, Color.WHITE, true));
        labels.addView(text(detail, 11, MUTED, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private void hideKeyboard(View view) {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        } catch (Exception ignored) { }
    }

    private Button primary(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(14);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans", Typeface.BOLD));
        b.setMinHeight(dp(48));
        b.setBackground(round(Color.rgb(83, 91, 242), 12));
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setLineSpacing(0, 1.08f);
        return t;
    }

    private LinearLayout panel(int color) {
        LinearLayout p = column();
        p.setPadding(dp(16), dp(15), dp(16), dp(15));
        p.setBackground(round(color, 14));
        return p;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable stroke(int fill, int line, int radiusDp) {
        GradientDrawable d = round(fill, radiusDp);
        d.setStroke(dp(1), line);
        return d;
    }

    private LinearLayout.LayoutParams margins(int w, int h, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(left, top, right, bottom);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
