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

public final class Mathematics {
	public static final float PI = (float)(Math.PI);
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private Mathematics() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static float angle(float angle) {
		if(angle < -PI || angle > PI) {
			angle = (angle + PI) / (2.0F * PI);
			angle -= floor(angle);
			angle = PI * (angle * 2.0F - 1.0F);
		}
		
		return angle;
	}
	
	public static float atan2(final float y, final float x) {
		return (float)(Math.atan2(y, x));
	}
	
	public static float cos(final float angle) {
		return (float)(Math.cos(angle));
	}
	
	public static float sin(final float angle) {
		return (float)(Math.sin(angle));
	}
	
	public static float sqrt(final float value) {
		return (float)(Math.sqrt(value));
	}
	
	public static int floor(final float x) {
		final int xi = (int)(x);
		
		return x < xi ? xi - 1 : xi;
	}
}