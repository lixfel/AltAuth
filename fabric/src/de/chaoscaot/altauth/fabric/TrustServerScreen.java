// SPDX-License-Identifier: MIT

package de.chaoscaot.altauth.fabric;

import de.chaoscaot.altauth.fabric.config.ClientConfig;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class TrustServerScreen extends ConfirmScreen {
    private static final Text FORCE_MOJANG = Text.translatable("gui.altauth.confirm.force_mojang");
    private static final Text TRUST_ONCE = Text.translatable("gui.altauth.confirm.once");

    private final String server;
    private final Screen parent;

    public TrustServerScreen(String server, Screen parent) {
        super(t -> {}, Text.translatable("gui.altauth.confirm.title", AltAuth.address.getAddress(), server), Text.translatable("gui.altauth.confirm.text", server), ScreenTexts.YES, ScreenTexts.CANCEL);
        this.server = server;
        this.parent = parent;
    }

    @Override
    protected void addButtons(int y) {
        this.addDrawableChild(ButtonWidget.builder(this.yesText, button -> {
            ClientConfig.INSTANCE.allowedServers.add(server);
            ClientConfig.INSTANCE.save();

            ConnectScreen.connect(parent, client, AltAuth.address, null);
        }).dimensions(this.width / 2 - 155, y, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(FORCE_MOJANG, button -> {
            ClientConfig.INSTANCE.forcedMojang.add(AltAuth.address.getAddress());
            ClientConfig.INSTANCE.save();

            ConnectScreen.connect(parent, client, AltAuth.address, null);
        }).dimensions(this.width / 2 + 5, y, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(TRUST_ONCE, button -> {
            AltAuth.trustOnce = true;
            ConnectScreen.connect(parent, client, AltAuth.address, null);
        }).dimensions(this.width / 2 - 155, y + 25, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(this.noText, button -> client.setScreen(parent)).dimensions(this.width / 2 + 5, y + 25, 150, 20).build());
    }
}
