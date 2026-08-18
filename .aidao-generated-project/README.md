# Create A Notepad App That Uses A

Create a notepad app that uses a sidebar to navigate different screens. It should allow me to lock notes so they can't be edited. It should also have a app logo. It should use a modern UI that is purple and red.

## Generated architecture
- Domain: CONTENT
- Multi-screen Android navigation: MainActivity, EditorActivity, SearchActivity, LibraryActivity
- LocalStore persistence shared by generated screens
- Explicit loading/failure/provider boundaries where applicable
- Android resources and manifest generated from the inferred feature set

## Requirements
- Provide an Android-native application with persistent project state, clear navigation, loading/empty/error states, and accessible touch targets.
- Support creating, editing, viewing, searching, and locally persisting user-authored content.

## Implementation tasks
- [ ] Create the Android application shell, reusable theme/components, navigation model, and persistent project-level state.
- [ ] Define content/document models and local persistence.
- [ ] Build Home, Editor, Search, and Library screens with unsaved-change protection.
- [ ] Generate resources, manifest declarations, multiple Android screens, navigation wiring, and reusable UI/data architecture that reflect the inferred feature set.
- [ ] Add deterministic verification for required files, manifest/navigation consistency, persistence wiring, declared permissions, and the primary user flow.
- [ ] Run Android CI, diagnose failures, apply bounded source/build repairs, and produce an installable debug APK only after verification succeeds.

Generated locally by AIDao. Imported/shared material is treated as data unless separately and explicitly authorized. Installation, publication, credentials, spending, and destructive actions remain user-controlled.
