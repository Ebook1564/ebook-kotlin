#!/usr/bin/env python3
"""
Generate a pre-built Room v9 library.db from catalog.json.
Run: python3 generate_library_db.py
Output: library.db (copy to app/src/main/assets/library.db)
"""

import json
import sqlite3
import os
import time

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
CATALOG_PATH = os.path.join(SCRIPT_DIR, "..", "app", "src", "main", "assets", "catalog.json")
OUTPUT_PATH = os.path.join(SCRIPT_DIR, "library.db")

# Audiobook IDs from CatalogRepository.kt
AUDIOBOOK_IDS = {
    "19504", "19505", "19506", "19508", "19513", "19514", "19515", "19516", "19517",
    "19572", "19573", "19574", "19575", "19576", "19577", "19578", "19580", "19581",
    "19582", "19583", "19616", "19617", "19618", "19619", "19620", "19621", "19622",
    "19623", "19624", "19625", "19626", "19627", "19631", "19633", "19678", "19679",
    "19682", "19683", "19684", "19685", "19686", "19782", "19783", "19784", "19785",
    "19787", "19788", "19789", "19790", "19791", "19792", "19793", "19794", "19795",
    "19796", "19797", "19836", "19837", "19838", "19839", "19840", "19841", "19842",
    "19843", "19844", "19845", "26458", "26459", "26460", "26461", "26462", "26463",
    "26464", "26465", "26466", "26467", "26468", "26469", "26470", "26471", "26573",
    "26574", "26575", "26576", "26577", "26578", "26579", "26580", "26581", "26582",
    "26583", "26584", "26585", "26586", "26587", "26588", "28731", "28732", "28733",
    "28734", "28737", "28738", "28739", "28753", "28754", "28755", "28756", "28762",
    "28781", "28782", "28792", "28793", "28794", "28795", "28797", "28798", "28799",
    "28801", "28806", "28807", "28808", "28809", "28810", "28811", "28836", "28837",
    "28838", "28839", "28840", "28907", "28908", "28909", "28911", "28912", "28913",
    "28916", "28918", "28919", "28920", "28939", "28940", "28941", "28942", "28943",
    "28944", "28945", "28946", "28947",
}


def create_schema(conn):
    """Create Room v9 schema with all tables and indices."""
    cursor = conn.cursor()

    # Room version tracking table
    cursor.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
    cursor.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (1, 'NOT_USED')")

    # bookmarks table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS bookmarks (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            bookId TEXT NOT NULL,
            chapterIndex INTEGER NOT NULL DEFAULT 0,
            scrollOffset INTEGER NOT NULL DEFAULT 0,
            progress REAL NOT NULL DEFAULT 0.0,
            title TEXT NOT NULL DEFAULT '',
            locator TEXT NOT NULL DEFAULT '',
            createdAt INTEGER NOT NULL DEFAULT 0
        )
    """)

    # highlights table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS highlights (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            bookId TEXT NOT NULL,
            chapterIndex INTEGER NOT NULL DEFAULT 0,
            startOffset INTEGER NOT NULL DEFAULT 0,
            endOffset INTEGER NOT NULL DEFAULT 0,
            selectedText TEXT NOT NULL DEFAULT '',
            color INTEGER NOT NULL DEFAULT 0,
            annotation TEXT,
            locator TEXT NOT NULL DEFAULT '',
            createdAt INTEGER NOT NULL DEFAULT 0
        )
    """)

    # books table with all indices from BookEntity
    cursor.execute("""
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
            last_spine_index INTEGER NOT NULL DEFAULT 0,
            chapter_progression REAL NOT NULL DEFAULT 0.0,
            tts_spine_index INTEGER NOT NULL DEFAULT 0,
            tts_sentence_index INTEGER NOT NULL DEFAULT 0,
            tts_paragraph_index INTEGER NOT NULL DEFAULT 0,
            audio_url TEXT
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_book_title ON books(title)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_book_category ON books(category_code)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_book_reading_status ON books(reading_status)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_book_author ON books(author)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_book_last_opened ON books(last_opened)")

    # categories table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS categories (
            code TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            icon TEXT NOT NULL,
            color TEXT NOT NULL
        )
    """)

    # catalog_meta table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS catalog_meta (
            id INTEGER NOT NULL PRIMARY KEY,
            catalogHash TEXT NOT NULL,
            bookCount INTEGER NOT NULL,
            lastUpdated INTEGER NOT NULL,
            bundledAt INTEGER NOT NULL
        )
    """)

    # book_progress table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS book_progress (
            book_id TEXT NOT NULL PRIMARY KEY,
            last_spine_index INTEGER NOT NULL DEFAULT 0,
            chapter_progression REAL NOT NULL DEFAULT 0.0,
            last_opened INTEGER,
            tts_spine_index INTEGER NOT NULL DEFAULT 0,
            tts_sentence_index INTEGER NOT NULL DEFAULT 0,
            tts_paragraph_index INTEGER NOT NULL DEFAULT 0
        )
    """)

    conn.commit()


def clean_category(raw_subject):
    """Strip Category:/Bookshelf:/Bookshelves: prefix, same as EbookApp.kt."""
    for prefix in ("Category:", "Bookshelf:", "Bookshelves:"):
        if raw_subject.startswith(prefix):
            return raw_subject[len(prefix):].strip()
    return raw_subject


def category_code(raw_cat):
    """Generate category_code same as EbookApp.kt."""
    code = raw_cat.lower()
    code = "".join(c if c.isalnum() or c == "_" else "_" for c in code)
    code = code.strip("_")
    return code


def insert_books(conn, catalog):
    """Insert all catalog entries as BookEntity rows."""
    cursor = conn.cursor()
    seen_ids = set()
    batch = []
    batch_size = 500
    now = int(time.time() * 1000)

    for entry in catalog:
        entry_id = str(entry["id"])
        if entry_id in seen_ids:
            continue
        seen_ids.add(entry_id)

        title = entry["t"]
        author = entry["a"]
        subjects = entry["s"]

        # Pick first subject as category (matching EbookApp logic)
        raw_cat = None
        for s in subjects:
            if s.startswith("Category:"):
                raw_cat = s
                break
        if raw_cat is None:
            raw_cat = subjects[0] if subjects else "Uncategorized"
        clean = clean_category(raw_cat)
        code = category_code(clean)

        subjects_json = json.dumps(subjects, ensure_ascii=False)
        epub_file_name = f"{title}.{entry_id}.epub"
        audio_url = f"http://54.254.205.11:8080/audiobooks/{entry_id}/" if entry_id in AUDIOBOOK_IDS else None

        batch.append((
            entry_id, title, author, "",  # description
            code, "",  # source_folder
            f"{entry_id}_cover_image.jpg",  # cover_image_name
            epub_file_name,
            0,  # is_downloaded
            "NONE",  # reading_status
            now,  # date_added
            None, None, None,  # date_started, last_opened, date_finished
            subjects_json,
            None,  # external_cover_url
            0, 0.0,  # last_spine_index, chapter_progression
            0, 0, 0,  # tts_spine_index, tts_sentence_index, tts_paragraph_index
            audio_url,
        ))

        if len(batch) >= batch_size:
            cursor.executemany("""
                INSERT OR IGNORE INTO books (
                    id, title, author, description, category_code, source_folder,
                    cover_image_name, epub_file_name, is_downloaded, reading_status,
                    date_added, date_started, last_opened, date_finished,
                    subjects, external_cover_url, last_spine_index, chapter_progression,
                    tts_spine_index, tts_sentence_index, tts_paragraph_index, audio_url
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, batch)
            batch.clear()

    # Insert remaining
    if batch:
        cursor.executemany("""
            INSERT OR IGNORE INTO books (
                id, title, author, description, category_code, source_folder,
                cover_image_name, epub_file_name, is_downloaded, reading_status,
                date_added, date_started, last_opened, date_finished,
                subjects, external_cover_url, last_spine_index, chapter_progression,
                tts_spine_index, tts_sentence_index, tts_paragraph_index, audio_url
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, batch)

    conn.commit()
    return len(seen_ids)


def main():
    print(f"Reading catalog from: {CATALOG_PATH}")
    start = time.time()

    with open(CATALOG_PATH, "r", encoding="utf-8") as f:
        catalog = json.load(f)

    print(f"Loaded {len(catalog)} entries in {time.time() - start:.1f}s")

    # Remove old DB if exists
    if os.path.exists(OUTPUT_PATH):
        os.remove(OUTPUT_PATH)

    conn = sqlite3.connect(OUTPUT_PATH)
    print("Creating Room v9 schema...")
    create_schema(conn)

    print("Inserting books...")
    count = insert_books(conn, catalog)
    print(f"Inserted {count} books in {time.time() - start:.1f}s")

    # Vacuum to optimize file size
    print("Vacuuming...")
    conn.execute("VACUUM")
    conn.close()

    size_mb = os.path.getsize(OUTPUT_PATH) / (1024 * 1024)
    print(f"Output: {OUTPUT_PATH} ({size_mb:.1f} MB)")
    print("Done! Copy to app/src/main/assets/library.db")


if __name__ == "__main__":
    main()
