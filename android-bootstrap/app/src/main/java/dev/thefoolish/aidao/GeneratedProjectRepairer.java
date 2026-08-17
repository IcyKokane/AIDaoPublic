package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;

/** Applies one narrowly-scoped repair pass to generated source after a real CI failure. */
final class GeneratedProjectRepairer {
    static final class RepairResult {
        final GeneratedProject project; final String action; final boolean changed;
        RepairResult(GeneratedProject project,String action,boolean changed){this.project=project;this.action=action;this.changed=changed;}
    }

    RepairResult repair(GeneratedProject original,String failureSummary){
        if(original==null)return new RepairResult(null,"No generated project was available to repair.",false);
        String f=failureSummary==null?"":failureSummary.toLowerCase();
        List<GeneratedProject.FileEntry> out=new ArrayList<>();boolean changed=false;String action;
        if(f.contains("android.widget")||f.contains("javac")||f.contains("compiledebugjava")||f.contains("package android.widget")){
            action="Repair generated Java type qualification and reject repeated Android package prefixes.";
            for(GeneratedProject.FileEntry e:original.files){String c=e.content;String n=sanitizeJava(e.path,c);changed|=!n.equals(c);out.add(new GeneratedProject.FileEntry(e.path,n,e.taskHint));}
        }else if(f.contains("resource")||f.contains("style")||f.contains("manifest")){
            action="Repair Android theme/resource linkage only.";
            for(GeneratedProject.FileEntry e:original.files){String c=e.content;
                if(e.path.equals("app/src/main/AndroidManifest.xml")&&!c.contains("android:theme=\"@style/AppTheme\"")){c=c.replace("<application ","<application android:theme=\"@style/AppTheme\" ");changed=true;}
                String n=sanitizeJava(e.path,c);changed|=!n.equals(c);out.add(new GeneratedProject.FileEntry(e.path,n,e.taskHint));}
        }else if(f.contains("gradle")||f.contains("plugin")){
            action="Repair generated Gradle plugin/toolchain compatibility only.";
            for(GeneratedProject.FileEntry e:original.files){String c=e.content;
                if(e.path.equals("build.gradle.kts")){String n=c.replace("version \"8.8.","version \"8.7.");changed|=!n.equals(c);c=n;}
                String n=sanitizeJava(e.path,c);changed|=!n.equals(c);out.add(new GeneratedProject.FileEntry(e.path,n,e.taskHint));}
        }else{
            action="Normalize generated Java source and regenerate deterministic source without changing the plan, repository default branch, or user permissions.";
            for(GeneratedProject.FileEntry e:original.files){String n=sanitizeJava(e.path,e.content);changed|=!n.equals(e.content);out.add(new GeneratedProject.FileEntry(e.path,n,e.taskHint));}
        }
        return new RepairResult(new GeneratedProject(original.projectName,original.packageName,out,original.verificationNotes),action,changed);
    }

    private static String sanitizeJava(String path,String source){
        if(source==null)return "";
        if(path==null||!path.endsWith(".java"))return source;
        String out=source;
        while(out.contains("android.widget.android.widget."))out=out.replace("android.widget.android.widget.","android.widget.");
        while(out.contains("android.graphics.android.graphics."))out=out.replace("android.graphics.android.graphics.","android.graphics.");
        while(out.contains("android.content.android.content."))out=out.replace("android.content.android.content.","android.content.");
        while(out.contains("android.app.android.app."))out=out.replace("android.app.android.app.","android.app.");
        return out;
    }
}
