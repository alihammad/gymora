package com.gymora.domain.model

/**
 * Routine template models (FR-011..FR-018). Pure-Kotlin mirrors of the Room
 * entities without persistence annotations (plan.md Structure Decision).
 */
data class RoutineSummary(
    val id: Long,
    val name: String,
    val exerciseCount: Int,
    val lastPerformedAt: Long?,
)

data class RoutineHeader(
    val id: Long,
    val name: String,
    val description: String?,
    val position: Int,
)

data class SetTemplate(
    val id: Long,
    val setNumber: Int,
    val targetReps: Int,
    val targetWeight: Double?,
    val weightUnit: WeightUnit?,
    val measurementType: MeasurementType,
    val targetDurationSeconds: Int? = null,
    val targetDistanceMeters: Double? = null,
)

data class SetTemplateInput(
    val targetReps: Int,
    val targetWeight: Double?,
    val weightUnit: WeightUnit?,
    val measurementType: MeasurementType,
    val targetDurationSeconds: Int? = null,
    val targetDistanceMeters: Double? = null,
)

data class RoutineExerciseDetail(
    val routineExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val position: Int,
    val notes: String?,
    val setTemplates: List<SetTemplate>,
    /** Adjacent exercises sharing a non-null group form a superset. */
    val supersetGroup: Long? = null,
    /** The library exercise's current tracking type; drives which targets are shown. */
    val measurementType: MeasurementType = MeasurementType.WEIGHT_AND_REPS,
    val isUnilateral: Boolean = false,
)

data class RoutineDetail(
    val header: RoutineHeader,
    val exercises: List<RoutineExerciseDetail>,
)

/**
 * Routine business rules (FR-011, FR-014, R-10). Single implementation of the
 * duplicate-naming rule (Constitution IX).
 */
object RoutineRules {

    fun validateName(name: String) {
        if (name.isBlank()) {
            throw ValidationException("name", "Workout name must not be blank")
        }
    }

    /** Derived duplicate name, e.g. "Chest Workout" → "Chest Workout Copy" (R-10). */
    fun duplicateName(name: String): String = "$name Copy"
}

/** One exercise's id and superset group, in workout order. */
data class SupersetEntry(val id: Long, val group: Long?)

/** A new exercise order with each exercise's canonical superset group. */
data class SupersetLayout(val order: List<Long>, val groups: Map<Long, Long?>)

/**
 * Superset grouping rules. A superset is a run of two or more adjacent
 * exercises sharing the same non-null group; in canonical form the group is
 * the first member's id and single exercises have no group.
 */
object SupersetRules {

    /** Splits [items] into display blocks: each superset, or a single exercise. */
    fun <T> blocks(items: List<T>, groupOf: (T) -> Long?): List<List<T>> {
        val result = mutableListOf<MutableList<T>>()
        var previousGroup: Long? = null
        items.forEach { item ->
            val group = groupOf(item)
            if (group != null && group == previousGroup) {
                result.last().add(item)
            } else {
                result.add(mutableListOf(item))
            }
            previousGroup = group
        }
        return result
    }

    /** Canonical group per id: runs keep their first member's id, singles lose theirs. */
    fun normalize(entries: List<SupersetEntry>): Map<Long, Long?> =
        blocks(entries) { it.group }.flatMap { block ->
            val group = if (block.size > 1) block.first().id else null
            block.map { it.id to group }
        }.toMap()

    /** Joins the exercise at [index] (and its superset) with the next one (and its superset). */
    fun linkWithNext(entries: List<SupersetEntry>, index: Int): Map<Long, Long?> {
        require(index in 0 until entries.lastIndex) { "No next exercise to link with" }
        val joined = entries[index].group ?: entries[index].id
        val nextGroup = entries[index + 1].group
        val relabelled = entries.mapIndexed { i, entry ->
            val inNextRun = i > index && nextGroup != null && entry.group == nextGroup &&
                entries.subList(index + 1, i + 1).all { it.group == nextGroup }
            when {
                i == index || i == index + 1 || inNextRun -> entry.copy(group = joined)
                else -> entry
            }
        }
        return normalize(relabelled)
    }

    /** Display number of each block: superset blocks count 1, 2, 3… in order; single exercises get null. */
    fun <T> numbers(blocks: List<List<T>>): List<Int?> {
        var count = 0
        return blocks.map { if (it.size > 1) ++count else null }
    }

    /**
     * Supersets [memberId] with [anchorId]: the member leaves any superset it was
     * in and moves to just after the anchor (and the anchor's superset).
     */
    fun join(entries: List<SupersetEntry>, anchorId: Long, memberId: Long): SupersetLayout {
        require(anchorId != memberId) { "An exercise cannot be supersetted with itself" }
        val anchor = entries.first { it.id == anchorId }
        val group = anchor.group ?: anchor.id
        val rest = entries.filter { it.id != memberId }
            .map { if (it.id == anchorId) it.copy(group = group) else it }
            .toMutableList()
        rest.add(rest.indexOfLast { it.group == group } + 1, SupersetEntry(memberId, group))
        return SupersetLayout(rest.map { it.id }, normalize(rest))
    }

    /** Takes [id] out of its superset and places it just after the remaining members. */
    fun leave(entries: List<SupersetEntry>, id: Long): SupersetLayout {
        val group = entries.first { it.id == id }.group
            ?: return SupersetLayout(entries.map { it.id }, normalize(entries))
        val rest = entries.filter { it.id != id }.toMutableList()
        rest.add(rest.indexOfLast { it.group == group } + 1, SupersetEntry(id, null))
        return SupersetLayout(rest.map { it.id }, normalize(rest))
    }

    /** Splits the superset between the exercise at [index] and the next one. */
    fun unlinkFromNext(entries: List<SupersetEntry>, index: Int): Map<Long, Long?> {
        require(index in 0 until entries.lastIndex) { "No next exercise to unlink from" }
        val group = entries[index].group
        if (group == null || entries[index + 1].group != group) return normalize(entries)
        val tailGroup = entries[index + 1].id
        val relabelled = entries.mapIndexed { i, entry ->
            val inTail = i > index && entries.subList(index + 1, i + 1).all { it.group == group }
            if (inTail) entry.copy(group = tailGroup) else entry
        }
        return normalize(relabelled)
    }
}
