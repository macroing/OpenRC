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
	public static final int RELATIVE_OFFSET_OF_SPHERE_COLOR = 6;
	public static final int RELATIVE_OFFSET_OF_SPHERE_POSITION = 2;
	public static final int RELATIVE_OFFSET_OF_SPHERE_RADIUS = 5;
	public static final int RELATIVE_OFFSET_OF_SPHERE_TEXTURE_COUNT = 9;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final int SIZE_OF_SPHERE = 1 + 1 + 3 + 1 + 3 + 1;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final float b;
	private final float g;
	private final float r;
	private final float radius;
	private final float textureCount;
	private final float x;
	private final float y;
	private final float z;
	private final float[] textureOffsets;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Sphere(final float x, final float y, final float z, final float radius, final float r, final float g, final float b, final float[] textureOffsets) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.radius = radius;
		this.r = r;
		this.g = g;
		this.b = b;
		this.textureCount = textureOffsets.length;
		this.textureOffsets = textureOffsets;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public float getType() {
		return TYPE_SPHERE;
	}
	
	@Override
	public float[] toFloatArray() {
		final float[] array = new float[size()];
		
		array[0] = getType();
		array[1] = size();
		array[2] = this.x;
		array[3] = this.y;
		array[4] = this.z;
		array[5] = this.radius;
		array[6] = this.r;
		array[7] = this.g;
		array[8] = this.b;
		array[9] = this.textureCount;
		
		for(int i = 0; i < this.textureCount; i++) {
			array[i + 10] = this.textureOffsets[i];
		}
		
		return array;
	}
	
	@Override
	public int size() {
		return SIZE_OF_SPHERE + (int)(this.textureCount);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static Sphere random(final float... textureOffsets) {
		return new Sphere(doRandom(4000.0F), 16.5F, doRandom(4000.0F), 16.5F, doRandom(255.0F), doRandom(255.0F), doRandom(255.0F), textureOffsets);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static float doRandom(final float range) {
		return ThreadLocalRandom.current().nextFloat() * range;
	}
}