package org.hantonr.pr3.crawler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WeightedQueueTest {

  @Test
  public void weightedQueueSameWeights() {
    String first = "first";
    String second = "second";
    String third = "third";

    WeightedQueue<String, Double> q = new WeightedQueue<>();
    q.add(first);
    q.add(second);
    q.add(third);

    assertEquals(first, q.extract());
    assertEquals(second, q.extract());
    assertEquals(third, q.extract());
    assertEquals(0, q.size());
  }

  @Test
  public void weightedQueueWeighted() {
    String heavy = "heavy";
    String medium = "medium";
    String light = "light";

    WeightedQueue<String, Double> q = new WeightedQueue<>();
    q.add(light, Double.valueOf(0.05));
    q.add(heavy, Double.valueOf(1.0));
    q.add(medium, Double.valueOf(0.2));

    assertEquals(heavy, q.extract());
    assertEquals(medium, q.extract());
    assertEquals(light, q.extract());
    assertEquals(0, q.size());
  }
}
