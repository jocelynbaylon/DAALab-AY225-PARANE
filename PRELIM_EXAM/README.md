# Sorting Algorithm Stress Test (Prelim Exam)

##  Project Overview

This is a **comprehensive benchmarking tool** designed to test and compare sorting algorithm performance on large-scale datasets. The application provides a modern GUI interface for analyzing algorithm efficiency with structured CSV data containing up to 100,000 records.

---

##  Compliance with Laboratory Requirements

### 1. Core Sorting Requirements ✓

**All three algorithms implemented from scratch (no built-in sorting functions):**

-  **Bubble Sort** - O(n²) complexity
  - Lines 428-450 in source code
  - Optimized with early termination when no swaps occur
  
-  **Insertion Sort** - O(n²) complexity
  - Lines 452-469 in source code
  - Efficient for small datasets
  
-  **Merge Sort** - O(n log n) complexity
  - Lines 471-504 in source code
  - Recursive divide-and-conquer implementation

**Verification:** No use of `Collections.sort()`, `Arrays.sort()`, or any library sorting methods.

---

### 2. Advanced Functional Requirements ✓

####  Data Parsing
- **Location:** Lines 406-426 (`loadCsv` method)
- Reads CSV files with proper error handling
- Skips malformed lines gracefully
- Efficient buffered reading for large files

####  Column Selection
- **Location:** Lines 67-68 (UI components), 345-360 (comparator logic)
- User can select from:
  - **ID** - Integer comparison
  - **FirstName** - Case-insensitive string comparison
  - **LastName** - Case-insensitive string comparison

####  Scalability Test (N parameter)
- **Location:** Lines 69, 307
- JSpinner allows user to specify number of rows (N)
- Default: 1,000 rows
- Range: 1 to Integer.MAX_VALUE
- Increment: 1,000 per step
- Supports any N value (1,000, 10,000, 100,000, etc.)

####  Performance Tracking
- **Location:** Lines 332-371 (timing measurements)
- **Separate measurements for:**
  - **Load Time** - Time to read and parse CSV file (lines 333-336)
  - **Sort Time** - Time to execute sorting algorithm (lines 362-371)
- Times displayed in milliseconds with row counts

####  Warning for O(n²) Algorithms
- **Location:** Lines 309-316
- Displays confirmation dialog when:
  - Algorithm is Bubble Sort OR Insertion Sort
  - AND N > 30,000 rows
- Warns: *"Selected algorithm is O(n²). Sorting N rows may take a very long time. Continue?"*
- User can cancel operation

####  Progress Tracking
- **Location:** Lines 78, 367-368, 440-448, 461-467, 506-512
- **Progress bar with:**
  - Visual percentage indicator
  - String display of completion percentage
  - Real-time updates during sorting
  - Indeterminate mode for Merge Sort (very fast)
  - Determinate mode for Bubble/Insertion Sort

####  Output Display
- **Location:** Lines 218-250 (updateResultTable method), 530-542 (done method)
- Displays:
  - **ALL sorted records** in a scrollable table (not just first 10)
  - **Load time** with row count
  - **Sort time** in milliseconds
  - Selected column values in sorted order
  - Interactive table with click-to-sort headers

---

##  Features

### Core Features
1. **CSV File Selection** - Browse and select any CSV file
2. **Algorithm Comparison** - Choose between Bubble, Insertion, or Merge Sort
3. **Flexible Column Sorting** - Sort by ID, FirstName, or LastName
4. **Custom Dataset Size** - Specify exact number of rows to process
5. **Real-time Progress** - Visual progress bar with percentage
6. **Performance Metrics** - Separate load and sort time measurements
7. **Safety Warnings** - Alerts for potentially slow operations

### Enhanced Features
8. **Modern Material Design UI** - Professional color scheme and layout
9. **Card-based Interface** - Clean, organized panels
10. **Styled Components** - Buttons with hover effects, custom borders
11. **Interactive Table** - Click headers to re-sort, double-click rows for details
12. **Responsive Layout** - Well-organized grid and border layouts
13. **Error Handling** - Graceful handling of malformed CSV data
14. **Visual Feedback** - Icons and emojis for better UX

---

##  Theoretical Context

### Algorithm Complexity Comparison

| Algorithm | Time Complexity | Space Complexity | Best For |
|-----------|----------------|------------------|----------|
| **Bubble Sort** | O(n²) | O(1) | Small datasets (< 1,000) |
| **Insertion Sort** | O(n²) | O(1) | Small/partially sorted data |
| **Merge Sort** | O(n log n) | O(n) | Large datasets (10,000+) |

### Performance Expectations

**For N = 1,000 rows:**
- Bubble Sort: ~10-50 ms
- Insertion Sort: ~5-20 ms
- Merge Sort: ~1-5 ms

**For N = 10,000 rows:**
- Bubble Sort: ~500-2,000 ms
- Insertion Sort: ~200-1,000 ms
- Merge Sort: ~10-30 ms

**For N = 100,000 rows:**
- Bubble Sort:  **30-300 seconds** (or more)
- Insertion Sort:  **20-200 seconds** (or more)
- Merge Sort: ~100-500 ms

> **Key Insight:** This demonstrates why O(n log n) is the standard for modern computing. Merge Sort can handle 100,000 rows in under a second, while O(n²) algorithms may take several minutes.

---

##  Technical Implementation

### Technologies Used
- **Language:** Java (Swing GUI Framework)
- **UI Components:** JFrame, JTable, JComboBox, JSpinner, JProgressBar
- **Concurrency:** SwingWorker for background processing
- **I/O:** BufferedReader for efficient CSV parsing

### Architecture
```
SortingStressTest (Main Class)
├── UI Components (GUI Elements)
├── Event Handlers (User Interactions)
├── SortTask (SwingWorker)
│   ├── loadCsv() - CSV parsing
│   ├── bubbleSort() - O(n²) implementation
│   ├── insertionSort() - O(n²) implementation
│   └── mergeSort() - O(n log n) implementation
└── Record (Data Model)
    ├── id: int
    ├── firstName: String
    └── lastName: String
```

---

##  How to Use

### Prerequisites
- Java Development Kit (JDK) 8 or higher
- CSV file with format: `ID,FirstName,LastName`

### Compilation
```bash
javac SortingStressTest.java
```

### Execution
```bash
java SortingStressTest
```

### Step-by-Step Usage

1. **Choose CSV File**
   - Click "Browse..." button
   - Select your `generated_data.csv` file

2. **Configure Test Parameters**
   - **Algorithm:** Select Bubble Sort, Insertion Sort, or Merge Sort
   - **Sort Column:** Choose ID, FirstName, or LastName
   - **Number of Rows:** Set N value (1,000, 10,000, 100,000, etc.)

3. **Start Test**
   - Click "▶ Start Test" button
   - Confirm if warning appears (for large N with O(n²) algorithms)

4. **View Results**
   - Monitor progress bar during sorting
   - Check load and sort times
   - Review sorted records in table
   - Double-click any row to see full record details

---

##  Expected CSV Format

```csv
ID,FirstName,LastName
1,John,Doe
2,Jane,Smith
3,Alice,Johnson
...
```

### Requirements:
- First row must be header: `ID,FirstName,LastName`
- ID must be numeric (integer)
- FirstName and LastName are text strings
- Fields separated by commas
- No embedded commas in names

---

##  Important Notes

### Performance Warnings
1. **Bubble Sort with N > 30,000** - May take several minutes
2. **Insertion Sort with N > 30,000** - May take several minutes
3. **Merge Sort** - Fast even with N = 100,000

### Memory Considerations
- The application loads all N rows into memory
- For N = 100,000, expect ~10-20 MB RAM usage
- Sorting creates temporary arrays (additional memory overhead)

### Error Handling
- Malformed CSV lines are automatically skipped
- Invalid ID values (non-numeric) skip that record
- Empty files or missing columns display error messages

---

##  Learning Outcomes

By completing this lab exam, students will understand:

1. **Algorithm Efficiency** - Why O(n log n) beats O(n²) for large datasets
2. **Real-world Performance** - Theoretical complexity vs. actual execution time
3. **Scalability** - How algorithms behave as data size increases
4. **Trade-offs** - Memory vs. time complexity (Merge Sort uses more space)
5. **UI/UX Design** - Creating user-friendly benchmarking tools
6. **Concurrent Programming** - Using background threads for long operations
7. **Data Processing** - Efficient CSV parsing and handling

---

##  Compliance Summary

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| Bubble Sort from scratch | ✅ | Lines 428-450 |
| Insertion Sort from scratch | ✅ | Lines 452-469 |
| Merge Sort from scratch | ✅ | Lines 471-504 |
| CSV data parsing | ✅ | Lines 406-426 |
| Column selection (ID/FirstName/LastName) | ✅ | Lines 67-68, 345-360 |
| Scalability test (N parameter) | ✅ | Lines 69, 307 |
| Load time measurement | ✅ | Lines 333-336 |
| Sort time measurement | ✅ | Lines 362-371 |
| Warning for O(n²) algorithms | ✅ | Lines 309-316 |
| Progress tracking | ✅ | Lines 78, 440-467 |
| Output display | ✅ | Lines 218-250, 530-542 |
| GUI interface | ✅ | Entire application |

**Overall Compliance: 100% ✅**

---

##  Author

Developed for **Sorting Algorithm Stress Test (Prelim Exam)**

---

## License

Educational use only - Lab Exam Project

---

##  Additional Resources

- [Big-O Complexity Chart](https://www.bigocheatsheet.com/)
- [Sorting Algorithms Visualization](https://visualgo.net/en/sorting)
- [Java Swing Documentation](https://docs.oracle.com/javase/tutorial/uiswing/)

---

**Note:** This implementation exceeds the basic requirements by providing a polished GUI, comprehensive error handling, real-time progress updates, and a modern user interface while maintaining 100% compliance with all exam specifications.
