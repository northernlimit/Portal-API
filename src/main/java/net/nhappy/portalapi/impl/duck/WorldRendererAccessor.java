package net.nhappy.portalapi.impl.duck;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public interface WorldRendererAccessor {
    void portalapi$render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera newCamera, Camera oldCamera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix, Matrix4f projectionMatrix, boolean clearFramebuffer);
}
