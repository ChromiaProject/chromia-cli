package com.chromia.cli.coverage

import net.postchain.rell.base.utils.UnitTestCaseResult
import java.nio.file.Path
import kotlin.io.path.createDirectories

class CodeCoverageCollector {
    private val coverageData = mutableMapOf<String, MutableList<CoverageEntry>>()

    fun addCoverageEntry(filePath: String, lineNumber: Int, isCovered: Boolean) {
        val entry = CoverageEntry(filePath, lineNumber, isCovered)
        coverageData.getOrPut(filePath) { mutableListOf() }.add(entry)
    }

    fun getCoverageData(): Map<String, List<CoverageEntry>> {
        return coverageData.mapValues { it.value.toList() }
    }
    
    fun writeReport(reportFile: Path) {
        if(coverageData.isEmpty()) {
            return
        }
        reportFile.parent.createDirectories()
        reportFile.toFile().bufferedWriter().use { writer ->
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            writer.write("<!DOCTYPE coverage SYSTEM \"http://cobertura.sourceforge.net/xml/coverage-04.dtd\">\n")
            writer.write("<coverage version=\"1.9\" timestamp=\"${System.currentTimeMillis() / 1000}\">\n")
            writer.write("  <sources>\n    <source>.</source>\n  </sources>\n")
            writer.write("  <packages>\n")
            
            val filesByPackage = coverageData.keys.groupBy { filePath ->
                val packagePath = filePath.substringBeforeLast('/', "")
                packagePath.ifEmpty { "default" }
            }
            
            for ((packageName, files) in filesByPackage) {
                val packageDisplayName = if (packageName == "default") "" else packageName
                writer.write("    <package name=\"$packageDisplayName\" line-rate=\"1\" branch-rate=\"0\">\n")
                writer.write("      <classes>\n")
                
                for (filePath in files) {
                    val className = filePath.substringAfterLast('/')
                    val entries = coverageData[filePath] ?: emptyList()
                    val totalLines = entries.size
                    val coveredLines = entries.count { it.isCovered }
                    val lineRate = if (totalLines > 0) coveredLines.toDouble() / totalLines else 0.0
                    
                    writer.write("        <class name=\"$className\" filename=\"$filePath\" line-rate=\"$lineRate\" branch-rate=\"0.0\">\n")
                    writer.write("          <lines>\n")
                    
                    for (entry in entries) {
                        val hits = if (entry.isCovered) 1 else 0
                        writer.write("            <line number=\"${entry.lineNumber}\" hits=\"$hits\" branch=\"false\"/>\n")
                    }
                    
                    writer.write("          </lines>\n")
                    writer.write("        </class>\n")
                }
                
                writer.write("      </classes>\n")
                writer.write("    </package>\n")
            }
            
            writer.write("  </packages>\n")
            writer.write("</coverage>")
        }
    }

    fun onTestCaseFinished(res: UnitTestCaseResult) {
        res.res.hits?.forEach { addCoverageEntry(it.file, it.line, true) }
    }

    data class CoverageEntry(val filePath: String, val lineNumber: Int, val isCovered: Boolean)
}