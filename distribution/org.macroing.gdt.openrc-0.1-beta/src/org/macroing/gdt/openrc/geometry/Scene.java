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
package org.macroing.gdt.openrc.geometry;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class Scene {
	private final Camera camera;
	private final float[] lightsAsArray;
	private final float[] materialsAsArray;
	private final float[] shapesAsArray;
	private final int[] shapeIndices;
	private final int[] texturesAsArray;
	private final List<Light> lightsAsList;
	private final List<Material> materialsAsList;
	private final List<Shape> shapesAsList;
	private final List<Texture> texturesAsList;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	Scene(final Camera camera, final float[] lightsAsArray, final float[] materialsAsArray, final float[] shapesAsArray, final int[] shapeIndices, final int[] texturesAsArray, final List<Light> lightsAsList, final List<Material> materialsAsList, final List<Shape> shapesAsList, final List<Texture> texturesAsList) {
		this.camera = camera;
		this.lightsAsArray = lightsAsArray;
		this.materialsAsArray = materialsAsArray;
		this.shapesAsArray = shapesAsArray;
		this.shapeIndices = shapeIndices;
		this.texturesAsArray = texturesAsArray;
		this.lightsAsList = lightsAsList;
		this.materialsAsList = materialsAsList;
		this.shapesAsList = shapesAsList;
		this.texturesAsList = texturesAsList;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Camera getCamera() {
		return this.camera;
	}
	
	public float[] getLightsAsArray() {
		return this.lightsAsArray;
	}
	
	public float[] getMaterialsAsArray() {
		return this.materialsAsArray;
	}
	
	public float[] getShapesAsArray() {
		return this.shapesAsArray;
	}
	
	public int getLightCount() {
		return this.lightsAsArray.length;
	}
	
	public int getShapeCount() {
		return this.shapesAsList.size();
	}
	
	public int[] getShapeIndices() {
		return this.shapeIndices;
	}
	
	public int[] getTexturesAsArray() {
		return this.texturesAsArray;
	}
	
	public List<Light> getLightsAsList() {
		return this.lightsAsList;
	}
	
	public List<Material> getMaterialsAsList() {
		return this.materialsAsList;
	}
	
	public List<Shape> getShapesAsList() {
		return this.shapesAsList;
	}
	
	public List<Texture> getTexturesAsList() {
		return this.texturesAsList;
	}
	
	public void write(final DataOutput dataOutput) {
		try {
			this.camera.write(dataOutput);
			
			dataOutput.writeFloat(Float.intBitsToFloat(this.texturesAsArray.length));
			
			for(final Texture texture : this.texturesAsList) {
				texture.write(dataOutput);
			}
			
			dataOutput.writeFloat(Float.intBitsToFloat(this.materialsAsArray.length));
			
			for(final Material material : this.materialsAsList) {
				material.write(dataOutput);
			}
			
			dataOutput.writeFloat(Float.intBitsToFloat(this.lightsAsArray.length));
			
			for(final Light light : this.lightsAsList) {
				light.write(dataOutput);
			}
			
			dataOutput.writeFloat(Float.intBitsToFloat(this.shapesAsArray.length));
			
			for(final Shape shape : this.shapesAsList) {
				shape.write(dataOutput);
			}
		} catch(final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public void write(final File file) {
		try(final DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
			write(dataOutputStream);
		} catch(final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static Scene create() {
		return create(new Camera());
	}
	
	public static Scene create(final Camera camera) {
		final
		Builder builder = new Builder(Objects.requireNonNull(camera, "camera == null"));
		builder.addTexture(Texture.createSolidTexture("Texture_0.jpg"));
//		builder.addTexture(Texture.createSolidTexture("Texture_1.png"));
		builder.addTexture(Texture.createSolidTexture("Texture_2.png"));
//		builder.addTexture(Texture.createSolidTexture("Texture_3.png"));
//		builder.addTexture(Texture.createDecalTexture("Texture_4.png"));
		builder.addTexture(Texture.createSolidTexture("Texture_5.jpg"));
		
		final int[] textureOffsets = builder.calculateTextureOffsets();
		
		builder.addMaterial(Material.blackPlastic().setSpecularPower(50.0F).setTextureOffsets(textureOffsets[1]));
		builder.addMaterial(Material.blue().setSpecularPower(50.0F).setTextureOffsets(textureOffsets[doRandom(textureOffsets.length)]));
		builder.addMaterial(Material.brass().setSpecularPower(50.0F).setTextureOffsets(textureOffsets[1]));
		builder.addMaterial(Material.green().setSpecularPower(50.0F).setTextureOffsets(textureOffsets[doRandom(textureOffsets.length)]));
		builder.addMaterial(Material.obsidian().setSpecularPower(50.0F).setTextureOffsets(textureOffsets[doRandom(textureOffsets.length)]));
		builder.addMaterial(Material.red().setSpecularPower(50.0F).setTextureOffsets(textureOffsets[doRandom(textureOffsets.length)]));
//		builder.addMaterial(new Material().setAmbientColor(0.0F, 0.0F, 0.0F).setDiffuseColor(0.0F, 0.0F, 0.0F).setSpecularColor(1.0F, 1.0F, 1.0F).setSpecularPower(32.0F).setTextureOffsets(0));
		
		builder.addLight(new PointLight(400.0F, -800.0F, 400.0F, 100.0F));
		builder.addLight(new PointLight(600.0F, 20.0F, 600.0F, 100.0F));
		builder.addLight(new PointLight(600.0F, 20.0F, 400.0F, 100.0F));
		builder.addLight(new PointLight(400.0F, 20.0F, 600.0F, 100.0F));
		
		final float[] materialOffsets = builder.calculateMaterialOffsets();
		
		for(int i = 0; i < 50; i++) {
			builder.addShape(Sphere.random(materialOffsets[doRandom(materialOffsets.length)]));
		}
		
		builder.addShape(new Plane(materialOffsets[2], 0.5F, 0.0F, 0.5F));
		builder.addShape(new Triangle(materialOffsets[0], 2500.0F, 40.0F, 2500.0F, 1000.0F, 40.0F, 1500.0F, -1000.0F, 40.0F, -1000.0F));
		
		final Scene scene = builder.build();
		
		camera.setScene(scene);
		
		return scene;
	}
	
	public static Scene read(final DataInput dataInput) {
		try {
			final Builder builder = new Builder(Camera.read(dataInput));
			
			final int texturesLength = Float.floatToIntBits(dataInput.readFloat());
			
			for(int i = 0; i < texturesLength;) {
				final Texture texture = Texture.read(dataInput);
				
				i += texture.size();
				
				builder.addTexture(texture);
			}
			
			final int materialsLength = Float.floatToIntBits(dataInput.readFloat());
			
			for(int i = 0; i < materialsLength;) {
				final Material material = Material.read(dataInput);
				
				i += material.size();
				
				builder.addMaterial(material);
			}
			
			final int lightsLength = Float.floatToIntBits(dataInput.readFloat());
			
			for(int i = 0; i < lightsLength;) {
				final Light light = Light.read(dataInput);
				
				i += light.size();
				
				builder.addLight(light);
			}
			
			final int shapesLength = Float.floatToIntBits(dataInput.readFloat());
			
			for(int i = 0; i < shapesLength;) {
				final Shape shape = Shape.read(dataInput);
				
				i += shape.size();
				
				builder.addShape(shape);
			}
			
			return builder.build();
		} catch(final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public static Scene read(final File file) {
		try(final DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			return read(dataInputStream);
		} catch(final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static final class Builder {
		private final AtomicInteger index = new AtomicInteger();
		private final Camera camera;
		private final List<Light> lights = new ArrayList<>();
		private final List<Material> materials = new ArrayList<>();
		private final List<Shape> shapes = new ArrayList<>();
		private final List<Texture> textures = new ArrayList<>();
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public Builder() {
			this(new Camera());
		}
		
		public Builder(final Camera camera) {
			this.camera = camera;
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		public Builder addLight(final Light light) {
			this.lights.add(Objects.requireNonNull(light, "light == null"));
			
			return this;
		}
		
		public Builder addMaterial(final Material material) {
			this.materials.add(Objects.requireNonNull(material, "material == null"));
			
			return this;
		}
		
		public Builder addShape(final Shape shape) {
			this.shapes.add(Objects.requireNonNull(shape, "shape == null"));
			
			shape.setIndex(this.index.getAndAdd(shape.size()));
			
			return this;
		}
		
		public Builder addTexture(final Texture texture) {
			this.textures.add(Objects.requireNonNull(texture, "texture == null"));
			
			return this;
		}
		
		public float[] calculateMaterialOffsets() {
			final float[] materialOffsets = new float[this.materials.size()];
			final float[] materials = doCreateMaterials();
			
			for(int i = 0, j = 0; i < this.materials.size() && j < materials.length; i++) {
				materialOffsets[i] = j;
				
				j += materials[j + Material.RELATIVE_OFFSET_OF_SIZE];
			}
			
			return materialOffsets;
		}
		
		public int[] calculateTextureOffsets() {
			final int[] textureOffsets = new int[this.textures.size()];
			final int[] textures = doCreateTextures();
			
			for(int i = 0, j = 0; i < this.textures.size() && j < textures.length; i++) {
				textureOffsets[i] = j;
				
				j += textures[j + Texture.RELATIVE_OFFSET_OF_SIZE];
			}
			
			return textureOffsets;
		}
		
		public Scene build() {
			return new Scene(this.camera, doCreateLights(), doCreateMaterials(), doCreateShapes(), doCreateShapeIndices(), doCreateTextures(), this.lights, this.materials, this.shapes, this.textures);
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		private float[] doCreateLights() {
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
		
		private float[] doCreateMaterials() {
			int length = 0;
			int offset = 0;
			
			for(final Material material : this.materials) {
				length += material.size();
			}
			
			final float[] array0 = new float[length];
			
			for(final Material material : this.materials) {
				final float[] array1 = material.toFloatArray();
				
				System.arraycopy(array1, 0, array0, offset, array1.length);
				
				offset += array1.length;
			}
			
			return array0;
		}
		
		private float[] doCreateShapes() {
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
		
		private int[] doCreateShapeIndices() {
			final int[] shapeIndices = new int[this.shapes.size()];
			
			for(int i = 0, j = 0; i < this.shapes.size(); i++) {
				final Shape shape = this.shapes.get(i);
				
				shapeIndices[i] = j;
				
				j += shape.size();
			}
			
			return shapeIndices;
		}
		
		private int[] doCreateTextures() {
			int length = 0;
			int offset = 0;
			
			for(final Texture texture : this.textures) {
				length += texture.size();
			}
			
			final int[] array0 = new int[length];
			
			for(final Texture texture : this.textures) {
				final int[] array1 = texture.toIntArray();
				
				System.arraycopy(array1, 0, array0, offset, array1.length);
				
				offset += array1.length;
			}
			
			return array0;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static int doRandom(final int bound) {
		return ThreadLocalRandom.current().nextInt(bound);
	}
}