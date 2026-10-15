package com.rnm;

import com.mojang.blaze3d.platform.InputConstants;
import com.rnm.gui.gui;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class RnmGuiScreenKey {

    private static final KeyMapping.Category category =KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rnm", "general"));

    private static KeyMapping openGuiKey;

    public static void initialize() {
        openGuiKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.rnm.open_gui",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_RIGHT_SHIFT,
                        category
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                client.setScreen(
                        new gui(client.screen)
                );
            }
        });
    }
}