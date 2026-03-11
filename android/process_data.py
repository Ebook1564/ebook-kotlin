import sqlite3
import os
import re
import json
import shutil

# --- CONFIGURATION ---
SEARCH_DIRS = ['all_extracted', 'law']
DB_PATH = 'library.db'
ASSETS_PATH = 'app/src/main/assets/library.db'

# Genre Mapping (LCC Codes)
GENRE_MAP = {
    "PR": "English Literature",
    "PS": "American Literature",
    "PZ": "Fiction & Juveniles",
    "K": "Law & Legal",
    "E": "History of America",
    "F": "History of America",
    "D": "World History",
    "PA": "Classical Languages",
    "PQ": "French/Spanish Literature",
    "PT": "German Literature",
    "BS": "The Bible",
    "QA": "Mathematics",
    "ML": "Music",
    "GV": "Recreation & Leisure",
    "NA": "Fine Arts",
    "QC": "Physics"
}

def process_js_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            # Match var json_data = [...]; OR var something = [...];
            match = re.search(r'=\s*(\[.*\]);', content, re.DOTALL)
            if match:
                return json.loads(match.group(1))
    except Exception as e:
        print(f"  [SKIP] {os.path.basename(file_path)}")
    return []

def init_db(conn):
    cursor = conn.cursor()
    cursor.execute('DROP TABLE IF EXISTS books')
    cursor.execute('DROP TABLE IF EXISTS categories')
    
    cursor.execute('''
        CREATE TABLE categories (
            code TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            icon TEXT NOT NULL,
            color TEXT NOT NULL
        )
    ''')
    
    cursor.execute('''
        CREATE TABLE books (
            id TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            author TEXT NOT NULL,
            description TEXT,
            category_code TEXT NOT NULL,
            source_folder TEXT NOT NULL,
            cover_image_name TEXT,
            epub_file_name TEXT,
            is_downloaded INTEGER NOT NULL DEFAULT 0,
            reading_status TEXT NOT NULL DEFAULT 'NONE',
            date_started INTEGER,
            last_opened INTEGER,
            date_finished INTEGER,
            FOREIGN KEY (category_code) REFERENCES categories (code)
        )
    ''')
    
    cursor.execute('CREATE INDEX idx_book_category ON books(category_code)')
    cursor.execute('CREATE INDEX idx_book_title ON books(title)')
    conn.commit()

def run_seeder():
    if os.path.exists(DB_PATH): os.remove(DB_PATH)
    conn = sqlite3.connect(DB_PATH)
    init_db(conn)
    cursor = conn.cursor()

    processed_book_ids = set()
    total_books = 0
    
    for data_dir in SEARCH_DIRS:
        if not os.path.exists(data_dir): continue
        
        print(f"Scanning {data_dir}...")
        js_files = [f for f in os.listdir(data_dir) if f.endswith('.js') and 'by_popularity' in f]
        
        for js_file in js_files:
            books_data = process_js_file(os.path.join(data_dir, js_file))
            if not books_data: continue

            book_entries = []
            for entry in books_data:
                if not isinstance(entry, list) or len(entry) < 4: continue 

                try:
                    raw_title = str(entry[0])
                    author = str(entry[1])
                    book_id = str(entry[3])
                    cat_code = str(entry[4]) if len(entry) > 4 else 'GEN'
                    
                    if book_id in processed_book_ids: continue

                    # Category setup
                    genre_name = GENRE_MAP.get(cat_code, f"Genre {cat_code}")
                    cursor.execute('INSERT OR IGNORE INTO categories VALUES (?, ?, ?, ?)', 
                                 (cat_code, genre_name, '📖', '#455A64'))

                    # Title/Desc extraction
                    if "$b" in raw_title:
                        parts = raw_title.split("$b")
                        display_title = parts[0].strip(" :;,").strip()
                        description = parts[1].strip()
                    else:
                        display_title = raw_title
                        description = f"A classic text by {author} in the field of {genre_name}."

                    cover_name = f"{book_id}_{book_id}-cover.png"
                    epub_name = f"{raw_title}.{book_id}.epub"

                    book_entries.append((
                        book_id, display_title, author, description, cat_code, 
                        data_dir, cover_name, epub_name, 0, 'NONE'
                    ))
                    processed_book_ids.add(book_id)
                except Exception: continue

            if book_entries:
                cursor.executemany('INSERT OR IGNORE INTO books (id, title, author, description, category_code, source_folder, cover_image_name, epub_file_name, is_downloaded, reading_status) VALUES (?,?,?,?,?,?,?,?,?,?)', book_entries)
                conn.commit()
                total_books += len(book_entries)

    conn.close()
    
    if os.path.exists(DB_PATH):
        os.makedirs(os.path.dirname(ASSETS_PATH), exist_ok=True)
        shutil.copy(DB_PATH, ASSETS_PATH)
        print(f"\nSUCCESS! Processed {total_books} unique books.")
        print(f"Database deployed to: {ASSETS_PATH}")

if __name__ == "__main__":
    run_seeder()
