package com.gymora.domain

import com.gymora.domain.model.RoutineRules
import com.gymora.domain.model.ValidationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * T022 [US2]: routine validation and duplicate naming (FR-011, FR-014, R-10).
 */
class RoutineRulesTest {

    @Test
    fun `blank routine name is rejected`() {
        assertThrows(ValidationException::class.java) {
            RoutineRules.validateName("   ")
        }
    }

    @Test
    fun `valid routine name is accepted`() {
        RoutineRules.validateName("Chest Workout")
    }

    @Test
    fun `duplicate name derives Copy suffix`() {
        assertEquals("Chest Workout Copy", RoutineRules.duplicateName("Chest Workout"))
    }

    @Test
    fun `duplicate name of already duplicated routine appends again`() {
        assertEquals(
            "Chest Workout Copy Copy",
            RoutineRules.duplicateName("Chest Workout Copy"),
        )
    }
}
