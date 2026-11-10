package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.animateChevronRotation
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_commit_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_copy_link
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_issue_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_more_links
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_new_pull_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_open_commit
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_open_issue
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_open_new_pull
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_open_pull
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_open_repo
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_open_run
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_pull_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_repo_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_github_run_description
import org.jetbrains.compose.resources.stringResource

/**
 * "Open #157" on a tool row: a tap opens the most relevant GitHub link in the browser, a long
 * press copies it, and the chevron lists the other links the tool printed.
 */
@Composable
internal fun GitHubLinkActions(
    links: List<GitHubLink>,
    partId: String,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = links.firstOrNull() ?: return
    val c = OcTheme.colors
    val uriHandler = LocalUriHandler.current
    val description = gitHubLinkDescription(primary)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(OcRadius.sm))
                .combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = stringResource(Res.string.chat_github_copy_link),
                    onLongClick = { onCopy(primary.url) },
                    onClick = { uriHandler.openSafely(primary.url) },
                )
                .semantics { contentDescription = description }
                .heightIn(min = 36.dp)
                .padding(horizontal = OcSpacing.xs)
                .testTag("tool_row_github_open_$partId"),
        ) {
            Text(
                text = gitHubLinkLabel(primary),
                style = MaterialTheme.typography.labelMedium,
                color = c.success.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
        }
        val others = links.drop(1)
        if (others.isNotEmpty()) GitHubLinksMenu(others, partId, onCopy)
    }
}

@Composable
private fun GitHubLinksMenu(links: List<GitHubLink>, partId: String, onCopy: (String) -> Unit) {
    val c = OcTheme.colors
    val uriHandler = LocalUriHandler.current
    var open by remember { mutableStateOf(false) }
    Box {
        OcIconButton(
            icon = Icons.Outlined.ExpandMore,
            contentDescription = stringResource(Res.string.chat_github_more_links),
            onClick = { open = true },
            tint = c.iconMuted,
            iconSize = 16.dp,
            // Turns up while its menu is open, like the other disclosure chevrons.
            modifier = Modifier.size(32.dp).rotate(animateChevronRotation(open)).testTag("tool_row_github_menu_$partId"),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.bgBase, modifier = Modifier.exposeTestTags()) {
            links.forEachIndexed { index, link ->
                val description = gitHubLinkDescription(link)
                DropdownMenuItem(
                    text = {
                        Text(
                            gitHubLinkLabel(link),
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textBase,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = {
                        OcIconButton(
                            icon = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(Res.string.chat_github_copy_link),
                            onClick = {
                                open = false
                                onCopy(link.url)
                            },
                            tint = c.iconMuted,
                            iconSize = 16.dp,
                            modifier = Modifier.testTag("tool_row_github_copy_${partId}_$index"),
                        )
                    },
                    onClick = {
                        open = false
                        uriHandler.openSafely(link.url)
                    },
                    modifier = Modifier
                        .semantics { contentDescription = description }
                        .testTag("tool_row_github_link_${partId}_$index"),
                )
            }
        }
    }
}

/** Opens [url] with the platform browser; a device without one leaves the row as it is. */
private fun UriHandler.openSafely(url: String) {
    runCatching { openUri(url) }
}

@Composable
private fun gitHubLinkLabel(link: GitHubLink): String = when (link.kind) {
    GitHubLink.Kind.Issue -> stringResource(Res.string.chat_github_open_issue, link.ref)
    GitHubLink.Kind.PullRequest -> stringResource(Res.string.chat_github_open_pull, link.ref)
    GitHubLink.Kind.NewPullRequest -> stringResource(Res.string.chat_github_open_new_pull)
    GitHubLink.Kind.Run -> stringResource(Res.string.chat_github_open_run, link.ref)
    GitHubLink.Kind.Commit -> stringResource(Res.string.chat_github_open_commit, link.ref)
    GitHubLink.Kind.Compare, GitHubLink.Kind.File -> stringResource(Res.string.chat_github_open_repo, link.ref)
}

@Composable
private fun gitHubLinkDescription(link: GitHubLink): String = when (link.kind) {
    GitHubLink.Kind.Issue -> stringResource(Res.string.chat_github_issue_description, link.ref)
    GitHubLink.Kind.PullRequest -> stringResource(Res.string.chat_github_pull_description, link.ref)
    GitHubLink.Kind.NewPullRequest -> stringResource(Res.string.chat_github_new_pull_description)
    GitHubLink.Kind.Run -> stringResource(Res.string.chat_github_run_description, link.ref)
    GitHubLink.Kind.Commit -> stringResource(Res.string.chat_github_commit_description, link.ref)
    GitHubLink.Kind.Compare, GitHubLink.Kind.File -> stringResource(Res.string.chat_github_repo_description, link.ref)
}
