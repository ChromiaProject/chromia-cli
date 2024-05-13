package com.chromia.cli.util

import com.chromia.build.tools.lib.LibraryInstallException
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.model.RellLibraryModel
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.name
import kotlin.io.path.writeText
import net.postchain.common.hexStringToWrappedByteArray

class TestRepositoryCloner : RepositoryCloner {
    val repos = listOf(foo, bar, filter).associateBy { it.model.registry }
    override fun clone(registry: String, target: Path, tagOrBranch: String?) {
        repos[registry]?.let { repo ->
            repo.files.forEach { createFile(target, it) }
        } ?: throw LibraryInstallException("Repository does not exist")
    }

    fun createFile(dir: Path, file: Path) {
        dir.resolve(file).createParentDirectories().writeText("module; //${file.name}")
    }

    companion object {
        val foo = TestRepo(
                RellLibraryModel("http://foo.com", path = "path/to/foo", rid = "046AC0AE25375C1CF7A819D0649F6373A49B53265937D6E9D6CC6CE4317B0EB1".hexStringToWrappedByteArray()),
                Path("path/to/foo/a.rell"), Path("path/to/foo/nested/b.rell"), Path("not/included/c.rell")
        )
        val bar = TestRepo(
                RellLibraryModel("http://bar.com", path = "some/path", rid = "E0013E89E674D21797B2A8F7522E87E085418613AA99EF042AD95B08A0D314C9".hexStringToWrappedByteArray()),
                Path("some/path/d.rell")
        )
        val filter = TestRepo(
                RellLibraryModel("http://filter.com", path = "path/lib/filter", rid = "046AC0AE25375C1CF7A819D0649F6373A49B53265937D6E9D6CC6CE4317B0EB1".hexStringToWrappedByteArray()),
                Path("path/lib/filter/a.rell"),
                Path("path/lib/filter/a.yml"),
                Path("path/lib/filter/nested/b.rell"),
                Path("path/lib/filter/nested/b.yml"),
        )
    }
    class TestRepo(val model: RellLibraryModel, vararg val files: Path)
}
