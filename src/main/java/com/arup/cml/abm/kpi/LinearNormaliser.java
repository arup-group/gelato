package com.arup.cml.abm.kpi;

public class LinearNormaliser implements Normaliser {
    private double leftIntervalBound;
    private double rightIntervalBound;
    private double leftValueBound;
    private double rightValueBound;
    private boolean isReversed = false;

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
            this.isReversed = true;
            leftValueBound = -leftValueBound;
            rightValueBound = -rightValueBound;
        }

        this.leftIntervalBound = leftIntervalBound;
        this.rightIntervalBound = rightIntervalBound;
        this.leftValueBound = leftValueBound;
        this.rightValueBound = rightValueBound;
    }

    public LinearNormaliser(double leftValueBound, double rightValueBound) {
        this(0, 10, leftValueBound, rightValueBound);
    }

    private boolean isReversed() {
        return this.isReversed;
    }

    public double getLowerIntervalBound() {
        return leftIntervalBound;
    }

    public double getUpperIntervalBound() {
        return rightIntervalBound;
    }

    private boolean isWithinBounds(double value) {
        return value >= leftValueBound && value <= rightValueBound;
    }

    @Override
    public double normalise(double value) {
        if (this.isReversed()) {
            value = -value;
        }
        if (this.isWithinBounds(value)) {
            return ((value - leftValueBound) / (rightValueBound - leftValueBound)) * (rightIntervalBound - leftIntervalBound);
        } else {
            if (value > rightValueBound) {
                return rightIntervalBound;
            } else {
                return leftIntervalBound;
            }
        }
    }
}
