package net.nhappy.portalapi.impl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import net.nhappy.portalapi.api.client.PortalRenderer;
import net.nhappy.portalapi.impl.duck.GameRendererAccessor;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class GameRendererMixin implements GameRendererAccessor {

    @Mutable
    @Shadow
    @Final
    private Camera camera;

    @Shadow
    private boolean renderHand;

    @Override
    public boolean doesRenderHand() {
        return this.renderHand;
    }

    @Override
    public void setCamera(Camera newCamera) {
        this.camera = newCamera;
    }

    @WrapOperation(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
    private void onRenderWorld(WorldRenderer instance, ObjectAllocator allocator, RenderTickCounter tickCounter,
                               boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                               LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix,
                               Matrix4f projectionMatrix, Operation<Void> original) {
        PortalRenderer.onNewTick();
        original.call(instance, allocator, tickCounter, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, positionMatrix, projectionMatrix);
        PortalRenderer.onTickEnd();
    }

}
