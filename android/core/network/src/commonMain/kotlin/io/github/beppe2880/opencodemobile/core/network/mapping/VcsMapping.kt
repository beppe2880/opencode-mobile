package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.VcsInfo
import io.github.beppe2880.opencodemobile.core.domain.VcsBranch

/** The branch of a repository; null without a provider (not a repository, or not opened yet). */
fun VcsInfo.toDomain(): VcsBranch? = provider?.let { VcsBranch(branch.current, branch.default) }
