/**
 * Copyright 2009 - 2015 J&#246;rgen Lundgren
 * 
 * This file is part of org.macroing.gdt.openrc.
 * 
 * org.macroing.gdt.openrc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * org.macroing.gdt.openrc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with org.macroing.gdt.openrc. If not, see <http://www.gnu.org/licenses/>.
 */
package org.macroing.gdt.openrc;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

final class Texture {
	public static final int RELATIVE_OFFSET_OF_TEXTURE_DATA = 4;
	public static final int RELATIVE_OFFSET_OF_TEXTURE_HEIGHT = 3;
	public static final int RELATIVE_OFFSET_OF_TEXTURE_SIZE = 1;
	public static final int RELATIVE_OFFSET_OF_TEXTURE_TYPE = 0;
	public static final int RELATIVE_OFFSET_OF_TEXTURE_WIDTH = 2;
	public static final int TYPE_DECAL_TEXTURE = 2;
	public static final int TYPE_SOLID_TEXTURE = 1;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final int SIZE_TEXTURE = 1 + 1 + 1 + 1;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final int height;
	private final int type;
	private final int width;
	private final int[] data;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private Texture(final int width, final int height, final int type, final int[] data) {
		this.width = width;
		this.height = height;
		this.type = type;
		this.data = data;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public int getHeight() {
		return this.height;
	}
	
	public int getType() {
		return this.type;
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int size() {
		return SIZE_TEXTURE + this.data.length;
	}
	
	public int[] getData() {
		return this.data;
	}
	
	public int[] toIntArray() {
		final int[] array = new int[size()];
		
		array[0] = getType();
		array[1] = size();
		array[2] = getWidth();
		array[3] = getHeight();
		
		for(int i = 0; i < this.data.length; i++) {
			array[i + 4] = this.data[i];
		}
		
		return array;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static Texture createDecalTexture() {
		return new Texture(1, 1, TYPE_DECAL_TEXTURE, new int[] {255});
	}
	
	public static Texture createDecalTexture(final InputStream inputStream) {
		return doCreateTexture(TYPE_DECAL_TEXTURE, inputStream);
	}
	
	public static Texture createDecalTexture(final String name) {
		try {
			return createDecalTexture(Texture.class.getResourceAsStream(name));
		} catch(final Exception e) {
			return Texture.createDecalTexture();
		}
	}
	
	public static Texture createSolidTexture() {
		return new Texture(1, 1, TYPE_SOLID_TEXTURE, new int[] {255});
	}
	
	public static Texture createSolidTexture(final InputStream inputStream) {
		return doCreateTexture(TYPE_SOLID_TEXTURE, inputStream);
	}
	
	public static Texture createSolidTexture(final String name) {
		try {
			return createSolidTexture(Texture.class.getResourceAsStream(name));
		} catch(final Exception e) {
			return Texture.createSolidTexture();
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static BufferedImage doCreateBufferedImageFrom(final InputStream inputStream) {
		try(final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
			BufferedImage bufferedImage0 = ImageIO.read(bufferedInputStream);
			
			if(bufferedImage0.getType() != BufferedImage.TYPE_INT_RGB) {
				final BufferedImage bufferedImage1 = new BufferedImage(bufferedImage0.getWidth(), bufferedImage0.getHeight(), BufferedImage.TYPE_INT_RGB);
				
				final
				Graphics2D graphics2D = bufferedImage1.createGraphics();
				graphics2D.drawImage(bufferedImage0, 0, 0, null);
				graphics2D.dispose();
				
				bufferedImage0 = bufferedImage1;
			}
			
			return bufferedImage0;
		} catch(final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private static int[] doGetDataFrom(final BufferedImage bufferedImage) {
		final WritableRaster writableRaster = bufferedImage.getRaster();
		
		final DataBuffer dataBuffer = writableRaster.getDataBuffer();
		
		final DataBufferInt dataBufferInt = DataBufferInt.class.cast(dataBuffer);
		
		final int[] data = dataBufferInt.getData();
		
		return data;
	}
	
	private static Texture doCreateTexture(final int type, final InputStream inputStream) {
		final BufferedImage bufferedImage = doCreateBufferedImageFrom(inputStream);
		
		final int width = bufferedImage.getWidth();
		final int height = bufferedImage.getHeight();
		
		final int[] data = doGetDataFrom(bufferedImage);
		
		return new Texture(width, height, type, data);
	}
}