package me.flashyreese.mods.fabricskyboxes_interop.mixin;

import me.flashyreese.mods.fabricskyboxes_interop.client.config.FSBInteropConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onGameJoin", at = @At(value = "TAIL"))
    private void fsbInterop$showMessages(GameJoinS2CPacket packet, CallbackInfo ci) {
        if (this.client.player == null) {
            return;
        }

        if (FSBInteropConfig.INSTANCE.interoperability) {
            if (FSBInteropConfig.INSTANCE.clearFSBFormatSky) {
                this.client.player.sendMessage(new TranslatableText("fsb-interop.clear_fsb_format_sky.message").formatted(Formatting.RED), false);
            }
            this.client.player.sendMessage(new TranslatableText("fsb-interop.interoperability.message").formatted(Formatting.GOLD), false);
        }
    }
}
