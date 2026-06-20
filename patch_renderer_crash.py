import re

with open("src/main/java/com/example/glassmenu/render/CrosshairRadarRenderer.java", "r") as f:
    code = f.read()

search = """        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        java.util.List<PlayerTarget> targets = new java.util.ArrayList<>();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator() || player.isInvisible()) continue;

            double dx = player.getX() - cameraPos.x;
            double dz = player.getZ() - cameraPos.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance < 1.0) continue; // Too close

            // Calculate angle to target
            float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
            float angleDiff = MathHelper.wrapDegrees(targetYaw - yaw);
            
            // Convert angle to screen space (0 is top, 90 is right, etc)
            float angleRad = (float) Math.toRadians(angleDiff);
            
            float targetX = centerX + (float) Math.sin(angleRad) * radius;
            float targetY = centerY - (float) Math.cos(angleRad) * radius;

            targets.add(new PlayerTarget(player.getName().getString(), (int)distance, targetX, targetY, angleRad));

            // Draw Arrow (Triangle pointing in angleRad)
            float arrowSize = GlassMenuClient.CONFIG.crosshairRadarIconSize();
            float p1x = targetX + (float) Math.sin(angleRad) * arrowSize;
            float p1y = targetY - (float) Math.cos(angleRad) * arrowSize;
            
            float p2x = targetX + (float) Math.sin(angleRad + Math.PI * 0.75) * arrowSize;
            float p2y = targetY - (float) Math.cos(angleRad + Math.PI * 0.75) * arrowSize;
            
            float p3x = targetX + (float) Math.sin(angleRad - Math.PI * 0.75) * arrowSize;
            float p3y = targetY - (float) Math.cos(angleRad - Math.PI * 0.75) * arrowSize;

            bufferBuilder.vertex(matrix, p1x, p1y, 0).color(r, g, b, 1.0f);
            bufferBuilder.vertex(matrix, p2x, p2y, 0).color(r, g, b, 1.0f);
            bufferBuilder.vertex(matrix, p3x, p3y, 0).color(r, g, b, 1.0f);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());"""

replace = """        java.util.List<PlayerTarget> targets = new java.util.ArrayList<>();
        float maxDistance = GlassMenuClient.CONFIG.crosshairRadarSearchDistance();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator() || player.isInvisible()) continue;

            double dx = player.getX() - cameraPos.x;
            double dz = player.getZ() - cameraPos.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance < 1.0 || distance > maxDistance) continue; // Too close or too far

            // Calculate angle to target
            float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
            float angleDiff = MathHelper.wrapDegrees(targetYaw - yaw);
            
            // Convert angle to screen space (0 is top, 90 is right, etc)
            float angleRad = (float) Math.toRadians(angleDiff);
            
            float targetX = centerX + (float) Math.sin(angleRad) * radius;
            float targetY = centerY - (float) Math.cos(angleRad) * radius;

            targets.add(new PlayerTarget(player.getName().getString(), (int)distance, targetX, targetY, angleRad));
        }

        if (targets.isEmpty()) {
            RenderSystem.disableBlend();
            return;
        }

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (PlayerTarget target : targets) {
            float arrowSize = GlassMenuClient.CONFIG.crosshairRadarIconSize();
            float p1x = target.x + (float) Math.sin(target.angleRad) * arrowSize;
            float p1y = target.y - (float) Math.cos(target.angleRad) * arrowSize;
            
            float p2x = target.x + (float) Math.sin(target.angleRad + Math.PI * 0.75) * arrowSize;
            float p2y = target.y - (float) Math.cos(target.angleRad + Math.PI * 0.75) * arrowSize;
            
            float p3x = target.x + (float) Math.sin(target.angleRad - Math.PI * 0.75) * arrowSize;
            float p3y = target.y - (float) Math.cos(target.angleRad - Math.PI * 0.75) * arrowSize;

            bufferBuilder.vertex(matrix, p1x, p1y, 0).color(r, g, b, 1.0f);
            bufferBuilder.vertex(matrix, p2x, p2y, 0).color(r, g, b, 1.0f);
            bufferBuilder.vertex(matrix, p3x, p3y, 0).color(r, g, b, 1.0f);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());"""

code = code.replace(search, replace)

with open("src/main/java/com/example/glassmenu/render/CrosshairRadarRenderer.java", "w") as f:
    f.write(code)
