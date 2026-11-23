package dev.invtools;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ConfigScreen extends Screen {
	private final Screen parent;
	private final ModConfig cfg = ModConfig.get();

	private record ColorRow(String label, int y, IntSupplier getter) {}
	private final List<ColorRow> colorRows = new ArrayList<>();
	private final List<TextFieldWidget> colorFields = new ArrayList<>();
	private final List<IntSupplier> colorGetters = new ArrayList<>();

	private int leftX, rightX, colW;

	public ConfigScreen(Screen parent) {
		super(Text.literal("InvTools Einstellungen"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		colorRows.clear();
		colorFields.clear();
		int cx = this.width / 2;
		colW = 200;
		leftX = cx - 205;
		rightX = cx + 5;
		int step = 22;
		int y = 40;

		// ----- linke Spalte: Funktionen -----
		addToggle(leftX, y, colW, "Disconnect-Verzögerung", () -> cfg.disconnectDelayEnabled, v -> cfg.disconnectDelayEnabled = v);
		y += step;
		addDrawableChild(new SecondsSlider(leftX, y, colW, 20));
		y += step + 6;
		addToggle(leftX, y, colW, "Invsee-Hotkey (Reichweite 15)", () -> cfg.invseeEnabled, v -> cfg.invseeEnabled = v);
		y += step + 6;
		addToggle(leftX, y, colW, "Invsee-Counter", () -> cfg.counterEnabled, v -> cfg.counterEnabled = v);
		y += step;
		addToggle(leftX, y, colW, "  Totems", () -> cfg.showTotems, v -> cfg.showTotems = v);
		y += step;
		addToggle(leftX, y, colW, "  Crystals", () -> cfg.showCrystals, v -> cfg.showCrystals = v);
		y += step;
		addToggle(leftX, y, colW, "  Pearls", () -> cfg.showPearls, v -> cfg.showPearls = v);
		y += step;
		addToggle(leftX, y, colW, "  Wind Charges", () -> cfg.showWindCharges, v -> cfg.showWindCharges = v);

		// ----- rechte Spalte: Farben (Hex: RRGGBB oder AARRGGBB) -----
		int cy = 40;
		addColor(cy, "Panel Hintergrund", () -> cfg.colorPanelBg, v -> cfg.colorPanelBg = v); cy += step;
		addColor(cy, "Panel Rahmen", () -> cfg.colorPanelBorder, v -> cfg.colorPanelBorder = v); cy += step;
		addColor(cy, "Titel", () -> cfg.colorTitle, v -> cfg.colorTitle = v); cy += step;
		addColor(cy, "Totems", () -> cfg.colorTotem, v -> cfg.colorTotem = v); cy += step;
		addColor(cy, "Crystals", () -> cfg.colorCrystal, v -> cfg.colorCrystal = v); cy += step;
		addColor(cy, "Pearls", () -> cfg.colorPearl, v -> cfg.colorPearl = v); cy += step;
		addColor(cy, "Wind Charges", () -> cfg.colorWind, v -> cfg.colorWind = v); cy += step;
		addColor(cy, "Disconnect-Countdown", () -> cfg.colorCountdown, v -> cfg.colorCountdown = v); cy += step + 4;

		addDrawableChild(ButtonWidget.builder(Text.literal("Farben zurücksetzen"), b -> {
			cfg.resetColors();
			for (int i = 0; i < colorFields.size(); i++) {
				colorFields.get(i).setText(hex(colorGetters.get(i).getAsInt()));
			}
		}).dimensions(rightX, cy, colW, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), b -> close())
				.dimensions(cx - 100, this.height - 28, 200, 20).build());
	}

	private void addToggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
		ButtonWidget[] ref = new ButtonWidget[1];
		ref[0] = ButtonWidget.builder(toggleText(label, get.getAsBoolean()), b -> {
			set.accept(!get.getAsBoolean());
			b.setMessage(toggleText(label, get.getAsBoolean()));
		}).dimensions(x, y, w, 20).build();
		addDrawableChild(ref[0]);
	}

	private Text toggleText(String label, boolean on) {
		return Text.literal(label + ": ")
				.append(Text.literal(on ? "AN" : "AUS").withColor(on ? 0x55FF55 : 0xFF5555));
	}

	private void addColor(int y, String label, IntSupplier get, IntConsumer set) {
		TextFieldWidget f = new TextFieldWidget(this.textRenderer, rightX + 115, y, 63, 20, Text.literal(label));
		f.setMaxLength(9);
		f.setText(hex(get.getAsInt()));
		f.setChangedListener(s -> {
			try {
				String t = s.trim();
				if (t.startsWith("#")) t = t.substring(1);
				if (t.length() == 6) t = "FF" + t;
				if (t.length() != 8) return;
				set.accept((int) Long.parseLong(t, 16));
			} catch (NumberFormatException ignored) {}
		});
		addDrawableChild(f);
		colorFields.add(f);
		colorGetters.add(get);
		colorRows.add(new ColorRow(label, y, get));
	}

	private static String hex(int argb) {
		return String.format("%08X", argb);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, 0xFFFFFFFF);
		ctx.drawText(this.textRenderer, Text.literal("Funktionen"), leftX, 28, 0xFFAAAAAA, true);
		ctx.drawText(this.textRenderer, Text.literal("Farben (Hex)"), rightX, 28, 0xFFAAAAAA, true);
		for (ColorRow r : colorRows) {
			ctx.drawText(this.textRenderer, Text.literal(r.label()), rightX, r.y() + 6, 0xFFFFFFFF, true);
			int px = rightX + 182;
			ctx.fill(px - 1, r.y() - 1 + 1, px + 19, r.y() + 19, 0xFF000000);
			ctx.fill(px, r.y() + 1, px + 18, r.y() + 18, r.getter().getAsInt() | 0xFF000000);
		}
	}

	@Override
	public void close() {
		ModConfig.save();
		this.client.setScreen(parent);
	}

	private class SecondsSlider extends SliderWidget {
		SecondsSlider(int x, int y, int w, int h) {
			super(x, y, w, h, Text.empty(), (cfg.disconnectDelaySeconds - 1) / 9.0);
			updateMessage();
		}

		private int seconds() {
			return 1 + (int) Math.round(this.value * 9);
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal("Wartezeit: " + seconds() + " s"));
		}

		@Override
		protected void applyValue() {
			cfg.disconnectDelaySeconds = seconds();
		}
	}
}
