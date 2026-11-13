package io.github.beppe2880.opencodemobile.core.ui.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mikepenz.markdown.model.ReferenceLinkHandlerImpl
import com.mikepenz.markdown.model.State
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/** The parse of a message whose formulas were replaced by placeholders, and those [formulas]. */
internal class ParsedMarkdown(val state: State, val formulas: List<MathFormula>)

/**
 * Parses markdown once per (id, content) pair. Held above the transcript list so
 * recycled items reuse the parsed tree; only the streaming part re-parses.
 */
class MarkdownCache {
    private val parser = MarkdownParser(GFMFlavourDescriptor())

    private data class Entry(val content: String, val parsed: ParsedMarkdown)

    // Least recently used first: hits are moved to the end, the head is evicted.
    private val cache = LinkedHashMap<String, Entry>()

    internal fun parsedFor(id: String, content: String): ParsedMarkdown {
        val cached = cache.remove(id)
        if (cached != null && cached.content == content) {
            cache[id] = cached
            return cached.parsed
        }
        val math = extractMath(content)
        val state = try {
            State.Success(parser.buildMarkdownTreeFromString(math.text), math.text, linksLookedUp = false, ReferenceLinkHandlerImpl())
        } catch (error: Throwable) {
            State.Error(error, ReferenceLinkHandlerImpl())
        }
        val parsed = ParsedMarkdown(state, math.formulas)
        cache[id] = Entry(content, parsed)
        if (cache.size > MAX_ENTRIES) cache.remove(cache.keys.first())
        return parsed
    }

    private companion object {
        const val MAX_ENTRIES = 300
    }
}

@Composable
fun rememberMarkdownCache(): MarkdownCache = remember { MarkdownCache() }
