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

import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

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
 * <li>Texture mapping such as spherical- and triangle texture mapping.</li>
 * <li>A simple camera for walking around in the scene.</li>
 * <li>Simple materials.</li>
 * <li>Occluding shapes create shadows.</li>
 * </ul>
 * <p>
 * Supported Controls:
 * <ul>
 * <li>A - Move left.</li>
 * <li>D - Move right.</li>
 * <li>DOWN ARROW - Look down.</li>
 * <li>E - Display the current execution mode to standard output.</li>
 * <li>ESC - Exit the program. You may have to press a few times if you're using the execution mode JTP (Java Thread Pool), as it's pretty unresponsive.</li>
 * <li>F - Fire invisible bullets to make the shapes bleed.</li>
 * <li>LEFT ARROW - Look left.</li>
 * <li>RIGHT ARROW - Look right.</li>
 * <li>S - Move backward.</li>
 * <li>T - Toggle between the two execution modes GPU and JTP (Java Thread Pool).</li>
 * <li>UP ARROW - Look up.</li>
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
	public void update() {
//		Calculate the movement based on some velocity, calculated as the distance moved per second:
		final float velocity = 250.0F;
		final float movement = getFPSCounter().getFrameTimeMillis() / 1000.0F * velocity;
		
		final Camera camera = getScene().getCamera();
		
		if(isKeyPressed(KeyEvent.VK_A)) {
			camera.move(-movement, 0.0F, movement);
		}
		
		if(isKeyPressed(KeyEvent.VK_D)) {
			camera.move(movement, 0.0F, -movement);
		}
		
		if(isKeyPressed(KeyEvent.VK_DOWN)) {
			camera.look(0.0F, movement, 0.0F);
		}
		
		if(isKeyPressed(KeyEvent.VK_E) && this.isPrintingExecutionMode.compareAndSet(false, true)) {
			System.out.printf("ExecutionMode: %s%n", getKernel().getExecutionMode());
		} else if(!isKeyPressed(KeyEvent.VK_E)) {
			this.isPrintingExecutionMode.compareAndSet(true, false);
		}
		
		if(isKeyPressed(KeyEvent.VK_ESCAPE) && this.isTerminationRequested.compareAndSet(false, true)) {
			this.isRunning.set(false);
		} else if(!isKeyPressed(KeyEvent.VK_ESCAPE)) {
			this.isTerminationRequested.compareAndSet(true, false);
		}
		
		if(isKeyPressed(KeyEvent.VK_F)) {
			final int textureOffset = (int)(getPick()[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_OFFSET]);
			final int textureU = (int)(getPick()[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_UV + 0]);
			final int textureV = (int)(getPick()[Constants.RELATIVE_OFFSET_OF_PICK_TEXTURE_UV + 1]);
			
			final int[] textures = getScene().getTexturesAsArray();
			
			final int width = textures[textureOffset + Texture.RELATIVE_OFFSET_OF_TEXTURE_WIDTH];
			final int startX = textureU - 5;
			final int startY = textureV - 5;
			final int radius = 2;
			
			for(int y = -radius; y <= radius; y++) {
				for(int x = -radius; x <= radius; x++) {
					if(x * x + y * y <= radius * radius) {
						if(ThreadLocalRandom.current().nextGaussian() < 0.1D) {
							final int rGB = ((ThreadLocalRandom.current().nextInt(100, 255) & 0xFF) << 16) | ((0 & 0xFF) << 8) | ((0 & 0xFF) << 0);
							
							final int offset = (startY + y) * width + (startX + x);
							
							textures[textureOffset + Texture.RELATIVE_OFFSET_OF_TEXTURE_DATA + offset] = rGB;
						}
					}
				}
			}
			
			setTextureUpdateRequired(true);
		}
		
		if(isKeyPressed(KeyEvent.VK_LEFT)) {
			camera.look(-movement, 0.0F, movement);
		}
		
		if(isKeyPressed(KeyEvent.VK_RIGHT)) {
			camera.look(movement, 0.0F, -movement);
		}
		
		if(isKeyPressed(KeyEvent.VK_S)) {
			camera.move(movement, 0.0F, movement);
		}
		
		if(isKeyPressed(KeyEvent.VK_T) && this.isTogglingExecutionMode.compareAndSet(false, true)) {
			getKernel().setExecutionMode(getKernel().getExecutionMode() == EXECUTION_MODE.GPU ? EXECUTION_MODE.JTP : EXECUTION_MODE.GPU);
		} else if(!isKeyPressed(KeyEvent.VK_T)) {
			this.isTogglingExecutionMode.compareAndSet(true, false);
		}
		
		if(isKeyPressed(KeyEvent.VK_UP)) {
			camera.look(0.0F, -movement, 0.0F);
		}
		
		if(isKeyPressed(KeyEvent.VK_W)) {
			camera.move(-movement, 0.0F, -movement);
		}
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
		
		return file != null && file.exists() ? Scene.read(file) : Scene.create();
	}
}