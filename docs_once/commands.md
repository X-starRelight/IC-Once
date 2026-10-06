# 命令参考

IC-Once 无全局命令，全部命令挂载在 IC-Root 统一入口下：`/ic run once <子命令>`，包含四个子命令：`run`、`reset`、`resetall`、`list`。

所有子命令都要求 **GAMEMASTERS 权限**（OP 2 / 控制台 / 命令方块，对应 `minecraft:commands` 权限节点），且 `/ic` 根命令另受 IC-Root 的 moderator 权限控制；权限不足时命令不可见也不可执行。`/ic run` 不提升权限，内部命令以原命令源的权限执行。

> 下表消息为游戏内实际显示文本；原始字符串含 § 颜色码（`§a` 绿、`§c` 红、`§6` 金），错误消息由 Brigadier 以红色命令错误显示。

---

## /ic run once run \<scope\> \<identifier\> \<command\>

在指定作用域下执行一条一次性命令：标识未执行过才运行，并在成功后写入执行记录。

### 用法

```text
/ic run once run <global|player|dimension> <identifier> <command>
```

- `<scope>`：作用域字面量，`global` / `player` / `dimension`。
- `<identifier>`：用户标识，单词参数（不含空格），Tab 补全已存标识。
- `<command>`：命令内容，贪婪字符串，可含空格，不需要以 `/` 开头；Tab 按原版规则补全服务器命令。

### 行为

| 情况 | 消息 | 消息类型 | 返回值 |
|---|---|---|---|
| 标识未执行，命令执行成功 | `[IC-Once] 已执行并记录标识 '{标识}'（{作用域}）` | `sendSuccess` | `1` |
| 标识已在该作用域执行过 | `[IC-Once] 标识 '{标识}' 已在{作用域}执行过，若想再次执行，请使用 /ic run once reset 重置` | `sendSuccess`（红色文本） | `0` |
| 命令执行抛出异常（不记录） | `[IC-Once] 命令执行失败：{异常信息}` | `sendSuccess`（红色文本） | `0` |
| 命令内容为空 | `[IC-Once] 命令内容不能为空` | 命令错误（红色） | `0` |
| 标识含空格 / 是保留字 / 超过 256 字符 | `[IC-Once] 标识不能包含空格` | 命令错误（红色） | `0` |
| 命令内容递归调用本命令 | `[IC-Once] 不允许递归调用 /ic run once 命令` | 命令错误（红色） | `0` |

作用域中文名：`global` → 全局、`player` → 玩家、`dimension` → 维度。

### 示例

```text
/ic run once run global welcome say hi
# [IC-Once] 已执行并记录标识 'welcome'（全局）            返回 1

/ic run once run global welcome say hi
# [IC-Once] 标识 'welcome' 已在全局执行过，若想再次执行，请使用 /ic run once reset 重置   返回 0

/ic run once run player join give @s diamond 1
# 每名玩家各自只执行一次                                  首次 1 / 再次 0
```

---

## /ic run once reset \<scope\> \<identifier\>

重置指定作用域下某个标识的执行记录，使其可以再次执行。

### 用法

```text
/ic run once reset <global|player|dimension> <identifier>
```

- `<identifier>` Tab 补全已存标识。

### 行为

| 情况 | 消息 | 消息类型 | 返回值 |
|---|---|---|---|
| 记录存在并删除 | `[IC-Once] 已重置标识 '{标识}'（{作用域}）` | `sendSuccess` | `1` |
| 记录不存在 | `[IC-Once] 标识 '{标识}' 不存在` | `sendSuccess`（红色文本） | `1` |

> 两种情况返回值均为 `1`（命令本身执行成功），是否真的删除以消息为准。

### 示例

```text
/ic run once reset global welcome
# [IC-Once] 已重置标识 'welcome'（全局）                  返回 1

/ic run once reset global nope
# [IC-Once] 标识 'nope' 不存在                            返回 1
```

---

## /ic run once resetall \[scope\]

重置全部记录，或只重置某个作用域下的记录。

### 用法

```text
/ic run once resetall
/ic run once resetall <global|player|dimension>
```

### 行为

| 情况 | 消息 | 消息类型 | 返回值 |
|---|---|---|---|
| 执行重置（无参数 = 全部） | `[IC-Once] 已重置{全部\|全局\|玩家\|维度}的 {N} 条记录` | `sendSuccess` | `1` |

`{N}` 为实际删除的记录数，为 `0` 时同样返回 `1`。

### 示例

```text
/ic run once resetall player
# [IC-Once] 已重置玩家的 3 条记录                          返回 1

/ic run once resetall
# [IC-Once] 已重置全部的 5 条记录                          返回 1
```

---

## /ic run once list \[scope\]

列出所有已执行标识，或只列出某个作用域下的标识。

### 用法

```text
/ic run once list
/ic run once list <global|player|dimension>
```

### 行为

| 情况 | 消息 | 消息类型 | 返回值 |
|---|---|---|---|
| 有记录 | `[IC-Once] 已执行标识{（作用域）}：{键列表}`（逗号分隔，显示完整存储键） | `sendSuccess` | `1` |
| 无记录 | `[IC-Once] 没有已执行的标识` | `sendSuccess`（金色文本） | `1` |

### 示例

```text
/ic run once list
# [IC-Once] 已执行标识：global:welcome, player:<UUID>:join  返回 1

/ic run once list dimension
# [IC-Once] 已执行标识（维度）：dimension:minecraft:overworld:first  返回 1

/ic run once list
# [IC-Once] 没有已执行的标识                              返回 1
```

---

## 作用域与存储键

执行记录按"作用域键"存储，键规则如下：

| 作用域 | 存储键 | 语义 |
|---|---|---|
| `global` | `global:<标识>` | 整个存档只执行一次（默认） |
| `player` | `player:<玩家 UUID>:<标识>` | 每名玩家各自执行一次 |
| `dimension` | `dimension:<维度 ID>:<标识>` | 每个维度各自执行一次 |

- **非玩家执行源的特例**：控制台 / 命令方块等没有玩家实体的来源执行 `player` 作用域时，键退化为 `global:<标识>`。
- `list` 显示的即上述完整存储键；标识补全时显示键中 `:` 之后的部分。
- 记录以 `SavedData`（`ic-once:once_records`）保存在主世界存档中，跨重启有效；卸载/删除存档数据则记录丢失。

## 标识校验与保留字

- 标识不能包含空格、不能超过 256 字符。
- 以下 8 个保留字不可用作标识（不区分大小写）：

```text
global  player  dimension  strict  loose  reset  resetall  list
```

- 校验失败统一报错 `[IC-Once] 标识不能包含空格`。

## 递归拦截

命令内容会先去掉前导 `/` 再检查，命中以下任一形式即拒绝执行（不记录）：

```text
once ...            旧式全局命令入口
ic run once         仅入口本身
ic run once ...     递归调用本命令
```

报错：`[IC-Once] 不允许递归调用 /ic run once 命令`，防止无限递归卡死服务器。

## 权限

| 项 | 说明 |
|---|---|
| 子命令可见/执行 | GAMEMASTERS（`minecraft:commands` 权限节点，OP 2 / 控制台 / 命令方块） |
| `/ic` 根命令 | 另受 IC-Root 的 moderator 权限控制 |
| 内部命令 | 以原命令源权限执行，**不提升权限**；目标命令自身的权限检查仍生效 |

## 返回值汇总

返回值均为**命令返回值**，供命令方块、函数、宏或自动化读取：

| 命令 | 返回值 |
|---|---|
| `/ic run once run <scope> <标识> <命令>` | 成功执行 `1`；已执行 / 执行失败 / 校验错误 `0` |
| `/ic run once reset <scope> <标识>` | 恒为 `1`（是否删除成功以消息为准） |
| `/ic run once resetall [scope]` | 恒为 `1` |
| `/ic run once list [scope]` | 恒为 `1` |

## 消息文案一览

| 消息（原始文案，含颜色码） | 方法 |
|---|---|
| `[IC-Once] §a已执行并记录标识 '{标识}'（{作用域}）` | `sendSuccess` |
| `[IC-Once] §c标识 '{标识}' 已在{作用域}执行过，若想再次执行，请使用 /ic run once reset 重置` | `sendSuccess` |
| `[IC-Once] §c命令执行失败：{异常信息}` | `sendSuccess` |
| `[IC-Once] 标识不能包含空格` | 命令错误 |
| `[IC-Once] 命令内容不能为空` | 命令错误 |
| `[IC-Once] 不允许递归调用 /ic run once 命令` | 命令错误 |
| `[IC-Once] §a已重置标识 '{标识}'（{作用域}）` | `sendSuccess` |
| `[IC-Once] §c标识 '{标识}' 不存在` | `sendSuccess` |
| `[IC-Once] §a已重置{作用域名}的 {N} 条记录` | `sendSuccess` |
| `[IC-Once] §6已执行标识{（作用域）}：§f{键列表}` | `sendSuccess` |
| `[IC-Once] §6没有已执行的标识` | `sendSuccess` |
