package dev.invtools;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class ConfigScreen extends Screen {
	private interface Painter { void paint(DrawContext ctx, int mx, int my); }

	private static final String[] TABS = {"Allgemein", "GUI", "Overlay", "Tasten"};
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREEN = 0xFF7FD9A0;
	private static final int RED = 0xFFE88A80;
	private static final int GREY = 0xFFB8B8B8;

	private final Screen parent;
	private final ModConfig cfg = ModConfig.get();
	private final List<Painter> painters = new ArrayList<>();
	private final List<TextFieldWidget> colorFields = new ArrayList<>();
	private final List<IntSupplier> colorGetters = new ArrayList<>();
	private final boolean[] wasDown = new boolean[350];
	private int tab = 0;
	private int listening = 0; // 0 = nichts, 1 = Invsee-Taste, 2 = Menü-Taste
	private int builtSize = -1;
	private int px, py, pw, ph;

	public ConfigScreen(Screen parent) {
		super(Text.literal("InvTools"));
		this.parent = parent;
	}

	// ------------------------------------------------------------ Aufbau

	@Override
	protected void init() {
		painters.clear();
		colorFields.clear();
		colorGetters.clear();
		builtSize = cfg.menuSizePercent;

		pw = Math.min(Math.max((int) (width * cfg.menuSizePercent / 100.0), 330), width - 8);
		ph = Math.min(Math.max((int) (height * cfg.menuSizePercent / 100.0), 232), height - 8);
		px = (width - pw) / 2;
		py = (height - ph) / 2;

		int cx = px + 12;
		int cw = pw - 24;

		int tabW = (cw - 18) / 4;
		for (int i = 0; i < 4; i++) {
			final int idx = i;
			final int x = cx + i * (tabW + 6);
			final int y = py + 30;
			painters.add((c, mx, my) -> drawTab(c, x, y, tabW, 18, TABS[idx], idx == tab, hover(mx, my, x, y, tabW, 18)));
			invisible(x, y, tabW, 18, () -> {
				if (listening == 0) {
					tab = idx;
					this.clearChildren();
					this.init();
				}
			});
		}

		int y0 = py + 56;
		switch (tab) {
			case 0 -> buildGeneral(cx, y0, cw);
			case 1 -> buildGui(cx, y0, cw);
			case 2 -> buildOverlay(cx, y0, cw);
			default -> buildKeys(cx, y0, cw);
		}
	}

	private void buildGeneral(int cx, int y0, int cw) {
		int tw = (cw - 12) / 3;
		int th = 36;
		tile(cx, y0, tw, th, "Disconnect-Delay",
				() -> cfg.disconnectDelayEnabled ? "an · " + cfg.disconnectDelaySeconds + " s" : "aus",
				() -> cfg.disconnectDelayEnabled, () -> cfg.disconnectDelayEnabled = !cfg.disconnectDelayEnabled);
		tile(cx + tw + 6, y0, tw, th, "Invsee-Hotkey",
				() -> cfg.invseeEnabled ? "an · Taste " + Keys.name(cfg.invseeKey) : "aus",
				() -> cfg.invseeEnabled, () -> cfg.invseeEnabled = !cfg.invseeEnabled);
		tile(cx + 2 * (tw + 6), y0, tw, th, "Invsee-Counter",
				() -> cfg.counterEnabled ? "an" : "aus",
				() -> cfg.counterEnabled, () -> cfg.counterEnabled = !cfg.counterEnabled);
		tile(cx, y0 + th + 6, tw, th, "Anti-Leak",
				() -> cfg.antiLeak ? "an" : "aus",
				() -> cfg.antiLeak, () -> {
					cfg.antiLeak = !cfg.antiLeak;
					try {
						this.client.worldRenderer.reload();
					} catch (Throwable ignored) {}
				});
		tile(cx + tw + 6, y0 + th + 6, tw, th, "Werkzeugschutz",
				() -> cfg.toolProtect ? "an · ab " + cfg.toolProtectPercent + " %" : "aus",
				() -> cfg.toolProtect, () -> cfg.toolProtect = !cfg.toolProtect);
		// Info-Kachel (nicht klickbar)
		final int ix = cx + 2 * (tw + 6);
		final int iy = y0 + th + 6;
		painters.add((c, mx, my) -> {
			rrect(c, ix, iy, tw, th, 4, 0x40000000);
			c.drawText(this.textRenderer, "Invsee-Reichweite", ix + 8, iy + 7, WHITE, true);
			c.drawText(this.textRenderer, "15 Blöcke", ix + 8, iy + 19, GREY, true);
		});

		int ry = y0 + 2 * th + 6 + 12;
		slider(cx, ry, cw, "Disconnect-Wartezeit", 0, (cfg.disconnectDelaySeconds - 1) / 9.0,
				v -> cfg.disconnectDelaySeconds = 1 + (int) Math.round(v * 9),
				() -> cfg.disconnectDelaySeconds + " s", null);
		slider(cx, ry + 24, cw, "Werkzeugschutz ab", 0, (cfg.toolProtectPercent - 1) / 49.0,
				v -> cfg.toolProtectPercent = 1 + (int) Math.round(v * 49),
				() -> cfg.toolProtectPercent + " %", null);
	}

	private void buildGui(int cx, int y0, int cw) {
		painters.add((c, mx, my) ->
				c.drawText(this.textRenderer, "Jede Fläche läuft von Farbe 1 in Farbe 2.", cx, y0, GREY, true));
		int y = y0 + 16;
		slider(cx, y, cw, "Menü-Größe", 0, (cfg.menuSizePercent - 50) / 50.0,
				v -> cfg.menuSizePercent = 50 + (int) Math.round(v * 50),
				() -> cfg.menuSizePercent + " %", null);
		y += 24;
		slider(cx, y, cw, "Farbe 1: Ton", 1, cfg.color1Hue, v -> cfg.color1Hue = (float) v,
				() -> "", null);
		y += 24;
		slider(cx, y, cw, "Farbe 1: Stärke", 2, cfg.color1Sat, v -> cfg.color1Sat = (float) v,
				() -> "", () -> cfg.color1Hue);
		y += 24;
		slider(cx, y, cw, "Farbe 2: Ton", 1, cfg.color2Hue, v -> cfg.color2Hue = (float) v,
				() -> "", null);
		y += 24;
		slider(cx, y, cw, "Farbe 2: Stärke", 2, cfg.color2Sat, v -> cfg.color2Sat = (float) v,
				() -> "", () -> cfg.color2Hue);
	}

	private void buildOverlay(int cx, int y0, int cw) {
		int tw = (cw - 18) / 4;
		int th = 30;
		String[] names = {"Totems", "Crystals", "Pearls", "Wind Charges"};
		BooleanSupplier[] gets = {() -> cfg.showTotems, () -> cfg.showCrystals, () -> cfg.showPearls, () -> cfg.showWindCharges};
		Runnable[] flips = {
				() -> cfg.showTotems = !cfg.showTotems, () -> cfg.showCrystals = !cfg.showCrystals,
				() -> cfg.showPearls = !cfg.showPearls, () -> cfg.showWindCharges = !cfg.showWindCharges};
		for (int i = 0; i < 4; i++) {
			final BooleanSupplier g = gets[i];
			tile(cx + i * (tw + 6), y0, tw, th, names[i], () -> g.getAsBoolean() ? "an" : "aus", g, flips[i]);
		}

		int ry = y0 + th + 12;
		int colW = (cw - 8) / 2;
		String[] labels = {"Titel", "Totems", "Crystals", "Pearls", "Wind Charges", "Countdown"};
		IntSupplier[] gs = {() -> cfg.colorTitle, () -> cfg.colorTotem, () -> cfg.colorCrystal,
				() -> cfg.colorPearl, () -> cfg.colorWind, () -> cfg.colorCountdown};
		IntConsumer[] ss = {v -> cfg.colorTitle = v, v -> cfg.colorTotem = v, v -> cfg.colorCrystal = v,
				v -> cfg.colorPearl = v, v -> cfg.colorWind = v, v -> cfg.colorCountdown = v};
		for (int i = 0; i < 6; i++) {
			int x = cx + (i % 2) * (colW + 8);
			int y = ry + (i / 2) * 22;
			colorRow(x, y, labels[i], gs[i], ss[i]);
		}

		final int bx = cx;
		final int by = ry + 3 * 22 + 6;
		painters.add((c, mx, my) -> {
			boolean hv = hover(mx, my, bx, by, 130, 18);
			rrect(c, bx, by, 130, 18, 4, hv ? 0x66FFFFFF : 0x44000000);
			c.drawText(this.textRenderer, "Farben zurücksetzen", bx + 8, by + 5, WHITE, true);
		});
		invisible(bx, by, 130, 18, () -> {
			cfg.resetColors();
			for (int i = 0; i < colorFields.size(); i++) {
				colorFields.get(i).setText(String.format("%08X", colorGetters.get(i).getAsInt()));
			}
		});
	}

	private void buildKeys(int cx, int y0, int cw) {
		keyRow(cx, y0, cw, "Invsee auf Ziel (Spieler anschauen)", 1, () -> cfg.invseeKey);
		keyRow(cx, y0 + 30, cw, "Menü öffnen", 2, () -> cfg.menuKey);
		painters.add((c, mx, my) -> {
			c.drawText(this.textRenderer, "Klicke auf eine Taste und drücke dann die neue Taste.", cx, y0 + 70, GREY, true);
			c.drawText(this.textRenderer, "Rücktaste = Taste entfernen · Esc = Abbrechen", cx, y0 + 84, GREY, true);
		});
	}

	// ------------------------------------------------------------ Bausteine

	private void invisible(int x, int y, int w, int h, Runnable action) {
		ButtonWidget b = ButtonWidget.builder(Text.empty(), btn -> action.run())
				.dimensions(x, y, w, h).build();
		b.setAlpha(0f);
		addDrawableChild(b);
	}

	private void tile(int x, int y, int w, int h, String title, Supplier<String> sub,
					  BooleanSupplier on, Runnable click) {
		painters.add((c, mx, my) -> {
			boolean hv = hover(mx, my, x, y, w, h);
			boolean isOn = on.getAsBoolean();
			rrect(c, x, y, w, h, 4, hv ? 0x66000000 : 0x44000000);
			if (isOn) ring(c, x, y, w, h, 4, (Theme.accent(cfg) & 0xFFFFFF) | 0x99000000);
			c.drawText(this.textRenderer, title, x + 8, y + 7, isOn ? WHITE : RED, true);
			c.drawText(this.textRenderer, sub.get(), x + 8, y + 19, isOn ? GREEN : RED, true);
			rrect(c, x + w - 12, y + 6, 5, 5, 2, isOn ? 0xFF55E08A : 0xFFE05555);
		});
		invisible(x, y, w, h, click);
	}

	private void slider(int x, int y, int w, String label, int kind, double init,
						DoubleConsumer set, Supplier<String> text, DoubleSupplier hueSource) {
		final int labelW = 124;
		final int trackX = x + labelW;
		final int trackW = w - labelW - 46;
		final Slider sl = new Slider(trackX, y, trackW, 16, init, set);
		addDrawableChild(sl);
		painters.add((c, mx, my) -> {
			c.drawText(this.textRenderer, label, x, y + 4, WHITE, true);
			int ty = y + 5;
			rrect(c, trackX, ty, trackW, 6, 3, 0x66000000);
			if (kind == 1) {
				for (int i = 0; i < trackW - 4; i++) {
					c.fill(trackX + 2 + i, ty + 1, trackX + 3 + i, ty + 5,
							0xFF000000 | Theme.hsv(i / (float) (trackW - 4), 1f, 1f));
				}
			} else if (kind == 2) {
				float hue = hueSource == null ? 0f : (float) hueSource.getAsDouble();
				int end = 0xFF000000 | Theme.hsv(hue, 1f, 1f);
				for (int i = 0; i < trackW - 4; i++) {
					c.fill(trackX + 2 + i, ty + 1, trackX + 3 + i, ty + 5,
							Theme.lerp(0xFFFFFFFF, end, i / (float) (trackW - 4)));
				}
			} else {
				int fillW = (int) (sl.val() * (trackW - 4));
				if (fillW > 0) rrect(c, trackX + 2, ty + 1, fillW, 4, 2, Theme.accent(cfg));
			}
			int tx = trackX + 4 + (int) (sl.val() * (trackW - 8)) - 3;
			rrect(c, tx, y + 2, 6, 12, 3, 0xFFF2E6E6);
			String t = text.get();
			if (!t.isEmpty()) {
				c.drawText(this.textRenderer, t, x + w - this.textRenderer.getWidth(t), y + 4, GREY, true);
			}
		});
	}

	private void colorRow(int x, int y, String label, IntSupplier get, IntConsumer set) {
		final int bx = x + 70;
		final int bw = 56;
		TextFieldWidget f = new TextFieldWidget(this.textRenderer, bx + 4, y + 5, bw - 8, 12, Text.literal(label));
		f.setDrawsBackground(false);
		f.setEditableColor(0xFFFFFFFF);
		f.setMaxLength(9);
		f.setText(String.format("%08X", get.getAsInt()));
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
		painters.add((c, mx, my) -> {
			c.drawText(this.textRenderer, label, x, y + 6, WHITE, true);
			rrect(c, bx, y + 1, bw, 18, 3, 0x66000000);
			rrect(c, bx + bw + 4, y + 3, 14, 14, 3, 0xFF000000 | get.getAsInt());
		});
	}

	private void keyRow(int x, int y, int w, String label, int which, IntSupplier get) {
		final int bw = 110;
		final int bx = x + w - bw;
		painters.add((c, mx, my) -> {
			boolean ls = listening == which;
			c.drawText(this.textRenderer, label, x, y + 6, WHITE, true);
			boolean hv = hover(mx, my, bx, y, bw, 20);
			rrect(c, bx, y, bw, 20, 4, hv ? 0x66000000 : 0x44000000);
			if (ls) ring(c, bx, y, bw, 20, 4, Theme.accent(cfg));
			String t = ls ? "Taste drücken …" : Keys.name(get.getAsInt());
			c.drawText(this.textRenderer, t, bx + (bw - this.textRenderer.getWidth(t)) / 2, y + 6,
					ls ? Theme.accent(cfg) : WHITE, true);
		});
		invisible(bx, y, bw, 20, () -> {
			listening = (listening == which) ? 0 : which;
			if (listening != 0) {
				long h = this.client.getWindow().getHandle();
				for (int k = 32; k < wasDown.length; k++) wasDown[k] = Keys.down(h, k);
			}
		});
	}

	// ------------------------------------------------------------ Zeichnen

	@Override
	public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
		super.renderBackground(ctx, mx, my, delta);
		gradientRound(ctx, px, py, pw, ph, 6, Theme.top(cfg), Theme.bottom(cfg));
		ring(ctx, px, py, pw, ph, 6, (Theme.accent(cfg) & 0xFFFFFF) | 0x66000000);

		// Kopfzeile
		rrect(ctx, px + 12, py + 9, 16, 16, 4, Theme.accent(cfg));
		ctx.drawText(this.textRenderer, "I", px + 18, py + 13, 0xFF202020, false);
		ctx.drawText(this.textRenderer, "InvTools", px + 34, py + 13, WHITE, true);

		for (Painter p : painters) p.paint(ctx, mx, my);

		// Fußzeile
		ctx.drawText(this.textRenderer, "InvTools v1.0.2", px + 12, py + ph - 16, 0xFF9FD8B0, true);
		String r = "Esc: schließen";
		ctx.drawText(this.textRenderer, r, px + pw - 12 - this.textRenderer.getWidth(r), py + ph - 16, 0xFF9FD8B0, true);
	}

	@Override
	public void render(DrawContext ctx, int mx, int my, float delta) {
		super.render(ctx, mx, my, delta);

		long h = this.client.getWindow().getHandle();

		// Tastenwahl
		if (listening != 0) {
			for (int k = 32; k < wasDown.length; k++) {
				boolean d = Keys.down(h, k);
				if (d && !wasDown[k]) {
					if (k == 256) {
						// Abbrechen
					} else if (k == 259) {
						setKey(listening, -1);
					} else {
						setKey(listening, k);
					}
					listening = 0;
				}
				wasDown[k] = d;
			}
		}

		// Menügröße geändert -> neu aufbauen, sobald die Maustaste losgelassen wurde
		if (builtSize != cfg.menuSizePercent
				&& GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
			this.clearChildren();
			this.init();
		}
	}

	private void setKey(int which, int key) {
		if (which == 1) cfg.invseeKey = key;
		else cfg.menuKey = key;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return listening == 0;
	}

	@Override
	public void close() {
		ModConfig.save();
		this.client.setScreen(parent);
	}

	@Override
	public void removed() {
		ModConfig.save();
	}

	// ------------------------------------------------------------ Hilfsfunktionen

	private static boolean hover(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void drawTab(DrawContext c, int x, int y, int w, int h, String name, boolean sel, boolean hv) {
		rrect(c, x, y, w, h, 4, sel ? 0x66FFFFFF : (hv ? 0x44FFFFFF : 0x33000000));
		if (sel) ring(c, x, y, w, h, 4, Theme.accent(cfg));
		c.drawText(this.textRenderer, name, x + (w - this.textRenderer.getWidth(name)) / 2, y + 5,
				sel ? WHITE : 0xFFD8B8B8, true);
	}

	private static int inset(int r, int i) {
		double dy = r - (i + 0.5);
		return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
	}

	static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) {
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		for (int i = 0; i < r; i++) {
			int in = inset(r, i);
			c.fill(x + in, y + i, x + w - in, y + i + 1, col);
			c.fill(x + in, y + h - i - 1, x + w - in, y + h - i, col);
		}
		c.fill(x, y + r, x + w, y + h - r, col);
	}

	static void ring(DrawContext c, int x, int y, int w, int h, int r, int col) {
		c.fill(x + r, y, x + w - r, y + 1, col);
		c.fill(x + r, y + h - 1, x + w - r, y + h, col);
		c.fill(x, y + r, x + 1, y + h - r, col);
		c.fill(x + w - 1, y + r, x + w, y + h - r, col);
		for (int i = 0; i < r; i++) {
			int in = inset(r, i);
			c.fill(x + in, y + i, x + in + 1, y + i + 1, col);
			c.fill(x + w - in - 1, y + i, x + w - in, y + i + 1, col);
			c.fill(x + in, y + h - i - 1, x + in + 1, y + h - i, col);
			c.fill(x + w - in - 1, y + h - i - 1, x + w - in, y + h - i, col);
		}
	}

	static void gradientRound(DrawContext c, int x, int y, int w, int h, int r, int top, int bottom) {
		for (int i = 0; i < h; i++) {
			int in = 0;
			if (i < r) in = inset(r, i);
			else if (i >= h - r) in = inset(r, h - 1 - i);
			c.fill(x + in, y + i, x + w - in, y + i + 1, Theme.lerp(top, bottom, i / (float) h));
		}
	}

	private static class Slider extends SliderWidget {
		private final DoubleConsumer onChange;

		Slider(int x, int y, int w, int h, double init, DoubleConsumer onChange) {
			super(x, y, w, h, Text.empty(), Math.max(0, Math.min(1, init)));
			this.onChange = onChange;
			this.setAlpha(0f);
		}

		double val() {
			return this.value;
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.empty());
		}

		@Override
		protected void applyValue() {
			onChange.accept(this.value);
		}
	}
}
