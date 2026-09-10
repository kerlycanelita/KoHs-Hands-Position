package dev.zymekoh.handposition;

import dev.zymekoh.handposition.render.HandsPreviewRenderer;
import dev.zymekoh.handposition.render.HandsPreviewState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;

public final class HandsPositionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandsConfig.load();
        PictureInPictureRendererRegistry.register(context -> new HandsPreviewRenderer<>(context.bufferSource(), HandsPreviewState.Main.class));
        PictureInPictureRendererRegistry.register(context -> new HandsPreviewRenderer<>(context.bufferSource(), HandsPreviewState.Off.class));
    }
}
