import sqlite3
import os
import re
import json
import shutil

# --- CONFIGURATION ---
SEARCH_DIRS = ['all_extracted', 'law', 'E:/WSL/all_extracted']
DB_PATH = 'library.db'
ASSETS_PATH = 'app/src/main/assets/library.db'

# COMPREHENSIVE LCC GENRE MAP (Updated for User Request)
GENRE_MAP = {
    "A": "General Works",
    "B": "Philosophy & Religion",
    "BC": "Logic",
    "BF": "Psychology",
    "BH": "Aesthetics",
    "BJ": "Ethics",
    "BL": "Religions",
    "BM": "Judaism",
    "BP": "Islam",
    "BS": "The Bible",
    "BT": "Theology",
    "BV": "Practical Theology",
    "BX": "Christianity",
    "C": "Historical Sciences",
    "D": "World History",
    "DA": "British History",
    "DC": "French History",
    "DE": "Greco-Roman History",
    "DK": "Russian History",
    "DS": "Asian History",
    "DT": "African History",
    "E": "American History",
    "F": "History of the Americas", # MERGED
    "G": "History of the Americas", # MERGED (User stated F & G are history of americas)
    "GV": "Recreation & Sports",
    "H": "Social Sciences",
    "HB": "Economics",
    "HQ": "Family & Gender",
    "J": "Political Science",
    "K": "Law & Legal",
    "KF": "American Law",
    "L": "Education",
    "M": "Music",
    "ML": "Music Literature",
    "N": "Fine Arts",
    "NA": "Architecture",
    "ND": "Painting",
    "P": "Language & Literature",
    "PA": "Classical Literature",
    "PB": "Modern European Languages",
    "PC": "Romance Languages",
    "PD": "Germanic Languages",
    "PE": "English Language",
    "PF": "West Germanic",
    "PG": "Slavic Literature",
    "PH": "Finno-Ugrian Literature",
    "PJ": "Oriental Literature",
    "PK": "Indo-Iranian Literature",
    "PL": "East Asian Literature",
    "PM": "Hyperborean Literature",
    "PN": "Poetry & Drama",
    "PQ": "Romance Literature",
    "PR": "English Literature",
    "PS": "American Literature",
    "PT": "Germanic Literature",
    "PZ": "Juvenile Fiction",
    "Q": "Science",
    "QA": "Mathematics",
    "QB": "Astronomy",
    "QC": "Physics",
    "QD": "Chemistry",
    "QE": "Geology",
    "QH": "Natural History",
    "QK": "Botany",
    "QL": "Zoology",
    "QM": "Human Anatomy",
    "QP": "Physiology",
    "QR": "Microbiology",
    "R": "Medicine",
    "S": "Agriculture",
    "T": "Technology",
    "TX": "Home Economics",
    "U": "Military Science",
    "V": "Naval Science",
    "Z": "Library Science",
    "GEN": "General Collection",
    "NONE": "General Collection"
}

def process_js_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            match = re.search(r'=\s*(\[.*\]);', content, re.DOTALL)
            if match: return json.loads(match.group(1))
    except Exception: pass
    return []

def init_db(conn):
    cursor = conn.cursor()
    cursor.execute('CREATE TABLE IF NOT EXISTS categories (code TEXT PRIMARY KEY, name TEXT, icon TEXT, color TEXT)')
    
    cursor.execute("PRAGMA table_info(books)")
    columns = {row[1]: {"notnull": row[3]} for row in cursor.fetchall()}
    
    if columns and 'external_cover_url' not in columns:
        print("Migrating schema...")
        cursor.execute("ALTER TABLE books RENAME TO books_old")
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
                date_added INTEGER,
                date_started INTEGER,
                last_opened INTEGER,
                date_finished INTEGER,
                subjects TEXT NOT NULL DEFAULT '[]',
                external_cover_url TEXT,
                FOREIGN KEY (category_code) REFERENCES categories (code) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
        ''')
        cursor.execute("PRAGMA table_info(books_old)")
        old_cols = [row[1] for row in cursor.fetchall()]
        common = [c for c in old_cols if c in ['id', 'title', 'author', 'description', 'category_code', 'source_folder', 'cover_image_name', 'epub_file_name', 'is_downloaded', 'reading_status', 'date_added', 'date_started', 'last_opened', 'date_finished', 'subjects']]
        col_str = ", ".join(common)
        cursor.execute(f"INSERT INTO books ({col_str}) SELECT {col_str} FROM books_old")
        cursor.execute("DROP TABLE books_old")

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS books (
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
            date_added INTEGER,
            date_started INTEGER,
            last_opened INTEGER,
            date_finished INTEGER,
            subjects TEXT NOT NULL DEFAULT '[]',
            external_cover_url TEXT,
            FOREIGN KEY (category_code) REFERENCES categories (code) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_book_category ON books(category_code)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_book_title ON books(title)')
    cursor.execute('CREATE TABLE IF NOT EXISTS sync_tracker (filename TEXT PRIMARY KEY, last_modified REAL)')
    conn.commit()

def run_seeder():
    conn = sqlite3.connect(DB_PATH)
    init_db(conn)
    cursor = conn.cursor()
    
    cursor.execute("SELECT filename, last_modified FROM sync_tracker")
    synced = {row[0]: row[1] for row in cursor.fetchall()}
    
    for data_dir in SEARCH_DIRS:
        if not os.path.exists(data_dir): continue
        print(f"Syncing {data_dir}...")
        try: files = [f for f in os.listdir(data_dir) if f.endswith('.js') and 'by_popularity' in f]
        except Exception: continue
        
        for js_file in files:
            file_path = os.path.join(data_dir, js_file)
            mtime = os.path.getmtime(file_path)
            if js_file in synced and synced[js_file] == mtime: continue

            books_data = process_js_file(file_path)
            if not books_data: continue

            book_entries = []
            for entry in books_data:
                if not isinstance(entry, list) or len(entry) < 4: continue 
                try:
                    raw_title, author, _, book_id = str(entry[0]), str(entry[1]), str(entry[2]), str(entry[3])
                    cat_code = str(entry[4]) if len(entry) > 4 else 'GEN'
                    if not cat_code or cat_code == 'None': cat_code = 'GEN'
                    
                    genre = GENRE_MAP.get(cat_code, f"Genre {cat_code}")
                    cursor.execute('INSERT OR REPLACE INTO categories VALUES (?, ?, ?, ?)', (cat_code, genre, '📖', '#455A64'))
                    
                    display_title = raw_title.split("$b")[0].strip(" :;,").strip()
                    book_entries.append((book_id, display_title, author, f"A classic text by {author}.", cat_code, os.path.basename(data_dir), f"{book_id}_{book_id}-cover.png", f"{raw_title}.{book_id}.epub", 0, 'NONE', '[]', None))
                except Exception: continue

            if book_entries:
                cursor.executemany('INSERT OR IGNORE INTO books (id, title, author, description, category_code, source_folder, cover_image_name, epub_file_name, is_downloaded, reading_status, subjects, external_cover_url) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)', book_entries)
            cursor.execute("INSERT OR REPLACE INTO sync_tracker VALUES (?, ?)", (js_file, mtime))
            
    # Final check: Map any generic "Genre X" to LCC if possible
    for code, name in GENRE_MAP.items():
        cursor.execute("UPDATE categories SET name = ? WHERE code = ?", (name, code))
        
    # User Request: Clean duplicates and ensure history mapping
    cursor.execute("UPDATE categories SET name = 'History of the Americas' WHERE code IN ('E', 'F', 'G')")
        
    conn.commit()
    conn.close()
    shutil.copy(DB_PATH, ASSETS_PATH)
    print(f"Sync complete.")

if __name__ == "__main__":
    run_seeder()
