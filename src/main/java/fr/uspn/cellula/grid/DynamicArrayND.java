package fr.uspn.cellula.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A recursively defined array whose number of dimensions is chosen at run time.
 *
 * <p>Java fixes the dimension of an array in its type ({@code int[][]} is two-dimensional forever).
 * This class lifts that restriction with the recursive definition required by the subject (where
 * it is called {@code TableauDynamiqueND}): an {@code n}-dimensional array of size
 * {@code k1 × k2 × ... × kn} allocates {@code k1} arrays of dimension {@code n - 1} and size
 * {@code k2 × ... × kn}; the recursion stops at one-dimensional arrays, which hold the elements.
 *
 * <p>The two cases of the definition are modelled by two private subclasses, following the
 * Composite pattern: a {@code Branch} holds sub-arrays, a {@code Leaf} holds elements. Client code
 * only ever sees this abstract type and obtains instances from {@link #create}.
 *
 * <p>Elements are visited in row-major (lexicographic) order: the last index varies fastest.
 *
 * @param <T> type of the elements
 */
public abstract class DynamicArrayND<T> implements Iterable<T> {

    /** Only the nested subclasses may extend this class. */
    private DynamicArrayND() {}

    /**
     * Allocates an array and fills it.
     *
     * @param initializer computes the element stored at given coordinates; called exactly once per
     *     element, in row-major order
     * @param sizes extent along each axis (variable arity: one argument per dimension)
     * @param <T> type of the elements
     * @return the new array
     * @throws IllegalArgumentException if {@code sizes} is empty or holds a non-positive extent
     */
    public static <T> DynamicArrayND<T> create(
            Function<? super Coordinates, ? extends T> initializer, int... sizes) {
        Objects.requireNonNull(initializer, "initializer");
        Shape shape = Shape.of(sizes);
        return allocate(shape, 0, new int[shape.dimension()], initializer);
    }

    /**
     * Recursive step of the construction: builds the sub-array that starts at axis {@code depth}.
     *
     * @param prefix scratch buffer holding the indices chosen along the axes above {@code depth}
     */
    private static <T> DynamicArrayND<T> allocate(
            Shape shape,
            int depth,
            int[] prefix,
            Function<? super Coordinates, ? extends T> initializer) {
        int length = shape.size(depth);
        if (depth == shape.dimension() - 1) {
            Object[] elements = new Object[length];
            for (int index = 0; index < length; index++) {
                prefix[depth] = index;
                elements[index] = initializer.apply(Coordinates.of(prefix));
            }
            return new Leaf<>(elements);
        }
        List<DynamicArrayND<T>> children = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            prefix[depth] = index;
            children.add(allocate(shape, depth + 1, prefix, initializer));
        }
        return new Branch<>(children);
    }

    /** Returns the number of dimensions of this array. */
    public abstract int dimension();

    /** Returns the extent of this array along its first axis. */
    public abstract int length();

    /** Returns the extent of this array along every axis. */
    public final Shape shape() {
        int[] sizes = new int[dimension()];
        collectSizes(sizes, 0);
        return Shape.of(sizes);
    }

    /**
     * Returns the element stored at the given indices.
     *
     * @param indices one index per dimension
     * @throws IllegalArgumentException if the number of indices is not {@link #dimension()}
     * @throws IndexOutOfBoundsException if an index is out of range
     */
    public final T get(int... indices) {
        requireArity(indices);
        return get(indices, 0);
    }

    /**
     * Replaces the element stored at the given indices.
     *
     * @param value the new element
     * @param indices one index per dimension
     * @throws IllegalArgumentException if the number of indices is not {@link #dimension()}
     * @throws IndexOutOfBoundsException if an index is out of range
     */
    public final void set(T value, int... indices) {
        requireArity(indices);
        set(indices, 0, value);
    }

    /** Returns the elements in row-major order, in a new list. */
    public final List<T> toList() {
        List<T> elements = new ArrayList<>();
        forEach(elements::add);
        return elements;
    }

    /** Recursive lookup: {@code indices[depth]} selects within this (sub-)array. */
    abstract T get(int[] indices, int depth);

    /** Recursive update: {@code indices[depth]} selects within this (sub-)array. */
    abstract void set(int[] indices, int depth, T value);

    /** Writes the extent of this (sub-)array and of its descendants from {@code depth} on. */
    abstract void collectSizes(int[] sizes, int depth);

    private void requireArity(int[] indices) {
        if (indices.length != dimension()) {
            throw new IllegalArgumentException(
                    "expected " + dimension() + " index(es) but got " + indices.length);
        }
    }

    private static int checkIndex(int index, int length, int axis) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(
                    "index " + index + " is out of bounds for axis " + axis + " of size " + length);
        }
        return index;
    }

    /** Base case of the recursion: a one-dimensional array of elements. */
    private static final class Leaf<T> extends DynamicArrayND<T> {

        private final Object[] elements;

        Leaf(Object[] elements) {
            this.elements = elements;
        }

        @SuppressWarnings("unchecked") // the array is only ever filled with instances of T
        private T element(int index) {
            return (T) elements[index];
        }

        @Override
        public int dimension() {
            return 1;
        }

        @Override
        public int length() {
            return elements.length;
        }

        @Override
        T get(int[] indices, int depth) {
            return element(checkIndex(indices[depth], elements.length, depth));
        }

        @Override
        void set(int[] indices, int depth, T value) {
            elements[checkIndex(indices[depth], elements.length, depth)] = value;
        }

        @Override
        void collectSizes(int[] sizes, int depth) {
            sizes[depth] = elements.length;
        }

        @Override
        public void forEach(Consumer<? super T> action) {
            for (int index = 0; index < elements.length; index++) {
                action.accept(element(index));
            }
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<>() {
                private int next;

                @Override
                public boolean hasNext() {
                    return next < elements.length;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return element(next++);
                }
            };
        }
    }

    /** Recursive case: an array of arrays that all have one dimension less. */
    private static final class Branch<T> extends DynamicArrayND<T> {

        private final List<DynamicArrayND<T>> children;

        Branch(List<DynamicArrayND<T>> children) {
            this.children = children;
        }

        @Override
        public int dimension() {
            return 1 + children.get(0).dimension();
        }

        @Override
        public int length() {
            return children.size();
        }

        @Override
        T get(int[] indices, int depth) {
            return child(indices, depth).get(indices, depth + 1);
        }

        @Override
        void set(int[] indices, int depth, T value) {
            child(indices, depth).set(indices, depth + 1, value);
        }

        private DynamicArrayND<T> child(int[] indices, int depth) {
            return children.get(checkIndex(indices[depth], children.size(), depth));
        }

        @Override
        void collectSizes(int[] sizes, int depth) {
            sizes[depth] = children.size();
            children.get(0).collectSizes(sizes, depth + 1);
        }

        @Override
        public void forEach(Consumer<? super T> action) {
            for (DynamicArrayND<T> child : children) {
                child.forEach(action);
            }
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<>() {
                private final Iterator<DynamicArrayND<T>> remaining = children.iterator();
                private Iterator<T> current = Collections.emptyIterator();

                @Override
                public boolean hasNext() {
                    while (!current.hasNext() && remaining.hasNext()) {
                        current = remaining.next().iterator();
                    }
                    return current.hasNext();
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return current.next();
                }
            };
        }
    }
}
