package com.arup.cml.abm.kpi;

public class LinearNormaliser implements Normaliser {
    private double leftScaleBound;
    private double rightScaleBound;
    private double leftValueBound;
    private double rightValueBound;
    private boolean isReversedLinearScale = false;

    public LinearNormaliser(double leftIntervalBound, double rightIntervalBound, double leftValueBound, double rightValueBound) {
        // leftValueBound is mapped to leftIntervalBound
        // rightValueBound is mapped to rightIntervalBound
        // any value between value bounds is mapped, linearly between values [leftIntervalBound, rightIntervalBound]
        // leftIntervalBound < rightIntervalBound necessarily
        // leftValueBound can be larger than rightValueBound a value will be scaled reversely.

        if (leftIntervalBound > rightIntervalBound) {
            throw new RuntimeException("leftIntervalBound cannot be larger than rightValueBound");
        }
        if (leftValueBound == rightValueBound && leftIntervalBound != rightIntervalBound) {
            throw new RuntimeException("The bounds given for linear interval are invalid. " +
                    "Left and right bounds cannot both be the same value.");
        }
        if (leftValueBound > rightValueBound) {
            this.isReversedLinearScale = true;
            leftValueBound = -leftValueBound;
            rightValueBound = -rightValueBound;
        }

        this.leftScaleBound = leftIntervalBound;
        this.rightScaleBound = rightIntervalBound;
        this.leftValueBound = leftValueBound;
        this.rightValueBound = rightValueBound;
    }

    public LinearNormaliser(double leftValueBound, double rightValueBound) {
        this(0, 10, leftValueBound, rightValueBound);
    }

    private boolean isReversedLinearScale() {
        return this.isReversedLinearScale;
    }

    public double getLowerScaleBound() {
        return leftScaleBound;
    }

    public double getUpperScaleBound() {
        return rightScaleBound;
    }

    private boolean isWithinBounds(double value) {
        return value >= leftValueBound && value <= rightValueBound;
    }

    @Override
    public double normalise(double value) {
        if (this.isReversedLinearScale()) {
            value = -value;
        }
        if (this.isWithinBounds(value)) {
            return ((value - leftValueBound) / (rightValueBound - leftValueBound)) * (rightScaleBound - leftScaleBound);
        } else {
            if (value > rightValueBound) {
                return rightScaleBound;
            } else {
                return leftScaleBound;
            }
        }
    }
}
