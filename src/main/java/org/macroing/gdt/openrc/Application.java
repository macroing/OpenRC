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

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

abstract class Application implements KeyListener, MouseMotionListener {
	private final AtomicBoolean isRecenteringMouse = new AtomicBoolean(true);
	private final AtomicBoolean isRunning = new AtomicBoolean();
	private final AtomicBoolean isTextureUpdateRequired = new AtomicBoolean();
	private final AtomicInteger mouseLeft = new AtomicInteger();
	private final AtomicInteger mouseUp = new AtomicInteger();
	private final boolean[] isKeyPressed = new boolean[1024];
	private final BufferedImage bufferedImage = new BufferedImage(Constants.WIDTH, Constants.HEIGHT, BufferedImage.TYPE_INT_RGB);
	private final float[] pick = new float[Constants.SIZE_OF_PICK];
	private final FPSCounter fPSCounter = new FPSCounter();
	private final int[] rGB;
	private final JFrame jFrame;
	private final Kernel kernel;
	private final Point centerPoint = new Point();
	private final Range range = Range.create(Constants.WIDTH * Constants.HEIGHT);
	private final Robot robot = doCreateRobot();
	private final Scene scene;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	protected Application(final Scene scene) {
		this.rGB = doToRGB(this.bufferedImage);
		this.scene = scene;
		this.jFrame = doCreateJFrame(this.bufferedImage, this.scene.getCamera(), this::render, this.fPSCounter);
		this.kernel = new RayCasterKernel(this.pick, this.rGB, this.scene);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public final boolean isKeyPressed(final int keyCode) {
		return this.isKeyPressed[keyCode];
	}
	
	public final float[] getPick() {
		return this.pick;
	}
	
	public final FPSCounter getFPSCounter() {
		return this.fPSCounter;
	}
	
	public final int getMouseLeftAndReset() {
		return this.mouseLeft.getAndSet(0);
	}
	
	public final int getMouseUpAndReset() {
		return this.mouseUp.getAndSet(0);
	}
	
	public final Kernel getKernel() {
		return this.kernel;
	}
	
	public final Scene getScene() {
		return this.scene;
	}
	
	/**
	 * Overridden to handle key typing.
	 * 
	 * @param e a {@code KeyEvent}
	 */
	@Override
	public final void keyTyped(final KeyEvent e) {
//		Do nothing here.
	}
	
	/**
	 * Overridden to handle key pressing.
	 * 
	 * @param e a {@code KeyEvent}
	 */
	@Override
	public final void keyPressed(final KeyEvent e) {
		this.isKeyPressed[e.getKeyCode()] = true;
	}
	
	/**
	 * Overridden to handle key releasing.
	 * 
	 * @param e a {@code KeyEvent}
	 */
	@Override
	public final void keyReleased(final KeyEvent e) {
		this.isKeyPressed[e.getKeyCode()] = false;
	}
	
	/**
	 * Overridden to handle mouse dragging.
	 * 
	 * @param e a {@code MouseEvent}
	 */
	@Override
	public void mouseDragged(final MouseEvent e) {
		doMoveMouse(e);
	}
	
	/**
	 * Overridden to handle mouse movement.
	 * 
	 * @param e a {@code MouseEvent}
	 */
	@Override
	public void mouseMoved(final MouseEvent e) {
		doMoveMouse(e);
	}
	
	public final void setTextureUpdateRequired(final boolean isTextureUpdateRequired) {
		this.isTextureUpdateRequired.set(isTextureUpdateRequired);
	}
	
	public abstract void render(final Graphics2D graphics2D);
	
	/**
	 * Called to start the application.
	 */
	public final void start() {
//		Add a shutdown hook that stops the execution and subsequently disposes of any resources:
		Runtime.getRuntime().addShutdownHook(new Thread(() -> this.isRunning.set(false)));
		
//		Initialize the field isRunning to true:
		this.isRunning.set(true);
		
//		Add a KeyListener and a MouseMotionListener to the JFrame:
		SwingUtilities2.invokeAndWait(() -> {
			this.jFrame.addKeyListener(this);
			this.jFrame.addMouseMotionListener(this);
		});
		
		while(this.isRunning.get()) {
//			Update the current frame:
			update();
			
//			Perform View Frustum Culling:
			doPerformFrustumCulling();
			
//			Tell the API to fetch the camera values before executing this Kernel instance (it will be transferred to the GPU every cycle):
			this.kernel.put(this.scene.getCamera().getArray());
			
//			Tell the API to fetch the shape indices before executing this Kernel instance (it will be transferred to the GPU every cycle):
			this.kernel.put(this.scene.getShapeIndices());
			
			if(this.isTextureUpdateRequired.compareAndSet(true, false)) {
				this.kernel.put(this.scene.getTexturesAsArray());
			}
			
//			Execute this Kernel instance:
			this.kernel.execute(this.range);
			
//			Fetch the pick result:
			this.kernel.get(this.pick);
			
//			Fetch the RGB-values calculated in the GPU to the rGB array, so we can display the result:
			this.kernel.get(this.rGB);
			
//			Tell the JFrame to repaint itself:
			this.jFrame.repaint();
			
//			Update the FPS in the FPSCounter:
			this.fPSCounter.update();
		}
		
//		Tell the Kernel to dispose of any resources used.
		this.kernel.dispose();
		
//		Tell the JFrame to dispose of any resources used.
		this.jFrame.dispose();
	}
	
	public abstract void update();
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private void doMoveMouse(final MouseEvent e) {
		if(this.jFrame.isActive()) {
			if(this.isRecenteringMouse.get() && this.centerPoint.x == e.getXOnScreen() && this.centerPoint.y == e.getYOnScreen()) {
				this.isRecenteringMouse.set(false);
			} else {
				final int x = e.getXOnScreen();
				final int y = e.getYOnScreen();
				final int deltaX = x - this.centerPoint.x;
				final int deltaY = y - this.centerPoint.y;
				final int amountX = Math.abs(deltaX);
				final int amountY = Math.abs(deltaY);
				
				doRecenterMouse();
				
				if(deltaX < 0) {
					this.mouseLeft.addAndGet(amountX);
				} else if(deltaX > 0) {
					this.mouseLeft.addAndGet(-amountX);
				}
				
				if(deltaY < 0) {
					this.mouseUp.addAndGet(amountY);
				} else if(deltaY > 0) {
					this.mouseUp.addAndGet(-amountY);
				}
			}
		}
	}
	
	private void doPerformFrustumCulling() {
//		TODO: Implement View Frustum Culling here.
		
		final int[] shapeIndices = this.scene.getShapeIndices();
		
		final List<Shape> shapes = this.scene.getShapesAsList();
		
		for(int i = 0; i < shapeIndices.length; i++) {
			shapeIndices[i] = shapes.get(i).getIndex();
		}
	}
	
	private void doRecenterMouse() {
		this.centerPoint.x = this.jFrame.getWidth() / 2;
		this.centerPoint.y = this.jFrame.getHeight() / 2;
		
		SwingUtilities.convertPointToScreen(this.centerPoint, this.jFrame);
		
		this.isRecenteringMouse.set(true);
		
		try {
			this.robot.mouseMove(this.centerPoint.x, this.centerPoint.y);
		} catch(final HeadlessException | NullPointerException | SecurityException e) {
			throw new UnsupportedOperationException("This method is not supported by the current configuration.");
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static int[] doToRGB(final BufferedImage bufferedImage) {
		final WritableRaster writableRaster = bufferedImage.getRaster();
		
		final DataBuffer dataBuffer = writableRaster.getDataBuffer();
		
		final DataBufferInt dataBufferInt = DataBufferInt.class.cast(dataBuffer);
		
		final int[] rGB = dataBufferInt.getData();
		
		return rGB;
	}
	
	private static JFrame doCreateJFrame(final BufferedImage bufferedImage, final Camera camera, final Consumer<Graphics2D> consumer, final FPSCounter fPSCounter) {
		final
		JFrame jFrame = new JFrame();
		jFrame.setContentPane(doCreateJPanel(bufferedImage, camera, consumer, fPSCounter));
		jFrame.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(Toolkit.getDefaultToolkit().getImage(""), new Point(0, 0), "invisible"));
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jFrame.setFocusTraversalKeysEnabled(false);
		jFrame.setIgnoreRepaint(true);
		jFrame.setSize(bufferedImage.getWidth(), bufferedImage.getHeight());
		jFrame.setLocationRelativeTo(null);
		jFrame.setTitle(String.format("OpenRC v%s", Constants.getVersion()));
		jFrame.setVisible(true);
		jFrame.createBufferStrategy(2);
		jFrame.repaint();
		
		return jFrame;
	}
	
	private static JPanel doCreateJPanel(final BufferedImage bufferedImage, final Camera camera, final Consumer<Graphics2D> consumer, final FPSCounter fPSCounter) {
		final
		JPanel jPanel = new JBufferedImagePanel(bufferedImage, camera, consumer, fPSCounter);
		jPanel.setLayout(new AbsoluteLayoutManager());
		jPanel.setPreferredSize(new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight()));
		
		return jPanel;
	}
	
	private static Robot doCreateRobot() {
		try {
			return new Robot();
		} catch(AWTException e) {
			return null;
		}
	}
}