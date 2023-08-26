package jsettlers.graphics.image.reader.shadowmap;

public class ShadowMapping43 extends IdentityShadowMapping {
	@Override
	public int getShadowIndex(int settlerIndex) {
            System.out.println("getShadowIndex("+settlerIndex+")");
            switch (settlerIndex) {
                case 17:
                    return 14;  // -3
                case 19:
                    return 17;  // -2
                case 22:
                    return 19;  // -3
                default:
                    return super.getShadowIndex(settlerIndex);
            }
	}
}
