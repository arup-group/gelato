package com.arup.cml.abm.kpi;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestLinearScale {
    double lowerScaleBound = 0.0;
    double upperScaleBound = 10.0;

    double lowerValueBound = 10.0;
    double upperValueBound = 30.0;

    LinearScale basicScaleFactor = new LinearScale(lowerScaleBound, upperScaleBound, lowerValueBound, upperValueBound);
    LinearScale reverseScaleFactor = new LinearScale(lowerScaleBound, upperScaleBound, upperValueBound, lowerValueBound);

    @Test
    public void mapsTheMidpoint() {
        assertThat(basicScaleFactor.scale(20)).isEqualTo(5.0);
    }

    @Test
    public void mapsCloserToLeftBound() {
        assertThat(basicScaleFactor.scale(11.0)).isEqualTo(0.5);
    }

    @Test
    public void mapsCloserToRightBound() {
        assertThat(basicScaleFactor.scale(29.0)).isEqualTo(9.5);
    }

    @Test
    public void outsideLeftBoundMapsToLowerScaleBound() {
        assertThat(basicScaleFactor.scale(5.0)).isEqualTo(lowerScaleBound);
    }

    @Test
    public void outsideRightBoundMapsToUpperScaleBound() {
        assertThat(basicScaleFactor.scale(35.0)).isEqualTo(upperScaleBound);
    }

    @Test
    public void reversedFactorStillMapsTheMidpoint() {
        assertThat(reverseScaleFactor.scale(20)).isEqualTo(5.0);
    }

    @Test
    public void reversedFactorMapsCloserToRightBound() {
        assertThat(reverseScaleFactor.scale(11.0)).isEqualTo(9.5);
    }

    @Test
    public void reversedFactorMapsCloserToLeftBound() {
        assertThat(reverseScaleFactor.scale(29.0)).isEqualTo(0.5);
    }

    @Test
    public void reversedFactorOutsideLeftBoundMapsToLowerScaleBound() {
        assertThat(reverseScaleFactor.scale(35.0)).isEqualTo(lowerScaleBound);
    }

    @Test
    public void reversedFactorOutsideRightBoundMapsToUpperScaleBound() {
        assertThat(reverseScaleFactor.scale(5.0)).isEqualTo(upperScaleBound);
    }
}
