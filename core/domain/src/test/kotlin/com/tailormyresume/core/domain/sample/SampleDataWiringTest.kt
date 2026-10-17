package com.tailormyresume.core.domain.sample

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AccountWallet
import com.tailormyresume.core.domain.di.DomainModule
import com.tailormyresume.core.domain.offline.OfflineAccountWallet
import org.junit.Test
import java.lang.reflect.Modifier
import javax.inject.Inject

class SampleDataWiringTest {

    private val constructor = OfflineSampleDataController::class.java.constructors.filterNot { it.isSynthetic }.single()

    @Test
    fun theControllerTakesTheOfflineAccountWalletAndNeverAFlavourBoundOne() {
        val parameterTypes = constructor.parameterTypes.toList()

        assertThat(parameterTypes).contains(OfflineAccountWallet::class.java)
        assertThat(parameterTypes).doesNotContain(AccountWallet::class.java)
    }

    @Test
    fun onlyTheDomainModuleCanBuildTheController() {
        assertThat(constructor.isAnnotationPresent(Inject::class.java)).isFalse()
    }

    @Test
    fun theDomainModuleDoesNotBindTheControllerToWhateverTheFlavourInjects() {
        val abstractSampleBindings = DomainModule::class.java.declaredMethods.filter {
            Modifier.isAbstract(it.modifiers) && it.returnType == SampleDataController::class.java
        }

        assertThat(abstractSampleBindings).isEmpty()
    }
}
