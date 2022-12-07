package me.flashyreese.mods.fabricskyboxes_interop.mixin;

import io.github.amerebagatelle.fabricskyboxes.SkyboxManager;
import io.github.amerebagatelle.fabricskyboxes.skyboxes.AbstractSkybox;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = SkyboxManager.class, remap = false)
public interface SkyboxManagerAccessor {
    @Accessor("skyboxMap")
    Map<Identifier, AbstractSkybox> getSkyboxes();
}
