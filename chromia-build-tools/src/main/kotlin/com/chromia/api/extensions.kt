package com.chromia.api

import com.chromia.build.tools.compile.ValidationException
import com.chromia.cli.model.ChromiaModel

fun ChromiaModel.filterBlockchains(blockchains: Collection<String>): ChromiaModel {
    if (!this.blockchains.keys.containsAll(blockchains)) throw ValidationException("Cannot compile blockchains $blockchains. Configured chains are ${this.blockchains.keys}")
    return copy(blockchains = this.blockchains.filter { it.key in blockchains })
}