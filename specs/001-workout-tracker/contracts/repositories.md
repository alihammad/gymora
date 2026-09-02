# Contracts: Domain Repository Interfaces

**Branch**: `001-workout-tracker` | **Date**: 2026-09-02

These are the stable contracts between the UI layer and the data layer (Constitution IV:
explicit inputs, outputs, and error behavior). They live in `domain/repository` as pure
Kotlin interfaces; implementations live in `data/repository`. All functions are `suspend`
unless noted; all write operations are **write-through** — they complete durably before
returning (R-05, FR-027).

## Error model

Domain failures surface as typed exceptions caught at the ViewModel edge and mapped to
user-friendly messages (R-15). No stack traces reach the UI (FR-060).

| Exception | Meaning |
|-----------|---------|
| `ValidationException(field, message)` | Input violated a domain rule (e.g., negative weight — BR-16) |
| `ActiveWorkoutConflictException(activeSessionId)` | Start requested while a session is ACTIVE (FR-020) |
| `EntityNotFoundException(id)` | Referenced routine/exercise/session does not exist or is soft-deleted where that matters |

## ExerciseRepository (FR-005..FR-010)

```kotlin
interface ExerciseRepository {
    /** All non-deleted exercises for library browsing, grouped access via muscleGroup. */
    fun observeLibrary(): Flow<List<Exercise>>

    /** Search by name substring (case-insensitive); includes custom, excludes soft-deleted (FR-008). */
    suspend fun search(query: String): List<Exercise>

    suspend fun getById(id: Long): Exercise  // throws EntityNotFoundException

    /** Creates a custom exercise. Throws ValidationException on blank name. */
    suspend fun createCustom(input: CreateExerciseInput): Exercise

    /** Updates name/muscleGroup/description/notes. Throws ValidationException / EntityNotFoundException.
     *  Does NOT touch history (FR-009); history displays snapshots (FR-043). */
    suspend fun update(id: Long, input: UpdateExerciseInput): Exercise

    /** Soft delete (R-03). With confirmation handled by UI, this:
     *  1) marks deleted_at, 2) removes the exercise from routine templates.
     *  History is never touched (FR-009, BR-04). */
    suspend fun delete(id: Long)
}
```

## RoutineRepository (FR-011..FR-018)

```kotlin
interface RoutineRepository {
    /** Routines ordered by position (FR-015). */
    fun observeAll(): Flow<List<RoutineSummary>>  // summary: id, name, exerciseCount, lastPerformedAt

    suspend fun getById(id: Long): RoutineDetail  // header + ordered exercises + set templates
                                                    // throws EntityNotFoundException

    suspend fun create(name: String, description: String?): Long  // ValidationException on blank name

    suspend fun rename(id: Long, name: String)   // history keeps snapshot (FR-043)
    suspend fun updateDescription(id: Long, description: String?)

    /** Deletes routine + template rows only. History untouched (FR-013, BR-03). */
    suspend fun delete(id: Long)

    /** Deep copy incl. exercises and set templates; name "<name> Copy" (FR-014, R-10). */
    suspend fun duplicate(id: Long): Long

    /** Persist a new home-screen order transactionally (FR-015). */
    suspend fun reorder(orderedRoutineIds: List<Long>)

    // Exercise membership (FR-016, FR-017)
    suspend fun addExercise(routineId: Long, exerciseId: Long, notes: String?)
    suspend fun removeExercise(routineId: Long, routineExerciseId: Long)
    suspend fun reorderExercises(routineId: Long, orderedRoutineExerciseIds: List<Long>)
    suspend fun updateExerciseNotes(routineExerciseId: Long, notes: String?)

    // Planned sets (FR-018)
    suspend fun addSetTemplate(routineExerciseId: Long, template: SetTemplateInput)
    suspend fun updateSetTemplate(templateId: Long, template: SetTemplateInput)
    suspend fun deleteSetTemplate(templateId: Long)
}
```

`SetTemplateInput`: `targetReps: Int`, `targetWeight: Double?`, `weightUnit: WeightUnit?`,
`measurementType: MeasurementType`. Validated per BR-16.

## WorkoutSessionRepository (FR-019..FR-039)

```kotlin
interface WorkoutSessionRepository {
    /** The single ACTIVE session, if any (BR-14). */
    fun observeActiveSession(): Flow<ActiveWorkout?>

    /** Creates session (status ACTIVE, started_at = now) by copying routine exercises +
     *  planned sets into historical rows (FR-019).
     *  Throws ActiveWorkoutConflictException if one is already active (FR-020). */
    suspend fun startFromRoutine(routineId: Long): Long

    /** Full active-workout graph for rendering/recovery (FR-039). */
    suspend fun getActiveWorkout(sessionId: Long): ActiveWorkout

    /** Most recent completed performance per exercise for pre-fill (FR-045/046). */
    suspend fun getPreviousPerformance(exerciseId: Long): PreviousPerformance?

    /** Set logging — every call persists immediately (FR-025, FR-027).
     *  Throws ValidationException on negative weight/reps (BR-16). */
    suspend fun updateSetValues(setId: Long, weight: Double?, weightUnit: WeightUnit?, reps: Int?)
    suspend fun completeSet(setId: Long)      // sets is_completed + completed_at
    suspend fun uncompleteSet(setId: Long)
    suspend fun addSet(workoutExerciseId: Long)          // FR-026
    suspend fun deleteSet(setId: Long)

    /** FR-028: add exercise to session; addToRoutine=true also appends it to the source routine. */
    suspend fun addExerciseToSession(sessionId: Long, exerciseId: Long, addToRoutine: Boolean)

    /** FR-029: remove from this session only; routine untouched. */
    suspend fun removeExerciseFromSession(workoutExerciseId: Long)

    suspend fun updateSessionNotes(sessionId: Long, notes: String?)

    /** Records ended_at, sets status COMPLETED, returns summary (FR-033/034/035). */
    suspend fun finish(sessionId: Long): WorkoutSummary

    /** Permanent deletion of the session graph (BR-15). UI requires confirmation (FR-037). */
    suspend fun discard(sessionId: Long)
}
```

## HistoryRepository (FR-040..FR-044)

```kotlin
interface HistoryRepository {
    /** Completed sessions, newest first, paged (FR-040, FR-058, R-07). */
    suspend fun listCompleted(limit: Int, offset: Int): List<HistoryEntry>

    /** Full read-only workout graph (FR-041). */
    suspend fun getWorkoutDetail(sessionId: Long): WorkoutDetail

    /** All performances of one exercise, newest first, paged (FR-044). */
    suspend fun getExerciseHistory(exerciseId: Long, limit: Int, offset: Int): List<ExercisePerformance>

    /** FR-042 historical correction — scoped strictly to the given session. */
    suspend fun correctSet(setId: Long, weight: Double?, weightUnit: WeightUnit?, reps: Int?, isCompleted: Boolean, notes: String?)
    suspend fun addExerciseToHistoricalWorkout(sessionId: Long, exerciseId: Long)
    suspend fun removeExerciseFromHistoricalWorkout(workoutExerciseId: Long)
    suspend fun updateHistoricalWorkoutNotes(sessionId: Long, notes: String?)
}
```

## RecordsRepository (FR-047, R-09)

```kotlin
interface RecordsRepository {
    /** Computed on demand from completed sessions; each record carries exercise + date context. */
    suspend fun getPersonalRecords(): PersonalRecords
    // PersonalRecords: heaviestWeight, highestReps, bestEstimatedOneRepMax (Epley),
    //                  largestWorkoutVolume — each with { value, exerciseName, date }
}
```

## SettingsRepository (FR-048..FR-051)

```kotlin
interface SettingsRepository {
    fun observeSettings(): Flow<Settings>   // weightUnit, defaultRestSeconds, theme
    suspend fun setWeightUnit(unit: WeightUnit)      // display-only; never rewrites stored weights (R-04)
    suspend fun setDefaultRestDuration(seconds: Int)
    suspend fun setTheme(theme: Theme)
}
```

## Cross-cutting contract rules

1. **Write-through**: no repository write returns until the row(s) are durable (R-05).
2. **History protection**: no routine/exercise mutation API may write historical tables
   (BR-02/03/04) — enforced by construction (separate DAOs) and regression tests.
3. **Unit handling**: repositories return weights with their stored `weight_unit`;
   conversion to the display unit happens in the domain layer only (R-04).
4. **Validation**: all numeric input is validated against BR-16 at the repository boundary
   before any write.
