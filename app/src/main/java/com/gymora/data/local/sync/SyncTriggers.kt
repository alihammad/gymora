package com.gymora.data.local.sync

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * SQLite triggers that keep the sync bookkeeping (v7) correct for every write path —
 * repositories, seeders, Health Connect import and backup restore — without each
 * caller having to remember it:
 *
 * - new synced rows get a random UUID `sync_id` (built-in exercises excluded: they
 *   take their stable exercises.json id, see [BuiltInSyncIds]);
 * - an update that writes a stale entity cannot clear `sync_id` or move `updated_at` back;
 * - any change to a routine's or workout's children bumps the parent's `updated_at`,
 *   since the parent is synced as one document;
 * - deleting a synced row leaves a `sync_tombstones` entry.
 *
 * Android runs SQLite with recursive triggers on, so every trigger that updates its own
 * table is guarded by a WHEN clause its own update no longer satisfies.
 *
 * Room does not validate triggers, so they are created with IF NOT EXISTS from the
 * database callbacks (see DatabaseModule) rather than in migrations.
 */
object SyncTriggers {

    /** Epoch milliseconds; `unixepoch('subsec')` needs a newer SQLite than older Android ships. */
    private const val NOW_MS = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"

    /** Strictly after [previous], even if the clock is behind it (keeps the touch trigger from recursing). */
    private fun touched(previous: String) = "MAX($previous + 1, $NOW_MS)"

    /** Random version-4 UUID in canonical lower-case form. */
    const val NEW_UUID_SQL =
        "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || " +
            "substr(hex(randomblob(2)), 2) || '-' || substr('89ab', 1 + (random() & 3), 1) || " +
            "substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6)))"

    /** Tables synced as their own document, with the extra condition for assigning a random id. */
    private val ROOTS = mapOf(
        "exercises" to "AND is_custom = 1",
        "routines" to "",
        "workout_sessions" to "",
        "body_measurements" to "",
    )

    private class Child(val table: String, val root: String, val rootId: (row: String) -> String)

    private val CHILDREN = listOf(
        Child("routine_exercises", "routines") { "$it.routine_id" },
        Child("set_templates", "routines") {
            "(SELECT routine_id FROM routine_exercises WHERE id = $it.routine_exercise_id)"
        },
        Child("workout_exercises", "workout_sessions") { "$it.session_id" },
        Child("workout_sets", "workout_sessions") {
            "(SELECT session_id FROM workout_exercises WHERE id = $it.workout_exercise_id)"
        },
    )

    val STATEMENTS: List<String> = ROOTS.flatMap { (table, idCondition) ->
        listOf(
            """
            CREATE TRIGGER IF NOT EXISTS sync_${table}_insert AFTER INSERT ON $table BEGIN
                UPDATE $table SET sync_id = $NEW_UUID_SQL WHERE id = NEW.id AND sync_id IS NULL $idCondition;
                UPDATE $table SET updated_at = $NOW_MS WHERE id = NEW.id AND updated_at = 0;
                DELETE FROM sync_tombstones WHERE entity = '$table' AND sync_id = NEW.sync_id;
            END
            """,
            """
            CREATE TRIGGER IF NOT EXISTS sync_${table}_keep_id AFTER UPDATE ON $table
            WHEN NEW.sync_id IS NULL AND OLD.sync_id IS NOT NULL BEGIN
                UPDATE $table SET sync_id = OLD.sync_id WHERE id = NEW.id;
            END
            """,
            """
            CREATE TRIGGER IF NOT EXISTS sync_${table}_touch AFTER UPDATE ON $table
            WHEN NEW.updated_at <= OLD.updated_at BEGIN
                UPDATE $table SET updated_at = ${touched("OLD.updated_at")} WHERE id = NEW.id;
            END
            """,
            """
            CREATE TRIGGER IF NOT EXISTS sync_${table}_delete AFTER DELETE ON $table
            WHEN OLD.sync_id IS NOT NULL BEGIN
                INSERT OR REPLACE INTO sync_tombstones (entity, sync_id, deleted_at)
                    VALUES ('$table', OLD.sync_id, $NOW_MS);
            END
            """,
        )
    } + CHILDREN.flatMap { child ->
        val touch = "UPDATE ${child.root} SET updated_at = ${touched("updated_at")} WHERE id"
        listOf(
            "insert" to "$touch = ${child.rootId("NEW")};",
            "update" to "$touch IN (${child.rootId("OLD")}, ${child.rootId("NEW")});",
            "delete" to "$touch = ${child.rootId("OLD")};",
        ).map { (event, body) ->
            "CREATE TRIGGER IF NOT EXISTS sync_${child.table}_$event AFTER ${event.uppercase()} " +
                "ON ${child.table} BEGIN $body END"
        }
    }

    fun install(db: SupportSQLiteDatabase) {
        STATEMENTS.forEach(db::execSQL)
    }
}
