/**
 * Copyright 2009 - 2016 J&#246;rgen Lundgren
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

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

/**
 * A class that consists exclusively of static methods that extends the functionality provided by the {@code SwingUtilities} class.
 * 
 * @since 1.0.0
 * @author J&#246;rgen Lundgren
 */
public final class SwingUtilities2 {
	private SwingUtilities2() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Runs arbitrary code in the Event Dispatch Thread.
	 * <p>
	 * Returns the fetched {@code Object}, which may be {@code null}.
	 * <p>
	 * This method is implemented in terms of {@code runInEDT(supplier, null)}.
	 * 
	 * @param <T> the type of the returned result
	 * @param supplier the {@code Supplier}, which may be {@code null}
	 * @return the fetched {@code Object}, which may be {@code null}
	 */
	public static <T> T runInEDT(final Supplier<T> supplier) {
		return runInEDT(supplier, null);
	}
	
	/**
	 * Runs arbitrary code in the Event Dispatch Thread.
	 * <p>
	 * Returns the fetched {@code Object}, which may be {@code null}.
	 * <p>
	 * This method is implemented in terms of {@code runInEDT(supplier, consumer, null)}.
	 * 
	 * @param <T> the type of the returned result
	 * @param supplier the {@code Supplier}, which may be {@code null}
	 * @param consumer the {@code Consumer}, which may be {@code null}
	 * @return the fetched {@code Object}, which may be {@code null}
	 */
	public static <T> T runInEDT(final Supplier<T> supplier, final Consumer<T> consumer) {
		return runInEDT(supplier, consumer, null);
	}
	
	/**
	 * Runs arbitrary code in the Event Dispatch Thread.
	 * <p>
	 * The first operation performed by this method is fetching the {@code Object} to return. If {@code supplier} is not {@code null}, its {@code get()} method will be called to fetch the {@code Object}. But if {@code supplier} is {@code null}, or it
	 * produces a {@code null} "value" from its {@code get()} method, then {@code defaultObject}, which may also be {@code null}, will be used instead.
	 * <p>
	 * The second operation performed by this method is handling the {@code Object} to return. So if neither {@code consumer} nor the fetched {@code Object} are {@code null}, the fetched {@code Object} will be handed to {@code consumer}.
	 * <p>
	 * Returns the fetched {@code Object}, which may be {@code null}.
	 * 
	 * @param <T> the type of the returned result
	 * @param supplier the {@code Supplier}, which may be {@code null}
	 * @param consumer the {@code Consumer}, which may be {@code null}
	 * @param defaultObject the default {@code Object}, which may be {@code null}
	 * @return the fetched {@code Object}, which may be {@code null}
	 */
	public static <T> T runInEDT(final Supplier<T> supplier, final Consumer<T> consumer, final T defaultObject) {
		final AtomicReference<T> atomicReference = new AtomicReference<>();
		
		if(supplier != null) {
			runInEDT(() -> atomicReference.set(supplier.get()));
		}
		
		if(defaultObject != null) {
			atomicReference.compareAndSet(null, defaultObject);
		}
		
		if(atomicReference.get() != null && consumer != null) {
			runInEDT(() -> consumer.accept(atomicReference.get()));
		}
		
		return atomicReference.get();
	}
	
	/**
	 * Invokes {@code runnable} in the Event Dispatch Thread and waits for its execution to complete.
	 * <p>
	 * This method is similar to the one provided by {@code SwingUtilities}. But it differs in that you don't have to catch its checked {@code Exception}s. It simply ignores them.
	 * <p>
	 * If {@code runnable} is {@code null}, nothing will happen.
	 * 
	 * @param runnable the {@code Runnable} to invoke and execute in the Event Dispatch Thread
	 */
	public static void invokeAndWait(final Runnable runnable) {
		if(runnable != null) {
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch(final InvocationTargetException | InterruptedException e) {
//				Do nothing.
			}
		}
	}
	
	/**
	 * Runs arbitrary code in the Event Dispatch Thread.
	 * 
	 * @param runnable the {@code Runnable} to run in the Event Dispatch Thread
	 */
	public static void runInEDT(final Runnable runnable) {
		if(runnable != null) {
			if(SwingUtilities.isEventDispatchThread()) {
				runnable.run();
			} else {
				invokeAndWait(runnable);
			}
		}
	}
}