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
package org.macroing.gdt.openrc.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.io.Serializable;

/**
 * An {@code AbsoluteLayout} lays out its {@code Component}s using their locations. But it can also take into account their preferred sizes. Only visible {@code Component}s will be taken into account.
 * <p>
 * Using this {@code LayoutManager}, you don't have to call {@code setLayout(null)} on a given {@code Container} instance. Not only is that discouraged, but it may also cause unintended side-effects.
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public final class AbsoluteLayout implements LayoutManager, Serializable {
	private static final long serialVersionUID = 1L;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private boolean isUsingPreferredSize;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Constructs a new {@code AbsoluteLayout}.
	 * <p>
	 * Calling this constructor is equivalent to calling {@code new AbsoluteLayout(true)}.
	 */
	public AbsoluteLayout() {
		this(true);
	}
	
	/**
	 * Constructs a new {@code AbsoluteLayout}.
	 * <p>
	 * If {@code isUsingPreferredSize} is {@code true}, this {@code AbsoluteLayout} instance will take the preferred size into account when laying out its {@code Component}s.
	 * 
	 * @param isUsingPreferredSize {@code true} if, and only if, this {@code AbsoluteLayout} instance should take the preferred size into account when laying out its {@code Component}s
	 */
	public AbsoluteLayout(final boolean isUsingPreferredSize) {
		setUsingPreferredSize(isUsingPreferredSize);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Returns {@code true} if, and only if, this {@code AbsoluteLayout} instance will take the preferred size into account when laying out its {@code Component}s, {@code false} otherwise.
	 * 
	 * @return {@code true} if, and only if, this {@code AbsoluteLayout} instance will take the preferred size into account when laying out its {@code Component}s, {@code false} otherwise
	 */
	public boolean isUsingPreferredSize() {
		return this.isUsingPreferredSize;
	}
	
	/**
	 * Calculates the minimum size for the specified {@code Container}, given the {@code Component}s it contains.
	 * <p>
	 * At this time, the minimum size is the same as the preferred size. So this method simply delegates to {@code preferredLayoutSize(Container)}.
	 * 
	 * @param parent the {@code Container} to be laid out
	 */
	@Override
	public Dimension minimumLayoutSize(final Container parent) {
		synchronized(parent.getTreeLock()) {
			return preferredLayoutSize(parent);
		}
	}
	
	/**
	 * Calculates the preferred size for the specified {@code Container}, given the {@code Component}s it contains.
	 * 
	 * @param parent the {@code Container} to be laid out
	 */
	@Override
	public Dimension preferredLayoutSize(final Container parent) {
		final Insets parentInsets = parent.getInsets();
		
		int x = parentInsets.left;
		int y = parentInsets.top;
		int width = 0;
		int height = 0;
		
		synchronized(parent.getTreeLock()) {
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
	
	/**
	 * Returns a {@code String} representation of this {@code AbsoluteLayout} instance.
	 * 
	 * @return a {@code String} representation of this {@code AbsoluteLayout} instance
	 */
	@Override
	public String toString() {
		return String.format("[%s]", getClass().getName());
	}
	
	/**
	 * This method is not supported by this class. So calling it will do nothing.
	 * 
	 * @param name the name to be associated with {@code component}
	 * @param component the {@code Component} to be added
	 */
	@Override
	public void addLayoutComponent(final String name, final Component component) {
//		Do nothing.
	}
	
	/**
	 * Lays out the specified {@code Container}.
	 * 
	 * @param parent the {@code Container} to be laid out
	 */
	@Override
	public void layoutContainer(final Container parent) {
		synchronized(parent.getTreeLock()) {
			final Insets parentInsets = parent.getInsets();
			
			int x = parentInsets.left;
			int y = parentInsets.top;
			
			for(final Component component : parent.getComponents()) {
				if(component.isVisible()) {
					final Point location = component.getLocation();
					
					x = Math.min(x, location.x);
					y = Math.min(y, location.y);
				}
			}
			
			x = x < parentInsets.left ? parentInsets.left - x : 0;
			y = y < parentInsets.top ? parentInsets.top - y : 0;
			
			for(final Component component : parent.getComponents()) {
				if(component.isVisible()) {
					final Point location = component.getLocation();
					
					final Dimension componentSize = doGetComponentSize(component);
					
					component.setBounds(location.x + x, location.y + y, componentSize.width, componentSize.height);
				}
			}
		}
	}
	
	/**
	 * This method is not supported by this class. So calling it will do nothing.
	 * 
	 * @param component the {@code Component} to be removed
	 */
	@Override
	public void removeLayoutComponent(final Component component) {
//		Do nothing.
	}
	
	/**
	 * Sets the preferred size usage for this {@code AbsoluteLayout}.
	 * <p>
	 * If {@code true} is specified, this {@code AbsoluteLayout} instance will take the preferred size into account when laying out its {@code Component}s.
	 * 
	 * @param isUsingPreferredSize {@code true} if, and only if, this {@code AbsoluteLayout} instance should take the preferred size into account when laying out its {@code Component}s
	 */
	public void setUsingPreferredSize(final boolean isUsingPreferredSize) {
		this.isUsingPreferredSize = isUsingPreferredSize;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private Dimension doGetComponentSize(final Component component) {
		final Dimension preferredSize = component.getPreferredSize();
		final Dimension size = component.getSize();
		
		return !this.isUsingPreferredSize && size.width > 0 && size.height > 0 ? size : preferredSize;
	}
}