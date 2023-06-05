package com.chromia.cli.model

import com.chromia.cli.parser.loadAnchor
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.github.ajalt.clikt.core.PrintMessage
import net.postchain.gtv.yaml.GtvYaml
import java.io.File

fun parseModel(src: File) = try {
   GtvYaml().loadAnchor<ChromiaCliModel>(src)
} catch (e: JsonMappingException) {
    throw RuntimeException("Failed to parse json", e)
}
