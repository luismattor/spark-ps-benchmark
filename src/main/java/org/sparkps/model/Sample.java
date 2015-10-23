package org.sparkps.model;

import com.intel.distml.util.KeyCollection;
import com.intel.distml.util.KeyRange;
import com.intel.distml.util.Matrix;

public class Sample extends Matrix {


    double d;

    KeyRange rowKeys;

    public Sample(double d) {
        this.d = d;
        rowKeys = new KeyRange(0, 0);
    }


    public KeyCollection getRowKeys() {
        return rowKeys;
    }

    public KeyCollection getColKeys() {
        return KeyRange.Single;
    }

}