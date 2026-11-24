package dev.invtools;

import dev.invtools.mixin.ButtonWidgetAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class InvToolsClient implements ClientModInitializer {
	private static boolean invWas = false;
	private static boolean menuWas = false;
	private static long lastWarn = 0;

	private static class DelayState {
		long start = -1;
		boolean confirmed = false; // Countdown vorbei -> nächster Klick trennt wirklich
	}

	@Override
	public void onInitializeClient() {
		ModConfig.load();

		// Hotkeys (eigene Tasten, im Menü einstellbar)
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.getWindow() == null) return;
			long h = client.getWindow().getHandle();
			ModConfig cfg = ModConfig.get();
			boolean inv = Keys.down(h, cfg.invseeKey);
			boolean menu = Keys.down(h, cfg.menuKey);
			boolean noScreen = client.currentScreen == null;
			if (inv && !invWas && noScreen && cfg.invseeEnabled && client.player != null && client.world != null) {
				invseeLookedAt(client);
			}
			if (menu && !menuWas && noScreen) {
				client.setScreen(new ConfigScreen(null));
			}
			invWas = inv;
			menuWas = menu;
		});

		ClientSendMessageEvents.COMMAND.register(cmd -> {
			if (cmd.toLowerCase().startsWith("invsee")) InvseeCounter.markInvsee();
		});

		// Werkzeugschutz: Abbauen blockieren, wenn das Werkzeug fast kaputt ist
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			ModConfig cfg = ModConfig.get();
			if (!cfg.toolProtect) return ActionResult.PASS;
			MinecraftClient mc = MinecraftClient.getInstance();
			if (player != mc.player || player.isCreative()) return ActionResult.PASS;
			ItemStack st = player.getMainHandStack();
			if (st.isEmpty() || !st.isDamageable()) return ActionResult.PASS;
			int max = st.getMaxDamage();
			if (max <= 0) return ActionResult.PASS;
			double pct = (max - st.getDamage()) * 100.0 / max;
			if (pct <= cfg.toolProtectPercent) {
				long now = Util.getMeasuringTimeMs();
				if (now - lastWarn > 800) {
					lastWarn = now;
					mc.inGameHud.setOverlayMessage(Text.literal("Dein Werkzeug hat nur noch "
							+ Math.max(0, (int) Math.round(pct)) + " % Haltbarkeit – bitte reparieren!")
							.withColor(0xFF5555), false);
				}
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof GameMenuScreen) {
				try {
					hookDisconnect(screen);
				} catch (Throwable t) {
					System.err.println("[InvTools] Disconnect-Verzögerung nicht verfügbar: " + t);
				}
			}
			if (screen instanceof HandledScreen<?>) {
				ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
					try {
						InvseeCounter.render(client, s, ctx);
					} catch (Throwable ignored) {}
				});
			}
		});
	}

	private static void hookDisconnect(Screen screen) {
		ButtonWidget btn = null;
		for (Element e : screen.children()) {
			if (e instanceof ButtonWidget bw
					&& bw.getMessage().getContent() instanceof TranslatableTextContent t
					&& (t.getKey().equals("menu.disconnect") || t.getKey().equals("menu.returnToMenu"))) {
				btn = bw;
				break;
			}
		}
		if (btn == null) return;

		final ButtonWidget button = btn;
		final ButtonWidgetAccessor acc = (ButtonWidgetAccessor) button;
		final ButtonWidget.PressAction original = acc.invtools$getOnPress();
		final Text originalText = button.getMessage();
		final DelayState st = new DelayState();

		acc.invtools$setOnPress(b -> {
			if (!ModConfig.get().disconnectDelayEnabled || st.confirmed) {
				// Zweiter Klick nach dem Countdown -> wirklich trennen
				original.onPress(b);
				return;
			}
			if (st.start < 0) {
				st.start = Util.getMeasuringTimeMs();
				b.active = false; // während des Countdowns gesperrt
			}
		});

		ScreenEvents.afterTick(screen).register(s -> {
			if (st.start < 0 || st.confirmed) return;
			ModConfig cfg = ModConfig.get();
			long remaining = cfg.disconnectDelaySeconds * 1000L - (Util.getMeasuringTimeMs() - st.start);
			if (remaining > 0) {
				button.setMessage(Text.literal(originalText.getString()
						+ " (" + String.format("%.1f", remaining / 1000.0) + "s)")
						.withColor(cfg.colorCountdown & 0xFFFFFF));
			} else {
				// Countdown vorbei: Knopf wieder freigeben, erneuter Klick bestätigt
				st.confirmed = true;
				button.active = true;
				button.setMessage(Text.literal(originalText.getString() + " – bestätigen")
						.withColor(0x55FF55));
			}
		});
	}

	private static void invseeLookedAt(MinecraftClient mc) {
		Entity cam = mc.getCameraEntity();
		if (cam == null) return;
		double range = ModConfig.get().invseeRange;
		Vec3d start = cam.getCameraPosVec(1.0f);
		Vec3d dir = cam.getRotationVec(1.0f);
		Vec3d end = start.add(dir.multiply(range));
		Box box = cam.getBoundingBox().stretch(dir.multiply(range)).expand(1.0);

		EntityHitResult hit = ProjectileUtil.raycast(cam, start, end, box,
				e -> e instanceof PlayerEntity && e != mc.player && !e.isSpectator(), range * range);
		if (hit == null) return;

		HitResult block = mc.world.raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, cam));
		if (block.getType() == HitResult.Type.BLOCK
				&& block.getPos().squaredDistanceTo(start) < hit.getPos().squaredDistanceTo(start)) return;

		String name = hit.getEntity().getName().getString();
		InvseeCounter.markInvsee();
		mc.player.networkHandler.sendChatCommand("invsee " + name);
	}
}
