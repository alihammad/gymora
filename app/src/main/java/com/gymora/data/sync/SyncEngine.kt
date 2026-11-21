package com.gymora.data.sync

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.SettingsEntity
import com.gymora.data.local.sync.SyncTriggers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class SyncResult(val downloaded: Int, val uploaded: Int)

/**
 * Two-way sync between this account's database and its cloud copy.
 *
 * Each workout, routine, custom exercise and measurement is one document keyed by its
 * `sync_id`, children nested inside, exercises and routines referenced by sync id. A sync
 * first downloads what changed in the cloud since last time and keeps whichever side was
 * edited last, then uploads every local row edited since the cloud last saw it.
 *
 * Bookkeeping lives in the account's database (tables created by [SyncTriggers]):
 * `sync_meta` holds the cloud's `updatedAt` for each document, so a row needs uploading
 * exactly when its `updated_at` is newer; `sync_state` holds how far each collection has
 * been downloaded. Downloaded changes are applied with the triggers stood down, so they
 * are not mistaken for local edits.
 *
 * Not synced: a workout still in progress, built-in exercises, step counts, exercise
 * media files, and settings that describe this phone (backup folder, Health Connect, reminders).
 */
@Singleton
class SyncEngine @Inject constructor(
    private val database: GymoraDatabase,
    private val remote: SyncRemote,
) {

    private val mutex = Mutex()

    private val db: SupportSQLiteDatabase get() = database.openHelper.writableDatabase

    /** Syncs [uid]'s data. With [onlyIfPending], skips the network unless something local needs uploading. */
    suspend fun sync(uid: String, onlyIfPending: Boolean = false): SyncResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (onlyIfPending && !hasPendingChangesLocked()) return@withContext SyncResult(0, 0)
            val downloaded = COLLECTIONS.sumOf { download(uid, it) }
            val uploaded = ROOTS.sumOf { upload(uid, it) } + uploadSettings(uid)
            setState(KEY_LAST_SYNC, System.currentTimeMillis())
            SyncResult(downloaded, uploaded)
        }
    }

    suspend fun hasPendingChanges(): Boolean = withContext(Dispatchers.IO) { hasPendingChangesLocked() }

    /** When the last sync finished, or null if this account never synced on this phone. */
    suspend fun lastSyncedAt(): Long? = withContext(Dispatchers.IO) { state(KEY_LAST_SYNC) }

    /**
     * Deletes [uid]'s cloud copy, for account deletion. Forgets what the cloud held, so if
     * deleting the login then fails, the next sync uploads everything again.
     */
    suspend fun deleteCloudData(uid: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            remote.deleteAll(uid, COLLECTIONS)
            db.execSQL("DELETE FROM sync_meta")
            db.execSQL("DELETE FROM sync_state")
        }
    }

    // ---- Download ----

    private suspend fun download(uid: String, collection: String): Int {
        val key = PULL_PREFIX + collection
        val docs = remote.pull(uid, collection, state(key) ?: 0L)
        if (docs.isEmpty()) return 0
        database.withTransaction {
            SyncTriggers.quietly(db) {
                val root = ROOTS.firstOrNull { it.collection == collection }
                docs.forEach { doc ->
                    if (root == null) applySettings(doc) else applyRoot(root, doc)
                    setMeta(collection, doc.id, doc.updatedAt)
                }
            }
            setState(key, docs.maxOf { it.serverTime })
        }
        return docs.size
    }

    private fun applyRoot(root: Root, doc: RemoteDoc) {
        val table = root.node.table
        val local = queryRows(db, "SELECT id, updated_at FROM $table WHERE sync_id = ?", doc.id).firstOrNull()
        val localId = local?.get(ID) as Long?
        val cloudIsNewer = doc.updatedAt > (local?.get(UPDATED_AT) as Long? ?: Long.MIN_VALUE)
        when {
            localId != null && root.isInProgress(localId) -> Unit
            localId != null && cloudIsNewer ->
                if (doc.deleted) deleteLocal(root, localId, doc.updatedAt) else replaceLocal(root, localId, doc)
            localId == null && !doc.deleted && !deletedHereSince(table, doc) -> insertLocal(root, doc)
        }
    }

    /** Deleted on this phone after the cloud's last edit: the deletion wins and uploads later. */
    private fun deletedHereSince(table: String, doc: RemoteDoc): Boolean {
        val deletedAt = queryRows(
            db,
            "SELECT deleted_at FROM sync_tombstones WHERE entity = ? AND sync_id = ?",
            table,
            doc.id,
        ).firstOrNull()?.get("deleted_at") as Long?
        return deletedAt != null && deletedAt >= doc.updatedAt
    }

    private fun replaceLocal(root: Root, localId: Long, doc: RemoteDoc) {
        val values = rowValues(root.node, doc.data) ?: return
        values.put(UPDATED_AT, doc.updatedAt)
        db.update(root.node.table, SQLiteDatabase.CONFLICT_ABORT, values, "id = ?", arrayOf(localId))
        root.node.children.forEach { child ->
            db.execSQL("DELETE FROM ${child.table} WHERE ${child.parentColumn} = ?", arrayOf(localId))
            insertChildren(child, localId, doc.data[child.key])
        }
    }

    private fun insertLocal(root: Root, doc: RemoteDoc) {
        val table = root.node.table
        val values = rowValues(root.node, doc.data) ?: return
        values.put(SYNC_ID, doc.id)
        values.put(UPDATED_AT, doc.updatedAt)
        val id = db.insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
        root.node.children.forEach { insertChildren(it, id, doc.data[it.key]) }
        db.execSQL("DELETE FROM sync_tombstones WHERE entity = ? AND sync_id = ?", arrayOf(table, doc.id))
    }

    private fun deleteLocal(root: Root, id: Long, deletedAt: Long) {
        if (root.collection == EXERCISES) {
            // Routines and history may still point at it; the app soft-deletes exercises anyway.
            db.execSQL(
                "UPDATE exercises SET deleted_at = ?, updated_at = ? WHERE id = ?",
                arrayOf(deletedAt, deletedAt, id),
            )
        } else {
            db.execSQL("DELETE FROM ${root.node.table} WHERE id = ?", arrayOf(id))
        }
    }

    private fun insertChildren(node: Node, parentId: Long, raw: Any?) {
        val items = (raw as? List<*>).orEmpty().filterIsInstance<Map<String, Any?>>()
        val groupFirstId = mutableMapOf<Any, Long>()
        val inserted = mutableListOf<Pair<Long, Any?>>()
        items.forEach { item ->
            val values = rowValues(node, item) ?: return@forEach
            values.put(node.parentColumn, parentId)
            val id = db.insert(node.table, SQLiteDatabase.CONFLICT_ABORT, values)
            val group = item[SUPERSET_GROUP]
            if (group != null) groupFirstId.putIfAbsent(group, id)
            inserted += id to group
            node.children.forEach { insertChildren(it, id, item[it.key]) }
        }
        // Superset groups are labelled by their first member's row id, which differs per phone.
        inserted.forEach { (id, group) ->
            val label = group?.let(groupFirstId::get) ?: return@forEach
            db.execSQL("UPDATE ${node.table} SET $SUPERSET_GROUP = ? WHERE id = ?", arrayOf(label, id))
        }
    }

    /** The row's columns from [data], references turned back into local ids; null if a required one is missing. */
    private fun rowValues(node: Node, data: Map<String, Any?>): ContentValues? {
        val columns = columnsOf(node.table)
        val skipped = setOf(ID, SYNC_ID, UPDATED_AT, node.parentColumn) + node.refs.map { it.column }
        val values = ContentValues()
        data.forEach { (key, value) -> if (key in columns && key !in skipped) values.putValue(key, value) }
        node.refs.forEach { ref ->
            val localId = (data[ref.syncKey] as? String)?.let { localIdOf(ref.table, it) }
            when {
                localId != null -> values.put(ref.column, localId)
                ref.required -> return null
                else -> values.putNull(ref.column)
            }
        }
        return values
    }

    private fun applySettings(doc: RemoteDoc) {
        val local = queryRows(db, "SELECT updated_at FROM settings WHERE id = ?", SettingsEntity.SINGLE_ROW_ID)
            .firstOrNull()
        val localUpdatedAt = local?.get("updated_at") as Long? ?: Long.MIN_VALUE
        if (doc.deleted || doc.updatedAt <= localUpdatedAt) return
        val columns = columnsOf(SETTINGS_TABLE)
        val values = ContentValues()
        doc.data.forEach { (key, value) ->
            if (key in columns && key !in SETTINGS_NOT_SYNCED) values.putValue(key, value)
        }
        values.put(UPDATED_AT, doc.updatedAt)
        if (local == null) {
            values.put(ID, SettingsEntity.SINGLE_ROW_ID)
            db.insert(SETTINGS_TABLE, SQLiteDatabase.CONFLICT_REPLACE, values)
        } else {
            db.update(
                SETTINGS_TABLE,
                SQLiteDatabase.CONFLICT_ABORT,
                values,
                "id = ?",
                arrayOf(SettingsEntity.SINGLE_ROW_ID),
            )
        }
    }

    // ---- Upload ----

    private suspend fun upload(uid: String, root: Root): Int {
        val table = root.node.table
        val rows = queryRows(db, root.pendingSql(), root.collection)
        val tombstones = queryRows(
            db,
            "SELECT t.sync_id, t.deleted_at FROM sync_tombstones t " +
                "LEFT JOIN sync_meta m ON m.collection = ? AND m.sync_id = t.sync_id " +
                "WHERE t.entity = ? AND (m.remote_updated_at IS NULL OR t.deleted_at > m.remote_updated_at)",
            root.collection,
            table,
        )
        val docs = rows.map { row ->
            RemoteDoc(row[SYNC_ID] as String, row[UPDATED_AT] as Long, data = documentData(root.node, row))
        } + tombstones.map { RemoteDoc(it[SYNC_ID] as String, it["deleted_at"] as Long, deleted = true) }
        if (docs.isEmpty()) return 0
        remote.push(uid, root.collection, docs)
        database.withTransaction {
            docs.forEach { setMeta(root.collection, it.id, it.updatedAt) }
            tombstones.forEach {
                db.execSQL(
                    "DELETE FROM sync_tombstones WHERE entity = ? AND sync_id = ? AND deleted_at = ?",
                    arrayOf(table, it[SYNC_ID], it["deleted_at"]),
                )
            }
        }
        return docs.size
    }

    private fun documentData(node: Node, row: Map<String, Any?>): Map<String, Any?> {
        val skipped = setOf(ID, SYNC_ID, UPDATED_AT, node.parentColumn) + node.refs.map { it.column }
        val data = row.filterKeys { it !in skipped }.toMutableMap()
        node.refs.forEach { ref -> data[ref.syncKey] = (row[ref.column] as Long?)?.let { syncIdOf(ref.table, it) } }
        node.children.forEach { child ->
            data[child.key!!] = queryRows(
                db,
                "SELECT * FROM ${child.table} WHERE ${child.parentColumn} = ? ORDER BY ${child.orderBy}",
                row[ID],
            ).map { documentData(child, it) }
        }
        return data
    }

    private suspend fun uploadSettings(uid: String): Int {
        val row = pendingSettings() ?: return 0
        val updatedAt = row[UPDATED_AT] as Long
        val data = row.filterKeys { it !in SETTINGS_NOT_SYNCED && it != ID && it != UPDATED_AT }
        remote.push(uid, SETTINGS, listOf(RemoteDoc(SETTINGS_DOC, updatedAt, data = data)))
        setMeta(SETTINGS, SETTINGS_DOC, updatedAt)
        return 1
    }

    private fun pendingSettings(): Map<String, Any?>? {
        val row = queryRows(db, "SELECT * FROM settings WHERE id = ?", SettingsEntity.SINGLE_ROW_ID).firstOrNull()
            ?: return null
        val known = meta(SETTINGS, SETTINGS_DOC)
        return row.takeIf { known == null || (it[UPDATED_AT] as Long) > known }
    }

    private fun hasPendingChangesLocked(): Boolean =
        ROOTS.any { queryRows(db, "${it.pendingSql()} LIMIT 1", it.collection).isNotEmpty() } ||
            queryRows(
                db,
                "SELECT 1 FROM sync_tombstones t LEFT JOIN sync_meta m ON m.sync_id = t.sync_id " +
                    "WHERE m.remote_updated_at IS NULL OR t.deleted_at > m.remote_updated_at LIMIT 1",
            ).isNotEmpty() ||
            pendingSettings() != null

    // ---- Bookkeeping ----

    private fun meta(collection: String, syncId: String): Long? = queryRows(
        db,
        "SELECT remote_updated_at FROM sync_meta WHERE collection = ? AND sync_id = ?",
        collection,
        syncId,
    ).firstOrNull()?.get("remote_updated_at") as Long?

    private fun setMeta(collection: String, syncId: String, updatedAt: Long) = db.execSQL(
        "INSERT OR REPLACE INTO sync_meta (collection, sync_id, remote_updated_at) VALUES (?, ?, ?)",
        arrayOf(collection, syncId, updatedAt),
    )

    private fun state(key: String): Long? =
        queryRows(db, "SELECT value FROM sync_state WHERE key = ?", key).firstOrNull()?.get("value") as Long?

    private fun setState(key: String, value: Long) =
        db.execSQL("INSERT OR REPLACE INTO sync_state (key, value) VALUES (?, ?)", arrayOf(key, value))

    private fun syncIdOf(table: String, id: Long): String? =
        queryRows(db, "SELECT sync_id FROM $table WHERE id = ?", id).firstOrNull()?.get(SYNC_ID) as String?

    private fun localIdOf(table: String, syncId: String): Long? =
        queryRows(db, "SELECT id FROM $table WHERE sync_id = ?", syncId).firstOrNull()?.get(ID) as Long?

    private val columnCache = mutableMapOf<String, Set<String>>()

    private fun columnsOf(table: String): Set<String> = columnCache.getOrPut(table) {
        queryRows(db, "PRAGMA table_info(`$table`)").map { it["name"] as String }.toSet()
    }

    private fun Root.isInProgress(id: Long): Boolean = collection == WORKOUTS &&
        queryRows(db, "SELECT 1 FROM workout_sessions WHERE id = ? AND status = 'ACTIVE'", id).isNotEmpty()

    /** A table synced as its own documents, or one nested inside its parent's document. */
    private class Node(
        val table: String,
        val parentColumn: String? = null,
        val key: String? = null,
        val orderBy: String = ID,
        val refs: List<Ref> = emptyList(),
        val children: List<Node> = emptyList(),
    )

    /** A column pointing at another synced table's row, stored in the cloud by sync id. */
    private class Ref(val column: String, val table: String, val required: Boolean) {
        val syncKey = "${column}_sync_id"
    }

    private class Root(val collection: String, val node: Node, val uploadFilter: String) {
        /** Rows edited since the cloud last saw them; one argument, the collection name. */
        fun pendingSql() = "SELECT t.* FROM ${node.table} t " +
            "LEFT JOIN sync_meta m ON m.collection = ? AND m.sync_id = t.sync_id " +
            "WHERE t.sync_id IS NOT NULL AND ($uploadFilter) " +
            "AND (m.remote_updated_at IS NULL OR t.updated_at > m.remote_updated_at)"
    }

    private companion object {
        const val ID = "id"
        const val SYNC_ID = "sync_id"
        const val UPDATED_AT = "updated_at"
        const val SUPERSET_GROUP = "superset_group"
        const val SETTINGS_TABLE = "settings"
        const val EXERCISES = "exercises"
        const val WORKOUTS = "workouts"
        const val SETTINGS = "settings"
        const val SETTINGS_DOC = "main"
        const val PULL_PREFIX = "pull:"
        const val KEY_LAST_SYNC = "last_sync"

        /** Phone-specific settings; each phone keeps its own. */
        val SETTINGS_NOT_SYNCED =
            SettingsEntity.DEVICE_LOCAL_COLUMNS.toSet() + setOf("reminder_days", "reminder_minute_of_day")

        /** Download order matters: routines and workouts refer to exercises, workouts to routines. */
        val ROOTS = listOf(
            Root(EXERCISES, Node("exercises"), uploadFilter = "t.is_custom = 1"),
            Root(
                "routines",
                Node(
                    "routines",
                    children = listOf(
                        Node(
                            "routine_exercises",
                            parentColumn = "routine_id",
                            key = "exercises",
                            orderBy = "position",
                            refs = listOf(Ref("exercise_id", "exercises", required = true)),
                            children = listOf(
                                Node(
                                    "set_templates",
                                    parentColumn = "routine_exercise_id",
                                    key = "sets",
                                    orderBy = "set_number",
                                ),
                            ),
                        ),
                    ),
                ),
                uploadFilter = "1",
            ),
            Root(
                WORKOUTS,
                Node(
                    "workout_sessions",
                    refs = listOf(Ref("routine_id", "routines", required = false)),
                    children = listOf(
                        Node(
                            "workout_exercises",
                            parentColumn = "session_id",
                            key = "exercises",
                            orderBy = "position",
                            refs = listOf(Ref("exercise_id", "exercises", required = false)),
                            children = listOf(
                                Node(
                                    "workout_sets",
                                    parentColumn = "workout_exercise_id",
                                    key = "sets",
                                    orderBy = "set_number",
                                ),
                            ),
                        ),
                    ),
                ),
                // A workout in progress syncs once it is finished.
                uploadFilter = "t.status != 'ACTIVE'",
            ),
            Root("measurements", Node("body_measurements"), uploadFilter = "1"),
        )

        val COLLECTIONS = ROOTS.map { it.collection } + SETTINGS

        fun queryRows(db: SupportSQLiteDatabase, sql: String, vararg args: Any?): List<Map<String, Any?>> =
            db.query(sql, args).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add((0 until cursor.columnCount).associate { cursor.getColumnName(it) to cursor.cell(it) })
                    }
                }
            }

        fun Cursor.cell(index: Int): Any? = when (getType(index)) {
            Cursor.FIELD_TYPE_INTEGER -> getLong(index)
            Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
            Cursor.FIELD_TYPE_STRING -> getString(index)
            else -> null
        }

        fun ContentValues.putValue(key: String, value: Any?) = when (value) {
            null -> putNull(key)
            is String -> put(key, value)
            is Boolean -> put(key, if (value) 1L else 0L)
            is Int, is Long -> put(key, (value as Number).toLong())
            is Number -> put(key, value.toDouble())
            else -> put(key, value.toString())
        }
    }
}
