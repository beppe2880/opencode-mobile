package io.github.beppe2880.opencodemobile.core.data.vcs

import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.VcsBranch
import io.github.beppe2880.opencodemobile.core.domain.VcsRepository
import io.github.beppe2880.opencodemobile.core.network.awaitBody
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
            val info = awaitBody { clients.active().vcs.vcsGet(locationDirectory = directory) }.data
            if (info.provider != null) {
                branch = VcsBranch(info.branch.current, info.branch.default)
                break
            }
        }
        branch
    }

    override suspend fun changedFiles(directory: String): Result<List<String>> = resultOf {
        awaitBody { clients.active().vcs.vcsStatus(locationDirectory = directory) }.data.map { it.file }
    }

    private companion object {
        const val WARM_UP_RETRIES = 2
        const val WARM_UP_DELAY_MS = 1_000L
    }
}
