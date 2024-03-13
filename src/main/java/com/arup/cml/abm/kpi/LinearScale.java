package com.arup.cml.abm.kpi;

public class LinearScale implements ScalingFactor {
    private double leftScaleBound;
    private double rightScaleBound;
    private double leftValueBound;
    private double rightValueBound;

    public LinearScale(double leftScaleBound, double rightScaleBound, double leftValueBound, double rightValueBound) {
        // leftValueBound is mapped to leftScaleBound
        // rightValueBound is mapped to rightScaleBound
        // any value between value bounds is mapped, linearly between values [leftScaleBound, rightScaleBound]
        // leftScaleBound < rightScaleBound necessarily
        // leftValueBound can be larger than rightValueBound a value will be scaled reversely.

        if (leftScaleBound > rightScaleBound) {
            throw new RuntimeException("leftScaleBound cannot be larger than rightValueBound");
        }
        if ((leftValueBound == rightValueBound) && (leftScaleBound != rightScaleBound)) {
            throw new RuntimeException("The bounds given for linear scale are invalid. " +
                    "Left and right bounds cannot both be the same value.");
        }

        this.leftScaleBound = leftScaleBound;
        this.rightScaleBound = rightScaleBound;
        this.leftValueBound = leftValueBound;
        this.rightValueBound = rightValueBound;
    }

    public LinearScale(double leftValueBound, double rightValueBound) {
        this(0, 10, leftValueBound, rightValueBound);
    }

    private boolean isReversedLinearScale() {
        return leftValueBound > rightValueBound;
    }

    private boolean isWithinBounds(double value) {
        if (this.isReversedLinearScale() && ((value <= leftValueBound) && (value >= rightValueBound))) {
            return true;
        } else {
            return (value >= leftValueBound) && (value <= rightValueBound);
        }
    }

    @Override
    public double scale(double value) {
        if (this.isWithinBounds(value)) {
            return ((value - leftValueBound) / (rightValueBound - leftValueBound)) * (rightScaleBound - leftScaleBound);
        } else if (this.isReversedLinearScale()) {
            if (value < rightValueBound) {
                return rightScaleBound;
            } else {
                return leftScaleBound;
            }
        } else {
            if (value > rightValueBound) {
                return rightScaleBound;
            } else {
                return leftScaleBound;
            }
        }
    }
}
