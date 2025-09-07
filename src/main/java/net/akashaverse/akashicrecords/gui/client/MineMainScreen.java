package net.akashaverse.akashicrecords.gui.client;

import net.akashaverse.akashicrecords.gui.menu.MineMainMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client‑side screen for the main mine management interface.  Renders a
 * chest‑style inventory background and delegates item rendering to the
 * underlying menu.  Additional widgets or buttons can be added to this
 * screen to provide a fully interactive GUI for managing mines (e.g.
 * listing existing mines, creating new types, adjusting layers and weights).
 */
public class MineMainScreen extends AbstractContainerScreen<MineMainMenu> {
    // The vanilla chest texture (six rows).  We reuse this to give our GUI a familiar look.
    // Use the static factory method to construct resource locations; constructors are private in 1.21
    private static final ResourceLocation BG = net.minecraft.resources.ResourceLocation.parse("minecraft:textures/gui/container/generic_54.png");

    public MineMainScreen(MineMainMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        // The chest background is 176 pixels wide by 222 pixels tall (6 rows + player inventory).
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 93;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Draw the chest background
        graphics.blit(BG, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}