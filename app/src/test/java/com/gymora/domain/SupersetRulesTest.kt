package com.gymora.domain

import com.gymora.domain.model.SupersetEntry
import com.gymora.domain.model.SupersetRules
import org.junit.Assert.assertEquals
import org.junit.Test

/** Superset grouping: display blocks, linking, unlinking and normalization. */
class SupersetRulesTest {

    private fun entries(vararg groups: Long?) =
        groups.mapIndexed { i, group -> SupersetEntry(id = i + 1L, group = group) }

    @Test
    fun `blocks group adjacent exercises sharing a group`() {
        val blocks = SupersetRules.blocks(entries(null, 2L, 2L, null, 5L, 5L, 5L)) { it.group }
        assertEquals(
            listOf(listOf(1L), listOf(2L, 3L), listOf(4L), listOf(5L, 6L, 7L)),
            blocks.map { block -> block.map { it.id } },
        )
    }

    @Test
    fun `normalize clears singles and relabels runs to their first member`() {
        val groups = SupersetRules.normalize(entries(9L, null, 3L, 3L))
        assertEquals(mapOf(1L to null, 2L to null, 3L to 3L, 4L to 3L), groups)
    }

    @Test
    fun `normalize splits a group that is no longer adjacent`() {
        val groups = SupersetRules.normalize(entries(1L, null, 1L))
        assertEquals(mapOf(1L to null, 2L to null, 3L to null), groups)
    }

    @Test
    fun `linking two singles creates a superset`() {
        val groups = SupersetRules.linkWithNext(entries(null, null, null), index = 0)
        assertEquals(mapOf(1L to 1L, 2L to 1L, 3L to null), groups)
    }

    @Test
    fun `linking extends a superset with the next exercise`() {
        val groups = SupersetRules.linkWithNext(entries(1L, 1L, null), index = 1)
        assertEquals(mapOf(1L to 1L, 2L to 1L, 3L to 1L), groups)
    }

    @Test
    fun `linking merges two adjacent supersets`() {
        val groups = SupersetRules.linkWithNext(entries(1L, 1L, 3L, 3L, null), index = 1)
        assertEquals(mapOf(1L to 1L, 2L to 1L, 3L to 1L, 4L to 1L, 5L to null), groups)
    }

    @Test
    fun `unlinking a pair leaves two singles`() {
        val groups = SupersetRules.unlinkFromNext(entries(1L, 1L), index = 0)
        assertEquals(mapOf(1L to null, 2L to null), groups)
    }

    @Test
    fun `unlinking in the middle splits into two supersets`() {
        val groups = SupersetRules.unlinkFromNext(entries(1L, 1L, 1L, 1L), index = 1)
        assertEquals(mapOf(1L to 1L, 2L to 1L, 3L to 3L, 4L to 3L), groups)
    }

    @Test
    fun `unlinking from a later superset leaves the earlier one intact`() {
        val groups = SupersetRules.unlinkFromNext(entries(1L, 1L, 3L, 3L, 3L), index = 3)
        assertEquals(mapOf(1L to 1L, 2L to 1L, 3L to 3L, 4L to 3L, 5L to null), groups)
    }
}
