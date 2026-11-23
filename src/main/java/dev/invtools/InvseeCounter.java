package dev.invtools;

import dev.invtools.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

public final class InvseeCounter {
	private static long lastInvsee = -1;

	private InvseeCounter() {}

	public static void markInvsee() {
		lastInvsee = Util.getMeasuringTimeMs();
	}

	/** 0 = nicht anzeigen, 1 = eigenes Inventar, 2 = fremdes Inventar (Invsee) */
	private static int mode(MinecraftClient mc, Screen s) {
		if (s instanceof InventoryScreen) return 1;
		if (s instanceof GenericContainerScreen) {
			if (lastInvsee >= 0 && Util.getMeasuringTimeMs() - lastInvsee < 15000) return 2;
			if (mc.world != null) {
				String title = s.getTitle().getString().toLowerCase();
				for (PlayerEntity p : mc.world.getPlayers()) {
					if (title.contains(p.getName().getString().toLowerCase())) return 2;
				}
			}
		}
		return 0;
	}

	public static void render(MinecraftClient mc, Screen screen, DrawContext ctx) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.counterEnabled || !(screen instanceof HandledScreen<?> hs)) return;
		int mode = mode(mc, screen);
		if (mode == 0) return;

		boolean own = mode == 1;
		int[] counts = new int[4];
		Item[] items = {Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, Items.ENDER_PEARL, Items.WIND_CHARGE};
		for (Slot slot : hs.getScreenHandler().slots) {
			boolean playerInv = slot.inventory instanceof PlayerInventory;
			if (playerInv != own) continue;
			ItemStack st = slot.getStack();
			if (st.isEmpty()) continue;
			for (int i = 0; i < items.length; i++) {
				if (st.isOf(items[i])) counts[i] += st.getCount();
			}
		}

		boolean[] show = {cfg.showTotems, cfg.showCrystals, cfg.showPearls, cfg.showWindCharges};
		int[] colors = {cfg.colorTotem, cfg.colorCrystal, cfg.colorPearl, cfg.colorWind};
		List<Integer> rows = new ArrayList<>();
		for (int i = 0; i < 4; i++) if (show[i]) rows.add(i);
		if (rows.isEmpty()) return;

		HandledScreenAccessor acc = (HandledScreenAccessor) hs;
		int w = 76;
		int h = 16 + rows.size() * 18 + 2;
		int x = acc.invtools$getX() + acc.invtools$getBackgroundWidth() + 6;
		if (x + w > screen.width - 2) x = screen.width - w - 2;
		int y = acc.invtools$getY();

		ctx.fill(x, y, x + w, y + h, cfg.colorPanelBg);
		ctx.drawBorder(x, y, w, h, cfg.colorPanelBorder);
		Text title = Text.literal(own ? "Dein Inventar" : "Invsee");
		ctx.drawText(mc.textRenderer, title, x + 4, y + 4, cfg.colorTitle, true);

		int ry = y + 16;
		for (int i : rows) {
			ctx.drawItem(new ItemStack(items[i]), x + 4, ry);
			ctx.drawText(mc.textRenderer, "x" + counts[i], x + 26, ry + 5, colors[i], true);
			ry += 18;
		}
	}
}
