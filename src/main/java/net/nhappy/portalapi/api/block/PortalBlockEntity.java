package net.nhappy.portalapi.api.block;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.nhappy.portalapi.api.PortalAPIInit;

import java.util.List;
import java.util.Optional;

public class PortalBlockEntity extends BlockEntity {

    private long age = 0L;
    private BlockPos linkedPos = new BlockPos(0, 0, 0);

    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(PortalAPIInit.PORTAL_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putLong("Age", this.age);
        if (this.linkedPos != null) {
            nbt.put("LinkedPos", NbtHelper.fromBlockPos(this.linkedPos));
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        this.age = nbt.getLong("Age");
        Optional<BlockPos> blockPos = NbtHelper.toBlockPos(nbt, "LinkedPos");
        if (blockPos.isPresent() && World.isValid(blockPos.get())) {
            this.linkedPos = blockPos.get();
        }
    }

    public BlockPos getLinkedPos() {
        return this.linkedPos;
    }

    public static void tick(World world, BlockPos pos, BlockState state, PortalBlockEntity blockEntity) {
        if (!world.isClient) {
            ++blockEntity.age;
            if (blockEntity.getLinkedPos() != null) {
                List<Entity> list = world.getEntitiesByClass(Entity.class, new Box(pos).expand(0, 1, 0), PortalBlockEntity::canTeleport);
                if (!list.isEmpty()) {
                    tryTeleportingEntity(world, pos, state, list.get(world.random.nextInt(list.size())), blockEntity);
                }
            }
        }
    }

    public static void tryTeleportingEntity(World world, BlockPos pos, BlockState state,
                                            Entity entity, PortalBlockEntity blockEntity) {
        if (world instanceof ServerWorld serverWorld) {
            Entity finalEntity;
            if (entity instanceof EnderPearlEntity pearl) {
                Entity pearlOwner = pearl.getOwner();
                if (pearlOwner instanceof ServerPlayerEntity) {
                    Criteria.ENTER_BLOCK.trigger((ServerPlayerEntity) pearlOwner, state);
                }

                if (pearlOwner != null) {
                    finalEntity = pearlOwner;
                    entity.discard();
                } else {
                    finalEntity = entity;
                }
            } else {
                finalEntity = entity.getRootVehicle();
            }

            finalEntity.resetPortalCooldown();

            BlockPos linkedPos = blockEntity.getLinkedPos();
            Vec3d distanceToExit = entity.getPos().subtract(pos.getX(), pos.getY(), pos.getZ());
            Vec3d teleportPos = distanceToExit.add(linkedPos.getX(), linkedPos.getY(), linkedPos.getZ());
            TeleportTarget teleportTarget = new TeleportTarget(
                    serverWorld, teleportPos, finalEntity.getVelocity(), finalEntity.getYaw(),
                    finalEntity.getPitch(), TeleportTarget.NO_OP);

            finalEntity.teleportTo(teleportTarget);
        }
    }

    public static boolean canTeleport(Entity entity) {
        return EntityPredicates.EXCEPT_SPECTATOR.test(entity) && !entity.getRootVehicle().hasPortalCooldown();
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return this.createNbt(registryLookup);
    }

    public boolean shouldDrawSide(Direction direction) {
        return Block
                .shouldDrawSide(this.getCachedState(), this.world.getBlockState(direction
                .equals(Direction.UP) ? this.getPos().up(2) : this.getPos().offset(direction)), direction);
    }

}
