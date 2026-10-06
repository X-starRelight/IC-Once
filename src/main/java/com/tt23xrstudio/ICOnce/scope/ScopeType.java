package com.tt23xrstudio.ICOnce.scope;

/**
 * 作用域类型枚举，定义一次性命令的影响范围
 */
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

    /**
     * 根据名称查找作用域类型，不区分大小写
     * @return 对应的ScopeType，未找到返回null
     */
    public static ScopeType fromName(String name) {
        for (ScopeType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
