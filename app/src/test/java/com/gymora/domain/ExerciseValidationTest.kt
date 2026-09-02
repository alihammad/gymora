package com.gymora.domain

import com.gymora.domain.model.CreateExerciseInput
import com.gymora.domain.model.ExerciseValidation
import com.gymora.domain.model.MuscleGroup
import com.gymora.domain.model.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * T011 [US1]: exercise input validation (FR-006).
 * Blank names are rejected; muscle group is restricted to the enum set
 * (type-enforced; every enum value must be accepted).
 */
class ExerciseValidationTest {

    @Test
    fun `blank name is rejected`() {
        assertThrows(ValidationException::class.java) {
            ExerciseValidation.validate(CreateExerciseInput(name = "   "))
        }
    }

    @Test
    fun `empty name is rejected`() {
        assertThrows(ValidationException::class.java) {
            ExerciseValidation.validate(CreateExerciseInput(name = ""))
        }
    }

    @Test
    fun `valid name with all optional fields is accepted`() {
        ExerciseValidation.validate(
            CreateExerciseInput(
                name = "Bench Press",
                muscleGroup = MuscleGroup.CHEST,
                description = "Barbell press",
                notes = "Grip shoulder width",
            ),
        )
    }

    @Test
    fun `valid name without optional fields is accepted`() {
        ExerciseValidation.validate(CreateExerciseInput(name = "Custom Curl"))
    }

    @Test
    fun `every muscle group value is accepted`() {
        MuscleGroup.entries.forEach { group ->
            ExerciseValidation.validate(
                CreateExerciseInput(name = "Exercise", muscleGroup = group),
            )
        }
    }
}
