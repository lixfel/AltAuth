package de.chaoscaot.altauth.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.WarningScreen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ApproveServerScreen extends WarningScreen {
    private static final Text TITLE = Text.of("Approve Server");
    private static final Text MESSAGE = Text.of("Do you want to approve this server?");
    private static final Text NARRATOR_MESSAGE = Text.of("Do you want to approve this server?");

    public ApproveServerScreen(String serverAddress) {
        super(TITLE, MESSAGE, NARRATOR_MESSAGE);
    }

    @Override
    protected void initButtons(int yOffset) {

    }
}
