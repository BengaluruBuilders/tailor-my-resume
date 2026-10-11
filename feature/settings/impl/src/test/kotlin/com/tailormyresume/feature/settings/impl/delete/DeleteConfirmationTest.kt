package com.tailormyresume.feature.settings.impl.delete

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeleteConfirmationTest {

    @Test
    fun acceptsDeleteTrimmedAndInAnyCase() {
        listOf("DELETE", "delete", "Delete", " DELETE ", "\tdelete\n").forEach { text ->
            assertThat(matchesDeleteConfirmation(text)).isTrue()
        }
    }

    @Test
    fun refusesAnythingElse() {
        listOf("", " ", "DELET", "DELETE!", "del ete", "DELETEDELETE", "DELETE ME").forEach { text ->
            assertThat(matchesDeleteConfirmation(text)).isFalse()
        }
    }
}
