package com.chromia.cli.util

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.restapi.withRellVersion
import com.chromia.cli.model.RellVersion
import com.chromia.cli.versionfinder.PostchainRellVersionFinder
import kotlin.test.assertEquals
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.Endpoint
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.rell.base.model.R_LangVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RellVersionControllerTest {

    val template = PostchainClientConfig(BlockchainRid.ZERO_RID, endpointPool = EndpointPool.singleUrl(""))
    val clientProvider = PostchainClientProviderImpl()

    @Test
    fun getTargetVersion200Status() {
        val expectedVersion = R_LangVersion.of(RellVersion)
        withModel(TestModel(BlockchainRid.ZERO_RID).withRellVersion(expectedVersion)) {
            val res = PostchainRellVersionFinder(template, PostchainClientProviderImpl()).getTargetVersion(Endpoint(RestApiInstance.apiUrl), BlockchainRid.ZERO_RID)
            assertEquals(res, expectedVersion)
        }
    }

    @Test
    fun getTargetVersion404Status() {
        withModel() {
            val res = assertThrows<RuntimeException> { PostchainRellVersionFinder(template, clientProvider).getTargetVersion(Endpoint(RestApiInstance.apiUrl), BlockchainRid(ByteArray(32))) }.message!!
            assertThat(res).contains("Can not find blockchain with blockchainRID: 0000000000000000000000000000000000000000000000000000000000000000")
        }
    }

    @Test
    fun getTargetVersion503Status() {
        val res = assertThrows<RuntimeException> { PostchainRellVersionFinder(template, clientProvider).getTargetVersion(Endpoint("https://localhost:7745"), BlockchainRid(ByteArray(32))) }.message!!
        assertThat(res).contains("Connection Refused from https://localhost:7745")
    }
}
