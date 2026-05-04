import os
import json
import re
import csv

# --- CONFIGURATION ---
# Path to your EPUB folder on EC2
SOURCE_DIR = "/var/www/ebooks/epubs" 
# Path to the raw Project Gutenberg CSV
CSV_PATH = "pg_catalog.csv" 
# Final minified JSON for the Android app
OUTPUT_PATH = "catalog.json"

def clean_author(author_raw):
    """Convert 'Last, First, 1800-1900' to 'First Last'."""
    if not author_raw or author_raw == "Unknown": 
        return "Unknown Author"
    
    # Take the first author listed if multiple exist
    primary = author_raw.split(';')[0].split('(')[0].strip()
    
    if ',' in primary:
        parts = primary.split(',')
        if len(parts) >= 2:
            # Remove birth/death years and extra dots/slashes
            first_name = re.sub(r'[\d\-\?\.\/]+', '', parts[1]).strip()
            last_name = parts[0].strip()
            return f"{first_name} {last_name}".strip()
            
    return primary

def clean_categories(subjects_raw, shelves_raw):
    """Normalize subjects and bookshelves into clean app genres."""
    all_raw = (subjects_raw or "") + "; " + (shelves_raw or "")
    items = [item.strip() for item in all_raw.split(';')]
    
    clean = set()
    for item in items:
        # 1. Remove "Category: " prefix
        c = item.replace("Category: ", "")
        
        # 2. Extract primary genre (remove sub-topics after -- or years)
        c = c.split(" -- ")[0].split(" - ")[0].split(" (")[0].strip()
        
        # 3. Filter out meta-tags or noise
        if 3 < len(c) < 25:
            # Map messy Gutenberg names to clean app versions
            MAPPING = {
                "Science Fiction": "Sci-Fi",
                "Historical Novels": "History",
                "Detective Fiction": "Mystery",
                "Humour": "Humor",
                "Children's Literature": "Children",
                "Short Stories": "Fiction"
            }
            clean.add(MAPPING.get(c, c))
            
    return list(clean)

def generate_catalog():
    if not os.path.exists(CSV_PATH):
        print(f"Error: {CSV_PATH} not found. Please place it in the same directory as this script.")
        return

    # 1. Load CSV into memory for O(1) lookup
    print(f"Indexing metadata from {CSV_PATH}...")
    metadata_map = {}
    try:
        with open(CSV_PATH, mode='r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                metadata_map[row['Text#']] = row
    except Exception as e:
        print(f"Failed to read CSV: {e}")
        return

    # 2. Scan local files
    print(f"Scanning EPUBs in {SOURCE_DIR}...")
    # Pattern: Title.ID.epub
    pattern = re.compile(r"^(.*)\.(\d+)\.epub$")
    
    if not os.path.exists(SOURCE_DIR):
        print(f"Error: SOURCE_DIR {SOURCE_DIR} not found.")
        return
        
    files = [f for f in os.listdir(SOURCE_DIR) if f.endswith('.epub')]
    
    catalog = []
    matched_count = 0
    legacy_count = 0
    
    for filename in files:
        match = pattern.match(filename)
        if match:
            raw_title = match.group(1).replace("_", " ").strip()
            book_id = match.group(2)
            
            if book_id in metadata_map:
                row = metadata_map[book_id]
                catalog.append({
                    "id": book_id,
                    "t": row['Title'].replace('\uf03a', ':').split('\n')[0].strip(),
                    "a": clean_author(row['Authors']),
                    "s": clean_categories(row['Subjects'], row['Bookshelves'])
                })
                matched_count += 1
            else:
                # Essential Fallback: Use filename data if CSV doesn't have the ID
                catalog.append({
                    "id": book_id,
                    "t": raw_title,
                    "a": "Unknown Author",
                    "s": ["Legacy"]
                })
                legacy_count += 1

    print(f"\nPROCESSING COMPLETE:")
    print(f"Total Files Found: {len(files)}")
    print(f"Rich Metadata Matched: {matched_count}")
    print(f"Legacy Entries: {legacy_count}")
    print(f"Writing minified JSON to {OUTPUT_PATH}...")
    
    # Save as minified JSON (separators remove spaces) to keep file size down for 78k items
    with open(OUTPUT_PATH, 'w', encoding='utf-8') as f:
        json.dump(catalog, f, separators=(',', ':'))
    
    print("\nDONE! Download 'catalog.json' and place it in app/src/main/assets/")

if __name__ == "__main__":
    generate_catalog()
