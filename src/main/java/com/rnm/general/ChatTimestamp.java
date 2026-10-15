package com.rnm.general;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.rnm.gui.GuiConfig;

public final class ChatTimestamp {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static Component addTimestamp(Component message) {
        if (!GuiConfig.chatTimestampEnabled) {
            return message;
        }
        String time = LocalTime.now().format(TIME_FORMAT);

        Component prefix = Component.literal("[" + time + "] ").withStyle(ChatFormatting.GRAY);

        return Component.empty()
                .append(prefix)
                .append(message);
    }

    public static void initialize() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) -> false);

        ClientReceiveMessageEvents.MODIFY_GAME.register(
                (message, overlay) -> {
                    if (overlay) {
                        return message;
                    }

                    return addTimestamp(message);
                }
        );
    }
}