package me.steven.entitydebug;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.math.ChunkPos;

import java.util.*;
import java.util.stream.Collectors;

public class EntityDebug implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((commandDispatcher, b) ->
				commandDispatcher.register(CommandManager.literal("debugentities").executes((ctx) -> {
					ServerCommandSource source = ctx.getSource();

					Object2IntMap<ChunkPos> count = new Object2IntOpenHashMap<>();
					source.getWorld().iterateEntities().forEach(entity -> {
						count.mergeInt(new ChunkPos(entity.getBlockPos()), 1, Integer::sum);
					});
					List<Object2IntMap.Entry<ChunkPos>> list = count.object2IntEntrySet()
							.stream()
							.sorted(Comparator.comparingInt(Object2IntMap.Entry::getIntValue))
							.collect(Collectors.toList());
					Collections.reverse(list);
					list.stream()
							.limit(15)
							.map((entry) -> {
								int x = entry.getKey().getStartX();
								int y = 128;
								int z = entry.getKey().getStartZ();
								return new LiteralText(entry.getKey().toString() + ": " + entry.getIntValue() + " entities")
										.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @p " + x + " " + y + " " + z)));
							})
							.forEach((t) -> source.sendFeedback(t, false));
					return 1;
				})));
	}
}
