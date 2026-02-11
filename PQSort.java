import java.util.ArrayList;
import java.util.concurrent.*;

public class PQSort {
    private static final int PARALLEL_THRESHOLD = 10000;
    private static ForkJoinPool forkJoinPool;

    public static void main(String[] args) {
        System.out.println("start");
        int size = 500_000;
        ArrayList<Integer> test = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            test.add((int)(Math.random() * 101));
        }
        forkJoinPool = new ForkJoinPool();
        ArrayList<Integer> warmup = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            warmup.add((int)(Math.random() * 101));
        }
        forkJoinPool.invoke(new SortTask(warmup, 0, warmup.size() - 1));
        forkJoinPool.shutdown();


        forkJoinPool = new ForkJoinPool();


        long startTime = System.nanoTime();
        forkJoinPool.invoke(new SortTask(test, 0, test.size() - 1));
        long endTime = System.nanoTime();

        forkJoinPool.shutdown();

        double dauerS = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("Zeit: " + dauerS + " s");
    }

    static class SortTask extends RecursiveAction {
        private ArrayList<Integer> a;
        private int lo, hi;

        SortTask(ArrayList<Integer> a, int lo, int hi) {
            this.a = a;
            this.lo = lo;
            this.hi = hi;
        }

        @Override
        protected void compute() {
            if (lo >= hi) return;

            int p = partition(a, lo, hi);

            if (hi - lo > PARALLEL_THRESHOLD) {
                // Parallel ausführen
                invokeAll(new SortTask(a, lo, p - 1),
                        new SortTask(a, p + 1, hi));
            } else {
                // Seriell für kleine Arrays
                new SortTask(a, lo, p - 1).compute();
                new SortTask(a, p + 1, hi).compute();
            }
        }
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