package com.gymora.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymora.data.local.dao.ExerciseDao
import com.gymora.data.local.dao.SettingsDao
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.seed.LibrarySeeder
import com.gymora.data.local.seed.LibrarySeederImpl
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

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        seeder: LibrarySeeder,
    ): GymoraDatabase {
        val databaseRef = AtomicReference<GymoraDatabase>()
        val database = Room.databaseBuilder(
            context,
            GymoraDatabase::class.java,
            GymoraDatabase.NAME,
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // R-08: seed the built-in exercise library on first database
                    // creation. Runs after the database object is fully built.
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        databaseRef.get()?.let { seeder.seed(it) }
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

    /** Built-in exercise library seeder (T017, R-08). */
    @Provides
    @Singleton
    fun provideLibrarySeeder(): LibrarySeeder = LibrarySeederImpl()
}
