package dev.zymekoh.handposition.mixin;

import dev.zymekoh.handposition.gui.HandsConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void kohs$hideHudWhileMoving(CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof HandsConfigScreen screen && screen.isFreeMove()) ci.cancel();
    }
}
