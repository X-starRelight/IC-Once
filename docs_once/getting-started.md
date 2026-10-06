# 快速上手

本指南帮助你构建并安装 IC-Once，在游戏内验证"某条命令只执行一次"。

## 1. 环境要求

| 项 | 版本 |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | >= 0.19.0 |
| Fabric API | 0.160.0+26.2 |
| Java | 25 |
| IC-Root | >= 0.1.0（前置依赖） |

## 2. 构建

在项目根目录执行：

```bash
gradlew build
```

产物：`build/libs/ic-once-0.1.0.jar`。

> **注意**：本项目通过普通文件依赖引用 IC-Root（`implementation files("./deps/ic-root/ic-root-0.1.0.jar")`，Minecraft 26.2 为非混淆版本，不可用 `modImplementation`）。若 `deps/ic-root/` 下缺少该 jar，请先构建 IC-Root 项目，把产物复制进来。

## 3. 安装

IC-Once 硬依赖 IC-Root，两个 jar 都要放入客户端/服务端的 `mods/` 目录：

```text
mods/
├── ic-root-0.1.0.jar     # 统一命令入口（前置依赖）
└── ic-once-0.1.0.jar     # 本 Mod
```

`fabric.mod.json` 已内置依赖声明，缺失 IC-Root 时加载器会直接报错而非运行时崩溃：

```json
{
    "depends": {
        "fabricloader": ">=0.19.0",
        "minecraft": "~26.2",
        "java": ">=25",
        "fabric-api": "*",
        "ic-root": ">=0.1.0"
    }
}
```

启动日志中可见 `[IC-Once] 已初始化，命令入口：/ic run once` 即加载成功。

## 4. 游戏内验证

需要 OP 或控制台权限（GAMEMASTERS；`/ic` 根命令另受 IC-Root 的 moderator 权限控制）。

| 命令 | 预期消息 | 返回值 |
|---|---|---|
| `/ic run once run global welcome say hi` | `[IC-Once] 已执行并记录标识 'welcome'（全局）`（绿色） | `1` |
| 再次执行同一条 | `[IC-Once] 标识 'welcome' 已在全局执行过，若想再次执行，请使用 /ic run once reset 重置`（红色） | `0` |
| `/ic run once list` | `[IC-Once] 已执行标识：global:welcome` | `1` |
| `/ic run once reset global welcome` | `[IC-Once] 已重置标识 'welcome'（全局）`（绿色） | `1` |
| 再次执行第一条 | 重新执行并记录（已重置） | `1` |

补充验证：

- **持久化**：重启服务器/重新进入世界，再次执行第一条命令 → 仍返回 `0`
- **玩家作用域**：`/ic run once run player join say hi` → 每名玩家各自只执行一次
- **维度作用域**：`/ic run once run dimension first say hi` → 每个维度各自只执行一次
- **补全**：`/ic run once ` 后按 Tab 补全四个子命令；标识参数补全已存标识；命令内容补全服务器命令

## 5. 下一步

- 命令完整语义、作用域键规则与消息文案：[commands.md](commands.md)
- 遇到问题：[faq.md](faq.md)
- IC-Root 统一入口与 API：[../docs_icroot/README.md](../docs_icroot/README.md)
