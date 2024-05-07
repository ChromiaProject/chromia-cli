package com.chromia.cli.check_compatilibity

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import net.postchain.common.exception.UserMistake
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CheckCompatibilityTest {

    @Test
    fun `no data - fail - entity attribute type changed`() {

        val oldVersion = BlockchainSource.fromXmlFile("economy_chain_test", javaClass.getResource("/com/chromia/cli/check_compatilibity/economy_chain_test.xml")!!.path)
        val newVersion = BlockchainSource.fromXmlFile("economy_chain_test", javaClass.getResource("/com/chromia/cli/check_compatilibity/economy_chain_test-new-broken-int.xml")!!.path)

        val assertThrows = assertThrows<UserMistake> {
            CheckCompatibility(newVersion).upgradeFrom(oldVersion, "economy_chain_test", withData = false)
        }
        assertThat(assertThrows.message)
                .isNotNull()
                .isEqualTo("Deployment failed:\n" +
                        "Type of attribute 'description' of entity 'common_proposal:common_proposal' (meta: common_proposal) changed: was sys:text, now sys:integer")
    }

    @Test
    fun `with data - success - economy chain`() {

        val oldVersion = BlockchainSource.fromXmlFile("economy_chain_test", javaClass.getResource("/com/chromia/cli/check_compatilibity/economy_chain_test.xml")!!.path)
        val newVersion = BlockchainSource.fromXmlFile("economy_chain_test", javaClass.getResource("/com/chromia/cli/check_compatilibity/economy_chain_test.xml")!!.path)

        CheckCompatibility(newVersion).upgradeFrom(oldVersion, "economy_chain_test")
    }

    @Test
    fun `with data - success - testchain`() {

        val oldVersion = BlockchainSource.fromXmlFile("testchain", javaClass.getResource("/com/chromia/cli/check_compatilibity/testchain-from-entity-without-default.xml")!!.path)
        val newVersion = BlockchainSource.fromXmlFile("testchain", javaClass.getResource("/com/chromia/cli/check_compatilibity/testchain-from-entity-without-default.xml")!!.path)

        CheckCompatibility(newVersion).upgradeFrom(oldVersion, "testchain")
    }

    @Test
    fun `with data - fail - missing default value`() {

        val oldVersion = BlockchainSource.fromXmlFile("testchain", javaClass.getResource("/com/chromia/cli/check_compatilibity/testchain-from-entity-without-default.xml")!!.path)
        val newVersion = BlockchainSource.fromXmlFile("testchain", javaClass.getResource("/com/chromia/cli/check_compatilibity/testchain-to.xml")!!.path)

        val exception = assertThrows<UserMistake> { CheckCompatibility(newVersion).upgradeFrom(oldVersion, "testchain") }
        assertThat(exception.message)
                .isNotNull()
                .isEqualTo("Deployment failed:\n" +
                        "New attribute 'new_attribute' of entity 'mocked_module:changed1' (meta: changed1) has no default value\n" +
                        "New attribute 'new_attribute' of entity 'mocked_module1:changed2_entity_3' (meta: changed2_entity_3) has no default value\n" +
                        "New attribute 'new_attribute' of entity 'mocked_module2:changed2_entity_4' (meta: changed2_entity_4) has no default value"
                )
    }

    @Test
    fun `with data - fail - missing default value in economy chain`() {

        val oldVersion = BlockchainSource.fromXmlFile("economy_chain_test", javaClass.getResource("/com/chromia/cli/check_compatilibity/economy_chain_test.xml")!!.path)
        val newVersion = BlockchainSource.fromXmlFile("economy_chain_test", javaClass.getResource("/com/chromia/cli/check_compatilibity/economy_chain_test-new-no-default-value.xml")!!.path)

        val exception = assertThrows<UserMistake> { CheckCompatibility(newVersion).upgradeFrom(oldVersion, "economy_chain_test") }
        assertThat(exception.message)
                .isNotNull()
                .isEqualTo("Deployment failed:\n" +
                        "New attribute 'dummy' of entity 'economy_chain:provider' (meta: provider) has no default value\n" +
                        "New attribute 'dummy' of entity 'common_proposal:common_voter_set_governance' (meta: common_voter_set_governance) has no default value")
    }

    @Test
    fun `with data - fail - mainnet missing default value`() {

        val oldVersion = BlockchainSource.fromXmlFile("testchain", javaClass.getResource("/com/chromia/cli/check_compatilibity/mainnet.xml")!!.path)
        val newVersion = BlockchainSource.fromXmlFile("testchain", javaClass.getResource("/com/chromia/cli/check_compatilibity/mainnet-entity-update.xml")!!.path)

        val exception = assertThrows<UserMistake> { CheckCompatibility(newVersion, LogWrapper(true)).upgradeFrom(oldVersion, "testchain") }
        assertThat(exception.message)
                .isNotNull()
                .isEqualTo("Deployment failed:\n" +
                        "New attribute 'dummy' of entity 'model:voter_set' (meta: voter_set) has no default value\n" +
                        "New attribute 'dummy' of entity 'proposal_container:pending_remove_container' (meta: pending_remove_container) has no default value")
    }

    // These tests can be used to test the compatibility tool with local d1
    //    @Test
//    fun `manual test with local d1`() {
//
//        val oldVersion = CompatibilityBlockchain.fromChromiaFile("/home/joh-nils/git/directory-chain/chromia.yml")
//        val newVersion = CompatibilityBlockchain.fromXmlFile("economy_chain_test", javaClass.getResource("/economy_chain_test-new-broken-int.xml")
//
//        val assertThrows = assertThrows<UserMistake> {
//            CheckCompatibility(newVersion).upgradeFrom(oldVersion, "economy_chain_test")
//        }
//        assertThat(assertThrows.message)
//                .isNotNull()
//                .isEqualTo("Deployment failed:\n" +
//                        "Type of attribute 'description' of entity 'common_proposal:common_proposal' (meta: common_proposal) changed: was sys:text, now sys:integer")
//    }
}