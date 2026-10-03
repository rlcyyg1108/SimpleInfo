package com.rlcyyg.simpleinfo.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import com.rlcyyg.simpleinfo.SimpleInfo;

/**
 * <b>mod 总线</b>事件：按键注册与 overlay 注册。
 * <p>
 * 注意总线归属 —— {@code RegisterKeyMappingsEvent} 与 {@code RegisterGuiOverlaysEvent}
 * 都在 <b>mod 总线</b>；{@code ClientTickEvent} / {@code RenderGuiEvent} 才是 Forge 总线。
 * 挂错总线事件永不触发。
 * <p>
 * 注解属性是 {@code value = Dist.CLIENT}，1.20.1 <b>没有</b> {@code dist =}。
 */
@Mod.EventBusSubscriber(modid = SimpleInfo.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModBusEvents {

    /**
     * KeyMapping 不能在类静态初始化时构造（那时游戏还没起来），
     * 而且 Forge 要求所有 KeyMapping 都在注册事件里登记，所以延迟创建。
     */
    public static final Lazy<KeyMapping> OPEN_CONFIG_KEY = Lazy.of(() -> new KeyMapping(
            "key.simpleinfo.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            "key.categories.simpleinfo"));

    private ClientModBusEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG_KEY.get());
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("simpleinfo", new HudOverlay());
    }
}
