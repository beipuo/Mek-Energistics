package com.beipuo.mekenergistics.block;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.registry.ModBlockTypes;
import java.util.ArrayList;
import java.util.List;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeGui;
import mekanism.common.block.attribute.AttributeState;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.block.interfaces.ITypeBlock;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.TileEntityUpdateable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MeMekanismMachineBlock extends Block implements ITypeBlock, IHasTileEntity<TileEntityMekanism> {
    private static final List<AttributeState> STATE_ATTRIBUTES = List.of(new AttributeStateFacing(), (AttributeState) Attributes.ACTIVE);
    private final MeMekanismMachine machine;

    public MeMekanismMachineBlock(MeMekanismMachine machine) {
        super(properties(machine));
        this.machine = machine;
        this.registerDefaultState(STATE_ATTRIBUTES.stream()
                .reduce(this.stateDefinition.any(), (state, attribute) -> attribute.getDefaultState(state), (left, right) -> right));
    }

    private static BlockBehaviour.Properties properties(MeMekanismMachine machine) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .strength(3.5F, 16.0F)
                .requiresCorrectToolForDrops();
        BlockTypeTile<? extends TileEntityMekanism> blockType = ModBlockTypes.getMachineBlockType(machine);
        if (blockType != null) {
            for (Attribute attribute : blockType.getAll()) {
                attribute.adjustProperties(properties);
            }
        }
        return machine.isFactory() || machine == MeMekanismMachine.NUTRITIONAL_LIQUIFIER ? properties.noOcclusion() : properties;
    }

    public MeMekanismMachine getMachine() {
        return this.machine;
    }

    @Override
    public BlockType getType() {
        return ModBlockTypes.getMachineBlockType(this.machine);
    }

    @Override
    public TileEntityTypeRegistryObject<? extends TileEntityMekanism> getTileType() {
        return ModBlockTypes.getMachineBlockType(this.machine).getTileType();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        for (AttributeState attribute : STATE_ATTRIBUTES) {
            state = attribute.getStateForPlacement(state, context.getLevel(), context.getClickedPos(), context.getPlayer(), context.getClickedFace());
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        List<Property<?>> properties = new ArrayList<>();
        STATE_ATTRIBUTES.forEach(attribute -> attribute.fillBlockStateContainer(this, properties));
        properties.forEach(builder::add);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof TileEntityMekanism tile) {
            if (level.isClientSide) {
                return Attribute.has(this, AttributeGui.class) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            return tile.openGui(player);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof TileEntityUpdateable updateable) {
            updateable.onAdded();
        }
        if (!level.isClientSide && placer instanceof ServerPlayer player) {
            if (tile instanceof MeAeMachine machineBlockEntity) {
                machineBlockEntity.setOwner(player);
            } else if (tile instanceof MeFactoryAeMachine machineBlockEntity) {
                machineBlockEntity.setOwner(player);
            } else if (tile instanceof TileEntityMekanism mekanismTile) {
                MeOwnerHelper.claimMekanismOwnerIfMissing(mekanismTile, player);
            }
        }
    }
}
