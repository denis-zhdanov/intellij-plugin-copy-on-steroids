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
}
