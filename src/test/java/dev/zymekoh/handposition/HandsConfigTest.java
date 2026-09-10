package dev.zymekoh.handposition;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HandsConfigTest {
    @Test void oldConfigKeepsBothHandsUntilOneIsEdited() {
        HandsConfig config = new Gson().fromJson("{\"size\":1.23,\"spacing\":-0.19}", HandsConfig.class);
        config.sanitize();
        assertFalse(config.perHandSize);
        assertFalse(config.perHandSpacing);
        assertEquals(1.23, config.effectiveSize(HandSide.OFF));
        config.setHandSize(HandSide.MAIN, 0.72);
        assertTrue(config.perHandSize);
        assertFalse(config.perHandSpacing);
        assertEquals(0.72, config.effectiveSize(HandSide.MAIN));
        assertEquals(1.23, config.effectiveSize(HandSide.OFF));
        assertEquals(-0.19, config.effectiveSpacing(HandSide.MAIN));
        assertEquals(-0.19, config.effectiveSpacing(HandSide.OFF));
    }

    @Test void spacingOverrideAndReturningToGlobalAreIndependentOfSize() {
        HandsConfig config = new HandsConfig();
        config.spacing = 0.22;
        config.setHandSize(HandSide.OFF, 1.4);
        config.setHandSpacing(HandSide.OFF, -0.35);
        assertEquals(0.22, config.effectiveSpacing(HandSide.MAIN));
        assertEquals(-0.35, config.effectiveSpacing(HandSide.OFF));
        config.spacing = 0.1;
        config.size = 0.9;
        assertEquals(0.22, config.effectiveSpacing(HandSide.MAIN));
        assertEquals(1.4, config.effectiveSize(HandSide.OFF));
        config.perHandSpacing = false;
        assertEquals(0.1, config.effectiveSpacing(HandSide.MAIN));
        assertEquals(0.1, config.effectiveSpacing(HandSide.OFF));
        assertEquals(1.4, config.effectiveSize(HandSide.OFF));
        config.setHandSpacing(HandSide.MAIN, -0.08);
        assertEquals(0.1, config.effectiveSpacing(HandSide.OFF));
        config.perHandSize = false;
        assertEquals(0.9, config.effectiveSize(HandSide.MAIN));
        assertEquals(0.9, config.effectiveSize(HandSide.OFF));
    }

    @Test void persistedIndependentValuesAndPositionsSurviveReload() {
        HandsConfig config = new HandsConfig();
        config.setHandSize(HandSide.MAIN, 1.35);
        config.setHandSize(HandSide.OFF, 0.55);
        config.setHandSpacing(HandSide.MAIN, -0.33);
        config.setHandSpacing(HandSide.OFF, 0.27);
        config.setPosition(HandSide.MAIN, -0.31, 0.12);
        config.setPosition(HandSide.OFF, 0.26, -0.44);
        Gson gson = new Gson();
        HandsConfig loaded = gson.fromJson(gson.toJson(config), HandsConfig.class);
        loaded.sanitize();
        assertTrue(loaded.perHandSize);
        assertTrue(loaded.perHandSpacing);
        assertEquals(1.35, loaded.effectiveSize(HandSide.MAIN));
        assertEquals(0.55, loaded.effectiveSize(HandSide.OFF));
        assertEquals(-0.33, loaded.effectiveSpacing(HandSide.MAIN));
        assertEquals(0.27, loaded.effectiveSpacing(HandSide.OFF));
        assertEquals(-0.31, loaded.mainHand.x);
        assertEquals(-0.44, loaded.offHand.y);
        loaded.resetPositions();
        assertEquals(0, loaded.mainHand.x);
        assertEquals(0, loaded.mainHand.y);
        assertEquals(0, loaded.offHand.x);
        assertEquals(0, loaded.offHand.y);
        assertEquals(0.55, loaded.effectiveSize(HandSide.OFF));
        assertEquals(0.27, loaded.effectiveSpacing(HandSide.OFF));
    }

    @Test void malformedHandValuesCannotProduceNonFiniteTransforms() {
        HandsConfig config = new HandsConfig();
        config.mainHand = null;
        config.offHand.size = Double.NaN;
        config.offHand.spacing = Double.POSITIVE_INFINITY;
        config.offHand.x = Double.NaN;
        config.offHand.y = -20;
        config.sanitize();
        assertNotNull(config.mainHand);
        assertEquals(1.0, config.offHand.size);
        assertEquals(0.0, config.offHand.spacing);
        assertEquals(0.0, config.offHand.x);
        assertEquals(-1.0, config.offHand.y);
        config.setHandSize(HandSide.MAIN, 8);
        config.setHandSpacing(HandSide.OFF, -6);
        config.setPosition(HandSide.MAIN, 4, -9);
        assertEquals(1.5, config.effectiveSize(HandSide.MAIN));
        assertEquals(-0.4, config.effectiveSpacing(HandSide.OFF));
        assertEquals(1.0, config.mainHand.x);
        assertEquals(-1.0, config.mainHand.y);
    }
}
