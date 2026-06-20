import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class TestDrawContext {
    public static void test(DrawContext context) {
        Identifier id = Identifier.of("test", "test");
        context.drawTexture(id, 0, 0, 100, 100, 0.0f, 0.0f, 1920, 1080, 1920, 1080);
    }
}
