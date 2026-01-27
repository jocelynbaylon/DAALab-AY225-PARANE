import time

def bubble_sort_descending(arr):
    """
    Sorts an array in DESCENDING order using the bubble sort algorithm.
    Uses time.perf_counter() for better precision.
    """
    start_time = time.perf_counter()
    n = len(arr)
   
    for i in range(n):
        swapped = False
        for j in range(0, n - i - 1):
            if arr[j] < arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        if not swapped:
            break
   
    end_time = time.perf_counter()
    time_taken = end_time - start_time
   
    return arr, time_taken


def insertion_sort_descending(arr):
    """
    Sorts an array using the insertion sort algorithm.
    Uses time.perf_counter() for better precision.
    """
    start_time = time.perf_counter()
    n = len(arr)
    
    for i in range(1, n):
        key = arr[i]
        j = i - 1
        while j >= 0 and arr[j] < key:
            arr[j + 1] = arr[j]
            j -= 1
        arr[j + 1] = key
    
    end_time = time.perf_counter()
    time_taken = end_time - start_time
    
    return arr, time_taken


def merge_sort_descending(arr):
    """
    Sorts an array using the merge sort algorithm.
    Uses time.perf_counter() for better precision.
    """
    start_time = time.perf_counter()
    
    def merge_sort_helper(array):
        if len(array) <= 1:
            return array
        
        mid = len(array) // 2
        left = merge_sort_helper(array[:mid])
        right = merge_sort_helper(array[mid:])
        
        return merge(left, right)
    
    def merge(left, right):
        result = []
        i = j = 0
        
        while i < len(left) and j < len(right):
            if left[i] >= right[j]:
                result.append(left[i])
                i += 1
            else:
                result.append(right[j])
                j += 1
        
        result.extend(left[i:])
        result.extend(right[j:])
        
        return result
    
    sorted_arr = merge_sort_helper(arr)
    end_time = time.perf_counter()
    time_taken = end_time - start_time
    
    return sorted_arr, time_taken


def benchmark_sort(sort_func, data, iterations=5):
    """
    Benchmark a sorting function with multiple iterations for accuracy.
    
    Args:
        sort_func: Sorting function to benchmark
        data: List of numbers to sort
        iterations: Number of times to run the sort
        
    Returns:
        Tuple of (sorted list, average time, min time, max time)
    """
    times = []
    result = None
    
    for _ in range(iterations):
        data_copy = data.copy()
        sorted_data, time_taken = sort_func(data_copy)
        times.append(time_taken)
        result = sorted_data
    
    avg_time = sum(times) / len(times)
    min_time = min(times)
    max_time = max(times)
    
    return result, avg_time, min_time, max_time


def read_dataset(filename):
    """Reads numbers from a file and returns them as a list."""
    try:
        with open(filename, 'r') as file:
            numbers = []
            for line in file:
                line = line.strip()
                if line:
                    try:
                        if '.' in line:
                            numbers.append(float(line))
                        else:
                            numbers.append(int(line))
                    except ValueError:
                        if ',' in line:
                            nums = [float(x.strip()) if '.' in x.strip() else int(x.strip())
                                   for x in line.split(',') if x.strip()]
                            numbers.extend(nums)
                        else:
                            nums = [float(x.strip()) if '.' in x.strip() else int(x.strip())
                                   for x in line.split() if x.strip()]
                            numbers.extend(nums)
            return numbers
    except FileNotFoundError:
        print(f"Error: File '{filename}' not found.")
        return None
    except Exception as e:
        print(f"Error reading file: {e}")
        return None


def display_menu():
    """Display the sorting algorithm menu."""
    print("\n" + "="*50)
    print("        SORTING ALGORITHMS MENU")
    print("="*50)
    print("1. Bubble Sort (Descending)")
    print("2. Insertion Sort (Descending)")
    print("3. Merge Sort (Descending)")
    print("4. Benchmark All Algorithms")
    print("5. View Sorting History")
    print("6. Exit")
    print("="*50)


def perform_sort(data, choice, history):
    """Perform the selected sorting algorithm with accurate timing."""
    sort_funcs = {
        1: ("Bubble Sort", bubble_sort_descending),
        2: ("Insertion Sort", insertion_sort_descending),
        3: ("Merge Sort", merge_sort_descending)
    }
    
    if choice not in sort_funcs:
        return
    
    sort_name, sort_func = sort_funcs[choice]
    
    # Run with benchmarking for better accuracy
    sorted_data, avg_time, min_time, max_time = benchmark_sort(sort_func, data, iterations=3)
    
    print(f"\n{sort_name} - SORTING COMPLETE!\n")
    print("Sorted elements (descending order):\n")
    
    for num in sorted_data:
        print(f"{num}")
    
    print(f"\n{'='*50}")
    print(f"Algorithm: {sort_name}")
    print(f"Average time: {avg_time:.9f} seconds")
    print(f"Min time: {min_time:.9f} seconds")
    print(f"Max time: {max_time:.9f} seconds")
    print(f"Total elements sorted: {len(sorted_data)}")
    
    is_sorted = all(sorted_data[i] >= sorted_data[i+1] for i in range(len(sorted_data)-1))
    print(f"Verification: {'✓ CORRECT!' if is_sorted else '✗ FAILED!'}")
    print("="*50)
    
    history.append({
        'algorithm': sort_name,
        'avg_time': avg_time,
        'min_time': min_time,
        'max_time': max_time,
        'elements': len(sorted_data),
        'status': 'Success' if is_sorted else 'Failed'
    })


def benchmark_all(data, history):
    """Benchmark all sorting algorithms and compare results."""
    print("\n" + "="*50)
    print("        BENCHMARKING ALL ALGORITHMS")
    print("="*50)
    
    algorithms = [
        ("Bubble Sort", bubble_sort_descending),
        ("Insertion Sort", insertion_sort_descending),
        ("Merge Sort", merge_sort_descending)
    ]
    
    results = []
    
    for name, func in algorithms:
        print(f"\nTesting {name}...")
        sorted_data, avg_time, min_time, max_time = benchmark_sort(func, data, iterations=5)
        
        results.append({
            'name': name,
            'avg_time': avg_time,
            'min_time': min_time,
            'max_time': max_time
        })
        
        history.append({
            'algorithm': name,
            'avg_time': avg_time,
            'min_time': min_time,
            'max_time': max_time,
            'elements': len(data),
            'status': 'Success'
        })
    
    # Sort by average time
    results.sort(key=lambda x: x['avg_time'])
    
    print("\n" + "="*50)
    print("        BENCHMARK RESULTS")
    print("="*50)
    print(f"{'Rank':<6} {'Algorithm':<20} {'Avg Time (sec)':<18} {'Min/Max'}")
    print("-"*70)
    
    for i, r in enumerate(results, 1):
        print(f"{i:<6} {r['name']:<20} {r['avg_time']:<18.9f} {r['min_time']:.9f}/{r['max_time']:.9f}")
    
    print("="*70)
    print(f"Fastest: {results[0]['name']} ({results[0]['avg_time']:.9f} sec)")
    print(f"Dataset size: {len(data)} elements")
    print("="*70)


def display_history(history):
    """Display the sorting history with improved formatting."""
    if not history:
        print("\n" + "="*50)
        print("No sorting history yet!")
        print("="*50)
        return
    
    print("\n" + "="*70)
    print("        SORTING HISTORY")
    print("="*70)
    print(f"{'#':<5} {'Algorithm':<20} {'Avg Time (sec)':<18} {'Elements':<10} {'Status'}")
    print("-"*70)
    
    for i, record in enumerate(history, 1):
        avg = record.get('avg_time', record.get('time', 0))
        print(f"{i:<5} {record['algorithm']:<20} {avg:<18.9f} {record['elements']:<10} {record['status']}")
    
    print("="*70)
    
    if len(history) > 1:
        print("\nSTATISTICS:")
        fastest = min(history, key=lambda x: x.get('avg_time', x.get('time', float('inf'))))
        slowest = max(history, key=lambda x: x.get('avg_time', x.get('time', 0)))
        avg_time = sum(r.get('avg_time', r.get('time', 0)) for r in history) / len(history)
        
        fastest_time = fastest.get('avg_time', fastest.get('time', 0))
        slowest_time = slowest.get('avg_time', slowest.get('time', 0))
        
        print(f"Fastest: {fastest['algorithm']} ({fastest_time:.9f} seconds)")
        print(f"Slowest: {slowest['algorithm']} ({slowest_time:.9f} seconds)")
        print(f"Average time: {avg_time:.9f} seconds")
        print(f"Total sorts performed: {len(history)}")
        print("="*70)


if __name__ == "__main__":
    filename = "dataset.txt"
    print(f"Reading data from '{filename}'...")
   
    data = read_dataset(filename)
   
    if data is not None:
        print(f"Dataset loaded: {len(data)} elements")
        
        sorting_history = []
        
        while True:
            display_menu()
            
            try:
                choice = int(input("\nEnter your choice (1-6): "))
                
                if choice == 6:
                    print("\n" + "="*50)
                    print("Thank you for using the sorting program!")
                    print(f"Total sorts performed: {len(sorting_history)}")
                    print("="*50)
                    break
                elif choice == 5:
                    display_history(sorting_history)
                elif choice == 4:
                    benchmark_all(data, sorting_history)
                elif choice in [1, 2, 3]:
                    print(f"\nStarting sort...")
                    perform_sort(data, choice, sorting_history)
                else:
                    print("\nInvalid choice! Please select 1-6.")
            except ValueError:
                print("\nInvalid input! Please enter a number.")
            except KeyboardInterrupt:
                print("\n\nProgram interrupted. Exiting...")
                break
    else:
        print("Failed to read data.")