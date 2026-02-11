import java.util.concurrent.*;

public class OptimizedPQSort {
    private static final int mach_schneller = 10000;
    private static final int zu_klein = 47;
    private static final int zu_groß = 128;
    private static ForkJoinPool forkJoinPool;

    public static void main(String[] args) {
        System.out.println("start2");
        int size = 500_000;
        int[] test = new int[size];
        for (int i = 0; i < size; i++) {
            test[i] = (int)(Math.random() * 101);
        }

        forkJoinPool = new ForkJoinPool(4);

        // Warmup
        int[] warmup = new int[100];
        for (int i = 0; i < 100; i++) {
            warmup[i] = (int)(Math.random() * 101);
        }
        forkJoinPool.invoke(new SortTask(warmup, 0, warmup.length - 1));

        long startTime = System.nanoTime();
        forkJoinPool.invoke(new SortTask(test, 0, test.length - 1));
        long endTime = System.nanoTime();

        forkJoinPool.shutdown();

        double dauerS = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("Zeit: " + dauerS + " s");

        if (!isSorted(test)) {
            System.out.println("Array ist immernoch nicht Sortiert");
        }
    }

    static class SortTask extends RecursiveAction {
        private int[] a;
        private int lo, hi;

        SortTask(int[] a, int lo, int hi) {
            this.a = a;
            this.lo = lo;
            this.hi = hi;
        }

        @Override
        protected void compute() {
            int len = hi - lo + 1;

            if (len <= zu_klein) {
                insertionSort(a, lo, hi);
                return;
            }

            int p = partition(a, lo, hi);

            // 3-Way Partitioning für viele Duplikate
            if (len > mach_schneller) {
                invokeAll(new SortTask(a, lo, p - 1),
                        new SortTask(a, p + 1, hi));
            } else {
                new SortTask(a, lo, p - 1).compute();
                new SortTask(a, p + 1, hi).compute();
            }
        }
    }

    private static int partition(int[] a, int lo, int hi) {
        int len = hi - lo + 1;
        int pivotIndex;

        // Ninther für größere Arrays (bessere Pivot-Wahl)
        if (len > zu_groß) {
            int eps = len / 8;
            int mid = lo + len / 2;
            int m1 = median3(a, lo, lo + eps, lo + 2 * eps);
            int m2 = median3(a, mid - eps, mid, mid + eps);
            int m3 = median3(a, hi - 2 * eps, hi - eps, hi);
            pivotIndex = median3(a, m1, m2, m3);
        } else {
            // Median-of-3 für kleinere Arrays
            int mid = lo + len / 2;
            pivotIndex = median3(a, lo, mid, hi);
        }

        swap(a, pivotIndex, hi);
        int pivot = a[hi];

        // Hoare-Partitionierung (effizienter)
        int i = lo - 1;
        int j = hi;

        while (true) {
            while (a[++i] < pivot);
            while (j > lo && a[--j] > pivot);

            if (i >= j) break;
            swap(a, i, j);
        }

        swap(a, i, hi);
        return i;
    }

    // Median von 3 Elementen
    private static int median3(int[] a, int i, int j, int k) {
        return (a[i] < a[j]) ?
                (a[j] < a[k] ? j : a[i] < a[k] ? k : i) :
                (a[j] > a[k] ? j : a[i] > a[k] ? k : i);
    }

    // Optimiertes Insertion Sort mit binärer Suche für größere Elemente
    private static void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int key = a[i];

            if (key < a[lo]) {
                // Schneller Pfad: Element gehört an den Anfang
                System.arraycopy(a, lo, a, lo + 1, i - lo);
                a[lo] = key;
            } else {
                int j = i - 1;
                while (a[j] > key) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = key;
            }
        }
    }

    private static void swap(int[] a, int i, int j) {
        int temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    private static boolean isSorted(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] > a[i + 1]) {
                return false;
            }
        }
        return true;
    }
}