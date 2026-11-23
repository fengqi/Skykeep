<p align="center">
  <img src="packaging/icons/skykeep.svg" alt="Skykeep Notes logo" width="96" height="96">
</p>

<h1 align="center">Skykeep Notes</h1>

<p align="center"><strong>在 Android 上用 Apple 备忘录。</strong><br>Skykeep 的单应用版本：只做备忘录，交互尽量复刻 iOS 备忘录。同一引擎，同一套 iCloud 同步，别无其他。</p>

<p align="center">
  <a href="README.md">English</a> · 简体中文
</p>

<p align="center">
  <a href="https://github.com/YashwanthKamireddi/Skykeep"><img src="https://img.shields.io/badge/基于-Skykeep-2f6fd6" alt="基于 Skykeep"></a>
  <a href="https://github.com/fengqi/Skykeep/blob/main/README.md"><img src="https://img.shields.io/badge/main_分支-README-2f6fd6" alt="main 分支 README"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-2f6fd6" alt="GPL-3.0 license"></a>
</p>

## 这个分支是什么

这是 [Skykeep](https://github.com/YashwanthKamireddi/Skykeep) fork 的 `notes-ios` 分支。Skykeep 是一个开源项目（GPL-3.0），用 Rust 引擎把 iCloud 带到 Windows、Linux 和 Android；它的 main 分支包含六个应用——备忘录、提醒事项、日历、通讯录、iCloud 云盘和照片。完整项目见 [main 分支 README](https://github.com/fengqi/Skykeep/blob/main/README.md)。

这个分支只做一件事：**把 Android 应用变成只含备忘录的单应用**，界面尽量复刻 iOS 备忘录。上游的主屏幕和其他五个应用仍留在代码里，但不再可达——应用启动后直接进入备忘录。

- **同一引擎。** Rust 部分（crates/，约 68k 行）一个字没动，所有改动都在 Android UI 层，方便跟随上游更新。
- **与原版共存安装。** 包名 `io.github.yashwanthkamireddi.skykeep.notes`，应用名"备忘录"，独立的黄色图标。
- **同一套同步。** 用 Apple 账户登录；首次运行会在本机创建一个仅备忘录的同步账号，并停用通讯录和日历同步适配器，不会弹出本应用用不到的数据权限。

## 当前状态

开发中（截至 2026 年 11 月）。核心体验已经就位：

- 文件夹页：iCloud 置顶与系统文件夹、自建文件夹、最近删除置底
- 搜索按 iOS 的方式做——是一种模式，不是独立页面
- iOS 风格的推入/弹出转场、大标题收起、搜索框 sticky
- 备忘录列表：置顶、时间分组（今天 / 昨天 / 前 7 天 / …）、悬浮新建按钮、画廊视图
- 选择模式：多选、全选、移动、删除
- 文件夹编辑：拖动排序、左滑"移动 / 删除"、一次只展开一行滑动
- 编辑页：读写与防抖自动保存、置顶、移动、删除；回收站支持恢复与立即删除

尚未完成：

- 按类别查看附件（照片、网站、文稿）
- 备忘录设置页
- iOS 风格的边缘右滑返回手势
- 富文本编辑、子文件夹、锁定笔记的解锁

## 构建

需要 Android SDK、Rust 和 cargo-ndk（Gradle 构建会自动编译引擎）：

```sh
cd android
./gradlew :app:assembleDebug
```

APK 输出在 `android/app/build/outputs/apk/debug/`。

## 致谢与许可

这个 fork 之所以存在，要归功于 Yashwanth Kamireddi 的 [Skykeep](https://github.com/YashwanthKamireddi/Skykeep)——本分支只是重塑了它的 Android UI。许可与上游相同，采用 [GPL-3.0](LICENSE)。与 Apple Inc. 无关。Apple、iCloud 和 iPhone 是 Apple Inc. 的商标。
