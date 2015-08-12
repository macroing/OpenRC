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

final class Triangle extends Shape {
	public static final float TYPE = 3.0F;
	public static final int RELATIVE_OFFSET_OF_TRIANGLE_A = 3;
	public static final int RELATIVE_OFFSET_OF_TRIANGLE_B = 6;
	public static final int RELATIVE_OFFSET_OF_TRIANGLE_C = 9;
	public static final int RELATIVE_OFFSET_OF_TRIANGLE_SURFACE_NORMAL = 12;
	public static final int SIZE_OF_TRIANGLE = 1 + 1 + 1 + 3 + 3 + 3 + 3;
	
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
		
		final float[] surfaceNormal = doToSurfaceNormal(aX, aY, aZ, bX, bY, bZ, cX, cY, cZ);
		
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
		return SIZE_OF_TRIANGLE;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static float[] doToSurfaceNormal(final float aX, final float aY, final float aZ, final float bX, final float bY, final float bZ, final float cX, final float cY, final float cZ) {
//		Subtract vector a from vector b:
		final float x0 = bX - aX;
		final float y0 = bY - aY;
		final float z0 = bZ - aZ;
		
//		Subtract vector a from vector c:
		final float x1 = cX - aX;
		final float y1 = cY - aY;
		final float z1 = cZ - aZ;
		
//		Perform the cross product on the two subtracted vectors:
		final float x2 = y0 * z1 - z0 * y1;
		final float y2 = z0 * x1 - x0 * z1;
		final float z2 = x0 * y1 - y0 * x1;
		
//		Get the length of the cross product vector:
		final float length = (float)(Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2));
		
//		Initialize the surface normal array to return:
		final float[] surfaceNormal = new float[] {x2, y2, z2};
		
		if(length > 0.0F) {
//			Get the reciprocal of the length, such that we can multiply rather than divide:
			final float lengthReciprocal = 1.0F / length;
			
//			Multiply the cross product vector with the reciprocal of the length of the cross product vector itself (normalize):
			surfaceNormal[0] *= lengthReciprocal;
			surfaceNormal[1] *= lengthReciprocal;
			surfaceNormal[2] *= lengthReciprocal;
		}
		
		return surfaceNormal;
	}
}