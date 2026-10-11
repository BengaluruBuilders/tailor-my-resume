package com.tailormyresume.feature.profile.impl.guidedform

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContactFieldValidatorTest {

    @Test
    fun contactFieldValidator_acceptsOnlyWellFormedEmailsAndPhones() {
        assertThat(ContactFieldValidator.isValidEmail("a.b@c.in")).isTrue()
        assertThat(ContactFieldValidator.isValidEmail("o'brien@example.com")).isTrue()
        assertThat(ContactFieldValidator.isValidEmail("a@b")).isFalse()
        assertThat(ContactFieldValidator.isValidEmail("x y@z.com")).isFalse()

        assertThat(ContactFieldValidator.isValidPhone("9876543210")).isTrue()
        assertThat(ContactFieldValidator.isValidPhone("(080) 2345-6789")).isTrue()
        assertThat(ContactFieldValidator.isValidPhone("+44 20 7946 0958")).isTrue()
        assertThat(ContactFieldValidator.isValidPhone("415.555.2671")).isTrue()
        assertThat(ContactFieldValidator.isValidPhone("030/1234567")).isTrue()
        assertThat(ContactFieldValidator.isValidPhone("abc")).isFalse()
        assertThat(ContactFieldValidator.isValidPhone("12345")).isFalse()
        assertThat(ContactFieldValidator.isValidPhone("+1234567890123456")).isFalse()
    }
}
