package com.gymora.di

import com.gymora.data.repository.ExerciseRepositoryImpl
import com.gymora.data.repository.HistoryRepositoryImpl
import com.gymora.data.repository.RoutineRepositoryImpl
import com.gymora.data.repository.SettingsRepositoryImpl
import com.gymora.data.repository.WorkoutSessionRepositoryImpl
import com.gymora.domain.repository.ExerciseRepository
import com.gymora.domain.repository.HistoryRepository
import com.gymora.domain.repository.RoutineRepository
import com.gymora.domain.repository.SettingsRepository
import com.gymora.domain.repository.WorkoutSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutSessionRepository(
        impl: WorkoutSessionRepositoryImpl,
    ): WorkoutSessionRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
}
