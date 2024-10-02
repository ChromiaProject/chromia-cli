package com.chromia.cli.command.deployment.proposal

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ProposalUtilsKtTest {

    @Test
    fun thresholdFormatDefaultTest() {
        assertThat(formatThreshold(0)).isEqualTo("super majority (>66.66%)")
    }

    @Test
    fun thresholdFormatSimpleMajorityTest() {
        assertThat(formatThreshold(-1)).isEqualTo("majority (>50%)")
    }

    @Test
    fun customThresholdFormatTest() {
        assertThat(formatThreshold(3)).isEqualTo("3")
    }
}