package me.flashyreese.mods.fabricskyboxes_interop.mixin;

import io.github.amerebagatelle.fabricskyboxes.SkyboxManager;
import io.github.amerebagatelle.fabricskyboxes.skyboxes.AbstractSkybox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = SkyboxManager.class, remap = false)
public interface SkyboxManagerAccessor {
    @Accessor("skyboxes")
    List<AbstractSkybox> getSkyboxes();
}
