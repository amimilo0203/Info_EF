import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;

public class QSortVisualization extends JPanel {

    private static final int NUM_ELEMENTS = 100;
    private static final int BAR_WIDTH = 9;
    private static final int PANEL_HEIGHT = 500;
    private static final int DELAY_MS = 60;

    private ArrayList<Integer> array = new ArrayList<>();
    private int highlightPivot = -1;
    private int highlightI = -1;
    private int highlightJ = -1;
    private int highlightPartitionRange_lo = -1;
    private int highlightPartitionRange_hi = -1;
    private boolean sorting = false;

    public QSortVisualization() {
        setPreferredSize(new Dimension(NUM_ELEMENTS * BAR_WIDTH + 20, PANEL_HEIGHT + 60));
        setBackground(new Color(20, 20, 30));
        reset();
    }

    public void reset() {
        array.clear();
        for (int i = 1; i <= NUM_ELEMENTS; i++) array.add(i);
        Collections.shuffle(array);
        highlightPivot = highlightI = highlightJ = -1;
        highlightPartitionRange_lo = highlightPartitionRange_hi = -1;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int maxVal = NUM_ELEMENTS;
        int panelWidth = getWidth();
        int panelHeight = getHeight() - 50;

        for (int i = 0; i < array.size(); i++) {
            int val = array.get(i);
            int barHeight = (int) ((double) val / maxVal * (panelHeight - 20));
            int x = 10 + i * BAR_WIDTH;
            int y = panelHeight - barHeight;

            Color barColor;
            if (i == highlightPivot) {
                barColor = new Color(255, 80, 80);   // Red = pivot
            } else if (i == highlightI) {
                barColor = new Color(80, 255, 80);   // Green = i pointer
            } else if (i == highlightJ) {
                barColor = new Color(80, 180, 255);  // Blue = j pointer
            } else if (highlightPartitionRange_lo >= 0 && i >= highlightPartitionRange_lo && i <= highlightPartitionRange_hi) {
                barColor = new Color(180, 140, 255); // Purple = current partition
            } else {
                // Color gradient based on value
                float hue = (float) val / maxVal * 0.65f;
                barColor = Color.getHSBColor(hue, 0.8f, 0.9f);
            }

            g2.setColor(barColor);
            g2.fillRect(x, y, BAR_WIDTH - 1, barHeight);
        }

        // Legend
        int ly = getHeight() - 38;
        drawLegend(g2, 10, ly, new Color(255, 80, 80), "Pivot");
        drawLegend(g2, 90, ly, new Color(80, 255, 80), "i (swap pos)");
        drawLegend(g2, 210, ly, new Color(80, 180, 255), "j (scan)");
        drawLegend(g2, 330, ly, new Color(180, 140, 255), "Active partition");
    }

    private void drawLegend(Graphics2D g2, int x, int y, Color c, String label) {
        g2.setColor(c);
        g2.fillRect(x, y, 14, 14);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(label, x + 18, y + 12);
    }

    public void startSort() {
        if (sorting) return;
        sorting = true;
        new Thread(() -> {
            pqsort(array, 0, array.size() - 1);
            highlightPivot = highlightI = highlightJ = -1;
            highlightPartitionRange_lo = highlightPartitionRange_hi = -1;
            sorting = false;
            repaint();
        }).start();
    }

    private void pqsort(ArrayList<Integer> a, int lo, int hi) {
        if (lo >= hi) return;
        int p = partition(a, lo, hi);
        pqsort(a, lo, p - 1);
        pqsort(a, p + 1, hi);
    }

    private int partition(ArrayList<Integer> a, int lo, int hi) {
        highlightPartitionRange_lo = lo;
        highlightPartitionRange_hi = hi;
        highlightPivot = hi;

        int pivot = a.get(hi);
        int i = lo - 1;

        for (int j = lo; j < hi; j++) {
            highlightJ = j;
            highlightI = (i >= lo) ? i : -1;
            repaintAndSleep();

            if (a.get(j) <= pivot) {
                i++;
                int temp = a.get(j);
                a.set(j, a.get(i));
                a.set(i, temp);
                highlightI = i;
                repaintAndSleep();
            }
        }

        int temp = a.get(i + 1);
        a.set(i + 1, a.get(hi));
        a.set(hi, temp);
        highlightI = i + 1;
        highlightJ = -1;
        repaintAndSleep();

        return i + 1;
    }

    private void repaintAndSleep() {
        repaint();
        try { Thread.sleep(DELAY_MS); } catch (InterruptedException ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("QuickSort Visualization");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            QSortVisualization panel = new QSortVisualization();

            JButton sortBtn = new JButton("▶ Sort");
            JButton resetBtn = new JButton("↺ Reset");

            sortBtn.addActionListener(e -> panel.startSort());
            resetBtn.addActionListener(e -> {
                if (!panel.sorting) panel.reset();
            });

            sortBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
            resetBtn.setFont(new Font("SansSerif", Font.BOLD, 13));

            JPanel controls = new JPanel();
            controls.setBackground(new Color(30, 30, 45));
            controls.add(sortBtn);
            controls.add(resetBtn);

            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            frame.add(controls, BorderLayout.SOUTH);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}