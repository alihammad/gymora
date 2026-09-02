package com.gymora.data.local.seed

import com.gymora.data.local.db.GymoraDatabase

/**
 * Hook for first-launch exercise library seeding (R-08, FR-005, BR-20).
 * Invoked transactionally from the Room onCreate callback (see DatabaseModule).
 * The real implementation with seed data lands in T017 (US1).
 */
interface LibrarySeeder {
    suspend fun seed(database: GymoraDatabase)
}

/** Placeholder until T017 provides the real seeder with built-in exercises. */
object NoOpLibrarySeeder : LibrarySeeder {
    override suspend fun seed(database: GymoraDatabase) {
        // Replaced by the real seeder in T017 (US1).
    }
}
