package jsettlers.graphics.image.reader.custom.graphics;

import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.SettlerImage;
import jsettlers.graphics.image.SingleImage;
import jsettlers.graphics.image.reader.DatFileReader;
import jsettlers.graphics.image.reader.ImageArrayProvider;
import jsettlers.graphics.image.reader.ImageMetadata;
import jsettlers.graphics.image.reader.translator.DatBitmapTranslator;
import jsettlers.graphics.image.sequence.ArraySequence;
import jsettlers.graphics.image.sequence.Sequence;
import jsettlers.graphics.image.sequence.SequenceList;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

public class DirectoryDatFile implements DatFileReader {


	public static final File CUSTOM_GRAPHICS_DIRECTORY = new File("graphics");

	private final DatFileReader fallback;
	private final SequenceList<Image> settlers;
	private final Sequence<SingleImage> guis;

	public DirectoryDatFile(DatFileReader reader, int fileIndex) {
		File dir = getDirectory(fileIndex);
		fallback = reader;

		settlers = readSettlerImages(new File(dir, "settlers"));
		guis = readSequence(new File(dir, "gui"), DirectoryDatFile::readSingleImage, SingleImage[]::new);
	}

	private <T extends Image> Sequence<T> readSequence(File dir, Function<File, T> readFunc, IntFunction<T[]> arrayFunc) {
		List<T> images = new ArrayList<>();

		for(int i = 0; i < 1000; i++) {
			File imageFile = new File(dir, String.format(Locale.ENGLISH, "%03d", i));
			T next = readFunc.apply(imageFile);
			if(next == null) break;
			images.add(next);
		}

		return new ArraySequence<>(images.toArray(arrayFunc));
	}

	private static SingleImage readSingleImage(File file) {
		File imageFile = new File(file.getParent(), file.getName() + ".png");
		if(!imageFile.exists()) return null;
		BufferedImage img;
		try {
			img = ImageIO.read(imageFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		File offsetFile = new File(file.getParent(), file.getName() + ".offset");
		int offsetX = 0;
		int offsetY = 0;
		if(offsetFile.exists()) {
			try(BufferedReader reader = new BufferedReader(new FileReader(offsetFile))) {
				String offsetLine = reader.readLine();
				String[] parts = offsetLine.split(" ");
				offsetX = Integer.parseInt(parts[0]);
				offsetY = Integer.parseInt(parts[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		int width = img.getWidth();
		int height = img.getHeight();

		ShortBuffer data = ByteBuffer.allocateDirect(width*height*2).order(ByteOrder.nativeOrder()).asShortBuffer();
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				int color = img.getRGB(x, y);

				int a = color>>24&0xFF;
				int r = color>>16&0xFF;
				int g = color>>8&0xFF;
				int b = color&0xFF;

				r = (int)((r/255f)*15f);
				g = (int)((g/255f)*15f);
				b = (int)((b/255f)*15f);
				a = (int)((a/255f)*15f);

				r&=15;
				g&=15;
				b&=15;
				a&=15;

				data.put((short) (r<<12 | g << 8 | b << 4 | a));
			}
		}

		data.rewind();

		img.flush();

		return new SingleImage(data, width, height, offsetX, offsetY, "unknown");
	}

	private static SettlerImage readSettlerImage(File file) {
		SingleImage origSettler = readSingleImage(file);
		if(origSettler == null) return null;

		ImageMetadata meta = new ImageMetadata();
		meta.height = origSettler.getHeight();
		meta.width = origSettler.getWidth();
		meta.offsetX = origSettler.getOffsetX();
		meta.offsetY = origSettler.getOffsetY();
		SettlerImage settler = new SettlerImage(meta, origSettler::getData, "unknown");

		settler.setTorso(readSingleImage(new File(file.getParent(), file.getName() + "_torso")));
		settler.setShadow(readSingleImage(new File(file.getParent(), file.getName() + "_shadow")));
		return settler;
	}

	private SequenceList<Image> readSettlerImages(File dir) {
		List<Sequence<Image>> sequences = new ArrayList<>();

		for(int i = 0;i < 1000; i++) {
			File sequenceDir = new File(dir, String.format(Locale.ENGLISH, "%03d", i));
			if(!sequenceDir.exists()) break;
			sequences.add(this.readSequence(sequenceDir, DirectoryDatFile::readSettlerImage, SettlerImage[]::new));
		}

		return new SequenceList<>() {
			@Override
			public Sequence<Image> get(int index) {
				return sequences.get(index);
			}

			@Override
			public int size() {
				return sequences.size();
			}
		};
	}

	public static File getDirectory(int fileIndex) {
		return new File(CUSTOM_GRAPHICS_DIRECTORY, String.format(Locale.ENGLISH, "%02d", fileIndex));
	}

	@Override
	public DatBitmapTranslator<SingleImage> getLandscapeTranslator() {
		return fallback.getLandscapeTranslator();
	}

	@Override
	public long getOffsetForLandscape(int index) {
		return fallback.getOffsetForLandscape(index);
	}

	@Override
	public <T extends Image> long readImageHeader(DatBitmapTranslator<T> translator, ImageMetadata metadata, long offset) throws IOException {
		return fallback.readImageHeader(translator, metadata, offset);
	}

	@Override
	public <T extends Image> void readCompressedData(DatBitmapTranslator<T> translator, ImageMetadata metadata, ImageArrayProvider array, long offset) throws IOException {
		fallback.readCompressedData(translator, metadata, array, offset);
	}

	@Override
	public SequenceList<Image> getSettlers() {
		return settlers;
	}

	@Override
	public Sequence<SingleImage> getLandscapes() {
		return fallback.getLandscapes();
	}

	@Override
	public Sequence<SingleImage> getGuis() {
		return guis;
	}
}
