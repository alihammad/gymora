package com.gymora.data.local.seed

import android.content.Context
import androidx.room.withTransaction
import com.gymora.data.local.ExerciseMetadataCodec
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.ExerciseEntity
import com.gymora.domain.model.DifficultyLevel
import com.gymora.domain.model.EquipmentItem
import com.gymora.domain.model.EquipmentType
import com.gymora.domain.model.EquipmentUsageType
import com.gymora.domain.model.ExerciseCategory
import com.gymora.domain.model.ExerciseType
import com.gymora.domain.model.ForceType
import com.gymora.domain.model.Mechanics
import com.gymora.domain.model.MuscleGroup
import com.gymora.domain.model.MuscleGroupRef
import com.gymora.domain.model.MuscleGroupType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hook for first-launch exercise library seeding (R-08, FR-005, BR-20).
 * Invoked transactionally from the Room onCreate callback (see DatabaseModule).
 */
interface LibrarySeeder {
    suspend fun seed(database: GymoraDatabase)
}

/**
 * Real seeder (T017): inserts the built-in exercise library from the bundled
 * `exercises.json` asset (docs/exercises.json) idempotently. Each exercise keeps
 * its full descriptive metadata — difficulty, force, mechanics, category,
 * step-by-step instructions, primary/secondary muscle groups, and equipment.
 * Routines are never seeded (FR-010, BR-20).
 */
@Singleton
class LibrarySeederImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LibrarySeeder {

    override suspend fun seed(database: GymoraDatabase) {
        val dao = database.exerciseDao()
        // Idempotency guard: the onCreate callback fires once per DB creation,
        // but seeding must stay safe if invoked again (R-08, T012).
        if (dao.count() > 0) return

        val exercises = loadExercises()
        val now = System.currentTimeMillis()
        val entities = exercises.map { it.toEntity(now) }

        database.withTransaction {
            entities.forEach { dao.insert(it) }
        }
    }

    private fun loadExercises(): List<ParsedExercise> {
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        return (0 until array.length()).map { index ->
            ParsedExercise.fromJson(array.getJSONObject(index))
        }
    }

    private fun ParsedExercise.toEntity(now: Long): ExerciseEntity = ExerciseEntity(
        name = name,
        muscleGroup = muscleGroups
            .firstOrNull { it.type == MuscleGroupType.PRIMARY }
            ?.group
            ?.name,
        description = null,
        notes = null,
        isCustom = false,
        deletedAt = null,
        createdAt = now,
        updatedAt = now,
        type = type.name,
        difficultyLevel = difficultyLevel.name,
        forceType = forceType.name,
        mechanics = mechanics.name,
        category = category.name,
        instructionsJson = ExerciseMetadataCodec.encodeInstructions(instructions),
        muscleGroupsJson = ExerciseMetadataCodec.encodeMuscleGroups(muscleGroups),
        equipmentJson = ExerciseMetadataCodec.encodeEquipment(equipment),
    )

    private companion object {
        const val ASSET_NAME = "exercises.json"
    }
}

/**
 * A single compiled exercise row of the seed source before persistence.
 * Muscle-group slugs and category strings are mapped to their typed enums here
 * so malformed values surface as a clear failure at seed time.
 */
private data class ParsedExercise(
    val name: String,
    val type: ExerciseType,
    val difficultyLevel: DifficultyLevel,
    val forceType: ForceType,
    val mechanics: Mechanics,
    val category: ExerciseCategory,
    val instructions: List<String>,
    val muscleGroups: List<MuscleGroupRef>,
    val equipment: List<EquipmentItem>,
) {
    companion object {
        fun fromJson(o: JSONObject): ParsedExercise {
            val instructions = o.getJSONArray("instructions").let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
            val groups = o.getJSONArray("muscleGroups").let { arr ->
                (0 until arr.length()).map { i ->
                    val g = arr.getJSONObject(i)
                    MuscleGroupRef(
                        group = MuscleGroup.fromSlug(g.getString("slug")),
                        type = MuscleGroupType.valueOf(g.getString("type").uppercase()),
                    )
                }
            }
            val equipment = o.getJSONArray("equipment").let { arr ->
                (0 until arr.length()).map { i ->
                    val e = arr.getJSONObject(i)
                    EquipmentItem(
                        name = e.getString("name"),
                        type = EquipmentType.valueOf(e.getString("type").uppercase()),
                        usageType = EquipmentUsageType.valueOf(e.getString("usageType").uppercase()),
                    )
                }
            }
            return ParsedExercise(
                name = o.getString("name"),
                type = ExerciseType.valueOf(o.getString("type").uppercase()),
                difficultyLevel = DifficultyLevel.valueOf(o.getString("difficultyLevel").uppercase()),
                forceType = ForceType.valueOf(o.getString("forceType").uppercase()),
                mechanics = Mechanics.valueOf(o.getString("mechanics").uppercase()),
                category = ExerciseCategory.fromSlug(o.getString("category")),
                instructions = instructions,
                muscleGroups = groups,
                equipment = equipment,
            )
        }
    }
}
