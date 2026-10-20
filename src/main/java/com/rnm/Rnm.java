package com.rnm;

import com.rnm.general.ChatTimestamp;
import com.rnm.mining.MetalDetector;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.rnm.general.tooltip.TooltipMouseInput;
import com.rnm.gui.GuiConfig;

public class Rnm implements ModInitializer {
    public void onInitialize() {
        RnmChatCommand.initialize();
        RnmGuiScreenKey.initialize();
        MetalDetector.init();
        ChatTimestamp.initialize();
        GuiConfig.load();
        TooltipMouseInput.initialize();

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("rnm", "hud"),
            (graphics, deltaTracker) -> {
                Minecraft minecraft = Minecraft.getInstance();

                graphics.text(
                    minecraft.font,
                    "RNM SkyBlock Mod",
                    8,
                    8,
                    0xFF55FF55,
                    true
                );
            }
        );
    }
}