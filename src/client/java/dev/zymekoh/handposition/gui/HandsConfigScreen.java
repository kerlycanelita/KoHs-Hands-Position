package dev.zymekoh.handposition.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.zymekoh.handposition.HandSide;
import dev.zymekoh.handposition.HandsConfig;
import dev.zymekoh.handposition.gui.HandsTheme.ActionButton;
import dev.zymekoh.handposition.mixin.GuiGraphicsExtractorAccessor;
import dev.zymekoh.handposition.render.HandTransform;
import dev.zymekoh.handposition.render.HandsPreviewState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class HandsConfigScreen extends Screen {
    private final Screen parent;
    private final List<SettingSlider> sliders = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
    private HandsScreenLayout layout;
    private boolean sizeTab = true;
    private boolean freeMove;
    private boolean dirty;
    private final HandDragController drag = new HandDragController();
    private HandPickRequest hoverPick;
    private HandSide hoveredHand;
    private long nextHoverTime;
    private HandSide frontHand = HandSide.OFF;

    public HandsConfigScreen(Screen parent) {
        super(Component.literal("KoHs Hands Position"));
        this.parent = parent;
    }

    @Override protected void init() {
        cancelDrag();
        sliders.clear();
        groups.clear();
        layout = HandsScreenLayout.calculate(width, height);
        if (freeMove) {
            addFooterButton(text("reset"), true, HandsTheme.OFF, false, () -> {
                cancelDrag();
                HandsConfig.get().resetPositions();
                dirty = true;
            }).setTooltip(Tooltip.create(text("reset_positions_hint")));
            addFooterButton(CommonComponents.GUI_DONE, false, HandsTheme.GREEN, true, this::finishFreeMove);
            return;
        }
        if (layout.tabs()) {
            int tabWidth = (layout.contentWidth() - 8) / 2;
            ActionButton sizeButton = addRenderableWidget(new ActionButton(layout.left(), layout.tabY(), tabWidth, 20,
                    text("size_group"), HandsTheme.GLOBAL, false, () -> selectTab(true)));
            ActionButton spacingButton = addRenderableWidget(new ActionButton(layout.left() + tabWidth + 8,
                    layout.tabY(), tabWidth, 20, text("spacing_group"), HandsTheme.GLOBAL, false, () -> selectTab(false)));
            sizeButton.selected(sizeTab);
            spacingButton.selected(!sizeTab);
            addGroup(sizeTab, layout.left());
        } else {
            addGroup(true, layout.cardX(0));
            addGroup(false, layout.cardX(1));
        }
        Button move = addFooterButton(text("free_move"), true, HandsTheme.MAIN, true, () -> {
            freeMove = true;
            rebuildWidgets();
        });
        move.active = hasPlayer();
        move.setTooltip(Tooltip.create(text(hasPlayer() ? "free_move_hint" : "join_world")));
        addFooterButton(CommonComponents.GUI_DONE, false, HandsTheme.GREEN, false, this::onClose);
        syncControls();
    }

    private void selectTab(boolean size) {
        if (sizeTab == size) return;
        sizeTab = size;
        rebuildWidgets();
    }

    private ActionButton addFooterButton(Component label, boolean first, int accent, boolean primary, Runnable action) {
        int buttonWidth = layout.footerButtonWidth();
        int left = (width - buttonWidth * 2 - 8) / 2;
        return addRenderableWidget(new ActionButton(left + (first ? 0 : buttonWidth + 8), layout.footerY(),
                buttonWidth, layout.footerHeight(), label, accent, primary, action));
    }

    private void addGroup(boolean size, int left) {
        int buttonWidth = Math.min(106, layout.cardWidth() / 2);
        ActionButton global = addRenderableWidget(new ActionButton(left + layout.cardWidth() - buttonWidth - 8,
                layout.cardTop() + 4, buttonWidth, 18, text("use_global"), HandsTheme.GLOBAL, false, () -> {
            if (size) HandsConfig.get().perHandSize = false;
            else HandsConfig.get().perHandSpacing = false;
            dirty = true;
            syncControls();
        }));
        global.setTooltip(Tooltip.create(text("use_global_hint")));
        groups.add(new Group(size, left, global));
        for (int row = 0; row < 3; row++) {
            HandSide hand = row == 0 ? null : row == 1 ? HandSide.MAIN : HandSide.OFF;
            SettingSlider slider = new SettingSlider(left + 8, layout.sliderY(row), layout.cardWidth() - 16, size, hand);
            sliders.add(addRenderableWidget(slider));
        }
    }

    private void syncControls() {
        for (SettingSlider slider : sliders) slider.sync();
        syncGroupButtons();
    }

    private void syncGroupButtons() {
        for (Group group : groups) {
            boolean separate = perHand(group.size());
            group.global().active = separate;
            group.global().selected(!separate);
            group.global().setMessage(text(separate ? "use_global" : "global_active"));
        }
    }

    private boolean perHand(boolean size) {
        return size ? HandsConfig.get().perHandSize : HandsConfig.get().perHandSpacing;
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        graphics.fill(0, 0, width, height, minecraft.level == null ? 0xFF211731 : 0x98532180);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        if (!minecraft.isWindowActive() || !hasPlayer()
                || (drag.selection() == HandSide.OFF && minecraft.player.getOffhandItem().isEmpty())) cancelDrag();
        if (freeMove) {
            updateDrag(mouseX, mouseY);
            updateHover(mouseX, mouseY);
        }
        extractHands(graphics);
        if (!freeMove) {
            graphics.fill(0, 0, width, layout.cardBottom() + 6, 0x7614041F);
            graphics.centeredText(font, title, width / 2, layout.shortScreen() ? 8 : 12, HandsTheme.TEXT);
            if (!layout.shortScreen()) graphics.centeredText(font, text("subtitle"), width / 2, 27, HandsTheme.MUTED);
            for (Group group : groups) {
                int x = group.left();
                HandsTheme.panel(graphics, x, layout.cardTop(), layout.cardWidth(), layout.cardBottom() - layout.cardTop(),
                        HandsTheme.PANEL, HandsTheme.PANEL_EDGE);
                graphics.fill(x + 2, layout.cardTop() + 5, x + 4, layout.cardTop() + 20, HandsTheme.GLOBAL);
                String heading = text(group.size() ? "size_group" : "spacing_group").getString();
                graphics.text(font, font.plainSubstrByWidth(heading, group.global().getX() - x - 14),
                        x + 10, layout.cardTop() + 9, HandsTheme.TEXT, false);
                if (!layout.shortScreen()) graphics.text(font, text(perHand(group.size()) ? "separate_mode" : "linked_mode"),
                        x + 10, layout.cardTop() + 22, HandsTheme.MUTED, false);
            }
            if (layout.footerY() - layout.cardBottom() >= 26) {
                drawHint(graphics, text(hasPlayer() ? "preview_hint" : "join_world"), layout.footerY() - 18);
            }
        } else {
            drawHint(graphics, text("drag_hint"), 10);
            HandSide selected = drag.holding() ? drag.selection() : hoveredHand;
            if (selected != null) {
                graphics.requestCursor(CursorTypes.RESIZE_ALL);
                Component label = Component.translatable("screen.kohs_hands_position." +
                        (drag.holding() ? "dragging_hand" : "drag_hand"), text(selected == HandSide.MAIN ? "main_label" : "off_label"));
                int labelWidth = Math.min(width - 16, font.width(label) + 16);
                int labelX = Math.clamp(mouseX + 12, 8, Math.max(8, width - labelWidth - 8));
                int labelY = Math.clamp(mouseY - 26, 32, Math.max(32, layout.footerY() - 26));
                HandsTheme.panel(graphics, labelX, labelY, labelWidth, 20, HandsTheme.PANEL,
                        selected == HandSide.MAIN ? HandsTheme.MAIN : HandsTheme.OFF);
                graphics.text(font, font.plainSubstrByWidth(label.getString(), labelWidth - 12), labelX + 6, labelY + 6, HandsTheme.TEXT, false);
            } else if (drag.pending()) {
                graphics.requestCursor(CursorTypes.CROSSHAIR);
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
    }

    private void drawHint(GuiGraphicsExtractor graphics, Component label, int y) {
        String line = font.plainSubstrByWidth(label.getString(), width - 32);
        int hintWidth = font.width(line) + 16;
        HandsTheme.panel(graphics, (width - hintWidth) / 2, y - 4, hintWidth, 17, 0xCE1B0928, 0x99B85BE8);
        graphics.centeredText(font, line, width / 2, y, HandsTheme.TEXT);
    }

    private void updateHover(int mouseX, int mouseY) {
        if (!hasPlayer() || drag.pending() || mouseY >= layout.footerY() - 4) {
            hoverPick = null;
            hoveredHand = null;
            return;
        }
        if (hoverPick != null && hoverPick.ready()) {
            hoveredHand = Math.abs(mouseX - hoverPick.x) <= 3 && Math.abs(mouseY - hoverPick.y) <= 3 ? hoverPick.selection() : null;
            hoverPick = null;
        }
        long now = System.nanoTime();
        if (hoverPick == null && now >= nextHoverTime) {
            hoverPick = new HandPickRequest(mouseX, mouseY, width, height, true,
                    !minecraft.player.getOffhandItem().isEmpty(), frontHand);
            nextHoverTime = now + 75_000_000L;
        }
    }

    private void extractHands(GuiGraphicsExtractor graphics) {
        if (!hasPlayer() || width <= 0 || height <= 0) return;
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float fov = HandTransform.validFov(minecraft.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.hudFov);
        var frame = new HandsPreviewState.Frame(width, height, minecraft.player,
                minecraft.player.getMainHandItem().copy(), minecraft.player.getOffhandItem().copy(),
                partialTick, fov, drag.request() != null ? drag.request() : hoverPick);
        HandSide back = frontHand == HandSide.MAIN ? HandSide.OFF : HandSide.MAIN;
        addHand(graphics, frame, back);
        addHand(graphics, frame, frontHand);
        graphics.nextStratum();
    }

    private void addHand(GuiGraphicsExtractor graphics, HandsPreviewState.Frame frame, HandSide hand) {
        if (hand == HandSide.OFF && frame.offHand().isEmpty()) {
            if (frame.pick() != null) frame.pick().complete(HandSide.OFF, false);
            return;
        }
        var state = ((GuiGraphicsExtractorAccessor) graphics).kohs$getGuiRenderState();
        state.addPicturesInPictureState(hand == HandSide.MAIN ? new HandsPreviewState.Main(frame) : new HandsPreviewState.Off(frame));
        graphics.nextStratum();
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (!freeMove || event.button() != 0 || !hasPlayer()) return false;
        cancelDrag();
        setFocused(null);
        setDragging(true);
        drag.begin(new HandPickRequest(event.x(), event.y(), width, height, true,
                !minecraft.player.getOffhandItem().isEmpty(), frontHand), HandsConfig.get());
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (freeMove && drag.holding() && event.button() == 0) {
            updateDrag(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        boolean consumed = freeMove && drag.holding() && event.button() == 0;
        if (consumed) {
            dirty |= drag.release(event.x(), event.y(), HandsConfig.get());
            if (drag.selection() != null) frontHand = drag.selection();
            setDragging(false);
        }
        return super.mouseReleased(event) || consumed;
    }

    private void updateDrag(double mouseX, double mouseY) {
        dirty |= drag.update(mouseX, mouseY, HandsConfig.get());
        if (drag.selection() != null) frontHand = drag.selection();
    }

    private void cancelDrag() {
        drag.cancel();
        hoverPick = null;
        hoveredHand = null;
        setDragging(false);
    }

    private boolean hasPlayer() { return minecraft != null && minecraft.player != null && minecraft.level != null; }
    public boolean isFreeMove() { return freeMove; }

    private void finishFreeMove() {
        cancelDrag();
        saveIfDirty();
        freeMove = false;
        rebuildWidgets();
    }

    @Override public void onClose() {
        if (freeMove) {
            finishFreeMove();
            return;
        }
        cancelDrag();
        saveIfDirty();
        minecraft.setScreen(parent);
    }
    @Override public void removed() { cancelDrag(); saveIfDirty(); }
    @Override public boolean isPauseScreen() { return true; }
    @Override public boolean isInGameUi() { return true; }

    private void saveIfDirty() {
        if (dirty) {
            HandsConfig.get().save();
            dirty = false;
        }
    }

    private static Component text(String key) { return Component.translatable("screen.kohs_hands_position." + key); }
    private record Group(boolean size, int left, ActionButton global) { }

    private final class SettingSlider extends AbstractSliderButton {
        private final boolean sizeSetting;
        private final HandSide hand;

        SettingSlider(int x, int y, int width, boolean sizeSetting, HandSide hand) {
            super(x, y, width, layout.sliderHeight(), Component.empty(), 0.0);
            this.sizeSetting = sizeSetting;
            this.hand = hand;
            sync();
        }

        private double currentValue() {
            HandsConfig config = HandsConfig.get();
            if (hand == null) return sizeSetting ? config.size : config.spacing;
            return sizeSetting ? config.effectiveSize(hand) : config.effectiveSpacing(hand);
        }

        private double minimum() { return sizeSetting ? HandsConfig.MIN_SIZE : HandsConfig.MIN_SPACING; }
        private double maximum() { return sizeSetting ? HandsConfig.MAX_SIZE : HandsConfig.MAX_SPACING; }

        private void sync() {
            value = (currentValue() - minimum()) / (maximum() - minimum());
            active = hand != null || !perHand(sizeSetting);
            setTooltip(Tooltip.create(text(hand == null ? (active ? "global_hint" : "global_disabled_hint") : "per_hand_hint")));
            updateMessage();
        }

        @Override protected void updateMessage() {
            int number = (int) Math.round(currentValue() * 100.0);
            String amount = (!sizeSetting && number > 0 ? "+" : "") + number + "%";
            String key = hand == null ? "global_value" : hand == HandSide.MAIN ? "main_value" : "off_value";
            setMessage(Component.translatable("screen.kohs_hands_position." + key, amount));
        }

        @Override public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            int accent = hand == null ? HandsTheme.GLOBAL : hand == HandSide.MAIN ? HandsTheme.MAIN : HandsTheme.OFF;
            boolean highlight = active && isHoveredOrFocused();
            int fill = highlight ? 0xD15D2877 : active ? 0xB53A1748 : 0x9E2A1136;
            HandsTheme.fillRounded(graphics, x, y, w, h, fill, HandsTheme.shade(fill, 0.45F));
            HandsTheme.roundedOutline(graphics, x, y, w, h,
                    highlight ? 0xE1D493FF : active ? 0xB58B50B5 : 0x7A6A4785);
            graphics.fill(x + 4, y + 5, x + 6, y + 12, active ? accent : 0xFF8A7A99);
            String label = text(hand == null ? (active ? "global_label" : "global_inactive") :
                    hand == HandSide.MAIN ? "main_label" : "off_label").getString();
            int number = (int) Math.round(currentValue() * 100.0);
            String amount = (!sizeSetting && number > 0 ? "+" : "") + number + "%";
            int amountWidth = font.width(amount);
            graphics.text(font, font.plainSubstrByWidth(label, w - amountWidth - 26), x + 10, y + 4,
                    active ? HandsTheme.TEXT : HandsTheme.DIM_TEXT, false);
            graphics.text(font, amount, x + w - amountWidth - 7, y + 4, active ? accent : HandsTheme.DIM_TEXT, false);
            int trackY = y + h - 7;
            int knob = x + 4 + (int) Math.round(value * (w - 8));
            graphics.fill(x + 4, trackY, x + w - 4, trackY + 2, 0xB5240E2E);
            graphics.fill(x + 4, trackY, knob, trackY + 2, active ? accent : 0xFF6A5A79);
            graphics.fill(knob - 3, trackY - 2, knob + 3, trackY + 4, active ? 0xFFD590F3 : 0xFF8A7A99);
            handleCursor(graphics);
        }

        @Override protected void applyValue() {
            HandsConfig config = HandsConfig.get();
            double amount = Math.round((minimum() + value * (maximum() - minimum())) * 100.0) / 100.0;
            if (hand == null) {
                if (sizeSetting) config.size = amount;
                else config.spacing = amount;
            } else if (sizeSetting) config.setHandSize(hand, amount);
            else config.setHandSpacing(hand, amount);
            dirty = true;
            // Preserve the dragged widget's raw position. Quantizing it here can
            // otherwise prevent keyboard arrows from advancing on wide sliders.
            for (SettingSlider slider : sliders) {
                if (slider != this) slider.sync();
            }
            syncGroupButtons();
        }
    }
}
