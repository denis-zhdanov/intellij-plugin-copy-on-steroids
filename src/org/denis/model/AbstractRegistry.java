package org.denis.model;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 * @since 3/23/13 3:17 PM
 */
public abstract class AbstractRegistry<T> {

  @NotNull private final TIntObjectHashMap<T> myDataById = new TIntObjectHashMap<T>();

  private transient TObjectIntHashMap<T> myIdsByData = new TObjectIntHashMap<T>();

  @NotNull
  public T dataById(int id) throws IllegalArgumentException {
    T result = myDataById.get(id);
    if (result == null) {
      throw new IllegalArgumentException("No data is registered for id " + id);
    }
    return result;
  }
  
  public int getId(@NotNull T data) throws IllegalStateException {
    if (myIdsByData == null) {
      throw new IllegalStateException(String.format(
        "Can't register data '%s'. Reason: the %s registry is already sealed", data, getClass().getName()
      ));
    }
    int id = myIdsByData.get(data);
    if (id < 0) {
      id = myIdsByData.size() + 1;
      myDataById.put(id, data);
      myIdsByData.put(data, id);
    }
    return id;
  }

  public void seal() {
    myIdsByData = null;
    myDataById.compact();
  }
}
