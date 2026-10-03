package com.rlcyyg.simpleinfo;

import java.util.Locale;

/**
 * 可显示的信息项。
 * <p>
 * 用枚举驱动而非平行字段：新增一项只改这个枚举，
 * 采样、渲染、配置界面三个关注点会自动适配。
 * <p>
 * 每一项由若干可独立取色的<b>部件</b>（{@link Part}）拼成，例如
 * {@code FPS: 60} 拆成 {@code NAME="FPS"} + {@code VALUE_GOOD="60"}。
 * 数值按健康度分「正常 / 较差 / 极差」三级各配一色；内存项额外拆出 {@code TOTAL} 与 {@code PERCENT}。
 */
public enum InfoItem {
    FPS("fps", 0),
    TPS("tps", 0),
    MSPT("mspt", 0),
    MEMORY("memory", 1),
    TIME("time", 2),
    GAMETIME("gametime", 2);

    /** 可独立取色的文本部件。 */
    public enum Part {
        /** 名称，如 "FPS"。 */
        NAME,
        /** 数值「正常」（健康）时的颜色，<b>关闭「自动」后生效</b>。 */
        VALUE_GOOD,
        /** 数值「较差」时的颜色，关闭「自动」后生效。 */
        VALUE_WARN,
        /** 数值「极差」时的颜色，关闭「自动」后生效。 */
        VALUE_BAD,
        /** 内存项：总分配内存。 */
        TOTAL,
        /** 内存项：占用百分比。 */
        PERCENT;

        /** 是否属于「数值」的三个颜色等级（正常 / 较差 / 极差）。 */
        public boolean isValueLevel() {
            return this == VALUE_GOOD || this == VALUE_WARN || this == VALUE_BAD;
        }

        /** 配置项 / 语言文件里的后缀，例如 {@code name}、{@code good}、{@code percent}。 */
        public String keySuffix() {
            switch (this) {
                case NAME:
                    return "name";
                case VALUE_GOOD:
                    return "good";
                case VALUE_WARN:
                    return "warn";
                case VALUE_BAD:
                    return "bad";
                case TOTAL:
                    return "total";
                case PERCENT:
                    return "percent";
                default:
                    return name().toLowerCase(Locale.ROOT);
            }
        }
    }

    /** 信息块的行数（第 0~2 行）。 */
    public static final int ROW_COUNT = 3;

    private final String key;
    /** 默认所在行（0 起）。 */
    private final int defaultRow;

    InfoItem(String key, int defaultRow) {
        this.key = key;
        this.defaultRow = defaultRow;
    }

    public String key() {
        return key;
    }

    /** 按 key 反查（用于反序列化显示顺序）；找不到返回 {@code null}。 */
    public static InfoItem byKey(String key) {
        if (key != null) {
            for (InfoItem item : values()) {
                if (item.key.equals(key)) {
                    return item;
                }
            }
        }
        return null;
    }

    public int defaultRow() {
        return defaultRow;
    }

    /** 该信息项包含哪些可取色的部件。 */
    public Part[] parts() {
        return this == MEMORY
                ? new Part[]{Part.NAME, Part.VALUE_GOOD, Part.VALUE_WARN, Part.VALUE_BAD, Part.TOTAL, Part.PERCENT}
                : new Part[]{Part.NAME, Part.VALUE_GOOD, Part.VALUE_WARN, Part.VALUE_BAD};
    }

    /** 部件的默认颜色（24 位 RGB）。 */
    public int defaultColor(Part part) {
        switch (part) {
            case NAME:
                return 0x55FFFF;              // 青 §b
            case VALUE_GOOD:
                return (this == TIME || this == GAMETIME) ? 0xFFFFFF : 0x55FF55;
            case VALUE_WARN:
                return 0xFFFF55;              // 黄 §e
            case VALUE_BAD:
                return 0xFF5555;              // 红 §c
            case TOTAL:
                return 0x55FF55;              // 绿 §a
            case PERCENT:
                return 0x55FF55;
            default:
                return 0xFFFFFF;
        }
    }

    /** 该部件是否支持「按健康度自动变色」。 */
    public boolean supportsAuto(Part part) {
        if (part.isValueLevel()) {
            return this != TIME && this != GAMETIME;
        }
        return part == Part.PERCENT && this == MEMORY;
    }

    /** 该部件默认是否启用「按健康度自动变色」。 */
    public boolean defaultAuto(Part part) {
        return supportsAuto(part);
    }

    /** 语言文件 key，例如 {@code info.simpleinfo.tps}。 */
    public String labelKey() {
        return "info.simpleinfo." + key;
    }

    public String enabledKey() {
        return key + ".enabled";
    }

    public String rowKey() {
        return key + ".row";
    }

    public String colorKey(Part part) {
        return key + ".color." + part.keySuffix();
    }

    public String autoKey(Part part) {
        // 三个数值等级共享同一个自动开关 auto.value
        return key + (part.isValueLevel() ? ".auto.value" : ".auto." + part.keySuffix());
    }
}
