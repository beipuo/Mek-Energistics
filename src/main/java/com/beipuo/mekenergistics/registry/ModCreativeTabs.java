package com.beipuo.mekenergistics.registry;

import com.beipuo.mekenergistics.MekEnergistics;
import com.beipuo.mekenergistics.common.MeMekanismMachine;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MekEnergistics.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mekenergistics"))
                    .icon(() -> ModItems.ME_METALLURGIC_INFUSER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (var item : ModItems.getMachineItems()) {
                            if (item == ModItems.getMachineItem(MeMekanismMachine.SEISMIC_VIBRATOR)) {
                                continue;
                            }
                            output.accept(item.get());
                        }
                        output.accept(ModItems.ME_FACTORY_INSTALLER.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
