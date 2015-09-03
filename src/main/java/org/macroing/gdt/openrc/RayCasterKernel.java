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

import org.macroing.gdt.openrc.geometry.Camera;
import org.macroing.gdt.openrc.geometry.Intersection;
import org.macroing.gdt.openrc.geometry.Scene;
import org.macroing.gdt.openrc.geometry.Shape;

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
public final class RayCasterKernel extends AbstractRayCasterKernel {
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
		this.intersections = Intersection.create((Constants.WIDTH / Constants.WIDTH_SCALE) * (Constants.HEIGHT / Constants.HEIGHT_SCALE));
		this.lights = scene.getLightsAsArray();
		this.materials = scene.getMaterialsAsArray();
		this.pick = pick;
		this.pixels = new float[(Constants.WIDTH / Constants.WIDTH_SCALE) * (Constants.HEIGHT / Constants.HEIGHT_SCALE) * Constants.SIZE_OF_PIXEL];
		this.rays = new float[(Constants.WIDTH / Constants.WIDTH_SCALE) * (Constants.HEIGHT / Constants.HEIGHT_SCALE) * Constants.SIZE_OF_RAY];
		this.shapes = scene.getShapesAsArray();
		this.height = Constants.HEIGHT / Constants.HEIGHT_SCALE;
		this.lightsLength = this.lights.length;
		this.shapeIndicesLength = scene.getShapeCount();
		this.width = Constants.WIDTH / Constants.WIDTH_SCALE;
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
		
//		Initialize zoom factor and zoom factor reciprocal:
		final float zoom = this.camera[Camera.ABSOLUTE_OFFSET_OF_ZOOM];
		final float zoomReciprocal = 1.0F / zoom;
		
//		Initialize the pick update state:
		final boolean isUpdatingPick = index == pickIndex;
		
//		Update the pixels with the RGB-values reset to black:
		clearPixel(this.pixels, pixelOffset);
		
//		Update the origin point of the ray to fire:
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN_0 + 0] = this.camera[Camera.ABSOLUTE_OFFSET_OF_EYE + 0];
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN_0 + 1] = this.camera[Camera.ABSOLUTE_OFFSET_OF_EYE + 1];
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN_0 + 2] = this.camera[Camera.ABSOLUTE_OFFSET_OF_EYE + 2];
		
//		Initialize default pixel sample count:
		final float samples = 1.0F;
		
//		Initialize default pixel sample point:
		final float sampleX = 0.5F;
		final float sampleY = 0.5F;
		
//		Initialize the U- and V-coordinates:
		final float u = (index % this.width - this.width * 0.5F + sampleX) * zoomReciprocal;
		final float v = (index / this.width - this.height * 0.5F + sampleY) * zoomReciprocal;
		
//		Update the direction vector of the ray to fire:
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION_0 + 0] = this.camera[Camera.ABSOLUTE_OFFSET_OF_ORTHONORMAL_BASIS_U + 0] * u + this.camera[Camera.ABSOLUTE_OFFSET_OF_ORTHONORMAL_BASIS_V + 0] * v - this.camera[Camera.ABSOLUTE_OFFSET_OF_ORTHONORMAL_BASIS_W + 0] * this.camera[Camera.ABSOLUTE_OFFSET_OF_VIEW_PLANE_DISTANCE];
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION_0 + 1] = this.camera[Camera.ABSOLUTE_OFFSET_OF_ORTHONORMAL_BASIS_U + 1] * u + this.camera[Camera.ABSOLUTE_OFFSET_OF_ORTHONORMAL_BASIS_V + 1] * v - this.camera[Camera.ABSOLUTE_OFFSET_OF_ORTHONORMAL_BASIS_W + 1] * this.camera[Camera.ABSOLUTE_OFFSET_OF_VIEW_PLANE_DISTANCE];
		this.rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION_0 + 2] = this.camera[Camera.ABSOLUTE_OFFSET_OF_ORTHONORMAL_BASIS_U + 2] * u + this.camera[Camera.ABSOLUTE_OFFSET_OF_ORTHONORMAL_BASIS_V + 2] * v - this.camera[Camera.ABSOLUTE_OFFSET_OF_ORTHONORMAL_BASIS_W + 2] * this.camera[Camera.ABSOLUTE_OFFSET_OF_VIEW_PLANE_DISTANCE];
		
//		Normalize the ray direction vector:
		normalize(this.rays, rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION_0);
		
//		Calculate the distance to the closest shape, if any:
		final float distance = findIntersection(true, true, isUpdatingPick, this.intersections, this.pick, this.rays, this.shapes, this.shapeIndicesLength, this.shapeIndices);
		
		if(distance > 0.0F && distance < Constants.MAXIMUM_DISTANCE) {
//			Initialize needed offset values:
			final int intersectionOffset = index * Intersection.SIZE;
			final int shapeOffset = (int)(this.intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_SHAPE_OFFSET]);
			final int materialOffset = (int)(this.shapes[shapeOffset + Shape.RELATIVE_OFFSET_OF_MATERIAL_OFFSET]);
			
//			Calculate the ambient and direct light:
			attemptToAddAmbientLight(isUpdatingPick, this.intersections, this.materials, this.pick, this.pixels, this.shapes, intersectionOffset, materialOffset, pixelOffset, shapeOffset, this.textures);
			attemptToAddDirectLight(isUpdatingPick, this.intersections, this.lights, this.materials, this.pick, this.pixels, this.rays, this.shapes, intersectionOffset, this.lightsLength, materialOffset, pixelOffset, rayOffset, this.shapeIndicesLength, shapeOffset, this.shapeIndices, this.textures);
		}
		
		if(isUpdatingPick) {
			this.pick[0] = (this.intersections[index * Intersection.SIZE + Intersection.RELATIVE_OFFSET_OF_SHAPE_OFFSET]);
			this.pick[1] = distance;
			
//			Uncomment the following code to show a white pixel at the 'center' of the screen, where the pick is used:
//			this.pixels[pixelOffset + 0] = 1.0F;
//			this.pixels[pixelOffset + 1] = 1.0F;
//			this.pixels[pixelOffset + 2] = 1.0F;
		}
		
//		Update the pixel by performing gamma correction, tone mapping and scaling:
		updatePixel(samples, this.pixels, pixelOffset, index, this.rGB);
	}
}