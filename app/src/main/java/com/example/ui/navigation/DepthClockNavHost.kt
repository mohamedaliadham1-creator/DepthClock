package com.example.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.screens.CreationsScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageImportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.CreationsViewModel
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.ImportViewModel
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun DepthClockNavHost() {
  val navController = rememberNavController()

  NavHost(
    navController = navController,
    startDestination = HomeRoute,
    enterTransition = { fadeIn() },
    exitTransition = { fadeOut() },
  ) {
    composable<HomeRoute> {
      val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
      HomeScreen(
        viewModel = vm,
        onCreateWallpaper = { navController.navigate(ImportRoute) },
        onOpenProject = { projectId -> navController.navigate(EditorRoute(projectId)) },
        onOpenCreations = { navController.navigate(CreationsRoute) },
        onOpenSettings = { navController.navigate(SettingsRoute) },
      )
    }

    composable<ImportRoute> {
      val vm: ImportViewModel = viewModel(factory = ImportViewModel.Factory)
      ImageImportScreen(
        viewModel = vm,
        onBack = { navController.popBackStack() },
        onProjectCreated = { projectId ->
          navController.navigate(EditorRoute(projectId)) {
            popUpTo(ImportRoute) { inclusive = true }
          }
        },
      )
    }

    composable<EditorRoute> { backStackEntry ->
      val route = backStackEntry.toRoute<EditorRoute>()
      val vm: EditorViewModel = viewModel(factory = EditorViewModel.factory(route.projectId))
      EditorScreen(
        viewModel = vm,
        onBack = { navController.popBackStack() },
      )
    }

    composable<CreationsRoute> {
      val vm: CreationsViewModel = viewModel(factory = CreationsViewModel.Factory)
      CreationsScreen(
        viewModel = vm,
        onBack = { navController.popBackStack() },
        onOpenProject = { projectId -> navController.navigate(EditorRoute(projectId)) },
        onCreateNew = { navController.navigate(ImportRoute) },
      )
    }

    composable<SettingsRoute> {
      val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
      SettingsScreen(
        viewModel = vm,
        onBack = { navController.popBackStack() },
      )
    }
  }
}
