package dev.thefoolish.aidao;

import java.util.Arrays;
import java.util.List;

/** CI-only acceptance coverage for structural and semantic validation of generated projects. */
public final class GeneratedProjectValidationAcceptance {
    public static void main(String[] args) {
        verifyStructuralValidation();
        verifyOfflineFinanceCoherence();
        verifyHabitCoherence();
        verifyUnsupportedCapabilitiesFailClosed();
        System.out.println("Generated-project structural and product-coherence acceptance passed");
    }

    private static void verifyStructuralValidation() {
        // Keep the baseline structural sample non-media. Media capability/fidelity modules
        // have their own end-to-end workflow and must not mask structural failures.
        List<String> requirements = Arrays.asList(
                "Provide local transactions, monthly budgeting, and reports.",
                "Persist finance state locally and validate user-entered amounts.",
                "Generate multiple Android screens, resources, manifest declarations, and reusable navigation."
        );
        List<String> tasks = Arrays.asList(
                "Generate the Android source tree.",
                "Verify required files and navigation.",
                "Build the generated project with trusted CI."
        );

        GeneratedProject good = new LocalSourceGenerator().generate(
                "AIDao V1 Validation Sample",
                "Create a multi-screen offline expense and budget app with transactions, budget controls, reports, persistence, and input validation.",
                requirements,
                tasks
        );
        GeneratedProjectValidator.Result result = new GeneratedProjectValidator().validate(good);
        for (String note : result.notes) System.out.println(note);
        if (!result.valid) throw new IllegalStateException("Expected generated project to pass structural validation");

        GeneratedProject broken = new GeneratedProject(
                "Broken",
                "dev.thefoolish.generated.broken",
                Arrays.asList(
                        new GeneratedProject.FileEntry("../escape.txt", "bad", "test"),
                        new GeneratedProject.FileEntry("settings.gradle.kts", "", "test"),
                        new GeneratedProject.FileEntry("settings.gradle.kts", "duplicate", "test")
                ),
                Arrays.asList("test")
        );
        GeneratedProjectValidator.Result brokenResult = new GeneratedProjectValidator().validate(broken);
        if (brokenResult.valid) throw new IllegalStateException("Expected unsafe/incomplete project to fail validation");
        boolean unsafeSeen = false, duplicateSeen = false;
        for (String note : brokenResult.notes) {
            if (note.contains("unsafe generated path")) unsafeSeen = true;
            if (note.contains("duplicate generated path")) duplicateSeen = true;
        }
        if (!unsafeSeen || !duplicateSeen) throw new IllegalStateException("Validator did not report expected path failures");
    }

    private static void verifyOfflineFinanceCoherence() {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Pocket Ledger",
                "Build an offline personal expense tracker named Pocket Ledger. Let me add transactions with a category and amount, set a monthly budget, and view reports that total spending by category. Everything must persist after the app restarts.",
                Arrays.asList(
                        "Add expense transactions with category and amount.",
                        "Set and edit a monthly budget.",
                        "Show computed spending reports by category.",
                        "Persist transactions and budget locally across restart.",
                        "Remain fully usable offline."),
                Arrays.asList("Generate expense entry","Generate budget editor","Generate reports","Persist local state")
        );
        assertNoFail(project,"finance");
        String all=all(project).toLowerCase();
        require(all,"transactions","finance transaction persistence");
        require(all,"monthly_budget","finance budget persistence");
        require(all,"save transaction","finance mutation action");
        require(all,"by category","computed category report");
        require(all,"sharedpreferences","restart-safe local persistence");
        verifyProductShell(project,"finance");
        rejectFakeCompletion(all,"finance");
    }

    private static void verifyHabitCoherence() {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Streak Garden",
                "Create an offline habit tracker named Streak Garden. I can create habits, check them off for today, undo a check-in, and see today's completion percentage plus an all-time check-in count. Keep the data after restart.",
                Arrays.asList(
                        "Create custom habits.",
                        "Complete or undo today's check-in.",
                        "Show today's completion percentage.",
                        "Show all-time check-in count.",
                        "Persist habits and progress locally across restart."),
                Arrays.asList("Generate habit editor","Generate check-in controls","Generate progress summary","Persist local state")
        );
        assertNoFail(project,"habit");
        String all=all(project).toLowerCase();
        require(all,"add habit","habit creation action");
        require(all,"complete today","habit check-in action");
        require(all,"undo today","habit undo action");
        require(all,"habit_done_today","daily completion persistence");
        require(all,"habit_checkins","all-time check-in persistence");
        require(all,"%","completion percentage UI");
        verifyProductShell(project,"habit");
        rejectFakeCompletion(all,"habit");
    }

    private static void verifyUnsupportedCapabilitiesFailClosed() {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "Trail Beacon",
                "Build Trail Beacon, an offline hiking journal that must record my live GPS location and send Android notifications when I reach saved locations.",
                Arrays.asList(
                        "Read live GPS location.",
                        "Save geofenced locations.",
                        "Send Android arrival notifications.",
                        "Persist journal entries across restart."),
                Arrays.asList("Generate journal","Implement GPS","Implement geofences","Implement notifications")
        );
        boolean locationFail=false,notificationFail=false;
        for(String note:project.verificationNotes) {
            String low=note.toLowerCase();
            if(note.startsWith("FAIL ")&&low.contains("location")) locationFail=true;
            if(note.startsWith("FAIL ")&&low.contains("notification")) notificationFail=true;
        }
        if(!locationFail||!notificationFail)
            throw new IllegalStateException("unsupported GPS/notification request was not failed closed; location="+locationFail+" notifications="+notificationFail);
    }

    private static void verifyProductShell(GeneratedProject p,String label) {
        String manifest=content(p,"app/src/main/AndroidManifest.xml");
        if(!manifest.contains("android:icon=\"@mipmap/ic_launcher\"")||!manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
            throw new IllegalStateException(label+" launcher icon wiring missing");
        content(p,"app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml");
        content(p,"app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml");
        content(p,"app/src/main/res/drawable/ic_launcher_foreground.xml");
        String all=all(p);
        if(!all.contains("WindowInsets")||!all.contains("systemBars")||!all.contains("ime"))
            throw new IllegalStateException(label+" phone-safe system/IME inset handling missing");
        if(!all.contains("ScrollView")) throw new IllegalStateException(label+" scroll-safe shell missing");
    }

    private static void rejectFakeCompletion(String all,String label) {
        String[] forbidden={"todo: implement","coming soon","placeholder data","sample data only","generated project shell","playback surface placeholder"};
        for(String marker:forbidden) if(all.contains(marker)) throw new IllegalStateException(label+" contains fake-completion marker: "+marker);
    }

    private static void require(String all,String token,String label) {
        if(!all.contains(token.toLowerCase())) throw new IllegalStateException(label+" missing: "+token);
    }

    private static void assertNoFail(GeneratedProject project,String label) {
        for(String note:project.verificationNotes) if(note.startsWith("FAIL ")) throw new IllegalStateException(label+" source generation blocked: "+note);
    }

    private static String content(GeneratedProject p,String path) {
        for(GeneratedProject.FileEntry f:p.files) if(f!=null&&path.equals(f.path)) return f.content==null?"":f.content;
        throw new IllegalStateException("missing generated file "+path);
    }

    private static String all(GeneratedProject p) {
        StringBuilder b=new StringBuilder();
        for(GeneratedProject.FileEntry f:p.files) if(f!=null&&f.content!=null) b.append('\n').append(f.content);
        return b.toString();
    }
}
