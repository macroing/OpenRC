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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

final class JBufferedImagePanel extends JPanel {
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