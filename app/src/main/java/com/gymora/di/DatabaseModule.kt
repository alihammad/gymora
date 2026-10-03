package com.gymora.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymora.data.local.dao.ExerciseDao
import com.gymora.data.local.dao.RoutineDao
import com.gymora.data.local.dao.RoutineExerciseDao
import com.gymora.data.local.dao.SetTemplateDao
import com.gymora.data.local.dao.SettingsDao
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.seed.LibrarySeeder
import com.gymora.data.local.seed.LibrarySeederImpl
import com.gymora.data.local.seed.RoutineSeeder
import com.gymora.data.local.seed.RoutineSeederImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val SINGLE_ACTIVE_INDEX_SQL =
        "CREATE UNIQUE INDEX IF NOT EXISTS index_workout_sessions_single_active " +
            "ON workout_sessions(status) WHERE status = 'ACTIVE'"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        seeder: LibrarySeeder,
        routineSeeder: RoutineSeeder,
    ): GymoraDatabase {
        val databaseRef = AtomicReference<GymoraDatabase>()
        val database = Room.databaseBuilder(
            context,
            GymoraDatabase::class.java,
            GymoraDatabase.NAME,
        )
            .addMigrations(GymoraDatabase.MIGRATION_1_2)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Runs after Room's schema validation, so the partial index
                    // (unknown to Room) cannot trip it. Recreates it after a
                    // migration dropped it; no-op otherwise.
                    db.execSQL(SINGLE_ACTIVE_INDEX_SQL)
                }

                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // BR-14 / Constitution V: at most one active session, enforced
                    // at the persistence layer. Room's @Index cannot express a
                    // partial index, so it is created here (data-model.md).
                    db.execSQL(SINGLE_ACTIVE_INDEX_SQL)
                    // R-08: seed the built-in exercise library on first database
                    // creation. Runs after the database object is fully built.
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        databaseRef.get()?.let { db ->
                            seeder.seed(db)
                            // Default routines depend on the library being seeded first.
                            routineSeeder.seed(db)
                        }
                    }
                }
            })
            .build()
        databaseRef.set(database)
        return database
    }

    @Provides
    fun provideSettingsDao(database: GymoraDatabase): SettingsDao = database.settingsDao()

    @Provides
    fun provideExerciseDao(database: GymoraDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideRoutineDao(database: GymoraDatabase): RoutineDao = database.routineDao()

    @Provides
    fun provideRoutineExerciseDao(database: GymoraDatabase): RoutineExerciseDao =
        database.routineExerciseDao()

    @Provides
    fun provideSetTemplateDao(database: GymoraDatabase): SetTemplateDao =
        database.setTemplateDao()


    /** Default-routine seeder (seeds home-screen routines on first launch). */
    @Provides
    @Singleton
    fun provideRoutineSeeder(seeder: RoutineSeederImpl): RoutineSeeder = seeder
    /** Built-in exercise library seeder (T017, R-08). */
    @Provides
    @Singleton
    fun provideLibrarySeeder(seeder: LibrarySeederImpl): LibrarySeeder = seeder
}
