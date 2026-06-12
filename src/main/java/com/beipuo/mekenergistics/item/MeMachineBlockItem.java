package com.beipuo.mekenergistics.item;

import java.util.List;
import java.util.Map.Entry;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.Upgrade;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.security.IItemSecurityUtils;
import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.client.key.MekKeyHandler;
import mekanism.client.key.MekanismKeyHandler;
import mekanism.common.MekanismLang;
import mekanism.common.attachments.IAttachmentAware;
import mekanism.common.attachments.component.UpgradeAware;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.energy.ComponentBackedNoClampEnergyContainer;
import mekanism.common.attachments.containers.energy.EnergyContainersBuilder;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeEnergy;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.attribute.Attributes.AttributeInventory;
import mekanism.common.block.attribute.Attributes.AttributeSecurity;
import mekanism.common.capabilities.ICapabilityAware;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.security.SecurityObject;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.WorldUtils;
import mekanism.common.util.text.BooleanStateDisplay.YesNo;
import mekanism.common.util.text.UpgradeDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.beipuo.mekenergistics.block.MeMekanismMachineBlock;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import org.jetbrains.annotations.NotNull;

public class MeMachineBlockItem extends BlockItem implements ICapabilityAware, IAttachmentAware {
    public MeMachineBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void onDestroyed(@NotNull ItemEntity item, @NotNull DamageSource damageSource) {
        InventoryUtils.dropItemContents(item, damageSource);
    }

    @Override
    public boolean placeBlock(@NotNull BlockPlaceContext context, @NotNull BlockState state) {
        AttributeHasBounding hasBounding = Attribute.get(state, AttributeHasBounding.class);
        if (hasBounding == null) {
            return super.placeBlock(context, state);
        }
        return hasBounding.handle(context.getLevel(), context.getClickedPos(), state, context,
                (level, pos, ctx) -> WorldUtils.isValidReplaceableBlock(level, ctx, pos)) && super.placeBlock(context, state);
    }

    @NotNull
    @Override
    public Component getName(@NotNull ItemStack stack) {
        if (getBlock() instanceof MeMekanismMachineBlock block) {
            TextColor color = block.getMachine().nameColor();
            if (color != null) {
                return TextComponentUtil.build(color, super.getName(stack));
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (getBlock() instanceof MeMekanismMachineBlock block) {
            if (MekKeyHandler.isKeyPressed(MekanismKeyHandler.descriptionKey)) {
                tooltip.add(block.getDescription().translate());
            } else if (MekKeyHandler.isKeyPressed(MekanismKeyHandler.detailsKey)) {
                addDetails(block, stack, tooltip);
            } else {
                tooltip.add(MekanismLang.HOLD_FOR_DETAILS.translateColored(EnumColor.GRAY, EnumColor.INDIGO,
                        MekanismKeyHandler.detailsKey.getTranslatedKeyMessage()));
                tooltip.add(MekanismLang.HOLD_FOR_DESCRIPTION.translateColored(EnumColor.GRAY, EnumColor.AQUA,
                        MekanismKeyHandler.descriptionKey.getTranslatedKeyMessage()));
            }
        }
    }

    private static void addDetails(MeMekanismMachineBlock block, ItemStack stack, List<Component> tooltip) {
        MeMekanismMachine machine = block.getMachine();
        IItemSecurityUtils.INSTANCE.addSecurityTooltip(stack, tooltip);
        FactoryType factoryType = machine.factoryType();
        if (factoryType != null && machine.isFactory()) {
            tooltip.add(MekanismLang.FACTORY_TYPE.translateColored(EnumColor.INDIGO, EnumColor.GRAY, factoryType));
        }
        if (exposesEnergyCapOrTooltips(block)) {
            StorageUtils.addStoredEnergy(stack, tooltip, false);
        }
        if (Attribute.has(block, AttributeInventory.class) && ContainerType.ITEM.supports(stack)) {
            tooltip.add(MekanismLang.HAS_INVENTORY.translateColored(EnumColor.AQUA, EnumColor.GRAY, YesNo.hasInventory(stack)));
        }
        if (Attribute.has(block, AttributeUpgradeSupport.class)) {
            UpgradeAware upgradeAware = stack.get(MekanismDataComponents.UPGRADES);
            if (upgradeAware != null) {
                for (Entry<Upgrade, Integer> entry : upgradeAware.upgrades().entrySet()) {
                    tooltip.add(UpgradeDisplay.of(entry.getKey(), entry.getValue()).getTextComponent());
                }
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        if (exposesEnergyCapOrTooltips()) {
            return slotChanged || oldStack.getItem() != newStack.getItem();
        }
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    @Override
    public boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        if (exposesEnergyCapOrTooltips()) {
            return oldStack.getItem() != newStack.getItem();
        }
        return super.shouldCauseBlockBreakReset(oldStack, newStack);
    }

    protected Predicate<@NotNull AutomationType> getEnergyCapInsertPredicate() {
        return ConstantPredicates.alwaysTrue();
    }

    protected boolean exposesEnergyCap() {
        return exposesEnergyCapOrTooltips();
    }

    protected boolean exposesEnergyCapOrTooltips() {
        return getBlock() instanceof MeMekanismMachineBlock block && exposesEnergyCapOrTooltips(block);
    }

    private static boolean exposesEnergyCapOrTooltips(MeMekanismMachineBlock block) {
        return Attribute.has(block, AttributeEnergy.class);
    }

    protected EnergyContainersBuilder addDefaultEnergyContainers(EnergyContainersBuilder builder) {
        Block block = getBlock();
        AttributeEnergy attributeEnergy = Attribute.get(block, AttributeEnergy.class);
        if (attributeEnergy == null) {
            throw new IllegalStateException("Expected block " + block + " to have the energy attribute");
        }
        LongSupplier maxEnergy = attributeEnergy::getStorage;
        if (Attribute.matches(block, AttributeUpgradeSupport.class, attribute -> attribute.supportedUpgrades().contains(Upgrade.ENERGY))) {
            return builder.addContainer((type, attachedTo, containerIndex) -> {
                LongSupplier capacity = new UpgradeBasedUnsignedLongCache(attachedTo, maxEnergy);
                return new ComponentBackedNoClampEnergyContainer(attachedTo, containerIndex, BasicEnergyContainer.manualOnly, getEnergyCapInsertPredicate(),
                        () -> MekanismUtils.calculateUsage(capacity.getAsLong()), capacity);
            });
        }
        return builder.addBasic(BasicEnergyContainer.manualOnly, getEnergyCapInsertPredicate(),
                () -> MekanismUtils.calculateUsage(maxEnergy.getAsLong()), maxEnergy);
    }

    @Override
    public void attachCapabilities(RegisterCapabilitiesEvent event) {
        if (Attribute.has(getBlock(), AttributeSecurity.class)) {
            event.registerItem(IItemSecurityUtils.INSTANCE.ownerCapability(), (stack, ctx) -> new SecurityObject(stack), this);
            event.registerItem(IItemSecurityUtils.INSTANCE.securityCapability(), (stack, ctx) -> new SecurityObject(stack), this);
        }
    }

    @Override
    public void attachAttachments(IEventBus eventBus) {
        if (Attribute.has(getBlock(), AttributeEnergy.class)) {
            IEventBus energyEventBus = exposesEnergyCap() ? eventBus : null;
            ContainerType.ENERGY.addDefaultCreators(energyEventBus, this,
                    () -> addDefaultEnergyContainers(EnergyContainersBuilder.builder()).build(),
                    MekanismConfig.storage, MekanismConfig.usage);
        }
    }

    private static class UpgradeBasedUnsignedLongCache implements LongSupplier {
        private final ItemStack stack;
        private final LongSupplier baseStorage;
        private int lastInstalled;
        private long value;

        private UpgradeBasedUnsignedLongCache(ItemStack stack, LongSupplier baseStorage) {
            this.stack = stack;
            this.baseStorage = baseStorage;
            UpgradeAware upgradeAware = stack.getOrDefault(MekanismDataComponents.UPGRADES, UpgradeAware.EMPTY);
            this.lastInstalled = upgradeAware.getUpgradeCount(Upgrade.ENERGY);
            this.value = MekanismUtils.getMaxEnergy(this.lastInstalled, this.baseStorage.getAsLong());
        }

        @Override
        public long getAsLong() {
            UpgradeAware upgradeAware = stack.getOrDefault(MekanismDataComponents.UPGRADES, UpgradeAware.EMPTY);
            int installed = upgradeAware.getUpgradeCount(Upgrade.ENERGY);
            if (installed != lastInstalled) {
                lastInstalled = installed;
                value = MekanismUtils.getMaxEnergy(this.lastInstalled, baseStorage.getAsLong());
            }
            return value;
        }
    }
}
