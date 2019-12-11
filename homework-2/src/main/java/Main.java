import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Main {

    public static void main (String[] args) throws IOException {

        //Read data from STDIN
        BufferedReader reader = new BufferedReader (new InputStreamReader (System.in));
        String line = reader.readLine ();
        String[] line_list = line.split ("\\s+");

        int nRatings = Integer.valueOf (line_list[0]);
        int nUsers = Integer.valueOf (line_list[1]);
        int nBooks = Integer.valueOf (line_list[2]);

        List<User> users = new ArrayList<> ();

        for (int i = 0; i < nUsers; i++) {
            users.add (new User (i));
        }

        List<Book> books = new ArrayList<> ();
        for (int i = 0; i < nBooks; i++) {
            books.add (new Book (i));
        }

        List<Rating> ratings = new ArrayList<> ();
        float alpha = 0.1f;

        for (int i = 0; i < nRatings; i++) {
            line = reader.readLine ();
            line_list = line.split ("\\s+");
            int user = Integer.valueOf (line_list[0]);
            int book = Integer.valueOf (line_list[1]);
            float score = Float.valueOf (line_list[2]) / 5.0f;
            ratings.add (new Rating (user, book, score));
            users.get (user).appendRated (book);
        }

        int nFeatures = 20;
        int nEpochs = 1000;

        Random rand = new Random ();
        Float[][] P = new Float[nUsers][nFeatures];
        Float[][] Q = new Float[nBooks][nFeatures];
        for (int i = 0; i < P.length; i++) {
            for (int j = 0; j < P[i].length; j++) {
                P[i][j] = rand.nextFloat ();
            }
        }
        for (int i = 0; i < Q.length; i++) {
            for (int j = 0; j < Q[i].length; j++) {
                Q[i][j] = rand.nextFloat ();
            }
        }

        for (int epoch = 0; epoch < nEpochs; epoch++) {
            float totalError = 0.0f;
            for (int i = 0; i < nRatings; i++) {
                Rating rating = ratings.get (i);

                int u = rating.getUser ();
                int b = rating.getBook ();
                float a = rating.getScore ();

                float p = predict (u, b, nFeatures, P, Q);
                float err = p - a;
                float err2 = (float) Math.pow (err, 2.0f);
                totalError += err2;

                for (int j = 0; j < nFeatures; j++) {
                    float gradP = -2.0f * err * Q[b][j];
                    float gradQ = -2.0f * err * P[u][j];
                    P[u][j] = P[u][j] + alpha * gradP;
                    Q[b][j] = Q[b][j] + alpha * gradQ;
                }
            }
        }

        for (int u = 0; u < nUsers; u++) {

            List<Integer> rec = new ArrayList<> ();
            for (int i = 0; i < nBooks; i++) {
                rec.add (i);
            }
            for (int b : users.get (u).getRated ()) {
                rec.remove (Integer.valueOf (b));
            }
            final int user = u;
            rec.sort (new Comparator<Integer> () {
                @Override
                public int compare (Integer o1, Integer o2) {
                    float cmp1 = predict (user, o1, nFeatures, P, Q);
                    float cmp2 = predict (user, o2, nFeatures, P, Q);
                    return Float.compare (cmp1, cmp2);
                }
            });

            for (int i = rec.size () - 1; i > rec.size () - 11; i--) {
                System.out.print (rec.get (i));
                if (i != rec.size () - 10) {
                    System.out.print ('\t');
                }
            }
            System.out.println ();
        }

    }

    private static float predict (int user, int book, int nFeatures, Float[][] P, Float[][] Q) {
        float s = 0.0f;
        for (int i = 0; i < nFeatures; i++) {
            s += P[user][i] * Q[book][i];
        }
        return s;
    }

    private float keyFunction (int u, int book, int nFeature, Float[][] P, Float[][] Q) {
        return predict (u, book, nFeature, P, Q);
    }

}

class User {

    private int id;

    private List<Integer> rated;

    public User (int id) {
        this.id = id;
        this.rated = new ArrayList<> ();
    }

    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    public List<Integer> getRated () {
        return rated;
    }

    public void setRated (List<Integer> rated) {
        this.rated = rated;
    }

    public void appendRated (int rate) {
        this.rated.add (rate);
    }

}

class Book {

    private int id;

    public Book (int id) {
        this.id = id;
    }

    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

}

class Rating {

    private int user;
    private int book;
    private float score;

    public Rating (int user, int book, float score) {
        this.user = user;
        this.book = book;
        this.score = score;
    }

    public int getUser () {
        return user;
    }

    public void setUser (int user) {
        this.user = user;
    }

    public int getBook () {
        return book;
    }

    public void setBook (int book) {
        this.book = book;
    }

    public float getScore () {
        return score;
    }

    public void setScore (float score) {
        this.score = score;
    }

}