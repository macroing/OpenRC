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
	private final float[] pixels;
	private final float[] rays;
	private final float[] shapes;
	private final int height;
	private final int lightsLength;
	private final int shapeIndicesLength;
	private final int textureHeight;
	private final int textureWidth;
	private final int width;
	private final int[] rGB;
	private final int[] shapeIndices;
	private final int[] texture;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public RayCasterKernel(final int[] rGB, final Scene scene) {
		this.camera = scene.getCamera().getArray();
		this.intersections = Intersection.create(Constants.WIDTH * Constants.HEIGHT);
		this.lights = scene.getLightsAsArray();
		this.pixels = new float[Constants.WIDTH * Constants.HEIGHT * Constants.SIZE_OF_PIXEL];
		this.rays = new float[Constants.WIDTH * Constants.HEIGHT * Constants.SIZE_OF_RAY];
		this.shapes = scene.getShapesAsArray();
		this.height = Constants.HEIGHT;
		this.lightsLength = this.lights.length;
		this.shapeIndicesLength = scene.getShapeCount();
		this.textureHeight = scene.getTexture().getHeight();
		this.textureWidth = scene.getTexture().getWidth();
		this.width = Constants.WIDTH;
		this.rGB = rGB;
		this.shapeIndices = scene.getShapeIndices();
		this.texture = scene.getTexture().getData();
		
//		Make the Kernel instance explicit, such that we have to take care of all array transfers to and from the GPU:
		setExplicit(true);
		
//		Tell the API to fetch the below arrays and their values before executing this Kernel instance (they will be transferred to the GPU):
		put(this.intersections);
		put(this.lights);
		put(this.rays);
		put(this.shapes);
		put(this.rGB);
		put(this.texture);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * This is what the {@code Kernel} executes on the GPU (or in the CPU).
	 */
	@Override
	public void run() {
//		Initialize index and offset values:
		final int index = getGlobalId();
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
		
//		Initialize the RGB-values of the current pixel to black:
		int r = 0;
		int g = 0;
		int b = 0;
		
		this.pixels[pixelOffset + 0] = r;
		this.pixels[pixelOffset + 1] = g;
		this.pixels[pixelOffset + 2] = b;
		
		if(distance > 0.0F && distance < Constants.MAXIMUM_DISTANCE) {
//			Initialize needed offset values:
			final int intersectionOffset = index * Intersection.SIZE_OF_INTERSECTION;
			final int shapeOffset = (int)(this.intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET]);
			
//			Calculate the shading for the intersected shape at the surface intersection point:
			final float shading = doCalculateShading(intersectionOffset, pixelOffset, shapeOffset);
			
			if(this.shapes[shapeOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE] == Plane.TYPE_PLANE) {
//				A temporary way to give the plane some color:
				this.pixels[pixelOffset + 0] += 255.0F;
				this.pixels[pixelOffset + 1] += 255.0F;
				this.pixels[pixelOffset + 2] += 0.0F;
			}
			
			if(this.shapes[shapeOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE] == Sphere.TYPE_SPHERE) {
//				Perform spherical texture mapping on the sphere:
				performSphericalTextureMapping(this.intersections, this.pixels, this.shapes, intersectionOffset, pixelOffset, shapeOffset, this.textureWidth, this.textureHeight, this.texture);
			}
			
			if(this.shapes[shapeOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE] == Triangle.TYPE_TRIANGLE) {
//				A temporary way to give the triangle some color:
				this.pixels[pixelOffset + 0] += 0.0F;
				this.pixels[pixelOffset + 1] += 255.0F;
				this.pixels[pixelOffset + 2] += 0.0F;
			}
			
//			Update the RGB-values of the current pixel, given the RGB-values of the intersected shape:
			r = (int)(this.pixels[pixelOffset + 0] * shading);
			g = (int)(this.pixels[pixelOffset + 1] * shading);
			b = (int)(this.pixels[pixelOffset + 2] * shading);
		}
		
//		Set the RGB-value of the current pixel:
		this.rGB[index] = toRGB(r, g, b);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@SuppressWarnings("unused")
	private float doCalculateShading(final int intersectionOffset, final int pixelOffset, final int shapeOffset) {
//		Initialize the shading value:
		float shading = 0.0F;
		
//		Initialize values from the intersection:
		final float surfaceIntersectionX = this.intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 0];
		final float surfaceIntersectionY = this.intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 1];
		final float surfaceIntersectionZ = this.intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 2];
		final float surfaceNormalX = this.intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 0];
		final float surfaceNormalY = this.intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 1];
		final float surfaceNormalZ = this.intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 2];
		
		for(int i = 0, j = 0; i < this.lightsLength; i += j) {
//			Initialize the temporary type and size variables of the current light:
			final float lightType = this.lights[i + Light.RELATIVE_OFFSET_OF_LIGHT_TYPE];
			final float lightSize = this.lights[i + Light.RELATIVE_OFFSET_OF_LIGHT_SIZE];
			
//			Set the light size as increment for the next loop iteration:
			j = (int)(lightSize);
			
			if(lightType == PointLight.TYPE_POINT_LIGHT) {
//				Initialize the temporary X-, Y-, Z- and distance falloff variables of the current point light:
				final float pointLightX = this.lights[i + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 0];
				final float pointLightY = this.lights[i + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 1];
				final float pointLightZ = this.lights[i + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 2];
				final float pointLightDistanceFalloff = this.lights[i + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_DISTANCE_FALLOFF];
				
//				Calculate the delta values between the current light and the surface intersection point of the shape:
				float dx = pointLightX - surfaceIntersectionX;
				float dy = pointLightY - surfaceIntersectionY;
				float dz = pointLightZ - surfaceIntersectionZ;
				
//				Calculate the length reciprocal of the vector produced by the delta values:
				final float lengthReciprocal = 1.0F / sqrt(dx * dx + dy * dy + dz * dz);
				
//				Multiply the delta values with the length reciprocal to normalize it:
				dx *= lengthReciprocal;
				dy *= lengthReciprocal;
				dz *= lengthReciprocal;
				
				final float dotProduct = dx * surfaceNormalX + dy * surfaceNormalY + dz * surfaceNormalZ;
				final float dotProductNegative = -dotProduct;
				
//				Calculate the shading as the maximum value of 0.0 and the dot product of the delta vector and the surface normal vector, then add it to the shading variable:
				shading += dotProduct;
				
//				TODO: Fix this specular calculation:
				if(dotProductNegative > 0.0F) {
					final float dotProductNegativeTimes2 = dotProductNegative * 2.0F;
					
					float surfaceNormal0X = surfaceNormalX * dotProductNegativeTimes2 + dx;
					float surfaceNormal0Y = surfaceNormalY * dotProductNegativeTimes2 + dy;
					float surfaceNormal0Z = surfaceNormalZ * dotProductNegativeTimes2 + dz;
					
					final float surfaceNormal0LengthReciprocal = 1.0F / sqrt(surfaceNormal0X * surfaceNormal0X + surfaceNormal0Y * surfaceNormal0Y + surfaceNormal0Z * surfaceNormal0Z);
					
					surfaceNormal0X *= surfaceNormal0LengthReciprocal;
					surfaceNormal0Y *= surfaceNormal0LengthReciprocal;
					surfaceNormal0Z *= surfaceNormal0LengthReciprocal;
					
					final float negativeDotProduct = -(dx * surfaceNormal0X + dy * surfaceNormal0Y + dz * surfaceNormal0Z);
					final float minimumValue = 0.0F;
					final float maximumValue = max(negativeDotProduct, minimumValue);
					final float specularPower = 32.0F;
					final float specular = pow(maximumValue, specularPower);
					
					this.pixels[pixelOffset + 0] += 1.0F * specular;
					this.pixels[pixelOffset + 1] += 1.0F * specular;
					this.pixels[pixelOffset + 2] += 1.0F * specular;
				}
			}
		}
		
//		Set the ambient shading value to use as a minimum:
		final float ambientShading = 0.1F;
		
//		Update the shading variable to be between ambientShading and 1.0:
		shading = max(min(shading, 1.0F), ambientShading);
		
		return shading;
	}
}