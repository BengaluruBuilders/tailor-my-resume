package com.tailormyresume.app.di

import com.tailormyresume.app.AppStartTask
import com.tailormyresume.app.BuildConfig
import com.tailormyresume.app.auth.CredentialManagerGoogleSource
import com.tailormyresume.app.auth.FirebaseAuthClient
import com.tailormyresume.app.auth.FirebaseConfig
import com.tailormyresume.app.auth.FirebaseSessionClient
import com.tailormyresume.app.auth.ForegroundActivity
import com.tailormyresume.app.auth.ForegroundActivityTracker
import com.tailormyresume.app.auth.GoogleCredentialSource
import com.tailormyresume.app.auth.LocalDataWiper
import com.tailormyresume.app.auth.PendingWipeStartTask
import com.tailormyresume.app.auth.RemoteAccountWipeFinisher
import com.tailormyresume.app.auth.RemoteServerAccountDeleter
import com.tailormyresume.app.auth.RemoteSignInGateway
import com.tailormyresume.app.auth.RoomLocalDataWiper
import com.tailormyresume.app.auth.SessionExpiryHandler
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.account.AccountWipeFinisher
import com.tailormyresume.core.domain.account.ServerAccountDeleter
import com.tailormyresume.core.network.IdTokenProvider
import com.tailormyresume.core.network.SessionExpiredListener
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface SignInBindings {
    @Binds
    fun bindSignInGateway(impl: RemoteSignInGateway): SignInGateway

    @Binds
    fun bindLocalDataWiper(impl: RoomLocalDataWiper): LocalDataWiper

    @Binds
    fun bindGoogleCredentialSource(impl: CredentialManagerGoogleSource): GoogleCredentialSource

    @Binds
    fun bindFirebaseSessionClient(impl: FirebaseAuthClient): FirebaseSessionClient

    @Binds
    fun bindIdTokenProvider(impl: FirebaseAuthClient): IdTokenProvider

    @Binds
    fun bindFirebaseUidProvider(impl: FirebaseAuthClient): FirebaseUidProvider

    @Binds
    fun bindSessionExpiredListener(impl: SessionExpiryHandler): SessionExpiredListener

    @Binds
    @IntoSet
    fun bindSessionExpiryStartTask(impl: SessionExpiryHandler): AppStartTask

    @Binds
    fun bindServerAccountDeleter(impl: RemoteServerAccountDeleter): ServerAccountDeleter

    @Binds
    fun bindAccountWipeFinisher(impl: RemoteAccountWipeFinisher): AccountWipeFinisher

    @Binds
    @IntoSet
    fun bindPendingWipeStartTask(impl: PendingWipeStartTask): AppStartTask

    companion object {
        @Provides
        fun foregroundActivity(): ForegroundActivity = ForegroundActivityTracker

        @Provides
        fun firebaseConfig(): FirebaseConfig = FirebaseConfig(
            apiKey = BuildConfig.TAILORMYRESUME_FIREBASE_API_KEY,
            appId = BuildConfig.TAILORMYRESUME_FIREBASE_APP_ID,
            projectId = BuildConfig.TAILORMYRESUME_FIREBASE_PROJECT_ID,
            webClientId = BuildConfig.TAILORMYRESUME_WEB_CLIENT_ID,
        )
    }
}
