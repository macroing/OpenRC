/**
 * Copyright 2009 - 2015 J&#246;rgen Lundgren
 * 
 * This file is part of org.macroing.gdt.opencl.
 * 
 * org.macroing.gdt.opencl is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * org.macroing.gdt.opencl is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with org.macroing.gdt.opencl. If not, see <http://www.gnu.org/licenses/>.
 */
package org.macroing.gdt.opencl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

/**
 * An OpenCL-based Ray Caster using Aparapi.
 * <p>
 * The values in the {@code float} array {@code cameraValues} consists of Eye X, Eye Y, Eye Z, Up X, Up Y, Up Z, Look-at X, Look-at Y, Look-at Z, ONB-U X, ONB-U Y, ONB-U Z, ONB-V X, ONB-V Y, ONB-V Z, ONB-W X, ONB-W Y, ONB-W Z and View-plane distance.
 * <p>
 * If you don't know what ONB stands for, then it is OrthoNormal Basis.
 * <p>
 * The values in the {@code float} array {@code intersections} consists of Shape Offset and Distance (T), for each shape currently being intersected by a ray.
 * <p>
 * The values in the {@code float} array {@code lights} consists of Type, Size and {@code float}[Size], for each light defined.
 * <p>
 * The values in the {@code float} array {@code rays} consists of Origin X, Origin Y, Origin Z, Direction X, Direction Y and Direction Z, for each ray fired from each pixel.
 * <p>
 * The values in the {@code float} array {@code shapes} consists of Type, Size and {@code float}[Size], for each shape defined.
 * <p>
 * The controls currently defined are the following:
 * <ul>
 * <li>A - Move left.</li>
 * <li>D - Move right.</li>
 * <li>E - Display the current execution mode to standard output.</li>
 * <li>ESC - Exit the program. You may have to press a few times if you're using the execution mode JTP (Java Thread Pool), as it's pretty unresponsive.</li>
 * <li>S - Move backward.</li>
 * <li>T - Toggle between the two execution modes GPU and JTP (Java Thread Pool).</li>
 * <li>W - Move forward.</li>
 * </ul>
 * <p>
 * The features currently supported are the following:
 * <ul>
 * <li>Shapes such as planes, spheres and triangles.</li>
 * <li>Lights such as point lights.</li>
 * <li>Textures such as normal color textures.</li>
 * <li>Texture mapping such as spherical texture mapping.</li>
 * <li>Ray Casting, which corresponds to the primary rays of Ray Tracing. This means no reflections and refractions are shown. At least not yet.</li>
 * </ul>
 * <p>
 * Note that the features above will probably be expanded upon with time.
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public final class RayCaster extends Kernel implements KeyListener {
	private static final float EPSILON = 1.e-4F;
	private static final float MAXIMUM_DISTANCE = Float.MAX_VALUE;
	private static final float PI = (float)(Math.PI);
	private static final float TYPE_PLANE = 2.0F;
	private static final float TYPE_POINT_LIGHT = 1.0F;
	private static final float TYPE_SPHERE = 1.0F;
	private static final float TYPE_TRIANGLE = 3.0F;
	private static final int ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT = 0;
	private static final int ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT_VECTOR = 6;
	private static final int ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR = 9;
	private static final int ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR = 12;
	private static final int ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR = 15;
	private static final int ABSOLUTE_OFFSET_OF_CAMERA_UP_VECTOR = 3;
	private static final int ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE_SCALAR = 18;
	private static final int HEIGHT = 768;
	private static final int RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE_SCALAR_IN_INTERSECTIONS = 1;
	private static final int RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS = 0;
	private static final int RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS = 2;
	private static final int RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS = 5;
	private static final int RELATIVE_OFFSET_OF_LIGHT_TYPE_SCALAR_IN_LIGHTS = 0;
	private static final int RELATIVE_OFFSET_OF_LIGHT_SIZE_SCALAR_IN_LIGHTS = 1;
	private static final int RELATIVE_OFFSET_OF_PLANE_SURFACE_NORMAL_VECTOR_IN_SHAPES = 2;
	private static final int RELATIVE_OFFSET_OF_POINT_LIGHT_DISTANCE_FALLOFF_SCALAR_IN_LIGHTS = 5;
	private static final int RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION_POINT_IN_LIGHTS = 2;
	private static final int RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS = 3;
	private static final int RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS = 0;
	private static final int RELATIVE_OFFSET_OF_SHAPE_TYPE_SCALAR_IN_SHAPES = 0;
	private static final int RELATIVE_OFFSET_OF_SHAPE_SIZE_SCALAR_IN_SHAPES = 1;
	@SuppressWarnings("unused")
	private static final int RELATIVE_OFFSET_OF_SPHERE_COLOR_RGB_IN_SHAPES = 6;
	private static final int RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES = 2;
	private static final int RELATIVE_OFFSET_OF_SPHERE_RADIUS_SCALAR_IN_SHAPES = 5;
	private static final int RELATIVE_OFFSET_OF_TRIANGLE_A_POINT_IN_SHAPES = 2;
	private static final int RELATIVE_OFFSET_OF_TRIANGLE_B_POINT_IN_SHAPES = 5;
	private static final int RELATIVE_OFFSET_OF_TRIANGLE_C_POINT_IN_SHAPES = 8;
	private static final int RELATIVE_OFFSET_OF_TRIANGLE_SURFACE_NORMAL_VECTOR_IN_SHAPES = 11;
	private static final int SIZE_OF_CAMERA = 3 + 3 + 3 + 3 + 3 + 3 + 1;
	private static final int SIZE_OF_INTERSECTION_IN_INTERSECTIONS = 1 + 1 + 3 + 3;
	private static final int SIZE_OF_PIXEL_IN_PIXELS = 1 + 1 + 1;
	private static final int SIZE_OF_PLANE_IN_SHAPES = 1 + 1 + 3;
	private static final int SIZE_OF_POINT_LIGHT_IN_LIGHTS = 1 + 1 + 3 + 1;
	private static final int SIZE_OF_RAY_IN_RAYS = 3 + 3;
	private static final int SIZE_OF_SPHERE_IN_SHAPES = 1 + 1 + 3 + 1 + 3;
	private static final int SIZE_OF_TRIANGLE_IN_SHAPES = 1 + 1 + 3 + 3 + 3 + 3;
	private static final int WIDTH = 1024;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
//	The following variables are only used by the CPU:
	private final AtomicBoolean isPrintingExecutionMode = new AtomicBoolean();
	private final AtomicBoolean isRunning = new AtomicBoolean();
	private final AtomicBoolean isTerminationRequested = new AtomicBoolean();
	private final AtomicBoolean isTogglingExecutionMode = new AtomicBoolean();
	private final boolean[] isKeyPressed;
	private final BufferedImage bufferedImage;
	private final Camera camera;
	private final FPSCounter fPSCounter;
	private final JFrame jFrame;
	private final Range range;
	
//	The following variables are used by the GPU (and if not all, at least a few are also used by the CPU):
	private final float[] cameraValues;
	private final float[] intersections;
	private final float[] lights;
	private final float[] pixels;
	private final float[] rays;
	private final float[] shapes;
	private final int height;
	private final int lightsLength;
	private final int shapesLength;
	private final int textureHeight;
	private final int textureWidth;
	private final int width;
	private final int[] rGB;
	private final int[] texture;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private RayCaster() {
		final Scene scene = doCreateScene();
		
		final Texture texture = doCreateTexture("Texture.jpg");
		
		this.bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		this.camera = new Camera();
		this.cameraValues = this.camera.getArray();
		this.fPSCounter = new FPSCounter();
		this.height = this.bufferedImage.getHeight();
		this.intersections = doCreateIntersections(this.bufferedImage.getWidth() * this.bufferedImage.getHeight());
		this.isKeyPressed = new boolean[256];
		this.jFrame = doCreateJFrame(this.bufferedImage, this.camera, this.fPSCounter);
		this.lights = scene.toLightArray();
		this.lightsLength = this.lights.length;
		this.pixels = doCreatePixels(this.bufferedImage.getWidth() * this.bufferedImage.getHeight());
		this.range = Range.create(this.bufferedImage.getWidth() * this.bufferedImage.getHeight());
		this.rays = doCreateRays(this.bufferedImage.getWidth() * this.bufferedImage.getHeight());
		this.rGB = doToRGB(this.bufferedImage);
		this.shapes = scene.toShapeArray();
		this.shapesLength = this.shapes.length;
		this.texture = texture.getData();
		this.textureHeight = texture.getHeight();
		this.textureWidth = texture.getWidth();
		this.width = this.bufferedImage.getWidth();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Overridden to handle key typing.
	 * 
	 * @param e a {@code KeyEvent}
	 */
	@Override
	public void keyTyped(final KeyEvent e) {
//		Do nothing here.
	}
	
	/**
	 * Overridden to handle key pressing.
	 * 
	 * @param e a {@code KeyEvent}
	 */
	@Override
	public void keyPressed(final KeyEvent e) {
		this.isKeyPressed[e.getKeyCode()] = true;
	}
	
	/**
	 * Overridden to handle key releasing.
	 * 
	 * @param e a {@code KeyEvent}
	 */
	@Override
	public void keyReleased(final KeyEvent e) {
		this.isKeyPressed[e.getKeyCode()] = false;
	}
	
	/**
	 * This is what the {@code Kernel} executes on the GPU (or in the CPU).
	 */
	@Override
	public void run() {
//		Initialize index and offset values:
		final int index = getGlobalId();
		final int pixelOffset = index * SIZE_OF_PIXEL_IN_PIXELS;
		final int rayOffset = index * SIZE_OF_RAY_IN_RAYS;
		
//		Initialize the U- and V-coordinates:
		final float u = index % this.width - this.width / 2.0F + 0.5F;
		final float v = index / this.width - this.height / 2.0F + 0.5F;
		
//		Update the origin point and direction vector of the ray to fire:
		this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 0] = this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 0];
		this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 1] = this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 1];
		this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 2] = this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 2];
		this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 0] = this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR + 0] * u + this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR + 0] * v - this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR + 0] * this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE_SCALAR];
		this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 1] = this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR + 1] * u + this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR + 1] * v - this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR + 1] * this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE_SCALAR];
		this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 2] = this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR + 2] * u + this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR + 2] * v - this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR + 2] * this.cameraValues[ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE_SCALAR];
		
//		Normalize the ray direction vector:
		doNormalize(this.rays, rayOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS);
		
//		Calculate the distance to the closest shape, if any:
		final float distance = doGetIntersection();
		
//		Initialize the RGB-values of the current pixel to black:
		int r = 0;
		int g = 0;
		int b = 0;
		
		if(distance > 0.0F && distance < MAXIMUM_DISTANCE) {
//			Initialize needed offset values:
			final int intersectionOffset = index * SIZE_OF_INTERSECTION_IN_INTERSECTIONS;
			final int shapeOffset = (int)(this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS]);
			
//			Initialize the shading variable:
			float shading = 1.0F;
			
			if(this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SHAPE_TYPE_SCALAR_IN_SHAPES] == TYPE_PLANE) {
				this.pixels[pixelOffset + 0] = 255.0F;
				this.pixels[pixelOffset + 1] = 255.0F;
				this.pixels[pixelOffset + 2] = 255.0F;
			}
			
			if(this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SHAPE_TYPE_SCALAR_IN_SHAPES] == TYPE_SPHERE) {
//				Calculate the shading for the intersected sphere at the surface intersection point:
				shading = doCalculateShading(intersectionOffset, shapeOffset);
				
				doPerformSphericalTextureMapping(intersectionOffset, pixelOffset, shapeOffset);
			}
			
			if(this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SHAPE_TYPE_SCALAR_IN_SHAPES] == TYPE_TRIANGLE) {
				this.pixels[pixelOffset + 0] = 255.0F;
				this.pixels[pixelOffset + 1] = 255.0F;
				this.pixels[pixelOffset + 2] = 255.0F;
			}
			
//			Update the RGB-values of the current pixel, given the RGB-values of the intersected shape:
			r = (int)(this.pixels[pixelOffset + 0] * shading);
			g = (int)(this.pixels[pixelOffset + 1] * shading);
			b = (int)(this.pixels[pixelOffset + 2] * shading);
		}
		
//		Set the RGB-value of the current pixel:
		this.rGB[index] = doToRGB(r, g, b);
	}
	
	/**
	 * Called to start the Ray Caster.
	 */
	public void start() {
//		Initialize the field isRunning to true:
		this.isRunning.set(true);
		
//		Add a KeyListener to the JFrame:
		doInvokeAndWait(() -> this.jFrame.addKeyListener(this));
		
//		Make this Kernel instance explicit, such that we have to take care of all array transfers to and from the GPU:
		setExplicit(true);
		
//		Tell the API to fetch the below arrays and their values before executing this Kernel instance (they will be transferred to the GPU):
		put(this.intersections);
		put(this.lights);
		put(this.rays);
		put(this.shapes);
		put(this.rGB);
		
		while(this.isRunning.get()) {
//			Update the current frame:
			doUpdate();
			
//			Tell the API to fetch the camera values before executing this Kernel instance (it will be transferred to the GPU every cycle):
			put(this.cameraValues);
			
//			Execute this Kernel instance:
			execute(this.range);
			
//			Fetch the RGB-values calculated in the GPU to the rGB array, so we can display the result:
			get(this.rGB);
			
//			Tell the JFrame to repaint itself:
			this.jFrame.repaint();
			
//			Update the FPS in the FPSCounter:
			this.fPSCounter.update();
		}
		
//		Tell the Kernel to dispose of any resources used.
		dispose();
		
//		Tell the JFrame to dispose of any resources used.
		this.jFrame.dispose();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * The entry-point of this Ray Caster.
	 * 
	 * @param args these are not used
	 */
	public static void main(final String[] args) {
		final
		RayCaster rayCaster = doRunInEDT(() -> new RayCaster());
		rayCaster.start();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@SuppressWarnings("unused")
	private float doCalculateShading(final int intersectionOffset, final int shapeOffset) {
//		Initialize the shading value:
		float shading = 0.0F;
		
//		Initialize values from the intersection:
		final float surfaceIntersectionX = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 0];
		final float surfaceIntersectionY = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 1];
		final float surfaceIntersectionZ = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 2];
		final float surfaceNormalX = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 0];
		final float surfaceNormalY = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 1];
		final float surfaceNormalZ = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 2];
		
		for(int i = 0, j = 0; i < this.lightsLength; i += j) {
//			Initialize the temporary type and size variables of the current light:
			final float lightType = this.lights[i + RELATIVE_OFFSET_OF_LIGHT_TYPE_SCALAR_IN_LIGHTS];
			final float lightSize = this.lights[i + RELATIVE_OFFSET_OF_LIGHT_SIZE_SCALAR_IN_LIGHTS];
			
//			Set the light size as increment for the next loop iteration:
			j = (int)(lightSize);
			
			if(lightType == TYPE_POINT_LIGHT) {
//				Initialize the temporary X-, Y-, Z- and distance falloff variables of the current point light:
				final float pointLightX = this.lights[i + RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION_POINT_IN_LIGHTS + 0];
				final float pointLightY = this.lights[i + RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION_POINT_IN_LIGHTS + 1];
				final float pointLightZ = this.lights[i + RELATIVE_OFFSET_OF_POINT_LIGHT_POSITION_POINT_IN_LIGHTS + 2];
				final float pointLightDistanceFalloff = this.lights[i + RELATIVE_OFFSET_OF_POINT_LIGHT_DISTANCE_FALLOFF_SCALAR_IN_LIGHTS];
				
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
				
//				Calculate the shading as the maximum value of 0.1 and the dot product of the delta vector and the surface normal vector:
				shading = max(dx * surfaceNormalX + dy * surfaceNormalY + dz * surfaceNormalZ, 0.1F);
			}
		}
		
		return shading;
	}
	
	private float doGetIntersection() {
//		Initialize the index and offset values:
		final int index = getGlobalId();
		final int intersectionOffset = index * SIZE_OF_INTERSECTION_IN_INTERSECTIONS;
		final int rayOffset = index * SIZE_OF_RAY_IN_RAYS;
		
//		Initialize offset to closest shape:
		int shapeClosestOffset = -1;
		
//		Initialize distance to closest shape:
		float shapeClosestDistance = MAXIMUM_DISTANCE;
		
//		Initialize the ray values:
		final float rayOriginX = this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 0];
		final float rayOriginY = this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 1];
		final float rayOriginZ = this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 2];
		final float rayDirectionX = this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 0];
		final float rayDirectionY = this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 1];
		final float rayDirectionZ = this.rays[rayOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 2];
		
//		Reset the float array intersections, so we can perform a new intersection test:
		this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS] = -1.0F;
		this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE_SCALAR_IN_INTERSECTIONS] = MAXIMUM_DISTANCE;
		
		for(int i = 0, j = 0; i < this.shapesLength; i += j) {
//			Initialize the temporary type and size variables of the current shape:
			final float shapeType = this.shapes[i + RELATIVE_OFFSET_OF_SHAPE_TYPE_SCALAR_IN_SHAPES];
			final float shapeSize = this.shapes[i + RELATIVE_OFFSET_OF_SHAPE_SIZE_SCALAR_IN_SHAPES];
			
//			Set the shape size as increment for the next loop iteration:
			j = (int)(shapeSize);
			
//			Initialize the shape distance to the maximum value:
			float shapeDistance = MAXIMUM_DISTANCE;
			
			if(shapeType == TYPE_PLANE) {
				shapeDistance = doGetIntersectionForPlane(rayOriginX, rayOriginY, rayOriginZ, rayDirectionX, rayDirectionY, rayDirectionZ, i);
			}
			
			if(shapeType == TYPE_SPHERE) {
				shapeDistance = doGetIntersectionForSphere(rayOriginX, rayOriginY, rayOriginZ, rayDirectionX, rayDirectionY, rayDirectionZ, i);
			}
			
			if(shapeType == TYPE_TRIANGLE) {
				shapeDistance = doGetIntersectionForTriangle(rayOriginX, rayOriginY, rayOriginZ, rayDirectionX, rayDirectionY, rayDirectionZ, i);
			}
			
			if(shapeDistance > 0.0F && shapeDistance < shapeClosestDistance) {
//				Update the distance to and the offset of the closest shape:
				shapeClosestDistance = shapeDistance;
				shapeClosestOffset = i;
			}
		}
		
		if(shapeClosestOffset > -1) {
//			Update the intersections array with values found:
			this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS] = shapeClosestOffset;
			this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE_SCALAR_IN_INTERSECTIONS] = shapeClosestDistance;
			this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 0] = rayOriginX + rayDirectionX * shapeClosestDistance;
			this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 1] = rayOriginY + rayDirectionY * shapeClosestDistance;
			this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 2] = rayOriginZ + rayDirectionZ * shapeClosestDistance;
			
			if(this.shapes[shapeClosestOffset + RELATIVE_OFFSET_OF_SHAPE_TYPE_SCALAR_IN_SHAPES] == TYPE_SPHERE) {
//				Initialize variables with the position of the sphere:
				final float sphereX = this.shapes[shapeClosestOffset + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 0];
				final float sphereY = this.shapes[shapeClosestOffset + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 1];
				final float sphereZ = this.shapes[shapeClosestOffset + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 2];
				
//				Initialize variables with the delta values between the surface intersection point and the center of the sphere:
				final float dx = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 0] - sphereX;
				final float dy = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 1] - sphereY;
				final float dz = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 2] - sphereZ;
				
//				Calculate the length of the delta vector:
				final float length = sqrt(dx * dx + dy * dy + dz * dz);
				
//				Initialize variables with the surface normal vector:
				float surfaceNormalX = 0.0F;
				float surfaceNormalY = 0.0F;
				float surfaceNormalZ = 0.0F;
				
				if(length > 0.0F) {
//					//Calculate the length reciprocal:
					final float lengthReciprocal = 1.0F / length;
					
//					Set the surface normal vector to the delta vector multiplied with the length reciprocal:
					surfaceNormalX = dx * lengthReciprocal;
					surfaceNormalY = dy * lengthReciprocal;
					surfaceNormalZ = dz * lengthReciprocal;
				}
				
//				Update the intersections array with the surface normal vector:
				this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 0] = surfaceNormalX;
				this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 1] = surfaceNormalY;
				this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 2] = surfaceNormalZ;
			}
		}
		
		return shapeClosestDistance;
	}
	
	private float doGetIntersectionForPlane(final float rayOriginX, final float rayOriginY, final float rayOriginZ, final float rayDirectionX, final float rayDirectionY, final float rayDirectionZ, final int shapeOffset) {
//		Initialize a variable with the plane constant:
		final float planeConstant = -2.0F;
		
//		Initialize the temporary X-, Y- and Z- variables of the current plane:
		final float planeSurfaceNormalX = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_PLANE_SURFACE_NORMAL_VECTOR_IN_SHAPES + 0];
		final float planeSurfaceNormalY = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_PLANE_SURFACE_NORMAL_VECTOR_IN_SHAPES + 1];
		final float planeSurfaceNormalZ = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_PLANE_SURFACE_NORMAL_VECTOR_IN_SHAPES + 2];
		
//		Calculate the dot product and its absolute version:
		final float dotProduct = rayDirectionX * planeSurfaceNormalX + rayDirectionY * planeSurfaceNormalY + rayDirectionZ * planeSurfaceNormalZ;
		final float dotProductAbsolute = abs(dotProduct);
		
//		Initialize the shape distance variable:
		float shapeDistance = 0.0F;
		
		if(dotProductAbsolute >= EPSILON) {
//			Calculate the distance:
			shapeDistance = (planeConstant - (rayOriginX * planeSurfaceNormalX + rayOriginY * planeSurfaceNormalY + rayOriginZ * planeSurfaceNormalZ)) / dotProduct;
		}
		
		return shapeDistance;
	}
	
	private float doGetIntersectionForSphere(final float rayOriginX, final float rayOriginY, final float rayOriginZ, final float rayDirectionX, final float rayDirectionY, final float rayDirectionZ, final int shapeOffset) {
//		Initialize the temporary X-, Y-, Z- and radius variables of the current sphere:
		final float sphereX = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 0];
		final float sphereY = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 1];
		final float sphereZ = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 2];
		final float sphereRadius = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SPHERE_RADIUS_SCALAR_IN_SHAPES];
		
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
			
			if(shapeDistance <= EPSILON) {
//				Recalculate the distance:
				shapeDistance = b + discriminant;
				
				if(shapeDistance <= EPSILON) {
//					We're too close to the shape, so we practically do not see it:
					shapeDistance = 0.0F;
				}
			}
		}
		
		return shapeDistance;
	}
	
	private float doGetIntersectionForTriangle(final float rayOriginX, final float rayOriginY, final float rayOriginZ, final float rayDirectionX, final float rayDirectionY, final float rayDirectionZ, final int shapeOffset) {
		float shapeDistance = 0.0F;
		
		final float triangleAX = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_TRIANGLE_A_POINT_IN_SHAPES + 0];
		final float triangleAY = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_TRIANGLE_A_POINT_IN_SHAPES + 1];
		final float triangleAZ = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_TRIANGLE_A_POINT_IN_SHAPES + 2];
		
		final float triangleBX = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_TRIANGLE_B_POINT_IN_SHAPES + 0];
		final float triangleBY = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_TRIANGLE_B_POINT_IN_SHAPES + 1];
		final float triangleBZ = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_TRIANGLE_B_POINT_IN_SHAPES + 2];
		
		final float triangleCX = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_TRIANGLE_C_POINT_IN_SHAPES + 0];
		final float triangleCY = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_TRIANGLE_C_POINT_IN_SHAPES + 1];
		final float triangleCZ = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_TRIANGLE_C_POINT_IN_SHAPES + 2];
		
		final float edge0X = triangleBX - triangleAX;
		final float edge0Y = triangleBY - triangleAY;
		final float edge0Z = triangleBZ - triangleAZ;
		
		final float edge1X = triangleCX - triangleAX;
		final float edge1Y = triangleCY - triangleAY;
		final float edge1Z = triangleCZ - triangleAZ;
		
		final float pX = rayDirectionY * edge1Z - rayDirectionZ * edge1Y;
		final float pY = rayDirectionZ * edge1X - rayDirectionX * edge1Z;
		final float pZ = rayDirectionX * edge1Y - rayDirectionY * edge1X;
		
		final float determinant = edge0X * pX + edge0Y * pY + edge0Z * pZ;
		
		if(determinant != 0.0F) {
			final float determinantReciprocal = 1.0F / determinant;
			
			final float vX = rayOriginX - triangleAX;
			final float vY = rayOriginY - triangleAY;
			final float vZ = rayOriginZ - triangleAZ;
			
			final float u = (vX * pX + vY * pY + vZ * pZ) * determinantReciprocal;
			
			if(u >= 0.0F && u <= 1.0F) {
				final float qX = vY * edge0Z - vZ * edge0Y;
				final float qY = vZ * edge0X - vX * edge0Z;
				final float qZ = vX * edge0Y - vY * edge0X;
				
				final float v = (rayDirectionX * qX + rayDirectionY * qY + rayDirectionZ * qZ) * determinantReciprocal;
				
				if(v >= 0.0F && u + v <= 1.0F) {
					shapeDistance = (edge1X * qX + edge1Y * qY + edge1Z * qZ) * determinantReciprocal;
				}
			}
		}
		
		return shapeDistance;
	}
	
	private float doLength(final float[] vector, final int offset) {
		return sqrt(doLengthSquared(vector, offset));
	}
	
	private void doNormalize(final float[] vector, final int offset) {
		final float lengthReciprocal = 1.0F / doLength(vector, offset);
		
		vector[offset + 0] *= lengthReciprocal;
		vector[offset + 1] *= lengthReciprocal;
		vector[offset + 2] *= lengthReciprocal;
	}
	
	private void doPerformSphericalTextureMapping(final int intersectionOffset, final int pixelOffset, final int shapeOffset) {
		final float sphereX = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 0];
		final float sphereY = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 1];
		final float sphereZ = this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 2];
		
		final float surfaceIntersectionX = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 0];
		final float surfaceIntersectionY = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 1];
		final float surfaceIntersectionZ = this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 2];
		
		final float dx = sphereX - surfaceIntersectionX;
		final float dy = sphereY - surfaceIntersectionY;
		final float dz = sphereZ - surfaceIntersectionZ;
		
		final float lengthReciprocal = 1.0F / sqrt(dx * dx + dy * dy + dz * dz);
		
		final float distanceX = dx * lengthReciprocal;
		final float distanceY = dy * lengthReciprocal;
		final float distanceZ = dz * lengthReciprocal;
		
		final float textureU = 0.5F + atan2(distanceX, distanceZ) / (2.0F * PI);
		final float textureV = 0.5F + asin(distanceY) / PI;
		
		final int textureX = (int)(this.textureWidth * ((textureU + 1.0F) * 0.5F));
		final int textureY = (int)(this.textureHeight * ((textureV + 1.0F) * 0.5F));
		final int textureIndex = textureY * this.textureWidth + textureX;
		final int textureRGB = this.texture[textureIndex];
		
		this.pixels[pixelOffset + 0] = doToR(textureRGB);
		this.pixels[pixelOffset + 1] = doToG(textureRGB);
		this.pixels[pixelOffset + 2] = doToB(textureRGB);
	}
	
	private void doUpdate() {
//		Calculate the movement based on some velocity, calculated as the distance moved per second:
		final float velocity = 250.0F;
		final float movement = this.fPSCounter.getFrameTimeMillis() / 1000.0F * velocity;
		
		if(this.isKeyPressed[KeyEvent.VK_A]) {
			this.camera.move(-movement, 0.0F, 0.0F);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_D]) {
			this.camera.move(movement, 0.0F, 0.0F);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_E] && this.isPrintingExecutionMode.compareAndSet(false, true)) {
			System.out.printf("ExecutionMode: %s%n", getExecutionMode());
		} else if(!this.isKeyPressed[KeyEvent.VK_E]) {
			this.isPrintingExecutionMode.compareAndSet(true, false);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_ESCAPE] && this.isTerminationRequested.compareAndSet(false, true)) {
			this.isRunning.set(false);
		} else if(!this.isKeyPressed[KeyEvent.VK_ESCAPE]) {
			this.isTerminationRequested.compareAndSet(true, false);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_S]) {
			this.camera.move(0.0F, 0.0F, movement);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_T] && this.isTogglingExecutionMode.compareAndSet(false, true)) {
			setExecutionMode(getExecutionMode() == EXECUTION_MODE.GPU ? EXECUTION_MODE.JTP : EXECUTION_MODE.GPU);
		} else if(!this.isKeyPressed[KeyEvent.VK_T]) {
			this.isTogglingExecutionMode.compareAndSet(true, false);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_W]) {
			this.camera.move(0.0F, 0.0F, -movement);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static BufferedImage doCreateBufferedImageFrom(final InputStream inputStream) {
		try(final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
			BufferedImage bufferedImage0 = ImageIO.read(bufferedInputStream);
			
			if(bufferedImage0.getType() != BufferedImage.TYPE_INT_RGB) {
				final BufferedImage bufferedImage1 = new BufferedImage(bufferedImage0.getWidth(), bufferedImage0.getHeight(), BufferedImage.TYPE_INT_RGB);
				
				final
				Graphics2D graphics2D = bufferedImage1.createGraphics();
				graphics2D.drawImage(bufferedImage0, 0, 0, null);
				graphics2D.dispose();
				
				bufferedImage0 = bufferedImage1;
			}
			
			return bufferedImage0;
		} catch(final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private static float doDotProduct(final float[] vector0, final int offset0, final float[] vector1, final int offset1) {
		return vector0[offset0] * vector1[offset1] + vector0[offset0 + 1] * vector1[offset1 + 1] + vector0[offset0 + 2] * vector1[offset1 + 2];
	}
	
	private static float doLengthSquared(final float[] vector, final int offset) {
		return doDotProduct(vector, offset, vector, offset);
	}
	
	private static float doRandom(final float range) {
		return ThreadLocalRandom.current().nextFloat() * range;
	}
	
	private static float[] doCreateIntersections(final int length) {
		final float[] intersections = new float[length * SIZE_OF_INTERSECTION_IN_INTERSECTIONS];
		
		for(int i = 0; i < intersections.length; i += SIZE_OF_INTERSECTION_IN_INTERSECTIONS) {
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS] = -1.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE_SCALAR_IN_INTERSECTIONS] = MAXIMUM_DISTANCE;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 0] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 1] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 2] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 0] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 1] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 2] = 0.0F;
		}
		
		return intersections;
	}
	
	private static float[] doCreatePixels(final int length) {
		final float[] pixels = new float[length * SIZE_OF_PIXEL_IN_PIXELS];
		
		for(int i = 0; i < pixels.length; i += SIZE_OF_PIXEL_IN_PIXELS) {
			pixels[i + 0] = 0.0F;
			pixels[i + 1] = 0.0F;
			pixels[i + 2] = 0.0F;
		}
		
		return pixels;
	}
	
	private static float[] doCreateRays(final int length) {
		return new float[length * SIZE_OF_RAY_IN_RAYS];
	}
	
	private static int doToB(final int rGB) {
		return (rGB >> 0) & 0xFF;
	}
	
	private static int doToG(final int rGB) {
		return (rGB >> 8) & 0xFF;
	}
	
	private static int doToR(final int rGB) {
		return (rGB >> 16) & 0xFF;
	}
	
	private static int doToRGB(final int r, final int g, final int b) {
		return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
	}
	
	private static int[] doGetDataFrom(final BufferedImage bufferedImage) {
		final WritableRaster writableRaster = bufferedImage.getRaster();
		
		final DataBuffer dataBuffer = writableRaster.getDataBuffer();
		
		final DataBufferInt dataBufferInt = DataBufferInt.class.cast(dataBuffer);
		
		final int[] data = dataBufferInt.getData();
		
		return data;
	}
	
	private static int[] doToRGB(final BufferedImage bufferedImage) {
		final WritableRaster writableRaster = bufferedImage.getRaster();
		
		final DataBuffer dataBuffer = writableRaster.getDataBuffer();
		
		final DataBufferInt dataBufferInt = DataBufferInt.class.cast(dataBuffer);
		
		final int[] rGB = dataBufferInt.getData();
		
		return rGB;
	}
	
	private static JFrame doCreateJFrame(final BufferedImage bufferedImage, final Camera camera, final FPSCounter fPSCounter) {
		final
		JFrame jFrame = new JFrame();
		jFrame.setContentPane(doCreateJPanel(bufferedImage, camera, fPSCounter));
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jFrame.setFocusTraversalKeysEnabled(false);
		jFrame.setIgnoreRepaint(true);
		jFrame.setSize(bufferedImage.getWidth(), bufferedImage.getHeight());
		jFrame.setLocationRelativeTo(null);
		jFrame.setTitle("OpenCL Ray Caster");
		jFrame.setVisible(true);
		jFrame.createBufferStrategy(2);
		jFrame.repaint();
		
		return jFrame;
	}
	
	private static JPanel doCreateJPanel(final BufferedImage bufferedImage, final Camera camera, final FPSCounter fPSCounter) {
		final
		JPanel jPanel = new JBufferedImagePanel(bufferedImage, camera, fPSCounter);
		jPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
		jPanel.setLayout(new AbsoluteLayoutManager());
		jPanel.setPreferredSize(new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight()));
		
		return jPanel;
	}
	
	private static Scene doCreateScene() {
		final
		Scene scene = new Scene();
		scene.addLight(new PointLight(400.0F, 20.0F, 400.0F, 100.0F));
		scene.addLight(new PointLight(600.0F, 20.0F, 600.0F, 100.0F));
		scene.addLight(new PointLight(600.0F, 20.0F, 400.0F, 100.0F));
		scene.addLight(new PointLight(400.0F, 20.0F, 600.0F, 100.0F));
		
		for(int i = 0; i < 500; i++) {
			scene.addShape(doCreateRandomSphere());
		}
		
//		scene.addShape(new Plane(1.0F, 0.0F, 0.0F));
		scene.addShape(new Triangle(2500.0F, 40.0F, 2500.0F, 1000.0F, 40.0F, 1500.0F, -1000.0F, 40.0F, -1000.0F));
		
		return scene;
	}
	
	private static Sphere doCreateRandomSphere() {
		return new Sphere(doRandom(4000.0F), 16.5F, doRandom(4000.0F), 16.5F, doRandom(255.0F), doRandom(255.0F), doRandom(255.0F));
	}
	
	private static Texture doCreateTexture(final String name) {
		try {
			return Texture.create(RayCaster.class.getResourceAsStream(name));
		} catch(final Exception e) {
			return Texture.create();
		}
	}
	
	private static void doCrossProduct(final float[] vector0, final int offset0, final float[] vector1, final int offset1, final float[] vector2, final int offset2) {
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
	
	private static void doSubtract(final float[] vector0, final int offset0, final float[] vector1, final int offset1, final float[] vector2, final int offset2) {
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
	
	private static <T> T doRunInEDT(final Supplier<T> supplier) {
		return doRunInEDT(supplier, object -> {});
	}
	
	private static <T> T doRunInEDT(final Supplier<T> supplier, final Consumer<T> consumer) {
		return doRunInEDT(supplier, consumer, null);
	}
	
	private static <T> T doRunInEDT(final Supplier<T> supplier, final Consumer<T> consumer, final T defaultObject) {
		final AtomicReference<T> atomicReference = new AtomicReference<>();
		
		if(supplier != null) {
			if(SwingUtilities.isEventDispatchThread()) {
				atomicReference.set(supplier.get());
			} else {
				doInvokeAndWait(() -> atomicReference.set(supplier.get()));
			}
		}
		
		if(defaultObject != null) {
			atomicReference.compareAndSet(null, defaultObject);
		}
		
		if(consumer != null) {
			consumer.accept(atomicReference.get());
		}
		
		return atomicReference.get();
	}
	
	private static void doInvokeAndWait(final Runnable runnable) {
		if(runnable != null) {
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch(final InvocationTargetException | InterruptedException e) {
//				Do nothing.
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final class AbsoluteLayoutManager implements LayoutManager, Serializable {
		private static final long serialVersionUID = 1L;
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		private boolean isUsingPreferredSize;
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public AbsoluteLayoutManager() {
			this(true);
		}
		
		public AbsoluteLayoutManager(final boolean isUsingPreferredSize) {
			setUsingPreferredSize(isUsingPreferredSize);
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		@SuppressWarnings("unused")
		public boolean isUsingPreferredSize() {
			return this.isUsingPreferredSize;
		}
		
		@Override
		public void addLayoutComponent(final String name, final Component component) {
//			Do nothing.
		}
		
		@Override
		public Dimension minimumLayoutSize(final Container parent) {
			synchronized(parent.getTreeLock()) {
				return preferredLayoutSize(parent);
			}
		}
		
		@Override
		public Dimension preferredLayoutSize(final Container parent) {
			synchronized(parent.getTreeLock()) {
				return doGetContainerSize(parent);
			}
		}
		
		@Override
		public String toString() {
			return String.format("[%s]", getClass().getName());
		}
		
		@Override
		public void layoutContainer(final Container parent) {
			synchronized(parent.getTreeLock()) {
				final Insets parentInsets = parent.getInsets();
				
				int x = parentInsets.left;
				int y = parentInsets.top;
				
				for(final Component component: parent.getComponents()) {
					if(component.isVisible()) {
						final Point location = component.getLocation();
						
						x = Math.min(x, location.x);
						y = Math.min(y, location.y);
					}
				}
				
				x = (x < parentInsets.left) ? parentInsets.left - x : 0;
				y = (y < parentInsets.top) ? parentInsets.top - y : 0;
				
				for(final Component component: parent.getComponents()) {
					if(component.isVisible()) {
						final Point location = component.getLocation();
						
						final Dimension componentSize = doGetComponentSize(component);
						
						component.setBounds(location.x + x, location.y + y, componentSize.width, componentSize.height);
					}
				}
			}
		}
		
		@Override
		public void removeLayoutComponent(final Component component) {
//			Do nothing.
		}
		
		public void setUsingPreferredSize(final boolean isUsingPreferredSize) {
			this.isUsingPreferredSize = isUsingPreferredSize;
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		private Dimension doGetComponentSize(final Component component) {
			final Dimension preferredSize = component.getPreferredSize();
			final Dimension size = component.getSize();
			
			Dimension componentSize = preferredSize;
			
			if(!this.isUsingPreferredSize && size.width > 0 && size.height > 0) {
				componentSize = size;
			}
			
			return componentSize;
		}
		
		private Dimension doGetContainerSize(final Container parent) {
			final Insets parentInsets = parent.getInsets();
			
			int x = parentInsets.left;
			int y = parentInsets.top;
			int width = 0;
			int height = 0;
			
			for(final Component component : parent.getComponents()) {
				if(component.isVisible()) {
					final Point location = component.getLocation();
					
					final Dimension componentSize = doGetComponentSize(component);
					
					x = Math.min(x, location.x);
					y = Math.min(y, location.y);
					width = Math.max(width, location.x + componentSize.width);
					height = Math.max(height, location.y + componentSize.height);
				}
			}
			
			if(x < parentInsets.left) {
				width += parentInsets.left - x;
			}
			
			if(y < parentInsets.top) {
				height += parentInsets.top - y;
			}
			
			width += parentInsets.right;
			height += parentInsets.bottom;
			
			return new Dimension(width, height);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
//	TODO: Find out whether Look-at Z and View-plane distance are correlated in some way!?
	public static final class Camera {
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
		
		@SuppressWarnings("synthetic-access")
		public void calculateOrthonormalBasis() {
			doSubtract(this.array, ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT, this.array, ABSOLUTE_OFFSET_OF_CAMERA_LOOK_AT_VECTOR, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR);
			doNormalize(this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR);
			doCrossProduct(this.array, ABSOLUTE_OFFSET_OF_CAMERA_UP_VECTOR, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR);
			doNormalize(this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR);
			doCrossProduct(this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR, this.array, ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR);
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
		
		@SuppressWarnings("synthetic-access")
		private static float doLength(final float[] vector, final int offset) {
			return (float)(Math.sqrt(doLengthSquared(vector, offset)));
		}
		
		private static void doNormalize(final float[] vector, final int offset) {
			final float lengthReciprocal = 1.0F / doLength(vector, offset);
			
			vector[offset + 0] *= lengthReciprocal;
			vector[offset + 1] *= lengthReciprocal;
			vector[offset + 2] *= lengthReciprocal;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static final class FPSCounter {
		private final AtomicLong newFPS = new AtomicLong();
		private final AtomicLong newFPSReferenceTimeMillis = new AtomicLong();
		private final AtomicLong newFrameTimeMillis = new AtomicLong();
		private final AtomicLong oldFPS = new AtomicLong();
		private final AtomicLong oldFrameTimeMillis = new AtomicLong();
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public FPSCounter() {
			
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public long getFPS() {
			return this.oldFPS.get();
		}
		
		public long getFrameTimeMillis() {
			return this.oldFrameTimeMillis.get();
		}
		
		public void update() {
			final long currentTimeMillis = System.currentTimeMillis();
			
			this.newFPS.incrementAndGet();
			this.newFPSReferenceTimeMillis.compareAndSet(0L, currentTimeMillis);
			this.oldFrameTimeMillis.set(currentTimeMillis - this.newFrameTimeMillis.get());
			this.newFrameTimeMillis.set(currentTimeMillis);
			
			final long newFPSReferenceTimeMillis = this.newFPSReferenceTimeMillis.get();
			final long newFPSElapsedTimeMillis = currentTimeMillis - newFPSReferenceTimeMillis;
			
			if(newFPSElapsedTimeMillis >= 1000L) {
				this.oldFPS.set(this.newFPS.get());
				this.newFPS.set(0L);
				this.newFPSReferenceTimeMillis.set(currentTimeMillis);
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final class JBufferedImagePanel extends JPanel {
		private static final long serialVersionUID = 1L;
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		private final BufferedImage bufferedImage;
		private final Camera camera;
		private final FPSCounter fPSCounter;
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public JBufferedImagePanel(final BufferedImage bufferedImage, final Camera camera, final FPSCounter fPSCounter) {
			this.bufferedImage = bufferedImage;
			this.camera = camera;
			this.fPSCounter = fPSCounter;
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		@Override
		public void paintComponent(final Graphics graphics) {
			final String string = String.format("FPS: %s    Location: %s, %s, %s", Long.toString(this.fPSCounter.getFPS()), Float.toString(this.camera.getEyeX()), Float.toString(this.camera.getEyeY()), Float.toString(this.camera.getEyeZ()));
			
			final
			Graphics2D graphics2D = Graphics2D.class.cast(graphics);
			graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			graphics2D.drawImage(this.bufferedImage, 0, 0, this.bufferedImage.getWidth(), this.bufferedImage.getHeight(), this);
			graphics2D.setColor(Color.BLACK);
			graphics2D.fillRect(10, 10, graphics2D.getFontMetrics().stringWidth(string) + 20, graphics2D.getFontMetrics().getHeight() + 20);
			graphics2D.setColor(Color.WHITE);
			graphics2D.drawRect(10, 10, graphics2D.getFontMetrics().stringWidth(string) + 20, graphics2D.getFontMetrics().getHeight() + 20);
			graphics2D.drawString(string, 20, 30);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static interface Light {
		float getType();
		
		float[] toFloatArray();
		
		int size();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final class Plane implements Shape {
		private final float surfaceNormalX;
		private final float surfaceNormalY;
		private final float surfaceNormalZ;
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public Plane(final float surfaceNormalX, final float surfaceNormalY, final float surfaceNormalZ) {
			this.surfaceNormalX = surfaceNormalX;
			this.surfaceNormalY = surfaceNormalY;
			this.surfaceNormalZ = surfaceNormalZ;
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		@Override
		public float getType() {
			return TYPE_PLANE;
		}
		
		@Override
		public float[] toFloatArray() {
			return new float[] {
				getType(),
				size(),
				this.surfaceNormalX,
				this.surfaceNormalY,
				this.surfaceNormalZ
			};
		}
		
		@Override
		public int size() {
			return SIZE_OF_PLANE_IN_SHAPES;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final class PointLight implements Light {
		private final float distanceFalloff;
		private final float x;
		private final float y;
		private final float z;
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public PointLight(final float x, final float y, final float z, final float distanceFalloff) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.distanceFalloff = distanceFalloff;
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		@Override
		public float getType() {
			return TYPE_POINT_LIGHT;
		}
		
		@Override
		public float[] toFloatArray() {
			return new float[] {
				getType(),
				size(),
				this.x,
				this.y,
				this.z,
				this.distanceFalloff
			};
		}
		
		@Override
		public int size() {
			return SIZE_OF_POINT_LIGHT_IN_LIGHTS;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final class Scene {
		private final List<Light> lights = new ArrayList<>();
		private final List<Shape> shapes = new ArrayList<>();
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public Scene() {
			
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public float[] toLightArray() {
			int length = 0;
			int offset = 0;
			
			for(final Light light : this.lights) {
				length += light.size();
			}
			
			final float[] array0 = new float[length];
			
			for(final Light light : this.lights) {
				final float[] array1 = light.toFloatArray();
				
				System.arraycopy(array1, 0, array0, offset, array1.length);
				
				offset += array1.length;
			}
			
			return array0;
		}
		
		public float[] toShapeArray() {
			int length = 0;
			int offset = 0;
			
			for(final Shape shape : this.shapes) {
				length += shape.size();
			}
			
			final float[] array0 = new float[length];
			
			for(final Shape shape : this.shapes) {
				final float[] array1 = shape.toFloatArray();
				
				System.arraycopy(array1, 0, array0, offset, array1.length);
				
				offset += array1.length;
			}
			
			return array0;
		}
		
		public void addLight(final Light light) {
			this.lights.add(Objects.requireNonNull(light, "light == null"));
		}
		
		public void addShape(final Shape shape) {
			this.shapes.add(Objects.requireNonNull(shape, "shape == null"));
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static interface Shape {
		float getType();
		
		float[] toFloatArray();
		
		int size();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final class Sphere implements Shape {
		private final float b;
		private final float g;
		private final float r;
		private final float radius;
		private final float x;
		private final float y;
		private final float z;
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public Sphere(final float x, final float y, final float z, final float radius, final float r, final float g, final float b) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = radius;
			this.r = r;
			this.g = g;
			this.b = b;
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		@Override
		public float getType() {
			return TYPE_SPHERE;
		}
		
		@Override
		public float[] toFloatArray() {
			return new float[] {
				getType(),
				size(),
				this.x,
				this.y,
				this.z,
				this.radius,
				this.r,
				this.g,
				this.b
			};
		}
		
		@Override
		public int size() {
			return SIZE_OF_SPHERE_IN_SHAPES;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final class Texture {
		private final int height;
		private final int width;
		private final int[] data;
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		private Texture(final int width, final int height, final int[] data) {
			this.width = width;
			this.height = height;
			this.data = data;
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public int getHeight() {
			return this.height;
		}
		
		public int getWidth() {
			return this.width;
		}
		
		public int[] getData() {
			return this.data;
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public static Texture create() {
			return new Texture(1, 1, new int[] {255});
		}
		
		@SuppressWarnings("synthetic-access")
		public static Texture create(final InputStream inputStream) {
			final BufferedImage bufferedImage = doCreateBufferedImageFrom(inputStream);
			
			final int width = bufferedImage.getWidth();
			final int height = bufferedImage.getHeight();
			
			final int[] data = doGetDataFrom(bufferedImage);
			
			return new Texture(width, height, data);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final class Triangle implements Shape {
		private final float aX;
		private final float aY;
		private final float aZ;
		private final float bX;
		private final float bY;
		private final float bZ;
		private final float cX;
		private final float cY;
		private final float cZ;
		private final float surfaceNormalX;
		private final float surfaceNormalY;
		private final float surfaceNormalZ;
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public Triangle(final float aX, final float aY, final float aZ, final float bX, final float bY, final float bZ, final float cX, final float cY, final float cZ) {
			final float[] surfaceNormal = doToSurfaceNormal(aX, aY, aZ, bX, bY, bZ, cX, cY, cZ);
			
			this.aX = aX;
			this.aY = aY;
			this.aZ = aZ;
			this.bX = bX;
			this.bY = bY;
			this.bZ = bZ;
			this.cX = cX;
			this.cY = cY;
			this.cZ = cZ;
			this.surfaceNormalX = surfaceNormal[0];
			this.surfaceNormalY = surfaceNormal[1];
			this.surfaceNormalZ = surfaceNormal[2];
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		@Override
		public float getType() {
			return TYPE_TRIANGLE;
		}
		
		@Override
		public float[] toFloatArray() {
			return new float[] {
				getType(),
				size(),
				this.aX,
				this.aY,
				this.aZ,
				this.bX,
				this.bY,
				this.bZ,
				this.cX,
				this.cY,
				this.cZ,
				this.surfaceNormalX,
				this.surfaceNormalY,
				this.surfaceNormalZ
			};
		}
		
		@Override
		public int size() {
			return SIZE_OF_TRIANGLE_IN_SHAPES;
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		private static float[] doToSurfaceNormal(final float aX, final float aY, final float aZ, final float bX, final float bY, final float bZ, final float cX, final float cY, final float cZ) {
//			Subtract vector a from vector b:
			final float x0 = bX - aX;
			final float y0 = bY - aY;
			final float z0 = bZ - aZ;
			
//			Subtract vector a from vector c:
			final float x1 = cX - aX;
			final float y1 = cY - aY;
			final float z1 = cZ - aZ;
			
//			Perform the cross product on the two subtracted vectors:
			final float x2 = y0 * z1 - z0 * y1;
			final float y2 = z0 * x1 - x0 * z1;
			final float z2 = x0 * y1 - y0 * x1;
			
//			Get the length of the cross product vector:
			final float length = (float)(Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2));
			
//			Initialize the surface normal array to return:
			final float[] surfaceNormal = new float[] {x2, y2, z2};
			
			if(length > 0.0F) {
//				Get the reciprocal of the length, such that we can multiply rather than divide:
				final float lengthReciprocal = 1.0F / length;
				
//				Multiply the cross product vector with the reciprocal of the length of the cross product vector itself (normalize):
				surfaceNormal[0] *= lengthReciprocal;
				surfaceNormal[1] *= lengthReciprocal;
				surfaceNormal[2] *= lengthReciprocal;
			}
			
			return surfaceNormal;
		}
	}
}