package com.tailormyresume.core.domain.offline

import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.account.AccountWipeFinisher
import com.tailormyresume.core.domain.account.ServerAccountDeleter
import javax.inject.Inject

class OfflineServerAccountDeleter @Inject constructor() : ServerAccountDeleter {
    override suspend fun delete(): Result<Unit> = Result.success(Unit)
}

class OfflineAccountWipeFinisher @Inject constructor() : AccountWipeFinisher {
    override suspend fun finish() = Unit
}

class OfflineFirebaseUidProvider @Inject constructor() : FirebaseUidProvider {
    override fun uid(): String? = null
}
