package dev.zymekoh.handposition.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.zymekoh.handposition.HandsConfig;
import dev.zymekoh.handposition.HandSide;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;

public final class HandTransform {
    private HandTransform() { }

    public static void apply(PoseStack poses, HandSide hand, HumanoidArm arm, float fov, float aspect) {
        HandsConfig config = HandsConfig.get();
        var settings = config.hand(hand);
        float size = (float) config.effectiveSize(hand);
        double spacing = config.effectiveSpacing(hand);
        if (size == 1.0F && spacing == 0.0 && settings.x == 0.0 && settings.y == 0.0) return;
        if (settings.x != 0.0 || settings.y != 0.0) {
            // A view-space shear produces a rigid 2D translation after perspective
            // division, at every depth. Preserve normals: this is a screen offset,
            // not a physical deformation or a change to the hand's lighting.
            poses.last().pose().mul(screenOffset(settings.x, settings.y, fov, aspect));
        }
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        // Scale around the resting hand, not the camera origin (which cancels
        // uniform scaling under perspective projection).
        float pivotX = side * 0.56F;
        poses.translate(side * spacing + pivotX, -0.52F, -0.72F);
        poses.scale(size, size, size);
        poses.translate(-pivotX, 0.52F, 0.72F);
    }

    public static Matrix4f screenOffset(double x, double y, float fov, float aspect) {
        double tangent = Math.tan(Math.toRadians(validFov(fov)) / 2.0);
        float safeAspect = Float.isFinite(aspect) && aspect > 0.0F ? aspect : 1.0F;
        return new Matrix4f().m20((float) (-2.0 * x * tangent * safeAspect)).m21((float) (2.0 * y * tangent));
    }

    public static float validFov(float fov) { return Float.isFinite(fov) && fov >= 1.0F && fov < 179.0F ? fov : 70.0F; }
}
