package net.nhappy.portalapi.impl.duck;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Framebuffer;

@Environment(EnvType.CLIENT)
public interface MinecraftClientAccessor {
    void setFramebuffer(Framebuffer framebuffer);
}
