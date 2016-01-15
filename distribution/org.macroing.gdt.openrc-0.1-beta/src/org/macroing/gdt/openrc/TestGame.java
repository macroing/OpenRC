/**
 * Copyright 2009 - 2016 J&#246;rgen Lundgren
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
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import org.macroing.gdt.openrc.geometry.Camera;
import org.macroing.gdt.openrc.geometry.Scene;
import org.macroing.gdt.openrc.geometry.Shape;
import org.macroing.gdt.openrc.geometry.Sphere;
import org.macroing.gdt.openrc.geometry.Texture;
import org.macroing.gdt.openrc.swing.SwingUtilities2;

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
 * Supported Features:
 * <ul>
 * <li>The Ray Casting algorithm.</li>
 * <li>Shapes such as planes, spheres and triangles.</li>
 * <li>Lights such as point lights.</li>
 * <li>Textures such as solid- and decal textures.</li>
 * <li>Texture mapping such as spherical- and planar triangle texture mapping.</li>
 * <li>A simple camera for walking around in the scene.</li>
 * <li>Simple materials.</li>
 * <li>Occluding shapes create shadows.</li>
 * <li>Simple collision detection.</li>
 * <li>Simple tone mapping and gamma correction.</li>
 * </ul>
 * <p>
 * Supported Controls:
 * <ul>
 * <li>A - Move left.</li>
 * <li>D - Move right.</li>
 * <li>E - Display the current execution mode to standard output.</li>
 * <li>ESC - Exit the program. You may have to press a few times if you're using the execution mode JTP (Java Thread Pool), as it's pretty unresponsive.</li>
 * <li>F - Fire invisible bullets to make the shapes bleed.</li>
 * <li>MOUSE - Look around.</li>
 * <li>S - Move backward.</li>
 * <li>T - Toggle between the two execution modes GPU and JTP (Java Thread Pool).</li>
 * <li>W - Move forward.</li>
 * </ul>
 * <p>
 * Note: More supported shapes, lights, materials, textures and texture mapping algorithms may very well be added in the future. The simple camera may be updated to support walking- and looking around like in an FPS-game.
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public final class TestGame extends Application {
	private final AtomicBoolean isPrintingExecutionMode = new AtomicBoolean();
	private final AtomicBoolean isRunning = new AtomicBoolean();
	private final AtomicBoolean isTerminationRequested = new AtomicBoolean();
	private final AtomicBoolean isTogglingExecutionMode = new AtomicBoolean();
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private TestGame(final Scene scene) {
		super(scene);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public void render(final Graphics2D graphics2D) {
		final Camera camera = getScene().getCamera();
		
		final FPSCounter fPSCounter = getFPSCounter();
		
		final String fPS = Long.toString(fPSCounter.getFPS());
		final String x = Float.toString(camera.getEyeX());
		final String y = Float.toString(camera.getEyeY());
		final String z = Float.toString(camera.getEyeZ());
		final String string = String.format("FPS: %s    Location: %s, %s, %s", fPS, x, y, z);
		
		graphics2D.setColor(Color.BLACK);
		graphics2D.fillRect(10, 10, graphics2D.getFontMetrics().stringWidth(string) + 20, graphics2D.getFontMetrics().getHeight() + 20);
		graphics2D.setColor(Color.WHITE);
		graphics2D.drawRect(10, 10, graphics2D.getFontMetrics().stringWidth(string) + 20, graphics2D.getFontMetrics().getHeight() + 20);
		graphics2D.drawString(string, 20, 30);
	}
	
	@Override
	public void update() {
//		Calculate the movement based on some velocity, calculated as the distance moved per second:
		final float velocity = 250.0F;
		final float movement = getFPSCounter().getFrameTimeMillis() / 1000.0F * velocity;
		
		final Camera camera = getScene().getCamera();
		
		camera.lookDown(-getMouseUpAndReset() * 0.005F);
		camera.rotateY(-getMouseLeftAndReset() * 0.005F);
		
		if(isKeyPressed(KeyEvent.VK_A)) {
			camera.moveLeft(movement);
		}
		
		if(isKeyPressed(KeyEvent.VK_D)) {
			camera.moveLeft(-movement);
		}
		
		if(isKeyPressed(KeyEvent.VK_E) && this.isPrintingExecutionMode.compareAndSet(false, true)) {
			System.out.printf("ExecutionMode: %s%n", getKernel().getExecutionMode());
		} else if(!isKeyPressed(KeyEvent.VK_E)) {
			this.isPrintingExecutionMode.compareAndSet(true, false);
		}
		
		if(isKeyPressed(KeyEvent.VK_ESCAPE) && this.isTerminationRequested.compareAndSet(false, true)) {
			this.isRunning.set(false);
			
			System.exit(0);
		} else if(!isKeyPressed(KeyEvent.VK_ESCAPE)) {
			this.isTerminationRequested.compareAndSet(true, false);
		}
		
		if(isKeyPressed(KeyEvent.VK_F)) {
			final int textureOffset = (int)(getPick()[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_OFFSET]);
			final int textureU = (int)(getPick()[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_UV + 0]);
			final int textureV = (int)(getPick()[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_UV + 1]);
			
			final int[] textures = getScene().getTexturesAsArray();
			
			final int width = textures[textureOffset + Texture.RELATIVE_OFFSET_OF_WIDTH];
			final int startX = textureU - 5;
			final int startY = textureV - 5;
			final int radius = 2;
			
			for(int y = -radius; y <= radius; y++) {
				for(int x = -radius; x <= radius; x++) {
					if(x * x + y * y <= radius * radius) {
						if(ThreadLocalRandom.current().nextGaussian() < 0.1D) {
							final int rGB = ((ThreadLocalRandom.current().nextInt(100, 255) & 0xFF) << 16) | ((0 & 0xFF) << 8) | ((0 & 0xFF) << 0);
							
							final int offset = (startY + y) * width + (startX + x);
							
							textures[textureOffset + Texture.RELATIVE_OFFSET_OF_DATA + offset] = rGB;
						}
					}
				}
			}
			
			setTextureUpdateRequired(true);
		}
		
		if(isKeyPressed(KeyEvent.VK_S)) {
			camera.moveBackward(-movement);
		}
		
		if(isKeyPressed(KeyEvent.VK_T) && this.isTogglingExecutionMode.compareAndSet(false, true)) {
			getKernel().setExecutionMode(getKernel().getExecutionMode() == EXECUTION_MODE.GPU ? EXECUTION_MODE.JTP : EXECUTION_MODE.GPU);
		} else if(!isKeyPressed(KeyEvent.VK_T)) {
			this.isTogglingExecutionMode.compareAndSet(true, false);
		}
		
		if(isKeyPressed(KeyEvent.VK_W)) {
			camera.moveBackward(movement);
		}
		
		final Scene scene = getScene();
		
		final float[] lights = scene.getLightsAsArray();
		
		lights[2] = camera.getEyeX() + (camera.getLookAtX() - camera.getEyeX()) * 50.0F;
		lights[3] = camera.getEyeY() + (camera.getLookAtY() - camera.getEyeY()) * 50.0F;
		lights[4] = camera.getEyeZ() + (camera.getLookAtZ() - camera.getEyeZ()) * 50.0F;
		
		setLightUpdateRequired(true);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * The entry-point of this application.
	 * 
	 * @param args these are not used
	 */
	public static void main(final String[] args) {
		final Scene scene = createScene(args);
		
		final
		Application application = SwingUtilities2.runInEDT(() -> new TestGame(scene));
		application.start();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static Scene createScene(final String[] args) {
		final File file = args.length > 0 ? new File(args[0]) : null;
		
		return file != null && file.exists() ? Scene.read(file) : Scene.create(new Camera((x, y, z, scene) -> {
			final boolean[] test = new boolean[] {true, true, true};
			
			if(scene != null) {
				for(final Shape shape : scene.getShapesAsList()) {
					if(shape instanceof Sphere) {
						final Sphere sphere = Sphere.class.cast(shape);
						
						final float radius = sphere.getRadius();
						
						final float x0 = sphere.getX();
						final float y0 = sphere.getY();
						final float z0 = sphere.getZ();
						
						final float dX = x - x0;
						final float dY = y - y0;
						final float dZ = z - z0;
						
						final float distance = Mathematics.sqrt(dX * dX + dY * dY + dZ * dZ);
						
						if(distance <= radius) {
							test[0] = false;
							test[1] = false;
							test[2] = false;
							
							break;
						}
					}
				}
			}
			
			return test;
		}));//TODO: Add collision detection using the CameraPredicate in the future!
	}
}