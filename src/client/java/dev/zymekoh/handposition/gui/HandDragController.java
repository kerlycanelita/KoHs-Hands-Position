package dev.zymekoh.handposition.gui;

import dev.zymekoh.handposition.HandSide;
import dev.zymekoh.handposition.HandsConfig;

/** Keeps the entire mouse gesture, including release before an async hit arrives. */
public final class HandDragController {
    private HandPickRequest request;
    private HandSide selected;
    private boolean holding;
    private boolean pending;
    private double mouseX, mouseY;
    private double pressX, pressY;
    private int width, height;
    private final double[] initialX = new double[2];
    private final double[] initialY = new double[2];

    public void begin(HandPickRequest request, HandsConfig config) {
        cancel();
        this.request = request;
        pressX = mouseX = request.x;
        pressY = mouseY = request.y;
        width = request.width;
        height = request.height;
        holding = pending = true;
        for (HandSide side : HandSide.values()) {
            initialX[side.ordinal()] = config.hand(side).x;
            initialY[side.ordinal()] = config.hand(side).y;
        }
    }

    public boolean update(double x, double y, HandsConfig config) {
        if (holding) { mouseX = x; mouseY = y; }
        return poll(config);
    }

    public boolean release(double x, double y, HandsConfig config) {
        if (!holding) return false;
        mouseX = x;
        mouseY = y;
        holding = false;
        return poll(config);
    }

    private boolean poll(HandsConfig config) {
        if (!pending) return false;
        if (request != null) {
            if (!request.ready()) return false;
            selected = request.selection();
            request = null;
            if (selected == null) { cancel(); return false; }
        }
        int index = selected.ordinal();
        double x = HandPickRequest.draggedOffset(initialX[index], pressX, mouseX, width);
        double y = HandPickRequest.draggedOffset(initialY[index], pressY, mouseY, height);
        var hand = config.hand(selected);
        boolean changed = hand.x != x || hand.y != y;
        if (changed) config.setPosition(selected, x, y);
        if (!holding) pending = false;
        return changed;
    }

    public HandPickRequest request() { return request; }
    public HandSide selection() { return selected; }
    public boolean holding() { return holding; }
    public boolean pending() { return pending; }
    public void cancel() { request = null; selected = null; holding = pending = false; }
}
