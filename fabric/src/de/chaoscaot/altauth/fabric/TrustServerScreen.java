// SPDX-License-Identifier: MIT

package de.chaoscaot.altauth.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WarningScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class TrustServerScreen extends WarningScreen {
    private static final Text MESSAGE = Text.of("Do you want to trust this server?");

    private final String serverId;
    private final Screen parent;

    public TrustServerScreen(String serverAddress, Screen parent) {
        super(Text.of("Trust '" + serverAddress + "'"), MESSAGE, Text.of("Do you want to Trust '" + serverAddress + "'?"));
        this.serverId = serverAddress;
        this.parent = null;
    }

    @Override
    protected void initButtons(int yOffset) {
        // Yes, Once, Cancel

        addDrawableChild(new ButtonWidget(width / 2 - 155, height / 6 + 96 + yOffset, 150, 20, Text.of("Yes"), (buttonWidget) -> {
            ClientConfig.INSTANCE.allowedServers.add(serverId);
            ClientConfig.INSTANCE.save();

            ConnectScreen.connect(parent, client, AltAuth.address, null);
        }));

        addDrawableChild(new ButtonWidget(width / 2 - 155 + 160, height / 6 + 96 + yOffset + 30, 150, 20, Text.of("Once"), (buttonWidget) -> {
            AltAuth.trustOnce = true;

            ConnectScreen.connect(parent, client, AltAuth.address, null);
        }));

        addDrawableChild(new ButtonWidget(width / 2 - 155 + 160, height / 6 + 96 + yOffset + 60, 150, 20, Text.of("Cancel"), (buttonWidget) -> {
            client.setScreen(parent);
        }));
    }
}
