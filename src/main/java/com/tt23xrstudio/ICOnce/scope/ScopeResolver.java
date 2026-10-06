package com.tt23xrstudio.ICOnce.scope;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * 作用域键生成器，根据作用域类型和上下文生成唯一的存储键
 */
public class ScopeResolver {

    /**
     * 根据作用域类型、用户标识和命令上下文生成完整的存储键
     */
    public static String resolveKey(ScopeType scopeType, String identifier, CommandContext<CommandSourceStack> context) {
        if (scopeType == null) {
            scopeType = ScopeType.GLOBAL;
        }
        return switch (scopeType) {
            case GLOBAL -> "global:" + identifier;
            case PLAYER -> {
                Player player = context.getSource().getPlayer();
                if (player == null) {
                    yield "global:" + identifier;
                }
                yield "player:" + player.getUUID() + ":" + identifier;
            }
            case DIMENSION -> {
                ServerLevel level = context.getSource().getLevel();
                String dimId = level.dimension().identifier().toString();
                yield "dimension:" + dimId + ":" + identifier;
            }
        };
    }

    /**
     * 获取指定作用域类型的前缀（用于批量查询）
     */
    public static String resolvePrefix(ScopeType scopeType) {
        return scopeType.getName() + ":";
    }

    /**
     * 获取指定作用域类型和作用域值的前缀
     */
    public static String resolvePrefix(ScopeType scopeType, String scopeValue) {
        if (scopeValue == null) {
            return scopeType.getName() + ":";
        }
        return scopeType.getName() + ":" + scopeValue + ":";
    }
}
