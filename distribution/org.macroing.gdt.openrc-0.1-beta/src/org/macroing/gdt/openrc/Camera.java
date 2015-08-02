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

//TODO: Find out whether Look-at Z and View-plane distance are correlated in some way!?
final class Camera {
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT = 0;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT_VECTOR = 6;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR = 9;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR = 12;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR = 15;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_UP_VECTOR = 3;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE_SCALAR = 18;
	public static final int SIZE_OF_CAMERA = 3 + 3 + 3 + 3 + 3 + 3 + 1;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final float[] array = new float[SIZE_OF_CAMERA];
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Camera() {
		setEye(500.0F, 0.0F, 500.0F);
		setUp(0.0F, 1.0F, 0.0F);
		setLookAt(0.0F, 0.0F, -800.0F);
		setViewPlaneDistance(800.0F);
		calculateOrthonormalBasis();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public float getEyeX() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 0];
	}
	
	public float getEyeY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 1];
	}
	
	public float getEyeZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 2];
	}
	
	public float getLookAtX() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT_VECTOR + 0];
	}
	
	public float getLookAtY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT_VECTOR + 1];
	}
	
	public float getLookAtZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT_VECTOR + 2];
	}
	
	public float getUpX() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP_VECTOR + 0];
	}
	
	public float getUpY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP_VECTOR + 1];
	}
	
	public float getUpZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP_VECTOR + 2];
	}
	
	public float getOrthoNormalBasisUX() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR + 0];
	}
	
	public float getOrthoNormalBasisUY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR + 1];
	}
	
	public float getOrthoNormalBasisUZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR + 2];
	}
	
	public float getOrthoNormalBasisVX() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR + 0];
	}
	
	public float getOrthoNormalBasisVY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR + 1];
	}
	
	public float getOrthoNormalBasisVZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR + 2];
	}
	
	public float getOrthoNormalBasisWX() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR + 0];
	}
	
	public float getOrthoNormalBasisWY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR + 1];
	}
	
	public float getOrthoNormalBasisWZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR + 2];
	}
	
	public float getViewPlaneDistance() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE_SCALAR];
	}
	
	public float[] getArray() {
		return this.array;
	}
	
	public void calculateOrthonormalBasis() {
		Vector.subtract(this.array, ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT, this.array, ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT_VECTOR, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR);
		doNormalize(this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR);
		Vector.crossProduct(this.array, ABSOLUTE_OFFSET_OF_CAMERA_UP_VECTOR, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR);
		doNormalize(this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR);
		Vector.crossProduct(this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR);
	}
	
	public void move(final float x, final float y, final float z) {
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 0] += x;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 1] += y;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 2] += z;
		
		calculateOrthonormalBasis();
	}
	
	public void setEye(final float x, final float y, final float z) {
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 0] = x;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 1] = y;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 2] = z;
	}
	
	public void setLookAt(final float x, final float y, final float z) {
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT_VECTOR + 0] = x;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT_VECTOR + 1] = y;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT_VECTOR + 2] = z;
	}
	
	public void setUp(final float x, final float y, final float z) {
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP_VECTOR + 0] = x;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP_VECTOR + 1] = y;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP_VECTOR + 2] = z;
	}
	
	public void setViewPlaneDistance(final float distance) {
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE_SCALAR] = distance;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static float doLength(final float[] vector, final int offset) {
		return (float)(Math.sqrt(Vector.lengthSquared(vector, offset)));
	}
	
	private static void doNormalize(final float[] vector, final int offset) {
		final float lengthReciprocal = 1.0F / doLength(vector, offset);
		
		vector[offset + 0] *= lengthReciprocal;
		vector[offset + 1] *= lengthReciprocal;
		vector[offset + 2] *= lengthReciprocal;
	}
}