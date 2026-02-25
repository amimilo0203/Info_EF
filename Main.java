import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class Main {
    public static void  main(String[] args) {
        System.out.println("start");
        int size = 100;
        ArrayList<Integer> test = new ArrayList<>(size); for (int i = 0; i < size; i++) { test.add((int)(Math.random() * 101)); }
        long start = System.nanoTime();
        pqsort(test, 0, 99);
        long ende = System.nanoTime();
        long dauerNs = ende - start;
        double dauerSekunden = dauerNs / 1_000_000_000.0;
        System.out.println();
        System.out.println("Fertig: ");
        for (int i = 0; i < size; i++) {
            System.out.println(test.get(i));
        }
        System.out.println("Dauer: " + dauerSekunden + " s");


    }
    public static void pqsort(ArrayList<Integer> a, int lo, int hi) {
        if (lo >= hi) return;

        int p = partition(a, lo, hi);
        pqsort(a, lo, p - 1);
        pqsort(a, p + 1, hi);
    }

    public static int partition(ArrayList<Integer> a, int lo, int hi) {
        int pivot = a.get(hi);
        int i = lo - 1;

        for (int j = lo; j < hi; j++) {
            if (a.get(j) <= pivot) {
                i++;
                int temp = a.get(j);
                a.set(j, a.get(i));
                a.set(i, temp);
            }
        }

        int temp = a.get(i + 1);
        a.set(i + 1, a.get(hi));
        a.set(hi, temp);

        return i + 1;
    }
}