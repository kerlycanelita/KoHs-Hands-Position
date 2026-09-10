package dev.zymekoh.handposition;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HandReadbackTest {
    @Test void minecraftRejectsVanillaPipUsageButAcceptsReadableHandTextures() {
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger callbacks = new AtomicInteger();
        var backend = (CommandEncoderBackend) Proxy.newProxyInstance(CommandEncoderBackend.class.getClassLoader(),
                new Class<?>[]{CommandEncoderBackend.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isInRenderPass")) return false;
                    if (method.getName().equals("copyTextureToBuffer")) {
                        reads.incrementAndGet();
                        ((Runnable) args[3]).run();
                        return null;
                    }
                    throw new AssertionError("Unexpected GPU call: " + method.getName());
                });
        // Exercise Minecraft's real validation without a window or GPU context.
        var encoder = new CommandEncoder(null, backend);
        int vanillaPipUsage = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;
        try (var vanilla = new TestTexture(vanillaPipUsage);
             var readable = new TestTexture(vanillaPipUsage | GpuTexture.USAGE_COPY_SRC);
             var buffer = new TestBuffer()) {
            var failure = assertThrows(IllegalArgumentException.class,
                    () -> encoder.copyTextureToBuffer(vanilla, buffer, 0, callbacks::incrementAndGet, 0, 12, 18, 1, 1));
            assertTrue(failure.getMessage().contains("USAGE_COPY_SRC"));
            assertEquals(0, reads.get());
            assertDoesNotThrow(() -> encoder.copyTextureToBuffer(readable, buffer, 0, callbacks::incrementAndGet, 0, 12, 18, 1, 1));
            assertEquals(1, reads.get());
            assertEquals(1, callbacks.get());
        }
    }

    private static class TestTexture extends GpuTexture {
        private boolean closed;
        TestTexture(int usage) { super(usage, "Test preview", TextureFormat.RGBA8, 640, 360, 1, 1); }
        @Override public boolean isClosed() { return closed; }
        @Override public void close() { closed = true; }
    }

    private static class TestBuffer extends GpuBuffer {
        private boolean closed;
        TestBuffer() { super(GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, 4); }
        @Override public boolean isClosed() { return closed; }
        @Override public void close() { closed = true; }
    }
}
