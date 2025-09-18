package com.chromia.cli.command.library.management

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.chromia.build.tools.lib.GitRepositoryCloner
import com.chromia.cli.command.BuildCommand
import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.command.library.developer.CreateDeveloperCommand
import com.chromia.cli.command.library.organization.CreateOrganizationCommand
import com.chromia.cli.command.library.organization.ViewOrganizationCommand
import com.chromia.library.chain.users.external.GET_DEVELOPER
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.testing.test
import net.postchain.common.BlockchainRid
import net.postchain.common.toHex
import net.postchain.crypto.KeyPair
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.gtv.gtvml.GtvMLParser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.io.readText

class LibraryCommandsIntegrationTest : IntegrationTestSetup() {

    @TempDir
    private lateinit var rellDappDir: Path

    @TempDir
    private lateinit var myLibDir: Path

    @TempDir
    private lateinit var libraryChainDir: Path

    @TempDir
    private lateinit var myLibDirWithCompilationErrors: Path

    private lateinit var libraryChainBrid: BlockchainRid
    private lateinit var libraryChainKeyPair: KeyPair
    private lateinit var adminSecretFile: File
    private lateinit var dev1SecretFile: File
    private lateinit var dev2SecretFile: File
    private lateinit var keyPair1: KeyPair
    private lateinit var keyPair2: KeyPair

    private val organisationId = "com.example"
    private val libraryName = "my_lib"

    @BeforeEach
    fun setupLibraryChain() {
        libraryChainKeyPair = KeyPair.of(
            "03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070",
            "0000000000000000000000000000000001000000000000000000000000000000"
        )
        cloneLibraryChain()
        libraryChainBrid = deployLibraryChain()
    }

    @Test
    fun `test complete library management workflow`() {
        setupKeysAndSecretFiles()

        // Run entire library flow's happy path
        testCreateDevelopers()
        testCreateAndViewOrganization()
        testCreateLibrary()
        testInstallLibrary()
        testInstallLibraryExplicitelyWithVersion()
        testAddDevToLibrary()
        testUpdateDevPermission()
        testRemoveDevFromLibrary()
        testDeployNewLibraryVersion()
        testDeployLibraryWithCompilationErrors()
    }

    fun testDeployLibraryWithCompilationErrors() {
        setupLibraryFilesWithCompilationErrors(myLibDirWithCompilationErrors)

        val result = deployNewLibraryVersion(
            dir = myLibDirWithCompilationErrors,
            libraryId = libraryName,
            version = "0.0.2",
            description = "Library with compilation errors",
            library = libraryName,
            secretFile = dev1SecretFile
        )

        assertThat(result.stdout).isNotNull()
        assertThat(result.stdout).contains("Compilation failed for library '$libraryName'")
    }

    private fun testCreateDevelopers() {
        val createDev1Result = createDeveloper("dev1", dev1SecretFile)
        assertThat(createDev1Result.stdout).contains("Developer[dev1] created successfully")

        val createDev2Result = createDeveloper("dev_2", dev2SecretFile)
        assertThat(createDev2Result.stdout).contains("Developer[dev_2] created successfully")
    }

    private fun testCreateAndViewOrganization() {
        val createOrgResult = createOrganization(
            orgId = organisationId,
            name = "org1",
            description = "organization description",
            secretFile = dev1SecretFile
        )

        assertThat(createOrgResult.stdout)
            .contains("Organization 'org1' created successfully with ID: com.example")

        val viewOrgResult = viewOrganization(organisationId)

        assertThat(viewOrgResult.stdout).all {
            contains(organisationId)
            contains("org1")
            contains("organization description")
        }
    }

    private fun testCreateLibrary() {
        setupLibraryFiles(myLibDir)
        val createLibraryResult = createLibrary(
            dir = myLibDir,
            library = libraryName,
            name = libraryName,
            description = "My test custom library",
            version = "0.0.1",
            organization = organisationId,
            secretFile = dev1SecretFile,
        )

        assertThat(createLibraryResult.stdout).all {
            contains("Uploading files from: ")
            // only upload files under module: lib.$libA from `chromia.yml file - 1 file, skips files in other modules
            contains("Found 1 files to include in library")
            contains("Library created successfully in transaction TxRid")
        }
    }

    private fun testInstallLibrary() {
        val installTestDir = rellDappDir.resolve("install_test_project")
        installTestDir.toFile().mkdirs()

        val chromiaYmlContent = """
            blockchains:
              my_chain:
                module: main
            compile:
              rellVersion: 0.14.2
            database:
              schema: schema_my_rell_dapp
            libs:
              ft4:
                registry: https://gitlab.com/chromaway/ft4-lib.git
                path: rell/src/lib/ft4
                tagOrBranch: v1.0.0r
                rid: x"FA487D75E63B6B58381F8D71E0700E69BEDEAD3A57D1E6C1A9ABB149FAC9E65F"
                insecure: true
              $organisationId.$libraryName:
                version: 0.0.1
                registry: localhost
                
        """.trimIndent()

        installTestDir.resolve("chromia.yml").writeText(chromiaYmlContent)

        val srcDir = installTestDir.resolve("src")
        srcDir.toFile().mkdirs()

        val mainRellContent = """
            module;
            query hello_world() = "Hello %s!".format(my_name.name);
        """.trimIndent()

        srcDir.resolve("main.rell").writeText(mainRellContent)

        val installResult = InstallLibraryCommand().test(
            listOf(
                "-s",
                installTestDir.resolve("chromia.yml").absolutePathString(),
                "--url",
                "localhost"
            )
        )

        assertThat(installResult.stderr).isEmpty()
        assertThat(installResult.stdout).contains("Dependencies installed successfully")

        val ft4LibDir = srcDir.resolve("lib/ft4")
        assertThat(ft4LibDir.toFile().exists()).isTrue()
        assertThat(ft4LibDir.toFile().isDirectory()).isTrue()
        assertThat(ft4LibDir.toFile().listFiles()?.isNotEmpty() ?: false).isTrue()

        val myLibDir = srcDir.resolve("lib/$libraryName")
        assertThat(myLibDir.toFile().exists()).isTrue()
        assertThat(myLibDir.toFile().isDirectory()).isTrue()
        assertThat(myLibDir.toFile().listFiles()?.isNotEmpty() ?: false).isTrue()
    }

    private fun testInstallLibraryExplicitelyWithVersion() {
        val installTestDir = rellDappDir.resolve("install_test_project")
        installTestDir.toFile().mkdirs()

        val chromiaYmlContent = """
            blockchains:
              my_chain:
                module: main
            compile:
              rellVersion: 0.14.2
            database:
              schema: schema_my_rell_dapp
            libs:
              ft4:
                registry: https://gitlab.com/chromaway/ft4-lib.git
                path: rell/src/lib/ft4
                tagOrBranch: v1.0.0r
                rid: x"FA487D75E63B6B58381F8D71E0700E69BEDEAD3A57D1E6C1A9ABB149FAC9E65F"
                insecure: true
              $organisationId.$libraryName:
                version: 0.0.1
                registry: localhost
                
        """.trimIndent()

        installTestDir.resolve("chromia.yml").writeText(chromiaYmlContent)

        val srcDir = installTestDir.resolve("src")
        srcDir.toFile().mkdirs()

        val mainRellContent = """
            module;
            query hello_world() = "Hello %s!".format(my_name.name);
        """.trimIndent()

        srcDir.resolve("main.rell").writeText(mainRellContent)

        val installResult = InstallLibraryCommand().test(
            listOf(
                "$organisationId.$libraryName@0.0.1",
                "-s",
                installTestDir.resolve("chromia.yml").absolutePathString(),
                "--url",
                "localhost"
            )
        )

        assertThat(installResult.stderr).isEmpty()
        assertThat(installResult.stdout).contains("com.example.my_lib")
        assertThat(installResult.stdout).contains("version: 0.0.1")

        val ft4LibDir = srcDir.resolve("lib/ft4")
        assertThat(ft4LibDir.toFile().exists()).isTrue()
        assertThat(ft4LibDir.toFile().isDirectory()).isTrue()
        assertThat(ft4LibDir.toFile().listFiles()?.isNotEmpty() ?: false).isTrue()

        val myLibDir = srcDir.resolve("lib/$libraryName")
        assertThat(myLibDir.toFile().exists()).isTrue()
        assertThat(myLibDir.toFile().isDirectory()).isTrue()
        assertThat(myLibDir.toFile().listFiles()?.isNotEmpty() ?: false).isTrue()
    }

    private fun testAddDevToLibrary() {
        val dev2AccountId = getDeveloperAccountId(dev2SecretFile)

        val inviteResult = inviteDevToLibrary(
            libraryId = "$organisationId.$libraryName",
            developerAccountId = dev2AccountId,
            accessLevel = "admin",
            secretFile = dev1SecretFile
        )

        assertThat(inviteResult.stdout).contains("Invitation sent successfully")

        val listInvitationsResult = listDevInvitations(dev2SecretFile)

        val invitationCode = extractInvitationCodeFromOutput(listInvitationsResult.stdout)

        val acceptResult = acceptLibraryInvitation(
            invitationCode = invitationCode,
            secretFile = dev2SecretFile
        )

        assertThat(acceptResult.stdout).contains("Invitation accepted successfully")
    }

    private fun testUpdateDevPermission() {
        val dev2AccountId = getDeveloperAccountId(dev2SecretFile)

        val updateResult = updateLibraryUserPermission(
            libraryId = "$organisationId.$libraryName",
            developerAccountId = dev2AccountId,
            accessLevel = "publisher",
            secretFile = dev1SecretFile
        )

        assertThat(updateResult.stdout).contains("Developer permissions updated successfully")
        assertThat(updateResult.stdout).contains("PUBLISHER")
    }

    private fun testRemoveDevFromLibrary() {
        val dev2AccountId = getDeveloperAccountId(dev2SecretFile)

        val removeResult = removeDevFromLibrary(
            libraryId = "$organisationId.$libraryName",
            developerAccountId = dev2AccountId,
            secretFile = dev1SecretFile
        )

        assertThat(removeResult.stdout).contains("User removed from library successfully")
    }

    private fun testDeployNewLibraryVersion() {
        val moduleFile = File(myLibDir.toFile(), "src/lib/$libraryName/module.rell")
        moduleFile.writeText("module; val ZERO = 0; function newFunction() = 10;")

        val deployResult = deployNewLibraryVersion(
            dir = myLibDir,
            libraryId = "$organisationId.$libraryName",
            version = "0.0.2",
            description = "Updated library with new function",
            library = libraryName,
            secretFile = dev1SecretFile
        )

        assertThat(deployResult.stdout).contains("New library version deployed successfully")
        assertThat(deployResult.stdout).contains("0.0.2")
    }

    private fun setupKeysAndSecretFiles() {
        val cs = Secp256K1CryptoSystem()
        keyPair1 = cs.generateKeyPair()
        keyPair2 = cs.generateKeyPair()

        val keysDir = File(myLibDir.toFile(), "keys").apply { mkdirs() }

        adminSecretFile = createSecretFile(keysDir, "admin", libraryChainKeyPair)
        dev1SecretFile = createSecretFile(keysDir, "dev1", keyPair1)
        dev2SecretFile = createSecretFile(keysDir, "dev2", keyPair2)
    }

    private fun createSecretFile(directory: File, filename: String, keyPair: KeyPair): File {
        return File(directory, filename).apply {
            writeText(
                """
                privkey=${keyPair.privKey.data.toHex()}
                pubkey=${keyPair.pubKey.data.toHex()}
                """.trimIndent()
            )
        }
    }

    fun createDeveloper(name: String, secretFile: File) =
        CreateDeveloperCommand().test(
            listOf(
                "--name",
                name,
                "--secret",
                secretFile.absolutePath,
                "--url",
                "localhost",
                "--strategy",
                "open"
            )
        )

    fun createOrganization(orgId: String, name: String, description: String, secretFile: File) =
        CreateOrganizationCommand().test(
            listOf(
                "--org-id", orgId,
                "--name", name,
                "--description", description,
                "--secret", secretFile.absolutePath,
                "--url", "localhost",
            )
        )

    fun viewOrganization(orgId: String) =
        ViewOrganizationCommand().test(
            listOf(
                "--org-id",
                orgId,
                "--url",
                "localhost"
            )
        )

    fun createLibrary(
        dir: Path,
        library: String,
        name: String,
        description: String,
        version: String,
        organization: String,
        secretFile: File
    ) = CreateLibraryCommand().test(
        listOf(
            "--library", library,
            "--name", name,
            "--description", description,
            "--version", version,
            "--organization", organization,
            "--secret", secretFile.absolutePath,
            "--url", "localhost",
            "-s", "${myLibDir.absolutePathString()}/chromia.yml"
        )
    )

    fun setupLibraryFiles(dir: Path) {
        File(dir.toFile(), "chromia.yml").writeText(
            """
                blockchains:
                  $libraryName:
                    module: lib.$libraryName
                    type: library
                compile:
                  rellVersion: 0.14.5
                database:
                  schema: schema_my_lib
            """.trimIndent()
        )

        File(dir.toFile(), "src/lib/$libraryName/module.rell").apply {
            parentFile.mkdirs()
            writeText("module; val PI = 1; function side_effect(){}")
        }

        File(dir.toFile(), "src/module.rell").apply {
            parentFile.mkdirs()
            writeText("module; query get_age() = 10;")
        }

        File(dir.toFile(), "src/utils.rell").apply {
            parentFile.mkdirs()
            writeText("module; function add(a: integer, b: integer) = a + b;")
        }
    }

    fun setupLibraryFilesWithCompilationErrors(dir: Path) {
        File(dir.toFile(), "chromia.yml").writeText(
            """
                blockchains:
                  $libraryName:
                    module: lib.$libraryName
                    type: library
                compile:
                  rellVersion: 0.14.5
                database:
                  schema: schema_my_lib
            """.trimIndent()
        )

        File(dir.toFile(), "src/lib/$libraryName/module.rell").apply {
            parentFile.mkdirs()
            writeText(
                """
                module;
                
                val INVALID_SYNTAX = "missing semicolon"
                function undefined_function_call() = nonexistent_function();
                function DUPLICATE_FUNCTION() = 1;
                function DUPLICATE_FUNCTION() = 2; 
                """.trimIndent()
            )
        }

        File(dir.toFile(), "src/module.rell").apply {
            parentFile.mkdirs()
            writeText("module; query get_age() = 10;")
        }

        File(dir.toFile(), "src/utils.rell").apply {
            parentFile.mkdirs()
            writeText("module test_lib.utils; function add(a: integer, b: integer) = a + b;")
        }
    }

    fun inviteDevToLibrary(
        libraryId: String,
        developerAccountId: String,
        accessLevel: String,
        secretFile: File
    ) = InviteLibraryUserCommand().test(
        listOf(
            "--library-id", libraryId,
            "--developer-id", developerAccountId,
            "--access-level", accessLevel,
            "--secret", secretFile.absolutePath,
            "--url", "localhost"
        )
    )

    fun listDevInvitations(
        secretFile: File
    ) = ListLibraryInvitationsCommand().test(
        listOf(
            "--secret",
            secretFile.absolutePath,
            "--url",
            "localhost"
        )
    )

    fun acceptLibraryInvitation(
        invitationCode: String,
        secretFile: File
    ) = AcceptLibraryInvitationCommand().test(
        listOf(
            invitationCode,
            "--secret",
            secretFile.absolutePath,
            "--url",
            "localhost"
        )
    )

    fun updateLibraryUserPermission(
        libraryId: String,
        developerAccountId: String,
        accessLevel: String,
        secretFile: File
    ) = UpdateLibraryUserPermissionCommand().test(
        listOf(
            libraryId,
            developerAccountId,
            "--access-level",
            accessLevel,
            "--secret",
            secretFile.absolutePath,
            "--url",
            "localhost"
        )
    )

    fun removeDevFromLibrary(
        libraryId: String,
        developerAccountId: String,
        secretFile: File
    ) = RemoveLibraryUserCommand().test(
        listOf(
            "--library-id",
            libraryId,
            "--dev-id",
            developerAccountId,
            "--secret",
            secretFile.absolutePath,
            "--url",
            "localhost",
        )
    )

    fun deployNewLibraryVersion(
        dir: Path,
        libraryId: String,
        version: String,
        description: String,
        library: String,
        secretFile: File
    ) = DeployNewLibraryVersionCommand().test(
        listOf(
            "--id", libraryId,
            "--version", version,
            "--description", description,
            "--library", library,
            "--secret", secretFile.absolutePath,
            "--url", "localhost",
            "-s", "${dir.absolutePathString()}/chromia.yml"
        )
    )

    fun getDeveloperAccountId(secretFile: File): String {
        // This command calls authorizeFtAuthOperation to returns the account ID
        val command = object : AbstractLibraryCommand("get-account-id", "Get account ID") {
            override fun run() {
                val (accountId, _) = authorizeFtAuthOperation(GET_DEVELOPER)
                echo(accountId.toHex())
            }
        }

        val result = command.test(
            listOf(
                "--secret",
                secretFile.absolutePath,
                "--url",
                "localhost",
            )
        )
        return result.stdout.trim()
    }

    fun extractInvitationCodeFromOutput(output: String): String {
        val lines = output.lines()

        val invitationLine = lines.firstOrNull {
            it.contains("• Code:") && it.contains("Library: $organisationId.$libraryName")
        } ?: fail { "Could not find invitation in output: $output" }

        val codePattern = Regex("Code: ([^|]+)")
        val match = codePattern.find(invitationLine)

        return match!!.groupValues[1].trim()
    }

    private fun cloneLibraryChain() {
        val repositoryCloner = GitRepositoryCloner(quiet = true)

        try {
            repositoryCloner.clone(
                registry = "https://bitbucket.org/chromawallet/library-chain.git",
                target = libraryChainDir,
                tagOrBranch = "main",
            )
        } catch (e: Exception) {
            fail { "Failed to clone library-chain repository: ${e.message}" }
        }
    }

    private fun deployLibraryChain(): BlockchainRid {
        val chromiaYmlPath = libraryChainDir.resolve("chromia-test.yml")

        val updatedConfig = chromiaYmlPath.readText().replace(
            "      query_cache_ttl_seconds: 5",
            """
            |      signers:
            |        - x"${libraryChainKeyPair.pubKey.data.toHex()}"
            |      query_cache_ttl_seconds: 5
            """.trimMargin()
        )

        chromiaYmlPath.writeText(updatedConfig)

        InstallLibraryCommand().parse(listOf("-s", chromiaYmlPath.absolutePathString()))
        BuildCommand().parse(listOf("-s", chromiaYmlPath.absolutePathString()))

        val buildDir = libraryChainDir.resolve("build")
        val configFiles = buildDir.toFile().listFiles { _, name -> name.endsWith(".xml") }

        val configPath = configFiles!!.first().absolutePath
        val gtvConfig = GtvMLParser.parseGtvML(File(configPath).readText())

        val setup = SystemSetupFactory.buildSystemSetup(listOf(BlockchainSetup.buildFromGtv(0, gtvConfig)))
        setup.needRestApi = true
        createNodesFromSystemSetup(setup, true)

        return setup.blockchainMap[0]?.rid!!
    }
}
