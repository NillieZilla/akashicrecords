package net.akashaverse.akashicrecords.gui.client;

import net.akashaverse.akashicrecords.gui.menu.MineTypeCreationMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the mine type creation menu.  Simply renders the chest
 * background and the player's inventory along with the menu's items.  No
 * additional widgets are needed; interaction is handled via clicks on items.
 */
public class MineTypeCreationScreen extends AbstractContainerScreen<MineTypeCreationMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("minecraft:textures/gui/container/generic_54.png");

    public MineTypeCreationScreen(MineTypeCreationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Draw background
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        // Draw player inventory title
        graphics.drawString(this.font, this.playerInventoryTitle, 8, 128, 0x404040, false);
    }
}