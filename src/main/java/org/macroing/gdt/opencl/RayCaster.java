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
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

/**
 * An OpenCL-based Ray Caster using Aparapi.
 * <p>
 * The values in the {@code float} array {@code camera} consists of Eye X, Eye Y, Eye Z, Up X, Up Y, Up Z, Look-at X, Look-at Y, Look-at Z, ONB-U X, ONB-U Y, ONB-U Z, ONB-V X, ONB-V Y, ONB-V Z, ONB-W X, ONB-W Y, ONB-W Z and View-plane distance.
 * <p>
 * If you don't know what ONB stands for, then it is OrthoNormal Basis.
 * <p>
 * The values in the {@code float} array {@code intersections} consists of Shape Offset and Distance (T), for each shape currently being intersected by a ray.
 * <p>
 * The values in the {@code float} array {@code rays} consists of Origin X, Origin Y, Origin Z, Direction X, Direction Y and Direction Z, for each ray fired from each pixel.
 * <p>
 * The values in the {@code float} array {@code shapes} consists of Type, Size and {@code float}[Size], for each shape defined.
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public final class RayCaster extends Kernel implements KeyListener {
	private static final float EPSILON = 1.e-4F;
	private static final float MAXIMUM_DISTANCE = Float.MAX_VALUE;
	private static final float TYPE_SPHERE = 1.0F;
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
	private static final int RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS = 3;
	private static final int RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS = 0;
	private static final int RELATIVE_OFFSET_OF_SHAPE_TYPE_SCALAR_IN_SHAPES = 0;
	private static final int RELATIVE_OFFSET_OF_SHAPE_SIZE_SCALAR_IN_SHAPES = 1;
	private static final int RELATIVE_OFFSET_OF_SPHERE_COLOR_RGB_IN_SHAPES = 6;
	private static final int RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES = 2;
	private static final int RELATIVE_OFFSET_OF_SPHERE_RADIUS_SCALAR_IN_SHAPES = 5;
	private static final int SIZE_OF_CAMERA = 3 + 3 + 3 + 3 + 3 + 3 + 1;
	private static final int SIZE_OF_INTERSECTION_IN_INTERSECTIONS = 1 + 1;
	private static final int SIZE_OF_RAY_IN_RAYS = 3 + 3;
	private static final int SIZE_OF_SPHERE_IN_SHAPES = 1 + 1 + 3 + 1 + 3;
	private static final int WIDTH = 1024;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final boolean[] isKeyPressed;
	private final BufferedImage bufferedImage;
	private final Camera camera;
	private final float[] cameraArray;
	private final float[] intersections;
	private final float[] rays;
	private final float[] shapes;
	private final FPSCounter fPSCounter;
	private final int height;
	private final int shapesLength;
	private final int width;
	private final int[] rGB;
	private final JFrame jFrame;
	private final Range range;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private RayCaster() {
		this.bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		this.camera = new Camera();
		this.cameraArray = this.camera.getArray();
		this.fPSCounter = new FPSCounter();
		this.height = this.bufferedImage.getHeight();
		this.intersections = doCreateIntersections(this.bufferedImage.getWidth() * this.bufferedImage.getHeight());
		this.isKeyPressed = new boolean[256];
		this.jFrame = doCreateJFrame(this.bufferedImage, this.camera, this.fPSCounter);
		this.range = Range.create(this.bufferedImage.getWidth() * this.bufferedImage.getHeight());
		this.rays = doCreateRays(this.bufferedImage.getWidth() * this.bufferedImage.getHeight());
		this.rGB = toRGB(this.bufferedImage);
		this.shapes = doCreateScene().toFloatArray();
		this.shapesLength = this.shapes.length;
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
//		Initialize index, coordinate and offset values:
		final int index = getGlobalId();
		final int intersectionOffset = index * SIZE_OF_INTERSECTION_IN_INTERSECTIONS;
		final int raysOffset = index * SIZE_OF_RAY_IN_RAYS;
		
//		Initialize the U- and V-coordinates:
		final float u = index % this.width - this.width / 2.0F + 0.5F;
		final float v = index / this.width - this.height / 2.0F + 0.5F;
		
//		Update the origin point and direction vector of the ray to fire:
		this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 0] = this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 0];
		this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 1] = this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 1];
		this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 2] = this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_EYE_POINT + 2];
		this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 0] = this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR + 0] * u + this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR + 0] * v - this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR + 0] * this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE_SCALAR];
		this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 1] = this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR + 1] * u + this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR + 1] * v - this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR + 1] * this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE_SCALAR];
		this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 2] = this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_U_VECTOR + 2] * u + this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_V_VECTOR + 2] * v - this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_ORTHONORMAL_BASIS_W_VECTOR + 2] * this.cameraArray[ABSOLUTE_OFFSET_OF_CAMERA_VIEW_PLANE_DISTANCE_SCALAR];
		
//		Normalize the ray direction vector:
		doNormalize(this.rays, raysOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS);
		
//		Calculate the distance to the closest sphere:
		final float distance = doGetIntersection();
		
		int shapeOffset = -1;
		
		int r = 0;
		int g = 0;
		int b = 0;
		
		if(distance > 0.0F && distance < MAXIMUM_DISTANCE) {
//			Fetch the offset of the intersected shape from the intersections array:
			shapeOffset = (int)(this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS]);
			
//			Update the RGB-values of the current pixel, given the RGB-values of the intersected shape:
			r = (int)(this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SPHERE_COLOR_RGB_IN_SHAPES + 0]);
			g = (int)(this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SPHERE_COLOR_RGB_IN_SHAPES + 1]);
			b = (int)(this.shapes[shapeOffset + RELATIVE_OFFSET_OF_SPHERE_COLOR_RGB_IN_SHAPES + 2]);
		}
		
//		Set the RGB-value of the current pixel:
		this.rGB[index] = doToRGB(r, g, b);
	}
	
	/**
	 * Called to start the Ray Caster.
	 */
	public void start() {
//		Add a KeyListener to the JFrame:
		doInvokeAndWait(() -> this.jFrame.addKeyListener(this));
		
//		Make this Kernel instance explicit, such that we have to take care of all array transfers to and from the GPU:
		setExplicit(true);
		
//		Tell the API to fetch the below arrays and their values before executing this Kernel instance (they will be transferred to the GPU):
		put(this.intersections);
		put(this.rays);
		put(this.shapes);
		put(this.rGB);
		
		while(true) {
//			Update the current frame:
			doUpdate();
			
//			Tell the API to fetch the camera array and its values before executing this Kernel instance (it will be transferred to the GPU every cycle):
			put(this.cameraArray);
			
//			Execute this Kernel instance:
			execute(this.range);
			
//			Fetch the RGB-values calculated in the GPU to the rGB array, so we can display the result:
			get(this.rGB);
			
//			Tell the JFrame to repaint itself:
			this.jFrame.repaint();
			
//			Update the FPS in the FPSCounter:
			this.fPSCounter.update();
		}
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
	
	private float doGetIntersection() {
//		Initialize the index and offset values:
		final int index = getGlobalId();
		final int intersectionOffset = index * SIZE_OF_INTERSECTION_IN_INTERSECTIONS;
		final int raysOffset = index * SIZE_OF_RAY_IN_RAYS;
		
//		Initialize offset to closest shape:
		int shapeClosestOffset = -1;
		
//		Initialize distance to closest shape:
		float shapeClosestDistance = MAXIMUM_DISTANCE;
		
//		Initialize the ray values:
		final float rayOriginX = this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 0];
		final float rayOriginY = this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 1];
		final float rayOriginZ = this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_ORIGIN_POINT_IN_RAYS + 2];
		final float rayDirectionX = this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 0];
		final float rayDirectionY = this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 1];
		final float rayDirectionZ = this.rays[raysOffset + RELATIVE_OFFSET_OF_RAY_DIRECTION_VECTOR_IN_RAYS + 2];
		
//		Initialize the temporary shape intersection values:
		float shapeType = 0.0F;
		float shapeSize = 0.0F;
		float shapeDistance = MAXIMUM_DISTANCE;
		float sphereX = 0.0F;
		float sphereY = 0.0F;
		float sphereZ = 0.0F;
		float sphereRadius = 0.0F;
		
//		Initialize the temporary delta values:
		float dx = 0.0F;
		float dy = 0.0F;
		float dz = 0.0F;
		
//		Initialize other temporarily used values:
		float b = 0.0F;
		float discriminant = 0.0F;
		
//		Reset the float array spheresIntersected, so we can perform a new intersection test:
		this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS] = -1.0F;
		this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE_SCALAR_IN_INTERSECTIONS] = MAXIMUM_DISTANCE;
		
		for(int i = 0; i < this.shapesLength; i += shapeSize) {
//			Update the temporary type and size values of the current shape to our temporary variables:
			shapeType = this.shapes[i + RELATIVE_OFFSET_OF_SHAPE_TYPE_SCALAR_IN_SHAPES];
			shapeSize = this.shapes[i + RELATIVE_OFFSET_OF_SHAPE_SIZE_SCALAR_IN_SHAPES];
			
			if(shapeType == TYPE_SPHERE) {
//				Update the temporary X-, Y-, Z- and radius values of the current sphere to our temporary variables:
				sphereX = this.shapes[i + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 0];
				sphereY = this.shapes[i + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 1];
				sphereZ = this.shapes[i + RELATIVE_OFFSET_OF_SPHERE_POSITION_POINT_IN_SHAPES + 2];
				sphereRadius = this.shapes[i + RELATIVE_OFFSET_OF_SPHERE_RADIUS_SCALAR_IN_SHAPES];
				
//				Calculate the delta values between the current sphere and the origin of the camera:
				dx = sphereX - rayOriginX;
				dy = sphereY - rayOriginY;
				dz = sphereZ - rayOriginZ;
				
//				Calculate the dot product:
				b = dx * rayDirectionX + dy * rayDirectionY + dz * rayDirectionZ;
				
//				Calculate the discriminant:
				discriminant = b * b - (dx * dx + dy * dy + dz * dz) + sphereRadius * sphereRadius;
				
				if(discriminant >= 0.0F) {
//					Recalculate the discriminant:
					discriminant = sqrt(discriminant);
					
//					Calculate the distance:
					shapeDistance = b - discriminant;
					
					if(shapeDistance <= EPSILON) {
//						Recalculate the distance:
						shapeDistance = b + discriminant;
						
						if(shapeDistance <= EPSILON) {
//							We're too close to the shape, so we practically do not see it:
							shapeDistance = 0.0F;
						}
					}
				}
			}
			
			if(shapeDistance > 0.0F && shapeDistance < shapeClosestDistance) {
//				Update the distance to and the offset of the closest shape:
				shapeClosestDistance = shapeDistance;
				shapeClosestOffset = i;
			}
		}
		
		if(shapeClosestOffset > -1) {
			this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS] = shapeClosestOffset;
			this.intersections[intersectionOffset + RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE_SCALAR_IN_INTERSECTIONS] = shapeClosestDistance;
		}
		
		return shapeClosestDistance;
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
		
		if(this.isKeyPressed[KeyEvent.VK_S]) {
			this.camera.move(0.0F, 0.0F, movement);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_W]) {
			this.camera.move(0.0F, 0.0F, -movement);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static int[] toRGB(final BufferedImage bufferedImage) {
		final WritableRaster writableRaster = bufferedImage.getRaster();
		
		final DataBuffer dataBuffer = writableRaster.getDataBuffer();
		
		final DataBufferInt dataBufferInt = DataBufferInt.class.cast(dataBuffer);
		
		final int[] rGB = dataBufferInt.getData();
		
		return rGB;
	}
	
	private static float doDotProduct(final float[] vector0, final int offset0, final float[] vector1, final int offset1) {
		return vector0[offset0] * vector1[offset1] + vector0[offset0 + 1] * vector1[offset1 + 1] + vector0[offset0 + 2] * vector1[offset1 + 2];
	}
	
	private static float doLengthSquared(final float[] vector, final int offset) {
		return doDotProduct(vector, offset, vector, offset);
	}
	
	private static float[] doCreateIntersections(final int length) {
		final float[] intersections = new float[length * SIZE_OF_INTERSECTION_IN_INTERSECTIONS];
		
		for(int i = 0; i < intersections.length; i += SIZE_OF_INTERSECTION_IN_INTERSECTIONS) {
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS] = -1.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE_SCALAR_IN_INTERSECTIONS] = MAXIMUM_DISTANCE;
		}
		
		return intersections;
	}
	
	private static float[] doCreateRays(final int length) {
		return new float[length * SIZE_OF_RAY_IN_RAYS];
	}
	
	@SuppressWarnings("unused")
	private static int doToB(final int rGB) {
		return (rGB >> 0) & 0xFF;
	}
	
	@SuppressWarnings("unused")
	private static int doToG(final int rGB) {
		return (rGB >> 8) & 0xFF;
	}
	
	@SuppressWarnings("unused")
	private static int doToR(final int rGB) {
		return (rGB >> 16) & 0xFF;
	}
	
	private static int doToRGB(final int r, final int g, final int b) {
		return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
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
		scene.addShape(new Sphere(27.0F, 16.5F, 47.0F, 16.5F, 100.0F, 200.0F, 255.0F));
		scene.addShape(new Sphere(73.0F, 16.5F, 78.0F, 16.5F, 255.0F, 200.0F, 100.0F));
		
		return scene;
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
			setEye(0.0F, 0.0F, 100.0F);
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
	
	private static final class Scene {
		private final List<Shape> shapes = new ArrayList<>();
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public Scene() {
			
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public float[] toFloatArray() {
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
}