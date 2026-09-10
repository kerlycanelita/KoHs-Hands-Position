package dev.zymekoh.handposition.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.zymekoh.handposition.gui.HandsConfigScreen;
import dev.zymekoh.handposition.HandSide;
import dev.zymekoh.handposition.render.HandTransform;
import dev.zymekoh.handposition.render.HandsPreviewRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void kohs$hideUnderlyingHands(CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof HandsConfigScreen) ci.cancel();
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void kohs$transformHand(AbstractClientPlayer player, float partialTick, float pitch,
                                    InteractionHand hand, float attack, ItemStack item, float equip,
                                    PoseStack poses, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        var gameState = Minecraft.getInstance().gameRenderer.getGameRenderState();
        float fov = gameState.levelRenderState.cameraRenderState.hudFov;
        float aspect = (float) gameState.windowRenderState.width / Math.max(1, gameState.windowRenderState.height);
        if (HandsPreviewRenderer.isRenderingPreview()) {
            fov = HandsPreviewRenderer.previewFov();
            aspect = HandsPreviewRenderer.previewAspect();
        }
        HandTransform.apply(poses, hand == InteractionHand.MAIN_HAND ? HandSide.MAIN : HandSide.OFF,
                hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite(), fov, aspect);
    }

    @ModifyExpressionValue(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 1))
    private boolean kohs$singleHandedMapPreview(boolean emptyOffhand) {
        // The preview must not invent an empty offhand, even for a map.
        return !HandsPreviewRenderer.isRenderingPreview() && emptyOffhand;
    }
}
