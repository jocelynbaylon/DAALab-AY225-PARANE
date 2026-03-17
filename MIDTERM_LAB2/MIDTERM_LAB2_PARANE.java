package MIDTERM_LAB2;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Travelling Salesman Problem — Road Map Edition
 * Theme  : Vintage Road-Trip / Highway Map
 * Algo   : Dijkstra's Shortest Path
 * Feature: Animated car driving along the shortest path
 * Layout : Force-Directed Graph — connected nodes cluster naturally
 */
public class MIDTERM_LAB2_PARANE extends JFrame {

    // ── Road-Trip Color Palette ───────────────────────────────────────────────
    private static final Color BG_ASPHALT    = new Color(22, 26, 30);
    private static final Color BG_PANEL      = new Color(18, 22, 26);
    private static final Color BG_CARD       = new Color(26, 31, 38);
    private static final Color ROAD_GRAY     = new Color(55, 62, 70);
    private static final Color ACCENT_AMBER  = new Color(255, 185, 30);
    private static final Color ACCENT_GREEN  = new Color(60, 210, 100);
    private static final Color ACCENT_RED    = new Color(255, 75, 75);
    private static final Color ACCENT_TEAL   = new Color(40, 200, 180);
    private static final Color ACCENT_ORANGE = new Color(255, 130, 40);
    private static final Color TEXT_BRIGHT   = new Color(235, 240, 250);
    private static final Color TEXT_DIM      = new Color(120, 135, 155);
    private static final Color NODE_STROKE   = new Color(60, 210, 100);

    // ── Graph Data ────────────────────────────────────────────────────────────
    private Map<String, List<Edge>> graph    = new LinkedHashMap<>();
    private List<Edge> allEdges              = new ArrayList<>();
    private List<String> nodes               = new ArrayList<>();
    private Map<String, Point> nodePositions = new LinkedHashMap<>();

    // ── UI ────────────────────────────────────────────────────────────────────
    private NodeMapPanel mapPanel;
    private JComboBox<String> fromCombo, toCombo, criteriaCombo;
    private JTextArea resultArea;
    private JLabel statusLabel, csvBadge, mapTitleLabel;
    private List<String> currentPath = new ArrayList<>();
    private String loadedFileName    = "none";

    // ── Car Animation ─────────────────────────────────────────────────────────
    private Timer carTimer;
    private float carT       = 0f;
    private int   carSegment = 0;
    private boolean animating = false;
    private static final float CAR_SPEED = 0.016f;

    // ── Edge ──────────────────────────────────────────────────────────────────
    static class Edge {
        String from, to;
        double distance, time, fuel;
        Edge(String f,String t,double d,double ti,double fu){
            from=f; to=t; distance=d; time=ti; fuel=fu;
        }
    }

    public MIDTERM_LAB2_PARANE(){ buildUI(); }

    // ── CSV Loader ────────────────────────────────────────────────────────────
    private boolean loadCSV(String path) {
        graph.clear(); allEdges.clear(); nodes.clear(); nodePositions.clear(); currentPath.clear();
        stopCarAnimation();
        try (BufferedReader br=new BufferedReader(new FileReader(path))) {
            String line; boolean header=true; int rows=0;
            while ((line=br.readLine())!=null){
                line=line.trim(); if(line.isEmpty()) continue;
                if(header){header=false;continue;}
                String[] p=line.split(","); if(p.length<5) continue;
                String fr=p[0].trim(), to=p[1].trim();
                double d=Double.parseDouble(p[2].trim()),
                       t=Double.parseDouble(p[3].trim()),
                       f=Double.parseDouble(p[4].trim());
                Edge e=new Edge(fr,to,d,t,f); allEdges.add(e);
                graph.computeIfAbsent(fr,k->new ArrayList<>()).add(e);
                Edge rev=new Edge(to,fr,d,t,f); allEdges.add(rev);
                graph.computeIfAbsent(to,k->new ArrayList<>()).add(rev);
                if(!nodes.contains(fr)) nodes.add(fr);
                if(!nodes.contains(to)) nodes.add(to);
                rows++;
            }
            if(rows==0){showErr("CSV has no valid data rows.");return false;}
        } catch(FileNotFoundException e){showErr("File not found:\n"+path);return false;
        } catch(NumberFormatException e){showErr("Number format error in CSV.");return false;
        } catch(Exception e){showErr("Error: "+e.getMessage());return false;}
        assignNodePositions(); return true;
    }
    private void showErr(String m){JOptionPane.showMessageDialog(this,m,"Error",JOptionPane.ERROR_MESSAGE);}

    // ── Force-Directed Layout ─────────────────────────────────────────────────
    // Connected nodes attract each other; all nodes repel.
    // Result: the graph draws itself as a natural road network
    // where every edge visibly connects its two endpoints.
    private void assignNodePositions() {
        int W = 760, H = 600, PAD = 75;
        int n = nodes.size();
        if (n == 0) return;

        // --- Seed: evenly spaced circle ---
        Map<String, double[]> pos = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            double r = Math.min(W, H) * 0.32;
            pos.put(nodes.get(i), new double[]{
                W / 2.0 + r * Math.cos(angle),
                H / 2.0 + r * Math.sin(angle)
            });
        }

        // Ideal spring length — scales with canvas area and node count
        double k      = Math.sqrt((double)(W * H) / Math.max(n, 1));
        double idealL = k * 1.05;
        double repK   = k * k * 2.0;

        // --- Iterative force simulation ---
        for (int iter = 0; iter < 400; iter++) {
            Map<String, double[]> disp = new HashMap<>();
            for (String nd : nodes) disp.put(nd, new double[]{0, 0});

            // Repulsion: every pair of nodes pushes apart
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    String ni = nodes.get(i), nj = nodes.get(j);
                    double[] pi = pos.get(ni), pj = pos.get(nj);
                    double dx = pi[0] - pj[0], dy = pi[1] - pj[1];
                    double dist = Math.max(Math.sqrt(dx * dx + dy * dy), 1.0);
                    double force = repK / (dist * dist);
                    double fx = (dx / dist) * force, fy = (dy / dist) * force;
                    disp.get(ni)[0] += fx;  disp.get(ni)[1] += fy;
                    disp.get(nj)[0] -= fx;  disp.get(nj)[1] -= fy;
                }
            }

            // Attraction: edges pull connected pairs together
            Set<String> visited = new HashSet<>();
            for (Edge e : allEdges) {
                String key = e.from.compareTo(e.to) < 0
                    ? e.from + "|" + e.to : e.to + "|" + e.from;
                if (!visited.add(key)) continue;
                double[] pi = pos.get(e.from), pj = pos.get(e.to);
                if (pi == null || pj == null) continue;
                double dx = pj[0] - pi[0], dy = pj[1] - pi[1];
                double dist = Math.max(Math.sqrt(dx * dx + dy * dy), 1.0);
                double stretch = (dist - idealL) / dist * 0.30;
                double fx = dx * stretch, fy = dy * stretch;
                disp.get(e.from)[0] += fx;  disp.get(e.from)[1] += fy;
                disp.get(e.to)[0]   -= fx;  disp.get(e.to)[1]   -= fy;
            }

            // Gravity: gentle pull toward canvas center (prevents drift)
            double cx = 0, cy = 0;
            for (double[] p : pos.values()) { cx += p[0]; cy += p[1]; }
            cx /= n; cy /= n;
            for (String nd : nodes) {
                disp.get(nd)[0] += (W / 2.0 - cx) * 0.05;
                disp.get(nd)[1] += (H / 2.0 - cy) * 0.05;
            }

            // Apply displacement with linear cooling
            double temp = Math.max(18.0 - iter * 0.045, 1.5);
            for (String nd : nodes) {
                double[] p = pos.get(nd), d = disp.get(nd);
                double dLen = Math.max(Math.sqrt(d[0] * d[0] + d[1] * d[1]), 0.001);
                double move = Math.min(dLen, temp);
                p[0] = Math.max(PAD, Math.min(W - PAD, p[0] + (d[0] / dLen) * move));
                p[1] = Math.max(PAD, Math.min(H - PAD, p[1] + (d[1] / dLen) * move));
            }
        }

        // Finalise: convert to integer Points
        nodePositions.clear();
        for (String nd : nodes) {
            double[] p = pos.get(nd);
            nodePositions.put(nd, new Point((int) p[0], (int) p[1]));
        }
    }

    // ── Dijkstra ──────────────────────────────────────────────────────────────
    private Object[] dijkstra(String start,String end,String crit){
        Map<String,Double> dist=new HashMap<>();
        Map<String,String> prev=new HashMap<>();
        PriorityQueue<String> pq=new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        for(String n:nodes) dist.put(n,Double.MAX_VALUE);
        dist.put(start,0.0); pq.add(start);
        while(!pq.isEmpty()){
            String c=pq.poll(); if(c.equals(end)) break;
            for(Edge e:graph.getOrDefault(c,Collections.emptyList())){
                double w=crit.equals("Distance")?e.distance:crit.equals("Time")?e.time:e.fuel;
                double nd=dist.get(c)+w;
                if(nd<dist.getOrDefault(e.to,Double.MAX_VALUE)){
                    dist.put(e.to,nd); prev.put(e.to,c); pq.remove(e.to); pq.add(e.to);
                }
            }
        }
        List<String> path=new ArrayList<>();
        for(String n=end;n!=null;n=prev.get(n)) path.add(0,n);
        if(path.isEmpty()||!path.get(0).equals(start)) return null;
        double td=0,tt=0,tf=0;
        for(int i=0;i<path.size()-1;i++){
            String a=path.get(i),b=path.get(i+1);
            for(Edge e:graph.getOrDefault(a,Collections.emptyList()))
                if(e.to.equals(b)){td+=e.distance;tt+=e.time;tf+=e.fuel;break;}
        }
        return new Object[]{path,td,tt,tf};
    }

    // ── Car Animation ─────────────────────────────────────────────────────────
    private void startCarAnimation(){
        stopCarAnimation();
        if(currentPath.size()<2) return;
        carT=0f; carSegment=0; animating=true;
        carTimer=new Timer(28, e->{
            carT+=CAR_SPEED;
            if(carT>=1f){
                carT=0f; carSegment++;
                if(carSegment>=currentPath.size()-1){
                    carSegment=currentPath.size()-2; carT=1f;
                    animating=false; ((Timer)e.getSource()).stop();
                    statusLabel.setText("🏁  Arrived at destination: "+currentPath.get(currentPath.size()-1));
                }
            }
            mapPanel.repaint();
        });
        carTimer.start();
    }
    private void stopCarAnimation(){
        if(carTimer!=null){carTimer.stop();carTimer=null;}
        animating=false; carT=0f; carSegment=0;
    }

    private Point2D.Float getCarPos(){
        if(currentPath.size()<2) return null;
        int seg=Math.min(carSegment,currentPath.size()-2);
        Point p1=nodePositions.get(currentPath.get(seg));
        Point p2=nodePositions.get(currentPath.get(seg+1));
        if(p1==null||p2==null) return null;
        return new Point2D.Float(p1.x+(p2.x-p1.x)*carT, p1.y+(p2.y-p1.y)*carT);
    }
    private double getCarAngle(){
        if(currentPath.size()<2) return 0;
        int seg=Math.min(carSegment,currentPath.size()-2);
        Point p1=nodePositions.get(currentPath.get(seg));
        Point p2=nodePositions.get(currentPath.get(seg+1));
        if(p1==null||p2==null) return 0;
        return Math.atan2(p2.y-p1.y, p2.x-p1.x);
    }

    // ── Build UI ──────────────────────────────────────────────────────────────
    private void buildUI(){
        setTitle("Travelling Salesman  — Road Map Edition");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300,880);
        setLocationRelativeTo(null);
        setBackground(BG_ASPHALT);
        JPanel root=new JPanel(new BorderLayout(0,0));
        root.setBackground(BG_ASPHALT);
        root.add(buildHeader(),    BorderLayout.NORTH);
        root.add(buildCenter(),    BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);
        setVisible(true);
    }

    private JPanel buildHeader(){
        JPanel hdr=new JPanel(new BorderLayout());
        hdr.setBackground(new Color(14,17,22));
        hdr.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,3,0,ACCENT_AMBER),
            new EmptyBorder(14,24,14,24)));

        JPanel left=new JPanel(); left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JPanel row1=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0)); row1.setOpaque(false);
        JLabel ico=new JLabel("🚗"); ico.setFont(new Font("Segoe UI Emoji",Font.PLAIN,28));
        JLabel tA=new JLabel("TRAVELLING"); tA.setFont(new Font("Monospaced",Font.BOLD,26)); tA.setForeground(ACCENT_AMBER);
        JLabel tB=new JLabel(" SALESMAN");  tB.setFont(new Font("Monospaced",Font.BOLD,26)); tB.setForeground(TEXT_BRIGHT);
        JLabel tC=new JLabel(" PROBLEM");   tC.setFont(new Font("Monospaced",Font.BOLD,26)); tC.setForeground(ACCENT_GREEN);
        row1.add(ico); row1.add(tA); row1.add(tB); row1.add(tC);

        JLabel sub=new JLabel("   Road Map Navigation  ·  Dijkstra's Algorithm  ·  Shortest Route Finder");
        sub.setFont(new Font("Monospaced",Font.PLAIN,11)); sub.setForeground(TEXT_DIM);
        left.add(row1); left.add(Box.createVerticalStrut(3)); left.add(sub);

        JButton uploadBtn=makeBtn("⬆  UPLOAD CSV",ACCENT_AMBER,new Color(14,17,22));
        uploadBtn.addActionListener(e->openCSVChooser());

        csvBadge=new JLabel("  NO FILE LOADED  ");
        csvBadge.setFont(new Font("Monospaced",Font.BOLD,10));
        csvBadge.setForeground(ACCENT_RED);
        csvBadge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_RED,1),new EmptyBorder(5,10,5,10)));
        csvBadge.setOpaque(true); csvBadge.setBackground(new Color(35,14,14));

        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,12,0)); right.setOpaque(false);
        right.add(uploadBtn); right.add(csvBadge);
        hdr.add(left,BorderLayout.WEST); hdr.add(right,BorderLayout.EAST);
        return hdr;
    }

    private JSplitPane buildCenter(){
        JSplitPane sp=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,buildMapSection(),buildSidebar());
        sp.setDividerLocation(780); sp.setDividerSize(4);
        sp.setBorder(null); sp.setBackground(BG_ASPHALT);
        return sp;
    }

    private void openCSVChooser(){
        JFileChooser fc=new JFileChooser();
        fc.setDialogTitle("Select CSV File");
        fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)","csv"));
        fc.setAcceptAllFileFilterUsed(false);
        if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File file=fc.getSelectedFile();
        statusLabel.setText("Loading "+file.getName()+"…");
        if(loadCSV(file.getAbsolutePath())){
            loadedFileName=file.getName();
            csvBadge.setText("  "+loadedFileName+"  |  "+nodes.size()+" NODES  ");
            csvBadge.setForeground(ACCENT_GREEN);
            csvBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_GREEN,1),new EmptyBorder(5,10,5,10)));
            csvBadge.setBackground(new Color(10,30,18));
            mapTitleLabel.setText("  ROAD MAP  ──  "+loadedFileName.toUpperCase());
            refreshCombos();
            resultArea.setForeground(ACCENT_GREEN);
            resultArea.setText(
                "  ✔  CSV LOADED\n"+
                "  ─────────────────────────\n"+
                "  File  : "+loadedFileName+"\n"+
                "  Nodes : "+nodes.size()+"\n"+
                "  Roads : "+(allEdges.size()/2)+"\n\n"+
                "  Select DEPARTURE / DESTINATION\n"+
                "  nodes and click\n"+
                "  [ FIND SHORTEST ROUTE ]\n");
            statusLabel.setText("✔  "+loadedFileName+"  ─  "+nodes.size()+" nodes, "+(allEdges.size()/2)+" roads");
            mapPanel.repaint();
        } else {
            statusLabel.setText("✗  Failed: "+file.getName());
        }
    }

    private void refreshCombos(){
        String[] arr=nodes.toArray(new String[0]);
        fromCombo.setModel(new DefaultComboBoxModel<>(arr));
        toCombo.setModel(new DefaultComboBoxModel<>(arr));
        if(arr.length>1) toCombo.setSelectedIndex(1);
    }

    private JPanel buildMapSection(){
        JPanel wrap=new JPanel(new BorderLayout());
        wrap.setBackground(BG_PANEL);
        wrap.setBorder(new EmptyBorder(12,12,12,6));
        mapTitleLabel=new JLabel("  ROAD MAP  ──  NO FILE LOADED");
        mapTitleLabel.setFont(new Font("Monospaced",Font.BOLD,12));
        mapTitleLabel.setForeground(ACCENT_AMBER);
        mapTitleLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,2,0,new Color(255,185,30,50)),
            new EmptyBorder(0,0,10,0)));
        mapPanel=new NodeMapPanel();
        mapPanel.setPreferredSize(new Dimension(750,660));
        wrap.add(mapTitleLabel,BorderLayout.NORTH);
        wrap.add(mapPanel,BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildSidebar(){
        JPanel sb=new JPanel(); sb.setLayout(new BoxLayout(sb,BoxLayout.Y_AXIS));
        sb.setBackground(BG_PANEL); sb.setBorder(new EmptyBorder(12,6,12,12));
        sb.add(buildFinderCard());
        sb.add(Box.createVerticalStrut(10));
        sb.add(buildResultCard());
        sb.add(Box.createVerticalStrut(10));
        sb.add(buildLegendCard());
        return sb;
    }

    private JPanel buildFinderCard(){
        JPanel card=roadCard("ROUTE FINDER",ACCENT_AMBER);
        fromCombo    =roadCombo(new String[]{"── upload CSV first ──"});
        toCombo      =roadCombo(new String[]{"── upload CSV first ──"});
        criteriaCombo=roadCombo(new String[]{"Distance","Time","Fuel"});
        card.add(dimLbl("DEPARTURE"));  card.add(Box.createVerticalStrut(4)); card.add(fromCombo);
        card.add(Box.createVerticalStrut(8));
        card.add(dimLbl("DESTINATION")); card.add(Box.createVerticalStrut(4)); card.add(toCombo);
        card.add(Box.createVerticalStrut(8));
        card.add(dimLbl("OPTIMIZE BY")); card.add(Box.createVerticalStrut(4)); card.add(criteriaCombo);
        card.add(Box.createVerticalStrut(14));
        JButton findBtn=makeBtn("FIND SHORTEST ROUTE",ACCENT_GREEN,new Color(8,20,12));
        findBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));
        findBtn.addActionListener(e->runDijkstra());
        card.add(findBtn);
        card.add(Box.createVerticalStrut(8));
        JButton replayBtn=makeBtn("REPLAY ANIMATION",ACCENT_AMBER,new Color(22,16,6));
        replayBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
        replayBtn.addActionListener(e->startCarAnimation());
        card.add(replayBtn);
        return card;
    }

    private JPanel buildResultCard(){
        JPanel glow=new JPanel(new BorderLayout());
        glow.setBackground(BG_ASPHALT);
        glow.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        glow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60,210,100,45),3),
            BorderFactory.createLineBorder(new Color(60,210,100,100),1)));
        JPanel inner=new JPanel(new BorderLayout());
        inner.setBackground(new Color(10,18,12));
        JPanel tbar=new JPanel(new BorderLayout());
        tbar.setBackground(new Color(12,28,16));
        tbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,2,0,new Color(60,210,100,100)),
            new EmptyBorder(8,14,8,14)));
        JLabel tlbl=new JLabel("ROUTE ANALYSIS  /  RESULT");
        tlbl.setFont(new Font("Monospaced",Font.BOLD,12)); tlbl.setForeground(ACCENT_GREEN);
        JLabel tsub=new JLabel("Dijkstra Output");
        tsub.setFont(new Font("Monospaced",Font.PLAIN,10)); tsub.setForeground(new Color(50,140,70));
        tbar.add(tlbl,BorderLayout.WEST); tbar.add(tsub,BorderLayout.EAST);
        resultArea=new JTextArea();
        resultArea.setFont(new Font("Monospaced",Font.PLAIN,12));
        resultArea.setForeground(new Color(170,255,200));
        resultArea.setBackground(new Color(10,18,12));
        resultArea.setCaretColor(ACCENT_GREEN);
        resultArea.setEditable(false);
        resultArea.setBorder(new EmptyBorder(12,14,12,14));
        resultArea.setText(
            "  Upload a CSV file,\n"+
            "  select nodes, then\n"+
            "  click FIND SHORTEST ROUTE");
        JScrollPane scroll=new JScrollPane(resultArea);
        scroll.setBorder(null); scroll.setBackground(new Color(10,18,12));
        scroll.getViewport().setBackground(new Color(10,18,12));
        scroll.setPreferredSize(new Dimension(0,360));
        inner.add(tbar,BorderLayout.NORTH); inner.add(scroll,BorderLayout.CENTER);
        glow.add(inner,BorderLayout.CENTER);
        return glow;
    }

    private JPanel buildLegendCard(){
        JPanel card=roadCard("LEGEND",TEXT_DIM);
        JPanel g=new JPanel(new GridLayout(2,2,8,6)); g.setOpaque(false);
        g.add(legendItem(NODE_STROKE,  "● City Node"));
        g.add(legendItem(ROAD_GRAY,    "─ Road"));
        g.add(legendItem(ACCENT_AMBER, "─ Shortest Route"));
        g.add(legendItem(ACCENT_GREEN, "● Route Node"));
        card.add(g);
        return card;
    }

    private JPanel buildStatusBar(){
        JPanel bar=new JPanel(new BorderLayout());
        bar.setBackground(new Color(12,14,18));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2,0,0,0,new Color(255,185,30,60)),
            new EmptyBorder(7,18,7,18)));
        statusLabel=new JLabel("Ready — upload a CSV to begin your journey");
        statusLabel.setFont(new Font("Monospaced",Font.PLAIN,11));
        statusLabel.setForeground(TEXT_DIM);
        JLabel credit=new JLabel("Dijkstra's Algorithm  ·  TSP Road Map  ·  Java Swing");
        credit.setFont(new Font("Monospaced",Font.PLAIN,10));
        credit.setForeground(new Color(50,60,70));
        bar.add(statusLabel,BorderLayout.WEST); bar.add(credit,BorderLayout.EAST);
        return bar;
    }

    @SuppressWarnings("unchecked")
    private void runDijkstra(){
        stopCarAnimation();
        if(nodes.isEmpty()){
            resultArea.setForeground(ACCENT_RED);
            resultArea.setText("  No CSV loaded.\n  Upload a CSV file first.");
            return;
        }
        String from=(String)fromCombo.getSelectedItem();
        String to=(String)toCombo.getSelectedItem();
        String crit=(String)criteriaCombo.getSelectedItem();
        if(from==null||to==null||from.equals(to)){
            resultArea.setForeground(ACCENT_RED);
            resultArea.setText("  Departure and destination\n  must be different nodes.");
            return;
        }
        Object[] res=dijkstra(from,to,crit);
        if(res==null){
            resultArea.setForeground(ACCENT_RED);
            resultArea.setText("  No route found\n  "+from+" to "+to);
            currentPath.clear(); mapPanel.repaint(); return;
        }
        currentPath=(List<String>)res[0];
        double tD=(double)res[1],tT=(double)res[2],tF=(double)res[3];

        StringBuilder sb=new StringBuilder();
        sb.append("  SHORTEST ROUTE FOUND\n");
        sb.append("  ╔══════════════════════════════════╗\n");
        sb.append("  ║  File      : ").append(pad(loadedFileName,21)).append("║\n");
        sb.append("  ║  Criterion : ").append(pad(crit,21)).append("║\n");
        sb.append("  ╚══════════════════════════════════╝\n\n");
        sb.append("  ROUTE\n");
        sb.append("  ┌────────────────────────────────────\n");
        sb.append("  │  ").append(String.join("  >>  ",currentPath)).append("\n");
        sb.append("  └────────────────────────────────────\n\n");
        sb.append("  ROAD SEGMENTS\n");
        sb.append("  ════════════════════════════════════\n");
        for(int i=0;i<currentPath.size()-1;i++){
            String a=currentPath.get(i),b=currentPath.get(i+1);
            for(Edge e:graph.getOrDefault(a,Collections.emptyList())){
                if(e.to.equals(b)){
                    sb.append(String.format("  > %s  >>  %s%n",a,b));
                    sb.append(String.format("    |-- Distance  : %6.1f km%n",   e.distance));
                    sb.append(String.format("    |-- Time      : %6.0f mins%n", e.time));
                    sb.append(String.format("    +-- Fuel      : %6.1f L%n",    e.fuel));
                    if(i<currentPath.size()-2) sb.append("    ................................\n");
                    break;
                }
            }
        }
        sb.append("  ════════════════════════════════════\n\n");
        sb.append("  TRIP TOTALS\n");
        sb.append("  ┌────────────────────────────────────\n");
        sb.append(String.format("  │  Distance : %.1f km%n",  tD));
        sb.append(String.format("  │  Time     : %.0f mins%n",tT));
        sb.append(String.format("  │  Fuel     : %.1f L%n",   tF));
        sb.append(String.format("  │  Stops    : %d cities%n",currentPath.size()));
        sb.append("  └────────────────────────────────────\n");

        resultArea.setForeground(new Color(170,255,200));
        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0);
        statusLabel.setText("Route: "+String.join(" >> ",currentPath));
        mapPanel.repaint();
        startCarAnimation();
    }

    private String pad(String s,int len){
        if(s.length()>=len) return s.substring(0,len);
        return s+" ".repeat(len-s.length());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Node Map Panel
    // ════════════════════════════════════════════════════════════════════════
    class NodeMapPanel extends JPanel {
        private String hoveredNode=null;
        private static final int NODE_R=28;

        NodeMapPanel(){
            setBackground(BG_ASPHALT); setOpaque(true);
            addMouseMotionListener(new MouseAdapter(){
                public void mouseMoved(MouseEvent e){
                    String prev=hoveredNode; hoveredNode=null;
                    for(Map.Entry<String,Point> en:nodePositions.entrySet())
                        if(e.getPoint().distance(en.getValue())<NODE_R+8){hoveredNode=en.getKey();break;}
                    if(!Objects.equals(prev,hoveredNode)){
                        if(hoveredNode!=null)
                            statusLabel.setText("City: "+hoveredNode+"  |  roads: "+
                                graph.getOrDefault(hoveredNode,Collections.emptyList()).size());
                        repaint();
                    }
                }
            });
        }

        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,     RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,        RenderingHints.VALUE_RENDER_QUALITY);
            drawMapBg(g2);
            if(nodes.isEmpty()){drawEmpty(g2);return;}
            drawRoads(g2);
            drawPathRoad(g2);
            drawCities(g2);
            drawCar(g2);
        }

        private void drawMapBg(Graphics2D g2){
            int w=getWidth(), h=getHeight();

            // ── 1. Grass / terrain base ──────────────────────────────────
            // Earthy green-brown like satellite road map terrain
            GradientPaint grassGrad = new GradientPaint(
                0, 0,   new Color(58, 80, 42),
                w, h,   new Color(46, 65, 34));
            g2.setPaint(grassGrad);
            g2.fillRect(0, 0, w, h);

            // Subtle grass texture — irregular light/dark patches
            java.util.Random rng = new java.util.Random(42); // fixed seed = stable
            for(int i=0; i<120; i++){
                int px = rng.nextInt(w), py = rng.nextInt(h);
                int pr = 18 + rng.nextInt(38);
                boolean light = rng.nextBoolean();
                g2.setColor(light
                    ? new Color(68, 95, 48, 55)
                    : new Color(34, 52, 22, 55));
                g2.fillOval(px-pr/2, py-pr/2, pr, pr);
            }

            // ── 2. Horizontal road bands (main roads across the map) ─────
            // These are purely decorative asphalt strips behind the graph
            int[] roadYs  = { h/6, h/2, 5*h/6 };
            int[] roadWs  = { 44,  56,  40 };
            for(int i=0; i<roadYs.length; i++){
                int ry = roadYs[i], rw = roadWs[i];
                // Road shoulder / curb
                g2.setColor(new Color(90, 95, 82, 160));
                g2.fillRect(0, ry - rw/2 - 5, w, rw + 10);
                // Asphalt surface
                g2.setColor(new Color(52, 55, 58, 200));
                g2.fillRect(0, ry - rw/2, w, rw);
                // Asphalt surface highlight (subtle lighter centre stripe)
                g2.setColor(new Color(65, 68, 72, 120));
                g2.fillRect(0, ry - 4, w, 8);
                // Dashed centre line
                g2.setColor(new Color(220, 195, 50, 160));
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{18f, 14f}, 0));
                g2.drawLine(0, ry, w, ry);
                // Lane edge white lines
                g2.setColor(new Color(240, 240, 240, 100));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawLine(0, ry - rw/2, w, ry - rw/2);
                g2.drawLine(0, ry + rw/2, w, ry + rw/2);
            }

            // ── 3. Vertical road bands ────────────────────────────────────
            int[] roadXs  = { w/5, w/2, 4*w/5 };
            int[] roadVWs = { 38,  52,  36 };
            for(int i=0; i<roadXs.length; i++){
                int rx = roadXs[i], rw = roadVWs[i];
                g2.setColor(new Color(90, 95, 82, 130));
                g2.fillRect(rx - rw/2 - 5, 0, rw + 10, h);
                g2.setColor(new Color(52, 55, 58, 185));
                g2.fillRect(rx - rw/2, 0, rw, h);
                g2.setColor(new Color(65, 68, 72, 100));
                g2.fillRect(rx - 4, 0, 8, h);
                g2.setColor(new Color(220, 195, 50, 140));
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{18f, 14f}, 0));
                g2.drawLine(rx, 0, rx, h);
                g2.setColor(new Color(240, 240, 240, 90));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawLine(rx - rw/2, 0, rx - rw/2, h);
                g2.drawLine(rx + rw/2, 0, rx + rw/2, h);
            }

            // ── 4. Intersections — slightly lighter asphalt squares ───────
            int[][] crossInfo = {
                {roadXs[0], roadYs[0], roadVWs[0], roadWs[0]},
                {roadXs[0], roadYs[1], roadVWs[0], roadWs[1]},
                {roadXs[0], roadYs[2], roadVWs[0], roadWs[2]},
                {roadXs[1], roadYs[0], roadVWs[1], roadWs[0]},
                {roadXs[1], roadYs[1], roadVWs[1], roadWs[1]},
                {roadXs[1], roadYs[2], roadVWs[1], roadWs[2]},
                {roadXs[2], roadYs[0], roadVWs[2], roadWs[0]},
                {roadXs[2], roadYs[1], roadVWs[2], roadWs[1]},
                {roadXs[2], roadYs[2], roadVWs[2], roadWs[2]},
            };
            for(int[] ci : crossInfo){
                int xi2=ci[0], yi2=ci[1], iw=ci[2]+10, ih=ci[3]+10;
                g2.setColor(new Color(60, 63, 67, 210));
                g2.fillRect(xi2 - iw/2, yi2 - ih/2, iw, ih);
                g2.setColor(new Color(240, 240, 240, 80));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(xi2 - iw/2, yi2 - ih/2, iw, ih);
            }

            // ── 5. Pavement / sidewalk border strips on main roads ────────
            g2.setStroke(new BasicStroke(1f));
            for(int yi : roadYs){
                g2.setColor(new Color(155, 148, 118, 70));
                g2.fillRect(0, yi - roadWs[0]/2 - 9, w, 4);
                g2.fillRect(0, yi + roadWs[0]/2 + 5, w, 4);
            }

            // ── 6. Road markings — pedestrian crossing zebra stripes ─────
            int[] crossX = { w/5, 4*w/5 };
            int[] crossY = { h/2 };
            for(int cx2 : crossX){
                for(int cy2 : crossY){
                    int cw = 14, ch = 5, gap = 9;
                    int stripes = 5;
                    int totalH  = stripes * ch + (stripes-1) * gap;
                    int startY  = cy2 - totalH/2;
                    for(int s=0; s<stripes; s++){
                        g2.setColor(new Color(255, 255, 255, 70));
                        g2.fillRect(cx2 - cw/2, startY + s*(ch+gap), cw, ch);
                    }
                }
            }

            // ── 7. Subtle noise / grain over whole background ─────────────
            rng = new java.util.Random(99);
            for(int i=0; i<1800; i++){
                int px = rng.nextInt(w), py = rng.nextInt(h);
                int alpha = 8 + rng.nextInt(18);
                g2.setColor(new Color(0, 0, 0, alpha));
                g2.fillRect(px, py, 1, 1);
            }

            // ── 8. Vignette — darkens edges for map depth ─────────────────
            RadialGradientPaint vig = new RadialGradientPaint(
                new Point2D.Float(w / 2f, h / 2f), Math.max(w, h) * 0.68f,
                new float[]{0.30f, 1f},
                new Color[]{new Color(0,0,0,0), new Color(0,0,0,185)});
            g2.setPaint(vig);
            g2.fillRect(0, 0, w, h);

            // ── 9. Compass rose ───────────────────────────────────────────
            drawCompass(g2, w-55, h-55, 38);
        }

        private void drawCompass(Graphics2D g2,int cx,int cy,int r){
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255,185,30,50)); g2.drawOval(cx-r,cy-r,r*2,r*2);
            g2.setColor(new Color(255,185,30,25)); g2.drawOval(cx-r+6,cy-r+6,(r-6)*2,(r-6)*2);
            int[] nax={cx,cx-5,cx+5}; int[] nay={cy-r+4,cy-4,cy-4};
            g2.setColor(new Color(255,185,30,130)); g2.fillPolygon(nax,nay,3);
            int[] sax={cx,cx-4,cx+4}; int[] say={cy+r-4,cy+4,cy+4};
            g2.setColor(new Color(120,135,155,80)); g2.fillPolygon(sax,say,3);
            g2.setFont(new Font("Monospaced",Font.BOLD,9));
            FontMetrics fm=g2.getFontMetrics();
            g2.setColor(new Color(255,185,30,180)); g2.drawString("N",cx-fm.stringWidth("N")/2,cy-r+16);
            g2.setColor(new Color(120,135,155,100));
            g2.drawString("S",cx-fm.stringWidth("S")/2,cy+r-6);
            g2.drawString("E",cx+r-10,cy+fm.getAscent()/2-1);
            g2.drawString("W",cx-r+4, cy+fm.getAscent()/2-1);
            g2.setColor(new Color(255,185,30,70)); g2.fillOval(cx-3,cy-3,6,6);
        }

        private void drawRoads(Graphics2D g2){
            Set<String> pathEdges=new HashSet<>();
            for(int i=0;i<currentPath.size()-1;i++){
                pathEdges.add(currentPath.get(i)+"--"+currentPath.get(i+1));
                pathEdges.add(currentPath.get(i+1)+"--"+currentPath.get(i));
            }
            Set<String> drawn=new HashSet<>();
            for(Edge e:allEdges){
                String key=e.from.compareTo(e.to)<0?e.from+"--"+e.to:e.to+"--"+e.from;
                if(drawn.contains(key)) continue; drawn.add(key);
                if(pathEdges.contains(e.from+"--"+e.to)) continue;
                Point p1=nodePositions.get(e.from),p2=nodePositions.get(e.to);
                if(p1==null||p2==null) continue;
                boolean hov=e.from.equals(hoveredNode)||e.to.equals(hoveredNode);
                g2.setStroke(new BasicStroke(hov?8f:5.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(20,25,30)); g2.drawLine(p1.x,p1.y,p2.x,p2.y);
                g2.setStroke(new BasicStroke(hov?5f:3.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g2.setColor(hov?new Color(80,100,120):new Color(48,56,66)); g2.drawLine(p1.x,p1.y,p2.x,p2.y);
                g2.setStroke(new BasicStroke(1f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,0,new float[]{6f,7f},0));
                g2.setColor(new Color(200,180,60,hov?120:45)); g2.drawLine(p1.x,p1.y,p2.x,p2.y);
                if(hov){
                    int mx=(p1.x+p2.x)/2,my=(p1.y+p2.y)/2;
                    drawRoadTag(g2,String.format("%.0fkm / %.0fmin / %.1fL",e.distance,e.time,e.fuel),
                        mx,my,new Color(30,40,55,230),new Color(100,130,170,180),new Color(180,210,255));
                }
            }
        }

        private void drawPathRoad(Graphics2D g2){
            if(currentPath.size()<2) return;
            for(int i=0;i<currentPath.size()-1;i++){
                Point p1=nodePositions.get(currentPath.get(i));
                Point p2=nodePositions.get(currentPath.get(i+1));
                if(p1==null||p2==null) continue;
                g2.setColor(new Color(255,185,30,12));
                g2.setStroke(new BasicStroke(26f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g2.drawLine(p1.x,p1.y,p2.x,p2.y);
                g2.setColor(new Color(255,185,30,28));
                g2.setStroke(new BasicStroke(16f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g2.drawLine(p1.x,p1.y,p2.x,p2.y);
                g2.setColor(new Color(100,65,0));
                g2.setStroke(new BasicStroke(8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g2.drawLine(p1.x,p1.y,p2.x,p2.y);
                g2.setColor(new Color(190,135,15));
                g2.setStroke(new BasicStroke(5.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g2.drawLine(p1.x,p1.y,p2.x,p2.y);
                g2.setColor(new Color(255,255,255,160));
                g2.setStroke(new BasicStroke(1.4f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,0,new float[]{9f,6f},0));
                g2.drawLine(p1.x,p1.y,p2.x,p2.y);
                drawArrow(g2,p1,p2);
                String a=currentPath.get(i),b=currentPath.get(i+1);
                for(Edge e:graph.getOrDefault(a,Collections.emptyList())){
                    if(e.to.equals(b)){
                        int mx=(p1.x+p2.x)/2,my=(p1.y+p2.y)/2;
                        drawRoadTag(g2,String.format("%.0fkm / %.0fmin / %.1fL",e.distance,e.time,e.fuel),
                            mx,my,new Color(28,18,0,230),new Color(255,185,30,170),ACCENT_AMBER);
                        break;
                    }
                }
            }
        }

        private void drawRoadTag(Graphics2D g2,String lbl,int mx,int my,Color bg,Color border,Color fg){
            g2.setFont(new Font("Monospaced",Font.BOLD,10));
            FontMetrics fm=g2.getFontMetrics(); int tw=fm.stringWidth(lbl);
            g2.setColor(bg);     g2.fillRoundRect(mx-tw/2-7,my-17,tw+14,17,8,8);
            g2.setColor(border); g2.setStroke(new BasicStroke(1.2f));
                                 g2.drawRoundRect(mx-tw/2-7,my-17,tw+14,17,8,8);
            g2.setColor(fg);     g2.drawString(lbl,mx-tw/2,my-4);
        }

        private void drawArrow(Graphics2D g2,Point f,Point t){
            double dx=t.x-f.x,dy=t.y-f.y,len=Math.sqrt(dx*dx+dy*dy);
            if(len==0) return;
            double ux=dx/len,uy=dy/len,mx=(f.x+t.x)/2.0,my=(f.y+t.y)/2.0; int as=11;
            int[] xs={(int)mx,(int)(mx-as*ux+as*.5*uy),(int)(mx-as*ux-as*.5*uy)};
            int[] ys={(int)my,(int)(my-as*uy-as*.5*ux),(int)(my-as*uy+as*.5*ux)};
            g2.setColor(new Color(255,240,140)); g2.setStroke(new BasicStroke(1));
            g2.fillPolygon(xs,ys,3);
        }

        private void drawCities(Graphics2D g2){
            for(String node:nodes){
                Point p=nodePositions.get(node); if(p==null) continue;
                boolean onPath=currentPath.contains(node),hov=node.equals(hoveredNode);
                boolean isStart=!currentPath.isEmpty()&&node.equals(currentPath.get(0));
                boolean isEnd  =!currentPath.isEmpty()&&node.equals(currentPath.get(currentPath.size()-1));
                if(onPath||hov){
                    Color gc=isStart?new Color(40,200,180,35):isEnd?new Color(255,185,30,35):
                             onPath?new Color(60,210,100,28):new Color(100,150,220,25);
                    g2.setColor(gc);
                    g2.fillOval(p.x-NODE_R-13,p.y-NODE_R-13,(NODE_R+13)*2,(NODE_R+13)*2);
                }
                g2.setColor(new Color(0,0,0,90));
                g2.fillOval(p.x-NODE_R+3,p.y-NODE_R+3,NODE_R*2,NODE_R*2);
                Color fill=isStart?new Color(0,90,72):isEnd?new Color(85,52,0):
                           onPath?new Color(0,65,32):hov?new Color(18,36,64):new Color(22,30,42);
                Color stroke=isStart?ACCENT_TEAL:isEnd?ACCENT_AMBER:
                             onPath?ACCENT_GREEN:hov?new Color(120,160,220):new Color(55,75,105);
                g2.setColor(fill); g2.fillOval(p.x-NODE_R,p.y-NODE_R,NODE_R*2,NODE_R*2);
                g2.setColor(stroke); g2.setStroke(new BasicStroke(onPath||hov?3f:1.8f));
                g2.drawOval(p.x-NODE_R,p.y-NODE_R,NODE_R*2,NODE_R*2);
                if(onPath){
                    g2.setColor(new Color(stroke.getRed(),stroke.getGreen(),stroke.getBlue(),55));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawOval(p.x-NODE_R+5,p.y-NODE_R+5,(NODE_R-5)*2,(NODE_R-5)*2);
                }
                int fs=node.length()>7?8:node.length()>5?9:10;
                g2.setFont(new Font("Monospaced",Font.BOLD,fs));
                FontMetrics fm=g2.getFontMetrics();
                g2.setColor(onPath?new Color(200,255,220):hov?new Color(180,210,255):new Color(175,190,210));
                g2.drawString(node,p.x-fm.stringWidth(node)/2,p.y+fm.getAscent()/2-1);
                if(isStart||isEnd){
                    String badge=isStart?"START":"END";
                    Color bc=isStart?ACCENT_TEAL:ACCENT_AMBER;
                    g2.setFont(new Font("Monospaced",Font.BOLD,9));
                    fm=g2.getFontMetrics(); int bw=fm.stringWidth(badge);
                    int bx=p.x-bw/2-5, by=p.y-NODE_R-20;
                    g2.setColor(new Color(0,0,0,170)); g2.fillRoundRect(bx,by,bw+10,15,5,5);
                    g2.setColor(bc); g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(bx,by,bw+10,15,5,5);
                    g2.setColor(bc); g2.drawString(badge,p.x-bw/2,by+12);
                }
            }
        }

        private void drawCar(Graphics2D g2){
            if(currentPath.size()<2) return;
            Point2D.Float pos=getCarPos(); if(pos==null) return;
            double angle=getCarAngle();
            AffineTransform saved=g2.getTransform();
            g2.translate(pos.x,pos.y);
            g2.rotate(angle);
            g2.setColor(new Color(0,0,0,90));
            g2.fillOval(-14,5,28,9);
            if(animating){
                GradientPaint beam=new GradientPaint(14,0,new Color(255,240,150,70),
                    45,0,new Color(255,240,150,0));
                g2.setPaint(beam);
                int[] bx={14,48,48}; int[] by={0,-14,14};
                g2.fillPolygon(bx,by,3);
            }
            RoundRectangle2D body=new RoundRectangle2D.Float(-13,-6,26,12,6,6);
            g2.setColor(new Color(200,45,45)); g2.fill(body);
            RoundRectangle2D roof=new RoundRectangle2D.Float(-5,-8,14,16,4,4);
            g2.setColor(new Color(170,35,35)); g2.fill(roof);
            g2.setColor(new Color(160,225,255,210));
            g2.fillRoundRect(4,-5,6,4,2,2);
            g2.setColor(new Color(120,180,210,180));
            g2.fillRoundRect(-10,-5,6,4,2,2);
            g2.setColor(new Color(140,200,235,160));
            g2.fillRoundRect(4,1,6,4,2,2);
            g2.fillRoundRect(-10,1,6,4,2,2);
            g2.setColor(new Color(240,80,80));
            g2.setStroke(new BasicStroke(0.9f));
            g2.draw(body);
            g2.setColor(new Color(25,25,25));
            g2.fillRoundRect(-14,-8,7,5,2,2);
            g2.fillRoundRect( 7,-8,7,5,2,2);
            g2.fillRoundRect(-14, 3,7,5,2,2);
            g2.fillRoundRect( 7, 3,7,5,2,2);
            g2.setColor(new Color(90,90,90));
            g2.setStroke(new BasicStroke(0.7f));
            g2.drawRoundRect(-14,-8,7,5,2,2);
            g2.drawRoundRect( 7,-8,7,5,2,2);
            g2.drawRoundRect(-14, 3,7,5,2,2);
            g2.drawRoundRect( 7, 3,7,5,2,2);
            g2.setColor(new Color(255,245,180));
            g2.fillOval(12,-6,4,3); g2.fillOval(12,3,4,3);
            g2.setColor(new Color(220,50,50));
            g2.fillOval(-16,-6,4,3); g2.fillOval(-16,3,4,3);
            g2.setColor(new Color(255,255,255,40));
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawLine(-10,0,10,0);
            g2.setTransform(saved);
        }

        private void drawEmpty(Graphics2D g2){
            int cx=getWidth()/2,cy=getHeight()/2;
            g2.setFont(new Font("Monospaced",Font.BOLD,18)); FontMetrics fm=g2.getFontMetrics();
            String l1="No road map loaded";
            g2.setColor(new Color(55,70,85)); g2.drawString(l1,cx-fm.stringWidth(l1)/2,cy-16);
            g2.setFont(new Font("Monospaced",Font.PLAIN,12)); fm=g2.getFontMetrics();
            String l2="Upload a CSV file to begin";
            g2.setColor(new Color(42,55,68)); g2.drawString(l2,cx-fm.stringWidth(l2)/2,cy+14);
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
    private JPanel roadCard(String title,Color accent){
        JPanel card=new JPanel(); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),55),1),
            new EmptyBorder(12,14,12,14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        JPanel trow=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)); trow.setOpaque(false);
        JLabel bar=new JLabel("| "); bar.setFont(new Font("Monospaced",Font.BOLD,13)); bar.setForeground(accent);
        JLabel lbl=new JLabel(title); lbl.setFont(new Font("Monospaced",Font.BOLD,11)); lbl.setForeground(accent);
        trow.add(bar); trow.add(lbl);
        trow.setMaximumSize(new Dimension(Integer.MAX_VALUE,22));
        trow.setBorder(new EmptyBorder(0,0,10,0));
        card.add(trow); return card;
    }
    private JButton makeBtn(String text,Color bg,Color fg){
        JButton b=new JButton(text);
        b.setFont(new Font("Monospaced",Font.BOLD,12));
        b.setForeground(fg); b.setBackground(bg);
        b.setBorder(new EmptyBorder(10,18,10,18)); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Color hover=bg.brighter();
        b.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){b.setBackground(hover);}
            public void mouseExited(MouseEvent e) {b.setBackground(bg);}
        });
        return b;
    }
    private JLabel dimLbl(String t){
        JLabel l=new JLabel(t); l.setFont(new Font("Monospaced",Font.BOLD,10)); l.setForeground(TEXT_DIM); return l;
    }
    private JComboBox<String> roadCombo(String[] items){
        Color cbBg  = new Color(26, 34, 46);
        Color cbFg  = new Color(255, 185, 30);
        Color selBg = new Color(50, 100, 160);
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(new Font("Monospaced", Font.BOLD, 12));
        cb.setBackground(cbBg); cb.setForeground(cbFg); cb.setOpaque(true);
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        cb.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 100, 130), 1),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,Object value,
                    int index,boolean isSelected,boolean cellHasFocus){
                JLabel lbl=(JLabel)super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
                lbl.setFont(new Font("Monospaced",Font.BOLD,12));
                lbl.setBorder(new EmptyBorder(5,10,5,10)); lbl.setOpaque(true);
                if(isSelected){lbl.setBackground(selBg);lbl.setForeground(Color.WHITE);}
                else{lbl.setBackground(cbBg);lbl.setForeground(cbFg);}
                return lbl;
            }
        });
        return cb;
    }
    private JPanel legendItem(Color c,String t){
        JPanel r=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0)); r.setOpaque(false);
        JLabel dot=new JLabel("X"); dot.setForeground(c); dot.setFont(new Font("Monospaced",Font.PLAIN,13));
        JLabel lbl=new JLabel(t); lbl.setFont(new Font("Monospaced",Font.PLAIN,11)); lbl.setForeground(TEXT_DIM);
        r.add(dot); r.add(lbl); return r;
    }

    public static void main(String[] args){
        try{
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("ComboBox.background",        new Color(26,34,46));
            UIManager.put("ComboBox.foreground",        new Color(220,230,250));
            UIManager.put("ComboBox.selectionBackground",new Color(50,100,160));
            UIManager.put("ComboBox.selectionForeground",Color.WHITE);
            UIManager.put("ComboBox.buttonBackground",  new Color(40,50,66));
            UIManager.put("ComboBox.disabledBackground",new Color(26,34,46));
        }catch(Exception ignored){}
        SwingUtilities.invokeLater(MIDTERM_LAB2_PARANE::new);
    }
}