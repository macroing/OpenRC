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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

final class Scene {
	private final AtomicInteger index = new AtomicInteger();
	private final Camera camera = new Camera();
	private final List<Light> lights = new ArrayList<>();
	private final List<Shape> shapes = new ArrayList<>();
	private final Texture texture = Texture.create("Texture.jpg");
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Scene() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Camera getCamera() {
		return this.camera;
	}
	
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
	
	public int getShapeCount() {
		return this.shapes.size();
	}
	
	public int[] createShapeIndices() {
		final int[] shapeIndices = new int[this.shapes.size()];
		
		for(int i = 0, j = 0; i < this.shapes.size(); i++) {
			final Shape shape = this.shapes.get(i);
			
			shapeIndices[i] = j;
			
			j += shape.size();
		}
		
		return shapeIndices;
	}
	
	public List<Shape> getShapes() {
		return this.shapes;
	}
	
	public Texture getTexture() {
		return this.texture;
	}
	
	public void addLight(final Light light) {
		this.lights.add(Objects.requireNonNull(light, "light == null"));
	}
	
	public void addShape(final Shape shape) {
		this.shapes.add(Objects.requireNonNull(shape, "shape == null"));
		
		shape.setIndex(this.index.getAndAdd(shape.size()));
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static Scene create() {
		final
		Scene scene = new Scene();
		scene.addLight(new PointLight(400.0F, 20.0F, 400.0F, 100.0F));
		scene.addLight(new PointLight(600.0F, 20.0F, 600.0F, 100.0F));
		scene.addLight(new PointLight(600.0F, 20.0F, 400.0F, 100.0F));
		scene.addLight(new PointLight(400.0F, 20.0F, 600.0F, 100.0F));
		
		for(int i = 0; i < 500; i++) {
			scene.addShape(doCreateRandomSphere());
		}
		
		scene.addShape(new Plane(1.0F, 0.0F, 0.0F));
		scene.addShape(new Triangle(2500.0F, 40.0F, 2500.0F, 1000.0F, 40.0F, 1500.0F, -1000.0F, 40.0F, -1000.0F));
		
		return scene;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static float doRandom(final float range) {
		return ThreadLocalRandom.current().nextFloat() * range;
	}
	
	private static Sphere doCreateRandomSphere() {
		return new Sphere(doRandom(4000.0F), 16.5F, doRandom(4000.0F), 16.5F, doRandom(255.0F), doRandom(255.0F), doRandom(255.0F));
	}
}