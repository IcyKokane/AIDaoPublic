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

        /**
         * Compatibility normalization for deterministic generated source.
         * Java cannot overload a getter/setter solely by return type, so the
         * generated LocalStore setter is named putText and its generated call
         * sites are rewritten consistently before the immutable tree exists.
         */
        private static String normalizeGeneratedContent(String path, String source) {
            if (source == null) return "";
            String out = source;
            if (path != null && path.endsWith("/LocalStore.java")) {
                out = out.replace("public void text(String k,String v)", "public void putText(String k,String v)");
            }
            out = out.replace("store.text(\"last_episode\"", "store.putText(\"last_episode\"");
            out = out.replace("store.text(\"last_surface\"", "store.putText(\"last_surface\"");
            out = out.replace("String last=store.putText(\"last_episode\",\"\")", "String last=store.text(\"last_episode\",\"\")");
            out = out.replace("Button save=button(\"Save +60s test progress\")", "android.widget.Button save=button(\"Save +60s test progress\")");
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

        this.projectName = capability.projectName;
        this.packageName = capability.packageName;
        List<FileEntry> immutableSource = new ArrayList<>(capability.files == null ? Collections.emptyList() : capability.files);
        this.files = Collections.unmodifiableList(immutableSource);

        List<String> notes = new ArrayList<>();
        if (verificationNotes != null) notes.addAll(verificationNotes);
        if (fidelity.notes != null) notes.addAll(fidelity.notes);
        if (capability.notes != null) notes.addAll(capability.notes);

        GeneratedProjectValidator.Result structural = GeneratedProjectValidator.validateRaw(this.packageName, immutableSource);
        notes.addAll(structural.notes);
        notes.addAll(validateFidelityIfAvailable(this.packageName, immutableSource));
        this.verificationNotes = Collections.unmodifiableList(notes);
    }

    /**
     * The legacy acceptance harness intentionally compiles the core generator as
     * a very small standalone Java set. Reflection keeps that harness compatible
     * while the full Android build loads the fidelity module normally.
     */
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

    /** Runs capability research after the fidelity transform but before semantic validation. */
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
