package net.akashaverse.akashicrecords.items.mine;

import net.akashaverse.akashicrecords.AkashicRecords;
import net.minecraft.client.gui.screens.Screen;
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
import org.jetbrains.annotations.NotNull;

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
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        CompoundTag tag = player.getPersistentData().getCompound(TAG_NAME);
        BlockPos clickedPos = context.getClickedPos();

        if (!tag.contains("pos1")) {
            tag.putLong("pos1", clickedPos.asLong());
            player.sendSystemMessage(Component.literal("First corner set at " + posToString(clickedPos)));
            player.getPersistentData().put(TAG_NAME, tag);
            return InteractionResult.SUCCESS;
        }
        if (!tag.contains("pos2")) {
            tag.putLong("pos2", clickedPos.asLong());
            player.sendSystemMessage(Component.literal("Second corner set at " + posToString(clickedPos)));
            player.getPersistentData().put(TAG_NAME, tag);
            return InteractionResult.SUCCESS;
        }
        BlockPos spawnPos = clickedPos.above();
        tag.putLong("spawn", spawnPos.asLong());
        player.sendSystemMessage(Component.literal("Spawn location set at " + posToString(spawnPos) + ". Use /mine create <name> <type> to create the mine."));
        player.getPersistentData().put(TAG_NAME, tag);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip." + AkashicRecords.MOD_ID + ".selection_wand"));
        } else {
            tooltip.add(Component.translatable("tooltip." + AkashicRecords.MOD_ID + ".shift"));
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        return super.use(level, player, hand);
    }

    private static String posToString(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

}