import java.util.concurrent.*;


public class OptimizedPQSort {
    private static int mach_schneller = 10000;
    private static int zu_klein = 47;
    private static ForkJoinPool Parrallel;

    public static void main(String[] args) {
        System.out.println("start2");
        int size = 100_000;
        int[] test = new int[size];
        for (int i = 0; i < size; i++) {
            test[i] = (int)(Math.random() * 10_001);
        }

        Parrallel = new ForkJoinPool(4);

        // Warmup
        int[] warmup = new int[100];
        for (int i = 0; i < 100; i++) {
            warmup[i] = (int)(Math.random() * 101);
        }
        Parrallel.invoke(new PQSort(warmup, 0, warmup.length - 1));

        long startTime = System.nanoTime();
        Parrallel.invoke(new PQSort(test, 0, test.length - 1));
        long endTime = System.nanoTime();

        Parrallel.shutdown();

        double dauerS = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("Zeit: " + dauerS + " s");

        if (!isSorted(test)) {
            System.out.println("Array ist immernoch nicht Sortiert");
            for (int i = 0; i < test.length; i++) {
                System.out.print(test[i] + " ");
            }
        }
    }

    static class PQSort extends RecursiveAction {
        private int[] a;
        private int lo, hi;

        PQSort (int[] a, int lo, int hi) {
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

            int[] p = partition(a, lo, hi);

            //"multithreading"
            if (len > mach_schneller) {
                invokeAll(
                new PQSort(a, lo, p[0] - 1),
                new PQSort(a, p[0] + 1, p[1] - 1),
                new PQSort(a, p[1] + 1, hi)
                );
            } else {
                if (p[0] - 1 > lo) new PQSort(a, lo, p[0] - 1).compute();
                if (p[1] - 1 > p[0] + 1) new PQSort(a, p[0] + 1, p[1] - 1).compute();
                if (hi > p[1] + 1) new PQSort(a, p[1] + 1, hi).compute();
            }
        }
    }

    private static int[] partition(int[] a, int lo, int hi) {
        int[] pivots = sampling5(a, lo, hi);
        int lowerP = pivots[0];
        int upperP = pivots[1];

        a[lo] = lowerP;
        a[hi] = upperP;

        int l = lo + 1;
        int s = lo + 1;
        int h = hi - 1;

        while (s <= h) {
            if (a[s] < lowerP) {
                swap(a, s, l);
                l++;
                s++;
            } else if (a[s] > upperP) {
                swap(a, s, h);
                h--;
            } else {
                s++;
            }
        }
        swap(a, lo, l - 1);
        swap(a, hi, h + 1);

        return new int[]{l - 1, h + 1};
    }

    /*
    * m1 = 1/4
    * m2 = 1/2
    * m3 = 3/4
    * */
    private static int[] sampling5(int[] a, int lo, int hi) {
        int len = hi - lo + 1;
        int m1 = lo + len / 4;
        int m2 = lo + len / 2;
        int m3 = lo + 3 * len / 4;

        if (a[lo] > a[m1]) swap(a, lo, m1);
        if (a[m2] > a[m3]) swap(a, m2, m3);
        if (a[lo] > a[m2]) swap(a, lo, m2);
        if (a[m1] > a[m3]) swap(a, m1, m3);
        if (a[m1] > a[m2]) swap(a, m1, m2);

        swap(a, m1, lo);
        swap(a, m3, hi);

        return new int[]{a[lo], a[hi]};
    }
    //Insertion Sort für wenig Elemente
    private static void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int key = a[i];

            if (key < a[lo]) {
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
        boolean flag = true;
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] > a[i + 1]) {
                System.out.println(a[i] + " > " + a[i + 1]);
                flag = false;
            }
        }

        return flag ? true : false;
    }
}
