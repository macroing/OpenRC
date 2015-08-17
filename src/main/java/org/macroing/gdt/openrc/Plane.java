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

import java.io.DataOutput;
import java.io.IOException;
import java.io.UncheckedIOException;

final class Plane extends Shape {
	public static final float TYPE = 2.0F;
	public static final int RELATIVE_OFFSET_OF_PLANE_SURFACE_NORMAL = 3;
	public static final int SIZE_OF_PLANE = 1 + 1 + 1 + 3;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final float surfaceNormalX;
	private final float surfaceNormalY;
	private final float surfaceNormalZ;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Plane(final float materialOffset, final float surfaceNormalX, final float surfaceNormalY, final float surfaceNormalZ) {
		super(materialOffset);
		
		this.surfaceNormalX = surfaceNormalX;
		this.surfaceNormalY = surfaceNormalY;
		this.surfaceNormalZ = surfaceNormalZ;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public float getType() {
		return TYPE;
	}
	
	@Override
	public float[] toFloatArray() {
		return new float[] {
			getType(),
			size(),
			getMaterialOffset(),
			this.surfaceNormalX,
			this.surfaceNormalY,
			this.surfaceNormalZ
		};
	}
	
	@Override
	public int size() {
		return SIZE_OF_PLANE;
	}
	
	@Override
	public void write(final DataOutput dataOutput) {
		try {
			dataOutput.writeFloat(getType());
			dataOutput.writeFloat(size());
			dataOutput.writeFloat(getMaterialOffset());
			dataOutput.writeFloat(this.surfaceNormalX);
			dataOutput.writeFloat(this.surfaceNormalY);
			dataOutput.writeFloat(this.surfaceNormalZ);
		} catch(final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}