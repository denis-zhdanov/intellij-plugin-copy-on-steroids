package org.denis.model;

/**
 * @author Denis Zhdanov
 * @since 3/23/13 3:11 PM
 */
public abstract class AbstractFlyweightInfo implements OutputInfo {
  
  private final int myId;

  public AbstractFlyweightInfo(int id) {
    myId = id;
  }

  public int getId() {
    return myId;
  }

  @Override
  public int hashCode() {
    return myId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AbstractFlyweightInfo info = (AbstractFlyweightInfo)o;
    return myId == info.myId;
  }
}
