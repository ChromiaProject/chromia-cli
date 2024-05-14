package com.chromia.api

import com.chromia.api.result.Named
import com.chromia.build.tools.compile.ValidationException
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.DeploymentModel

fun ChromiaModel.filterBlockchains(blockchains: Collection<String>): ChromiaModel {
    if (blockchains.isEmpty()) return this
    if (!this.blockchains.keys.containsAll(blockchains)) throw ValidationException("Cannot compile blockchains $blockchains. Configured chains are ${this.blockchains.keys}")
    return copy(blockchains = this.blockchains.filter { blockchains.isEmpty() || it.key in blockchains })
}

fun ChromiaModel.filterBlockchains(predicate: (String, BlockchainModel) -> Boolean): ChromiaModel {
    return copy(blockchains = this.blockchains.filter { predicate(it.key, it.value) })
}

fun ChromiaModel.filterLibraries(libraries: List<String>?): ChromiaModel {
    if (libraries != null && !this.libs.keys.containsAll(libraries)) throw ValidationException("Cannot filter libraries $libraries. Configured libraries are ${libs.keys}")
    return copy(libs = libs.filter { libraries == null || it.key in libraries })
}

fun DeploymentModel.filterChains(blockchainsToKeep: List<String>?): DeploymentModel {
    if (blockchainsToKeep != null && !this.chains.keys.containsAll(blockchainsToKeep)) throw ValidationException("Cannot filter deployed chains")
    return copy(chains = this.chains.filter { blockchainsToKeep == null ||  it.key in blockchainsToKeep })
}

internal fun <T> Map<String, T>.toNamed(): List<Named<T>> = entries.map { Named(it.key, it.value) }