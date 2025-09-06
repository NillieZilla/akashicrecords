package net.akashaverse.akashicrecords.items;

import net.akashaverse.akashicrecords.AkashicRecords;
import net.akashaverse.akashicrecords.items.custom.SelectionWandItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.Item;

public class MineItems {
    // create a specialised deferred register for items
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(AkashicRecords.MOD_ID);

    // register your selection wand; registerItem supplies an Item.Properties object separately
    public static final java.util.function.Supplier<Item> SELECTION_WAND = ITEMS.registerItem(
            "selection_wand",
            SelectionWandItem::new,
            new Item.Properties().stacksTo(1)
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
