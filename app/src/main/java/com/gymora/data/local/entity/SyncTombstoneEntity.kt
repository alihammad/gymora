package com.gymora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Record of a deleted synced row (v7), so a later cloud sync can delete it remotely.
 * Written by SyncTriggers; removed again if a row with the same sync id comes back.
 * Device-local: not part of backups.
 */
@Entity(tableName = "sync_tombstones", primaryKeys = ["entity", "sync_id"])
data class SyncTombstoneEntity(
    /** Table name of the deleted row. */
    @ColumnInfo(name = "entity") val entity: String,
    @ColumnInfo(name = "sync_id") val syncId: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long,
)
