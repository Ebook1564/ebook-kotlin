import sqlite3
import os
import re
import json

def process_js_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
        match = re.search(r'var\s+\w+\s*=\s*(\[.*\]);', content, re.DOTALL)
        if match:
            return json.loads(match.group(1))
    return []

def create_database(output_path):
    conn = sqlite3.connect(output_path)
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
            category_code TEXT NOT NULL,
            source_folder TEXT NOT NULL,
            cover_image_name TEXT,
            epub_file_name TEXT,
            is_downloaded INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY (category_code) REFERENCES categories (code) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
    ''')
    cursor.executemany('INSERT INTO categories (code, name, icon, color) VALUES (?, ?, ?, ?)', [
        ('K', 'Legal Classics', '⚖️', '#2196F3'),
        ('H', 'Historical Law', '📜', '#FF9800'),
        ('T', 'Trials', '🔨', '#F44336'),
        ('P', 'Popular', '⭐', '#4CAF50')
    ])
    conn.commit()
    return conn

def find_best_cover(folder, book_id, title):
    # Potential patterns in order of preference
    patterns = [
        f"{book_id}_{book_id}-cover.png",
        f"{book_id}_cover.jpg",
        f"{book_id}_cover.png",
        f"{title}_cover.{book_id}",
        f"{book_id}_icover.jpg",
        f"{book_id}_fcover.jpg"
    ]
    
    for p in patterns:
        if os.path.exists(os.path.join(folder, p)):
            return p
            
    # If no exact match, do a fuzzy search for the ID
    try:
        files = os.listdir(folder)
        for f in files:
            if f.startswith(f"{book_id}_") and ("cover" in f.lower()):
                return f
    except:
        pass
        
    return None

def run_seeder():
    db_path = 'library.db'
    if os.path.exists(db_path):
        os.remove(db_path)
    
    conn = create_database(db_path)
    cursor = conn.cursor()
    folders = ['law']
    
    for folder in folders:
        js_file = os.path.join(folder, 'full_by_popularity.js')
        if not os.path.exists(js_file): continue
        
        books_data = process_js_file(js_file)
        entries = []
        for i, book in enumerate(books_data):
            title = book[0]
            author = book[1]
            book_id = str(book[3])
            
            # Use intelligent detection
            cover_name = find_best_cover(folder, book_id, title)
            epub_name = f"{title}.{book_id}.epub"
            
            # Distribute books across sections
            cat_code = 'K'
            if "Trial" in title or "Court" in title: cat_code = 'T'
            elif i % 4 == 0: cat_code = 'P'
            elif i % 3 == 0: cat_code = 'H'
            
            unique_id = f"{folder}_{book_id}"
            entries.append((unique_id, title, author, cat_code, folder, cover_name, epub_name, 0))
        
        cursor.executemany('''
            INSERT INTO books 
            (id, title, author, category_code, source_folder, cover_image_name, epub_file_name, is_downloaded) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ''', entries)
        
    conn.commit()
    conn.close()
    print(f"Success! Generated {db_path} with intelligent cover detection.")

if __name__ == "__main__":
    run_seeder()
