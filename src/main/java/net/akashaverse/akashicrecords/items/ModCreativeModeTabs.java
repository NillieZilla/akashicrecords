package net.akashaverse.akashicrecords.items;

import net.akashaverse.akashicrecords.AkashicRecords;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

/**
 * Defines the creative mode tab for AutoRefillingMine.  Items registered
 * under this mod will be displayed here.  The tab icon is set to the
 * selection wand.
 */
public class ModCreativeModeTabs {
    // Deferred register for creative mode tabs
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AkashicRecords.MOD_ID);

    // Our single creative tab
    public static final java.util.function.Supplier<CreativeModeTab> MINE_TAB = TABS.register(
            "mine_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + AkashicRecords.MOD_ID + ".mine"))
                    .icon(() -> new ItemStack(MineItems.SELECTION_WAND.get()))
                    .displayItems((enabledFeatures, output) -> {
                        output.accept(MineItems.SELECTION_WAND.get());
                    })
                    .build()
    );

    /**
     * Registers the creative tab with the provided event bus.
     */
    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}