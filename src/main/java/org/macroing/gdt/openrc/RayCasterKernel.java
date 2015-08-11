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
 * The values in the {@code float} array {@code rays} consists of the following:
 * <ol>
 * <li>Origin X</li>
 * <li>Origin Y</li>
 * <li>Origin Z</li>
 * <li>Direction X</li>
 * <li>Direction Y</li>
 * <li>Direction Z</li>
 * </ol>
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
final class RayCasterKernel extends AbstractRayCasterKernel {
	private final float[] camera;
	private final float[] intersections;
	private final float[] lights;
	private final float[] materials;
	private final float[] pick;
	private final float[] pixels;
	private final float[] rays;
	private final float[] shapes;
	private final int height;
	private final int lightsLength;
	private final int shapeIndicesLength;
	private final int width;
	private final int[] rGB;
	private final int[] shapeIndices;
	private final int[] textures;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public RayCasterKernel(final float[] pick, final int[] rGB, final Scene scene) {
		this.camera = scene.getCamera().getArray();
		this.intersections = Intersection.create(Constants.WIDTH * Constants.HEIGHT);
		this.lights = scene.getLightsAsArray();
		this.materials = scene.getMaterialsAsArray();
		this.pick = pick;
		this.pixels = new float[Constants.WIDTH * Constants.HEIGHT * Constants.SIZE_OF_PIXEL];
		this.rays = new float[Constants.WIDTH * Constants.HEIGHT * Constants.SIZE_OF_RAY];
		this.shapes = scene.getShapesAsArray();
		this.height = Constants.HEIGHT;
		this.lightsLength = this.lights.length;
		this.shapeIndicesLength = scene.getShapeCount();
		this.width = Constants.WIDTH;
		this.rGB = rGB;
		this.shapeIndices = scene.getShapeIndices();
		this.textures = scene.getTexturesAsArray();
		
//		Make the Kernel instance explicit, such that we have to take care of all array transfers to and from the GPU:
		setExplicit(true);
		
//		Tell the API to fetch the below arrays and their values before executing this Kernel instance (they will be transferred to the GPU):
		put(this.intersections);
		put(this.lights);
		put(this.materials);
		put(this.pick);
		put(this.pixels);
		put(this.rays);
		put(this.shapes);
		put(this.rGB);
		put(this.textures);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * This is what the {@code Kernel} executes on the GPU (or in the CPU).
	 */
	@Override
	public void run() {
//		Initialize index and offset values:
		final int index = getGlobalId();
		final int pickIndex = this.height / 2 * this.width + this.width / 2;
		final int pixelOffset = index * Constants.SIZE_OF_PIXEL;
		final int rayOffset = index * Constants.SIZE_OF_RAY;
		
//		Initialize the U- and V-coordinates:
		final float u = index % this.width - this.width / 2.0F + 0.5F;
		final float v = index / this.width - this.height / 2.0F + 0.5F;
		
//		Update the origin point and direction vector of the ray to fire:
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN + 0] = this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_EYE + 0];
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN + 1] = this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_EYE + 1];
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN + 2] = this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_EYE + 2];
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION + 0] = this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U + 0] * u + this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V + 0] * v - this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W + 0] * this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE];
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION + 1] = this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U + 1] * u + this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V + 1] * v - this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W + 1] * this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE];
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION + 2] = this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U + 2] * u + this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V + 2] * v - this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W + 2] * this.camera[Camera.ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE];
		
//		Normalize the ray direction vector:
		normalize(this.rays, rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION);
		
//		Calculate the distance to the closest shape, if any:
		final float distance = findIntersection(this.intersections, this.rays, this.shapes, this.shapeIndicesLength, this.shapeIndices);
		
//		Update the pixels with the RGB-values reset to black:
		clearPixel(this.pixels, pixelOffset);
		
		if(distance > 0.0F && distance < Constants.MAXIMUM_DISTANCE) {
//			Initialize needed offset values:
			final int intersectionOffset = index * Intersection.SIZE_OF_INTERSECTION;
			final int shapeOffset = (int)(this.intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET]);
			final int materialOffset = (int)(this.shapes[shapeOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_MATERIAL_OFFSET]);
			
//			Calculate the ambient and direct light:
			attemptToAddAmbientLight(this.materials, this.pixels, materialOffset, pixelOffset);
			attemptToAddDirectLight(this.intersections, this.lights, this.materials, this.pixels, this.shapes, intersectionOffset, this.lightsLength, materialOffset, pixelOffset, shapeOffset, this.textures);
		}
		
		if(index == pickIndex) {
			this.pick[0] = (this.intersections[index * Intersection.SIZE_OF_INTERSECTION + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET]);
			this.pick[1] = distance;
			
			this.pixels[pixelOffset + 0] = 1.0F;
			this.pixels[pixelOffset + 1] = 1.0F;
			this.pixels[pixelOffset + 2] = 1.0F;
		}
		
//		Update the pixel by performing gamma correction, tone mapping and scaling:
		updatePixel(this.pixels, pixelOffset, index, this.rGB);
	}
}