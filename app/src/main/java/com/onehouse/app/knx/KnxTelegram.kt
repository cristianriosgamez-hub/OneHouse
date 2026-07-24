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
