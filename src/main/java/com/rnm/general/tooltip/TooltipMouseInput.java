package com.rnm.general.tooltip;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import com.rnm.gui.GuiConfig;

public final class TooltipMouseInput {
    private TooltipMouseInput() {}

    public static void initialize() {
        ScreenEvents.BEFORE_INIT.register(
            (client, screen, scaledWidth, scaledHeight) -> {
                ScreenMouseEvents.allowMouseScroll(screen).register(
                    (currentScreen, mouseX, mouseY,
                     horizontalAmount, verticalAmount) -> {

                        if (!GuiConfig.enabled) {
                            return true;
                        }

                        long window = Minecraft.getInstance()
                            .getWindow()
                            .handle();

                        boolean shiftDown =
                            GLFW.glfwGetKey(
                                window, GLFW.GLFW_KEY_LEFT_SHIFT
                            ) == GLFW.GLFW_PRESS
                            || GLFW.glfwGetKey(
                                window, GLFW.GLFW_KEY_RIGHT_SHIFT
                            ) == GLFW.GLFW_PRESS;

                        if (shiftDown && GuiConfig.verticalScrolling) {
                            TooltipScrollState.scroll(
                                horizontalAmount,
                                verticalAmount
                            );
                            return false;
                        }
                        return true;
                    }
                );
            }
        );
    }
}