import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class MultiThreadQSortViz extends JFrame {

    static final int SIZE = 200;
    static final int DELAY_PARALLEL = 100;
    static final int PARALLEL_THRESHOLD = 20;
    static final int INSERTION_THRESHOLD = 12;
    static final int NUM_THREADS = 4;
    static final int BAR_W = 5;

    // Thread colors (one per worker thread)
    static final Color[] THREAD_COLORS = {
            new Color(255, 100, 100),  // Red   - Thread 0
            new Color(100, 220, 100),  // Green - Thread 1
            new Color(100, 160, 255),  // Blue  - Thread 2
            new Color(255, 200, 80),   // Gold  - Thread 3
    };
    static final Color COLOR_IDLE    = new Color(60, 60, 80);
    static final Color COLOR_DONE    = new Color(80, 220, 160);
    static final Color COLOR_BG      = new Color(18, 18, 28);

    // ── shared state ──────────────────────────────────────────────────────────
    int[] array = new int[SIZE]; // parallel sort
    int[] colors = new int[SIZE]; // which thread last touched each bar (-1=idle, -2=done)

    // Thread lane activity: threadLanes[t] = {lo, hi, active}
    int[]     laneLo     = new int[NUM_THREADS];
    int[]     laneHi     = new int[NUM_THREADS];
    boolean[] laneActive = new boolean[NUM_THREADS];

    // Timeline: list of {time, thread, lo, hi, type}
    List<long[]> timeline = Collections.synchronizedList(new ArrayList<>());
    long sortStartTime;

    volatile boolean running = false;
    volatile long sortTime = 0;

    // Map ForkJoin worker index → our thread slot
    Map<String, Integer> threadSlotMap = new ConcurrentHashMap<>();
    AtomicInteger slotCounter = new AtomicInteger(0);

    // ── UI panels ─────────────────────────────────────────────────────────────
    ArrayPanel arrayPanel;
    TimelinePanel timelinePanel;
    JLabel labelTime, labelStatus;
    JButton btnStart;

    public MultiThreadQSortViz() {
        setTitle("Parallel QuickSort Visualizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setBackground(COLOR_BG);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout(4, 4));

        // ── top: array panel ─────────────────────────────────────────────
        arrayPanel = new ArrayPanel(array, colors, "Parallel QuickSort (4 Threads)");
        add(arrayPanel, BorderLayout.CENTER);

        // ── middle: thread timeline ────────────────────────────────────────────
        timelinePanel = new TimelinePanel();
        add(timelinePanel, BorderLayout.SOUTH);

        // ── bottom: controls + info ───────────────────────────────────────────
        labelTime   = styledLabel("–");
        labelStatus = styledLabel("Press Start");

        btnStart = new JButton("▶  Start");
        btnStart.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnStart.addActionListener(e -> startSort());

        JPanel info = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 6));
        info.setBackground(COLOR_BG);
        info.add(colorDot(THREAD_COLORS[0])); info.add(colorDot(THREAD_COLORS[1]));
        info.add(colorDot(THREAD_COLORS[2])); info.add(colorDot(THREAD_COLORS[3]));
        info.add(new JLabel("  "));
        info.add(styledLabel("Time: ")); info.add(labelTime);
        info.add(new JLabel("  "));
        info.add(labelStatus);
        info.add(btnStart);

        add(info, BorderLayout.NORTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        initArrays();
    }

    // ── helpers ───────────────────────────────────────────────────────────────
    JLabel styledLabel(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return l;
    }
    JLabel colorDot(Color c) {
        JLabel l = new JLabel("●");
        l.setForeground(c);
        l.setFont(new Font("SansSerif", Font.BOLD, 16));
        return l;
    }

    void initArrays() {
        Random rng = new Random(42);
        for (int i = 0; i < SIZE; i++) {
            int v = rng.nextInt(SIZE) + 1;
            array[i] = v;
            colors[i] = -1;
        }
        threadSlotMap.clear();
        slotCounter.set(0);
        Arrays.fill(laneActive, false);
        timeline.clear();
        arrayPanel.repaint();
        timelinePanel.repaint();
    }

    // ── sorting ────────────────────────────────────────────────────────────────
    void startSort() {
        if (running) return;
        running = true;
        btnStart.setEnabled(false);
        labelTime.setText("running…");
        labelStatus.setText("Sorting…");
        initArrays();

        new Thread(() -> {
            // run parallel
            ForkJoinPool pool = new ForkJoinPool(NUM_THREADS);
            sortStartTime = System.nanoTime();
            pool.invoke(new PQSortTask(array, colors, 0, SIZE - 1, true));
            sortTime = System.nanoTime() - sortStartTime;
            pool.shutdown();
            markAllDone(colors);
            arrayPanel.repaint();
            
            SwingUtilities.invokeLater(() -> {
                labelTime.setText(String.format("%.2f ms", sortTime / 1e6));
                labelStatus.setText("Complete");
                btnStart.setEnabled(true);
                running = false;
                timelinePanel.repaint();
            });
        }).start();
    }

    void markAllDone(int[] colors) {
        Arrays.fill(colors, -2);
    }

    // ── ForkJoin task ─────────────────────────────────────────────────────────
    class PQSortTask extends RecursiveAction {
        int[] a;
        int[] colors;
        int lo, hi;
        boolean parallel;

        PQSortTask(int[] a, int[] colors, int lo, int hi, boolean parallel) {
            this.a = a; this.colors = colors;
            this.lo = lo; this.hi = hi; this.parallel = parallel;
        }

        @Override
        protected void compute() {
            int len = hi - lo + 1;
            if (len <= 1) return;

            int slot = getSlot();
            paintRange(colors, lo, hi, slot);
            recordTimeline(slot, lo, hi, 0);
            sleep(DELAY_PARALLEL);

            if (len <= INSERTION_THRESHOLD) {
                insertionSort(a, colors, lo, hi, slot);
                return;
            }

            int[] p = partition(a, colors, lo, hi, slot);

            if (len > PARALLEL_THRESHOLD) {
                invokeAll(
                        new PQSortTask(a, colors, lo, p[0] - 1, true),
                        new PQSortTask(a, colors, p[0] + 1, p[1] - 1, true),
                        new PQSortTask(a, colors, p[1] + 1, hi, true)
                );
            } else {
                if (p[0] - 1 > lo)       new PQSortTask(a, colors, lo,         p[0] - 1, false).compute();
                if (p[1] - 1 > p[0] + 1) new PQSortTask(a, colors, p[0] + 1,  p[1] - 1, false).compute();
                if (hi > p[1] + 1)        new PQSortTask(a, colors, p[1] + 1,  hi,        false).compute();
            }
        }
    }

    int getSlot() {
        String name = Thread.currentThread().getName();
        return threadSlotMap.computeIfAbsent(name, k -> slotCounter.getAndIncrement() % NUM_THREADS);
    }

    void paintRange(int[] colors, int lo, int hi, int slot) {
        for (int i = lo; i <= hi; i++) colors[i] = slot;
        arrayPanel.repaint();
    }

    void recordTimeline(int slot, int lo, int hi, int type) {
        long t = System.nanoTime() - sortStartTime;
        timeline.add(new long[]{t, slot, lo, hi, type});
        timelinePanel.repaint();
    }

    void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ── dual-pivot partition ─────────────────────────────────────────────────
    int[] partition(int[] a, int[] colors, int lo, int hi, int slot) {
        int[] pivots = sampling5(a, lo, hi);
        int lp = pivots[0], up = pivots[1];
        a[lo] = lp; a[hi] = up;

        int l = lo + 1, s = lo + 1, h = hi - 1;
        while (s <= h) {
            if (a[s] < lp) {
                swap(a, s, l); l++; s++;
            } else if (a[s] > up) {
                swap(a, s, h); h--;
            } else {
                s++;
            }
            colors[s >= h ? h : s] = slot;
        }
        swap(a, lo, l - 1);
        swap(a, hi, h + 1);
        paintRange(colors, lo, hi, slot);
        sleep(DELAY_PARALLEL / 2);
        return new int[]{l - 1, h + 1};
    }

    int[] sampling5(int[] a, int lo, int hi) {
        int len = hi - lo + 1;
        int m1 = lo + len / 4, m2 = lo + len / 2, m3 = lo + 3 * len / 4;
        if (a[lo] > a[m1]) swap(a, lo, m1);
        if (a[m2] > a[m3]) swap(a, m2, m3);
        if (a[lo] > a[m2]) swap(a, lo, m2);
        if (a[m1] > a[m3]) swap(a, m1, m3);
        if (a[m1] > a[m2]) swap(a, m1, m2);
        swap(a, m1, lo); swap(a, m3, hi);
        return new int[]{a[lo], a[hi]};
    }

    void insertionSort(int[] a, int[] colors, int lo, int hi, int slot) {
        for (int i = lo + 1; i <= hi; i++) {
            int key = a[i];
            if (key < a[lo]) {
                System.arraycopy(a, lo, a, lo + 1, i - lo);
                a[lo] = key;
            } else {
                int j = i - 1;
                while (a[j] > key) { a[j + 1] = a[j]; j--; }
                a[j + 1] = key;
            }
            colors[i] = slot;
        }
        paintRange(colors, lo, hi, slot);
    }

    void swap(int[] a, int i, int j) {
        int t = a[i]; a[i] = a[j]; a[j] = t;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Array visualization panel
    // ══════════════════════════════════════════════════════════════════════════
    class ArrayPanel extends JPanel {
        int[] arr, colors;
        String title;

        ArrayPanel(int[] arr, int[] colors, String title) {
            this.arr = arr; this.colors = colors; this.title = title;
            setBackground(COLOR_BG);
            setPreferredSize(new Dimension(SIZE * BAR_W + 20, 260));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            int maxH = getHeight() - 30;
            int maxV = SIZE;

            for (int i = 0; i < arr.length; i++) {
                int bh = Math.max(2, (int)((double) arr[i] / maxV * maxH));
                int x  = 10 + i * BAR_W;
                int y  = maxH - bh + 5;

                int c = colors[i];
                Color col;
                if      (c == -2) col = COLOR_DONE;
                else if (c == -1) col = COLOR_IDLE;
                else              col = THREAD_COLORS[c % THREAD_COLORS.length];

                g2.setColor(col);
                g2.fillRect(x, y, BAR_W - 1, bh);
            }

            // title
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(Color.WHITE);
            g2.drawString(title, 10, getHeight() - 6);

            // thread color legend
            for (int t = 0; t < NUM_THREADS; t++) {
                g2.setColor(THREAD_COLORS[t]);
                g2.fillRect(130 + t * 40, getHeight() - 18, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.drawString("T" + t, 145 + t * 40, getHeight() - 6);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Thread-activity timeline panel - Fixed version
    // Shows a Gantt-style chart: x=time, y=thread lane, colored rectangles
    // ══════════════════════════════════════════════════════════════════════════
    class TimelinePanel extends JPanel {
        TimelinePanel() {
            setBackground(new Color(12, 12, 22));
            setPreferredSize(new Dimension(100, 140));
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(60, 60, 90)),
                    "Thread Activity Timeline (each row = one thread)",
                    0, 0, new Font("SansSerif", Font.PLAIN, 11), Color.LIGHT_GRAY));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (timeline.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            int W = getWidth() - 20;
            int H = getHeight() - 40;
            int laneH = H / NUM_THREADS;

            long maxT = 0;
            for (long[] e : timeline) if (e[0] > maxT) maxT = e[0];
            if (maxT == 0) maxT = 1;

            // draw lane backgrounds
            for (int t = 0; t < NUM_THREADS; t++) {
                int y = 25 + t * laneH;
                g2.setColor(new Color(28, 28, 45));
                g2.fillRect(10, y, W, laneH - 2);
                g2.setColor(new Color(60, 60, 80));
                g2.drawRect(10, y, W, laneH - 2);
                g2.setColor(THREAD_COLORS[t]);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.drawString("T" + t, 10 + W + 3, y + laneH / 2 + 4);
            }

            // draw blocks - Fixed timeline visualization
            synchronized (timeline) {
                List<long[]> snap = new ArrayList<>(timeline);
                // Group events into blocks for better visualization
                for (int i = 0; i < snap.size(); i++) {
                    long[] e = snap.get(i);
                    int thread = (int) e[1];
                    long startTime = e[0];
                    long endTime = startTime + 50000000; // 50ms default duration
                    
                    // Find next event from same thread to determine actual duration
                    for (int j = i + 1; j < snap.size(); j++) {
                        if (snap.get(j)[1] == thread) {
                            endTime = snap.get(j)[0];
                            break;
                        }
                    }

                    int x = 10 + (int)((double) startTime / maxT * W);
                    int x2 = 10 + (int)((double) endTime / maxT * W);
                    int y = 25 + thread * laneH + 2;

                    Color c = THREAD_COLORS[thread % THREAD_COLORS.length];
                    // Brightness based on partition range size
                    long range = e[3] - e[2];
                    float brightness = 0.6f + (float) range / SIZE * 0.4f;
                    c = new Color(
                        Math.min(255, (int)(c.getRed() * brightness)),
                        Math.min(255, (int)(c.getGreen() * brightness)),
                        Math.min(255, (int)(c.getBlue() * brightness))
                    );

                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 220));
                    g2.fillRect(x, y, Math.max(2, x2 - x - 1), laneH - 4);
                    
                    // Add border for better visibility
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 255));
                    g2.drawRect(x, y, Math.max(2, x2 - x - 1), laneH - 4);
                }
            }

            // time axis
            g2.setColor(new Color(120, 120, 140));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawLine(10, 25 + NUM_THREADS * laneH, 10 + W, 25 + NUM_THREADS * laneH);
            g2.drawString("0", 10, 25 + NUM_THREADS * laneH + 12);
            g2.drawString(String.format("%.0f ms", maxT / 1e6), 10 + W - 30, 25 + NUM_THREADS * laneH + 12);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MultiThreadQSortViz::new);
    }
}
