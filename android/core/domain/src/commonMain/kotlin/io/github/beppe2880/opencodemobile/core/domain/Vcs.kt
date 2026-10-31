package io.github.beppe2880.opencodemobile.core.domain

/** The branch a project is on; [current] is null on a detached HEAD. */
data class VcsBranch(val current: String?, val default: String?)

interface VcsRepository {
    /** The branch of [directory]; null when it is not in a repository. */
    suspend fun branch(directory: String): Result<VcsBranch?>

    /** Files with uncommitted changes in [directory]'s working copy; empty without a repository. */
    suspend fun changedFiles(directory: String): Result<List<String>>

    /** The uncommitted changes of [directory] with unified diffs, like [changedFiles]; empty without a repository. */
    suspend fun workingChanges(directory: String): Result<List<FileDiff>>
}
