import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

public class Main {

    private final InputStream input;
    private final PrintStream output;

    private static final int NUM_TRAINING_SAMPLES = 17011;
    private static final int NUM_EVALUATION_SAMPLES = 4252;
    private static final int NUM_FEATURES = 81;

    private static final int NUM_LAYERS = 3;
    private static final int[] LAYER_NEURON_COUNTS = new int[] {
            100, 100, 1
    };

    private static final Random GLOBAL_RANDOM = new Random ();

    private double[][] trainingFeatures = new double[NUM_TRAINING_SAMPLES][NUM_FEATURES];
    private double[] originalTrainingValues = new double[NUM_TRAINING_SAMPLES];
    private double[] trainingValues = new double[NUM_TRAINING_SAMPLES];
    private double[][] evaluationFeatures = new double[NUM_EVALUATION_SAMPLES][NUM_FEATURES];
    private double[] featureNormalizationOffsets = new double[NUM_FEATURES];
    private double[] featureNormalizationScales = new double[NUM_FEATURES];
    private double valueNormalizationOffset, valueNormalizationScale;

    private double[][][] weightMatrices = new double[NUM_LAYERS][][];
    private double[][] biasVectors = new double[NUM_LAYERS][];
    private double[][] nodeSums = new double[NUM_LAYERS][];
    private double[][] nodeOutputs = new double[NUM_LAYERS][];

    private static final boolean DEBUG = false;
    private static final boolean TRACE = false;

    private static final long TOTAL_MAX_RUNTIME_MS = 1000 * 20;
    private long startTime = System.currentTimeMillis ();

    private void debugf (String format, Object... params) {
        if (DEBUG) {
            System.out.printf ("DEBUG: " + format + "\n", params);
        }
    }

    private void tracef (String format, Object... params) {
        if (TRACE) {
            System.out.printf ("TRACE: " + format + "\n", params);
        }
    }

    public Main (InputStream input, PrintStream output) {

        this.input = input;
        this.output = output;

    }

    private void readAndProcessInput () {

        long start = System.currentTimeMillis ();

        Scanner scanner = new Scanner (input);

        double[] featureMinimums = new double[NUM_FEATURES];
        double[] featureMaximums = new double[NUM_FEATURES];

        Arrays.fill (featureMinimums, Double.POSITIVE_INFINITY);
        Arrays.fill (featureMaximums, Double.NEGATIVE_INFINITY);

        double valueMinimum = Double.POSITIVE_INFINITY;
        double valueMaximum = Double.NEGATIVE_INFINITY;

        // Read training data features
        for (int i = 0; i < NUM_TRAINING_SAMPLES; i++) {

            for (int j = 0; j < NUM_FEATURES; j++) {

                double feature = scanner.nextDouble ();
                trainingFeatures[i][j] = feature;

                if (featureMaximums[j] < feature) {
                    featureMaximums[j] = feature;
                }
                if (featureMinimums[j] > feature) {
                    featureMinimums[j] = feature;
                }

            }

        }

        // Read training data values
        for (int i = 0; i < NUM_TRAINING_SAMPLES; i++) {

            double value = scanner.nextDouble ();
            originalTrainingValues[i] = value;

            if (valueMaximum < value) {
                valueMaximum = value;
            }
            if (valueMinimum > value) {
                valueMinimum = value;
            }

        }

        // Read evaluation samples
        for (int i = 0; i < NUM_EVALUATION_SAMPLES; i++) {

            for (int j = 0; j < NUM_FEATURES; j++) {

                double feature = scanner.nextDouble ();
                evaluationFeatures[i][j] = feature;

                if (featureMaximums[j] < feature) {
                    featureMaximums[j] = feature;
                }
                if (featureMinimums[j] > feature) {
                    featureMinimums[j] = feature;
                }

            }

        }

        // Data normalization
        for (int i = 0; i < NUM_FEATURES; i++) {
            featureNormalizationOffsets[i] = -1.0 * featureMinimums[i];
            featureNormalizationScales[i] = 1.0 / (featureMaximums[i] - featureMinimums[i]);
        }
        valueNormalizationOffset = -1.0 * valueMinimum;
        valueNormalizationScale = 1.0 / (valueMaximum - valueMinimum);

        // Normalize training samples
        for (int i = 0; i < NUM_TRAINING_SAMPLES; i++) {
            for (int j = 0; j < NUM_FEATURES; j++) {
                trainingFeatures[i][j] = (trainingFeatures[i][j] + featureNormalizationOffsets[j]) * featureNormalizationScales[j];
            }
        }
        // Normalize evaluation samples
        for (int i = 0; i < NUM_EVALUATION_SAMPLES; i++) {
            for (int j = 0; j < NUM_FEATURES; j++) {
                evaluationFeatures[i][j] = (evaluationFeatures[i][j] + featureNormalizationOffsets[j]) * featureNormalizationScales[j];
            }
        }
        // Normalize training values
        for (int i = 0; i < NUM_TRAINING_SAMPLES; i++) {
            trainingValues[i] = (originalTrainingValues[i] + valueNormalizationOffset) * valueNormalizationScale;
        }

        long end = System.currentTimeMillis ();

        debugf ("Reading and processing input took %d ms", (end - start));

    }

    private void randomizeLayerWeightsAndBiases (int layerIndex) {

        for (int row = 0; row < weightMatrices[layerIndex].length; row++) {
            for (int col = 0; col < weightMatrices[layerIndex][row].length; col++) {

                weightMatrices[layerIndex][row][col] = GLOBAL_RANDOM.nextDouble () * 2.0 - 1.0;

            }
        }

        for (int i = 0; i < biasVectors[layerIndex].length; i++) {

            biasVectors[layerIndex][i] = GLOBAL_RANDOM.nextDouble () * 2.0 - 1.0;

        }

    }

    private void createModel () {

        long start = System.currentTimeMillis ();

        for (int i = 0; i < NUM_LAYERS; i++) {

            if (i == 0) {
                // First layer
                weightMatrices[i] = new double[NUM_FEATURES][LAYER_NEURON_COUNTS[i]];
            } else {
                weightMatrices[i] = new double[LAYER_NEURON_COUNTS[i - 1]][LAYER_NEURON_COUNTS[i]];
            }

            biasVectors[i] = new double[LAYER_NEURON_COUNTS[i]];
            randomizeLayerWeightsAndBiases (i);

            nodeSums[i] = new double[LAYER_NEURON_COUNTS[i]];
            nodeOutputs[i] = new double[LAYER_NEURON_COUNTS[i]];

        }

        long end = System.currentTimeMillis ();

        debugf ("Creating the model took %d ms", (end - start));

    }

    private double activation (double sum) {
        return 1.0 / (1.0 + Math.exp (-1.0 * sum));
    }

    private double error (double predicted, double actual) {
        return 0.5 * Math.pow (actual - predicted, 2.0);
    }

    private double networkOutput () {
        return nodeOutputs[NUM_LAYERS - 1][0];
    }

    private void feedForward (double[] input) {

        for (int layer = 0; layer < NUM_LAYERS; layer++) {

            for (int node = 0; node < LAYER_NEURON_COUNTS[layer]; node++) {

                double[] nodeInput = layer == 0
                        ? input
                        : nodeOutputs[layer - 1];

                double weighedSum = 0.0;

                for (int inputNode = 0; inputNode < nodeInput.length; inputNode++) {

                    weighedSum += nodeInput[inputNode] * weightMatrices[layer][inputNode][node];

                }

                weighedSum += biasVectors[layer][node];

                nodeSums[layer][node] = weighedSum;
                nodeOutputs[layer][node] = activation (weighedSum);

            }

        }

    }

    private void trainModel () {

        long start = System.currentTimeMillis ();

        double learningRate = 0.01;

        for (int epoch = 0; epoch < 10; epoch++) {

            long epochStart = System.currentTimeMillis ();

            debugf ("Starting epoch %d", epoch);

            double totalError = 0.0;

            for (int sample = 0; sample < NUM_TRAINING_SAMPLES; sample++) {

                final double[] input = trainingFeatures[sample];
                feedForward (input);
                double prediction = networkOutput ();
                double actual = trainingValues[sample];
                double error = error (prediction, actual);
                tracef ("Output for sample #%d: %.3f, error = %.3f", sample, prediction, error);

                double[][] delta = new double[NUM_LAYERS][];
                double[][][] newWeights = new double[NUM_LAYERS][][];
                double[][] newBiases = new double[NUM_LAYERS][];

                for (int layer = NUM_LAYERS - 1; layer >= 0; layer--) {

                    delta[layer] = new double[LAYER_NEURON_COUNTS[layer]];
                    newWeights[layer] = new double[weightMatrices[layer].length][LAYER_NEURON_COUNTS[layer]];
                    newBiases[layer] = new double[biasVectors[layer].length];

                    if (layer == NUM_LAYERS - 1) {

                        // Output layer
                        delta[layer][0] = -2.0 * (actual - prediction)
                                * prediction
                                * (1.0 - prediction);

                        for (int inputNode = 0; inputNode < weightMatrices[layer].length; inputNode++) {

                            newWeights[layer][inputNode][0] = weightMatrices[layer][inputNode][0]
                                    - learningRate * delta[layer][0] * weightMatrices[layer][inputNode][0];

                        }

                    } else {

                        // Hidden layer
                        for (int node = 0; node < LAYER_NEURON_COUNTS[layer]; node++) {

                            delta[layer][node] = 0.0;
                            for (int outputNode = 0; outputNode < delta[layer + 1].length; outputNode++) {
                                delta[layer][node] += delta[layer + 1][outputNode]
                                        * weightMatrices[layer + 1][node][outputNode];
                            }

                            double dOut_dNet = nodeOutputs[layer][node] * (1.0 - nodeOutputs[layer][node]);
                            delta[layer][node] *= dOut_dNet;

                            for (int inputNode = 0; inputNode < weightMatrices[layer].length; inputNode++) {

                                double dWeight = learningRate * delta[layer][node];

                                if (layer > 0) {
                                    dWeight *= nodeOutputs[layer - 1][inputNode];
                                } else {
                                    dWeight *= input[inputNode];
                                }

                                newWeights[layer][inputNode][node] = weightMatrices[layer][inputNode][node]
                                        - dWeight;

                            }

                        }

                    }

                }

                // Apply the new weights
                weightMatrices = newWeights;

                totalError += error;

            }

            long epochEnd = System.currentTimeMillis ();

            debugf ("Completed epoch %d in %d ms, total error = %.3f", epoch, (epochEnd - epochStart), totalError);

        }

        long end = System.currentTimeMillis ();

        debugf ("Training the model took %d ms", (end - start));

    }

    private void evaluateModel () {

        long start = System.currentTimeMillis ();

        for (int sample = 0; sample < NUM_EVALUATION_SAMPLES; sample++) {

            feedForward (evaluationFeatures[sample]);

            double normalizedOutput = networkOutput ();
            double denormalizedOutput = (normalizedOutput / valueNormalizationScale) - valueNormalizationOffset;
            output.println (denormalizedOutput);

        }

        long end = System.currentTimeMillis ();

        debugf ("Evaluating the model took %d ms", (end - start));

    }

    public void solve () {

        readAndProcessInput ();
        createModel ();
        trainModel ();
        evaluateModel ();

    }

    public static void main (String [] args) {

        Main m = new Main (System.in, System.out);
        m.solve ();

    }

}
