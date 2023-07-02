package me.flashyreese.mods.fabricskyboxes_interop.mixin;

import io.github.amerebagatelle.fabricskyboxes.resource.SkyboxResourceListener;
import me.flashyreese.mods.fabricskyboxes_interop.FSBInterop;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyboxResourceListener.class)
public class MixinSkyboxResourceListener {
    @Unique
    private FSBInterop converter;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void postInit(CallbackInfo ci) {
        this.converter = new FSBInterop();
    }

    @Inject(method = "reload", at = @At(value = "TAIL"))
    public void reload(ResourceManager manager, CallbackInfo ci) {
        this.converter.inject(manager);
    }
}
