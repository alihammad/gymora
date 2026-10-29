package com.gymora.data.local.sync

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray

/**
 * Gives exercises without a `sync_id` one (v7). Built-ins take the stable id from the
 * bundled exercises.json, matched by name, so the same exercise has the same id on every
 * device; anything unmatched gets a random id. Needed after the 6→7 migration and after
 * restoring a pre-v7 backup — [SyncTriggers] deliberately leaves built-ins to this.
 */
@Singleton
class BuiltInSyncIds @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun backfill(db: SupportSQLiteDatabase) {
        val missing = db.query("SELECT id, name FROM exercises WHERE sync_id IS NULL").use { c ->
            buildList { while (c.moveToNext()) add(c.getLong(0) to c.getString(1)) }
        }
        if (missing.isEmpty()) return
        val used = db.query("SELECT sync_id FROM exercises WHERE sync_id IS NOT NULL").use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(0)) }.toMutableSet()
        }
        val known = loadBuiltInIds()
        missing.forEach { (id, name) ->
            val syncId = known[name.lowercase()]?.takeIf(used::add) ?: UUID.randomUUID().toString()
            db.execSQL("UPDATE exercises SET sync_id = ? WHERE id = ?", arrayOf<Any>(syncId, id))
        }
    }

    /** Lower-cased built-in name → exercises.json id. */
    private fun loadBuiltInIds(): Map<String, String> {
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        return (0 until array.length()).associate { i ->
            val o = array.getJSONObject(i)
            o.getString("name").lowercase() to o.getString("id")
        }
    }

    private companion object {
        const val ASSET_NAME = "exercises.json"
    }
}
