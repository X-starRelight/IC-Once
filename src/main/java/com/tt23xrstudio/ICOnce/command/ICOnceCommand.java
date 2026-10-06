package com.tt23xrstudio.ICOnce.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.tt23xrstudio.ICOnceMod;
import com.tt23xrstudio.ICOnce.scope.ScopeResolver;
import com.tt23xrstudio.ICOnce.scope.ScopeType;
import com.tt23xrstudio.ICOnce.storage.ExecutionRecord;
import com.tt23xrstudio.ICOnce.storage.OnceStorage;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /ic run once 命令注册与执行逻辑（原全局 /once 命令已迁移到 IC-Root 统一入口）
 * 使用 Brigadier literal() 树实现自动补全
 */
public class ICOnceCommand {

    // 保留字，不能作为标识使用
    private static final Set<String> RESERVED_WORDS = Set.of(
            "global", "player", "dimension",
            "strict", "loose",
            "reset", "resetall", "list"
    );

    private static final SimpleCommandExceptionType INVALID_IDENTIFIER = new SimpleCommandExceptionType(
            Component.literal("[IC-Once] 标识不能包含空格"));
    private static final SimpleCommandExceptionType EMPTY_COMMAND = new SimpleCommandExceptionType(
            Component.literal("[IC-Once] 命令内容不能为空"));
    private static final SimpleCommandExceptionType RECURSIVE_CALL = new SimpleCommandExceptionType(
            Component.literal("[IC-Once] 不允许递归调用 /ic run once 命令"));

    /**
     * 注册命令子树，由 IC-Root 挂载到 /ic run once 下（带自动补全）。
     * <p>
     * 原全局 /once 根节点已去除：IC 下由 mod 名 "once" 标识归属，
     * 无需再重复一层根 literal，故此处直接注册
     * run / reset / resetall / list 四个顶级节点。
     * <p>
     * 必须在初始化阶段（注册表冻结前）由 ICMod.RegMod 调用。
     */
    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        // 权限要求与原 /once 根节点一致（GAMEMASTERS）。
        // /ic run 不提升权限，目标命令自身的权限检查仍然生效；
        // 原先由统一的根节点控制可见性，现在四个顶级节点各自声明。
        Predicate<CommandSourceStack> moderatorOnly = source -> source.checkPermission(
                Identifier.fromNamespaceAndPath("minecraft", "commands"),
                PermissionLevel.GAMEMASTERS
        );

        // /ic run once run <scope> <identifier> <command>
        dispatcher.register(literal("run")
                .requires(moderatorOnly)
                .then(literal("global")
                        .then(argument("identifier", word())
                                .suggests(ICOnceCommand::suggestIdentifiers)
                                .then(argument("command", greedyString())
                                        .suggests(ICOnceCommand::suggestCommands)
                                        .executes(ctx -> handleExecute(ctx,
                                                ScopeType.GLOBAL,
                                                getString(ctx, "identifier"),
                                                getString(ctx, "command"))))
                                .executes(ctx -> handleExecute(ctx,
                                        ScopeType.GLOBAL,
                                        getString(ctx, "identifier"),
                                        null))))
                .then(literal("player")
                        .then(argument("identifier", word())
                                .suggests(ICOnceCommand::suggestIdentifiers)
                                .then(argument("command", greedyString())
                                        .suggests(ICOnceCommand::suggestCommands)
                                        .executes(ctx -> handleExecute(ctx,
                                                ScopeType.PLAYER,
                                                getString(ctx, "identifier"),
                                                getString(ctx, "command"))))
                                .executes(ctx -> handleExecute(ctx,
                                        ScopeType.PLAYER,
                                        getString(ctx, "identifier"),
                                        null))))
                .then(literal("dimension")
                        .then(argument("identifier", word())
                                .suggests(ICOnceCommand::suggestIdentifiers)
                                .then(argument("command", greedyString())
                                        .suggests(ICOnceCommand::suggestCommands)
                                        .executes(ctx -> handleExecute(ctx,
                                                ScopeType.DIMENSION,
                                                getString(ctx, "identifier"),
                                                getString(ctx, "command"))))
                                .executes(ctx -> handleExecute(ctx,
                                        ScopeType.DIMENSION,
                                        getString(ctx, "identifier"),
                                        null)))));

        // /ic run once reset <scope> <identifier>
        dispatcher.register(literal("reset")
                .requires(moderatorOnly)
                .then(literal("global")
                        .then(argument("identifier", word())
                                .suggests(ICOnceCommand::suggestIdentifiers)
                                .executes(ctx -> resetRecord(ctx,
                                        ScopeType.GLOBAL,
                                        getString(ctx, "identifier")))))
                .then(literal("player")
                        .then(argument("identifier", word())
                                .suggests(ICOnceCommand::suggestIdentifiers)
                                .executes(ctx -> resetRecord(ctx,
                                        ScopeType.PLAYER,
                                        getString(ctx, "identifier")))))
                .then(literal("dimension")
                        .then(argument("identifier", word())
                                .suggests(ICOnceCommand::suggestIdentifiers)
                                .executes(ctx -> resetRecord(ctx,
                                        ScopeType.DIMENSION,
                                        getString(ctx, "identifier"))))));

        // /ic run once resetall <scope>  /  /ic run once resetall
        dispatcher.register(literal("resetall")
                .requires(moderatorOnly)
                .then(literal("global")
                        .executes(ctx -> resetAll(ctx, ScopeType.GLOBAL)))
                .then(literal("player")
                        .executes(ctx -> resetAll(ctx, ScopeType.PLAYER)))
                .then(literal("dimension")
                        .executes(ctx -> resetAll(ctx, ScopeType.DIMENSION)))
                .executes(ctx -> resetAll(ctx, null)));

        // /ic run once list <scope>  /  /ic run once list
        dispatcher.register(literal("list")
                .requires(moderatorOnly)
                .then(literal("global")
                        .executes(ctx -> listRecords(ctx, ScopeType.GLOBAL)))
                .then(literal("player")
                        .executes(ctx -> listRecords(ctx, ScopeType.PLAYER)))
                .then(literal("dimension")
                        .executes(ctx -> listRecords(ctx, ScopeType.DIMENSION)))
                .executes(ctx -> listRecords(ctx, null)));
    }

    /**
     * 补全建议：过滤保留字，补全已存储的标识
     */
    private static CompletableFuture<Suggestions> suggestIdentifiers(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();

        // 补全已存储的标识
        MinecraftServer server = ctx.getSource().getServer();
        OnceStorage storage = OnceStorage.get(server);
        storage.getAllKeys().forEach(key -> {
            // 取冒号后面的部分作为用户标识
            int colon = key.indexOf(':');
            String display = colon >= 0 ? key.substring(colon + 1) : key;
            if (!RESERVED_WORDS.contains(display.toLowerCase())
                    && display.toLowerCase().startsWith(remaining)) {
                builder.suggest(display);
            }
        });

        return builder.buildFuture();
    }

    /**
     * 补全建议：补全服务器命令（类似 execute run 的补全效果）
     */
    private static CompletableFuture<Suggestions> suggestCommands(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        var dispatcher = ctx.getSource().getServer().getCommands().getDispatcher();
        var parseResults = dispatcher.parse(builder.getRemaining(), ctx.getSource());
        return dispatcher.getCompletionSuggestions(parseResults)
                .thenApply(suggestions -> {
                    for (var s : suggestions.getList()) {
                        builder.suggest(s.getText());
                    }
                    return builder.build();
                });
    }

    /**
     * 处理执行命令（指定作用域）
     */
    private static int handleExecute(CommandContext<CommandSourceStack> ctx,
                                     ScopeType scopeType,
                                     String identifier, String restCommand) throws CommandSyntaxException {
        if (RESERVED_WORDS.contains(identifier.toLowerCase())) {
            throw INVALID_IDENTIFIER.create();
        }
        if (identifier.contains(" ")) {
            throw INVALID_IDENTIFIER.create();
        }
        if (identifier.length() > 256) {
            throw INVALID_IDENTIFIER.create();
        }

        String fullCommand = restCommand;
        if (fullCommand == null || fullCommand.isEmpty()) {
            throw EMPTY_COMMAND.create();
        }

        // 检查递归调用：命令内容不得再次调用本命令自身。
        // 入口已迁移到 IC-Root，需同时拦截新式 "ic run once ..." 与旧式 "once ..."，
        // 防止 /ic run once run ... 嵌套调用造成无限递归。
        String cmd = fullCommand.stripLeading();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        String lowerCmd = cmd.toLowerCase();
        if (lowerCmd.startsWith("once ")
                || lowerCmd.equals("ic run once")
                || lowerCmd.startsWith("ic run once ")) {
            throw RECURSIVE_CALL.create();
        }

        MinecraftServer server = ctx.getSource().getServer();
        OnceStorage storage = OnceStorage.get(server);
        String key = ScopeResolver.resolveKey(scopeType, identifier, ctx);

        if (storage.hasRecord(key)) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("[IC-Once] §c标识 '" + identifier + "' 已在" + getScopeName(scopeType) + "执行过，若想再次执行，请使用 /ic run once reset 重置"),
                    false
            );
            return 0;
        }

        try {
            server.getCommands().performPrefixedCommand(ctx.getSource(), fullCommand);
        } catch (Exception e) {
            ICOnceMod.LOGGER.error("[IC-Once] 执行一次性命令失败: {}", fullCommand, e);
            ctx.getSource().sendSuccess(
                    () -> Component.literal("[IC-Once] §c命令执行失败：" + e.getMessage()),
                    false
            );
            return 0;
        }

        ExecutionRecord record = new ExecutionRecord(
                identifier,
                scopeType,
                null,
                System.currentTimeMillis(),
                ctx.getSource().getTextName()
        );
        storage.addRecord(key, record);
        ctx.getSource().sendSuccess(
                () -> Component.literal("[IC-Once] §a已执行并记录标识 '" + identifier + "'（" + getScopeName(scopeType) + "）"),
                true
        );
        return 1;
    }

    /**
     * 重置指定标识的执行记录
     */
    private static int resetRecord(CommandContext<CommandSourceStack> ctx, ScopeType scopeType, String identifier) {
        String key = ScopeResolver.resolveKey(scopeType, identifier, ctx);
        MinecraftServer server = ctx.getSource().getServer();
        OnceStorage storage = OnceStorage.get(server);

        if (storage.removeRecord(key)) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("[IC-Once] §a已重置标识 '" + identifier + "'（" + getScopeName(scopeType) + "）"),
                    false
            );
        } else {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("[IC-Once] §c标识 '" + identifier + "' 不存在"),
                    false
            );
        }
        return 1;
    }

    /**
     * 重置所有记录或指定作用域的记录
     */
    private static int resetAll(CommandContext<CommandSourceStack> ctx, ScopeType scopeType) {
        MinecraftServer server = ctx.getSource().getServer();
        OnceStorage storage = OnceStorage.get(server);

        int count;
        if (scopeType != null) {
            count = storage.removeByPrefix(ScopeResolver.resolvePrefix(scopeType));
        } else {
            count = storage.removeAll();
        }

        String scopeName = scopeType != null ? getScopeName(scopeType) : "全部";
        ctx.getSource().sendSuccess(
                () -> Component.literal("[IC-Once] §a已重置" + scopeName + "的 " + count + " 条记录"),
                false
        );
        return 1;
    }

    /**
     * 列出所有已执行标识或指定作用域的标识
     */
    private static int listRecords(CommandContext<CommandSourceStack> ctx, ScopeType scopeType) {
        MinecraftServer server = ctx.getSource().getServer();
        OnceStorage storage = OnceStorage.get(server);

        var keys = (scopeType != null)
                ? storage.getKeysByPrefix(ScopeResolver.resolvePrefix(scopeType))
                : storage.getAllKeys();

        String list = keys.collect(Collectors.joining("§f, §6"));
        if (list.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("[IC-Once] §6没有已执行的标识"),
                    false
            );
        } else {
            String scopeName = scopeType != null ? "（" + getScopeName(scopeType) + "）" : "";
            ctx.getSource().sendSuccess(
                    () -> Component.literal("[IC-Once] §6已执行标识" + scopeName + "：§f" + list),
                    false
            );
        }
        return 1;
    }

    /**
     * 获取作用域的中文名称
     */
    private static String getScopeName(ScopeType scopeType) {
        return switch (scopeType) {
            case GLOBAL -> "全局";
            case PLAYER -> "玩家";
            case DIMENSION -> "维度";
        };
    }
}
