package com.gymora.account

import android.content.Context
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-level record of the accounts known on this phone and which one is active.
 * Lives in its own preferences file, outside every account's database, and is not part
 * of Android Auto Backup (backup_rules.xml only includes the database and media).
 */
@Singleton
class AccountStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Guest first, then signed-in accounts in the order they were added. */
    fun accounts(): List<Account> = listOf(Account.GUEST) + readAccounts()

    fun find(id: String): Account? = accounts().firstOrNull { it.id == id }

    fun activeId(): String = prefs.getString(KEY_ACTIVE, null)
        ?.takeIf { it == Account.GUEST_ID || readAccounts().any { account -> account.id == it } }
        ?: Account.GUEST_ID

    /** Adds the account, or refreshes its name and email if it is already known. */
    fun upsert(account: Account) {
        require(!account.isGuest) { "The guest profile is built in" }
        val current = readAccounts()
        val index = current.indexOfFirst { it.id == account.id }
        writeAccounts(if (index >= 0) current.toMutableList().apply { set(index, account) } else current + account)
    }

    fun remove(id: String) {
        writeAccounts(readAccounts().filterNot { it.id == id })
    }

    /**
     * Makes [id] the active account from the next process start. Written synchronously
     * because the caller restarts the process right after.
     */
    fun setActive(id: String) {
        require(find(id) != null) { "Unknown account" }
        prefs.edit().putString(KEY_ACTIVE, id).commit()
    }

    /** The account that takes over the guest's data on the next start, if any. */
    var pendingGuestAdoption: String?
        get() = prefs.getString(KEY_PENDING_ADOPTION, null)
        set(value) {
            prefs.edit().putString(KEY_PENDING_ADOPTION, value).commit()
        }

    /** Accounts whose local data is deleted on the next start, once no database is open. */
    var pendingDeletions: Set<String>
        get() = prefs.getStringSet(KEY_PENDING_DELETIONS, null).orEmpty().toSet()
        set(value) {
            prefs.edit().putStringSet(KEY_PENDING_DELETIONS, value).commit()
        }

    private fun readAccounts(): List<Account> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val json = array.optJSONObject(i) ?: return@mapNotNull null
            val id = json.optString(FIELD_ID).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Account(
                id = id,
                displayName = json.optString(FIELD_NAME),
                email = json.optString(FIELD_EMAIL).takeIf { json.has(FIELD_EMAIL) && it.isNotBlank() },
            )
        }
    }

    private fun writeAccounts(accounts: List<Account>) {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject()
                    .put(FIELD_ID, account.id)
                    .put(FIELD_NAME, account.displayName)
                    .apply { account.email?.let { put(FIELD_EMAIL, it) } },
            )
        }
        prefs.edit().putString(KEY_ACCOUNTS, array.toString()).commit()
    }

    @VisibleForTesting
    fun clear() {
        prefs.edit().clear().commit()
    }

    private companion object {
        const val PREFS = "gymora_accounts"
        const val KEY_ACCOUNTS = "accounts"
        const val KEY_ACTIVE = "active"
        const val KEY_PENDING_ADOPTION = "pending_guest_adoption"
        const val KEY_PENDING_DELETIONS = "pending_deletions"
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
    }
}
