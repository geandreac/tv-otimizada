package com.gathin.dormir;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StepTest {
    @Test public void movesInside() {
        float[] v = {5};
        assertEquals(15f, DormirActivity.step(10, v, 100), 0f);
        assertEquals(5f, v[0], 0f);
    }

    @Test public void bouncesOffFarEdge() {
        float[] v = {5};
        assertEquals(97f, DormirActivity.step(98, v, 100), 0f);
        assertEquals(-5f, v[0], 0f);
    }

    @Test public void noRoomStaysAtZero() {
        float[] v = {5};
        assertEquals(0f, DormirActivity.step(10, v, 0), 0f);
    }

    @Test public void levelStaysInRange() {
        assertEquals(0, DormirActivity.clampLevel(-1));
        assertEquals(DormirActivity.LEVELS.length - 1, DormirActivity.clampLevel(99));
        assertEquals(2, DormirActivity.clampLevel(2));
    }

    @Test public void bouncesOffNearEdge() {
        float[] v = {-5};
        assertEquals(3f, DormirActivity.step(2, v, 100), 0f);
        assertEquals(5f, v[0], 0f);
    }
}
