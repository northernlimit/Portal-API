package net.nhappy.portalapi.api;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.nhappy.portalapi.api.block.PortalBlock;
import net.nhappy.portalapi.api.block.PortalBlockEntity;
import net.nhappy.portalapi.impl.PortalAPI;

import java.util.function.BiFunction;
import java.util.function.Function;

public class PortalAPIInit {

    private static Block register(String id, Function<AbstractBlock.Settings, Block> blockFactory, AbstractBlock.Settings blockSettings, BiFunction<Block, Item.Settings, BlockItem> itemFactory, Item.Settings itemSettings) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, PortalAPI.id(id));
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, PortalAPI.id(id));

        Block block = blockFactory.apply(blockSettings.registryKey(blockKey));
        BlockItem item = itemFactory.apply(block, itemSettings.registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);
        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static Block register(String id, Function<AbstractBlock.Settings, Block> blockFactory, AbstractBlock.Settings blockSettings) {
        return register(id, blockFactory, blockSettings, BlockItem::new, new Item.Settings());
    }

    public static <T extends BlockEntityType<?>> T register(String id, T blockEntityType) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, PortalAPI.id(id), blockEntityType);
    }

    public static final Block PORTAL_BLOCK = register("portal_block", PortalBlock::new,
            AbstractBlock.Settings.copy(Blocks.GLASS)
                    .dropsNothing()
                    .noCollision()
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .noBlockBreakParticles()
                    .luminance(state -> state.get(PortalBlock.LINKED) ? 9 : 0));

    public static final BlockEntityType<PortalBlockEntity> PORTAL_BLOCK_ENTITY = register("portal_block",
            FabricBlockEntityTypeBuilder.create(PortalBlockEntity::new, PORTAL_BLOCK).build());

    public static void init() {
    }
}
