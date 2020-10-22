package me.steven.entitydebug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.EntitySummonArgumentType;
import net.minecraft.command.argument.ItemPredicateArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntityDebug implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((commandDispatcher, b) ->
				commandDispatcher.register(CommandManager.literal("debugentities")
						.requires((src) -> src.hasPermissionLevel(2))
						.executes(this::execute)
						.then((CommandManager.argument("entity", StringArgumentType.greedyString())
								.executes((ctx) -> execute(ctx, StringArgumentType.getString(ctx, "entity")))))));
		CommandRegistrationCallback.EVENT.register((commandDispatcher, b) ->
				commandDispatcher.register(CommandManager.literal("ischunkloaded")
						.requires((src) -> src.hasPermissionLevel(2))
						.executes((ctx) -> {
							ChunkPos chunk = new ChunkPos(new BlockPos(ctx.getSource().getPosition()));
							return executeIsLoaded(ctx, chunk.x, chunk.z);
						})
						.then(CommandManager.argument("x", IntegerArgumentType.integer())
								.then(CommandManager.argument("z", IntegerArgumentType.integer())
										.executes((ctx) -> executeIsLoaded(ctx, IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "z")))))));
		CommandRegistrationCallback.EVENT.register((commandDispatcher, b) ->
				commandDispatcher.register(CommandManager.literal("scan")
						.requires((src) -> src.hasPermissionLevel(2))
						.executes(this::execute)
						.then((CommandManager.argument("radius", IntegerArgumentType.integer())
								.executes((ctx) -> executeScan(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))));
	}

	private int executeScan(CommandContext<ServerCommandSource> ctx, int radius) {
		ChunkPos center = new ChunkPos(new BlockPos(ctx.getSource().getPosition()));
		List<ChunkPos> positions = new ArrayList<>();
		for (int x = -radius; x < radius; x++) {
			for (int z = -radius; z < radius; z++) {
				ChunkPos pos = new ChunkPos(center.x + x, center.z + z);
				positions.add(pos);
			}
		}
		ServerCommandSource source = ctx.getSource();

		Object2IntMap<EntityType<?>> count = new Object2IntOpenHashMap<>();
		source.getWorld().iterateEntities().forEach(entity -> {
			ChunkPos chunkPos = new ChunkPos(entity.getBlockPos());
			if (source.getWorld().getChunkManager().shouldTickEntity(entity) && positions.contains(chunkPos)) {
				count.mergeInt(entity.getType(), 1, Integer::sum);
			}
		});
		List<Object2IntMap.Entry<EntityType<?>>> list = count.object2IntEntrySet()
				.stream()
				.sorted(Comparator.comparingInt(Object2IntMap.Entry::getIntValue))
				.collect(Collectors.toList());
		Collections.reverse(list);
		list
				.stream()
				.map((entry) -> new LiteralText(Registry.ENTITY_TYPE.getId(entry.getKey()).toString() + ": " + entry.getIntValue()))
				.forEach((t) -> source.sendFeedback(t, false));
		return 1;
	}

	private int executeIsLoaded(CommandContext<ServerCommandSource> ctx, int x, int z) {
		boolean isLoaded = ctx.getSource().getWorld().isChunkLoaded(x, z);
		ctx.getSource().sendFeedback(new LiteralText(isLoaded + ""), false);
		return 1;
	}

	private int execute(CommandContext<ServerCommandSource> ctx, String regex) {
		Pattern pattern = Pattern.compile(regex);
		ServerCommandSource source = ctx.getSource();

		Object2IntMap<ChunkPos> count = new Object2IntOpenHashMap<>();
		source.getWorld().iterateEntities().forEach(entity -> {
			if (source.getWorld().getChunkManager().shouldTickEntity(entity)
					&& pattern.matcher(Registry.ENTITY_TYPE.getId(entity.getType()).toString()).matches()) {
				count.mergeInt(new ChunkPos(entity.getBlockPos()), 1, Integer::sum);
			}
		});
		List<Object2IntMap.Entry<ChunkPos>> list = count.object2IntEntrySet()
				.stream()
				.sorted(Comparator.comparingInt(Object2IntMap.Entry::getIntValue))
				.collect(Collectors.toList());
		Collections.reverse(list);
		list.stream()
				.limit(20)
				.map((entry) -> {
					int x = entry.getKey().getStartX();
					int y = 128;
					int z = entry.getKey().getStartZ();
					return new LiteralText(entry.getKey().toString() + ": " + entry.getIntValue() + " entities")
							.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @p " + x + " " + y + " " + z)));
				})
				.forEach((t) -> source.sendFeedback(t, false));
		return 1;
	}
	private int execute(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();

		Object2IntMap<ChunkPos> count = new Object2IntOpenHashMap<>();
		source.getWorld().iterateEntities().forEach(entity -> {
			if (source.getWorld().getChunkManager().shouldTickEntity(entity))
				count.mergeInt(new ChunkPos(entity.getBlockPos()), 1, Integer::sum);
		});
		List<Object2IntMap.Entry<ChunkPos>> list = count.object2IntEntrySet()
				.stream()
				.sorted(Comparator.comparingInt(Object2IntMap.Entry::getIntValue))
				.collect(Collectors.toList());
		Collections.reverse(list);
		list.stream()
				.limit(20)
				.map((entry) -> {
					int x = entry.getKey().getStartX();
					int y = 128;
					int z = entry.getKey().getStartZ();
					return new LiteralText(entry.getKey().toString() + ": " + entry.getIntValue() + " entities")
							.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @p " + x + " " + y + " " + z)));
				})
				.forEach((t) -> source.sendFeedback(t, false));
		return 1;
	}
}
