package net.akashaverse.akashicrecords.items;

import net.akashaverse.akashicrecords.AkashicRecords;
import net.akashaverse.akashicrecords.items.custom.SelectionWandItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * Holds all custom items for the mod.  Items are registered via a
 * {@link DeferredRegister} at mod initialisation time.  We expose a
 * {@link #register(IEventBus)} method to hook into the mod event bus.
 */
public class MineItems {
    // Deferred register tied to the built‑in item registry and our mod id
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AkashicRecords.MOD_ID);

    // Our selection wand item.  We give it a custom behaviour defined in
    // SelectionWandItem.  The item only stacks to one, as players only need
    // one wand to define mines.
    public static final RegistryObject<Item> SELECTION_WAND = ITEMS.register(
            "selection_wand",
            () -> new SelectionWandItem(new Item.Properties().stacksTo(1))
    );

    /**
     * Register all deferred registers on the provided event bus.  This must
     * be called from the mod constructor.
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
