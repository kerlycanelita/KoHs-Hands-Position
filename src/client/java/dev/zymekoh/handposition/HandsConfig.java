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
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("kohs_hands_position.json");
    private static HandsConfig instance = new HandsConfig();
    public double size = 1.0;
    public double spacing = 0.0;

    public static HandsConfig get() { return instance; }

    public static void load() {
        if (!Files.isRegularFile(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
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
    }

    private static double finiteClamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }

    public void save() {
        sanitize();
        Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(temporary, GSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save hand position settings", exception);
        }
    }
}
