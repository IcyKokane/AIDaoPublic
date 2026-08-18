package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;

/** Final cleanup for products that were materially specialized from generic domain scaffolds. */
final class ExplicitRequestCleanupPostProcessor {
    static final class Result {
        final String projectName, packageName;
        final List<GeneratedProject.FileEntry> files;
        final List<String> notes;
        Result(String projectName,String packageName,List<GeneratedProject.FileEntry> files,List<String> notes){
            this.projectName=projectName;this.packageName=packageName;this.files=files;this.notes=notes;
        }
    }

    static Result process(String projectName,String packageName,List<GeneratedProject.FileEntry> incoming){
        if (!"QuestFit".equals(projectName) && !"LockNote".equals(projectName))
            return new Result(projectName,packageName,incoming,new ArrayList<>());
        List<GeneratedProject.FileEntry> out=new ArrayList<>();
        for(GeneratedProject.FileEntry f:incoming){
            if(f==null)continue;
            String c=f.content==null?"":f.content;
            if(c.contains("Save local sample state")) continue;
            if("QuestFit".equals(projectName) && isGenericWorkoutSurface(f.path)) continue;
            if("app/src/main/AndroidManifest.xml".equals(f.path) && "QuestFit".equals(projectName)){
                c=questFitManifest();
            }
            out.add(new GeneratedProject.FileEntry(f.path,c,f.taskHint));
        }
        List<String> notes=new ArrayList<>();
        notes.add("PASS specialized request cleanup removed unused generic/sample completion surfaces");
        if("QuestFit".equals(projectName)) notes.add("PASS QuestFit manifest exposes only the generated workout/stat/history surfaces");
        return new Result(projectName,packageName,out,notes);
    }

    private static boolean isGenericWorkoutSurface(String p){
        if(p==null)return false;
        return p.endsWith("/ExploreActivity.java") || p.endsWith("/DetailActivity.java") ||
                p.endsWith("/SettingsActivity.java") || p.endsWith("/DomainRecord.java");
    }

    private static String questFitManifest(){
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"+
                "  <application android:theme=\"@style/AppTheme\" android:label=\"QuestFit\" android:allowBackup=\"true\" android:supportsRtl=\"true\">\n"+
                "    <activity android:name=\".TimelineActivity\" android:exported=\"false\"/>\n"+
                "    <activity android:name=\".ReportsActivity\" android:exported=\"false\"/>\n"+
                "    <activity android:name=\".DataControlsActivity\" android:exported=\"false\"/>\n"+
                "    <activity android:name=\".MainActivity\" android:exported=\"true\">\n"+
                "      <intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter>\n"+
                "    </activity>\n"+
                "  </application>\n</manifest>\n";
    }

    private ExplicitRequestCleanupPostProcessor(){}
}
