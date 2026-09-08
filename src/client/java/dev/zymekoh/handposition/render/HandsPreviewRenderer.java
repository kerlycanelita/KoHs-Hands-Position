package dev.zymekoh.handposition.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.zymekoh.handposition.mixin.ItemInHandRendererInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.world.InteractionHand;

public final class HandsPreviewRenderer extends PictureInPictureRenderer<HandsPreviewState> {
    private final Projection projection = new Projection();
    private final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("KoHs hands preview");
    private static boolean renderingPreview;

    public HandsPreviewRenderer(MultiBufferSource.BufferSource buffers) { super(buffers); }
    public static boolean isRenderingPreview() { return renderingPreview; }
    @Override public Class<HandsPreviewState> getRenderStateClass() { return HandsPreviewState.class; }
    @Override protected String getTextureLabel() { return "KoHs hands"; }

    @Override
    protected void renderToTexture(HandsPreviewState state, PoseStack unusedGuiPose) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != state.player() || minecraft.level == null) return;
        projection.setupPerspective(0.05F, 100.0F, state.fov(), state.x1(), state.y1());
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
            renderer.kohs$renderArm(state.player(), state.partialTick(), state.player().getXRot(),
                    InteractionHand.MAIN_HAND, 0.0F, state.mainHand(), 0.0F, poses, collector, LightCoordsUtil.FULL_BRIGHT);
            if (!state.offHand().isEmpty()) {
                renderer.kohs$renderArm(state.player(), state.partialTick(), state.player().getXRot(),
                        InteractionHand.OFF_HAND, 0.0F, state.offHand(), 0.0F, poses, collector, LightCoordsUtil.FULL_BRIGHT);
            }
            minecraft.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
            bufferSource.endBatch();
        } finally {
            renderingPreview = false;
            modelView.popMatrix();
            minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        }
    }

    @Override public void close() {
        super.close();
        projectionBuffer.close();
    }
}
