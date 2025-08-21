package net.nhappy.portalapi.impl.duck;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

@Environment(EnvType.CLIENT)
public interface CameraAccessor {
    void prepareCamera(boolean ready, BlockView area, Entity focusedEntity, boolean thirdPerson,
                       float lastTickDelta, float yaw, float pitch, Vec3d pos, float cameraY);
}
