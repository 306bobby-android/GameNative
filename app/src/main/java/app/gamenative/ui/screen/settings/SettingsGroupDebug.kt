package app.gamenative.ui.screen.settings

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import app.gamenative.service.SteamService
import app.gamenative.ui.component.dialog.CrashLogDialog
import app.gamenative.ui.theme.settingsTileColors
import app.gamenative.ui.theme.settingsTileColorsDebug
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import app.gamenative.PrefManager
import app.gamenative.ui.theme.settingsTileColorsAlt
import com.winlator.PrefManager as WinlatorPrefManager
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import app.gamenative.ui.component.dialog.WineDebugChannelsDialog

@Suppress("UnnecessaryOptInAnnotation") // ExperimentalFoundationApi
@OptIn(ExperimentalCoilApi::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsGroupDebug() {
    val context = LocalContext.current
    // initialize preference managers
    PrefManager.init(context)
    WinlatorPrefManager.init(context)

    // Load Wine debug channels and prepare selection state
    var allWineChannels by remember { mutableStateOf<List<String>>(emptyList()) }
    var showChannelsDialog by remember { mutableStateOf(false) }
    var selectedWineChannels by remember { mutableStateOf(PrefManager.wineDebugChannels.split(",")) }
    LaunchedEffect(Unit) {
        // Read the list of channels from assets
        val json = context.assets.open("wine_debug_channels.json").bufferedReader().use { it.readText() }
        allWineChannels = Json.decodeFromString(json)
    }

    // Dialog for selecting channels
    WineDebugChannelsDialog(
        openDialog = showChannelsDialog,
        allChannels = allWineChannels,
        currentSelection = selectedWineChannels,
        onSave = { newSelection ->
            selectedWineChannels = newSelection
            PrefManager.wineDebugChannels = newSelection.joinToString(",")
            showChannelsDialog = false
        },
        onDismiss = { showChannelsDialog = false }
    )

    /* Crash Log stuff */
    var showLogcatDialog by rememberSaveable { mutableStateOf(false) }
    // states for debug toggles
    var enableWineDebugPref by rememberSaveable { mutableStateOf(PrefManager.enableWineDebug) }
    var enableBox86Logs by rememberSaveable { mutableStateOf(WinlatorPrefManager.getBoolean("enable_box86_64_logs", false)) }
    var latestCrashFile: File? by rememberSaveable { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        val crashDir = File(context.getExternalFilesDir(null), "crash_logs")
        latestCrashFile = crashDir.listFiles()
            ?.filter { it.name.startsWith("pluvia_crash_") }
            ?.maxByOrNull { it.lastModified() }
    }

    /* Save crash log */
    val saveResultContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { resultUri ->
        try {
            resultUri?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    latestCrashFile?.inputStream()?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save logcat to destination", Toast.LENGTH_SHORT).show()
        }
    }

    CrashLogDialog(
        visible = showLogcatDialog && latestCrashFile != null,
        fileName = latestCrashFile?.name ?: "No Filename",
        fileText = latestCrashFile?.readText() ?: "Couldn't read crash log.",
        onSave = { latestCrashFile?.let { file -> saveResultContract.launch(file.name) } },
        onDismissRequest = { showLogcatDialog = false },
    )

    /* Wine Debug Log export setup */
    var showWineLogDialog by rememberSaveable { mutableStateOf(false) }
    var latestWineLogFile: File? by rememberSaveable { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        val wineLogDir = File(context.getExternalFilesDir(null), "wine_logs")
        wineLogDir.mkdirs()
        val wineLogFile = File(wineLogDir, "wine_debug.log")
        latestWineLogFile = if (wineLogFile.exists()) wineLogFile else null
    }
    val saveWineLogContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { resultUri ->
        try {
            resultUri?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    latestWineLogFile?.inputStream()?.use { input -> input.copyTo(output) }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save Wine log to destination", Toast.LENGTH_SHORT).show()
        }
    }

    CrashLogDialog(
        visible = showWineLogDialog && latestWineLogFile != null,
        fileName = latestWineLogFile?.name ?: "wine_debug.log",
        fileText = latestWineLogFile?.readText() ?: "Couldn't read Wine log.",
        onSave = { latestWineLogFile?.let { file -> saveWineLogContract.launch(file.name) } },
        onDismissRequest = { showWineLogDialog = false },
    )

    SettingsGroup(title = { Text(text = "Debug") }) {
        // Link to open channel selector
        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = "Select Wine Debug Channels") },
            subtitle = { Text(text = if (selectedWineChannels.isNotEmpty()) selectedWineChannels.joinToString(",") else "No channels selected") },
            onClick = { showChannelsDialog = true },
        )
        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            state = enableWineDebugPref,
            title = { Text(text = "Enable Wine Debug Logs") },
            subtitle = { Text(text = "Write Wine debug output to file") },
            onCheckedChange = {
                enableWineDebugPref = it
                PrefManager.enableWineDebug = it
            },
        )
        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            state = enableBox86Logs,
            title = { Text(text = "Enable Box86/64 Logs") },
            subtitle = { Text(text = "Write Box86 & Box64 debug output to file") },
            onCheckedChange = {
                enableBox86Logs = it
                WinlatorPrefManager.putBoolean("enable_box86_64_logs", it)
            },
        )
        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = "View latest crash") },
            subtitle = {
                val text = if (latestCrashFile != null) {
                    "Shows the most recent crash log"
                } else {
                    "No recent crash logs found"
                }
                Text(text = text)
            },
            enabled = latestCrashFile != null,
            onClick = { showLogcatDialog = true },
        )

        SettingsMenuLink(
            colors = settingsTileColors(),
            title = { Text(text = "View game debug log") },
            subtitle = {
                val text = if (latestWineLogFile != null) {
                    "Shows the latest Wine/Box64 debug log"
                } else {
                    "No Wine debug logs found"
                }
                Text(text = text)
            },
            enabled = latestWineLogFile != null,
            onClick = { showWineLogDialog = true },
        )

        SettingsMenuLink(
            modifier = Modifier.combinedClickable(
                onLongClick = {
                    SteamService.logOut()
                    (context as ComponentActivity).finishAffinity()
                },
                onClick = {
                    Toast.makeText(context, "Long click to activate", Toast.LENGTH_SHORT).show()
                },
            ),
            colors = settingsTileColorsDebug(),
            title = { Text(text = "Clear Preferences") },
            subtitle = { Text("[Closes App] Logs out the client and wipes local preference data.") },
            onClick = {},
        )

        SettingsMenuLink(
            modifier = Modifier.combinedClickable(
                onLongClick = {
                    SteamService.stop()
                    SteamService.clearDatabase()
                    (context as ComponentActivity).finishAffinity()
                },
                onClick = {
                    Toast.makeText(context, "Long click to activate", Toast.LENGTH_SHORT).show()
                },
            ),
            colors = settingsTileColorsDebug(),
            title = { Text(text = "Clear Local Database") },
            subtitle = { Text("[Closes app] May help fix issues with library items or messages.") },
            onClick = {},
        )

        SettingsMenuLink(
            modifier = Modifier.combinedClickable(
                onLongClick = {
                    context.imageLoader.diskCache?.clear()
                    context.imageLoader.memoryCache?.clear()
                },
                onClick = {
                    Toast.makeText(context, "Long click to activate", Toast.LENGTH_SHORT).show()
                },
            ),
            colors = settingsTileColorsDebug(),
            title = { Text(text = "Clear Image Cache") },
            subtitle = { Text(text = "Remove all images that were loaded.") },
            onClick = {},
        )
    }
}
