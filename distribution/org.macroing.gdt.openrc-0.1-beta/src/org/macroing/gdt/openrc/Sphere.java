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

import java.util.concurrent.ThreadLocalRandom;

final class Sphere extends Shape {
	public static final float TYPE_SPHERE = 1.0F;
	public static final int RELATIVE_OFFSET_OF_SPHERE_COLOR_RGB_IN_SHAPES = 6;
	public static final int RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES = 2;
	public static final int RELATIVE_OFFSET_OF_SPHERE_RADIUS_SCALAR_IN_SHAPES = 5;
	public static final int SIZE_OF_SPHERE_IN_SHAPES = 1 + 1 + 3 + 1 + 3;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final float b;
	private final float g;
	private final float r;
	private final float radius;
	private final float x;
	private final float y;
	private final float z;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Sphere(final float x, final float y, final float z, final float radius, final float r, final float g, final float b) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.radius = radius;
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public float getType() {
		return TYPE_SPHERE;
	}
	
	@Override
	public float[] toFloatArray() {
		return new float[] {
			getType(),
			size(),
			this.x,
			this.y,
			this.z,
			this.radius,
			this.r,
			this.g,
			this.b
		};
	}
	
	@Override
	public int size() {
		return SIZE_OF_SPHERE_IN_SHAPES;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static Sphere random() {
		return new Sphere(doRandom(4000.0F), 16.5F, doRandom(4000.0F), 16.5F, doRandom(255.0F), doRandom(255.0F), doRandom(255.0F));
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static float doRandom(final float range) {
		return ThreadLocalRandom.current().nextFloat() * range;
	}
}