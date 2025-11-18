package com.chromia.cli.util

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.*
import org.yaml.snakeyaml.composer.Composer
import org.yaml.snakeyaml.parser.ParserImpl
import org.yaml.snakeyaml.reader.StreamReader
import org.yaml.snakeyaml.resolver.Resolver
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.StringWriter

fun updateChromiaYamlForLibrary(yamlFile: File, libraryName: String, libraryVersion: String) {
    val originalContent = yamlFile.readText()
    
    val yamlDir = yamlFile.parentFile
    val updatedRootNode = yamlFile.bufferedReader().use { reader ->
        val rootNode = reader.parseYaml()
        addNodeForChromiaLib(rootNode, libraryName, libraryVersion, yamlDir)
        rootNode
    }
    
    val stringWriter = StringWriter()
    BufferedWriter(stringWriter).use { writer ->
        dumpYaml(updatedRootNode, writer)
    }
    val updatedContent = stringWriter.toString()
    
    printYamlDiff(yamlFile.name, originalContent, updatedContent)
    
    yamlFile.bufferedWriter().use { writer ->
        dumpYaml(updatedRootNode, writer)
    }
}

fun printYamlDiff(fileName: String, originalContent: String, updatedContent: String) {
    val term = Terminal()
    val originalLines = originalContent.lines()
    val updatedLines = updatedContent.lines()
    
    if (originalLines == updatedLines) {
        return
    }
    
    val coloredText = TextColors.brightYellow("Updated '$fileName' file with chromia library")
    val boldText = TextStyles.underline(coloredText)
    term.println(boldText)

    val patch = DiffUtils.diff(originalLines, updatedLines)
    val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
        fileName,
        fileName,
        originalLines,
        patch,
        3
    )

    unifiedDiff.forEach { line ->
        val coloredLine = when {
            line.startsWith("---") || line.startsWith("+++") -> {
                TextColors.brightMagenta(line)
            }
            line.startsWith("-") -> {
                TextColors.red(line)
            }
            line.startsWith("+") -> {
                TextColors.green(line)
            }
            line.startsWith("@@") -> {
                TextColors.cyan(line)
            }
            else -> line
        }
        term.println(coloredLine)
    }
}

private fun Node.findMappingNode(key: String): MappingNode? = when (this) {
    is MappingNode -> value.find { it.keyNode.isScalarWithValue(key) }?.valueNode as? MappingNode
    else -> null
}

private fun Node.isScalarWithValue(value: String): Boolean =
    this is ScalarNode && this.value == value

private fun Node.isIncludeNode(): Boolean =
    this is ScalarNode && this.tag == Tag("!include")

private fun BufferedReader.parseYaml(): Node {
    val reader = StreamReader(this)
    val loaderOptions = LoaderOptions().apply {
        isProcessComments = true
    }
    val parser = ParserImpl(reader, loaderOptions)
    val composer = Composer(parser, Resolver(), loaderOptions)
    return composer.singleNode
}

private fun addNodeForChromiaLib(rootNode: Node, libraryName: String, libraryVersion: String, yamlDir: File) {
    if (rootNode !is MappingNode) return

    val libsTuple = rootNode.value.find { it.keyNode.isScalarWithValue("libs") }
    if (libsTuple != null) {
        when {
            libsTuple.valueNode.isIncludeNode() -> {
                val includeScalar = libsTuple.valueNode as ScalarNode
                val includePath = includeScalar.value
                val includeFile = File(yamlDir, includePath)
                updateIncludedLibsFile(includeFile, libraryName, libraryVersion)
            }
            libsTuple.valueNode is MappingNode -> {
                val libsNode = libsTuple.valueNode as MappingNode
                libsNode.ensureChromiaLib(libraryName, libraryVersion)
            }
            else -> {
                createLibsNodeInRoot(rootNode, libraryName, libraryVersion)
            }
        }
    } else {
        createLibsNodeInRoot(rootNode, libraryName, libraryVersion)
    }
}

private fun updateIncludedLibsFile(includeFile: File, libraryName: String, libraryVersion: String) {
    val originalContent = if (includeFile.exists()) includeFile.readText() else ""
    if (!includeFile.exists()) {
        includeFile.parentFile.mkdirs()
        includeFile.createNewFile()

        val emptyLibsNode = MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK)
        includeFile.bufferedWriter().use { writer ->
            dumpYaml(emptyLibsNode, writer)
        }
    }

    val updatedLibsNode = includeFile.bufferedReader().use { reader ->
        val rootNode = reader.parseYaml()
        if (rootNode is MappingNode) {
            rootNode.ensureChromiaLib(libraryName, libraryVersion)
        }
        rootNode
    }

    val stringWriter = StringWriter()
    BufferedWriter(stringWriter).use { writer ->
        dumpYaml(updatedLibsNode, writer)
    }
    val updatedContent = stringWriter.toString()

    printYamlDiff(includeFile.name, originalContent, updatedContent)

    includeFile.bufferedWriter().use { writer ->
        dumpYaml(updatedLibsNode, writer)
    }
}

private fun createLibsNodeInRoot(rootNode: MappingNode, libraryName: String, libraryVersion: String) {
    val libsKey = createScalarNode("libs")
    val libsNode = MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK)
    libsNode.ensureChromiaLib(libraryName, libraryVersion)
    rootNode.value.add(NodeTuple(libsKey, libsNode))
}

private fun MappingNode.ensureChromiaLib(libraryName: String, libraryVersion: String) {
    val chromiaLibNode = findMappingNode(libraryName) ?: run {
        val newKey = createScalarNode(libraryName)
        val newNode = MappingNode(Tag.MAP, mutableListOf(), DumperOptions.FlowStyle.BLOCK)
        value.add(NodeTuple(newKey, newNode))
        newNode
    }
    ensureVersion(chromiaLibNode, libraryVersion)
}

private fun ensureVersion(libraryNode: MappingNode, version: String) {
    val versionIndex = libraryNode.value.indexOfFirst { it.keyNode.isScalarWithValue("version") }
    
    if (versionIndex != -1) {
        val versionKey = libraryNode.value[versionIndex].keyNode
        val versionValue = createScalarNode(version)
        libraryNode.value[versionIndex] = NodeTuple(versionKey, versionValue)
    } else {
        val versionKey = createScalarNode("version")
        val versionValue = createScalarNode(version)
        libraryNode.value.add(NodeTuple(versionKey, versionValue))
    }
}

private fun dumpYaml(yamlNode: Node, writer: BufferedWriter) {
    val options = DumperOptions().apply {
        isProcessComments = true
        indentWithIndicator = true
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
    }
    
    val yaml = Yaml(options)
    yaml.serialize(yamlNode, writer)
}

private fun createScalarNode(value: String): ScalarNode =
    ScalarNode(Tag.STR, value, null, null, DumperOptions.ScalarStyle.PLAIN)