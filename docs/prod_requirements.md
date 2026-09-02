# Build an Android Fitness / Workout Tracking App

You are a senior Android engineer, mobile architect, UX designer, and database designer.

I want you to design and build a production-quality Android fitness application for creating workout routines, performing workouts, and permanently tracking workout history.

The application should be simple and fast enough to use during an actual gym session.

Do not treat this as a prototype. Structure the code so that additional capabilities such as cloud sync, authentication, analytics, AI recommendations, charts, and social features can be added later without rewriting the core application.

---

# 1. Application Overview

Build an Android workout tracking application where a user can:

1. Create workout routines.
2. Add exercises to routines.
3. Configure sets, repetitions, and weights.
4. Start a workout from an existing routine.
5. Log exercises and sets while training.
6. Track the duration of every workout.
7. Store every completed workout permanently.
8. Review previous workouts.
9. Edit routines without modifying historical workout records.
10. Quickly reuse previous workout performance when doing the same routine again.

Example:

Routine:

Chest Workout

Exercises:

* Bench Press
* Incline Dumbbell Press
* Chest Fly
* Dips

Bench Press may contain:

Set 1: 10 reps × 60 kg
Set 2: 8 reps × 70 kg
Set 3: 6 reps × 80 kg

The application should store this information for every workout session so the user can see exactly what they performed on any previous date.

---

# 2. Core Design Principle

There must be a clear distinction between:

## Workout Templates / Routines

A routine defines what the user intends to perform.

Example:

Chest Workout

* Bench Press
* Incline Dumbbell Press
* Chest Fly

## Workout Sessions

A workout session represents what actually happened during a particular gym session.

Example:

Chest Workout
Monday, 31 August

Bench Press

Set 1: 10 × 60 kg
Set 2: 8 × 70 kg
Set 3: 6 × 75 kg

A routine may later be edited, but previous workout sessions MUST NOT change.

For example:

If the user completed a Chest Workout containing Bench Press and Chest Fly on 1 August, and later removes Chest Fly from the routine, the 1 August workout history must still contain Chest Fly.

Historical workout data must therefore be stored independently from the editable routine template.

---

# 3. Main Application Features

## Feature 1 — Home Screen

Create a clean home screen.

The home screen should prominently display:

* My Routines
* Recent Workouts
* Create Routine button
* Workout History

Each routine should appear as a card.

Example:

Chest Workout
4 exercises
Last performed: 28 Aug
[START]

Back Workout
5 exercises
Last performed: 26 Aug
[START]

Leg Workout
6 exercises
Last performed: 23 Aug
[START]

Clicking the routine opens its details.

Clicking START immediately creates a workout session and starts the workout timer.

---

# 4. Create Routine

The user must be able to create a workout routine.

Required fields:

* Routine name
* Optional description

Example:

Name:
Chest Workout

Description:
Heavy chest workout

After creating the routine, allow the user to add exercises.

Users must be able to:

* Create routine
* Rename routine
* Edit routine
* Delete routine
* Duplicate routine
* Reorder routines if appropriate

Before deleting a routine, show a confirmation dialog.

Deleting a routine must NOT delete historical workout sessions that were created from it.

---

# 5. Exercise Management

Each routine can contain multiple exercises.

Example:

Chest Workout

1. Barbell Bench Press
2. Incline Dumbbell Press
3. Chest Fly
4. Dips

Users must be able to:

* Add exercise
* Edit exercise
* Remove exercise from routine
* Reorder exercises
* Add notes to an exercise

Each exercise should contain:

* Exercise name
* Optional description
* Optional muscle group
* Optional notes

Example:

Exercise:
Barbell Bench Press

Muscle Group:
Chest

Notes:
Keep elbows tucked.

---

# 6. Exercise Library

Create an exercise library.

The application should contain some predefined common exercises such as:

Chest:

* Barbell Bench Press
* Dumbbell Bench Press
* Incline Dumbbell Press
* Chest Fly
* Cable Fly
* Dips

Back:

* Deadlift
* Barbell Row
* Dumbbell Row
* Lat Pulldown
* Pull Up
* Seated Cable Row

Shoulders:

* Overhead Press
* Dumbbell Shoulder Press
* Lateral Raise
* Front Raise
* Rear Delt Fly

Arms:

* Barbell Curl
* Dumbbell Curl
* Hammer Curl
* Tricep Pushdown
* Skull Crusher

Legs:

* Squat
* Leg Press
* Leg Extension
* Leg Curl
* Romanian Deadlift
* Calf Raise

The user must also be able to create custom exercises.

Do not prevent users from creating exercises because they do not exist in the built-in exercise library.

---

# 7. Sets

Each exercise within a routine can have a planned number of sets.

Example:

Bench Press

Set 1
Set 2
Set 3

A workout set should support:

* Set number
* Repetitions
* Weight
* Completion status
* Optional notes

Example:

Set 1:
10 reps
60 kg

Set 2:
8 reps
70 kg

Set 3:
6 reps
80 kg

Users must be able to:

* Add a set
* Edit a set
* Delete a set
* Change repetitions
* Change weight
* Mark a set complete

The user should be able to add additional sets during an active workout even if they were not originally part of the routine.

---

# 8. Starting a Workout

The user selects a routine and presses:

START WORKOUT

Immediately:

1. Create a new workout session.
2. Record the start timestamp.
3. Start a workout timer.
4. Copy the routine exercises into the workout session.
5. Display the active workout screen.

The timer should show:

00:00:00

and continuously increase while the workout is active.

The workout timer must continue accurately even if:

* The user navigates to another screen.
* The application goes into the background.
* The phone screen turns off.
* The app process temporarily loses UI focus.

Do not rely purely on an in-memory counter.

The timer should derive workout duration from stored timestamps.

For example:

duration = currentTime - workoutStartTime

---

# 9. Active Workout Screen

The active workout screen is one of the most important parts of the application.

It should be optimized for fast interaction during a gym session.

Example:

Chest Workout

Duration: 00:31:42

---

BENCH PRESS

Previous workout:

60 kg × 10
70 kg × 8
75 kg × 7

Today's workout:

Set | Previous | Weight | Reps | Done

1 | 60×10 | 60 kg | 10 | ✓
2 | 70×8  | 70 kg | 8  | ✓
3 | 75×7  | 75 kg | 6  | □

[+ ADD SET]

---

INCLINE DUMBBELL PRESS

Set | Previous | Weight | Reps | Done

1 | 25×10 | 25 kg | 10 | □
2 | 25×9  | 25 kg | 8  | □
3 | 22×10 | 22 kg | 10 | □

---

[FINISH WORKOUT]

Each exercise should display the user's previous performance where available.

This is important because the user should be able to compare today's workout with the previous workout.

---

# 10. Previous Workout Values

When starting a workout, show the user's most recent performance for each exercise.

Example:

Previous Bench Press:

Set 1: 60 kg × 10
Set 2: 70 kg × 8
Set 3: 75 kg × 7

These values should appear next to today's set.

Optionally prepopulate today's values using previous workout values.

For example:

Previous:
70 kg × 8

Today's fields automatically start as:

Weight:
70

Reps:
8

The user can then modify them.

This feature should significantly reduce typing during gym sessions.

---

# 11. Completing Sets

The user should be able to enter:

* Weight
* Number of repetitions

and then press a completion button.

Example:

70 kg
8 reps

[✓]

When completed:

* Save the set immediately.
* Mark it visually as completed.
* Record completion if useful.

Do not wait until the entire workout finishes before saving workout data.

Persist changes continuously so data is not lost if the application crashes or the phone shuts down.

---

# 12. Add Exercise During Workout

The user may decide to perform an exercise that was not originally in the routine.

Provide:

* ADD EXERCISE

The user can select an exercise from the library or create a new one.

Adding an exercise during a workout should add it to that workout session.

Ask the user whether they also want to add the exercise permanently to the routine.

Example dialog:

"Add Cable Fly to Chest Workout for future workouts?"

Options:

ADD TO ROUTINE
THIS WORKOUT ONLY

---

# 13. Remove Exercise During Workout

Allow the user to remove or skip an exercise during the current workout.

Do not automatically remove it from the underlying routine.

Provide actions such as:

Skip exercise
Remove from this workout

If appropriate, separately provide:

Remove from routine

These actions must have clearly different meanings.

---

# 14. Finish Workout

Provide a prominent:

FINISH WORKOUT

button.

When clicked, display a confirmation.

Example:

Finish Chest Workout?

Duration: 01:04:32
Exercises: 5
Completed sets: 16
Total volume: 7,850 kg

Options:

CANCEL
FINISH

When confirmed:

* Record end timestamp.
* Calculate duration.
* Mark workout as completed.
* Persist all exercise data.
* Persist all set data.
* Return to a workout summary screen.

---

# 15. Workout Summary

After finishing a workout, show:

Chest Workout

31 August 2026

Duration:
1h 04m

Exercises:
5

Sets:
16

Total reps:
132

Total volume:
7,850 kg

Exercise breakdown:

Bench Press
60 × 10
70 × 8
75 × 6

Incline Dumbbell Press
25 × 10
25 × 9
25 × 8

Chest Fly
15 × 12
15 × 12
15 × 10

Total volume can be calculated as:

SUM(weight × repetitions)

across all completed weighted sets.

---

# 16. Workout History

Create a Workout History screen.

Display workouts ordered newest first.

Example:

31 Aug
Chest Workout
1h 04m

28 Aug
Back Workout
58m

26 Aug
Leg Workout
1h 12m

Clicking a workout opens the complete historical workout.

Historical workout data should be read-only by default.

Optionally provide an Edit Workout action if the user needs to correct incorrectly entered data.

---

# 17. Exercise History

Users should also be able to select an individual exercise and see its history.

Example:

BARBELL BENCH PRESS

31 Aug

60 × 10
70 × 8
75 × 6

24 Aug

60 × 10
67.5 × 8
72.5 × 7

17 Aug

57.5 × 10
65 × 9
70 × 8

This feature will later support progress charts.

Design the database so exercise history can be queried efficiently.

---

# 18. Personal Records

Detect basic personal records.

Examples:

Heaviest weight:
100 kg

Highest number of reps:
15

Highest estimated 1RM:
110 kg

Largest workout volume:
8,420 kg

Personal records do not need to be highly sophisticated in version 1, but structure the system so this capability can be expanded later.

---

# 19. Rest Timer

After completing a set, optionally start a rest timer.

Default rest duration could be:

90 seconds

Allow configuration of:

* 30 seconds
* 60 seconds
* 90 seconds
* 2 minutes
* 3 minutes
* Custom

The timer should be visible during the workout.

Allow:

Skip
+30 seconds
Restart

Rest timers should not interfere with the main workout duration timer.

---

# 20. Units

Support:

Kilograms
Pounds

Store weight carefully so future unit conversion is possible.

The user should be able to select their preferred unit in Settings.

Example:

Weight Unit:
kg

---

# 21. Data Persistence

All workout information must be stored permanently in a local database.

Use an offline-first architecture.

The application must work completely without internet connectivity.

Use Android Room backed by SQLite for local persistence.

Database access should be abstracted through repositories so a remote/cloud database can be added later.

Do not store important workout data exclusively in:

* SharedPreferences
* UI state
* ViewModels
* temporary memory

Workout records must exist in persistent storage.

---

# 22. Database Design

Design a normalized relational model.

At minimum consider entities similar to:

Exercise

* id
* name
* muscleGroup
* description
* notes
* isCustom
* createdAt
* updatedAt

Routine

* id
* name
* description
* createdAt
* updatedAt

RoutineExercise

* id
* routineId
* exerciseId
* position
* notes

RoutineSetTemplate

* id
* routineExerciseId
* setNumber
* targetReps
* targetWeight

WorkoutSession

* id
* routineId nullable
* routineNameSnapshot
* startTime
* endTime
* status
* notes
* createdAt

WorkoutExercise

* id
* workoutSessionId
* exerciseId nullable
* exerciseNameSnapshot
* position
* notes

WorkoutSet

* id
* workoutExerciseId
* setNumber
* repetitions
* weight
* status
* completedAt
* notes

Settings

* weightUnit
* defaultRestDuration
* other user preferences

Use foreign keys and appropriate indexes.

Use UUIDs or another robust ID strategy.

---

# 23. Snapshot Historical Information

Historical records must not rely solely on editable records.

For example, store:

routineNameSnapshot

and:

exerciseNameSnapshot

inside historical workout entities where appropriate.

This prevents historical displays from becoming incorrect if an exercise or routine is later renamed or deleted.

Example:

A user completes:

"Chest Workout"

and later renames the template to:

"Chest Strength"

The historical workout should still be capable of displaying the original name.

---

# 24. Handling Deleted Exercises

Deleting an exercise from the exercise library must not destroy historical workout data.

Use either:

* soft deletion

or:

* historical snapshots

or preferably a sensible combination.

Never cascade-delete completed workout history merely because a routine or exercise is deleted.

---

# 25. Workout Recovery

An important requirement:

If the application is closed while a workout is active, reopening the application should detect the unfinished workout.

Display:

Workout in progress

Chest Workout
Started 43 minutes ago

[RESUME WORKOUT]

The user should be able to resume exactly where they left off.

All already-entered sets should still exist.

---

# 26. Cancel Workout

Allow the user to cancel an active workout.

Ask for confirmation:

Discard this workout?

Options:

KEEP WORKING OUT

DISCARD WORKOUT

Do not accidentally delete a workout because of navigation or app closure.

---

# 27. User Experience Requirements

The application should feel designed specifically for gym usage.

Optimize for:

* minimal typing
* large touch targets
* fast set completion
* one-handed use where practical
* dark mode
* readable text
* minimal navigation
* immediate saving
* clear workout state

Avoid unnecessary animations and complex screens.

The active workout page should prioritize:

Weight
Reps
Complete Set

These should be extremely easy to interact with.

---

# 28. Navigation

Suggested navigation structure:

Bottom navigation:

Home
History
Exercises
Settings

Home contains routines.

During an active workout, switch into a dedicated workout experience rather than forcing the user through normal navigation.

---

# 29. Technical Stack

Use native Android development.

Preferred technologies:

* Kotlin
* Jetpack Compose
* Material 3
* Room
* ViewModel
* Kotlin Coroutines
* Flow / StateFlow
* Navigation Compose
* Dependency Injection using Hilt

Follow modern Android architecture.

Use a layered structure similar to:

UI / Presentation

↓

Domain

↓

Repository interfaces

↓

Data layer

↓

Room Database

Avoid putting business logic directly inside Compose screens.

---

# 30. Suggested Project Structure

Use something similar to:

app/

data/
local/
dao/
database/
entity/
repository/

domain/
model/
repository/
usecase/

ui/
home/
routines/
exercises/
workout/
history/
settings/
components/
navigation/

di/

util/

The architecture should remain understandable and not introduce unnecessary enterprise complexity.

---

# 31. State Management

Use:

ViewModel + StateFlow

Each major screen should have a UI state representation.

For example:

WorkoutUiState

containing:

* workout
* elapsedDuration
* exercises
* sets
* restTimer
* loadingState
* errorState

The UI should render from state rather than manually coordinating database values across composables.

---

# 32. Domain Operations / Use Cases

Create clear business operations such as:

CreateRoutine

UpdateRoutine

DeleteRoutine

AddExerciseToRoutine

RemoveExerciseFromRoutine

StartWorkout

ResumeWorkout

CompleteSet

AddSet

DeleteSet

AddExerciseToWorkout

FinishWorkout

CancelWorkout

GetWorkoutHistory

GetExerciseHistory

GetPreviousExercisePerformance

CalculateWorkoutVolume

Keep these operations testable independently of the UI.

---

# 33. Data Autosave

During an active workout:

Every meaningful change should be saved immediately.

For example:

User changes weight:

Persist it.

User changes reps:

Persist it.

User completes a set:

Persist it.

User adds a set:

Persist it.

User adds an exercise:

Persist it.

This avoids losing a workout due to:

* app crashes
* operating-system process termination
* low battery
* accidental app closure

---

# 34. Input Behaviour

Weight input should support decimals.

Examples:

20
22.5
72.5
102.5

Repetitions should normally be integer values.

Prevent invalid values such as:

negative weight
negative repetitions

Do not unnecessarily prevent zero-weight exercises because bodyweight exercises may use different semantics.

---

# 35. Bodyweight Exercises

Design the system so bodyweight exercises can be supported.

Examples:

Pull Ups
Push Ups
Dips

Do not force every exercise to require a weight.

Exercise measurement type should eventually support concepts such as:

WEIGHT_AND_REPS

REPS_ONLY

DURATION

DISTANCE

However, version 1 can primarily implement:

WEIGHT_AND_REPS
REPS_ONLY

Design the model so duration/distance exercises can be added later without a major database redesign.

---

# 36. Routine Editing

Routine detail screen should allow:

* Rename routine
* Edit description
* Add exercise
* Remove exercise
* Reorder exercises
* Configure default sets
* Duplicate routine
* Delete routine

Example:

CHEST WORKOUT

Bench Press
3 sets

Incline Dumbbell Press
3 sets

Chest Fly
3 sets

[+ ADD EXERCISE]

[EDIT ROUTINE]

[START WORKOUT]

---

# 37. Routine Duplication

Provide a duplicate feature.

Example:

Duplicate:

Chest Workout

Result:

Chest Workout Copy

The user can then rename and modify it.

This is useful for creating variations such as:

Chest Heavy
Chest Light

---

# 38. Search

The exercise library should support search.

Typing:

bench

should return relevant exercises.

Search should include custom exercises.

---

# 39. Settings

Create a settings page.

Include at minimum:

Weight unit:
kg / lb

Default rest timer:
90 seconds

Theme:
System / Light / Dark

Potential future settings should be easy to add.

---

# 40. Dark Mode

Support:

System theme
Light theme
Dark theme

The dark theme should be comfortable to use in a gym environment.

---

# 41. Empty States

Create good empty-state experiences.

Example when no routines exist:

"No workout routines yet."

[CREATE YOUR FIRST ROUTINE]

Example when no workout history exists:

"Your completed workouts will appear here."

---

# 42. Error Handling

Handle errors gracefully.

Never silently discard workout data.

Examples:

Database operation failed.

Display an appropriate message and keep recoverable UI state when possible.

Avoid exposing stack traces or technical implementation details to the user.

---

# 43. Testing

Create tests for important business logic.

At minimum test:

* Creating a routine
* Adding exercises
* Starting workout
* Completing sets
* Saving workout
* Resuming active workout
* Finishing workout
* Calculating duration
* Calculating volume
* Previous-workout retrieval
* Routine deletion does not delete workout history
* Exercise deletion does not corrupt workout history

Add unit tests for use cases and repository logic.

Use database tests where appropriate.

---

# 44. Seed Data

On first installation, populate the exercise library with common exercises.

Do NOT automatically create workout routines.

The user should create their own routines.

---

# 45. Example End-to-End User Journey

The application must successfully support this flow:

Step 1:

User opens application.

Step 2:

User selects:

Create Routine

Step 3:

User enters:

Chest Workout

Step 4:

User adds:

Bench Press
Incline Dumbbell Press
Chest Fly

Step 5:

User configures three sets for each exercise.

Step 6:

User saves routine.

Step 7:

Chest Workout appears on Home.

Step 8:

User presses:

START

Step 9:

Workout timer begins.

Step 10:

User selects Bench Press.

Step 11:

User enters:

Set 1:
60 kg
10 reps

and marks it complete.

Step 12:

User enters:

Set 2:
70 kg
8 reps

Step 13:

User continues through all exercises.

Step 14:

User presses:

FINISH WORKOUT

Step 15:

Workout is permanently stored.

Step 16:

One week later, user opens Chest Workout again.

Step 17:

Next to Bench Press the application displays:

Previous:

60 × 10
70 × 8
75 × 6

Step 18:

The user can reuse these numbers or modify them.

This entire workflow must work reliably.

---

# 46. Version 1 Scope

Version 1 MUST include:

* Exercise library
* Custom exercises
* Create/edit/delete routines
* Add/remove/reorder exercises
* Configure sets
* Start workout
* Workout timer
* Enter weight
* Enter reps
* Complete sets
* Add/delete sets
* Add exercise while working out
* Finish workout
* Resume unfinished workout
* Workout history
* Exercise history
* Previous workout values
* Persistent Room database
* kg/lb setting
* Dark mode
* Basic workout statistics
* Automated tests for core logic

---

# 47. Do NOT Include in Version 1

Do not initially implement:

* Account registration
* Social networking
* Friends
* Leaderboards
* Payments
* Subscriptions
* AI workout recommendations
* Nutrition tracking
* Cloud synchronization
* Wear OS
* Apple Health
* Google Health Connect integration
* Personal trainer marketplace
* Complex analytics dashboards

However, avoid architectural decisions that would make these features difficult to add later.

---

# 48. Future Architecture Considerations

Design the application so future releases could support:

Cloud account
↓
Cloud database
↓
Synchronization layer
↓
Local Room database
↓
Repository
↓
Application

The local Room database should remain the application's offline source of truth.

Cloud synchronization can be added later.

---

# 49. Code Quality Requirements

Write production-quality Kotlin.

Follow:

* SOLID principles where useful
* clear naming
* small focused classes
* immutable UI state
* separation of concerns
* repository pattern
* dependency injection
* testable business logic

Avoid:

* giant ViewModels
* giant Compose functions
* unnecessary abstraction
* premature microservices-style architecture
* global mutable state
* direct SQL/database calls from UI
* duplicated business logic

Use comments only when they clarify non-obvious decisions.

Prefer readable code over excessive comments.

---

# 50. Database Migration Strategy

Configure Room migrations properly.

Do not use destructive migration as the normal production strategy because workout history must never accidentally disappear after an application update.

Create database migrations as the schema evolves.

Workout history is considered important user data.

---

# 51. Performance

The application must remain responsive even after the user accumulates years of workout history.

Use:

* indexed database columns
* efficient queries
* asynchronous database operations
* Flow where appropriate
* lazy Compose lists
* appropriate query limits

Do not load the user's complete lifetime workout history into memory unnecessarily.

---

# 52. Accessibility

Use accessible:

* labels
* touch targets
* content descriptions where appropriate
* text sizes
* contrast

Do not communicate set completion using colour alone.

---

# 53. Important Domain Rules

Enforce these rules throughout the implementation:

1. Routines are templates.
2. Workout sessions are historical records.
3. Editing a routine must never edit previous workouts.
4. Deleting a routine must never delete previous workouts.
5. Workout data should save continuously.
6. An active workout must survive application restart.
7. Timer duration must be derived from timestamps.
8. Custom exercises are always allowed.
9. Users may modify sets during an active workout.
10. Users may modify exercises during an active workout.
11. Historical workouts must preserve the data as it existed when the workout occurred.

---

# 54. Build Process

Do not attempt to generate the entire application as one enormous implementation without structure.

Follow this development sequence.

## Phase 1 — Architecture

First produce:

* Application architecture
* Main screens
* Navigation structure
* Domain model
* Database schema
* Entity relationships
* Repository interfaces
* Major use cases
* Package structure

Explain important architectural decisions.

Do not start implementing screens before the architecture is coherent.

## Phase 2 — Project Foundation

Create:

* Android project
* Gradle configuration
* Compose
* Room
* Hilt
* Navigation
* Database
* Entities
* DAOs
* Repositories

Ensure the application builds.

## Phase 3 — Exercise Management

Implement:

* Exercise library
* Custom exercises
* Search
* Exercise editing

## Phase 4 — Routine Management

Implement:

* Create routine
* Edit routine
* Add exercises
* Remove exercises
* Reorder exercises
* Configure sets
* Duplicate routine

## Phase 5 — Workout Engine

Implement:

* Start workout
* Workout session creation
* Timer
* Set editing
* Set completion
* Exercise navigation
* Add exercise
* Remove exercise
* Autosave
* Resume workout

## Phase 6 — Workout Completion

Implement:

* Finish workout
* Summary
* Duration
* Volume
* Persistence

## Phase 7 — History

Implement:

* Workout history
* Workout details
* Exercise history
* Previous workout lookup

## Phase 8 — UX Refinement

Improve:

* Active workout interaction
* dark mode
* keyboard behaviour
* touch targets
* empty states
* loading states
* error states

## Phase 9 — Testing

Add:

* unit tests
* repository tests
* Room tests
* ViewModel tests
* core workflow tests

---

# 55. Before Implementing

Before writing significant code, provide me with:

1. Proposed Android architecture.
2. Screen/navigation diagram.
3. Database/entity model.
4. Relationship diagram.
5. Important domain models.
6. Repository interfaces.
7. Main use cases.
8. Package/folder structure.
9. State management approach.
10. Explanation of how workout history is protected when routines change.
11. Explanation of how an unfinished workout survives an app restart.
12. Suggested implementation phases.

Then begin implementation.

Do not ask me to make routine technical decisions unless a decision materially changes the application's product behaviour.

Choose sensible Android best practices yourself and document the choice.

---

# 56. Definition of Done

Version 1 is considered complete when I can:

1. Install the app.
2. Create "Chest Workout".
3. Add Bench Press, Incline Dumbbell Press, and Chest Fly.
4. Configure three sets for each exercise.
5. Start the workout.
6. See the workout timer running.
7. Enter reps and weight for every set.
8. Mark sets complete.
9. Close the application halfway through the workout.
10. Reopen it and resume the same workout.
11. Finish the workout.
12. See a workout summary.
13. Find the workout in history.
14. Open the workout and inspect every set.
15. Perform Chest Workout again.
16. See the previous weight/reps beside each exercise.
17. Modify the Chest Workout routine.
18. Confirm the previous historical workout remains unchanged.
19. Delete the routine.
20. Confirm previous workout history still exists.

Build the system around this definition of done.
