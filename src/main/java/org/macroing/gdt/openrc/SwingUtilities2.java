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

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

final class SwingUtilities2 {
	private SwingUtilities2() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static <T> T doRunInEDT(final Supplier<T> supplier) {
		return doRunInEDT(supplier, object -> {});
	}
	
	public static <T> T doRunInEDT(final Supplier<T> supplier, final Consumer<T> consumer) {
		return doRunInEDT(supplier, consumer, null);
	}
	
	public static <T> T doRunInEDT(final Supplier<T> supplier, final Consumer<T> consumer, final T defaultObject) {
		final AtomicReference<T> atomicReference = new AtomicReference<>();
		
		if(supplier != null) {
			if(SwingUtilities.isEventDispatchThread()) {
				atomicReference.set(supplier.get());
			} else {
				doInvokeAndWait(() -> atomicReference.set(supplier.get()));
			}
		}
		
		if(defaultObject != null) {
			atomicReference.compareAndSet(null, defaultObject);
		}
		
		if(consumer != null) {
			consumer.accept(atomicReference.get());
		}
		
		return atomicReference.get();
	}
	
	public static void doInvokeAndWait(final Runnable runnable) {
		if(runnable != null) {
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch(final InvocationTargetException | InterruptedException e) {
//				Do nothing.
			}
		}
	}
}