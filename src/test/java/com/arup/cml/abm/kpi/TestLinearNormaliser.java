package com.arup.cml.abm.kpi;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestLinearNormaliser {
    double lowerIntervalBound = 0.0;
    double upperIntervalBound = 10.0;

    double lowerValueBound = 10.0;
    double upperValueBound = 30.0;

    LinearNormaliser basicScaleFactor = new LinearNormaliser(lowerIntervalBound, upperIntervalBound, lowerValueBound, upperValueBound);
    LinearNormaliser reverseScaleFactor = new LinearNormaliser(lowerIntervalBound, upperIntervalBound, upperValueBound, lowerValueBound);

    @Test
    public void mapsTheMidpoint() {
        assertThat(basicScaleFactor.normalise(20)).isEqualTo(5.0);
    }

    @Test
    public void mapsCloserToLeftBound() {
        assertThat(basicScaleFactor.normalise(11.0)).isEqualTo(0.5);
    }

    @Test
    public void mapsCloserToRightBound() {
        assertThat(basicScaleFactor.normalise(29.0)).isEqualTo(9.5);
    }

    @Test
    public void outsideLeftBoundMapsToLowerScaleBound() {
        assertThat(basicScaleFactor.normalise(5.0)).isEqualTo(lowerIntervalBound);
    }

    @Test
    public void outsideRightBoundMapsToUpperScaleBound() {
        assertThat(basicScaleFactor.normalise(35.0)).isEqualTo(upperIntervalBound);
    }

    @Test
    public void reversedFactorStillMapsTheMidpoint() {
        assertThat(reverseScaleFactor.normalise(20)).isEqualTo(5.0);
    }

    @Test
    public void reversedFactorMapsCloserToRightBound() {
        assertThat(reverseScaleFactor.normalise(11.0)).isEqualTo(9.5);
    }

    @Test
    public void reversedFactorMapsCloserToLeftBound() {
        assertThat(reverseScaleFactor.normalise(29.0)).isEqualTo(0.5);
    }

    @Test
    public void reversedFactorOutsideLeftBoundMapsToLowerScaleBound() {
        assertThat(reverseScaleFactor.normalise(35.0)).isEqualTo(lowerIntervalBound);
    }

    @Test
    public void reversedFactorOutsideRightBoundMapsToUpperScaleBound() {
        assertThat(reverseScaleFactor.normalise(5.0)).isEqualTo(upperIntervalBound);
    }

    @Test
    public void defaultsToZeroToTenScaleBounds() {
        LinearNormaliser defaultedLinearNormaliser = new LinearNormaliser(45, 50);
        assertThat(defaultedLinearNormaliser.getLowerScaleBound()).isEqualTo(0.0);
        assertThat(defaultedLinearNormaliser.getUpperScaleBound()).isEqualTo(10.0);
    }

    @Test(expected = RuntimeException.class)
    public void throwsExceptionWhenRightScaleBoundIsLargerThanLeftScaleBound() {
        new LinearNormaliser(10, 0, 45, 50);
    }

    @Test(expected = RuntimeException.class)
    public void throwsExceptionWhenValueBoundsEqualButScaleBoundsAreNot() {
        new LinearNormaliser(0, 1, 45, 45);
    }

}
