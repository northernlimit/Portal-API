package net.nhappy.portalapi.impl.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.SimpleFramebufferFactory;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.nhappy.portalapi.impl.PortalAPI;
import net.nhappy.portalapi.impl.duck.WorldRendererAccessor;
import net.nhappy.portalapi.impl.config.PortalAPIConfig;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin implements WorldRendererAccessor {

    @Shadow protected abstract boolean canDrawEntityOutlines();

    @Shadow @Final private MinecraftClient client;

    @Shadow public abstract boolean isRenderingReady(BlockPos pos);

    @Shadow @Nullable private ClientWorld world;

    @Shadow @Final private BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow @Nullable private Frustum capturedFrustum;

    @Shadow private Frustum frustum;

    @Shadow @Final private DefaultFramebufferSet framebufferSet;

    @Shadow private boolean shouldCaptureFrustum;

    @Shadow private int renderedEntitiesCount;

    @Shadow @Final private List<Entity> renderedEntities;

    @Shadow protected abstract void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator);

    @Shadow protected abstract void updateChunks(Camera camera);

    @Shadow @Nullable protected abstract PostEffectProcessor getTransparencyPostEffectProcessor();

    @Shadow @Nullable private Framebuffer entityOutlineFramebuffer;

    @Shadow protected abstract void renderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog);

    @Shadow protected abstract void renderMain(FrameGraphBuilder frameGraphBuilder, Frustum frustum, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Fog fog, boolean renderBlockOutline, boolean hasEntitiesToRender, RenderTickCounter renderTickCounter, Profiler profiler);

    @Shadow protected abstract void renderParticles(FrameGraphBuilder frameGraphBuilder, Camera camera, LightmapTextureManager lightmapTextureManager, float tickDelta, Fog fog);

    @Shadow private int ticks;

    @Shadow protected abstract void renderClouds(FrameGraphBuilder frameGraphBuilder, Matrix4f positionMatrix, Matrix4f projectionMatrix, CloudRenderMode renderMode, Vec3d cameraPos, float ticks, int color, float cloudHeight);

    @Shadow protected abstract void renderWeather(FrameGraphBuilder frameGraphBuilder, LightmapTextureManager lightmapTextureManager, Vec3d pos, float tickDelta, Fog fog);

    @Shadow protected abstract void renderLateDebug(FrameGraphBuilder frameGraphBuilder, Vec3d pos, Fog fog);

    @Shadow @Final private static Identifier ENTITY_OUTLINE;

    @Override
    public void portalapi$render(
            ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera newCamera,
            Camera oldCamera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            boolean clearFramebuffer
    ) {
        float f = tickCounter.getTickDelta(false);
        final Fog oldFog = RenderSystem.getShaderFog();
        // Restore later
        this.blockEntityRenderDispatcher.configure(this.world, newCamera, this.client.crosshairTarget);
        this.entityRenderDispatcher.configure(this.world, newCamera, this.client.targetedEntity);
        final Profiler profiler = Profilers.get();

        Vec3d vec3d = newCamera.getPos();
        double d = vec3d.getX();
        double e = vec3d.getY();
        double g = vec3d.getZ();
        profiler.swap("culling");
        boolean bl = this.capturedFrustum != null;
        Frustum frustum = bl ? this.capturedFrustum : this.frustum;

        // Restore later
        frustum.setPosition(d, e, g);
        final boolean oldShouldCaptureFrustum = this.shouldCaptureFrustum;
        final Frustum oldCapturedFrustum = this.capturedFrustum;

        Profilers.get().swap("captureFrustum");
        if (this.shouldCaptureFrustum) {
            this.capturedFrustum = bl ? new Frustum(positionMatrix, projectionMatrix) : frustum;
            this.capturedFrustum.setPosition(d, e, g);
            this.shouldCaptureFrustum = false;
        }

        profiler.swap("fog");
        float h = PortalAPIConfig.getPortalViewDistance();
        boolean bl2 = this.client.world.getDimensionEffects().useThickFog(MathHelper.floor(d), MathHelper.floor(e))
                || this.client.inGameHud.getBossBarHud().shouldThickenFog();
        // Restore later
        final Vector4f oldBgColor = BackgroundRenderer.getFogColor(oldCamera, f, this.client.world, this.client.options.getClampedViewDistance(), gameRenderer.getSkyDarkness(f));
        Vector4f vector4f = BackgroundRenderer.getFogColor(newCamera, f, this.client.world, PortalAPIConfig.getClampedPortalViewDistance(), gameRenderer.getSkyDarkness(f));
        Fog fog = BackgroundRenderer.applyFog(newCamera, BackgroundRenderer.FogType.FOG_TERRAIN, vector4f, h, bl2, f);
        Fog fog2 = BackgroundRenderer.applyFog(newCamera, BackgroundRenderer.FogType.FOG_SKY, vector4f, h, bl2, f);
        profiler.swap("cullEntities");
        // Restore later
        final double oldRenderDistanceMultiplier = Entity.getRenderDistanceMultiplier();
        final int oldRenderedEntitiesCount = this.renderedEntitiesCount;
        final List<Entity> oldRenderedEntities = List.copyOf(this.renderedEntities);
        boolean bl3 = this.portalapi$getEntitiesToRender(newCamera, frustum, this.renderedEntities);
        this.renderedEntitiesCount = this.renderedEntities.size();
        // Restore later
        profiler.swap("terrain_setup");
        this.setupTerrain(newCamera, frustum, bl, this.client.player.isSpectator());
        profiler.swap("compile_sections");
        this.updateChunks(newCamera);

        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
        matrix4fStack.pushMatrix();
        matrix4fStack.set(positionMatrix);

        FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
        final Handle<Framebuffer> oldMainFramebuffer = this.framebufferSet.mainFramebuffer;
        final Handle<Framebuffer> oldTranslucentFramebuffer = this.framebufferSet.translucentFramebuffer;
        final Handle<Framebuffer> oldItemFramebuffer = this.framebufferSet.itemEntityFramebuffer;
        final Handle<Framebuffer> oldParticleFramebuffer = this.framebufferSet.particlesFramebuffer;
        final Handle<Framebuffer> oldWeatherFramebuffer = this.framebufferSet.weatherFramebuffer;
        final Handle<Framebuffer> oldCloudFramebuffer = this.framebufferSet.cloudsFramebuffer;
        final Handle<Framebuffer> oldEntityFramebuffer = this.framebufferSet.entityOutlineFramebuffer;

        try {
            this.framebufferSet.mainFramebuffer = frameGraphBuilder.createObjectNode("main", this.client.getFramebuffer());
            int i = this.client.getFramebuffer().textureWidth;
            int j = this.client.getFramebuffer().textureHeight;
            SimpleFramebufferFactory simpleFramebufferFactory = new SimpleFramebufferFactory(i, j, true);
            PostEffectProcessor postEffectProcessor = this.getTransparencyPostEffectProcessor();
            if (postEffectProcessor != null) {
                this.framebufferSet.translucentFramebuffer = frameGraphBuilder.createResourceHandle("translucent", simpleFramebufferFactory);
                this.framebufferSet.itemEntityFramebuffer = frameGraphBuilder.createResourceHandle("item_entity", simpleFramebufferFactory);
                this.framebufferSet.particlesFramebuffer = frameGraphBuilder.createResourceHandle("particles", simpleFramebufferFactory);
                this.framebufferSet.weatherFramebuffer = frameGraphBuilder.createResourceHandle("weather", simpleFramebufferFactory);
                this.framebufferSet.cloudsFramebuffer = frameGraphBuilder.createResourceHandle("clouds", simpleFramebufferFactory);
            }

            if (this.entityOutlineFramebuffer != null) {
                this.framebufferSet.entityOutlineFramebuffer = frameGraphBuilder.createObjectNode("entity_outline", this.entityOutlineFramebuffer);
            }

            RenderPass renderPass = frameGraphBuilder.createPass("clear");
            this.framebufferSet.mainFramebuffer = renderPass.transfer(this.framebufferSet.mainFramebuffer);
            if (clearFramebuffer) {
                renderPass.setRenderer(() -> {
                    RenderSystem.clearColor(vector4f.x, vector4f.y, vector4f.z, 0.0F);
                    RenderSystem.clear(16640);
                });
            }
            if (!bl2) {
                this.renderSky(frameGraphBuilder, newCamera, f, fog2);
            }

            this.renderMain(frameGraphBuilder, frustum, newCamera, positionMatrix, projectionMatrix, fog, renderBlockOutline, bl3, tickCounter, profiler);
            PostEffectProcessor postEffectProcessor2 = this.client.getShaderLoader().loadPostEffect(ENTITY_OUTLINE, DefaultFramebufferSet.MAIN_AND_ENTITY_OUTLINE);
            if (bl3 && postEffectProcessor2 != null) {
                postEffectProcessor2.render(frameGraphBuilder, i, j, this.framebufferSet);
            }

            this.renderParticles(frameGraphBuilder, newCamera, lightmapTextureManager, f, fog);
            CloudRenderMode cloudRenderMode = this.client.options.getCloudRenderModeValue();
            if (cloudRenderMode != CloudRenderMode.OFF) {
                float k = this.world.getDimensionEffects().getCloudsHeight();
                if (!Float.isNaN(k)) {
                    float l = this.ticks + f;
                    int m = this.world.getCloudsColor(f);
                    this.renderClouds(frameGraphBuilder, positionMatrix, projectionMatrix, cloudRenderMode, newCamera.getPos(), l, m, k + 0.33F);
                }
            }

            this.renderWeather(frameGraphBuilder, lightmapTextureManager, newCamera.getPos(), f, fog);
            if (postEffectProcessor != null) {
                postEffectProcessor.render(frameGraphBuilder, i, j, this.framebufferSet);
            }

            this.renderLateDebug(frameGraphBuilder, vec3d, fog);
            profiler.swap("framegraph");
            frameGraphBuilder.run(allocator, new FrameGraphBuilder.Profiler() {
                @Override
                public void push(String location) {
                    profiler.push(location);
                }

                @Override
                public void pop(String location) {
                    profiler.pop();
                }
            });
        } catch (Exception ex) {
            PortalAPI.LOGGER.error("Exception rendering world: {}", String.valueOf(ex));
        } finally {
            this.framebufferSet.mainFramebuffer = oldMainFramebuffer;
            this.framebufferSet.translucentFramebuffer = oldTranslucentFramebuffer;
            this.framebufferSet.itemEntityFramebuffer = oldItemFramebuffer;
            this.framebufferSet.particlesFramebuffer = oldParticleFramebuffer;
            this.framebufferSet.weatherFramebuffer = oldWeatherFramebuffer;
            this.framebufferSet.cloudsFramebuffer = oldCloudFramebuffer;
            this.framebufferSet.entityOutlineFramebuffer = oldEntityFramebuffer;
            RenderSystem.clearColor(oldBgColor.x, oldBgColor.y, oldBgColor.z, 0.0F);
            matrix4fStack.popMatrix();
        }

        this.client.getFramebuffer().beginWrite(false);
        this.renderedEntities.clear();
        this.renderedEntities.addAll(oldRenderedEntities);

        // Restore everything needed
        this.blockEntityRenderDispatcher.configure(this.world, oldCamera, this.client.crosshairTarget);
        this.entityRenderDispatcher.configure(this.world, oldCamera, this.client.targetedEntity);
        Vec3d oldCameraPos = oldCamera.getPos();
        frustum.setPosition(oldCameraPos.getX(), oldCameraPos.getY(), oldCameraPos.getZ());
        this.shouldCaptureFrustum = oldShouldCaptureFrustum;
        this.capturedFrustum = oldCapturedFrustum;
        if (this.capturedFrustum != null) {
            this.capturedFrustum.setPosition(oldCameraPos.getX(), oldCameraPos.getY(), oldCameraPos.getZ());
        }
        Entity.setRenderDistanceMultiplier(oldRenderDistanceMultiplier);
        this.renderedEntitiesCount = oldRenderedEntitiesCount;

        this.setupTerrain(oldCamera, frustum, bl, this.client.player.isSpectator());
        this.updateChunks(oldCamera);

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderFog(oldFog);
    }

    @Unique
    private boolean portalapi$getEntitiesToRender(Camera camera, Frustum frustum, List<Entity> output) {
        Vec3d vec3d = camera.getPos();
        double d = vec3d.getX();
        double e = vec3d.getY();
        double f = vec3d.getZ();
        boolean bl = false;
        boolean bl2 = this.canDrawEntityOutlines();
        // Restore later
        Entity.setRenderDistanceMultiplier(
                MathHelper.clamp(PortalAPIConfig.getClampedPortalViewDistance() / 8.0, 1.0, 2.5) * this.client.options.getEntityDistanceScaling().getValue()
        );

        for (Entity entity : this.world.getEntities()) {
            if (this.entityRenderDispatcher.shouldRender(entity, frustum, d, e, f) || entity.hasPassengerDeep(this.client.player)) {
                BlockPos blockPos = entity.getBlockPos();
                if ((this.world.isOutOfHeightLimit(blockPos.getY()) || this.isRenderingReady(blockPos))
                        && (!(entity instanceof ClientPlayerEntity) || camera.getFocusedEntity() == entity)) {
                    output.add(entity);
                    if (bl2 && this.client.hasOutline(entity)) {
                        bl = true;
                    }
                }
            }
        }

        return bl;
    }

}
