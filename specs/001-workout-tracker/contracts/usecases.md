# Contracts: Domain Use Cases & Calculators

**Branch**: `001-workout-tracker` | **Date**: 2026-09-02

Use cases live in `domain/usecase` and calculators in `domain/calculator`. They are pure
Kotlin, unit-testable on the JVM, and are the single implementation of each business rule
(Constitution IX). They depend only on the repository interfaces in
`contracts/repositories.md`.

## Use cases

| Use case | Responsibility | Key rules enforced |
|----------|----------------|--------------------|
| `StartWorkoutUseCase` | Start a session from a routine; enforce single-active invariant | FR-019, FR-020, BR-14 → throws `ActiveWorkoutConflictException` |
| `LogSetUseCase` | Validate + persist set value edits and completion | FR-023..FR-025, BR-16, BR-05 (write-through) |
| `ModifySessionStructureUseCase` | Add/delete sets; add/remove exercises in a session; "add to routine" choice | FR-026, FR-028, FR-029, BR-09/10 |
| `FinishWorkoutUseCase` | Record end timestamp, mark completed, build summary | FR-033..FR-036, BR-13 |
| `DiscardWorkoutUseCase` | Permanently delete an active session | FR-037, BR-15 |
| `ResumeWorkoutUseCase` | Detect + restore active session on launch | FR-038, FR-039, BR-06 |
| `PreviousPerformanceUseCase` | Fetch last performance + build pre-fill values | FR-045, FR-046 |
| `DeleteExerciseUseCase` | Soft-delete + remove from routine templates | FR-009, BR-04, R-03 |
| `DeleteRoutineUseCase` | Delete routine without touching history | FR-012, FR-013, BR-03 |
| `CorrectHistoricalWorkoutUseCase` | Scoped edits to one completed session | FR-042, BR-11 |
| `DuplicateRoutineUseCase` | Deep-copy routine + templates | FR-014, R-10 |
| `SeedLibraryUseCase` | Idempotent first-launch seeding | FR-005, FR-010, BR-20, R-08 |

## Calculators (pure functions — unit tested)

```kotlin
object WorkoutCalculators {
    /** BR-12: total volume = Σ (weight × reps) over completed weighted sets (weight > 0).
     *  Weights are normalized to the display unit before summing (R-04). */
    fun totalVolume(sets: List<CompletedSet>, displayUnit: WeightUnit): Double

    /** BR-13: duration = endedAt − startedAt (timestamps, never an in-memory counter). */
    fun duration(startedAt: Instant, endedAt: Instant): Duration
    fun elapsed(startedAt: Instant, now: Instant): Duration   // live timer display (FR-021)

    /** R-09: Epley estimated 1RM = weight × (1 + reps/30); reps ≥ 1, weight > 0. */
    fun estimatedOneRepMax(weight: Double, reps: Int): Double

    /** R-04: lossless unit conversion using 1 lb = 0.45359237 kg. */
    fun convertWeight(value: Double, from: WeightUnit, to: WeightUnit): Double
}
```

## Domain models (value shapes)

These pure-Kotlin models mirror the Room entities in `data-model.md` without persistence
annotations. Key shapes:

- `Exercise(id, name, muscleGroup?, description?, notes?, isCustom, isDeleted)`
- `RoutineSummary(id, name, exerciseCount, lastPerformedAt?)`
- `RoutineDetail(header, exercises: List<RoutineExerciseDetail>)`
- `ActiveWorkout(session, exercises: List<ActiveExercise>, startedAt)` where each
  `ActiveExercise` carries `previousPerformance: List<SetValue>?` for pre-fill (FR-045).
- `WorkoutSummary(routineNameSnapshot, date, duration, exerciseCount, completedSetCount,
  totalReps, totalVolume, perExerciseBreakdown)` (FR-035)
- `HistoryEntry(id, routineNameSnapshot, startedAt, duration)` (FR-040)
- `PersonalRecords(heaviestWeight, highestReps, bestEstimatedOneRepMax, largestVolume)`
  each with exercise-name + date context (FR-047)
- `Settings(weightUnit, defaultRestSeconds, theme)`
- Enums: `WeightUnit { KG, LB }`, `Theme { SYSTEM, LIGHT, DARK }`,
  `MeasurementType { WEIGHT_AND_REPS, REPS_ONLY }`, `MuscleGroup { CHEST, BACK, SHOULDERS, ARMS, LEGS }`

## Screen → contract mapping (UI consumes only the above)

| Screen | Primary contracts |
|--------|-------------------|
| Home | `RoutineRepository.observeAll`, `HistoryRepository.listCompleted(limit=3)`, `StartWorkoutUseCase`, `ResumeWorkoutUseCase` |
| Routine editor | `RoutineRepository.*`, `ExerciseRepository.observeLibrary` |
| Exercise library | `ExerciseRepository.*`, `DeleteExerciseUseCase` |
| Active workout | `WorkoutSessionRepository.*`, `LogSetUseCase`, `ModifySessionStructureUseCase`, `PreviousPerformanceUseCase`, `FinishWorkoutUseCase`, `DiscardWorkoutUseCase` |
| History list/detail | `HistoryRepository.listCompleted`, `getWorkoutDetail`, `CorrectHistoricalWorkoutUseCase` |
| Exercise history | `HistoryRepository.getExerciseHistory` |
| Records | `RecordsRepository.getPersonalRecords` |
| Settings | `SettingsRepository.*` |
