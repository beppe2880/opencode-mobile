package io.github.beppe2880.opencodemobile.feature.connect

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.ConnectTarget
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ServerDiscovery
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.core.ui.text.UiText
import io.github.beppe2880.opencodemobile.core.ui.text.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val PROBE_TIMEOUT_MS = 4_000L

class ConnectViewModel(
    private val servers: ServerRepository,
    private val discovery: ServerDiscovery,
) : BaseViewModel<ConnectState, ConnectEvent, ConnectEffect>(ConnectState()) {

    private var probeJob: Job? = null
    private var discoveryJob: Job? = null

    init {
        viewModelScope.launch {
            combine(servers.servers, servers.activeServer) { list, active -> list to active?.id }
                .collect { (list, activeId) -> updateState { it.copy(servers = list, activeServerId = activeId) } }
        }
        discover()
    }

    private fun discover() {
        discoveryJob?.cancel()
        updateState { it.copy(discovering = true, discovered = emptyList()) }
        discoveryJob = viewModelScope.launch {
            discovery.discover()
                .catch { /* No network or no permission: discovery is best effort. */ }
                .collect { url ->
                    updateState { if (url in it.discovered) it else it.copy(discovered = it.discovered + url) }
                }
            updateState { it.copy(discovering = false) }
        }
    }

    private fun useDiscovered(baseUrl: String) {
        updateState {
            it.copy(url = baseUrl, method = AuthMethod.PairingCode, secret = "", urlError = null, secretError = null, submitError = null, probe = null)
        }
        sendEffect(ConnectEffect.FocusSecret)
    }

    override fun onEvent(event: ConnectEvent) {
        when (event) {
            is ConnectEvent.UrlChanged -> onUrlChanged(event.value)
            is ConnectEvent.SecretChanged -> updateState {
                // Pairing codes tolerate stray whitespace; passwords keep spaces but lose pasted line breaks.
                val secret = if (it.method == AuthMethod.Password) event.value.trim('\r', '\n') else event.value.trim()
                it.copy(secret = secret, secretError = null, submitError = null)
            }
            is ConnectEvent.MethodChanged -> updateState {
                it.copy(method = event.method, secret = "", secretError = null, submitError = null, probe = null)
            }
            ConnectEvent.TestConnection -> testConnection()
            ConnectEvent.Submit -> submit()
            is ConnectEvent.LinkPasted -> applyLink(event.text, fromScanner = false)
            ConnectEvent.OpenScanner -> updateState { it.copy(scannerOpen = true) }
            ConnectEvent.CloseScanner -> updateState { it.copy(scannerOpen = false) }
            is ConnectEvent.QrScanned -> {
                updateState { it.copy(scannerOpen = false) }
                applyLink(event.text, fromScanner = true)
            }
            is ConnectEvent.SelectServer -> viewModelScope.launch {
                servers.activate(event.serverId)
                sendEffect(ConnectEffect.Connected)
            }
            is ConnectEvent.UseDiscovered -> useDiscovered(event.baseUrl)
            ConnectEvent.Rediscover -> discover()
        }
    }

    private fun onUrlChanged(value: String) {
        // A full pairing link typed or pasted in the URL field fills both fields; the text of an
        // `opencode pair` QR code (copied by another scanner app) pairs like a scan.
        val looksLikePairing = "/auth/connect/" in value || value.trimStart().startsWith("{")
        val target = value.takeIf { looksLikePairing }?.let(servers::parseConnectTarget)
        if (target is ConnectTarget.Pairing && target.otherUrls.isNotEmpty()) {
            applyLink(value, fromScanner = false)
            return
        }
        if (target is ConnectTarget.Pairing) {
            updateState {
                it.copy(
                    url = target.baseUrl,
                    method = AuthMethod.PairingCode,
                    secret = target.code,
                    urlError = null,
                    secretError = null,
                    probe = null,
                    submitError = null,
                )
            }
            return
        }
        updateState { it.copy(url = value, urlError = null, probe = null, submitError = null) }
    }

    private fun applyLink(text: String?, fromScanner: Boolean) {
        val target = text?.let(servers::parseConnectTarget)
        when (target) {
            is ConnectTarget.Pairing -> {
                updateState {
                    it.copy(
                        url = target.baseUrl,
                        method = AuthMethod.PairingCode,
                        secret = target.code,
                        urlError = null,
                        secretError = null,
                        submitError = null,
                        probe = null,
                    )
                }
                if (target.otherUrls.isEmpty()) submit() else pairWithReachableUrl(target.urls)
            }
            is ConnectTarget.Url -> {
                updateState { it.copy(url = target.baseUrl, urlError = null, submitError = null, probe = null) }
                sendEffect(ConnectEffect.FocusSecret)
            }
            null -> sendEffect(
                ConnectEffect.ShowMessage(
                    UiText.Res(if (fromScanner) R.string.connect_scan_invalid else R.string.connect_paste_invalid),
                ),
            )
        }
    }

    /**
     * The `opencode pair` QR code lists every server address; most are unreachable from the
     * phone (Docker bridges, loopback, other networks). Probe them all at once and pair through
     * the first one in the server's order that answers, so the one-time code is spent only once.
     */
    private fun pairWithReachableUrl(urls: List<String>) {
        if (currentState.submitting) return
        probeJob?.cancel()
        updateState { it.copy(submitting = true) }
        viewModelScope.launch {
            val reachable = firstReachable(urls)
            updateState {
                it.copy(
                    submitting = false,
                    url = reachable ?: it.url,
                    submitError = if (reachable == null) UiText.Res(R.string.connect_error_no_reachable_url) else null,
                )
            }
            if (reachable != null) submit()
        }
    }

    private suspend fun firstReachable(urls: List<String>): String? = coroutineScope {
        val checks = urls.map { url ->
            async {
                // Unauthorized still proves the server answers on this address.
                val result = withTimeoutOrNull(PROBE_TIMEOUT_MS) { servers.probe(url, null) }
                result != null && (result.isSuccess || result.exceptionOrNull() is OpenCodeError.Unauthorized)
            }
        }
        val index = checks.indexOfFirst { it.await() }
        checks.forEach { it.cancel() }
        urls.getOrNull(index)
    }

    /** Returns the normalized base URL or sets a field error. */
    private fun validatedUrl(): String? {
        val raw = currentState.url.trim()
        if (raw.isEmpty()) {
            updateState { it.copy(urlError = UiText.Res(R.string.connect_error_url_required)) }
            return null
        }
        val base = when (val target = servers.parseConnectTarget(raw)) {
            is ConnectTarget.Pairing -> target.baseUrl
            is ConnectTarget.Url -> target.baseUrl
            null -> null
        }
        if (base == null) updateState { it.copy(urlError = UiText.Res(R.string.connect_error_url_invalid)) }
        return base
    }

    private fun testConnection() {
        val base = validatedUrl() ?: return
        val state = currentState
        val password = state.secret.takeIf { state.method == AuthMethod.Password && it.isNotEmpty() }
        probeJob?.cancel()
        updateState { it.copy(url = base, probing = true, probe = null, submitError = null) }
        probeJob = viewModelScope.launch {
            val result = servers.probe(base, password).fold(
                onSuccess = { ProbeResult.Reachable(it.version) },
                onFailure = { error ->
                    if (error is OpenCodeError.Unauthorized && password == null) ProbeResult.NeedsPairing
                    else ProbeResult.Failed(error.toUiText())
                },
            )
            updateState { it.copy(probing = false, probe = result) }
        }
    }

    private fun submit() {
        if (currentState.submitting) return
        val base = validatedUrl()
        val state = currentState
        val secretMissing = if (state.method == AuthMethod.PairingCode) state.secret.isBlank() else state.secret.isEmpty()
        if (secretMissing) {
            updateState {
                it.copy(
                    secretError = UiText.Res(
                        if (state.method == AuthMethod.PairingCode) R.string.connect_error_code_required else R.string.connect_error_password_required,
                    ),
                )
            }
        }
        if (base == null || secretMissing) return
        probeJob?.cancel()
        updateState { it.copy(url = base, submitting = true, probing = false, submitError = null) }
        viewModelScope.launch {
            val result = when (state.method) {
                AuthMethod.PairingCode -> servers.pair(base, state.secret)
                AuthMethod.Password -> servers.addWithPassword(base, state.secret)
            }
            result.fold(
                onSuccess = {
                    updateState { it.copy(submitting = false, secret = "") }
                    sendEffect(ConnectEffect.Connected)
                },
                onFailure = { error ->
                    val message = error.toUiText()
                    val secretRelated = error is OpenCodeError.Unauthorized || error is OpenCodeError.PairingFailed
                    updateState {
                        it.copy(
                            submitting = false,
                            secretError = if (secretRelated) message else null,
                            submitError = if (secretRelated) null else message,
                        )
                    }
                },
            )
        }
    }
}
