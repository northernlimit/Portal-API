package net.nhappy.portalapi.impl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.nhappy.portalapi.api.PortalAPIInit;
import net.nhappy.portalapi.api.client.PortalRenderer;

@Environment(EnvType.CLIENT)
public class PortalAPIClient implements ClientModInitializer {

    public static ShaderProgramKey RENDERTYPE_PORTAL;

    @Override
    public void onInitializeClient() {
        RENDERTYPE_PORTAL = ShaderProgramKeys.register("rendertype_portal", VertexFormats.POSITION);
        BlockRenderLayerMap.INSTANCE.putBlock(PortalAPIInit.PORTAL_BLOCK, RenderLayer.getCutout());
        BlockEntityRendererFactories.register(PortalAPIInit.PORTAL_BLOCK_ENTITY, PortalRenderer::new);
    }

}
