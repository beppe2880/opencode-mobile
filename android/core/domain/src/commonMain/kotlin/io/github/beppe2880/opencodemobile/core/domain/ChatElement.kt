package io.github.beppe2880.opencodemobile.core.domain

/**
 * A part of the chat whose look can be changed: the chat preview in Settings reports which one
 * was tapped, and Settings offers the options of that part.
 */
enum class ChatElement {
    /** The title bar and the line under the title (folder, branch, changed files). */
    Header,

    /** Prompts and answers: their text size. */
    Messages,

    /** Thinking rows. */
    Thoughts,

    /** Tool rows. */
    Tools,

    /** Thinking and tool rows grouped into one summary row. */
    Activity,

    /** The line under an answer: agent, model, time and cost. */
    Footer,

    /** Permission requests and questions above the composer. */
    PendingCard,

    /** The prompt field with the agent and model chips. */
    Composer,
}
