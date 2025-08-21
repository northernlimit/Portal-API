package net.nhappy.portalapi.impl.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.nhappy.portalapi.impl.duck.MinecraftClientAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin implements MinecraftClientAccessor {

    @Mutable
    @Shadow
    @Final
    private Framebuffer framebuffer;

    @Override
    public void setFramebuffer(Framebuffer newFramebuffer) {
        this.framebuffer = newFramebuffer;
    }

}
