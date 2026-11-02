package com.tutormatch.matching;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CosineSimilarityTest {
    @Test
    void comparesDirectionRatherThanMagnitude() {
        assertEquals(1, CosineSimilarity.calculate(new double[]{1, 2}, new double[]{2, 4}), 1e-10);
        assertEquals(0, CosineSimilarity.calculate(new double[]{1, 0}, new double[]{0, 1}));
        assertEquals(-1, CosineSimilarity.calculate(new double[]{1, 0}, new double[]{-1, 0}));
    }

    @Test
    void handlesZeroVectorsAndRejectsInvalidInputs() {
        assertEquals(0, CosineSimilarity.calculate(new double[]{0, 0}, new double[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> CosineSimilarity.calculate(new double[]{}, new double[]{}));
        assertThrows(IllegalArgumentException.class, () -> CosineSimilarity.calculate(new double[]{1}, new double[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> CosineSimilarity.calculate(new double[]{Double.NaN}, new double[]{1}));
    }
}
