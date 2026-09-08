package dev.zymekoh.handposition.gui;

import dev.zymekoh.handposition.HandsConfig;
import dev.zymekoh.handposition.mixin.GuiGraphicsExtractorAccessor;
import dev.zymekoh.handposition.render.HandsPreviewState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class HandsConfigScreen extends Screen {
    private final Screen parent;
    private int margin;
    private int panelBottom;
    private boolean dirty;

    public HandsConfigScreen(Screen parent) {
        super(Component.literal("KoHs Hands Position"));
        this.parent = parent;
    }

    @Override protected void init() {
        margin = width < 420 ? 8 : 16;
        int available = Math.max(20, width - margin * 2);
        int controlsWidth = Math.min(620, available);
        int left = (width - controlsWidth) / 2;
        int top = height < 220 ? 24 : 34;
        if (available >= 380) {
            int sliderWidth = (controlsWidth - 12) / 2;
            addRenderableWidget(new SettingSlider(left, top, sliderWidth, true));
            addRenderableWidget(new SettingSlider(left + sliderWidth + 12, top, sliderWidth, false));
            panelBottom = top + 30;
        } else {
            addRenderableWidget(new SettingSlider(left, top, controlsWidth, true));
            addRenderableWidget(new SettingSlider(left, top + 26, controlsWidth, false));
            panelBottom = top + 56;
        }
        int doneWidth = Math.min(120, available);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - doneWidth) / 2, Math.max(panelBottom + 4, height - 28), doneWidth, 20).build());
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        // Do not call the default background: its blur would obscure the world.
        graphics.fill(0, 0, width, height, minecraft.level == null ? 0xFF251039 : 0x98532180);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        if (minecraft.player != null && minecraft.level != null && width > 0 && height > 0) {
            float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            float fov = minecraft.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.hudFov;
            if (!Float.isFinite(fov) || fov < 1.0F) fov = 70.0F;
            ((GuiGraphicsExtractorAccessor) graphics).kohs$getGuiRenderState().addPicturesInPictureState(
                    new HandsPreviewState(width, height, minecraft.player,
                            minecraft.player.getMainHandItem().copy(), minecraft.player.getOffhandItem().copy(), partialTick, fov));
            // Explicit strata guarantee background < hands < controls.
            graphics.nextStratum();
        } else {
            graphics.centeredText(font, Component.translatable("screen.kohs_hands_position.join_world"),
                    width / 2, Math.max(panelBottom + 12, height / 2), 0xFFEADAF6);
        }
        graphics.fill(0, 0, width, panelBottom, 0xAD210B35);
        graphics.fill(0, panelBottom - 1, width, panelBottom, 0xFF9D64CC);
        graphics.centeredText(font, title, width / 2, height < 220 ? 8 : 13, 0xFFF7ECFF);
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
    }

    @Override public void onClose() {
        saveIfDirty();
        minecraft.setScreen(parent);
    }
    @Override public void removed() { saveIfDirty(); }
    @Override public boolean isPauseScreen() { return true; }
    @Override public boolean isInGameUi() { return true; }

    private void saveIfDirty() {
        if (dirty) {
            HandsConfig.get().save();
            dirty = false;
        }
    }

    private final class SettingSlider extends AbstractSliderButton {
        private final boolean sizeSetting;

        SettingSlider(int x, int y, int width, boolean sizeSetting) {
            super(x, y, width, 20, Component.empty(), normalizedValue(sizeSetting));
            this.sizeSetting = sizeSetting;
            updateMessage();
        }

        @Override protected void updateMessage() {
            int displayed = (int) Math.round((sizeSetting ? HandsConfig.get().size : HandsConfig.get().spacing) * 100.0);
            setMessage(Component.translatable(sizeSetting ? "screen.kohs_hands_position.size" : "screen.kohs_hands_position.spacing",
                    sizeSetting ? displayed + "%" : (displayed > 0 ? "+" : "") + displayed + "%"));
        }

        @Override protected void applyValue() {
            HandsConfig config = HandsConfig.get();
            if (sizeSetting) config.size = Math.round((HandsConfig.MIN_SIZE + value * (HandsConfig.MAX_SIZE - HandsConfig.MIN_SIZE)) * 100.0) / 100.0;
            else config.spacing = Math.round((HandsConfig.MIN_SPACING + value * (HandsConfig.MAX_SPACING - HandsConfig.MIN_SPACING)) * 100.0) / 100.0;
            dirty = true;
        }
    }

    private static double normalizedValue(boolean sizeSetting) {
        return sizeSetting ? (HandsConfig.get().size - HandsConfig.MIN_SIZE) / (HandsConfig.MAX_SIZE - HandsConfig.MIN_SIZE)
                : (HandsConfig.get().spacing - HandsConfig.MIN_SPACING) / (HandsConfig.MAX_SPACING - HandsConfig.MIN_SPACING);
    }
}
