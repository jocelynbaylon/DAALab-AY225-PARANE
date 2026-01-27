package PRELIM_LABWORK2;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BubbleSortDescending extends JFrame {
    // Minimalist color palette
    private static final Color BG_PRIMARY = new Color(250, 250, 250);
    private static final Color BG_SECONDARY = new Color(255, 255, 255);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    private static final Color ACCENT = new Color(0, 0, 0);
    private static final Color SUCCESS = new Color(76, 175, 80);
    private static final Color ERROR = new Color(244, 67, 54);
    
    private JTextArea inputArea;
    private JTextArea outputArea;
    private JTextArea statsArea;
    private MinimalButton loadFileButton;
    private MinimalButton sortButton;
    private MinimalButton clearButton;
    private JComboBox<String> algorithmSelector;
    private JLabel statusLabel;
    private List<Double> currentData;
    private List<SortResult> sortHistory;
    
    public BubbleSortDescending() {
        setTitle("Sorting Visualizer");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_PRIMARY);
        
        currentData = new ArrayList<>();
        sortHistory = new ArrayList<>();
        
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_SECONDARY);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(25, 40, 25, 40)
        ));
        
        JLabel titleLabel = new JLabel("Sorting Visualizer");
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 24));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(BG_PRIMARY);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controlPanel.setBackground(BG_PRIMARY);
        
        String[] algorithms = {"Bubble Sort (Desc)", "Insertion Sort (Asc)", "Merge Sort (Asc)"};
        algorithmSelector = new JComboBox<>(algorithms);
        algorithmSelector.setFont(new Font("Arial", Font.PLAIN, 13));
        algorithmSelector.setPreferredSize(new Dimension(180, 36));
        styleComboBox(algorithmSelector);
        
        loadFileButton = new MinimalButton("Load File");
        loadFileButton.addActionListener(e -> loadFile());
        
        sortButton = new MinimalButton("Sort");
        sortButton.setEnabled(false);
        sortButton.addActionListener(e -> performSort());
        
        clearButton = new MinimalButton("Clear");
        clearButton.addActionListener(e -> clearAll());
        
        controlPanel.add(algorithmSelector);
        controlPanel.add(loadFileButton);
        controlPanel.add(sortButton);
        controlPanel.add(clearButton);
        
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        
        // Data panels
        JPanel dataContainer = new JPanel(new GridLayout(1, 3, 20, 0));
        dataContainer.setBackground(BG_PRIMARY);
        
        // Input card
        JPanel inputCard = createDataCard("Input");
        inputArea = new JTextArea();
        styleTextArea(inputArea, true);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        styleScrollPane(inputScroll);
        inputCard.add(inputScroll, BorderLayout.CENTER);
        
        // Output card
        JPanel outputCard = createDataCard("Output");
        outputArea = new JTextArea();
        styleTextArea(outputArea, false);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        styleScrollPane(outputScroll);
        outputCard.add(outputScroll, BorderLayout.CENTER);
        
        // Stats card
        JPanel statsCard = createDataCard("Statistics");
        statsArea = new JTextArea();
        styleTextArea(statsArea, false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane statsScroll = new JScrollPane(statsArea);
        styleScrollPane(statsScroll);
        statsCard.add(statsScroll, BorderLayout.CENTER);
        
        dataContainer.add(inputCard);
        dataContainer.add(outputCard);
        dataContainer.add(statsCard);
        
        mainPanel.add(dataContainer, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Footer panel
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(BG_SECONDARY);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(15, 40, 15, 40)
        ));
        
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        statusLabel.setForeground(TEXT_SECONDARY);
        
        footerPanel.add(statusLabel, BorderLayout.WEST);
        
        add(footerPanel, BorderLayout.SOUTH);
        
        updateStatsDisplay();
    }
    
    private void styleComboBox(JComboBox<String> combo) {
        combo.setBackground(BG_SECONDARY);
        combo.setForeground(TEXT_PRIMARY);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        combo.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    private JPanel createDataCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(BG_SECONDARY);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        titleLabel.setForeground(TEXT_SECONDARY);
        
        card.add(titleLabel, BorderLayout.NORTH);
        
        return card;
    }
    
    private void styleTextArea(JTextArea area, boolean editable) {
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBackground(BG_SECONDARY);
        area.setForeground(TEXT_PRIMARY);
        area.setCaretColor(TEXT_PRIMARY);
        area.setEditable(editable);
        area.setLineWrap(false);
        area.setMargin(new Insets(8, 8, 8, 8));
        area.setBorder(null);
    }
    
    private void styleScrollPane(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scroll.getVerticalScrollBar().setUI(new MinimalScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new MinimalScrollBarUI());
    }
    
    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            List<Double> data = readDataset(file.getAbsolutePath());
            
            if (data != null && !data.isEmpty()) {
                currentData = data;
                displayInputData();
                sortButton.setEnabled(true);
                statusLabel.setText("Loaded " + data.size() + " elements");
            } else {
                statusLabel.setText("Failed to load file");
            }
        }
    }
    
    private void displayInputData() {
        StringBuilder sb = new StringBuilder();
        for (Double num : currentData) {
            if (num == num.longValue()) {
                sb.append(num.longValue()).append("\n");
            } else {
                sb.append(num).append("\n");
            }
        }
        inputArea.setText(sb.toString());
    }
    
    private void performSort() {
        String manualInput = inputArea.getText().trim();
        if (!manualInput.isEmpty()) {
            currentData = parseManualInput(manualInput);
        }
        
        if (currentData == null || currentData.isEmpty()) {
            statusLabel.setText("No data to sort");
            return;
        }
        
        double[] dataArray = new double[currentData.size()];
        for (int i = 0; i < currentData.size(); i++) {
            dataArray[i] = currentData.get(i);
        }
        
        String algorithm = (String) algorithmSelector.getSelectedItem();
        statusLabel.setText("Sorting...");
        
        SwingWorker<SortResult, Void> worker = new SwingWorker<SortResult, Void>() {
            @Override
            protected SortResult doInBackground() {
                if (algorithm.contains("Bubble")) {
                    return bubbleSortDescending(dataArray);
                } else if (algorithm.contains("Insertion")) {
                    return insertionSortAscending(dataArray);
                } else {
                    return mergeSortAscending(dataArray);
                }
            }
            
            @Override
            protected void done() {
                try {
                    SortResult result = get();
                    result.algorithmName = algorithm;
                    sortHistory.add(result);
                    displaySortedData(result);
                    updateStatsDisplay();
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                }
            }
        };
        
        worker.execute();
    }
    
    private List<Double> parseManualInput(String input) {
        List<Double> numbers = new ArrayList<>();
        String[] lines = input.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            try {
                numbers.add(Double.parseDouble(line));
            } catch (NumberFormatException e) {
                String[] parts = line.contains(",") ? line.split(",") : line.split("\\s+");
                for (String part : parts) {
                    part = part.trim();
                    if (!part.isEmpty()) {
                        try {
                            numbers.add(Double.parseDouble(part));
                        } catch (NumberFormatException ex) {
                            // Skip invalid numbers
                        }
                    }
                }
            }
        }
        
        return numbers;
    }
    
    private void displaySortedData(SortResult result) {
        StringBuilder sb = new StringBuilder();
        
        for (double num : result.sortedArray) {
            if (num == (long) num) {
                sb.append((long) num).append("\n");
            } else {
                sb.append(num).append("\n");
            }
        }
        
        outputArea.setText(sb.toString());
        
        statusLabel.setText(String.format("Completed in %.6f seconds", result.timeTaken));
    }
    
    private void updateStatsDisplay() {
        if (sortHistory.isEmpty()) {
            statsArea.setText("No operations performed yet.\n\nRun a sort to see statistics.");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        
        SortResult latest = sortHistory.get(sortHistory.size() - 1);
        sb.append("Latest Sort\n");
        sb.append("─────────────────────────\n");
        sb.append(String.format("Algorithm: %s\n", latest.algorithmName));
        sb.append(String.format("Elements: %d\n", latest.sortedArray.length));
        sb.append(String.format("Time: %.6f s\n\n", latest.timeTaken));
        
        if (sortHistory.size() > 1) {
            sb.append("History\n");
            sb.append("─────────────────────────\n");
            
            for (int i = sortHistory.size() - 1; i >= 0 && i >= sortHistory.size() - 5; i--) {
                SortResult r = sortHistory.get(i);
                sb.append(String.format("%s\n", r.algorithmName));
                sb.append(String.format("%d elements, %.6f s\n\n", 
                    r.sortedArray.length, r.timeTaken));
            }
        }
        
        statsArea.setText(sb.toString());
        statsArea.setCaretPosition(0);
    }
    
    private void clearAll() {
        inputArea.setText("");
        outputArea.setText("");
        currentData.clear();
        sortButton.setEnabled(false);
        statusLabel.setText("Ready");
    }
    
    // ========== SORTING ALGORITHMS ==========
    
    public static SortResult bubbleSortDescending(double[] arr) {
        long startTime = System.nanoTime();
        int n = arr.length;
        
        for (int i = 0; i < n; i++) {
            boolean swapped = false;
            
            for (int j = 0; j < n - i - 1; j++) {
                if (arr[j] < arr[j + 1]) {
                    double temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }
            
            if (!swapped) break;
        }
        
        long endTime = System.nanoTime();
        double timeTaken = (endTime - startTime) / 1_000_000_000.0;
        
        return new SortResult(arr, timeTaken);
    }
    
    public static SortResult insertionSortAscending(double[] arr) {
        long startTime = System.nanoTime();
        int n = arr.length;
        
        for (int i = 1; i < n; i++) {
            double key = arr[i];
            int j = i - 1;
            
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            
            arr[j + 1] = key;
        }
        
        long endTime = System.nanoTime();
        double timeTaken = (endTime - startTime) / 1_000_000_000.0;
        
        return new SortResult(arr, timeTaken);
    }
    
    public static SortResult mergeSortAscending(double[] arr) {
        long startTime = System.nanoTime();
        
        if (arr.length > 1) {
            mergeSortHelper(arr, 0, arr.length - 1);
        }
        
        long endTime = System.nanoTime();
        double timeTaken = (endTime - startTime) / 1_000_000_000.0;
        
        return new SortResult(arr, timeTaken);
    }
    
    private static void mergeSortHelper(double[] arr, int left, int right) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            
            mergeSortHelper(arr, left, mid);
            mergeSortHelper(arr, mid + 1, right);
            merge(arr, left, mid, right);
        }
    }
    
    private static void merge(double[] arr, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;
        
        double[] L = new double[n1];
        double[] R = new double[n2];
        
        for (int i = 0; i < n1; i++) {
            L[i] = arr[left + i];
        }
        for (int j = 0; j < n2; j++) {
            R[j] = arr[mid + 1 + j];
        }
        
        int i = 0, j = 0, k = left;
        
        while (i < n1 && j < n2) {
            if (L[i] <= R[j]) {
                arr[k] = L[i];
                i++;
            } else {
                arr[k] = R[j];
                j++;
            }
            k++;
        }
        
        while (i < n1) {
            arr[k] = L[i];
            i++;
            k++;
        }
        
        while (j < n2) {
            arr[k] = R[j];
            j++;
            k++;
        }
    }
    
    public static List<Double> readDataset(String filename) {
        List<Double> numbers = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                try {
                    numbers.add(Double.parseDouble(line));
                } catch (NumberFormatException e) {
                    String[] parts = line.contains(",") ? line.split(",") : line.split("\\s+");
                    for (String part : parts) {
                        part = part.trim();
                        if (!part.isEmpty()) {
                            try {
                                numbers.add(Double.parseDouble(part));
                            } catch (NumberFormatException ex) {
                                // Skip invalid
                            }
                        }
                    }
                }
            }
            
            return numbers;
            
        } catch (IOException e) {
            return null;
        }
    }
    
    static class SortResult {
        double[] sortedArray;
        double timeTaken;
        String algorithmName;
        
        SortResult(double[] sortedArray, double timeTaken) {
            this.sortedArray = sortedArray;
            this.timeTaken = timeTaken;
            this.algorithmName = "";
        }
    }
    
    static class MinimalButton extends JButton {
        private boolean isHovered = false;
        
        public MinimalButton(String text) {
            super(text);
            
            setFont(new Font("Arial", Font.PLAIN, 13));
            setForeground(TEXT_PRIMARY);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(100, 36));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        isHovered = true;
                        repaint();
                    }
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (isEnabled()) {
                if (isHovered) {
                    g2.setColor(ACCENT);
                } else {
                    g2.setColor(BG_SECONDARY);
                }
            } else {
                g2.setColor(new Color(240, 240, 240));
            }
            
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            
            g2.setColor(BORDER_COLOR);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
            
            g2.dispose();
            
            if (isHovered && isEnabled()) {
                setForeground(Color.WHITE);
            } else if (isEnabled()) {
                setForeground(TEXT_PRIMARY);
            } else {
                setForeground(TEXT_SECONDARY);
            }
            
            super.paintComponent(g);
        }
    }
    
    static class MinimalScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(200, 200, 200);
            this.trackColor = BG_SECONDARY;
        }
        
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }
        
        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }
        
        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            return button;
        }
        
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 4, 4);
            g2.dispose();
        }
        
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(trackColor);
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.dispose();
        }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            BubbleSortDescending gui = new BubbleSortDescending();
            gui.setVisible(true);
        });
    }
}