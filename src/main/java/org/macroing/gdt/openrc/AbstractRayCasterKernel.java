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

import com.amd.aparapi.Kernel;

abstract class AbstractRayCasterKernel extends Kernel {
	protected AbstractRayCasterKernel() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public float findIntersectionForPlane(final float rayOriginX, final float rayOriginY, final float rayOriginZ, final float rayDirectionX, final float rayDirectionY, final float rayDirectionZ, final float[] shapes, final int shapeOffset) {
//		Initialize a variable with the plane constant:
		final float planeConstant = -2.0F;
		
//		Initialize the temporary X-, Y- and Z- variables of the current plane:
		final float planeSurfaceNormalX = shapes[shapeOffset + Plane.RELATIVE_OFFSET_OF_PLANE_SURFACE_NORMAL + 0];
		final float planeSurfaceNormalY = shapes[shapeOffset + Plane.RELATIVE_OFFSET_OF_PLANE_SURFACE_NORMAL + 1];
		final float planeSurfaceNormalZ = shapes[shapeOffset + Plane.RELATIVE_OFFSET_OF_PLANE_SURFACE_NORMAL + 2];
		
//		Calculate the dot product and its absolute version:
		final float dotProduct = rayDirectionX * planeSurfaceNormalX + rayDirectionY * planeSurfaceNormalY + rayDirectionZ * planeSurfaceNormalZ;
		final float dotProductAbsolute = abs(dotProduct);
		
//		Initialize the shape distance variable:
		float shapeDistance = 0.0F;
		
		if(dotProductAbsolute >= Constants.EPSILON) {
//			Calculate the distance:
			shapeDistance = (planeConstant - (rayOriginX * planeSurfaceNormalX + rayOriginY * planeSurfaceNormalY + rayOriginZ * planeSurfaceNormalZ)) / dotProduct;
		}
		
		return shapeDistance;
	}
	
	public float findIntersectionForSphere(final float rayOriginX, final float rayOriginY, final float rayOriginZ, final float rayDirectionX, final float rayDirectionY, final float rayDirectionZ, final float[] shapes, final int shapeOffset) {
//		Initialize the temporary X-, Y-, Z- and radius variables of the current sphere:
		final float sphereX = shapes[shapeOffset + Sphere.RELATIVE_OFFSET_OF_SPHERE_POSITION + 0];
		final float sphereY = shapes[shapeOffset + Sphere.RELATIVE_OFFSET_OF_SPHERE_POSITION + 1];
		final float sphereZ = shapes[shapeOffset + Sphere.RELATIVE_OFFSET_OF_SPHERE_POSITION + 2];
		final float sphereRadius = shapes[shapeOffset + Sphere.RELATIVE_OFFSET_OF_SPHERE_RADIUS];
		
//		Calculate the delta values between the current sphere and the origin of the camera:
		final float dx = sphereX - rayOriginX;
		final float dy = sphereY - rayOriginY;
		final float dz = sphereZ - rayOriginZ;
		
//		Calculate the dot product:
		final float b = dx * rayDirectionX + dy * rayDirectionY + dz * rayDirectionZ;
		
//		Calculate the discriminant:
		float discriminant = b * b - (dx * dx + dy * dy + dz * dz) + sphereRadius * sphereRadius;
		
//		Initialize the shape distance variable:
		float shapeDistance = 0.0F;
		
		if(discriminant >= 0.0F) {
//			Recalculate the discriminant:
			discriminant = sqrt(discriminant);
			
//			Calculate the distance:
			shapeDistance = b - discriminant;
			
			if(shapeDistance <= Constants.EPSILON) {
//				Recalculate the distance:
				shapeDistance = b + discriminant;
				
				if(shapeDistance <= Constants.EPSILON) {
//					We're too close to the shape, so we practically do not see it:
					shapeDistance = 0.0F;
				}
			}
		}
		
		return shapeDistance;
	}
	
	public float length(final float[] vector, final int offset) {
		return sqrt(lengthSquared(vector, offset));
	}
	
	public void normalize(final float[] vector, final int offset) {
		final float lengthReciprocal = 1.0F / length(vector, offset);
		
		vector[offset + 0] *= lengthReciprocal;
		vector[offset + 1] *= lengthReciprocal;
		vector[offset + 2] *= lengthReciprocal;
	}
	
	public void performSphericalTextureMapping(final float[] intersections, final float[] pixels, final float[] shapes, final int intersectionOffset, final int pixelOffset, final int shapeOffset, final int textureWidth, final int textureHeight, final int[] texture) {
//		Initialize the variables with the position (the X-, Y- and Z-values) of the sphere:
		final float sphereX = shapes[shapeOffset + Sphere.RELATIVE_OFFSET_OF_SPHERE_POSITION + 0];
		final float sphereY = shapes[shapeOffset + Sphere.RELATIVE_OFFSET_OF_SPHERE_POSITION + 1];
		final float sphereZ = shapes[shapeOffset + Sphere.RELATIVE_OFFSET_OF_SPHERE_POSITION + 2];
		
//		Initialize the variables with the surface intersection point (the X-, Y- and Z-values) of the sphere:
		final float surfaceIntersectionX = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 0];
		final float surfaceIntersectionY = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 1];
		final float surfaceIntersectionZ = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 2];
		
//		Calculate the delta values between the position and the surface intersection point of the sphere:
		final float dx = sphereX - surfaceIntersectionX;
		final float dy = sphereY - surfaceIntersectionY;
		final float dz = sphereZ - surfaceIntersectionZ;
		
//		Calculate the length reciprocal of the delta values:
		final float lengthReciprocal = 1.0F / sqrt(dx * dx + dy * dy + dz * dz);
		
//		Calculate the distance values by multiplying the delta values with the reciprocal of the length (normalize):
		final float distanceX = dx * lengthReciprocal;
		final float distanceY = dy * lengthReciprocal;
		final float distanceZ = dz * lengthReciprocal;
		
//		Calculate the U- and V-values of the sphere on the surface intersection point:
		final float textureU = 0.5F + atan2(distanceX, distanceZ) / (2.0F * Constants.PI);
		final float textureV = 0.5F + asin(distanceY) / Constants.PI;
		
//		Calculate the X- and Y-values of the texture to be applied to the sphere on the surface intersection point:
		final int textureX = (int)(textureWidth * ((textureU + 1.0F) * 0.5F));
		final int textureY = (int)(textureHeight * ((textureV + 1.0F) * 0.5F));
		
//		Calculate the index of the RGB-value and fetch the RGB-value using said index:
		final int textureIndex = textureY * textureWidth + textureX;
		final int textureRGB = texture[textureIndex];
		
//		Update the RGB-values of the pixels array:
		pixels[pixelOffset + 0] = toR(textureRGB);
		pixels[pixelOffset + 1] = toG(textureRGB);
		pixels[pixelOffset + 2] = toB(textureRGB);
	}
	
	public void updateSurfaceNormalForSphere(final float[] intersections, final float[] shapes, final int intersectionOffset, final int shapeOffset) {
//		Initialize variables with the position of the sphere:
		final float sphereX = shapes[shapeOffset + Sphere.RELATIVE_OFFSET_OF_SPHERE_POSITION + 0];
		final float sphereY = shapes[shapeOffset + Sphere.RELATIVE_OFFSET_OF_SPHERE_POSITION + 1];
		final float sphereZ = shapes[shapeOffset + Sphere.RELATIVE_OFFSET_OF_SPHERE_POSITION + 2];
		
//		Initialize variables with the delta values between the surface intersection point and the center of the sphere:
		final float dx = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 0] - sphereX;
		final float dy = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 1] - sphereY;
		final float dz = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 2] - sphereZ;
		
//		Calculate the length of the delta vector:
		final float length = sqrt(dx * dx + dy * dy + dz * dz);
		
//		Initialize variables with the surface normal vector:
		float surfaceNormalX = 0.0F;
		float surfaceNormalY = 0.0F;
		float surfaceNormalZ = 0.0F;
		
		if(length > 0.0F) {
//			//Calculate the length reciprocal:
			final float lengthReciprocal = 1.0F / length;
			
//			Set the surface normal vector to the delta vector multiplied with the length reciprocal:
			surfaceNormalX = dx * lengthReciprocal;
			surfaceNormalY = dy * lengthReciprocal;
			surfaceNormalZ = dz * lengthReciprocal;
		}
		
//		Update the intersections array with the surface normal vector:
		intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 0] = surfaceNormalX;
		intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 1] = surfaceNormalY;
		intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 2] = surfaceNormalZ;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static float dotProduct(final float[] vector0, final int offset0, final float[] vector1, final int offset1) {
		return vector0[offset0] * vector1[offset1] + vector0[offset0 + 1] * vector1[offset1 + 1] + vector0[offset0 + 2] * vector1[offset1 + 2];
	}
	
	public float findIntersection(final float[] intersections, final float[] rays, final float[] shapes, final int shapeIndicesLength, final int[] shapeIndices) {
//		Initialize the index and offset values:
		final int index = getGlobalId();
		final int intersectionOffset = index * Intersection.SIZE_OF_INTERSECTION;
		final int rayOffset = index * Constants.SIZE_OF_RAY;
		
//		Initialize offset to closest shape:
		int shapeClosestOffset = -1;
		
//		Initialize distance to closest shape:
		float shapeClosestDistance = Constants.MAXIMUM_DISTANCE;
		
//		Initialize the ray values (origin and direction):
		final float rayOriginX = rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN + 0];
		final float rayOriginY = rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN + 1];
		final float rayOriginZ = rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN + 2];
		final float rayDirectionX = rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION + 0];
		final float rayDirectionY = rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION + 1];
		final float rayDirectionZ = rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION + 2];
		
//		Reset the float array intersections, so we can perform a new intersection test:
		intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET] = -1.0F;
		intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE] = Constants.MAXIMUM_DISTANCE;
		
		for(int i = 0, shapeOffset = shapeIndices[i]; i < shapeIndicesLength && shapeOffset >= 0; i++, shapeOffset = shapeIndices[min(i, shapeIndicesLength - 1)]) {
//			Initialize the temporary type and size variables of the current shape:
			final float shapeType = shapes[shapeOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE];
//			final float shapeSize = shapes[shapeOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_SIZE];
			
//			Initialize the shape distance to the maximum value:
			float shapeDistance = Constants.MAXIMUM_DISTANCE;
			
			if(shapeType == Plane.TYPE_PLANE) {
//				Update the shape distance based on the intersected plane:
				shapeDistance = findIntersectionForPlane(rayOriginX, rayOriginY, rayOriginZ, rayDirectionX, rayDirectionY, rayDirectionZ, shapes, shapeOffset);
			}
			
			if(shapeType == Sphere.TYPE_SPHERE) {
//				Update the shape distance based on the intersected sphere:
				shapeDistance = findIntersectionForSphere(rayOriginX, rayOriginY, rayOriginZ, rayDirectionX, rayDirectionY, rayDirectionZ, shapes, shapeOffset);
			}
			
			if(shapeType == Triangle.TYPE_TRIANGLE) {
//				Update the shape distance based on the intersected triangle:
				shapeDistance = findIntersectionForTriangle(rayOriginX, rayOriginY, rayOriginZ, rayDirectionX, rayDirectionY, rayDirectionZ, shapes, shapeOffset);
			}
			
			if(shapeDistance > 0.0F && shapeDistance < shapeClosestDistance) {
//				Update the distance to and the offset of the closest shape:
				shapeClosestDistance = shapeDistance;
				shapeClosestOffset = shapeOffset;
			}
		}
		
		if(shapeClosestOffset > -1) {
//			Update the intersections array with values found:
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET] = shapeClosestOffset;
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE] = shapeClosestDistance;
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 0] = rayOriginX + rayDirectionX * shapeClosestDistance;
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 1] = rayOriginY + rayDirectionY * shapeClosestDistance;
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 2] = rayOriginZ + rayDirectionZ * shapeClosestDistance;
			
			if(shapes[shapeClosestOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE] == Plane.TYPE_PLANE) {
//				Update the intersections array with the surface normal of the intersected plane:
				updateSurfaceNormalForPlane(intersections, shapes, intersectionOffset, shapeClosestOffset);
			}
			
			if(shapes[shapeClosestOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE] == Sphere.TYPE_SPHERE) {
//				Update the intersections array with the surface normal of the intersected sphere:
				updateSurfaceNormalForSphere(intersections, shapes, intersectionOffset, shapeClosestOffset);
			}
			
			if(shapes[shapeClosestOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE] == Triangle.TYPE_TRIANGLE) {
//				Update the intersections array with the surface normal of the intersected triangle:
				updateSurfaceNormalForTriangle(intersections, shapes, intersectionOffset, shapeClosestOffset);
			}
		}
		
		return shapeClosestDistance;
	}
	
	public static float findIntersectionForTriangle(final float rayOriginX, final float rayOriginY, final float rayOriginZ, final float rayDirectionX, final float rayDirectionY, final float rayDirectionZ, final float[] shapes, final int shapeOffset) {
//		Initialize the shape distance variable to be returned:
		float shapeDistance = 0.0F;
		
//		Initialize the X-, Y- and Z-values of the A point of the triangle:
		final float triangleAX = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_A + 0];
		final float triangleAY = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_A + 1];
		final float triangleAZ = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_A + 2];
		
//		Initialize the X-, Y- and Z-values of the B point of the triangle:
		final float triangleBX = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_B + 0];
		final float triangleBY = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_B + 1];
		final float triangleBZ = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_B + 2];
		
//		Initialize the X-, Y- and Z-values of the C point of the triangle:
		final float triangleCX = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_C + 0];
		final float triangleCY = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_C + 1];
		final float triangleCZ = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_C + 2];
		
//		Calculate the X-, Y- and Z-values of the first edge of the triangle:
		final float edge0X = triangleBX - triangleAX;
		final float edge0Y = triangleBY - triangleAY;
		final float edge0Z = triangleBZ - triangleAZ;
		
//		Calculate the X-, Y- and Z-values of the second edge of the triangle:
		final float edge1X = triangleCX - triangleAX;
		final float edge1Y = triangleCY - triangleAY;
		final float edge1Z = triangleCZ - triangleAZ;
		
//		Calculate the cross product between the ray direction and the second edge:
		final float pX = rayDirectionY * edge1Z - rayDirectionZ * edge1Y;
		final float pY = rayDirectionZ * edge1X - rayDirectionX * edge1Z;
		final float pZ = rayDirectionX * edge1Y - rayDirectionY * edge1X;
		
//		Calculate the determinant:
		final float determinant = edge0X * pX + edge0Y * pY + edge0Z * pZ;
		
		if(determinant != 0.0F) {
//			Calculate the reciprocal of the determinant:
			final float determinantReciprocal = 1.0F / determinant;
			
//			Calculate the direction between the ray origin and the triangle A point:
			final float vX = rayOriginX - triangleAX;
			final float vY = rayOriginY - triangleAY;
			final float vZ = rayOriginZ - triangleAZ;
			
//			Calculate the dot product and multiply it with the reciprocal of the determinant:
			final float u = (vX * pX + vY * pY + vZ * pZ) * determinantReciprocal;
			
			if(u >= 0.0F && u <= 1.0F) {
//				Calculate the cross product between the previously calculated direction (between the ray origin and the triangle A point) and the first edge:
				final float qX = vY * edge0Z - vZ * edge0Y;
				final float qY = vZ * edge0X - vX * edge0Z;
				final float qZ = vX * edge0Y - vY * edge0X;
				
//				Calculate the dot product and multiply it with the reciprocal of the determinant:
				final float v = (rayDirectionX * qX + rayDirectionY * qY + rayDirectionZ * qZ) * determinantReciprocal;
				
				if(v >= 0.0F && u + v <= 1.0F) {
//					Update the shape distance as the dot product multiplied with the reciprocal of the determinant:
					shapeDistance = (edge1X * qX + edge1Y * qY + edge1Z * qZ) * determinantReciprocal;
				}
			}
		}
		
		return shapeDistance;
	}
	
	public static float lengthSquared(final float[] vector, final int offset) {
		return dotProduct(vector, offset, vector, offset);
	}
	
	public static int toB(final int rGB) {
		return (rGB >> 0) & 0xFF;
	}
	
	public static int toG(final int rGB) {
		return (rGB >> 8) & 0xFF;
	}
	
	public static int toR(final int rGB) {
		return (rGB >> 16) & 0xFF;
	}
	
	public static int toRGB(final int r, final int g, final int b) {
		return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
	}
	
	public static void updateSurfaceNormalForPlane(final float[] intersections, final float[] shapes, final int intersectionOffset, final int shapeOffset) {
//		Initialize variables with the surface normal vector:
		final float surfaceNormalX = shapes[shapeOffset + Plane.RELATIVE_OFFSET_OF_PLANE_SURFACE_NORMAL + 0];
		final float surfaceNormalY = shapes[shapeOffset + Plane.RELATIVE_OFFSET_OF_PLANE_SURFACE_NORMAL + 1];
		final float surfaceNormalZ = shapes[shapeOffset + Plane.RELATIVE_OFFSET_OF_PLANE_SURFACE_NORMAL + 2];
		
//		Update the intersections array with the surface normal vector:
		intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 0] = surfaceNormalX;
		intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 1] = surfaceNormalY;
		intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 2] = surfaceNormalZ;
	}
	
	public static void updateSurfaceNormalForTriangle(final float[] intersections, final float[] shapes, final int intersectionOffset, final int shapeOffset) {
//		Initialize variables with the surface normal vector:
		final float surfaceNormalX = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_SURFACE_NORMAL + 0];
		final float surfaceNormalY = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_SURFACE_NORMAL + 1];
		final float surfaceNormalZ = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_SURFACE_NORMAL + 2];
		
//		Update the intersections array with the surface normal vector:
		intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 0] = surfaceNormalX;
		intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 1] = surfaceNormalY;
		intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 2] = surfaceNormalZ;
	}
}