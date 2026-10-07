package com.gymora.data.transfer

import androidx.room.withTransaction
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.ExerciseEntity
import com.gymora.data.local.entity.WorkoutExerciseEntity
import com.gymora.data.local.entity.WorkoutSessionEntity
import com.gymora.data.local.entity.WorkoutSetEntity
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.calculator.SetFormat
import com.gymora.domain.model.SessionStatus
import com.gymora.domain.model.Side
import com.gymora.domain.model.WeightUnit
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of a CSV import. [errors] holds the first few row-level problems. */
data class CsvImportResult(
    val workoutsImported: Int,
    val duplicatesSkipped: Int,
    val rowsSkipped: Int,
    val exercisesCreated: Int,
    val errors: List<String>,
)

class CsvImportException(message: String) : Exception(message)

/**
 * Exports completed workouts to CSV and imports them back.
 *
 * One row per completed set. Columns: [HEADER]. A workout with no sets is written as a single
 * row with blank exercise fields so it still round-trips. Imported rows are grouped into a
 * workout by (Start, Workout); workouts already present (same name, same start second) are
 * skipped, so importing the same file twice is harmless.
 */
@Singleton
class WorkoutCsvService @Inject constructor(private val database: GymoraDatabase) {

    suspend fun export(out: OutputStream): Int {
        val sessions = database.workoutSessionDao().listCompleted(Int.MAX_VALUE, 0).reversed()
        out.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(BOM + CsvCodec.formatRow(HEADER) + EOL)
            sessions.forEach { session ->
                rowsFor(session).forEach { writer.write(CsvCodec.formatRow(it) + EOL) }
            }
        }
        return sessions.size
    }

    private suspend fun rowsFor(session: WorkoutSessionEntity): List<List<String>> {
        val head = listOf(
            formatInstant(session.startedAt),
            session.endedAt?.let(::formatInstant).orEmpty(),
            session.routineNameSnapshot,
            session.notes.orEmpty(),
        )
        val rows = mutableListOf<List<String>>()
        database.workoutExerciseDao().getForSession(session.id).forEach { exercise ->
            database.workoutSetDao().getForExercise(exercise.id)
                .filter { it.isCompleted }
                .forEach { set ->
                    rows += head + listOf(
                        exercise.exerciseNameSnapshot,
                        set.setNumber.toString(),
                        set.weight?.let(::formatNumber).orEmpty(),
                        if (set.weight != null) set.weightUnit.orEmpty() else "",
                        set.reps?.toString().orEmpty(),
                        set.notes.orEmpty(),
                        set.durationSeconds?.toString().orEmpty(),
                        set.distanceMeters?.let(::formatNumber).orEmpty(),
                        set.side.orEmpty(),
                    )
                }
        }
        if (rows.isEmpty()) rows += head + List(HEADER.size - head.size) { "" }
        return rows
    }

    suspend fun import(input: InputStream): CsvImportResult {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val rows = CsvCodec.parse(text)
        if (rows.isEmpty()) throw CsvImportException("The file is empty.")

        val columns = rows.first().mapIndexed { i, name -> name.trim().lowercase() to i }.toMap()
        val missing = REQUIRED.filter { it.lowercase() !in columns }
        if (missing.isNotEmpty()) {
            throw CsvImportException("Missing required column(s): ${missing.joinToString()}.")
        }

        val errors = mutableListOf<String>()
        var rowsSkipped = 0
        val workouts = linkedMapOf<Pair<Long, String>, MutableList<ParsedRow>>()
        rows.drop(1).forEachIndexed { index, fields ->
            val parsed = parseRow(fields, columns)
            if (parsed.row == null) {
                rowsSkipped++
                if (errors.size < MAX_ERRORS) errors += "Row ${index + 2}: ${parsed.error}"
            } else {
                workouts.getOrPut(parsed.row.startedAt to parsed.row.workout) { mutableListOf() }
                    .add(parsed.row)
            }
        }

        val counts = database.withTransaction { insertWorkouts(workouts) }
        return CsvImportResult(counts.imported, counts.duplicates, rowsSkipped, counts.created, errors)
    }

    private class Counts(var imported: Int = 0, var duplicates: Int = 0, var created: Int = 0) {
        val createdExerciseIds = mutableSetOf<Long>()
    }

    private suspend fun insertWorkouts(
        workouts: Map<Pair<Long, String>, List<ParsedRow>>,
    ): Counts {
        val counts = Counts()
        val routines = database.routineDao().getAllOnce()
        val exerciseCache = mutableMapOf<String, ExerciseEntity>()
        workouts.forEach { (key, rows) ->
            val (startedAt, name) = key
            if (database.workoutSessionDao().countCompletedAt(startedAt / 1000, name) > 0) {
                counts.duplicates++
                return@forEach
            }
            val sessionId = database.workoutSessionDao().insert(
                WorkoutSessionEntity(
                    routineId = routines.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id,
                    routineNameSnapshot = name,
                    startedAt = startedAt,
                    endedAt = rows.firstNotNullOfOrNull { it.endedAt } ?: startedAt,
                    status = SessionStatus.COMPLETED.name,
                    notes = rows.firstNotNullOfOrNull { it.workoutNotes },
                    createdAt = System.currentTimeMillis(),
                ),
            )
            insertExercises(sessionId, startedAt, rows, exerciseCache, counts)
            counts.imported++
        }
        return counts
    }

    private suspend fun insertExercises(
        sessionId: Long,
        startedAt: Long,
        rows: List<ParsedRow>,
        cache: MutableMap<String, ExerciseEntity>,
        counts: Counts,
    ) {
        val byExercise = rows.filter { it.exercise.isNotEmpty() }.groupBy { it.exercise.lowercase() }
        byExercise.values.forEachIndexed { position, sets ->
            val exercise = resolveExercise(sets.first().exercise, cache, counts)
            val workoutExerciseId = database.workoutExerciseDao().insert(
                WorkoutExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    exerciseNameSnapshot = exercise.name,
                    position = position,
                    notes = null,
                ),
            )
            sets.forEachIndexed { index, set ->
                database.workoutSetDao().insert(
                    WorkoutSetEntity(
                        workoutExerciseId = workoutExerciseId,
                        setNumber = index + 1,
                        reps = set.reps,
                        weight = set.weight,
                        weightUnit = set.weight?.let { set.unit.name },
                        // A library exercise keeps its tracking; a new one is inferred from the values.
                        measurementType = if (exercise.id in counts.createdExerciseIds) {
                            inferType(set).name
                        } else {
                            exercise.measurementType
                        },
                        isCompleted = true,
                        completedAt = set.endedAt ?: startedAt,
                        notes = set.setNotes,
                        durationSeconds = set.durationSeconds,
                        distanceMeters = set.distanceMeters,
                        side = set.side?.name,
                    ),
                )
            }
        }
    }

    private fun inferType(set: ParsedRow): MeasurementType = when {
        set.distanceMeters != null && set.weight != null -> MeasurementType.WEIGHT_AND_DISTANCE
        set.distanceMeters != null -> MeasurementType.DISTANCE_AND_DURATION
        set.durationSeconds != null && set.reps == null -> MeasurementType.DURATION
        set.weight != null -> MeasurementType.WEIGHT_AND_REPS
        else -> MeasurementType.REPS_ONLY
    }

    private suspend fun resolveExercise(
        name: String,
        cache: MutableMap<String, ExerciseEntity>,
        counts: Counts,
    ): ExerciseEntity = cache.getOrPut(name.lowercase()) {
        database.exerciseDao().getByName(name) ?: run {
            val now = System.currentTimeMillis()
            val draft = ExerciseEntity(
                name = name,
                muscleGroup = null,
                description = null,
                notes = null,
                isCustom = true,
                deletedAt = null,
                createdAt = now,
                updatedAt = now,
            )
            counts.created++
            draft.copy(id = database.exerciseDao().insert(draft)).also { counts.createdExerciseIds += it.id }
        }
    }

    @Suppress("LongParameterList") // One field per CSV column.
    private class ParsedRow(
        val startedAt: Long,
        val endedAt: Long?,
        val workout: String,
        val workoutNotes: String?,
        val exercise: String,
        val weight: Double?,
        val unit: WeightUnit,
        val reps: Int?,
        val setNotes: String?,
        val durationSeconds: Int?,
        val distanceMeters: Double?,
        val side: Side?,
    )

    private class ParseOutcome(val row: ParsedRow? = null, val error: String = "")

    private fun parseRow(fields: List<String>, columns: Map<String, Int>): ParseOutcome {
        fun cell(name: String) = columns[name.lowercase()]?.let { fields.getOrNull(it) }?.trim().orEmpty()

        val startedAt = parseInstant(cell("Start"))
            ?: return ParseOutcome(error = "unrecognised start date '${cell("Start")}'")
        val weightText = cell("Weight")
        val weight = weightText.toDoubleOrNull()
        val repsText = cell("Reps")
        val reps = repsText.toIntOrNull()
        val secondsText = cell("Seconds")
        val seconds = SetFormat.parseDuration(secondsText)
        val distanceText = cell("Distance m")
        val distance = distanceText.toDoubleOrNull()
        val error = listOf(
            Triple("weight", weightText, weight),
            Triple("reps", repsText, reps),
            Triple("time", secondsText, seconds),
            Triple("distance", distanceText, distance),
        ).firstOrNull { (_, text, value) -> text.isNotEmpty() && value == null }
            ?.let { (field, text, _) -> "invalid $field '$text'" }
        if (error != null) return ParseOutcome(error = error)

        return ParseOutcome(
            ParsedRow(
                startedAt = startedAt,
                endedAt = parseInstant(cell("End")),
                workout = cell("Workout").ifEmpty { DEFAULT_WORKOUT_NAME },
                workoutNotes = cell("Workout Notes").ifEmpty { null },
                exercise = cell("Exercise"),
                weight = weight,
                unit = WeightUnit.entries.firstOrNull { it.name.equals(cell("Unit"), true) }
                    ?: WeightUnit.KG,
                reps = reps,
                setNotes = cell("Set Notes").ifEmpty { null },
                durationSeconds = seconds,
                distanceMeters = distance,
                side = Side.entries.firstOrNull { it.name.equals(cell("Side"), ignoreCase = true) },
            ),
        )
    }

    private fun formatInstant(millis: Long): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).withNano(0),
        )

    private fun formatNumber(value: Double): String =
        if (value == Math.floor(value)) value.toLong().toString() else value.toString()

    /** Accepts ISO offset/local date-times, `yyyy-MM-dd HH:mm[:ss]`, or a bare date. */
    private fun parseInstant(text: String): Long? {
        if (text.isEmpty()) return null
        val zone = ZoneId.systemDefault()
        return try {
            when {
                text.length <= DATE_ONLY_LENGTH ->
                    LocalDate.parse(text).atStartOfDay(zone).toInstant().toEpochMilli()
                text.endsWith("Z") || text.lastIndexOf('+') > DATE_ONLY_LENGTH ||
                    text.lastIndexOf('-') > DATE_ONLY_LENGTH ->
                    OffsetDateTime.parse(text).toInstant().toEpochMilli()
                else -> LocalDateTime.parse(text.replace(' ', 'T')).atZone(zone).toInstant().toEpochMilli()
            }
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private companion object {
        const val BOM = "﻿"
        const val EOL = "\r\n"
        const val MAX_ERRORS = 5
        const val DATE_ONLY_LENGTH = 10
        const val DEFAULT_WORKOUT_NAME = "Imported workout"
        val HEADER = listOf(
            "Start", "End", "Workout", "Workout Notes", "Exercise", "Set", "Weight", "Unit", "Reps",
            "Set Notes", "Seconds", "Distance m", "Side",
        )
        val REQUIRED = listOf("Start", "Exercise")
    }
}
