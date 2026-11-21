package com.gymora.data.sync

import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.firestore.firestoreSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Firestore layout: `users/{uid}/{collection}/{syncId}`, each document holding
 * `updatedAt`, `deleted`, `serverUpdatedAt` and the row itself under `data`.
 * The security rules (firestore.rules) only let a user reach their own `users/{uid}`.
 */
@Singleton
class FirestoreSyncRemote @Inject constructor() : SyncRemote {

    override suspend fun pull(uid: String, collection: String, sinceServerTime: Long): List<RemoteDoc> {
        val docs = mutableListOf<RemoteDoc>()
        var since = sinceServerTime
        do {
            val page = collection(uid, collection)
                .whereGreaterThan(FIELD_SERVER_TIME, since.toTimestamp())
                .orderBy(FIELD_SERVER_TIME, Query.Direction.ASCENDING)
                .limit(PAGE_SIZE.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { it.toRemoteDoc() }
            docs += page
            since = page.lastOrNull()?.serverTime ?: since
        } while (page.size == PAGE_SIZE)
        return docs
    }

    override suspend fun push(uid: String, collection: String, docs: List<RemoteDoc>) {
        val target = collection(uid, collection)
        docs.chunked(BATCH_SIZE).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc ->
                batch.set(
                    target.document(doc.id),
                    mapOf(
                        FIELD_UPDATED_AT to doc.updatedAt,
                        FIELD_DELETED to doc.deleted,
                        FIELD_DATA to doc.data,
                        FIELD_SERVER_TIME to FieldValue.serverTimestamp(),
                    ),
                )
            }
            batch.commit().await()
        }
    }

    override suspend fun deleteAll(uid: String, collections: List<String>) {
        collections.forEach { name ->
            val target = collection(uid, name)
            do {
                val page = target.limit(BATCH_SIZE.toLong()).get().await().documents
                if (page.isEmpty()) break
                val batch = firestore.batch()
                page.forEach { batch.delete(it.reference) }
                batch.commit().await()
            } while (page.size == BATCH_SIZE)
        }
    }

    private fun collection(uid: String, name: String): CollectionReference =
        firestore.collection(USERS).document(uid).collection(name)

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toRemoteDoc(): RemoteDoc? {
        val time = getTimestamp(FIELD_SERVER_TIME) ?: return null
        return RemoteDoc(
            id = id,
            updatedAt = getLong(FIELD_UPDATED_AT) ?: 0L,
            deleted = getBoolean(FIELD_DELETED) ?: false,
            data = get(FIELD_DATA) as? Map<String, Any?> ?: emptyMap(),
            serverTime = time.seconds * MICROS_PER_SECOND + time.nanoseconds / NANOS_PER_MICRO,
        )
    }

    private fun Long.toTimestamp() =
        Timestamp(this / MICROS_PER_SECOND, ((this % MICROS_PER_SECOND) * NANOS_PER_MICRO).toInt())

    private companion object {
        const val USERS = "users"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_DELETED = "deleted"
        const val FIELD_DATA = "data"
        const val FIELD_SERVER_TIME = "serverUpdatedAt"
        const val PAGE_SIZE = 300

        /** Firestore allows 500 writes per batch. */
        const val BATCH_SIZE = 400
        const val MICROS_PER_SECOND = 1_000_000L
        const val NANOS_PER_MICRO = 1_000L

        /** Room is the local copy, so Firestore keeps only a small in-memory cache. */
        val firestore: FirebaseFirestore by lazy {
            FirebaseFirestore.getInstance().apply {
                firestoreSettings = firestoreSettings { setLocalCacheSettings(memoryCacheSettings {}) }
            }
        }
    }
}
