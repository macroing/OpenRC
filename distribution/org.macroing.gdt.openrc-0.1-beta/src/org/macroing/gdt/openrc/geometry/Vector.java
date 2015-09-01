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
package org.macroing.gdt.openrc.geometry;

import static org.macroing.gdt.openrc.Mathematics.sqrt;

/**
 * A class that consists exclusively of static methods that performs various operations on vectors.
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public final class Vector {
	private Vector() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Returns the dot product between two vectors.
	 * <p>
	 * The first vector corresponds to X, Y and Z, located at {@code vector0[offset0]}, {@code vector0[offset0 + 1]} and {@code vector0[offset0 + 2]}, respectively.
	 * <p>
	 * The second vector corresponds to X, Y and Z, located at {@code vector1[offset1]}, {@code vector1[offset1 + 1]} and {@code vector1[offset1 + 2]}, respectively.
	 * <p>
	 * If either {@code vector0} or {@code vector1} are {@code null}, a {@code NullPointerException} will be thrown.
	 * <p>
	 * If {@code offset0} is less than {@code 0} or {@code offset0 + 2} is greater than or equal to {@code vector0.length}, or {@code offset1} is less than {@code 0} or {@code offset1 + 2} is greater than or equal to {@code vector1.length}, an
	 * {@code ArrayIndexOutOfBoundsException} will be thrown.
	 * 
	 * @param vector0 a {@code float} array with one or more vectors in
	 * @param offset0 the start offset in {@code vector0}
	 * @param vector1 a {@code float} array with one or more vectors in
	 * @param offset1 the start offset in {@code vector1}
	 * @return the dot product between two vectors
	 * @throws ArrayIndexOutOfBoundsException thrown if, and only if, {@code offset0} is less than {@code 0} or {@code offset0 + 2} is greater than or equal to {@code vector0.length}, or {@code offset1} is less than {@code 0} or {@code offset1 + 2} is
	 * greater than or equal to {@code vector1.length}
	 * @throws NullPointerException thrown if, and only if, either {@code vector0} or {@code vector1} are {@code null}
	 */
	public static float dotProduct(final float[] vector0, final int offset0, final float[] vector1, final int offset1) {
		return vector0[offset0] * vector1[offset1] + vector0[offset0 + 1] * vector1[offset1 + 1] + vector0[offset0 + 2] * vector1[offset1 + 2];
	}
	
	/**
	 * Returns the length of a vector.
	 * <p>
	 * The vector corresponds to X, Y and Z, located at {@code vector[offset]}, {@code vector[offset + 1]} and {@code vector[offset + 2]}, respectively.
	 * <p>
	 * If {@code vector} is {@code null}, a {@code NullPointerException} will be thrown.
	 * <p>
	 * If {@code offset} is less than {@code 0} or {@code offset + 2} is greater than or equal to {@code vector.length}, an {@code ArrayIndexOutOfBoundsException} will be thrown.
	 * 
	 * @param vector a {@code float} array with one or more vectors in
	 * @param offset the start offset in {@code vector}
	 * @return the length of a vector
	 * @throws ArrayIndexOutOfBoundsException thrown if, and only if, {@code offset} is less than {@code 0} or {@code offset + 2} is greater than or equal to {@code vector.length}
	 * @throws NullPointerException thrown if, and only if, {@code vector} is {@code null}
	 */
	public static float length(final float[] vector, final int offset) {
		return sqrt(lengthSquared(vector, offset));
	}
	
	/**
	 * Returns the squared length of a vector.
	 * <p>
	 * The vector corresponds to X, Y and Z, located at {@code vector[offset]}, {@code vector[offset + 1]} and {@code vector[offset + 2]}, respectively.
	 * <p>
	 * If {@code vector} is {@code null}, a {@code NullPointerException} will be thrown.
	 * <p>
	 * If {@code offset} is less than {@code 0} or {@code offset + 2} is greater than or equal to {@code vector.length}, an {@code ArrayIndexOutOfBoundsException} will be thrown.
	 * 
	 * @param vector a {@code float} array with one or more vectors in
	 * @param offset the start offset in {@code vector}
	 * @return the squared length of a vector
	 * @throws ArrayIndexOutOfBoundsException thrown if, and only if, {@code offset} is less than {@code 0} or {@code offset + 2} is greater than or equal to {@code vector.length}
	 * @throws NullPointerException thrown if, and only if, {@code vector} is {@code null}
	 */
	public static float lengthSquared(final float[] vector, final int offset) {
		return dotProduct(vector, offset, vector, offset);
	}
	
	public static float[] surfaceNormal(final float aX, final float aY, final float aZ, final float bX, final float bY, final float bZ, final float cX, final float cY, final float cZ) {
//		Subtract vector a from vector b:
		final float x0 = bX - aX;
		final float y0 = bY - aY;
		final float z0 = bZ - aZ;
		
//		Subtract vector a from vector c:
		final float x1 = cX - aX;
		final float y1 = cY - aY;
		final float z1 = cZ - aZ;
		
//		Perform the cross product on the two subtracted vectors:
		final float x2 = y0 * z1 - z0 * y1;
		final float y2 = z0 * x1 - x0 * z1;
		final float z2 = x0 * y1 - y0 * x1;
		
//		Get the length of the cross product vector:
		final float length = sqrt(x2 * x2 + y2 * y2 + z2 * z2);
		
//		Initialize the surface normal array to return:
		final float[] surfaceNormal = new float[] {x2, y2, z2};
		
		if(length > 0.0F) {
//			Get the reciprocal of the length, such that we can multiply rather than divide:
			final float lengthReciprocal = 1.0F / length;
			
//			Multiply the cross product vector with the reciprocal of the length of the cross product vector itself (normalize):
			surfaceNormal[0] *= lengthReciprocal;
			surfaceNormal[1] *= lengthReciprocal;
			surfaceNormal[2] *= lengthReciprocal;
		}
		
		return surfaceNormal;
	}
	
	/**
	 * Calculates the cross product of two vectors.
	 * <p>
	 * The first vector corresponds to X, Y and Z, located at {@code vector0[offset0]}, {@code vector0[offset0 + 1]} and {@code vector0[offset0 + 2]}, respectively.
	 * <p>
	 * The second vector corresponds to X, Y and Z, located at {@code vector1[offset1]}, {@code vector1[offset1 + 1]} and {@code vector1[offset1 + 2]}, respectively.
	 * <p>
	 * The third vector corresponds to X, Y and Z, located at {@code vector2[offset2]}, {@code vector2[offset2 + 1]} and {@code vector2[offset2 + 2]}, respectively.
	 * <p>
	 * The first and second vectors are part of the cross product calculation. The third vector is where the result will be found after the calculation finishes.
	 * <p>
	 * If either {@code vector0}, {@code vector1} or {@code vector2} are {@code null}, a {@code NullPointerException} will be thrown.
	 * <p>
	 * If {@code offset0} is less than {@code 0} or {@code offset0 + 2} is greater than or equal to {@code vector0.length}, or {@code offset1} is less than {@code 0} or {@code offset1 + 2} is greater than or equal to {@code vector1.length}, or
	 * {@code offset2} is less than {@code 0} or {@code offset2 + 2} is greater than or equal to {@code vector2.length}, an {@code ArrayIndexOutOfBoundsException} will be thrown.
	 * 
	 * @param vector0 a {@code float} array with one or more vectors in
	 * @param offset0 the start offset in {@code vector0}
	 * @param vector1 a {@code float} array with one or more vectors in
	 * @param offset1 the start offset in {@code vector1}
	 * @param vector2 a {@code float} array with one or more vectors in
	 * @param offset2 the start offset in {@code vector2}
	 * @throws ArrayIndexOutOfBoundsException thrown if, and only if, {@code offset0} is less than {@code 0} or {@code offset0 + 2} is greater than or equal to {@code vector0.length}, or {@code offset1} is less than {@code 0} or {@code offset1 + 2} is
	 * greater than or equal to {@code vector1.length}, or {@code offset2} is less than {@code 0} or {@code offset2 + 2} is greater than or equal to {@code vector2.length}
	 * @throws NullPointerException thrown if, and only if, either {@code vector0}, {@code vector1} or {@code vector2} are {@code null}
	 */
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
	
	/**
	 * Performs vector normalization.
	 * <p>
	 * The vector corresponds to X, Y and Z, located at {@code vector[offset]}, {@code vector[offset + 1]} and {@code vector[offset + 2]}, respectively.
	 * <p>
	 * The original vector will be overwritten by the normalized representation of itself.
	 * <p>
	 * If {@code vector} is {@code null}, a {@code NullPointerException} will be thrown.
	 * <p>
	 * If {@code offset} is less than {@code 0} or {@code offset + 2} is greater than or equal to {@code vector.length}, an {@code ArrayIndexOutOfBoundsException} will be thrown.
	 * 
	 * @param vector a {@code float} array with one or more vectors in
	 * @param offset the start offset in {@code vector}
	 * @throws ArrayIndexOutOfBoundsException thrown if, and only if, {@code offset} is less than {@code 0} or {@code offset + 2} is greater than or equal to {@code vector.length}
	 * @throws NullPointerException thrown if, and only if, {@code vector} is {@code null}
	 */
	public static void normalize(final float[] vector, final int offset) {
		final float lengthReciprocal = 1.0F / length(vector, offset);
		
		vector[offset + 0] *= lengthReciprocal;
		vector[offset + 1] *= lengthReciprocal;
		vector[offset + 2] *= lengthReciprocal;
	}
	
	/**
	 * Subtracts one vector from another one.
	 * <p>
	 * The first vector corresponds to X, Y and Z, located at {@code vector0[offset0]}, {@code vector0[offset0 + 1]} and {@code vector0[offset0 + 2]}, respectively.
	 * <p>
	 * The second vector corresponds to X, Y and Z, located at {@code vector1[offset1]}, {@code vector1[offset1 + 1]} and {@code vector1[offset1 + 2]}, respectively.
	 * <p>
	 * The third vector corresponds to X, Y and Z, located at {@code vector2[offset2]}, {@code vector2[offset2 + 1]} and {@code vector2[offset2 + 2]}, respectively.
	 * <p>
	 * The first and second vectors are part of the subtraction calculation. The third vector is where the result will be found after the calculation finishes.
	 * <p>
	 * Think of the subtraction calculation as {@code vector0 - vector1}.
	 * <p>
	 * If either {@code vector0}, {@code vector1} or {@code vector2} are {@code null}, a {@code NullPointerException} will be thrown.
	 * <p>
	 * If {@code offset0} is less than {@code 0} or {@code offset0 + 2} is greater than or equal to {@code vector0.length}, or {@code offset1} is less than {@code 0} or {@code offset1 + 2} is greater than or equal to {@code vector1.length}, or
	 * {@code offset2} is less than {@code 0} or {@code offset2 + 2} is greater than or equal to {@code vector2.length}, an {@code ArrayIndexOutOfBoundsException} will be thrown.
	 * 
	 * @param vector0 a {@code float} array with one or more vectors in
	 * @param offset0 the start offset in {@code vector0}
	 * @param vector1 a {@code float} array with one or more vectors in
	 * @param offset1 the start offset in {@code vector1}
	 * @param vector2 a {@code float} array with one or more vectors in
	 * @param offset2 the start offset in {@code vector2}
	 * @throws ArrayIndexOutOfBoundsException thrown if, and only if, {@code offset0} is less than {@code 0} or {@code offset0 + 2} is greater than or equal to {@code vector0.length}, or {@code offset1} is less than {@code 0} or {@code offset1 + 2} is
	 * greater than or equal to {@code vector1.length}, or {@code offset2} is less than {@code 0} or {@code offset2 + 2} is greater than or equal to {@code vector2.length}
	 * @throws NullPointerException thrown if, and only if, either {@code vector0}, {@code vector1} or {@code vector2} are {@code null}
	 */
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