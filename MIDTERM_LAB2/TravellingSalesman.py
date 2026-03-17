"""
╔══════════════════════════════════════════════════════════════╗
║   TRAVELLING SALESMAN PROBLEM — Road Map Edition             ║
║   Python · Tkinter · Canvas · Dijkstra's Algorithm           ║
║   Animated car · Force-Directed Layout · CSV Import          ║
╚══════════════════════════════════════════════════════════════╝

Requirements:
    Python 3.8+  (stdlib only — no pip installs needed)
    tkinter  (ships with standard Python on Windows/macOS/Linux)

Run:
    python travelling_salesman.py
"""

import tkinter as tk
from tkinter import ttk, filedialog, messagebox, font as tkfont
import csv, math, random, heapq, os, time
from collections import defaultdict


# ══════════════════════════════════════════════════════════════════════
#  PALETTE
# ══════════════════════════════════════════════════════════════════════
BG_DEEP    = "#0a0d12"
BG_PANEL   = "#0f1318"
BG_CARD    = "#141a22"
BG_RAISED  = "#1a2230"
AMBER      = "#ffb91e"
AMBER_DIM  = "#c48a00"
GREEN      = "#3cd264"
GREEN_DARK = "#0a3318"
TEAL       = "#28c8b4"
RED        = "#ff4b4b"
BLUE       = "#4a9eff"
ROAD_COL   = "#2a323c"
ROAD_LT    = "#364250"
TEXT_BRIGHT= "#e8eef8"
TEXT_MID   = "#8ca0b8"
TEXT_DIM   = "#3a4a5e"
WHITE      = "#ffffff"


def hex_to_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))

def rgb_to_hex(r, g, b):
    return f"#{int(r):02x}{int(g):02x}{int(b):02x}"

def blend(c1, c2, t):
    r1,g1,b1 = hex_to_rgb(c1)
    r2,g2,b2 = hex_to_rgb(c2)
    return rgb_to_hex(r1+(r2-r1)*t, g1+(g2-g1)*t, b1+(b2-b1)*t)

def alpha_blend(color_hex, alpha, bg_hex=BG_DEEP):
    return blend(bg_hex, color_hex, alpha)


# ══════════════════════════════════════════════════════════════════════
#  DATA
# ══════════════════════════════════════════════════════════════════════
class Edge:
    __slots__ = ("frm","to","distance","time","fuel")
    def __init__(self, frm, to, distance, time_, fuel):
        self.frm = frm
        self.to = to
        self.distance = distance
        self.time = time_
        self.fuel = fuel


class Graph:
    def __init__(self):
        self.adj: dict[str, list[Edge]] = defaultdict(list)
        self.all_edges: list[Edge] = []
        self.nodes: list[str] = []

    def clear(self):
        self.adj.clear()
        self.all_edges.clear()
        self.nodes.clear()

    def add_edge(self, frm, to, d, t, f):
        e1 = Edge(frm, to, d, t, f)
        e2 = Edge(to, frm, d, t, f)
        self.adj[frm].append(e1)
        self.adj[to].append(e2)
        self.all_edges.append(e1)
        if frm not in self.nodes: self.nodes.append(frm)
        if to  not in self.nodes: self.nodes.append(to)

    def dijkstra(self, start, end, criterion):
        dist = {n: math.inf for n in self.nodes}
        prev = {}
        dist[start] = 0.0
        pq = [(0.0, start)]
        while pq:
            d, u = heapq.heappop(pq)
            if d > dist[u]: continue
            if u == end: break
            for e in self.adj.get(u, []):
                w = e.distance if criterion == "Distance" else e.time if criterion == "Time" else e.fuel
                nd = dist[u] + w
                if nd < dist[e.to]:
                    dist[e.to] = nd
                    prev[e.to] = u
                    heapq.heappush(pq, (nd, e.to))
        path = []
        n = end
        while n in prev or n == start:
            path.append(n)
            if n == start: break
            n = prev.get(n)
            if n is None: return None
        path.reverse()
        if not path or path[0] != start: return None
        tD = tT = tF = 0.0
        for i in range(len(path)-1):
            a, b = path[i], path[i+1]
            for e in self.adj.get(a, []):
                if e.to == b:
                    tD += e.distance; tT += e.time; tF += e.fuel; break
        return path, tD, tT, tF


# ══════════════════════════════════════════════════════════════════════
#  FORCE-DIRECTED LAYOUT
# ══════════════════════════════════════════════════════════════════════
def force_layout(nodes, edges, W, H, pad=80):
    n = len(nodes)
    if n == 0: return {}
    pos = {}
    for i, nd in enumerate(nodes):
        angle = 2*math.pi*i/n - math.pi/2
        r = min(W, H) * 0.33
        pos[nd] = [W/2 + r*math.cos(angle), H/2 + r*math.sin(angle)]

    k      = math.sqrt(W * H / max(n, 1))
    idealL = k * 1.1
    repK   = k * k * 2.2

    for itr in range(450):
        disp = {nd: [0.0, 0.0] for nd in nodes}

        # Repulsion
        for i in range(n):
            for j in range(i+1, n):
                ni, nj = nodes[i], nodes[j]
                dx = pos[ni][0]-pos[nj][0]
                dy = pos[ni][1]-pos[nj][1]
                dist_ = max(math.sqrt(dx*dx+dy*dy), 1.0)
                force = repK / (dist_*dist_)
                fx, fy = (dx/dist_)*force, (dy/dist_)*force
                disp[ni][0]+=fx; disp[ni][1]+=fy
                disp[nj][0]-=fx; disp[nj][1]-=fy

        # Attraction
        seen = set()
        for e in edges:
            key = (e.frm, e.to) if e.frm < e.to else (e.to, e.frm)
            if key in seen: continue
            seen.add(key)
            pi, pj = pos.get(e.frm), pos.get(e.to)
            if not pi or not pj: continue
            dx = pj[0]-pi[0]; dy = pj[1]-pi[1]
            dist_ = max(math.sqrt(dx*dx+dy*dy), 1.0)
            stretch = (dist_ - idealL) / dist_ * 0.28
            disp[e.frm][0] += dx*stretch; disp[e.frm][1] += dy*stretch
            disp[e.to][0]  -= dx*stretch; disp[e.to][1]  -= dy*stretch

        # Gravity
        cx = sum(pos[nd][0] for nd in nodes)/n
        cy = sum(pos[nd][1] for nd in nodes)/n
        for nd in nodes:
            disp[nd][0] += (W/2-cx)*0.04
            disp[nd][1] += (H/2-cy)*0.04

        # Apply
        temp = max(16 - itr*0.035, 1.2)
        for nd in nodes:
            d = disp[nd]
            dLen = max(math.sqrt(d[0]**2+d[1]**2), 0.001)
            move = min(dLen, temp)
            pos[nd][0] = max(pad, min(W-pad, pos[nd][0]+(d[0]/dLen)*move))
            pos[nd][1] = max(pad, min(H-pad, pos[nd][1]+(d[1]/dLen)*move))

    return {nd: (int(pos[nd][0]), int(pos[nd][1])) for nd in nodes}


# ══════════════════════════════════════════════════════════════════════
#  MAIN APPLICATION
# ══════════════════════════════════════════════════════════════════════
class TSPApp(tk.Tk):
    # ── CAR SPEED ──
    CAR_SPEED = 0.016
    NODE_R    = 26

    def __init__(self):
        super().__init__()
        self.title("Travelling Salesman — Road Map Edition")
        self.configure(bg=BG_DEEP)
        self.minsize(1100, 720)
        self.geometry("1280x820")

        self.graph = Graph()
        self.node_pos: dict[str, tuple] = {}
        self.current_path: list[str] = []
        self.loaded_file = "none"
        self.hovered_node = None

        # Car animation
        self.car_segment = 0
        self.car_t       = 0.0
        self.car_animating = False
        self._car_after_id = None

        # Terrain dots (seeded random, stable)
        rng = random.Random(42)
        self._terrain = [(rng.random(), rng.random(), 18+rng.random()*38, rng.random()>0.5)
                         for _ in range(90)]

        self._build_ui()
        self.after(100, self._initial_draw)

    # ────────────────────────────────────────────────────────────────
    #  UI BUILD
    # ────────────────────────────────────────────────────────────────
    def _build_ui(self):
        self._build_header()
        self._build_body()
        self._build_statusbar()

    def _build_header(self):
        hdr = tk.Frame(self, bg="#07090d", height=60)
        hdr.pack(fill=tk.X, side=tk.TOP)
        hdr.pack_propagate(False)

        # Bottom amber line
        sep = tk.Frame(hdr, bg=AMBER, height=2)
        sep.place(relx=0, rely=1.0, relwidth=1.0, anchor="sw")

        left = tk.Frame(hdr, bg="#07090d")
        left.pack(side=tk.LEFT, padx=20, pady=8)

        icon_lbl = tk.Label(left, text="🚗", font=("Segoe UI Emoji", 22),
                            bg="#07090d", fg=AMBER)
        icon_lbl.pack(side=tk.LEFT, padx=(0,10))

        title_frame = tk.Frame(left, bg="#07090d")
        title_frame.pack(side=tk.LEFT)

        row1 = tk.Frame(title_frame, bg="#07090d")
        row1.pack(anchor="w")
        for word, color in [("TRAVELLING", AMBER), (" SALESMAN", TEXT_BRIGHT), (" PROBLEM", GREEN)]:
            tk.Label(row1, text=word, font=("Courier", 16, "bold"),
                     bg="#07090d", fg=color).pack(side=tk.LEFT)

        tk.Label(title_frame,
                 text="  Road Map Navigation · Dijkstra's Algorithm · Shortest Route Finder",
                 font=("Courier", 9), bg="#07090d", fg=TEXT_DIM).pack(anchor="w")

        right = tk.Frame(hdr, bg="#07090d")
        right.pack(side=tk.RIGHT, padx=16)

        self.csv_badge = tk.Label(right, text="  NO FILE LOADED  ",
                                  font=("Courier", 9, "bold"),
                                  bg=alpha_blend(RED, 0.15), fg=RED,
                                  relief="flat", padx=10, pady=5,
                                  bd=1)
        self.csv_badge.pack(side=tk.RIGHT, padx=(6,0))
        self._badge_border(self.csv_badge, RED)

        upload_btn = self._make_btn(right, "⬆  UPLOAD CSV", AMBER, "#07090d",
                                    self._open_csv)
        upload_btn.pack(side=tk.RIGHT)

    def _build_body(self):
        body = tk.Frame(self, bg=BG_DEEP)
        body.pack(fill=tk.BOTH, expand=True)

        # Map
        map_frame = tk.Frame(body, bg=BG_PANEL)
        map_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=(10,4), pady=10)

        self.map_title = tk.Label(map_frame,
                                  text="  ROAD MAP ──  NO FILE LOADED",
                                  font=("Courier", 10, "bold"),
                                  bg=BG_PANEL, fg=AMBER, anchor="w")
        self.map_title.pack(fill=tk.X, padx=4, pady=(4,2))

        sep = tk.Frame(map_frame, bg=alpha_blend(AMBER, 0.25), height=1)
        sep.pack(fill=tk.X, padx=4, pady=(0,6))

        self.canvas = tk.Canvas(map_frame, bg=BG_DEEP,
                                highlightthickness=0, cursor="crosshair")
        self.canvas.pack(fill=tk.BOTH, expand=True)
        self.canvas.bind("<Motion>",   self._on_mouse_move)
        self.canvas.bind("<Leave>",    self._on_mouse_leave)
        self.canvas.bind("<Configure>",self._on_canvas_resize)

        # Sidebar
        self._build_sidebar(body)

    def _build_sidebar(self, parent):
        sb = tk.Frame(parent, bg=BG_PANEL, width=295)
        sb.pack(side=tk.RIGHT, fill=tk.Y, padx=(4,10), pady=10)
        sb.pack_propagate(False)

        # Scrollable content
        sb_canvas = tk.Canvas(sb, bg=BG_PANEL, highlightthickness=0, width=290)
        sb_canvas.pack(fill=tk.BOTH, expand=True)

        sb_inner = tk.Frame(sb_canvas, bg=BG_PANEL)
        sb_canvas.create_window((0,0), window=sb_inner, anchor="nw", width=290)
        sb_inner.bind("<Configure>", lambda e: sb_canvas.configure(
            scrollregion=sb_canvas.bbox("all")))

        # ── ROUTE FINDER CARD ──
        self._make_card(sb_inner, "ROUTE FINDER", AMBER, self._build_finder_card)

        # ── RESULT CARD ──
        self._make_card(sb_inner, "ROUTE ANALYSIS", GREEN, self._build_result_card)

        # ── LEGEND CARD ──
        self._make_card(sb_inner, "LEGEND", TEXT_MID, self._build_legend_card)

    def _make_card(self, parent, title, accent, builder):
        outer = tk.Frame(parent, bg=alpha_blend(accent, 0.08),
                         highlightbackground=alpha_blend(accent, 0.25),
                         highlightthickness=1)
        outer.pack(fill=tk.X, pady=6, padx=4)

        inner = tk.Frame(outer, bg=BG_CARD)
        inner.pack(fill=tk.X, padx=1, pady=1)

        title_row = tk.Frame(inner, bg=BG_CARD)
        title_row.pack(fill=tk.X, padx=12, pady=(10,6))

        bar = tk.Frame(title_row, bg=accent, width=3, height=14)
        bar.pack(side=tk.LEFT, padx=(0,7))

        tk.Label(title_row, text=title,
                 font=("Courier", 9, "bold"),
                 bg=BG_CARD, fg=accent).pack(side=tk.LEFT)

        content = tk.Frame(inner, bg=BG_CARD)
        content.pack(fill=tk.X, padx=12, pady=(0,10))
        builder(content)

    def _build_finder_card(self, parent):
        def dim_label(text):
            tk.Label(parent, text=text, font=("Courier", 8, "bold"),
                     bg=BG_CARD, fg=TEXT_DIM).pack(anchor="w", pady=(6,2))

        dim_label("DEPARTURE")
        self.from_var = tk.StringVar()
        self.from_combo = self._make_combo(parent, self.from_var)

        dim_label("DESTINATION")
        self.to_var = tk.StringVar()
        self.to_combo = self._make_combo(parent, self.to_var)

        dim_label("OPTIMIZE BY")
        self.crit_var = tk.StringVar(value="Distance")
        self._make_combo(parent, self.crit_var,
                         values=["Distance","Time","Fuel"])

        tk.Frame(parent, bg=BG_CARD, height=10).pack()

        self._make_btn(parent, "▶  FIND SHORTEST ROUTE", GREEN, "#050e08",
                       self._run_dijkstra).pack(fill=tk.X, ipady=6, pady=(0,6))

        self._make_btn(parent, "↺  REPLAY ANIMATION", AMBER, "#150d00",
                       self._replay).pack(fill=tk.X, ipady=4)

    def _build_result_card(self, parent):
        self.result_text = tk.Text(parent, font=("Courier", 10),
                                   bg="#081410", fg=alpha_blend(GREEN, 0.9),
                                   insertbackground=GREEN,
                                   state=tk.DISABLED, height=18,
                                   wrap=tk.NONE, relief="flat",
                                   padx=8, pady=8,
                                   highlightthickness=1,
                                   highlightbackground=alpha_blend(GREEN, 0.2))
        self.result_text.pack(fill=tk.X)

        sb = tk.Scrollbar(parent, command=self.result_text.yview,
                          bg=BG_CARD, troughcolor=BG_CARD,
                          activebackground=TEXT_DIM)
        self.result_text.configure(yscrollcommand=sb.set)

        self._set_result("  Upload a CSV file,\n  select nodes, then\n  click FIND SHORTEST ROUTE",
                         color=alpha_blend(GREEN, 0.6))

    def _build_legend_card(self, parent):
        items = [
            (GREEN,   "● City Node"),
            (ROAD_LT, "─ Road"),
            (AMBER,   "─ Shortest Route"),
            (TEAL,    "● Start Node"),
        ]
        frame = tk.Frame(parent, bg=BG_CARD)
        frame.pack(fill=tk.X)
        for i, (color, text) in enumerate(items):
            r, c = divmod(i, 2)
            cell = tk.Frame(frame, bg=BG_CARD)
            cell.grid(row=r, column=c, sticky="w", padx=4, pady=3)
            dot = tk.Frame(cell, bg=color, width=10, height=10)
            dot.pack(side=tk.LEFT, padx=(0,6))
            tk.Label(cell, text=text, font=("Courier", 9),
                     bg=BG_CARD, fg=TEXT_MID).pack(side=tk.LEFT)

    def _build_statusbar(self):
        bar = tk.Frame(self, bg="#07090d", height=28)
        bar.pack(fill=tk.X, side=tk.BOTTOM)
        bar.pack_propagate(False)

        sep = tk.Frame(bar, bg=alpha_blend(AMBER, 0.3), height=1)
        sep.place(x=0, y=0, relwidth=1.0)

        self.status_label = tk.Label(bar,
                                     text="Ready — upload a CSV to begin",
                                     font=("Courier", 9), bg="#07090d", fg=TEXT_DIM)
        self.status_label.pack(side=tk.LEFT, padx=16)

        tk.Label(bar, text="Dijkstra's Algorithm · TSP · Python Tkinter",
                 font=("Courier", 8), bg="#07090d",
                 fg=alpha_blend(TEXT_DIM, 0.5)).pack(side=tk.RIGHT, padx=12)

    # ── Helpers ──
    def _make_btn(self, parent, text, bg, fg, command):
        btn = tk.Button(parent, text=text,
                        font=("Courier", 10, "bold"),
                        bg=bg, fg=fg, activebackground=blend(bg, "#ffffff", 0.15),
                        activeforeground=fg,
                        relief="flat", cursor="hand2",
                        command=command, bd=0, padx=12, pady=4)
        def enter(e): btn.config(bg=blend(bg, "#ffffff", 0.18))
        def leave(e): btn.config(bg=bg)
        btn.bind("<Enter>", enter)
        btn.bind("<Leave>", leave)
        return btn

    def _make_combo(self, parent, var, values=None):
        style = ttk.Style()
        style.theme_use("clam")
        style.configure("Road.TCombobox",
                         fieldbackground=BG_RAISED,
                         background=BG_RAISED,
                         foreground=AMBER,
                         selectbackground=alpha_blend(BLUE, 0.5),
                         selectforeground=WHITE,
                         arrowcolor=AMBER,
                         bordercolor=alpha_blend(AMBER, 0.3),
                         lightcolor=BG_RAISED,
                         darkcolor=BG_RAISED)
        cb = ttk.Combobox(parent, textvariable=var,
                          values=values or ["── upload CSV first ──"],
                          font=("Courier", 11),
                          state="readonly",
                          style="Road.TCombobox")
        cb.pack(fill=tk.X, pady=(0,4))
        return cb

    def _badge_border(self, widget, color):
        widget.config(highlightbackground=color,
                      highlightthickness=1,
                      highlightcolor=color)

    def _set_result(self, text, color=None):
        self.result_text.config(state=tk.NORMAL)
        self.result_text.delete("1.0", tk.END)
        if color:
            self.result_text.config(fg=color)
        self.result_text.insert("1.0", text)
        self.result_text.config(state=tk.DISABLED)

    def _set_status(self, msg):
        self.status_label.config(text=msg)

    # ────────────────────────────────────────────────────────────────
    #  CSV LOADING
    # ────────────────────────────────────────────────────────────────
    def _open_csv(self):
        path = filedialog.askopenfilename(
            title="Select CSV File",
            filetypes=[("CSV files","*.csv"),("All files","*.*")])
        if not path: return
        self._load_csv(path)

    def _load_csv(self, path):
        self.graph.clear()
        self.current_path.clear()
        self._stop_car()

        try:
            rows = 0
            with open(path, newline="", encoding="utf-8-sig") as f:
                reader = csv.reader(f)
                next(reader, None)  # skip header
                for row in reader:
                    if len(row) < 5: continue
                    fr, to = row[0].strip(), row[1].strip()
                    d, t, fuel = float(row[2]), float(row[3]), float(row[4])
                    self.graph.add_edge(fr, to, d, t, fuel)
                    rows += 1
            if rows == 0:
                messagebox.showerror("Error","No valid data rows in CSV.")
                return
        except Exception as ex:
            messagebox.showerror("Error", str(ex))
            return

        self.loaded_file = os.path.basename(path)
        fname = self.loaded_file
        n = len(self.graph.nodes)
        roads = len(self.graph.all_edges)

        # Update badge
        self.csv_badge.config(
            text=f"  {fname}  |  {n} NODES  ",
            bg=alpha_blend(GREEN, 0.1), fg=GREEN)
        self._badge_border(self.csv_badge, GREEN)

        self.map_title.config(text=f"  ROAD MAP ──  {fname.upper()}")

        # Update combos
        names = self.graph.nodes
        self.from_combo["values"] = names
        self.to_combo["values"]   = names
        self.from_var.set(names[0] if names else "")
        self.to_var.set(names[1] if len(names)>1 else names[0] if names else "")

        self._set_result(
            f"  ✔ CSV LOADED\n"
            f"  ─────────────────────────\n"
            f"  File  : {fname}\n"
            f"  Nodes : {n}\n"
            f"  Roads : {roads}\n\n"
            f"  Select DEPARTURE / DESTINATION\n"
            f"  and click FIND SHORTEST ROUTE",
            color=alpha_blend(GREEN, 0.85)
        )
        self._set_status(f"✔  {fname}  —  {n} nodes, {roads} roads")

        # Layout
        W = self.canvas.winfo_width()  or 780
        H = self.canvas.winfo_height() or 560
        self.node_pos = force_layout(self.graph.nodes, self.graph.all_edges, W, H)
        self._draw_map()

    # ────────────────────────────────────────────────────────────────
    #  DIJKSTRA (UI)
    # ────────────────────────────────────────────────────────────────
    def _run_dijkstra(self):
        self._stop_car()
        if not self.graph.nodes:
            self._set_result("  No CSV loaded.\n  Upload a CSV file first.", color=RED)
            return

        frm  = self.from_var.get()
        to   = self.to_var.get()
        crit = self.crit_var.get()

        if not frm or not to or frm == to:
            self._set_result("  Departure and destination\n  must be different nodes.", color=RED)
            return

        result = self.graph.dijkstra(frm, to, crit)
        if result is None:
            self._set_result(f"  No route found:\n  {frm} → {to}", color=RED)
            self.current_path = []
            self._draw_map()
            return

        path, tD, tT, tF = result
        self.current_path = path

        def pad(s, n):
            s = str(s)
            return s[:n] if len(s) >= n else s + " "*(n-len(s))

        out  = "  SHORTEST ROUTE FOUND\n"
        out += "  ╔══════════════════════════════════╗\n"
        out += f"  ║  File      : {pad(self.loaded_file,21)}║\n"
        out += f"  ║  Criterion : {pad(crit,21)}║\n"
        out += "  ╚══════════════════════════════════╝\n\n"
        out += "  ROUTE\n"
        out += "  ┌────────────────────────────────────\n"
        out += f"  │  {'  >>  '.join(path)}\n"
        out += "  └────────────────────────────────────\n\n"
        out += "  ROAD SEGMENTS\n"
        out += "  ════════════════════════════════════\n"
        for i in range(len(path)-1):
            a, b = path[i], path[i+1]
            for e in self.graph.adj.get(a,[]):
                if e.to == b:
                    out += f"  > {a}  >>  {b}\n"
                    out += f"    |-- Distance  : {e.distance:6.1f} km\n"
                    out += f"    |-- Time      : {e.time:6.0f} mins\n"
                    out += f"    +-- Fuel      : {e.fuel:6.1f} L\n"
                    if i < len(path)-2: out += "    ................................\n"
                    break
        out += "  ════════════════════════════════════\n\n"
        out += "  TRIP TOTALS\n"
        out += "  ┌────────────────────────────────────\n"
        out += f"  │  Distance : {tD:.1f} km\n"
        out += f"  │  Time     : {tT:.0f} mins\n"
        out += f"  │  Fuel     : {tF:.1f} L\n"
        out += f"  │  Stops    : {len(path)} cities\n"
        out += "  └────────────────────────────────────\n"

        self._set_result(out, color="#aaffcc")
        self._set_status("Route: " + " >> ".join(path))
        self._draw_map()
        self._start_car()

    # ────────────────────────────────────────────────────────────────
    #  CAR ANIMATION
    # ────────────────────────────────────────────────────────────────
    def _start_car(self):
        self._stop_car()
        if len(self.current_path) < 2: return
        self.car_segment = 0
        self.car_t = 0.0
        self.car_animating = True
        self._car_tick()

    def _stop_car(self):
        if self._car_after_id:
            self.after_cancel(self._car_after_id)
            self._car_after_id = None
        self.car_animating = False
        self.car_t = 0.0
        self.car_segment = 0

    def _car_tick(self):
        self.car_t += self.CAR_SPEED
        if self.car_t >= 1.0:
            self.car_t = 0.0
            self.car_segment += 1
            if self.car_segment >= len(self.current_path)-1:
                self.car_segment = len(self.current_path)-2
                self.car_t = 1.0
                self.car_animating = False
                dest = self.current_path[-1]
                self._set_status(f"🏁  Arrived at destination: {dest}")
                self._draw_map()
                return
        self._draw_map()
        self._car_after_id = self.after(28, self._car_tick)

    def _replay(self):
        if len(self.current_path) >= 2:
            self._start_car()

    def _get_car_pos(self):
        if len(self.current_path) < 2: return None
        seg = min(self.car_segment, len(self.current_path)-2)
        p1 = self.node_pos.get(self.current_path[seg])
        p2 = self.node_pos.get(self.current_path[seg+1])
        if not p1 or not p2: return None
        t = self.car_t
        return (p1[0]+(p2[0]-p1[0])*t, p1[1]+(p2[1]-p1[1])*t)

    def _get_car_angle(self):
        if len(self.current_path) < 2: return 0
        seg = min(self.car_segment, len(self.current_path)-2)
        p1 = self.node_pos.get(self.current_path[seg])
        p2 = self.node_pos.get(self.current_path[seg+1])
        if not p1 or not p2: return 0
        return math.atan2(p2[1]-p1[1], p2[0]-p1[0])

    # ────────────────────────────────────────────────────────────────
    #  CANVAS DRAWING
    # ────────────────────────────────────────────────────────────────
    def _initial_draw(self):
        self._draw_map()

    def _on_canvas_resize(self, event):
        if self.graph.nodes:
            self.node_pos = force_layout(
                self.graph.nodes, self.graph.all_edges,
                event.width, event.height)
        self._draw_map()

    def _draw_map(self):
        c = self.canvas
        c.delete("all")
        W = c.winfo_width()
        H = c.winfo_height()
        if W < 10 or H < 10: return

        self._draw_bg(c, W, H)

        if not self.graph.nodes:
            self._draw_empty(c, W, H)
            return

        self._draw_roads(c)
        self._draw_path_roads(c)
        self._draw_nodes(c)
        self._draw_car(c)

    # ── Background ──
    def _draw_bg(self, c, W, H):
        # Base grass gradient (approximate with rectangle)
        c.create_rectangle(0, 0, W, H, fill="#1a2d10", outline="")

        # Terrain patches
        for (rx, ry, pr, light) in self._terrain:
            px, py = int(rx*W), int(ry*H)
            col = alpha_blend("#50721e", 0.18) if light else alpha_blend("#1c2e10", 0.18)
            c.create_oval(px-pr//2, py-pr//2, px+pr//2, py+pr//2,
                          fill=col, outline="")

        # Horizontal road bands
        road_ys = [int(H*0.17), int(H*0.5), int(H*0.83)]
        road_ws = [42, 54, 40]
        for ry, rw in zip(road_ys, road_ws):
            # Shoulder
            c.create_rectangle(0, ry-rw//2-5, W, ry+rw//2+5,
                                fill=alpha_blend("#5a5f52", 0.55), outline="")
            # Asphalt
            c.create_rectangle(0, ry-rw//2, W, ry+rw//2,
                                fill=alpha_blend("#323638", 0.88), outline="")
            # Center dashes
            dash_x = 0
            while dash_x < W:
                c.create_line(dash_x, ry, min(dash_x+18, W), ry,
                              fill=alpha_blend("#d4b432", 0.45), width=2)
                dash_x += 32
            # Lane lines
            c.create_line(0, ry-rw//2, W, ry-rw//2,
                          fill=alpha_blend("#ffffff", 0.12), width=1)
            c.create_line(0, ry+rw//2, W, ry+rw//2,
                          fill=alpha_blend("#ffffff", 0.12), width=1)

        # Vertical road bands
        road_xs = [int(W*0.2), int(W*0.5), int(W*0.8)]
        road_vws = [38, 50, 36]
        for rx, rw in zip(road_xs, road_vws):
            c.create_rectangle(rx-rw//2-5, 0, rx+rw//2+5, H,
                                fill=alpha_blend("#5a5f52", 0.45), outline="")
            c.create_rectangle(rx-rw//2, 0, rx+rw//2, H,
                                fill=alpha_blend("#323638", 0.8), outline="")
            dash_y = 0
            while dash_y < H:
                c.create_line(rx, dash_y, rx, min(dash_y+18, H),
                              fill=alpha_blend("#d4b432", 0.38), width=2)
                dash_y += 32
            c.create_line(rx-rw//2, 0, rx-rw//2, H,
                          fill=alpha_blend("#ffffff", 0.1), width=1)
            c.create_line(rx+rw//2, 0, rx+rw//2, H,
                          fill=alpha_blend("#ffffff", 0.1), width=1)

        # Vignette (4 dark corner gradients)
        for x0, y0, x1, y1, anchor in [
            (0, 0, W//2, H//2, "nw"),
            (W, 0, W//2, H//2, "ne"),
            (0, H, W//2, H//2, "sw"),
            (W, H, W//2, H//2, "se"),
        ]:
            steps = 12
            for i in range(steps, 0, -1):
                alpha = (1 - i/steps) * 0.85
                gray = int(alpha * 10)
                col = f"#{gray:02x}{gray:02x}{gray:02x}"
                f = i/steps
                c.create_rectangle(
                    x0 + (x1-x0)*(1-f), y0 + (y1-y0)*(1-f),
                    x0 + (x1-x0)*f,     y0 + (y1-y0)*f,
                    fill=col, outline="")

        # Compass rose
        self._draw_compass(c, W-52, H-52, 34)

    def _draw_compass(self, c, cx, cy, r):
        # Outer circle
        c.create_oval(cx-r, cy-r, cx+r, cy+r,
                      outline=alpha_blend(AMBER, 0.4), width=1)
        # N arrow
        c.create_polygon(cx, cy-r+4, cx-5, cy-5, cx+5, cy-5,
                         fill=alpha_blend(AMBER, 0.75), outline="")
        # S arrow
        c.create_polygon(cx, cy+r-4, cx-4, cy+5, cx+4, cy+5,
                         fill=alpha_blend(TEXT_MID, 0.5), outline="")
        # Labels
        c.create_text(cx, cy-r+14, text="N", font=("Courier",8,"bold"),
                      fill=alpha_blend(AMBER, 0.85))
        c.create_text(cx, cy+r-6,  text="S", font=("Courier",8),
                      fill=alpha_blend(TEXT_MID, 0.7))
        c.create_text(cx+r-7, cy,  text="E", font=("Courier",8),
                      fill=alpha_blend(TEXT_MID, 0.7))
        c.create_text(cx-r+7, cy,  text="W", font=("Courier",8),
                      fill=alpha_blend(TEXT_MID, 0.7))
        c.create_oval(cx-3, cy-3, cx+3, cy+3,
                      fill=alpha_blend(AMBER, 0.7), outline="")

    def _draw_roads(self, c):
        path_edges = set()
        for i in range(len(self.current_path)-1):
            path_edges.add((self.current_path[i], self.current_path[i+1]))
            path_edges.add((self.current_path[i+1], self.current_path[i]))

        drawn = set()
        for e in self.graph.all_edges:
            key = (e.frm, e.to) if e.frm < e.to else (e.to, e.frm)
            if key in drawn: continue
            drawn.add(key)
            if (e.frm, e.to) in path_edges: continue
            p1 = self.node_pos.get(e.frm)
            p2 = self.node_pos.get(e.to)
            if not p1 or not p2: continue
            hov = e.frm == self.hovered_node or e.to == self.hovered_node

            x1,y1 = p1; x2,y2 = p2
            # shadow
            c.create_line(x1,y1,x2,y2, fill=alpha_blend("#000000",0.7),
                          width=8 if hov else 7, capstyle=tk.ROUND)
            # surface
            col = "#506070" if hov else "#2e3844"
            c.create_line(x1,y1,x2,y2, fill=col,
                          width=5 if hov else 4, capstyle=tk.ROUND)
            # center dashes
            dash_col = alpha_blend("#c8b040", 0.6 if hov else 0.22)
            c.create_line(x1,y1,x2,y2, fill=dash_col,
                          width=1, dash=(6,7), capstyle=tk.ROUND)

            if hov:
                mx, my = (x1+x2)//2, (y1+y2)//2
                edge_label = f"{e.distance:.0f}km / {e.time:.0f}min / {e.fuel:.1f}L"
                self._draw_tag(c, edge_label, mx, my,
                               alpha_blend("#1e2838", 0.92),
                               alpha_blend(BLUE, 0.55), "#b4d0ff")

    def _draw_path_roads(self, c):
        if len(self.current_path) < 2: return
        for i in range(len(self.current_path)-1):
            p1 = self.node_pos.get(self.current_path[i])
            p2 = self.node_pos.get(self.current_path[i+1])
            if not p1 or not p2: continue
            x1,y1 = p1; x2,y2 = p2

            # glow layers
            c.create_line(x1,y1,x2,y2, fill=alpha_blend(AMBER, 0.07),
                          width=28, capstyle=tk.ROUND)
            c.create_line(x1,y1,x2,y2, fill=alpha_blend(AMBER, 0.16),
                          width=16, capstyle=tk.ROUND)
            # road body
            c.create_line(x1,y1,x2,y2, fill="#5a3800", width=8, capstyle=tk.ROUND)
            c.create_line(x1,y1,x2,y2, fill="#c08010", width=5, capstyle=tk.ROUND)
            # white dashes
            c.create_line(x1,y1,x2,y2, fill=alpha_blend("#ffffff",0.55),
                          width=1, dash=(9,6), capstyle=tk.ROUND)

            # Arrow
            self._draw_arrow(c, x1, y1, x2, y2)

            # Edge label
            for e in self.graph.adj.get(self.current_path[i],[]):
                if e.to == self.current_path[i+1]:
                    mx, my = (x1+x2)//2, (y1+y2)//2
                    label = f"{e.distance:.0f}km / {e.time:.0f}min / {e.fuel:.1f}L"
                    self._draw_tag(c, label, mx, my,
                                   alpha_blend("#1c1200", 0.9),
                                   alpha_blend(AMBER, 0.6), AMBER)
                    break

    def _draw_arrow(self, c, x1, y1, x2, y2):
        dx, dy = x2-x1, y2-y1
        length = math.sqrt(dx*dx+dy*dy)
        if length == 0: return
        ux, uy = dx/length, dy/length
        mx, my = (x1+x2)/2, (y1+y2)/2
        s = 11
        ax = [mx, mx - s*ux + s*0.5*uy, mx - s*ux - s*0.5*uy]
        ay = [my, my - s*uy - s*0.5*ux, my - s*uy + s*0.5*ux]
        c.create_polygon(ax[0],ay[0],ax[1],ay[1],ax[2],ay[2],
                         fill=alpha_blend("#fff090", 0.9), outline="")

    def _draw_tag(self, c, text, mx, my, bg, border, fg):
        font_ = ("Courier", 8, "bold")
        # Estimate text width
        tw = len(text) * 6 + 16
        x0, y0, x1, y1 = mx-tw//2, my-22, mx+tw//2, my-8
        c.create_rectangle(x0, y0, x1, y1, fill=bg, outline=border)
        c.create_text((x0+x1)//2, (y0+y1)//2, text=text,
                      font=font_, fill=fg)

    def _draw_nodes(self, c):
        R = self.NODE_R
        for nd in self.graph.nodes:
            p = self.node_pos.get(nd)
            if not p: continue
            x, y = p
            on_path  = nd in self.current_path
            hov      = nd == self.hovered_node
            is_start = bool(self.current_path) and nd == self.current_path[0]
            is_end   = bool(self.current_path) and nd == self.current_path[-1]

            # Outer glow
            if on_path or hov:
                glow_col = (TEAL if is_start else AMBER if is_end
                            else GREEN if on_path else BLUE)
                c.create_oval(x-R-14, y-R-14, x+R+14, y+R+14,
                              fill=alpha_blend(glow_col, 0.14), outline="")

            # Shadow
            c.create_oval(x-R+3, y-R+4, x+R+3, y+R+4,
                          fill=alpha_blend("#000000", 0.55), outline="")

            # Fill
            fill = ("#005940" if is_start else "#553200" if is_end
                    else "#004020" if on_path else "#12243e" if hov else "#141c2a")
            stroke = (TEAL if is_start else AMBER if is_end
                      else GREEN if on_path else BLUE if hov else "#374a62")
            width = 2.5 if (on_path or hov) else 1.6

            c.create_oval(x-R, y-R, x+R, y+R,
                          fill=fill, outline=stroke, width=width)

            # Inner dashed ring
            if on_path:
                c.create_oval(x-R+6, y-R+6, x+R-6, y+R-6,
                              fill="", outline=alpha_blend(stroke, 0.3),
                              width=1, dash=(3,4))

            # Label
            fs = 7 if len(nd) > 7 else 8 if len(nd) > 5 else 9
            fg = "#c8ffd0" if on_path else "#b4d0ff" if hov else "#a0b4cc"
            c.create_text(x, y, text=nd, font=("Courier", fs, "bold"), fill=fg)

            # START / END badge
            if is_start or is_end:
                badge = "START" if is_start else "END"
                bc = TEAL if is_start else AMBER
                bw = len(badge)*7 + 10
                bx, by = x - bw//2, y - R - 20
                c.create_rectangle(bx, by, bx+bw, by+15,
                                   fill=alpha_blend("#000000", 0.8), outline=bc, width=1)
                c.create_text(x, by+8, text=badge,
                              font=("Courier", 7, "bold"), fill=bc)

    def _draw_car(self, c):
        if len(self.current_path) < 2: return
        pos = self._get_car_pos()
        if not pos: return
        cx, cy = pos
        angle = self._get_car_angle()
        ca, sa = math.cos(angle), math.sin(angle)

        def rot(px, py):
            return (cx + px*ca - py*sa, cy + px*sa + py*ca)

        # Shadow
        shadow_pts = [rot(-14,7), rot(14,7), rot(14,12), rot(-14,12)]
        flat_shadow = [coord for pt in shadow_pts for coord in pt]
        c.create_polygon(flat_shadow, fill=alpha_blend("#000000",0.45), outline="")

        # Headlight beam
        if self.car_animating:
            beam = [rot(14,0), rot(50,-15), rot(50,15)]
            flat_beam = [coord for pt in beam for coord in pt]
            c.create_polygon(flat_beam, fill=alpha_blend("#fff096", 0.25), outline="")

        # Body
        body = [rot(-14,-7), rot(14,-7), rot(14,7), rot(-14,7)]
        flat_body = [coord for pt in body for coord in pt]
        c.create_polygon(flat_body, fill="#cc2a2a", outline="#f05050", width=1)

        # Roof
        roof = [rot(-6,-9), rot(9,-9), rot(9,9), rot(-6,9)]
        flat_roof = [coord for pt in roof for coord in pt]
        c.create_polygon(flat_roof, fill="#aa2222", outline="")

        # Windows
        for wx, wy, ww, wh in [(4,-6,7,5),(-11,-6,7,5),(4,1,7,5),(-11,1,7,5)]:
            pts = [rot(wx,wy), rot(wx+ww,wy), rot(wx+ww,wy+wh), rot(wx,wy+wh)]
            flat = [coord for pt in pts for coord in pt]
            c.create_polygon(flat, fill=alpha_blend("#8cd7ff", 0.85), outline="")

        # Wheels
        for wx, wy in [(-14,-9),(6,-9),(-14,4),(6,4)]:
            pts = [rot(wx,wy),rot(wx+8,wy),rot(wx+8,wy+5),rot(wx,wy+5)]
            flat = [coord for pt in pts for coord in pt]
            c.create_polygon(flat, fill="#1a1a1a", outline="#555", width=1)

        # Lights
        for wx, wy, col in [(12,-6,"#fff5a0"),(12,3,"#fff5a0"),
                             (-16,-6,"#dc3232"),(-16,3,"#dc3232")]:
            pts = [rot(wx,wy),rot(wx+4,wy),rot(wx+4,wy+3),rot(wx,wy+3)]
            flat = [coord for pt in pts for coord in pt]
            c.create_polygon(flat, fill=col, outline="")

    def _draw_empty(self, c, W, H):
        c.create_text(W//2, H//2 - 14,
                      text="No road map loaded",
                      font=("Courier", 16, "bold"),
                      fill=alpha_blend(TEXT_MID, 0.3))
        c.create_text(W//2, H//2 + 18,
                      text="Upload a CSV file to begin",
                      font=("Courier", 11),
                      fill=alpha_blend(TEXT_DIM, 0.4))

    # ────────────────────────────────────────────────────────────────
    #  MOUSE HOVER
    # ────────────────────────────────────────────────────────────────
    def _on_mouse_move(self, event):
        HIT = self.NODE_R + 8
        found = None
        for nd, (nx, ny) in self.node_pos.items():
            if math.hypot(event.x - nx, event.y - ny) < HIT:
                found = nd
                break
        if found != self.hovered_node:
            self.hovered_node = found
            if found:
                conns = len(self.graph.adj.get(found, []))
                self._set_status(f"City: {found}  |  Roads: {conns}")
            if not self.car_animating:
                self._draw_map()

    def _on_mouse_leave(self, event):
        self.hovered_node = None
        if not self.car_animating:
            self._draw_map()


# ══════════════════════════════════════════════════════════════════════
#  ENTRY POINT
# ══════════════════════════════════════════════════════════════════════
def main():
    app = TSPApp()
    app.mainloop()

if __name__ == "__main__":
    main()