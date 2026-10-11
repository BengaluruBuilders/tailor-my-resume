package com.tailormyresume.app.di

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.ai.RemoteJobImporter
import com.tailormyresume.app.ai.RemoteResumeTextParser
import org.junit.Test

class AiBindingsJobImporterTest {
    @Test
    fun prodBindsRemoteAndJobImporter() {
        val parameterTypes = AiBindings::class.java.methods.associate { it.name to it.parameterTypes.single() }

        assertThat(parameterTypes["bindJobImporter"]).isEqualTo(RemoteJobImporter::class.java)
        assertThat(parameterTypes["bindResumeTextParser"]).isEqualTo(RemoteResumeTextParser::class.java)
    }
}
