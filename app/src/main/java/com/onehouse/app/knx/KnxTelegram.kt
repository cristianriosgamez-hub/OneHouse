package com.onehouse.app.knx

/** Telegramas de grupo soportados inicialmente por OneHouse. */
sealed interface KnxTelegram {
    val destination: KnxGroupAddress

    data class GroupValueRead(
        override val destination: KnxGroupAddress
    ) : KnxTelegram

    data class GroupValueWriteBoolean(
        override val destination: KnxGroupAddress,
        val value: Boolean
    ) : KnxTelegram
}

internal object KnxTelegramEncoder {
    private const val CEMI_L_DATA_REQ = 0x11
    private const val CONTROL_1_STANDARD_FRAME = 0xBC
    private const val CONTROL_2_GROUP_HOP_COUNT_6 = 0xE0

    fun encode(telegram: KnxTelegram): ByteArray {
        val destination = telegram.destination.toBytes()
        val apduSecondByte = when (telegram) {
            is KnxTelegram.GroupValueRead -> 0x00
            is KnxTelegram.GroupValueWriteBoolean -> 0x80 or if (telegram.value) 0x01 else 0x00
        }

        return byteArrayOf(
            CEMI_L_DATA_REQ.toByte(),
            0x00, // longitud de información adicional
            CONTROL_1_STANDARD_FRAME.toByte(),
            CONTROL_2_GROUP_HOP_COUNT_6.toByte(),
            0x00, 0x00, // dirección individual de origen: la asigna el servidor de túnel
            destination[0], destination[1],
            0x01, // longitud NPDU: APDU de dos bytes menos uno
            0x00,
            apduSecondByte.toByte()
        )
    }
}
