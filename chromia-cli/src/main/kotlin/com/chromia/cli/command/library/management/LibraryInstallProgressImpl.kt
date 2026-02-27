package com.chromia.cli.command.library.management

import com.chromia.build.tools.lib.LibraryInstallProgress
import com.chromia.build.tools.model.writer.ChromiaYmlWriter
import com.chromia.build.tools.util.ifNotEmpty
import com.chromia.build.tools.util.isChromiaLib
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.util.printChromiaYmlDiff
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.mordant.animation.coroutines.CoroutineProgressAnimator
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.animation.progress.ProgressTask
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.spinner
import com.github.ajalt.mordant.widgets.progress.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2

class LibraryInstallProgressImpl(
    private val terminal: Terminal,
    private val model: ChromiaModel,
    override val errors: ConcurrentHashMap<String, String> = ConcurrentHashMap(),
) : LibraryInstallProgress {

    private val taskBars = mutableMapOf<String, ProgressTask<String>>()
    lateinit var progress: CoroutineProgressAnimator
    private var animationJob: Job? = null
    private lateinit var libIds: List<Pair<String, Boolean>>

    companion object {
        private const val FPS = 60
        private const val HUNDRED_PERCENT = 100L

        private const val ICON_INSTALLING = "📦"
        private const val ICON_SUCCESS = "✅"
        private const val ICON_ERROR = "❌"
        private const val ICON_BRANCH = "[git]"

    }

    fun initialize(scope: CoroutineScope) {
        libIds = model.libs.map { (id, model) -> id to model.isChromiaLib }

        val taskLayout = progressBarContextLayout(
            alignColumns = false,
            textFps = FPS,
            animationFps = FPS
        ) {
            text(align = TextAlign.LEFT) { context }
            spinner(Spinner.Dots())
        }

        progress = MultiProgressBarAnimation(terminal).animateInCoroutine()
        
        animationJob = scope.launch {
            progress.execute()
        }

        libIds.forEach { (libraryId) ->
            taskBars[libraryId] = progress.addTask(
                taskLayout,
                total = HUNDRED_PERCENT,
                context = formatProgress(libraryId, "Initializing")
            )
        }
    }

    override fun onStart(libraryId: String) {
        taskBars[libraryId]?.update {
            context = formatProgress(libraryId, "Starting installation")
        }
    }

    override fun onProgress(libraryId: String, current: Long, total: Long, message: String) {
        taskBars[libraryId]?.update {
            context = formatProgress(libraryId, message)
            completed = current
            this.total = total
        }
    }

    override fun onSuccess(libraryId: String) {
        taskBars[libraryId]?.update {
            context = formatSuccess(libraryId)
            completed = HUNDRED_PERCENT
            total = HUNDRED_PERCENT
        }
    }

    override fun onError(libraryId: String, errMessage: String) {
        taskBars[libraryId]?.update {
            context = formatError(libraryId)
            completed = HUNDRED_PERCENT
            total = HUNDRED_PERCENT
        }
        errors.putIfAbsent(libraryId, errMessage)
        taskBars.remove(libraryId)
    }

    override fun onPostInstall(libraryId: String, libraryVersion: String) {
        val chromiaYmlFile = model.compile.root.resolve("chromia.yml").toFile()
        ChromiaYmlWriter.updateLibraryNode(chromiaYmlFile, libraryId, libraryVersion) { diff ->
            terminal.printChromiaYmlDiff(chromiaYmlFile, diff)
        }
    }

    private fun formatProgress(libraryId: String, message: String): String {
        val styledLibraryName = TextColors.cyan + TextStyles.bold
        val styledMessage = TextColors.yellow
        return "$ICON_INSTALLING ${styledLibraryName(libraryId)} ${gitSymbol(libraryId)}: ${styledMessage(message)}"
    }

    private fun formatSuccess(libraryId: String): String {
        val styledLibraryName = TextColors.cyan + TextStyles.bold
        val successStyle = TextColors.green + TextStyles.bold
        return buildString {
            append("$ICON_SUCCESS ${styledLibraryName(libraryId)} ${gitSymbol(libraryId)}: ")
            append(successStyle("Installed successfully"))
        }
    }

    private fun formatError(libraryId: String) = buildString {
        val errorStyle = TextColors.red + TextStyles.bold
        append("$ICON_ERROR ${errorStyle(libraryId)} ${gitSymbol(libraryId)}")
    }

    suspend fun finish() {
        animationJob?.join()

        // after all animations finish only then we want to print the summary and throw if we have errors
        errors.ifNotEmpty {
            val horizontalLine = TextColors.gray("─".repeat(terminal.size.width.coerceAtMost(80)))
            terminal.println(horizontalLine)
            val errorStyle = TextColors.red + TextStyles.bold
            val summaryTitle = errorStyle("Failed to install ${it.size} library/libraries:")
            terminal.println(summaryTitle)
            terminal.println()

            it.forEach { (libraryId, errorMsg) ->
                val styledLibraryId = TextColors.red + TextStyles.bold
                val bullet = TextColors.red("•")
                terminal.println("  $bullet ${styledLibraryId(libraryId)}")
                terminal.println("    $errorMsg")
            }
            throw CliktError(statusCode = 1)
        }
    }

    private fun isChromiaLib(libraryId: String) = libIds.first { (id, _) -> id == libraryId}.second
    private fun gitSymbol(libraryId: String) = if (isChromiaLib(libraryId)) "" else ICON_BRANCH
}

