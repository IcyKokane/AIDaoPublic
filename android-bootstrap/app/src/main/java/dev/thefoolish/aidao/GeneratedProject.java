package dev.thefoolish.aidao;

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
            return out;
        }
    }

    final String projectName;
    final String packageName;
    final List<FileEntry> files;
    final List<String> verificationNotes;

    GeneratedProject(String projectName, String packageName, List<FileEntry> files, List<String> verificationNotes) {
        List<FileEntry> raw = new ArrayList<>(files == null ? Collections.emptyList() : files);
        GeneratedProjectFidelityPostProcessor.Result fidelity =
                GeneratedProjectFidelityPostProcessor.process(projectName, packageName, raw);

        this.projectName = fidelity.projectName;
        this.packageName = fidelity.packageName;
        List<FileEntry> immutableSource = new ArrayList<>(fidelity.files == null ? Collections.emptyList() : fidelity.files);
        this.files = Collections.unmodifiableList(immutableSource);

        List<String> notes = new ArrayList<>();
        if (verificationNotes != null) notes.addAll(verificationNotes);
        if (fidelity.notes != null) notes.addAll(fidelity.notes);

        GeneratedProjectValidator.Result structural = GeneratedProjectValidator.validateRaw(this.packageName, immutableSource);
        notes.addAll(structural.notes);
        notes.addAll(GeneratedFidelityValidator.validate(this.packageName, immutableSource));
        this.verificationNotes = Collections.unmodifiableList(notes);
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
