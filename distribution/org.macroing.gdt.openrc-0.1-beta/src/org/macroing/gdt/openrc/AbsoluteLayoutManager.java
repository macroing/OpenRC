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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.io.Serializable;

final class AbsoluteLayoutManager implements LayoutManager, Serializable {
	private static final long serialVersionUID = 1L;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private boolean isUsingPreferredSize;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public AbsoluteLayoutManager() {
		this(true);
	}
	
	public AbsoluteLayoutManager(final boolean isUsingPreferredSize) {
		setUsingPreferredSize(isUsingPreferredSize);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public boolean isUsingPreferredSize() {
		return this.isUsingPreferredSize;
	}
	
	@Override
	public void addLayoutComponent(final String name, final Component component) {
//		Do nothing.
	}
	
	@Override
	public Dimension minimumLayoutSize(final Container parent) {
		synchronized(parent.getTreeLock()) {
			return preferredLayoutSize(parent);
		}
	}
	
	@Override
	public Dimension preferredLayoutSize(final Container parent) {
		synchronized(parent.getTreeLock()) {
			return doGetContainerSize(parent);
		}
	}
	
	@Override
	public String toString() {
		return String.format("[%s]", getClass().getName());
	}
	
	@Override
	public void layoutContainer(final Container parent) {
		synchronized(parent.getTreeLock()) {
			final Insets parentInsets = parent.getInsets();
			
			int x = parentInsets.left;
			int y = parentInsets.top;
			
			for(final Component component: parent.getComponents()) {
				if(component.isVisible()) {
					final Point location = component.getLocation();
					
					x = Math.min(x, location.x);
					y = Math.min(y, location.y);
				}
			}
			
			x = (x < parentInsets.left) ? parentInsets.left - x : 0;
			y = (y < parentInsets.top) ? parentInsets.top - y : 0;
			
			for(final Component component: parent.getComponents()) {
				if(component.isVisible()) {
					final Point location = component.getLocation();
					
					final Dimension componentSize = doGetComponentSize(component);
					
					component.setBounds(location.x + x, location.y + y, componentSize.width, componentSize.height);
				}
			}
		}
	}
	
	@Override
	public void removeLayoutComponent(final Component component) {
//		Do nothing.
	}
	
	public void setUsingPreferredSize(final boolean isUsingPreferredSize) {
		this.isUsingPreferredSize = isUsingPreferredSize;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private Dimension doGetComponentSize(final Component component) {
		final Dimension preferredSize = component.getPreferredSize();
		final Dimension size = component.getSize();
		
		Dimension componentSize = preferredSize;
		
		if(!this.isUsingPreferredSize && size.width > 0 && size.height > 0) {
			componentSize = size;
		}
		
		return componentSize;
	}
	
	private Dimension doGetContainerSize(final Container parent) {
		final Insets parentInsets = parent.getInsets();
		
		int x = parentInsets.left;
		int y = parentInsets.top;
		int width = 0;
		int height = 0;
		
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
}