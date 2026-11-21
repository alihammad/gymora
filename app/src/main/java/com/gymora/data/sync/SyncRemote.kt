package com.gymora.data.sync

/**
 * One synced document: a workout, routine, custom exercise, measurement or the settings.
 * [updatedAt] is the device-clock time of the last edit and decides conflicts (latest wins);
 * [serverTime] is when the cloud stored it, used only to fetch what changed since last time.
 */
data class RemoteDoc(
    val id: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val data: Map<String, Any?> = emptyMap(),
    /** Server timestamp in microseconds; set by the cloud, ignored when pushing. */
    val serverTime: Long = 0,
)

/** The cloud copy of one account's data. Every call is scoped to that account's [uid]. */
interface SyncRemote {

    /** Documents stored after [sinceServerTime], oldest first. */
    suspend fun pull(uid: String, collection: String, sinceServerTime: Long): List<RemoteDoc>

    suspend fun push(uid: String, collection: String, docs: List<RemoteDoc>)

    /** Deletes everything stored for [uid], for account deletion. */
    suspend fun deleteAll(uid: String, collections: List<String>)
}
