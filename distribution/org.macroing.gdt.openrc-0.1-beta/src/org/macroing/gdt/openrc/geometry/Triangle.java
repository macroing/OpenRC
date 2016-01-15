/**
 * Copyright 2009 - 2016 J&#246;rgen Lundgren
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

import java.io.DataOutput;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class Triangle extends Shape {
	public static final float TYPE = 3.0F;
	public static final int RELATIVE_OFFSET_OF_A = 3;
	public static final int RELATIVE_OFFSET_OF_B = 6;
	public static final int RELATIVE_OFFSET_OF_C = 9;
	public static final int RELATIVE_OFFSET_OF_SURFACE_NORMAL = 12;
	public static final int SIZE = 1 + 1 + 1 + 3 + 3 + 3 + 3;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final float aX;
	private final float aY;
	private final float aZ;
	private final float bX;
	private final float bY;
	private final float bZ;
	private final float cX;
	private final float cY;
	private final float cZ;
	private final float surfaceNormalX;
	private final float surfaceNormalY;
	private final float surfaceNormalZ;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Triangle(final float materialOffset, final float aX, final float aY, final float aZ, final float bX, final float bY, final float bZ, final float cX, final float cY, final float cZ) {
		super(materialOffset);
		
		final float[] surfaceNormal = Vector.surfaceNormal(aX, aY, aZ, bX, bY, bZ, cX, cY, cZ);
		
		this.aX = aX;
		this.aY = aY;
		this.aZ = aZ;
		this.bX = bX;
		this.bY = bY;
		this.bZ = bZ;
		this.cX = cX;
		this.cY = cY;
		this.cZ = cZ;
		this.surfaceNormalX = surfaceNormal[0];
		this.surfaceNormalY = surfaceNormal[1];
		this.surfaceNormalZ = surfaceNormal[2];
	}
	
	public Triangle(final float materialOffset, final float aX, final float aY, final float aZ, final float bX, final float bY, final float bZ, final float cX, final float cY, final float cZ, final float surfaceNormalX, final float surfaceNormalY, final float surfaceNormalZ) {
		super(materialOffset);
		
		this.aX = aX;
		this.aY = aY;
		this.aZ = aZ;
		this.bX = bX;
		this.bY = bY;
		this.bZ = bZ;
		this.cX = cX;
		this.cY = cY;
		this.cZ = cZ;
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
			this.aX,
			this.aY,
			this.aZ,
			this.bX,
			this.bY,
			this.bZ,
			this.cX,
			this.cY,
			this.cZ,
			this.surfaceNormalX,
			this.surfaceNormalY,
			this.surfaceNormalZ
		};
	}
	
	@Override
	public int size() {
		return SIZE;
	}
	
	@Override
	public void write(final DataOutput dataOutput) {
		try {
			dataOutput.writeFloat(getType());
			dataOutput.writeFloat(size());
			dataOutput.writeFloat(getMaterialOffset());
			dataOutput.writeFloat(this.aX);
			dataOutput.writeFloat(this.aY);
			dataOutput.writeFloat(this.aZ);
			dataOutput.writeFloat(this.bX);
			dataOutput.writeFloat(this.bY);
			dataOutput.writeFloat(this.bZ);
			dataOutput.writeFloat(this.cX);
			dataOutput.writeFloat(this.cY);
			dataOutput.writeFloat(this.cZ);
			dataOutput.writeFloat(this.surfaceNormalX);
			dataOutput.writeFloat(this.surfaceNormalY);
			dataOutput.writeFloat(this.surfaceNormalZ);
		} catch(final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}