package dev.invtools;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;

import java.util.concurrent.ConcurrentHashMap;

public final class AntiLeak {
	private static final ConcurrentHashMap<Block, Integer> CACHE = new ConcurrentHashMap<>();

	private AntiLeak() {}

	/** Liefert den Block, als der dieser Block aussehen soll, oder null (unverändert). */
	public static Block replacement(Block block) {
		int kind = CACHE.computeIfAbsent(block, AntiLeak::classify);
		if (kind == 1) return Blocks.DEEPSLATE;
		if (kind == 2) return Blocks.NETHERRACK;
		return null;
	}

	private static int classify(Block block) {
		if (block == Blocks.SPAWNER) return 1;
		String path = Registries.BLOCK.getId(block).getPath();
		if (path.equals("ancient_debris")) return 2;
		if (path.endsWith("_ore")) return path.startsWith("nether_") ? 2 : 1;
		return 0;
	}
}
