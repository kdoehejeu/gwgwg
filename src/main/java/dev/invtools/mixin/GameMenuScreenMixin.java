package dev.invtools.mixin;

import dev.invtools.GameMenuAccess;
import dev.invtools.ModConfig;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen implements GameMenuAccess {
	@Unique private long invtools$start = -1;
	@Unique private boolean invtools$bypass = false;
	@Unique private Text invtools$originalText = null;

	protected GameMenuScreenMixin(Text title) {
		super(title);
	}

	@Shadow
	protected abstract void disconnect();

	@Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
	private void invtools$delayDisconnect(CallbackInfo ci) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.disconnectDelayEnabled || invtools$bypass) return;
		if (invtools$start < 0) invtools$start = Util.getMeasuringTimeMs();
		ci.cancel();
	}

	@Override
	public void invtools$tick() {
		if (invtools$start < 0 || invtools$bypass) return;
		ModConfig cfg = ModConfig.get();
		long total = cfg.disconnectDelaySeconds * 1000L;
		long remaining = total - (Util.getMeasuringTimeMs() - invtools$start);

		for (Element e : this.children()) {
			if (e instanceof ButtonWidget bw
					&& bw.getMessage().getContent() instanceof TranslatableTextContent t
					&& (t.getKey().equals("menu.disconnect") || t.getKey().equals("menu.returnToMenu"))) {
				if (invtools$originalText == null) invtools$originalText = bw.getMessage();
				double sec = Math.max(0, remaining) / 1000.0;
				bw.setMessage(Text.literal(invtools$originalText.getString() + " (" + String.format("%.1f", sec) + "s)")
						.withColor(cfg.colorCountdown & 0xFFFFFF));
				break;
			}
		}

		if (remaining <= 0) {
			invtools$bypass = true;
			this.disconnect();
		}
	}
}
