/**
 * Copyright 2009 - 2021 J&#246;rgen Lundgren
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
package org.macroing.gdt.openrc.swing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JPanel;

import org.macroing.gdt.openrc.util.Ranges;

/**
 * A {@code JBufferedImagePanel} draws a {@code BufferedImage}, and optionally delegates further rendering to a {@code Consumer} of a {@code Graphics2D} instance.
 * <p>
 * Another useful feature supported by this class is image scaling.
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public final class JBufferedImagePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private final BufferedImage bufferedImage;
	private final Consumer<Graphics2D> consumer;
	private final int heightScale;
	private final int widthScale;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Constructs a new {@code JBufferedImagePanel} given a {@code BufferedImage}.
	 * <p>
	 * The {@code Consumer} of {@code Graphics2D}s will do nothing and the width and height scales will both be {@code 1}.
	 * <p>
	 * If {@code bufferedImage} is {@code null}, a {@code NullPointerException} will be thrown.
	 * 
	 * @param bufferedImage the {@code BufferedImage} to draw
	 * @throws NullPointerException thrown if, and only if, {@code bufferedImage} is {@code null}
	 */
	public JBufferedImagePanel(final BufferedImage bufferedImage) {
		this(bufferedImage, graphics2D -> {});
	}
	
	/**
	 * Constructs a new {@code JBufferedImagePanel} given a {@code BufferedImage} and a {@code Consumer} of {@code Graphics2D}s.
	 * <p>
	 * The width and height scales will both be {@code 1}.
	 * <p>
	 * If either {@code bufferedImage} or {@code consumer} are {@code null}, a {@code NullPointerException} will be thrown.
	 * 
	 * @param bufferedImage the {@code BufferedImage} to draw
	 * @param consumer the {@code Consumer} to accept {@code Graphics2D}s
	 * @throws NullPointerException thrown if, and only if, either {@code bufferedImage} or {@code consumer} are {@code null}
	 */
	public JBufferedImagePanel(final BufferedImage bufferedImage, final Consumer<Graphics2D> consumer) {
		this(bufferedImage, consumer, 1, 1);
	}
	
	/**
	 * Constructs a new {@code JBufferedImagePanel} given a {@code BufferedImage}, a {@code Consumer} of {@code Graphics2D}s and the width- and height scales.
	 * <p>
	 * If either {@code bufferedImage} or {@code consumer} are {@code null}, a {@code NullPointerException} will be thrown.
	 * <p>
	 * If either {@code widthScale} or {@code heightScale} are less than {@code 1}, an {@code IllegalArgumentException} will be thrown.
	 * 
	 * @param bufferedImage the {@code BufferedImage} to draw
	 * @param consumer the {@code Consumer} to accept {@code Graphics2D}s
	 * @param widthScale the width scale to use
	 * @param heightScale the height scale to use
	 * @throws IllegalArgumentException thrown if, and only if, either {@code widthScale} or {@code heightScale} are less than {@code 1}
	 * @throws NullPointerException thrown if, and only if, either {@code bufferedImage} or {@code consumer} are {@code null}
	 */
	public JBufferedImagePanel(final BufferedImage bufferedImage, final Consumer<Graphics2D> consumer, final int widthScale, final int heightScale) {
		this.bufferedImage = Objects.requireNonNull(bufferedImage, "bufferedImage == null");
		this.consumer = Objects.requireNonNull(consumer, "consumer == null");
		this.widthScale = Ranges.requireRange(widthScale, 1, Integer.MAX_VALUE);
		this.heightScale = Ranges.requireRange(heightScale, 1, Integer.MAX_VALUE);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Overridden to draw the {@code BufferedImage}.
	 * <p>
	 * This method also delegates optional rendering to the {@code Consumer} of {@code Graphics2D}s.
	 * <p>
	 * The {@code isOpaque()} method is currently not taken into account for.
	 * 
	 * @param graphics the {@code Graphics} instance to draw to
	 */
	@Override
	protected void paintComponent(final Graphics graphics) {
//		Get the BufferedImage:
		final BufferedImage bufferedImage = this.bufferedImage;
		
//		Calculate the scaled width and height:
		final int width = bufferedImage.getWidth();
		final int widthScale = this.widthScale;
		final int widthScaled = width * widthScale;
		final int height = bufferedImage.getHeight();
		final int heightScale = this.heightScale;
		final int heightScaled = height * heightScale;
		
//		Set the RenderingHints for the Graphics2D instance and draw the BufferedImage:
		final
		Graphics2D graphics2D = Graphics2D.class.cast(graphics);
		graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		graphics2D.drawImage(bufferedImage, 0, 0, widthScaled, heightScaled, this);
		
//		Let the Consumer accept the Graphics2D instance to perform additional rendering:
		this.consumer.accept(graphics2D);
	}
}