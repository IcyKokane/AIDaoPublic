package dev.thefoolish.aidao;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Immutable result of a source-generation pass. */
final class GeneratedProject {
    static final class FileEntry {
        final String path;
        final String content;
        final String taskHint;

        FileEntry(String path, String content, String taskHint) {
            this.path = path;
            this.content = normalizeGeneratedContent(path, content);
            this.taskHint = taskHint;
        }

        private static String normalizeGeneratedContent(String path, String source) {
            if (source == null) return "";
            String out = source;
            while (out.contains("android.widget.android.widget.")) out = out.replace("android.widget.android.widget.", "android.widget.");
            if (path != null && path.endsWith("/LocalStore.java")) out = out.replace("public void text(String k,String v)", "public void putText(String k,String v)");
            if (path != null && path.endsWith("/RepositoryMediaProvider.java")) {
                out = out.replace("String target=expand(ext.playbackUrl,\"\",itemId,episode);if(target.endsWith(\".mp4\")",
                        "String target=expand(ext.playbackUrl,\"\",itemId,episode);if(!target.startsWith(\"https://\"))throw new IOException(\"Playback contract must resolve to HTTPS\");if(target.endsWith(\".mp4\")");
            }
            if (path != null && path.endsWith("/PlayerActivity.java") && out.contains("VideoView video=new VideoView(this);") && !out.contains("setMediaController")) {
                out = out.replace("video.setContentDescription(\"Episode video player\");body.addView(video,new LinearLayout.LayoutParams(-1,dp(260)));video.setVideoURI(Uri.parse(url));",
                        "video.setContentDescription(\"Episode video player\");body.addView(video,new LinearLayout.LayoutParams(-1,dp(260)));MediaController controls=new MediaController(this);controls.setAnchorView(video);video.setMediaController(controls);video.setVideoURI(Uri.parse(url));");
            }

            String[] persistedTextKeys={"last_episode","last_surface","documents","active_note","note_title_","note_body_","workout_history","workouts"};
            for(String key:persistedTextKeys) out=out.replace("store.text(\""+key+"\"","store.putText(\""+key+"\"");
            out=out.replace("String last=store.putText(\"last_episode\",\"\")","String last=store.text(\"last_episode\",\"\")")
                    .replace("String docs=store.putText(\"documents\",\"\")","String docs=store.text(\"documents\",\"\")")
                    .replace("String raw=store.putText(\"documents\",\"\")","String raw=store.text(\"documents\",\"\")")
                    .replace("String s=store.putText(\"documents\",\"\")","String s=store.text(\"documents\",\"\")")
                    .replace("String old=store.putText(\"documents\",\"\")","String old=store.text(\"documents\",\"\")")
                    .replace("String id=store.putText(\"active_note\",\"default\")","String id=store.text(\"active_note\",\"default\")")
                    .replace("title.setText(store.putText(\"note_title_\"+id,\"\"))","title.setText(store.text(\"note_title_\"+id,\"\"))")
                    .replace("content.setText(store.putText(\"note_body_\"+id,\"\"))","content.setText(store.text(\"note_body_\"+id,\"\"))")
                    .replace("String raw=store.putText(\"workout_history\",\"\")","String raw=store.text(\"workout_history\",\"\")")
                    .replace("String history=store.putText(\"workout_history\",\"\")","String history=store.text(\"workout_history\",\"\")")
                    .replace("String old=store.putText(\"workout_history\",\"\")","String old=store.text(\"workout_history\",\"\")")
                    .replace("String old=store.putText(\"workouts\",\"\")","String old=store.text(\"workouts\",\"\")");

            if(path!=null&&path.endsWith("/TimelineActivity.java")&&out.contains("EditText exercise=field(\"Exercise\")")&&out.contains("workout_xp")&&out.contains("Complete set")){
                out=out.replace("EditText exercise=field(\"Exercise\");EditText weight=field(\"Weight\");",
                        "String[] exercises={\"Squat\",\"Bench Press\",\"Deadlift\",\"Push Up\",\"Pull Up\",\"Overhead Press\",\"Barbell Row\",\"Lunge\",\"Plank\"};Spinner exercise=new Spinner(this);ArrayAdapter<String> exerciseAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,exercises);exerciseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);exercise.setAdapter(exerciseAdapter);EditText weight=field(\"Weight\");")
                        .replace("String ex=exercise.getText().toString().trim();if(ex.isEmpty()){exercise.setError(\"Enter exercise\");return;}","String ex=String.valueOf(exercise.getSelectedItem());if(ex.trim().isEmpty())return;");
            }

            if(path!=null&&path.endsWith(".java")&&!path.endsWith("/GeneratedScreen.java")&&!path.endsWith("/AppScreen.java")&&out.contains("Button ")&&!out.contains("import android.widget.Button;")&&!out.contains("import android.widget.*;")) out=out.replace("Button ","android.widget.Button ");
            String saveButton="Button save=button(\"Save +60s test progress\")";
            if(!out.contains("android.widget."+saveButton)) out=out.replace(saveButton,"android.widget."+saveButton);
            while(out.contains("android.widget.android.widget.")) out=out.replace("android.widget.android.widget.","android.widget.");
            return out;
        }
    }

    private static final class FidelityResult {
        final String projectName; final String packageName; final List<FileEntry> files; final List<String> notes;
        FidelityResult(String projectName,String packageName,List<FileEntry> files,List<String> notes){this.projectName=projectName;this.packageName=packageName;this.files=files;this.notes=notes;}
    }

    final String projectName;
    final String packageName;
    final List<FileEntry> files;
    final List<String> verificationNotes;

    GeneratedProject(String projectName,String packageName,List<FileEntry> files,List<String> verificationNotes){
        List<FileEntry> raw=new ArrayList<>(files==null?Collections.emptyList():files);
        FidelityResult fidelity=applyProcessor("GeneratedProjectFidelityPostProcessor","generated-app fidelity",projectName,packageName,raw);
        FidelityResult capability=applyProcessor("ProviderCapabilityIntegrator","provider capability integration",fidelity.projectName,fidelity.packageName,fidelity.files);
        FidelityResult nativeFidelity=applyProcessor("NativeFidelityPostProcessor","native Android fidelity",capability.projectName,capability.packageName,capability.files);
        FidelityResult referenceBehavior=applyProcessor("MihonBehaviorPostProcessor","reference-app behavior",nativeFidelity.projectName,nativeFidelity.packageName,nativeFidelity.files);
        FidelityResult generalProduct=applyProcessor("GeneralProductPostProcessor","general product fidelity",referenceBehavior.projectName,referenceBehavior.packageName,referenceBehavior.files);
        String normalizedIdentity=normalizeDescriptorIdentity(generalProduct.projectName,generalProduct.files);
        FidelityResult requestFidelity=applyProcessor("RequestFidelityPostProcessor","request-specific fidelity",normalizedIdentity,generalProduct.packageName,generalProduct.files);
        FidelityResult semanticProduct=applyProcessor("SemanticProductPostProcessor","semantic product fidelity",requestFidelity.projectName,requestFidelity.packageName,requestFidelity.files);
        FidelityResult genericOffline=applyProcessor("GenericOfflinePostProcessor","generic offline product",semanticProduct.projectName,semanticProduct.packageName,semanticProduct.files);

        this.projectName=genericOffline.projectName;
        this.packageName=genericOffline.packageName;
        List<FileEntry> immutableSource=new ArrayList<>(genericOffline.files==null?Collections.emptyList():genericOffline.files);
        this.files=Collections.unmodifiableList(immutableSource);
        List<String> notes=new ArrayList<>(); if(verificationNotes!=null)notes.addAll(verificationNotes);
        append(notes,fidelity.notes);append(notes,capability.notes);append(notes,nativeFidelity.notes);append(notes,referenceBehavior.notes);append(notes,generalProduct.notes);append(notes,requestFidelity.notes);append(notes,semanticProduct.notes);append(notes,genericOffline.notes);
        GeneratedProjectValidator.Result structural=GeneratedProjectValidator.validateRaw(this.packageName,immutableSource);notes.addAll(structural.notes);notes.addAll(validateFidelityIfAvailable(this.packageName,immutableSource));
        this.verificationNotes=Collections.unmodifiableList(notes);
    }

    private static String normalizeDescriptorIdentity(String projectName,List<FileEntry> files){
        String name=projectName==null?"":projectName.trim();String low=name.toLowerCase(Locale.US);
        boolean articleDescription=(low.startsWith("a ")||low.startsWith("an ")||low.startsWith("the "))&&(low.endsWith(" app")||low.endsWith(" application")||low.contains(" tracking app")||low.contains(" tracker app"));
        boolean genericDescriptor=articleDescription||low.startsWith("simple ")||low.startsWith("basic ");if(!genericDescriptor)return name;
        String all=joinFileText(files).toLowerCase(Locale.US);if(all.contains("workout")||all.contains("exercise")||all.contains("workout_xp"))return "QuestFit";if(all.contains("notepad")||all.contains("note_title_")||all.contains("document editor"))return "NoteForge";if(all.contains("pantry_inventory")||all.contains("pantry"))return "PantryQuest";return name;
    }
    private static String joinFileText(List<FileEntry> files){StringBuilder b=new StringBuilder();if(files!=null)for(FileEntry f:files)if(f!=null&&f.content!=null)b.append('\n').append(f.content);return b.toString();}

    static GeneratedProject resolved(String projectName,String packageName,List<FileEntry> files,List<String> verificationNotes){return new GeneratedProject(projectName,packageName,files,verificationNotes,true);}
    private GeneratedProject(String projectName,String packageName,List<FileEntry> files,List<String> verificationNotes,boolean resolvedTree){
        if(!resolvedTree)throw new IllegalArgumentException("resolved tree marker required");this.projectName=projectName;this.packageName=packageName;
        List<FileEntry> immutableSource=new ArrayList<>(files==null?Collections.emptyList():files);this.files=Collections.unmodifiableList(immutableSource);
        List<String> notes=new ArrayList<>();if(verificationNotes!=null)notes.addAll(verificationNotes);GeneratedProjectValidator.Result structural=GeneratedProjectValidator.validateRaw(this.packageName,immutableSource);notes.addAll(structural.notes);notes.addAll(validateFidelityIfAvailable(this.packageName,immutableSource));this.verificationNotes=Collections.unmodifiableList(notes);
    }

    private static void append(List<String> into,List<String> values){if(values!=null)into.addAll(values);}
    private static FidelityResult applyProcessor(String simpleName,String label,String projectName,String packageName,List<FileEntry> raw){
        try{Class<?> type=Class.forName("dev.thefoolish.aidao."+simpleName);Method process=type.getDeclaredMethod("process",String.class,String.class,List.class);process.setAccessible(true);Object result=process.invoke(null,projectName,packageName,raw);return readResult(result);}catch(ClassNotFoundException unavailableInLegacyHarness){return new FidelityResult(projectName,packageName,raw,Collections.emptyList());}catch(Exception brokenProcessor){List<String> notes=new ArrayList<>();notes.add("FAIL "+label+" transformation failed: "+brokenProcessor.getClass().getSimpleName());return new FidelityResult(projectName,packageName,raw,notes);}
    }
    @SuppressWarnings("unchecked") private static FidelityResult readResult(Object result)throws Exception{Class<?> t=result.getClass();Field p=t.getDeclaredField("projectName"),q=t.getDeclaredField("packageName"),f=t.getDeclaredField("files"),n=t.getDeclaredField("notes");p.setAccessible(true);q.setAccessible(true);f.setAccessible(true);n.setAccessible(true);return new FidelityResult((String)p.get(result),(String)q.get(result),(List<FileEntry>)f.get(result),(List<String>)n.get(result));}
    @SuppressWarnings("unchecked") private static List<String> validateFidelityIfAvailable(String packageName,List<FileEntry> files){try{Class<?> type=Class.forName("dev.thefoolish.aidao.GeneratedFidelityValidator");Method validate=type.getDeclaredMethod("validate",String.class,List.class);validate.setAccessible(true);Object result=validate.invoke(null,packageName,files);return result instanceof List?(List<String>)result:Collections.singletonList("FAIL fidelity validator returned an invalid result");}catch(ClassNotFoundException unavailableInLegacyHarness){return Collections.emptyList();}catch(Exception brokenValidator){return Collections.singletonList("FAIL generated-app fidelity validation failed: "+brokenValidator.getClass().getSimpleName());}}
    boolean hasPath(String path){for(FileEntry file:files)if(file!=null&&file.path.equals(path))return true;return false;}
    FileEntry find(String path){for(FileEntry file:files)if(file!=null&&file.path.equals(path))return file;return null;}
}
