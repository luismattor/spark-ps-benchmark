package org.sparkps.model;

import com.intel.distml.model.rosenblatt.Weights;
import com.intel.distml.api.DParam;
import com.intel.distml.api.databus.DataBus;
import com.intel.distml.api.Model;
import com.intel.distml.api.neuralnetwork.DUpdate;
import com.intel.distml.util.KeyCollection;
import com.intel.distml.util.Matrix;

public class UpdateModel extends Model {

    public class Parameters extends DParam {

        int dimension;

        public Parameters(int d) {
            super(d);
            this.dimension = d;
        }

        @Override
        public void initOnServer(int psIndex, KeyCollection keys) {
            System.out.println("Initializing parameters: " + dimension);
            Double w[] = new Double[dimension];
            for (int i = 0; i < dimension; i++){
                w[i] = 0.0;
            }
            setLocalCache(new Weights(w));
        }

         public void mergeUpdate(int serverIndex, Matrix update) {
            ((Weights) localCache).mergeUpdates((Weights) update);
        }

    }

    public class Updates extends DUpdate {

        int dimension;

        public Updates(int d) {
            super(d);
            this.dimension = d;
        }

        @Override
        public void initOnWorker(int psIndex, KeyCollection keys) {
            System.out.println("Initializing updates: " + dimension);
            Double w[] = new Double[dimension];
            for (int i = 0; i < dimension; i++) {
                w[i] = 0.1;//((double)(Math.random()-.5));
            }
            setLocalCache(new Weights(w));
        }
        
    }    

    public UpdateModel(int dim) {
        super();
        registerMatrix("weights", new Parameters(dim));
        registerMatrix("updates", new Updates(dim));
    }

    public void show() {
        System.out.println("Showing matrix" );
        Double[] w = ((Weights) getMatrix("weights").localCache).values;
        double sum = 0.0;
        for (int i = 0; i < w.length; i++) {
            sum += w[i];
        }
        System.out.println("sum [computed]    :\t" + sum);
    }

    public Matrix transformSample(Object sample) {
        return (Matrix) sample;
    }

    @Override
    public void compute(Matrix s, int workerIndex, DataBus dataBus, int iter) {

        Weights weights = (Weights) getMatrix("weights").localCache;
        Weights updates = (Weights) getMatrix("updates").localCache;

        System.out.println("TMP = " + workerIndex + ". " + weights.values[0]);
        dataBus.pushUpdate("weights", updates);
    }

}