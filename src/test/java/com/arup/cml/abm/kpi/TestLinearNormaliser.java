package com.arup.cml.abm.kpi;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestLinearNormaliser {
    double lowerIntervalBound = 0.0;
    double upperIntervalBound = 10.0;

    double lowerValueBound = 10.0;
    double upperValueBound = 30.0;

    LinearNormaliser basicLinearNormaliser = new LinearNormaliser(lowerIntervalBound, upperIntervalBound, lowerValueBound, upperValueBound);
    LinearNormaliser reverseLinearNormaliser = new LinearNormaliser(lowerIntervalBound, upperIntervalBound, upperValueBound, lowerValueBound);

    @Test
    public void mapsTheMidpoint() {
        assertThat(basicLinearNormaliser.normalise(20)).isEqualTo(5.0);
    }

    @Test
    public void mapsCloserToLeftBound() {
        assertThat(basicLinearNormaliser.normalise(11.0)).isEqualTo(0.5);
    }

    @Test
    public void mapsCloserToRightBound() {
        assertThat(basicLinearNormaliser.normalise(29.0)).isEqualTo(9.5);
    }

    @Test
    public void outsideLeftBoundMapsToLowerIntervalBound() {
        assertThat(basicLinearNormaliser.normalise(5.0)).isEqualTo(lowerIntervalBound);
    }

    @Test
    public void outsideRightBoundMapsToUpperIntervalBound() {
        assertThat(basicLinearNormaliser.normalise(35.0)).isEqualTo(upperIntervalBound);
    }

    @Test
    public void reversedFactorMapsTheMidpoint() {
        assertThat(reverseLinearNormaliser.normalise(20)).isEqualTo(5.0);
    }

    @Test
    public void reversedFactorMapsCloserToRightBound() {
        assertThat(reverseLinearNormaliser.normalise(11.0)).isEqualTo(9.5);
    }

    @Test
    public void reversedFactorMapsCloserToLeftBound() {
        assertThat(reverseLinearNormaliser.normalise(29.0)).isEqualTo(0.5);
    }

    @Test
    public void reversedFactorOutsideLeftBoundMapsToLowerIntervalBound() {
        assertThat(reverseLinearNormaliser.normalise(35.0)).isEqualTo(lowerIntervalBound);
    }

    @Test
    public void reversedFactorOutsideRightBoundMapsToUpperIntervalBound() {
        assertThat(reverseLinearNormaliser.normalise(5.0)).isEqualTo(upperIntervalBound);
    }

    @Test
    public void defaultsToZeroToTenIntervalBounds() {
        LinearNormaliser defaultedLinearNormaliser = new LinearNormaliser(45, 50);
        assertThat(defaultedLinearNormaliser.getLowerIntervalBound()).isEqualTo(0.0);
        assertThat(defaultedLinearNormaliser.getUpperIntervalBound()).isEqualTo(10.0);
    }

    @Test(expected = RuntimeException.class)
    public void throwsExceptionWhenRightIntervalBoundIsLargerThanLeftIntervalBound() {
        new LinearNormaliser(10, 0, 45, 50);
    }

    @Test(expected = RuntimeException.class)
    public void throwsExceptionWhenValueBoundsEqualButIntervalBoundsAreNot() {
        new LinearNormaliser(0, 1, 45, 45);
    }

}
