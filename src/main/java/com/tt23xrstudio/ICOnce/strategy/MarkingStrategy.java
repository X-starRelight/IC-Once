package com.tt23xrstudio.ICOnce.strategy;

/**
 * 标记策略枚举，决定何时将命令标记为已执行
 */
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

    /**
     * 根据名称查找策略，未找到返回默认策略
     * @return 对应的MarkingStrategy
     */
    public static MarkingStrategy fromName(String name) {
        for (MarkingStrategy strategy : values()) {
            if (strategy.name.equalsIgnoreCase(name)) {
                return strategy;
            }
        }
        return DEFAULT;
    }
}
