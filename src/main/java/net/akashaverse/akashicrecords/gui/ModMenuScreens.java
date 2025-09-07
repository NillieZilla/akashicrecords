package net.akashaverse.akashicrecords.gui;

import net.akashaverse.akashicrecords.AkashicRecords;
import net.akashaverse.akashicrecords.gui.client.MineMainScreen;
import net.akashaverse.akashicrecords.gui.client.MineTypeCreationScreen;
import net.akashaverse.akashicrecords.gui.client.BlockSelectionScreen;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Registers screen factories for the mod's menu types.  In NeoForge 21.x,
 * screens must be registered via the {@link RegisterMenuScreensEvent} fired on
 * the mod event bus rather than directly using {@code MenuScreens.register}.
 *
 * <p>This class uses the {@link EventBusSubscriber} annotation to
 * automatically subscribe the event handler to the mod bus.  The handler
 * registers the {@link MineMainScreen} as the screen for the corresponding
 * {@link net.minecraft.world.inventory.MenuType} defined in
 * {@link ModMenuTypes#MINE_MAIN_MENU}.  Registration only occurs on the
 * physical client because the {@code RegisterMenuScreensEvent} is not fired
 * on dedicated servers.</p>
 */
@EventBusSubscriber(modid = AkashicRecords.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModMenuScreens {

    private ModMenuScreens() {
        // utility class; no instantiation
    }

    /**
     * Registers our container screens when the {@link RegisterMenuScreensEvent}
     * fires on the mod bus.  This associates each {@link net.minecraft.world.inventory.MenuType}
     * with its screen constructor on the client.
     *
     * @param event the registration event
     */
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        // Register the main mine management screen
        event.register(ModMenuTypes.MINE_MAIN_MENU.get(), MineMainScreen::new);
        // Register the mine type creation screen
        event.register(ModMenuTypes.MINE_TYPE_CREATION_MENU.get(),
                MineTypeCreationScreen::new);
        // Register the block selection screen
        event.register(ModMenuTypes.BLOCK_SELECTION_MENU.get(),
                BlockSelectionScreen::new);
    }
}