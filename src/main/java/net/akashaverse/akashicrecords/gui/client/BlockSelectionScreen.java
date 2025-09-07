package net.akashaverse.akashicrecords.gui.client;

import net.akashaverse.akashicrecords.gui.menu.BlockSelectionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for selecting blocks.  Draws a chest background and displays
 * the container items.  Navigation arrows are drawn using the item icons.
 */
public class BlockSelectionScreen extends AbstractContainerScreen<BlockSelectionMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("minecraft:textures/gui/container/generic_54.png");

    public BlockSelectionScreen(BlockSelectionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, 128, 0x404040, false);
    }
}