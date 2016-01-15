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
package org.macroing.gdt.openrc;

public final class Constants {
	public static final float EPSILON = 1.e-4F;
	public static final float MAXIMUM_DISTANCE = Float.MAX_VALUE;
	public static final float PI = (float)(Math.PI);
	public static final int HEIGHT = 768;//1080;
	public static final int HEIGHT_SCALE = 1;
	public static final int RELATIVE_OFFSET_OF_PICK_SHAPE_DISTANCE = 1;
	public static final int RELATIVE_OFFSET_OF_PICK_SHAPE_OFFSET = 0;
	public static final int RELATIVE_OFFSET_OF_PICK_TEXTURE_OFFSET = 2;
	public static final int RELATIVE_OFFSET_OF_PICK_TEXTURE_UV = 3;
	public static final int RELATIVE_OFFSET_OF_RAY_DIRECTION_0 = 3;
	public static final int RELATIVE_OFFSET_OF_RAY_DIRECTION_1 = 9;
	public static final int RELATIVE_OFFSET_OF_RAY_ORIGIN_0 = 0;
	public static final int RELATIVE_OFFSET_OF_RAY_ORIGIN_1 = 6;
	public static final int SIZE_OF_PICK = 1 + 1 + 1 + 2;
	public static final int SIZE_OF_PIXEL = 3 + 3;
	public static final int SIZE_OF_RAY = 3 + 3 + 3 + 3;
	public static final int WIDTH = 1024;//1920;
	public static final int WIDTH_SCALE = 1;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final String DEFAULT_VERSION = "0.1-beta";
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private Constants() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static String getVersion() {
//		return Constants.class.getPackage().getImplementationVersion();
		return DEFAULT_VERSION;
	}
}