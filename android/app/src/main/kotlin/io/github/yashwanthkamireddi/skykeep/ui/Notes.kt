package io.github.yashwanthkamireddi.skykeep.ui

import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import io.github.yashwanthkamireddi.skykeep.ui.theme.Palette
import android.content.ContentResolver
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
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
    // 设置页的“上次同步”用同一份记录。
    context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
      .edit().putString("lastSync", (System.currentTimeMillis() / 1000).toString()).apply()
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

/** 导航的深度：文件夹 0，列表 1，编辑 2。转场方向由它决定，如 iOS 的推入与弹出。 */
private fun NPlace.depth(): Int = when (this) {
  NPlace.Folders -> 0
  is NPlace.Folder -> 1
  is NPlace.Open -> 2
}

/**
 * 备忘录。布局复刻 iOS 备忘录：文件夹页、笔记列表（置顶与时间分组、滑动置顶与删除、长按菜单、
 * 列表与画廊两种视图）、可读可写的编辑页。每次改动立刻上屏，后台发往 iCloud；格式由引擎保全。
 */
@Composable
fun NotesPage(
  engine: AccountInterface,
  onOpenSettings: () -> Unit = {},
  newNoteLaunch: Boolean = false,
  onNewNoteConsumed: () -> Unit = {},
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val palette = iosPalette()
  var place by rememberSaveable(stateSaver = NPlaceSaver) { mutableStateOf<NPlace>(NPlace.Folders) }
  // 长按图标的新备忘录：一进来就在 Apple 的“备忘录”文件夹里开一篇新的。
  LaunchedEffect(newNoteLaunch) {
    if (newNoteLaunch) {
      place = NPlace.Open(null, NPlace.Folder(DEFAULT_FOLDER, "备忘录"))
      onNewNoteConsumed()
    }
  }
  var folders by remember { mutableStateOf<List<NFolder>>(emptyList()) }
  var notes by remember { mutableStateOf<List<NItem>>(emptyList()) }
  var query by remember { mutableStateOf("") }
  // 搜索是一个模式：点搜索框进入（标题栏消失、搜索框上移、右侧出现取消），取消或返回退出。
  var searching by rememberSaveable { mutableStateOf(false) }
  fun cancelSearch() { searching = false; query = "" }
  var problem by remember { mutableStateOf<String?>(null) }
  var generation by remember { mutableIntStateOf(0) }
  // 第一次读取还没有落地：先不画任何屏，免得闪一眼“所有 iCloud 0”的空样子。
  var ready by remember { mutableStateOf(false) }
  var newFolder by remember { mutableStateOf(false) }
  var renaming by remember { mutableStateOf<NFolder?>(null) }
  var dropping by remember { mutableStateOf<NFolder?>(null) }
  // 文件夹页的编辑（排序）模式：菜单“编辑”进入，“完成”退出。
  var editingFolders by rememberSaveable { mutableStateOf(false) }
  // 列表页的选择模式：FAB 要让位（提升到这层，选择时隐藏）。
  var selectingNotes by rememberSaveable { mutableStateOf(false) }

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
        folders = f; notes = n; problem = null; ready = true
      } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
      } catch (t: Throwable) {
        problem = reason(t); ready = true
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
  BackHandler(enabled = searching) { cancelSearch() }
  BackHandler(enabled = editingFolders) { editingFolders = false }
  BackHandler(enabled = !searching && !editingFolders && place is NPlace.Folder) { place = NPlace.Folders; query = "" }

  Box(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding()) {
    Column(Modifier.fillMaxSize()) {
      problem?.let { Text(it, color = palette.delete, modifier = Modifier.padding(20.dp, 12.dp)) }
      // iOS 的推入转场：向深处走时新页从右侧滑入、旧页向左让出三分之一；返回时相反。
      AnimatedContent(
        targetState = place,
        transitionSpec = {
          val spec = tween<androidx.compose.ui.unit.IntOffset>(300)
          if (targetState.depth() >= initialState.depth()) {
            slideInHorizontally(spec) { it } togetherWith slideOutHorizontally(spec) { -it / 3 }
          } else {
            slideInHorizontally(spec) { -it / 3 } togetherWith slideOutHorizontally(spec) { it }
          }
        },
        label = "notes-navigation",
        modifier = Modifier.fillMaxSize(),
      ) { p ->
        // 转场期间新旧两页同时在场：每页用纵向容器铺上自己的底色，否则两屏会透叠错位。
        // 各屏（顶栏、标题、列表）本是 Column 的子元素，这里也必须按纵向摆放。
        Column(Modifier.fillMaxSize().background(palette.bg)) {
          when {
            // 等第一次读取落地再画，冷启动就不会先闪空数据。
            !ready -> {}
            else -> when (p) {
          is NPlace.Open -> NoteEditor(
            engine, p.guid, p.from.guid, folders,
            onBack = { place = p.from; generation++ },
            onProblem = { problem = it },
          )
          is NPlace.Folder -> NoteListScreen(
            engine, p, folders, notes, query, palette,
            searching = searching,
            selecting = selectingNotes,
            onSelecting = { selectingNotes = it },
            onSearchStart = { searching = true },
            onCancelSearch = { cancelSearch() },
            onQuery = { query = it },
            onBack = { place = NPlace.Folders; query = "" },
            onOpen = { n -> place = NPlace.Open(n.guid, p) },
            onNew = { place = NPlace.Open(null, p) },
            onRename = { renaming = folders.firstOrNull { it.guid == p.guid && !it.trash } },
            onGeneration = { generation++ },
            onProblem = { problem = it },
          )
          NPlace.Folders -> FolderScreen(
            folders, notes, query, palette,
            searching = searching,
            editing = editingFolders,
            onEditing = { editingFolders = it },
            onSearchStart = { searching = true },
            onCancelSearch = { cancelSearch() },
            onQuery = { query = it },
            onOpen = { place = it },
            onOpenNote = { n -> place = NPlace.Open(n.guid, NPlace.Folder(null, "所有 iCloud")) },
            onNewFolder = { newFolder = true },
            onSettings = onOpenSettings,
            onDeleteFolder = { f -> dropping = f },
          )
          }
        }
      }
      }
    }
    // 右下角悬浮的新建备忘录：嵌在转场之上，文件夹页与列表页之间不消失；选择/搜索/编辑排序时让位。
    val cur = place
    val showFab = ready && !searching && !editingFolders && !selectingNotes && when (cur) {
      NPlace.Folders -> true
      is NPlace.Folder -> folders.firstOrNull { it.guid == cur.guid }?.trash != true
      else -> false
    }
    if (showFab) {
      Fab(
        onClick = {
          when (val p = place) {
            is NPlace.Folder -> place = NPlace.Open(null, p)
            else -> place = NPlace.Open(null, NPlace.Folder(DEFAULT_FOLDER, "备忘录"))
          }
        },
        palette, Modifier.align(Alignment.BottomEnd),
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

/** Apple 系统的“备忘录”文件夹：位置固定，如 iOS 把“所有 iCloud”与“备忘录”钉在顶部。 */
private const val DEFAULT_FOLDER = "DefaultFolder-CloudKit"

/**
 * Apple 的“快速备忘录”文件夹：在 iCloud 里是一条普通 Folder 记录，引擎没有专门的 guid，
 * 只能认它的名字（账号语言的原文，英文账号是 "Quick Notes"）。它按 iOS 置顶显示，位置固定。
 */
private const val QUICK_FOLDER_TITLE = "Quick Notes"

/** 系统文件夹的显示名按中文（引擎给的是账号语言的原文）；自建的照原样。 */
private fun folderDisplay(f: NFolder) = when {
  f.guid == DEFAULT_FOLDER -> "备忘录"
  f.title == QUICK_FOLDER_TITLE -> "快速备忘录"
  else -> f.title
}

/** 自建文件夹的顺序（编辑模式拖出来的），按 guid 存在本机，如 iOS 存它自己的顺序。 */
private fun savedFolderOrder(context: android.content.Context): List<String> =
  context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    .getString("folderOrder", null)?.split(',')?.filter { it.isNotBlank() } ?: emptyList()

private fun saveFolderOrder(context: android.content.Context, order: List<String>) {
  context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    .edit().putString("folderOrder", order.joinToString(",")).apply()
}

/** 通用设置（备忘录排序、按日期分组、新备忘录开头、勾选排序），与本机顺序同库。 */
fun savedSetting(context: android.content.Context, key: String, def: String): String =
  context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    .getString(key, null) ?: def

fun saveSetting(context: android.content.Context, key: String, value: String) {
  context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    .edit().putString(key, value).apply()
}

/** 一个文件夹在文件夹页的一行。 */
private data class FolderRow(val title: String, val count: Int, val icon: Int, val place: NPlace.Folder, val fixed: Boolean = false)

/** 文件夹页的分两组：快速备忘录自己一张卡（iOS 置顶的独立区块），其余一张卡。 */
private class FolderSections(val quick: List<FolderRow>, val main: List<FolderRow>)

private fun folderSections(folders: List<NFolder>, order: List<String>): FolderSections {
  val all = folders.filter { !it.trash }.sumOf { it.count.toInt() }
  val quick = folders.filter { it.guid != DEFAULT_FOLDER && it.title == QUICK_FOLDER_TITLE && !it.trash }
  val system = folders.filter { it.guid == DEFAULT_FOLDER && !it.trash }
  val user = folders.filter { it.guid != DEFAULT_FOLDER && it.title != QUICK_FOLDER_TITLE && !it.trash }
  val ordered = order.mapNotNull { g -> user.firstOrNull { it.guid == g } } + user.filterNot { f -> order.contains(f.guid) }
  val quickRows = quick.map { FolderRow(folderDisplay(it), it.count.toInt(), R.drawable.ic_folder, NPlace.Folder(it.guid, folderDisplay(it)), fixed = true) }
  val main = buildList {
    add(FolderRow("所有 iCloud", all, R.drawable.ic_folder, NPlace.Folder(null, "所有 iCloud"), fixed = true))
    system.forEach { add(FolderRow(folderDisplay(it), it.count.toInt(), R.drawable.ic_folder, NPlace.Folder(it.guid, folderDisplay(it)), fixed = true)) }
    ordered.forEach { add(FolderRow(folderDisplay(it), it.count.toInt(), R.drawable.ic_folder, NPlace.Folder(it.guid, folderDisplay(it)))) }
    folders.firstOrNull { it.trash }?.let { add(FolderRow("最近删除", it.count.toInt(), R.drawable.ic_trash, NPlace.Folder(it.guid, "最近删除"), fixed = true)) }
  }
  return FolderSections(quickRows, main)
}

/** 编辑模式把两组合成一列（固定的行置灰）。 */
private fun folderRows(folders: List<NFolder>, order: List<String>): List<FolderRow> =
  folderSections(folders, order).let { it.quick + it.main }

/** 文件夹页一行（普通或编辑模式共用）。置灰的是固定行（编辑模式不能动）；grip 是编辑模式的排序把手。 */
@Composable
private fun FolderRowView(r: FolderRow, palette: IosPalette, dimmed: Boolean = false, grip: Boolean = false, onClick: (() -> Unit)? = { }) {
  Row(
    Modifier.fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable(enabled = !dimmed) { onClick() } else Modifier)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(painterResource(r.icon), contentDescription = null, tint = if (dimmed) palette.secondary else palette.accent, modifier = Modifier.size(22.dp))
    Spacer(Modifier.width(14.dp))
    Text(r.title, style = MaterialTheme.typography.bodyLarge, color = if (dimmed) palette.secondary else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text("${r.count}", style = MaterialTheme.typography.bodyLarge, color = palette.secondary)
    if (grip) {
      Spacer(Modifier.width(12.dp))
      Icon(painterResource(R.drawable.ic_reorder), contentDescription = null, tint = palette.secondary, modifier = Modifier.size(20.dp))
    } else {
      Spacer(Modifier.width(6.dp))
      Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = palette.secondary, modifier = Modifier.size(16.dp))
    }
  }
}

/** 编辑模式下的拖拽排序：按住一行上下拖，越过邻行就交换。 */
private fun <T> MutableList<T>.swapAt(a: Int, b: Int) {
  if (a !in indices || b !in indices || a == b) return
  val t = this[a]; this[a] = this[b]; this[b] = t
}

/** The folders, as iOS lists them: All iCloud, each folder, Recently Deleted, one white card. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FolderScreen(
  folders: List<NFolder>,
  notes: List<NItem>,
  query: String,
  palette: IosPalette,
  searching: Boolean,
  editing: Boolean,
  onEditing: (Boolean) -> Unit,
  onSearchStart: () -> Unit,
  onCancelSearch: () -> Unit,
  onQuery: (String) -> Unit,
  onOpen: (NPlace.Folder) -> Unit,
  onOpenNote: (NItem) -> Unit,
  onNewFolder: () -> Unit,
  onSettings: () -> Unit,
  onDeleteFolder: (NFolder) -> Unit,
) {
  val context = LocalContext.current
  if (searching) {
    // 搜索模式：标题栏消失，搜索框上移到顶，右侧是取消；输入后才出现结果。
    SearchRow(query, onQuery, palette, onCancelSearch, Modifier.padding(top = 4.dp))
    if (query.isNotBlank()) {
      if (notes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("没有找到备忘录", color = palette.secondary) }
      } else {
        NoteList(notes, grouped = false, palette, pinnable = false, onOpen = onOpenNote, onChoose = {})
      }
    }
    return
  }
  val listState = rememberLazyListState()
  // 大标题滚出屏幕后，顶栏中间淡入小标题，如 iOS 的收起。
  val collapsed = listState.firstVisibleItemIndex > 0
  // 左滑动作一次只开一行：滚动、滑开另一行、点别的行，都会把开着的收起。
  var openSwipe by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(listState.isScrollInProgress) {
    if (listState.isScrollInProgress) openSwipe = null
  }
  if (editing) {
    // 编辑模式：整张列表照旧（固定的行置灰），自建文件夹右侧有把手，按住拖动排序。
    // 拖动按列追踪手指的绝对位置：行换位后把手位置跟着换，手指不松，行不脱手。
    val scroll = rememberScrollState()
    var rows by remember(folders) { mutableStateOf(folderRows(folders, savedFolderOrder(context))) }
    var dragIndex by remember { mutableStateOf(-1) }
    var touchShift by remember { mutableFloatStateOf(0f) }
    var fingerY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val rowHeight = with(density) { 56.dp.toPx() }
    fun save() = saveFolderOrder(context, rows.filterNot { it.fixed }.mapNotNull { it.place.guid })
    IosBar {
      Spacer(Modifier.weight(1f))
      TextButton(onClick = { save(); onEditing(false) }) { Text("完成", color = palette.accent) }
    }
    Column(
      Modifier.fillMaxSize().verticalScroll(scroll).pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
          onDragStart = { at ->
            val i = ((at.y + scroll.value) / rowHeight).toInt()
            if (i in rows.indices && !rows[i].fixed) {
              dragIndex = i
              touchShift = at.y % rowHeight
              fingerY = at.y + scroll.value
            }
          },
          onDrag = { change, drag ->
            change.consume()
            if (dragIndex >= 0) {
              fingerY += drag.y
              val to = (fingerY / rowHeight).toInt().coerceIn(0, rows.lastIndex)
              if (to != dragIndex && !rows[to].fixed) {
                rows = rows.toMutableList().also { it.swapAt(dragIndex, to) }
                dragIndex = to
              }
            }
          },
          onDragEnd = { if (dragIndex >= 0) save(); dragIndex = -1 },
          onDragCancel = { dragIndex = -1 },
        )
      }.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      rows.forEachIndexed { i, r ->
        val dragging = dragIndex == i
        Row(
          Modifier.fillMaxWidth().height(56.dp)
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
              translationY = if (dragging) fingerY - touchShift - i * rowHeight + scroll.value else 0f
            }
            .background(palette.card, if (dragging) MaterialTheme.shapes.large else androidx.compose.ui.graphics.RectangleShape),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FolderRowView(r, palette, dimmed = r.fixed, grip = !r.fixed, onClick = null)
        }
      }
    }
    return
  }
  IosBar {
    Spacer(Modifier.weight(1f))
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
      Text("文件夹", style = MaterialTheme.typography.bodyMedium, color = palette.secondary,
        modifier = Modifier.graphicsLayer { alpha = if (collapsed) 1f else 0f })
    }
    // 右上角依次：新建文件夹、排序、设置（直接进设置页），紧凑排开、贴着右边。
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onNewFolder, modifier = Modifier.size(40.dp)) {
        Icon(painterResource(R.drawable.ic_folder_plus), contentDescription = "新建文件夹", tint = palette.accent, modifier = Modifier.size(22.dp))
      }
      IconButton(onClick = { onEditing(true) }, modifier = Modifier.size(40.dp)) {
        Icon(painterResource(R.drawable.ic_sort), contentDescription = "编辑", tint = palette.accent, modifier = Modifier.size(22.dp))
      }
      IconButton(onClick = onSettings, modifier = Modifier.size(40.dp)) {
        Icon(painterResource(R.drawable.ic_settings), contentDescription = "设置", tint = palette.accent, modifier = Modifier.size(22.dp))
      }
    }
  }
  // 大标题与搜索框随内容滚动：搜索框触顶后停在顶栏下（sticky），如 iOS。
  Box(Modifier.fillMaxSize()) {
  LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)) {
    item(key = "title") {
      Text("文件夹", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 4.dp))
    }
    stickyHeader(key = "search") {
      Box(Modifier.background(palette.bg).padding(top = 8.dp, bottom = 12.dp)) {
        IosSearch(query, onQuery, palette, Modifier.fillMaxWidth(), onActivated = onSearchStart)
      }
    }
    // 快速备忘录是 iOS 置顶的独立区块：自己一张卡，与 iCloud 的文件夹之间留出间隔。
    val sections = folderSections(folders, savedFolderOrder(context))
    if (sections.quick.isNotEmpty()) {
      groupItems(sections.quick, key = { it.title }, inset = 54) { r ->
        FolderRowView(r, palette) { onOpen(r.place) }
      }
      item(key = "icloud-label") {
        Text("iCloud", style = MaterialTheme.typography.titleSmall, color = palette.secondary,
          modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 6.dp))
      }
    }
    groupItems(sections.main, key = { it.title }, inset = 54) { r ->
      val folder = folders.firstOrNull { it.guid == r.place.guid }
      if (r.fixed || folder == null) {
        FolderRowView(r, palette) { onOpen(r.place) }
      } else {
        // iOS 的左滑：滑出一截停下，露出“移动”与“删除”两个按钮。移动要子文件夹（引擎
        // 还没暴露），先置灰；点删除弹确认，进“最近删除”。一次只开一行：滑开这行时别的
        // 行收起；开着的行被滚动或点了别的行，也会收起（点别的行不进入，先收起）。
        val open = openSwipe == r.place.guid
        var dragging by remember { mutableStateOf(false) }
        var dragX by remember { mutableFloatStateOf(0f) }
        val actionPx = with(LocalDensity.current) { 72.dp.toPx() }
        val revealPx = actionPx * 2
        val shown by animateFloatAsState(
          targetValue = if (dragging) dragX else if (open) revealPx else 0f,
          animationSpec = if (dragging) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(200),
          label = "folder-swipe",
        )
        val dimBg = if (isSystemInDarkTheme()) Color(0xFF48484A) else Color(0xFFD1D1D6)
        Box(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
          Row(
            Modifier.align(Alignment.CenterEnd)
              .width(with(LocalDensity.current) { revealPx.toDp() }).fillMaxHeight(),
          ) {
            FolderSwipeAction(R.drawable.ic_folder, "移动", dimBg, palette.secondary, enabled = false, onClick = {})
            FolderSwipeAction(R.drawable.ic_trash, "删除", palette.delete, Color.White) {
              openSwipe = null
              onDeleteFolder(folder)
            }
          }
          Row(
            Modifier.fillMaxWidth()
              .graphicsLayer { translationX = -shown }
              .background(MaterialTheme.colorScheme.surfaceContainer)
              .pointerInput(r.place.guid) {
                detectHorizontalDragGestures(
                  onDragStart = { dragging = true; dragX = if (open) revealPx else 0f },
                  onHorizontalDrag = { change, amount ->
                    change.consume()
                    dragX = (dragX - amount).coerceIn(0f, revealPx)
                    // 一开拖就占住这一行，别行（若开着）随即收起。
                    if (dragX > 8f && openSwipe != r.place.guid) openSwipe = r.place.guid
                  },
                  onDragEnd = { dragging = false; openSwipe = if (dragX > revealPx / 2) r.place.guid else null },
                  onDragCancel = { dragging = false; openSwipe = if (dragX > revealPx / 2) r.place.guid else null },
                )
              }
              .clickable {
                when {
                  open -> openSwipe = null
                  openSwipe != null -> openSwipe = null // 有行开着：先收起，不进入
                  else -> onOpen(r.place)
                }
              },
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FolderRowView(r, palette, onClick = null)
          }
        }
      }
    }
  }
  }
}

/** iOS 的搜索框：圆角灰底，占位“搜索”。`onActivated` 在获得焦点（点进去）时触发，`focusNow` 直接弹键盘。 */
@Composable
private fun IosSearch(
  value: String,
  onChange: (String) -> Unit,
  palette: IosPalette,
  modifier: Modifier = Modifier,
  onActivated: () -> Unit = {},
  focusNow: Boolean = false,
) {
  val focus = remember { FocusRequester() }
  LaunchedEffect(focusNow) { if (focusNow) focus.requestFocus() }
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
        modifier = Modifier.weight(1f)
          .onFocusChanged { if (it.isFocused) onActivated() }
          .focusRequester(focus),
        decorationBox = { field ->
          if (value.isEmpty()) Text("搜索", style = MaterialTheme.typography.bodyLarge, color = palette.secondary)
          field()
        },
      )
    }
  }
}

/** 搜索模式下的那一行：搜索框加右侧的取消。 */
@Composable
private fun SearchRow(query: String, onQuery: (String) -> Unit, palette: IosPalette, onCancel: () -> Unit, modifier: Modifier = Modifier) {
  Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
    IosSearch(query, onQuery, palette, Modifier.weight(1f), focusNow = true)
    TextButton(onClick = onCancel) { Text("取消", color = palette.accent) }
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

/** 选中集合的保存：转屏后已选的还在。 */
private val StringSetSaver = listSaver<Set<String>, String>(
  save = { it.toList() },
  restore = { it.toSet() },
)

/** One folder's notes, as iOS lists or shows them in the gallery. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NoteListScreen(
  engine: AccountInterface,
  folder: NPlace.Folder,
  folders: List<NFolder>,
  notes: List<NItem>,
  query: String,
  palette: IosPalette,
  searching: Boolean,
  onSearchStart: () -> Unit,
  onCancelSearch: () -> Unit,
  onQuery: (String) -> Unit,
  onBack: () -> Unit,
  onOpen: (NItem) -> Unit,
  onNew: () -> Unit,
  onRename: () -> Unit,
  onGeneration: () -> Unit,
  onProblem: (String) -> Unit,
  selecting: Boolean,
  onSelecting: (Boolean) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val trash = folders.firstOrNull { it.guid == folder.guid }?.trash == true
  // 自建的文件夹才能改名：系统的两个（备忘录、快速备忘录）和回收站不行。
  val own = folders.firstOrNull { it.guid == folder.guid && !it.trash }?.let {
    it.guid != DEFAULT_FOLDER && it.title != QUICK_FOLDER_TITLE
  } == true
  var menu by remember { mutableStateOf(false) }
  var view by rememberSaveable { mutableStateOf("list") }
  var chosen by remember { mutableStateOf<NItem?>(null) }
  // 选择模式的状态在这层（NotesPage）：FAB 要跟着让位。
  var selected by rememberSaveable(stateSaver = StringSetSaver) { mutableStateOf<Set<String>>(emptySet()) }
  var moving by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()
  // 大标题滚出屏幕后，顶栏中间淡入文件夹名，如 iOS 的收起。
  val collapsed = listState.firstVisibleItemIndex > 0

  /** 长按或画廊里长按的唯一操作：置顶（回收站里没有）。删除在笔记打开后的“更多”里。 */
  fun pin(n: NItem) = scope.launch {
    runCatching { withContext(Dispatchers.IO) { engine.notePin(n.guid, !n.pinned) } }
      .onFailure { onProblem((it as? uniffi.quince_mobile.EngineException.Failed)?.reason ?: "没有送到 iCloud，稍后会再试。") }
    syncNotes(context); onGeneration()
  }
  BackHandler(enabled = selecting) { onSelecting(false); selected = emptySet() }

  if (searching) {
    // 搜索模式：顶栏与大标题都消失，搜索框上移，右侧是取消；输入后才出现结果。
    SearchRow(query, onQuery, palette, onCancelSearch, Modifier.padding(top = 4.dp))
    if (query.isNotBlank()) {
      if (notes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("没有找到备忘录", color = palette.secondary) }
      } else {
        NoteList(notes, grouped = false, palette, pinnable = true, onOpen = onOpen, onChoose = { chosen = it })
      }
    }
  } else if (selecting && !trash) {
    // 选择模式：顶栏是“全选”与“完成”，行前出现圆圈，底部是移动与删除，如 iOS。
    val all = notes.map { it.guid }.toSet()
    val allPicked = selected.containsAll(all)
    IosBar {
      TextButton(onClick = { selected = if (allPicked) emptySet() else all }) {
        Text(if (allPicked) "取消全选" else "全选", color = palette.accent)
      }
      Spacer(Modifier.weight(1f))
      Text("${selected.size}", style = MaterialTheme.typography.bodyLarge, color = palette.secondary)
      Spacer(Modifier.weight(1f))
      TextButton(onClick = { onSelecting(false); selected = emptySet() }) { Text("完成", color = palette.accent) }
    }
    Box(Modifier.fillMaxSize()) {
      NoteList(
        notes, grouped = true, palette, pinnable = false,
        selecting = true, isSelected = { selected.contains(it) },
        onToggle = { g -> selected = if (selected.contains(g)) selected - g else selected + g },
        onOpen = {}, onChoose = {},
        modifier = Modifier.fillMaxSize(), bottomPad = 110.dp,
      )
      Column(Modifier.align(Alignment.BottomCenter).background(palette.bg)) {
        IosBar {
          IconButton(enabled = selected.isNotEmpty(), onClick = { moving = true }) {
            Icon(painterResource(R.drawable.ic_folder), contentDescription = "移动",
              tint = if (selected.isEmpty()) palette.secondary else palette.accent)
          }
          Spacer(Modifier.weight(1f))
          IconButton(enabled = selected.isNotEmpty(), onClick = {
            val pick = selected; onSelecting(false); selected = emptySet()
            scope.launch {
              for (g in pick) {
                runCatching { withContext(Dispatchers.IO) { engine.noteDelete(g) } }
                  .onFailure { onProblem((it as? uniffi.quince_mobile.EngineException.Failed)?.reason ?: "没有送到 iCloud，稍后会再试。") }
              }
              syncNotes(context)
            }
          }) {
            Icon(painterResource(R.drawable.ic_trash), contentDescription = "删除",
              tint = if (selected.isEmpty()) palette.secondary else palette.delete)
          }
        }
      }
    }
  } else {
    IosBar {
      // “< 文件夹”紧贴成一组，如 iOS 顶栏；chevron 的画框左右各空一段，右移让它贴住文字。
      Row(
        Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(
          interactionSource = remember { MutableInteractionSource() }, indication = null,
          onClick = onBack,
        ).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(painterResource(R.drawable.ic_back), contentDescription = "文件夹", tint = palette.accent, modifier = Modifier.size(30.dp).graphicsLayer { translationX = 9.dp.toPx(); translationY = 1.5.dp.toPx() })
        Text("文件夹", style = MaterialTheme.typography.bodyLarge, color = palette.accent, modifier = Modifier.padding(start = 2.dp))
      }
      // 收起后文件夹名淡入顶栏中间，如 iOS。
      Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        if (collapsed) {
          Text(folder.title, style = MaterialTheme.typography.bodyLarge, color = palette.accent,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
      Box {
        IconButton(onClick = { menu = true }) {
          Icon(painterResource(R.drawable.ic_more), contentDescription = "更多", tint = palette.accent)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = palette.card) {
          if (!trash) DropdownMenuItem(
            text = { Text(if (view == "list") "画廊视图" else "列表视图") },
            onClick = { menu = false; view = if (view == "list") "gallery" else "list" },
          )
          if (!trash) DropdownMenuItem(text = { Text("选择备忘录") }, onClick = { menu = false; selected = emptySet(); onSelecting(true) })
          if (own) DropdownMenuItem(text = { Text("重新命名文件夹") }, onClick = { menu = false; onRename() })
        }
      }
    }
    if (view == "gallery" && !trash) {
      // 画廊：两列卡片。卡片网格是自己的滚动容器，标题与搜索固定在顶部。
      Column(Modifier.padding(horizontal = 20.dp)) {
        Text(folder.title, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          when {
            notes.size == 1 -> "1 个备忘录"
            notes.isEmpty() -> "没有备忘录"
            else -> "${notes.size} 个备忘录"
          },
          style = MaterialTheme.typography.bodyMedium, color = palette.secondary,
        )
        IosSearch(query, onQuery, palette, Modifier.padding(top = 8.dp, bottom = 12.dp), onActivated = onSearchStart)
      }
      Box(Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("没有备忘录", color = palette.secondary) }
        } else {
          Gallery(notes, palette, onOpen = onOpen, onChoose = { chosen = it })
        }
      }
    } else {
      // 列表：大标题与搜索框随内容滚动，搜索框触顶后停在顶栏下（sticky），如 iOS。
      LazyColumn(
        state = listState, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
      ) {
        item(key = "title") {
          Column(Modifier.padding(horizontal = 4.dp)) {
            Text(folder.title, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
              when {
                notes.size == 1 -> "1 个备忘录"
                notes.isEmpty() -> "没有备忘录"
                else -> "${notes.size} 个备忘录"
              },
              style = MaterialTheme.typography.bodyMedium, color = palette.secondary,
            )
          }
        }
        stickyHeader(key = "search") {
          Box(Modifier.background(palette.bg).padding(top = 8.dp, bottom = 12.dp)) {
            IosSearch(query, onQuery, palette, Modifier.fillMaxWidth(), onActivated = onSearchStart)
          }
        }
        if (notes.isEmpty()) {
          item {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
              Text("没有备忘录", color = palette.secondary)
            }
          }
        } else {
          // 排序与分组照设置：编辑日期（默认）/创建日期/标题，按日期分组（默认开）。
          // 置顶的始终在前。创建日期引擎还没有字段，暂与编辑日期一致。
          val today = LocalDate.now()
          val collator = java.text.Collator.getInstance(java.util.Locale.getDefault())
          val ordered = when (savedSetting(context, "noteSort", "edited")) {
            "title" -> notes.sortedWith(compareByDescending<NItem> { it.pinned }.thenComparator { a, b -> collator.compare(a.title, b.title) })
            else -> notes.sortedWith(compareByDescending<NItem> { it.pinned }.then(compareByDescending { it.modifiedAt ?: 0 }))
          }
          if (savedSetting(context, "noteGroup", "on") == "on") {
            for ((name, list) in ordered.groupBy { bucket(it, today) }.toList()) {
              item(key = "h-$name") {
                Text(
                  name, style = MaterialTheme.typography.titleSmall, color = palette.secondary,
                  modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 6.dp),
                )
              }
              groupItems(list, key = { "n-${it.guid}" }, inset = 16) { n ->
                NoteRow(n, palette, pinnable = !trash, onOpen = onOpen, onChoose = { chosen = it })
              }
            }
          } else {
            groupItems(ordered, key = { "n-${it.guid}" }, inset = 16) { n ->
              NoteRow(n, palette, pinnable = !trash, onOpen = onOpen, onChoose = { chosen = it })
            }
          }
        }
      }
    }
  }
  chosen?.let { n ->
    DropdownMenu(expanded = true, onDismissRequest = { chosen = null }, containerColor = palette.card) {
      DropdownMenuItem(
        text = { Text(if (n.pinned) "取消置顶" else "置顶") },
        onClick = { chosen = null; pin(n) },
      )
    }
  }
  if (moving && selected.isNotEmpty()) {
    MoveDialog(folders, folder.guid, palette, onDismiss = { moving = false }) { to ->
      moving = false
      val pick = selected; onSelecting(false); selected = emptySet()
      scope.launch {
        for (g in pick) {
          runCatching { withContext(Dispatchers.IO) { engine.noteMove(g, to) } }
            .onFailure { onProblem((it as? uniffi.quince_mobile.EngineException.Failed)?.reason ?: "没有送到 iCloud，稍后会再试。") }
        }
        syncNotes(context)
      }
    }
  }
}

/** 悬浮圆钮：白底、主题色图标。列表页右下角新建备忘录，文件夹页左下角新建文件夹。 */
@Composable
private fun Fab(
  onClick: () -> Unit,
  palette: IosPalette,
  modifier: Modifier = Modifier,
  icon: Int = R.drawable.ic_edit,
  label: String = "新备忘录",
) {
  Surface(
    onClick = onClick,
    color = palette.card, shape = CircleShape, shadowElevation = 8.dp,
    modifier = modifier.padding(20.dp).size(56.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(painterResource(icon), contentDescription = label, tint = palette.accent, modifier = Modifier.size(24.dp))
    }
  }
}

/** 文件夹左滑露出的一个动作按钮：整块底色、中间图标，如 iOS（“移动”暂置灰不可点）。 */
@Composable
private fun FolderSwipeAction(icon: Int, label: String, bg: Color, tint: Color, enabled: Boolean = true, onClick: () -> Unit = {}) {
  Box(
    Modifier.width(72.dp).fillMaxHeight().background(bg)
      .clickable(enabled = enabled) { onClick() },
    contentAlignment = Alignment.Center,
  ) {
    Icon(painterResource(icon), contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
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
  selecting: Boolean = false,
  isSelected: (String) -> Boolean = { false },
  onToggle: (String) -> Unit = {},
  onOpen: (NItem) -> Unit,
  onChoose: (NItem) -> Unit,
  modifier: Modifier = Modifier,
  bottomPad: androidx.compose.ui.unit.Dp = 24.dp,
) {
  val today = LocalDate.now()
  val sections = if (grouped) notes.groupBy { bucket(it, today) }.toList() else listOf("" to notes)
  LazyColumn(modifier = modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPad)) {
    for ((name, list) in sections) {
      if (name.isNotEmpty()) item(key = "h-$name") {
        Text(
          name, style = MaterialTheme.typography.titleSmall, color = palette.secondary,
          modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 6.dp),
        )
      }
      groupItems(list, key = { "n-${it.guid}" }, inset = 16) { n ->
        NoteRow(n, palette, pinnable, selecting, isSelected(n.guid), onOpen = onOpen, onChoose = onChoose, onToggle = { onToggle(n.guid) })
      }
    }
  }
}

/** 选择模式的圆圈：空心为未选，主题色实心加白勾为已选，如 iOS。 */
@Composable
private fun SelectCircle(checked: Boolean, palette: IosPalette) {
  Box(
    Modifier.size(22.dp)
      .clip(CircleShape)
      .then(
        if (checked) Modifier.background(palette.accent)
        else Modifier.border(1.5.dp, palette.secondary, CircleShape)
      ),
    contentAlignment = Alignment.Center,
  ) {
    if (checked) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
  }
}

/** 一行备忘录：点开（选择模式里点了是选中），长按弹出置顶（删除只在笔记打开后的“更多”菜单里，如 iOS 原版）。 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(
  n: NItem,
  palette: IosPalette,
  pinnable: Boolean = true,
  selecting: Boolean = false,
  selected: Boolean = false,
  onOpen: (NItem) -> Unit,
  onChoose: (NItem) -> Unit,
  onToggle: () -> Unit = {},
) {
  var menu by remember { mutableStateOf(false) }
  Box {
    Column(
      Modifier.fillMaxWidth().combinedClickable(
        onClick = { if (selecting) onToggle() else onOpen(n) },
        onLongClick = { if (pinnable && !selecting) menu = true },
      ).padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (selecting) {
          SelectCircle(selected, palette)
          Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
          Text(n.title.ifBlank { "新备忘录" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Row(Modifier.padding(top = 2.dp)) {
            Text(changed(n.modifiedAt), style = MaterialTheme.typography.bodyMedium, color = palette.secondary)
            Spacer(Modifier.width(8.dp))
            Text(noteSnippet(n), style = MaterialTheme.typography.bodyMedium, color = palette.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }
    if (pinnable && !selecting) DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = palette.card) {
      DropdownMenuItem(text = { Text(if (n.pinned) "取消置顶" else "置顶") }, onClick = { menu = false; onChoose(n) })
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
  // 正文连同光标：从富文本点进编辑时，光标落在点的那个字上。
  var text by remember { mutableStateOf(TextFieldValue("")) }
  var saved by remember { mutableStateOf("") }
  // 新笔记刚在 iCloud 建立的那一刻（有了 guid，文档还没读回），别把界面拆了——键盘会掉。
  var fresh by remember { mutableStateOf(false) }
  var moving by remember { mutableStateOf(false) }
  var menu by remember { mutableStateOf(false) }
  // 系统文件夹按中文显示（引擎给的是账号语言的原文），自建的照原样。
  val folderTitle = folders.firstOrNull { it.guid == folderGuid }?.let { folderDisplay(it) } ?: "备忘录"

  fun reason(t: Throwable) = (t as? uniffi.quince_mobile.EngineException.Failed)?.reason ?: "这篇备忘录没有保存下来。"
  fun load() = scope.launch {
    val g = guid ?: return@launch
    runCatching { withContext(Dispatchers.IO) { engine.note(g) } }
      .onSuccess { doc = it; fresh = false; if (!editing) { text = TextFieldValue(it.text); saved = it.text } }
      .onFailure { onProblem(reason(it)) }
  }
  fun save(then: () -> Unit = {}) = scope.launch {
    if (text.text != saved) {
      val g = guid
      runCatching {
        withContext(Dispatchers.IO) {
          // A new note is made with its first text; one left empty is never made.
          if (g == null) { if (text.text.isNotBlank()) { guid = engine.noteCreate(folderGuid, text.text); fresh = true } } else engine.noteSave(g, text.text)
        }
      }.onSuccess { saved = text.text; syncNotes(context) }.onFailure { onProblem(reason(it)) }
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
    if (editing && text.text != saved) { delay(800); save() }
  }
  BackHandler { save { onBack() } }

  // A new note has no document yet: it is an empty one being typed — and, right after it is
  // first saved, one whose real document is still on its way back, so the editor stays put.
  val d = doc ?: NDoc(guid ?: "", folderGuid, "", text.text, emptyList(), pinned = false, locked = false, editable = true, modifiedAt = null)
    .takeIf { guid == null || fresh } ?: return
  val trashed = d.folderGuid == TRASH

  IosBar {
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
      // “< 所在文件夹”紧贴成一组，如 iOS；chevron 的画框左右各空一段，右移让它贴住文字。
      Row(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable(
          interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { save { onBack() } }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(painterResource(R.drawable.ic_back), contentDescription = "返回", tint = palette.accent, modifier = Modifier.size(30.dp).graphicsLayer { translationX = 9.dp.toPx(); translationY = 1.5.dp.toPx() })
        Text(folderTitle, style = MaterialTheme.typography.bodyLarge, color = palette.accent, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
      }
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
    // 日期只在顶部往下拉时露出：拉过头（overscroll）的剩余量会从 onPostScroll 送到这里；
    // 一往上滚（onPreScroll）就收起；停手也收起。没有涟漪——正文不像按钮。
    var showDate by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val dateScroll = remember {
      object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
          if (available.y < -4f) showDate = false
          return Offset.Zero
        }
        override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
          if (available.y > 4f) showDate = true
          return Offset.Zero
        }
      }
    }
    LaunchedEffect(listState.isScrollInProgress) { if (!listState.isScrollInProgress) showDate = false }
    AnimatedVisibility(
      visible = showDate && d.modifiedAt != null,
      enter = fadeIn(tween(120)) + expandVertically(tween(120)),
      exit = fadeOut(tween(120)) + shrinkVertically(tween(120)),
    ) {
      Text(
        d.modifiedAt?.let {
          Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
        } ?: "",
        style = MaterialTheme.typography.bodySmall, color = palette.secondary,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), textAlign = TextAlign.Center,
      )
    }
    Box(
      Modifier.fillMaxSize()
        .nestedScroll(dateScroll)
        .pointerInput(d.editable, trashed) {
          // 点行与行之间的空隙时兜底：光标落在文末。
          detectTapGestures {
            if (d.editable && !trashed) { text = TextFieldValue(d.text, TextRange(d.text.length)); saved = d.text; editing = true }
          }
        },
    ) {
      when {
        d.locked -> Text(
          "此备忘录已锁定。请在 iPhone、iPad 或 Mac 上用它的密码打开。",
          style = MaterialTheme.typography.bodyLarge, color = palette.secondary, modifier = Modifier.padding(24.dp),
        )
        editing -> {
          // The keyboard comes up with the editor, as Notes opens a new or tapped note ready to type.
          // Hot-start jumps (the shortcut while the app is alive) need a beat before the field is
          // ready to take focus; the tiny wait costs nothing on a cold start. Android 12 only
          // raises the keyboard for a user interaction, so the jump from the icon shortcut goes
          // through the window's insets controller — that route is always allowed.
          val focus = remember { FocusRequester() }
          val keyboard = LocalSoftwareKeyboardController.current
          LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(400)
            focus.requestFocus()
            val activity = context as? android.app.Activity
            val controller = activity?.window?.decorView?.let { androidx.core.view.ViewCompat.getWindowInsetsController(it) }
            if (controller != null) controller.show(androidx.core.view.WindowInsetsCompat.Type.ime()) else keyboard?.show()
          }
          BasicTextField(
            value = text, onValueChange = { text = it },
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
          listState,
          Modifier.fillMaxSize(),
          // 打勾的待办按设置排到段末（自动）或原地不动（手动）。
          autoCheck = savedSetting(context, "checkSort", "auto") == "auto",
          // 点正文哪一行，就落到哪个字上进入修改。
          onPlace = { off ->
            text = TextFieldValue(d.text, TextRange(off)); saved = d.text; editing = true
          },
        ) { at, checked ->
          val g = guid ?: return@RichNote
          act({ engine.noteCheck(g, at, checked) }) { load() }
        }
      }
    }
  }
  if (moving) {
    val g = guid
    MoveDialog(folders, d.folderGuid, palette, onDismiss = { moving = false }) { to ->
      moving = false
      if (g != null && to != d.folderGuid) act({ engine.noteMove(g, to) }) { load() }
    }
  }
}

/** 移动备忘录的对话框：Notes 自己的文件夹在最前，最近删除不在此列。列表页的选择模式也用它。 */
@Composable
private fun MoveDialog(
  folders: List<NFolder>,
  current: String?,
  palette: IosPalette,
  onDismiss: () -> Unit,
  onMove: (String?) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.card,
    title = { Text("移动备忘录") },
    text = {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        // Notes' own folder (no folder) first, then the person's; Recently Deleted is not a place to move to.
        val choices = listOf<Pair<String?, String>>(null to "备忘录") + folders.filter { !it.trash }.map { it.guid to it.title }
        for ((to, title) in choices) {
          Row(
            Modifier.fillMaxWidth().clickable { onMove(to) }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(painterResource(R.drawable.ic_folder), contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (to == current) Icon(painterResource(R.drawable.ic_check), contentDescription = "在这里", tint = palette.accent, modifier = Modifier.size(18.dp))
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = palette.accent) } },
  )
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
/** 勾选项自动排序：每段连续的待办列表里，打勾的排到未打勾的后面（行带原偏移，勾选不受影响）。 */
private fun checkSorted(lines: List<NoteLine>): List<NoteLine> {
  val out = mutableListOf<NoteLine>()
  var block = mutableListOf<NoteLine>()
  fun flush() {
    if (block.isNotEmpty()) {
      val (open, done) = block.partition { it.para?.checked != true }
      out.addAll(open); out.addAll(done); block = mutableListOf()
    }
  }
  for (l in lines) {
    if (l.para?.style?.toInt() == 103) block.add(l) else flush().also { out.add(l) }
  }
  flush()
  return out
}

@Composable
private fun RichNote(d: NDoc, engine: AccountInterface, state: LazyListState, modifier: Modifier, autoCheck: Boolean, onPlace: (Int) -> Unit, onCheck: (UInt, Boolean) -> Unit) {
  val lines = remember(d, autoCheck) { lay(d).let { if (autoCheck) checkSorted(it) else it } }
  LazyColumn(modifier, state, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
    items(lines.size) { i -> NoteLineView(lines[i], engine, onPlace, onCheck) }
  }
}

@Composable
private fun NoteLineView(l: NoteLine, engine: AccountInterface, onPlace: (Int) -> Unit, onCheck: (UInt, Boolean) -> Unit) {
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
        val circleInteraction = remember { MutableInteractionSource() }
        Box(
          Modifier.padding(top = 3.dp, end = 10.dp).size(20.dp)
            .border(1.75.dp, if (ticked) palette.accent else palette.secondary, androidx.compose.foundation.shape.CircleShape)
            .background(if (ticked) palette.accent else Color.Transparent, androidx.compose.foundation.shape.CircleShape)
            .toggleable(
              ticked, interactionSource = circleInteraction, indication = null, role = Role.Checkbox,
            ) { onCheck(l.start.toUInt(), it) },
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
      // 点行里的哪个字，就把那个位置交给编辑器：光标落在原地。
      var layout by remember(l) { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
      Text(
        styled,
        style = style.copy(lineHeight = (style.fontSize.value * 1.35f).sp),
        onTextLayout = { layout = it },
        modifier = Modifier.fillMaxWidth().pointerInput(l.start, l.text) {
          detectTapGestures { pos ->
            layout?.let { onPlace(l.start + it.getOffsetForPosition(pos).coerceAtMost(l.text.length)) }
          }
        },
      )
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
