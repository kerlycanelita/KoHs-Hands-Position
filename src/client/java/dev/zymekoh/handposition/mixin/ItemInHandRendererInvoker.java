package dev.zymekoh.handposition.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererInvoker {
    @Invoker("renderArmWithItem")
    void kohs$renderArm(AbstractClientPlayer player, float partialTick, float pitch,
                        InteractionHand hand, float attack, ItemStack item, float equip,
                        PoseStack poses, SubmitNodeCollector collector, int light);
}
