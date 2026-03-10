package MIDTERM;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class PathFinderUI extends JFrame {

    // ── Data ──────────────────────────────────────────────────────
    private final java.util.List<Edge> edges = new ArrayList<>();

    // ── Controls ──────────────────────────────────────────────────
    private final JComboBox<String> sourceCombo;
    private final JComboBox<String> criteriaCombo;
    private final JLabel            statusLabel;

    // ── Panels ────────────────────────────────────────────────────
    private final GraphPanel graphPanel;
    private final JTextArea  calcArea;
    private final JTextArea  summaryArea;

    // ── Graph state ───────────────────────────────────────────────
    private java.util.List<String> highlightPath = new ArrayList<>();
    private String currentSrc = "";
    private String currentDst = "";

    // ── Colour palette ────────────────────────────────────────────
    private static final Color BG        = new Color(12,  16,  28);
    private static final Color PANEL_BG  = new Color(18,  24,  38);
    private static final Color ACCENT    = new Color( 0, 200, 255);
    private static final Color ACCENT3   = new Color( 0, 230, 120);
    private static final Color NODE_DEF  = new Color(30,  50,  90);
    private static final Color NODE_SRC  = new Color( 0, 200,  80);
    private static final Color NODE_DST  = new Color(255, 100,  30);
    private static final Color NODE_PATH = new Color( 0, 180, 255);
    private static final Color EDGE_DEF  = new Color(50,  75, 110);
    private static final Color EDGE_HI   = new Color( 0, 220, 255);
    private static final Color TEXT_DIM  = new Color(100, 140, 180);
    private static final Color YELLOW_BG = new Color(255, 252, 230);
    private static final Color INK_BLUE  = new Color(15,  30, 120);

    // ================================================================
    //  CONSTRUCTOR
    // ================================================================
    public PathFinderUI() {
        setTitle("Network Path Optimizer — Dijkstra's Algorithm");
        setSize(1400, 860);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(4, 4));
        getContentPane().setBackground(BG);

        // ── TOP BAR ──────────────────────────────────────────────
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        top.setBackground(new Color(22, 30, 50));
        top.setBorder(new EmptyBorder(4, 10, 4, 10));

        top.add(styledLabel("Source Node:"));
        sourceCombo = new JComboBox<>();
        styleCombo(sourceCombo);
        top.add(sourceCombo);

        top.add(styledLabel("Criteria:"));
        criteriaCombo = new JComboBox<>(new String[]{"FUEL", "DISTANCE", "TIME", "ALL"});
        styleCombo(criteriaCombo);
        top.add(criteriaCombo);

        JButton uploadBtn  = styledBtn("Upload CSV",  new Color(45, 100, 185));
        JButton analyzeBtn = styledBtn("Analyze",     new Color(35, 145,  65));
        JButton clearBtn   = styledBtn("Clear",       new Color(130,  35,  35));
        top.add(uploadBtn); top.add(analyzeBtn); top.add(clearBtn);

        statusLabel = new JLabel("  No dataset loaded");
        statusLabel.setForeground(new Color(180, 160, 80));
        statusLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        top.add(statusLabel);

        // ── GRAPH PANEL ──────────────────────────────────────────
        graphPanel = new GraphPanel();
        graphPanel.setPreferredSize(new Dimension(420, 0));
        graphPanel.setBorder(titledBorder("DIJKSTRA GRAPH", ACCENT));

        // ── CALC AREA ────────────────────────────────────────────
        calcArea = new JTextArea();
        calcArea.setEditable(false);
        calcArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        calcArea.setBackground(YELLOW_BG);
        calcArea.setForeground(INK_BLUE);
        calcArea.setMargin(new Insets(10, 14, 10, 14));
        calcArea.setText("Upload CSV then click Analyze.");
        JScrollPane calcScroll = new JScrollPane(calcArea);
        calcScroll.setBorder(titledBorder("CALCULATION  (all paths)", new Color(80, 130, 220)));

        // ── SUMMARY AREA ─────────────────────────────────────────
        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        summaryArea.setBackground(new Color(14, 20, 32));
        summaryArea.setForeground(new Color(170, 230, 190));
        summaryArea.setMargin(new Insets(10, 14, 10, 14));
        summaryArea.setText("Summary will appear here.");
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(titledBorder("BEST PATH SUMMARY", ACCENT3));

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, calcScroll, summaryScroll);
        rightSplit.setDividerLocation(500);
        rightSplit.setBackground(BG);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphPanel, rightSplit);
        mainSplit.setDividerLocation(420);
        mainSplit.setBackground(BG);

        add(top,       BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);

        // ── LISTENERS ────────────────────────────────────────────
        uploadBtn .addActionListener(ev -> loadCSV());
        analyzeBtn.addActionListener(ev -> analyze());
        clearBtn  .addActionListener(ev -> {
            calcArea.setText(""); summaryArea.setText("");
            highlightPath.clear(); currentSrc = ""; currentDst = "";
            graphPanel.repaint();
        });
        sourceCombo.addActionListener(ev -> {
            String sel = (String) sourceCombo.getSelectedItem();
            currentSrc = (sel == null) ? "" : sel;
            highlightPath.clear(); currentDst = "";
            graphPanel.repaint();
        });
    }

    // ================================================================
    //  LOAD CSV
    // ================================================================
    private void loadCSV() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (BufferedReader br = new BufferedReader(new FileReader(fc.getSelectedFile()))) {
            edges.clear();
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",");
                edges.add(new Edge(p[0].trim(), p[1].trim(),
                    Double.parseDouble(p[2].trim()),
                    Double.parseDouble(p[3].trim()),
                    Double.parseDouble(p[4].trim())));
            }
            java.util.List<String> nodes = new ArrayList<>(getSortedNodes());
            sourceCombo.removeAllItems();
            nodes.forEach(sourceCombo::addItem);
            currentSrc = nodes.isEmpty() ? "" : nodes.get(0);
            statusLabel.setText("  Loaded " + edges.size() + " edges | Nodes: " + nodes);
            statusLabel.setForeground(ACCENT3);
            graphPanel.repaint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "CSV Error:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    //  ANALYZE
    // ================================================================
    private void analyze() {
        if (edges.isEmpty()) { calcArea.setText("Upload a dataset first."); return; }
        String src      = (String) sourceCombo.getSelectedItem();
        String criteria = (String) criteriaCombo.getSelectedItem();
        if (src == null || criteria == null) return;
        currentSrc = src;

        java.util.List<String> dests = new ArrayList<>(getSortedNodes());
        dests.remove(src);

        boolean doFuel = criteria.equals("FUEL")     || criteria.equals("ALL");
        boolean doDist = criteria.equals("DISTANCE") || criteria.equals("ALL");
        boolean doTime = criteria.equals("TIME")     || criteria.equals("ALL");

        StringBuilder calc    = new StringBuilder();
        StringBuilder summary = new StringBuilder();

        if (doFuel) appendCalcBlock(calc,    src, dests, "FUEL");
        if (doDist) appendCalcBlock(calc,    src, dests, "DISTANCE");
        if (doTime) appendCalcBlock(calc,    src, dests, "TIME");
        if (doFuel) appendSummaryBlock(summary, src, dests, "FUEL");
        if (doDist) appendSummaryBlock(summary, src, dests, "DISTANCE");
        if (doTime) appendSummaryBlock(summary, src, dests, "TIME");

        calcArea   .setText(calc.toString());
        summaryArea.setText(summary.toString());
        calcArea   .setCaretPosition(0);
        summaryArea.setCaretPosition(0);

        String mode = doFuel ? "FUEL" : doDist ? "DISTANCE" : "TIME";
        highlightBestOverall(src, dests, mode);
    }

    private void highlightBestOverall(String src, java.util.List<String> dests, String mode) {
        double best = Double.MAX_VALUE;
        java.util.List<String> bestPath = new ArrayList<>();
        String bestDst = "";
        for (String dst : dests) {
            for (PathCandidate c : enumerateAllPaths(src, dst)) {
                double v = getCrit(c, mode);
                if (v < best) { best = v; bestPath = c.path; bestDst = dst; }
            }
        }
        highlightPath = bestPath;
        currentDst    = bestDst;
        graphPanel.repaint();
    }

    // ================================================================
    //  CALCULATION BLOCK
    // ================================================================
    private void appendCalcBlock(StringBuilder sb, String src,
                                  java.util.List<String> dests, String mode) {
        String unit = unitOf(mode);
        sb.append(bar('═', 68)).append("\n");
        sb.append("  ").append(mode).append(" =\n");
        sb.append(bar('─', 68)).append("\n\n");
        double grandTotal = 0;

        for (String dst : dests) {
            java.util.List<PathCandidate> paths = enumerateAllPaths(src, dst);
            if (paths.isEmpty()) {
                sb.append("  ").append(src).append(" -> ").append(dst)
                  .append("  : (unreachable)\n\n"); continue;
            }
            paths.sort(Comparator.comparingDouble(p -> getCrit(p, mode)));
            double minVal = getCrit(paths.get(0), mode);
            grandTotal += minVal;

            sb.append("  ").append(src).append(" -> ").append(dst).append("\n");
            sb.append(bar('-', 58)).append("\n");

            for (PathCandidate c : paths) {
                double val     = getCrit(c, mode);
                boolean isBest = (Math.abs(val - minVal) < 1e-9);
                StringBuilder segStr = new StringBuilder();
                java.util.List<Double> segVals = new ArrayList<>();

                for (int k = 0; k < c.path.size() - 1; k++) {
                    String u = c.path.get(k), v = c.path.get(k + 1);
                    Edge e  = findEdge(u, v);
                    double sv = (e == null) ? 0 : getCritEdge(e, mode);
                    segVals.add(sv);
                    if (segStr.length() > 0) segStr.append(" + ");
                    segStr.append(u).append(",").append(v).append("=").append(fmt(sv));
                }
                String bestTag = isBest ? "  <- " + fmt(val) + " " + unit : "";
                if (c.path.size() == 2) {
                    sb.append("  ").append(segStr)
                      .append(" = ").append(fmt(val)).append(bestTag).append("\n");
                } else {
                    sb.append("  ").append(segStr).append("\n");
                    StringBuilder runStr = new StringBuilder();
                    for (int k = 0; k < segVals.size(); k++) {
                        if (k > 0) runStr.append(" + ");
                        runStr.append(fmt(segVals.get(k)));
                    }
                    sb.append("       = ").append(runStr)
                      .append(" = ").append(fmt(val)).append(bestTag).append("\n");
                }
            }
            sb.append("\n");
        }
        sb.append(bar('═', 68)).append("\n");
        sb.append(String.format("  TOTAL CHEAPEST %-10s = %.2f %s%n", mode, grandTotal, unit));
        sb.append(bar('═', 68)).append("\n\n");
    }

    // ================================================================
    //  SUMMARY BLOCK
    // ================================================================
    private void appendSummaryBlock(StringBuilder sb, String src,
                                     java.util.List<String> dests, String mode) {
        String unit = unitOf(mode);
        sb.append(bar('═', 54)).append("\n");
        sb.append("  ").append(mode).append(" - BEST PATHS FROM NODE ").append(src).append("\n");
        sb.append(bar('═', 54)).append("\n");
        sb.append(String.format("  %-8s  %-26s  %s%n", "DEST", "BEST PATH", "VALUE"));
        sb.append(bar('-', 54)).append("\n");
        double total = 0;
        for (String dst : dests) {
            java.util.List<PathCandidate> paths = enumerateAllPaths(src, dst);
            if (paths.isEmpty()) {
                sb.append(String.format("  %-8s  %-26s  N/A%n",
                    src + "->" + dst, "(unreachable)")); continue;
            }
            paths.sort(Comparator.comparingDouble(p -> getCrit(p, mode)));
            PathCandidate best = paths.get(0);
            double val = getCrit(best, mode);
            total += val;
            sb.append(String.format("  %-8s  %-26s  %.2f %s%n",
                src + "->" + dst, pathStr(best.path), val, unit));
        }
        sb.append(bar('-', 54)).append("\n");
        sb.append(String.format("  %-8s  %-26s  %.2f %s%n", "TOTAL", "", total, unit));
        sb.append(bar('═', 54)).append("\n\n");
    }

    // ================================================================
    //  GRAPH PANEL
    // ================================================================
    class GraphPanel extends JPanel {

        private final Map<String, double[]> layout = new LinkedHashMap<>();

        GraphPanel() {
            setBackground(PANEL_BG);
            layout.put("1", new double[]{0.25, 0.22});
            layout.put("2", new double[]{0.72, 0.12});
            layout.put("3", new double[]{0.88, 0.45});
            layout.put("4", new double[]{0.75, 0.80});
            layout.put("5", new double[]{0.45, 0.78});
            layout.put("6", new double[]{0.18, 0.62});

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    String clicked = nodeAt(e.getX(), e.getY());
                    if (clicked == null || clicked.equals(currentSrc)) return;

                    // FIX 1: capture criteria into a final local variable
                    //        so it can be used safely inside the lambda below
                    String sel = (String) criteriaCombo.getSelectedItem();
                    final String resolvedMode = (sel == null || sel.equals("ALL")) ? "FUEL" : sel;

                    java.util.List<PathCandidate> paths = enumerateAllPaths(currentSrc, clicked);
                    if (paths.isEmpty()) return;

                    // FIX 2: use the final resolvedMode in the lambda — no more
                    //        "local variable referenced from lambda must be final" error
                    paths.sort(Comparator.comparingDouble(p -> getCrit(p, resolvedMode)));

                    highlightPath = paths.get(0).path;
                    currentDst    = clicked;
                    repaint();
                    scrollCalcTo(currentSrc, clicked);
                }
            });
        }

        private void ensureLayout() {
            Set<String> nodes = getSortedNodes();
            java.util.List<String> missing = new ArrayList<>();
            for (String n : nodes) if (!layout.containsKey(n)) missing.add(n);
            if (!missing.isEmpty()) {
                int total = layout.size() + missing.size();
                int idx   = layout.size();
                for (String n : missing) {
                    double angle = 2 * Math.PI * idx / total;
                    layout.put(n, new double[]{
                        0.5 + 0.38 * Math.cos(angle - Math.PI / 2),
                        0.5 + 0.38 * Math.sin(angle - Math.PI / 2)
                    });
                    idx++;
                }
            }
        }

        private Point2D.Double nodeCenter(String id) {
            double[] pos = layout.getOrDefault(id, new double[]{0.5, 0.5});
            int pad = 40;
            double w = getWidth()  - 2.0 * pad;
            double h = getHeight() - 2.0 * pad;
            return new Point2D.Double(pad + pos[0] * w, pad + pos[1] * h);
        }

        private String nodeAt(int mx, int my) {
            ensureLayout();
            int R = 22;
            for (String id : layout.keySet()) {
                Point2D.Double c = nodeCenter(id);
                double dx = mx - c.x, dy = my - c.y;
                if (dx * dx + dy * dy <= (double)(R * R)) return id;
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (edges.isEmpty()) {
                g.setColor(TEXT_DIM);
                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                g.drawString("Load a CSV to visualize the graph", 30, getHeight() / 2);
                return;
            }
            ensureLayout();
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int R = 22;

            // Grid
            g2.setColor(new Color(255, 255, 255, 8));
            g2.setStroke(new BasicStroke(1));
            for (int x = 0; x < getWidth();  x += 35) g2.drawLine(x, 0, x, getHeight());
            for (int y = 0; y < getHeight(); y += 35) g2.drawLine(0, y, getWidth(), y);

            // Highlighted edge set
            Set<String> hiEdges = new HashSet<>();
            for (int k = 0; k < highlightPath.size() - 1; k++)
                hiEdges.add(highlightPath.get(k) + "->" + highlightPath.get(k + 1));

            // Get the current display mode for edge labels
            String dispSel = (String) criteriaCombo.getSelectedItem();
            final String dispMode = (dispSel == null) ? "FUEL" : dispSel;

            // Draw edges
            for (Edge e : edges) {
                Point2D.Double fromPt = nodeCenter(e.from);
                Point2D.Double toPt   = nodeCenter(e.to);
                boolean hi = hiEdges.contains(e.from + "->" + e.to);

                double dx = toPt.x - fromPt.x, dy = toPt.y - fromPt.y;
                double cx = (fromPt.x + toPt.x) / 2.0 - dy * 0.12;
                double cy = (fromPt.y + toPt.y) / 2.0 + dx * 0.12;

                if (hi) {
                    g2.setColor(new Color(0, 220, 255, 40));
                    g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    drawCurvedEdge(g2, fromPt, toPt, cx, cy);
                    g2.setColor(EDGE_HI);
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                } else {
                    g2.setColor(EDGE_DEF);
                    g2.setStroke(new BasicStroke(1.4f));
                }
                drawCurvedEdge(g2, fromPt, toPt, cx, cy);
                drawArrow(g2, toPt, cx, cy, R, hi ? EDGE_HI : EDGE_DEF);

                // Edge weight label — FIX 3: use final dispMode (not a re-assigned variable)
                String wLabel = switch (dispMode) {
                    case "DISTANCE" -> fmt(e.dist) + "km";
                    case "TIME"     -> fmt(e.time) + "m";
                    default         -> fmt(e.fuel) + "L";
                };

                double lx = cx - dy * 0.08;
                double ly = cy + dx * 0.08;
                g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
                g2.setColor(hi ? new Color(0, 230, 255, 220) : new Color(140, 170, 200, 160));
                g2.drawString(wLabel, (float)(lx - 10), (float)(ly));
            }

            // Draw nodes
            for (String id : layout.keySet()) {
                if (!getSortedNodes().contains(id)) continue;
                Point2D.Double c = nodeCenter(id);

                boolean isSrc  = id.equals(currentSrc);
                boolean isDst  = id.equals(currentDst);
                boolean inPath = highlightPath.contains(id);

                Color fill   = isSrc ? NODE_SRC : isDst ? NODE_DST
                             : inPath ? NODE_PATH : NODE_DEF;
                Color border = isSrc ? new Color(0, 255, 120)
                             : isDst ? new Color(255, 140, 30)
                             : inPath ? ACCENT : new Color(70, 110, 160);

                if (isSrc || isDst || inPath) {
                    g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 45));
                    g2.fillOval((int)(c.x - R - 8), (int)(c.y - R - 8), (R + 8) * 2, (R + 8) * 2);
                }

                g2.setColor(fill);
                g2.fillOval((int)(c.x - R), (int)(c.y - R), R * 2, R * 2);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(isSrc || isDst ? 2.5f : 1.8f));
                g2.drawOval((int)(c.x - R), (int)(c.y - R), R * 2, R * 2);

                g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(isSrc || isDst || inPath ? Color.BLACK : Color.WHITE);
                g2.drawString(id,
                    (int)(c.x - fm.stringWidth(id) / 2.0),
                    (int)(c.y + fm.getAscent()    / 2.0 - 1));
            }

            drawLegend(g2);

            // Path label at bottom
            if (!highlightPath.isEmpty()) {
                // FIX 4: capture into final variable before use in comparingDouble lambda
                final String labelMode;
                String lSel = (String) criteriaCombo.getSelectedItem();
                if (lSel == null || lSel.equals("ALL")) labelMode = "FUEL";
                else labelMode = lSel;

                java.util.List<PathCandidate> ps = enumerateAllPaths(currentSrc, currentDst);
                if (!ps.isEmpty()) {
                    ps.sort(Comparator.comparingDouble(p -> getCrit(p, labelMode)));
                    double val = getCrit(ps.get(0), labelMode);
                    String label = pathStr(highlightPath) + "  =  "
                        + fmt(val) + " " + unitOf(labelMode);
                    g2.setColor(new Color(0, 220, 255, 220));
                    g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
                    g2.drawString(label, 12, getHeight() - 12);
                }
            }
        }

        private void drawCurvedEdge(Graphics2D g2,
                Point2D.Double from, Point2D.Double to, double cx, double cy) {
            g2.draw(new QuadCurve2D.Double(from.x, from.y, cx, cy, to.x, to.y));
        }

        // FIX 5: removed unused 'from' parameter — renamed signature to only what is needed
        private void drawArrow(Graphics2D g2,
                Point2D.Double to, double cx, double cy, int nodeR, Color col) {
            double dx  = to.x - cx, dy = to.y - cy;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 1) return;
            double ax    = to.x - dx / len * nodeR;
            double ay    = to.y - dy / len * nodeR;
            double angle = Math.atan2(dy, dx);
            int    aLen  = 9;
            int[]  xs    = { (int)ax,
                (int)(ax - aLen * Math.cos(angle - 0.42)),
                (int)(ax - aLen * Math.cos(angle + 0.42)) };
            int[]  ys    = { (int)ay,
                (int)(ay - aLen * Math.sin(angle - 0.42)),
                (int)(ay - aLen * Math.sin(angle + 0.42)) };
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1));
            g2.fillPolygon(xs, ys, 3);
        }

        private void drawLegend(Graphics2D g2) {
            int x = 8, y = getHeight() - 76;
            int sw = 12;
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            Object[][] items = {
                { NODE_SRC,  "Source"         },
                { NODE_DST,  "Destination"    },
                { NODE_PATH, "On Path"        },
                { EDGE_HI,   "Best Path Edge" },
            };
            for (Object[] item : items) {
                g2.setColor((Color) item[0]);
                g2.fillOval(x, y - sw + 2, sw, sw);
                g2.setColor(new Color(180, 200, 230));
                g2.drawString((String) item[1], x + sw + 4, y);
                y += 16;
            }
        }
    }

    // ================================================================
    //  SCROLL CALC AREA TO SECTION
    // ================================================================
    private void scrollCalcTo(String src, String dst) {
        String text   = calcArea.getText();
        String marker = "  " + src + " -> " + dst;
        int    idx    = text.indexOf(marker);
        if (idx >= 0) {
            calcArea.setCaretPosition(idx);
            try {
                calcArea.scrollRectToVisible(
                    calcArea.modelToView2D(idx).getBounds());
            } catch (Exception ignored) {}
        }
    }

    // ================================================================
    //  ENUMERATE ALL PATHS
    // ================================================================
    private java.util.List<PathCandidate> enumerateAllPaths(String src, String dst) {
        java.util.List<PathCandidate> results = new ArrayList<>();
        if (src == null || dst == null || src.equals(dst)) return results;
        Deque<java.util.List<String>> stack = new ArrayDeque<>();
        stack.push(new ArrayList<>(Collections.singletonList(src)));
        while (!stack.isEmpty()) {
            java.util.List<String> path = stack.pop();
            String last = path.get(path.size() - 1);
            if (last.equals(dst)) {
                double d = 0, t = 0, f = 0; boolean ok = true;
                for (int i = 0; i < path.size() - 1; i++) {
                    Edge e = findEdge(path.get(i), path.get(i + 1));
                    if (e == null) { ok = false; break; }
                    d += e.dist; t += e.time; f += e.fuel;
                }
                if (ok) results.add(new PathCandidate(new ArrayList<>(path), d, t, f));
                continue;
            }
            for (Edge e : edges) {
                if (!e.from.equals(last) || path.contains(e.to)) continue;
                java.util.List<String> next = new ArrayList<>(path);
                next.add(e.to);
                stack.push(next);
            }
        }
        return results;
    }

    // ================================================================
    //  HELPERS
    // ================================================================
    private double getCrit(PathCandidate c, String mode) {
        return switch (mode) {
            case "DISTANCE" -> c.dist;
            case "TIME"     -> c.time;
            default         -> c.fuel;
        };
    }

    private double getCritEdge(Edge e, String mode) {
        return switch (mode) {
            case "DISTANCE" -> e.dist;
            case "TIME"     -> e.time;
            default         -> e.fuel;
        };
    }

    private Edge findEdge(String u, String v) {
        return edges.stream()
            .filter(e -> e.from.equals(u) && e.to.equals(v))
            .findFirst().orElse(null);
    }

    private Set<String> getSortedNodes() {
        Set<String> s = new TreeSet<>(Comparator.comparingInt(x -> {
            try { return Integer.parseInt(x); }
            catch (NumberFormatException ex) { return x.hashCode(); }
        }));
        for (Edge e : edges) { s.add(e.from); s.add(e.to); }
        return s;
    }

    private String pathStr(java.util.List<String> p) {
        return p.isEmpty() ? "(none)" : String.join(" -> ", p);
    }

    private String fmt(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((int) v);
        return String.format("%.1f", v);
    }

    private String unitOf(String mode) {
        return switch (mode) {
            case "DISTANCE" -> "km";
            case "TIME"     -> "min";
            default         -> "L";
        };
    }

    private String bar(char ch, int n) {
        return "  " + String.valueOf(ch).repeat(n);
    }

    // ── UI helpers ───────────────────────────────────────────────────
    private void styleCombo(JComboBox<String> c) {
        c.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        c.setBackground(new Color(38, 50, 72));
        c.setForeground(new Color(200, 230, 255));
        c.setPreferredSize(new Dimension(110, 30));
    }

    private JButton styledBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.brighter()),
            new EmptyBorder(5, 14, 5, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(170, 205, 255));
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        return l;
    }

    private Border titledBorder(String title, Color col) {
        TitledBorder tb = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(col), "  " + title + "  ");
        tb.setTitleColor(col);
        tb.setTitleFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        return tb;
    }

    // ================================================================
    //  INNER CLASSES
    // ================================================================
    static class Edge {
        final String from, to;
        final double dist, time, fuel;
        Edge(String f, String t, double d, double tm, double fl) {
            from = f; to = t; dist = d; time = tm; fuel = fl;
        }
    }

    static class PathCandidate {
        final java.util.List<String> path;
        final double dist, time, fuel;
        PathCandidate(java.util.List<String> p, double d, double t, double f) {
            path = p; dist = d; time = t; fuel = f;
        }
    }

    // ================================================================
    //  MAIN
    // ================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PathFinderUI().setVisible(true));
    }
}