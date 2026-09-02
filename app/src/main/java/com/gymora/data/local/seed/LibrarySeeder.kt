package com.gymora.data.local.seed

import androidx.room.withTransaction
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.ExerciseEntity

/**
 * Hook for first-launch exercise library seeding (R-08, FR-005, BR-20).
 * Invoked transactionally from the Room onCreate callback (see DatabaseModule).
 */
interface LibrarySeeder {
    suspend fun seed(database: GymoraDatabase)
}

/**
 * Real seeder (T017): inserts the built-in exercise library idempotently.
 * Routines are never seeded (FR-010, BR-20).
 */
class LibrarySeederImpl : LibrarySeeder {

    override suspend fun seed(database: GymoraDatabase) {
        val dao = database.exerciseDao()
        // Idempotency guard: the onCreate callback fires once per DB creation,
        // but seeding must stay safe if invoked again (R-08, T012).
        if (dao.count() > 0) return

        val now = System.currentTimeMillis()
        val entities = ExerciseSeedData.EXERCISES.map { seed ->
            ExerciseEntity(
                name = seed.name,
                muscleGroup = seed.muscleGroup.name,
                description = null,
                notes = null,
                isCustom = false,
                deletedAt = null,
                createdAt = now,
                updatedAt = now,
            )
        }
        database.withTransaction {
            entities.forEach { dao.insert(it) }
        }
    }
}
