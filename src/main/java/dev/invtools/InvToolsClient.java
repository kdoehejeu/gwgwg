package dev.invtools;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

public class InvToolsClient implements ClientModInitializer {
	private static KeyBinding invseeKey;

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

		// Eigene /invsee-Befehle merken, damit der Counter erscheint
		ClientSendMessageEvents.COMMAND.register(cmd -> {
			if (cmd.toLowerCase().startsWith("invsee")) InvseeCounter.markInvsee();
		});

		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof GameMenuScreen gms) {
				ScreenEvents.afterTick(screen).register(s -> ((GameMenuAccess) gms).invtools$tick());
			}
			if (screen instanceof HandledScreen<?>) {
				ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) ->
						InvseeCounter.render(client, s, ctx));
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

		// Nicht durch Blöcke hindurch
		HitResult block = mc.world.raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, cam));
		if (block.getType() == HitResult.Type.BLOCK
				&& block.getPos().squaredDistanceTo(start) < hit.getPos().squaredDistanceTo(start)) return;

		String name = hit.getEntity().getName().getString();
		InvseeCounter.markInvsee();
		mc.player.networkHandler.sendChatCommand("invsee " + name);
	}
}
