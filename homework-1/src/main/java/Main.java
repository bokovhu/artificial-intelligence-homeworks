import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    private final InputStream input;
    private final PrintStream output;
    private int width, height;
    private int numCars;
    private List<Car> cars = new ArrayList<> ();

    private String stateToString (State state) {
        StringBuilder sb = new StringBuilder ();
        int[][] map = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = -1;
            }
        }
        for (PlacedCar pc : state.placedCars) {
            final int fromX = pc.point.x;
            final int fromY = pc.point.y;
            final int toX = pc.point.x + pc.width;
            final int toY = pc.point.y + pc.height;
            for (int y = fromY; y < toY; y++) {
                for (int x = fromX; x < toX; x++) {
                    map[y][x] = pc.car.id + 1;
                }
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sb.append (map[y][x]);
                if (x != width - 1) {
                    sb.append ("\t");
                }
            }
            if (y != height - 1) {
                sb.append ("\n");
            }
        }
        return sb.toString ();
    }

    public Main (InputStream input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    private void readInput () {

        Scanner scanner = new Scanner (this.input);

        this.height = scanner.nextInt ();
        this.width = scanner.nextInt ();

        this.numCars = scanner.nextInt ();

        for (int i = 0; i < this.numCars; i++) {
            cars.add (
                    new Car (i, scanner.nextInt (), scanner.nextInt ())
            );
        }

    }

    private List<Decision> validDecisions (State state) {

        List<Decision> decisions = new ArrayList<> ();

        Car car = state.remainingCars.get (0);

        for (Point point : state.availablePoints) {
            if (width - point.x >= car.width
                    && height - point.y >= car.height) {
                decisions.add (
                        new Decision (car, false, point)
                );
            }
            if (car.width != car.height) {
                if (width - point.x >= car.height
                        && height - point.y >= car.width) {
                    decisions.add (
                            new Decision (car, true, point)
                    );
                }
            }
        }

        Iterator<Decision> iterator = decisions.iterator ();
        while (iterator.hasNext ()) {

            Decision decision = iterator.next ();

            final int x1 = decision.point.x;
            final int y1 = decision.point.y;
            final int w1 = decision.rotated ? decision.car.height : decision.car.width;
            final int h1 = decision.rotated ? decision.car.width : decision.car.height;
            for (PlacedCar placedCar : state.placedCars) {

                final int x2 = placedCar.point.x;
                final int y2 = placedCar.point.y;
                final int w2 = placedCar.width;
                final int h2 = placedCar.height;

                if (x1 < x2 + w2 &&
                        x1 + w1 > x2 &&
                        y1 < y2 + h2 &&
                        y1 + h1 > y2) {
                    iterator.remove ();
                    break;
                }

            }

        }

        decisions.sort (
                Comparator.comparingInt ((Decision dc) -> dc.point.x).thenComparingInt (dc -> dc.point.y)
        );

        return decisions;

    }

    private State stateAfterDecision (State before, Decision decision) {

        List<PlacedCar> placedCars = new ArrayList<> (before.placedCars);
        PlacedCar placedCar = new PlacedCar (decision.car, decision.rotated, decision.point);
        placedCars.add (placedCar);

        List<Car> remainingCars = new ArrayList<> (before.remainingCars);
        remainingCars.remove (decision.car);

        List<Point> availablePoints = new ArrayList<> (before.availablePoints);

        final int fromY = decision.point.y;
        final int toY = decision.point.y + placedCar.height;
        final int fromX = decision.point.x;
        final int toX = decision.point.x + placedCar.width;

        for (int y = fromY; y < toY; y++) {
            for (int x = fromX; x < toX; x++) {
                availablePoints.remove (new Point (x, y));
            }
        }
        availablePoints.sort (
                Comparator.comparingInt ((Point p) -> p.x).thenComparingInt (p -> p.y)
        );

        return new State (placedCars, remainingCars, availablePoints);

    }

    private Result findSolution (int level, State state) {

        List<Decision> decisions = validDecisions (state);
        for (Decision decision : decisions) {

            State stateAfterDecision = stateAfterDecision (state, decision);
            Result result = new Result (stateAfterDecision, stateAfterDecision.remainingCars.isEmpty ());
            if (result.finished) {
                return result;
            }
            result = findSolution (level + 1, stateAfterDecision);
            if (result != null && result.finished) {
                return result;
            }
        }
        return null;

    }

    public void solve () {

        // First, read the input
        readInput ();

        List<Point> initialAvailablePoints = new ArrayList<> ();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                initialAvailablePoints.add (new Point (x, y));
            }
        }
        cars.sort (
                (c1, c2) -> -1 * Integer.compare (c1.width * c1.height, c2.width * c2.height)
        );

        State initialState = new State (
                Collections.emptyList (),
                cars,
                initialAvailablePoints
        );

        Result result = findSolution (1, initialState);
        if (result != null && result.finished) {
            output.println (stateToString (result.state));
        } else {
            System.err.println ("NO RESULT FOUND!");
        }

    }

    private static final class Result {

        public final State state;
        public final boolean finished;

        private Result (State state, boolean finished) {
            this.state = state;
            this.finished = finished;
        }

    }

    private static final class State {

        public final List<PlacedCar> placedCars;
        public final List<Car> remainingCars;
        public final List<Point> availablePoints;

        private State (List<PlacedCar> placedCars, List<Car> remainingCars, List<Point> availablePoints) {
            this.placedCars = new ArrayList<> (placedCars);
            this.remainingCars = new ArrayList<> (remainingCars);
            this.availablePoints = new ArrayList<> (availablePoints);
        }

        public String cacheKey () {
            StringBuilder sb = new StringBuilder ();
            placedCars.stream ().sorted (Comparator.comparing (pc -> pc.car.id))
                    .forEach (
                            pc -> sb.append ("[A")
                                    .append (pc.car.id)
                                    .append (",").append (pc.point.x).append (",").append (pc.point.y)
                                    .append (",").append (pc.rotated ? "1" : "0")
                                    .append ("]")
                    );
            remainingCars.stream ().sorted (Comparator.comparing (c -> c.id))
                    .forEach (
                            car -> sb.append ("[B").append (car.id).append ("]")
                    );
            return sb.toString ();
        }

        @Override
        public String toString () {
            return "placed cars: " + placedCars
                    + ", remaining cars: " + remainingCars
                    + ", available points: " + availablePoints;

        }

    }

    private static final class PlacedCar {

        public final Car car;
        public final boolean rotated;
        public final Point point;
        public final int width, height;

        private PlacedCar (Car car, boolean rotated, Point point) {
            this.car = car;
            this.rotated = rotated;
            this.point = new Point (point);
            this.width = rotated ? car.height : car.width;
            this.height = rotated ? car.width : car.height;
        }

        @Override
        public String toString () {
            return car + " @ " + point + " "
                    + (rotated ? "rotated" : "unrotated");
        }

    }

    private static final class Car {

        public final int id;
        public final int width, height;

        private Car (int id, int width, int height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals (Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass () != o.getClass ()) {
                return false;
            }
            Car car = (Car) o;
            return id == car.id;
        }

        @Override
        public int hashCode () {
            return Objects.hash (id);
        }

        @Override
        public String toString () {
            return id + " -> " + width + "x" + height;
        }

    }

    private static final class Decision {

        public final Car car;
        public final boolean rotated;
        public final Point point;

        private Decision (Car car, boolean rotated, Point point) {
            this.car = car;
            this.rotated = rotated;
            this.point = new Point (point);
        }

        @Override
        public String toString () {
            return "put car (" + car + ") "
                    + (rotated ? "rotated" : "unrotated")
                    + " at " + point;
        }

    }

    private static final class Point {

        public final int x, y;

        private Point (int x, int y) {
            this.x = x;
            this.y = y;
        }

        private Point (Point other) {
            this.x = other.x;
            this.y = other.y;
        }

        @Override
        public boolean equals (Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass () != o.getClass ()) {
                return false;
            }
            Point point = (Point) o;
            return x == point.x &&
                    y == point.y;
        }

        @Override
        public int hashCode () {
            return Objects.hash (x, y);
        }

        @Override
        public String toString () {
            return "(" + x + ", " + y + ")";
        }

    }

    public static void main (String[] args) {

        Main m = new Main (System.in, System.out);
        m.solve ();

    }

}
