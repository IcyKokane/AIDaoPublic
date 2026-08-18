from pathlib import Path

ROOT = Path("android-bootstrap/app/src/main/java/dev/thefoolish/aidao")

# Repeated-package strings must exist in sanitizers/validators as regression
# signatures, so never reject the repository merely because those literals are
# present in code whose job is to detect or repair them. Ordinary production
# implementation files must still remain free of the corrupted qualifiers.
intentional_guard_files = {
    "GeneratedProject.java",
    "GeneratedProjectRepairer.java",
    "GeneratedProjectValidator.java",
    "MihonBehaviorPostProcessor.java",
}
qualifier_markers = [
    "android.widget.android.widget.",
    "android.graphics.android.graphics.",
    "android.content.android.content.",
    "android.app.android.app.",
]

for path in ROOT.glob("*.java"):
    if path.name in intentional_guard_files:
        continue
    text = path.read_text()
    leaked = [marker for marker in qualifier_markers if marker in text]
    if leaked:
        raise SystemExit(f"Release-blocking Android qualifier corruption in {path.name}: {', '.join(leaked)}")

all_text = "\n".join(path.read_text() for path in ROOT.glob("*.java"))
for secret_marker in ["BEGIN RSA PRIVATE KEY", "BEGIN PRIVATE KEY", "github_pat_"]:
    if secret_marker in all_text:
        raise SystemExit("Release-blocking credential/private-key marker: " + secret_marker)

normalizer = (ROOT / "GeneratedProject.java").read_text()
repairer = (ROOT / "GeneratedProjectRepairer.java").read_text()
validator = (ROOT / "GeneratedProjectValidator.java").read_text()
for marker in qualifier_markers:
    if marker not in normalizer and marker not in repairer:
        raise SystemExit("Missing repeated-package normalization regression coverage: " + marker)
    if marker not in validator:
        raise SystemExit("Missing repeated-package validation regression coverage: " + marker)

activity6 = (ROOT / "AIDaoActivityV6.java").read_text()
required6 = [
    "GITHUB_APP_CLIENT_ID", "sessionToken", "run_url", "artifact_name",
    "GeneratedProjectRepairer", "BUILD BLOCKED", "APK READY",
    "GeneratedProjectOverrideResolver",
    "Manual source edits conflict with regenerated source",
]
missing6 = [marker for marker in required6 if marker not in activity6]
if missing6:
    raise SystemExit("Missing V1 build-handoff/revision marker(s): " + ", ".join(missing6))

activity5 = (ROOT / "AIDaoActivityV5.java").read_text()
required5 = ["override-base::", "ProjectRevisionLedger.hash", "GeneratedProjectOverrideResolver", "SOURCE MODIFIED"]
missing5 = [marker for marker in required5 if marker not in activity5]
if missing5:
    raise SystemExit("Missing V1 workspace revision marker(s): " + ", ".join(missing5))

for marker in ["PLACEHOLDER_COMPLETION_MARKERS", "Save local sample state", "DemoProvider"]:
    if marker not in validator:
        raise SystemExit("Generated completion honesty gate missing: " + marker)

print("Release-blocking source invariants passed")
