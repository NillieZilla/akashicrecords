package net.akashaverse.akashicrecords.gui;

import net.akashaverse.akashicrecords.AkashicRecords;
import net.akashaverse.akashicrecords.gui.menu.MineMainMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/**
 * Holds and registers all menu (container) types used by the AkashicRecords mod.  The
 * {@link #MINE_MAIN_MENU} is used for the main mine management interface.  Each
 * menu type must be registered on the mod event bus during mod construction.
 */
public class ModMenuTypes {
    /**
     * Deferred register for menu types.  We use the vanilla Registries.MENU
     * registry to register our custom containers.  The mod id comes from
     * {@link AkashicRecords#MOD_ID}.
     */
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AkashicRecords.MOD_ID);

    /**
     * The main menu used to manage mines.  It opens when the player runs
     * the `/mine` command with no arguments.  See {@link MineMainMenu}
     * for details.
     */
    /**
     * The main menu used to manage mines.  It opens when the player runs
     * the `/mine` command with no arguments.  See {@link MineMainMenu}
     * for details.
     *
     * <p>We use a {@link Supplier} here instead of the NeoForge-specific
     * {@code RegistrySupplier} because {@link DeferredRegister#register(String, Supplier)}
     * returns a plain {@code Supplier} in NeoForge 21.x.  The supplier’s {@link Supplier#get()} method
     * returns the registered {@link MenuType} instance once the registry has been populated.</p>
     */
    public static final Supplier<MenuType<MineMainMenu>> MINE_MAIN_MENU =
            MENUS.register("mine_main_menu",
                    () -> new MenuType<>(MineMainMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /**
     * Menu for creating new mine types.  This menu allows the player to set
     * interval, warning time, select blocks, and save the configuration.
     */
    public static final Supplier<MenuType<net.akashaverse.akashicrecords.gui.menu.MineTypeCreationMenu>> MINE_TYPE_CREATION_MENU =
            MENUS.register("mine_type_creation_menu",
                    () -> new MenuType<>(net.akashaverse.akashicrecords.gui.menu.MineTypeCreationMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /**
     * Menu for selecting blocks from the registry.  Displays blocks in pages.
     */
    public static final Supplier<MenuType<net.akashaverse.akashicrecords.gui.menu.BlockSelectionMenu>> BLOCK_SELECTION_MENU =
            MENUS.register("block_selection_menu",
                    () -> new MenuType<>(net.akashaverse.akashicrecords.gui.menu.BlockSelectionMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /**
     * Register all menu types on the given event bus.  Call this during mod
     * construction from the mod class.
     *
     * @param eventBus the mod event bus
     */
    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}