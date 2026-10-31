package io.github.beppe2880.opencodemobile.core.data.vcs

import io.github.beppe2880.opencodemobile.api.models.VcsMode
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.FileDiff
import io.github.beppe2880.opencodemobile.core.domain.VcsBranch
import io.github.beppe2880.opencodemobile.core.domain.VcsRepository
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import io.github.beppe2880.opencodemobile.core.network.mapping.toDomain
import kotlinx.coroutines.delay

/**
 * The server starts watching a project's repository on the first request for it and answers
 * without a provider until then (under a second), so a missing provider is asked again
 * [warmUpRetries] times before the directory counts as not being a repository.
 */
internal class RemoteVcsRepository(
    private val clients: OpenCodeClients,
    private val warmUpRetries: Int = WARM_UP_RETRIES,
    private val warmUpDelayMs: Long = WARM_UP_DELAY_MS,
) : VcsRepository {

    override suspend fun branch(directory: String): Result<VcsBranch?> = resultOf {
        var branch: VcsBranch? = null
        for (attempt in 0..warmUpRetries) {
            if (attempt > 0) delay(warmUpDelayMs)
            branch = awaitBody { clients.active().vcs.vcsGet(locationDirectory = directory) }.data.toDomain()
            if (branch != null) break
        }
        branch
    }

    override suspend fun changedFiles(directory: String): Result<List<String>> = resultOf {
        awaitBody { clients.active().vcs.vcsStatus(locationDirectory = directory) }.data.map { it.file }
    }

    override suspend fun workingChanges(directory: String): Result<List<FileDiff>> = resultOf {
        awaitBody { clients.active().vcs.vcsDiff(VcsMode.WORKING, locationDirectory = directory, context = DIFF_CONTEXT_LINES.toString()) }
            .data.map { it.toDomain() }
    }

    private companion object {
        const val WARM_UP_RETRIES = 2
        const val WARM_UP_DELAY_MS = 1_000L
        const val DIFF_CONTEXT_LINES = 3
    }
}
