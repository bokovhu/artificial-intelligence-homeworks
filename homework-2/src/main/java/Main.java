import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private final InputStream input;
    private final PrintStream output;

    private final List<Book> books = new ArrayList<> ();
    private final List<User> users = new ArrayList<> ();
    private final List<Rating> ratings = new ArrayList<> ();

    int numFeatures = 20;
    final float trainingDataRatio = 0.5f;
    final int numTrainingEpochs = 1000;
    final float alpha = 0.1f;

    float [][] userFeatureMatrix;
    float [][] bookFeatureMatrix;

    private final boolean debugEnabled = false;
    private final boolean traceEnabled = false;
    private final List<Float> errorValues = new ArrayList<> ();

    private void debugf (String format, Object ... args) {
        if (debugEnabled) {
            System.err.printf (format + "\n", args);
        }
    }

    private void tracef (String format, Object ... args) {
        if (debugEnabled && traceEnabled) {
            System.err.printf (format + "\n", args);
        }
    }

    public Main (InputStream input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    private void readInput () {

        Scanner scanner = new Scanner (input);

        int numRatings = scanner.nextInt ();
        int numUsers = scanner.nextInt ();
        int numBooks = scanner.nextInt ();

        for (int i = 0; i < numUsers; i++) {
            users.add (new User (i));
        }
        for (int i = 0; i < numBooks; i++) {
            books.add (new Book (i));
        }

        for (int i = 0; i < numRatings; i++) {

            ratings.add (
                    new Rating (
                            users.get (scanner.nextInt ()),
                            books.get (scanner.nextInt ()),
                            scanner.nextInt ()
                    )
            );

        }

    }

    private void initPredictionFramework () {

        debugf ("Initializing feature matrices");
        long matrixInitStart = System.currentTimeMillis ();

        Random random = new Random ();

        userFeatureMatrix = new float[users.size ()][numFeatures];
        bookFeatureMatrix = new float[books.size ()][numFeatures];

        for (int i = 0; i < users.size (); i++) {
            for (int j = 0; j < numFeatures; j++) {
                userFeatureMatrix [i][j] = random.nextFloat ();
            }
        }

        for (int i = 0; i < books.size (); i++) {
            for (int j = 0; j < numFeatures; j++) {
                bookFeatureMatrix [i][j] = random.nextFloat ();
            }
        }

        long matrixInitEnd = System.currentTimeMillis ();
        debugf ("Finished feature matrix initialization in %d ms", (matrixInitEnd - matrixInitStart));

    }

    private float predictRating (int user, int book) {

        float sum = 0.0f;

        for (int i = 0; i < numFeatures; i++) {
            sum += userFeatureMatrix [user][i] * bookFeatureMatrix [book][i];
        }

        return sum;

    }

    private float trainModel () {

        Collections.shuffle (ratings);
        List <Rating> trainingData = new ArrayList<> ();
        List <Rating> testData = new ArrayList<> ();

        final int numTrainingSamples = (int) ((float) ratings.size () * trainingDataRatio);
        final int numTestSamples = ratings.size () - numTrainingSamples;

        for (int i = 0; i < numTrainingSamples; i++) {
            trainingData.add (ratings.get (i));
        }
        for (int i = numTrainingSamples; i < ratings.size (); i++) {
            testData.add (ratings.get (i));
        }

        debugf ("Number of training samples: %d, Number of test samples: %d", numTrainingSamples, numTestSamples);

        for (int i = 0; i < numTrainingSamples; i++) {

            Rating sample = trainingData.get (i);
            float predicted = predictRating (sample.user.id, sample.book.id);
            float actual = (float) sample.score / 5.0f;
            float error = predicted - actual;
            float errorSquared = error * error;

            if (Float.isNaN (predicted)) {
                // debugf ("Problem");
            }

            tracef ("Prediction for user %d, book %d is %f, actual is %f. Error: %f", sample.user.id, sample.book.id, predicted, actual, errorSquared);

            for (int k = 0; k < numFeatures; k++) {

                float userFeatureErrorGradient = -2.0f * error * bookFeatureMatrix[sample.book.id][k];
                float bookFeatureErrorGradient = -2.0f * error * userFeatureMatrix[sample.user.id][k];

                tracef ("Error gradients: %f, %f", userFeatureErrorGradient, bookFeatureErrorGradient);

                userFeatureMatrix[sample.user.id][k] = userFeatureMatrix[sample.user.id][k] + alpha * userFeatureErrorGradient;
                bookFeatureMatrix[sample.book.id][k] = bookFeatureMatrix[sample.book.id][k] + alpha * bookFeatureErrorGradient;

            }

        }

        debugf ("Testing ...");

        float totalError = 0.0f;

        for (int i = 0; i < testData.size (); i++) {

            Rating sample = testData.get (i);
            float predicted = predictRating (sample.user.id, sample.book.id);
            float actual = (float) sample.score / 5.0f;
            float error = (float) Math.pow (predicted - actual, 2.0);

            tracef ("Prediction for user %d, book %d is %f, actual is %f. Error: %f", sample.user.id, sample.book.id, predicted, actual, error);

            totalError += error;

        }

        return totalError;

    }

    public void solve () {

        readInput ();
        initPredictionFramework ();

        for (int i = 0; i < numTrainingEpochs; i++) {

            long epochStart = System.currentTimeMillis ();

            float epochError = trainModel ();
            errorValues.add (epochError);

            long epochEnd = System.currentTimeMillis ();

            debugf ("Epoch %d finished, took %d ms, error: %f", i, (epochEnd - epochStart), epochError);

        }

        debugf ("Training finished, predicting ...");

        for (User user : users) {

            List <Book> recommendations = books.stream ()
                    .filter (b -> !user.ratedBooks.contains (b))
                    .sorted (Comparator.<Book>comparingDouble (b -> predictRating (user.id, b.id)).reversed ())
                    .limit (10L)
                    .peek (b -> debugf ("User %d would rate book %d --> %f", user.id, b.id, predictRating (user.id, b.id)))
                    .collect(Collectors.toList());

            for (int i = 0; i < recommendations.size (); i++) {
                output.print (recommendations.get (i).id);
                if (i != recommendations.size () - 1) {
                    output.print ("\t");
                }
            }
            output.println ();

        }

    }

    private static class Rating {

        public final User user;
        public final Book book;
        public final int score;

        private Rating (User user, Book book, int score) {
            this.user = user;
            this.book = book;
            this.score = score;

            this.user.addRating (this);
            this.book.addRating (this);

        }

        @Override
        public boolean equals (Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass () != o.getClass ()) {
                return false;
            }
            Rating rating = (Rating) o;
            return score == rating.score &&
                    Objects.equals (user, rating.user) &&
                    Objects.equals (book, rating.book);
        }

        @Override
        public int hashCode () {
            return Objects.hash (user, book, score);
        }

    }

    private static class User {

        public final int id;
        private final Set<Rating> ratings = new HashSet<> ();
        public final Set <Book> ratedBooks = new HashSet<> ();

        private User (int id) {
            this.id = id;
        }

        public void addRating (Rating rating) {
            this.ratedBooks.add (rating.book);
            this.ratings.add (rating);
        }

        public Set<Rating> getRatings () {
            return ratings;
        }

        @Override
        public boolean equals (Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass () != o.getClass ()) {
                return false;
            }
            User user = (User) o;
            return id == user.id;
        }

        @Override
        public int hashCode () {
            return Objects.hash (id);
        }

    }

    private static class Book {

        public final int id;
        private final Set<Rating> ratings = new HashSet<> ();
        public final Set<User> ratedBy = new HashSet<> ();

        private Book (int id) {
            this.id = id;
        }

        public void addRating (Rating rating) {
            ratedBy.add (rating.user);
            ratings.add (rating);
        }

        public Set<Rating> getRatings () {
            return ratings;
        }

        @Override
        public boolean equals (Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass () != o.getClass ()) {
                return false;
            }
            Book book = (Book) o;
            return id == book.id;
        }

        @Override
        public int hashCode () {
            return Objects.hash (id);
        }

    }

    public static void main (String[] args) {

        Main m = new Main (System.in, System.out);
        m.solve ();

    }

}
