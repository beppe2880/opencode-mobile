package io.github.beppe2880.opencodemobile.core.ui.text

import androidx.compose.runtime.Composable
import io.github.beppe2880.opencodemobile.core.domain.RunningWork
import io.github.beppe2880.opencodemobile.core.ui.resources.Res
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_running_agents
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_running_commands
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_working
import org.jetbrains.compose.resources.stringResource

@Composable
fun runningSummary(work: RunningWork, executing: Boolean = false): String = listOfNotNull(
    if (executing) stringResource(Res.string.oc_working) else null,
    if (work.agents.isNotEmpty()) stringResource(Res.string.oc_running_agents, work.agents.size) else null,
    if (work.commands.isNotEmpty()) stringResource(Res.string.oc_running_commands, work.commands.size) else null,
).joinToString(" · ")
