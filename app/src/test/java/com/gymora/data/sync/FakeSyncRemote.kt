package com.gymora.data.sync

/** In-memory cloud: what Firestore stores per user and collection, with an increasing server clock. */
class FakeSyncRemote : SyncRemote {

    private val store = mutableMapOf<String, MutableMap<String, MutableMap<String, RemoteDoc>>>()
    private var clock = 0L

    var pushes = 0
        private set
    var deletedAllFor: String? = null
        private set
    var failNext: Exception? = null

    fun docs(uid: String, collection: String): Map<String, RemoteDoc> =
        store[uid]?.get(collection).orEmpty()

    override suspend fun pull(uid: String, collection: String, sinceServerTime: Long): List<RemoteDoc> {
        failNext?.let { failNext = null; throw it }
        return docs(uid, collection).values.filter { it.serverTime > sinceServerTime }.sortedBy { it.serverTime }
    }

    override suspend fun push(uid: String, collection: String, docs: List<RemoteDoc>) {
        failNext?.let { failNext = null; throw it }
        pushes++
        val target = store.getOrPut(uid) { mutableMapOf() }.getOrPut(collection) { mutableMapOf() }
        docs.forEach { target[it.id] = it.copy(serverTime = ++clock) }
    }

    override suspend fun deleteAll(uid: String, collections: List<String>) {
        failNext?.let { failNext = null; throw it }
        store.remove(uid)
        deletedAllFor = uid
    }
}
