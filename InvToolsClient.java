package dev.invtools;

import dev.invtools.mixin.ButtonWidgetAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

public class InvToolsClient implements ClientModInitializer {
	private static KeyBinding invseeKey;

	private static class DelayState {
		long start = -1;
		boolean bypass = false;
	}

	@Override
	public void onInitializeClient() {
		ModConfig.load();

		invseeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.invtools.invsee",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				KeyBinding.Category.create(Identifier.of("invtools", "main"))));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (invseeKey.wasPressed()) {
				if (client.player == null || client.world == null || client.currentScreen != null) continue;
				if (!ModConfig.get().invseeEnabled) continue;
				invseeLookedAt(client);
			}
		});

		ClientSendMessageEvents.COMMAND.register(cmd -> {
			if (cmd.toLowerCase().startsWith("invsee")) InvseeCounter.markInvsee();
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
			if (!ModConfig.get().disconnectDelayEnabled || st.bypass) {
				original.onPress(b);
				return;
			}
			if (st.start < 0) st.start = Util.getMeasuringTimeMs();
			b.active = false;
		});

		ScreenEvents.afterTick(screen).register(s -> {
			if (st.start < 0 || st.bypass) return;
			ModConfig cfg = ModConfig.get();
			long remaining = cfg.disconnectDelaySeconds * 1000L - (Util.getMeasuringTimeMs() - st.start);
			if (remaining > 0) {
				button.setMessage(Text.literal(originalText.getString()
						+ " (" + String.format("%.1f", remaining / 1000.0) + "s)")
						.withColor(cfg.colorCountdown & 0xFFFFFF));
			} else {
				st.bypass = true;
				button.setMessage(originalText);
				button.active = true;
				original.onPress(button);
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
