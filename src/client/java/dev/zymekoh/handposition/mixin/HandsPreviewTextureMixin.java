package dev.zymekoh.handposition.mixin;

import com.mojang.blaze3d.textures.GpuTexture;
import dev.zymekoh.handposition.render.HandsPreviewRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PictureInPictureRenderer.class)
public abstract class HandsPreviewTextureMixin {
    @ModifyArg(method = "prepareTexturesAndProjection", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuDevice;createTexture(Ljava/util/function/Supplier;ILcom/mojang/blaze3d/textures/TextureFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;",
            ordinal = 0), index = 1)
    private int kohs$allowHandSelection(int usage) {
        // Vanilla PIP textures only support upload, sampling and rendering.
        // Reading a clicked pixel requires COPY_SRC, validated by CommandEncoder.
        return (Object) this instanceof HandsPreviewRenderer<?> ? usage | GpuTexture.USAGE_COPY_SRC : usage;
    }
}
