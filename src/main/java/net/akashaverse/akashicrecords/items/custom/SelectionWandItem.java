package net.akashaverse.akashicrecords.items.custom;

import net.akashaverse.akashicrecords.AkashicRecords;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The selection wand allows players to define a mine region and entrance by
 * right‑clicking blocks in the world.  The wand stores two corner positions
 * and, on the third click, creates a new mine with default configuration.
 */
public class SelectionWandItem extends Item {
    public static final String TAG_NAME = AkashicRecords.MOD_ID;

    public SelectionWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        CompoundTag tag = player.getPersistentData().getCompound(TAG_NAME);
        BlockPos clickedPos = context.getClickedPos();

        // First corner selection
        if (!tag.contains("pos1")) {
            tag.putLong("pos1", clickedPos.asLong());
            player.sendSystemMessage(Component.literal("First corner set at " + posToString(clickedPos)));
            player.getPersistentData().put(TAG_NAME, tag);
            return InteractionResult.SUCCESS;
        }
        // Second corner selection
        if (!tag.contains("pos2")) {
            tag.putLong("pos2", clickedPos.asLong());
            player.sendSystemMessage(Component.literal("Second corner set at " + posToString(clickedPos)));
            player.getPersistentData().put(TAG_NAME, tag);
            return InteractionResult.SUCCESS;
        }
        // Spawn / entrance selection.  If spawn already exists, update it.
        tag.putLong("spawn", clickedPos.asLong());
        player.sendSystemMessage(Component.literal("Spawn location set at " + posToString(clickedPos) + ". Use /mine create <name> <type> to create the mine."));
        player.getPersistentData().put(TAG_NAME, tag);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        // Show different text depending on whether the Shift key is held.  Translatable
        // components allow server admins to localise the tooltip in a lang file.
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip." + AkashicRecords.MOD_ID + ".selection_wand.shift"));
        } else {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip." + AkashicRecords.MOD_ID + ".selection_wand.default"));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Right clicking in air does nothing; we rely on useOn to set entrance.
        return super.use(level, player, hand);
    }

    private static String posToString(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /*
     * The selection wand no longer parses a default distribution or creates
     * mines directly.  Instead, it stores the selected positions in the
     * player's persistent data.  The /mine create command will read these
     * positions and construct a mine of the specified type.
     */
}