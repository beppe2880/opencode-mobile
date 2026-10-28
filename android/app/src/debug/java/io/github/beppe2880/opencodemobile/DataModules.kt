package io.github.beppe2880.opencodemobile

import io.github.beppe2880.opencodemobile.core.data.di.dataModule
import org.koin.core.module.Module

/** Background services always use real repositories; the demo has an isolated UI container. */
fun dataModules(): List<Module> = listOf(dataModule)
