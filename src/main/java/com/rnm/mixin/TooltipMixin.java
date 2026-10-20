package com.rnm.mixin;

import java.util.List;

import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.rnm.gui.GuiConfig;
import com.rnm.general.tooltip.TooltipScrollState;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;

@Mixin(GuiGraphicsExtractor.class)
public abstract class TooltipMixin {
    @Unique
    private boolean rnm$transformActive;

    @Inject(
        method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V",
        at = @At("HEAD")
    )
    private void rnm$beforeTooltip(
            Font font,
            List<?> lines,
            int x,
            int y,
            ClientTooltipPositioner positioner,
            Identifier style,
            CallbackInfo ci) {

        if (!GuiConfig.enabled) {
            return;
        }

        GuiGraphicsExtractor graphics =
            (GuiGraphicsExtractor) (Object) this;

        Matrix3x2fStack pose = graphics.pose();

        double scrollY = TooltipScrollState.getScrollY();

        pose.pushMatrix();

        pose.translate(
            (float) x,
            (float) (y + scrollY)
        );

        pose.translate(-x, -y);

        rnm$transformActive = true;
    }

    @Inject(
        method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V",
        at = @At("TAIL")
    )
    private void rnm$afterTooltip(
            Font font,
            List<?> lines,
            int x,
            int y,
            ClientTooltipPositioner positioner,
            Identifier style,
            CallbackInfo ci) {

        if (!rnm$transformActive) {
            return;
        }

        GuiGraphicsExtractor graphics =
            (GuiGraphicsExtractor) (Object) this;

        graphics.pose().popMatrix();
        rnm$transformActive = false;
    }
}