# Improved Commands - Once

Improved Commands - Once（IC-Once）是一个 Fabric Mod，在 Minecraft 中实现"某条命令只执行一次"：为每条一次性命令指定唯一标识，未执行过才运行并把记录写入世界存档；命令挂载在 IC-Root 统一入口 `/ic run once` 下，不污染全局命令空间，返回值可供命令方块、函数、自动化读取。

## 基本信息

| 项 | 值 |
|---|---|
| Mod ID | `ic-once` |
| 包名 | `com.tt23xrstudio` |
| 开发者 | tt23xrstudio |
| Minecraft | 26.2 |
| Fabric Loader | >= 0.19.0 |
| Fabric API | 0.160.0+26.2 |
| Java | 25 |
| 构建 | Gradle + fabric-loom |
| 依赖 | IC-Root >= 0.1.0 |

## 特性

- **一次性执行**：标识未执行过才运行并记录；已执行则拒绝并返回 0
- **持久化**：执行记录以 `SavedData` 存入主世界存档，服务器重启不丢失
- **三种作用域**：`global`（整个存档一次）、`player`（每名玩家一次）、`dimension`（每个维度一次）
- **统一入口**：无全局命令，全部挂在 `/ic run once` 下，继承 IC-Root 的权限与反馈机制
- **重置与查询**：`reset` / `resetall` / `list`，可按作用域过滤，Tab 补全已存标识与服务器命令
- **权限与安全**：限制 GAMEMASTERS（OP 2）执行源，内部命令不提权；拦截 `once` / `ic run once` 递归调用；标识禁保留字、禁空格、≤ 256 字符
- **返回值驱动**：执行成功 `1`，已执行或失败 `0`，供命令方块、函数与自动化读取

## 效果速览

```text
/ic run once run global welcome say hi
# [IC-Once] 已执行并记录标识 'welcome'（全局）            （返回 1）

/ic run once run global welcome say hi
# [IC-Once] 标识 'welcome' 已在全局执行过，若想再次执行，请使用 /ic run once reset 重置   （返回 0）

/ic run once list
# [IC-Once] 已执行标识：global:welcome                    （返回 1）

/ic run once reset global welcome
# [IC-Once] 已重置标识 'welcome'（全局）                  （返回 1）
```

## 快速上手

### 环境要求

| 项 | 版本 |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | >= 0.19.0 |
| Fabric API | 0.160.0+26.2 |
| Java | 25 |
| IC-Root | >= 0.1.0 |

### 构建

```bash
gradlew build
```

### 安装

IC-Once 硬依赖 IC-Root（`fabric.mod.json` 已声明 `depends: ic-root >= 0.1.0`），两个 jar 都要放入 `mods/`：

```text
mods/
├── ic-root-0.1.0.jar     # 统一命令入口（前置依赖）
└── ic-once-0.1.0.jar     # 本 Mod
```

### 游戏内验证

需要 OP 或控制台权限（GAMEMASTERS）：

```text
/ic run once run global welcome say hi
# [IC-Once] 已执行并记录标识 'welcome'（全局）            （返回 1）

/ic run once run global welcome say hi
# [IC-Once] 标识 'welcome' 已在全局执行过……              （返回 0）
```

补全：`/ic run once ` 后按 Tab，可补全 `run` / `reset` / `resetall` / `list`；标识参数补全已存标识，命令内容补全服务器命令。

完整安装与验证流程见 [docs_once/getting-started.md](docs_once/getting-started.md)。

## 项目结构

```text
IC-Once/
├── src/main/java/com/tt23xrstudio/
│   ├── ICOnceMod.java              # 入口：注册到 IC-Root（name = "once"）
│   └── ICOnce/
│       ├── command/ICOnceCommand.java   # 命令树：run / reset / resetall / list
│       ├── scope/ScopeType.java         # 作用域枚举：global / player / dimension
│       ├── scope/ScopeResolver.java     # 存储键生成
│       ├── storage/OnceStorage.java     # SavedData 持久化存储
│       ├── storage/ExecutionRecord.java # 单条执行记录（Codec 序列化）
│       └── strategy/MarkingStrategy.java # 标记策略（预留）
├── src/client/java/.../ICOnceClient.java # 客户端入口（空实现）
├── src/main/resources/fabric.mod.json
├── docs_once/                      # 本 Mod 使用文档
├── docs_icroot/                    # IC-Root 使用文档（基础依赖）
├── deps/ic-root/                   # IC-Root 构建产物（文件依赖）
├── build.gradle                    # Fabric Loom 构建配置
└── .github/workflows/              # CI 自动构建
```

## 文档导航

| 文档 | 内容 |
|---|---|
| [docs_once/README.md](docs_once/README.md) | **文档索引**：简介、特性、阅读路径 |
| [docs_once/getting-started.md](docs_once/getting-started.md) | **安装上手**：环境、构建、安装、游戏内验证 |
| [docs_once/commands.md](docs_once/commands.md) | **命令参考**：run / reset / resetall / list 的语义、消息、返回值、权限 |
| [docs_once/faq.md](docs_once/faq.md) | **常见问题**：重复执行、递归拦截、作用域、重置与排查 |
| [docs_icroot/README.md](docs_icroot/README.md) | **IC-Root 文档**：统一入口 `/ic` 的接入与 API（本 Mod 基础依赖） |

## 许可证

本项目采用 [GNU GPL-3.0](LICENSE) 许可证。
