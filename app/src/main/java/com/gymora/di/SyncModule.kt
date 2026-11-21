package com.gymora.di

import com.gymora.data.sync.FirestoreSyncRemote
import com.gymora.data.sync.SyncRemote
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    abstract fun bindSyncRemote(remote: FirestoreSyncRemote): SyncRemote
}
