package io.github.beppe2880.opencodemobile.feature.connect

import io.github.beppe2880.opencodemobile.core.domain.ConnectTarget
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ServerDiscovery
import io.github.beppe2880.opencodemobile.core.domain.ServerInfo
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import io.github.beppe2880.opencodemobile.core.ui.text.UiText
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConnectViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = FakeServerRepository()
    private val discovery = FakeDiscovery()

    @Test
    fun `servers found on the network are listed except saved ones`() = runTest(main.testDispatcher) {
        discovery.found = listOf("http://192.168.1.42:4096", "http://localhost:4096", "http://192.168.1.42:4096")
        val vm = ConnectViewModel(repo, discovery)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(false, state.discovering)
        assertEquals(listOf("http://192.168.1.42:4096", "http://localhost:4096"), state.discovered)
        assertEquals(listOf("http://192.168.1.42:4096"), state.newDiscovered)
    }

    @Test
    fun `discovery failures are ignored`() = runTest(main.testDispatcher) {
        discovery.error = SecurityException("no permission")
        val vm = ConnectViewModel(repo, discovery)
        advanceUntilIdle()

        assertEquals(false, vm.state.value.discovering)
        assertTrue(vm.state.value.discovered.isEmpty())
    }

    @Test
    fun `choosing a found server fills the URL and asks for the pairing code`() = runTest(main.testDispatcher) {
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.MethodChanged(AuthMethod.Password))
        vm.onEvent(ConnectEvent.UseDiscovered("http://192.168.1.42:4096"))
        advanceUntilIdle()

        assertEquals("http://192.168.1.42:4096", vm.state.value.url)
        assertEquals(AuthMethod.PairingCode, vm.state.value.method)
        assertEquals(ConnectEffect.FocusSecret, vm.effect.first())
    }

    @Test
    fun `scanned pairing link fills fields and pairs`() = runTest(main.testDispatcher) {
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.OpenScanner)
        vm.onEvent(ConnectEvent.QrScanned("http://192.168.1.5:4096/auth/connect/ABC123"))
        advanceUntilIdle()

        assertEquals("http://192.168.1.5:4096" to "ABC123", repo.paired.single())
        assertEquals(false, vm.state.value.scannerOpen)
        assertEquals(ConnectEffect.Connected, vm.effect.first())
    }

    @Test
    fun `QR code with several addresses pairs once through the first reachable one`() = runTest(main.testDispatcher) {
        repo.probeByUrl = mapOf(
            "http://172.17.0.1:4096" to null, // never answers
            "http://192.168.1.5:4096" to Result.failure(OpenCodeError.Unauthorized()),
            "http://10.0.0.9:4096" to Result.success(ServerInfo("2.0.27", emptyList())),
        )
        repo.qrTarget = ConnectTarget.Pairing(
            "http://172.17.0.1:4096",
            "ABC123",
            listOf("http://192.168.1.5:4096", "http://10.0.0.9:4096"),
        )
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.QrScanned("qr"))
        advanceUntilIdle()

        assertEquals("http://192.168.1.5:4096" to "ABC123", repo.paired.single())
        assertEquals(ConnectEffect.Connected, vm.effect.first())
    }

    @Test
    fun `QR code with no reachable address reports it without spending the code`() = runTest(main.testDispatcher) {
        repo.probeByUrl = mapOf(
            "http://172.17.0.1:4096" to null,
            "http://192.168.1.5:4096" to Result.failure(OpenCodeError.Unreachable("refused")),
        )
        repo.qrTarget = ConnectTarget.Pairing("http://172.17.0.1:4096", "ABC123", listOf("http://192.168.1.5:4096"))
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.QrScanned("qr"))
        advanceUntilIdle()

        assertTrue(repo.paired.isEmpty())
        assertEquals(UiText.Res(R.string.connect_error_no_reachable_url), vm.state.value.submitError)
        assertEquals(false, vm.state.value.submitting)
    }

    @Test
    fun `invalid pasted text shows a message and keeps the form`() = runTest(main.testDispatcher) {
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.LinkPasted("not a url at all"))
        advanceUntilIdle()

        assertTrue(vm.effect.first() is ConnectEffect.ShowMessage)
        assertTrue(repo.paired.isEmpty())
    }

    @Test
    fun `submit validates required fields`() = runTest(main.testDispatcher) {
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.Res(R.string.connect_error_url_required), vm.state.value.urlError)
        assertEquals(UiText.Res(R.string.connect_error_code_required), vm.state.value.secretError)
    }

    @Test
    fun `unauthorized password is reported on the secret field`() = runTest(main.testDispatcher) {
        repo.addResult = Result.failure(OpenCodeError.Unauthorized())
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.UrlChanged("localhost:4096"))
        vm.onEvent(ConnectEvent.MethodChanged(AuthMethod.Password))
        vm.onEvent(ConnectEvent.SecretChanged("wrong"))
        vm.onEvent(ConnectEvent.Submit)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("http://localhost:4096", state.url)
        assertNotNull(state.secretError)
        assertNull(state.submitError)
        assertEquals(false, state.submitting)
    }

    @Test
    fun `probe without password treats 401 as reachable pairing server`() = runTest(main.testDispatcher) {
        repo.probeResult = Result.failure(OpenCodeError.Unauthorized())
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.UrlChanged("http://localhost:4096"))
        vm.onEvent(ConnectEvent.TestConnection)
        advanceUntilIdle()

        assertEquals(ProbeResult.NeedsPairing, vm.state.value.probe)
    }

    @Test
    fun `probe reports server version`() = runTest(main.testDispatcher) {
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.UrlChanged("http://localhost:4096"))
        vm.onEvent(ConnectEvent.MethodChanged(AuthMethod.Password))
        vm.onEvent(ConnectEvent.SecretChanged("pw"))
        vm.onEvent(ConnectEvent.TestConnection)
        advanceUntilIdle()

        assertEquals(ProbeResult.Reachable("2.0.27"), vm.state.value.probe)
    }

    @Test
    fun `selecting a saved server activates it`() = runTest(main.testDispatcher) {
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.SelectServer("s1"))
        advanceUntilIdle()

        assertEquals("s1", repo.activated.single())
        assertEquals(ConnectEffect.Connected, vm.effect.first())
    }

    @Test
    fun `a password keeps its leading and trailing spaces`() = runTest(main.testDispatcher) {
        val vm = ConnectViewModel(repo, discovery)
        advanceUntilIdle()

        vm.onEvent(ConnectEvent.MethodChanged(AuthMethod.Password))
        vm.onEvent(ConnectEvent.SecretChanged("  s3cret "))

        assertEquals("  s3cret ", vm.state.value.secret)
    }

    @Test
    fun `a pasted password loses only its line breaks`() = runTest(main.testDispatcher) {
        val vm = ConnectViewModel(repo, discovery)
        advanceUntilIdle()

        vm.onEvent(ConnectEvent.MethodChanged(AuthMethod.Password))
        vm.onEvent(ConnectEvent.SecretChanged("  s3cret \r\n"))

        assertEquals("  s3cret ", vm.state.value.secret)
    }

    @Test
    fun `password is submitted exactly as typed`() = runTest(main.testDispatcher) {
        val password = "  correct secret\t"
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.UrlChanged("localhost:4096"))
        vm.onEvent(ConnectEvent.MethodChanged(AuthMethod.Password))
        vm.onEvent(ConnectEvent.SecretChanged(password))
        assertEquals(password, vm.state.value.secret)
        vm.onEvent(ConnectEvent.Submit)
        advanceUntilIdle()

        assertEquals(password, repo.addedPasswords.single())
        assertEquals(ConnectEffect.Connected, vm.effect.first())
    }

    @Test
    fun `connection probe preserves password whitespace`() = runTest(main.testDispatcher) {
        val password = "  "
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.UrlChanged("localhost:4096"))
        vm.onEvent(ConnectEvent.MethodChanged(AuthMethod.Password))
        vm.onEvent(ConnectEvent.SecretChanged(password))
        vm.onEvent(ConnectEvent.TestConnection)
        advanceUntilIdle()

        assertEquals(password, repo.probedPasswords.single())
        assertEquals(ProbeResult.Reachable("2.0.27"), vm.state.value.probe)
    }

    @Test
    fun `pairing code is still trimmed`() = runTest(main.testDispatcher) {
        val vm = ConnectViewModel(repo, discovery)
        vm.onEvent(ConnectEvent.UrlChanged("localhost:4096"))
        vm.onEvent(ConnectEvent.SecretChanged("  ABC123\n"))
        vm.onEvent(ConnectEvent.Submit)
        advanceUntilIdle()

        assertEquals("http://localhost:4096" to "ABC123", repo.paired.single())
    }
}

private class FakeDiscovery : ServerDiscovery {
    var found: List<String> = emptyList()
    var error: Throwable? = null

    override fun discover(): Flow<String> = flow {
        error?.let { throw it }
        found.forEach { emit(it) }
    }
}

private class FakeServerRepository : ServerRepository {
    private val profile = ServerProfile("s1", "Desk", "http://localhost:4096", secret = "x", credential = CredentialKind.PairingToken, addedAt = 0)
    override val servers: StateFlow<List<ServerProfile>> = MutableStateFlow(listOf(profile))
    override val activeServer: StateFlow<ServerProfile?> = MutableStateFlow(null)
    var probeResult: Result<ServerInfo> = Result.success(ServerInfo("2.0.27", emptyList()))
    var addResult: Result<ServerProfile> = Result.success(profile)
    val paired = mutableListOf<Pair<String, String>>()
    val activated = mutableListOf<String>()
    val addedPasswords = mutableListOf<String>()
    val probedPasswords = mutableListOf<String?>()

    /** Per-address probe results; a null value never answers (suspends past the timeout). */
    var probeByUrl: Map<String, Result<ServerInfo>?>? = null
    var qrTarget: ConnectTarget? = null

    override suspend fun probe(baseUrl: String, password: String?): Result<ServerInfo> {
        probedPasswords += password
        val byUrl = probeByUrl ?: return probeResult
        return byUrl[baseUrl] ?: awaitCancellation()
    }
    override suspend fun pair(baseUrl: String, code: String, name: String?): Result<ServerProfile> {
        paired += baseUrl to code
        return Result.success(profile)
    }
    override suspend fun addWithPassword(baseUrl: String, password: String, name: String?): Result<ServerProfile> {
        addedPasswords += password
        return addResult
    }
    override suspend fun activate(serverId: String) {
        activated += serverId
    }
    override suspend fun rename(serverId: String, name: String) = Unit
    override suspend fun remove(serverId: String) = Unit

    /** The real parser is tested in core:network; this covers pairing links and bare hosts. */
    override fun parseConnectTarget(raw: String): ConnectTarget? {
        val text = raw.trim()
        if (text == "qr") return qrTarget
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null
        Regex("(https?://[^/]+)/auth/connect/(\\w+)").matchEntire(text)?.let {
            return ConnectTarget.Pairing(it.groupValues[1], it.groupValues[2])
        }
        return ConnectTarget.Url(if ("://" in text) text.trimEnd('/') else "http://${text.trimEnd('/')}")
    }
}
