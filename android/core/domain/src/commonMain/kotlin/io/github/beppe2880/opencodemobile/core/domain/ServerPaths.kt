package io.github.beppe2880.opencodemobile.core.domain

/**
 * Folder paths on the server's machine, which may run Windows or a POSIX system. A drive letter
 * or a backslash marks a Windows path: `/` and `\` both separate its folders and results use `\`
 * (`C:\Users\dev`, root `C:\`; UNC roots `\\server\share\` best effort). Other paths are POSIX
 * (`/home/dev`, root `/`). Results never end with a separator except at a root.
 */
object ServerPaths {
    fun isWindows(path: String): Boolean = hasDrive(path) || '\\' in path

    fun separator(path: String): Char = if (isWindows(path)) '\\' else '/'

    /** Whether [path] starts at a root, so it does not depend on the server's folder. */
    fun isAbsolute(path: String): Boolean = parse(path).root != null

    /** The root [path] starts at (`/`, `C:\`), or null for a relative path. */
    fun root(path: String): String? = parse(path).root

    fun normalize(path: String): String = parse(path).format()

    /** The folder containing [path]; null at a root and for a relative single folder. */
    fun parent(path: String): String? {
        val parsed = parse(path)
        if (parsed.segments.isEmpty() || (parsed.root == null && parsed.segments.size == 1)) return null
        return parsed.copy(segments = parsed.segments.dropLast(1)).format()
    }

    /** The last folder of [path]; null at a root or for an empty path. */
    fun name(path: String): String? = parse(path).segments.lastOrNull()

    fun child(parent: String, name: String): String {
        val parsed = parse(parent)
        return parsed.copy(segments = collapse(parsed.root, parsed.segments + split(name, parsed.windows))).format()
    }

    /** Each folder of [path] after its root with the full path up to it: `/a/b` -> [(a, /a), (b, /a/b)]. */
    fun segments(path: String): List<Pair<String, String>> {
        val parsed = parse(path)
        return parsed.segments.mapIndexed { i, name -> name to parsed.copy(segments = parsed.segments.take(i + 1)).format() }
    }

    /**
     * Resolves [path] against the folder [base] like the server does: an absolute [path] stands
     * on its own (also on another drive), a rooted one without a drive gets [base]'s drive, and a
     * relative one is appended to [base]. `..` never climbs above the root.
     */
    fun resolve(base: String, path: String): String {
        val windows = isWindows(base) || (!isAbsolute(base) && isWindows(path))
        val from = parse(base, windows)
        val target = parse(path, windows)
        return when {
            target.root == null -> from.copy(segments = collapse(from.root, from.segments + target.segments))
            target.root == WINDOWS_ROOTED && from.root != null && hasDrive(from.root) -> target.copy(root = from.root)
            else -> target
        }.format()
    }

    private data class Parsed(val root: String?, val segments: List<String>, val windows: Boolean) {
        fun format(): String {
            val joined = segments.joinToString(if (windows) "\\" else "/")
            return if (root == null) joined else root + joined
        }
    }

    private fun parse(path: String, windows: Boolean = isWindows(path)): Parsed {
        val (root, rest) = when {
            !windows -> if (path.startsWith('/')) "/" to path.substring(1) else null to path
            hasDrive(path) -> "${path[0]}:\\" to path.substring(2)
            path.length >= 2 && path[0].isSeparator() && path[1].isSeparator() -> {
                val parts = split(path.substring(2), windows = true).filter { it.isNotEmpty() }
                "\\\\" + parts.take(2).joinToString("") { "$it\\" } to parts.drop(2).joinToString("\\")
            }
            path.startsWith('/') || path.startsWith('\\') -> WINDOWS_ROOTED to path.substring(1)
            else -> null to path
        }
        return Parsed(root, collapse(root, split(rest, windows)), windows)
    }

    private fun split(path: String, windows: Boolean): List<String> =
        if (windows) path.split('/', '\\') else path.split('/')

    private fun collapse(root: String?, parts: List<String>): List<String> {
        val out = mutableListOf<String>()
        for (part in parts) {
            when {
                part.isEmpty() || part == "." -> Unit
                part != ".." -> out += part
                out.isNotEmpty() && out.last() != ".." -> out.removeAt(out.lastIndex)
                root == null -> out += part
            }
        }
        return out
    }

    private fun hasDrive(path: String): Boolean =
        path.length >= 2 && path[1] == ':' && (path[0] in 'A'..'Z' || path[0] in 'a'..'z') &&
            (path.length == 2 || path[2].isSeparator())

    private fun Char.isSeparator(): Boolean = this == '/' || this == '\\'

    /** A Windows path rooted at the current drive (`\Users`). */
    private const val WINDOWS_ROOTED = "\\"
}
