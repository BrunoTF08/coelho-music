package com.coelho.music.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        granted = allowed; denied = !allowed
        if (allowed) { vm.boolean("welcomed", true); vm.refresh() }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
            granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            if (granted) vm.refresh()
        } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    CoelhoTheme(settings?.dark ?: true, settings?.accent ?: 0) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (settings == null) { Box(Modifier.safeDrawingPadding()) { CircularProgressIndicator() }; return@Surface }
            if (!granted || settings?.welcomed != true) {
                WelcomeScreen(denied, onStart = { if (granted) { vm.boolean("welcomed", true); vm.refresh() } else launcher.launch(permission) })
                return@Surface
            }
            val nav = rememberNavController()
            val entry by nav.currentBackStackEntryAsState()
            val route = entry?.destination?.route ?: "home"
            val snackbar = remember { SnackbarHostState() }
            LaunchedEffect(error, playback.error) {
                (error ?: playback.error)?.let { snackbar.showSnackbar(it); vm.error.value = null; vm.playback.clearError() }
            }
            Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                if (route != "player") Column {
                    MiniPlayer(playback, { nav.navigate("player") }, vm.playback::toggle, vm.playback::next)
                    NavigationBar {
                        listOf(Triple("home", "Início", Icons.Rounded.Home), Triple("library", "Biblioteca", Icons.Rounded.LibraryMusic),
                            Triple("playlists", "Playlists", Icons.AutoMirrored.Rounded.QueueMusic), Triple("settings", "Ajustes", Icons.Rounded.Settings)).forEach { (path, label, icon) ->
                            NavigationBarItem(selected = route == path, onClick = { nav.navigate(path) { popUpTo("home"); launchSingleTop = true } }, icon = { Icon(icon, label) }, label = { Text(label) })
                        }
                    }
                }
            }) { padding ->
                NavHost(nav, "home", Modifier.padding(padding)) {
                    composable("home") { LibraryScreen(vm, true, { nav.navigate("player") }, { nav.navigate("playlists") }) }
                    composable("library") { LibraryScreen(vm, false, { nav.navigate("player") }, { nav.navigate("playlists") }) }
                    composable("playlists") { PlaylistsScreen(vm) { nav.navigate("player") } }
                    composable("player") { PlayerScreen(vm, { nav.popBackStack() }, { nav.navigate("equalizer") }) }
                    composable("settings") { SettingsScreen(vm) { nav.navigate("equalizer") } }
                    composable("equalizer") { EqualizerScreen(vm) { nav.popBackStack() } }
                }
            }
        }
    }
}
