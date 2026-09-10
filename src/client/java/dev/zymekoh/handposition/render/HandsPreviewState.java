package dev.zymekoh.handposition.render;

import dev.zymekoh.handposition.HandSide;
import dev.zymekoh.handposition.gui.HandPickRequest;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.item.ItemStack;

public sealed interface HandsPreviewState extends PictureInPictureRenderState {
    record Frame(int width, int height, LocalPlayer player, ItemStack mainHand,
                 ItemStack offHand, float partialTick, float fov, HandPickRequest pick) { }

    Frame frame();
    HandSide hand();
    @Override default int x0() { return 0; }
    @Override default int y0() { return 0; }
    @Override default int x1() { return frame().width(); }
    @Override default int y1() { return frame().height(); }
    @Override default float scale() { return 1.0F; }
    @Override default ScreenRectangle scissorArea() { return null; }
    @Override default ScreenRectangle bounds() { return new ScreenRectangle(0, 0, x1(), y1()); }

    // Independent state classes get independent PIP textures. A click can read
    // each hand's actual alpha without one hand overwriting the other's texture.
    record Main(Frame frame) implements HandsPreviewState {
        @Override public HandSide hand() { return HandSide.MAIN; }
    }
    record Off(Frame frame) implements HandsPreviewState {
        @Override public HandSide hand() { return HandSide.OFF; }
    }
}
