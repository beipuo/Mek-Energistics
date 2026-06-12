package com.beipuo.mekenergistics.block;

import com.beipuo.mekenergistics.blockentity.api.MeAeMachine;
import com.beipuo.mekenergistics.blockentity.api.MeFactoryAeMachine;
import com.beipuo.mekenergistics.blockentity.support.MeMemoryCardSettings;
import com.beipuo.mekenergistics.blockentity.support.MeOwnerHelper;
import com.beipuo.mekenergistics.blockentity.support.MePatternSlotTransfer;
import com.beipuo.mekenergistics.common.MeLangEntry;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import com.beipuo.mekenergistics.compat.eme.EvolvedMekanismCompat;
import com.beipuo.mekenergistics.compat.eme.EvolvedMekanismExtrasCompat;
import com.beipuo.mekenergistics.compat.meke.MekanismExtrasCompat;
import com.beipuo.mekenergistics.item.MeInstallerUpgradeHandler;
import com.beipuo.mekenergistics.item.MeTierInstallerItem;
import com.beipuo.mekenergistics.registry.ModBlockTypes;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.text.ILangEntry;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeCustomShape;
import mekanism.common.block.attribute.AttributeGui;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.block.attribute.AttributeState;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.block.interfaces.IHasDescription;
import mekanism.common.block.interfaces.ITypeBlock;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.resource.BlockResourceInfo;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.TileEntityUpdateable;
import mekanism.common.tile.interfaces.ITileRadioactive;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public class MeMekanismMachineBlock extends Block implements IHasDescription, ITypeBlock, IHasTileEntity<TileEntityMekanism> {
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
                .requiresCorrectToolForDrops()
                .mapColor(BlockResourceInfo.STEEL.getMapColor());
        BlockTypeTile<? extends TileEntityMekanism> blockType = ModBlockTypes.getMachineBlockType(machine);
        if (blockType != null) {
            for (Attribute attribute : blockType.getAll()) {
                attribute.adjustProperties(properties);
            }
        }
        if (machine == MeMekanismMachine.NUTRITIONAL_LIQUIFIER || blockType != null && blockType.has(AttributeCustomShape.class)) {
            properties.noOcclusion();
        }
        return properties;
    }

    public MeMekanismMachine getMachine() {
        return this.machine;
    }

    @Override
    public BlockType getType() {
        return ModBlockTypes.getMachineBlockType(this.machine);
    }

    @Override
    public ILangEntry getDescription() {
        return MeLangEntry.of(this.machine.descriptionKey());
    }

    @Override
    public MutableComponent getName() {
        TextColor color = this.machine.nameColor();
        return color == null ? super.getName() : TextComponentUtil.build(color, super.getName());
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AttributeCustomShape customShape = Attribute.get(state, AttributeCustomShape.class);
        if (customShape != null) {
            VoxelShape[] bounds = customShape.bounds();
            if (bounds.length == 1) {
                return bounds[0];
            }
            AttributeStateFacing attr = Attribute.get(state, AttributeStateFacing.class);
            int index = attr == null ? 0 : attr.getDirection(state).ordinal() - (attr.getFacingProperty() == BlockStateProperties.FACING ? 0 : 2);
            return bounds[index];
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getShape(state, level, pos, CollisionContext.empty());
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return !Attribute.has(state, AttributeCustomShape.class) && super.isPathfindable(state, pathComputationType);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        return AttributeStateFacing.rotate(state, level, pos, rotation);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return AttributeStateFacing.rotate(state, rotation);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return AttributeStateFacing.mirror(state, mirror);
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockEntity tile = level.getBlockEntity(pos);
        if (!(tile instanceof TileEntityMekanism mekanismTile)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        ItemInteractionResult memoryCardResult = MeMemoryCardSettings.use(stack, level, player, tile);
        if (memoryCardResult.consumesAction()) {
            return memoryCardResult;
        }
        if (!player.isShiftKeyDown() && isInstaller(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return genericClientActivated(stack, tile);
        }
        if (player.isShiftKeyDown()) {
            ItemInteractionResult installerResult = MeInstallerUpgradeHandler.tryUpgrade(stack, state, level, pos, player);
            if (installerResult.consumesAction()) {
                return installerResult;
            }
        }
        boolean shouldDropPatterns = shouldDropPatternsBeforeDismantle(stack, state, player, level, pos, mekanismTile);
        if (shouldDropPatterns) {
            MePatternSlotTransfer.dropAndClear(level, pos, mekanismTile);
        }
        ItemInteractionResult wrenchResult = mekanismTile.tryWrench(state, player, stack).getInteractionResult();
        if (wrenchResult.consumesAction()) {
            return wrenchResult;
        }
        return wrenchResult;
    }

    private static ItemInteractionResult genericClientActivated(ItemStack stack, BlockEntity tile) {
        if (MekanismUtils.canUseAsWrench(stack)) {
            if (tile instanceof ITileRadioactive radioactiveTile && radioactiveTile.getRadiationScale() > 0) {
                return ItemInteractionResult.FAIL;
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static boolean isInstaller(ItemStack stack) {
        if (stack.getItem() instanceof MeTierInstallerItem || stack.getItem() instanceof ItemTierInstaller) {
            return true;
        }
        if (ModList.get().isLoaded("mekanism_extras") && MekanismExtrasCompat.isInstaller(stack)) {
            return true;
        }
        if (ModList.get().isLoaded("evolvedmekanism") && EvolvedMekanismCompat.isInstaller(stack)) {
            return true;
        }
        return ModList.get().isLoaded("emextras") && EvolvedMekanismExtrasCompat.isInstaller(stack);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity tile = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (tile instanceof TileEntityUpdateable updateable) {
            for (ItemStack drop : drops) {
                if (drop.is(this.asItem())) {
                    drop.applyComponents(updateable.collectComponents());
                }
            }
        }
        return drops;
    }

    private static boolean shouldDropPatternsBeforeDismantle(ItemStack stack, BlockState state, Player player, Level level, BlockPos pos, TileEntityMekanism tile) {
        return player.isShiftKeyDown()
                && canDismantle(stack)
                && tile.getRadiationScale() <= 0
                && IBlockSecurityUtils.INSTANCE.canAccess(player, level, pos, state, tile);
    }

    private static boolean canDismantle(ItemStack stack) {
        return MekanismUtils.canUseAsWrench(stack);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        AttributeHasBounding hasBounding = Attribute.get(state, AttributeHasBounding.class);
        if (hasBounding != null) {
            hasBounding.placeBoundingBlocks(level, pos, state);
        }
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

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            AttributeHasBounding hasBounding = Attribute.get(state, AttributeHasBounding.class);
            if (hasBounding != null) {
                hasBounding.removeBoundingBlocks(level, pos, state);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
