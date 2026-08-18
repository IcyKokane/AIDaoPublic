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
            out = out.replace("store.text(\"last_episode\"", "store.putText(\"last_episode\"");
            out = out.replace("store.text(\"last_surface\"", "store.putText(\"last_surface\"");
            out = out.replace("String last=store.putText(\"last_episode\",\"\")", "String last=store.text(\"last_episode\",\"\")");
            String saveButton = "Button save=button(\"Save +60s test progress\")";
            if (!out.contains("android.widget." + saveButton)) {
                out = out.replace(saveButton, "android.widget." + saveButton);
            }
            // Generated activities may use the simple Android Button type while a
            // provider-specific pass only imported Intent/Base64/collections. Keep
            // this idempotent and narrow: add the import only to Java sources that
            // actually use an unqualified Button declaration and lack widget imports.
            if (path != null && path.endsWith(".java")
                    && out.contains("Button ")
                    && !out.contains("import android.widget.Button;")
                    && !out.contains("import android.widget.*;")
                    && !out.contains("class Button")) {
                int packageEnd = out.indexOf(';');
                if (packageEnd >= 0) {
                    out = out.substring(0, packageEnd + 1)
                            + "\nimport android.widget.Button;"
                            + out.substring(packageEnd + 1);
                }
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
