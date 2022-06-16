package jsettlers.graphics.image.reader.shadowmap;

public interface ShadowMapping {

	static ShadowMapping getMappingFor(int fileIndex) {
            System.out.println("getMappingFor("+fileIndex+")");
		// TODO remove all hacks in AdvancedDatFileReader#initialize
		switch (fileIndex) {
			case 1:
				return new ShadowMapping1();
			case 6:
				return new ShadowMapping6();
			case 22:
				return new ShadowMapping22();
			case 42:
				return new ShadowMapping42();
			case 43:
				return new ShadowMapping43();
			default:
				return new IdentityShadowMapping();
		}
	}

	int getShadowIndex(int settlerIndex);
}
