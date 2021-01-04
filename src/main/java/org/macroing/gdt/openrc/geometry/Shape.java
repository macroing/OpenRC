/**
 * Copyright 2009 - 2021 J&#246;rgen Lundgren
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
package org.macroing.gdt.openrc.geometry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * The values in the {@code float} array created by the {@code toFloatArray()} method consists of the following:
 * <ol>
 * <li>Type</li>
 * <li>Size</li>
 * <li>MaterialOffset</li>
 * <li>Data[Size - 3]</li>
 * </ol>
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public abstract class Shape {
	public static final int RELATIVE_OFFSET_OF_MATERIAL_OFFSET = 2;
	public static final int RELATIVE_OFFSET_OF_SIZE = 1;
	public static final int RELATIVE_OFFSET_OF_TYPE = 0;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final float materialOffset;
	private int index;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	protected Shape(final float materialOffset) {
		this.materialOffset = materialOffset;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public final float getMaterialOffset() {
		return this.materialOffset;
	}
	
	public abstract float getType();
	
	public abstract float[] toFloatArray();
	
	public final int getIndex() {
		return this.index;
	}
	
	public abstract int size();
	
	public final void setIndex(final int index) {
		this.index = index;
	}
	
	public abstract void write(final DataOutput dataOutput);
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static Shape read(final DataInput dataInput) {
		try {
			final float type = dataInput.readFloat();
			final float size = dataInput.readFloat();
			final float materialOffset = dataInput.readFloat();
			
			if(type == Plane.TYPE && size == Plane.SIZE) {
				final float surfaceNormalX = dataInput.readFloat();
				final float surfaceNormalY = dataInput.readFloat();
				final float surfaceNormalZ = dataInput.readFloat();
				
				return new Plane(materialOffset, surfaceNormalX, surfaceNormalY, surfaceNormalZ);
			} else if(type == Sphere.TYPE && size == Sphere.SIZE) {
				final float x = dataInput.readFloat();
				final float y = dataInput.readFloat();
				final float z = dataInput.readFloat();
				final float radius = dataInput.readFloat();
				
				return new Sphere(materialOffset, x, y, z, radius);
			} else if(type == Triangle.TYPE && size == Triangle.SIZE) {
				final float aX = dataInput.readFloat();
				final float aY = dataInput.readFloat();
				final float aZ = dataInput.readFloat();
				final float bX = dataInput.readFloat();
				final float bY = dataInput.readFloat();
				final float bZ = dataInput.readFloat();
				final float cX = dataInput.readFloat();
				final float cY = dataInput.readFloat();
				final float cZ = dataInput.readFloat();
				final float surfaceNormalX = dataInput.readFloat();
				final float surfaceNormalY = dataInput.readFloat();
				final float surfaceNormalZ = dataInput.readFloat();
				
				return new Triangle(materialOffset, aX, aY, aZ, bX, bY, bZ, cX, cY, cZ, surfaceNormalX, surfaceNormalY, surfaceNormalZ);
			}
			
			throw new IllegalArgumentException();
		} catch(final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}