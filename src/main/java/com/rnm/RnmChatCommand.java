package com.rnm;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class RnmChatCommand {
    private static boolean initialized = false;

    private RnmChatCommand() {}

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        // 輸入 !tps：只在自己的聊天視窗顯示
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!message.trim().equalsIgnoreCase("!tps")) {
                return true;
            }

            Minecraft client = Minecraft.getInstance();
            client.execute(() -> sendTpsResponse(false));

            return false;
        });

        // 輸入 /pc !tps：把估算結果傳送到派對頻道
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            if (!command.trim().equalsIgnoreCase("pc !tps")) {
                return true;
            }

            Minecraft client = Minecraft.getInstance();
            client.execute(() -> sendTpsResponse(true));

            // 阻止原本的 /pc !tps 傳送到伺服器
            return false;
        });
    }

    private static void sendTpsResponse(boolean partyChat) {
        Minecraft client = Minecraft.getInstance();

        if (client.gui == null) {
            return;
        }

        double tps = RnmTpsEstimator.getTps();

        String responseText;

        if (tps < 0) {
            responseText =
                RnmText.PREFIX + "TPS ...";
        } else {
            responseText = String.format(
                Locale.ROOT,
                RnmText.PREFIX + " TPS: %.1f",
                tps
            );
        }

        if (partyChat && client.player != null) {
            // 傳送給 Hypixel 派對頻道的所有成員
            client.player.connection.sendCommand(
                "pc " + responseText
            );
        } else {
            // 只顯示在自己的聊天視窗
            client.gui.getChat().addClientSystemMessage(
                Component.literal(responseText)
            );
        }
    }
}