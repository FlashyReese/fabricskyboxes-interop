package me.flashyreese.mods.fabricskyboxes_interop.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new FSBInteropConfigScreen(parent, FSBInteropConfig.INSTANCE);
    }
}