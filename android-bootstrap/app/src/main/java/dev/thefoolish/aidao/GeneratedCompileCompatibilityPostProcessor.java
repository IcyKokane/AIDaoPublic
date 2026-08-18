package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;

/** Final source-compatibility normalization after all product post-processors. */
final class GeneratedCompileCompatibilityPostProcessor {
    static final class Result {
        final String projectName, packageName;
        final List<GeneratedProject.FileEntry> files;
        final List<String> notes;
        Result(String projectName,String packageName,List<GeneratedProject.FileEntry> files,List<String> notes){
            this.projectName=projectName;this.packageName=packageName;this.files=files;this.notes=notes;
        }
    }
    static Result process(String projectName,String packageName,List<GeneratedProject.FileEntry> incoming){
        List<GeneratedProject.FileEntry> out=new ArrayList<>();
        boolean changed=false;
        for(GeneratedProject.FileEntry f:incoming){
            if(f==null)continue;
            String c=f.content==null?"":f.content;
            if(f.path!=null&&f.path.endsWith(".java")){
                String before=c;
                c=c.replace("Button b=button(","android.widget.Button b=button(")
                   .replace("Button toggle=button(","android.widget.Button toggle=button(")
                   .replace("Button save=button(","android.widget.Button save=button(")
                   .replace("Button add=button(","android.widget.Button add=button(")
                   .replace("Button go=button(","android.widget.Button go=button(")
                   .replace("Button open=button(","android.widget.Button open=button(")
                   .replace("Button lock=button(","android.widget.Button lock=button(")
                   .replace("Button repos=button(","android.widget.Button repos=button(")
                   .replace("Button downloads=button(","android.widget.Button downloads=button(")
                   .replace("Button extensions=button(","android.widget.Button extensions=button(")
                   .replace("Button repositories=button(","android.widget.Button repositories=button(")
                   .replace("android.widget.android.widget.Button","android.widget.Button");
                changed|=!before.equals(c);
            }
            out.add(new GeneratedProject.FileEntry(f.path,c,f.taskHint));
        }
        List<String> notes=new ArrayList<>();
        if(changed)notes.add("PASS final generated-source compatibility pass qualified Android Button declarations");
        return new Result(projectName,packageName,out,notes);
    }
    private GeneratedCompileCompatibilityPostProcessor(){}
}
