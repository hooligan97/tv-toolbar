package com.example.tvtoolbar

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// ==== KONFIGURACJA – podmień nazwy pakietów na swoje ====
data class AppEntry(val name: String, val pkg: String)

val fileManagers = listOf(
    AppEntry("Menedżer A", "com.example.filemanager1"),
    AppEntry("Menedżer B", "com.example.filemanager2"),
)

val browsers = listOf(
    AppEntry("Chrome", "com.android.chrome"),
    AppEntry("Firefox", "org.mozilla.tv.firefox"),
)
// ========================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainScreen(this) }
    }
}

@Composable
fun MainScreen(context: Context) {
    var showFilePicker by remember { mutableStateOf(false) }
    var showBrowserPicker by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("tv_toolbar", Context.MODE_PRIVATE) }
    var lastFilePkg by remember { mutableStateOf(prefs.getString("file_pkg", fileManagers.first().pkg)!!) }
    var lastBrowserPkg by remember { mutableStateOf(prefs.getString("browser_pkg", browsers.first().pkg)!!) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        Text("TvToolbar", color = Color(0xFF444444), fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(140.dp)
                .background(Color(0xFF111111)).padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MouseActionButton(
                label = "Pliki", icon = Icons.Default.Folder,
                onClick = { launchByPackage(context, lastFilePkg) },
                onLongClick = { showFilePicker = true }
            )
            MouseActionButton(
                label = "Przeglądarka", icon = Icons.Default.Language,
                onClick = { launchByPackage(context, lastBrowserPkg) },
                onLongClick = { showBrowserPicker = true }
            )
            MouseActionButton(
                label = "Opcje", icon = Icons.Default.Settings,
                onClick = { openSetting(context, Settings.ACTION_WIFI_SETTINGS) },
                onLongClick = { openSetting(context, Settings.ACTION_SETTINGS) }
            )
        }
    }

    if (showFilePicker) {
        AppPickerDialog("Wybierz menedżer plików", fileManagers, { showFilePicker = false }) { entry ->
            lastFilePkg = entry.pkg; prefs.edit().putString("file_pkg", entry.pkg).apply()
            launchByPackage(context, entry.pkg); showFilePicker = false
        }
    }
    if (showBrowserPicker) {
        AppPickerDialog("Wybierz przeglądarkę", browsers, { showBrowserPicker = false }) { entry ->
            lastBrowserPkg = entry.pkg; prefs.edit().putString("browser_pkg", entry.pkg).apply()
            launchByPackage(context, entry.pkg); showBrowserPicker = false
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MouseActionButton(label: String, icon: ImageVector, onClick: () -> Unit, onLongClick: () -> Unit) {
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val bg = when { focused -> Color.White; hovered -> Color(0xFF3A3A3A); else -> Color(0xFF1E1E1E) }
    val fg = if (focused) Color.Black else Color.White

    Box(
        modifier = Modifier.size(110.dp).clip(RoundedCornerShape(18.dp)).background(bg)
            .hoverable(hover).focusable().onFocusChanged { focused = it.isFocused }
            .combinedClickable(interactionSource = hover, indication = null, onClick = onClick, onLongClick = onLongClick)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = fg, fontSize = 13.sp)
        }
    }
}

@Composable
fun AppPickerDialog(title: String, entries: List<AppEntry>, onDismiss: () -> Unit, onPick: (AppEntry) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp)).padding(24.dp).width(420.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            entries.forEach { entry ->
                Text(entry.name, color = Color.White, fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable { onPick(entry) }.padding(vertical = 14.dp, horizontal = 8.dp))
            }
        }
    }
}

fun launchByPackage(context: Context, pkg: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    if (intent != null) try { context.startActivity(intent) } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Nie znaleziono: $pkg", Toast.LENGTH_SHORT).show()
    } else Toast.makeText(context, "Brak aplikacji: $pkg", Toast.LENGTH_SHORT).show()
}

fun openSetting(context: Context, action: String) {
    val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { context.startActivity(intent) } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
