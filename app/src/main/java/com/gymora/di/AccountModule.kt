package com.gymora.di

import com.gymora.account.AppProcessRestarter
import com.gymora.account.AuthService
import com.gymora.account.FirebaseAuthService
import com.gymora.account.ProcessRestarter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    abstract fun bindProcessRestarter(restarter: AppProcessRestarter): ProcessRestarter

    @Binds
    abstract fun bindAuthService(service: FirebaseAuthService): AuthService
}
