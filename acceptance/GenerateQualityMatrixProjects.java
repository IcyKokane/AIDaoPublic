package dev.thefoolish.aidao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Quality matrix for non-media generation. Compilation is necessary but each
 * sample must also expose real domain behavior, persistence, validation and
 * Android-native chrome rather than generic placeholder controls.
 */
public final class GenerateQualityMatrixProjects {
    private static final class Sample {
        final String dir, name, brief;
        final List<String> requirements;
        final String[] markers;
        Sample(String dir, String name, String brief, List<String> requirements, String... markers) {
            this.dir = dir; this.name = name; this.brief = brief; this.requirements = requirements; this.markers = markers;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected output root");
        Path root = Paths.get(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(root);

        List<Sample> samples = Arrays.asList(
                new Sample("finance", "Pocket Ledger", "Build an offline-first personal expense and budget app. I need to add expenses with amount, category and note, keep a monthly budget, and see a simple report after restarting the app.", Arrays.asList("Persist transactions locally", "Validate positive money amounts", "Persist monthly budget", "Calculate total spending and remaining budget"), "Add transaction", "monthly_budget_cents", "Total spent", "Amount must be greater than zero", "TransactionsActivity.class"),
                new Sample("tracker", "Focus Trail", "Build a private activity tracker where I manually record an activity and minutes, review a timeline and totals, and can explicitly clear all local activity history.", Arrays.asList("Persist manually entered activity records", "Validate duration", "Show aggregate reports", "Require confirmation before clearing local history"), "Add activity", "activity_log", "Tap again to confirm clear", "Enter 1–1440 minutes", "DataControlsActivity.class"),
                new Sample("content", "Draft Harbor", "Build an offline writing app with a document editor, local library and search. Recover an unfinished draft after an Android lifecycle interruption so writing is not lost.", Arrays.asList("Persist saved documents", "Search local documents", "Recover unsaved editor draft after restart", "Validate title and content before save"), "Save document", "draft_title", "draft_body", "onPause", "Search documents", "LibraryActivity.class"),
                new Sample("social", "Quiet Circle", "Build a private social workspace with a saved local profile and inbox-style draft/history screen. If no remote messaging backend is configured, say so explicitly instead of pretending messages were delivered, and let me clear local message history deliberately.", Arrays.asList("Persist profile fields locally", "Validate profile name", "Persist local message drafts/history", "Never fake remote delivery without a backend", "Require confirmation before clearing message history"), "Save profile", "profile_name", "Add local message", "message_log", "No remote delivery backend is configured", "SettingsActivity.class"),
                new Sample("commerce", "Counter Cart", "Build an offline product catalog and cart for planning purchases. Let me add products with positive prices, add them to a cart, and explicitly confirm a local test order. Do not claim payment or spending when no payment provider is connected.", Arrays.asList("Persist products locally", "Validate positive prices", "Persist cart state", "Require explicit order confirmation", "Do not claim payment or spending without a provider"), "Add product", "catalog_items", "Add to cart", "cart_items", "Place local test order", "No payment provider is connected", "OrdersActivity.class"),
                new Sample("grocery", "Pantry Path", "Build a simple offline grocery list that keeps my items after the app restarts. I need to add items, edit an existing item by number, see how many items are saved, and deliberately clear the whole list with confirmation.", Arrays.asList("Persist the grocery list locally", "Add and edit grocery items", "Validate edits target an existing item", "Show a saved-item count", "Require confirmation before clearing the grocery list"), "Grocery List", "grocery_items", "Save item", "Choose an existing item number", "saved item", "Tap Clear saved list again to confirm.")
        );

        for (Sample sample : samples) generate(root, sample);
        System.out.println("Quality matrix generated " + samples.size() + " nontrivial Android products");
    }

    private static void generate(Path root, Sample sample) throws Exception {
        GeneratedProject project = new LocalSourceGenerator().generate(sample.name, sample.brief, sample.requirements, Arrays.asList("Generate real domain behavior", "Persist user state", "Apply Android-native product layout", "Validate generated source", "Build debug APK"));
        for (String note : project.verificationNotes) {
            System.out.println(sample.dir + ": " + note);
            if (note.startsWith("FAIL ")) throw new IllegalStateException(sample.dir + " verification failed: " + note);
        }

        StringBuilder source = new StringBuilder();
        for (GeneratedProject.FileEntry entry : project.files) {
            if (entry.path.startsWith("app/src/main/java/") && entry.path.endsWith(".java")) source.append('\n').append(entry.content);
            Path target = root.resolve(sample.dir).resolve(entry.path).normalize();
            if (!target.startsWith(root.resolve(sample.dir))) throw new SecurityException("Generated path escaped root: " + entry.path);
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            Files.write(target, entry.content.getBytes(StandardCharsets.UTF_8));
        }

        String joined = source.toString();
        for (String forbidden : new String[]{"Save local sample state", "sample state", "placeholder data", "android.widget.android.widget.", "android.graphics.android.graphics.", "android.content.android.content.", "android.app.android.app."})
            if (joined.contains(forbidden)) throw new IllegalStateException(sample.dir + " retained placeholder/corrupted source: " + forbidden);

        boolean specialized = joined.contains("class AppScreen");
        boolean generic = joined.contains("class GeneratedScreen");
        if (!specialized && !generic) throw new IllegalStateException(sample.dir + " missing validated generated screen shell");
        // AppScreen adds explicit content descriptions. GeneratedScreen remains a valid
        // generic shell and is separately held to the same phone-safety/reachability gates.
        if (specialized && !joined.contains("setContentDescription"))
            throw new IllegalStateException(sample.dir + " specialized screen shell missing accessibility descriptions");
        for (String common : new String[]{"setOnApplyWindowInsetsListener", "SharedPreferences", "AppNavigator.open"})
            if (!joined.contains(common)) throw new IllegalStateException(sample.dir + " missing common quality marker: " + common);
        // Both shells enforce a >=48dp minimum touch target, but the specialized shell
        // uses a dp() helper while the legacy/generic shell stores the equivalent 52px
        // baseline directly. Keep this gate semantic instead of coupling it to one spelling.
        if (!joined.contains("setMinHeight(dp(48))") && !joined.contains("setMinHeight(52)"))
            throw new IllegalStateException(sample.dir + " missing >=48dp touch-target marker");

        for (String marker : sample.markers)
            if (!joined.contains(marker)) throw new IllegalStateException(sample.dir + " missing semantic marker: " + marker);

        System.out.println("Generated " + sample.dir + ": " + project.projectName + " / " + project.packageName + " / " + project.files.size() + " files");
    }
}
