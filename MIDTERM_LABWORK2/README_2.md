# Travelling Salesman Problem — Shortest Path Finder
### Midterm Lab Work 2 | Design and Analysis of Algorithms

---

## Overview

This program is a visual implementation of the **Travelling Salesman Problem (TSP)** using **Dijkstra's Algorithm** to find the shortest path between nodes on an interactive road map. It is built using **Java Swing** and supports CSV-driven graph data with a fully animated car that drives along the computed shortest route.

---

## Algorithm Used

### Dijkstra's Shortest Path Algorithm

Dijkstra's Algorithm was chosen because it efficiently finds the **single-source shortest path** in a weighted graph with non-negative edge weights. It works by:

1. Initializing all node distances to infinity except the start node (set to 0).
2. Using a **Priority Queue** to always process the node with the smallest known distance next.
3. For each neighbor, calculating a new tentative distance and updating if it is shorter.
4. Repeating until the destination node is reached.

The algorithm supports three optimization criteria selectable by the user:
- **Distance** (kilometers)
- **Time** (minutes)
- **Fuel** (liters)

Each criterion uses the corresponding edge weight during path calculation, so the "shortest" path differs depending on what is being optimized.

---

## Approach

### Graph Representation
- The graph is built as an **adjacency list** using `Map<String, List<Edge>>`.
- Each CSV row creates **two directed edges** (forward and reverse) to make the graph **bidirectional**, ensuring paths can be found in both directions between any two nodes.

### CSV Format
```
From Node, To Node, Distance (km), Time (mins), Fuel (Liters)
```

### Visual Map
- Nodes are placed in a **circular layout** and drawn as road-sign style city markers.
- Roads are drawn with casing, surface color, and center lane dashes to simulate a real map look.
- The **shortest path** is highlighted in amber/gold with directional arrows and segment labels showing Distance, Time, and Fuel for each leg.

### Car Animation
- After finding the shortest path, a **top-down animated car** automatically drives along the route using a `javax.swing.Timer`.
- The car rotates to face the correct direction on each road segment.
- A **Replay Animation** button allows re-watching the route traversal.

### Result Display
- The result panel shows a full breakdown of each road segment with its individual Distance, Time, and Fuel values, followed by the trip totals.

---

## Challenges Faced

### 1. Path Calculation — Shortest vs. Longest Path

I struggled a bit more with the path calculation this time because the previous logic focus on finding the longest path.
This was the most significant challenge during development. The initial implementation only stored edges one-directionally as listed in the CSV file. Because of this, Dijkstra's algorithm could not traverse routes in reverse forcing it to take longer detours instead of direct connections.

For example, the correct shortest path from **IMUS → INDANG** by distance should be:

```
IMUS → BACOOR → SILANG → INDANG  =  34 km
```

But the program was returning:

```
IMUS → BACOOR → SILANG → KAWIT → INDANG  =  46 km
```

**Root Cause:** The CSV only listed `SILANG → INDANG` as a one-way connection. Since `INDANG → SILANG` did not exist in the graph, Dijkstra could not use that road and was forced onto a longer route — effectively finding what appeared to be the **longest available path** rather than the true shortest one.

**Fix Applied:** When loading the CSV, each edge is now added **twice** — once in the original direction and once in reverse — making the graph fully bidirectional:

```java
Edge e   = new Edge(from, to, dist, time, fuel);   // forward
Edge rev = new Edge(to, from, dist, time, fuel);   // reverse
graph.get(from).add(e);
graph.get(to).add(rev);
```

After this fix, the algorithm correctly computes the true shortest path for all criteria.

### 2. Timer Ambiguity Error

Using `import java.util.*` caused a naming conflict between `java.util.Timer` and `javax.swing.Timer`. This was resolved by replacing the wildcard import with explicit individual imports, removing `java.util.Timer` from scope entirely.

### 3. ComboBox Visibility

Java's system Look-and-Feel on Windows overrides custom combo box colors, making text invisible against dark backgrounds. This was fixed by switching to the **Cross-Platform Look-and-Feel** and implementing a custom `DefaultListCellRenderer` that enforces the dark background with amber text explicitly.

---

## Tools & Technologies

| Component       | Technology                    |
|----------------|-------------------------------|
| Language        | Java (JDK 8+)                 |
| UI Framework    | Java Swing                    |
| Algorithm       | Dijkstra's Shortest Path      |
| Data Input      | CSV File Upload               |
| Animation       | `javax.swing.Timer`           |
| Graph Structure | Adjacency List (bidirectional)|

---

## How to Run

1. Compile: `javac MIDTERM_LABWORK2/TravelingSalesmanApp.java`
2. Run: `java MIDTERM_LABWORK2.TravelingSalesmanApp`
3. Click **UPLOAD CSV** and select your CSV file.
4. Choose **Departure**, **Destination**, and **Optimize By** criteria.
5. Click **FIND SHORTEST ROUTE** to compute and animate the path.

---


