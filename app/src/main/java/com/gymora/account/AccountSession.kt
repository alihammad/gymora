package com.gymora.account

import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The account this process runs as. It is fixed for the life of the process: switching
 * records the new choice and restarts the app, so every singleton (the database, the
 * repositories, the workers' dependencies) is rebuilt for the new account and nothing
 * from the previous one can linger in memory.
 *
 * File moves and deletions requested before a restart are applied here, on the next
 * start, before the database opens.
 */
@Singleton
class AccountSession @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: AccountStore,
    private val restarter: ProcessRestarter,
) {

    val activeAccount: Account

    init {
        applyPendingOperations()
        activeAccount = store.find(ActiveAccount.id(context)) ?: Account.GUEST
    }

    fun accounts(): List<Account> = store.accounts()

    /**
     * Records a signed-in account and switches to it. [keepGuestData] moves what was
     * logged as a guest into the account, if the account has no data on this device yet.
     */
    fun signIn(account: Account, keepGuestData: Boolean) {
        store.upsert(account)
        if (keepGuestData) store.pendingGuestAdoption = account.id
        switchTo(account.id, force = keepGuestData)
    }

    /** Restarts the app as [accountId]; does nothing if it is already active. */
    fun switchTo(accountId: String) = switchTo(accountId, force = false)

    /**
     * Returns to the guest profile. With [removeLocalData] the account is forgotten and
     * its database and media are deleted from this device on the restart.
     */
    fun signOut(removeLocalData: Boolean) {
        val account = activeAccount.takeUnless { it.isGuest } ?: return
        if (removeLocalData) forget(account.id)
        switchTo(Account.GUEST_ID, force = true)
    }

    /** Forgets an account that is not active and deletes its data from this device. */
    fun removeFromDevice(accountId: String) {
        require(accountId != Account.GUEST_ID) { "The guest profile cannot be removed" }
        require(accountId != activeAccount.id) { "Sign out before removing the active account" }
        // Its database is not open in this process, so it can go right away.
        store.remove(accountId)
        AccountFiles.deleteAccountData(context, accountId)
    }

    private fun forget(accountId: String) {
        store.remove(accountId)
        store.pendingDeletions = store.pendingDeletions + accountId
    }

    private fun switchTo(accountId: String, force: Boolean) {
        if (accountId == activeAccount.id && !force) return
        store.setActive(accountId)
        restarter.restart()
    }

    private fun applyPendingOperations() {
        store.pendingDeletions.forEach { AccountFiles.deleteAccountData(context, it) }
        store.pendingDeletions = emptySet()
        store.pendingGuestAdoption?.let { accountId ->
            AccountFiles.adoptGuestData(context, accountId)
            store.pendingGuestAdoption = null
        }
    }
}

/**
 * The account id read once per process, for code that cannot be injected (Compose
 * helpers, static file paths). Matches [AccountSession.activeAccount].
 */
object ActiveAccount {

    @Volatile
    private var cached: String? = null

    fun id(context: Context): String =
        cached ?: synchronized(this) {
            cached ?: AccountStore(context).activeId().also { cached = it }
        }

    @VisibleForTesting
    fun reset() {
        cached = null
    }
}

/** Ends this process and relaunches the app, so it starts again as the newly active account. */
interface ProcessRestarter {
    fun restart()
}

class AppProcessRestarter @Inject constructor(
    @ApplicationContext private val context: Context,
) : ProcessRestarter {

    override fun restart() {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launch?.component?.let { context.startActivity(Intent.makeRestartActivityTask(it)) }
        // Committed transactions are already durable; this is the same as the system
        // reclaiming the app in the background.
        Runtime.getRuntime().exit(0)
    }
}
