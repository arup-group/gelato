package com.arup.cml.abm.kpi;

public class LinearNormaliser implements Normaliser {
    private double leftIntervalBound;
    private double rightIntervalBound;
    private double leftValueBound;
    private double rightValueBound;
    private boolean isReversed = false;

    /**
     * Provides 1D affine transformation: M * value + C
     * mapping values: [leftValueBound, rightValueBound] -> [leftIntervalBound, rightIntervalBound]
     * leftValueBound can be larger than rightValueBound, a given value will be scaled reversely.
     * Values outside the boundaries, are mapped to their closest bound.
     * E.g.
     * If [0, 1] -> [0, 10] then 0.4 -> 4, -5 -> 0, 2 -> 10
     * If [1, 0] -> [0, 10] then 0.4 -> 6, -5 -> 10, 2 -> 0
     * @param leftIntervalBound left bound of the destination interval,
     *                          leftIntervalBound < rightIntervalBound, necessarily.
     * @param rightIntervalBound right bound of the destination interval,
     *                           rightIntervalBound > leftIntervalBound, necessarily.
     * @param leftValueBound left bound of the origin interval, it is mapped to leftIntervalBound.
     * @param rightValueBound right bound of the origin interval, it is mapped to rightIntervalBound.
     */
    public LinearNormaliser(double leftIntervalBound, double rightIntervalBound, double leftValueBound, double rightValueBound) {
        if (leftIntervalBound > rightIntervalBound) {
            throw new IllegalArgumentException("leftIntervalBound cannot be larger than rightValueBound");
        }
        if (leftValueBound == rightValueBound && leftIntervalBound != rightIntervalBound) {
            throw new IllegalArgumentException("The bounds given for linear interval are invalid. " +
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

    /**
     * Provides 1D affine transformation: M * value + C
     * mapping values: [leftValueBound, rightValueBound] -> [0, 10]
     * leftValueBound can be larger than rightValueBound, a given value will be scaled reversely.
     * Values outside the boundaries, are mapped to their closest bound.
     * E.g.
     * If [0, 1] -> [0, 10] then 0.4 -> 4, -5 -> 0, 2 -> 10
     * If [1, 0] -> [0, 10] then 0.4 -> 6, -5 -> 10, 2 -> 0
     * @param leftValueBound left bound of the origin interval, it is mapped to 0.
     * @param rightValueBound right bound of the origin interval, it is mapped to 10.
     */
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
