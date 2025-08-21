package net.nhappy.portalapi.impl.duck;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;

@Environment(EnvType.CLIENT)
public interface GameRendererAccessor {

    boolean doesRenderHand();

    void setCamera(Camera newCamera);

}
