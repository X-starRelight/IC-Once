package com.tt23xrstudio.ICOnce.storage;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 持久化存储实现，管理所有一次性命令的执行记录
 * 使用SavedData + Codec机制，数据保存在世界存档中
 */
public class OnceStorage extends SavedData {
    private static final String DATA_NAME = "once_records";

    // 使用Codec进行序列化/反序列化
    public static final Codec<OnceStorage> CODEC = Codec.unboundedMap(Codec.STRING, ExecutionRecord.CODEC)
            .xmap(
                    map -> {
                        OnceStorage storage = new OnceStorage();
                        storage.records.putAll(map);
                        return storage;
                    },
                    storage -> storage.records
            );

    private final Map<String, ExecutionRecord> records = new HashMap<>();

    public OnceStorage() {
    }

    /**
     * 检查指定键是否已存在执行记录
     */
    public boolean hasRecord(String key) {
        return records.containsKey(key);
    }

    /**
     * 添加执行记录
     */
    public void addRecord(String key, ExecutionRecord record) {
        records.put(key, record);
        setDirty();
    }

    /**
     * 删除指定键的执行记录
     * @return 是否成功删除
     */
    public boolean removeRecord(String key) {
        boolean removed = records.remove(key) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    /**
     * 删除指定前缀的所有记录
     * @return 删除的记录数
     */
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

    /**
     * 删除所有记录
     * @return 删除的记录数
     */
    public int removeAll() {
        int count = records.size();
        records.clear();
        if (count > 0) {
            setDirty();
        }
        return count;
    }

    /**
     * 获取指定前缀的所有记录条目
     */
    public Stream<Map.Entry<String, ExecutionRecord>> getRecordsByPrefix(String prefix) {
        return records.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix));
    }

    /**
     * 获取所有键
     */
    public Stream<String> getAllKeys() {
        return records.keySet().stream();
    }

    /**
     * 获取指定前缀的所有键
     */
    public Stream<String> getKeysByPrefix(String prefix) {
        return records.keySet().stream()
                .filter(k -> k.startsWith(prefix));
    }

    /**
     * 定义SavedDataType，用于SavedData的序列化/反序列化
     */
    public static final SavedDataType<OnceStorage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("ic-once", DATA_NAME),
            OnceStorage::new,
            CODEC,
            null
    );

    /**
     * 获取服务器的存储实例
     */
    public static OnceStorage get(MinecraftServer server) {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) {
            return new OnceStorage();
        }
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
