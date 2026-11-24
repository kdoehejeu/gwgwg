package dev.invtools.mixin;

import dev.invtools.AntiLeak;
import dev.invtools.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModels.class)
public abstract class BlockModelsMixin {
	@Inject(method = "getModel", at = @At("HEAD"), cancellable = true, require = 0)
	private void invtools$antiLeak(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
		if (!ModConfig.get().antiLeak) return;
		Block target = AntiLeak.replacement(state.getBlock());
		if (target == null) return;
		cir.setReturnValue(((BlockModels) (Object) this).getModel(target.getDefaultState()));
	}
}
