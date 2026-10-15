package com.rnm;

import com.rnm.general.ChatTimestamp;
import com.rnm.mining.MetalDetector;
import net.fabricmc.api.ModInitializer;

public class Rnm implements ModInitializer {
    public void onInitialize() {
        RnmChatCommand.initialize();
        RnmGuiScreenKey.initialize();
        MetalDetector.init();
        ChatTimestamp.initialize();
    }
}