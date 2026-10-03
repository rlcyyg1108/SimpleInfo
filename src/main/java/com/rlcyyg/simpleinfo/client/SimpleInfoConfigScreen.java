package com.rlcyyg.simpleinfo.client;

import com.rlcyyg.simpleinfo.Anchor;
import com.rlcyyg.simpleinfo.InfoItem;
import com.rlcyyg.simpleinfo.SimpleInfoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 配置界面 —— 模组的<b>唯一</b>交互入口。
 * <p>
 * 整体是一个 <b>2×2 方块阵</b>，两列等宽、<b>水平与垂直都居中</b>（<b>无标题</b>）：
 * <pre>
 * [ 上·左 ] 6 个全局设置（名称 | 选项）  [ 上·右 ] 6 个信息项
 *   总开关      | 开                  [开] FPS / TPS / MSPT /
 *   文字阴影    | 开                  内存占用 / 系统时间 / 游戏时间
 *   聊天时不显示 | 关                  每行末尾 [▲][▼] 调同行内的先后
 *   预设位置    | 左上
 *   内存单位    | G
 *   内存百分比  | 开
 * [ 下·左 ] 数值 2×2 + 颜色区        [ 下·右 ] 预览（与左块等大）
 *   字体 | 边距X                        ┌──────────────┐
 *   刷新 | 边距Y                        │  实时预览      │
 *   [项·部件] / [hex][自动]             │              │
 *   ■■■■■■■■  右键选项可重置该项        │              │
 *   ■■■■■■■■  右键输入框可清空内容      └──────────────┘
 * [        重置全部（红）        ] [          完成          ]
 * </pre>
 * <ul>
 *   <li><b>提示语两行</b>并排在色板右侧、在左块「右半边」居中，不单独占一行高度。</li>
 *   <li><b>同行排序</b>：每个信息项行末尾的 {@code ▲/▼} 与「同一行内」的上/下邻居交换位置
 *       （配置项 {@code order} 列表）；跨行不动，行号仍由 {@code [第N行]} 循环按钮负责。
 *       列表本身保持固定的枚举顺序，排序结果实时反映在右上方的预览里。</li>
 * </ul>
 * <ul>
 *   <li><b>下排两块等大</b>：左块高度由内容（数值网格 + 颜色区 + 提示语）决定，
 *       预览与它同宽同高、同顶边，所以两个矩形的底边严格对齐。</li>
 *   <li><b>右键 = 重置该项</b>：可交互控件在 {@link #resetZones} 里登记了恢复默认的动作；
 *       但数字输入框除外 —— 它的右键是「清空内容」，重置由行尾的红色 {@code ↺} 负责。</li>
 *   <li><b>输入框失焦还原</b>：数字框被清空后若没输入就点别处，由 {@link #restoreNumberBox} 还原。</li>
 *   <li>没有拖拽：指针锁定与鼠标坐标更新天然冲突，位置靠「预设位置 + 边距XY」控制。</li>
 * </ul>
 */
public class SimpleInfoConfigScreen extends Screen {

    private static final int MARGIN = 8;
    /** 下带底边 → 底部按钮之间的空隙。 */
    private static final int BUTTON_GAP = 10;
    private static final int BTN_H = 16;
    private static final int ROW_GAP = 2;
    /** 上带与下带之间的空隙。 */
    private static final int BAND_GAP = 3;

    // 数值输入： [标签][-][输入框][+][红色重置 ↺]
    private static final int NUM_LABEL_W = 28;
    private static final int NUM_BTN_W = 12;
    private static final int NUM_BOX_W = 28;
    private static final int NUM_RESET_W = 12;
    private static final int NUM_FIELD_W = NUM_LABEL_W + NUM_BTN_W + 2 + NUM_BOX_W + 2 + NUM_BTN_W + 4 + NUM_RESET_W;
    /** 2×2 网格里，左右两个子列之间的间距。 */
    private static final int NUM_COL_GAP = 8;

    // 两列等宽：左列 = 6 个设置 + 数值/颜色；右列 = 6 个信息项 + 预览
    private static final int COL_W = 2 * NUM_FIELD_W + NUM_COL_GAP;    // 下排左块宽度 = 数值网格宽度
    private static final int COL_GAP = 12;

    // 上带·左：每行「左半写名称、右半写选项」两段式。
    // 两半等宽，且分别与下方 2×2 数值网格的两个子列逐列对齐 → 上下两块共享同一条竖向分栏线。
    private static final int OPT_GAP = NUM_COL_GAP;
    private static final int OPT_W = NUM_FIELD_W;
    private static final int OPT_NAME_W = COL_W - OPT_W - OPT_GAP;

    // 信息项行：[开关][名称][第N行][▲][▼]
    private static final int TOGGLE_W = 28;
    private static final int ROW_BTN_W = 52;
    /** 同行排序箭头（▲/▼）的宽度与它们之间的间距。 */
    private static final int MOV_W = 12;
    private static final int MOV_GAP = 2;
    private static final int INNER_GAP = 3;

    // 调色板：Java 版标准 16 色，两排，<b>鲜艳的在上</b>
    private static final int SWATCH = 11;
    private static final int SWATCH_PITCH = 13;
    private static final int SWATCH_PER_ROW = 8;
    private static final int[] PALETTE = {
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA
    };

    /** 一个数值控件的几何。 */
    private record NumField(int labelX, int minusX, int boxX, int plusX, int y, String labelKey) {
    }

    /** 一块「右键即重置」的命中区域。 */
    private record ResetZone(int x, int y, int w, int h, Runnable action) {
    }

    private final Screen parent;

    /** 当前正在编辑哪一项。 */
    private InfoItem selected = InfoItem.FPS;
    /** 当前正在编辑该项的哪个部件。 */
    private InfoItem.Part selectedPart = InfoItem.Part.NAME;

    // 需要在改动后同步文案的控件
    private final Map<InfoItem, Button> itemToggles = new EnumMap<>(InfoItem.class);
    private final Map<InfoItem, Button> itemNameButtons = new EnumMap<>(InfoItem.class);
    private final Map<InfoItem, Button> itemRowButtons = new EnumMap<>(InfoItem.class);
    private final Map<InfoItem, Button> itemUpButtons = new EnumMap<>(InfoItem.class);
    private final Map<InfoItem, Button> itemDownButtons = new EnumMap<>(InfoItem.class);
    private final Map<String, NumField> numFields = new LinkedHashMap<>();
    private final Map<String, EditBox> numBoxes = new LinkedHashMap<>();
    private final List<ResetZone> resetZones = new ArrayList<>();

    private Button globalButton;
    private Button shadowButton;
    private Button chatButton;
    private Button anchorButton;
    private Button memoryUnitButton;
    private Button memoryPercentButton;
    private Button partButton;
    private Button autoButton;
    private EditBox colorBox;

    /** 防止 setValue 触发 responder 造成回环。 */
    private boolean updatingFields = false;

    // 调色板几何 / 提示语（两行，在左块右半边居中）
    private int swatchX, swatchY, hintY, hintCenterX;

    // 预览矩形
    private int previewX, previewY, previewW, previewH;

    public SimpleInfoConfigScreen(Screen parent) {
        super(Component.translatable("gui.simpleinfo.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        itemToggles.clear();
        itemNameButtons.clear();
        itemRowButtons.clear();
        itemUpButtons.clear();
        itemDownButtons.clear();
        numFields.clear();
        numBoxes.clear();
        resetZones.clear();

        // ===== 整体居中：两列等宽（左列 = 开关 + 数值/颜色；右列 = 信息项 + 预览）=====
        int contentW = COL_W * 2 + COL_GAP;
        int originX = Math.max(MARGIN, (width - contentW) / 2);
        int colLeftX = originX;
        int colRightX = originX + COL_W + COL_GAP;
        int nameW = Math.max(30, COL_W - TOGGLE_W - ROW_BTN_W - 2 * MOV_W - MOV_GAP - 3 * INNER_GAP);

        // ===== 垂直：先算出「上带 → 底部按钮」的总高，再让整块垂直居中（无标题） =====
        int upperRows = Math.max(InfoItem.values().length, 6);
        int upperH = upperRows * (BTN_H + ROW_GAP) - ROW_GAP;
        // 下带 = 数值 2×2 + 空隙 + 部件按钮 + 色号行 + 色板两排
        // （提示语已并入色板右侧，不再单独占一行高度）
        int lowerH = 2 * BTN_H + ROW_GAP + BAND_GAP
                + BTN_H + ROW_GAP + BTN_H + ROW_GAP + 2 * SWATCH_PITCH;
        int totalH = upperH + BAND_GAP + lowerH + BUTTON_GAP + BTN_H;
        int top = Math.max(MARGIN, (height - totalH) / 2);

        int topY = top;
        int bandTop = topY + upperH + BAND_GAP;
        int bottomY = bandTop + lowerH + BUTTON_GAP;

        // ================= 上带 · 左：6 个全局设置（左半名称 / 右半选项） =================
        int leftY = topY;
        globalButton = addOptionRow("gui.simpleinfo.global", colLeftX, leftY,
                () -> {
                    SimpleInfoConfig.ENABLED.set(!SimpleInfoConfig.ENABLED.get());
                    commit();
                },
                () -> SimpleInfoConfig.ENABLED.set(SimpleInfoConfig.ENABLED.getDefault()));
        leftY += BTN_H + ROW_GAP;

        shadowButton = addOptionRow("gui.simpleinfo.showShadow", colLeftX, leftY,
                () -> {
                    SimpleInfoConfig.SHOW_SHADOW.set(!SimpleInfoConfig.SHOW_SHADOW.get());
                    commit();
                },
                () -> SimpleInfoConfig.SHOW_SHADOW.set(SimpleInfoConfig.SHOW_SHADOW.getDefault()));
        leftY += BTN_H + ROW_GAP;

        chatButton = addOptionRow("gui.simpleinfo.hideInChatScreen", colLeftX, leftY,
                () -> {
                    SimpleInfoConfig.HIDE_IN_CHAT_SCREEN.set(!SimpleInfoConfig.HIDE_IN_CHAT_SCREEN.get());
                    commit();
                },
                () -> SimpleInfoConfig.HIDE_IN_CHAT_SCREEN.set(SimpleInfoConfig.HIDE_IN_CHAT_SCREEN.getDefault()));
        leftY += BTN_H + ROW_GAP;

        anchorButton = addOptionRow("gui.simpleinfo.anchor", colLeftX, leftY,
                () -> {
                    SimpleInfoConfig.setAnchor(nextAnchor());
                    commit();
                },
                () -> SimpleInfoConfig.ANCHOR.set(SimpleInfoConfig.ANCHOR.getDefault()));
        leftY += BTN_H + ROW_GAP;

        memoryUnitButton = addOptionRow("gui.simpleinfo.memoryUnit", colLeftX, leftY,
                () -> {
                    SimpleInfoConfig.setMemoryUnit("G".equals(SimpleInfoConfig.memoryUnit()) ? "M" : "G");
                    commit();
                },
                () -> SimpleInfoConfig.MEMORY_UNIT.set(SimpleInfoConfig.MEMORY_UNIT.getDefault()));
        leftY += BTN_H + ROW_GAP;

        memoryPercentButton = addOptionRow("gui.simpleinfo.showMemoryPercent", colLeftX, leftY,
                () -> {
                    SimpleInfoConfig.setShowMemoryPercent(!SimpleInfoConfig.showMemoryPercent());
                    commit();
                },
                () -> SimpleInfoConfig.SHOW_MEMORY_PERCENT.set(SimpleInfoConfig.SHOW_MEMORY_PERCENT.getDefault()));

        // ================= 上带 · 右：6 个信息项（1 列 6 行） =================
        int itemY = topY;
        for (InfoItem item : InfoItem.values()) {
            addItemRow(item, colRightX, itemY, nameW);
            itemY += BTN_H + ROW_GAP;
        }

        // ===== 下带：左块（数值 2×2 + 颜色）与右块（预览）同宽同高、顶边对齐 =====
        // 左块 · 2×2 数值网格（左子列 字体/刷新，右子列 边距X/边距Y，两子列等宽）
        int col2X = colLeftX + NUM_FIELD_W + NUM_COL_GAP;
        addNumberField("font", "gui.simpleinfo.fontScale", colLeftX, bandTop);
        addNumberField("marginX", "gui.simpleinfo.marginX", col2X, bandTop);
        addNumberField("refresh", "gui.simpleinfo.refreshTicks", colLeftX, bandTop + BTN_H + ROW_GAP);
        addNumberField("marginY", "gui.simpleinfo.marginY", col2X, bandTop + BTN_H + ROW_GAP);

        // 左块 · 颜色区（部件按钮 → 色号+自动 → 16 色两排；提示语并排在色板右侧）
        int colorY = bandTop + 2 * BTN_H + ROW_GAP + BAND_GAP;
        partButton = addRenderableWidget(Button
                .builder(Component.empty(), b -> cyclePart())
                .bounds(colLeftX, colorY, COL_W, BTN_H).build());
        zone(partButton, () -> {
            SimpleInfoConfig.resetColor(selected, selectedPart);
            SimpleInfoConfig.resetAuto(selected, selectedPart);
        });

        int hexY = colorY + BTN_H + ROW_GAP;
        colorBox = new EditBox(font, colLeftX, hexY + (BTN_H - 16) / 2, 50, 16,
                Component.translatable("gui.simpleinfo.color"));
        colorBox.setMaxLength(6);
        colorBox.setFilter(s -> s.matches("[0-9a-fA-F]{0,6}"));
        colorBox.setResponder(this::onColorTyped);
        addRenderableWidget(colorBox);
        zone(colorBox, () -> SimpleInfoConfig.resetColor(selected, selectedPart));

        autoButton = addRenderableWidget(Button
                .builder(Component.empty(), b -> {
                    SimpleInfoConfig.setAuto(selected, selectedPart,
                            !SimpleInfoConfig.autoOf(selected, selectedPart));
                    commit();
                })
                .bounds(colLeftX + 54, hexY, COL_W - 54, BTN_H).build());
        zone(autoButton, () -> SimpleInfoConfig.resetAuto(selected, selectedPart));

        swatchX = colLeftX;
        swatchY = hexY + BTN_H + ROW_GAP;
        // 提示语拆成两行，并排在色板右侧、在左块「右半边」水平居中；
        // 两行分别与两排色板竖直对齐，正好把这块空地填满。
        hintCenterX = colLeftX + COL_W * 3 / 4;
        hintY = swatchY + (SWATCH - font.lineHeight) / 2;

        // ================= 下带 · 右：预览（与左块等宽等高、四边对齐） =================
        previewX = colRightX;
        previewW = COL_W;
        previewY = bandTop;
        previewH = lowerH;

        // ================= 底部按钮：横跨整个内容块 =================
        int bottomW = Math.max(60, (contentW - 8) / 2);
        addRenderableWidget(Button
                .builder(Component.translatable("gui.simpleinfo.resetAll").withStyle(ChatFormatting.RED),
                        b -> confirmResetAll())
                .bounds(originX, bottomY, bottomW, BTN_H).build());
        addRenderableWidget(Button
                .builder(Component.translatable("gui.simpleinfo.done"), b -> onClose())
                .bounds(originX + bottomW + 8, bottomY, bottomW, BTN_H).build());

        refreshEditors();
    }

    /** 登记一块「右键即恢复默认」的区域。 */
    private void zone(AbstractWidget w, Runnable action) {
        resetZones.add(new ResetZone(w.getX(), w.getY(), w.getWidth(), w.getHeight(), action));
    }

    /**
     * 上带·左的一行：<b>左半只写名称、右半只写选项内容</b>（原来一个按钮里塞「名称 值」太挤）。
     * 两半等宽，且分别与下方 2×2 数值网格的两个子列对齐。
     * 点名称或点选项执行同一个动作；右键任一都恢复该项默认值。
     *
     * @return 右侧的「选项内容」按钮（名称按钮的内容固定，无需持有引用）
     */
    private Button addOptionRow(String labelKey, int x, int y, Runnable action, Runnable reset) {
        Button name = addRenderableWidget(Button
                .builder(Component.translatable(labelKey), b -> action.run())
                .bounds(x, y, OPT_NAME_W, BTN_H).build());
        zone(name, reset);

        Button value = addRenderableWidget(Button
                .builder(Component.empty(), b -> action.run())
                .bounds(x + OPT_NAME_W + OPT_GAP, y, OPT_W, BTN_H).build());
        zone(value, reset);
        return value;
    }

    private void addItemRow(InfoItem item, int x, int y, int nameW) {
        Button toggle = addRenderableWidget(Button
                .builder(Component.empty(), b -> {
                    SimpleInfoConfig.setEnabled(item, !SimpleInfoConfig.isEnabled(item));
                    commit();
                })
                .bounds(x, y, TOGGLE_W, BTN_H).build());
        itemToggles.put(item, toggle);
        zone(toggle, () -> SimpleInfoConfig.resetEnabled(item));

        Button name = addRenderableWidget(Button
                .builder(Component.translatable(item.labelKey()), b -> {
                    selected = item;
                    if (!hasPart(item, selectedPart)) {
                        selectedPart = item.parts()[0];
                    }
                    commit();
                })
                .bounds(x + TOGGLE_W + INNER_GAP, y, nameW, BTN_H).build());
        itemNameButtons.put(item, name);
        zone(name, () -> SimpleInfoConfig.resetItem(item));

        Button rowBtn = addRenderableWidget(Button
                .builder(Component.empty(), b -> {
                    SimpleInfoConfig.setRow(item, (SimpleInfoConfig.rowOf(item) + 1) % InfoItem.ROW_COUNT);
                    commit();
                })
                .bounds(x + TOGGLE_W + INNER_GAP + nameW + INNER_GAP, y, ROW_BTN_W, BTN_H).build());
        itemRowButtons.put(item, rowBtn);
        zone(rowBtn, () -> SimpleInfoConfig.resetRow(item));

        // 同一行内的排序：▲ 与同行的上一个交换，▼ 与下一个交换（跨行不动）
        int upX = x + TOGGLE_W + INNER_GAP + nameW + INNER_GAP + ROW_BTN_W + INNER_GAP;
        Button up = addRenderableWidget(Button
                .builder(Component.literal("\u25B2"), b -> {
                    SimpleInfoConfig.moveWithinRow(item, -1);
                    commit();
                })
                .bounds(upX, y, MOV_W, BTN_H).build());
        itemUpButtons.put(item, up);

        Button down = addRenderableWidget(Button
                .builder(Component.literal("\u25BC"), b -> {
                    SimpleInfoConfig.moveWithinRow(item, 1);
                    commit();
                })
                .bounds(upX + MOV_W + MOV_GAP, y, MOV_W, BTN_H).build());
        itemDownButtons.put(item, down);
    }

    private void addNumberField(String key, String labelKey, int x, int y) {
        int minusX = x + NUM_LABEL_W;
        int boxX = minusX + NUM_BTN_W + 2;
        int plusX = boxX + NUM_BOX_W + 2;
        int resetX = plusX + NUM_BTN_W + 4;

        Button minus = addRenderableWidget(Button.builder(Component.literal("-"), b -> stepNumber(key, -1))
                .bounds(minusX, y, NUM_BTN_W, BTN_H).build());
        Button plus = addRenderableWidget(Button.builder(Component.literal("+"), b -> stepNumber(key, 1))
                .bounds(plusX, y, NUM_BTN_W, BTN_H).build());

        EditBox box = new EditBox(font, boxX, y + (BTN_H - 16) / 2 + 1, NUM_BOX_W, 16, Component.empty());
        box.setMaxLength(8);
        box.setFilter(s -> s.matches("[0-9.]{0,8}"));
        box.setResponder(s -> onNumberTyped(key, s));
        addRenderableWidget(box);
        numBoxes.put(key, box);
        numFields.put(key, new NumField(x, minusX, boxX, plusX, y, labelKey));

        // 红色 ↺：把该数值恢复为默认值
        addRenderableWidget(Button
                .builder(Component.literal("↺").withStyle(ChatFormatting.RED), b -> {
                    numberReset(key);
                    commit();
                })
                .bounds(resetX, y, NUM_RESET_W, BTN_H).build());

        // 右键重置：标签区 / -/+ 区；输入框本身不登记（右键 = 清空内容）
        zone(minus, () -> numberReset(key));
        zone(plus, () -> numberReset(key));
        resetZones.add(new ResetZone(x, y, NUM_LABEL_W, BTN_H, () -> numberReset(key)));
    }

    private static boolean hasPart(InfoItem item, InfoItem.Part part) {
        for (InfoItem.Part p : item.parts()) {
            if (p == part) {
                return true;
            }
        }
        return false;
    }

    private static Anchor nextAnchor() {
        Anchor[] all = Anchor.values();
        return all[(SimpleInfoConfig.anchor().ordinal() + 1) % all.length];
    }

    // ================= 数值读写 =================

    private static double numberGet(String key) {
        switch (key) {
            case "font":
                return SimpleInfoConfig.FONT_SCALE.get();
            case "refresh":
                return SimpleInfoConfig.REFRESH_TICKS.get();
            case "marginX":
                return SimpleInfoConfig.MARGIN_X.get();
            case "marginY":
                return SimpleInfoConfig.MARGIN_Y.get();
            default:
                return 0.0D;
        }
    }

    private static void numberSet(String key, double v) {
        switch (key) {
            case "font":
                SimpleInfoConfig.FONT_SCALE.set(clamp(v, 0.5D, 5.0D));
                break;
            case "refresh":
                SimpleInfoConfig.REFRESH_TICKS.set((int) clamp(Math.round(v), 1, 1200));
                break;
            case "marginX":
                SimpleInfoConfig.MARGIN_X.set((int) clamp(Math.round(v), 0, 512));
                break;
            case "marginY":
                SimpleInfoConfig.MARGIN_Y.set((int) clamp(Math.round(v), 0, 512));
                break;
            default:
                break;
        }
    }

    private static void numberReset(String key) {
        switch (key) {
            case "font":
                SimpleInfoConfig.FONT_SCALE.set(SimpleInfoConfig.FONT_SCALE.getDefault());
                break;
            case "refresh":
                SimpleInfoConfig.REFRESH_TICKS.set(SimpleInfoConfig.REFRESH_TICKS.getDefault());
                break;
            case "marginX":
                SimpleInfoConfig.MARGIN_X.set(SimpleInfoConfig.MARGIN_X.getDefault());
                break;
            case "marginY":
                SimpleInfoConfig.MARGIN_Y.set(SimpleInfoConfig.MARGIN_Y.getDefault());
                break;
            default:
                break;
        }
    }

    private static String numberText(String key) {
        if ("font".equals(key)) {
            return String.format(Locale.ROOT, "%.1f", numberGet(key));
        }
        return String.valueOf((int) Math.round(numberGet(key)));
    }

    private void stepNumber(String key, int dir) {
        double step = "font".equals(key) ? 0.1D : (hasShiftDown() ? 5 : 1);
        numberSet(key, numberGet(key) + dir * step);
        commit();
    }

    private void onNumberTyped(String key, String text) {
        if (updatingFields || text.isEmpty() || ".".equals(text)) {
            return;
        }
        try {
            numberSet(key, Double.parseDouble(text));
            SimpleInfoConfig.save();
        } catch (NumberFormatException ignored) {
            // setFilter 已限制字符集，这里只是兜底
        }
    }

    // ================= 颜色 =================

    private void cyclePart() {
        InfoItem.Part[] parts = selected.parts();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] == selectedPart) {
                selectedPart = parts[(i + 1) % parts.length];
                break;
            }
        }
        commit();
    }

    /**
     * 写入当前部件的颜色。
     * <p>
     * 「自动」= 用内置默认健康色。手动改色意味着「我要用自己的颜色」，
     * 所以会自动把该部件的「自动」关掉（改完立刻能看到效果）。
     */
    private void applyColor(int rgb) {
        SimpleInfoConfig.setRgb(selected, selectedPart, rgb);
        if (selected.supportsAuto(selectedPart)) {
            SimpleInfoConfig.setAuto(selected, selectedPart, false);
        }
        SimpleInfoConfig.save();
    }

    private void onColorTyped(String text) {
        if (updatingFields || text.length() != 6) {
            return;
        }
        try {
            applyColor(Integer.parseInt(text, 16));
        } catch (NumberFormatException ignored) {
            // 兜底
        }
    }

    private void commit() {
        SimpleInfoConfig.save();
        refreshEditors();
    }

    // ================= 重置 =================

    private void confirmResetAll() {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        SimpleInfoConfig.resetAll();
                    }
                    minecraft.setScreen(this);
                    refreshEditors();
                },
                Component.translatable("gui.simpleinfo.resetAll.title"),
                Component.translatable("gui.simpleinfo.resetAll.message"),
                Component.translatable("gui.simpleinfo.resetAll").withStyle(ChatFormatting.RED),
                Component.translatable("gui.simpleinfo.cancel")));
    }

    private void refreshEditors() {
        // 上带·左：名称固定画在左侧的「名称按钮」里（见 addOptionRow），这里只刷新右侧的选项内容
        globalButton.setMessage(onOff(SimpleInfoConfig.ENABLED.get()));
        shadowButton.setMessage(onOff(SimpleInfoConfig.SHOW_SHADOW.get()));
        chatButton.setMessage(onOff(SimpleInfoConfig.HIDE_IN_CHAT_SCREEN.get()));
        anchorButton.setMessage(Component.translatable(SimpleInfoConfig.anchor().translationKey()));
        memoryUnitButton.setMessage(Component.literal(SimpleInfoConfig.memoryUnit()));
        memoryPercentButton.setMessage(onOff(SimpleInfoConfig.showMemoryPercent()));

        for (InfoItem item : InfoItem.values()) {
            itemToggles.get(item).setMessage(onOff(SimpleInfoConfig.isEnabled(item)));
            itemRowButtons.get(item).setMessage(Component.translatable("gui.simpleinfo.row." + SimpleInfoConfig.rowOf(item)));
            // 同方向没有同行的邻居时置灰，明确「到头了」
            itemUpButtons.get(item).active = SimpleInfoConfig.canMoveWithinRow(item, -1);
            itemDownButtons.get(item).active = SimpleInfoConfig.canMoveWithinRow(item, 1);
        }

        partButton.setMessage(labeled(selected.labelKey(), Component.translatable(partKey(selectedPart))));

        boolean auto = SimpleInfoConfig.autoOf(selected, selectedPart);
        autoButton.setMessage(labeled("gui.simpleinfo.auto", onOff(auto)));
        autoButton.active = selected.supportsAuto(selectedPart);

        updatingFields = true;
        for (Map.Entry<String, EditBox> e : numBoxes.entrySet()) {
            e.getValue().setValue(numberText(e.getKey()));
        }
        if (colorBox != null) {
            colorBox.setValue(String.format(Locale.ROOT, "%06X", SimpleInfoConfig.rgbOf(selected, selectedPart)));
        }
        updatingFields = false;
    }

    private static String partKey(InfoItem.Part part) {
        return "gui.simpleinfo.part." + part.keySuffix();
    }

    /** 标签 + 一个空格 + 值。<b>不要冒号</b>。 */
    private static Component labeled(String labelKey, Component value) {
        return Component.translatable(labelKey).copy().append(" ").append(value);
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "gui.simpleinfo.on" : "gui.simpleinfo.off");
    }

    // ================= 渲染 =================

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg);

        super.render(gg, mouseX, mouseY, partialTick);

        drawNumberLabels(gg);
        drawSelectionHighlight(gg);
        drawSwatches(gg);
        drawHint(gg);
        renderPreview(gg);
    }

    private void drawNumberLabels(GuiGraphics gg) {
        int textDy = (BTN_H - 8) / 2 + 1;
        for (NumField f : numFields.values()) {
            String text = Component.translatable(f.labelKey()).getString();
            int lx = f.minusX() - 3 - font.width(text);
            if (lx >= f.labelX() - 6) {
                gg.drawString(font, text, lx, f.y() + textDy, 0xFFAAAAAA, false);
            }
        }
    }

    private void drawSelectionHighlight(GuiGraphics gg) {
        Button b = itemNameButtons.get(selected);
        if (b == null) {
            return;
        }
        int c = 0xFFFFAA00;
        int x0 = b.getX() - 1;
        int y0 = b.getY() - 1;
        int x1 = b.getX() + b.getWidth() + 1;
        int y1 = b.getY() + b.getHeight() + 1;
        gg.fill(x0, y0, x1, y0 + 1, c);
        gg.fill(x0, y1 - 1, x1, y1, c);
        gg.fill(x0, y0, x0 + 1, y1, c);
        gg.fill(x1 - 1, y0, x1, y1, c);
    }

    private void drawSwatches(GuiGraphics gg) {
        for (int i = 0; i < PALETTE.length; i++) {
            int cx = swatchX + (i % SWATCH_PER_ROW) * SWATCH_PITCH;
            int cy = swatchY + (i / SWATCH_PER_ROW) * SWATCH_PITCH;
            boolean current = PALETTE[i] == SimpleInfoConfig.rgbOf(selected, selectedPart);
            gg.fill(cx - 1, cy - 1, cx + SWATCH + 1, cy + SWATCH + 1, current ? 0xFFFFFFFF : 0xFF707070);
            gg.fill(cx, cy, cx + SWATCH, cy + SWATCH, PALETTE[i] | 0xFF000000);
        }
    }

    /** 提示语：两行，在左块右半边水平居中，分别与两排色板等高。 */
    private void drawHint(GuiGraphics gg) {
        drawCenteredHint(gg, "gui.simpleinfo.hint.1", hintY);
        drawCenteredHint(gg, "gui.simpleinfo.hint.2", hintY + SWATCH_PITCH);
    }

    private void drawCenteredHint(GuiGraphics gg, String key, int y) {
        Component c = Component.translatable(key);
        gg.drawString(font, c, hintCenterX - font.width(c) / 2, y, 0xFF808080, false);
    }

    private void renderPreview(GuiGraphics gg) {
        if (previewW <= 0 || previewH <= 0) {
            return;
        }
        gg.fill(previewX - 1, previewY - 1, previewX + previewW + 1, previewY + previewH + 1, 0xFF707070);
        gg.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0xFF1B2028);

        List<InfoBlock.Line> lines = InfoBlock.build(font);
        if (lines.isEmpty()) {
            return;
        }
        int bw = InfoBlock.width(lines);
        int bh = InfoBlock.height(font, lines);
        float ps = 1.5F;
        if (bw > 0 && bh > 0) {
            ps = Math.min(ps, Math.min((previewW - 10.0F) / bw, (previewH - 10.0F) / bh));
        }
        ps = Math.max(0.4F, ps);

        Anchor anchor = SimpleInfoConfig.anchor();
        int[] o = InfoBlock.origin(lines, font, anchor,
                SimpleInfoConfig.marginX(), SimpleInfoConfig.marginY(), ps, previewW, previewH);

        gg.enableScissor(previewX, previewY, previewX + previewW, previewY + previewH);
        InfoBlock.draw(gg, font, lines, SimpleInfoConfig.showShadow(),
                previewX + o[0], previewY + o[1], ps, anchor);
        gg.disableScissor();
    }

    // ================= 交互 =================

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 1) {
            // 右键数字输入框：清空其内容（不是重置）
            for (EditBox box : numBoxes.values()) {
                if (box.isMouseOver(mx, my)) {
                    updatingFields = true;
                    box.setValue("");
                    updatingFields = false;
                    setFocused(box);
                    return true;
                }
            }
            // 右键：把命中的那一项恢复为默认值
            for (ResetZone z : resetZones) {
                if (mx >= z.x() && mx < z.x() + z.w() && my >= z.y() && my < z.y() + z.h()) {
                    z.action().run();
                    commit();
                    return true;
                }
            }
            return true;    // 右键不透传给子控件
        }

        if (button == 0) {
            for (int i = 0; i < PALETTE.length; i++) {
                int cx = swatchX + (i % SWATCH_PER_ROW) * SWATCH_PITCH;
                int cy = swatchY + (i / SWATCH_PER_ROW) * SWATCH_PITCH;
                if (mx >= cx && mx < cx + SWATCH && my >= cy && my < cy + SWATCH) {
                    applyColor(PALETTE[i]);
                    commit();
                    return true;
                }
            }
        }

        boolean handled = super.mouseClicked(mx, my, button);

        // 点到任何输入框以外的区域 → 输入框失焦（会触发空内容还原、光标移出）
        if (button == 0 && getFocused() instanceof EditBox && !isOverAnyEditBox(mx, my)) {
            setFocused(null);
        }
        return handled;
    }

    /** 是否点在任意一个输入框上（数字框或色号框）。 */
    private boolean isOverAnyEditBox(double mx, double my) {
        for (EditBox box : numBoxes.values()) {
            if (box.isMouseOver(mx, my)) {
                return true;
            }
        }
        return colorBox != null && colorBox.isMouseOver(mx, my);
    }

    /** 数字框失焦时归一化内容：空串（右键清空后没输入）→ 还原为当前配置值。 */
    private void restoreNumberBox(EditBox box) {
        for (Map.Entry<String, EditBox> e : numBoxes.entrySet()) {
            if (e.getValue() == box) {
                updatingFields = true;
                box.setValue(numberText(e.getKey()));
                updatingFields = false;
                return;
            }
        }
    }

    @Override
    public void setFocused(GuiEventListener listener) {
        GuiEventListener previous = getFocused();
        super.setFocused(listener);
        if (previous != null && previous != listener && previous instanceof EditBox box) {
            restoreNumberBox(box);
        }
    }

    // ================= 生命周期 =================

    @Override
    public boolean isPauseScreen() {
        return false;   // 联机时打开设置不该暂停游戏
    }

    @Override
    public void onClose() {
        SimpleInfoConfig.save();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
