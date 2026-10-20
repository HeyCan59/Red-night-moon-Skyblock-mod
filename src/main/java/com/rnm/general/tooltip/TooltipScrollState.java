package com.rnm.general.tooltip;

import com.rnm.gui.GuiConfig;

public final class TooltipScrollState {
    private static double scrollY;

    private TooltipScrollState() {}

    public static double getScrollY() {
        return scrollY;
    }

    public static void scroll(double horizontal, double vertical) {
        if (!GuiConfig.enabled) return;

        if (GuiConfig.verticalScrolling) {
            scrollY -= vertical * 12.0;
        }
    }

    public static void reset() {
        scrollY = 0.0;
    }
}