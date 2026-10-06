package com.tt23xrstudio.ICOnce.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tt23xrstudio.ICOnce.scope.ScopeType;

import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

/**
 * 单条执行记录数据类
 */
public class ExecutionRecord {
    // Codec定义：用于NBT序列化和反序列化
    public static final Codec<ExecutionRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("identifier").forGetter(ExecutionRecord::getUserIdentifier),
            Codec.STRING.xmap(ScopeType::fromName, ScopeType::getName).fieldOf("scope").forGetter(ExecutionRecord::getScopeType),
            Codec.STRING.optionalFieldOf("scopeValue").forGetter(r -> Optional.ofNullable(r.getScopeValue())),
            Codec.LONG.fieldOf("time").forGetter(ExecutionRecord::getExecutionTime),
            Codec.STRING.fieldOf("executor").forGetter(ExecutionRecord::getExecutorName)
    ).apply(instance, (id, scope, sv, time, exec) -> new ExecutionRecord(id, scope, sv.orElse(null), time, exec)));

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

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("identifier", userIdentifier);
        nbt.putString("scope", scopeType.getName());
        if (scopeValue != null) {
            nbt.putString("scopeValue", scopeValue);
        }
        nbt.putLong("time", executionTime);
        nbt.putString("executor", executorName);
        return nbt;
    }

    public static ExecutionRecord fromNbt(CompoundTag nbt) {
        String identifier = nbt.getString("identifier").orElse("");
        ScopeType scope = ScopeType.fromName(nbt.getString("scope").orElse(""));
        String scopeValue = nbt.getString("scopeValue").orElse(null);
        long time = nbt.getLong("time").orElse(0L);
        String executor = nbt.getString("executor").orElse("");
        return new ExecutionRecord(identifier, scope, scopeValue, time, executor);
    }
}
