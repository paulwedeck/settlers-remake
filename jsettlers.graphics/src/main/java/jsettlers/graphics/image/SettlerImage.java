/*******************************************************************************
 * Copyright (c) 2015
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.graphics.image;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import go.graphics.EUnifiedMode;
import go.graphics.GLDrawContext;
import jsettlers.graphics.image.reader.ImageMetadata;
import jsettlers.graphics.image.reader.translator.ImageDataProducer;
import jsettlers.shared.Color;

/**
 * This is the image of something that is displayed as an object on the map, e.g. an settler.
 * <p>
 * It can have a torso, an overlay that is always drawn together with the image.
 * 
 * @author michael
 */
public class SettlerImage extends SingleImage {

	public static float shadow_offset = 0;
	private SingleImage torso = null;
	private SingleImage shadow = null;

	/**
	 * Creates a new settler image.
	 * 
	 * @param metadata
	 *            The mata data to use.
	 * @param data
	 *            The data to use.
	 * @param name
	 * 				The name of the image.
	 */
	public SettlerImage(ImageMetadata metadata, ImageDataProducer data, String name) {
		super(metadata, data, name);
	}

	@Override
	public void drawAt(GLDrawContext gl, float x, float y, float z, Color torsoColor, float fow) {
		checkHandles(gl);
		geometryIndex.drawComplexQuad(EUnifiedMode.SETTLER_SHADOW, x, y, z, 1, 1, torsoColor, fow);
	}

	@Override
	public void drawOnlyImageAt(GLDrawContext gl, float x, float y, float z, Color torsoColor, float fow) {
		checkHandles(gl);
		geometryIndex.drawComplexQuad(EUnifiedMode.SETTLER, x, y, z, 1, 1, torsoColor, fow);
	}

	@Override
	public void drawOnlyShadowAt(GLDrawContext gl, float x, float y, float z) {
		checkHandles(gl);
		geometryIndex.drawComplexQuad(EUnifiedMode.SHADOW_ONLY, x, y, z, 1, 1, Color.TRANSPARENT, 1);
	}

	/**
	 * Sets the image overlay.
	 * 
	 * @param torso
	 *            The torso. May be null.
	 */
	public void setTorso(SingleImage torso) {
		this.torso = torso;
	}

	public void setShadow(SingleImage shadow) {
		this.shadow = shadow;
	}

	/**
	 * Gets the torso for this image.
	 * 
	 * @return The torso.
	 */
	public SingleImage getTorso() {
		return this.torso;
	}

	public SingleImage getShadow() {
		return shadow;
	}

	protected ShortBuffer generateTextureData() {
		toffsetX = offsetX;
		toffsetY = offsetY;

		int tx = offsetX+width;
		int ty = offsetY+height;

		if(torso != null) {
			if(torso.offsetX < toffsetX) toffsetX = torso.offsetX;
			if(torso.offsetY < toffsetY) toffsetY = torso.offsetY;
			if(torso.offsetX+torso.width > tx) tx = torso.offsetX+torso.width;
			if(torso.offsetY+torso.height > ty) ty = torso.offsetY+torso.height;
		}

		if(shadow != null) {
			if(shadow.offsetX < toffsetX) toffsetX = shadow.offsetX;
			if(shadow.offsetY < toffsetY) toffsetY = shadow.offsetY;
			if(shadow.offsetX+shadow.width > tx) tx = shadow.offsetX+shadow.width;
			if(shadow.offsetY+shadow.height > ty) ty = shadow.offsetY+shadow.height;
		}

		twidth = tx-toffsetX;
		theight = ty-toffsetY;

		ShortBuffer tdata = ByteBuffer.allocateDirect(twidth * theight * 2).order(ByteOrder.nativeOrder()).asShortBuffer();

		short[] temp = new short[0];

		if(shadow != null) {
			ShortBuffer shadowData = shadow.getData();

			int hoffX = shadow.offsetX-toffsetX;
			int hoffY = shadow.offsetY-toffsetY;

			if(temp.length < shadow.width) temp = new short[shadow.width];

			for(int y = 0;y != shadow.height;y++) {
				shadowData.position(y*shadow.width);
				shadowData.get(temp, 0, shadow.width);

				for(int x = 0;x != shadow.width;x++) {
					if(temp[x] == 0) continue;
					tdata.put((y+hoffY)*twidth+hoffX+x, (short)((temp[x]&0xF)<<8)); // move alpha to green
				}
			}
		}

		if(torso != null) {
			ShortBuffer torsoData = torso.getData();

			int toffX = torso.offsetX-toffsetX;
			int toffY = torso.offsetY-toffsetY;

			if(temp.length < torso.width) temp = new short[torso.width];

			for(int y = 0;y != torso.height;y++) {
				torsoData.position(y*torso.width);
				torsoData.get(temp, 0, torso.width);

				for(int x = 0;x != torso.width;x++) {
					if(temp[x] == 0) continue;
					tdata.put((y+toffY)*twidth+toffX+x, (short) ((temp[x]&0xF0)|0xF000)); // strip out everything except blue channel and set full red channel
				}
			}
		}

		int soffX = offsetX-toffsetX;
		int soffY = offsetY-toffsetY;

		if(temp.length < width) temp = new short[width];

		ShortBuffer data = getData();

		for(int y = 0;y != height;y++) {
			data.position(y*width);
			data.get(temp, 0, width);

			for(int x = 0;x != width;x++) {
				if(temp[x] != 0) tdata.put((y+soffY)*twidth+soffX+x, temp[x]);
			}
		}
		return tdata;
	}
}
