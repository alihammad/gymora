package com.gymora.data.backup

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.SettingsEntity
import com.gymora.data.media.ExerciseMediaStore
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Counts of what a backup holds. */
data class BackupSummary(
    val workouts: Int,
    val routines: Int,
    val exercises: Int,
    val measurements: Int,
    val mediaFiles: Int,
)

class BackupException(message: String) : Exception(message)

/**
 * Full backup and restore: every table (workouts, routines, exercise library and
 * custom exercises, body measurements, settings) plus exercise media, in one zip.
 *
 * The zip holds `backup.json` (rows keyed by column name, so a backup restores into
 * later schema versions: unknown columns are dropped, new ones take their defaults)
 * and `media/<file>` for each demo image. Restore replaces all data atomically;
 * device-local settings (auto-backup folder, Health Connect) are kept.
 */
@Singleton
class BackupService @Inject constructor(
    private val database: GymoraDatabase,
    private val mediaStore: ExerciseMediaStore,
) {

    suspend fun export(out: OutputStream): BackupSummary = withContext(Dispatchers.IO) {
        val db = database.openHelper.readableDatabase
        val tables = JSONObject()
        // One read transaction gives a consistent snapshot across tables.
        database.withTransaction {
            TABLES.forEach { table -> tables.put(table, dumpTable(db, table)) }
        }
        val mediaFiles = mediaStore.directory.listFiles().orEmpty().filter { it.isFile }
        val root = JSONObject()
            .put("format", FORMAT)
            .put("formatVersion", FORMAT_VERSION)
            .put("schemaVersion", db.version)
            .put("createdAt", System.currentTimeMillis())
            .put("tables", tables)

        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(root.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            mediaFiles.forEach { file ->
                zip.putNextEntry(ZipEntry(MEDIA_PREFIX + file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        summarize(tables, mediaFiles.size)
    }

    /** Replaces all app data with the backup's. Throws [BackupException] for unusable files. */
    suspend fun restore(input: InputStream): BackupSummary = withContext(Dispatchers.IO) {
        val staging = File(mediaStore.directory.parentFile, STAGING_DIR).apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            val root = readZip(input, staging)
            val tables = validate(root)
            val db = database.openHelper.writableDatabase
            database.withTransaction {
                val keptSettings = currentDeviceSettings(db)
                TABLES.reversed().forEach { db.execSQL("DELETE FROM `$it`") }
                TABLES.forEach { table ->
                    val columns = columnsOf(db, table)
                    val rows = tables.optJSONArray(table) ?: JSONArray()
                    for (i in 0 until rows.length()) {
                        val values = toContentValues(rows.getJSONObject(i), columns)
                        if (table == SETTINGS_TABLE) keptSettings?.let(values::putAll)
                        db.insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
                    }
                }
            }
            replaceMedia(staging)
            summarize(tables, staging.listFiles().orEmpty().size)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun dumpTable(db: SupportSQLiteDatabase, table: String): JSONArray {
        val rows = JSONArray()
        db.query("SELECT * FROM `$table`").use { cursor ->
            while (cursor.moveToNext()) {
                val row = JSONObject()
                for (i in 0 until cursor.columnCount) {
                    row.put(cursor.getColumnName(i), cellValue(cursor, i))
                }
                rows.put(row)
            }
        }
        return rows
    }

    private fun cellValue(cursor: Cursor, index: Int): Any = when (cursor.getType(index)) {
        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
        Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
        else -> JSONObject.NULL // The schema has no BLOB columns.
    }

    private fun readZip(input: InputStream, staging: File): JSONObject {
        val text = ZipInputStream(input.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.mapNotNull { readEntry(zip, it, staging) }.lastOrNull()
        } ?: invalid("This is not a Gymora backup (backup.json is missing).")
        return runCatching { JSONObject(text) }.getOrElse { invalid(DAMAGED) }
    }

    /** Returns backup.json's text; stages media files; ignores anything else. */
    private fun readEntry(zip: ZipInputStream, entry: ZipEntry, staging: File): String? {
        val mediaName = entry.name.removePrefix(MEDIA_PREFIX)
        when {
            entry.name == JSON_ENTRY -> return zip.readBytes().toString(Charsets.UTF_8)
            entry.name.startsWith(MEDIA_PREFIX) && isFlatFileName(mediaName) ->
                File(staging, mediaName).outputStream().use { zip.copyTo(it) }
        }
        return null
    }

    /** Flat names only: a zip entry must never escape the staging folder. */
    private fun isFlatFileName(name: String): Boolean =
        name.isNotEmpty() && name != ".." && name.none { it == '/' || it == '\\' }

    private fun validate(root: JSONObject): JSONObject {
        if (root.optString("format") != FORMAT) invalid("This is not a Gymora backup.")
        val current = database.openHelper.readableDatabase.version
        if (root.optInt("schemaVersion", -1) > current) {
            invalid("This backup is from a newer version of Gymora. Update the app first.")
        }
        return root.optJSONObject("tables") ?: invalid(DAMAGED)
    }

    private fun invalid(message: String): Nothing = throw BackupException(message)

    private fun columnsOf(db: SupportSQLiteDatabase, table: String): Set<String> =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildSet { while (cursor.moveToNext()) add(cursor.getString(nameIndex)) }
        }

    private fun toContentValues(row: JSONObject, columns: Set<String>): ContentValues {
        val values = ContentValues()
        row.keys().forEach { key ->
            if (key !in columns) return@forEach
            when (val value = row.get(key)) {
                JSONObject.NULL -> values.putNull(key)
                is String -> values.put(key, value)
                is Boolean -> values.put(key, if (value) 1L else 0L)
                is Int, is Long -> values.put(key, (value as Number).toLong())
                is Number -> values.put(key, value.toDouble())
                else -> values.put(key, value.toString())
            }
        }
        return values
    }

    private fun currentDeviceSettings(db: SupportSQLiteDatabase): ContentValues? =
        db.query("SELECT * FROM `$SETTINGS_TABLE`").use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val values = ContentValues()
            SettingsEntity.DEVICE_LOCAL_COLUMNS
                .filter { cursor.getColumnIndex(it) >= 0 }
                .forEach { values.putCell(it, cellValue(cursor, cursor.getColumnIndex(it))) }
            values
        }

    private fun ContentValues.putCell(column: String, value: Any) = when (value) {
        JSONObject.NULL -> putNull(column)
        is Long -> put(column, value)
        is Double -> put(column, value)
        else -> put(column, value.toString())
    }

    private fun replaceMedia(staging: File) {
        val target = mediaStore.directory
        target.deleteRecursively()
        target.mkdirs()
        staging.listFiles().orEmpty().forEach { it.copyTo(File(target, it.name), overwrite = true) }
    }

    private fun summarize(tables: JSONObject, mediaFiles: Int) = BackupSummary(
        workouts = tables.optJSONArray("workout_sessions")?.length() ?: 0,
        routines = tables.optJSONArray("routines")?.length() ?: 0,
        exercises = tables.optJSONArray("exercises")?.length() ?: 0,
        measurements = tables.optJSONArray("body_measurements")?.length() ?: 0,
        mediaFiles = mediaFiles,
    )

    companion object {
        const val FORMAT = "gymora-backup"
        const val FORMAT_VERSION = 1
        const val MIME_TYPE = "application/zip"
        private const val JSON_ENTRY = "backup.json"
        private const val MEDIA_PREFIX = "media/"
        private const val STAGING_DIR = "restore_staging"
        private const val SETTINGS_TABLE = "settings"
        private const val DAMAGED = "The backup file is damaged."

        /** Parents before children: insert in this order, delete in reverse. */
        private val TABLES = listOf(
            SETTINGS_TABLE,
            "exercises",
            "routines",
            "routine_exercises",
            "set_templates",
            "workout_sessions",
            "workout_exercises",
            "workout_sets",
            "body_measurements",
        )
    }
}
