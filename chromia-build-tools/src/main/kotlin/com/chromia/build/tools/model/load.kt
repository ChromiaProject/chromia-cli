package com.chromia.cli.model

import com.chromia.build.tools.compile.ValidationException
import com.chromia.cli.parser.loadAnchor
import org.yaml.snakeyaml.parser.ParserException
import java.io.File
import java.nio.file.Path


fun parseModel(src: Path) = parseModel(src.toFile())
fun parseModel(src: File): ChromiaModel {
    return try {
        val anc = loadAnchor(src, ChromiaModel.schema)
        ChromiaModel.load(anc)
    } catch (e: ValidationException) {
        throw e
    } catch (e: ParserException) {
        throw ValidationException("Unable to parse file ${src.name} due to unexpected value at line: ${e.problemMark.line}, column: ${e.problemMark.column}\n${e.problemMark._snippet}")
    }
}
