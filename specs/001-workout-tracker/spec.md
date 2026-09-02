# Feature Specification: Gymora — Workout Planning, Execution & History Tracking (v1)

**Feature Branch**: `001-workout-tracker`

**Created**: 2026-09-02

**Status**: Draft

**Input**: User description: "Transform the detailed product requirements for a fitness / workout tracking application (docs/prod_requirements.md) into a structured software specification. Preserve all functional requirements, group them into prioritized, independently deliverable user stories, define acceptance criteria, business rules and constraints, separate MVP from future scope, flag ambiguities, and remain technology-agnostic."

**Source of truth**: `docs/prod_requirements.md` (sections referenced inline as PRD-§N).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Exercise Library & Custom Exercises (Priority: P1)

A person who trains at the gym opens the exercise library on first launch and finds common exercises already available, grouped by muscle group (Chest, Back, Shoulders, Arms, Legs). They can browse, search, and — when their exercise is missing — create their own custom exercise with a name and optional details (muscle group, description, notes). Custom exercises are treated as first-class: they are searchable, editable, and usable in routines. (PRD-§6, §38, §44)

**Why this priority**: Every routine and every workout references exercises. Without a usable exercise catalog, no other journey can start. On its own, this story already delivers value (a browsable, searchable, extensible exercise catalog).

**Independent Test**: Can be fully tested by launching the app fresh, browsing the seeded library, searching "bench" and getting relevant results, creating a custom exercise, editing it, and confirming it appears in search — without any routine or workout existing.

**Acceptance Scenarios**:

1. **Given** the app is launched for the first time, **When** the user opens the exercise library, **Then** common exercises are available grouped by muscle group, and no workout routines exist.
2. **Given** the exercise library, **When** the user searches for "bench", **Then** matching built-in and custom exercises are returned.
3. **Given** the exercise library, **When** the user creates a custom exercise with a name and optional muscle group, description, and notes, **Then** it is saved and becomes selectable for routines.
4. **Given** an existing exercise, **When** the user edits its details, **Then** the changes are saved and visible everywhere the exercise is referenced by name.
5. **Given** an exercise that appears in completed workout history, **When** the user deletes it from the library, **Then** all historical workouts remain intact and viewable with the exercise's name as it was at the time.

---

### User Story 2 — Routine Creation & Management (Priority: P1)

The user creates a routine ("Chest Workout"), optionally adds a description, then adds exercises from the library (or custom ones), reorders them, attaches notes, and configures planned sets per exercise (e.g., Bench Press: 3 sets of target reps × target weight). The user can rename, edit, duplicate, and delete routines. Deleting a routine requires confirmation and never touches workouts already performed from it. (PRD-§4, §5, §7, §36, §37)

**Why this priority**: Routines are the template half of the core loop and the entry point for starting workouts. Independently delivers planning value.

**Independent Test**: Can be fully tested by creating a routine with three exercises and three sets each, reordering exercises, duplicating it, renaming it, deleting it, and verifying everything persists across an app restart — no workout execution required.

**Acceptance Scenarios**:

1. **Given** a home screen with no routines, **When** the user creates a routine named "Chest Workout" with an optional description, **Then** it appears as a card on the home screen.
2. **Given** a routine, **When** the user adds exercises (from library or custom), removes exercises, and reorders them, **Then** the routine reflects and persists the new order.
3. **Given** an exercise inside a routine, **When** the user configures planned sets (add/edit/delete sets with target reps and target weight) and per-exercise notes, **Then** the template is saved.
4. **Given** a routine with completed workouts in history, **When** the user deletes the routine and confirms, **Then** the routine disappears but every historical workout created from it remains unchanged in history.
5. **Given** a routine, **When** the user duplicates it, **Then** a copy (e.g., "Chest Workout Copy") is created containing all exercises and set templates, independently editable.
6. **Given** a routine used in a past workout, **When** the user renames the routine, **Then** the historical workout continues to display the name it had when the workout occurred.
7. **Given** a delete request, **When** the user has not confirmed, **Then** nothing is deleted.

---

### User Story 3 — Workout Execution & Logging (Priority: P1)

At the gym, the user taps START on a routine. A workout session is created, the start time is recorded, and a timer begins. The routine's exercises appear with their planned sets. For each set the user enters weight (decimals supported) and reps, and marks the set complete — each change is saved immediately. The user can add extra sets, add exercises that were not in the routine (choosing whether to also add them to the routine for the future), and skip/remove exercises for this workout only. When done, the user taps FINISH WORKOUT, sees a confirmation with duration, exercises, completed sets, and total volume, confirms, and gets a full summary. Every change survives app crashes, process death, and battery loss. (PRD-§8, §9, §11, §12, §13, §14, §15, §27, §33, §34)

**Why this priority**: This is the primary value of the product — fast, reliable logging during an actual training session. Nothing else matters if this experience fails in the gym.

**Independent Test**: Can be fully tested by starting a workout from a routine, logging sets (including decimal weights), adding a set, adding and removing an exercise, force-stopping the app mid-workout and verifying entered data is still present on relaunch, then finishing and verifying the summary figures (duration, sets, reps, volume).

**Acceptance Scenarios**:

1. **Given** a routine on the home screen, **When** the user taps START, **Then** a workout session is created, the start timestamp is recorded, a timer shows elapsed time (00:00:00 format), and the routine's exercises appear with their planned sets.
2. **Given** an active workout, **When** the user enters weight (e.g., 22.5) and reps for a set and marks it complete, **Then** the set is saved immediately and visibly marked as completed in a way that does not rely on color alone.
3. **Given** an active workout, **When** the user adds an extra set beyond the planned ones, **Then** the set is added and persisted.
4. **Given** an active workout, **When** the user adds an exercise that is not part of the routine (from the library or newly created), **Then** the user is asked whether to also add it to the routine for future workouts ("Add to routine" vs "This workout only"), and in both cases the exercise is added to the current session.
5. **Given** an active workout, **When** the user skips/removes an exercise from this workout, **Then** the underlying routine is not modified; removing an exercise from the routine itself is a separate, clearly distinct action.
6. **Given** an active workout, **When** the user navigates to another screen, the app goes to the background, or the screen turns off, **Then** upon return the timer reflects the true elapsed time.
7. **Given** logged sets, **When** the user taps FINISH WORKOUT, **Then** a confirmation shows duration, exercise count, completed set count, and total volume; upon confirming, the end timestamp is recorded, the workout is marked completed, all data is persisted, and a summary screen is shown (routine name, date, duration, exercises, sets, total reps, total volume, and a per-exercise breakdown of completed sets).
8. **Given** an active workout, **When** the user chooses to cancel, **Then** a confirmation is shown with "Keep working out" and "Discard workout" options; ordinary navigation or closing the app never discards the workout.
9. **Given** any set input, **When** the user enters a negative weight or negative reps, **Then** the input is rejected; zero weight is allowed (bodyweight exercises); weight supports decimals and reps are whole numbers.
10. **Given** any single change during the workout (weight edit, reps edit, set completion, set added, exercise added), **When** the app crashes or the device loses power immediately after, **Then** that change is recoverable on next launch.

---

### User Story 4 — Workout Recovery After Interruption (Priority: P1)

If the app is closed, killed, or the phone restarts while a workout is active, reopening the app detects the unfinished workout and shows "Workout in progress — Chest Workout, started 43 minutes ago" with a RESUME action. Resuming restores exactly where the user left off, with every already-entered set intact and the elapsed duration correct. (PRD-§25, §26, §53)

**Why this priority**: A non-negotiable trust requirement (core domain rule: an active workout must survive application restart). Without it, a single crash could destroy a recorded session.

**Independent Test**: Can be fully tested by starting a workout, logging two sets, force-stopping the app, relaunching, verifying the recovery prompt appears with correct elapsed time, resuming, and verifying all sets and the timer are intact.

**Acceptance Scenarios**:

1. **Given** an unfinished workout, **When** the app is reopened, **Then** the user sees a recovery prompt with the workout name, how long ago it started, and a RESUME action.
2. **Given** the recovery prompt, **When** the user resumes, **Then** the active workout is restored with all previously entered sets and a correct elapsed duration.
3. **Given** the recovery prompt, **When** the user chooses to discard instead, **Then** confirmation is required before the session is discarded.

---

### User Story 5 — Workout History Review (Priority: P1)

The user opens Workout History and sees every completed workout ordered newest first (date, routine name, duration). Tapping one opens the complete historical workout — every exercise and every set exactly as performed. History is read-only and immune to later changes: renaming, editing, or deleting routines or exercises never alters what history displays, because historical records preserve the names and data as they existed at workout time. (PRD-§16, §23, §24, §53)

**Why this priority**: The product's promise is permanent tracking. Logging without faithful, protected review would not satisfy "tracking".

**Independent Test**: Can be fully tested by completing a workout, then renaming the routine, removing an exercise from it, deleting an exercise from the library, and verifying the history entry still shows the original name and complete original data.

**Acceptance Scenarios**:

1. **Given** completed workouts, **When** the user opens history, **Then** workouts are listed newest first with date, name, and duration.
2. **Given** a history entry, **When** the user opens it, **Then** the full workout is shown read-only: every exercise and set with the recorded values.
3. **Given** a workout performed on 1 Aug as "Chest Workout" including Chest Fly, **When** the routine is later renamed and Chest Fly is removed from it, **Then** the 1 Aug history entry still displays "Chest Workout" including Chest Fly.
4. **Given** no completed workouts, **When** the user opens history, **Then** a helpful empty state is shown ("Your completed workouts will appear here.").

---

### User Story 6 — Previous Performance Display & Pre-Fill (Priority: P2)

When the user starts a workout, each exercise shows their most recent performance for that exercise (e.g., Bench Press: 60×10, 70×8, 75×7) next to today's sets. Today's weight/reps fields are pre-filled with those previous values, so the user only confirms or adjusts them — dramatically reducing typing during gym sessions. (PRD-§9, §10, §45)

**Why this priority**: Major usability multiplier for the core loop (explicitly called out as significantly reducing typing), but the app remains fully usable without it.

**Independent Test**: Can be fully tested by completing a routine once, starting it again, verifying previous values are displayed beside each exercise and today's fields are pre-filled, modifying one value, and verifying the saved set reflects the edit.

**Acceptance Scenarios**:

1. **Given** an exercise performed in an earlier workout, **When** a new workout containing that exercise starts, **Then** the most recent performance (weight × reps per set) is displayed next to today's sets.
2. **Given** previous performance exists, **When** today's set rows are shown, **Then** weight and reps are pre-filled with the previous values and remain fully editable.
3. **Given** an exercise never performed before, **When** it appears in an active workout, **Then** no previous values are shown and fields start empty.
4. **Given** pre-filled values, **When** the user modifies them and completes the set, **Then** the modified values are what gets saved.

---

### User Story 7 — Exercise History (Priority: P2)

The user selects an individual exercise and sees its full history across workouts, newest first (e.g., Barbell Bench Press on 31 Aug, 24 Aug, 17 Aug with all sets for each date). This supports reviewing progress and lays the groundwork for future progress charts. Retrieval stays fast even with large history. (PRD-§17, §51)

**Why this priority**: Valuable review capability and a stated v1 item, but depends on history existing and is not required for the plan→do→save loop.

**Independent Test**: Can be fully tested by completing two workouts containing the same exercise on different dates and verifying both performances appear in the exercise's history in newest-first order.

**Acceptance Scenarios**:

1. **Given** an exercise performed in multiple workouts, **When** the user opens its history, **Then** all performances are listed by date, newest first, with every set.
2. **Given** a large accumulated history, **When** the user opens an exercise's history, **Then** results are retrieved efficiently without loading the entire lifetime history into memory.

---

### User Story 8 — Rest Timer (Priority: P2)

After completing a set, the user can start a rest timer (default 90 seconds; configurable to 30s, 60s, 90s, 2 min, 3 min, or a custom duration). The timer is visible during the workout and offers Skip, +30 seconds, and Restart. It never interferes with the main workout duration timer. (PRD-§19)

**Why this priority**: Explicit v1 scope and valuable for gym pacing, but logging works completely without it.

**Independent Test**: Can be fully tested by completing a set, starting the rest timer, verifying it counts down visibly, using Skip/+30s/Restart, and verifying the workout duration timer is unaffected.

**Acceptance Scenarios**:

1. **Given** a completed set, **When** the user starts a rest timer, **Then** it counts down visibly using the configured default duration.
2. **Given** a running rest timer, **When** the user taps Skip, +30 seconds, or Restart, **Then** the timer behaves accordingly.
3. **Given** the settings, **When** the user changes the default rest duration, **Then** subsequent rest timers use the new default.
4. **Given** a running rest timer, **When** observed against the workout timer, **Then** the workout duration is unaffected by the rest timer.

---

### User Story 9 — Settings & Personalization (Priority: P2)

The user configures: weight unit (kg / lb), default rest timer duration, and theme (System / Light / Dark). The dark theme is comfortable to use in a gym environment. Preferences persist across launches. (PRD-§20, §39, §40)

**Why this priority**: Personalization that improves daily use; unit selection is a stated v1 requirement, but the core loop works with sensible defaults.

**Independent Test**: Can be fully tested by changing each setting, restarting the app, and verifying the preference persists and is applied (unit shown on weight inputs/labels, rest timer default, theme rendering).

**Acceptance Scenarios**:

1. **Given** the settings screen, **When** the user selects lb as the weight unit, **Then** the preference persists, weight entry uses lb, and all previously recorded weights are displayed converted to lb (history, summaries, previous-performance values, records), while the originally entered values remain stored losslessly.
2. **Given** theme selection, **When** the user chooses Dark, **Then** the app renders in a gym-comfortable dark theme with readable contrast; System follows the device setting.
3. **Given** a changed default rest duration, **When** a rest timer is started later, **Then** the new default applies.

---

### User Story 10 — Basic Personal Records & Statistics (Priority: P2)

The app detects basic personal records from completed workouts: heaviest weight lifted, highest number of reps, highest estimated one-rep max (1RM), and largest workout volume. Records update as new workouts are completed. The capability is structured to be expanded later. (PRD-§15, §18, §46 "basic workout statistics")

**Why this priority**: Motivating and in v1 scope, but derived from data the core loop already stores; nothing blocks on it.

**Independent Test**: Can be fully tested by completing workouts with known values and verifying the recorded maxima (heaviest weight, most reps, best estimated 1RM, largest volume) match expectations and update when surpassed.

**Acceptance Scenarios**:

1. **Given** completed workouts, **When** the user views personal records, **Then** heaviest weight, highest reps, highest estimated 1RM, and largest workout volume are shown with enough context to identify the exercise/date.
2. **Given** a new completed workout that surpasses an existing record, **When** records are viewed afterwards, **Then** the record reflects the new best.

---

### User Story 11 — Historical Workout Correction (Priority: P3)

If the user entered data incorrectly in a past workout, they can correct it. History is read-only by default; the user explicitly opts into editing a workout, where they can correct set values (weight, reps, completion, notes), add or remove exercises within that workout, and edit workout-level notes. Corrections affect only that one historical workout — never routines, templates, or other workouts. (PRD-§16; scope confirmed via clarification Q1: full editing in v1)

**Why this priority**: In v1 scope (confirmed), but error correction is infrequent compared to the core plan→do→save loop, so it is delivered after the essential journeys.

**Independent Test**: Can be fully tested by completing a workout, editing a recorded set's weight/reps in history, adding and removing an exercise in that historical workout, and verifying the corrected values persist while all other historical data, routines, and templates remain untouched.

**Acceptance Scenarios**:

1. **Given** a completed workout in history, **When** the user explicitly chooses to edit it and corrects a set's weight/reps/completion and saves, **Then** the corrected values are stored and displayed, and no other historical data changes.
2. **Given** a historical workout being edited, **When** the user adds or removes an exercise within it and saves, **Then** only that workout changes; the underlying routine and all other workouts are unaffected.
3. **Given** history browsing, **When** the user does not explicitly choose to edit, **Then** historical workouts remain read-only.

---

### Edge Cases

- **Starting a workout while another workout is already active**: the system allows at most one active workout; attempting to start another surfaces the active one (resume or discard) rather than silently creating a second session (derived from PRD-§25/§26; see Assumptions).
- **Finishing a workout with zero completed sets**: allowed; the workout is recorded as performed with zero completed sets and zero volume (see Assumptions).
- **App crash / process death / battery loss mid-workout**: no entered data is lost; every change is persisted as it happens and the workout is resumable (US3, US4).
- **Device clock change during an active workout**: duration is derived from recorded timestamps; a manual clock change may distort the displayed duration. No special compensation in v1 (known limitation).
- **Bodyweight exercises**: zero weight is valid; total volume counts only weighted completed sets.
- **Decimal weights**: values like 2.5, 22.5, 72.5, 102.5 must be accepted.
- **Renaming or deleting a routine/exercise referenced by history**: history remains intact, displaying names as they were at workout time (snapshot behavior, US5).
- **Deleting an exercise that is referenced by routine templates**: the deletion must not corrupt routines; affected routine references are handled gracefully (proposed default in Assumptions; mechanism decided during planning).
- **Search with no results**: a clear empty-result state is shown.
- **Routine duplication naming**: duplicates get a derived name (e.g., "… Copy") that the user can rename; routine names are not required to be unique (see Assumptions).
- **Very large history (years of workouts)**: lists load incrementally and remain responsive; the full lifetime history is never loaded at once (PRD-§51).
- **Empty states**: no routines ("No workout routines yet." + create action) and no history ("Your completed workouts will appear here.") (PRD-§41).

## Requirements *(mandatory)*

### Functional Requirements

**Home & Navigation**

- **FR-001**: The home screen MUST display the user's routines as cards showing routine name, number of exercises, last-performed date, and a START action.
- **FR-002**: The home screen MUST provide prominent access to My Routines, Recent Workouts, Workout History, and a Create Routine action.
- **FR-003**: Selecting a routine card MUST open its details; selecting START MUST immediately create a workout session and start the workout timer.
- **FR-004**: Navigation MUST provide Home, History, Exercises (library), and Settings areas; an active workout MUST switch into a dedicated workout experience rather than forcing normal navigation.

**Exercise Library**

- **FR-005**: The system MUST ship with a built-in library of common exercises grouped by muscle group (Chest, Back, Shoulders, Arms, Legs) as listed in the source requirements, populated on first install.
- **FR-006**: Users MUST be able to create custom exercises with a name and optional description, muscle group, and notes; creation MUST NOT be blocked because an exercise is absent from the built-in library.
- **FR-007**: Users MUST be able to edit exercises.
- **FR-008**: The exercise library MUST support search, and search results MUST include custom exercises.
- **FR-009**: Deleting an exercise from the library MUST NOT destroy or alter historical workout data.
- **FR-010**: The system MUST NOT automatically create workout routines on first install; only the exercise library is seeded.

**Routine Management**

- **FR-011**: Users MUST be able to create a routine with a required name and an optional description.
- **FR-012**: Users MUST be able to rename, edit, and delete routines; deletion MUST require explicit confirmation.
- **FR-013**: Deleting a routine MUST NOT delete or alter any historical workout session created from it.
- **FR-014**: Users MUST be able to duplicate a routine, producing an independently editable copy including its exercises and planned sets.
- **FR-015**: Users MUST be able to manually reorder routines on the home screen (e.g., drag to reorder), and the chosen order MUST persist across app launches.
- **FR-016**: Users MUST be able to add exercises to a routine (from the library or custom), remove them, and reorder them within the routine.
- **FR-017**: Each exercise inside a routine MUST support optional per-exercise notes.
- **FR-018**: Users MUST be able to configure planned sets per routine exercise (set number, target reps, target weight), including adding, editing, and deleting template sets.

**Starting a Workout**

- **FR-019**: Starting a workout from a routine MUST create a new workout session, record the start timestamp, start the workout timer, and copy the routine's exercises (with their planned sets) into the session.
- **FR-020**: The system MUST allow at most one active workout session at a time; attempting to start another workout while one is active MUST surface the active workout (resume/discard) rather than silently creating a second one.
- **FR-021**: The workout timer MUST display continuously increasing elapsed time and MUST remain accurate when the user navigates away, the app is backgrounded, the screen turns off, or the UI loses focus; duration MUST be derived from recorded timestamps (duration = current time − start time), not from an in-memory counter.

**Active Workout & Set Logging**

- **FR-022**: The active workout screen MUST be optimized for fast gym use: minimal typing, large touch targets, fast set completion, readable text, and immediate saving.
- **FR-023**: Each workout set MUST support set number, repetitions, weight, completion status, and optional notes.
- **FR-024**: Weight input MUST support decimal values; repetitions MUST be whole numbers; negative weights and negative repetitions MUST be rejected; zero weight MUST be allowed (bodyweight exercises).
- **FR-025**: Marking a set complete MUST save the set immediately and mark it visually as completed; completion MUST NOT be communicated by color alone.
- **FR-026**: Users MUST be able to add sets during an active workout beyond those originally planned in the routine.
- **FR-027**: Every meaningful change during an active workout (weight edit, reps edit, set completion, set added/deleted, exercise added/removed, notes) MUST be persisted immediately so no data is lost to crashes, process termination, or power loss.
- **FR-028**: Users MUST be able to add an exercise during a workout (from the library or newly created); the system MUST ask whether to also add the exercise permanently to the routine ("Add to routine" vs "This workout only").
- **FR-029**: Users MUST be able to skip/remove an exercise from the current workout without modifying the underlying routine; "remove from routine" MUST exist only as a clearly separate action with a distinct meaning.
- **FR-030**: The set measurement model MUST support WEIGHT_AND_REPS and REPS_ONLY exercise types in v1, and MUST be designed so DURATION and DISTANCE types can be added later without a major data redesign.

**Rest Timer**

- **FR-031**: After completing a set, the user MUST be able to start a rest timer; the default duration is 90 seconds, with options for 30s, 60s, 90s, 2 min, 3 min, and a custom duration.
- **FR-032**: The rest timer MUST be visible during the workout, MUST offer Skip, +30 seconds, and Restart, and MUST NOT interfere with the main workout duration timer.

**Finishing & Summary**

- **FR-033**: FINISH WORKOUT MUST present a confirmation showing duration, number of exercises, number of completed sets, and total volume before completing.
- **FR-034**: On confirmation, the system MUST record the end timestamp, calculate duration, mark the workout completed, persist all exercise and set data, and return a workout summary.
- **FR-035**: The workout summary MUST display routine name, date, duration, exercise count, set count, total reps, total volume, and a per-exercise breakdown of completed sets.
- **FR-036**: Total volume MUST be calculated as the sum of weight × repetitions across all completed weighted sets.

**Canceling a Workout**

- **FR-037**: Canceling an active workout MUST require explicit confirmation with "Keep working out" and "Discard workout" options; navigation or app closure MUST never accidentally discard a workout.

**Workout Recovery**

- **FR-038**: On app launch, the system MUST detect any unfinished workout and present it with the workout name, how long ago it started, and a RESUME action.
- **FR-039**: Resuming MUST restore the workout exactly where the user left off, with all already-entered sets present and elapsed duration correct.

**History & Previous Performance**

- **FR-040**: Workout history MUST list completed workouts ordered newest first, showing date, workout name, and duration.
- **FR-041**: Opening a history entry MUST display the complete historical workout read-only by default, including every exercise and set as performed.
- **FR-042**: The system MUST allow full editing of a historical workout to correct incorrectly entered data: set values (weight, reps, completion status, notes), exercises within the workout (add/remove), and workout-level details (notes). Editing MUST require an explicit user action (history is read-only by default), and edits MUST affect only that historical workout — never routines, templates, or other workouts.
- **FR-043**: Historical records MUST preserve the routine and exercise names as they existed at workout time; later renames, edits, or deletions of routines/exercises MUST NOT alter displayed historical data.
- **FR-044**: Users MUST be able to view the history of an individual exercise (all performances by date, newest first, with all sets), retrieved efficiently even with large accumulated history.
- **FR-045**: When starting a workout, the system MUST display the user's most recent performance for each exercise alongside today's sets.
- **FR-046**: Today's set values MUST be pre-filled from the previous performance where available, and MUST remain editable.

**Personal Records**

- **FR-047**: The system MUST detect and present basic personal records: heaviest weight, highest number of reps, highest estimated 1RM, and largest workout volume, in a way that can be expanded later.

**Settings & Units**

- **FR-048**: Users MUST be able to select their weight unit (kg / lb); the selection MUST persist and apply to weight entry and display.
- **FR-049**: When the user switches weight units, already-recorded weights MUST be converted for display into the currently selected unit everywhere weights are shown (history, summaries, previous-performance values, personal records). Recorded values MUST be stored in a unit-agnostic way so conversion is lossless and reversible; the values the user originally typed are preserved.
- **FR-050**: Users MUST be able to configure the default rest timer duration.
- **FR-051**: Users MUST be able to choose System / Light / Dark theme; the selection MUST persist, and the dark theme MUST be comfortable in a gym environment.
- **FR-052**: Weights MUST be stored in a way that keeps future unit conversion possible.

**Data Persistence & Integrity**

- **FR-053**: All workout information MUST be stored permanently in local persistent storage; the application MUST work completely offline (offline-first).
- **FR-054**: Important workout data MUST NOT exist exclusively in transient memory, UI state, or lightweight preferences; it MUST exist in persistent storage.
- **FR-055**: The data model MUST be normalized and MUST distinguish template entities (routines and their planned exercises/sets) from historical record entities (workout sessions, their exercises, and sets), using robust unique identifiers, explicit relationships, preserved ordering, and efficient lookup paths for history queries.
- **FR-056**: Historical entities MUST carry snapshot copies of names (routine name, exercise name) so historical displays remain correct after later renames or deletions.
- **FR-057**: Data schema evolution MUST be non-destructive; application updates MUST never accidentally erase workout history.
- **FR-058**: The application MUST remain responsive with years of accumulated history: history MUST be loaded incrementally with appropriate limits and MUST never load the complete lifetime history into memory at once.

**UX, Accessibility & Error Handling**

- **FR-059**: Empty states MUST guide the user to the first action: no routines ("No workout routines yet." + create action) and no history ("Your completed workouts will appear here.").
- **FR-060**: Errors MUST be handled gracefully with user-friendly messages; workout data MUST never be silently discarded; technical implementation details and stack traces MUST never be exposed to users; recoverable UI state MUST be preserved where possible.
- **FR-061**: The application MUST be accessible: labeled controls, adequate touch targets, content descriptions where appropriate, readable text sizes and contrast; set completion MUST NOT be communicated by color alone.
- **FR-062**: Unnecessary animations and complex screens MUST be avoided; the active workout experience MUST prioritize Weight, Reps, and Complete Set interactions.

### Key Entities

- **Exercise**: A physical exercise, either built-in or user-created. Attributes: identity, name, optional muscle group, optional description, optional notes, whether it is custom, creation/update timestamps, and a deletion state that never destroys history.
- **Routine**: A reusable workout template. Attributes: identity, name, optional description, creation/update timestamps.
- **Routine Exercise**: The ordered link between a routine and an exercise, with position and optional notes.
- **Set Template**: A planned set within a routine exercise: set number, target repetitions, target weight.
- **Workout Session**: A historical record of one gym session. Attributes: identity, optional reference to the source routine, snapshot of the routine name at workout time, start time, end time, status (active / completed / discarded), optional notes, creation time.
- **Workout Exercise**: An exercise as performed in a session: ordered position, optional reference to the exercise, snapshot of the exercise name at workout time, optional notes.
- **Workout Set**: A set as actually performed: set number, repetitions, weight, completion status, completion time, optional notes.
- **Settings**: User preferences — weight unit, default rest duration, theme, and room for future preferences.

Relationships: a Routine contains ordered Routine Exercises, each with Set Templates. A Workout Session contains ordered Workout Exercises, each with Workout Sets. A Workout Session optionally references its source Routine; a Workout Exercise optionally references an Exercise. Historical entities store name snapshots so they never depend on the current state of templates or library entries.

## Business Rules & Constraints

Core domain rules (PRD-§53, enforced throughout):

- **BR-01**: Routines are templates (intent); workout sessions are historical records (what actually happened).
- **BR-02**: Editing a routine MUST never edit previous workouts.
- **BR-03**: Deleting a routine MUST never delete previous workouts.
- **BR-04**: Deleting an exercise MUST never destroy or corrupt workout history (protection mechanism — e.g., soft deletion and/or snapshots — decided during planning).
- **BR-05**: Workout data MUST save continuously during an active workout.
- **BR-06**: An active workout MUST survive application restart.
- **BR-07**: Timer duration MUST be derived from timestamps, never from an in-memory counter.
- **BR-08**: Custom exercises are always allowed.
- **BR-09**: Users may modify sets during an active workout.
- **BR-10**: Users may modify exercises during an active workout.
- **BR-11**: Historical workouts MUST preserve the data as it existed when the workout occurred.

Derived rules and constraints:

- **BR-12**: Total volume = SUM(weight × repetitions) across completed weighted sets.
- **BR-13**: Workout duration = end timestamp − start timestamp.
- **BR-14**: At most one active workout session at a time (derived from PRD-§25/§26; see Assumptions).
- **BR-15**: A discarded workout is removed permanently and never appears in history (see Assumptions).
- **BR-16**: Weight ≥ 0 with decimal support; repetitions ≥ 0 as whole numbers; zero weight is valid (bodyweight).
- **BR-17**: v1 measurement types are WEIGHT_AND_REPS and REPS_ONLY; the model MUST accommodate DURATION and DISTANCE later without major redesign.
- **BR-18**: Offline-first: every v1 capability MUST work with zero network connectivity.
- **BR-19**: Data schema changes MUST be explicit and non-destructive; workout history is user data that MUST survive application updates.
- **BR-20**: The exercise library is seeded on first install; routines are NEVER seeded.
- **BR-21**: Future capabilities (cloud sync, accounts, analytics, charts, social) MUST NOT be blocked by v1 decisions; local persistent storage remains the offline source of truth that a future sync layer could sit above (PRD-§47, §48).

Quality & verification constraint (PRD-§43): automated tests MUST cover at minimum — creating a routine, adding exercises, starting a workout, completing sets, saving a workout, resuming an active workout, finishing a workout, calculating duration, calculating volume, previous-workout retrieval, routine deletion not deleting history, and exercise deletion not corrupting history.

## Scope Boundaries

**In scope for v1** (PRD-§46): exercise library; custom exercises; create/edit/delete routines; add/remove/reorder exercises; configure sets; start workout; workout timer; enter weight; enter reps; complete sets; add/delete sets; add exercise while working out; finish workout; resume unfinished workout; workout history; exercise history; previous workout values; persistent local storage; kg/lb setting; dark mode; basic workout statistics; automated tests for core logic.

**Additionally confirmed in scope for v1 via clarifications**: full editing of historical workouts (Q1 — set values, exercises within a workout, and workout notes, behind an explicit edit action); display conversion of previously recorded weights when the weight unit is switched (Q2); manual reordering of routines on the home screen (Q3).

**Explicitly out of scope for v1** (PRD-§47): account registration, social networking, friends, leaderboards, payments, subscriptions, AI workout recommendations, nutrition tracking, cloud synchronization, Wear OS, Apple Health, Google Health Connect integration, personal trainer marketplace, complex analytics dashboards.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time user can complete the full end-to-end journey — create "Chest Workout" with 3 exercises × 3 sets, start it, see the timer running, log weight/reps for every set, mark sets complete, close the app mid-workout, reopen and resume, finish, see the summary, find and inspect the workout in history, repeat the routine and see previous values beside each exercise, modify and then delete the routine and confirm history is unchanged — with 100% of steps succeeding (PRD-§56 definition of done).
- **SC-002**: When previous performance exists, completing a set identical to the last session requires at most 1 user action per set (confirm completion), thanks to pre-filled values.
- **SC-003**: Zero data loss on interruption: after force-stopping the app mid-workout, 100% of entered sets are recoverable on relaunch.
- **SC-004**: Workout duration stays correct across navigation, background, and screen-off — displayed elapsed time matches real elapsed time within seconds, derived from timestamps.
- **SC-005**: History immutability: renaming, editing, or deleting routines or exercises changes 0 historical records (verified by regression tests).
- **SC-006**: History, routines, and exercise-history screens open and scroll smoothly with 1,000+ stored workouts (years of history), loading data incrementally.
- **SC-007**: 100% of v1 functionality works with zero network connectivity.
- **SC-008**: First launch presents a seeded library of 30+ common exercises across 5 muscle groups and zero routines.
- **SC-009**: Logging a single set requires at most 3 interactions (enter weight, enter reps, mark complete) with large touch targets and no mandatory navigation.
- **SC-010**: All core domain logic listed in the quality constraint has automated tests, and they pass.

## Assumptions

Recorded defaults where the source requirements were silent (each can be revisited):

- **Single user, single device**: no accounts, profiles, or multi-user support in v1 (consistent with PRD-§47).
- **One active workout at a time** (BR-14): the recovery flow (PRD-§25) implies a single active session; starting another surfaces resume/discard.
- **Discarded workouts are removed permanently** (BR-15) and never appear in history.
- **Unfinished workouts remain resumable indefinitely** until finished or discarded (no automatic expiry).
- **Rest timer is ephemeral**: it is not restored after app restart; the workout session and its timer are restored.
- **Recent Workouts on home** shows the 3 most recent completed workouts.
- **"Last performed"** on a routine card reflects the most recent completed session started from that routine.
- **Finishing with zero completed sets is allowed** and recorded with zero completed sets and zero volume.
- **Deleting an exercise referenced by routine templates** removes it from those routine templates (with confirmation) while leaving history untouched; exact behavior/mechanism confirmed during planning.
- **Routine and exercise names are not required to be unique**; duplicates get derived names ("… Copy") on duplication.
- **Estimated 1RM** uses a standard estimation formula (proposed: Epley: weight × (1 + reps/30)); final formula is an open question (OQ-1).
- **Technology neutrality**: this specification intentionally makes no technology decisions. The source requirements document (PRD-§29–§32, §49–§50) states a preferred implementation direction (native Android with Kotlin, Jetpack Compose, Material 3, Room, Hilt, Coroutines/Flow, Navigation Compose, a layered UI → domain → data architecture, non-destructive migrations, and a suggested package structure). That direction is recorded here as input for the planning phase, not adopted by this specification; final technology and architecture decisions are deferred to the implementation plan.
- PRD sections on project structure, use-case decomposition, and code quality (PRD-§30, §32, §49) inform the planning phase and are governed by the project constitution; they are not restated as functional requirements here.

## Open Questions

Flagged ambiguities (per the request to flag rather than assume). Q1–Q3 were raised with the product owner and resolved on 2026-09-02; their answers are incorporated into the spec. OQ items have proposed defaults recorded in Assumptions and can be confirmed during planning.

- **Q1 (scope) — RESOLVED**: Historical workout editing is in v1 scope at full level (set values, exercises within the workout, workout notes), behind an explicit edit action; history remains read-only by default. → FR-042, User Story 11.
- **Q2 (UX/data semantics) — RESOLVED**: On unit switch, previously recorded weights are converted for display into the selected unit everywhere; values are stored unit-agnostically so conversion is lossless and the originally typed values are preserved. → FR-049.
- **Q3 (scope) — RESOLVED**: Manual routine reordering (drag to reorder) is in v1 scope; the order persists across launches. → FR-015.
- **OQ-1**: 1RM estimation formula (proposed default: Epley).
- **OQ-2**: Behavior when deleting an exercise still referenced by routine templates (proposed default: remove from templates with confirmation; history untouched).
- **OQ-3**: Whether "last performed" and history ordering should ever include non-completed (abandoned-but-not-discarded) sessions (proposed default: no — only completed sessions appear in history).
