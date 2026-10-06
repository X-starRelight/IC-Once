package com.tt23xrstudio;

import com.tt23xrstudio.ICOnce.command.ICOnceCommand;
import com.tt23xrstudio.icroot.ICMod;
import com.tt23xrstudio.icroot.ICSeries;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ICOnce 模组主入口
 * 在 Minecraft 中实现"某条命令只执行一次"的功能
 *
 * <p>命令不占用全局命令空间，而是注册到 IC-Root 统一入口下，
 * 游戏内通过 {@code /ic run once ...} 调用。</p>
 */
public class ICOnceMod implements ModInitializer {
    public static final String MOD_ID = "ic-once";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 第 1 步：注册到 IC-Root 注册表，name 全表唯一，使用默认全局反馈开关（开）
        ICMod mod = ICSeries.register("once");

        // 第 2 步：把 /once 命令树交给 IC，挂载到 /ic run once 下。
        // 必须在初始化阶段完成：服务器启动、命令树构建后注册表会冻结。
        // 第 3 步无需代码：ic-root 在命令树构建时自动挂载并冻结注册表。
        mod.RegMod(ICOnceCommand::register);

        LOGGER.info("[IC-Once] 已初始化，命令入口：/ic run once");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
