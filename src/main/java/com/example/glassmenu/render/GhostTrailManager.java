package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class GhostTrailManager {

    private static final long  DURATION_MS = 1100L;
    private static final float MAX_ALPHA   = 0.88f;
    private static final float HOLD_FRAC   = 0.70f;

    private static volatile Snap    snap              = null;
    private static          boolean wasOnGround       = true;
    private static          boolean wasRisingLastTick = false;

    private static final class Snap {
        OtherClientPlayerEntity dummy;
        double x, y, z;
        long captureTime;
        boolean released = false;

        float alpha() {
            float t = (System.currentTimeMillis() - captureTime) / (float) DURATION_MS;
            if (t >= 1f) return 0f;
            if (t < HOLD_FRAC) return MAX_ALPHA;
            float u = (t - HOLD_FRAC) / (1f - HOLD_FRAC);
            return MAX_ALPHA * (1f - u * u);
        }
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        boolean onGround = client.player.isOnGround();
        double  vy       = client.player.getVelocity().y;

        boolean atApex = !onGround && wasRisingLastTick && vy <= 0.0;
        if (atApex && GlassMenuClient.CONFIG.enableGhostTrail()) {
            Snap s = new Snap();
            s.x = client.player.getX();
            s.y = client.player.getY();
            s.z = client.player.getZ();
            s.captureTime = System.currentTimeMillis();

            // Create a dummy player to freeze the animation state
            s.dummy = new OtherClientPlayerEntity(client.world, client.player.getGameProfile());
            s.dummy.copyPositionAndRotation(client.player);
            s.dummy.bodyYaw = client.player.bodyYaw;
            s.dummy.prevBodyYaw = client.player.prevBodyYaw;
            s.dummy.headYaw = client.player.headYaw;
            s.dummy.prevHeadYaw = client.player.prevHeadYaw;
            s.dummy.setPitch(client.player.getPitch());
            s.dummy.setYaw(client.player.getYaw());
            
            // Mathematical backdoor to set limbAnimator.pos
            float targetPos = client.player.limbAnimator.getPos();
            float currentPos = s.dummy.limbAnimator.getPos();
            s.dummy.limbAnimator.updateLimbs(targetPos - currentPos, 1.0f);
            s.dummy.limbAnimator.setSpeed(client.player.limbAnimator.getSpeed());

            snap = s;
        }

        wasRisingLastTick = !onGround && vy > 0.0;
        wasOnGround       = onGround;
    }

    public static void render(WorldRenderContext context) {
        if (!GlassMenuClient.CONFIG.enableGhostTrail()) return;

        Snap s = snap;
        if (s == null || s.dummy == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        // Delay appearance until the player moves away from the snapshot origin 
        // or a short timeout passes (to prevent it taking too long or Z-fighting).
        if (!s.released) {
            if (client.player.squaredDistanceTo(s.x, s.y, s.z) > 0.15 || System.currentTimeMillis() - s.captureTime > 70) {
                s.released = true;
                s.captureTime = System.currentTimeMillis();
            } else {
                return;
            }
        }

        float alpha = s.alpha();
        if (alpha <= 0.002f) return;

        Vec3d camPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();

        matrices.push();
        matrices.translate(s.x - camPos.x, s.y - camPos.y, s.z - camPos.z);

        // Apply entity transformations like in LivingEntityRenderer
        float bodyYaw = s.dummy.bodyYaw;
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        
        // Flip and offset scale since models are upside down
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.translate(0.0f, -1.501f, 0.0f);

        // Calculate custom color
        int   cfg = GlassMenuClient.CONFIG.ghostTrailColor();
        float cr  = ((cfg >> 16) & 0xFF) / 255f;
        float cg  = ((cfg >>  8) & 0xFF) / 255f;
        float cb  = ( cfg        & 0xFF) / 255f;

        if (GlassMenuClient.CONFIG.ghostTrailRgb()) {
            float hue = (System.currentTimeMillis() % 6000L) / 6000f;
            int rgb = hsvToRgb(hue, 1f, 1f);
            cr = ((rgb >> 16) & 0xFF) / 255f;
            cg = ((rgb >>  8) & 0xFF) / 255f;
            cb = ( rgb        & 0xFF) / 255f;
        }

        final float finalR = cr;
        final float finalG = cg;
        final float finalB = cb;
        final Identifier WHITE_TEX = Identifier.of("minecraft", "textures/misc/white.png");

        PlayerEntityRenderer renderer = (PlayerEntityRenderer) client.getEntityRenderDispatcher().getRenderer(s.dummy);
        PlayerEntityModel<AbstractClientPlayerEntity> model = renderer.getModel();

        // Sync visibility of all outer layers to make sure sleeves, pants, and jacket render
        model.setVisible(true);
        model.hat.visible = true;
        model.jacket.visible = true;
        model.leftPants.visible = true;
        model.rightPants.visible = true;
        model.leftSleeve.visible = true;
        model.rightSleeve.visible = true;

        // Sync state to the model
        model.handSwingProgress = s.dummy.getHandSwingProgress(0f);
        model.riding = s.dummy.hasVehicle();
        model.child = s.dummy.isBaby();
        
        // Calculate proper head rotation offsets relative to body
        float headYawOffset = s.dummy.headYaw - bodyYaw;
        float pitch = s.dummy.getPitch();
        model.setAngles(s.dummy, s.dummy.limbAnimator.getPos(), s.dummy.limbAnimator.getSpeed(), 0f, headYawOffset, pitch);

        int rInt = (int)(finalR * 255);
        int gInt = (int)(finalG * 255);
        int bInt = (int)(finalB * 255);

        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();

        // Render the model at 1.0f scale to ensure limbs stay connected and solid
        renderModelWithScale(matrices, consumers, model, WHITE_TEX, 1.0f, rInt, gInt, bInt, alpha);

        consumers.draw();

        matrices.pop();
    }

    private static void renderModelWithScale(
        MatrixStack matrices, 
        VertexConsumerProvider consumers, 
        PlayerEntityModel<AbstractClientPlayerEntity> model, 
        Identifier whiteTex, 
        float scale, 
        int r, int g, int b, float alpha
    ) {
        matrices.push();
        if (scale != 1.0f) {
            // Scale relative to the center of the player model (Y = 0.9f block height)
            matrices.translate(0.0, 0.9, 0.0);
            matrices.scale(scale, scale, scale);
            matrices.translate(0.0, -0.9, 0.0);
        }
        
        int aInt = Math.max(0, Math.min(255, (int)(alpha * 255)));
        int packedColor = (aInt << 24) | (r << 16) | (g << 8) | b;
        
        VertexConsumer consumer = consumers.getBuffer(RenderLayer.getEntityTranslucent(whiteTex));
        model.render(matrices, consumer, 0xF000F0, net.minecraft.client.render.OverlayTexture.DEFAULT_UV, packedColor);
        
        matrices.pop();
    }

    private static int hsvToRgb(float h, float s, float v) {
        float c = v*s, x = c*(1f - Math.abs(h*6f%2f - 1f)), m = v-c;
        float r, g, b;
        switch ((int)(h*6f)%6) {
            case 0: r=c;g=x;b=0; break; case 1: r=x;g=c;b=0; break;
            case 2: r=0;g=c;b=x; break; case 3: r=0;g=x;b=c; break;
            case 4: r=x;g=0;b=c; break; default: r=c;g=0;b=x; break;
        }
        return ((int)((r+m)*255)<<16)|((int)((g+m)*255)<<8)|(int)((b+m)*255);
    }
}
