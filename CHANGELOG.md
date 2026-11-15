# 更新公告 / CHANGELOG

CTimer2 的所有版本更新记录。APK 不放进仓库，也不作为 Release 附件发布（安装包内嵌了云端同步用的 Token，公开分发等于公开凭证）。

---

## v2.4.1（当前版本，versionCode 22）

### 新增

- **云端成绩可以删除了**。排行榜每行右侧多了一个垃圾桶按钮：
  - 点击后先弹确认框（显示 Issue 编号、成绩、列表名，并注明不可撤销）；
  - 确认后通过 **GraphQL `deleteIssue` mutation** 把对应 Issue 从 GitHub 仓库彻底删除；
  - 若 GraphQL 不可用（权限/网络问题），自动回退为 `PATCH` 把 Issue 状态改成 `closed`；
  - 排行榜只拉 `state=open` 的 Issue，因此「关闭」同样等同于移出榜单，不会再被拉回来；
  - 删除进行中该行转圈并禁止重复点击；成功后立即从列表消失并提示「已从 GitHub 删除成绩 12.34」；失败不做本地移除，顶部提示具体原因（403 / 超时 等）。

### 说明

- 原计划使用的 `DELETE /repos/{owner}/{repo}/issues/{number}` 对个人账号仓库不可靠，已移除，仅保留 GraphQL 与 PATCH 两条路径。

---

## v2.4.0（versionCode 21）

### 新增

- **页面改为切换式**：侧栏图标点一次打开、再点一次收起，同一时刻只保留一个功能页，不用每次点左上角返回；设置页同理。
- **用户系统（只要用户名，不要密码）**：
  - 侧栏最底部新增人形图标入口；
  - 登录后上传的成绩标题带用户名前缀：`[CTimer2] [用户名] 12.34 · 默认列表`；
  - 登录后拉取只拉这个用户名的成绩，别人的不会混进自己的榜单；未登录显示全部；
  - 用户名存 DataStore，重启保持登录态。

### 改进

- **上传提示置顶**：顶部「成绩正在上传至 GitHub 仓库…」提示加 `zIndex`，不再被证书卡片、抽屉、浮层遮挡。
- **侧栏顶部秒表图标加深**：从浅蓝改为主蓝，在玻璃底色上更清楚。
- **启动器图标改磨砂**：半透明白色磨砂主体 + 淡阴影，背景色压深到 `#CBDDEE`，形成磨砂玻璃压在浅蓝底上的质感。
- **侧栏图标间距适配矮屏**：7 个图标间距压缩（格子 40dp、间隙 4dp），避免横屏矮屏时最底部的用户图标被裁掉。

---

## v2.3.0（versionCode 20）

### 修复

- **毛玻璃白边**：取景图改用根布局真实像素尺寸换算（原用 `screenWidthDp`，横屏全屏下与真实窗口像素不一致，导致取景图铺不满、卡片边缘露出一条白底）。
- **面板渲染割裂**：整屏背景只模糊一次并缓存为 `GlassBackdrop`，每块面板用 `drawImage(srcOffset, srcSize)` 精确裁自己那块；`backdrop` 参数区分「窗口」与「分区」，避免双层玻璃叠加。
- **证书链错误**：针对 `java.security.cert.CertPathValidatorException: Trust anchor for certification path not found`（加速器 / 运营商替换 TLS 证书链），对 GitHub 域名放宽证书校验，并把失败原因翻译成人话显示在顶部。
- 切页时自动关闭残留浮层。

---

## v2.2.0 – v2.2.3（versionCode 17–19）

### 新增

- **云端排行榜**：数据载体是 GitHub Issue，一条成绩 = 一个 Issue（仓库 `Dramtwo/CTimer-List`）。
- **自动上传**：每次停表自动把成绩推到云端（3 次重试），顶部浮条显示进度。
- **自动下载**：每次启动拉一次云端全量成绩，按「用户 + 列表名 + 时间戳 + 毫秒」去重后补进本地；云端有而本地缺的列表自动创建。

### 修复

- 卡片尺寸回归正常（避免玻璃面板参与父测量导致卡片被撑高）。

---

## v2.1.0 – v2.1.1（versionCode 15–16）

### 新增

- **材质系统**：毛玻璃 / 云母 / 关闭三种模式，面板模糊与背景模糊两个滑块（0–40dp，背景默认 0 保持通亮）。
- 设置页材质板块；抽屉、悬浮菜单、总评页、公式页全部玻璃化。

### 修复

- 侧栏改直角（右侧与底部无圆角，避免被屏幕裁切露缝）。

---

## v2.0 – v2.0.3（versionCode 9–14）

### 新增

- **更名 CTimer2**、包名全量迁移。
- 自定义背景（Android 13+ Photo Picker，零权限），背景落盘到私有目录，重启不丢。
- 内置默认背景（海岸草地照片）。
- 打乱公式显示中文口诀（`R=上 / R'=下 / R2=上上` …）。
- 键鼠连接检测（左上角 Logo 卡显示小胶囊）。
- 成绩自动备份到公共下载目录 + 首次启动自动恢复 + 手动导入导出。

---

## v1.0 – v1.9（versionCode 1–8）

基础计时、多列表管理、成绩差值标注、成绩总评页、公式速记页、卡片拖动与位置记忆、拖动呼出成绩菜单、淡蓝 + 奶白配色、几何图标体系。

---

## 关于安装包

本仓库只放源码，**不提供 APK 下载**：

1. APK 内 `network/RetrofitClient.kt` 的 `GITHUB_TOKEN` 是真实凭证，公开下载即可被反编译提取，等于公开自己仓库的写权限；
2. 源码里的 Token 在提交前已替换为占位符 `PUT_YOUR_GITHUB_TOKEN_HERE`。

自行编译：

```bash
git clone https://github.com/Dramtwo/CTimer-Repository.git
cd CTimer-Repository
# 填入自己的 GitHub 细粒度 Token（需 Issues: Read and write 权限）
# app/src/main/java/com/ctimer2/network/RetrofitClient.kt → const val GITHUB_TOKEN
./gradlew assembleDebug
```

产物在 `app/build/outputs/apk/debug/app-debug.apk`。
