# CTimer2 · 魔方计时器

> 一个横屏全屏、原生 Android 的魔方速拧计时器。
> Kotlin + Jetpack Compose + Room，自绘几何图标、可调毛玻璃材质、多列表成绩管理，并接入 GitHub Issues 做云端成绩同步与排行榜。

- **包名**：`com.ctimer2`
- **应用名**：CTimer2
- **当前版本**：2.4.0（versionCode 21）
- **最低系统**：Android 8.0（API 26）
- **目标 / 编译 SDK**：API 36（Android 16）
- **形态**：横屏锁定、全 Compose UI、无 XML 布局

---

## 一、项目定位

CTimer2 是为「自己练速拧」设计的计时器，不是教学工具也不是社交平台。它解决三件事：

1. **按得爽**：整屏任意位置按下去就能开始计时，抬起即停，不需要瞄准某个按钮。
2. **看得清**：横屏大字、扁平照排的计时数字、可切换字体与颜色、可换自定义背景。
3. **记得住**：成绩按「列表」分门别类，本地 Room 存储 + 自动备份 + 云端同步，换设备/重装/改包名都不会丢。

视觉上走两条线：**淡蓝 + 奶白的清爽配色**，以及**可开关的毛玻璃 / 云母材质**（局部取景模糊，面板外的背景始终清晰）。

---

## 二、功能特性

### 1. 计时核心

- **整屏感应**：`pointerInput` 铺满根布局，屏幕任意位置按下都触发。
- **状态机**：`IDLE → HOLDING → READY → RUNNING`。
  - 按住超过 400ms 变绿（`READY`），此时抬起才开表；按住期间松手则取消。
  - 运行中任意点击即停表。
- **空格键计时**：外接键盘时，空格键走同一套状态机（`ACTION_DOWN / ACTION_UP`）。
- **键鼠检测**：`util/InputDevices.kt` 通过 `Configuration.keyboard` + `InputManager` 扫描 `SOURCE_KEYBOARD / SOURCE_MOUSE`，在左上角 Logo 卡显示「键鼠已连接 / 键盘已连接 / 鼠标已连接」小胶囊。
- **打乱公式**：csTimer 风格 random-move 生成（默认 21 步），显示在计时数字上方。
  - 中文口诀主显（16sp），字母原文辅显（11sp），每 5 步用「，」断句。
  - 口诀映射：`R=上 / R'=下 / R2=上上`、`L=左 / L'=左逆 / L2=左左`、`U=勾 / U'=回 / U2=勾勾`、`D=底 / D'=底逆 / D2=底底`、`F=顺 / F'=逆 / F2=顺顺`、`B=提 / B'=压 / B2=提提`。

### 2. 多列表成绩管理

- 左侧抽屉（280dp）里新建 / 长按重命名 / 删除列表，切换列表即切换当前成绩集。
- 成绩列表卡片与顶部切换条合并为一整块玻璃面板（`embed` 模式），换背景照片时不会露缝。
- 两种排序视图，用卡片顶部的切换条分两格点击切换：
  - **按时间**（列表顺序）
  - **按成绩**（升序排名）
- 每条成绩显示与上一条的差值标注（快 = 绿，慢 = 红）。

### 3. 成绩总评（独立页面）

`SummaryPage` 计算并展示：最佳 / 最差 / 平均 / ao5 / 稳定性等，`Stats.kt` 里集中实现。

### 4. 公式速记（独立页面）

三阶入门七步法的中文口诀对照表（`FormulaPage`），字母与中文口诀逐条对应。

### 5. 材质系统（毛玻璃 / 云母 / 关闭）

`ui/theme/Glass.kt`、`ui/components/FrostedGlass.kt`、`ui/theme/GlassBackdrop.kt` 三件套：

- **三种模式**：`FRIT`（毛玻璃，强模糊高透）/ `MICA`（云母，轻模糊较实）/ `SOLID`（关闭，回到实色）。
- **两个滑块**：面板模糊 `0–40dp`、背景模糊 `0–40dp`（默认 0，即背景保持清晰通亮）。
- **局部取景，不是整屏模糊**：面板自己覆盖的那块背景才被模糊，面板外始终清晰。

**实现原理（关键）**：

1. 整屏背景**只模糊一次**（下采样 → 双线性放大近似高斯），生成一张与窗口同比例的 `ImageBitmap` 缓存，通过 `LocalGlassBackdrop` 下发。
2. 每块玻璃面板在 `drawBehind` 里，用 `drawImage(srcOffset, srcSize, dstSize)` 把这张图**对应自己位置的那一块**裁出来画满面板 —— 纯位图平移，滚动时不做任何实时模糊，因此不掉帧。
3. 面板坐标 `positionInWindow`（像素）+ 根布局真实像素尺寸（`parentLayoutCoordinates` 向上回溯）换算比例，不用 `screenWidthDp` —— 后者在横屏全屏下和真实窗口像素对不上，会让取景图铺不满、卡片边缘露出一条白底。
4. 最后叠一层半透明白 + 1dp 白色高光描边，`clip(shape)` 裁切。

`FrostedGlass` 的 `backdrop` 参数：`true` = 玻璃「窗口」（自己出糊影），`false` = 玻璃「分区」（只上色，避免双层模糊造成割裂）。

### 6. 自定义背景

- **默认背景**：内置一张海岸草地照片（`res/drawable/default_background.jpg`），开箱即用。
- **自定义**：Android 13+ 系统 Photo Picker（`PickVisualMedia`），**零权限**。
- **不存临时 URI**：选中的图会被解码、等比缩到最长边 1400、以 JPEG 90 写进私有目录 `filesDir/custom_background.jpg`，之后只读本地文件 —— 这修掉了「重启后背景概率消失」的问题。

### 7. 云端排行榜 + 用户系统

数据载体是 GitHub 仓库的 **Issue**：一条成绩 = 一个 Issue。

- 仓库：`Dramtwo/CTimer-List`
- 接口：`GET/POST /repos/{owner}/{repo}/issues`
- **自动上传**：每停一次表，立刻把这条成绩推到云端（带 3 次重试，间隔 1s / 2s），顶部浮出一条玻璃提示：「成绩 12.34 正在上传至 GitHub 仓库…」。
- **自动下载**：每次启动都拉一次云端全量成绩，按「用户 + 列表名 + 时间戳 + 毫秒」去重后补进本地；云端有而本地缺的列表会自动建出来。
- **删除云端成绩**：排行榜每行右侧有垃圾桶，确认后通过 GraphQL `deleteIssue` 把 Issue 从仓库彻底删除；若 GraphQL 不可用则回退为 PATCH 关闭（榜单只拉 `state=open`，关闭后同样不再显示）。删除中该行转圈，删完立即从列表消失。
- **用户系统（只要用户名，不要密码）**：
  - 侧栏最底部的人形图标进入用户中心。
  - 登录后上传的成绩标题带用户名前缀：`[CTimer2] [Boziwan] 12.34 · 默认列表`。
  - 登录后拉取只拉这个用户名的成绩，别人的不会混进来；未登录则显示全部。
  - 用户名存在 DataStore，重启保持登录态。

### 8. 数据安全网（三层兜底）

1. **每次停表自动备份**：全量 JSON 写到公共下载目录 `Download/CTimer2/ctimer2_scores.json`（MediaStore，API 29+ 免权限）。
2. **首次启动自动恢复**：库为空时读取上述备份并导入。
3. **手动导出 / 导入**：SAF `CreateDocument` / `OpenDocument`，走系统文件选择器。

### 9. 交互细节

- **卡片拖动**：左上 Logo 卡与成绩列表卡都可长按拖动，位置持久化（`card_x_<key> / card_y_<key>`），侧栏有「重置卡片位置」。
- **拖动呼出成绩菜单**：在成绩卡上按下并横向拖动超过阈值，弹出 300dp 玻璃浮层（上一条 / 下一条 / 删除 / 关闭），可逐条浏览。
- **页面是切换式，不是叠加式**：侧栏图标点一次打开、再点一次收起，同一时刻只有一个功能页。
- **左侧侧栏常驻**：56dp 宽、直角（右侧与底部无圆角，避免被屏幕裁掉露缝），竖排 7 个功能图标。

---

## 三、技术栈

| 分类 | 选型 |
|---|---|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose（BOM 2024.09.03，Material 3） |
| 构建 | AGP 8.7.2 / Gradle 8.11.1 / JDK 17 |
| 数据库 | Room 2.6.1（KSP `2.0.21-1.0.27`） |
| 偏好 | DataStore Preferences 1.1.1 |
| 网络 | Retrofit 2.11.0 + OkHttp 4.12.0 + Gson 2.11.0 |
| 系统集成 | activity-compose 1.9.3（Photo Picker）、MediaStore、SAF |

---

## 四、工程结构

```
CubeTimer/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml          # INTERNET / ACCESS_NETWORK_STATE，横屏锁定
│       ├── res/
│       │   ├── drawable/default_background.jpg    # 内置默认背景
│       │   ├── drawable/ic_launcher_foreground.xml # 磨砂几何秒表
│       │   ├── mipmap-anydpi-v26/ic_launcher.xml
│       │   └── values/{colors,strings,themes}.xml
│       └── java/com/ctimer2/
│           ├── MainActivity.kt
│           ├── data/
│           │   ├── AppDatabase.kt        # ctimer2.db, version 2
│           │   ├── SessionEntity.kt      # 列表
│           │   ├── SolveRecord.kt        # 单条成绩
│           │   ├── SessionDao.kt
│           │   ├── SolveDao.kt
│           │   └── SessionDataStore.kt   # 所有偏好键
│           ├── network/
│           │   ├── Models.kt             # IssueDto / RemoteScore / ScorePayload
│           │   ├── GitHubApi.kt          # Retrofit 接口
│           │   ├── RetrofitClient.kt     # OkHttp + Token 拦截器
│           │   └── GitHubRepository.kt   # 拉取 / 上传 / 重试 / 错误翻译
│           ├── viewmodel/
│           │   └── LeaderboardViewModel.kt   # 排行榜 StateFlow 状态机
│           ├── ui/
│           │   ├── TimerScreen.kt        # 主界面组装、页面切换、输入路由
│           │   ├── TimerViewModel.kt     # 计时/成绩/材质/用户 全部状态
│           │   ├── LeaderboardScreen.kt  # 云端排行榜
│           │   ├── UserScreen.kt         # 用户中心
│           │   ├── components/           # 玻璃面板、抽屉、卡片、各页面
│           │   └── theme/                # Color / Glass / GlassBackdrop / Type / Appearance
│           └── util/
│               ├── Scramble.kt           # 打乱生成 + 中文口诀
│               ├── Stats.kt              # ao5 / avg / 差值 / 总评
│               ├── ScoreBackup.kt        # JSON 备份 / SAF 导入导出
│               ├── BackgroundStore.kt    # 背景落盘
│               └── InputDevices.kt       # 键鼠探测
```

---

## 五、数据模型

### Room（`ctimer2.db`，version 2，`fallbackToDestructiveMigration`）

```kotlin
SessionEntity(id, name, createdAt, sortOrder)          // 一个「列表」
SolveRecord(id, sessionId, timeMs, timestamp)          // 一条成绩，外键级联删除
```

### DataStore（`ctimer2_settings`）

| 键 | 类型 | 说明 |
|---|---|---|
| `current_session_id` | Long | 上次选中的列表 |
| `best_solve_<sid>` | Long | 该列表最佳成绩 |
| `best_ao5_<sid>` | Double | 该列表最佳 ao5 |
| `card_x_<key>` / `card_y_<key>` | Float | 卡片拖拽位置（key = logo / list） |
| `font_family` | String | MONO / SANS / SERIF / CURSIVE |
| `font_color` | Long | 字体颜色 ARGB |
| `glass_mode` | String | FRIT / MICA / SOLID |
| `glass_blur` | Float | 面板模糊 dp |
| `bg_blur` | Float | 背景模糊 dp（默认 0） |
| `user_name` | String | 云端用户名 |

---

## 六、云端成绩协议

**上传的 Issue 标题**

```
[CTimer2] [<用户名>] 12.34 · <列表名>
```

用户名未登录时省略中括号段。

**Issue 正文**：一段人可读表格 + 一段机器可解析的标记

```markdown
**CTimer2 云端成绩**

| 项目 | 值 |
|---|---|
| 成绩 | 12.34 |
| 用户 | Boziwan |
| 列表 | 默认列表 |
| 毫秒 | 12340 |
| 时间戳 | 1763xxxxxxx |

<!--CT2SCORE:{"v":1,"app":"CTimer2","user":"Boziwan","session":"默认列表","ms":12340,"ts":1763xxxxxxx}:CT2SCORE-->
```

解析端只认 `<!--CT2SCORE: ... :CT2SCORE-->` 之间的 JSON，`app` 必须等于 `CTimer2` 才收；带 `pull_request` 字段的 Issue 会跳过（那是 PR 不是成绩）。

**同步去重键**：`用户|列表名|时间戳|毫秒` —— 同一条成绩重复拉取不会产生重复记录。

---

## 七、构建与运行

### 依赖下载必须走代理

该开发环境的 Gradle 依赖下载依赖火山引擎代理，配置写在 `gradle.properties`：

```properties
systemProp.http.proxyHost=vortex-xxxx.vortexip.cn-beijing2.volces.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=vortex-xxxx.vortexip.cn-beijing2.volces.com
systemProp.https.proxyPort=8080
```

镜像源在 `settings.gradle.kts` 里配了阿里云。

### 编译

```bash
cd CubeTimer
export JAVA_HOME=<path>/jdk-17.0.12+7
export ANDROID_HOME=<path>/android-sdk
export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug --console=plain
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

### 校验

```bash
aapt dump badging app-debug.apk | grep -E "^package|application-label:"
```

---

## 八、版本历史

> 详细的逐版更新公告见 [CHANGELOG.md](CHANGELOG.md)。


| 版本 | 主要内容 |
|---|---|
| 1.0 – 1.9 | 基础计时、多列表、成绩差值、总评页、公式速记、卡片拖动、悬浮成绩菜单、配色改淡蓝奶白、几何图标体系 |
| 2.0 | **更名 CTimer2**、包名全量迁移、横幅改静态居中、打乱中文口诀、`IconReset` 重画、键鼠检测 |
| 2.0.1 | Photo Picker 自定义背景、设置页（字体/颜色/导入导出）、侧栏底部三入口、自动备份 |
| 2.0.2 | 背景落盘修复（不再存临时 URI）、切换条与列表卡合并、内置默认背景 |
| 2.0.3 | 首次材质尝试（局部玻璃、整屏背景模糊）——用户否决整屏模糊 |
| 2.1.0 | 玻璃材质第一版：`Glass.kt` / `FrostedGlass` / 设置页材质板块 + 两个模糊滑块 |
| 2.1.1 | 侧栏改直角、抽屉与悬浮菜单玻璃化、设置页滚动优化、总评/公式整页玻璃 |
| 2.2.0 – 2.2.3 | 云端排行榜接入（Retrofit + GitHub Issues）、自动上传/下载、上传提示、卡片尺寸回归 |
| 2.3.0 | 修复取景错位导致的白边与割裂（真实窗口像素裁切）、SSL 证书链兼容、切页关浮层 |
| **2.4.0** | **页面改切换式、用户系统（用户名身份 + 前缀 + 过滤）、上传提示置顶、侧栏图标加深、启动器图标磨砂** |
| **2.4.1** | **云端成绩可删除：每行垃圾桶按钮 + 确认框，走 GraphQL `deleteIssue` 真删除，失败自动回退「关闭 + 榜单只拉 open」** |
| **2.5.0** | **抽屉与侧栏联动（抽屉从侧栏背后拉出、遮罩只压暗内容区）、功能页从侧栏一侧滑入动效、侧栏按屏高自适应保证用户图标可见、用户入口淡蓝高亮** |
| **2.5.1** | **修复特殊页面误触发计时（整屏手势改读 ViewModel 状态判定）、云端列表右上角用户选择框（可选任意用户 / 所有用户，榜单改拉全量）** |
| **2.6.0** | **毛玻璃渲染链重写（修自定义背景后玻璃仍糊默认背景）、手指位置展开的 CS-Timer 风格五槽轮盘菜单、打乱公式历史前后翻、云端成绩列表改双列卡片 + 增删动画** |
| **2.6.1** | **轮盘改中心钮 + 5 个独立圆形玻璃按钮（去掉整盘大玻璃）、尺寸缩小并自动夹紧不出屏、修跟手（死区与按钮半径分离、触发阈值 16dp）、云端卡片文字居中、卡片阶梯入场与侧栏按压动画、默认材质改实色浅底且 SOLID 跳过全屏烘焙** |
| **2.6.2** | **修云端卡片入场动画滞后（改整网格一次入场）、玻璃边缘改渐变高光、全界面动画补齐（切页缩放、抽屉错开滑入、提示条滑落、成绩列表 animateItem）** |
| **2.7.0** | **动效旗舰化：切页 400ms 右侧推近 + 旧页左移缩小、云端卡片零延迟弹入 + 弹簧让位、轮盘/侧栏/拖动卡片改 Q 弹弹簧、拖动卡片 1.05 放大 + 阴影抬起、玻璃边缘呼吸高光、底部提示条上滑凝成玻璃；修上传行文字不居中** |

---

## 九、注意事项与已知限制

1. **Token 硬编码**：GitHub PAT 写在 `network/RetrofitClient.kt` 的 `const val GITHUB_TOKEN`，任何反编译本 APK 的人都能拿到并拥有该仓库写权限。**仅限自用，不要公开分发。** 若要发布正式版，应改为运行时输入 + 只开 Issues 读写的最小权限 Token。
2. **SSL 放宽**：为应对加速器 / 运营商对 `api.github.com` 的证书替换（`Trust anchor for certification path not found`），`RetrofitClient` 对 GitHub 域名放宽了证书校验。这降低中间人攻击成本，同样只适合自用。
3. **国内直连不稳**：`api.github.com` 直连经常超时。当前策略是 25s/35s 超时 + 3 次重试；失败时顶部提示条会给出具体原因（超时 / 401 / 403 / 422）。
4. **GitHub 速率**：认证后 5000 次/小时。正常使用（一次停表一个 Issue）远远够用。
5. **页面切换语义**：功能页是切换式（再点同一图标即收起），返回按钮仍保留。
6. **材质性能**：毛玻璃模式模糊值拉很高时，多面板同时取景仍有 GPU 开销；建议 8–12dp，或切云母模式。

---

## 十、可以继续做的事

- 成绩曲线图（按日期的最佳 / 平均走势）
- 阶段拆分（CFOP 分段计时）
- 云端列表重命名 / 批量删除（现在只能删本地）
- 用户名升级为「用户名 + 校验码」防止重名冒用
- 材质预设（提供几组调好的参数，不用手动拖滑块）
