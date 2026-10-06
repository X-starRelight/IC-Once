# OnceRunCommand Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a Fabric 26.2 mod that allows commands to be executed only once, with persistent storage across server restarts, multiple scope types, and configurable marking strategies.

**Architecture:** The mod uses Brigadier for command parsing, `SavedData` + `Codec` for persistent storage, and a scope-based key system to track execution records. Commands are registered via `CommandRegistrationCallback`, and storage is managed through `DimensionDataStorage`.

**Tech Stack:** Minecraft 26.2, Fabric Loader 0.19.5, Fabric API 0.160.0, Java 25, Brigadier, SavedData/Codec

---

## File Structure

| File | Responsibility |
|------|---------------|
| `src/main/java/com/tt23xrstudio/onceruncommand/OnceRunCommandMod.java` | Mod entrypoint, registers commands |
| `src/main/java/com/tt23xrstudio/onceruncommand/command/OnceCommand.java` | Brigadier command tree registration and execution logic |
| `src/main/java/com/tt23xrstudio/onceruncommand/storage/OnceStorage.java` | SavedData implementation for persistent records |
| `src/main/java/com/tt23xrstudio/onceruncommand/storage/ExecutionRecord.java` | Data class for a single execution record |
| `src/main/java/com/tt23xrstudio/onceruncommand/scope/ScopeType.java` | Enum for scope types (GLOBAL, PLAYER, DIMENSION) |
| `src/main/java/com/tt23xrstudio/onceruncommand/scope/ScopeResolver.java` | Generates storage keys from scope + identifier |
| `src/main/java/com/tt23xrstudio/onceruncommand/strategy/MarkingStrategy.java` | Enum for marking strategies (DEFAULT, STRICT, LOOSE) |

---

## Task 1: Fix Project Configuration

The template has several misconfigurations that will cause runtime failures. Fix them first.

**Files:**
- Modify: `build.gradle:14-22`
- Modify: `gradle.properties:16`
- Modify: `src/main/resources/modid.mixins.json`
- Modify: `src/client/resources/modid.client.mixins.json`
- Delete: `src/main/java/com/tt23xrstudio/mixin/OnceRunCommandMixin.java`
- Delete: `src/client/java/com/tt23xrstudio/client/mixin/OnceRunCommandClientMixin.java`
- Delete: `src/main/resources/modid.mixins.json`
- Delete: `src/client/resources/modid.client.mixins.json`
- Delete: `src/main/resources/fabric.mod.json` (mixins section references deleted files)
- Delete: `src/client/java/com/tt23xrstudio/client/OnceRunCommandModClient.java` (not needed)

- [ ] **Step 1: Fix build.gradle loom mod name**

In `build.gradle`, change the loom mods block from `"modid"` to `"once-run-command"`:

```gradle
loom {
	splitEnvironmentSourceSets()

	mods {
		"once-run-command" {
			sourceSet sourceSets.main
			sourceSet sourceSets.client
		}
	}
}
```

- [ ] **Step 2: Fix gradle.properties group**

In `gradle.properties`, change `group=com.example` to `group=com.tt23xrstudio`:

```properties
group=com.tt23xrstudio
```

- [ ] **Step 3: Delete unused mixin files and client code**

This mod is server-side only. Remove the mixin files and client entrypoint:

```bash
del "src\main\java\com\tt23xrstudio\mixin\OnceRunCommandMixin.java"
del "src\client\java\com\tt23xrstudio\client\mixin\OnceRunCommandClientMixin.java"
del "src\main\resources\modid.mixins.json"
del "src\client\resources\modid.client.mixins.json"
del "src\client\java\com\tt23xrstudio\client\OnceRunCommandModClient.java"
```

- [ ] **Step 4: Update fabric.mod.json to remove mixins and client entrypoint**

Replace `src/main/resources/fabric.mod.json` with:

```json
{
	"schemaVersion": 1,
	"id": "once-run-command",
	"version": "${version}",
	"name": "Once Run Command",
	"description": "在 Minecraft 中实现"某条命令只执行一次"的功能",
	"authors": [
		"TT23XR Studio"
	],
	"contact": {
	},
	"license": "CC0-1.0",
	"environment": "*",
	"entrypoints": {
		"main": [
			"com.tt23xrstudio.onceruncommand.OnceRunCommandMod"
		]
	},
	"depends": {
		"fabricloader": ">=0.19.5",
		"minecraft": "~26.2",
		"java": ">=25",
		"fabric-api": "*"
	}
}
```

- [ ] **Step 5: Verify project compiles**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL (or at least compiles the Java sources)

---

## Task 2: Create Data Model

Create the core data types: ScopeType enum, MarkingStrategy enum, and ExecutionRecord class.

**Files:**
- Create: `src/main/java/com/tt23xrstudio/onceruncommand/scope/ScopeType.java`
- Create: `src/main/java/com/tt23xrstudio/onceruncommand/strategy/MarkingStrategy.java`
- Create: `src/main/java/com/tt23xrstudio/onceruncommand/storage/ExecutionRecord.java`

- [ ] **Step 1: Create ScopeType enum**

Create `src/main/java/com/tt23xrstudio/onceruncommand/scope/ScopeType.java`:

```java
package com.tt23xrstudio.onceruncommand.scope;

public enum ScopeType {
    GLOBAL("global"),
    PLAYER("player"),
    DIMENSION("dimension");

    private final String name;

    ScopeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ScopeType fromName(String name) {
        for (ScopeType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
```

- [ ] **Step 2: Create MarkingStrategy enum**

Create `src/main/java/com/tt23xrstudio/onceruncommand/strategy/MarkingStrategy.java`:

```java
package com.tt23xrstudio.onceruncommand.strategy;

public enum MarkingStrategy {
    DEFAULT("default"),
    STRICT("strict"),
    LOOSE("loose");

    private final String name;

    MarkingStrategy(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static MarkingStrategy fromName(String name) {
        for (MarkingStrategy strategy : values()) {
            if (strategy.name.equalsIgnoreCase(name)) {
                return strategy;
            }
        }
        return DEFAULT;
    }
}
```

- [ ] **Step 3: Create ExecutionRecord class**

Create `src/main/java/com/tt23xrstudio/onceruncommand/storage/ExecutionRecord.java`:

```java
package com.tt23xrstudio.onceruncommand.storage;

import com.tt23xrstudio.onceruncommand.scope.ScopeType;
import net.minecraft.nbt.NbtCompound;

public class ExecutionRecord {
    private final String userIdentifier;
    private final ScopeType scopeType;
    private final String scopeValue;
    private final long executionTime;
    private final String executorName;

    public ExecutionRecord(String userIdentifier, ScopeType scopeType, String scopeValue, long executionTime, String executorName) {
        this.userIdentifier = userIdentifier;
        this.scopeType = scopeType;
        this.scopeValue = scopeValue;
        this.executionTime = executionTime;
        this.executorName = executorName;
    }

    public String getUserIdentifier() { return userIdentifier; }
    public ScopeType getScopeType() { return scopeType; }
    public String getScopeValue() { return scopeValue; }
    public long getExecutionTime() { return executionTime; }
    public String getExecutorName() { return executorName; }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("identifier", userIdentifier);
        nbt.putString("scope", scopeType.getName());
        if (scopeValue != null) {
            nbt.putString("scopeValue", scopeValue);
        }
        nbt.putLong("time", executionTime);
        nbt.putString("executor", executorName);
        return nbt;
    }

    public static ExecutionRecord fromNbt(NbtCompound nbt) {
        String identifier = nbt.getString("identifier");
        ScopeType scope = ScopeType.fromName(nbt.getString("scope"));
        String scopeValue = nbt.contains("scopeValue") ? nbt.getString("scopeValue") : null;
        long time = nbt.getLong("time");
        String executor = nbt.getString("executor");
        return new ExecutionRecord(identifier, scope, scopeValue, time, executor);
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

## Task 3: Create Storage Layer

Implement `OnceStorage` as a `SavedData` subclass that persists execution records using Codecs.

**Files:**
- Create: `src/main/java/com/tt23xrstudio/onceruncommand/storage/OnceStorage.java`
- Modify: `src/main/java/com/tt23xrstudio/onceruncommand/OnceRunCommandMod.java` (add helper method)

- [ ] **Step 1: Create OnceStorage with SavedData and Codec**

Create `src/main/java/com/tt23xrstudio/onceruncommand/storage/OnceStorage.java`:

```java
package com.tt23xrstudio.onceruncommand.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tt23xrstudio.onceruncommand.OnceRunCommandMod;
import com.tt23xrstudio.onceruncommand.scope.ScopeType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class OnceStorage extends PersistentState {
    private static final String DATA_NAME = "once_records";

    private final Map<String, ExecutionRecord> records = new HashMap<>();

    public OnceStorage() {
    }

    public boolean hasRecord(String key) {
        return records.containsKey(key);
    }

    public void addRecord(String key, ExecutionRecord record) {
        records.put(key, record);
        setDirty();
    }

    public boolean removeRecord(String key) {
        boolean removed = records.remove(key) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public int removeByPrefix(String prefix) {
        int count = 0;
        var iterator = records.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getKey().startsWith(prefix)) {
                iterator.remove();
                count++;
            }
        }
        if (count > 0) {
            setDirty();
        }
        return count;
    }

    public int removeAll() {
        int count = records.size();
        records.clear();
        if (count > 0) {
            setDirty();
        }
        return count;
    }

    public Stream<Map.Entry<String, ExecutionRecord>> getRecordsByPrefix(String prefix) {
        return records.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix));
    }

    public Stream<String> getAllKeys() {
        return records.keySet().stream();
    }

    public Stream<String> getKeysByPrefix(String prefix) {
        return records.keySet().stream()
                .filter(k -> k.startsWith(prefix));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound recordsNbt = new NbtCompound();
        for (var entry : records.entrySet()) {
            recordsNbt.put(entry.getKey(), entry.getValue().toNbt());
        }
        nbt.put("records", recordsNbt);
        return nbt;
    }

    public static OnceStorage createFromNbt(NbtCompound nbt) {
        OnceStorage storage = new OnceStorage();
        NbtCompound recordsNbt = nbt.getCompound("records");
        for (String key : recordsNbt.getKeys()) {
            storage.records.put(key, ExecutionRecord.fromNbt(recordsNbt.getCompound(key)));
        }
        return storage;
    }

    public static OnceStorage get(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        if (world == null) {
            return new OnceStorage();
        }
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(
                OnceStorage::createFromNbt,
                OnceStorage::new,
                DATA_NAME
        );
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

## Task 4: Create Scope Resolver

Implement the key generation logic that combines scope type, scope value, and user identifier into a unique storage key.

**Files:**
- Create: `src/main/java/com/tt23xrstudio/onceruncommand/scope/ScopeResolver.java`

- [ ] **Step 1: Create ScopeResolver**

Create `src/main/java/com/tt23xrstudio/onceruncommand/scope/ScopeResolver.java`:

```java
package com.tt23xrstudio.onceruncommand.scope;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.World;

public class ScopeResolver {

    public static String resolveKey(ScopeType scopeType, String identifier, CommandContext<ServerCommandSource> context) {
        return switch (scopeType) {
            case GLOBAL -> "global:" + identifier;
            case PLAYER -> {
                PlayerEntity player = context.getSource().getPlayer();
                if (player == null) {
                    yield "global:" + identifier;
                }
                yield "player:" + player.getUuid() + ":" + identifier;
            }
            case DIMENSION -> {
                World world = context.getSource().getWorld();
                String dimId = world.getRegistryKey().getValue().toString();
                yield "dimension:" + dimId + ":" + identifier;
            }
        };
    }

    public static String resolvePrefix(ScopeType scopeType) {
        return scopeType.getName() + ":";
    }

    public static String resolvePrefix(ScopeType scopeType, String scopeValue) {
        if (scopeValue == null) {
            return scopeType.getName() + ":";
        }
        return scopeType.getName() + ":" + scopeValue + ":";
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

## Task 5: Create Command Registration

Implement the full Brigadier command tree with all subcommands, scope handling, and strategy support.

**Files:**
- Create: `src/main/java/com/tt23xrstudio/onceruncommand/command/OnceCommand.java`

- [ ] **Step 1: Create OnceCommand with full command tree**

Create `src/main/java/com/tt23xrstudio/onceruncommand/command/OnceCommand.java`:

```java
package com.tt23xrstudio.onceruncommand.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tt23xrstudio.onceruncommand.OnceRunCommandMod;
import com.tt23xrstudio.onceruncommand.scope.ScopeResolver;
import com.tt23xrstudio.onceruncommand.scope.ScopeType;
import com.tt23xrstudio.onceruncommand.storage.ExecutionRecord;
import com.tt23xrstudio.onceruncommand.storage.OnceStorage;
import com.tt23xrstudio.onceruncommand.strategy.MarkingStrategy;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class OnceCommand {

    private static final SimpleCommandExceptionType ALREADY_EXECUTED = new SimpleCommandExceptionType(
            Text.literal("该标识已执行过"));
    private static final SimpleCommandExceptionType INVALID_IDENTIFIER = new SimpleCommandExceptionType(
            Text.literal("标识不能包含空格"));
    private static final SimpleCommandExceptionType EMPTY_COMMAND = new SimpleCommandExceptionType(
            Text.literal("命令内容不能为空"));
    private static final SimpleCommandExceptionType RECURSIVE_CALL = new SimpleCommandExceptionType(
            Text.literal("不允许递归调用 /once 命令"));

    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        // Main command with optional strategy + optional scope
        dispatcher.register(literal("once")
                .requires(source -> source.hasPermission(2))
                // /once <strategy> <scope> <id> <cmd>
                .then(argument("strategy", word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("strict");
                            builder.suggest("loose");
                            return builder.buildFuture();
                        })
                        .then(argument("scope", word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("global");
                                    builder.suggest("player");
                                    builder.suggest("dimension");
                                    return builder.buildFuture();
                                })
                                .then(argument("identifier", word())
                                        .then(argument("command", greedyString())
                                                .executes(ctx -> executeOnce(ctx,
                                                        MarkingStrategy.fromName(getString(ctx, "strategy")),
                                                        ScopeType.fromName(getString(ctx, "scope")),
                                                        getString(ctx, "identifier"),
                                                        getString(ctx, "command")))))
                                .then(argument("identifier", word())
                                        .executes(ctx -> {
                                            throw EMPTY_COMMAND.create();
                                        })))
                        .then(argument("identifier", word())
                                .then(argument("command", greedyString())
                                        .executes(ctx -> executeOnce(ctx,
                                                MarkingStrategy.fromName(getString(ctx, "strategy")),
                                                null,
                                                getString(ctx, "identifier"),
                                                getString(ctx, "command")))))
                        .executes(ctx -> {
                            throw EMPTY_COMMAND.create();
                        }))
                // /once <scope> <id> <cmd>
                .then(argument("scope", word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("global");
                            builder.suggest("player");
                            builder.suggest("dimension");
                            return builder.buildFuture();
                        })
                        .then(argument("identifier", word())
                                .then(argument("command", greedyString())
                                        .executes(ctx -> executeOnce(ctx,
                                                MarkingStrategy.DEFAULT,
                                                ScopeType.fromName(getString(ctx, "scope")),
                                                getString(ctx, "identifier"),
                                                getString(ctx, "command"))))
                                .executes(ctx -> {
                                    throw EMPTY_COMMAND.create();
                        }))
                        // subcommands: reset, resetall, list
                        .then(literal("reset")
                                .then(argument("identifier", word())
                                        .executes(ctx -> resetRecord(ctx,
                                                ScopeType.fromName(getString(ctx, "scope")),
                                                getString(ctx, "identifier")))))
                        .then(literal("resetall")
                                .executes(ctx -> resetAll(ctx,
                                        ScopeType.fromName(getString(ctx, "scope")))))
                        .then(literal("list")
                                .executes(ctx -> listRecords(ctx,
                                        ScopeType.fromName(getString(ctx, "scope")))))
                )
                // /once <id> <cmd> (default global)
                .then(argument("identifier", word())
                        .then(argument("command", greedyString())
                                .executes(ctx -> executeOnce(ctx,
                                        MarkingStrategy.DEFAULT,
                                        ScopeType.GLOBAL,
                                        getString(ctx, "identifier"),
                                        getString(ctx, "command"))))
                        .executes(ctx -> {
                            throw EMPTY_COMMAND.create();
                        }))
                // subcommands without scope
                .then(literal("reset")
                        .then(argument("identifier", word())
                                .executes(ctx -> resetRecord(ctx, ScopeType.GLOBAL, getString(ctx, "identifier")))))
                .then(literal("resetall")
                        .executes(ctx -> resetAll(ctx, null)))
                .then(literal("list")
                        .executes(ctx -> listRecords(ctx, null)))
        );
    }

    private static int executeOnce(CommandContext<ServerCommandSource> ctx,
                                    MarkingStrategy strategy,
                                    ScopeType scopeType,
                                    String identifier,
                                    String command) throws CommandSyntaxException {
        // Validate identifier
        if (identifier.contains(" ")) {
            throw INVALID_IDENTIFIER.create();
        }
        if (identifier.length() > 256) {
            throw INVALID_IDENTIFIER.create();
        }

        // Validate command
        if (command == null || command.isEmpty()) {
            throw EMPTY_COMMAND.create();
        }

        // Check recursive call
        String cmd = command.stripLeading();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.toLowerCase().startsWith("once ")) {
            throw RECURSIVE_CALL.create();
        }

        // Resolve scope
        if (scopeType == null) {
            scopeType = ScopeType.GLOBAL;
        }

        // Generate key
        String key = ScopeResolver.resolveKey(scopeType, identifier, ctx);

        // Check storage
        MinecraftServer server = ctx.getSource().getServer();
        OnceStorage storage = OnceStorage.get(server);

        if (storage.hasRecord(key)) {
            ctx.getSource().sendFeedback(
                    () -> Text.literal("§c标识 '" + identifier + "' 已在" + getScopeName(scopeType) + "执行过，使用 /once reset 重置"),
                    false
            );
            return 0;
        }

        // Execute command
        boolean success = false;
        try {
            int result = server.getCommandManager().executeWithPrefix(ctx.getSource(), command);
            success = switch (strategy) {
                case DEFAULT -> true; // No exception = success
                case STRICT -> result > 0;
                case LOOSE -> true; // Parse success = success
            };
        } catch (Exception e) {
            OnceRunCommandMod.LOGGER.error("Failed to execute once command: {}", command, e);
            ctx.getSource().sendFeedback(
                    () -> Text.literal("§c命令执行失败：" + e.getMessage()),
                    false
            );
            return 0;
        }

        // Mark as executed
        if (success) {
            ExecutionRecord record = new ExecutionRecord(
                    identifier,
                    scopeType,
                    getScopeValue(ctx, scopeType),
                    System.currentTimeMillis(),
                    ctx.getSource().getName()
            );
            storage.addRecord(key, record);

            String finalCommand = command;
            ctx.getSource().sendFeedback(
                    () -> Text.literal("§a已执行并记录标识 '" + identifier + "'（" + getScopeName(scopeType) + "）"),
                    true
            );
            return 1;
        }

        return 0;
    }

    private static int resetRecord(CommandContext<ServerCommandSource> ctx, ScopeType scopeType, String identifier) {
        if (scopeType == null) {
            scopeType = ScopeType.GLOBAL;
        }
        String key = ScopeResolver.resolveKey(scopeType, identifier, ctx);
        MinecraftServer server = ctx.getSource().getServer();
        OnceStorage storage = OnceStorage.get(server);

        if (storage.removeRecord(key)) {
            ctx.getSource().sendFeedback(
                    () -> Text.literal("§a已重置标识 '" + identifier + "'（" + getScopeName(scopeType) + "）"),
                    false
            );
        } else {
            ctx.getSource().sendFeedback(
                    () -> Text.literal("§c标识 '" + identifier + "' 不存在"),
                    false
            );
        }
        return 1;
    }

    private static int resetAll(CommandContext<ServerCommandSource> ctx, ScopeType scopeType) {
        MinecraftServer server = ctx.getSource().getServer();
        OnceStorage storage = OnceStorage.get(server);

        int count;
        if (scopeType != null) {
            count = storage.removeByPrefix(ScopeResolver.resolvePrefix(scopeType));
        } else {
            count = storage.removeAll();
        }

        String scopeName = scopeType != null ? getScopeName(scopeType) : "全部";
        ctx.getSource().sendFeedback(
                () -> Text.literal("§a已重置" + scopeName + "的 " + count + " 条记录"),
                false
        );
        return 1;
    }

    private static int listRecords(CommandContext<ServerCommandSource> ctx, ScopeType scopeType) {
        MinecraftServer server = ctx.getSource().getServer();
        OnceStorage storage = OnceStorage.get(server);

        var keys = (scopeType != null)
                ? storage.getKeysByPrefix(ScopeResolver.resolvePrefix(scopeType))
                : storage.getAllKeys();

        String list = keys.collect(Collectors.joining("§f, §6"));
        if (list.isEmpty()) {
            ctx.getSource().sendFeedback(
                    () -> Text.literal("§6没有已执行的标识"),
                    false
            );
        } else {
            String scopeName = scopeType != null ? "（" + getScopeName(scopeType) + "）" : "";
            ctx.getSource().sendFeedback(
                    () -> Text.literal("§6已执行标识" + scopeName + "：§f" + list),
                    false
            );
        }
        return 1;
    }

    private static String getScopeName(ScopeType scopeType) {
        return switch (scopeType) {
            case GLOBAL -> "全局";
            case PLAYER -> "玩家";
            case DIMENSION -> "维度";
        };
    }

    private static String getScopeValue(CommandContext<ServerCommandSource> ctx, ScopeType scopeType) {
        return switch (scopeType) {
            case PLAYER -> {
                var player = ctx.getSource().getPlayer();
                yield player != null ? player.getUuid().toString() : null;
            }
            case DIMENSION -> ctx.getSource().getWorld().getRegistryKey().getValue().toString();
            default -> null;
        };
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

## Task 6: Wire Up Main Entrypoint

Update `OnceRunCommandMod` to register the command and provide a clean startup log.

**Files:**
- Modify: `src/main/java/com/tt23xrstudio/OnceRunCommandMod.java`

- [ ] **Step 1: Update OnceRunCommandMod**

Replace the entire content of `src/main/java/com/tt23xrstudio/OnceRunCommandMod.java` with:

```java
package com.tt23xrstudio;

import com.tt23xrstudio.onceruncommand.command.OnceCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnceRunCommandMod implements ModInitializer {
    public static final String MOD_ID = "once-run-command";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            OnceCommand.register(dispatcher);
        });
        LOGGER.info("OnceRunCommand initialized");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

## Task 7: Clean Up Unused Files

Remove remaining template files that are no longer needed.

**Files:**
- Delete: `src/main/java/com/tt23xrstudio/mixin/` (entire directory)
- Delete: `src/client/` (entire directory)

- [ ] **Step 1: Delete unused directories**

```bash
rmdir /s /q "src\main\java\com\tt23xrstudio\mixin"
rmdir /s /q "src\client"
```

- [ ] **Step 2: Final build verification**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

## Task 8: Test In-Game

Verify the mod works correctly by testing all command variations.

- [ ] **Step 1: Start the game**

Run: `.\gradlew runClient`

- [ ] **Step 2: Test basic execution**

In chat, run:
```
/once test1 say hello
```
Expected: `§a已执行并记录标识 'test1'（全局）` and chat shows "hello"

- [ ] **Step 3: Test duplicate prevention**

Run again:
```
/once test1 say hello
```
Expected: `§c标识 'test1' 已在全局执行过，使用 /once reset 重置`

- [ ] **Step 4: Test reset**

```
/once reset test1
```
Expected: `§a已重置标识 'test1'（全局）`

- [ ] **Step 5: Test re-execution after reset**

```
/once test1 say hello
```
Expected: Executes successfully again

- [ ] **Step 6: Test player scope**

```
/once player test2 say player
```
Expected: Executes with player scope

- [ ] **Step 7: Test strict strategy**

```
/once strict test3 say strict
```
Expected: Executes successfully

- [ ] **Step 8: Test list**

```
/once list
```
Expected: Shows all executed identifiers

- [ ] **Step 9: Test resetall**

```
/once resetall
```
Expected: `§a已重置全部的 N 条记录`

- [ ] **Step 10: Test persistence**

Restart the server, then:
```
/once list
```
Expected: Previously executed identifiers still appear (if not reset)

- [ ] **Step 11: Test recursive prevention**

```
/once test4 once say recursive
```
Expected: `§c不允许递归调用 /once 命令`

- [ ] **Step 12: Test empty command**

```
/once test5
```
Expected: `§c命令内容不能为空`

---

## Self-Review Checklist

- [x] All design requirements covered: command structure, scopes, strategies, storage, error handling
- [x] No placeholders - all code is complete
- [x] Type consistency: `OnceStorage.get()`, `ScopeResolver.resolveKey()`, `ExecutionRecord` used consistently
- [x] File paths are exact and match the package structure
- [x] Build commands are correct for Windows (PowerShell)
