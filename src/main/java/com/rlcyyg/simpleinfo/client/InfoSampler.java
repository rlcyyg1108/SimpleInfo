package com.rlcyyg.simpleinfo.client;

import com.rlcyyg.simpleinfo.SimpleInfoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 按配置的刷新频率采样数据，并缓存成字符串 / 数值。
 * <p>
 * 这是"防止频繁刷新导致性能损失"的落点：昂贵的格式化（String.format）只在
 * 到达刷新周期时才执行，其余 tick 直接返回。渲染每帧都只是读缓存。
 * <p>
 * 对外只暴露<b>取数</b>接口；分段、上色、排版全部交给 {@link InfoBlock}。
 */
public final class InfoSampler {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static int tickCounter = 0;
    private static int frameCounter = 0;

    // ===== 缓存值 =====
    private static boolean tickTimesAvailable = false;
    private static double tps = Double.NaN;
    private static double mspt = Double.NaN;
    private static double fps = Double.NaN;
    private static String timeText = "--:--";
    private static long gameTicks = -1L;
    private static long memUsedBytes = 0L;
    private static long memMaxBytes = 0L;

    private InfoSampler() {
    }

    // ================= 对外取数 =================

    /** TPS 是否可用（多人服务器下没有本地服务端，不可用）。 */
    public static boolean tickTimesAvailable() {
        return tickTimesAvailable;
    }

    public static double tps() {
        return tps;
    }

    public static double mspt() {
        return mspt;
    }

    public static double fps() {
        return fps;
    }

    /** 系统时间 "HH:mm"。 */
    public static String timeText() {
        return timeText;
    }

    /** 游戏时间，小时（1 位小数）。 */
    public static String gameTimeText() {
        if (gameTicks < 0L) {
            return "--";
        }
        return String.format(Locale.ROOT, "%.1fh", gameTicks / 72000.0D);   // 20 tick/s × 3600 s
    }

    /** 已用内存占最大堆的百分比（0~100）。 */
    public static double memoryPercent() {
        return memMaxBytes <= 0L ? 0.0D : memUsedBytes * 100.0D / (double) memMaxBytes;
    }

    /** 已用内存，按当前配置单位格式化（如 "3.2G"）。 */
    public static String memoryUsedText() {
        return formatMemory(memUsedBytes);
    }

    /** 总分配内存，按当前配置单位格式化（如 "8.0G"）。 */
    public static String memoryTotalText() {
        return formatMemory(memMaxBytes);
    }

    public static String memoryPercentText() {
        return String.format(Locale.ROOT, "%.0f%%", memoryPercent());
    }

    private static String formatMemory(long bytes) {
        boolean mega = "M".equalsIgnoreCase(SimpleInfoConfig.memoryUnit());
        if (mega) {
            return String.format(Locale.ROOT, "%.0fM", bytes / (1024.0D * 1024.0D));
        }
        return String.format(Locale.ROOT, "%.1fG", bytes / (1024.0D * 1024.0D * 1024.0D));
    }

    // ================= 采样 =================

    /** 每帧调用：累加真实帧数。 */
    public static void onFrame() {
        frameCounter++;
    }

    /** 每 tick 调用（Phase.END）。未到刷新周期就直接返回。 */
    public static void onTick() {
        int interval = SimpleInfoConfig.refreshTicks();
        if (++tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        sampleTickTimes();
        sampleTime();
        sampleGameTime();
        sampleMemory();
        sampleFps();
        frameCounter = 0;
    }

    /**
     * TPS / MSPT。
     * <p>
     * 只能取单人世界的数据 —— 没有网络封包，所以多人服务器下显示 {@code --}。
     * <p>
     * {@code tickTimes} 是 {@code public final long[]} 字段（不是 getTickTimes()），
     * 长度 100，单位<b>纳秒</b>。
     */
    private static void sampleTickTimes() {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            tickTimesAvailable = false;
            tps = Double.NaN;
            mspt = Double.NaN;
            return;
        }
        long[] times = server.tickTimes;
        if (times == null || times.length == 0) {
            tickTimesAvailable = false;
            tps = Double.NaN;
            mspt = Double.NaN;
            return;
        }
        long sum = 0L;
        for (long t : times) {
            sum += t;
        }
        double msptValue = sum / (double) times.length / 1_000_000.0;   // 纳秒 -> 毫秒
        mspt = msptValue;
        tps = msptValue <= 0.0 ? 20.0 : Math.min(20.0, 1000.0 / msptValue);
        tickTimesAvailable = true;
    }

    private static void sampleTime() {
        timeText = LocalTime.now().format(TIME_FMT);
    }

    /** 游戏时间：世界创建以来的总游戏刻数。 */
    private static void sampleGameTime() {
        ClientLevel level = Minecraft.getInstance().level;
        gameTicks = level == null ? -1L : level.getGameTime();
    }

    /**
     * 内存占用。分母用 {@code maxMemory()}（JVM 堆上限）。
     */
    private static void sampleMemory() {
        Runtime rt = Runtime.getRuntime();
        memUsedBytes = rt.totalMemory() - rt.freeMemory();
        memMaxBytes = rt.maxMemory();
    }

    /**
     * FPS = 本周期内累计的帧数。
     * <p>
     * 累加点在 {@code RenderGuiEvent.Pre}（每帧触发）而不是 ClientTickEvent（每 tick 一次）
     * —— 后者与真实帧数无关，低帧率时会严重高估。
     */
    private static void sampleFps() {
        double seconds = SimpleInfoConfig.refreshTicks() / 20.0;
        fps = seconds <= 0.0 ? 0.0 : frameCounter / seconds;
    }
}
