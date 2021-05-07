/*
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;

/** A class representing a two member tuple 
 * @param <L> Type of leftmost element
 * @param <R> Type of rightmost element 
 * */
public class Pair<L, R> {
    public final L first;
    public final R second;
 
    public Pair(final L left, final R right) {
        this.first = left;
        this.second = right;
    }
 
    /** The first (or leftmost) element 
     * @return leftmost element
     * */
    public L getFirst() {
        return first;
    }
 
    /** The first (or leftmost) element 
     * @return rightmost element
     * */
    public R getSecond() {
         return second;
    }
 
    /** Make a Pair 
     * @param left left element 
     * @param right right element
     * @param <L> Type of leftmost element
     * @param <R> Type of rightmost element 
     * @return pair
     * */
    public static <L, R> Pair<L, R> make(L left, R right) {
        return new Pair<>(left, right);
    }
 
    public final boolean equals(Object o) {
        if (!(o instanceof Pair<?,?>))
            return false;
 
        final Pair<?, ?> other = (Pair<?,?>) o;
        return localEquals(first, other.first) 
        	&& localEquals(second, other.second);
    }
    
    private static final boolean localEquals(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }
     
    public int hashCode() {
        int hLeft = first == null ? 0 : first.hashCode();
        int hRight = second == null ? 0 : second.hashCode();
        
        return hLeft*37 + hRight;
    }
    
    public String toString() {
    	return "("+first.toString()+", "+second.toString()+")";
    }
}

