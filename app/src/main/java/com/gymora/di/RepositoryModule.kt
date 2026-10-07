package com.gymora.di

import com.gymora.data.repository.ExerciseRepositoryImpl
import com.gymora.data.repository.HistoryRepositoryImpl
import com.gymora.data.repository.RecordsRepositoryImpl
import com.gymora.data.repository.RoutineRepositoryImpl
import com.gymora.data.repository.SettingsRepositoryImpl
import com.gymora.data.repository.WorkoutSessionRepositoryImpl
import com.gymora.domain.repository.ExerciseRepository
import com.gymora.domain.repository.HistoryRepository
import com.gymora.domain.repository.RecordsRepository
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

    @Binds
    @Singleton
    abstract fun bindBodyMeasurementRepository(
        impl: com.gymora.data.repository.BodyMeasurementRepositoryImpl,
    ): com.gymora.domain.repository.BodyMeasurementRepository

    @Binds
    @Singleton
    abstract fun bindRecordsRepository(impl: RecordsRepositoryImpl): RecordsRepository

    @Binds
    @Singleton
    abstract fun bindStepRepository(
        impl: com.gymora.data.repository.StepRepositoryImpl,
    ): com.gymora.domain.repository.StepRepository
}
