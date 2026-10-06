# IC-Once 使用文档

Improved Commands - Once（IC-Once）是一个 Fabric Mod，在 Minecraft 中实现"某条命令只执行一次"：用唯一标识记录执行状态，未执行过才运行并把记录写入世界存档；命令挂载在 IC-Root 统一入口 `/ic run once` 下。

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
| 依赖 | IC-Root >= 0.1.0 |

## 特性

- **一次性执行**：标识未执行过才运行并记录；已执行则拒绝并返回 0
- **持久化**：执行记录以 `SavedData` 存入主世界存档，服务器重启不丢失
- **三种作用域**：`global`（整个存档一次）、`player`（每名玩家一次）、`dimension`（每个维度一次）
- **统一入口**：无全局命令，全部挂在 `/ic run once` 下，继承 IC-Root 的权限与反馈机制
- **重置与查询**：`reset` / `resetall` / `list`，可按作用域过滤，Tab 补全已存标识与服务器命令
- **权限与安全**：限制 GAMEMASTERS（OP 2）执行源，内部命令不提权；拦截递归调用；标识禁保留字、禁空格、≤ 256 字符
- **返回值驱动**：执行成功 `1`，已执行或失败 `0`，供命令方块、函数与自动化读取

## 效果速览

```text
/ic run once run global welcome say hi
# [IC-Once] 已执行并记录标识 'welcome'（全局）            （返回 1）

/ic run once run global welcome say hi
# [IC-Once] 标识 'welcome' 已在全局执行过，若想再次执行，请使用 /ic run once reset 重置   （返回 0）

/ic run once list
# [IC-Once] 已执行标识：global:welcome                    （返回 1）
```

## 文档导航

| 文档 | 内容 |
|---|---|
| [getting-started.md](getting-started.md) | **安装上手**：环境要求、构建、安装、游戏内验证 |
| [commands.md](commands.md) | **命令参考**：`run` / `reset` / `resetall` / `list` 的语义、消息文案、返回值、权限 |
| [faq.md](faq.md) | **常见问题**：重复执行、递归拦截、作用域、重置与排查 |

## 阅读路径

- **第一次使用**：[getting-started.md](getting-started.md) → [commands.md](commands.md)
- **查阅命令语义与返回值**：[commands.md](commands.md)
- **遇到问题/无输出**：[faq.md](faq.md)
- **IC-Root 接入与 API**：[../docs_icroot/README.md](../docs_icroot/README.md)

## 项目结构

```text
IC-Once/
├── docs_once/                   # 本文档目录
│   ├── README.md                # 你在这里
│   ├── getting-started.md
│   ├── commands.md
│   └── faq.md
├── docs_icroot/                 # IC-Root 使用文档
├── src/main/java/com/tt23xrstudio/
│   ├── ICOnceMod.java           # 入口：注册到 IC-Root（name = "once"）
│   └── ICOnce/
│       ├── command/ICOnceCommand.java   # 命令树：run / reset / resetall / list
│       ├── scope/                        # 作用域：类型与键生成
│       ├── storage/                      # 持久化存储与执行记录
│       └── strategy/MarkingStrategy.java # 标记策略（预留）
└── deps/ic-root/                # IC-Root 构建产物（文件依赖）
```
