package net.nhappy.portalapi.api.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.nhappy.portalapi.api.block.PortalBlock;
import net.nhappy.portalapi.api.block.PortalBlockEntity;
import net.nhappy.portalapi.impl.PortalAPI;
import net.nhappy.portalapi.impl.PortalAPIClient;
import net.nhappy.portalapi.impl.duck.CameraAccessor;
import net.nhappy.portalapi.impl.duck.GameRendererAccessor;
import net.nhappy.portalapi.impl.duck.MinecraftClientAccessor;
import net.nhappy.portalapi.impl.duck.WorldRendererAccessor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@Environment(EnvType.CLIENT)
public class PortalRenderer implements BlockEntityRenderer<PortalBlockEntity> {

    private boolean rendering = false;
    private static boolean frameStart = true;
    private static boolean renderFrame = false;
    private static StencilFramebuffer framebuffer;

    public PortalRenderer(BlockEntityRendererFactory.Context ctx) {
        framebuffer = new StencilFramebuffer();
    }

    @Override
    public void render(PortalBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        if (!rendering && entity.getCachedState().get(PortalBlock.LINKED)) {
            rendering = true;

            MinecraftClient client = MinecraftClient.getInstance();
            Framebuffer oldFramebuffer = client.getFramebuffer();

            if (frameStart) {
                framebuffer.resize(oldFramebuffer.textureWidth, oldFramebuffer.textureHeight);
            }
            framebuffer.beginWrite(true);

            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            GL11.glStencilMask(0xFF);

            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);

            matrices.push();
            Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
            this.renderCube(entity, positionMatrix);
            matrices.pop();

            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
            GL11.glStencilMask(0x00);

            /*
            BufferBuilder buffer2 = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            int r = 255, g = 0, b = 0, a = 255;
            buffer2.vertex(positionMatrix, 0.0f, 2.0f, 0.0f).color(r, g, b, a);
            buffer2.vertex(positionMatrix, 0.5f, 0.2f, 0.9f).color(g, r, b, a);
            buffer2.vertex(positionMatrix, 1.3f, 0.5f, 0.5f).color(b, g, r, a);

            BufferRenderer.drawWithGlobalProgram(buffer2.end());

             */

            // Render the world =)
            Camera newCamera = new Camera();
            GameRenderer gameRenderer = client.gameRenderer;
            boolean oldDoRenderHand = ((GameRendererAccessor) gameRenderer).doesRenderHand();
            Camera oldCamera = gameRenderer.getCamera();

            BlockPos bePos = entity.getPos();
            BlockPos exitPos = entity.getLinkedPos();
            Vec3d distanceToCamera = oldCamera.getPos().subtract(bePos.getX(), bePos.getY(), bePos.getZ());
            Vec3d newCameraPos = distanceToCamera.add(exitPos.getX(), exitPos.getY(), exitPos.getZ());
            ((CameraAccessor) newCamera).prepareCamera(
                    true, client.world, client.player, !client.options.getPerspective().isFirstPerson(), tickDelta,
                    oldCamera.getYaw(), oldCamera.getPitch(), newCameraPos, (float) newCameraPos.y);

            gameRenderer.setRenderHand(false);
            ((GameRendererAccessor) gameRenderer).setCamera(newCamera);
            ((MinecraftClientAccessor) client).setFramebuffer(framebuffer);

            RenderSystem.backupProjectionMatrix();
            try {
                Quaternionf quaternionf = newCamera.getRotation().conjugate(new Quaternionf());
                Matrix4f rotation = new Matrix4f().rotation(quaternionf);
                ((WorldRendererAccessor) client.worldRenderer).portalapi$render(
                        null, client.getRenderTickCounter(), false,
                        newCamera, oldCamera, gameRenderer, gameRenderer.getLightmapTextureManager(),
                        rotation, RenderSystem.getProjectionMatrix(), frameStart
                );
            } catch (Exception e) {
                PortalAPI.LOGGER.error("Exception rendering portal: {}", String.valueOf(e));
            } finally {
                RenderSystem.restoreProjectionMatrix();
                gameRenderer.setRenderHand(oldDoRenderHand);
                ((GameRendererAccessor) gameRenderer).setCamera(oldCamera);
                ((MinecraftClientAccessor) client).setFramebuffer(oldFramebuffer);

                renderFrame = true;
                frameStart = false;
            }

            GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthFunc(GL11.GL_ALWAYS);

            this.renderCube(entity, positionMatrix);

            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthFunc(GL11.GL_LESS);

            framebuffer.endWrite();
        }

        rendering = false;
    }

    private boolean renderQuad(PortalBlockEntity entity, Matrix4f model, BufferBuilder buffer,
                               float x1, float x2, float y1, float y2, float z1, float z2, float z3,
                               float z4, Direction side) {
        if (entity.shouldDrawSide(side)) {
            buffer.vertex(model, x1, y1, z1);
            buffer.vertex(model, x2, y1, z2);
            buffer.vertex(model, x2, y2, z3);
            buffer.vertex(model, x1, y2, z4);
            return true;
        }
        return false;
    }

    private void renderCube(PortalBlockEntity entity, Matrix4f positionMatrix) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        boolean drawn = false;
        drawn |= this.renderQuad(entity, positionMatrix, buffer, 0.0F, 1.0F, 0.0F, 2.0F, 1.0F, 1.0F, 1.0F, 1.0F, Direction.SOUTH);
        drawn |= this.renderQuad(entity, positionMatrix, buffer, 0.0F, 1.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Direction.NORTH);
        drawn |= this.renderQuad(entity, positionMatrix, buffer, 1.0F, 1.0F, 2.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.EAST);
        drawn |= this.renderQuad(entity, positionMatrix, buffer, 0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.WEST);
        drawn |= this.renderQuad(entity, positionMatrix, buffer, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, Direction.DOWN);
        drawn |= this.renderQuad(entity, positionMatrix, buffer, 0.0F, 1.0F, 2.0F, 2.0F, 1.0F, 1.0F, 0.0F, 0.0F, Direction.UP);

        if (drawn) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    public static void onNewTick() {
        frameStart = true;
    }

    public static void onTickEnd() {
        if (renderFrame) {
            Framebuffer oldFramebuffer = MinecraftClient.getInstance().getFramebuffer();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            boolean debug = false;
            if (debug) {
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebuffer.fbo);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldFramebuffer.fbo);
                GL30.glBlitFramebuffer(
                        0, 0, framebuffer.textureWidth, framebuffer.textureHeight,
                        0, 0, oldFramebuffer.textureWidth, oldFramebuffer.textureHeight,
                        GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
            } else {
                Matrix4f projectionMatrix = new Matrix4f().setOrtho(
                        0.0f, oldFramebuffer.textureWidth,
                        0.0f, oldFramebuffer.textureHeight,
                        0.1f, 1000.0f);

                ShaderProgram shader = RenderSystem.setShader(PortalAPIClient.RENDERTYPE_PORTAL);
                shader.addSamplerTexture("MainColor", oldFramebuffer.getColorAttachment());
                shader.addSamplerTexture("MainDepth", oldFramebuffer.getDepthAttachment());
                shader.addSamplerTexture("PortalColor", framebuffer.getColorAttachment());
                shader.addSamplerTexture("MaskDepth", framebuffer.getDepthAttachment());
                shader.getUniform("OutSize").set((float) oldFramebuffer.textureWidth, (float) oldFramebuffer.textureHeight);

                RenderSystem.backupProjectionMatrix();
                RenderSystem.setProjectionMatrix(projectionMatrix, ProjectionType.ORTHOGRAPHIC);
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
                bufferBuilder.vertex(0.0F, 0.0F, 500.0F);
                bufferBuilder.vertex(oldFramebuffer.textureWidth, 0.0F, 500.0F);
                bufferBuilder.vertex(oldFramebuffer.textureWidth, oldFramebuffer.textureHeight, 500.0F);
                bufferBuilder.vertex(0.0F, oldFramebuffer.textureHeight, 500.0F);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                RenderSystem.restoreProjectionMatrix();
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);

            renderFrame = false;
        }
    }
}
