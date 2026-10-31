package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.VcsInfo
import io.github.beppe2880.opencodemobile.core.domain.VcsBranch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import io.github.beppe2880.opencodemobile.api.models.VcsBranch as ApiVcsBranch

class VcsMappingTest {
    @Test
    fun `a repository has a provider`() {
        assertEquals(VcsBranch("feat", "main"), VcsInfo(ApiVcsBranch("feat", "main"), provider = "git").toDomain())
        assertEquals(VcsBranch(null, "main"), VcsInfo(ApiVcsBranch(null, "main"), provider = "git").toDomain())
    }

    @Test
    fun `without a provider there is no repository`() {
        assertNull(VcsInfo(ApiVcsBranch()).toDomain())
    }
}
