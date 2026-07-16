package com.lumalauncher.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {
    @Test
    fun comparesVersionNumbersByEachPart() {
        assertTrue(UpdateRepository.compareVersions("0.2.0", "0.1.9") > 0)
        assertTrue(UpdateRepository.compareVersions("1.0.0", "0.99.99") > 0)
        assertEquals(0, UpdateRepository.compareVersions("1.2", "1.2.0"))
    }
}
