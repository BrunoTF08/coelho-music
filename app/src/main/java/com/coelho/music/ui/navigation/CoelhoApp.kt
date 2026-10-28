package com.coelho.music.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.coelho.music.ui.MusicViewModel
import com.coelho.music.ui.components.MiniPlayer
import com.coelho.music.ui.screens.*
import com.coelho.music.ui.theme.CoelhoTheme

@Composable
fun CoelhoApp(vm: MusicViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val playback by vm.playback.state.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) }
    var denied by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        granted = allowed; denied = !allowed
        if (allowed) { vm.boolean("welcomed", true) }
        vm.setForeground(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED), allowed)
    }
    DisposableEffect(owner, permission) {
        fun resume() {
            granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            vm.setForeground(true, granted)
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resume()
            if (event == Lifecycle.Event.ON_STOP) vm.setForeground(false, granted)
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resume()
        onDispose { owner.lifecycle.removeObserver(observer); vm.setForeground(false, granted) }
    }
    CoelhoTheme(settings?.dark ?: true, settings?.accent ?: 0) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (settings == null) { Box(Modifier.safeDrawingPadding()) { CircularProgressIndicator() }; return@Surface }
            if (!granted || settings?.welcomed != true) {
                WelcomeScreen(denied, onStart = { if (granted) { vm.boolean("welcomed", true); vm.refresh() } else launcher.launch(permission) })
                return@Surface
            }
            val nav = rememberNavController()
            val openPlayer: () -> Unit = { nav.navigate("player") { launchSingleTop = true } }
            val entry by nav.currentBackStackEntryAsState()
            val route = entry?.destination?.route ?: "home"
            val snackbar = remember { SnackbarHostState() }
            LaunchedEffect(error, playback.error) {
                (error ?: playback.error)?.let { snackbar.showSnackbar(it); vm.error.value = null; vm.playback.clearError() }
            }
            Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                AnimatedVisibility(visible = route != "player", enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { Column {
                    MiniPlayer(playback, vm.playback.progress, openPlayer, vm.playback::toggle, vm.playback::next)
                    NavigationBar {
                        listOf(Triple("home", "Início", Icons.Rounded.Home), Triple("library", "Biblioteca", Icons.Rounded.LibraryMusic),
                            Triple("playlists", "Playlists", Icons.AutoMirrored.Rounded.QueueMusic), Triple("settings", "Ajustes", Icons.Rounded.Settings)).forEach { (path, label, icon) ->
                            NavigationBarItem(selected = route == path || (route == "recent" && path == "library"), onClick = { nav.navigate(path) { popUpTo("home"); launchSingleTop = true } }, icon = { Icon(icon, label) }, label = { Text(label) })
                        }
                    }
                }
                }
            }) { padding ->
                NavHost(nav, "home", Modifier.padding(padding),
                    enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(140)) }) {
                    composable("home") { LibraryScreen(vm, true, openPlayer, { nav.navigate("playlists") }, { nav.navigate("recent") { launchSingleTop = true } }) }
                    composable("library") { LibraryScreen(vm, false, openPlayer, { nav.navigate("playlists") }, { nav.navigate("recent") { launchSingleTop = true } }) }
                    composable("recent") { RecentSongsScreen(vm, { nav.popBackStack() }, openPlayer) }
                    composable("playlists") { PlaylistsScreen(vm, openPlayer) }
                    composable("player", enterTransition = { slideInVertically(spring(stiffness = 420f)) { it } + fadeIn() },
                        popExitTransition = { slideOutVertically(spring(stiffness = 420f)) { it } + fadeOut() }) {
                        PlayerScreen(vm, { nav.popBackStack() }, { nav.navigate("equalizer") })
                    }
                    composable("settings") { SettingsScreen(vm) { nav.navigate("equalizer") } }
                    composable("equalizer") { EqualizerScreen(vm) { nav.popBackStack() } }
                }
            }
        }
    }
}
