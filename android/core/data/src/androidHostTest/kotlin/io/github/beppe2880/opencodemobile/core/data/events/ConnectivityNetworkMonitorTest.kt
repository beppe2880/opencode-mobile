package io.github.beppe2880.opencodemobile.core.data.events

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork

/**
 * The real monitor against Robolectric's ConnectivityManager. Android hands every callback a
 * new [Network] object for the same network, which these tests reproduce with
 * [ShadowNetwork.newInstance]: comparing by identity would never notice the loss.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConnectivityNetworkMonitorTest {

    private lateinit var connectivity: ConnectivityManager
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val reports = Channel<Boolean>(Channel.UNLIMITED)

    @Before
    fun setUp() {
        connectivity = RuntimeEnvironment.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @After
    fun tearDown() = scope.cancel()

    private fun collect() {
        scope.launch { ConnectivityNetworkMonitor(connectivity).connected.collect { reports.send(it) } }
    }

    private fun callback() = shadowOf(connectivity).networkCallbacks.single()

    private fun copyOf(network: Network): Network = ShadowNetwork.newInstance(shadowOf(network).netId)

    private fun next(): Boolean = runBlocking { withTimeout(1_000) { reports.receive() } }

    private fun nothingMore() = assertNull(runBlocking { withTimeoutOrNull(100) { reports.receive() } })

    @Test
    fun `losing the default network reported as another object is offline`() {
        val active = connectivity.activeNetwork!!
        collect()
        assertTrue(next())

        val lost = copyOf(active)
        assertNotSame(active, lost)
        callback().onLost(lost)

        assertEquals(false, next())
    }

    @Test
    fun `the default network announced again at registration is not a change`() {
        val active = connectivity.activeNetwork!!
        collect()
        assertTrue(next())

        callback().onAvailable(copyOf(active))

        nothingMore()
    }

    @Test
    fun `a new default network is reported, the late loss of the old one is ignored`() {
        val wifi = connectivity.activeNetwork!!
        collect()
        next()
        val cellular = ShadowNetwork.newInstance(shadowOf(wifi).netId + 1)

        callback().onAvailable(cellular)
        callback().onLost(copyOf(wifi))
        assertEquals(true, next())
        nothingMore()

        callback().onLost(copyOf(cellular))
        assertEquals(false, next())
        callback().onAvailable(copyOf(cellular))
        assertEquals(true, next())
    }

    @Test
    fun `a blocked default network counts as offline`() {
        val active = connectivity.activeNetwork!!
        collect()
        next()

        callback().onBlockedStatusChanged(copyOf(active), true)
        assertEquals(false, next())
        callback().onBlockedStatusChanged(copyOf(active), true)
        nothingMore()
        callback().onBlockedStatusChanged(copyOf(active), false)
        assertEquals(true, next())
    }

    @Test
    fun `the callback is unregistered when collection stops`() {
        collect()
        next()
        assertEquals(1, shadowOf(connectivity).networkCallbacks.size)

        scope.cancel()

        assertTrue(shadowOf(connectivity).networkCallbacks.isEmpty())
    }
}
