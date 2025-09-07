package net.akashaverse.akashicrecords.gui.menu;

import net.akashaverse.akashicrecords.gui.ModMenuTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

/**
 * Menu for selecting a block from all registered blocks.  Uses paging to
 * display 27 blocks at a time.  Click a block to select it and return to
 * the mine type creation menu.  Two arrow items at slots 27 and 35 allow
 * navigation between pages.
 */
public class BlockSelectionMenu extends AbstractContainerMenu {
    private final Container container;
    private int page = 0;
    private final List<ItemStack> blockStacks = new ArrayList<>();

    public BlockSelectionMenu(int id, Inventory playerInventory) {
        this(ModMenuTypes.BLOCK_SELECTION_MENU.get(), id, playerInventory);
    }

    public BlockSelectionMenu(MenuType<?> type, int id, Inventory playerInventory) {
        super(type, id);
        this.container = new SimpleContainer(36);
        // gather all block stacks
        BuiltInRegistries.BLOCK.stream().forEach(block -> {
            ItemStack stack = new ItemStack(block.asItem());
            if (!stack.isEmpty()) {
                blockStacks.add(stack);
            }
        });
        updatePage();
    }

    private void updatePage() {
        // Fill with glass panes
        ItemStack filler = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, filler.copy());
        }
        // Show 27 blocks per page (slots 0–26)
        int start = page * 27;
        for (int i = 0; i < 27; i++) {
            int idx = start + i;
            if (idx < blockStacks.size()) {
                ItemStack stack = blockStacks.get(idx).copy();
                stack.setHoverName(stack.getHoverName());
                container.setItem(i, stack);
            }
        }
        // Previous page arrow at slot 27
        ItemStack prev = new ItemStack(Items.ARROW);
        
        container.setItem(27, prev);
        // Next page arrow at slot 35
        ItemStack next = new ItemStack(Items.ARROW);
        
        container.setItem(35, next);
    }

    public Container getContainer() {
        return container;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (clickType != ClickType.PICKUP || player.level().isClientSide) {
            super.clicked(slotId, dragType, clickType, player);
            return;
        }
        if (slotId >= 0 && slotId < 27) {
            int idx = page * 27 + slotId;
            if (idx < blockStacks.size()) {
                ItemStack stack = blockStacks.get(idx);
                String id = BuiltInRegistries.BLOCK.getKey(((BlockItem) stack.getItem()).getBlock()).toString();
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    // open the previous menu and add the block via its method
                    sp.openMenu(new net.minecraft.world.inventory.MenuProvider() {
                        @Override
                        public net.minecraft.network.chat.Component getDisplayName() {
                            return Component.literal("Create Mine Type");
                        }
                        @Override
                        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id1, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player pl) {
                            MineTypeCreationMenu menu = new MineTypeCreationMenu(id1, inv);
                            menu.loadFromPlayer(sp);
                            menu.addBlock(id);
                            return menu;
                        }
                    });
                }
            }
        } else if (slotId == 27) {
            if (page > 0) {
                page--;
                updatePage();
            }
        } else if (slotId == 35) {
            if ((page + 1) * 27 < blockStacks.size()) {
                page++;
                updatePage();
            }
        }
    }
}