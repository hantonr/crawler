package org.hantonr.pr3.crawler;

import java.util.LinkedList;
import org.slf4j.Logger;

public class WeightedQueue<T, V extends Comparable> {

  private static final Logger log = org.slf4j.LoggerFactory.getLogger(WeightedQueue.class);
  private LinkedList<T> queue = new LinkedList<>();
  private LinkedList<V> weights = new LinkedList<>();

  public WeightedQueue() {
  }

  /**
   * Appends the specified element to the Queue, giving it a weight = 1. Only works when weight is Double type.
   *
   * @param obj the object to add to the queue
   * @return {@code true} once add is complete
   */
  public boolean add(T obj) {
    try {
      return add(obj, (V) Double.valueOf(1.0));
    } catch (ClassCastException e) {
      log.error("Unable to add {}, likely because weight type is not Double", obj);
    }
    return false;
  }

  /**
   * Appends the specified element to the Queue, placing it based on the provided weight.
   *
   * @param obj the object to add to the queue
   * @param weight the weight to give the object
   * @return {@code true} once add is complete
   */
  public boolean add(T obj, V weight) {
    // set comparison to -1 if lists are empty
    if (!queue.contains(obj)) {
      int comparison = (weights.size() == 0) ? -1 : weights.getFirst().compareTo(weight);

      if (comparison < 0) {
        queue.addFirst(obj);
        weights.addFirst(weight);
      } else {
        int position = 1;
        while (weights.size() > position && weights.get(position).compareTo(weight) >= 0) {
          position++;
        }
        queue.add(position, obj);
        weights.add(position, weight);
      }
      return true;
    }

    return false;
  }

  /**
   * Extract the highest-weighted (and if weights are the same, the oldest) object in the Queue
   *
   * @return the highest weight object and removes it from the queue
   */
  public T extract() {
    T ret = queue.removeFirst();
    weights.removeFirst();

    return ret;
  }

  public int size() {
    return weights.size();
  }
}
