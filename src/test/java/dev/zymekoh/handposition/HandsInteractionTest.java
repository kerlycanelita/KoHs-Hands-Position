package dev.zymekoh.handposition;

import dev.zymekoh.handposition.gui.HandPickRequest;
import dev.zymekoh.handposition.gui.HandDragController;
import dev.zymekoh.handposition.gui.HandsScreenLayout;
import dev.zymekoh.handposition.render.HandTransform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HandsInteractionTest {
    @Test void overlappingHandsSelectTheVisuallyFrontHandRegardlessOfReadbackOrder() {
        var pick = new HandPickRequest(210, 190, 640, 360, true, true, HandSide.MAIN);
        assertTrue(pick.claim(HandSide.MAIN));
        assertFalse(pick.claim(HandSide.MAIN));
        pick.complete(HandSide.OFF, true);
        assertFalse(pick.ready());
        assertNull(pick.selection());
        pick.complete(HandSide.MAIN, true);
        assertEquals(HandSide.MAIN, pick.selection());
        var offOnTop = new HandPickRequest(210, 190, 640, 360, true, true, HandSide.OFF);
        offOnTop.complete(HandSide.MAIN, true);
        offOnTop.complete(HandSide.OFF, true);
        assertEquals(HandSide.OFF, offOnTop.selection());
    }

    @Test void emptyOffhandAndTransparentBackgroundCannotBeDragged() {
        var pick = new HandPickRequest(15, 15, 320, 240, true, false, HandSide.OFF);
        assertFalse(pick.claim(HandSide.OFF));
        pick.complete(HandSide.MAIN, false);
        assertTrue(pick.ready());
        assertNull(pick.selection());
        var mainOnly = new HandPickRequest(250, 200, 320, 240, true, false, HandSide.OFF);
        mainOnly.complete(HandSide.MAIN, true);
        assertEquals(HandSide.MAIN, mainOnly.selection());
    }

    @Test void dragPreservesGrabOffsetAndUsesNormalizedScreenCoordinates() {
        assertEquals(0.12, HandPickRequest.draggedOffset(0.12, 140, 140, 640), 1e-9);
        assertEquals(0.22, HandPickRequest.draggedOffset(0.12, 140, 204, 640), 1e-9);
        assertEquals(0.22, HandPickRequest.draggedOffset(0.12, 280, 408, 1280), 1e-9);
        assertEquals(-0.1, HandPickRequest.draggedOffset(0.0, 210, 186, 240), 1e-9);
        assertEquals(1, HandPickRequest.draggedOffset(0.0, 0, 5000, 320));
    }

    @Test void releasingBeforeTheReadbackArrivesStillCommitsTheReleasePosition() {
        var config = new HandsConfig();
        config.setPosition(HandSide.MAIN, 0.1, -0.2);
        var drag = new HandDragController();
        var pick = new HandPickRequest(200, 100, 640, 360, true, false, HandSide.MAIN);
        drag.begin(pick, config);
        assertFalse(drag.update(220, 120, config));
        assertFalse(drag.release(264, 136, config));
        assertTrue(drag.pending());
        pick.complete(HandSide.MAIN, true);
        // Subsequent cursor movement must not replace the release location.
        assertTrue(drag.update(600, 300, config));
        assertEquals(0.2, config.hand(HandSide.MAIN).x, 1e-9);
        assertEquals(-0.1, config.hand(HandSide.MAIN).y, 1e-9);
        assertFalse(drag.pending());
        assertFalse(drag.update(10, 10, config));
    }

    @Test void draggingKeepsTheGrabbedHandAfterLeavingItsSilhouetteAndStopsOnRelease() {
        var config = new HandsConfig();
        var drag = new HandDragController();
        var pick = new HandPickRequest(100, 180, 640, 360, true, true, HandSide.OFF);
        drag.begin(pick, config);
        pick.complete(HandSide.MAIN, true);
        pick.complete(HandSide.OFF, true);
        assertFalse(drag.update(100, 180, config));
        assertEquals(HandSide.OFF, drag.selection());
        assertTrue(drag.update(420, 90, config));
        assertEquals(0.5, config.hand(HandSide.OFF).x, 1e-9);
        assertEquals(-0.25, config.hand(HandSide.OFF).y, 1e-9);
        assertEquals(0, config.hand(HandSide.MAIN).x);
        assertFalse(drag.release(420, 90, config));
        assertFalse(drag.update(500, 250, config));
        assertEquals(0.5, config.hand(HandSide.OFF).x, 1e-9);
    }

    @Test void cancelledGesturesIgnoreLateResultsAfterResetResizeOrFocusLoss() {
        var config = new HandsConfig();
        var drag = new HandDragController();
        var pick = new HandPickRequest(100, 100, 320, 180, true, false, HandSide.MAIN);
        drag.begin(pick, config);
        drag.update(200, 150, config);
        drag.cancel();
        pick.complete(HandSide.MAIN, true);
        assertFalse(drag.update(200, 150, config));
        assertFalse(drag.holding());
        assertEquals(0, config.hand(HandSide.MAIN).x);
    }

    @Test void freeMoveTranslatesEveryPointEquallyAcrossDepthFovAndAspectRatio() {
        for (float fov : new float[]{40, 70, 110}) {
            for (float aspect : new float[]{1, 16.0F / 9, 21.0F / 9}) {
                Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov), aspect, 0.05F, 100.0F);
                Matrix4f movedProjection = new Matrix4f(projection).mul(HandTransform.screenOffset(0.17, -0.23, fov, aspect));
                for (float z : new float[]{-0.2F, -0.72F, -2.5F}) {
                    Vector3f point = new Vector3f(0.19F, -0.34F, z);
                    Vector3f before = projection.transformProject(point, new Vector3f());
                    Vector3f after = movedProjection.transformProject(point, new Vector3f());
                    assertEquals(0.34, after.x - before.x, 1e-5);
                    assertEquals(0.46, after.y - before.y, 1e-5);
                    assertEquals(before.z, after.z, 1e-5);
                }
            }
        }
    }

    @Test void controlsFitCompactAndWideLogicalScreensWithoutHidingTheFooter() {
        for (int width : new int[]{320, 360, 426, 480, 540, 559, 560, 640, 960, 1920}) {
            for (int height : new int[]{180, 200, 240, 270, 360, 540, 1080}) {
                var layout = HandsScreenLayout.calculate(width, height);
                assertTrue(layout.left() >= 0);
                assertTrue(layout.left() + layout.contentWidth() <= width);
                assertTrue(layout.cardWidth() - 16 >= 240);
                assertTrue(layout.cardBottom() + 8 <= layout.footerY());
                assertTrue(layout.footerY() + layout.footerHeight() <= height);
                for (int row = 0; row < 3; row++) {
                    assertTrue(layout.sliderY(row) >= layout.cardTop());
                    assertTrue(layout.sliderY(row) + layout.sliderHeight() <= layout.cardBottom());
                }
                assertEquals(width < 560, layout.tabs());
                if (!layout.tabs()) assertTrue(layout.cardX(1) + layout.cardWidth() <= width);
            }
        }
    }
}
