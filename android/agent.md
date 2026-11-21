# Skykeep Notes（notes-ios 分支）开发交接

> 本文件写给下一个继续开发的 agent / 开发者，记录截至 2026-11-21 的状态。
> 分支：`notes-ios`（基于 origin/main 的 98bb9c26 "Skykeep 0.1.35"）

## 1. 项目定位

这是 Skykeep（GPL-3.0-only，Rust 引擎 + Tauri 桌面 + Kotlin Compose Android）的 fork，
目标：**只做 Apple 备忘录（Notes）单应用**，交互尽量复刻 iOS 备忘录。上游的 6 合 1
Android UI（Drive/Photos/Reminders/Calendar/Contacts + Notes）保留在代码里但不再进入
（MainActivity 的入口换成了 `NotesScreens` / `NotesSetup`）。

**跟随上游的策略**：只改 Android UI 层（主要是 `Notes.kt`、`NotesApp.kt`、`MainActivity.kt`、
build.gradle.kts、strings/图标资源）；Rust 引擎（crates/，约 68k 行）一个字没动。上游更新后
`git merge origin/main`，冲突只会出现在我们改过的 UI 文件里。

## 2. 关键信息

- **包名**：`io.github.yashwanthkamireddi.skykeep.notes`（与原版共存；buildType verify 同后缀）
- **应用名**：备忘录（strings.xml）
- **图标**：黄底 #F7CE46 + 三条白线（`ic_launcher_notes_fg.xml`）
- **compileSdk 37**（别降 36，compose-ui 1.12.1/core-ktx 要求 ≥37）
- **fork 首个提交**：b99b0255；后续提交都是一轮用户反馈

### 提交历史（按序）
1. `b99b0255` Notes only: an iOS Notes-styled Android app on the same engine
2. `6e6a4d9c` Setup without Contacts or Calendar: Notes makes its own account
3. `c8ebda09` Notes list: no swipe actions, menus as iOS has them
4. `0715a557` iOS-style push transitions between folders, list and editor
5. `a611915b` transitions: screens in a Column on their own background
6. `777fed3b` search as iOS runs it — a mode, not a page
7. `7e521d1d` compose FAB, sortable folders, collapsing titles and note selection
8. `6521687c` edit mode as iOS runs it, swipe to delete a folder
9. `a1f319e2` swipe reveals Move and Delete buttons, as iOS
10. `ce1cd26d` one swipe open at a time

## 3. 构建与真机

```bash
cd android && ./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- preBuild 自动跑 cargo-ndk（buildEngine）、uniffi-bindgen（engineBindings）、cargo-about
- 本机构建链已就绪：VS 2022 Build Tools（MSVC）、Rust 1.98.1（rust-toolchain.toml，
  不要改成 stable）、cargo-about 0.9.3 预编译版在 ~/.cargo/bin
- **真机**：Samsung R5CYA05YXFF，1080x2340 / density 480。`adb exec-out screencap -p`
  出的图是 480 宽缩放图，**tap 坐标 ×2.25（x）/ ×2.2159（y）**
- adb 验证长按拖动用 `adb shell input motionevent DOWN/MOVE/UP` 分步注入（DOWN 后 sleep 1s
  触发长按）；`input swipe` 匀速滑动触发不了 Compose 长按
- 手机有锁屏密码：息屏后 adb 解不了锁，需要用户解锁
- 登录账号后 NotesSetup 自动建本机账号同步（无通讯录/日历权限弹窗），
  Contacts/Calendar authority `setIsSyncable(0)`

## 4. 代码地图（android/app/src/main/kotlin/.../）

- **`ui/Notes.kt`**（约 1400 行，核心）：全部 Notes UI
  - `NotesPage`：路由（NPlace.Folders/Folder/Open + NPlaceSaver）、AnimatedContent 推入/弹出
    转场（depth 决定方向）、reload/searchNotes、syncNotes + Outcome.watch、BackHandler 链
  - `FolderScreen`：文件夹页。搜索模式（SearchRow）；编辑模式（整表照旧、fixed 行置灰无把手、
    自建行 ic_reorder 把手、列级 pointerInput 跟手拖动、folderOrder 存 SharedPreferences
    "settings"/"folderOrder"）；普通模式（大标题收起 + stickyHeader 搜索 + 左滑动作）
  - 左滑动作：`openSwipe` 列表级状态（一次一行）、自绘 detectHorizontalDragGestures +
    animateFloatAsState；按钮 移动(置灰)/删除(红)；滚动、滑另一行、点其他行都收起
  - `NoteListScreen`：列表页。FAB（右下悬浮新建）、大标题收起居中、sticky 搜索、画廊视图、
    选择备忘录模式（全选/计数/完成、SelectCircle、底栏移动+删除、MoveDialog 共享）
  - `NoteEditor`：iOS 顶栏、日期居中、800ms 防抖保存、置顶/移动/删除、回收站恢复/立即删除
  - 渲染管线（上游的 lay/NoteLine/RichNote/AttachmentMarks）保持未动
- **`ui/NotesApp.kt`**：NotesScreens（NotesPage 或设置页）+ NotesSetup（建账号、禁其他 authority）
- **`MainActivity.kt`**：Stage.Setup → NotesSetup；Stage.Main → NotesScreens
- **`sync/`**：上游同步适配器，NOTES_AUTHORITY = `${applicationId}.notes`
- 引擎桥（UniFFI，Kotlin 侧 camelCase）：noteFolders/notes/searchNotes/note/noteCreate/
  noteSave/noteCheck/noteMove/notePin/noteDelete/noteFolderCreate/Rename/Delete/noteAttachment
- 常量：TRASH = "TrashFolder-CloudKit"（最近删除），DEFAULT_FOLDER = "DefaultFolder-CloudKit"（系统"备忘录"文件夹）

## 5. 与 iOS 备忘录的对照（已完成 ✔ / 待办 ⬜）

- ✔ 文件夹页：所有 iCloud 置顶、系统文件夹、自建、最近删除置底；白色分组卡
- ✔ 搜索：点搜索框进入搜索模式（标题栏消失、框上移、右侧取消）
- ✔ 推入/弹出转场、大标题收起、搜索框 sticky
- ✔ 列表：置顶 + 时间分组（今天/昨天/前 7 天/前 30 天/月份/年——iOS 18 也有分组，勿删 bucket()）
- ✔ FAB 新建、画廊视图、长按置顶
- ✔ 选择备忘录：多选、全选、移动（MoveDialog）、删除
- ✔ 文件夹：编辑模式拖动排序（fixed 置灰）、左滑 移动(置灰)/删除、新建文件夹
- ✔ 编辑页：读写、完成、置顶、移动、删除、回收站（恢复/立即删除）
- ⬜ **查看附件**（文件夹 ⋯ 菜单，按照片/网站/文稿分类——用户说"先不做"）
- ⬜ **设置页选项**（iOS 系统设置的备忘录设置项对齐——用户说"后面再说"）
- ⬜ iOS 边缘右滑返回手势（现在只有返回按钮 + 系统返回键）
- ⬜ 富文本编辑（用户选了 v1 不做；二期建议给上游提 PR）
- ⬜ 子目录（子文件夹）：引擎底层 `create_folder_cloudkit` 已有 parent 参数（传 None），
  但 UniFFI 未暴露。暴露后：点亮左滑的"移动"按钮 + 新建文件夹选父级
- ⬜ 锁定笔记（现在只显示"已锁定"提示，不能解锁）

## 6. 已知的坑（务必记住）

1. `stickyHeader` 是 LazyListScope **接口成员**（compose foundation 1.12.1，javap 验证过），
   不需要 import，但要 `@OptIn(ExperimentalFoundationApi::class)`
2. LazyColumn 默认占满可用高度：要盖底栏/FAB 必须 Box + align，别用 Column 排
3. SwipeToDismissBox 的 content 必须自己铺不透明背景，否则 backgroundContent 常驻透出
   （我们已经换成自绘手势，没有 SwipeToDismissBox 了）
4. 指针拖动要用"列级绝对坐标"模式（fingerY + touchShift - i*rowHeight + scroll.value），
   行内闭包捕获 index 会在换位后过期（当初"不跟手"的根因）
5. 上游依赖要求 compileSdk ≥ 37；rust-toolchain.toml 必须保持 1.98.1
6. `local.properties` 加进了 .git/info/exclude（不要提交）
7. `bucket()` 日期分组曾被按用户要求删过、又恢复了（iOS 18 有分组）——别再删
8. NameDialog 用上游 Drive.kt 里的（Notes.kt 里不要重复定义）
9. 用户偏好：列表行不要滑动删除（删除在编辑页 ⋯ 或选择模式）、长按只有置顶

## 7. 用户交互习惯

- 中文交流；改动后构建 → adb 安装 → 截图逐项自验 → git commit（一行式英文消息，
  风格如 "Notes folders: one swipe open at a time"）
- 用户会逐条截图反馈 UI 细节，改完一项验一项；不要一次性大改后让他整体验收
