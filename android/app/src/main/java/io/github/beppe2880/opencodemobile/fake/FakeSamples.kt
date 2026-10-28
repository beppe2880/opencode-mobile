package io.github.beppe2880.opencodemobile.fake

import io.github.beppe2880.opencodemobile.core.domain.FileDiff
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.FormField
import io.github.beppe2880.opencodemobile.core.domain.FormOption

/** Sample diffs, questions and shell output shared by the demo and debug builds. */
internal object FakeSamples {

    val checkoutForm = Form(
        id = "form_1",
        sessionId = "ses_3",
        title = "Checkout redesign",
        fields = listOf(
            FormField.Text(
                key = "layout",
                title = "Which checkout layout should I plan for?",
                required = true,
                options = listOf(
                    FormOption("single", "Single page", "Address, shipping and payment on one screen"),
                    FormOption("steps", "Three steps", "Separate address, shipping and payment steps"),
                    FormOption("express", "Express first", "Apple Pay / Google Pay on top, form below"),
                ),
                custom = true,
            ),
            FormField.Toggle(key = "guest", title = "Allow guest checkout?", default = true),
        ),
    )

    fun shellOutput(command: String): String = when (command.trim().substringBefore(' ')) {
        "git" ->
            "On branch main\nYour branch is up to date with 'origin/main'.\n\n" +
                "Changes not staged for commit:\n\tmodified:   src/cart/Cart.kt\n\tmodified:   src/cart/Coupon.kt"
        "ls" -> "README.md\nbuild.gradle.kts\nsettings.gradle.kts\nsrc"
        "pwd" -> "/home/user/code/shop"
        else -> "ok"
    }

    /** Counts come from the patch, like the server does. */
    fun diff(path: String, status: String, patch: String): FileDiff {
        val lines = patch.lines().filterNot { it.startsWith("@@") }
        return FileDiff(path, status, lines.count { it.startsWith("+") }, lines.count { it.startsWith("-") }, patch)
    }

    fun diffs(directory: String): List<FileDiff> = when {
        directory.endsWith("dotfiles") -> listOf(aliases, zshrc)
        directory.endsWith("blog") -> listOf(feedTest, theme)
        else -> listOf(coupon, cart, couponTest)
    }

    private val coupon = diff(
        path = "src/cart/Coupon.kt",
        status = "added",
        patch = """
            @@ -0,0 +1,18 @@
            +package shop.cart
            +
            +/** A discount code; [percent] is applied to the cart subtotal. */
            +data class Coupon(
            +    val code: String,
            +    val percent: Int,
            +    val expiresAt: Long,
            +) {
            +    init {
            +        require(percent in 1..100) { "percent must be 1..100" }
            +    }
            +
            +    fun isValid(now: Long): Boolean = now < expiresAt
            +
            +    fun apply(subtotal: Long): Long = subtotal - subtotal * percent / 100
            +}
            +
            +fun String.normalizedCode(): String = trim().uppercase()
        """.trimIndent(),
    )

    private val cart = diff(
        path = "src/cart/Cart.kt",
        status = "modified",
        patch = """
            @@ -12,14 +12,20 @@ class Cart(private val items: MutableList<Item> = mutableListOf()) {
                 val subtotal: Long
                     get() = items.sumOf { it.price * it.quantity }
             
            -    val total: Long
            -        get() = subtotal + shipping
            +    var coupon: Coupon? = null
            +        private set
            +
            +    val total: Long
            +        get() = (coupon?.apply(subtotal) ?: subtotal) + shipping
            +
            +    fun applyCoupon(coupon: Coupon, now: Long = System.currentTimeMillis()): Boolean {
            +        if (!coupon.isValid(now)) return false
            +        this.coupon = coupon
            +        return true
            +    }
             
                 fun add(item: Item) {
            -        items += item
            +        items.firstOrNull { it.id == item.id }?.let { it.quantity += item.quantity } ?: items.add(item)
                 }
             
            -    fun clear() = items.clear()
            +    fun clear() { items.clear(); coupon = null }
             }
        """.trimIndent(),
    )

    private val couponTest = diff(
        path = "src/test/cart/CouponTest.kt",
        status = "added",
        patch = """
            @@ -0,0 +1,14 @@
            +package shop.cart
            +
            +import kotlin.test.Test
            +import kotlin.test.assertEquals
            +import kotlin.test.assertFalse
            +
            +class CouponTest {
            +    @Test fun `ten percent off`() = assertEquals(900, Coupon("TEN", 10, Long.MAX_VALUE).apply(1_000))
            +
            +    @Test fun `expired coupons are rejected`() {
            +        val cart = Cart()
            +        assertFalse(cart.applyCoupon(Coupon("OLD", 10, expiresAt = 0), now = 1))
            +    }
            +}
        """.trimIndent(),
    )

    val aliases = diff(
        path = ".config/zsh/aliases.zsh",
        status = "modified",
        patch = """
            @@ -1,10 +1,11 @@
             # Navigation
            -alias ..='cd ..'
            -alias ...='cd ../..'
            +alias ..='cd ..' ...='cd ../..' ....='cd ../../..'
             
             # Git
            -alias g='git'
            -alias gco='git checkout'
            +alias g='git'
            +alias gco='git switch'
            +alias gcb='git switch -c'
            +alias glo='git log --oneline --graph -20'
             
            -alias ll='ls -la'
            +alias ll='eza -la --git'
        """.trimIndent(),
    )

    val zshrc = diff(
        path = ".zshrc",
        status = "modified",
        patch = """
            @@ -20,3 +20,3 @@
             export EDITOR=nvim
            -source ~/.aliases
            +source ~/.config/zsh/aliases.zsh
             eval "${'$'}(starship init zsh)"
        """.trimIndent(),
    )

    private val feedTest = diff(
        path = "test/feed.test.ts",
        status = "modified",
        patch = """
            @@ -8,8 +8,10 @@ describe("rss feed", () => {
               it("lists posts newest first", async () => {
            -    const feed = await buildFeed()
            -    expect(feed.items[0].date).toBe("2026-10-01")
            +    vi.setSystemTime(new Date("2026-10-15T12:00:00Z"))
            +    const feed = await buildFeed({ posts: fixtures })
            +    expect(feed.items.map((i) => i.slug)).toEqual(["kotlin-flows", "nix-shells"])
            +    vi.useRealTimers()
               })
             })
        """.trimIndent(),
    )

    private val theme = diff(
        path = "src/styles/theme.css",
        status = "modified",
        patch = """
            @@ -1,4 +1,10 @@
             :root { --bg: #ffffff; --fg: #1a1a1a; }
            +
            +@media (prefers-color-scheme: dark) {
            +  :root { --bg: #121212; --fg: #e6e6e6; }
            +  img { filter: brightness(.9); }
            +  code { background: #1e1e1e; }
            +}
        """.trimIndent(),
    )
}
