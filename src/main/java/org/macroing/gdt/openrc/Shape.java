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

/**
 * The values in the {@code float} array created by the {@code toFloatArray()} method consists of the following:
 * <ol>
 * <li>Type</li>
 * <li>Size</li>
 * <li>Data[Size - 2]</li>
 * </ol>
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
abstract class Shape {
	public static final int RELATIVE_OFFSET_OF_SHAPE_SIZE = 1;
	public static final int RELATIVE_OFFSET_OF_SHAPE_TYPE = 0;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private int index;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	protected Shape() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public abstract float getType();
	
	public abstract float[] toFloatArray();
	
	public final int getIndex() {
		return this.index;
	}
	
	public abstract int size();
	
	public final void setIndex(final int index) {
		this.index = index;
	}
}