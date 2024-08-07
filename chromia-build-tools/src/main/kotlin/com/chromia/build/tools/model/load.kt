package com.chromia.cli.model

import com.chromia.build.tools.compile.ValidationException
import com.chromia.cli.parser.loadAnchor
import java.io.File
import java.nio.file.Path
import org.yaml.snakeyaml.parser.ParserException


fun parseModel(src: Path) = parseModel(src.toFile())
fun parseModel(src: File): ChromiaModel {
    return try {
        val anc = loadAnchor(src, ChromiaModel.schema)
        ChromiaModel.load(anc, src.toPath().parent)
    } catch (e: ValidationException) {
        throw e
    } catch (e: ParserException) {
        throw ValidationException("Unable to parse file ${src.name} due to unexpected value at line: ${e.problemMark.line}, column: ${e.problemMark.column}\n${e.problemMark._snippet}")
    }
}

fun getLibPaths(model: ChromiaModel, sourceDir: Path): Set<Path> {
    return model.libs.keys.map { libName -> sourceDir.resolve("lib/$libName") }.toSet()
}
