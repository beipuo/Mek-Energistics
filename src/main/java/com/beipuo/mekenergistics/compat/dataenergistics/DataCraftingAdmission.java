package com.beipuo.mekenergistics.compat.dataenergistics;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.beipuo.mekenergistics.blockentity.support.AbstractMeAeSupport;
import com.beipuo.mekenergistics.blockentity.support.io.MeCountedInputAdmission;
import com.fish_dan_.data_energistics.api.crafting.dispatch.CountedCraftingAdmission;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.commit.CountedCraftingPreparation;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.model.CraftingDispatchTarget;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.model.CraftingDispatchTargetAvailability;
import org.jetbrains.annotations.Nullable;

public final class DataCraftingAdmission {
    private DataCraftingAdmission() {
    }

    @Nullable
    public static CountedCraftingAdmission prepare(AbstractMeAeSupport<?> support,
            IPatternDetails patternDetails, KeyCounter[] prototype, long requestedCount) {
        if (support == null || patternDetails == null || prototype == null || requestedCount <= 0
                || support.isPatternBusy() || !support.hasRegisteredPattern(patternDetails)) {
            return null;
        }
        if (!MeCountedInputAdmission.validPrototype(prototype)) {
            return null;
        }
        MeCountedInputAdmission admission = MeCountedInputAdmission.prepare(prototype, requestedCount,
                support::maxAcceptedCopies, scaled -> support.routeDataPatternInputs(scaled));
        return admission == null ? null : new Adapter(admission);
    }

    public static CountedCraftingPreparation prepareTargetAware(AbstractMeAeSupport<?> support,
            IPatternDetails patternDetails, KeyCounter[] prototype, long requestedCount,
            CraftingDispatchTargetAvailability availability) {
        if (availability == null || !availability.canAttempt(CraftingDispatchTarget.provider())) {
            return CountedCraftingPreparation.rejected(java.util.List.of());
        }
        CountedCraftingAdmission admission = prepare(support, patternDetails, prototype, requestedCount);
        return admission == null
                ? CountedCraftingPreparation.rejected(java.util.List.of())
                : CountedCraftingPreparation.accepted(admission, CraftingDispatchTarget.provider());
    }

    private static final class Adapter implements CountedCraftingAdmission {
        private final MeCountedInputAdmission admission;

        private Adapter(MeCountedInputAdmission admission) {
            this.admission = admission;
        }

        @Override
        public long count() {
            return admission.count();
        }

        @Override
        public boolean hasTransferredInputOwnership() {
            return admission.hasTransferredInputOwnership();
        }

        @Override
        public boolean commit(KeyCounter[] prototype) {
            return admission.commit(prototype);
        }
    }
}
