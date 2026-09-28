package com.onehouse.app.knx

import org.junit.Assert.*
import org.junit.Test

class KnxStartupReadPolicyTest {
    private fun targets(vararg addresses: String) = addresses.associateWith {
        KnxLoadTarget(it, setOf("Vivienda"), setOf(KnxLoadKind.SENSOR), setOf(it))
    }
    private fun state(address: String) = KnxStateRepository.State(
        address, false, "0", "3.15.2",
        KnxConnectionManager.IncomingGroupTelegram.Kind.RESPONSE, "GroupValueResponse", Long.MAX_VALUE
    )

    @Test fun generalCommandsNeverBecomeReadsAndValveFeedbackIsDeduplicated() {
        val input = KnxStartupReadPolicy.commandOnly.toList() + listOf(" 2/4/1 ", "2/4/1", "1/2/1", "5/3/2", "15/0/21")
        assertEquals(listOf("2/4/1", "1/2/1", "5/3/2"), KnxStartupReadPolicy.readable(input))
        assertTrue("15/0/21" in KnxStartupReadPolicy.tracked(input))
        assertFalse(KnxStartupReadPolicy.tracked(input).any { it in KnxStartupReadPolicy.commandOnly })
    }

    @Test fun missingValvePreventsOneHundredPercentEvenWhenRoundFinished() {
        val progress = KnxLoadProgressSnapshot(targets("1/2/1", "2/4/1", "15/0/21"),
            receivedAddresses = setOf("1/2/1"), noResponseAddresses = setOf("2/4/1"), finished = true)
        assertEquals(2, progress.total)
        assertEquals(50, progress.percent)
        assertEquals(setOf("15/0/21"), progress.waitingEventAddresses)
        assertEquals(100, progress.copy(receivedAddresses = setOf("1/2/1", "2/4/1")).percent)
    }

    @Test fun rainAloneCannotIncreaseReadingProgress() {
        val progress = KnxLoadProgressSnapshot(targets("2/4/1", "15/0/21"), receivedAddresses = setOf("15/0/21"))
        assertEquals(0, progress.percent)
        assertEquals(0, progress.received)
        assertTrue(progress.waitingEventAddresses.isEmpty())
        assertEquals(0, KnxLoadProgressSnapshot().percent)
    }

    @Test fun noResponseDoesNotInventDryRainAndLateFeedbackReconciles() {
        KnxLoadProgressRepository.begin(targets("2/4/1", "15/0/21"))
        KnxLoadProgressRepository.finish(listOf("2/4/1", "15/0/21"), emptyMap())
        val missing = KnxLoadProgressRepository.progress.value
        assertEquals(0, missing.percent)
        assertEquals(setOf("2/4/1"), missing.noResponseAddresses)
        assertEquals(setOf("15/0/21"), missing.waitingEventAddresses)
        KnxLoadProgressRepository.reconcileAcceptedState(state("2/4/1"))
        assertEquals(100, KnxLoadProgressRepository.progress.value.percent)
        assertTrue(KnxLoadProgressRepository.progress.value.noResponseAddresses.isEmpty())
        KnxLoadProgressRepository.reconcileAcceptedState(state("15/0/21"))
        assertTrue(KnxLoadProgressRepository.progress.value.waitingEventAddresses.isEmpty())
        assertEquals(100, KnxLoadProgressRepository.progress.value.percent)
    }

    @Test fun omittedFailuresStillRemainMissingAndUnrelatedTelegramDoesNotCount() {
        KnxLoadProgressRepository.begin(targets("2/4/1"))
        KnxLoadProgressRepository.finish(emptyList(), mapOf("1/1/50" to state("1/1/50")))
        assertEquals(setOf("2/4/1"), KnxLoadProgressRepository.progress.value.noResponseAddresses)
        assertEquals(0, KnxLoadProgressRepository.progress.value.percent)
        KnxLoadProgressRepository.reconcileAcceptedState(state("1/1/50"))
        assertEquals(0, KnxLoadProgressRepository.progress.value.percent)
    }

    @Test fun persistedOldStateCannotCompleteCurrentSessionAndOtherSensorsRemainRequired() {
        assertTrue("5/4/1" in KnxStartupReadPolicy.readable(listOf("5/4/1", "15/0/21")))
        assertFalse(KnxStartupReadPolicy.isEvent("5/4/2"))
        assertTrue(KnxStartupReadPolicy.isEvent(KnxAddressBook.Terrace.RAINING))
        KnxLoadProgressRepository.begin(targets("2/4/1", "15/0/21"))
        KnxLoadProgressRepository.finish(emptyList(), mapOf("2/4/1" to state("2/4/1").copy(timestampMillis = 0)))
        assertEquals(0, KnxLoadProgressRepository.progress.value.percent)
        KnxLoadProgressRepository.syncReceived(mapOf("2/4/1" to state("2/4/1")))
        assertEquals(100, KnxLoadProgressRepository.progress.value.percent)
        assertTrue(KnxLoadProgressRepository.progress.value.noResponseAddresses.isEmpty())
        assertEquals(setOf("15/0/21"), KnxLoadProgressRepository.progress.value.waitingEventAddresses)
    }
}
