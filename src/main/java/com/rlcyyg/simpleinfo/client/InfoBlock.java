package com.rlcyyg.simpleinfo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rlcyyg.simpleinfo.Anchor;
import com.rlcyyg.simpleinfo.InfoItem;
import com.rlcyyg.simpleinfo.SimpleInfoConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 信息块：把各项拼成「名称 + 冒号 + 数值」的分段文本，按行分组、上色，
 * 并负责整块的绘制。<b>HUD 与配置界面预览共用这里</b>，保证所见即所得。
 * <p>
 * 布局为流式：同一行内的项依次左排，禁用项直接跳过，其余项自动左移补位，
 * 分隔符 {@code " | "} 只出现在「实际存在的相邻两项」之间。
 */
public final class InfoBlock {

    /** 一段带颜色的文本。{@code color} 已含 alpha 位。 */
    public record Segment(String text, int color) {
    }

    /** 一行：若干分段 + 该行的未缩放宽度。 */
    public record Line(List<Segment> segments, int width) {
    }

    /** 行与行之间的额外空隙（未缩放像素）。 */
    public static final int ROW_GAP = 1;

    private static final int WHITE = 0xFFFFFFFF;
    /** 「自动」开启时使用的内置默认健康色。 */
    private static final int DEFAULT_GOOD = 0xFF55FF55;
    private static final int DEFAULT_WARN = 0xFFFFFF55;
    private static final int DEFAULT_BAD = 0xFFFF5555;

    private InfoBlock() {
    }

    // ================= 构建 =================

    /** 按当前配置与采样值构建整块内容（未缩放）。 */
    public static List<Line> build(Font font) {
        List<Line> lines = new ArrayList<>();
        for (int row = 0; row < InfoItem.ROW_COUNT; row++) {
            List<Segment> segments = new ArrayList<>();
            boolean first = true;
            for (InfoItem item : SimpleInfoConfig.orderedItems()) {
                if (!SimpleInfoConfig.isEnabled(item) || SimpleInfoConfig.rowOf(item) != row) {
                    continue;
                }
                if (!first) {
                    segments.add(new Segment(" | ", WHITE));
                }
                first = false;
                segments.addAll(itemSegments(item));
            }
            if (segments.isEmpty()) {
                continue;   // 空行塌陷
            }
            int width = 0;
            for (Segment s : segments) {
                width += font.width(s.text());
            }
            lines.add(new Line(segments, width));
        }
        return lines;
    }

    private static List<Segment> itemSegments(InfoItem item) {
        List<Segment> out = new ArrayList<>();
        out.add(new Segment(Component.translatable(item.labelKey()).getString(), color(item, InfoItem.Part.NAME)));
        out.add(new Segment(": ", WHITE));

        if (item == InfoItem.MEMORY) {
            out.add(new Segment(InfoSampler.memoryUsedText(), color(item, InfoItem.Part.VALUE_GOOD)));
            out.add(new Segment(" / ", WHITE));
            out.add(new Segment(InfoSampler.memoryTotalText(), color(item, InfoItem.Part.TOTAL)));
            if (SimpleInfoConfig.showMemoryPercent()) {
                out.add(new Segment(" (", WHITE));
                out.add(new Segment(InfoSampler.memoryPercentText(), color(item, InfoItem.Part.PERCENT)));
                out.add(new Segment(")", WHITE));
            }
        } else {
            out.add(new Segment(valueText(item), color(item, InfoItem.Part.VALUE_GOOD)));
        }
        return out;
    }

    private static String valueText(InfoItem item) {
        switch (item) {
            case TPS:
                return InfoSampler.tickTimesAvailable() ? String.format(Locale.ROOT, "%.1f", InfoSampler.tps()) : "--";
            case MSPT:
                return InfoSampler.tickTimesAvailable() ? String.format(Locale.ROOT, "%.2f", InfoSampler.mspt()) : "--";
            case FPS:
                return String.format(Locale.ROOT, "%.0f", InfoSampler.fps());
            case TIME:
                return InfoSampler.timeText();
            case GAMETIME:
                return InfoSampler.gameTimeText();
            default:
                return "--";
        }
    }

    /**
     * 部件取色。
     * <ul>
     *   <li><b>自动开</b>：数值 / 内存百分比用<b>内置</b>默认健康色（绿 / 黄 / 红）。</li>
     *   <li><b>自动关</b>：改用自己的颜色 —— 数值按健康度取「正常 / 较差 / 极差」三个自定义色之一，
     *       百分比用它自己的那个固定色。</li>
     * </ul>
     * 名称、总分配等不支持自动的部件始终用自己的固定配置色。
     */
    private static int color(InfoItem item, InfoItem.Part part) {
        boolean healthDriven = part.isValueLevel() || part == InfoItem.Part.PERCENT;
        if (!healthDriven || !item.supportsAuto(part)) {
            return SimpleInfoConfig.drawColorOf(item, part);
        }
        Level level = autoLevel(item, part);
        if (level == null) {
            return WHITE;   // 数据暂不可用（如 tickTimes 尚未采样）
        }
        if (SimpleInfoConfig.autoOf(item, part)) {
            return defaultHealthColor(level);
        }
        return part.isValueLevel()
                ? SimpleInfoConfig.drawColorOf(item, partOf(level))
                : SimpleInfoConfig.drawColorOf(item, part);
    }

    /** 内置默认健康色。 */
    private static int defaultHealthColor(Level level) {
        switch (level) {
            case WARN:
                return DEFAULT_WARN;
            case BAD:
                return DEFAULT_BAD;
            default:
                return DEFAULT_GOOD;
        }
    }

    /** 健康度等级。 */
    private enum Level {
        GOOD, WARN, BAD
    }

    /** 该部件当前应对应的健康度等级；数据不可用时返回 {@code null}。 */
    private static Level autoLevel(InfoItem item, InfoItem.Part part) {
        if (part == InfoItem.Part.PERCENT) {
            return memoryLevel(InfoSampler.memoryPercent());
        }
        switch (item) {
            case FPS:
                return fpsLevel(InfoSampler.fps());
            case TPS:
                return InfoSampler.tickTimesAvailable() ? tpsLevel(InfoSampler.tps()) : null;
            case MSPT:
                return InfoSampler.tickTimesAvailable() ? msptLevel(InfoSampler.mspt()) : null;
            case MEMORY:
                return memoryLevel(InfoSampler.memoryPercent());
            default:
                return null;
        }
    }

    /** 健康度等级 → 对应的数值颜色部件（三个等级各自的配置色）。 */
    private static InfoItem.Part partOf(Level level) {
        switch (level) {
            case WARN:
                return InfoItem.Part.VALUE_WARN;
            case BAD:
                return InfoItem.Part.VALUE_BAD;
            default:
                return InfoItem.Part.VALUE_GOOD;
        }
    }

    // ================= 健康度阈值 =================

    private static Level fpsLevel(double v) {
        if (Double.isNaN(v)) {
            return null;
        }
        if (v >= 60.0) {
            return Level.GOOD;
        }
        return v >= 30.0 ? Level.WARN : Level.BAD;
    }

    private static Level tpsLevel(double v) {
        if (Double.isNaN(v)) {
            return null;
        }
        if (v >= 19.0) {
            return Level.GOOD;
        }
        return v >= 15.0 ? Level.WARN : Level.BAD;
    }

    private static Level msptLevel(double v) {
        if (Double.isNaN(v)) {
            return null;
        }
        if (v <= 30.0) {
            return Level.GOOD;
        }
        return v < 50.0 ? Level.WARN : Level.BAD;
    }

    private static Level memoryLevel(double percent) {
        if (percent < 90.0) {
            return Level.GOOD;
        }
        return percent < 95.0 ? Level.WARN : Level.BAD;
    }

    // ================= 尺寸 / 定位 =================

    /** 整块未缩放宽度。 */
    public static int width(List<Line> lines) {
        int w = 0;
        for (Line line : lines) {
            w = Math.max(w, line.width());
        }
        return w;
    }

    /** 整块未缩放高度。 */
    public static int height(Font font, List<Line> lines) {
        if (lines.isEmpty()) {
            return 0;
        }
        return lines.size() * font.lineHeight + (lines.size() - 1) * ROW_GAP;
    }

    /**
     * 整块在屏幕上的左上角（已缩放坐标）。
     *
     * @param scale 字体缩放
     */
    public static int[] origin(List<Line> lines, Font font, Anchor anchor,
                               int marginX, int marginY, float scale,
                               int screenW, int screenH) {
        int bw = Math.round(width(lines) * scale);
        int bh = Math.round(height(font, lines) * scale);
        int x = anchor.isRight() ? screenW - marginX - bw : marginX;
        int y = anchor.isBottom() ? screenH - marginY - bh : marginY;
        return new int[]{x, y};
    }

    /**
     * 绘制整块。
     *
     * @param baseX/baseY 整块左上角（已缩放坐标）
     * @param scale       字体缩放
     */
    public static void draw(GuiGraphics gg, Font font, List<Line> lines, boolean shadow,
                            int baseX, int baseY, float scale, Anchor anchor) {
        if (lines.isEmpty()) {
            return;
        }
        int blockW = width(lines);
        PoseStack pose = gg.pose();
        pose.pushPose();
        pose.translate(baseX, baseY, 0.0D);
        pose.scale(scale, scale, 1.0F);

        int y = 0;
        for (Line line : lines) {
            int x = anchor.isRight() ? blockW - line.width() : 0;
            for (Segment s : line.segments()) {
                gg.drawString(font, s.text(), x, y, s.color(), shadow);
                x += font.width(s.text());
            }
            y += font.lineHeight + ROW_GAP;
        }

        pose.popPose();
    }
}
