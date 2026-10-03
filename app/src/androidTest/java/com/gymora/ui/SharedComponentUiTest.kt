package com.gymora.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gymora.ui.components.ConfirmDialog
import com.gymora.ui.components.EmptyState
import com.gymora.ui.components.EmptyStateCopy
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * T073: Compose UI tests for shared components and empty states.
 *
 * These cover the spec-mandated empty-state copy (FR-059) and confirmation
 * dialog flows without requiring database/Hilt setup. Journey-level tests
 * (start→log→finish, recovery, cancel/discard) additionally require an
 * emulator and are exercised via `connectedDebugAndroidTest` (Constitution VII).
 */
class SharedComponentUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateShowsSpecCopyAndOptionalAction() {
        composeRule.setContent {
            EmptyState(
                message = EmptyStateCopy.NO_ROUTINES,
                actionLabel = EmptyStateCopy.CREATE_ROUTINE_ACTION,
                onAction = {},
            )
        }

        composeRule.onNodeWithText(EmptyStateCopy.NO_ROUTINES).assertIsDisplayed()
        composeRule.onNodeWithText(EmptyStateCopy.CREATE_ROUTINE_ACTION).assertIsDisplayed()
    }

    @Test
    fun noHistoryEmptyStateShowsSpecCopy() {
        composeRule.setContent {
            EmptyState(message = EmptyStateCopy.NO_HISTORY)
        }

        composeRule.onNodeWithText(EmptyStateCopy.NO_HISTORY).assertIsDisplayed()
    }

    @Test
    fun confirmDialogInvokesConfirmAndDismiss() {
        var confirmed = false
        var dismissed = false
        composeRule.setContent {
            ConfirmDialog(
                title = "Delete workout",
                message = "This cannot be undone.",
                confirmLabel = "Delete",
                dismissLabel = "Cancel",
                onConfirm = { confirmed = true },
                onDismiss = { dismissed = true },
            )
        }

        composeRule.onNodeWithText("Delete workout").assertIsDisplayed()
        composeRule.onNodeWithText("This cannot be undone.").assertIsDisplayed()

        composeRule.onNodeWithText("Delete").performClick()
        assertTrue(confirmed)

        composeRule.onNodeWithText("Cancel").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun discardConfirmUsesSpecCopy() {
        composeRule.setContent {
            ConfirmDialog(
                title = "Discard workout?",
                message = "Your progress will be permanently deleted.",
                confirmLabel = "Discard workout",
                dismissLabel = "Keep working out",
                onConfirm = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Discard workout?").assertIsDisplayed()
        composeRule.onNodeWithText("Keep working out").assertIsDisplayed()
        composeRule.onNodeWithText("Discard workout").assertIsDisplayed()
    }
}
