from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
V5 = ROOT / "android-bootstrap/app/src/main/java/dev/thefoolish/aidao/AIDaoActivityV5.java"
V6 = ROOT / "android-bootstrap/app/src/main/java/dev/thefoolish/aidao/AIDaoActivityV6.java"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


v5 = V5.read_text()
old_inspect = '''    private void inspectFile(String path){GeneratedProject g=regenerateWithOverrides();GeneratedProject.FileEntry f=g.find(path);if(f==null)return;LinearLayout box=col();box.setPadding(dp(18),dp(4),dp(18),0);TextView pathView=text(path,12,BLUE,true);box.addView(pathView);EditText src=new EditText(this);src.setTypeface(android.graphics.Typeface.MONOSPACE);src.setText(f.content);src.setTextSize(11);src.setMinLines(14);src.setGravity(Gravity.TOP|Gravity.START);src.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);box.addView(src,new LinearLayout.LayoutParams(-1,dp(420)));new AlertDialog.Builder(this).setTitle("Inspect / edit generated file").setMessage("Edits remain local project overrides and are included in the next explicit GitHub build. AIDao will not execute this text on-device.").setView(box).setPositiveButton("Save Override",(d,w)->{prefs.edit().putString("override::"+path,src.getText().toString()).putString("stage","SOURCE MODIFIED").remove("ci_state").remove("run_url").apply();setLast(TaskExecutionState.VERIFYING);workspace();}).setNeutralButton("Reset File",(d,w)->{prefs.edit().remove("override::"+path).apply();workspace();}).setNegativeButton("Close",null).show();}'''
new_inspect = '''    private void inspectFile(String path){GeneratedProject g=regenerateWithOverrides();GeneratedProject.FileEntry f=g.find(path);GeneratedProject base=generateBaseProject();GeneratedProject.FileEntry baseline=base.find(path);if(f==null||baseline==null)return;LinearLayout box=col();box.setPadding(dp(18),dp(4),dp(18),0);TextView pathView=text(path,12,BLUE,true);box.addView(pathView);EditText src=new EditText(this);src.setTypeface(android.graphics.Typeface.MONOSPACE);src.setText(f.content);src.setTextSize(11);src.setMinLines(14);src.setGravity(Gravity.TOP|Gravity.START);src.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);box.addView(src,new LinearLayout.LayoutParams(-1,dp(420)));new AlertDialog.Builder(this).setTitle("Inspect / edit generated file").setMessage("Edits remain local project overrides and are included in the next explicit GitHub build. AIDao records the generation baseline so later refinements cannot silently overwrite or misapply this edit.").setView(box).setPositiveButton("Save Override",(d,w)->{prefs.edit().putString("override::"+path,src.getText().toString()).putString("override-base::"+path,ProjectRevisionLedger.hash(baseline.content)).putString("stage","SOURCE MODIFIED").remove("ci_state").remove("run_url").apply();setLast(TaskExecutionState.VERIFYING);workspace();}).setNeutralButton("Reset File",(d,w)->{prefs.edit().remove("override::"+path).remove("override-base::"+path).apply();workspace();}).setNegativeButton("Close",null).show();}'''
v5 = replace_once(v5, old_inspect, new_inspect, "V5 inspectFile")

old_clear = '''    private void confirmClearOverrides(){new AlertDialog.Builder(this).setTitle("Clear manual edits?").setMessage("Generated source will return to AIDao's deterministic output. Your brief, plan, and directed knowledge are preserved.").setPositiveButton("Clear",(d,w)->{SharedPreferences.Editor e=prefs.edit();for(String k:new HashSet<>(prefs.getAll().keySet()))if(k.startsWith("override::"))e.remove(k);e.apply();workspace();}).setNegativeButton("Cancel",null).show();}'''
new_clear = '''    private void confirmClearOverrides(){new AlertDialog.Builder(this).setTitle("Clear manual edits?").setMessage("Generated source will return to AIDao's deterministic output. Your brief, plan, and directed knowledge are preserved.").setPositiveButton("Clear",(d,w)->{SharedPreferences.Editor e=prefs.edit();for(String k:new HashSet<>(prefs.getAll().keySet()))if(k.startsWith("override::")||k.startsWith("override-base::"))e.remove(k);e.apply();workspace();}).setNegativeButton("Cancel",null).show();}'''
v5 = replace_once(v5, old_clear, new_clear, "V5 clear overrides")

old_regen = '''    private GeneratedProject regenerateWithOverrides(){GeneratedProject base=new LocalSourceGenerator().generate(project,prefs.getString("brief",""),decode(prefs.getString("requirements","")),decode(prefs.getString("tasks","")));List<GeneratedProject.FileEntry> files=new ArrayList<>();for(GeneratedProject.FileEntry f:base.files){String override=prefs.getString("override::"+f.path,null);files.add(new GeneratedProject.FileEntry(f.path,override==null?f.content:override,f.taskHint));}return new GeneratedProject(base.projectName,base.packageName,files,verifyOverrides(base,files));}
    private List<String> verifyOverrides(GeneratedProject base,List<GeneratedProject.FileEntry> files){List<String> notes=new ArrayList<>(base.verificationNotes);int edited=0;for(GeneratedProject.FileEntry f:files)if(hasOverride(f.path))edited++;if(edited>0)notes.add("PASS "+edited+" user-controlled source override(s) included; CI remains authoritative verification");return notes;}'''
new_regen = '''    private GeneratedProject generateBaseProject(){return new LocalSourceGenerator().generate(project,prefs.getString("brief",""),decode(prefs.getString("requirements","")),decode(prefs.getString("tasks","")));}
    private GeneratedProject regenerateWithOverrides(){GeneratedProject base=generateBaseProject();Map<String,String> overrides=new HashMap<>(),bases=new HashMap<>();for(String k:new HashSet<>(prefs.getAll().keySet())){if(k.startsWith("override::")){String v=prefs.getString(k,null);if(v!=null)overrides.put(k.substring("override::".length()),v);}else if(k.startsWith("override-base::")){String v=prefs.getString(k,null);if(v!=null)bases.put(k.substring("override-base::".length()),v);}}GeneratedProjectOverrideResolver.Resolution resolution=new GeneratedProjectOverrideResolver().resolve(base,overrides,bases);return resolution.project;}'''
v5 = replace_once(v5, old_regen, new_regen, "V5 regenerateWithOverrides")
V5.write_text(v5)

v6 = V6.read_text()
old_v6 = '''    private GeneratedProject regenerateWithOverrides(){
        String project=prefs.getString("project_name","Project");
        GeneratedProject base=new LocalSourceGenerator().generate(project,prefs.getString("brief",""),decode(prefs.getString("requirements","")),decode(prefs.getString("tasks","")));
        List<GeneratedProject.FileEntry> files=new ArrayList<>();
        int edited=0;
        for(GeneratedProject.FileEntry f:base.files){String override=prefs.getString("override::"+f.path,null);if(override!=null)edited++;files.add(new GeneratedProject.FileEntry(f.path,override==null?f.content:override,f.taskHint));}
        List<String> notes=new ArrayList<>(base.verificationNotes);if(edited>0)notes.add("PASS "+edited+" user-controlled source override(s) included; trusted CI remains authoritative verification");
        return new GeneratedProject(base.projectName,base.packageName,files,notes);
    }'''
new_v6 = '''    private GeneratedProject regenerateWithOverrides(){
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
    }'''
v6 = replace_once(v6, old_v6, new_v6, "V6 regenerateWithOverrides")
V6.write_text(v6)

print("Applied workspace revision-safety patch")
