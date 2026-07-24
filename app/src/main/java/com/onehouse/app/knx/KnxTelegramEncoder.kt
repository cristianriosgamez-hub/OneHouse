package com.onehouse.app.knx

/** Codificación cEMI de los telegramas de grupo soportados por OneHouse. */
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
            0x00,
            CONTROL_1_STANDARD_FRAME.toByte(),
            CONTROL_2_GROUP_HOP_COUNT_6.toByte(),
            0x00, 0x00,
            destination[0], destination[1],
            0x01,
            0x00,
            apduSecondByte.toByte()
        )
    }
}

/** Formato hexadecimal estable para diagnóstico y comparación con ETS/Wireshark. */
internal object KnxHex {
    fun format(bytes: ByteArray): String = bytes.joinToString(" ") { byte ->
        "%02X".format(byte.toInt() and 0xFF)
    }
}
