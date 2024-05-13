package com.chromia.build.tools.lib

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.lib.DirectoryHashCalculator.RidStrategy
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.wrap
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash
import net.postchain.rell.base.utils.RellGtxConfigConstants
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DirectoryHashCalculatorTest {

    @TempDir
    lateinit var sourceDir: Path

    val hashCalculator = GtvMerkleHashCalculator(Secp256K1CryptoSystem())

    @Test
    fun `Given set of files, rid is computed as the hash of the map of files to their content`() {
        sourceDir.resolve("path/to/file/a.rell").createParentDirectories().writeText("content a")
        sourceDir.resolve("path/to/other/file/b.rell").createParentDirectories().writeText("content b")

        val expectedRid = gtv(RellGtxConfigConstants.SOURCES_KEY to gtv("path/to/file/a.rell" to gtv("content a"), "path/to/other/file/b.rell" to gtv("content b"))).merkleHash(hashCalculator)
        val computedRid = DirectoryHashCalculator(sourceDir).compute(sourceDir.resolve("path/to"), RidStrategy.MAP)
        assertThat(computedRid).isEqualTo(expectedRid.wrap())
        assertThat(computedRid).isEqualTo(("C7F369AFD42DF7E8F64F55F65946841B0D5339E989FAE8EDF8655C2665D5DB8D").hexStringToWrappedByteArray())
    }

    @Test
    fun `Given set of files, legacy rid is computed as the has of the ordered set of file contents`() {
        sourceDir.resolve("path/to/file/a.rell").createParentDirectories().writeText("content a")
        sourceDir.resolve("path/to/other/file/b.rell").createParentDirectories().writeText("content b")
        sourceDir.resolve("path/to/a/file/c.rell").createParentDirectories().writeText("content c")

        val expectedRid = gtv(RellGtxConfigConstants.SOURCES_KEY to gtv(gtv("content c"), gtv("content a"), gtv("content b"))).merkleHash(hashCalculator)
        val computedRid = DirectoryHashCalculator(sourceDir).compute(sourceDir.resolve("path/to"), RidStrategy.LIST)
        assertThat(computedRid).isEqualTo(expectedRid.wrap())
        assertThat(computedRid).isEqualTo("C1401B4ED25FC1C96A3FC8CD7957F445EA47E408019AC58E640EC56FB9615168".hexStringToWrappedByteArray())
    }
}
