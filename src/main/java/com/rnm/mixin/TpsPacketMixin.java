package com.rnm.mixin;

import com.rnm.RnmTpsEstimator;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class TpsPacketMixin {

    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void rnm$onTimeUpdate(
        ClientboundSetTimePacket packet,
        CallbackInfo ci
    ) {
        RnmTpsEstimator.onTimeUpdate(packet.gameTime());
    }
}