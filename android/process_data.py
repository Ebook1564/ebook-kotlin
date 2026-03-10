import sqlite3
import os
import re
import json
import shutil

# --- CONFIGURATION ---
DATA_DIR = 'law' 
DB_PATH = 'library.db'
ASSETS_PATH = 'app/src/main/assets/library.db'

IGNORE_FILES = {'authors.js', 'tags.js', 'l10n.js', 'tools.js', 'config.js', 'categories.js'}

def process_js_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            match = re.search(r'=\s*(\[.*\]);', content, re.DOTALL)
            if match:
                return json.loads(match.group(1))
    except Exception as e:
        print(f"  [SKIP] {os.path.basename(file_path)}: {str(e)[:60]}")
    return []

def init_db(conn):
    """Initializes the database with exact schema expected by Room."""
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
    
    # SCHEMA MUST MATCH Room EXPECTATION EXACTLY (No DEFAULT values unless specified in Kotlin)
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
            is_downloaded INTEGER NOT NULL,
            reading_status TEXT NOT NULL,
            date_started INTEGER,
            last_opened INTEGER,
            date_finished INTEGER,
            FOREIGN KEY (category_code) REFERENCES categories (code) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
    ''')
    
    # MATCH Room GENERATED INDEX NAMES
    cursor.execute('CREATE INDEX index_books_category_code ON books(category_code)')
    cursor.execute('CREATE INDEX index_books_title ON books(title)')
    cursor.execute('CREATE INDEX index_books_author ON books(author)')
    conn.commit()

def run_seeder():
    if not os.path.exists(DATA_DIR):
        print(f"[ERROR] Directory '{DATA_DIR}' not found.")
        return

    if os.path.exists(DB_PATH): os.remove(DB_PATH)
    conn = sqlite3.connect(DB_PATH)
    init_db(conn)
    cursor = conn.cursor()

    processed_categories = set()
    total_books = 0
    files_processed = 0
    
    js_files = sorted([f for f in os.listdir(DATA_DIR) if f.endswith('.js') and f not in IGNORE_FILES])
    print(f"Found {len(js_files)} metadata files.")

    for js_file in js_files:
        files_processed += 1
        books_data = process_js_file(os.path.join(DATA_DIR, js_file))
        if not books_data: continue

        book_entries = []
        for entry in books_data:
            if not isinstance(entry, list) or len(entry) < 4: continue 

            try:
                raw_title = str(entry[0])
                author = str(entry[1])
                book_id = str(entry[3])
                cat_code = str(entry[4]) if len(entry) > 4 else 'GEN'
                
                if cat_code not in processed_categories:
                    cursor.execute('INSERT OR IGNORE INTO categories VALUES (?, ?, ?, ?)', 
                                 (cat_code, f"Genre {cat_code}", '📖', '#455A64'))
                    processed_categories.add(cat_code)

                if "$b" in raw_title:
                    parts = raw_title.split("$b")
                    display_title = parts[0].strip(" :;,").strip()
                    description = parts[1].strip()
                else:
                    display_title = raw_title
                    description = f"A classic text by {author}."

                cover_name = f"{book_id}_{book_id}-cover.png"
                epub_name = f"{raw_title}.{book_id}.epub"
                unique_id = f"{cat_code}_{book_id}"

                # values for table (is_downloaded=0, reading_status='NONE')
                book_entries.append((
                    unique_id, display_title, author, description, cat_code, 
                    DATA_DIR, cover_name, epub_name, 0, 'NONE', None, None, None
                ))
            except Exception: continue

        if book_entries:
            cursor.executemany('INSERT OR IGNORE INTO books VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)', book_entries)
            conn.commit()
            total_books += len(book_entries)
            
        if files_processed % 500 == 0:
            print(f"  [{files_processed}/{len(js_files)}] Processed {total_books} books...")

    conn.close()
    
    if os.path.exists(DB_PATH):
        os.makedirs(os.path.dirname(ASSETS_PATH), exist_ok=True)
        shutil.move(DB_PATH, ASSETS_PATH)
        print(f"\nSUCCESS! Database deployed to: {ASSETS_PATH}")
    else:
        print("\n[ERROR] Database was not generated.")

if __name__ == "__main__":
    run_seeder()
