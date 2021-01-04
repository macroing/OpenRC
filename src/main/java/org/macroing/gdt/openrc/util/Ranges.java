/**
 * Copyright 2009 - 2021 J&#246;rgen Lundgren
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
package org.macroing.gdt.openrc.util;

/**
 * A class that consists exclusively of static methods that performs various operations on ranges.
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public final class Ranges {
	private Ranges() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Returns {@code value}, but only if it is within the range of {@code minimum} (inclusive) and {@code maximum} (inclusive).
	 * <p>
	 * If it is not within said range, an {@code IllegalArgumentException} will be thrown.
	 * 
	 * @param value the value to verify
	 * @param minimum the minimum value allowed (inclusive)
	 * @param maximum the maximum value allowed (inclusive)
	 * @return {@code value}, but only if it is within the range of {@code minimum} (inclusive) and {@code maximum} (inclusive)
	 * @throws IllegalArgumentException thrown if, and only if, {@code value} is less than {@code minimum} or greater than {@code maximum}
	 */
	public static int requireRange(final int value, final int minimum, final int maximum) {
		if(value < minimum) {
			throw new IllegalArgumentException(String.format("%s is less than %s", Integer.toString(value), Integer.toString(minimum)));
		} else if(value > maximum) {
			throw new IllegalArgumentException(String.format("%s is greater than %s", Integer.toString(value), Integer.toString(maximum)));
		} else {
			return value;
		}
	}
}