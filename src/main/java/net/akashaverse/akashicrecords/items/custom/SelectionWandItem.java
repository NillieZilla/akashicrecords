package net.akashaverse.akashicrecords.items.custom;

import net.akashaverse.akashicrecords.AkashicRecords;
import net.akashaverse.akashicrecords.configs.MineConfig;
import net.akashaverse.akashicrecords.core.mine.Mine;
import net.akashaverse.akashicrecords.core.mine.MineManager;
import net.akashaverse.akashicrecords.core.mine.WeightedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
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
        ServerLevel serverLevel = (ServerLevel) level;
        CompoundTag tag = player.getPersistentData().getCompound(TAG_NAME);
        BlockPos clickedPos = context.getClickedPos();

        // first position
        if (!tag.contains("pos1")) {
            tag.putLong("pos1", clickedPos.asLong());
            player.sendSystemMessage(Component.literal("First corner set at " + posToString(clickedPos)));
            player.getPersistentData().put(TAG_NAME, tag);
            return InteractionResult.SUCCESS;
        }
        // second position
        if (!tag.contains("pos2")) {
            tag.putLong("pos2", clickedPos.asLong());
            player.sendSystemMessage(Component.literal("Second corner set at " + posToString(clickedPos)));
            player.getPersistentData().put(TAG_NAME, tag);
            return InteractionResult.SUCCESS;
        }
        // create mine
        BlockPos pos1 = BlockPos.of(tag.getLong("pos1"));
        BlockPos pos2 = BlockPos.of(tag.getLong("pos2"));
        BlockPos entrance = clickedPos;
        // parse default distribution from config
        List<WeightedBlock> distribution = parseDefaultDistribution();
        int intervalMinutes = MineConfig.DEFAULT_REFILL_INTERVAL_MINUTES.get();
        int warningSeconds = MineConfig.WARNING_TIME_SECONDS.get();
        int intervalTicks = intervalMinutes * 20 * 60;
        int warningTicks = warningSeconds * 20;
        BlockState border = Blocks.BEDROCK.defaultBlockState();
        Mine mine = new Mine(pos1, pos2, entrance, intervalTicks, warningTicks, border, distribution);
        // schedule first reset immediately
        mine.nextReset = serverLevel.getGameTime() + intervalTicks;
        // name the mine based on player name and current time
        String name = player.getScoreboardName() + "_" + System.currentTimeMillis();
        MineManager manager = MineManager.get(serverLevel);
        manager.putMine(name, mine);
        // initial generation
        mine.regenerate(serverLevel);
        player.sendSystemMessage(Component.literal("Mine created successfully: " + name));
        // clear stored positions
        tag.remove("pos1");
        tag.remove("pos2");
        player.getPersistentData().put(TAG_NAME, tag);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Right clicking in air does nothing; we rely on useOn to set entrance.
        return super.use(level, player, hand);
    }

    private static String posToString(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static List<WeightedBlock> parseDefaultDistribution() {
        List<WeightedBlock> list = new ArrayList<>();
        for (String entry : MineConfig.DEFAULT_DISTRIBUTION.get()) {
            String[] parts = entry.split("=");
            String id = parts[0];
            double weight = 1.0;
            if (parts.length > 1) {
                try {
                    weight = Double.parseDouble(parts[1]);
                } catch (NumberFormatException ignored) {
                }
            }
            list.add(new WeightedBlock(id, weight));
        }
        return list;
    }
}