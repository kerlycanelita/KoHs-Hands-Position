package dev.zymekoh.handposition.gui;

/** Logical GUI dimensions, independent of monitor pixels or GUI scale. */
public record HandsScreenLayout(int left, int contentWidth, int cardWidth, boolean tabs,
                                int cardTop, int cardBottom, int footerY, int footerButtonWidth,
                                int sliderHeight, int rowPitch, int groupHeader, int footerHeight,
                                int tabY, boolean shortScreen) {
    public static HandsScreenLayout calculate(int width, int height) {
        int margin = width < 540 ? 8 : 16;
        int contentWidth = Math.min(700, Math.max(20, width - margin * 2));
        boolean tabs = contentWidth < 528;
        boolean tiny = height < 220;
        boolean shortScreen = height < 260;
        int tabY = tiny ? 22 : shortScreen ? 28 : 44;
        int cardTop = tabs ? tabY + 24 : tiny ? 24 : shortScreen ? 28 : 44;
        int cardWidth = tabs ? contentWidth : (contentWidth - 12) / 2;
        int sliderHeight = tiny ? 20 : shortScreen ? 26 : 30;
        int rowPitch = sliderHeight + (tiny ? 2 : 4);
        int groupHeader = tiny ? 24 : shortScreen ? 28 : 34;
        int footerHeight = tiny ? 22 : 26;
        return new HandsScreenLayout((width - contentWidth) / 2, contentWidth, cardWidth, tabs,
                cardTop, cardTop + groupHeader + rowPitch * 2 + sliderHeight + 6,
                height - footerHeight - 8, Math.min(180, (contentWidth - 8) / 2),
                sliderHeight, rowPitch, groupHeader, footerHeight, tabY, shortScreen);
    }

    public int cardX(int index) { return left + index * (cardWidth + 12); }
    public int sliderY(int row) { return cardTop + groupHeader + row * rowPitch; }
}
