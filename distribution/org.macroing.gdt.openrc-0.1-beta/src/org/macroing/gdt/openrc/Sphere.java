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
import java.util.concurrent.ThreadLocalRandom;

final class Sphere extends Shape {
	public static final float TYPE = 1.0F;
	public static final int RELATIVE_OFFSET_OF_SPHERE_POSITION = 3;
	public static final int RELATIVE_OFFSET_OF_SPHERE_RADIUS = 6;
	public static final int SIZE_OF_SPHERE = 1 + 1 + 1 + 3 + 1;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final float radius;
	private final float x;
	private final float y;
	private final float z;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Sphere(final float materialOffset, final float x, final float y, final float z, final float radius) {
		super(materialOffset);
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.radius = radius;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public float getRadius() {
		return this.radius;
	}
	
	@Override
	public float getType() {
		return TYPE;
	}
	
	public float getX() {
		return this.x;
	}
	
	public float getY() {
		return this.y;
	}
	
	public float getZ() {
		return this.z;
	}
	
	@Override
	public float[] toFloatArray() {
		final float[] array = new float[size()];
		
		array[0] = getType();
		array[1] = size();
		array[2] = getMaterialOffset();
		array[3] = this.x;
		array[4] = this.y;
		array[5] = this.z;
		array[6] = this.radius;
		
		return array;
	}
	
	@Override
	public int size() {
		return SIZE_OF_SPHERE;
	}
	
	@Override
	public void write(final DataOutput dataOutput) {
		try {
			dataOutput.writeFloat(getType());
			dataOutput.writeFloat(size());
			dataOutput.writeFloat(getMaterialOffset());
			dataOutput.writeFloat(this.x);
			dataOutput.writeFloat(this.y);
			dataOutput.writeFloat(this.z);
			dataOutput.writeFloat(this.radius);
		} catch(final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static Sphere random(final float materialOffset) {
		return new Sphere(materialOffset, doRandom(1000.0F), 12.5F, doRandom(1000.0F), 16.5F);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static float doRandom(final float range) {
		return ThreadLocalRandom.current().nextFloat() * range;
	}
}