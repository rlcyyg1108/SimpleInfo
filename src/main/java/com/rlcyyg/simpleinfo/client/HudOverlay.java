package com.rlcyyg.simpleinfo.client;

import com.rlcyyg.simpleinfo.Anchor;
import com.rlcyyg.simpleinfo.SimpleInfoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/**
 * HUD 渲染。内容与排版全部委托给 {@link InfoBlock}，与配置界面预览共用同一份逻辑。
 */
public class HudOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics gg, float partialTick, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();

        // 尊重 F1 隐藏 HUD
        if (mc.options.hideGui) {
            return;
        }
        // F3 调试界面打开时固定隐藏（不受任何配置影响）。
        // 注意 F3 不打开 Screen —— 它只是把 options.renderDebug 置为 true，所以要单独判。
        if (mc.options.renderDebug) {
            return;
        }
        // 有 Screen 打开（聊天 / 背包 / 各种界面）时，按配置隐藏。
        if (SimpleInfoConfig.hideInChatScreen() && mc.screen != null) {
            return;
        }
        if (!SimpleInfoConfig.hudEnabled()) {
            return;
        }

        Font font = mc.font;
        List<InfoBlock.Line> lines = InfoBlock.build(font);
        if (lines.isEmpty()) {
            return;
        }

        float scale = (float) SimpleInfoConfig.fontScale();
        Anchor anchor = SimpleInfoConfig.anchor();
        int[] origin = InfoBlock.origin(lines, font, anchor,
                SimpleInfoConfig.marginX(), SimpleInfoConfig.marginY(),
                scale, screenW, screenH);

        InfoBlock.draw(gg, font, lines, SimpleInfoConfig.showShadow(),
                origin[0], origin[1], scale, anchor);
    }
}
