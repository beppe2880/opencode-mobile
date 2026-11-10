package io.github.beppe2880.opencodemobile.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerPathsTest {

    @Test
    fun posixPathsNormalizeAndClimbToRoot() {
        assertEquals("/home/dev", ServerPaths.normalize("/home//dev/./"))
        assertEquals("/", ServerPaths.normalize("///"))
        assertEquals("/", ServerPaths.normalize("/../.."))
        assertEquals("/home", ServerPaths.parent("/home/dev/"))
        assertEquals("/", ServerPaths.parent("/home"))
        assertNull(ServerPaths.parent("/"))
        assertEquals("/", ServerPaths.root("/home/dev"))
        assertEquals("dev", ServerPaths.name("/home/dev/"))
        assertNull(ServerPaths.name("/"))
        assertEquals("/home/dev", ServerPaths.child("/home", "dev"))
        assertEquals("/home", ServerPaths.child("/", "home"))
        assertEquals('/', ServerPaths.separator("/home"))
        assertFalse(ServerPaths.isWindows("/home/dev"))
    }

    @Test
    fun posixSegmentsCarryTheirFullPaths() {
        assertEquals(listOf("a" to "/a", "b" to "/a/b"), ServerPaths.segments("/a/b/"))
        assertEquals(emptyList(), ServerPaths.segments("/"))
        assertEquals(emptyList(), ServerPaths.segments(""))
    }

    @Test
    fun posixEntriesResolveAgainstTheServerFolder() {
        val base = "/tmp/opencode/fs-scratch"
        assertEquals("/tmp/opencode/fs-scratch/src", ServerPaths.resolve(base, "src/"))
        assertEquals("/home/beppe", ServerPaths.resolve(base, "../../../home/beppe/"))
        assertEquals("/tmp", ServerPaths.resolve(base, "../../"))
        assertEquals("/", ServerPaths.resolve(base, "../../../../../"))
        assertEquals("/home", ServerPaths.resolve(base, "/home/"))
        assertEquals(base, ServerPaths.resolve(base, ""))
        assertEquals(base, ServerPaths.resolve(base, "./"))
    }

    @Test
    fun relativePathsAreNotAbsolute() {
        assertTrue(ServerPaths.isAbsolute("/srv"))
        assertTrue(ServerPaths.isAbsolute("C:\\work"))
        assertFalse(ServerPaths.isAbsolute("projects/app"))
        assertFalse(ServerPaths.isAbsolute(""))
        assertNull(ServerPaths.parent("app"))
        assertEquals("projects", ServerPaths.parent("projects/app"))
        assertEquals("..", ServerPaths.name("../"))
    }

    @Test
    fun windowsDriveRootsInEveryForm() {
        assertEquals("C:\\", ServerPaths.normalize("C:\\"))
        assertEquals("C:\\", ServerPaths.normalize("C:/"))
        assertEquals("C:\\", ServerPaths.normalize("C:"))
        assertEquals("C:\\", ServerPaths.root("C:\\Users\\dev"))
        assertNull(ServerPaths.parent("C:\\"))
        assertNull(ServerPaths.parent("C:"))
        assertNull(ServerPaths.name("C:\\"))
        assertTrue(ServerPaths.isWindows("C:/Users"))
        assertEquals('\\', ServerPaths.separator("C:/Users"))
    }

    @Test
    fun windowsParentsChildrenAndNames() {
        assertEquals("C:\\", ServerPaths.parent("C:\\Users"))
        assertEquals("C:\\Users", ServerPaths.parent("C:\\Users\\dev\\"))
        assertEquals("dev", ServerPaths.name("C:\\Users\\dev\\"))
        assertEquals("C:\\Users\\dev", ServerPaths.child("C:\\Users", "dev"))
        assertEquals("C:\\src", ServerPaths.child("C:\\", "src"))
        assertEquals(
            listOf("Users" to "C:\\Users", "dev" to "C:\\Users\\dev"),
            ServerPaths.segments("C:\\Users\\dev"),
        )
    }

    @Test
    fun mixedSeparatorsBecomeBackslashes() {
        assertEquals("C:\\Users\\dev\\proj\\src", ServerPaths.normalize("C:\\Users\\dev\\proj/src\\"))
        assertEquals("C:\\Users\\dev\\proj", ServerPaths.normalize("C:/Users/dev/proj"))
        assertEquals("C:\\Users\\dev\\proj\\src", ServerPaths.resolve("C:/Users/dev/proj", "src\\"))
    }

    @Test
    fun windowsEntriesResolveLikeTheServer() {
        val base = "C:\\Users\\dev\\proj"
        assertEquals("C:\\Users\\dev\\proj\\src", ServerPaths.resolve(base, "src\\"))
        assertEquals("C:\\home", ServerPaths.resolve(base, "/home"))
        assertEquals("C:\\", ServerPaths.resolve(base, "/"))
        assertEquals("C:\\src", ServerPaths.resolve(base, "..\\..\\..\\src\\"))
        assertEquals("C:\\", ServerPaths.resolve(base, "..\\..\\..\\..\\..\\"))
        assertEquals("C:\\Users\\dev", ServerPaths.resolve(base, "C:\\Users\\dev"))
        assertEquals("C:\\Users\\dev\\src", ServerPaths.resolve(base, "..\\src\\"))
        assertEquals("D:\\work", ServerPaths.resolve(base, "D:\\work"))
        assertEquals("D:\\work\\src", ServerPaths.resolve(base, "D:\\work\\src\\"))
        assertEquals(base, ServerPaths.resolve(base, ".\\"))
        assertEquals(base, ServerPaths.resolve(base, ""))
    }

    @Test
    fun uncPathsKeepTheirShareAsRoot() {
        assertEquals("\\\\server\\share\\", ServerPaths.root("\\\\server\\share\\dir"))
        assertEquals("\\\\server\\share\\", ServerPaths.parent("\\\\server\\share\\dir"))
        assertNull(ServerPaths.parent("\\\\server\\share"))
        assertNull(ServerPaths.parent("\\\\"))
        assertEquals("\\\\server\\share\\dir", ServerPaths.resolve("C:\\proj", "\\\\server\\share\\dir\\"))
        assertEquals("\\\\server\\share\\", ServerPaths.resolve("\\\\server\\share\\dir", "..\\..\\.."))
    }
}
