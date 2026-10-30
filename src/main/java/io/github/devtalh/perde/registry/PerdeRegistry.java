package io.github.devtalh.perde.registry;

import io.github.devtalh.perde.block.PerdeBlock;
import io.github.devtalh.perde.block.PerdeBlockEntity;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class PerdeRegistry {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("perde", "perde");
    public static final PerdeBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, ID,
            new PerdeBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, ID))
                    .strength(2.0F).sound(SoundType.COPPER)));
    public static final BlockItem ITEM = Registry.register(BuiltInRegistries.ITEM, ID,
            new BlockItem(BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ID)).useBlockDescriptionPrefix()));
    public static final BlockEntityType<PerdeBlockEntity> BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ID,
            FabricBlockEntityTypeBuilder.create(PerdeBlockEntity::new, BLOCK).build());

    private PerdeRegistry() { }

    public static void initialize() {
        ITEM.registerBlocks(Item.BY_BLOCK, ITEM);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(ITEM));
    }
}
