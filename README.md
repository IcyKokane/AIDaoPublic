# AIDao

**Working name.** An Android-first, open-source AI application builder whose goal is:

> Describe software in ordinary language and receive a tested Android APK.

## Current milestone: v0.11.0 alpha readiness

This repository currently contains the first Android client shell and architectural contracts for:

- plain-language app requests;
- intent expansion;
- knowledge ingestion;
- GitHub-backed project workspaces;
- automated Android builds;
- APK artifact delivery.

The initial screen already accepts an ordinary-language description and converts it into an inferred project brief locally. AI-backed planning, file/video ingestion, repository creation, source generation, CI repair loops, and APK retrieval are the next implementation milestones.

## Design rules

1. Users describe behavior; the system chooses implementation details.
2. Secrets are never embedded in a public APK.
3. Generated code is treated as untrusted until it builds and passes validation.
4. GitHub is the durable source/build history, not the AI brain.
5. Knowledge can be supplied by files, docs, screenshots, videos, archives and repositories.
6. Provider/plugin architectures remain source-agnostic.

## Planned execution loop

User request
→ intent expansion
→ relevant knowledge retrieval
→ technical specification
→ project generation
→ GitHub commit
→ CI compile/test
→ failure analysis
→ patch/rebuild loop
→ APK artifact
→ user review

## Android baseline

- Kotlin
- Jetpack Compose
- compileSdk 37
- targetSdk 37
- Android Gradle Plugin 9.3.0
- Gradle 9.5.0
- JDK 17 in CI

## Security model

The Android client will never require contributors to hard-code private AI or GitHub credentials in source. Production deployments should use OAuth/device authorization and a user-controlled backend/provider layer where secrets are necessary.

## Status

This is an early foundation, not yet an autonomous app generator.

## License

Apache-2.0


## v0.3.0 changes

- New Discord-inspired workspace hierarchy mixed with a cleaner Linear-style project dashboard.
- Responsive navigation: bottom navigation on phones; workspace rail/navigation rail on wider displays.
- Separate Build, Projects, Knowledge, and Activity areas.
- Persistent local Knowledge Library metadata.
- Persistent local planned-project history.
- Workspace activity feed.
- Build pipeline visualization.

The visual system intentionally borrows interaction principles rather than copying Discord branding or layouts exactly.


## v0.4.0 changes

AIDao now processes imported text/code locally, indexes supported ZIP source archives, persists extracted content, and retrieves relevant knowledge for a build request. PDFs, images, video, and audio are explicitly marked for multimodal/backend processing rather than falsely treated as understood.

The Android client also includes an HTTPS-only, provider-neutral AI gateway contract. Provider secrets are intentionally excluded from the distributable APK.


## v0.5.0 changes

AIDao can now take a plain-language request, retrieve matching imported knowledge, produce a structured plan, validate that plan, generate a concrete Android project source tree, preview the generated files, and export the source as a ZIP from Android.

A local deterministic planner is included to exercise the full pipeline without an AI account. A real model-backed planner remains behind the HTTPS gateway boundary so provider secrets never need to ship inside the APK.


## v0.6.0 changes

AIDao now includes a GitHub REST execution client capable of serially committing generated project files, reading workflow runs, and listing workflow artifacts. The app also includes a dedicated GitHub workspace for selecting the destination repository and showing execution readiness.

The public authentication model is intentionally GitHub App/OAuth based. AIDao does not add a permanent-token textbox to the production-oriented UI and never hard-codes a GitHub secret into the APK.


## v0.7.0 changes

AIDao now implements GitHub's OAuth device authorization flow, stores the resulting token encrypted with the Android Keystore, can upload a generated Android source tree to the selected repository, monitor GitHub Actions, enumerate jobs, retrieve failed-job logs, classify common Android build failures, and discover successful APK-like workflow artifacts.

A registered AIDao GitHub App with device flow enabled is still required before public users can actually authenticate.


## v0.8.0 changes

AIDao now has its first bounded self-repair loop. Failed GitHub Actions jobs can be classified, converted into a validated repair request, patched when a conservative local repair is provable, re-uploaded as changed files only, and rebuilt for up to three attempts.

The GitHub writer now correctly fetches the current blob SHA before replacing an existing source file. Successful APK build artifacts can be downloaded from GitHub Actions and exported to Android storage as the artifact ZIP.


## v0.9.0 changes

AIDao now has a model-backed repair gateway boundary, diff-aware repair context construction, and persistent repair gateway settings. Model-produced patches remain constrained by the same local repair validator before any repository write occurs.

Successful GitHub Actions artifact ZIPs can now be downloaded to app cache, inspected for an APK, extracted, exposed through Android FileProvider, and handed to Android's package installer with temporary read permission.


## v0.10.0 changes

AIDao now uses a composite repair strategy: safe deterministic repairs first, then an HTTPS model-backed repair gateway when configured. Model patches remain constrained by local validation and are shown as before/after previews.

Generated-project state and execution events are persisted locally, including repair counts and recent build messages. Before opening a generated APK, AIDao checks whether Android currently allows this app to request package installation and opens the system permission screen when necessary.


## v0.11.0 changes
AIDao can now create a repository for the authenticated GitHub user when its token has Administration write permission, browse generated source files, and persist/display build provenance from source commit through workflow artifact.

This is the first alpha-readiness pass. The remaining external gate is registering AIDao's GitHub App and getting the source through a real Android/Gradle compile environment.
