package dev.zymekoh.handposition.gui;

import dev.zymekoh.handposition.HandSide;

/** One asynchronous pointer sample, shared by the two independent hand previews. */
public final class HandPickRequest {
    public final double x;
    public final double y;
    public final int width;
    public final int height;
    private final HandSide front;
    private final int[] results = {-1, -1};
    private final boolean[] claimed = new boolean[2];

    public HandPickRequest(double x, double y, int width, int height, boolean mainVisible, boolean offVisible, HandSide front) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.front = front;
        if (!mainVisible) results[0] = 0;
        if (!offVisible) results[1] = 0;
    }

    public synchronized boolean claim(HandSide hand) {
        int index = hand.ordinal();
        if (claimed[index] || results[index] != -1) return false;
        claimed[index] = true;
        return true;
    }

    public synchronized void complete(HandSide hand, boolean hit) { results[hand.ordinal()] = hit ? 1 : 0; }
    public synchronized boolean ready() { return results[0] != -1 && results[1] != -1; }

    public synchronized HandSide selection() {
        if (!ready()) return null;
        if (results[front.ordinal()] == 1) return front;
        if (results[0] == 1) return HandSide.MAIN;
        return results[1] == 1 ? HandSide.OFF : null;
    }

    public static double draggedOffset(double initial, double press, double current, int dimension) {
        return Math.clamp(initial + (current - press) / Math.max(1, dimension), -1.0, 1.0);
    }
}
