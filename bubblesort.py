import time
 
def bubble_sort_descending(arr):
    """
    Sorts an array in DESCENDING order using the bubble sort algorithm.
   
    Args:
        arr: List of comparable elements to sort
       
    Returns:
        Tuple of (sorted list, time taken in seconds)
    """
    start_time = time.time()
    n = len(arr)
   
    # Traverse through all array elements
    for i in range(n):
        # Flag to optimize by detecting if array is already sorted
        swapped = False
       
        # Last i elements are already in place
        for j in range(0, n - i - 1):
            # Swap if the element found is LESS than the next element (for descending)
            if arr[j] < arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
       
        # If no swaps occurred, array is sorted
        if not swapped:
            break
   
    end_time = time.time()
    time_taken = end_time - start_time
   
    return arr, time_taken


def insertion_sort_descending(arr):
    """
    Sorts an array using the insertion sort algorithm.
   
    Args:
        arr: List of comparable elements to sort
       
    Returns:
        Tuple of (sorted list, time taken in seconds)
    """
    start_time = time.time()
    n = len(arr)
    
    for i in range(1, n):
        key = arr[i]
        j = i - 1
        
        # Move elements that are smaller than key to one position ahead
        while j >= 0 and arr[j] < key:
            arr[j + 1] = arr[j]
            j -= 1
        
        arr[j + 1] = key
    
    end_time = time.time()
    time_taken = end_time - start_time
    
    return arr, time_taken


def merge_sort_descending(arr):
    """
    Sorts an array using the merge sort algorithm.
   
    Args:
        arr: List of comparable elements to sort
       
    Returns:
        Tuple of (sorted list, time taken in seconds)
    """
    start_time = time.time()
    
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
        
        # Merge in descending order
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
    end_time = time.time()
    time_taken = end_time - start_time
    
    return sorted_arr, time_taken
 
 
def read_dataset(filename):
    """
    Reads numbers from a file and returns them as a list.
    Each number should be on a separate line or separated by spaces/commas.
   
    Args:
        filename: Path to the file containing numbers
       
    Returns:
        List of numbers read from the file
    """
    try:
        with open(filename, 'r') as file:
            numbers = []
            for line in file:
                line = line.strip()
                if line:  # Skip empty lines
                    try:
                        # Try to convert to int or float
                        if '.' in line:
                            numbers.append(float(line))
                        else:
                            numbers.append(int(line))
                    except ValueError:
                        # If a line contains multiple numbers separated by spaces or commas
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
    print("4. View Sorting History")
    print("5. Exit")
    print("="*50)


def perform_sort(data, choice, history):
    """
    Perform the selected sorting algorithm and display results.
    
    Args:
        data: List of numbers to sort
        choice: Menu option selected by user
        history: List to store sorting history
    """
    sort_name = ""
    
    if choice == 1:
        sort_name = "Bubble Sort"
        sorted_data, time_taken = bubble_sort_descending(data.copy())
    elif choice == 2:
        sort_name = "Insertion Sort"
        sorted_data, time_taken = insertion_sort_descending(data.copy())
    elif choice == 3:
        sort_name = "Merge Sort"
        sorted_data, time_taken = merge_sort_descending(data.copy())
    else:
        return
    
    print(f"\n{sort_name} - SORTING COMPLETE!\n")
    print("Sorted elements (descending order):\n")
    
    # Print every element
    for num in sorted_data:
        print(f"{num}")
    
    print(f"\n{'='*50}")
    print(f"Algorithm: {sort_name}")
    print(f"Time taken: {time_taken:.6f} seconds")
    print(f"Total elements sorted: {len(sorted_data)}")
    
    # Verify
    is_sorted = all(sorted_data[i] >= sorted_data[i+1] for i in range(len(sorted_data)-1))
    print(f"Verification: {'✓ CORRECT!' if is_sorted else '✗ FAILED!'}")
    print("="*50)
    
    # Add to history
    history.append({
        'algorithm': sort_name,
        'time': time_taken,
        'elements': len(sorted_data),
        'status': 'Success' if is_sorted else 'Failed'
    })


def display_history(history):
    """
    Display the sorting history.
    
    Args:
        history: List of sorting history records
    """
    if not history:
        print("\n" + "="*50)
        print("No sorting history yet!")
        print("="*50)
        return
    
    print("\n" + "="*50)
    print("        SORTING HISTORY")
    print("="*50)
    print(f"{'#':<5} {'Algorithm':<20} {'Time (sec)':<15} {'Elements':<10} {'Status'}")
    print("-"*50)
    
    for i, record in enumerate(history, 1):
        print(f"{i:<5} {record['algorithm']:<20} {record['time']:<15.6f} {record['elements']:<10} {record['status']}")
    
    print("="*50)
    
    # Display statistics
    if len(history) > 1:
        print("\nSTATISTICS:")
        fastest = min(history, key=lambda x: x['time'])
        slowest = max(history, key=lambda x: x['time'])
        avg_time = sum(r['time'] for r in history) / len(history)
        
        print(f"Fastest: {fastest['algorithm']} ({fastest['time']:.6f} seconds)")
        print(f"Slowest: {slowest['algorithm']} ({slowest['time']:.6f} seconds)")
        print(f"Average time: {avg_time:.6f} seconds")
        print(f"Total sorts performed: {len(history)}")
        print("="*50)


# Main program
if __name__ == "__main__":
    filename = "dataset.txt"
    print(f"Reading data from '{filename}'...")
   
    data = read_dataset(filename)
   
    if data is not None:
        print(f"Dataset loaded: {len(data)} elements")
        
        # Initialize sorting history
        sorting_history = []
        
        while True:
            display_menu()
            
            try:
                choice = int(input("\nEnter your choice (1-5): "))
                
                if choice == 5:
                    print("\n" + "="*50)
                    print("Thank you for using the sorting program!")
                    print(f"Total sorts performed: {len(sorting_history)}")
                    print("="*50)
                    break
                elif choice == 4:
                    display_history(sorting_history)
                elif choice in [1, 2, 3]:
                    print(f"\nStarting sort...")
                    perform_sort(data, choice, sorting_history)
                else:
                    print("\nInvalid choice! Please select 1-5.")
            except ValueError:
                print("\nInvalid input! Please enter a number.")
            except KeyboardInterrupt:
                print("\n\nProgram interrupted. Exiting...")
                break
    else:
        print("Failed to read data.")