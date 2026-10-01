package com.onehouse.app.knx

/** Installation 3.15: group actions have no aggregate feedback object. */
internal object KnxStartupReadPolicy {
    const val VALVE_STATE = "2/4/1"
    val commandOnly = setOf("1/1/50", "2/1/50", "2/1/53", "5/5/3", "5/5/13", "5/5/14", "2/3/1")
    fun isEvent(address: String): Boolean =
        address == KnxAddressBook.Terrace.RAINING || address == "15/0/21"

    fun tracked(addresses: List<String>): List<String> = addresses
        .map(String::trim).filter(String::isNotEmpty).filterNot { it in commandOnly }.distinct()

    fun readable(addresses: List<String>): List<String> = tracked(addresses).filterNot(::isEvent)
}
