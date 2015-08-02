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

import java.util.concurrent.atomic.AtomicLong;

final class FPSCounter {
	private final AtomicLong newFPS = new AtomicLong();
	private final AtomicLong newFPSReferenceTimeMillis = new AtomicLong();
	private final AtomicLong newFrameTimeMillis = new AtomicLong();
	private final AtomicLong oldFPS = new AtomicLong();
	private final AtomicLong oldFrameTimeMillis = new AtomicLong();
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public FPSCounter() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public long getFPS() {
		return this.oldFPS.get();
	}
	
	public long getFrameTimeMillis() {
		return this.oldFrameTimeMillis.get();
	}
	
	public void update() {
		final long currentTimeMillis = System.currentTimeMillis();
		
		this.newFPS.incrementAndGet();
		this.newFPSReferenceTimeMillis.compareAndSet(0L, currentTimeMillis);
		this.oldFrameTimeMillis.set(currentTimeMillis - this.newFrameTimeMillis.get());
		this.newFrameTimeMillis.set(currentTimeMillis);
		
		final long newFPSReferenceTimeMillis = this.newFPSReferenceTimeMillis.get();
		final long newFPSElapsedTimeMillis = currentTimeMillis - newFPSReferenceTimeMillis;
		
		if(newFPSElapsedTimeMillis >= 1000L) {
			this.oldFPS.set(this.newFPS.get());
			this.newFPS.set(0L);
			this.newFPSReferenceTimeMillis.set(currentTimeMillis);
		}
	}
}