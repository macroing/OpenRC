package org.macroing.gdt.openrc;

final class Intersection {
	public static final int RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE_SCALAR_IN_INTERSECTIONS = 1;
	public static final int RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS = 0;
	public static final int RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS = 2;
	public static final int RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS = 5;
	public static final int SIZE_OF_INTERSECTION_IN_INTERSECTIONS = 1 + 1 + 3 + 3;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private Intersection() {
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static float[] create(final int length) {
		final float[] intersections = new float[length * SIZE_OF_INTERSECTION_IN_INTERSECTIONS];
		
		for(int i = 0; i < intersections.length; i += SIZE_OF_INTERSECTION_IN_INTERSECTIONS) {
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SHAPE_OFFSET_SCALAR_IN_INTERSECTIONS] = -1.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_DISTANCE_SCALAR_IN_INTERSECTIONS] = Constants.MAXIMUM_DISTANCE;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 0] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 1] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_INTERSECTION_POINT_IN_INTERSECTIONS + 2] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 0] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 1] = 0.0F;
			intersections[i + RELATIVE_OFFSET_OF_INTERSECTION_SURFACE_NORMAL_VECTOR_IN_INTERSECTIONS + 2] = 0.0F;
		}
		
		return intersections;
	}
}