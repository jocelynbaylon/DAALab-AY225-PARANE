import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class SortingStressTest extends JFrame {
    private JButton chooseFileButton;
    private JTextField fileField;
    private JComboBox<String> algorithmCombo;
    private JComboBox<String> columnCombo;
    private JSpinner nSpinner;
    private JButton startButton;
    private JProgressBar progressBar;
    private JLabel loadTimeLabel;
    private JLabel sortTimeLabel;
    private JTable resultTable;
    private JPanel bottom; // made field so we can update title dynamically

    private File csvFile;

    // Design colors
    private static final Color PRIMARY_COLOR = new Color(63, 81, 181);
    private static final Color PRIMARY_DARK = new Color(48, 63, 159);
    private static final Color ACCENT_COLOR = new Color(255, 87, 34);
    private static final Color BACKGROUND = new Color(250, 250, 250);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private static final Color TEXT_SECONDARY = new Color(117, 117, 117);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color BORDER_COLOR = new Color(224, 224, 224);

    public SortingStressTest() {
        super("Sorting Algorithm Stress Test");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 700);
        setLocationRelativeTo(null);
        
        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback to default
        }
        
        // Main panel with background color
        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top panel: file chooser with card design
        JPanel top = createCardPanel();
        top.setLayout(new BorderLayout(10, 10));
        top.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // File selection panel
        JPanel filePanel = new JPanel(new BorderLayout(10, 10));
        filePanel.setOpaque(false);
        
        JLabel fileLabel = new JLabel("📁 Select CSV File");
        fileLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        fileLabel.setForeground(TEXT_PRIMARY);
        filePanel.add(fileLabel, BorderLayout.NORTH);
        
        JPanel fileInputPanel = new JPanel(new BorderLayout(8, 8));
        fileInputPanel.setOpaque(false);
        fileField = new JTextField();
        fileField.setEditable(false);
        fileField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fileField.setBackground(BACKGROUND);
        fileField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        chooseFileButton = createStyledButton("Browse...", PRIMARY_COLOR);
        fileInputPanel.add(fileField, BorderLayout.CENTER);
        fileInputPanel.add(chooseFileButton, BorderLayout.EAST);
        filePanel.add(fileInputPanel, BorderLayout.CENTER);
        top.add(filePanel, BorderLayout.NORTH);

        // Options panel with better spacing
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        options.setOpaque(false);
        options.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(15, 0, 5, 0)
        ));
        
        JLabel optionsTitle = new JLabel("⚙️ Configuration");
        optionsTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        optionsTitle.setForeground(TEXT_PRIMARY);
        
        algorithmCombo = createStyledComboBox(new String[] {"Bubble Sort", "Insertion Sort", "Merge Sort"});
        columnCombo = createStyledComboBox(new String[] {"ID", "FirstName", "LastName"});
        nSpinner = new JSpinner(new SpinnerNumberModel(1000, 1, Integer.MAX_VALUE, 1000));
        styleSpinner(nSpinner);
        
        startButton = createStyledButton("▶ Start Test", SUCCESS_COLOR);
        startButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        JPanel configPanel = new JPanel(new BorderLayout());
        configPanel.setOpaque(false);
        configPanel.add(optionsTitle, BorderLayout.NORTH);
        
        JPanel configOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        configOptions.setOpaque(false);
        configOptions.add(createLabeledComponent("Algorithm:", algorithmCombo));
        configOptions.add(createLabeledComponent("Sort Column:", columnCombo));
        configOptions.add(createLabeledComponent("Number of Rows:", nSpinner));
        configOptions.add(startButton);
        
        configPanel.add(configOptions, BorderLayout.CENTER);
        top.add(configPanel, BorderLayout.SOUTH);

        mainPanel.add(top, BorderLayout.NORTH);

        // Middle: progress and times with card design
        JPanel middle = new JPanel(new GridLayout(2, 1, 0, 15));
        middle.setOpaque(false);
        
        // Progress panel
        JPanel progressPanel = createCardPanel();
        progressPanel.setLayout(new BorderLayout(10, 10));
        progressPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel progressTitle = new JLabel("📊 Progress");
        progressTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        progressTitle.setForeground(TEXT_PRIMARY);
        progressPanel.add(progressTitle, BorderLayout.NORTH);
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progressBar.setForeground(PRIMARY_COLOR);
        progressBar.setBackground(BACKGROUND);
        progressBar.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        progressBar.setPreferredSize(new Dimension(0, 30));
        
        JPanel timesPanel = new JPanel(new GridLayout(2, 1, 5, 8));
        timesPanel.setOpaque(false);
        timesPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        loadTimeLabel = createStyledLabel("⏱️ Load time: -");
        sortTimeLabel = createStyledLabel("⚡ Sort time: -");
        timesPanel.add(loadTimeLabel);
        timesPanel.add(sortTimeLabel);

        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(timesPanel, BorderLayout.SOUTH);
        middle.add(progressPanel);

        // Bottom: table for sorted results
        bottom = createCardPanel();
        bottom.setLayout(new BorderLayout(10, 10));
        bottom.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel tableTitle = new JLabel("📋 Sorted Records");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tableTitle.setForeground(TEXT_PRIMARY);
        bottom.add(tableTitle, BorderLayout.NORTH);
        
        // Initialize with styled table
        resultTable = new JTable(new DefaultTableModel(new Object[]{"Value"}, 0));
        styleTable(resultTable);
        resultTable.setAutoCreateRowSorter(true);
        
        JScrollPane scroll = new JScrollPane(resultTable);
        scroll.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        scroll.getViewport().setBackground(Color.WHITE);
        bottom.add(scroll, BorderLayout.CENTER);
        middle.add(bottom);

        mainPanel.add(middle, BorderLayout.CENTER);
        add(mainPanel);

        // Event handlers
        chooseFileButton.addActionListener(e -> chooseFile());
        startButton.addActionListener(e -> startProcess());

        // Double click table row to copy content
        resultTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = resultTable.getSelectedRow();
                    if (r >= 0) {
                        int modelRow = resultTable.convertRowIndexToModel(r);
                        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
                        int cols = model.getColumnCount();
                        StringBuilder sb = new StringBuilder();
                        for (int c = 0; c < cols; c++) {
                            String colName = model.getColumnName(c);
                            Object val = model.getValueAt(modelRow, c);
                            if (c > 0) sb.append(", ");
                            sb.append(colName).append(": ").append(Objects.toString(val, ""));
                        }
                        JOptionPane.showMessageDialog(SortingStressTest.this, sb.toString());
                    }
                }
            }
        });
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BACKGROUND);
        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return combo;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setFont(new Font("Segoe UI", Font.PLAIN, 12));
        }
        spinner.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    private JPanel createLabeledComponent(String labelText, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);
        panel.add(label, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.setShowGrid(true);
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(new Color(232, 240, 254));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setBackground(Color.WHITE);
        table.setIntercellSpacing(new Dimension(1, 1));
        
        // Style header
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));
        header.setBorder(BorderFactory.createEmptyBorder());
        
        // Center align cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            csvFile = chooser.getSelectedFile();
            fileField.setText(csvFile.getAbsolutePath());
        }
    }

    private void startProcess() {
        if (csvFile == null || !csvFile.exists()) {
            JOptionPane.showMessageDialog(this, "Please choose a valid CSV file first.", "No file", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int n = ((Number) nSpinner.getValue()).intValue();
        String alg = (String) algorithmCombo.getSelectedItem();
        String column = (String) columnCombo.getSelectedItem();

        // Warn for O(n^2)
        if ((alg.equals("Bubble Sort") || alg.equals("Insertion Sort")) && n > 30000) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Selected algorithm is O(n^2). Sorting " + n + " rows may take a very long time. Continue?",
                    "Potentially long operation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }

        // Update bottom panel title to reflect selected column
        JLabel tableTitle = new JLabel("📋 Sorted Records (by " + column + ")");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tableTitle.setForeground(TEXT_PRIMARY);
        bottom.removeAll();
        bottom.add(tableTitle, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(resultTable);
        scroll.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        scroll.getViewport().setBackground(Color.WHITE);
        bottom.add(scroll, BorderLayout.CENTER);
        bottom.revalidate();
        bottom.repaint();

        startButton.setEnabled(false);
        progressBar.setValue(0);
        loadTimeLabel.setText("⏱️ Load time: -");
        sortTimeLabel.setText("⚡ Sort time: -");

        SortTask task = new SortTask(csvFile, n, alg, column);
        task.execute();
    }

    /**
     * Update table to show ONLY the selected column's values (in sorted order).
     */
    private void updateResultTable(List<Record> list, String column) {
        String[] cols = new String[] { column };
        Object[][] data = new Object[list.size()][1];
        for (int i = 0; i < list.size(); i++) {
            Record r = list.get(i);
            switch (column) {
                case "ID":
                    data[i][0] = r.id;
                    break;
                case "FirstName":
                    data[i][0] = r.firstName;
                    break;
                case "LastName":
                    data[i][0] = r.lastName;
                    break;
                default:
                    data[i][0] = "";
            }
        }
        DefaultTableModel model = new DefaultTableModel(data, cols) {
            // make cells non-editable
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable.setModel(model);
        styleTable(resultTable);
        resultTable.setAutoCreateRowSorter(true);
    }

    private class SortTask extends SwingWorker<List<Record>, Integer> {
        private final File file;
        private final int n;
        private final String algorithm;
        private final String column;
        private long loadMillis = -1;
        private long sortMillis = -1;
        private Exception error = null;

        public SortTask(File file, int n, String algorithm, String column) {
            this.file = file;
            this.n = n;
            this.algorithm = algorithm;
            this.column = column;
        }

        @Override
        protected List<Record> doInBackground() {
            try {
                // 1) Load N rows and measure time
                long t0 = System.nanoTime();
                List<Record> data = loadCsv(file, n);
                long t1 = System.nanoTime();
                loadMillis = (t1 - t0) / 1_000_000;

                if (data.isEmpty()) {
                    return data;
                }

                // Prepare comparator
                Comparator<Record> cmp;
                switch (column) {
                    case "ID":
                        cmp = Comparator.comparingInt(r -> r.id);
                        break;
                    case "FirstName":
                        cmp = Comparator.comparing(r -> r.firstName, String.CASE_INSENSITIVE_ORDER);
                        break;
                    case "LastName":
                        cmp = Comparator.comparing(r -> r.lastName, String.CASE_INSENSITIVE_ORDER);
                        break;
                    default:
                        cmp = Comparator.comparingInt(r -> r.id);
                }

                // 2) Sort and measure time
                long st0 = System.nanoTime();
                progressBar.setIndeterminate(false);

                if (algorithm.equals("Bubble Sort")) {
                    bubbleSort(data, cmp);
                } else if (algorithm.equals("Insertion Sort")) {
                    insertionSort(data, cmp);
                } else { // Merge Sort
                    progressBar.setIndeterminate(true);
                    List<Record> sorted = mergeSort(data, cmp);
                    data = sorted;
                    progressBar.setIndeterminate(false);
                    setProgress(100);
                }

                long st1 = System.nanoTime();
                sortMillis = (st1 - st0) / 1_000_000;
                return data;
            } catch (Exception ex) {
                error = ex;
                return Collections.emptyList();
            }
        }

        private List<Record> loadCsv(File file, int maxRows) throws IOException {
            // initialize with expected size to reduce reallocation
            List<Record> list = new ArrayList<>(Math.min(Math.max(100, maxRows), maxRows));
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                // Assume header on first line. If not, this will skip first record.
                boolean first = true;
                while ((line = br.readLine()) != null && list.size() < maxRows) {
                    if (first) { first = false; continue; }
                    // Basic CSV split by comma. Assumes commas not embedded in fields.
                    String[] parts = line.split(",", -1);
                    if (parts.length < 3) continue;
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        String fn = parts[1].trim();
                        String ln = parts[2].trim();
                        list.add(new Record(id, fn, ln));
                    } catch (NumberFormatException nfe) {
                        // skip malformed line
                    }
                }
            }
            return list;
        }

        private void bubbleSort(List<Record> data, Comparator<Record> cmp) {
            int n = data.size();
            // Convert to array for faster swaps
            Record[] arr = data.toArray(new Record[0]);
            for (int i = 0; i < n - 1; i++) {
                boolean swapped = false;
                for (int j = 0; j < n - 1 - i; j++) {
                    if (cmp.compare(arr[j], arr[j + 1]) > 0) {
                        Record tmp = arr[j];
                        arr[j] = arr[j + 1];
                        arr[j + 1] = tmp;
                        swapped = true;
                    }
                }
                int progress = (int) (((i + 1) / (double) n) * 100);
                setProgress(progress);
                if (!swapped) break;
            }
            // copy back to list
            data.clear();
            data.addAll(Arrays.asList(arr));
            setProgress(100);
        }

        private void insertionSort(List<Record> data, Comparator<Record> cmp) {
            int n = data.size();
            Record[] arr = data.toArray(new Record[0]);
            for (int i = 1; i < n; i++) {
                Record key = arr[i];
                int j = i - 1;
                while (j >= 0 && cmp.compare(arr[j], key) > 0) {
                    arr[j + 1] = arr[j];
                    j--;
                }
                arr[j + 1] = key;
                if (i % 100 == 0 || i == n - 1) {
                    int progress = (int) (((i + 1) / (double) n) * 100);
                    setProgress(progress);
                }
            }
            data.clear();
            data.addAll(Arrays.asList(arr));
            setProgress(100);
        }

        private List<Record> mergeSort(List<Record> data, Comparator<Record> cmp) {
            Record[] arr = data.toArray(new Record[0]);
            mergeSortRec(arr, 0, arr.length - 1, cmp);
            return new ArrayList<>(Arrays.asList(arr));
        }

        private void mergeSortRec(Record[] arr, int l, int r, Comparator<Record> cmp) {
            if (l >= r) return;
            int m = l + (r - l) / 2;
            mergeSortRec(arr, l, m, cmp);
            mergeSortRec(arr, m + 1, r, cmp);
            merge(arr, l, m, r, cmp);
        }

        private void merge(Record[] arr, int l, int m, int r, Comparator<Record> cmp) {
            int n1 = m - l + 1;
            int n2 = r - m;
            Record[] L = new Record[n1];
            Record[] R = new Record[n2];
            System.arraycopy(arr, l, L, 0, n1);
            System.arraycopy(arr, m + 1, R, 0, n2);
            int i = 0, j = 0, k = l;
            while (i < n1 && j < n2) {
                if (cmp.compare(L[i], R[j]) <= 0) {
                    arr[k++] = L[i++];
                } else {
                    arr[k++] = R[j++];
                }
            }
            while (i < n1) arr[k++] = L[i++];
            while (j < n2) arr[k++] = R[j++];
        }

        @Override
        protected void process(List<Integer> chunks) {
            if (!chunks.isEmpty()) {
                int p = chunks.get(chunks.size() - 1);
                progressBar.setValue(p);
            }
        }

        @Override
        protected void done() {
            startButton.setEnabled(true);
            try {
                List<Record> result = get();
                if (error != null) {
                    JOptionPane.showMessageDialog(SortingStressTest.this, "Error: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                loadTimeLabel.setText(String.format("⏱️ Load time: %d ms (loaded %d rows)", loadMillis, result.size()));
                sortTimeLabel.setText(String.format("⚡ Sort time: %d ms", sortMillis));
                updateResultTable(result, column);
                progressBar.setValue(100);
            } catch (InterruptedException | ExecutionException e) {
                JOptionPane.showMessageDialog(SortingStressTest.this, "Failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SortingStressTest s = new SortingStressTest();
            s.setVisible(true);
        });
    }

    // Simple record holder
    private static class Record {
        int id;
        String firstName;
        String lastName;
        Record(int id, String fn, String ln) {
            this.id = id;
            this.firstName = fn;
            this.lastName = ln;
        }
    }
}