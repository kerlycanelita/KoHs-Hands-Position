package dev.zymekoh.handposition.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.zymekoh.handposition.mixin.ItemInHandRendererInvoker;
import dev.zymekoh.handposition.HandSide;
import dev.zymekoh.handposition.gui.HandPickRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.world.InteractionHand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HandsPreviewRenderer<T extends HandsPreviewState> extends PictureInPictureRenderer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger("kohs_hands_position");
    private final Class<T> stateClass;
    private final Projection projection = new Projection();
    private final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("KoHs hands preview");
    private static boolean renderingPreview;
    private static float previewFov = 70.0F;
    private static float previewAspect = 1.0F;

    public HandsPreviewRenderer(MultiBufferSource.BufferSource buffers, Class<T> stateClass) {
        super(buffers);
        this.stateClass = stateClass;
    }
    public static boolean isRenderingPreview() { return renderingPreview; }
    public static float previewFov() { return previewFov; }
    public static float previewAspect() { return previewAspect; }
    @Override public Class<T> getRenderStateClass() { return stateClass; }
    @Override protected String getTextureLabel() { return "KoHs hand " + stateClass.getSimpleName(); }

    @Override
    protected void renderToTexture(T state, PoseStack unusedGuiPose) {
        Minecraft minecraft = Minecraft.getInstance();
        var frame = state.frame();
        if (minecraft.player != frame.player() || minecraft.level == null) {
            if (frame.pick() != null) frame.pick().complete(state.hand(), false);
            return;
        }
        previewFov = HandTransform.validFov(frame.fov());
        previewAspect = (float) state.x1() / Math.max(1, state.y1());
        projection.setupPerspective(0.05F, 100.0F, previewFov, state.x1(), state.y1());
        RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection), ProjectionType.PERSPECTIVE);
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix().identity();
        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        renderingPreview = true;
        try {
            // Use the same vanilla arm/item renderer and transform as gameplay,
            // with the real skin/stacks and a resting pose suitable for editing.
            var renderer = (ItemInHandRendererInvoker) minecraft.gameRenderer.itemInHandRenderer;
            var collector = minecraft.gameRenderer.getSubmitNodeStorage();
            PoseStack poses = new PoseStack();
            boolean main = state.hand() == HandSide.MAIN;
            if (main || !frame.offHand().isEmpty()) {
                renderer.kohs$renderArm(frame.player(), frame.partialTick(), frame.player().getXRot(),
                        main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, 0.0F,
                        main ? frame.mainHand() : frame.offHand(), 0.0F, poses, collector, LightCoordsUtil.FULL_BRIGHT);
            }
            minecraft.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
            bufferSource.endBatch();
            pickRenderedPixel(state);
        } finally {
            renderingPreview = false;
            modelView.popMatrix();
            minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        }
    }

    private void pickRenderedPixel(T state) {
        HandPickRequest pick = state.frame().pick();
        if (pick == null || !pick.claim(state.hand())) return;
        var textureView = RenderSystem.outputColorTextureOverride;
        if (textureView == null || pick.x < 0 || pick.y < 0 || pick.x >= pick.width || pick.y >= pick.height) {
            pick.complete(state.hand(), false);
            return;
        }
        var texture = textureView.texture();
        int textureWidth = texture.getWidth(0);
        int textureHeight = texture.getHeight(0);
        int x = Math.clamp((int) (pick.x * textureWidth / pick.width), 0, textureWidth - 1);
        // PIP's blit flips V. Read the matching bottom-up texture coordinate.
        int y = textureHeight - 1 - Math.clamp((int) (pick.y * textureHeight / pick.height), 0, textureHeight - 1);
        var device = RenderSystem.getDevice();
        GpuBuffer readback = device.createBuffer(() -> "KoHs hand selection", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, 4);
        var encoder = device.createCommandEncoder();
        try {
            // Four bytes per hand per sample. Hover requests are throttled by
            // the screen, and an ongoing drag needs no further readbacks.
            encoder.copyTextureToBuffer(texture, readback, 0L, () -> {
                try (GpuBuffer.MappedView mapped = encoder.mapBuffer(readback, true, false)) {
                    pick.complete(state.hand(), (mapped.data().get(3) & 0xFF) > 8);
                } catch (RuntimeException exception) {
                    pick.complete(state.hand(), false);
                    LOGGER.warn("Could not read hand selection", exception);
                } finally {
                    readback.close();
                }
            }, 0, x, y, 1, 1);
        } catch (RuntimeException exception) {
            readback.close();
            pick.complete(state.hand(), false);
            LOGGER.warn("Could not request hand selection", exception);
        }
    }

    @Override public void close() {
        super.close();
        projectionBuffer.close();
    }
}
