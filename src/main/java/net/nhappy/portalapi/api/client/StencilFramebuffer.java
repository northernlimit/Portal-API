package net.nhappy.portalapi.api.client;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@Environment(EnvType.CLIENT)
public class StencilFramebuffer extends Framebuffer {

    private int depthTex = -1;
    private int stencilRbo = -1;

    public StencilFramebuffer() {
        super(true);
        this.copyMainDimensions();
    }

    public void copyMainDimensions() {
        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        int width = main.textureWidth;
        int height = main.textureHeight;
        if (width != this.textureWidth || height != this.textureHeight) {
            this.resize(width, height);
        }
    }

    @Override
    public void initFbo(int width, int height) {
        RenderSystem.assertOnRenderThreadOrInit();

        int max = RenderSystem.maxSupportedTextureSize();
        if (width <= 0 || height <= 0 || width > max || height > max) {
            throw new IllegalArgumentException("Invalid framebuffer size: " + width + "x" + height);
        }

        this.viewportWidth = width;
        this.viewportHeight = height;
        this.textureWidth = width;
        this.textureHeight = height;
        this.fbo = GlStateManager.glGenFramebuffers();

        // === Create color texture ===
        this.colorAttachment = TextureUtil.generateTextureId();

        // === Create depth texture (separate) ===
        this.depthTex = TextureUtil.generateTextureId();
        GlStateManager._bindTexture(this.depthTex);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_NEAREST);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_NEAREST);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_COMPARE_MODE, 0);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_WRAP_S, GlConst.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_WRAP_T, GlConst.GL_CLAMP_TO_EDGE);
        GlStateManager._texImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT24, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT, null);

        this.texFilter = GlConst.GL_NEAREST;
        GlStateManager._bindTexture(this.colorAttachment);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, texFilter);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, texFilter);
        GlStateManager._bindTexture(0);

        GlStateManager._bindTexture(this.colorAttachment);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_WRAP_S, GlConst.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_WRAP_T, GlConst.GL_CLAMP_TO_EDGE);
        GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_RGBA8, width, height, 0, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, null);

        // === Create stencil renderbuffer (separate) ===
        this.stencilRbo = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, this.stencilRbo);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_STENCIL_INDEX8, width, height);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);

        // === Attach textures to framebuffer ===
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, this.fbo);
        GlStateManager._glFramebufferTexture2D(GlConst.GL_FRAMEBUFFER, GlConst.GL_COLOR_ATTACHMENT0, GlConst.GL_TEXTURE_2D, this.colorAttachment, 0);
        GlStateManager._glFramebufferTexture2D(GlConst.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GlConst.GL_TEXTURE_2D, this.depthTex, 0);
        GL30.glFramebufferRenderbuffer(GlConst.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, this.stencilRbo);

        this.checkFramebufferStatus();
        this.clear();
        this.endRead();
    }

    @Override
    public void clear() {
        RenderSystem.assertOnRenderThreadOrInit();
        this.beginWrite(true);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GlStateManager._clearColor(this.clearColor[0], this.clearColor[1], this.clearColor[2], this.clearColor[3]);
        GlStateManager._clearDepth(1.0);
        GlStateManager._clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        this.endWrite();
    }

    @Override
    public void delete() {
        super.delete();
        if (this.depthTex != -1) {
            TextureUtil.releaseTextureId(this.depthTex);
            this.depthTex = -1;
        }
        if (this.stencilRbo != -1) {
            GL30.glDeleteRenderbuffers(this.stencilRbo);
            this.stencilRbo = -1;
        }
    }

    @Override
    public int getDepthAttachment() {
        return this.depthTex;
    }

}
