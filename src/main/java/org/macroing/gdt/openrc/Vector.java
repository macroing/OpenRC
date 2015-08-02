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

final class Vector {
	private Vector() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static float dotProduct(final float[] vector0, final int offset0, final float[] vector1, final int offset1) {
		return vector0[offset0] * vector1[offset1] + vector0[offset0 + 1] * vector1[offset1 + 1] + vector0[offset0 + 2] * vector1[offset1 + 2];
	}
	
	public static float lengthSquared(final float[] vector, final int offset) {
		return dotProduct(vector, offset, vector, offset);
	}
	
	public static void crossProduct(final float[] vector0, final int offset0, final float[] vector1, final int offset1, final float[] vector2, final int offset2) {
		final float x0 = vector0[offset0 + 0];
		final float y0 = vector0[offset0 + 1];
		final float z0 = vector0[offset0 + 2];
		final float x1 = vector0[offset1 + 0];
		final float y1 = vector0[offset1 + 1];
		final float z1 = vector0[offset1 + 2];
		
		vector2[offset2 + 0] = y0 * z1 - z0 * y1;
		vector2[offset2 + 1] = z0 * x1 - x0 * z1;
		vector2[offset2 + 2] = x0 * y1 - y0 * x1;
	}
	
	public static void subtract(final float[] vector0, final int offset0, final float[] vector1, final int offset1, final float[] vector2, final int offset2) {
		final float x0 = vector0[offset0 + 0];
		final float y0 = vector0[offset0 + 1];
		final float z0 = vector0[offset0 + 2];
		final float x1 = vector0[offset1 + 0];
		final float y1 = vector0[offset1 + 1];
		final float z1 = vector0[offset1 + 2];
		
		vector2[offset2 + 0] = x0 - x1;
		vector2[offset2 + 1] = y0 - y1;
		vector2[offset2 + 2] = z0 - z1;
	}
}