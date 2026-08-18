from pathlib import Path

root = Path(__file__).resolve().parents[1]
project = root / 'android-bootstrap/app/src/main/java/dev/thefoolish/aidao/GeneratedProject.java'
resolver = root / 'android-bootstrap/app/src/main/java/dev/thefoolish/aidao/GeneratedProjectOverrideResolver.java'
repairer = root / 'android-bootstrap/app/src/main/java/dev/thefoolish/aidao/GeneratedProjectRepairer.java'

text = project.read_text()
anchor = '''        this.verificationNotes = Collections.unmodifiableList(notes);
    }

    @SuppressWarnings("unchecked")
    private static FidelityResult applyFidelityIfAvailable'''
insert = '''        this.verificationNotes = Collections.unmodifiableList(notes);
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
    private static FidelityResult applyFidelityIfAvailable'''
if text.count(anchor) != 1:
    raise SystemExit(f'GeneratedProject anchor expected once, found {text.count(anchor)}')
project.write_text(text.replace(anchor, insert, 1))

r = resolver.read_text()
old = '''        GeneratedProject project = new GeneratedProject(
                generated.projectName,
                generated.packageName,
                resolved,
                notes);'''
new = '''        GeneratedProject project = GeneratedProject.resolved(
                generated.projectName,
                generated.packageName,
                resolved,
                notes);'''
if r.count(old) != 1:
    raise SystemExit(f'override resolver constructor expected once, found {r.count(old)}')
resolver.write_text(r.replace(old, new, 1))

p = repairer.read_text()
old2 = '''        return new RepairResult(new GeneratedProject(original.projectName,original.packageName,out,original.verificationNotes),action,changed);'''
new2 = '''        return new RepairResult(GeneratedProject.resolved(original.projectName,original.packageName,out,original.verificationNotes),action,changed);'''
if p.count(old2) != 1:
    raise SystemExit(f'repairer constructor expected once, found {p.count(old2)}')
repairer.write_text(p.replace(old2, new2, 1))

print('Applied resolved-tree preservation patch')
