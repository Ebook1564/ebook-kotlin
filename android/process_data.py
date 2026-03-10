import sqlite3
import os
import re
import json
import shutil

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
    # SCHEMA MUST MATCH BookEntity.kt EXACTLY
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
    patterns = [
        f"{book_id}_{book_id}-cover.png",
        f"{book_id}_cover.jpg",
        f"{title}_cover.{book_id}",
        f"{book_id}_icover.jpg",
        f"{book_id}_cover_image.jpg"
    ]
    for p in patterns:
        if os.path.exists(os.path.join(folder, p)): return p
    return None

def run_seeder():
    db_path = 'library.db'
    if os.path.exists(db_path): os.remove(db_path)
    
    conn = create_database(db_path)
    cursor = conn.cursor()
    folders = ['law']
    
    for folder in folders:
        js_file = os.path.join(folder, 'full_by_popularity.js')
        if not os.path.exists(js_file): continue
        
        books_data = process_js_file(js_file)
        entries = []
        for i, book in enumerate(books_data):
            raw_title = book[0]
            author = book[1]
            book_id = str(book[3])
            
            if "$b" in raw_title:
                parts = raw_title.split("$b")
                display_title = parts[0].strip(" :;,").strip()
                description = parts[1].strip()
            else:
                display_title = raw_title
                description = f"A classic legal text regarding {display_title}, authored by {author}."

            cover_name = find_best_cover(folder, book_id, display_title)
            epub_name = f"{raw_title}.{book_id}.epub"
            
            cat_code = 'K'
            if "Trial" in display_title or "Court" in display_title: cat_code = 'T'
            elif i % 4 == 0: cat_code = 'P'
            elif i % 3 == 0: cat_code = 'H'
            
            unique_id = f"{folder}_{book_id}"
            entries.append((unique_id, display_title, author, description, cat_code, folder, cover_name, epub_name, 0, 'NONE', None, None, None))
        
        cursor.executemany('''
            INSERT INTO books 
            (id, title, author, description, category_code, source_folder, cover_image_name, epub_file_name, is_downloaded, reading_status, date_started, last_opened, date_finished) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', entries)
        
    conn.commit()
    conn.close()
    
    # MOVE TO ASSETS
    assets_path = 'app/src/main/assets/library.db'
    os.makedirs(os.path.dirname(assets_path), exist_ok=True)
    shutil.move(db_path, assets_path)
    print(f"Success! Generated and moved library.db to {assets_path}")

if __name__ == "__main__":
    run_seeder()
