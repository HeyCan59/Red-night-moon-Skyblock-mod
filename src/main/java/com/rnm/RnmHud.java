package com.rnm;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class RnmHud implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
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