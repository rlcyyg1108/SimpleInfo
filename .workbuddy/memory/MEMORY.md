# Simple Info — 项目长期记忆

`D:\AAAmp\minecraft\SimpleInfo`｜MC 1.20.1 / Forge 47.4.10｜mod_id `simpleinfo`，包 `com.rlcyyg.simpleinfo`。

## 用户偏好
- **不跑 Gradle 编译**（本机做不了），用户 IDEA `runClient` 自验；交付前用**裸 javac 全量类型检查**；沟通简洁直给。

## 自检（裸 javac）
- JDK17 `C:\Program Files\Java\jdk-17.0.16`。
- `-cp` = 映射主 jar（`forge_gradle/minecraft_user_repo/**/forge-1.20.1-47.4.10_mapped_*.jar`）+ `modules-2`/`maven_downloader` 非 sources/javadoc jar（`;` 分隔）；源码列表与 `-cp` 写进 **argfile**，路径**全正斜杠**，必加 `-encoding UTF-8`。
- `javap -cp <jar> <类>` 查签名。`Mod`/`DistExecutor` 在 javafmllanguage jar，`ModLoadingContext` 在 fmlcore jar。

## 工程约定
- `gradle.properties` 非 ASCII 必须 `\uXXXX`（Gradle 按 ISO-8859-1 读）。
- lang key 前缀：`item.`/`itemGroup.`/`tooltip….lineN`/`key.`/`key.categories.`/`gui.`/`config.`/`info.`。
- 打包名 = `archivesName-version`：`base.archivesName=mod_id` + `version = "${mod_version}-${minecraft_version}-forge"` → `simpleinfo-0.0.1-1.20.1-forge.jar`；mods.toml 版本走 `${mod_version}` 不受影响；`build/libs` 只留一个 jar（reobfJar 同名覆盖）。
- MC 内存 = `build.gradle` runs 的 `jvmArgs '-Xms4G','-Xmx4G'`。

## API（1.20.1 + Forge 47.4.10）
- ForgeConfigSpec：`IntValue`/`DoubleValue`/`BooleanValue`（**无 `IntegerValue`**）；`Builder` 无 `get(path,key)` → define 时缓存；`get()` 返回装箱；有 `getDefault()`；路径按 `.` 分层。
- 只用 `DistExecutor.unsafeRunWhenOn`（`safe*` 对内联 lambda 抛 FATAL）；公共侧类禁 import `net.minecraft.client.*`。
- Screen：覆写 `init()` 而非 `rebuildWidgets()`；`drawString` 色**必含 alpha**；**别用 `CycleButton`**（多冒号）；红字 `withStyle(ChatFormatting.RED)`；`ConfirmScreen` 5 参、返回不重新 init()；`Screen.hasShiftDown()` 静态；`Button` 画不了纯色 → `gg.fill`。
- 右键命中：`List<ResetZone>`，`mouseClicked(button==1)` 先于 super 并 `return true`。
- 总线：`RegisterKeyMappingsEvent`/`RegisterGuiOverlaysEvent` 在 mod；`ClientTickEvent`/`RenderGuiEvent` 在 Forge。
- `tickTimes` 是 `public final long[]`（纳秒）；`Minecraft` 只有 `getSingleplayerServer()`；指针锁定 → **不做「游戏内拖 HUD」**。

## HUD 数据模型
- 流式 3 行一整块；禁用项跳过左移补位；`|` 只在相邻项间出现；默认顺序 = `InfoItem` 枚举序 = `ORDER` 默认值。
- 定位：锚点（左上）+ 边距 marginX/Y；每项只有行号 row；同行排序 moveWithinRow。
- **取色 `Part`**：`NAME`｜`VALUE_GOOD/WARN/BAD`｜内存额外 `TOTAL`、`PERCENT`；key = `color.<name|good|warn|bad|total|percent>`。
- 自动（`auto.value` 三数值级共享、`auto.percent` 独立）：**开 → 内置默认色 绿/黄/红；关 → 自定义色**（数值按健康度取三级色之一，百分比取自身固定色）；手动改色会自动关「自动」。
- 阈值：FPS ≥60/≥30、TPS ≥19/≥15、MSPT ≤30/<50、内存 <90/<95 %；不可用 → 白。时间 `HH:mm`；游戏时间 = `getGameTime()`/72000 h；内存分母 `maxMemory()`。
- 隐藏：**F3 开启（`options.renderDebug`）→ 无条件隐藏**；Screen 打开 → 看 `HIDE_IN_CHAT_SCREEN`。其他默认：`SHOW_SHADOW` true、`MARGIN_X/Y` 1、开配置键 **F6**。
- HUD 与预览共用 `client/InfoBlock.java`。

## 配置界面（2×2 方块阵，无标题）
- `COL_W 208`、`COL_GAP 12`、`contentW 428`，水平+垂直居中；`totalH = upperH + BAND_GAP + lowerH + BUTTON_GAP + BTN_H`。
- 上带左 6 设置两段式（`addOptionRow`）；上带右 6 信息项 `[开关][名称][第N行][▲][▼]`；下带左=数值 2×2+颜色区，右=预览（等宽等高同顶边）。BTN_H 16/ROW_GAP 2/BAND_GAP 3/BUTTON_GAP 10/SWATCH 11 pitch 13/MOV_W 12。
- 数值控件 `[标签][-][EditBox][+][红 ↺]`；responder 内不要 refreshEditors（`updatingFields` 哨兵）；数字框右键=清空、失焦还原。
- `[项·部件]` 循环 `selectedPart`；**手动改色（色板/色号）自动关掉该部件的「自动」**。
