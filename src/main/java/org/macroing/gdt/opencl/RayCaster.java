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
 * The values in the {@code float} array {@code rays} consists of Origin X, Origin Y, Origin Z, Direction X, Direction Y and Direction Z, for each ray fired from each pixel.
 * <p>
 * The values in the {@code float} array {@code spheres} consists of X, Y, Z and Radius, for each sphere defined.
 * <p>
 * The values in the {@code float} array {@code spheresIntersected} consists of X, Y, Z, Radius and Distance (T), for each sphere currently being intersected by a ray.
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public final class RayCaster extends Kernel implements KeyListener {
	private static final int HEIGHT = 768;
	private static final int WIDTH = 1024;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final boolean[] isKeyPressed;
	private final BufferedImage bufferedImage;
	private final float[] camera;
	private final float[] rays;
	private final float[] spheres;
	private final float[] spheresIntersected;
	private final FPSCounter fPSCounter;
	private final int height;
	private final int spheresLength;
	private final int width;
	private final int[] rGB;
	private final JFrame jFrame;
	private final Range range;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private RayCaster() {
		this.bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		this.camera = doCreateCamera();
		this.fPSCounter = new FPSCounter();
		this.height = HEIGHT;
		this.isKeyPressed = new boolean[256];
		this.jFrame = doCreateJFrame(this.bufferedImage, this.camera, this.fPSCounter);
		this.range = Range.create(this.bufferedImage.getWidth() * this.bufferedImage.getHeight());
		this.rays = doCreateRays(WIDTH * HEIGHT);
		this.rGB = toRGB(this.bufferedImage);
		this.spheres = doCreateSpheres();
		this.spheresIntersected = doCreateSpheresIntersected(WIDTH * HEIGHT);
		this.spheresLength = this.spheres.length;
		this.width = WIDTH;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public void keyTyped(final KeyEvent e) {
		
	}
	
	@Override
	public void keyPressed(final KeyEvent e) {
		this.isKeyPressed[e.getKeyCode()] = true;
	}
	
	@Override
	public void keyReleased(final KeyEvent e) {
		this.isKeyPressed[e.getKeyCode()] = false;
	}
	
	@Override
	public void run() {
//		Initialize index, coordinate and offset values:
		final int index = getGlobalId();
		final int raysOffset = index * 6;
		
//		Initialize the U- and V-coordinates:
		final float x = index % this.width - this.width / 2.0F + 0.5F;
		final float y = index / this.width - this.height / 2.0F + 0.5F;
		
//		Update the origin point and direction vector of the ray to fire:
		this.rays[raysOffset + 0] = this.camera[ 0];
		this.rays[raysOffset + 1] = this.camera[ 1];
		this.rays[raysOffset + 2] = this.camera[ 2];
		this.rays[raysOffset + 3] = this.camera[ 9] * x + this.camera[12] * y - this.camera[15] * this.camera[18];
		this.rays[raysOffset + 4] = this.camera[10] * x + this.camera[13] * y - this.camera[16] * this.camera[18];
		this.rays[raysOffset + 5] = this.camera[11] * x + this.camera[14] * y - this.camera[17] * this.camera[18];
		
//		Normalize the ray direction:
		doNormalize(this.rays, raysOffset + 3);
		
//		Calculate the distance to the closest sphere:
		final float distance = doGetIntersection();
		
//		Update the RGB-values of the current pixel:
		final int r = distance > 0.0F && distance < Float.MAX_VALUE ? 255 : 0;
		final int g = distance > 0.0F && distance < Float.MAX_VALUE ? 255 : 0;
		final int b = distance > 0.0F && distance < Float.MAX_VALUE ? 255 : 0;
		
//		Set the RGB-value of the current pixel:
		this.rGB[index] = doToRGB(r, g, b);
	}
	
	public void start() {
//		Add a KeyListener to the JFrame:
		doInvokeAndWait(() -> this.jFrame.addKeyListener(this));
		
//		Make this Kernel instance explicit, such that we have to take care of all array transfers to and from the GPU:
		setExplicit(true);
		
//		Tell the API to fetch the below arrays and their values before executing this Kernel instance (they will be transferred to the GPU):
		put(this.rays);
		put(this.spheres);
		put(this.spheresIntersected);
		put(this.rGB);
		
//		Calculate the current OrthoNormal Basis vectors for the camera:
		doCalculateOrthonormalBasis();
		
		while(true) {
			doUpdate();
			
//			Tell the API to fetch the camera array and its values before executing this Kernel instance (it will be transferred to the GPU every cycle):
			put(this.camera);
			
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
	
	public static void main(final String[] args) {
		final
		RayCaster rayCaster = doRunInEDT(() -> new RayCaster());
		rayCaster.start();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private float doGetIntersection() {
//		Initialize the index and offset values:
		final int index = getGlobalId();
		final int raysOffset = index * 6;
		final int spheresIntersectedOffset = index * 5;
		
//		Initialize offset to closest sphere:
		int sphereClosestOffset = -1;
		
//		Initialize distance to closest sphere:
		float sphereClosestDistance = Float.MAX_VALUE;
		
//		Initialize the ray values:
		final float rayOriginX = this.rays[raysOffset + 0];
		final float rayOriginY = this.rays[raysOffset + 1];
		final float rayOriginZ = this.rays[raysOffset + 2];
		final float rayDirectionX = this.rays[raysOffset + 3];
		final float rayDirectionY = this.rays[raysOffset + 4];
		final float rayDirectionZ = this.rays[raysOffset + 5];
		
//		Initialize the temporary sphere intersection values:
		float sphereX = 0.0F;
		float sphereY = 0.0F;
		float sphereZ = 0.0F;
		float sphereRadius = 0.0F;
		float sphereDistance = Float.MAX_VALUE;
		
//		Initialize the temporary delta values:
		float dx = 0.0F;
		float dy = 0.0F;
		float dz = 0.0F;
		
//		Initialize the epsilon value:
		final float epsilon = 1.e-4F;
		
//		Initialize other temporarily used values:
		float b = 0.0F;
		float discriminant = 0.0F;
		
//		Reset the float array spheresIntersected, so we can perform a new intersection test:
		this.spheresIntersected[spheresIntersectedOffset + 0] = 0.0F;
		this.spheresIntersected[spheresIntersectedOffset + 1] = 0.0F;
		this.spheresIntersected[spheresIntersectedOffset + 2] = 0.0F;
		this.spheresIntersected[spheresIntersectedOffset + 3] = 0.0F;
		this.spheresIntersected[spheresIntersectedOffset + 4] = Float.MAX_VALUE;
		
//		TODO: Find out why 'this.spheres.length' cannot be compiled by Aparapi.
		for(int i = 0; i < this.spheresLength; i += 4) {
//			Update the temporary X-, Y-, Z- and radius values of the current sphere to our temporary variables:
			sphereX = this.spheres[i + 0];
			sphereY = this.spheres[i + 1];
			sphereZ = this.spheres[i + 2];
			sphereRadius = this.spheres[i + 3];
			
//			Calculate the delta values between the current sphere and the origin of the camera:
			dx = sphereX - rayOriginX;
			dy = sphereY - rayOriginY;
			dz = sphereZ - rayOriginZ;
			
			b = dx * rayDirectionX + dy * rayDirectionY + dz * rayDirectionZ;
			
			discriminant = b * b - (dx * dx + dy * dy + dz * dz) + sphereRadius * sphereRadius;
			
			if(discriminant >= 0.0F) {
				discriminant = sqrt(discriminant);
				
				sphereDistance = b - discriminant;
				
				if(sphereDistance <= epsilon) {
					sphereDistance = b + discriminant;
					
					if(sphereDistance <= epsilon) {
						sphereDistance = 0.0F;
					}
				}
			}
			
			if(sphereDistance > 0.0F && sphereDistance < sphereClosestDistance) {
				sphereClosestDistance = sphereDistance;
				sphereClosestOffset = i;
			}
		}
		
		if(sphereClosestOffset > -1) {
			this.spheresIntersected[spheresIntersectedOffset + 0] = this.spheres[sphereClosestOffset + 0];
			this.spheresIntersected[spheresIntersectedOffset + 1] = this.spheres[sphereClosestOffset + 1];
			this.spheresIntersected[spheresIntersectedOffset + 2] = this.spheres[sphereClosestOffset + 2];
			this.spheresIntersected[spheresIntersectedOffset + 3] = this.spheres[sphereClosestOffset + 3];
			this.spheresIntersected[spheresIntersectedOffset + 4] = sphereClosestDistance;
		}
		
		return sphereClosestDistance;
	}
	
	private float doLength(final float[] vector, final int offset) {
		return sqrt(doLengthSquared(vector, offset));
	}
	
	private void doCalculateOrthonormalBasis() {
		doSubtract(this.camera, 0, this.camera, 6, this.camera, 15);
		doNormalize(this.camera, 15);
		doCrossProduct(this.camera, 3, this.camera, 15, this.camera, 9);
		doNormalize(this.camera, 9);
		doCrossProduct(this.camera, 15, this.camera, 9, this.camera, 12);
	}
	
	private void doMoveCamera(final float x, final float y, final float z) {
		this.camera[0] += x;
		this.camera[1] += y;
		this.camera[2] += z;
		
		doCalculateOrthonormalBasis();
	}
	
	private void doNormalize(final float[] vector, final int offset) {
		final float lengthReciprocal = 1.0F / doLength(vector, offset);
		
		vector[offset + 0] *= lengthReciprocal;
		vector[offset + 1] *= lengthReciprocal;
		vector[offset + 2] *= lengthReciprocal;
	}
	
	private void doUpdate() {
		final float velocity = 250.0F;
		final float movement = this.fPSCounter.getFrameTimeMillis() / 1000.0F * velocity;
		
		if(this.isKeyPressed[KeyEvent.VK_A]) {
			doMoveCamera(-movement, 0.0F, 0.0F);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_D]) {
			doMoveCamera(movement, 0.0F, 0.0F);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_S]) {
			doMoveCamera(0.0F, 0.0F, movement);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_W]) {
			doMoveCamera(0.0F, 0.0F, -movement);
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
	
	private static float[] doCreateCamera() {
//		TODO: Find out whether Look-at Z and View-plane distance are correlated in some way!?
		final float[] camera = new float[] {
			0.0F, 0.0F, 100.0F,//Eye(0).
			0.0F, 1.0F, 0.0F,//Up(3).
			0.0F, 0.0F, -800.0F,//Look-at(6).
			0.0F, 0.0F, 0.0F,//Orthonormal-basis U(9).
			0.0F, 0.0F, 0.0F,//Orthonormal-basis V(12).
			0.0F, 0.0F, 0.0F,//Orthonormal-basis W(15).
			800.0F//View-plane distance(18).
		};
		
		return camera;
	}
	
	private static float[] doCreateRays(final int length) {
		return new float[length * 6];
	}
	
	private static float[] doCreateSpheres() {
		return new float[] {
//			1.e5F + 1.0F, 40.8F, 81.6F, 1.e5F,
//			-1.e5F + 99.0F, 40.8F, 81.6F, 1.e5F,
//			50.0F, 40.8F, 1.e5F, 1.e5F,
//			50.0F, 40.8F, -1.e5F + 170.0F, 1.e5F,
//			50.0F, 1.e5F, 81.6F, 1.e5F,
//			50.0F, -1.e5F + 81.6F, 81.6F, 1.e5F,
			27.0F, 16.5F, 47.0F, 16.5F,
			73.0F, 16.5F, 78.0F, 16.5F,
//			50.0F, 681.6F - 0.27F, 81.6F, 600.0F
		};
	}
	
	private static float[] doCreateSpheresIntersected(final int length) {
		final float[] spheresIntersected = new float[length * 5];
		
		for(int i = 0; i < spheresIntersected.length; i += 5) {
			spheresIntersected[i + 0] = 0.0F;
			spheresIntersected[i + 1] = 0.0F;
			spheresIntersected[i + 2] = 0.0F;
			spheresIntersected[i + 3] = 0.0F;
			spheresIntersected[i + 4] = Float.MAX_VALUE;
		}
		
		return spheresIntersected;
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
	
	private static JFrame doCreateJFrame(final BufferedImage bufferedImage, final float[] camera, final FPSCounter fPSCounter) {
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
	
	private static JPanel doCreateJPanel(final BufferedImage bufferedImage, final float[] camera, final FPSCounter fPSCounter) {
		final
		JPanel jPanel = new JBufferedImagePanel(bufferedImage, camera, fPSCounter);
		jPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
		jPanel.setLayout(new AbsoluteLayoutManager());
		jPanel.setPreferredSize(new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight()));
		
		return jPanel;
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
		private final float[] camera;
		private final FPSCounter fPSCounter;
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public JBufferedImagePanel(final BufferedImage bufferedImage, final float[] camera, final FPSCounter fPSCounter) {
			this.bufferedImage = bufferedImage;
			this.camera = camera;
			this.fPSCounter = fPSCounter;
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		@Override
		public void paintComponent(final Graphics graphics) {
			final String string = String.format("FPS: %s    Location: %s, %s, %s", Long.toString(this.fPSCounter.getFPS()), Float.toString(this.camera[0]), Float.toString(this.camera[1]), Float.toString(this.camera[2]));
			
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
}