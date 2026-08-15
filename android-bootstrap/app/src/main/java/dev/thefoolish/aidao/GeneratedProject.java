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
            this.content = content;
            this.taskHint = taskHint;
        }
    }

    final String projectName;
    final String packageName;
    final List<FileEntry> files;
    final List<String> verificationNotes;

    GeneratedProject(String projectName, String packageName, List<FileEntry> files, List<String> verificationNotes) {
        this.projectName = projectName;
        this.packageName = packageName;
        this.files = Collections.unmodifiableList(new ArrayList<>(files));
        this.verificationNotes = Collections.unmodifiableList(new ArrayList<>(verificationNotes));
    }

    boolean hasPath(String path) {
        for (FileEntry file : files) if (file.path.equals(path)) return true;
        return false;
    }

    FileEntry find(String path) {
        for (FileEntry file : files) if (file.path.equals(path)) return file;
        return null;
    }
}
