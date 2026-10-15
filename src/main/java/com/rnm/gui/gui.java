package com.rnm.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class gui extends Screen {

    private final Screen parent;
    private int selectedCategory = 0;

    private static final String[] CATEGORIES = {
        "rnm.gui.general",
        "rnm.gui.combat",
        "rnm.gui.farming",
        "rnm.gui.fishing",
        "rnm.gui.mining",
        "rnm.gui.foraging",
        "rnm.gui.dungeon",
    };

    // 配色
    private static final int PANEL_COLOR = 0xF0181B24;
    private static final int SIDEBAR_COLOR = 0xFF20232D;
    private static final int LINE_COLOR = 0xFF454957;

    private static final int BUTTON_COLOR = 0xFF303642;
    private static final int BUTTON_HOVER_COLOR = 0xFF465675;
    private static final int BUTTON_SELECTED_COLOR = 0xFF3D5FA8;
    private static final int BUTTON_BORDER_COLOR = 0xFF596273;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    public gui(Screen parent) {
        super(Component.translatable("screen.rnm.settings.title"));
        this.parent = parent;
    }

    // 自訂深色按鈕
    private static class DarkButton extends Button {

        private boolean selected;

        public DarkButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Button.OnPress onPress,
            boolean selected
        ) {
            super(
                x, y, width, height,
                message,
                onPress,
                Button.DEFAULT_NARRATION
            );

            this.selected = selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected void extractContents(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
        ) {
            int background;

            if (!this.active) {
                background = 0xFF252830;
            } else if (this.selected) {
                background = BUTTON_SELECTED_COLOR;
            } else if (this.isHovered()) {
                background = BUTTON_HOVER_COLOR;
            } else {
                background = BUTTON_COLOR;
            }

            int x = this.getX();
            int y = this.getY();
            int w = this.width;
            int h = this.height;

            // 按鈕背景
            graphics.fill(x, y, x + w, y + h, background);

            // 細邊框
            graphics.fill(
                x, y, x + w, y + 1,
                BUTTON_BORDER_COLOR
            );
            graphics.fill(
                x, y + h - 1, x + w, y + h,
                BUTTON_BORDER_COLOR
            );

            // 文字置中
            var font = Minecraft.getInstance().font;
            int textX = x + (w - font.width(this.getMessage())) / 2;
            int textY = y + (h - font.lineHeight) / 2;

            int textColor = this.active
                ? TEXT_COLOR
                : 0xFF999999;

            graphics.text(
                font,
                this.getMessage(),
                textX,
                textY,
                textColor,
                true
            );
        }

        @Override
        public void updateWidgetNarration(
            NarrationElementOutput builder
        ) {
            super.updateWidgetNarration(builder);
        }
    }

    @Override
    protected void init() {
        int left = (this.width - 560) / 2;
        int top = (this.height - 340) / 2;

        // 左側分類選單
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int index = i;
            int y = top + 48 + i * 32;

            addRenderableWidget(
                new DarkButton(
                    left + 12,
                    y,
                    112,
                    26,
                    Component.literal(selectedCategory == i ? "» " : "  ").append(Component.translatable(CATEGORIES[i])),
                    button -> {
                        selectedCategory = index;
                        rebuildWidgets();
                    },
                    selectedCategory == i
                )
            );
        }

        int panelX = left + 142;
        int panelY = top + 56;

        // 一般設定
        if (selectedCategory == 0) {
            // 測試用
            addRenderableWidget(
                new DarkButton(
                    panelX, panelY, 250, 26,
                    Component.translatable(
                        "text.rnm.enabled",
                        Component.translatable(
                            GuiConfig.enabled
                                ? "text.rnm.on"
                                : "text.rnm.off"
                        )
                    ),
                    button -> {
                        GuiConfig.enabled = !GuiConfig.enabled;
                        GuiConfig.save();

                        button.setMessage(
                            Component.translatable(
                                "text.rnm.enabled",
                                Component.translatable(
                                    GuiConfig.enabled
                                        ? "text.rnm.on"
                                        : "text.rnm.off"
                                )
                            )
                        );
                        ((DarkButton) button).setSelected(GuiConfig.enabled);
                        Minecraft.getInstance().levelRenderer.needsUpdate();
                    },
                    false
                )
            );
            
            // 聊天時間戳記
            addRenderableWidget(
                new DarkButton(
                    panelX,
                    panelY + 34,
                    250,
                    26,
                    Component.translatable(
                        "rnm.gui.chatTimestamp",
                        Component.translatable(
                            GuiConfig.chatTimestampEnabled
                                ? "text.rnm.on"
                                : "text.rnm.off"
                        )
                    ),
                    button -> {
                        GuiConfig.chatTimestampEnabled =
                            !GuiConfig.chatTimestampEnabled;

                        GuiConfig.save();

                        button.setMessage(
                            Component.translatable(
                                "rnm.gui.chatTimestamp",
                                Component.translatable(
                                    GuiConfig.chatTimestampEnabled
                                        ? "text.rnm.on"
                                        : "text.rnm.off"
                                )
                            )
                        );
                        ((DarkButton) button).setSelected(
                            GuiConfig.chatTimestampEnabled
                        );
                        Minecraft.getInstance().levelRenderer.needsUpdate();
                    },
                    GuiConfig.chatTimestampEnabled
                )
            );
        }

        // 挖礦設定
        if (selectedCategory == 4) {
            // Metal Detector 開關
            addRenderableWidget(
                new DarkButton(
                    panelX, panelY, 250, 26,
                    Component.translatable(
                        "rnm.gui.mining.MetalDetector",
                        Component.translatable(
                            GuiConfig.MetalDetector
                                ? "text.rnm.on"
                                : "text.rnm.off"
                        )
                    ),
                    button -> {
                        GuiConfig.MetalDetector = !GuiConfig.MetalDetector;
                        GuiConfig.save();

                        button.setMessage(
                            Component.translatable(
                                "rnm.gui.mining.MetalDetector",
                                Component.translatable(
                                    GuiConfig.MetalDetector
                                        ? "text.rnm.on"
                                        : "text.rnm.off"
                                )
                            )
                        );
                        ((DarkButton) button).setSelected(
                            GuiConfig.MetalDetector
                        );
                        Minecraft.getInstance().levelRenderer.needsUpdate();
                    },
                    GuiConfig.MetalDetector
                )
            );
        }
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float delta
    ) {
        int left = (this.width - 560) / 2;
        int top = (this.height - 340) / 2;

        // 先畫面板，避免遮住按鈕
        graphics.fill(
            left, top, left + 560, top + 340,
            PANEL_COLOR
        );

        // 左側側欄
        graphics.fill(
            left, top, left + 132, top + 340,
            SIDEBAR_COLOR
        );

        // 分隔線
        graphics.fill(
            left + 132,
            top,
            left + 133,
            top + 339,
            LINE_COLOR
        );

        // 標題
        graphics.text(
            this.font,
            "Red night moon",
            left + 14,
            top + 16,
            TEXT_COLOR,
            true
        );

        // 分類標題
        graphics.text(
            this.font,
            Component.translatable(CATEGORIES[selectedCategory]),
            left + 150,
            top + 24,
            TEXT_COLOR,
            true
        );

        // 標題下方分隔線
        graphics.fill(
            left + 148, top + 42,
            left + 426, top + 43,
            LINE_COLOR
        );

        // 底部資訊欄
        int footerHeight = 30;
        int footerY = top + 340 - footerHeight;

        graphics.fill(
            left + 1,
            footerY,
            left + 559,
            top + 339,
            0xFF101219
        );

        // 資訊欄上方分隔線
        graphics.fill(
            left + 1,
            footerY,
            left + 559,
            footerY + 1,
            LINE_COLOR
        );

        // 製作人文字
        Component credit = Component.translatable("rnmt");

        int textX = left + (560 - this.font.width(credit)) / 2;
        int textY = footerY + (footerHeight - this.font.lineHeight) / 2;

        graphics.text(
            this.font,
            credit,
            textX,
            textY,
            0xFFFFD54F,
            true
        );

        // 最後繪製按鈕
        super.extractRenderState(
            graphics, mouseX, mouseY, delta
        );
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}