package com.rlcyyg.simpleinfo;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 模组全部配置（{@link net.minecraftforge.fml.config.ModConfig.Type#CLIENT}）。
 * <p>
 * 公共侧类，<b>不得</b> import 任何 {@code net.minecraft.client.*}。
 * <p>
 * 所有 {@code ConfigValue} 句柄都在静态块里<b>随 define 的返回值直接缓存</b>。
 * {@code ForgeConfigSpec.Builder} 没有公开的 {@code get(path, key)} 反查方法，
 * 所以只能这样做 —— 不要试图事后按路径反查。
 */
public final class SimpleInfoConfig {

    public static final ForgeConfigSpec SPEC;

    // ================= 全局 =================
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue FONT_SCALE;
    public static final ForgeConfigSpec.IntValue REFRESH_TICKS;
    public static final ForgeConfigSpec.IntValue MARGIN_X;
    public static final ForgeConfigSpec.IntValue MARGIN_Y;
    public static final ForgeConfigSpec.ConfigValue<String> ANCHOR;
    public static final ForgeConfigSpec.BooleanValue SHOW_SHADOW;
    public static final ForgeConfigSpec.BooleanValue HIDE_IN_CHAT_SCREEN;
    public static final ForgeConfigSpec.ConfigValue<String> MEMORY_UNIT;
    public static final ForgeConfigSpec.BooleanValue SHOW_MEMORY_PERCENT;
    /** HUD 的显示顺序（item key 列表）。同行的先后由此决定。 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ORDER;

    // ================= 每项 =================
    private static final Map<InfoItem, ForgeConfigSpec.BooleanValue> ITEM_ENABLED = new EnumMap<>(InfoItem.class);
    private static final Map<InfoItem, ForgeConfigSpec.IntValue> ITEM_ROW = new EnumMap<>(InfoItem.class);
    private static final Map<InfoItem, Map<InfoItem.Part, ForgeConfigSpec.IntValue>> ITEM_COLOR = new EnumMap<>(InfoItem.class);
    private static final Map<InfoItem, Map<InfoItem.Part, ForgeConfigSpec.BooleanValue>> ITEM_AUTO = new EnumMap<>(InfoItem.class);

    /** 无参入口，兼容 {@code SimpleInfoConfig.INSTANCE} 写法。 */
    public static final SimpleInfoConfig INSTANCE = new SimpleInfoConfig();

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        // ================= 全局 =================
        b.comment("Simple Info - client side HUD").push("general");
        b.comment("Master toggle for the whole HUD.").translation("config.simpleinfo.enabled");
        ENABLED = b.define("enabled", true);
        b.comment("Font scale. 1.0 = vanilla size.").translation("config.simpleinfo.fontScale");
        FONT_SCALE = b.defineInRange("fontScale", 1.0D, 0.5D, 5.0D);
        b.comment("Ticks between two updates. Higher = less overhead.").translation("config.simpleinfo.refreshTicks");
        REFRESH_TICKS = b.defineInRange("refreshTicks", 20, 1, 1200);
        b.comment("Horizontal distance in pixels from the anchored screen corner.").translation("config.simpleinfo.marginX");
        MARGIN_X = b.defineInRange("marginX", 1, 0, 512);
        b.comment("Vertical distance in pixels from the anchored screen corner.").translation("config.simpleinfo.marginY");
        MARGIN_Y = b.defineInRange("marginY", 1, 0, 512);

        b.comment("Which screen corner the whole info block sticks to.").translation("config.simpleinfo.anchor");
        ANCHOR = b.defineInList("anchor", Anchor.TOP_LEFT.name(),
                List.of(Anchor.TOP_LEFT.name(), Anchor.TOP_RIGHT.name(),
                        Anchor.BOTTOM_LEFT.name(), Anchor.BOTTOM_RIGHT.name()));

        b.comment("Draw a drop shadow behind the text.").translation("config.simpleinfo.showShadow");
        SHOW_SHADOW = b.define("showShadow", true);
        b.comment("Hide the HUD while a chat/screen is open. The F3 debug overlay always hides the HUD.")
                .translation("config.simpleinfo.hideInChatScreen");
        HIDE_IN_CHAT_SCREEN = b.define("hideInChatScreen", true);

        b.comment("Memory display unit: G or M.").translation("config.simpleinfo.memoryUnit");
        MEMORY_UNIT = b.defineInList("memoryUnit", "G", List.of("G", "M"));
        b.comment("Append the used/max percentage in brackets after the memory values.")
                .translation("config.simpleinfo.showMemoryPercent");
        SHOW_MEMORY_PERCENT = b.define("showMemoryPercent", true);
        b.comment("HUD display order of the info items (list of item keys).")
                .translation("config.simpleinfo.order");
        ORDER = b.defineList("order", defaultOrderKeys(), o -> o instanceof String);
        b.pop();

        // ================= 每项 =================
        for (InfoItem item : InfoItem.values()) {
            b.comment(item.name() + " settings").push(item.key());

            // 颜色只存 24 位 RGB；渲染时统一补 alpha（0xFFFFFF 不带 alpha 会完全透明）
            // 注意：defineInRange(String,int,int,int) 返回的是 IntValue，不是 IntegerValue
            Map<InfoItem.Part, ForgeConfigSpec.IntValue> colors = new EnumMap<>(InfoItem.Part.class);
            Map<InfoItem.Part, ForgeConfigSpec.BooleanValue> autos = new EnumMap<>(InfoItem.Part.class);

            // 颜色：数值拆成「正常 / 较差 / 极差」三个等级各一个，其余部件各一个
            for (InfoItem.Part part : item.parts()) {
                colors.put(part, b.defineInRange("color." + part.keySuffix(),
                        item.defaultColor(part), 0x000000, 0xFFFFFF));
            }

            // 自动：三个数值等级<b>共享同一个</b> BooleanValue 句柄 ——
            // 点任意一个等级上的「自动」即等于切换整个数值的健康度自动变色。
            if (item.supportsAuto(InfoItem.Part.VALUE_GOOD)) {
                ForgeConfigSpec.BooleanValue valueAuto =
                        b.define("auto.value", item.defaultAuto(InfoItem.Part.VALUE_GOOD));
                autos.put(InfoItem.Part.VALUE_GOOD, valueAuto);
                autos.put(InfoItem.Part.VALUE_WARN, valueAuto);
                autos.put(InfoItem.Part.VALUE_BAD, valueAuto);
            }
            if (item.supportsAuto(InfoItem.Part.PERCENT)) {
                autos.put(InfoItem.Part.PERCENT,
                        b.define("auto.percent", item.defaultAuto(InfoItem.Part.PERCENT)));
            }

            ForgeConfigSpec.IntValue row = b.defineInRange("row", item.defaultRow(), 0, InfoItem.ROW_COUNT - 1);
            ForgeConfigSpec.BooleanValue enabled = b.define("enabled", true);

            b.pop();

            ITEM_COLOR.put(item, colors);
            ITEM_AUTO.put(item, autos);
            ITEM_ROW.put(item, row);
            ITEM_ENABLED.put(item, enabled);
        }

        SPEC = b.build();
    }

    private SimpleInfoConfig() {
    }

    // ================= 便捷读取 =================

    public static boolean hudEnabled() {
        return ENABLED.get();
    }

    public static double fontScale() {
        return FONT_SCALE.get();
    }

    public static int refreshTicks() {
        return Math.max(1, REFRESH_TICKS.get());
    }

    public static int marginX() {
        return MARGIN_X.get();
    }

    public static int marginY() {
        return MARGIN_Y.get();
    }

    public static Anchor anchor() {
        return Anchor.parse(ANCHOR.get());
    }

    public static void setAnchor(Anchor a) {
        ANCHOR.set(a.name());
    }

    public static boolean showShadow() {
        return SHOW_SHADOW.get();
    }

    public static boolean hideInChatScreen() {
        return HIDE_IN_CHAT_SCREEN.get();
    }

    /** "G" 或 "M"。 */
    public static String memoryUnit() {
        return MEMORY_UNIT.get();
    }

    public static void setMemoryUnit(String unit) {
        MEMORY_UNIT.set("M".equalsIgnoreCase(unit) ? "M" : "G");
    }

    /** 内存值后面是否附加 {@code (百分比%)}。 */
    public static boolean showMemoryPercent() {
        return SHOW_MEMORY_PERCENT.get();
    }

    public static void setShowMemoryPercent(boolean value) {
        SHOW_MEMORY_PERCENT.set(value);
    }

    public static boolean isEnabled(InfoItem item) {
        return ITEM_ENABLED.get(item).get();
    }

    public static void setEnabled(InfoItem item, boolean value) {
        ITEM_ENABLED.get(item).set(value);
    }

    public static int rowOf(InfoItem item) {
        return ITEM_ROW.get(item).get();
    }

    public static void setRow(InfoItem item, int row) {
        int max = InfoItem.ROW_COUNT - 1;
        ITEM_ROW.get(item).set(row < 0 ? 0 : (row > max ? max : row));
    }

    // ================= 显示顺序 =================

    /** 默认显示顺序 = 枚举声明顺序（改枚举即改默认，单一真源）。 */
    private static List<String> defaultOrderKeys() {
        List<String> keys = new ArrayList<>(InfoItem.values().length);
        for (InfoItem item : InfoItem.values()) {
            keys.add(item.key());
        }
        return keys;
    }

    /**
     * 当前 HUD 显示顺序。以配置里的 key 列表为准，
     * 非法 key 忽略、枚举里新增却没写进配置的项自动补到末尾 —— 保证永不丢项。
     */
    public static List<InfoItem> orderedItems() {
        List<InfoItem> result = new ArrayList<>(InfoItem.values().length);
        for (String key : ORDER.get()) {
            InfoItem item = InfoItem.byKey(key);
            if (item != null && !result.contains(item)) {
                result.add(item);
            }
        }
        for (InfoItem item : InfoItem.values()) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    private static void setOrder(List<InfoItem> items) {
        List<String> keys = new ArrayList<>(items.size());
        for (InfoItem item : items) {
            keys.add(item.key());
        }
        ORDER.set(keys);
    }

    /** 在 {@code dir}（-1 上 / +1 下）方向，是否还有「同一行」的邻居可以交换。 */
    public static boolean canMoveWithinRow(InfoItem item, int dir) {
        return neighborIndex(orderedItems(), item, dir) >= 0;
    }

    /** 与同一行内、{@code dir} 方向上最近的邻居交换位置；跨行原地不动。 */
    public static void moveWithinRow(InfoItem item, int dir) {
        List<InfoItem> order = new ArrayList<>(orderedItems());
        int i = order.indexOf(item);
        int j = neighborIndex(order, item, dir);
        if (i < 0 || j < 0) {
            return;
        }
        Collections.swap(order, i, j);
        setOrder(order);
    }

    /** 在 order 里沿 dir 找第一个「与 item 同行」的下标；找不到返回 -1。 */
    private static int neighborIndex(List<InfoItem> order, InfoItem item, int dir) {
        int i = order.indexOf(item);
        if (i < 0) {
            return -1;
        }
        int row = rowOf(item);
        for (int j = i + dir; j >= 0 && j < order.size(); j += dir) {
            if (rowOf(order.get(j)) == row) {
                return j;
            }
        }
        return -1;
    }

    /** 返回 24 位 RGB（不含 alpha）。 */
    public static int rgbOf(InfoItem item, InfoItem.Part part) {
        return ITEM_COLOR.get(item).get(part).get() & 0xFFFFFF;
    }

    public static void setRgb(InfoItem item, InfoItem.Part part, int rgb) {
        ITEM_COLOR.get(item).get(part).set(rgb & 0xFFFFFF);
    }

    /** 该部件是否走「健康度自动变色」。没有自动开关的部件恒为 {@code false}。 */
    public static boolean autoOf(InfoItem item, InfoItem.Part part) {
        ForgeConfigSpec.BooleanValue v = ITEM_AUTO.get(item).get(part);
        return v != null && v.get();
    }

    public static void setAuto(InfoItem item, InfoItem.Part part, boolean auto) {
        ForgeConfigSpec.BooleanValue v = ITEM_AUTO.get(item).get(part);
        if (v != null) {
            v.set(auto);
        }
    }

    /**
     * 固定色（手动取色）转成可直接传给 {@code drawString} 的颜色，<b>已补上 alpha 位</b>。
     * <p>
     * 1.20.1 取 {@code (color >> 24) & 0xFF} 作为 alpha 参与混合，
     * 所以 {@code 0xFFFFFF} 画出来是完全透明的，必须 OR 上 {@code 0xFF000000}。
     */
    public static int drawColorOf(InfoItem item, InfoItem.Part part) {
        return rgbOf(item, part) | 0xFF000000;
    }

    /** 立即写盘。每次配置改动后都要调，否则只改了内存值。 */
    public static void save() {
        if (SPEC.isLoaded()) {
            SPEC.save();
        }
    }

    // ================= 恢复默认值 =================
    //
    // 全部走 ConfigValue.getDefault()，这样默认值只有 define 那一处是真源，
    // 不会出现「改了 define 的默认值却忘了改重置逻辑」的漂移。

    /** 把所有配置恢复为默认值。 */
    public static void resetAll() {
        ENABLED.set(ENABLED.getDefault());
        FONT_SCALE.set(FONT_SCALE.getDefault());
        REFRESH_TICKS.set(REFRESH_TICKS.getDefault());
        MARGIN_X.set(MARGIN_X.getDefault());
        MARGIN_Y.set(MARGIN_Y.getDefault());
        ANCHOR.set(ANCHOR.getDefault());
        SHOW_SHADOW.set(SHOW_SHADOW.getDefault());
        HIDE_IN_CHAT_SCREEN.set(HIDE_IN_CHAT_SCREEN.getDefault());
        MEMORY_UNIT.set(MEMORY_UNIT.getDefault());
        SHOW_MEMORY_PERCENT.set(SHOW_MEMORY_PERCENT.getDefault());
        resetOrder();
        for (InfoItem item : InfoItem.values()) {
            resetItem(item);
        }
        save();
    }

    /** 把某一项（开关 / 行号 / 全部件颜色 / 自动标记）恢复默认。 */
    public static void resetItem(InfoItem item) {
        resetEnabled(item);
        resetRow(item);
        for (InfoItem.Part part : item.parts()) {
            resetColor(item, part);
            resetAuto(item, part);
        }
    }

    public static void resetEnabled(InfoItem item) {
        ITEM_ENABLED.get(item).set(ITEM_ENABLED.get(item).getDefault());
    }

    public static void resetRow(InfoItem item) {
        ITEM_ROW.get(item).set(ITEM_ROW.get(item).getDefault());
    }

    /** 显示顺序恢复默认（= 枚举顺序）。 */
    public static void resetOrder() {
        ORDER.set(ORDER.getDefault());
    }

    public static void resetColor(InfoItem item, InfoItem.Part part) {
        ForgeConfigSpec.IntValue v = ITEM_COLOR.get(item).get(part);
        if (v != null) {
            v.set(v.getDefault());
        }
    }

    public static void resetAuto(InfoItem item, InfoItem.Part part) {
        ForgeConfigSpec.BooleanValue v = ITEM_AUTO.get(item).get(part);
        if (v != null) {
            v.set(v.getDefault());
        }
    }
}
