package net.nhappy.portalapi.impl.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.nhappy.portalapi.impl.duck.CameraAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class CameraMixin implements CameraAccessor {

    @Shadow private boolean ready;

    @Shadow private BlockView area;

    @Shadow private Entity focusedEntity;

    @Shadow private boolean thirdPerson;

    @Shadow private float lastTickDelta;

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Shadow protected abstract void setPos(double x, double y, double z);

    @Shadow private float lastCameraY;

    @Shadow private float cameraY;

    @Override
    public void prepareCamera(boolean ready, BlockView area, Entity focusedEntity, boolean thirdPerson, float lastTickDelta, float yaw, float pitch, Vec3d pos, float newY) {
        this.ready = ready;
        this.area = area;
        this.focusedEntity = focusedEntity;
        this.thirdPerson = thirdPerson;
        this.lastTickDelta = lastTickDelta;
        this.setRotation(yaw, pitch);
        this.setPos(
                pos.x,
                pos.y,
                pos.z
        );
        this.lastCameraY = this.cameraY = newY;
    }

}
