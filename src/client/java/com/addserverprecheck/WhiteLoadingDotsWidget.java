package com.addserverprecheck;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public final class WhiteLoadingDotsWidget extends LoadingDotsWidget {

    private final Font dotFont;

    public WhiteLoadingDotsWidget(Font font) {
        super(font, Component.empty());

        this.dotFont = font;
        this.setSize(44, 20);
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        String dots = LoadingDotsText.get(Util.getMillis());

        int x = this.getX()
                + (this.getWidth() - this.dotFont.width(dots)) / 2;

        int y = this.getY()
                + (this.getHeight() - 9) / 2;

        // Draw Minecraft's animated dots in pure white.
        graphics.text(this.dotFont, dots, x, y, 0xFFFFFFFF);
    }
}