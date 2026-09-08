package dev.zymekoh.handposition.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.item.ItemStack;

public record HandsPreviewState(int x1, int y1, LocalPlayer player, ItemStack mainHand,
                                ItemStack offHand, float partialTick, float fov)
        implements PictureInPictureRenderState {
    @Override public int x0() { return 0; }
    @Override public int y0() { return 0; }
    @Override public float scale() { return 1.0F; }
    @Override public ScreenRectangle scissorArea() { return null; }
    @Override public ScreenRectangle bounds() { return new ScreenRectangle(0, 0, x1, y1); }
}
