import net.minecraft.client.network.AbstractClientPlayerEntity;
public class TestLerp {
    public void test(AbstractClientPlayerEntity p, float tickDelta) {
        net.minecraft.util.math.Vec3d pos = p.getLerpedPos(tickDelta);
    }
}
