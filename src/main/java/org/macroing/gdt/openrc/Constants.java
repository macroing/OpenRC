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

final class Constants {
	public static final float EPSILON = 1.e-4F;
	public static final float MAXIMUM_DISTANCE = Float.MAX_VALUE;
	public static final float PI = (float)(Math.PI);
	public static final int HEIGHT = 768;
	public static final int RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE_SCALAR_IN_INTERSECTIONS = 1;
	public static final int RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS = 0;
	public static final int RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS = 2;
	public static final int RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS = 5;
	public static final int RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS = 3;
	public static final int RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS = 0;
	public static final int SIZE_OF_INTERSECTION_IN_INTERSECTIONS = 1 + 1 + 3 + 3;
	public static final int SIZE_OF_PIXEL_IN_PIXELS = 1 + 1 + 1;
	public static final int SIZE_OF_RAY_IN_RAYS = 3 + 3;
	public static final int WIDTH = 1024;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private Constants() {
		
	}
}