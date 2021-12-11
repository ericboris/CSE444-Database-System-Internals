package simpledb;

/** A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {
    /**
     * Create a new IntHistogram.
     *
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     *
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     *
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't
     * simply store every value that you see in a sorted list.
     *
     * @param buckets The number of buckets to split the input value into.
     * @param min The minimum integer value that will ever be passed to this class for histogramming
     * @param max The maximum integer value that will ever be passed to this class for histogramming
     */
    int[] hist;
    int min;
    int max;
    int buckets;
    int width;
    double tNum;

    public IntHistogram(int buckets, int min, int max) {
        this.buckets = Math.min(buckets, max - min + 1);
        this.hist = new int[this.buckets];
        this.min = min;
        this.max = max;
        this.width = (max - min + 1 - (max - min + 1) % this.buckets) / this.buckets;
        this.tNum = (float) 0.0;
    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
        int bucketId = Math.min((v - min) / width, this.hist.length - 1);
        this.hist[bucketId]++;
        this.tNum++;
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     *
     * For example, if "op" is "GREATER_THAN" and "v" is 5,
     * return your estimate of the fraction of elements that are greater than 5.
     *
     * @param op Operator
     * @param v Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {
        int bucketId = Math.min((v - min) / width, this.hist.length - 1);
        switch (op) {

            case GREATER_THAN:

                if (v <= min){
                    return 1.0;
                } else if (v >= max){
                    return 0.0;
                } else {
                    int curr = hist[bucketId];

                    int currWidth = width;
                    int currEnd = (bucketId + 1) * width;
                    if (bucketId == buckets) {
                        currWidth += (max - min) % buckets;
                        currEnd = max;
                    }

                    int tn = curr * (currEnd - v - min) / (currWidth);
                    for (int i = bucketId + 1; i < buckets; i++) {
                        tn += hist[i];
                    }

                    return tn / tNum;
                }

            case LESS_THAN:
                return estimateSelectivity(Predicate.Op.LESS_THAN_OR_EQ, v - 1);

            case LESS_THAN_OR_EQ:
                return 1 - estimateSelectivity(Predicate.Op.GREATER_THAN, v);

            case GREATER_THAN_OR_EQ:
                return estimateSelectivity(Predicate.Op.GREATER_THAN, v - 1);

            case EQUALS:
                if (v < min || v > max){
                    return 0.0;
                }
                int curr = hist[bucketId];
                return curr / tNum;

            case NOT_EQUALS:
                return 1 - estimateSelectivity(Predicate.Op.EQUALS, v);
        }
        return 1.0;
    }

    /**
     * @return
     *     the average selectivity of this histogram.
     *
     *     This is not an indispensable method to implement the basic
     *     join optimization. It may be needed if you want to
     *     implement a more efficient optimization
     */
    public double avgSelectivity() {
        return 1.0;
    }

    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        return null;
    }
}