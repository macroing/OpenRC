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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;
import com.amd.aparapi.Kernel.EXECUTION_MODE;

/**
 * This is the main class of OpenRC. This may, however, change in the future, as OpenRC matures.
 * <p>
 * OpenRC (short for Open Ray Caster) is, as its name suggests, an open source program. But it's not just any program. It's a program for rendering 3D scenes using the Ray Casting algorithm in realtime on the GPU.
 * <p>
 * Because of the name, Open Ray Caster, some of you might believe Ray Casting is the only goal with this project. This is, however, not true. Additional algorithms, such as Whitted Ray Tracing and Path Tracing, are likely to be implemented.
 * <p>
 * The program runs a portion of its code on the GPU to speed things up, as previously mentioned. It does this using OpenCL, via a Java library called Aparapi. So OpenRC is written exclusively in Java. At least for now.
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
 * <li>Textures such as normal solid colored textures.</li>
 * <li>Texture mapping such as spherical texture mapping.</li>
 * <li>The Ray Casting algorithm.</li>
 * </ul>
 * <p>
 * Note that the features above will probably be expanded upon with time.
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public final class Application implements KeyListener {
	private final AtomicBoolean isPrintingExecutionMode = new AtomicBoolean();
	private final AtomicBoolean isRunning = new AtomicBoolean();
	private final AtomicBoolean isTerminationRequested = new AtomicBoolean();
	private final AtomicBoolean isTogglingExecutionMode = new AtomicBoolean();
	private final boolean[] isKeyPressed = new boolean[1024];
	private final BufferedImage bufferedImage = new BufferedImage(Constants.WIDTH, Constants.HEIGHT, BufferedImage.TYPE_INT_RGB);
	private final FPSCounter fPSCounter = new FPSCounter();
	private final int[] rGB;
	private final JFrame jFrame;
	private final Kernel kernel;
	private final Range range = Range.create(Constants.WIDTH * Constants.HEIGHT);
	private final Scene scene = Scene.create();
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Application() {
		this.rGB = doToRGB(this.bufferedImage);
		this.jFrame = doCreateJFrame(this.bufferedImage, this.scene.getCamera(), this.fPSCounter);
		this.kernel = new RayCasterKernel(this.rGB, this.scene);
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
	 * Called to start the application.
	 */
	public void start() {
//		Add a shutdown hook that stops the execution and subsequently disposes of any resources:
		Runtime.getRuntime().addShutdownHook(new Thread(() -> this.isRunning.set(false)));
		
//		Initialize the field isRunning to true:
		this.isRunning.set(true);
		
//		Add a KeyListener to the JFrame:
		SwingUtilities2.invokeAndWait(() -> this.jFrame.addKeyListener(this));
		
		while(this.isRunning.get()) {
//			Update the current frame:
			doUpdate();
			
//			Tell the API to fetch the camera values before executing this Kernel instance (it will be transferred to the GPU every cycle):
			this.kernel.put(this.scene.getCamera().getArray());
			
//			Tell the API to fetch the shape indices before executing this Kernel instance (it will be transferred to the GPU every cycle):
			this.kernel.put(this.scene.getShapeIndices());
			
//			Execute this Kernel instance:
			this.kernel.execute(this.range);
			
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
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * The entry-point of this application.
	 * 
	 * @param args these are not used
	 */
	public static void main(final String[] args) {
		final
		Application application = SwingUtilities2.runInEDT(() -> new Application());
		application.start();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private void doPerformFrustumCulling() {
//		TODO: Implement View Frustum Culling here.
		
		final int[] shapeIndices = this.scene.getShapeIndices();
		
		final List<Shape> shapes = this.scene.getShapesAsList();
		
		for(int i = 0; i < shapeIndices.length; i++) {
			shapeIndices[i] = shapes.get(i).getIndex();
		}
	}
	
	private void doUpdate() {
//		Calculate the movement based on some velocity, calculated as the distance moved per second:
		final float velocity = 250.0F;
		final float movement = this.fPSCounter.getFrameTimeMillis() / 1000.0F * velocity;
		
		final Camera camera = this.scene.getCamera();
		
		if(this.isKeyPressed[KeyEvent.VK_A]) {
			camera.move(-movement, 0.0F, 0.0F);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_D]) {
			camera.move(movement, 0.0F, 0.0F);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_E] && this.isPrintingExecutionMode.compareAndSet(false, true)) {
			System.out.printf("ExecutionMode: %s%n", this.kernel.getExecutionMode());
		} else if(!this.isKeyPressed[KeyEvent.VK_E]) {
			this.isPrintingExecutionMode.compareAndSet(true, false);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_ESCAPE] && this.isTerminationRequested.compareAndSet(false, true)) {
			this.isRunning.set(false);
		} else if(!this.isKeyPressed[KeyEvent.VK_ESCAPE]) {
			this.isTerminationRequested.compareAndSet(true, false);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_S]) {
			camera.move(0.0F, 0.0F, movement);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_T] && this.isTogglingExecutionMode.compareAndSet(false, true)) {
			this.kernel.setExecutionMode(this.kernel.getExecutionMode() == EXECUTION_MODE.GPU ? EXECUTION_MODE.JTP : EXECUTION_MODE.GPU);
		} else if(!this.isKeyPressed[KeyEvent.VK_T]) {
			this.isTogglingExecutionMode.compareAndSet(true, false);
		}
		
		if(this.isKeyPressed[KeyEvent.VK_W]) {
			camera.move(0.0F, 0.0F, -movement);
		}
		
//		Perform View Frustum Culling:
		doPerformFrustumCulling();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
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
		jFrame.setTitle(String.format("OpenRC v%s", Constants.getVersion()));
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
}