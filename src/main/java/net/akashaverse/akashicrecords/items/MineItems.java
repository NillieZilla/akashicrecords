package net.akashaverse.akashicrecords.items;

import net.akashaverse.akashicrecords.AkashicRecords;
import net.akashaverse.akashicrecords.items.mine.SelectionWandItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class MineItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(AkashicRecords.MOD_ID);

    public static final Supplier<Item> SELECTION_WAND = ITEMS.registerItem(
            "selection_wand",
            SelectionWandItem::new,
            new Item.Properties().stacksTo(1)
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
