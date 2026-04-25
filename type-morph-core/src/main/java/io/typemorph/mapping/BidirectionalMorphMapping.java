package io.typemorph.mapping;

/**
 * Defines a bidirectional mapping between types A and B.
 *
 * <p>Register both directions with TypeMorph:
 * <pre>
 *   BidirectionalMorphMapping{@literal <}ClassA, ClassB{@literal >} biMapper = new MyBiMapper();
 *   morph.register(ClassA.class, ClassB.class, biMapper.forwardMapping());
 *   morph.register(ClassB.class, ClassA.class, biMapper.reverseMapping());
 * </pre>
 *
 * @param <A> the first type
 * @param <B> the second type
 */
public interface BidirectionalMorphMapping<A, B> {

    /** Converts A to B. */
    B forward(A source);

    /** Converts B to A. */
    A reverse(B source);

    /** Returns a MorphMapping for the A → B direction. */
    default MorphMapping<A, B> forwardMapping() {
        return this::forward;
    }

    /** Returns a MorphMapping for the B → A direction. */
    default MorphMapping<B, A> reverseMapping() {
        return this::reverse;
    }
}
