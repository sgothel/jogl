package jogamp.opengl;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Adapts an iterable of one type to an another.
 *
 * @param <F> The type of the original iterable
 * @param <T> The type of the desired iterable
 *
 * @since 2012.0
 */
abstract class ALinIterableAdapter<F, T> implements Iterable<T> {
  private final Iterable<F> fFromIterable;

  /**
   * Creates a new adapter.
   *
   * @param aFromIterable The original data
   */
  public ALinIterableAdapter(Iterable<F> aFromIterable) {
    fFromIterable = aFromIterable;
  }

  /**
   * Convert an object from the original iterable to an object in the target iterable.
   * Note: if you return {@code null}, the object will be skipped.
   *
   * @param aFrom The object to convert from.
   * @return A new object of the desired type, or {@code null} if the object must be skipped.
   */
  protected abstract T adapt(F aFrom);

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      private final Iterator<F> fFromIterator = fFromIterable.iterator();
      private T fNextTo = null;

      @Override
      public boolean hasNext() {
        while (fNextTo == null && fFromIterator.hasNext()) {
          fNextTo = adapt(fFromIterator.next());
        }

        return (fNextTo != null);
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException("No next item available: please call hasNext() before next().");
        }
        T next = fNextTo;
        fNextTo = null;
        return next;
      }

      @Override
      public void remove() {
        if (fNextTo != null) {
          throw new IllegalStateException("Please call next() before remove()");
        }
        fFromIterator.remove();
      }
    };
  }
}
