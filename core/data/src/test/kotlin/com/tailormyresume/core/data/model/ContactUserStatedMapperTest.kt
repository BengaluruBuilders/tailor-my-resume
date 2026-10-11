package com.tailormyresume.core.data.model

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.database.model.PopulatedProfile
import com.tailormyresume.core.database.model.ProfileEntity
import com.tailormyresume.core.testing.data.sampleProfile
import org.junit.Test

class ContactUserStatedMapperTest {

    @Test
    fun contactUserStatedRoundTripsThroughEntities() {
        listOf(true, false).forEach { flag ->
            val profile = sampleProfile.copy(contactUserStated = flag)

            val restored = PopulatedProfile(profile.asEntity(), profile.asEntryEntities()).asExternalModel()

            assertThat(restored.contactUserStated).isEqualTo(flag)
        }
    }

    @Test
    fun aRowWrittenBeforeTheFlagReadsAsNotUserStated() {
        val legacy = ProfileEntity(fullName = "Priya", email = "", phone = "", headline = "", skills = emptyList())

        val restored = PopulatedProfile(legacy, emptyList()).asExternalModel()

        assertThat(restored.contactUserStated).isFalse()
    }
}
