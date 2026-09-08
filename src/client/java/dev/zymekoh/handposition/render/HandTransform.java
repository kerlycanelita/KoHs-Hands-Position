package dev.zymekoh.handposition.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.zymekoh.handposition.HandsConfig;
import net.minecraft.world.entity.HumanoidArm;

public final class HandTransform {
    private HandTransform() { }

    public static void apply(PoseStack poses, HumanoidArm arm) {
        HandsConfig config = HandsConfig.get();
        if (config.size == 1.0 && config.spacing == 0.0) return;
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float size = (float) config.size;
        // Scale around the resting hand, not the camera origin (which cancels
        // uniform scaling under perspective projection).
        float pivotX = side * 0.56F;
        poses.translate(side * config.spacing + pivotX, -0.52F, -0.72F);
        poses.scale(size, size, size);
        poses.translate(-pivotX, 0.52F, 0.72F);
    }
}
