import java.util.ArrayList;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends Thread{

    public static void main(String[] args) {
        System.out.println("start");
        int size = 1000;
        ArrayList<Integer> test = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            test.add((int)(Math.random() * 101));
        }
        long startTime = System.nanoTime();
        pqsort(test, 0, test.size() - 1);
        long endTime = System.nanoTime();

        long duration = (endTime - startTime);
        //for (Object o : test) System.out.println(o);
        System.out.println(duration/ 1000000);
    }
    public static void pqsort(ArrayList<Integer> a, int lo, int hi){
        if (lo >= hi) return;

        int p = partition(a, lo, hi);
        if(hi - lo > 1000){
            Thread t1;
            t1.pqsort(a, lo, p - 1);
            pqsort(a, p + 1, hi);
            t1.start();
        }else {
            pqsort(a, lo, p - 1);
            pqsort(a, p + 1, hi);
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
