# log/ 目录说明

本目录存放 **sodium_plugin** 模组的更新日志与版本记录，供发版时追溯变更。

## 文件清单

| 文件 | 用途 |
|---|---|
| `更新日志.txt` | 主更新日志，按版本倒序记录每次发版的变更内容 |
| `README.md` | 本文件，说明目录用途与更新日志的书写规范 |

## 更新日志书写规范

### 版本条目格式

新版本**追加到文件顶部**（倒序，最新版在最前）。每个版本一个条目：

```
==== v1.0.2（2026-08-28）====
适配 Minecraft 1.21.11 / Fabric Loader 0.19.3。

【新增】
- 受击回放：记录你受到的每一次伤害，可在设置页浏览历史与死亡信息。

【修复】
- 受击回放记录界面文字不可见（颜色缺少 alpha 通道导致完全透明）。

【变更】
- 伽马值上限由 1500 提升至 3000。

【移除】
- （无则整节省略）
```

### 分类标签

按下列顺序使用，无内容的分类直接省略：

| 标签 | 含义 |
|---|---|
| `【新增】` | 新功能、新选项 |
| `【变更】` | 既有功能的调整（取值范围、默认值、行为变化） |
| `【修复】` | Bug 修复 |
| `【移除】` | 删除的功能 |
| `【内部】` | 重构、依赖升级等用户不可见的改动 |

### 书写要求

- 一条一行，动词开头，说清**对用户的影响**，不写实现细节（如「改了 Mixin」应写为「受击抖动可单独关闭」）。
- 涉及数值范围的变更，写明**旧值 → 新值**。
- 破坏性变更（如存档路径变化、旧配置失效）用 `【变更】` 并加 `⚠️` 标注。

## 版本号规范

遵循 `主版本.次版本.修订号`：

| 段位 | 递增时机 |
|---|---|
| 主版本 | 不兼容的架构调整、modid 变更、存档格式变更 |
| 次版本 | 新增功能、新增设置项 |
| 修订号 | Bug 修复、文案调整、依赖升级 |

版本号在 `gradle.properties` 的 `mod_version` 中修改，`fabric.mod.json` 通过 `${version}` 自动继承，**不要手改后者**。

产物命名由 `archives_base_name` 决定，当前为 `sodium_plugin-<版本>.jar`。

## 发版检查清单

1. 修改 `gradle.properties` 的 `mod_version`。
2. 在 `更新日志.txt` 顶部追加版本条目。
3. 执行 `./gradlew clean build`。
4. 取 `build/libs/sodium_plugin-<版本>.jar` 放入游戏 `mods/` 目录。
5. **删除 `mods/` 中旧版本的 jar**，仅保留一个。

> ⚠️ **modid 或 jar 名变更后，必须删除 `mods/` 里的旧 jar。**
> 两个 jar 若包含同名类（本模组为 `ws.sodiunplugin.*`），Fabric 加载顺序不确定，
> 会导致配置字段取到 null，进而在打开 Sodium 视频设置时崩溃
> （`SteppedValidator.getValidatedValue` 处 NPE）。

## 数值范围同步约定

以下设置项的取值范围在**三处**必须保持一致，改动时缺一不可：

| 设置项 | ① 滑块范围 | ② 逻辑夹取 | ③ 文案 |
|---|---|---|---|
| 伽马值 | `ShakeConfigEntryPoint` 的 `setRange(1, 3000, 1)` | `ShakeConfig.gammaValue` 的 `coerceIn(1, 3000)` | 中英 lang 的 `gamma_value.tooltip` |
| 视角抖动强度 | `setRange(0, 100, 1)` | `shakeStrength` 的 `coerceIn(0, 100)` | — |
| 粒子数量 | `setRange(0, 100, 1)` | `particlePercentage` 的 `coerceIn(0, 100)` | — |
| 视场角效果 | `setRange(50, 300, 1)` | `fovEffect` 的 `coerceIn(50, 300)` | — |
| 最大记录条数 | `setRange(10, 500, 10)` | `maxRecords` 的 `coerceIn(10, 500)` | — |

① 决定滑块可拖动范围，② 决定越界值写入时的夹取，③ 仅为展示文案。三者不一致会导致滑块拖不动或提示与实际不符。

## 模组信息速查

写日志时对照使用：

- **modid**：`sodium_plugin`（Java 包名仍为 `ws.sodiunplugin`，两者独立，不随 modid 变更）
- **显示名**：Sodium View Shake Control
- **环境**：Minecraft 1.21.11 / Fabric Loader 0.19.3 / Java 21+
- **依赖**：fabric-api、fabric-language-kotlin、sodium ≥ 0.8.13
- **设置入口**：Sodium 视频设置 → 「Sodium View Shake Control」分组，下设「视角抖动控制」与「受击回放」两页

### 功能清单

**视角抖动控制页**

- 视角抖动：视角抖动强度、疾跑抖动、受击抖动
- 镜头特效：反胃扭曲、药水视场角缩放、粒子数量、视场角效果、伽马值、显示隐身玩家
- HUD 增强：药水剩余时间、临期闪烁边框、伤害显示条、爆炸伤害归因

**受击回放页**

- 启用受击记录、最大记录条数、查看受击回放

## 待办

- [ ] `更新日志.txt` 顶部的模组简介仍使用旧 modid `ws_sodium_plugin`，且功能列表不完整，需按上表同步更新。
