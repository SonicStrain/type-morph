package io.typemorph.reflect;

import io.typemorph.exception.MorphCircularReferenceException;

import java.util.IdentityHashMap;

/**
 * Thread-local guard that detects circular object references during reflective mapping.
 *
 * <p>Uses an {@link IdentityHashMap} (object identity, not {@code equals}) so that
 * value-equal but distinct objects are correctly tracked as separate.
 *
 * <p>The guard is re-entrant per object identity: the same guard instance can be
 * used across nested mapping calls on the same thread.
 */
public final class CycleGuard {

    public static final CycleGuard INSTANCE = new CycleGuard();

    private final ThreadLocal<IdentityHashMap<Object, Boolean>> inProgress =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private CycleGuard() {}

    /**
     * Marks {@code obj} as currently being mapped.
     * Throws {@link MorphCircularReferenceException} if it is already in-flight.
     */
    public void beginMapping(Object obj) {
        IdentityHashMap<Object, Boolean> map = inProgress.get();
        if (map.containsKey(obj)) {
            throw new MorphCircularReferenceException(obj.getClass());
        }
        map.put(obj, Boolean.TRUE);
    }

    /**
     * Marks {@code obj} as no longer being mapped.
     * Cleans up the ThreadLocal when no objects remain in-flight.
     */
    public void endMapping(Object obj) {
        IdentityHashMap<Object, Boolean> map = inProgress.get();
        map.remove(obj);
        if (map.isEmpty()) {
            inProgress.remove(); // prevent ThreadLocal leak
        }
    }
}
