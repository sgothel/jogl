package jogamp.opengl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Hash map implementation with weak keys and identity-based comparison semantics. Keys are weakly
 * referenced and not protected from a potential garbage collection. If a key becomes garbage
 * collected, the corresponding entry is discarded. Cleanup is not asynchronous; it piggybacks on
 * other operations. See {@link java.util.WeakHashMap} for a more detailed discussion.
 *
 * @since 2012.0
 */
class TLcdWeakIdentityHashMap<K, V> implements Map<K, V> {

  private static final int INITIAL_CAPACITY = 16;
  private static final float LOAD_FACTOR = 0.75f;
  private Map<MyKey<K>, V> fHashMap;
  private ReferenceQueue<K> fQueue = new ReferenceQueue<>();
  private float fLoadFactor;

  private transient Set<K> fKeySet = null;
  private transient Set<Entry<K, V>> fEntrySet = null;

  /**
   * Creates a new map with an initial capacity of 16 and load factor of 0.75.
   */
  public TLcdWeakIdentityHashMap() {
    this(INITIAL_CAPACITY, LOAD_FACTOR);
  }

  /**
   * Creates a new map with the given initial capacity and load factor of 0.75.
   *
   * @param aInitialCapacity the initial capacity of the map
   */
  public TLcdWeakIdentityHashMap(int aInitialCapacity) {
    this(aInitialCapacity, LOAD_FACTOR);
  }

  /**
   * Creates a new map with the given initial capacity and the given load factor.
   *
   * @param aInitialCapacity the initial capacity of the map
   * @param aLoadFactor      the load factor
   *
   * @see java.util.HashMap
   */
  public TLcdWeakIdentityHashMap(int aInitialCapacity, float aLoadFactor) {
    fLoadFactor = aLoadFactor;
    fHashMap = createMap(aInitialCapacity, aLoadFactor);
  }

  /**
   * Creates a new map containing all entries in the given map.
   * The effect of this constructor is equivalent to that of calling {@code put(k, v)} on this map once for each mapping
   * from key k to value v in the specified map.
   *
   * @param aMap the map whose entries to load into this map.
   *
   * @see java.util.HashMap
   */
  public TLcdWeakIdentityHashMap(Map aMap) {
    this(Math.max((int) (aMap.size() / 0.6) + 1, 19), 0.6f);
    putAll(aMap);
  }

  /**
   * Override this method to use a custom map implementation, such as, for example, an LRU cache:
   * <blockquote>
   * <pre>
   * private static final int MAX_ENTRIES = 1000;
   *
   * private TLcdWeakIdentityHashMap fCache = new TLcdWeakIdentityHashMap( MAX_ENTRIES ) {
   * protected Map createMap( int aInitialCapacity, float aLoadFactor ) {
   * float load = 0.75f;
   * return new LinkedHashMap( ( int ) ( 1 + MAX_ENTRIES / load ), load, true ) {
   * protected boolean removeEldestEntry( Map.Entry aEldest ) {
   * return size() > MAX_ENTRIES;
   * }
   * };
   * }
   * };
   * </pre>
   * </blockquote>
   */
  private Map<MyKey<K>, V> createMap(int aInitialCapacity, float aLoadFactor) {
    return new HashMap<>(aInitialCapacity, aLoadFactor);
  }

  /**
   * Called when an entry is removed from the map (either by explicitly removing it,
   * when it's overridden, or when it was removed because it was unreferenced)
   *
   * @param aKey   the key. Can be {@code null} if it was unreferenced (when not hard-referenced anymore).
   * @param aValue the value
   */
  protected void onRemove(Object aKey, V aValue) {
  }

  public int size() {
    return getExpungedMap().size();
  }

  public boolean isEmpty() {
    return getExpungedMap().isEmpty();
  }

  private Iterator<Entry<K, V>> entries() {
    return new ALinIterableAdapter<Entry<MyKey<K>, V>, Entry<K, V>>(getExpungedMap().entrySet()) {
      @Override
      protected Entry<K, V> adapt(Entry<MyKey<K>, V> aEntry) {
        K key = aEntry.getKey().get();
        return key == null ? null : new MyEntry<>(key, aEntry.getValue(), aEntry);
      }
    }.iterator();
  }

  private Iterator<K> keys() {
    return new ALinIterableAdapter<MyKey<K>, K>(getExpungedMap().keySet()) {
      @Override
      protected K adapt(MyKey<K> aMyKey) {
        return aMyKey.get();
      }
    }.iterator();
  }

  /**
   * {@inheritDoc}
   *
   * <p>It is not allowed to access the map (for example calling {@link #get(Object) get}) while iterating over
   * the result of this method, except via the {@code Iterator.remove} method. Not doing so may lead to
   * {@code ConcurrentModificationException}s in rare cases.</p>
   */
  public Set<Entry<K, V>> entrySet() {
    if (fEntrySet == null) {
      fEntrySet = new EntrySetView();
    }
    return fEntrySet;
  }

  /**
   * {@inheritDoc}
   *
   * <p>It is not allowed to access the map (for example calling {@link #get(Object) get}) while iterating over
   * the result of this method, except via the {@code Iterator.remove} method. Not doing so may lead to
   * {@code ConcurrentModificationException}s in rare cases.</p>
   */
  public Set<K> keySet() {
    if (fKeySet == null) {
      fKeySet = new KeySetView();
    }
    return fKeySet;
  }

  private class EntrySetView extends AbstractSet<Entry<K, V>> {

    public int size() {
      return TLcdWeakIdentityHashMap.this.size();
    }

    public void clear() {
      TLcdWeakIdentityHashMap.this.clear();
    }

    public boolean isEmpty() {
      return TLcdWeakIdentityHashMap.this.isEmpty();
    }

    public boolean contains(Object o) {
      expungeStaleEntries();
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<K, V> e = (Map.Entry) o;
      V val = TLcdWeakIdentityHashMap.this.get(e.getKey());
      return (val != null)
             ? val.equals(e.getValue())
             : (e.getValue() == null);
    }

    public boolean remove(Object o) {
      expungeStaleEntries();
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<K, V> e = (Map.Entry) o;
      return TLcdWeakIdentityHashMap.this.remove(e.getKey()) != null;
    }

    public boolean addAll(Collection aCollection) {
      expungeStaleEntries();
      int size = TLcdWeakIdentityHashMap.this.size();
      for (Object object : aCollection) {
        Entry<K, V> e = (Entry<K, V>) object;
        TLcdWeakIdentityHashMap.this.put(e.getKey(), e.getValue());
      }
      return size != TLcdWeakIdentityHashMap.this.size();
    }

    public Iterator<Entry<K, V>> iterator() {
      return entries();
    }
  }

  private class KeySetView extends AbstractSet<K> {
    public int size() {
      return TLcdWeakIdentityHashMap.this.size();
    }

    public void clear() {
      TLcdWeakIdentityHashMap.this.clear();
    }

    public boolean isEmpty() {
      return TLcdWeakIdentityHashMap.this.isEmpty();
    }

    public boolean contains(Object o) {
      return TLcdWeakIdentityHashMap.this.containsKey(o);
    }

    public boolean remove(Object o) {
      return TLcdWeakIdentityHashMap.this.remove(o) != null;
    }

    public Iterator<K> iterator() {
      return keys();
    }
  }

  public void putAll(Map<? extends K, ? extends V> t) {
    Set<? extends Entry<? extends K, ? extends V>> entry_set = t.entrySet();
    for (Entry<? extends K, ? extends V> entry : entry_set) {
      put(entry.getKey(), entry.getValue());
    }
  }

  public V get(Object key) {
    return getExpungedMap().get(new MyKey<>(key));
  }

  public V remove(Object key) {
    V previous = getExpungedMap().remove(new MyKey<>(key));
    if (previous != null) {
      onRemove(key, previous);
    }
    return previous;
  }

  public V put(K key, V value) {
    if (value == null) {
      throw new IllegalArgumentException("Null values are not allowed");
    }
    V previous = getExpungedMap().put(new MyKey<>(key, fQueue), value);
    if (previous != null) {
      onRemove(key, previous);
    }
    return previous;
  }

  public void clear() {
    for (Entry<MyKey<K>, V> entry : getExpungedMap().entrySet()) {
      onRemove(entry.getKey().get(), entry.getValue());
    }
    fHashMap.clear();
  }

  public boolean containsKey(Object key) {
    return getExpungedMap().containsKey(new MyKey<>(key));
  }

  public boolean containsValue(Object value) {
    return getExpungedMap().containsValue(value);
  }

  public Collection<V> values() {
    return getExpungedMap().values();
  }

  private Map<MyKey<K>, V> getExpungedMap() {
    expungeStaleEntries();
    return fHashMap;
  }

  @Override
  public Object clone() {
    try {
      @SuppressWarnings("unchecked") TLcdWeakIdentityHashMap<K, V> clone = (TLcdWeakIdentityHashMap<K, V>) super.clone();
      clone.fHashMap = clone.createMap(Math.max(INITIAL_CAPACITY, clone.size()), fLoadFactor);
      clone.fQueue = new ReferenceQueue<>();
      clone.fEntrySet = null;
      clone.fKeySet = null;
      Set<Entry<MyKey<K>, V>> entries = fHashMap.entrySet();
      for (Entry<MyKey<K>, V> entry : entries) {
        MyKey<K> weakReference = entry.getKey();
        clone.put(weakReference.get(), entry.getValue());
      }
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    expungeStaleEntries();
    return "TLcdWeakIdentityHashMap" + fHashMap;
  }

  /**
   * Expunges stale entries from the table.
   */
  @SuppressWarnings("unchecked")
  private void expungeStaleEntries() {
    MyKey<K> e;
    while ((e = (MyKey<K>) fQueue.poll()) != null) {
      V previousValue = fHashMap.remove(e);
      // LCD-7367: avoid calling onRemove() twice for objects that were explicitly removed first, and then garbage collected.
      if (previousValue != null) {
        onRemove(e.get(), previousValue);
      }
    }
  }

  private static class MyKey<T> extends WeakReference<T> {
    private int fHashCode;

    public MyKey(T referent) {
      this(referent, null);
    }

    public MyKey(T aObject, ReferenceQueue<T> aQueue) {
      super(aObject, aQueue);
      fHashCode = System.identityHashCode(aObject);
    }

    @Override
    public boolean equals(Object aObject) {
      if (aObject instanceof MyKey<?>) {
        MyKey<?> other_key = (MyKey<?>) aObject;
        return other_key.get() == get();
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return fHashCode;
    }

    @Override
    public String toString() {
      return String.valueOf(get());
    }
  }

  private static class MyEntry<K, V> implements Map.Entry<K, V> {

    private final K fKey;
    private V fValue;
    private final Entry<MyKey<K>, V> fDelegateEntry;

    private MyEntry(K aKey, V aValue, Entry<MyKey<K>, V> aDelegateEntry) {
      fKey = aKey;
      fValue = aValue;
      fDelegateEntry = aDelegateEntry;
    }

    public K getKey() {
      return fKey;
    }

    public V getValue() {
      return fValue;
    }

    public V setValue(V value) {
      fValue = value;
      return fDelegateEntry.setValue(value);
    }

  }

}

