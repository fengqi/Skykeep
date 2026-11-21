package io.github.yashwanthkamireddi.skykeep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * 备忘录专用的一次设置：只创建同步账号、发起备忘录的第一次同步。纯备忘录应用不需要通讯录和
 * 日历，权限不请求，这两个 authority 也直接关掉，Android 不会再为它们跑同步。
 */
@Composable
fun NotesSetup(engine: AccountInterface, onDone: () -> Unit) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val palette = iosPalette()
  var problem by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(Unit) {
    val made = withContext(Dispatchers.IO) {
      val tag = engine.accountTag()
      if (tag == null) null
      else {
        val account = io.github.yashwanthkamireddi.skykeep.sync.ensureAccount(
          context, engine.appleId() ?: "iCloud", tag,
        )
        // Contacts and Calendar stay out: no permission is held for them, so their syncs
        // would only fail; switched off here, Android never asks for them on this account.
        for (authority in listOf(
          android.provider.ContactsContract.AUTHORITY,
          android.provider.CalendarContract.AUTHORITY,
        )) {
          android.content.ContentResolver.cancelSync(account, authority)
          android.content.ContentResolver.setSyncAutomatically(account, authority, false)
          android.content.ContentResolver.setIsSyncable(account, authority, 0)
        }
        android.content.ContentResolver.requestSync(account, io.github.yashwanthkamireddi.skykeep.sync.NOTES_AUTHORITY, android.os.Bundle().apply {
          putBoolean(android.content.ContentResolver.SYNC_EXTRAS_MANUAL, true)
          putBoolean(android.content.ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        })
        account
      }
    }
    if (made == null) {
      problem = "还没有登录会话，请重新打开应用登录。"
    } else {
      onDone()
    }
  }
  Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding().padding(28.dp)) {
    Spacer(Modifier.height(120.dp))
    Text("正在准备同步…", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(
      "正在把你的 iCloud 备忘录接到这台手机上。",
      style = MaterialTheme.typography.bodyLarge, color = palette.secondary,
    )
    problem?.let { Text(it, color = palette.delete, modifier = Modifier.padding(top = 16.dp)) }
  }
}

/**
 * 备忘录应用的全部界面：一进来就是备忘录（iOS 备忘录的黄），设置里有账户、没送出的改动和
 * 退出登录。其余服务的同步仍在引擎与系统账户里照常运行，只是不在这里显示。
 */
@Composable
fun NotesScreens(
  engine: AccountInterface,
  newNoteLaunch: Boolean = false,
  onNewNoteConsumed: () -> Unit = {},
  onSignedOut: () -> Unit,
) {
  var place by rememberSaveable { mutableStateOf(NotesPlace.Notes) }
  // 快捷方式的标记在这层转成待办：不管当前停在备忘录还是设置，都能消费。
  var newNotePending by remember { mutableStateOf(false) }
  LaunchedEffect(newNoteLaunch) {
    if (newNoteLaunch) { place = NotesPlace.Notes; newNotePending = true }
  }
  when (place) {
    NotesPlace.Notes -> AppTheme(Palette.App.Notes) {
      NotesPage(
        engine,
        onOpenSettings = { place = NotesPlace.Settings },
        newNoteLaunch = newNotePending,
        onNewNoteConsumed = { newNotePending = false; onNewNoteConsumed() },
      )
    }
    NotesPlace.Settings -> NotesSettingsScreen(engine, onBack = { place = NotesPlace.Notes }, onSignedOut = onSignedOut)
  }
}

/** 设置页里的一行：左边名字，右边当前值加箭头，点击弹选择。 */
@Composable
private fun SettingRow(palette: IosPalette, label: String, value: String, onClick: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    Text(value, style = MaterialTheme.typography.bodyLarge, color = palette.secondary)
    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = palette.secondary, modifier = Modifier.padding(start = 4.dp).size(15.dp))
  }
}

@Composable
private fun NotesSettingsScreen(engine: AccountInterface, onBack: () -> Unit, onSignedOut: () -> Unit) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val scope = rememberCoroutineScope()
  val palette = iosPalette()
  val account = remember { phoneAccount(context) }
  // 手势与返回键都回到备忘录，不退出应用。
  androidx.activity.compose.BackHandler(onBack = onBack)
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
      Text("查看", style = MaterialTheme.typography.bodyMedium, color = palette.secondary, modifier = Modifier.padding(top = 24.dp))
      val noteSort = remember { mutableStateOf(savedSetting(context, "noteSort", "edited")) }
      val noteGroup = remember { mutableStateOf(savedSetting(context, "noteGroup", "on")) }
      val noteStart = remember { mutableStateOf(savedSetting(context, "noteStart", "title")) }
      val checkSort = remember { mutableStateOf(savedSetting(context, "checkSort", "auto")) }
      var picking by remember { mutableStateOf<Pair<String, String>?>(null) }
      Surface(color = palette.card, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column {
          SettingRow(
            palette,
            "备忘录排序",
            when (noteSort.value) { "created" -> "创建日期"; "title" -> "标题"; else -> "编辑日期" },
          ) { picking = "noteSort" to "备忘录排序" }
          HorizontalDivider(color = palette.separator)
          Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("按日期分组备忘录", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
              checked = noteGroup.value == "on",
              onCheckedChange = { on -> noteGroup.value = if (on) "on" else "off"; saveSetting(context, "noteGroup", if (on) "on" else "off") },
            )
          }
          HorizontalDivider(color = palette.separator)
          SettingRow(palette, "新备忘录开头", when (noteStart.value) { "heading" -> "小标题"; "subheading" -> "副标题"; "body" -> "正文"; else -> "标题" }) {
            picking = "noteStart" to "新备忘录开头"
          }
          HorizontalDivider(color = palette.separator)
          SettingRow(palette, "排序已勾选项目", when (checkSort.value) { "manual" -> "手动"; else -> "自动" }) {
            picking = "checkSort" to "排序已勾选项目"
          }
        }
      }
      picking?.let { (key, title) ->
        val current = when (key) {
          "noteSort" -> noteSort.value; "noteStart" -> noteStart.value; "checkSort" -> checkSort.value; else -> ""
        }
        val options = when (key) {
          "noteSort" -> listOf("edited" to "编辑日期", "created" to "创建日期", "title" to "标题")
          "noteStart" -> listOf("title" to "标题", "heading" to "小标题", "subheading" to "副标题", "body" to "正文")
          else -> listOf("auto" to "自动", "manual" to "手动")
        }
        AlertDialog(
          onDismissRequest = { picking = null },
          containerColor = palette.card,
          titleContentColor = MaterialTheme.colorScheme.onSurface,
          textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          title = { Text(title) },
          text = {
            Column {
              for ((value, label) in options) {
                Row(
                  Modifier.fillMaxWidth().clickable { picking = null; saveSetting(context, key, value); (if (key == "noteSort") noteSort else if (key == "noteStart") noteStart else checkSort).value = value }
                    .padding(vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                  if (value == current) Icon(painterResource(R.drawable.ic_check), contentDescription = "已选", tint = palette.accent, modifier = Modifier.size(18.dp))
                }
              }
            }
          },
          confirmButton = {},
        )
      }
      Text("关于", style = MaterialTheme.typography.bodyMedium, color = palette.secondary, modifier = Modifier.padding(top = 24.dp))
      Surface(color = palette.card, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
          val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() }
          Text(
            "备忘录${version?.let { " $it" } ?: ""} · 基于开源的 Skykeep 引擎（GPL-3.0），与 iCloud 同步。与 Apple 无关。iCloud 与 iPhone 是 Apple Inc. 的商标。",
            style = MaterialTheme.typography.bodyMedium, color = palette.secondary,
          )
        }
      }
      // 同步：手动拉一次，按钮下小字是最近一次发起同步的时间（系统没有公开的上次成功时间）。
      Text("同步", style = MaterialTheme.typography.bodyMedium, color = palette.secondary, modifier = Modifier.padding(top = 24.dp))
      Surface(color = palette.card, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
          var syncing by remember { mutableStateOf(false) }
          var lastSync by remember { mutableStateOf(savedSetting(context, "lastSync", "")) }
          LaunchedEffect(syncing) {
            if (syncing) { for (i in 1..3) { kotlinx.coroutines.delay(1000); lastSync = savedSetting(context, "lastSync", "") }; syncing = false }
          }
          OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !syncing,
            onClick = {
              account?.let { a ->
                syncing = true
                saveSetting(context, "lastSync", (System.currentTimeMillis() / 1000).toString())
                lastSync = savedSetting(context, "lastSync", "")
                android.content.ContentResolver.requestSync(
                  a, io.github.yashwanthkamireddi.skykeep.sync.NOTES_AUTHORITY,
                  android.os.Bundle().apply {
                    putBoolean(android.content.ContentResolver.SYNC_EXTRAS_MANUAL, true)
                    putBoolean(android.content.ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                  },
                )
              }
            },
          ) { Text(if (syncing) "同步中…" else "立即同步") }
          Text(
            "上次同步：" + (lastSync.toLongOrNull()?.let {
              java.time.Instant.ofEpochSecond(it).atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM, java.time.format.FormatStyle.SHORT))
            } ?: "还没有同步过"),
            style = MaterialTheme.typography.bodySmall, color = palette.secondary,
            modifier = Modifier.padding(top = 8.dp),
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
