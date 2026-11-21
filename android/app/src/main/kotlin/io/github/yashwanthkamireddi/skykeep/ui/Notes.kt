package io.github.yashwanthkamireddi.skykeep.ui

import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import io.github.yashwanthkamireddi.skykeep.ui.theme.Palette
import android.content.ContentResolver
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashwanthkamireddi.skykeep.R
import io.github.yashwanthkamireddi.skykeep.sync.NOTES_AUTHORITY
import io.github.yashwanthkamireddi.skykeep.sync.Outcome
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.quince_mobile.AccountInterface
import uniffi.quince_mobile.NDoc
import uniffi.quince_mobile.NAttachment
import uniffi.quince_mobile.NFolder
import uniffi.quince_mobile.NItem
import uniffi.quince_mobile.NPart
import uniffi.quince_mobile.NSpan

/** Apple's id for Recently Deleted (quince-core's TRASH_FOLDER). */
private const val TRASH = "TrashFolder-CloudKit"

/**
 * iOS 备忘录的一组颜色：浅色是分组背景 #F2F2F7 上的白卡片，深色是纯黑上的 #1C1C1E；主题色用
 * Notes 的黄（与上游 Palette.App.Notes 一致），删除用 iOS 红。
 */
internal data class IosPalette(val bg: Color, val card: Color, val secondary: Color, val separator: Color, val accent: Color, val delete: Color)

@Composable
internal fun iosPalette(): IosPalette {
  val dark = isSystemInDarkTheme()
  return IosPalette(
    bg = if (dark) Color(0xFF000000) else Color(0xFFF2F2F7),
    card = if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
    secondary = if (dark) Color(0xFF98989F) else Color(0xFF8A8A8E),
    separator = if (dark) Color(0xFF38383A) else Color(0xFFE3E3E8),
    accent = Color(if (dark) Palette.App.Notes.dark else Palette.App.Notes.light),
    delete = if (dark) Color(0xFFFF453A) else Color(0xFFFF3B30),
  )
}

private fun syncNotes(context: android.content.Context) {
  phoneAccount(context)?.let {
    ContentResolver.requestSync(it, NOTES_AUTHORITY, Bundle().apply {
      putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
      putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
    })
  }
}

/** Where Notes is: the folders, one folder's notes (or all), or one note open. */
/** Where Notes is, kept through a rotation: kind, folder, folder title, note. */
private val NPlaceSaver = listSaver<NPlace, String>(
  save = { p ->
    when (p) {
      NPlace.Folders -> listOf("folders")
      is NPlace.Folder -> listOf("folder", p.guid ?: "", p.title)
      is NPlace.Open -> listOf("open", p.from.guid ?: "", p.from.title, p.guid ?: "")
    }
  },
  restore = { s ->
    when (s[0]) {
      "folder" -> NPlace.Folder(s[1].ifEmpty { null }, s[2])
      "open" -> NPlace.Open(s[3].ifEmpty { null }, NPlace.Folder(s[1].ifEmpty { null }, s[2]))
      else -> NPlace.Folders
    }
  },
)

private sealed interface NPlace {
  data object Folders : NPlace
  data class Folder(val guid: String?, val title: String) : NPlace
  /** A note open; `guid` null for a new one, made only once something is typed in it. */
  data class Open(val guid: String?, val from: Folder) : NPlace
}

/**
 * 备忘录。布局复刻 iOS 备忘录：文件夹页、笔记列表（置顶与时间分组、滑动置顶与删除、长按菜单、
 * 列表与画廊两种视图）、可读可写的编辑页。每次改动立刻上屏，后台发往 iCloud；格式由引擎保全。
 */
@Composable
fun NotesPage(engine: AccountInterface, onOpenSettings: () -> Unit = {}) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val palette = iosPalette()
  var place by rememberSaveable(stateSaver = NPlaceSaver) { mutableStateOf<NPlace>(NPlace.Folders) }
  var folders by remember { mutableStateOf<List<NFolder>>(emptyList()) }
  var notes by remember { mutableStateOf<List<NItem>>(emptyList()) }
  var query by remember { mutableStateOf("") }
  var problem by remember { mutableStateOf<String?>(null) }
  var generation by remember { mutableIntStateOf(0) }
  var newFolder by remember { mutableStateOf(false) }
  var renaming by remember { mutableStateOf<NFolder?>(null) }
  var dropping by remember { mutableStateOf<NFolder?>(null) }

  fun reason(t: Throwable) = (t as? uniffi.quince_mobile.EngineException.Failed)?.reason ?: "无法读取备忘录。"

  // The read in flight: a new one cancels it, so a search typed fast never shows an older
  // query's answer arriving after a newer one's.
  val reading = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }
  fun reload(): kotlinx.coroutines.Job {
    reading[0]?.cancel()
    return scope.launch {
      val p = place
      try {
        val (f, n) = withContext(Dispatchers.IO) {
          val f = engine.noteFolders()
          val n = when {
            query.isNotBlank() -> engine.searchNotes(query)
            p is NPlace.Folder -> engine.notes(p.guid)
            else -> emptyList()
          }
          f to n
        }
        folders = f; notes = n; problem = null
      } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
      } catch (t: Throwable) {
        problem = reason(t)
      }
    }.also { reading[0] = it }
  }

  /** A folder made, renamed or deleted at Apple, then everything read again. */
  fun folderChange(what: String, write: () -> Unit) = scope.launch {
    runCatching { withContext(Dispatchers.IO) { write() } }
      .onFailure { problem = (it as? uniffi.quince_mobile.EngineException.Failed)?.reason ?: "没有送到 iCloud，稍后会再试。" }
    generation++
  }

  LaunchedEffect(Unit) { syncNotes(context) }
  DisposableEffect(Unit) {
    val stop = Outcome.watch(context) { generation++ }
    onDispose { stop() }
  }
  LaunchedEffect(place, query, generation) { reload() }
  // Back goes up one level: an open folder to the folders, search results to where they began;
  // an open note handles its own back (it saves).
  BackHandler(enabled = place is NPlace.Folder) { place = NPlace.Folders; query = "" }
  BackHandler(enabled = place is NPlace.Folders && query.isNotBlank()) { query = "" }

  /** 置顶或删除一篇搜索结果里的备忘录，然后刷新。 */
  fun searchAct(n: NItem, write: () -> Unit) = scope.launch {
    runCatching { withContext(Dispatchers.IO) { write() } }
      .onFailure { problem = (it as? uniffi.quince_mobile.EngineException.Failed)?.reason ?: "没有送到 iCloud，稍后会再试。" }
    syncNotes(context); generation++
  }

  Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding()) {
    problem?.let { Text(it, color = palette.delete, modifier = Modifier.padding(20.dp, 12.dp)) }
    when (val p = place) {
      is NPlace.Open -> NoteEditor(
        engine, p.guid, p.from.guid, folders,
        onBack = { place = p.from; generation++ },
        onProblem = { problem = it },
      )
      is NPlace.Folder -> NoteListScreen(
        engine, p, folders, notes, query, palette,
        onQuery = { query = it },
        onBack = { place = NPlace.Folders; query = "" },
        onOpen = { n -> place = NPlace.Open(n.guid, p) },
        onNew = { place = NPlace.Open(null, p) },
        onRename = { renaming = folders.firstOrNull { it.guid == p.guid && !it.trash } },
        onDelete = { dropping = folders.firstOrNull { it.guid == p.guid && !it.trash } },
        onGeneration = { generation++ },
        onProblem = { problem = it },
      )
      NPlace.Folders -> if (query.isNotBlank()) {
        // 在文件夹页搜索：搜的是所有 iCloud，结果是一个平铺的列表。
        IosBar {
          Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { query = "" }) {
              Icon(painterResource(R.drawable.ic_back), contentDescription = "文件夹", tint = palette.accent)
            }
            Text("文件夹", style = MaterialTheme.typography.bodyLarge, color = palette.accent)
          }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
          Text("搜索", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
          Text("${notes.size} 个搜索结果", style = MaterialTheme.typography.bodyMedium, color = palette.secondary)
          IosSearch(query, { query = it }, palette, Modifier.padding(top = 8.dp, bottom = 4.dp))
        }
        if (notes.isEmpty()) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("没有找到备忘录", color = palette.secondary) }
        } else {
          NoteList(notes, grouped = false, palette, onOpen = { n -> place = NPlace.Open(n.guid, NPlace.Folder(null, "所有 iCloud")) }, onChoose = {}, onPin = { n -> searchAct(n) { engine.notePin(n.guid, !n.pinned) } }, onRemove = { n -> searchAct(n) { engine.noteDelete(n.guid) } })
        }
      } else FolderScreen(
        folders, query, palette,
        onQuery = { query = it },
        onOpen = { place = it },
        onNewFolder = { newFolder = true },
        onSettings = onOpenSettings,
      )
    }
  }
  if (newFolder) NameDialog("新建文件夹", "", onDismiss = { newFolder = false }) { name ->
    newFolder = false
    folderChange(name) { engine.noteFolderCreate(name) }
  }
  renaming?.let { f ->
    NameDialog("重新命名", f.title, onDismiss = { renaming = null }) { name ->
      renaming = null
      if (name != f.title) {
        if (place is NPlace.Folder) place = NPlace.Folder(f.guid, name)
        folderChange(name) { engine.noteFolderRename(f.guid, name) }
      }
    }
  }
  dropping?.let { f ->
    AlertDialog(
      onDismissRequest = { dropping = null },
      containerColor = palette.card,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      title = { Text("要删除“${f.title}”吗？") },
      text = { Text("这个文件夹里的所有备忘录会在你的所有设备上移到“最近删除”，并保留 30 天。") },
      confirmButton = {
        TextButton(onClick = {
          dropping = null; place = NPlace.Folders
          folderChange(f.title) { engine.noteFolderDelete(f.guid) }
        }) { Text("删除文件夹", color = palette.delete) }
      },
      dismissButton = { TextButton(onClick = { dropping = null }) { Text("取消", color = palette.accent) } },
    )
  }
}

/** iOS 顶栏的一行：内容左右排开。 */
@Composable
internal fun IosBar(content: @Composable RowScope.() -> Unit) {
  Row(
    Modifier.fillMaxWidth().padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 2.dp).height(44.dp),
    verticalAlignment = Alignment.CenterVertically,
    content = content,
  )
}

/** The folders, as iOS lists them: All iCloud, each folder, Recently Deleted, one white card. */
@Composable
private fun FolderScreen(
  folders: List<NFolder>,
  query: String,
  palette: IosPalette,
  onQuery: (String) -> Unit,
  onOpen: (NPlace.Folder) -> Unit,
  onNewFolder: () -> Unit,
  onSettings: () -> Unit,
) {
  var menu by remember { mutableStateOf(false) }
  IosBar {
    Spacer(Modifier.weight(1f))
    Box {
      IconButton(onClick = { menu = true }) {
        Icon(painterResource(R.drawable.ic_more), contentDescription = "更多", tint = palette.accent)
      }
      DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = palette.card) {
        DropdownMenuItem(text = { Text("设置") }, onClick = { menu = false; onSettings() })
      }
    }
  }
  Column(Modifier.padding(horizontal = 20.dp)) {
    Text("文件夹", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
    IosSearch(query, onQuery, palette, Modifier.padding(top = 8.dp))
  }
  val all = folders.filter { !it.trash }.sumOf { it.count.toInt() }
  data class Row(val title: String, val count: Int, val icon: Int, val place: NPlace.Folder)
  val rows = buildList {
    add(Row("所有 iCloud", all, R.drawable.ic_folder, NPlace.Folder(null, "所有 iCloud")))
    // Apple's own "Notes" folder is the one with no folder: notes not filed anywhere.
    folders.filter { !it.trash }.forEach { add(Row(it.title, it.count.toInt(), R.drawable.ic_folder, NPlace.Folder(it.guid, it.title))) }
    folders.firstOrNull { it.trash }?.let { add(Row("最近删除", it.count.toInt(), R.drawable.ic_trash, NPlace.Folder(it.guid, "最近删除"))) }
  }
  LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)) {
    groupItems(rows, key = { it.title }, inset = 54) { r ->
      Row(
        Modifier.fillMaxWidth().clickable { onOpen(r.place) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(painterResource(r.icon), contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(r.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${r.count}", style = MaterialTheme.typography.bodyLarge, color = palette.secondary)
        Spacer(Modifier.width(6.dp))
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = palette.secondary, modifier = Modifier.size(16.dp))
      }
    }
    item {
      TextButton(onClick = onNewFolder, modifier = Modifier.padding(top = 8.dp, start = 2.dp)) {
        Icon(painterResource(R.drawable.ic_folder_plus), contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("新建文件夹", color = palette.accent)
      }
    }
  }
}

/** iOS 的搜索框：圆角灰底，占位“搜索”。 */
@Composable
private fun IosSearch(value: String, onChange: (String) -> Unit, palette: IosPalette, modifier: Modifier = Modifier) {
  Surface(
    color = if (isSystemInDarkTheme()) Color(0xFF1C1C1E) else Color(0xFFE3E3E9),
    shape = RoundedCornerShape(10.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = palette.secondary, modifier = Modifier.size(17.dp))
      Spacer(Modifier.width(8.dp))
      BasicTextField(
        value = value, onValueChange = onChange, singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(palette.accent),
        modifier = Modifier.weight(1f),
        decorationBox = { field ->
          if (value.isEmpty()) Text("搜索", style = MaterialTheme.typography.bodyLarge, color = palette.secondary)
          field()
        },
      )
    }
  }
}

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** When a note was changed, as Notes says it: the time today, 昨天, the weekday this week, else the date. */
private fun changed(at: Long?): String {
  at ?: return ""
  val z = Instant.ofEpochSecond(at).atZone(ZoneId.systemDefault())
  val today = LocalDate.now()
  val d = z.toLocalDate()
  return when {
    d == today -> z.toLocalTime().format(timeFormat)
    d == today.minusDays(1) -> "昨天"
    d.isAfter(today.minusDays(7)) -> d.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
    else -> d.format(dayFormat)
  }
}

/** Which section a note's last change puts it in, as Notes groups them. */
private fun bucket(n: NItem, today: LocalDate): String {
  if (n.pinned) return "置顶"
  val d = n.modifiedAt?.let { Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate() } ?: return "更早"
  return when {
    d == today -> "今天"
    d == today.minusDays(1) -> "昨天"
    d.isAfter(today.minusDays(7)) -> "前 7 天"
    d.isAfter(today.minusDays(30)) -> "前 30 天"
    d.year == today.year -> d.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
    else -> d.year.toString()
  }
}

/** One folder's notes, as iOS lists or shows them in the gallery. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NoteListScreen(
  engine: AccountInterface,
  folder: NPlace.Folder,
  folders: List<NFolder>,
  notes: List<NItem>,
  query: String,
  palette: IosPalette,
  onQuery: (String) -> Unit,
  onBack: () -> Unit,
  onOpen: (NItem) -> Unit,
  onNew: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
  onGeneration: () -> Unit,
  onProblem: (String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val trash = folders.firstOrNull { it.guid == folder.guid }?.trash == true
  val own = folders.firstOrNull { it.guid == folder.guid && !it.trash } != null
  var menu by remember { mutableStateOf(false) }
  var view by rememberSaveable { mutableStateOf("list") }
  var chosen by remember { mutableStateOf<NItem?>(null) }

  fun act(n: NItem, write: () -> Unit) = scope.launch {
    runCatching { withContext(Dispatchers.IO) { write() } }.onFailure { onProblem((it as? uniffi.quince_mobile.EngineException.Failed)?.reason ?: "没有送到 iCloud，稍后会再试。") }
    syncNotes(context); onGeneration()
  }
  fun remove(n: NItem) = act(n) { engine.noteDelete(n.guid) }
  fun pin(n: NItem) = act(n) { engine.notePin(n.guid, !n.pinned) }

  IosBar {
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack) {
        Icon(painterResource(R.drawable.ic_back), contentDescription = "文件夹", tint = palette.accent)
      }
      Text("文件夹", style = MaterialTheme.typography.bodyLarge, color = palette.accent)
    }
    if (!trash && query.isBlank()) {
      IconButton(onClick = onNew) {
        Icon(painterResource(R.drawable.ic_edit), contentDescription = "新备忘录", tint = palette.accent)
      }
    }
    Box {
      IconButton(onClick = { menu = true }) {
        Icon(painterResource(R.drawable.ic_more), contentDescription = "更多", tint = palette.accent)
      }
      DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = palette.card) {
        if (!trash && query.isBlank()) DropdownMenuItem(
          text = { Text(if (view == "list") "画廊视图" else "列表视图") },
          onClick = { menu = false; view = if (view == "list") "gallery" else "list" },
        )
        if (own) {
          DropdownMenuItem(text = { Text("重新命名文件夹") }, onClick = { menu = false; onRename() })
          DropdownMenuItem(text = { Text("删除文件夹", color = palette.delete) }, onClick = { menu = false; onDelete() })
        }
      }
    }
  }
  Column(Modifier.padding(horizontal = 20.dp)) {
    Text(folder.title, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(
      when {
        query.isNotBlank() -> "${notes.size} 个搜索结果"
        notes.size == 1 -> "1 个备忘录"
        notes.isEmpty() -> "没有备忘录"
        else -> "${notes.size} 个备忘录"
      },
      style = MaterialTheme.typography.bodyMedium, color = palette.secondary,
    )
    IosSearch(query, onQuery, palette, Modifier.padding(top = 8.dp, bottom = 4.dp))
  }
  if (notes.isEmpty()) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(if (query.isBlank()) "没有备忘录" else "没有找到备忘录", color = palette.secondary)
    }
  } else if (view == "gallery" && query.isBlank()) {
    Gallery(notes, palette, onOpen = onOpen, onChoose = { chosen = it })
  } else {
    NoteList(notes, grouped = query.isBlank(), palette, pinnable = !trash, onOpen = onOpen, onChoose = { chosen = it }, onPin = ::pin, onRemove = ::remove)
  }
  chosen?.let { n ->
    DropdownMenu(expanded = true, onDismissRequest = { chosen = null }, containerColor = palette.card) {
      if (!trash) DropdownMenuItem(
        text = { Text(if (n.pinned) "取消置顶" else "置顶") },
        onClick = { chosen = null; pin(n) },
      )
      DropdownMenuItem(text = { Text("删除", color = palette.delete) }, onClick = { chosen = null; remove(n) })
    }
  }
}

/** 备忘录卡片：一行标题，下面时间加摘要。画廊视图里是一张竖卡。 */
private fun noteSnippet(n: NItem) = if (n.locked) "已锁定" else n.snippet?.lineSequence()?.firstOrNull { it.isNotBlank() } ?: "没有更多文字"

@Composable
private fun NoteList(
  notes: List<NItem>,
  grouped: Boolean,
  palette: IosPalette,
  pinnable: Boolean = true,
  onOpen: (NItem) -> Unit,
  onChoose: (NItem) -> Unit,
  onPin: (NItem) -> Unit,
  onRemove: (NItem) -> Unit,
) {
  val today = LocalDate.now()
  val sections = if (grouped) notes.groupBy { bucket(it, today) }.toList() else listOf("" to notes)
  LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
    for ((name, list) in sections) {
      if (name.isNotEmpty()) item(key = "h-$name") {
        Text(
          name, style = MaterialTheme.typography.titleSmall, color = palette.secondary,
          modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 6.dp),
        )
      }
      groupItems(list, key = { "n-${it.guid}" }, inset = 16) { n ->
        SwipeRow(n, palette, pinnable, onOpen = onOpen, onChoose = onChoose, onPin = onPin, onRemove = onRemove)
      }
    }
  }
}

/** 一行备忘录，左右滑动是 iOS 的两个操作：右滑置顶（黄），左滑删除（红）。 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SwipeRow(
  n: NItem,
  palette: IosPalette,
  pinnable: Boolean = true,
  onOpen: (NItem) -> Unit,
  onChoose: (NItem) -> Unit,
  onPin: (NItem) -> Unit,
  onRemove: (NItem) -> Unit,
) {
  val state = rememberSwipeToDismissBoxState(
    confirmValueChange = { v ->
      when (v) {
        SwipeToDismissBoxValue.StartToEnd -> { if (pinnable) onPin(n); false }
        SwipeToDismissBoxValue.EndToStart -> { onRemove(n); true }
        else -> false
      }
    },
  )
  SwipeToDismissBox(
    state = state,
    enableDismissFromStartToEnd = pinnable,
    enableDismissFromEndToStart = true,
    backgroundContent = {
      val (icon, colour, atStart) = when (state.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Triple(R.drawable.ic_pin, palette.accent, true)
        else -> Triple(R.drawable.ic_trash, palette.delete, false)
      }
      Box(
        Modifier.fillMaxSize().background(colour).padding(horizontal = 20.dp),
        contentAlignment = if (atStart) Alignment.CenterStart else Alignment.CenterEnd,
      ) { Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
    },
  ) {
    var menu by remember { mutableStateOf(false) }
    Box {
      Column(
        Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(n) }, onLongClick = { menu = true })
          .padding(horizontal = 14.dp, vertical = 10.dp),
      ) {
        Text(n.title.ifBlank { "新备忘录" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.padding(top = 2.dp)) {
          Text(changed(n.modifiedAt), style = MaterialTheme.typography.bodyMedium, color = palette.secondary)
          Spacer(Modifier.width(8.dp))
          Text(noteSnippet(n), style = MaterialTheme.typography.bodyMedium, color = palette.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
      DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = palette.card) {
        DropdownMenuItem(text = { Text(if (n.pinned) "取消置顶" else "置顶") }, onClick = { menu = false; onPin(n) })
        DropdownMenuItem(text = { Text("删除", color = palette.delete) }, onClick = { menu = false; onRemove(n) })
      }
    }
  }
}

/** 画廊：两列卡片，置顶的在最前。长按弹出菜单。 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun Gallery(
  notes: List<NItem>,
  palette: IosPalette,
  onOpen: (NItem) -> Unit,
  onChoose: (NItem) -> Unit,
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    gridItems(notes, key = { it.guid }) { n ->
      Surface(
        color = palette.card, shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(n) }, onLongClick = { onChoose(n) }),
      ) {
        Column(Modifier.padding(12.dp)) {
          Text(n.title.ifBlank { "新备忘录" }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
          Spacer(Modifier.size(6.dp))
          Text(noteSnippet(n), style = MaterialTheme.typography.bodySmall, color = palette.secondary, maxLines = 4, overflow = TextOverflow.Ellipsis)
          Spacer(Modifier.size(8.dp))
          Text(changed(n.modifiedAt), style = MaterialTheme.typography.bodySmall, color = palette.secondary)
        }
      }
    }
  }
}

/** The colours of Notes' coloured text: 1 purple, 2 pink, 3 orange, 4 mint, 5 blue. */
private fun emphasisColour(e: UInt?): Color? = when (e?.toInt()) {
  1 -> Color(0xFFAF52DE); 2 -> Color(0xFFFF2D55); 3 -> Color(0xFFFF9500); 4 -> Color(0xFF00C7BE); 5 -> Color(0xFF007AFF)
  else -> null
}

/**
 * 一篇备忘录打开的样子，复刻 iOS：顶栏是返回加所在文件夹，右侧编辑时是“完成”、否则是“更多”；
 * 日期居中；正文可读可点进去写。最近删除里只有恢复和立即删除。
 */
@Composable
private fun NoteEditor(
  engine: AccountInterface,
  opened: String?,
  folderGuid: String?,
  folders: List<NFolder>,
  onBack: () -> Unit,
  onProblem: (String) -> Unit,
) {
  val palette = iosPalette()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var guid by remember { mutableStateOf(opened) }
  var doc by remember { mutableStateOf<NDoc?>(null) }
  var editing by remember { mutableStateOf(opened == null) }
  var text by remember { mutableStateOf("") }
  var saved by remember { mutableStateOf("") }
  var moving by remember { mutableStateOf(false) }
  var menu by remember { mutableStateOf(false) }
  val folderTitle = folders.firstOrNull { it.guid == folderGuid }?.title ?: "备忘录"

  fun reason(t: Throwable) = (t as? uniffi.quince_mobile.EngineException.Failed)?.reason ?: "这篇备忘录没有保存下来。"
  fun load() = scope.launch {
    val g = guid ?: return@launch
    runCatching { withContext(Dispatchers.IO) { engine.note(g) } }
      .onSuccess { doc = it; if (!editing) { text = it.text; saved = it.text } }
      .onFailure { onProblem(reason(it)) }
  }
  fun save(then: () -> Unit = {}) = scope.launch {
    if (text != saved) {
      val g = guid
      runCatching {
        withContext(Dispatchers.IO) {
          // A new note is made with its first text; one left empty is never made.
          if (g == null) { if (text.isNotBlank()) guid = engine.noteCreate(folderGuid, text) } else engine.noteSave(g, text)
        }
      }.onSuccess { saved = text; syncNotes(context) }.onFailure { onProblem(reason(it)) }
    }
    then()
  }
  fun act(write: () -> Unit, after: () -> Unit = {}) = scope.launch {
    runCatching { withContext(Dispatchers.IO) { write() } }.onFailure { onProblem(reason(it)) }
    syncNotes(context); after()
  }

  LaunchedEffect(guid) { load() }
  // Typing is saved a moment after it stops, as Notes saves: never lost, never a save per key.
  LaunchedEffect(text, editing) {
    if (editing && text != saved) { delay(800); save() }
  }
  BackHandler { save { onBack() } }

  // A new note has no document yet: it is an empty one being typed.
  val d = doc ?: NDoc(guid ?: "", folderGuid, "", text, emptyList(), pinned = false, locked = false, editable = true, modifiedAt = null).takeIf { guid == null } ?: return
  val trashed = d.folderGuid == TRASH

  IosBar {
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = { save { onBack() } }) {
        Icon(painterResource(R.drawable.ic_back), contentDescription = "返回", tint = palette.accent)
      }
      Text(folderTitle, style = MaterialTheme.typography.bodyLarge, color = palette.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (!d.locked && editing) {
      TextButton(onClick = { save { editing = false; load() } }) { Text("完成", color = palette.accent) }
    } else if (!d.locked) {
      Box {
        IconButton(onClick = { menu = true }) {
          Icon(painterResource(R.drawable.ic_more), contentDescription = "更多", tint = palette.accent)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = palette.card) {
          if (trashed) {
            DropdownMenuItem(text = { Text("恢复") }, onClick = { menu = false; act({ engine.noteMove(guid!!, null) }, onBack) })
            DropdownMenuItem(text = { Text("立即删除", color = palette.delete) }, onClick = { menu = false; act({ engine.noteDelete(guid!!) }, onBack) })
          } else {
            DropdownMenuItem(
              text = { Text(if (d.pinned) "取消置顶" else "置顶") },
              onClick = { menu = false; act({ engine.notePin(guid!!, !d.pinned) }) { load() } },
            )
            DropdownMenuItem(text = { Text("移动到…") }, onClick = { menu = false; moving = true })
            DropdownMenuItem(text = { Text("删除", color = palette.delete) }, onClick = { menu = false; act({ engine.noteDelete(guid!!) }, onBack) })
          }
        }
      }
    }
  }
  Column(Modifier.fillMaxSize().imePadding()) {
    // When it last changed, small and centred under the bar, as iOS shows it.
    d.modifiedAt?.let { at ->
      Text(
        Instant.ofEpochSecond(at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
        style = MaterialTheme.typography.bodySmall, color = palette.secondary,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), textAlign = TextAlign.Center,
      )
    }
    when {
      d.locked -> Text(
        "此备忘录已锁定。请在 iPhone、iPad 或 Mac 上用它的密码打开。",
        style = MaterialTheme.typography.bodyLarge, color = palette.secondary, modifier = Modifier.padding(24.dp),
      )
      editing -> {
        // The keyboard comes up with the editor, as Notes opens a new or tapped note ready to type.
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        BasicTextField(
          text, { text = it },
          // An attachment shows as 📎, not the font's dotted "OBJ" box (discussion #199).
          visualTransformation = AttachmentMarks,
          textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = 24.sp),
          cursorBrush = SolidColor(palette.accent),
          modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp).focusRequester(focus),
        )
      }
      else -> RichNote(
        d,
        engine,
        Modifier.fillMaxSize().clickable(enabled = d.editable && !trashed) { text = d.text; saved = d.text; editing = true },
      ) { at, checked ->
        val g = guid ?: return@RichNote
        act({ engine.noteCheck(g, at, checked) }) { load() }
      }
    }
  }
  if (moving) {
    val g = guid
    AlertDialog(
      onDismissRequest = { moving = false },
      containerColor = palette.card,
      title = { Text("移动备忘录") },
      text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
          // Notes' own folder (no folder) first, then the person's; Recently Deleted is not a place to move to.
          val choices = listOf<Pair<String?, String>>(null to "备忘录") + folders.filter { !it.trash }.map { it.guid to it.title }
          for ((to, title) in choices) {
            Row(
              Modifier.fillMaxWidth().clickable {
                moving = false
                if (g != null && to != d.folderGuid) act({ engine.noteMove(g, to) }) { load() }
              }.padding(vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(painterResource(R.drawable.ic_folder), contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
              Spacer(Modifier.width(14.dp))
              Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
              if (to == d.folderGuid) Icon(painterResource(R.drawable.ic_check), contentDescription = "在这里", tint = palette.accent, modifier = Modifier.size(18.dp))
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = { moving = false }) { Text("取消", color = palette.accent) } },
    )
  }
}

/** One line of a note, laid out once: where it starts, its text, its paragraph and the spans on it. */
private class NoteLine(val start: Int, val text: String, val para: NSpan?, val spans: List<NSpan>, val number: Int)

/**
 * A note's lines with the spans on each, found in one pass (each span placed on the lines it
 * covers by binary search), so a note of thousands of lines and spans lays out in milliseconds.
 */
private fun lay(d: NDoc): List<NoteLine> {
  val texts = d.text.split('\n')
  val starts = IntArray(texts.size)
  var at = 0
  texts.forEachIndexed { i, s -> starts[i] = at; at += s.length + 1 }
  val on = Array(texts.size) { mutableListOf<NSpan>() }
  for (s in d.spans) {
    val from = s.start.toInt(); val to = (s.start + s.length).toInt()
    if (to <= from) continue
    var i = starts.binarySearch(from).let { if (it >= 0) it else -it - 2 }.coerceAtLeast(0)
    while (i < texts.size && starts[i] < to) { on[i].add(s); i++ }
  }
  var numbered = 0
  return texts.indices.map { i ->
    val para = on[i].firstOrNull { starts[i] >= it.start.toInt() && starts[i] < (it.start + it.length).toInt() }
    numbered = if (para?.style?.toInt() == 102) numbered + 1 else 0
    NoteLine(starts[i], texts[i], para, on[i], numbered)
  }
}

/**
 * A note drawn with its formatting, each line by its paragraph style, checklist items tickable;
 * lines composed only as they scroll into view.
 */
@Composable
private fun RichNote(d: NDoc, engine: AccountInterface, modifier: Modifier, onCheck: (UInt, Boolean) -> Unit) {
  val lines = remember(d) { lay(d) }
  LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
    items(lines.size) { i -> NoteLineView(lines[i], engine, onCheck) }
  }
}

@Composable
private fun NoteLineView(l: NoteLine, engine: AccountInterface, onCheck: (UInt, Boolean) -> Unit) {
  val palette = iosPalette()
  val para = l.para
  // The attachments on this line, in order: each U+FFFC is one, named by the span over it.
  val attachments = remember(l) {
    l.text.indices.filter { l.text[it] == '\uFFFC' }.mapNotNull { i ->
      val at = l.start + i
      l.spans.firstOrNull { it.attachment != null && at >= it.start.toInt() && at < (it.start + it.length).toInt() }?.attachment
    }
  }
  val style = when (para?.style?.toInt()) {
    0 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
    1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    4 -> MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    else -> MaterialTheme.typography.bodyLarge
  }
  val styled = buildAnnotatedString {
    // Each attachment's character kept as one that draws nothing, so every style after it stays
    // on its own words; the attachment itself is drawn below the line's words.
    append(l.text.replace('\uFFFC', '\u200B'))
    // Character formatting, stretch by stretch, clipped to this line.
    for (s in l.spans) {
      val from = maxOf(s.start.toInt(), l.start) - l.start
      val to = minOf((s.start + s.length).toInt(), l.start + l.text.length) - l.start
      if (to <= from) continue
      addStyle(
        SpanStyle(
          fontWeight = if (s.bold) FontWeight.Bold else null,
          fontStyle = if (s.italic) FontStyle.Italic else null,
          textDecoration = when {
            s.underline && s.strike -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
            s.underline || s.link != null -> TextDecoration.Underline
            s.strike -> TextDecoration.LineThrough
            else -> null
          },
          color = emphasisColour(s.emphasis) ?: if (s.link != null) palette.accent else Color.Unspecified,
        ),
        from, to,
      )
    }
  }
  val indent = (para?.indent?.toInt() ?: 0) * 20
  Row(Modifier.padding(start = indent.dp), verticalAlignment = Alignment.Top) {
    when (para?.style?.toInt()) {
      103 -> {
        // Notes' checklist circle: hollow, or filled yellow with a check.
        val ticked = para.checked == true
        Box(
          Modifier.padding(top = 3.dp, end = 10.dp).size(20.dp)
            .border(1.75.dp, if (ticked) palette.accent else palette.secondary, androidx.compose.foundation.shape.CircleShape)
            .background(if (ticked) palette.accent else Color.Transparent, androidx.compose.foundation.shape.CircleShape)
            .toggleable(ticked, role = Role.Checkbox) { onCheck(l.start.toUInt(), it) },
          contentAlignment = Alignment.Center,
        ) { if (ticked) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp)) }
      }
      100 -> Text("•  ", style = style)
      101 -> Text("–  ", style = style)
      102 -> Text("${l.number}.  ", style = style)
    }
    if (para?.quote == true) Box(Modifier.padding(end = 10.dp).size(3.dp, 22.dp).background(palette.separator))
    // A line that is only its attachments shows them alone, without an empty line of text.
    if (attachments.isEmpty() || l.text.any { it != '\uFFFC' && !it.isWhitespace() }) {
      Text(styled, style = style.copy(lineHeight = (style.fontSize.value * 1.35f).sp))
    }
  }
  for (id in attachments) NoteAttachmentView(id, engine)
}

/**
 * One attachment in a note, as Notes shows it: a picture or drawing drawn in the note, a tag
 * or mention as its words, anything else (a PDF, a scan, a file) a card that opens in the app
 * the phone uses for it. Its kind is read once and kept by the engine; the picture is fetched
 * once into the cache.
 */
@Composable
private fun NoteAttachmentView(id: String, engine: AccountInterface) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var info by remember(id) { mutableStateOf<NAttachment?>(null) }
  var picture by remember(id) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
  var problem by remember(id) { mutableStateOf<String?>(null) }
  val key = id.filter { it.isLetterOrDigit() }
  LaunchedEffect(id) {
    runCatching { withContext(Dispatchers.IO) { engine.noteAttachment(id) } }
      .onSuccess { a ->
        info = a
        if (a.kind == "image" || a.kind == "drawing" || a.hasPreview) {
          picture = runCatching {
            withContext(Dispatchers.IO) {
              val file = java.io.File(context.cacheDir, "note-pictures/$key")
              // Apple's picture of it; a photo just added has none yet (Apple makes it later),
              // and a photo's own file is its picture.
              val got = runCatching { engine.noteAttachmentFile(id, NPart.PICTURE, file.path) }
              if (got.isFailure && a.kind == "image") engine.noteAttachmentFile(id, NPart.FILE, file.path)
              else got.getOrThrow()
              decodeScaled(file, 1600)
            }
          }.getOrNull()
        }
      }
      .onFailure { problem = "此附件无法从 iCloud 读取。" }
  }
  fun open(a: NAttachment) = scope.launch {
    runCatching {
      withContext(Dispatchers.IO) {
        // In the cache's share/ folder: the one place the app hands files to other apps from.
        val file = java.io.File(context.cacheDir, "share/note-files/$key/${a.name}")
        engine.noteAttachmentFile(id, NPart.FILE, file.path)
        file
      }
    }.onSuccess { file ->
      val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.share", file)
      val type = android.webkit.MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(a.name.substringAfterLast('.', "").lowercase()) ?: "application/octet-stream"
      val view = android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri, type)
        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
      try {
        context.startActivity(android.content.Intent.createChooser(view, a.name).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
      } catch (_: android.content.ActivityNotFoundException) {
        problem = "此手机上没有应用可以打开 ${a.name.substringAfterLast('.', "这类")} 文件。"
      }
    }.onFailure { problem = "此附件无法从 iCloud 下载。" }
  }
  val a = info
  val shape = RoundedCornerShape(12.dp)
  Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
    when {
      a == null && problem == null -> Box(Modifier.fillMaxWidth().size(height = 56.dp, width = 0.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape))
      a == null -> {}
      a.kind == "inline" -> Text(a.title ?: "", style = MaterialTheme.typography.bodyLarge, color = iosPalette().accent)
      (a.kind == "image" || a.kind == "drawing") && picture != null -> {
        val p = picture!!
        androidx.compose.foundation.Image(
          p, contentDescription = a.name,
          modifier = Modifier.fillMaxWidth().aspectRatio(p.width.toFloat() / p.height.coerceAtLeast(1))
            .clip(shape).clickable { open(a) },
          contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
      }
      else -> Row(
        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
          .clickable(enabled = a.kind != "table") { open(a) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val p = picture
        if (p != null) androidx.compose.foundation.Image(
          p, contentDescription = null,
          modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
          contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        ) else Icon(
          painterResource(attachmentIcon(a)), contentDescription = null,
          tint = iosPalette().accent, modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
          Text(if (a.kind == "table") "表格" else a.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
          val under = when {
            a.kind == "table" -> "在 iPhone、iPad 或 Mac 上的备忘录里打开"
            else -> a.size?.let { android.text.format.Formatter.formatShortFileSize(context, it) }
          }
          if (under != null) Text(under, style = MaterialTheme.typography.bodySmall, color = iosPalette().secondary)
        }
      }
    }
    problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = iosPalette().delete, modifier = Modifier.padding(top = 4.dp)) }
  }
}

/** The icon a file's card shows for what kind of file it is. */
private fun attachmentIcon(a: NAttachment): Int = when {
  a.kind == "audio" -> R.drawable.ic_file_audio
  a.kind == "video" -> R.drawable.ic_file_video
  a.kind == "image" || a.kind == "drawing" -> R.drawable.ic_file_image
  a.uti.contains("pdf") || a.kind == "document" -> R.drawable.ic_file_text
  a.uti.contains("spreadsheet") || a.uti.contains("numbers") -> R.drawable.ic_file_sheet
  a.uti.contains("presentation") || a.uti.contains("keynote") -> R.drawable.ic_file_slides
  else -> R.drawable.ic_file
}

/** A picture read at no more than `most` pixels a side, so a 48-megapixel photo never fills memory. */
private fun decodeScaled(file: java.io.File, most: Int): androidx.compose.ui.graphics.ImageBitmap? {
  val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
  android.graphics.BitmapFactory.decodeFile(file.path, bounds)
  var sample = 1
  while (bounds.outWidth / (sample * 2) >= most || bounds.outHeight / (sample * 2) >= most) sample *= 2
  val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
  return android.graphics.BitmapFactory.decodeFile(file.path, opts)?.asImageBitmap()
}

/**
 * The editor's view of a note's text: each attachment's character (U+FFFC, drawn by most fonts
 * as a dotted "OBJ" box) shown as 📎, two characters for one, with every cursor position mapped
 * across, so what is typed and saved is the note's own text, attachments in place.
 */
internal object AttachmentMarks : androidx.compose.ui.text.input.VisualTransformation {
  private const val MARK = "\uD83D\uDCCE"
  override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
    val raw = text.text
    if ('\uFFFC' !in raw) return androidx.compose.ui.text.input.TransformedText(text, androidx.compose.ui.text.input.OffsetMapping.Identity)
    val marks = raw.indices.filter { raw[it] == '\uFFFC' }
    val shown = raw.replace("\uFFFC", MARK)
    val mapping = object : androidx.compose.ui.text.input.OffsetMapping {
      override fun originalToTransformed(offset: Int) = offset + marks.count { it < offset }
      override fun transformedToOriginal(offset: Int): Int {
        var extra = 0
        for ((k, m) in marks.withIndex()) {
          // The k-th mark is shown from m + k, two places wide; a place inside it is before it.
          val shownAt = m + k
          when {
            offset >= shownAt + 2 -> extra++
            offset == shownAt + 1 -> return m
            else -> break
          }
        }
        return (offset - extra).coerceIn(0, raw.length)
      }
    }
    return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(shown), mapping)
  }
}
