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
        if(f.contains("resource")||f.contains("style")||f.contains("manifest")){
            action="Repair Android theme/resource linkage only.";
            for(GeneratedProject.FileEntry e:original.files){String c=e.content;
                if(e.path.equals("app/src/main/AndroidManifest.xml")&&!c.contains("android:theme=\"@style/AppTheme\"")){c=c.replace("<application ","<application android:theme=\"@style/AppTheme\" ");changed=true;}
                out.add(new GeneratedProject.FileEntry(e.path,c,e.taskHint));}
        }else if(f.contains("gradle")||f.contains("plugin")){
            action="Repair generated Gradle plugin/toolchain compatibility only.";
            for(GeneratedProject.FileEntry e:original.files){String c=e.content;
                if(e.path.equals("build.gradle.kts")){String n=c.replace("version \"8.8.","version \"8.7.");changed|=!n.equals(c);c=n;}
                out.add(new GeneratedProject.FileEntry(e.path,c,e.taskHint));}
        }else{
            action="Regenerate deterministic source without changing the plan, repository default branch, or user permissions.";
            out.addAll(original.files);
        }
        return new RepairResult(new GeneratedProject(original.projectName,original.packageName,out,original.verificationNotes),action,changed);
    }
}
