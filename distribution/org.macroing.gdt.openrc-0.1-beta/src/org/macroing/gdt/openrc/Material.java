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

final class Material {
	public static final int RELATIVE_OFFSET_OF_AMBIENT_COLOR = 1;
	public static final int RELATIVE_OFFSET_OF_AMBIENT_INTENSITY = 5;
	public static final int RELATIVE_OFFSET_OF_DIFFUSE_COLOR = 6;
	public static final int RELATIVE_OFFSET_OF_DIFFUSE_INTENSITY = 10;
	public static final int RELATIVE_OFFSET_OF_REFLECTION = 17;
	public static final int RELATIVE_OFFSET_OF_REFRACTION = 18;
	public static final int RELATIVE_OFFSET_OF_SIZE = 0;
	public static final int RELATIVE_OFFSET_OF_SPECULAR_COLOR = 11;
	public static final int RELATIVE_OFFSET_OF_SPECULAR_INTENSITY = 15;
	public static final int RELATIVE_OFFSET_OF_SPECULAR_POWER = 16;
	public static final int RELATIVE_OFFSET_OF_TEXTURE_COUNT = 19;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static final int SIZE_MATERIAL = 1 + 4 + 1 + 4 + 1 + 4 + 1 + 1 + 1 + 1 + 1;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private float ambientColorA = 255.0F / 255.0F;
	private float ambientColorB = 1.0F / 255.0F;
	private float ambientColorG = 1.0F / 255.0F;
	private float ambientColorR = 1.0F / 255.0F;
	private float ambientIntensity = 0.0F;
	private float diffuseColorA = 255.0F / 255.0F;
	private float diffuseColorB = 32.0F / 255.0F;
	private float diffuseColorG = 32.0F / 255.0F;
	private float diffuseColorR = 32.0F / 255.0F;
	private float diffuseIntensity = 1.0F;
	private float reflection = 1.0F;
	private float refraction = 1.0F;
	private float specularColorA = 255.0F / 255.0F;
	private float specularColorB = 255.0F / 255.0F;
	private float specularColorG = 255.0F / 255.0F;
	private float specularColorR = 255.0F / 255.0F;
	private float specularIntensity = 1.0F;
	private float specularPower = 5.0F;
	private float[] textureOffsets = new float[0];
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Material() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public float getAmbientColorA() {
		return this.ambientColorA;
	}
	
	public float getAmbientColorB() {
		return this.ambientColorB;
	}
	
	public float getAmbientColorG() {
		return this.ambientColorG;
	}
	
	public float getAmbientColorR() {
		return this.ambientColorR;
	}
	
	public float getAmbientIntensity() {
		return this.ambientIntensity;
	}
	
	public float getDiffuseColorA() {
		return this.diffuseColorA;
	}
	
	public float getDiffuseColorB() {
		return this.diffuseColorB;
	}
	
	public float getDiffuseColorG() {
		return this.diffuseColorG;
	}
	
	public float getDiffuseColorR() {
		return this.diffuseColorR;
	}
	
	public float getDiffuseIntensity() {
		return this.diffuseIntensity;
	}
	
	public float getReflection() {
		return this.reflection;
	}
	
	public float getRefraction() {
		return this.refraction;
	}
	
	public float getSpecularColorA() {
		return this.specularColorA;
	}
	
	public float getSpecularColorB() {
		return this.specularColorB;
	}
	
	public float getSpecularColorG() {
		return this.specularColorG;
	}
	
	public float getSpecularColorR() {
		return this.specularColorR;
	}
	
	public float getSpecularIntensity() {
		return this.specularIntensity;
	}
	
	public float getSpecularPower() {
		return this.specularPower;
	}
	
	public float[] getTextureOffsets() {
		return this.textureOffsets;
	}
	
	public float[] toFloatArray() {
		final float[] array = new float[size()];
		
		array[0] = size();
		array[1] = this.ambientColorR;
		array[2] = this.ambientColorG;
		array[3] = this.ambientColorB;
		array[4] = this.ambientColorA;
		array[5] = this.ambientIntensity;
		array[6] = this.diffuseColorR;
		array[7] = this.diffuseColorG;
		array[8] = this.diffuseColorB;
		array[9] = this.diffuseColorA;
		array[10] = this.diffuseIntensity;
		array[11] = this.specularColorR;
		array[12] = this.specularColorG;
		array[13] = this.specularColorB;
		array[14] = this.specularColorA;
		array[15] = this.specularIntensity;
		array[16] = this.specularPower;
		array[17] = this.reflection;
		array[18] = this.refraction;
		array[19] = this.textureOffsets.length;
		
		for(int i = 0; i < this.textureOffsets.length; i++) {
			array[i + 20] = this.textureOffsets[i];
		}
		
		return array;
	}
	
	public int size() {
		return SIZE_MATERIAL + this.textureOffsets.length;
	}
	
	public Material setAmbientColor(final float ambientColorR, final float ambientColorG, final float ambientColorB) {
		return setAmbientColor(ambientColorR, ambientColorG, ambientColorB, 1.0F);
	}
	
	public Material setAmbientColor(final float ambientColorR, final float ambientColorG, final float ambientColorB, final float ambientColorA) {
		this.ambientColorR = ambientColorR;
		this.ambientColorG = ambientColorG;
		this.ambientColorB = ambientColorB;
		this.ambientColorA = ambientColorA;
		
		return this;
	}
	
	public Material setAmbientIntensity(final float ambientIntensity) {
		this.ambientIntensity = ambientIntensity;
		
		return this;
	}
	
	public Material setDiffuseColor(final float diffuseColorR, final float diffuseColorG, final float diffuseColorB) {
		return setDiffuseColor(diffuseColorR, diffuseColorG, diffuseColorB, 1.0F);
	}
	
	public Material setDiffuseColor(final float diffuseColorR, final float diffuseColorG, final float diffuseColorB, final float diffuseColorA) {
		this.diffuseColorR = diffuseColorR;
		this.diffuseColorG = diffuseColorG;
		this.diffuseColorB = diffuseColorB;
		this.diffuseColorA = diffuseColorA;
		
		return this;
	}
	
	public Material setDiffuseIntensity(final float diffuseIntensity) {
		this.diffuseIntensity = diffuseIntensity;
		
		return this;
	}
	
	public Material setReflection(final float reflection) {
		this.reflection = reflection;
		
		return this;
	}
	
	public Material setRefraction(final float refraction) {
		this.refraction = refraction;
		
		return this;
	}
	
	public Material setSpecularColor(final float specularColorR, final float specularColorG, final float specularColorB) {
		return setSpecularColor(specularColorR, specularColorG, specularColorB, 1.0F);
	}
	
	public Material setSpecularColor(final float specularColorR, final float specularColorG, final float specularColorB, final float specularColorA) {
		this.specularColorR = specularColorR;
		this.specularColorG = specularColorG;
		this.specularColorB = specularColorB;
		this.specularColorA = specularColorA;
		
		return this;
	}
	
	public Material setSpecularIntensity(final float specularIntensity) {
		this.specularIntensity = specularIntensity;
		
		return this;
	}
	
	public Material setSpecularPower(final float specularPower) {
		this.specularPower = specularPower;
		
		return this;
	}
	
	public Material setTextureOffsets(final float... textureOffsets) {
		this.textureOffsets = textureOffsets;
		
		return this;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static Material blackPlastic() {
		final
		Material material = new Material();
		material.setAmbientColor(0.0F, 0.0F, 0.0F);
		material.setDiffuseColor(0.01F, 0.01F, 0.01F);
		material.setSpecularColor(0.5F, 0.5F, 0.5F);
		material.setSpecularPower(32.0F);
		
		return material;
	}
	
	public static Material blue() {
		Material material = new Material();
		material.setDiffuseColor(8.0F / 255.0F, 8.0F / 255.0F, 64.0F / 255.0F);
		material.setReflection(0.3F);
		material.setSpecularPower(32.0F);
		
		return material;
	}
	
	public static Material brass() {
		Material material = new Material();
		material.setAmbientColor(0.329412F, 0.223529F, 0.027451F);
		material.setDiffuseColor(0.780392F, 0.568627F, 0.113725F);
		material.setSpecularColor(0.992157F, 0.941176F, 0.807843F);
		material.setSpecularPower(27.8974F);
		
		return material;
	}
	
	public static Material green() {
		Material material = new Material();
		material.setDiffuseColor(8.0F / 255.0F, 64.0F / 255.0F, 8.0F / 255.0F);
		material.setReflection(0.3F);
		material.setSpecularPower(32.0F);
		
		return material;
	}
	
	public static Material obsidian() {
		Material material = new Material();
		material.setAmbientColor(0.05375F, 0.05F, 0.06625F, 0.82F);
		material.setDiffuseColor(0.18275F, 0.17F, 0.22525F, 0.82F);
		material.setReflection(0.3F);
		material.setSpecularColor(0.332741F, 0.328634F, 0.346435F, 0.82F);
		material.setSpecularPower(32.0F);
		
		return material;
	}
	
	public static Material red() {
		Material material = new Material();
		material.setDiffuseColor(64.0F / 255.0F, 8.0F / 255.0F, 8.0F / 255.0F);
		material.setReflection(0.3F);
		material.setSpecularPower(32.0F);
		
		return material;
	}
}