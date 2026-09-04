from pathlib import Path

root = Path('.')
client = (root / 'android-bootstrap/app/src/main/java/dev/thefoolish/aidao/GitHubGeneratedBuildClient.java').read_text()
activity = (root / 'android-bootstrap/app/src/main/java/dev/thefoolish/aidao/AIDaoActivityV6.java').read_text()
workflow = (root / '.github/workflows/generated-project.yml').read_text()

required_client = [
    'source_sha', 'repository', 'sourceSha', 'artifactId', 'archive_download_url',
    'findRunForBranch(runs,branch,sourceSha)', 'aidao-generated-apk-',
    'artifactId>0', 'SOURCE IDENTITY ERROR', 'ARTIFACT ERROR'
]
for marker in required_client:
    if marker not in client:
        raise SystemExit('Missing exact generated-build identity client marker: ' + marker)

for forbidden in [
    'findRunForBranch(runs,branch)',
    'return firstRun',
    'artifacts[0]'
]:
    if forbidden in client:
        raise SystemExit('Unsafe generated-build handoff fallback regressed: ' + forbidden)

required_workflow = [
    'REQUESTED_SOURCE_SHA:', 'REQUESTED_REPOSITORY:', 'git rev-parse HEAD',
    'Generated source identity mismatch', 'aidao-generated-apk-${{ github.run_id }}'
]
for marker in required_workflow:
    if marker not in workflow:
        raise SystemExit('Missing trusted-workflow source/artifact identity marker: ' + marker)

required_activity = [
    'artifact_id', 'source_sha', 'build_repo', 'build_project',
    'clearBuildHandoff()', 'Previous APK handoff incomplete',
    'exact source/artifact identity is incomplete', 'APK READY'
]
for marker in required_activity:
    if marker not in activity:
        raise SystemExit('Missing persisted install-handoff identity marker: ' + marker)

if activity.index('clearBuildHandoff();') > activity.index('client.sendBuildAndWait'):
    raise SystemExit('Stale APK handoff is not cleared before starting a fresh remote build')

print('Generated build identity acceptance passed')
