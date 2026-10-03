package com.rlcyyg.simpleinfo.client;

import com.rlcyyg.simpleinfo.SimpleInfo;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * <b>Forge 总线</b>事件：每 tick 采样 + 每帧累加 FPS + 快捷键检测。
 * <p>
 * 不指定 {@code bus=} 时默认就是 {@code Bus.FORGE}。
 */
@Mod.EventBusSubscriber(modid = SimpleInfo.MODID, value = Dist.CLIENT)
public final class ClientForgeBusEvents {

    private ClientForgeBusEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // 按配置的刷新频率采样（未到周期时内部直接返回，不做昂贵的格式化）
        InfoSampler.onTick();

        // consumeClick 必须在 tick 里调，且要 while 循环把积压的点击排空
        while (ClientModBusEvents.OPEN_CONFIG_KEY.get().consumeClick()) {
            Minecraft.getInstance().setScreen(new SimpleInfoConfigScreen(null));
        }
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        // 每帧触发，用于统计真实帧数
        InfoSampler.onFrame();
    }
}
