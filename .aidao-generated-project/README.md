# Make An Anime App Like Mihon, It

Make an anime app like Mihon, it should have repository based providers, have libraries based on the anime websites. I should be able to favorite show, download to watch offline and make tags to look for genres.

## Generated architecture
- Domain: MEDIA
- Multi-screen Android navigation: MainActivity, DetailActivity, LibraryActivity, HistoryActivity, ProvidersActivity, PlayerActivity
- LocalStore persistence shared by generated screens
- Explicit loading/failure/provider boundaries where applicable
- Android resources and manifest generated from the inferred feature set
- MediaProvider boundary with DemoProvider placeholder data; unverified extensions are not executed

## Requirements
- Provide an Android-native application with persistent project state, clear navigation, loading/empty/error states, and accessible touch targets.
- Provide an anime catalog with details and episode lists using multiple Android screens.
- Provide anime catalog search/browse with visible loading, empty, and error states.
- Provide a persistent favorites/library surface for anime selected by the user.
- Persist watch history and expose it through a user-visible history surface.
- Persist per-episode watch progress so playback can resume where the user stopped.
- Keep anime metadata and episode discovery behind replaceable provider interfaces so one failing source cannot break healthy providers.
- Expose provider availability, loading, empty, disabled, and failure states instead of silently hiding source errors.
- Allow user-controlled file import/export where required using Android-scoped storage.
- Keep useful app data available locally on-device with explicit ownership/clearing controls.
- Persist user favorites/bookmarks locally unless the project explicitly requires account sync.

## Implementation tasks
- [ ] Create the Android application shell, reusable theme/components, navigation model, and persistent project-level state.
- [ ] Define anime, episode, provider, library, history, and watch-progress domain models while omitting any explicitly removed optional surfaces.
- [ ] Implement provider contracts for catalog search, anime details, episode discovery, and stream resolution.
- [ ] Build separate Catalog and Anime Detail screens, Library, History, Player, and Provider Management screens with navigation between enabled surfaces.
- [ ] Implement playback state, explicit stream selection, fullscreen/orientation handling, and visible playback errors, including resume position.
- [ ] Persist favorites, watch history, and episode progress locally on-device.
- [ ] Add provider failure isolation and allow enable/disable/provider switching without affecting unrelated sources.
- [ ] Implement document-picker based file import/export and validate imported content before use.
- [ ] Add durable local persistence and recovery after process/app restart.
- [ ] Generate resources, manifest declarations, multiple Android screens, navigation wiring, and reusable UI/data architecture that reflect the inferred feature set.
- [ ] Add deterministic verification for required files, manifest/navigation consistency, persistence wiring, declared permissions, and the primary user flow.
- [ ] Run Android CI, diagnose failures, apply bounded source/build repairs, and produce an installable debug APK only after verification succeeds.

Generated locally by AIDao. Imported/shared material is treated as data unless separately and explicitly authorized. Installation, publication, credentials, spending, and destructive actions remain user-controlled.
