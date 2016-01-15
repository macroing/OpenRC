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

import org.macroing.gdt.openrc.Constants;

/**
 * The values in the {@code float} array created by the {@code create(int)} method consists of the following:
 * <ol>
 * <li>Shape offset</li>
 * <li>Shape distance (T)</li>
 * </ol>
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public final class Intersection {
	public static final int RELATIVE_OFFSET_OF_DISTANCE = 1;
	public static final int RELATIVE_OFFSET_OF_SHAPE_OFFSET = 0;
	public static final int RELATIVE_OFFSET_OF_SURFACE_INTERSECTION_POINT = 2;
	public static final int RELATIVE_OFFSET_OF_SURFACE_NORMAL = 5;
	public static final int SIZE = 1 + 1 + 3 + 3;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private Intersection() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static float[] create(final int length) {
		final float[] intersections = new float[length * SIZE];
		
		for(int i = 0; i < intersections.length; i += SIZE) {
			intersections[i + RELATIVE_OFFSET_OF_SHAPE_OFFSET] = -1.0F;
			intersections[i + RELATIVE_OFFSET_OF_DISTANCE] = Constants.MAXIMUM_DISTANCE;
			intersections[i + RELATIVE_OFFSET_OF_SURFACE_INTERSECTION_POINT + 0] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_SURFACE_INTERSECTION_POINT + 1] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_SURFACE_INTERSECTION_POINT + 2] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_SURFACE_NORMAL + 0] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_SURFACE_NORMAL + 1] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_SURFACE_NORMAL + 2] = 0.0F;
		}
		
		return intersections;
	}
}