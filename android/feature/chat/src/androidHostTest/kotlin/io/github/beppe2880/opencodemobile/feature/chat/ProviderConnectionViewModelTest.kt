package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.FormField
import io.github.beppe2880.opencodemobile.core.domain.IntegrationRepository
import io.github.beppe2880.opencodemobile.core.domain.KeyIntegration
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ServerInfo
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProviderConnectionViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val server = ServerProfile("s1", "Test server", "http://example.test", secret = "test", credential = CredentialKind.Password, addedAt = 1)
    private val servers = object : ServerRepository {
        override val servers = MutableStateFlow(listOf(server))
        override val activeServer = MutableStateFlow<ServerProfile?>(server)
        override suspend fun probe(baseUrl: String, password: String?) = Result.success(ServerInfo("test", emptyList()))
        override suspend fun pair(baseUrl: String, code: String, name: String?) = Result.success(server)
        override suspend fun addWithPassword(baseUrl: String, password: String, name: String?) = Result.success(server)
        override suspend fun activate(serverId: String) {}
        override suspend fun rename(serverId: String, name: String) {}
        override suspend fun remove(serverId: String) {}
    }
    private val integration = KeyIntegration("custom", "Custom", false, null, listOf(FormField.Text("region", default = "eu", required = true)))
    private val repository = object : IntegrationRepository {
        var result = Result.success(Unit)
        var listResult = Result.success(listOf(integration))
        var gate: CompletableDeferred<Unit>? = null
        var calls = 0
        var target: Pair<String, String>? = null
        override suspend fun listKeys(serverId: String, directory: String) = listResult
        override suspend fun connectKey(
            serverId: String,
            directory: String,
            integrationId: String,
            key: String,
            answers: Map<String, FormAnswer>,
        ): Result<Unit> {
            calls++
            target = serverId to directory
            assertEquals("dummy-key", key)
            assertEquals(FormAnswer.Text("eu"), answers["region"])
            gate?.await()
            return result
        }
    }
    private val catalog = object : CatalogRepository {
        var invalidated: String? = null
        override fun invalidateModels(serverId: String) { invalidated = serverId }
        override suspend fun agents(directory: String?) = Result.success(emptyList<Agent>())
        override suspend fun models(directory: String?) = Result.success(emptyList<ModelInfo>())
    }
    private fun vm() = ProviderConnectionViewModel(repository, servers, catalog)
    private fun ProviderConnectionViewModel.select() {
        onEvent(ProviderConnectionEvent.Select("custom"))
        onEvent(ProviderConnectionEvent.KeyChanged("dummy-key"))
    }

    @Test fun `save uses captured target and clears credentials on success`() = runTest(main.testDispatcher) {
        val vm = vm()
        vm.onEvent(ProviderConnectionEvent.Open("/repo"))
        advanceUntilIdle()
        vm.select()
        repository.gate = CompletableDeferred()
        vm.onEvent(ProviderConnectionEvent.Submit)
        vm.onEvent(ProviderConnectionEvent.Submit)
        runCurrent()
        assertEquals(1, repository.calls)
        assertTrue(vm.state.value.saving)
        repository.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals("s1" to "/repo", repository.target)
        assertEquals("s1", catalog.invalidated)
        assertTrue(vm.state.value.connected)
        assertEquals("", vm.state.value.key)
        assertTrue(vm.state.value.answers.isEmpty())
    }

    @Test fun `failed save clears key and can be retried without leaking the error`() = runTest(main.testDispatcher) {
        val vm = vm()
        vm.onEvent(ProviderConnectionEvent.Open("/repo"))
        advanceUntilIdle()
        vm.select()
        repository.result = Result.failure(OpenCodeError.Server(400, null, "dummy-key"))
        vm.onEvent(ProviderConnectionEvent.Submit)
        advanceUntilIdle()
        assertTrue(vm.state.value.failed)
        assertEquals("", vm.state.value.key)
        assertFalse(vm.state.value.toString().contains("dummy-key"))
        repository.result = Result.success(Unit)
        vm.onEvent(ProviderConnectionEvent.KeyChanged("dummy-key"))
        vm.onEvent(ProviderConnectionEvent.Submit)
        advanceUntilIdle()
        assertTrue(vm.state.value.connected)
    }

    @Test fun `changing server or closing resets all sensitive state`() = runTest(main.testDispatcher) {
        val vm = vm()
        vm.onEvent(ProviderConnectionEvent.Open("/repo"))
        advanceUntilIdle()
        vm.select()
        servers.activeServer.value = server.copy(id = "s2")
        advanceUntilIdle()
        assertEquals(ProviderConnectionState(), vm.state.value)
        vm.onEvent(ProviderConnectionEvent.Submit)
        assertEquals(0, repository.calls)
        vm.onEvent(ProviderConnectionEvent.Close)
        assertEquals(ProviderConnectionState(), vm.state.value)
    }

    @Test fun `list failures are retryable and blank keys never save`() = runTest(main.testDispatcher) {
        repository.listResult = Result.failure(OpenCodeError.Unreachable("offline"))
        val vm = vm()
        vm.onEvent(ProviderConnectionEvent.Open("/repo"))
        advanceUntilIdle()
        assertTrue(vm.state.value.failed)
        repository.listResult = Result.success(listOf(integration))
        vm.onEvent(ProviderConnectionEvent.Retry)
        advanceUntilIdle()
        vm.onEvent(ProviderConnectionEvent.Select("custom"))
        vm.onEvent(ProviderConnectionEvent.KeyChanged("  "))
        vm.onEvent(ProviderConnectionEvent.Submit)
        assertEquals(0, repository.calls)
    }
}
