package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object ImportRoute

@Serializable
data class EditorRoute(val projectId: String)

@Serializable
object CreationsRoute

@Serializable
object SettingsRoute
