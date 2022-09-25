// SPDX-License-Identifier: MIT

package de.chaoscaot.altauth.fabric.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.translatable("config.altauth.title"));

            builder.setSavingRunnable(ClientConfig.INSTANCE::save);
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.altauth.general"));
            general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.altauth.enabled"), ClientConfig.INSTANCE.enabled)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> ClientConfig.INSTANCE.enabled = value)
                    .build());

            general.addEntry(entryBuilder.startStrList(Text.translatable("config.altauth.servers"), ClientConfig.INSTANCE.allowedServers)
                    .setDefaultValue(new ArrayList<>())
                    .setSaveConsumer(list -> ClientConfig.INSTANCE.allowedServers = list)
                    .build());
            return builder.build();
        };
    }
}
