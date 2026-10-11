package com.tailormyresume.app.di

import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.account.AccountWipeFinisher
import com.tailormyresume.core.domain.account.ServerAccountDeleter
import com.tailormyresume.core.domain.offline.OfflineAccountWipeFinisher
import com.tailormyresume.core.domain.offline.OfflineFirebaseUidProvider
import com.tailormyresume.core.domain.offline.OfflineServerAccountDeleter
import com.tailormyresume.core.domain.offline.OfflineSignInGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SignInBindings {
    @Binds
    fun bindSignInGateway(impl: OfflineSignInGateway): SignInGateway

    @Binds
    fun bindServerAccountDeleter(impl: OfflineServerAccountDeleter): ServerAccountDeleter

    @Binds
    fun bindAccountWipeFinisher(impl: OfflineAccountWipeFinisher): AccountWipeFinisher

    @Binds
    fun bindFirebaseUidProvider(impl: OfflineFirebaseUidProvider): FirebaseUidProvider
}
