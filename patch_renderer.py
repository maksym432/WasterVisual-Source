import re

with open("src/main/java/com/example/glassmenu/render/CrosshairRadarRenderer.java", "r") as f:
    code = f.read()

# Replace the text drawing loop with an anti-overlap logic and background squares
text_loop_search = """        // Draw Text
        for (PlayerTarget target : targets) {
            String text = target.name + " " + target.distance + "m";
            int textWidth = client.textRenderer.getWidth(text);
            
            float textOffsetX = (float) Math.sin(target.angleRad) * 12.0f;
            float textOffsetY = -(float) Math.cos(target.angleRad) * 12.0f;
            
            float drawX = target.x + textOffsetX - textWidth / 2f;
            float drawY = target.y + textOffsetY - 4f;

            context.drawTextWithShadow(client.textRenderer, text, (int)drawX, (int)drawY, 0xFFFFFFFF);
        }"""

text_loop_replace = """        // Draw Text with background squares and anti-overlap
        java.util.List<net.minecraft.client.util.math.Rect2i> drawnRects = new java.util.ArrayList<>();
        
        targets.sort(java.util.Comparator.comparingInt(t -> t.distance));

        for (PlayerTarget target : targets) {
            String text = target.name + " " + target.distance + "m";
            int textWidth = client.textRenderer.getWidth(text);
            int textHeight = client.textRenderer.fontHeight;
            
            float currentRad = 12.0f;
            float drawX = 0;
            float drawY = 0;
            boolean overlapping = true;
            
            // Push text further away if it overlaps with existing text
            while (overlapping && currentRad < 200.0f) {
                float textOffsetX = (float) Math.sin(target.angleRad) * currentRad;
                float textOffsetY = -(float) Math.cos(target.angleRad) * currentRad;
                
                drawX = target.x + textOffsetX - textWidth / 2f;
                drawY = target.y + textOffsetY - textHeight / 2f;
                
                overlapping = false;
                net.minecraft.client.util.math.Rect2i currentRect = new net.minecraft.client.util.math.Rect2i((int)drawX - 2, (int)drawY - 2, textWidth + 4, textHeight + 4);
                
                for (net.minecraft.client.util.math.Rect2i rect : drawnRects) {
                    // Check intersection
                    if (currentRect.getX() < rect.getX() + rect.getWidth() && 
                        currentRect.getX() + currentRect.getWidth() > rect.getX() &&
                        currentRect.getY() < rect.getY() + rect.getHeight() && 
                        currentRect.getY() + currentRect.getHeight() > rect.getY()) {
                        overlapping = true;
                        break;
                    }
                }
                
                if (overlapping) {
                    currentRad += 10.0f;
                } else {
                    drawnRects.add(currentRect);
                }
            }

            // Draw dark background square
            context.fill((int)drawX - 2, (int)drawY - 2, (int)drawX + textWidth + 2, (int)drawY + textHeight + 1, 0x80000000);
            
            // Draw text
            context.drawTextWithShadow(client.textRenderer, text, (int)drawX, (int)drawY, 0xFFFFFFFF);
        }"""

code = code.replace(text_loop_search, text_loop_replace)

with open("src/main/java/com/example/glassmenu/render/CrosshairRadarRenderer.java", "w") as f:
    f.write(code)
