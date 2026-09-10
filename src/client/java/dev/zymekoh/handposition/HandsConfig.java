package dev.zymekoh.handposition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HandsConfig {
    public static final double MIN_SIZE = 0.5;
    public static final double MAX_SIZE = 1.5;
    public static final double MIN_SPACING = -0.4;
    public static final double MAX_SPACING = 0.4;
    private static final Logger LOGGER = LoggerFactory.getLogger("kohs_hands_position");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HandsConfig instance = new HandsConfig();
    public double size = 1.0;
    public double spacing = 0.0;
    public boolean perHandSize;
    public boolean perHandSpacing;
    public HandSettings mainHand = new HandSettings();
    public HandSettings offHand = new HandSettings();

    public static final class HandSettings {
        public double size = 1.0;
        public double spacing;
        /** Offsets as fractions of viewport width/height; positive Y is down. */
        public double x;
        public double y;
    }

    public static HandsConfig get() { return instance; }

    public static void load() {
        Path file = configFile();
        if (!Files.isRegularFile(file)) return;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            HandsConfig loaded = GSON.fromJson(reader, HandsConfig.class);
            if (loaded != null) {
                loaded.sanitize();
                instance = loaded;
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not read hand position settings; using defaults", exception);
        }
    }

    public void sanitize() {
        size = finiteClamp(size, MIN_SIZE, MAX_SIZE, 1.0);
        spacing = finiteClamp(spacing, MIN_SPACING, MAX_SPACING, 0.0);
        if (mainHand == null) mainHand = new HandSettings();
        if (offHand == null) offHand = new HandSettings();
        sanitizeHand(mainHand);
        sanitizeHand(offHand);
    }

    private void sanitizeHand(HandSettings hand) {
        hand.size = finiteClamp(hand.size, MIN_SIZE, MAX_SIZE, size);
        hand.spacing = finiteClamp(hand.spacing, MIN_SPACING, MAX_SPACING, spacing);
        hand.x = finiteClamp(hand.x, -1.0, 1.0, 0.0);
        hand.y = finiteClamp(hand.y, -1.0, 1.0, 0.0);
    }

    public HandSettings hand(HandSide side) { return side == HandSide.MAIN ? mainHand : offHand; }
    public double effectiveSize(HandSide side) { return perHandSize ? hand(side).size : size; }
    public double effectiveSpacing(HandSide side) { return perHandSpacing ? hand(side).spacing : spacing; }

    public void setHandSize(HandSide side, double value) {
        if (!perHandSize) {
            mainHand.size = offHand.size = size;
            perHandSize = true;
        }
        hand(side).size = finiteClamp(value, MIN_SIZE, MAX_SIZE, size);
    }

    public void setHandSpacing(HandSide side, double value) {
        if (!perHandSpacing) {
            mainHand.spacing = offHand.spacing = spacing;
            perHandSpacing = true;
        }
        hand(side).spacing = finiteClamp(value, MIN_SPACING, MAX_SPACING, spacing);
    }

    public void setPosition(HandSide side, double x, double y) {
        hand(side).x = finiteClamp(x, -1.0, 1.0, 0.0);
        hand(side).y = finiteClamp(y, -1.0, 1.0, 0.0);
    }

    public void resetPositions() {
        mainHand.x = mainHand.y = offHand.x = offHand.y = 0.0;
    }

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("kohs_hands_position.json");
    }

    private static double finiteClamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }

    public void save() {
        sanitize();
        Path file = configFile();
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temporary, GSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save hand position settings", exception);
        }
    }
}
