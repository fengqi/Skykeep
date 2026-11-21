package io.github.yashwanthkamireddi.skykeep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashwanthkamireddi.skykeep.R
import io.github.yashwanthkamireddi.skykeep.sync.SignOut
import io.github.yashwanthkamireddi.skykeep.ui.theme.AppTheme
import io.github.yashwanthkamireddi.skykeep.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.quince_mobile.AccountInterface

/** Where the app is: the notes themselves, or their settings. */
private enum class NotesPlace { Notes, Settings }

/**
 * 备忘录应用的全部界面：一进来就是备忘录（iOS 备忘录的黄），设置里有账户、没送出的改动和
 * 退出登录。其余服务的同步仍在引擎与系统账户里照常运行，只是不在这里显示。
 */
@Composable
fun NotesScreens(
  engine: AccountInterface,
  onSignedOut: () -> Unit,
) {
  var place by rememberSaveable { mutableStateOf(NotesPlace.Notes) }
  when (place) {
    NotesPlace.Notes -> AppTheme(Palette.App.Notes) {
      NotesPage(engine, onOpenSettings = { place = NotesPlace.Settings })
    }
    NotesPlace.Settings -> NotesSettingsScreen(engine, onBack = { place = NotesPlace.Notes }, onSignedOut = onSignedOut)
  }
}

@Composable
private fun NotesSettingsScreen(engine: AccountInterface, onBack: () -> Unit, onSignedOut: () -> Unit) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val scope = rememberCoroutineScope()
  val palette = iosPalette()
  val account = remember { phoneAccount(context) }
  Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding()) {
    IosBar {
      IconButton(onClick = onBack) {
        Icon(painterResource(R.drawable.ic_back), contentDescription = "返回", tint = palette.accent)
      }
      Text("返回", style = MaterialTheme.typography.bodyLarge, color = palette.accent)
      Spacer(Modifier.weight(1f))
    }
    Column(Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
      Text("设置", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
      Surface(color = palette.card, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Box(Modifier.padding(16.dp)) { AccountRow(engine, account) }
      }
      NotSentSection(engine, account)
      Surface(color = palette.card, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
          Text("关于", style = MaterialTheme.typography.titleMedium)
          val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() }
          Text(
            "备忘录${version?.let { " $it" } ?: ""} · 基于开源的 Skykeep 引擎（GPL-3.0），与 iCloud 同步。与 Apple 无关。iCloud 与 iPhone 是 Apple Inc. 的商标。",
            style = MaterialTheme.typography.bodyMedium, color = palette.secondary, modifier = Modifier.padding(top = 6.dp),
          )
        }
      }
      var leaving by remember { mutableStateOf(false) }
      var unsent by remember { mutableIntStateOf(0) }
      var working by remember { mutableStateOf(false) }
      var trouble by remember { mutableStateOf<String?>(null) }
      OutlinedButton(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.delete),
        enabled = !working,
        onClick = {
          scope.launch {
            unsent = withContext(Dispatchers.IO) {
              (account?.let { SignOut.unsent(context.contentResolver, it) } ?: 0) +
                (runCatching { engine.unsentChanges().toInt() }.getOrDefault(0))
            }
            leaving = true
          }
        },
      ) { Text("退出登录") }
      trouble?.let { Text(it, color = palette.delete, modifier = Modifier.padding(top = 8.dp)) }
      if (leaving) {
        AlertDialog(
          onDismissRequest = { leaving = false },
          containerColor = palette.card,
          title = { Text("要在此手机上退出 iCloud 吗？") },
          text = {
            Text(
              (if (unsent > 0) "这台手机上有 $unsent 处改动还没送到 iCloud，退出后不会再发送。\n\n" else "") +
                "选择保留副本时，联系人会在手机上留下不再同步的一份；iCloud 本身不受影响。",
            )
          },
          confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
              for ((label, keep) in listOf("保留副本" to true, "从手机移除" to false)) {
                TextButton(
                  onClick = {
                    leaving = false
                    working = true
                    scope.launch {
                      val done = withContext(Dispatchers.IO) {
                        runCatching {
                          account?.let { a ->
                            if (keep) SignOut.keepCopy(context.contentResolver, a)
                            SignOut.removeAccount(context, a)
                          }
                          if (!keep) { engine.forgetAccount(); SignOut.forgetHere(context) }
                          engine.signOut()
                        }
                      }
                      working = false
                      done.onSuccess { onSignedOut() }.onFailure { trouble = "退出失败：${it.message}" }
                    }
                  },
                  colors = if (keep) ButtonDefaults.textButtonColors() else ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(label) }
              }
              TextButton(onClick = { leaving = false }) { Text("取消") }
            }
          },
      )
      }
    }
  }
}
