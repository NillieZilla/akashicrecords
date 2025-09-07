package net.akashaverse.akashicrecords.gui.menu;

import net.akashaverse.akashicrecords.gui.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;

/**
 * The server‑side menu for the main mine management interface.  This menu
 * presents a chest‑like inventory to the player.  Slots are not meant to be
 * interactive; instead items act as buttons to trigger actions via click
 * handlers in the corresponding {@link net.akashaverse.akashicrecords.gui.client.MineMainScreen}.
 */
public class MineMainMenu extends AbstractContainerMenu {
    private final Container container;

    /**
     * Client constructor.  Called by the {@code MenuType} to create the menu on the client.
     * Since the client cannot access the server-side type, we call {@link ModMenuTypes#MINE_MAIN_MENU#get()} here.
     */
    public MineMainMenu(int id, Inventory playerInventory) {
        super(ModMenuTypes.MINE_MAIN_MENU.get(), id);
        this.container = new SimpleContainer(54);
        setupMenuContents();
    }

    /**
     * Server constructor.  Takes an explicit {@link MenuType} parameter for the menu type.  The
     * {@link MenuType} is provided by the registry when the menu is opened on the server.  This
     * constructor matches the signature used by the client constructor via method reference
     * {@code MineMainMenu::new}.
     */
    public MineMainMenu(MenuType<?> type, int id, Inventory playerInventory) {
        super(type, id);
        this.container = new SimpleContainer(54);
        setupMenuContents();
    }

    /**
     * Return the underlying container used by the screen to draw items.  Entries
     * in this container should be populated when the screen is opened on the
     * client side (see {@code MineMainScreen}).
     */
    public Container getContainer() {
        return container;
    }

    /**
     * Populate the menu with decorative and interactive items.  The menu is
     * structured like a double chest (54 slots).  We fill all slots with
     * glass panes and place functional icons at specific slots:
     * <ul>
     *   <li>Slot 10 – List Mines (paper icon)</li>
     *   <li>Slot 12 – Create Mine Type (book icon)</li>
     *   <li>Slot 14 – Create Mine (iron pickaxe icon)</li>
     * </ul>
     * Each button’s display name and lore will be shown on hover.
     */
    private void setupMenuContents() {
        // Fill with stained glass panes for decoration
        net.minecraft.world.item.ItemStack filler = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }
        // List Mines button
        net.minecraft.world.item.ItemStack listItem = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
        
        container.setItem(10, listItem);
        // Create Mine Type button
        net.minecraft.world.item.ItemStack typeItem = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK);
        
        container.setItem(12, typeItem);
        // Create Mine button
        net.minecraft.world.item.ItemStack mineItem = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
        
        container.setItem(14, mineItem);
    }

    /**
     * Handle click events on the menu.  We override this method to detect
     * interactions with our functional icons and trigger the appropriate
     * server‑side behaviour.  The dragType parameter is unused for simple
     * clicks (0 = PICKUP).  ClickType.PICKUP is used for left and right
     * clicks without shift.
     */
    @Override
    public void clicked(int slotId, int dragType, net.minecraft.world.inventory.ClickType clickType, net.minecraft.world.entity.player.Player player) {
        if (clickType == net.minecraft.world.inventory.ClickType.PICKUP) {
            // Only handle clicks on the server side
            if (!player.level().isClientSide) {
                switch (slotId) {
                    case 10 -> {
                        // List mines; sends a chat list to the player similar to /mine list
                        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                            net.minecraft.server.level.ServerLevel level = sp.serverLevel();
                            net.akashaverse.akashicrecords.core.mine.MineManager manager = net.akashaverse.akashicrecords.core.mine.MineManager.get(level);
                            if (manager.getMines().isEmpty()) {
                                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("No mines defined."));
                            } else {
                                manager.getMines().forEach((name, mine) -> {
                                    net.minecraft.core.BlockPos pos = mine.entrance;
                                    var centre = new net.minecraft.world.phys.Vec3(
                                            (mine.min.getX() + mine.max.getX()) / 2.0 + 0.5,
                                            (mine.min.getY() + mine.max.getY()) / 2.0,
                                            (mine.min.getZ() + mine.max.getZ()) / 2.0 + 0.5);
                                    double dx = centre.x - (pos.getX() + 0.5);
                                    double dz = centre.z - (pos.getZ() + 0.5);
                                    float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                                    String tpCommand = String.format("/tp @s %d %d %d %.1f 0.0", pos.getX(), pos.getY(), pos.getZ(), yaw);
                                    var line = net.minecraft.network.chat.Component.literal("- " + name + " at " + pos.getX() + "," + pos.getY() + "," + pos.getZ() + " ")
                                            .append(net.minecraft.network.chat.Component.literal("[Teleport]")
                                                    .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0x00FFFF)
                                                            .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, tpCommand))));
                                    sp.sendSystemMessage(line);
                                });
                            }
                        }
                    }
                    case 12 -> {
                        // Open the mine type creation menu
                        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                            sp.openMenu(new net.minecraft.world.inventory.MenuProvider() {
                                @Override
                                public net.minecraft.network.chat.Component getDisplayName() {
                                    return net.minecraft.network.chat.Component.literal("Create Mine Type");
                                }
                                @Override
                                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id2, net.minecraft.world.entity.player.Inventory inv2, net.minecraft.world.entity.player.Player pl) {
                                    return new net.akashaverse.akashicrecords.gui.menu.MineTypeCreationMenu(id2, inv2);
                                }
                            });
                        }
                    }
                    case 14 -> {
                        // Give the player the selection wand and instruct them to select positions
                        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                            // Give a selection wand temporarily (1 item).  Use the registered item supplier.
                            var wand = net.akashaverse.akashicrecords.items.MineItems.SELECTION_WAND.get().getDefaultInstance();
                            if (!sp.getInventory().contains(wand)) {
                                sp.getInventory().placeItemBackInInventory(wand);
                            }
                            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("Use the selection wand to select two corners and a spawn point, then run /mine create <name> <type>."));
                            // Close the menu so the player can select
                            sp.closeContainer();
                        }
                    }
                    default -> {
                        // Do nothing for other slots
                    }
                }
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Disable quick move; this menu is not intended for item transfer
        return ItemStack.EMPTY;
    }
}