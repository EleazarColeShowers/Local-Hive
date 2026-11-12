package com.eleazar.localhive.ui.util

import com.eleazar.localhive.data.remote.ApiException
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessagesTest {

    @Test
    fun connectTimeoutBecomesOfflineMessage() {
        val e = Exception("Connect timeout has expired [url=https://api.example.com/notify, connect_timeout=15000 ms]")
        assertEquals(OfflineMessage, e.friendlyMessage("Couldn't send"))
    }

    @Test
    fun networkFailureInCauseIsFound() {
        val e = RuntimeException("Request failed", Exception("Unable to resolve host \"firestore.googleapis.com\""))
        assertEquals(OfflineMessage, e.friendlyMessage())
    }

    @Test
    fun ownMessagesPassThrough() {
        assertEquals("That invite code isn't valid", Exception("That invite code isn't valid").friendlyMessage())
    }

    @Test
    fun technicalTextFallsBack() {
        assertEquals("Couldn't load", Exception("java.lang.IllegalStateException: User is null").friendlyMessage("Couldn't load"))
        assertEquals(GenericErrorMessage, Exception().friendlyMessage())
    }

    @Test
    fun workerRefusalsShowButServerErrorsDont() {
        assertEquals("You're not an admin here", ApiException(403, "You're not an admin here").friendlyMessage())
        assertEquals("Couldn't save", ApiException(500, "/notify failed (500)").friendlyMessage("Couldn't save"))
    }
}
