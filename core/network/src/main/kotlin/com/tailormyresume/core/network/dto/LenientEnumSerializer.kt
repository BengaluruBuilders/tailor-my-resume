package com.tailormyresume.core.network.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal abstract class LenientEnumSerializer<E : Enum<E>>(
    private val values: Array<E>,
    private val fallback: E,
    name: String,
) : KSerializer<E> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): E {
        val name = decoder.decodeString()
        return values.firstOrNull { it.name == name } ?: fallback
    }

    override fun serialize(encoder: Encoder, value: E) = encoder.encodeString(value.name)
}
