package vn.linkmap

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import vn.linkmap.core.SyncRules
import java.util.UUID

class AccountSwitchTest {
    @Test fun loadedAccountSwitchDoesNotHideAndCancelTheCallingUi()=runBlocking {
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as LinkMapApplication
        app.ready.first {it}
        val previous=app.sync.state.value.profile?.subject
        val subject="account-switch-test-${UUID.randomUUID()}"
        val name="linkmap-${SyncRules.id(subject).takeLast(32)}.db"
        val events=mutableListOf<Boolean>()
        val observer=launch(Dispatchers.Unconfined) {app.ready.collect {events+=it}}
        try {
            withContext(Dispatchers.Main) {app.openAccount(subject)}
            assertTrue("Ready must stay true so the calling Compose scope survives",events.all {it})
            assertTrue(app.ready.value)
        } finally {
            observer.cancelAndJoin()
            val fixture=app.store
            withContext(Dispatchers.Main) {app.openAccount(previous)}
            fixture.close();app.deleteDatabase(name)
        }
    }
}
