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
 * The values in the {@code float} array {@code array} consists of the following:
 * <ol>
 * <li>Eye X</li>
 * <li>Eye Y</li>
 * <li>Eye Z</li>
 * <li>Up X</li>
 * <li>Up Y</li>
 * <li>Up Z</li>
 * <li>Look-at X</li>
 * <li>Look-at Y</li>
 * <li>Look-at Z</li>
 * <li>ONB-U X</li>
 * <li>ONB-U Y</li>
 * <li>ONB-U Z</li>
 * <li>ONB-V X</li>
 * <li>ONB-V Y</li>
 * <li>ONB-V Z</li>
 * <li>ONB-W X</li>
 * <li>ONB-W Y</li>
 * <li>ONB-W Z</li>
 * <li>View-plane distance</li>
 * </ol>
 * <p>
 * If you don't know what ONB stands for, then it is OrthoNormal Basis.
 * 
 * TODO: Find out whether Look-at Z and View-plane distance are correlated in some way!?
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
final class Camera {
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_EYE = 0;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT = 6;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U = 9;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V = 12;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W = 15;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_UP = 3;
	public static final int ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE = 18;
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
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE + 0];
	}
	
	public float getEyeY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE + 1];
	}
	
	public float getEyeZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE + 2];
	}
	
	public float getLookAtX() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT + 0];
	}
	
	public float getLookAtY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT + 1];
	}
	
	public float getLookAtZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT + 2];
	}
	
	public float getUpX() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP + 0];
	}
	
	public float getUpY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP + 1];
	}
	
	public float getUpZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP + 2];
	}
	
	public float getOrthoNormalBasisUX() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U + 0];
	}
	
	public float getOrthoNormalBasisUY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U + 1];
	}
	
	public float getOrthoNormalBasisUZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U + 2];
	}
	
	public float getOrthoNormalBasisVX() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V + 0];
	}
	
	public float getOrthoNormalBasisVY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V + 1];
	}
	
	public float getOrthoNormalBasisVZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V + 2];
	}
	
	public float getOrthoNormalBasisWX() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W + 0];
	}
	
	public float getOrthoNormalBasisWY() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W + 1];
	}
	
	public float getOrthoNormalBasisWZ() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W + 2];
	}
	
	public float getViewPlaneDistance() {
		return this.array[ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE];
	}
	
	public float[] getArray() {
		return this.array;
	}
	
	public void calculateOrthonormalBasis() {
		Vector.subtract(this.array, ABSOLUTE_OFFSET_OF_CAMERA_EYE, this.array, ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W);
		doNormalize(this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W);
		Vector.crossProduct(this.array, ABSOLUTE_OFFSET_OF_CAMERA_UP, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U);
		doNormalize(this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U);
		Vector.crossProduct(this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V);
	}
	
	public void move(final float x, final float y, final float z) {
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE + 0] += x;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE + 1] += y;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE + 2] += z;
		
		calculateOrthonormalBasis();
	}
	
	public void setEye(final float x, final float y, final float z) {
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE + 0] = x;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE + 1] = y;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_EYE + 2] = z;
	}
	
	public void setLookAt(final float x, final float y, final float z) {
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT + 0] = x;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT + 1] = y;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT + 2] = z;
	}
	
	public void setUp(final float x, final float y, final float z) {
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP + 0] = x;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP + 1] = y;
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_UP + 2] = z;
	}
	
	public void setViewPlaneDistance(final float distance) {
		this.array[ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE] = distance;
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