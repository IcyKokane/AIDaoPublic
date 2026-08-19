package dev.thefoolish.aidao;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            while (out.contains("android.widget.android.widget.")) {
                out = out.replace("android.widget.android.widget.", "android.widget.");
            }
            if (path != null && path.endsWith("/LocalStore.java")) {
                out = out.replace("public void text(String k,String v)", "public void putText(String k,String v)");
            }
            if (path != null && path.endsWith("/RepositoryMediaProvider.java")) {
                out = out.replace(
                        "String target=expand(ext.playbackUrl,\"\",itemId,episode);if(target.endsWith(\".mp4\")",
                        "String target=expand(ext.playbackUrl,\"\",itemId,episode);if(!target.startsWith(\"https://\"))throw new IOException(\"Playback contract must resolve to HTTPS\");if(target.endsWith(\".mp4\")");
            }

            // The generator historically exposed text(key, default) as the string getter
            // and also emitted text(key, value) for mutations. Once the setter is normalized
            // to putText, mutation call-sites must follow it or they compile as getter calls
            // and silently discard user data. Keep these replacements narrow to known mutation
            // keys so read paths continue using text(key, default).
            String[] persistedTextKeys = {
                    "last_episode", "last_surface", "documents", "active_note",
                    "note_title_", "note_body_", "workout_history", "workouts"
            };
            for (String key : persistedTextKeys) {
                out = out.replace("store.text(\"" + key + "\"", "store.putText(\"" + key + "\"");
            }
            // Restore known getter expressions that share a mutation-key prefix. Content
            // products use several local variable names for the same documents collection,
            // so keep each read form explicit instead of letting the mutation normalization
            // turn a String getter into the void putText setter.
            out = out.replace("String last=store.putText(\"last_episode\",\"\")", "String last=store.text(\"last_episode\",\"\")");
            out = out.replace("String docs=store.putText(\"documents\",\"\")", "String docs=store.text(\"documents\",\"\")");
            out = out.replace("String raw=store.putText(\"documents\",\"\")", "String raw=store.text(\"documents\",\"\")");
            out = out.replace("String s=store.putText(\"documents\",\"\")", "String s=store.text(\"documents\",\"\")");
            out = out.replace("String old=store.putText(\"documents\",\"\")", "String old=store.text(\"documents\",\"\")");
            out = out.replace("String id=store.putText(\"active_note\",\"default\")", "String id=store.text(\"active_note\",\"default\")");
            out = out.replace("title.setText(store.putText(\"note_title_\"+id,\"\"))", "title.setText(store.text(\"note_title_\"+id,\"\"))");
            out = out.replace("content.setText(store.putText(\"note_body_\"+id,\"\"))", "content.setText(store.text(\"note_body_\"+id,\"\"))");
            out = out.replace("String raw=store.putText(\"workout_history\",\"\")", "String raw=store.text(\"workout_history\",\"\")");
            out = out.replace("String history=store.putText(\"workout_history\",\"\")", "String history=store.text(\"workout_history\",\"\")");
            out = out.replace("String old=store.putText(\"workouts\",\"\")", "String old=store.text(\"workouts\",\"\")");

            // When the workout request says exercises should already be in the app,
            // convert the legacy free-text exercise field into a concrete preset catalog.
            // Weight and reps remain user-entered measurements and the existing persistence
            // and RPG progression paths are preserved unchanged.
            if (path != null && path.endsWith("/TimelineActivity.java")
                    && out.contains("EditText exercise=field(\"Exercise\")")
                    && out.contains("workout_xp") && out.contains("Complete set")) {
                out = out.replace(
                        "EditText exercise=field(\"Exercise\");EditText weight=field(\"Weight\");",
                        "String[] exercises={\"Squat\",\"Bench Press\",\"Deadlift\",\"Push Up\",\"Pull Up\",\"Overhead Press\",\"Barbell Row\",\"Lunge\",\"Plank\"};Spinner exercise=new Spinner(this);ArrayAdapter<String> exerciseAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,exercises);exerciseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);exercise.setAdapter(exerciseAdapter);EditText weight=field(\"Weight\");");
                out = out.replace(
                        "String ex=exercise.getText().toString().trim();if(ex.isEmpty()){exercise.setError(\"Enter exercise\");return;}",
                        "String ex=String.valueOf(exercise.getSelectedItem());if(ex.trim().isEmpty())return;");
            }

            // Request-specific compact home screens may use Button locals without importing
            // android.widget.Button. Imports are not inherited from AppScreen, so qualify the
            // type here before any generated project reaches the Gradle build gate.
            if (path != null && path.endsWith("/MainActivity.java")
                    && out.contains("Button ")
                    && !out.contains("import android.widget.Button;")
                    && !out.contains("import android.widget.*;")) {
                out = out.replace("Button ", "android.widget.Button ");
            }

            String saveButton = "Button save=button(\"Save +60s test progress\")";
            if (!out.contains("android.widget." + saveButton)) {
                out = out.replace(saveButton, "android.widget." + saveButton);
            }
            while (out.contains("android.widget.android.widget.")) {
                out = out.replace("android.widget.android.widget.", "android.widget.");
            }
            return out;
        }
    }

    private static final class FidelityResult {
        final String projectName;
        final String packageName;
        final List<FileEntry> files;
        final List<String> notes;
        FidelityResult(String projectName, String packageName, List<FileEntry> files, List<String> notes) {
            this.projectName = projectName;
            this.packageName = packageName;
            this.files = files;
            this.notes = notes;
        }
    }

    final String projectName;
    final String packageName;
    final List<FileEntry> files;
    final List<String> verificationNotes;

    GeneratedProject(String projectName, String packageName, List<FileEntry> files, List<String> verificationNotes) {
        List<FileEntry> raw = new ArrayList<>(files == null ? Collections.emptyList() : files);
        FidelityResult fidelity = applyFidelityIfAvailable(projectName, packageName, raw);
        FidelityResult capability = applyProviderCapabilityIntegrationIfAvailable(
                fidelity.projectName, fidelity.packageName, fidelity.files);
        FidelityResult nativeFidelity = applyNativeFidelityIfAvailable(
                capability.projectName, capability.packageName, capability.files);
        FidelityResult referenceBehavior = applyMihonBehaviorIfAvailable(
                nativeFidelity.projectName, nativeFidelity.packageName, nativeFidelity.files);
        FidelityResult generalProduct = applyGeneralProductIfAvailable(
                referenceBehavior.projectName, referenceBehavior.packageName, referenceBehavior.files);
        FidelityResult requestFidelity = applyRequestFidelityIfAvailable(
                generalProduct.projectName, generalProduct.packageName, generalProduct.files);

        this.projectName = requestFidelity.projectName;
        this.packageName = requestFidelity.packageName;
        List<FileEntry> immutableSource = new ArrayList<>(requestFidelity.files == null ? Collections.emptyList() : requestFidelity.files);
        this.files = Collections.unmodifiableList(immutableSource);

        List<String> notes = new ArrayList<>();
        if (verificationNotes != null) notes.addAll(verificationNotes);
        if (fidelity.notes != null) notes.addAll(fidelity.notes);
        if (capability.notes != null) notes.addAll(capability.notes);
        if (nativeFidelity.notes != null) notes.addAll(nativeFidelity.notes);
        if (referenceBehavior.notes != null) notes.addAll(referenceBehavior.notes);
        if (generalProduct.notes != null) notes.addAll(generalProduct.notes);
        if (requestFidelity.notes != null) notes.addAll(requestFidelity.notes);

        GeneratedProjectValidator.Result structural = GeneratedProjectValidator.validateRaw(this.packageName, immutableSource);
        notes.addAll(structural.notes);
        notes.addAll(validateFidelityIfAvailable(this.packageName, immutableSource));
        this.verificationNotes = Collections.unmodifiableList(notes);
    }

    /**
     * Wrap an already-transformed/resolved source tree without running product
     * post-processors a second time. This is required after user overrides or
     * bounded CI repairs so unrelated transformations cannot overwrite edits.
     */
    static GeneratedProject resolved(String projectName, String packageName, List<FileEntry> files, List<String> verificationNotes) {
        return new GeneratedProject(projectName, packageName, files, verificationNotes, true);
    }

    private GeneratedProject(String projectName, String packageName, List<FileEntry> files, List<String> verificationNotes, boolean resolvedTree) {
        if (!resolvedTree) throw new IllegalArgumentException("resolved tree marker required");
        this.projectName = projectName;
        this.packageName = packageName;
        List<FileEntry> immutableSource = new ArrayList<>(files == null ? Collections.emptyList() : files);
        this.files = Collections.unmodifiableList(immutableSource);
        List<String> notes = new ArrayList<>();
        if (verificationNotes != null) notes.addAll(verificationNotes);
        GeneratedProjectValidator.Result structural = GeneratedProjectValidator.validateRaw(this.packageName, immutableSource);
        notes.addAll(structural.notes);
        notes.addAll(validateFidelityIfAvailable(this.packageName, immutableSource));
        this.verificationNotes = Collections.unmodifiableList(notes);
    }

    @SuppressWarnings("unchecked")
    private static FidelityResult applyFidelityIfAvailable(String projectName, String packageName, List<FileEntry> raw) {
        try {
            Class<?> type = Class.forName("dev.thefoolish.aidao.GeneratedProjectFidelityPostProcessor");
            Method process = type.getDeclaredMethod("process", String.class, String.class, List.class);
            process.setAccessible(true);
            Object result = process.invoke(null, projectName, packageName, raw);
            return readResult(result);
        } catch (ClassNotFoundException unavailableInLegacyHarness) {
            return new FidelityResult(projectName, packageName, raw, Collections.emptyList());
        } catch (Exception brokenFidelityModule) {
            List<String> notes = new ArrayList<>();
            notes.add("FAIL generated-app fidelity transformation failed: " + brokenFidelityModule.getClass().getSimpleName());
            return new FidelityResult(projectName, packageName, raw, notes);
        }
    }

    private static FidelityResult applyProviderCapabilityIntegrationIfAvailable(String projectName, String packageName, List<FileEntry> raw) {
        try {
            Class<?> type = Class.forName("dev.thefoolish.aidao.ProviderCapabilityIntegrator");
            Method process = type.getDeclaredMethod("process", String.class, String.class, List.class);
            process.setAccessible(true);
            Object result = process.invoke(null, projectName, packageName, raw);
            return readResult(result);
        } catch (ClassNotFoundException unavailableInLegacyHarness) {
            return new FidelityResult(projectName, packageName, raw, Collections.emptyList());
        } catch (Exception brokenCapabilityModule) {
            List<String> notes = new ArrayList<>();
            notes.add("FAIL provider capability integration failed: " + brokenCapabilityModule.getClass().getSimpleName());
            return new FidelityResult(projectName, packageName, raw, notes);
        }
    }

    private static FidelityResult applyNativeFidelityIfAvailable(String projectName, String packageName, List<FileEntry> raw) {
        try {
            Class<?> type = Class.forName("dev.thefoolish.aidao.NativeFidelityPostProcessor");
            Method process = type.getDeclaredMethod("process", String.class, String.class, List.class);
            process.setAccessible(true);
            Object result = process.invoke(null, projectName, packageName, raw);
            return readResult(result);
        } catch (ClassNotFoundException unavailableInLegacyHarness) {
            return new FidelityResult(projectName, packageName, raw, Collections.emptyList());
        } catch (Exception brokenNativeFidelityModule) {
            List<String> notes = new ArrayList<>();
            notes.add("FAIL native Android fidelity transformation failed: " + brokenNativeFidelityModule.getClass().getSimpleName());
            return new FidelityResult(projectName, packageName, raw, notes);
        }
    }

    private static FidelityResult applyMihonBehaviorIfAvailable(String projectName, String packageName, List<FileEntry> raw) {
        try {
            Class<?> type = Class.forName("dev.thefoolish.aidao.MihonBehaviorPostProcessor");
            Method process = type.getDeclaredMethod("process", String.class, String.class, List.class);
            process.setAccessible(true);
            Object result = process.invoke(null, projectName, packageName, raw);
            return readResult(result);
        } catch (ClassNotFoundException unavailableInLegacyHarness) {
            return new FidelityResult(projectName, packageName, raw, Collections.emptyList());
        } catch (Exception brokenReferenceProfile) {
            List<String> notes = new ArrayList<>();
            notes.add("FAIL reference-app behavior transformation failed: " + brokenReferenceProfile.getClass().getSimpleName());
            return new FidelityResult(projectName, packageName, raw, notes);
        }
    }

    private static FidelityResult applyGeneralProductIfAvailable(String projectName, String packageName, List<FileEntry> raw) {
        try {
            Class<?> type = Class.forName("dev.thefoolish.aidao.GeneralProductPostProcessor");
            Method process = type.getDeclaredMethod("process", String.class, String.class, List.class);
            process.setAccessible(true);
            Object result = process.invoke(null, projectName, packageName, raw);
            return readResult(result);
        } catch (ClassNotFoundException unavailableInLegacyHarness) {
            return new FidelityResult(projectName, packageName, raw, Collections.emptyList());
        } catch (Exception brokenProductPass) {
            List<String> notes = new ArrayList<>();
            notes.add("FAIL general product fidelity transformation failed: " + brokenProductPass.getClass().getSimpleName());
            return new FidelityResult(projectName, packageName, raw, notes);
        }
    }

    private static FidelityResult applyRequestFidelityIfAvailable(String projectName, String packageName, List<FileEntry> raw) {
        try {
            Class<?> type = Class.forName("dev.thefoolish.aidao.RequestFidelityPostProcessor");
            Method process = type.getDeclaredMethod("process", String.class, String.class, List.class);
            process.setAccessible(true);
            Object result = process.invoke(null, projectName, packageName, raw);
            return readResult(result);
        } catch (ClassNotFoundException unavailableInLegacyHarness) {
            return new FidelityResult(projectName, packageName, raw, Collections.emptyList());
        } catch (Exception brokenRequestFidelity) {
            List<String> notes = new ArrayList<>();
            notes.add("FAIL request-specific fidelity transformation failed: " + brokenRequestFidelity.getClass().getSimpleName());
            return new FidelityResult(projectName, packageName, raw, notes);
        }
    }

    @SuppressWarnings("unchecked")
    private static FidelityResult readResult(Object result) throws Exception {
        Class<?> resultType = result.getClass();
        Field projectField = resultType.getDeclaredField("projectName");
        Field packageField = resultType.getDeclaredField("packageName");
        Field filesField = resultType.getDeclaredField("files");
        Field notesField = resultType.getDeclaredField("notes");
        projectField.setAccessible(true);
        packageField.setAccessible(true);
        filesField.setAccessible(true);
        notesField.setAccessible(true);
        return new FidelityResult((String) projectField.get(result), (String) packageField.get(result),
                (List<FileEntry>) filesField.get(result), (List<String>) notesField.get(result));
    }

    @SuppressWarnings("unchecked")
    private static List<String> validateFidelityIfAvailable(String packageName, List<FileEntry> files) {
        try {
            Class<?> type = Class.forName("dev.thefoolish.aidao.GeneratedFidelityValidator");
            Method validate = type.getDeclaredMethod("validate", String.class, List.class);
            validate.setAccessible(true);
            Object result = validate.invoke(null, packageName, files);
            return result instanceof List ? (List<String>) result : Collections.singletonList("FAIL fidelity validator returned an invalid result");
        } catch (ClassNotFoundException unavailableInLegacyHarness) {
            return Collections.emptyList();
        } catch (Exception brokenValidator) {
            return Collections.singletonList("FAIL generated-app fidelity validation failed: " + brokenValidator.getClass().getSimpleName());
        }
    }

    boolean hasPath(String path) {
        for (FileEntry file : files) if (file != null && file.path.equals(path)) return true;
        return false;
    }

    FileEntry find(String path) {
        for (FileEntry file : files) if (file != null && file.path.equals(path)) return file;
        return null;
    }
}