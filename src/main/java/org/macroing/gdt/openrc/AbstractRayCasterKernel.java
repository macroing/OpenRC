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
	public static final float RGB_RECIPROCAL = 1.0F / 255.0F;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	protected AbstractRayCasterKernel() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public float calculateShadeForPointLight(final boolean isUpdatingPick, final float[] intersections, final float[] lights, final float[] pick, final float[] rays, final float[] shapes, final int intersectionOffset, final int lightOffset, final int rayOffset, final int shapeIndicesLength, final int[] shapeIndices) {
//		Get the location of the point light:
		final float pointLightX = lights[lightOffset + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 0];
		final float pointLightY = lights[lightOffset + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 1];
		final float pointLightZ = lights[lightOffset + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 2];
		
//		Get the surface intersection point:
		final float surfaceIntersectionX = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 0];
		final float surfaceIntersectionY = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 1];
		final float surfaceIntersectionZ = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 2];
		
//		Calculate the direction from the surface intersection point of the shape to the location of the point light:
		float directionX = pointLightX - surfaceIntersectionX;
		float directionY = pointLightY - surfaceIntersectionY;
		float directionZ = pointLightZ - surfaceIntersectionZ;
		
//		Calculate the length reciprocal of the direction vector:
		float lengthReciprocal = 1.0F / sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
		
//		Multiply the direction with the reciprocal of the length to normalize it:
		directionX *= lengthReciprocal;
		directionY *= lengthReciprocal;
		directionZ *= lengthReciprocal;
		
//		Update the secondary ray with the origin and direction:
		rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN_1 + 0] = surfaceIntersectionX + directionX;
		rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN_1 + 1] = surfaceIntersectionY + directionY;
		rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN_1 + 2] = surfaceIntersectionZ + directionZ;
		rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION_1 + 0] = directionX;
		rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION_1 + 1] = directionY;
		rays[rayOffset + Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION_1 + 2] = directionZ;
		
//		Calculate the distance between the surface intersection point and the point light:
		final float deltaX = pointLightX - surfaceIntersectionX;
		final float deltaY = pointLightY - surfaceIntersectionY;
		final float deltaZ = pointLightZ - surfaceIntersectionZ;
		final float distance0 = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
		
//		Calculate the distance between the surface intersection point and the closest intersecting shape:
		final float distance1 = findIntersection(false, false, isUpdatingPick, intersections, pick, rays, shapes, shapeIndicesLength, shapeIndices);
		
//		Calculate the shade as 1.0 if, and only if, the distance between the surface intersection point and the point light is less than the distance between the surface intersection point and the closest intersecting shape, 0.0 otherwise:
		final float shade = distance0 < distance1 ? 1.0F : 0.0F;
		
		return shade;
	}
	
	public float findIntersection(final boolean isPrimaryIntersection, final boolean isUpdatingIntersection, final boolean isUpdatingPick, final float[] intersections, final float[] pick, final float[] rays, final float[] shapes, final int shapeIndicesLength, final int[] shapeIndices) {
//		Initialize the index and offset values:
		final int index = getGlobalId();
		final int intersectionOffset = index * Intersection.SIZE_OF_INTERSECTION;
		final int rayOffset = index * Constants.SIZE_OF_RAY;
		
//		Initialize offset to closest shape:
		int shapeClosestOffset = -1;
		
//		Initialize distance to closest shape:
		float shapeClosestDistance = Constants.MAXIMUM_DISTANCE;
		
		final int rayOriginOffset = isPrimaryIntersection ? Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN_0 : Constants.RELATIVE_OFFSET_OF_RAY_ORIGIN_1;
		final int rayDirectionOffset = isPrimaryIntersection ? Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION_0 : Constants.RELATIVE_OFFSET_OF_RAY_DIRECTION_1;
		
//		Initialize the ray values (origin and direction):
		final float rayOriginX = rays[rayOffset + rayOriginOffset + 0];
		final float rayOriginY = rays[rayOffset + rayOriginOffset + 1];
		final float rayOriginZ = rays[rayOffset + rayOriginOffset + 2];
		final float rayDirectionX = rays[rayOffset + rayDirectionOffset + 0];
		final float rayDirectionY = rays[rayOffset + rayDirectionOffset + 1];
		final float rayDirectionZ = rays[rayOffset + rayDirectionOffset + 2];
		
		if(isUpdatingIntersection) {
//			Reset the float array intersections, so we can perform a new intersection test:
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET] = -1.0F;
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE] = Constants.MAXIMUM_DISTANCE;
		}
		
		for(int i = 0, shapeOffset = shapeIndices[i]; i < shapeIndicesLength && shapeOffset >= 0; i++, shapeOffset = shapeIndices[min(i, shapeIndicesLength - 1)]) {
//			Initialize the temporary type and size variables of the current shape:
			final float shapeType = shapes[shapeOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE];
//			final float shapeSize = shapes[shapeOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_SIZE];
			
//			Initialize the shape distance to the maximum value:
			float shapeDistance = Constants.MAXIMUM_DISTANCE;
			
			if(shapeType == Plane.TYPE) {
//				Update the shape distance based on the intersected plane:
				shapeDistance = findIntersectionForPlane(rayOriginX, rayOriginY, rayOriginZ, rayDirectionX, rayDirectionY, rayDirectionZ, shapes, shapeOffset);
			}
			
			if(shapeType == Sphere.TYPE) {
//				Update the shape distance based on the intersected sphere:
				shapeDistance = findIntersectionForSphere(rayOriginX, rayOriginY, rayOriginZ, rayDirectionX, rayDirectionY, rayDirectionZ, shapes, shapeOffset);
			}
			
			if(shapeType == Triangle.TYPE) {
//				Update the shape distance based on the intersected triangle:
				shapeDistance = findIntersectionForTriangle(rayOriginX, rayOriginY, rayOriginZ, rayDirectionX, rayDirectionY, rayDirectionZ, shapes, shapeOffset);
			}
			
			if(shapeDistance > 0.0F && shapeDistance < shapeClosestDistance) {
//				Update the distance to and the offset of the closest shape:
				shapeClosestDistance = shapeDistance;
				shapeClosestOffset = shapeOffset;
			}
		}
		
		if(shapeClosestOffset > -1 && isUpdatingIntersection) {
//			Update the intersections array with values found:
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET] = shapeClosestOffset;
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE] = shapeClosestDistance;
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 0] = rayOriginX + rayDirectionX * shapeClosestDistance;
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 1] = rayOriginY + rayDirectionY * shapeClosestDistance;
			intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 2] = rayOriginZ + rayDirectionZ * shapeClosestDistance;
			
			if(shapes[shapeClosestOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE] == Plane.TYPE) {
//				Update the intersections array with the surface normal of the intersected plane:
				updateSurfaceNormalForPlane(intersections, shapes, intersectionOffset, shapeClosestOffset);
			}
			
			if(shapes[shapeClosestOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE] == Sphere.TYPE) {
//				Update the intersections array with the surface normal of the intersected sphere:
				updateSurfaceNormalForSphere(intersections, shapes, intersectionOffset, shapeClosestOffset);
			}
			
			if(shapes[shapeClosestOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE] == Triangle.TYPE) {
//				Update the intersections array with the surface normal of the intersected triangle:
				updateSurfaceNormalForTriangle(intersections, shapes, intersectionOffset, shapeClosestOffset);
			}
		}
		
		return shapeClosestDistance;
	}
	
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
	
	public void attemptToAddAmbientLight(final boolean isUpdatingPick, final float[] intersections, final float[] materials, final float[] pick, final float[] pixels, final float[] shapes, final int intersectionOffset, final int materialOffset, final int pixelOffset, final int shapeOffset, final int[] textures) {
//		Get the ambient intensity:
		final float ambientIntensity = materials[materialOffset + Material.RELATIVE_OFFSET_OF_AMBIENT_INTENSITY];
		
		if(ambientIntensity > 0.0F) {
//			Get the RGB-components for the ambient color:
			final float ambientColorR = materials[materialOffset + Material.RELATIVE_OFFSET_OF_AMBIENT_COLOR + 0];
			final float ambientColorG = materials[materialOffset + Material.RELATIVE_OFFSET_OF_AMBIENT_COLOR + 1];
			final float ambientColorB = materials[materialOffset + Material.RELATIVE_OFFSET_OF_AMBIENT_COLOR + 2];
			
//			Reset the temporary pixel buffer:
			pixels[pixelOffset + 3] = 0.0F;
			pixels[pixelOffset + 4] = 0.0F;
			pixels[pixelOffset + 5] = 0.0F;
			
//			Perform texture mapping on the shape:
			performTextureMapping(isUpdatingPick, intersections, materials, pick, pixels, shapes, intersectionOffset, materialOffset, pixelOffset, shapeOffset, textures);
			
//			Add the RGB-components of the ambient color multiplied by the ambient intensity, to the pixel:
			pixels[pixelOffset + 0] += (ambientColorR + pixels[pixelOffset + 3]) * ambientIntensity;
			pixels[pixelOffset + 1] += (ambientColorG + pixels[pixelOffset + 4]) * ambientIntensity;
			pixels[pixelOffset + 2] += (ambientColorB + pixels[pixelOffset + 5]) * ambientIntensity;
		}
	}
	
	public void attemptToAddDiffuseReflectionFromPointLight(final boolean isUpdatingPick, final float shade, final float[] intersections, final float[] lights, final float[] materials, final float[] pick, final float[] pixels, final float[] shapes, final int intersectionOffset, final int lightOffset, final int materialOffset, final int pixelOffset, final int shapeOffset, final int[] textures) {
//		Get the diffuse intensity:
		final float diffuseIntensity = materials[materialOffset + Material.RELATIVE_OFFSET_OF_DIFFUSE_INTENSITY];
		
		if(diffuseIntensity > 0.0F) {
//			Get the RGB-components for the diffuse color:
			final float diffuseColorR = materials[materialOffset + Material.RELATIVE_OFFSET_OF_DIFFUSE_COLOR + 0];
			final float diffuseColorG = materials[materialOffset + Material.RELATIVE_OFFSET_OF_DIFFUSE_COLOR + 1];
			final float diffuseColorB = materials[materialOffset + Material.RELATIVE_OFFSET_OF_DIFFUSE_COLOR + 2];
			
//			Get the location from the point light:
			final float pointLightX = lights[lightOffset + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 0];
			final float pointLightY = lights[lightOffset + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 1];
			final float pointLightZ = lights[lightOffset + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 2];
			
//			Get the surface intersection point:
			final float surfaceIntersectionX = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 0];
			final float surfaceIntersectionY = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 1];
			final float surfaceIntersectionZ = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 2];
			
//			Get the surface normal on the surface intersection point:
			final float surfaceNormalX = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 0];
			final float surfaceNormalY = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 1];
			final float surfaceNormalZ = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 2];
			
//			Calculate the direction from the surface intersection point of the shape and the location of the point light:
			float directionX = pointLightX - surfaceIntersectionX;
			float directionY = pointLightY - surfaceIntersectionY;
			float directionZ = pointLightZ - surfaceIntersectionZ;
			
//			Calculate the length reciprocal of the direction vector:
			final float lengthReciprocal = 1.0F / sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
			
//			Multiply the direction with the reciprocal of the length to normalize it:
			directionX *= lengthReciprocal;
			directionY *= lengthReciprocal;
			directionZ *= lengthReciprocal;
			
//			Calculate the dot product as the dot product of the direction and the surface normal, or 0.0 if less than that:
			final float dotProduct = directionX * surfaceNormalX + directionY * surfaceNormalY + directionZ * surfaceNormalZ;
			
			if(dotProduct > 0.0F) {
//				Calculate the diffuse component as the dot product multiplied with the diffuse intensity multiplied by the shade:
				final float diffuseComponent = dotProduct * diffuseIntensity * shade;
				
//				Reset the temporary pixel buffer:
				pixels[pixelOffset + 3] = 0.0F;
				pixels[pixelOffset + 4] = 0.0F;
				pixels[pixelOffset + 5] = 0.0F;
				
//				Perform texture mapping on the shape:
				performTextureMapping(isUpdatingPick, intersections, materials, pick, pixels, shapes, intersectionOffset, materialOffset, pixelOffset, shapeOffset, textures);
				
//				Add the RGB-components of the diffuse color multiplied by the diffuse component, to the pixel:
				pixels[pixelOffset + 0] += (diffuseColorR + pixels[pixelOffset + 3]) * diffuseComponent;
				pixels[pixelOffset + 1] += (diffuseColorG + pixels[pixelOffset + 4]) * diffuseComponent;
				pixels[pixelOffset + 2] += (diffuseColorB + pixels[pixelOffset + 5]) * diffuseComponent;
			}
		}
	}
	
	public void attemptToAddDirectLight(final boolean isUpdatingPick, final float[] intersections, final float[] lights, final float[] materials, final float[] pick, final float[] pixels, final float[] rays, final float[] shapes, final int intersectionOffset, final int lightsLength, final int materialOffset, final int pixelOffset, final int rayOffset, final int shapeIndicesLength, final int shapeOffset, final int[] shapeIndices, final int[] textures) {
		for(int i = 0, j = 0; i < lightsLength; i += j) {
//			Initialize the temporary type and size variables of the current light:
			final float lightType = lights[i + Light.RELATIVE_OFFSET_OF_LIGHT_TYPE];
			final float lightSize = lights[i + Light.RELATIVE_OFFSET_OF_LIGHT_SIZE];
			
//			Set the light size as increment for the next loop iteration:
			j = (int)(lightSize);
			
			if(lightType == PointLight.TYPE_POINT_LIGHT) {
				final float shade = calculateShadeForPointLight(isUpdatingPick, intersections, lights, pick, rays, shapes, intersectionOffset, i, rayOffset, shapeIndicesLength, shapeIndices);
				
				if(shade > 0.0F) {
					attemptToAddDiffuseReflectionFromPointLight(isUpdatingPick, shade, intersections, lights, materials, pick, pixels, shapes, intersectionOffset, i, materialOffset, pixelOffset, shapeOffset, textures);
					attemptToAddSpecularReflectionFromPointLight(shade, intersections, lights, materials, pixels, intersectionOffset, i, materialOffset, pixelOffset);
				}
			}
		}
	}
	
	public void attemptToAddSpecularReflectionFromPointLight(final float shade, final float[] intersections, final float[] lights, final float[] materials, final float[] pixels, final int intersectionOffset, final int lightOffset, final int materialOffset, final int pixelOffset) {
//		Get the specular intensity:
		final float specularIntensity = materials[materialOffset + Material.RELATIVE_OFFSET_OF_SPECULAR_INTENSITY];
		
		if(specularIntensity > 0.0F) {
//			Get the location from the point light:
			final float pointLightX = lights[lightOffset + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 0];
			final float pointLightY = lights[lightOffset + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 1];
			final float pointLightZ = lights[lightOffset + PointLight.RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION + 2];
			
//			Get the RGB-components for the specular color:
			final float specularColorR = materials[materialOffset + Material.RELATIVE_OFFSET_OF_SPECULAR_COLOR + 0];
			final float specularColorG = materials[materialOffset + Material.RELATIVE_OFFSET_OF_SPECULAR_COLOR + 1];
			final float specularColorB = materials[materialOffset + Material.RELATIVE_OFFSET_OF_SPECULAR_COLOR + 2];
			
//			Get the surface intersection point:
			final float surfaceIntersectionX = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 0];
			final float surfaceIntersectionY = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 1];
			final float surfaceIntersectionZ = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 2];
			
//			Get the surface normal on the surface intersection point:
			float surfaceNormalX = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 0];
			float surfaceNormalY = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 1];
			float surfaceNormalZ = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL + 2];
			
//			Calculate the direction from the surface intersection point of the shape to the location of the point light:
			float directionX = surfaceIntersectionX - pointLightX;
			float directionY = surfaceIntersectionY - pointLightY;
			float directionZ = surfaceIntersectionZ - pointLightZ;
			
//			Calculate the length reciprocal of the direction vector:
			float lengthReciprocal = 1.0F / sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
			
//			Multiply the direction with the reciprocal of the length to normalize it:
			directionX *= lengthReciprocal;
			directionY *= lengthReciprocal;
			directionZ *= lengthReciprocal;
			
//			Calculate the dot product as the negative dot product of the direction and the surface normal:
			float dotProduct = -(directionX * surfaceNormalX + directionY * surfaceNormalY + directionZ * surfaceNormalZ);
			
			if(dotProduct > 0.0F) {
//				Calculate the dot product multiplied by 2:
				final float dotProductMultipliedByTwo = dotProduct * 2.0F;
				
//				Multiply the surface normal with the dot product multiplied by 2:
				surfaceNormalX *= dotProductMultipliedByTwo;
				surfaceNormalY *= dotProductMultipliedByTwo;
				surfaceNormalZ *= dotProductMultipliedByTwo;
				
//				Add the direction to the surface normal:
				surfaceNormalX += directionX;
				surfaceNormalY += directionY;
				surfaceNormalZ += directionZ;
				
//				Calculate the length reciprocal of the surface normal vector:
				lengthReciprocal = 1.0F / sqrt(surfaceNormalX * surfaceNormalX + surfaceNormalY * surfaceNormalY + surfaceNormalZ * surfaceNormalZ);
				
//				Multiply the surface normal with the reciprocal of the length to normalize it:
				surfaceNormalX *= lengthReciprocal;
				surfaceNormalY *= lengthReciprocal;
				surfaceNormalZ *= lengthReciprocal;
				
//				Calculate the dot product as the negative dot product between the direction and the surface normal:
				dotProduct = -(directionX * surfaceNormalX + directionY * surfaceNormalY + directionZ * surfaceNormalZ);
				
//				Initialize some minimum and maximum values:
				final float minimumValue = 0.0F;
				final float maximumValue = max(dotProduct, minimumValue);
				
//				Get the specular power and calculate the specular component:
				final float specularPower = materials[materialOffset + Material.RELATIVE_OFFSET_OF_SPECULAR_POWER];
				final float specularComponent = pow(maximumValue, specularPower) * specularIntensity * shade;
				
//				Add the RGB-components of the specular color multiplied by the specular component, to the pixel:
				pixels[pixelOffset + 0] += specularColorR * specularComponent;
				pixels[pixelOffset + 1] += specularColorG * specularComponent;
				pixels[pixelOffset + 2] += specularColorB * specularComponent;
			}
		}
	}
	
	public void normalize(final float[] vector, final int offset) {
//		Calculate the reciprocal of the length of the vector:
		final float lengthReciprocal = 1.0F / length(vector, offset);
		
//		Multiply the vector with the reciprocal of the length to normalize it:
		vector[offset + 0] *= lengthReciprocal;
		vector[offset + 1] *= lengthReciprocal;
		vector[offset + 2] *= lengthReciprocal;
	}
	
	public void performPlanarTriangleTextureMapping(final boolean isUpdatingPick, final float[] intersections, final float[] materials, final float[] pick, final float[] pixels, final float[] shapes, final int intersectionOffset, final int materialOffset, final int pixelOffset, final int shapeOffset, final int textureOffset, final int[] textures) {
//		Initialize the variables with the position (the X-, Y- and Z-values) of the triangle:
		final float triangleAX = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_A + 0];
		final float triangleAY = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_A + 1];
		final float triangleAZ = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_A + 2];
		final float triangleBX = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_B + 0];
		final float triangleBY = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_B + 1];
		final float triangleBZ = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_B + 2];
		final float triangleCX = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_C + 0];
		final float triangleCY = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_C + 1];
		final float triangleCZ = shapes[shapeOffset + Triangle.RELATIVE_OFFSET_OF_TRIANGLE_C + 2];
		
//		Initialize the variables with the surface intersection point (the X-, Y- and Z-values) of the triangle:
		final float surfaceIntersectionX = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 0];
		final float surfaceIntersectionY = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 1];
		final float surfaceIntersectionZ = intersections[intersectionOffset + Intersection.RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT + 2];
		
		final float factorAX = triangleAX - surfaceIntersectionX;
		final float factorAY = triangleAY - surfaceIntersectionY;
		final float factorAZ = triangleAZ - surfaceIntersectionZ;
		final float factorBX = triangleBX - surfaceIntersectionX;
		final float factorBY = triangleBY - surfaceIntersectionY;
		final float factorBZ = triangleBZ - surfaceIntersectionZ;
		final float factorCX = triangleCX - surfaceIntersectionX;
		final float factorCY = triangleCY - surfaceIntersectionY;
		final float factorCZ = triangleCZ - surfaceIntersectionZ;
		
		final float factorALength = sqrt(factorAX * factorAX + factorAY * factorAY + factorAZ * factorAZ);
		final float factorBLength = sqrt(factorBX * factorBX + factorBY * factorBY + factorBZ * factorBZ);
		final float factorCLength = sqrt(factorCX * factorCX + factorCY * factorCY + factorCZ * factorCZ);
		
		final float deltaABX = triangleAX - triangleBX;
		final float deltaABY = triangleAY - triangleBY;
		final float deltaABZ = triangleAZ - triangleBZ;
		final float deltaACX = triangleAX - triangleCX;
		final float deltaACY = triangleAY - triangleCY;
		final float deltaACZ = triangleAZ - triangleCZ;
		
		final float crossProduct0X = deltaABY * deltaACZ - deltaABZ * deltaACY;
		final float crossProduct0Y = deltaABZ * deltaACX - deltaABX * deltaACZ;
		final float crossProduct0Z = deltaABX * deltaACY - deltaABY * deltaACX;
		
		final float lengthReciprocal0 = 1.0F / sqrt(crossProduct0X * crossProduct0X + crossProduct0Y * crossProduct0Y + crossProduct0Z * crossProduct0Z);
		
		final float crossProduct1X = factorBY * factorCZ - factorBZ * factorCY;
		final float crossProduct1Y = factorBZ * factorCX - factorBX * factorCZ;
		final float crossProduct1Z = factorBX * factorCY - factorBY * factorCX;
		
		final float length1 = sqrt(crossProduct1X * crossProduct1X + crossProduct1Y * crossProduct1Y + crossProduct1Z * crossProduct1Z) * lengthReciprocal0;
		
		final float crossProduct2X = factorCY * factorAZ - factorCZ * factorAY;
		final float crossProduct2Y = factorCZ * factorAX - factorCX * factorAZ;
		final float crossProduct2Z = factorCX * factorAY - factorCY * factorAX;
		
		final float length2 = sqrt(crossProduct2X * crossProduct2X + crossProduct2Y * crossProduct2Y + crossProduct2Z * crossProduct2Z) * lengthReciprocal0;
		
		final float crossProduct3X = factorAY * factorBZ - factorAZ * factorBY;
		final float crossProduct3Y = factorAZ * factorBX - factorAX * factorBZ;
		final float crossProduct3Z = factorAX * factorBY - factorAY * factorBX;
		
		final float length3 = sqrt(crossProduct3X * crossProduct3X + crossProduct3Y * crossProduct3Y + crossProduct3Z * crossProduct3Z) * lengthReciprocal0;
		
//		TODO: Fix these UV-coordinates, so they're not hard-coded:
		final float triangleAU = triangleAX * 0.001F;
		final float triangleAV = triangleAZ * 0.001F;
		final float triangleBU = triangleBX * 0.001F;
		final float triangleBV = triangleBZ * 0.001F;
		final float triangleCU = triangleCX * 0.001F;
		final float triangleCV = triangleCZ * 0.001F;
		
//		Calculate the UV-coordinates:
		final float textureU = triangleAU * length1 + triangleBU * length2 + triangleCU * length3;
		final float textureV = triangleAV * length1 + triangleBV * length2 + triangleCV * length3;
		
//		Initialize the width and height of the texture:
		final int textureWidth = textures[textureOffset + Texture.RELATIVE_OFFSET_OF_TEXTURE_WIDTH];
		final int textureHeight = textures[textureOffset + Texture.RELATIVE_OFFSET_OF_TEXTURE_HEIGHT];
		
//		Calculate the X- and Y-values of the texture to be applied to the triangle on the surface intersection point:
		final int textureX = (int)(IEEEremainder(textureU * factorALength + textureU * factorBLength + textureU * factorCLength, textureWidth));
		final int textureY = (int)(IEEEremainder(textureV * factorALength + textureV * factorBLength + textureV * factorCLength, textureHeight));
		
//		Calculate the index of the RGB-value and fetch the RGB-value using said index:
		final int textureIndex = textureY * textureWidth + textureX;
		final int textureRGB = textures[textureOffset + Texture.RELATIVE_OFFSET_OF_TEXTURE_DATA + (int)(IEEEremainder(abs(textureIndex), textureWidth * textureHeight))];
		
//		Calculate the R-, G- and B-components of the RGB-value:
		float r = toR(textureRGB) * RGB_RECIPROCAL;
		float g = toG(textureRGB) * RGB_RECIPROCAL;
		float b = toB(textureRGB) * RGB_RECIPROCAL;
		
		if(textures[textureOffset + Texture.RELATIVE_OFFSET_OF_TEXTURE_TYPE] == Texture.TYPE_DECAL_TEXTURE) {
//			Update the decal RGB-components:
			r = r < 0.5F ? 0.0F : ((r - 0.5F) * 2.0F);
			g = g < 0.5F ? 0.0F : ((g - 0.5F) * 2.0F);
			b = b < 0.5F ? 0.0F : ((b - 0.5F) * 2.0F);
			
			if(isUpdatingPick) {
				pick[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_OFFSET] = textureOffset;
				pick[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_UV + 0] = textureX;
				pick[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_UV + 1] = textureY;
			}
		}
		
//		Update the RGB-values of the pixels array:
		pixels[pixelOffset + 3] += r;
		pixels[pixelOffset + 4] += g;
		pixels[pixelOffset + 5] += b;
	}
	
	public void performSphericalTextureMapping(final boolean isUpdatingPick, final float[] intersections, final float[] materials, final float[] pick, final float[] pixels, final float[] shapes, final int intersectionOffset, final int materialOffset, final int pixelOffset, final int shapeOffset, final int textureOffset, final int[] textures) {
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
		
//		Initialize the width and height of the texture:
		final int textureWidth = textures[textureOffset + Texture.RELATIVE_OFFSET_OF_TEXTURE_WIDTH];
		final int textureHeight = textures[textureOffset + Texture.RELATIVE_OFFSET_OF_TEXTURE_HEIGHT];
		
//		Calculate the U- and V-values of the sphere on the surface intersection point:
		final float textureU = 0.5F + atan2(distanceX, distanceZ) / (2.0F * Constants.PI);
		final float textureV = 0.5F + asin(distanceY) / Constants.PI;
		
//		Calculate the X- and Y-values of the texture to be applied to the sphere on the surface intersection point:
		final int textureX = (int)(textureWidth * ((textureU + 1.0F) * 0.5F));
		final int textureY = (int)(textureHeight * ((textureV + 1.0F) * 0.5F));
		
//		Calculate the index of the RGB-value and fetch the RGB-value using said index:
		final int textureIndex = textureY * textureWidth + textureX;
		final int textureRGB = textures[textureOffset + Texture.RELATIVE_OFFSET_OF_TEXTURE_DATA + textureIndex];
		
//		Calculate the R-, G- and B-components of the RGB-value:
		float r = toR(textureRGB) * RGB_RECIPROCAL;
		float g = toG(textureRGB) * RGB_RECIPROCAL;
		float b = toB(textureRGB) * RGB_RECIPROCAL;
		
		if(textures[textureOffset + Texture.RELATIVE_OFFSET_OF_TEXTURE_TYPE] == Texture.TYPE_DECAL_TEXTURE) {
//			Update the decal RGB-components:
			r = r < 0.5F ? 0.0F : ((r - 0.5F) * 2.0F);
			g = g < 0.5F ? 0.0F : ((g - 0.5F) * 2.0F);
			b = b < 0.5F ? 0.0F : ((b - 0.5F) * 2.0F);
		}
		
//		Update the RGB-values of the pixels array:
		pixels[pixelOffset + 3] += r;
		pixels[pixelOffset + 4] += g;
		pixels[pixelOffset + 5] += b;
		
		if(isUpdatingPick) {
			pick[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_OFFSET] = textureOffset;
			pick[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_UV + 0] = textureX;
			pick[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_UV + 1] = textureY;
		}
	}
	
	public void performTextureMapping(final boolean isUpdatingPick, final float[] intersections, final float[] materials, final float[] pick, final float[] pixels, final float[] shapes, final int intersectionOffset, final int materialOffset, final int pixelOffset, final int shapeOffset, final int[] textures) {
//		Initialize the texture count:
		final int textureCount = (int)(materials[materialOffset + Material.RELATIVE_OFFSET_OF_TEXTURE_COUNT]);
		
		if(textureCount > 0) {
			if(shapes[shapeOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE] == Sphere.TYPE) {
				for(int i = 0; i < textureCount; i++) {
//					Initialize the texture offset:
					final int textureOffset = (int)(materials[materialOffset + Material.RELATIVE_OFFSET_OF_TEXTURE_COUNT + i + 1]);
					
//					Perform spherical texture mapping on the sphere:
					performSphericalTextureMapping(isUpdatingPick, intersections, materials, pick, pixels, shapes, intersectionOffset, materialOffset, pixelOffset, shapeOffset, textureOffset, textures);
				}
			}
			
			if(shapes[shapeOffset + Shape.RELATIVE_OFFSET_OF_SHAPE_TYPE] == Triangle.TYPE) {
				for(int i = 0; i < textureCount; i++) {
//					Initialize the texture offset:
					final int textureOffset = (int)(materials[materialOffset + Material.RELATIVE_OFFSET_OF_TEXTURE_COUNT + i + 1]);
					
//					Perform texture mapping on a triangle:
					performPlanarTriangleTextureMapping(isUpdatingPick, intersections, materials, pick, pixels, shapes, intersectionOffset, materialOffset, pixelOffset, shapeOffset, textureOffset, textures);
				}
			}
		}
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
	
	public void updatePixel(final float samples, final float[] pixels, final int pixelOffset, final int rGBOffset, final int[] rGB) {
//		Calculate the reciprocal of samples:
		final float samplesReciprocal = 1.0F / samples;
		
//		Get the RGB-components from the current pixel and multiply them with the reciprocal of samples:
		float r = pixels[pixelOffset + 0] * samplesReciprocal;
		float g = pixels[pixelOffset + 1] * samplesReciprocal;
		float b = pixels[pixelOffset + 2] * samplesReciprocal;
		
//		Calculate the maximum component value (used in Tone Mapping):
		final float maximumComponentValue = max(r, max(g, b));
		
		if(maximumComponentValue > 1.0F) {
//			Calculate the reciprocal of the maximum component value:
			final float maximumComponentValueReciprocal = 1.0F / maximumComponentValue;
			
//			Perform the Tone Mapping, by multiplying the RGB-components with the reciprocal of the maximum component value:
			r *= maximumComponentValueReciprocal;
			g *= maximumComponentValueReciprocal;
			b *= maximumComponentValueReciprocal;
		}
		
//		Scale the color from [0-1) to [0-256):
		final int scaledR = (int)(r * 255.0F);
		final int scaledG = (int)(g * 255.0F);
		final int scaledB = (int)(b * 255.0F);
		
//		Set the RGB-components of the current pixel as an int in the rGB-array:
		rGB[rGBOffset] = toRGB(scaledR, scaledG, scaledB);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static float dotProduct(final float[] vector0, final int offset0, final float[] vector1, final int offset1) {
		return vector0[offset0] * vector1[offset1] + vector0[offset0 + 1] * vector1[offset1 + 1] + vector0[offset0 + 2] * vector1[offset1 + 2];
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
	
	public static void clearPixel(final float[] pixels, final int pixelOffset) {
		pixels[pixelOffset + 0] = 0.0F;
		pixels[pixelOffset + 1] = 0.0F;
		pixels[pixelOffset + 2] = 0.0F;
		pixels[pixelOffset + 3] = 0.0F;
		pixels[pixelOffset + 4] = 0.0F;
		pixels[pixelOffset + 5] = 0.0F;
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