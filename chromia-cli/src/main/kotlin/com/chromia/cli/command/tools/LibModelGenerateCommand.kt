package com.chromia.cli.command.tools

import com.chromia.cli.command.ChromiaCommand
import com.chromia.build.tools.lib.DirectoryHashCalculator
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.path

class LibModelGenerateCommand : ChromiaCommand(name = "lib-model", help = "Generate a library model") {
    private val name by option(help = "Name of the library")
    private val libSource by option("-s", "--library-source")
                    .path(mustExist = true, canBeFile = false, canBeDir = true)
                    .required()
    private val registry by option(help = "Git reference")
            .default("<git reference>")
            .validate {
                require(it.isNotEmpty()) { "Registry must not be empty" }
                require(it.matches(GIT_URL_REGEX)) { "Registry must be a valid git URL" }
            }
    private val tagOrBranch by option(help = "Tag or branch the library is published on")
            .default("<Tag or branch the library is published on>")
    private val insecure by option(help = "Allow insecure connections")
            .boolean()
            .default(false)

    override fun run() {
        val calculator = DirectoryHashCalculator(libSource)
        val rid = calculator.compute(libSource, DirectoryHashCalculator.RidStrategy.LIST)

        echo(
                """
                libs:
                    $name:
                        registry: $registry
                        path: $libSource
                        tagOrBranch: $tagOrBranch
                        rid: ${rid.toHex()}
                        insecure: $insecure
                 
                """.trimIndent()
        )
    }

    companion object {
        /* note: regex matches git URLs in these formats:
            - SSH: git@chromaway.com:user/repo.git
            - HTTPS: https://github.com/user/repo.git
            - With ports: https://gitlab.com:8080/user/repo.git
            - With path segments: https://bitbucket.com/org/group/repo.git

            (?:https://|git@)                      # Protocol
            (?=                                    # Lookahead for domain validation
                [^\s/:]+                           # Domain without spaces/slashes/colons
                (?::\d+)?                          # Optional port
                [/:]                               # Path separator
            )
            [a-z\d][\w.-]*\.[a-z]{2,}               # Domain (must start with letter/digit, contain dot)
            (?::\d+)?                               # Port number
            [/:]                                    # Separator (: for SSH, / for HTTPS)
            (?!.*\.\.)                              # No consecutive dots anywhere in path
            (?:[\w-]+/)*                            # Path segments
            [\w-]+(?<!\.git)                        # Repository name (no dots, not ending in .git)
            \.git$                                  # .git suffix
        */
        val GIT_URL_REGEX = "^(?:https?://|git@)[a-z\\d][\\w.-]*\\.[a-z]{2,}(?::\\d+)?[/:](?!.*\\.\\.)(?:[\\w-]+/)*[\\w-]+(?<!\\.git)\\.git$".toRegex()
    }

}