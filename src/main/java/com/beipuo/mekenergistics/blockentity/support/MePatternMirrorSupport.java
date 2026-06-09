package com.beipuo.mekenergistics.blockentity.support;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import com.beipuo.mekenergistics.blockentity.api.MePatternMirrorOwner;
import com.beipuo.mekenergistics.blockentity.api.PatternMirrorRole;
import com.beipuo.mekenergistics.common.machine.MeMekanismMachine;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableInt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public final class MePatternMirrorSupport {
    private static final String TAG_CHANNEL_CARD_INSTALLED = "PatternMirrorChannelCard";
    private static final String TAG_CHANNEL = "PatternMirrorChannel";
    private static final String TAG_ROLE = "PatternMirrorRole";
    private static final int MIN_CHANNEL = 1;
    private static final int MAX_CHANNEL = 16;
    private static final List<WeakReference<MePatternMirrorSupport>> SUPPORTS = new ArrayList<>();

    private final MePatternMirrorOwner owner;
    private boolean channelCardInstalled;
    private int channel = MIN_CHANNEL;
    private PatternMirrorRole role = PatternMirrorRole.OFF;

    public MePatternMirrorSupport(MePatternMirrorOwner owner) {
        this.owner = owner;
        register(this);
    }

    public boolean isChannelCardInstalled() {
        return this.channelCardInstalled;
    }

    public boolean installChannelCard() {
        if (this.channelCardInstalled) {
            return false;
        }
        this.channelCardInstalled = true;
        updateGroup();
        this.owner.saveChanges();
        return true;
    }

    public int channel() {
        return this.channel;
    }

    public PatternMirrorRole role() {
        return this.role;
    }

    public void setClientChannelCardInstalled(boolean installed) {
        this.channelCardInstalled = installed;
    }

    public void setClientChannel(int channel) {
        this.channel = sanitizeChannel(channel);
    }

    public void setClientRole(int role) {
        this.role = PatternMirrorRole.byId(role);
    }

    public void addContainerTrackers(MekanismContainer container) {
        container.track(SyncableBoolean.create(this::isChannelCardInstalled, this::setClientChannelCardInstalled));
        container.track(SyncableInt.create(this::channel, this::setClientChannel));
        container.track(SyncableInt.create(() -> this.role.ordinal(), this::setClientRole));
    }

    public void setConfig(int channel, PatternMirrorRole role) {
        int sanitizedChannel = sanitizeChannel(channel);
        PatternMirrorRole sanitizedRole = role == null ? PatternMirrorRole.OFF : role;
        if (this.channel == sanitizedChannel && this.role == sanitizedRole) {
            return;
        }
        this.channel = sanitizedChannel;
        this.role = sanitizedRole;
        updateGroup();
        this.owner.saveChanges();
    }

    public void cycleRole() {
        setConfig(this.channel, this.role.next());
    }

    public void setChannel(int channel) {
        setConfig(channel, this.role);
    }

    public List<IPatternDetails> getEffectivePatterns() {
        if (!isMirrorEnabled()) {
            return this.owner.getLocalAvailablePatterns();
        }
        if (this.role == PatternMirrorRole.MASTER) {
            return this.owner.getLocalAvailablePatterns();
        }
        MePatternMirrorSupport master = findSingleMaster();
        return master == null ? Collections.emptyList() : master.owner.getLocalAvailablePatterns();
    }

    public void updateGroup() {
        this.owner.requestPatternMirrorUpdate();
        String groupKey = groupKey();
        if (groupKey == null) {
            return;
        }
        IGrid grid = grid();
        Level level = this.owner.getOwnerLevel();
        for (MePatternMirrorSupport support : liveSupports()) {
            if (support != this && support.matches(level, grid, groupKey, this.channel)) {
                support.owner.requestPatternMirrorUpdate();
            }
        }
    }

    public void save(CompoundTag tag) {
        tag.putBoolean(TAG_CHANNEL_CARD_INSTALLED, this.channelCardInstalled);
        tag.putInt(TAG_CHANNEL, this.channel);
        tag.putInt(TAG_ROLE, this.role.ordinal());
    }

    public void load(CompoundTag tag) {
        this.channelCardInstalled = tag.getBoolean(TAG_CHANNEL_CARD_INSTALLED);
        this.channel = sanitizeChannel(tag.contains(TAG_CHANNEL) ? tag.getInt(TAG_CHANNEL) : MIN_CHANNEL);
        this.role = PatternMirrorRole.byId(tag.getInt(TAG_ROLE));
    }

    public String groupKey() {
        MeMekanismMachine machine = this.owner.getMachine();
        if (machine == null) {
            return null;
        }
        if (machine.customFactoryTypeName() != null) {
            return "factory:" + machine.customFactoryTypeName();
        }
        FactoryType factoryType = machine.factoryType();
        if (machine.isFactory() && factoryType != null) {
            return "factory:" + factoryType.name().toLowerCase(java.util.Locale.ROOT);
        }
        if (machine.moreMachineFactoryTypeName() != null) {
            return "mekmm_factory:" + machine.moreMachineFactoryTypeName();
        }
        if (machine.moreMachineAdvancedFactoryTypeName() != null) {
            return "mekmm_advanced_factory:" + machine.moreMachineAdvancedFactoryTypeName();
        }
        return "machine:" + machine.registryName();
    }

    private boolean isMirrorEnabled() {
        return this.channelCardInstalled && this.role != PatternMirrorRole.OFF;
    }

    private MePatternMirrorSupport findSingleMaster() {
        String groupKey = groupKey();
        IGrid grid = grid();
        Level level = this.owner.getOwnerLevel();
        MePatternMirrorSupport found = null;
        for (MePatternMirrorSupport support : liveSupports()) {
            if (!support.matches(level, grid, groupKey, this.channel) || support.role != PatternMirrorRole.MASTER) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = support;
        }
        return found;
    }

    private boolean matches(Level level, IGrid grid, String groupKey, int channel) {
        return this.channelCardInstalled
                && this.channel == channel
                && this.owner.getOwnerLevel() == level
                && this.grid() == grid
                && groupKey != null
                && groupKey.equals(groupKey());
    }

    private IGrid grid() {
        return this.owner.getMainNode().getGrid();
    }

    private static int sanitizeChannel(int channel) {
        return Math.max(MIN_CHANNEL, Math.min(MAX_CHANNEL, channel));
    }

    private static void register(MePatternMirrorSupport support) {
        cleanup();
        SUPPORTS.add(new WeakReference<>(support));
    }

    private static List<MePatternMirrorSupport> liveSupports() {
        cleanup();
        List<MePatternMirrorSupport> supports = new ArrayList<>();
        for (WeakReference<MePatternMirrorSupport> reference : SUPPORTS) {
            MePatternMirrorSupport support = reference.get();
            if (support != null) {
                supports.add(support);
            }
        }
        return supports;
    }

    private static void cleanup() {
        Iterator<WeakReference<MePatternMirrorSupport>> iterator = SUPPORTS.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().get() == null) {
                iterator.remove();
            }
        }
    }
}
