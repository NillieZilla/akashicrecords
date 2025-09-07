package net.akashaverse.akashicrecords.gui.menu;

import net.akashaverse.akashicrecords.core.mine.WeightedBlock;
import net.akashaverse.akashicrecords.gui.ModMenuTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Menu for creating a new mine type.  Players can cycle through preset
 * interval and warning durations, select blocks with simple weights,
 * and save the type to a TOML file.  This menu is server‑side only;
 * the screen is responsible for drawing the chest background.
 */
public class MineTypeCreationMenu extends AbstractContainerMenu {
    private final Container container;
    private int intervalMinutes = 10;
    private int warningSeconds = 60;
    private final List<WeightedBlock> blocks = new ArrayList<>();

    public MineTypeCreationMenu(int id, Inventory playerInventory) {
        this(ModMenuTypes.MINE_TYPE_CREATION_MENU.get(), id, playerInventory);
    }

    public MineTypeCreationMenu(MenuType<?> type, int id, Inventory playerInventory) {
        super(type, id);
        this.container = new SimpleContainer(54);
        // Prepopulate with some defaults
        setupContents();
    }

    public Container getContainer() {
        return container;
    }

    /**
     * Populate the menu with decorative and functional items.  The menu layout:
     * <pre>
     * [10] Interval (clock) – cycles 1, 5, 10, 20, 30, 60 minutes
     * [12] Warning (redstone torch) – cycles 0, 30, 60, 120 seconds
     * [14–21] Selected blocks (max 8)
     * [23] Add block (emerald) – opens block selection menu
     * [25] Save (written book) – saves the type to file
     * [27] Cancel (barrier) – closes and returns to main menu
     * All other slots filled with light gray panes for decoration.
     * </pre>
     */
    private void setupContents() {
        // Fill with decoration
        ItemStack deco = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, deco.copy());
        }
        // Interval selector
        updateIntervalItem();
        // Warning selector
        updateWarningItem();
        // Selected block slots will be populated in updateBlockList()
        updateBlockList();
        // Add block button
        ItemStack add = new ItemStack(Items.EMERALD);
        container.setItem(23, add);
        // Save button
        ItemStack save = new ItemStack(Items.WRITTEN_BOOK);
        container.setItem(25, save);
        // Cancel button
        ItemStack cancel = new ItemStack(Items.BARRIER);
        container.setItem(27, cancel);
    }

    /**
     * Refresh the interval selector item based on the current value.
     */
    private void updateIntervalItem() {
        ItemStack clock = new ItemStack(Items.CLOCK);
        container.setItem(10, clock);
    }

    /**
     * Refresh the warning selector item based on the current value.
     */
    private void updateWarningItem() {
        ItemStack torch = new ItemStack(Items.REDSTONE_TORCH);
        container.setItem(12, torch);
    }

    /**
     * Refresh the block slots to reflect the current selection.  We display
     * up to 8 entries starting at slot 14.  Each entry shows the block item
     * with its weight appended to the display name.
     */
    private void updateBlockList() {
        // clear block slots (14–21)
        for (int i = 14; i <= 21; i++) {
            ItemStack filler = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
            container.setItem(i, filler);
        }
        for (int i = 0; i < blocks.size() && i < 8; i++) {
            WeightedBlock wb = blocks.get(i);
            Block block = BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.ResourceLocation.parse(wb.blockId())).orElse(Blocks.STONE);
            ItemStack stack = new ItemStack(block);
            container.setItem(14 + i, stack);
        }
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
        if (clickType != ClickType.PICKUP) {
            super.clicked(slotId, dragType, clickType, player);
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        switch (slotId) {
            case 10 -> {
                // cycle interval minutes through preset values
                int[] values = {1, 5, 10, 20, 30, 60};
                int idx = 0;
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == intervalMinutes) {
                        idx = (i + 1) % values.length;
                        break;
                    }
                }
                intervalMinutes = values[idx];
                updateIntervalItem();
            }
            case 12 -> {
                // cycle warning seconds through preset values
                int[] values = {0, 30, 60, 120, 300};
                int idx = 0;
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == warningSeconds) {
                        idx = (i + 1) % values.length;
                        break;
                    }
                }
                warningSeconds = values[idx];
                updateWarningItem();
            }
            default -> {
                if (slotId >= 14 && slotId <= 21) {
                    int index = slotId - 14;
                    if (index < blocks.size()) {
                        // remove this block from selection
                        blocks.remove(index);
                        updateBlockList();
                    }
                } else if (slotId == 23) {
                    // open block selection screen
                    if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                        // store current selections in persistent data for retrieval
                        saveToPlayer(sp);
                        // open selection menu
                    sp.openMenu(new net.minecraft.world.inventory.MenuProvider() {
                        @Override
                        public net.minecraft.network.chat.Component getDisplayName() {
                            return Component.literal("Select Block");
                        }
                        @Override
                        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player pl) {
                            return new net.akashaverse.akashicrecords.gui.menu.BlockSelectionMenu(id, inv);
                        }
                    });
                    }
                } else if (slotId == 25) {
                    // save file
                    if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                        saveToFile(sp);
                        sp.sendSystemMessage(Component.literal("Mine type saved."));
                        // close menu
                        sp.closeContainer();
                    }
                } else if (slotId == 27) {
                    // cancel: close and return to main menu
                    if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                        sp.openMenu(new net.minecraft.world.inventory.MenuProvider() {
                            @Override
                            public net.minecraft.network.chat.Component getDisplayName() {
                                return Component.literal("Mine Manager");
                            }
                            @Override
                            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player pl) {
                                return new net.akashaverse.akashicrecords.gui.menu.MineMainMenu(id, inv);
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * Save the current interval, warning, and blocks to the player's persistent data.
     */
    private void saveToPlayer(net.minecraft.server.level.ServerPlayer sp) {
        var tag = sp.getPersistentData().getCompound("mineTypeTmp");
        tag.putInt("intervalMinutes", intervalMinutes);
        tag.putInt("warningSeconds", warningSeconds);
        // serialize blocks as id|weight
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (WeightedBlock wb : blocks) {
            list.add(net.minecraft.nbt.StringTag.valueOf(wb.blockId() + "|" + wb.weight()));
        }
        tag.put("blocks", list);
        sp.getPersistentData().put("mineTypeTmp", tag);
    }

    /**
     * Save the mine type to disk.  A unique name is generated using a UUID.
     */
    private void saveToFile(net.minecraft.server.level.ServerPlayer sp) {
        try {
            Path dir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("AkashicRecords/Mine");
            Files.createDirectories(dir);
            String name = "custom_type_" + UUID.randomUUID().toString().substring(0, 8);
            Path file = dir.resolve(name + ".toml");
            // build toml string
            StringBuilder sb = new StringBuilder();
            sb.append("intervalMinutes = ").append(intervalMinutes).append("\n");
            sb.append("warningSeconds = ").append(warningSeconds).append("\n");
            sb.append("blocks = [");
            for (int i = 0; i < blocks.size(); i++) {
                WeightedBlock wb = blocks.get(i);
                sb.append("\"").append(wb.blockId()).append("=").append(wb.weight()).append("\"");
                if (i < blocks.size() - 1) sb.append(",");
            }
            sb.append("]\n");
            Files.writeString(file, sb.toString());
        } catch (Exception ex) {
            sp.sendSystemMessage(Component.literal("Failed to save mine type: " + ex.getMessage()));
        }
    }

    /**
     * Restore state from player persistent data when returning from block selection.
     */
    public void loadFromPlayer(net.minecraft.server.level.ServerPlayer sp) {
        var tag = sp.getPersistentData().getCompound("mineTypeTmp");
        if (tag.contains("intervalMinutes")) intervalMinutes = tag.getInt("intervalMinutes");
        if (tag.contains("warningSeconds")) warningSeconds = tag.getInt("warningSeconds");
        blocks.clear();
        if (tag.contains("blocks")) {
            net.minecraft.nbt.ListTag list = tag.getList("blocks", net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String[] parts = list.getString(i).split("\\|");
                if (parts.length >= 2) {
                    String id = parts[0];
                    double weight;
                    try { weight = Double.parseDouble(parts[1]); } catch (NumberFormatException ex) { weight = 1.0; }
                    blocks.add(new WeightedBlock(id, weight));
                }
            }
        }
        updateIntervalItem();
        updateWarningItem();
        updateBlockList();
    }

    /**
     * Called from BlockSelectionMenu when a new block is selected.
     */
    public void addBlock(String blockId) {
        blocks.add(new WeightedBlock(blockId, 1.0));
        updateBlockList();
    }
}