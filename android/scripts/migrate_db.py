#!/usr/bin/env python3
"""
EPUB Library Database Migration Script
Converts pg_catalog.csv to library.db matching Room Entity schema.
"""

import sqlite3
import csv
import re
import os
from datetime import datetime

# Configuration
PG_CATALOG_PATH = "/mnt/d/Work - Aayush/misc/Downloads/pg_catalog.csv"
OUTPUT_DB_PATH = "/var/www/ebooks/library.db"
BUNDLED_DB_PATH = "/mnt/d/Work - Aayush/Ebook Reader Flutter/VC/ebook-kotlin/android/app/src/main/assets/library.db"

# For local testing without overwriting bundled DB
LOCAL_TEST_DB = "/mnt/d/Work - Aayush/Ebook Reader Flutter/VC/ebook-kotlin/android/scripts/library.db"


def log(msg):
    print(f"[{datetime.now().strftime('%H:%M:%S')}] {msg}")


def log_progress(current, total, prefix=""):
    pct = (current / total) * 100
    print(f"\r{prefix} {current:,}/{total:,} ({pct:.1f}%)", end="", flush=True)


def parse_authors(author_str):
    """Parse author string into list of (name, birth_year, death_year)"""
    if not author_str:
        return []

    authors = []
    # Split by semicolon for multiple authors
    for author in author_str.split(";"):
        author = author.strip()
        if not author:
            continue

        # Try to extract years: "Name, 1743-1826"
        birth_year = None
        death_year = None

        year_match = re.search(r"(\d{4})\s*[-–]\s*(\d{4})", author)
        if year_match:
            birth_year = int(year_match.group(1))
            death_year = int(year_match.group(2))
            # Remove years from name
            author = re.sub(r"\s*[,.\s]*\d{4}\s*[-–]\s*\d{4}", "", author).strip()

        # Clean up name
        author = author.strip().strip(",")

        if author:
            authors.append((author, birth_year, death_year))

    return authors


def parse_date(date_str):
    """Parse YYYY-MM-DD string to Unix timestamp"""
    if not date_str:
        return None
    try:
        dt = datetime.strptime(date_str.strip(), "%Y-%m-%d")
        return int(dt.timestamp())
    except:
        return None


def parse_languages(lang_str):
    """Parse language string to list of ISO codes"""
    if not lang_str:
        return []

    codes = []
    for code in lang_str.split(";"):
        code = code.strip().lower()
        if code:
            codes.append(code)
    return codes if codes else []


def parse_multi_value(mv_str):
    """Parse semicolon-separated values"""
    if not mv_str:
        return []
    return [v.strip() for v in mv_str.split(";") if v.strip()]


def create_schema(conn):
    """Create the new database schema"""
    log("Creating database schema...")

    cursor = conn.cursor()

    # Categories table - schema must match Room Entity exactly
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS categories (
            code TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            icon TEXT NOT NULL,
            color TEXT NOT NULL
        )
    """)

    # Books table - schema must match Room Entity exactly
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
            FOREIGN KEY (category_code) REFERENCES categories(code)
        )
    """)

    # User data tables (empty, will be created fresh)
    # Schema matches Room Entity expectations exactly
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS bookmarks (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            bookId TEXT NOT NULL,
            locator TEXT NOT NULL,
            createdAt INTEGER NOT NULL
        )
    """)

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS highlights (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            bookId TEXT NOT NULL,
            locator TEXT NOT NULL,
            color INTEGER NOT NULL,
            annotation TEXT,
            createdAt INTEGER NOT NULL
        )
    """)

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS catalog_meta (
            id INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
            catalogHash TEXT NOT NULL,
            bookCount INTEGER NOT NULL,
            lastUpdated INTEGER NOT NULL,
            bundledAt INTEGER NOT NULL
        )
    """)

    # Create indexes - must match Room Entity indices exactly
    log("Creating indexes...")
    indexes = [
        "CREATE INDEX IF NOT EXISTS idx_book_title ON books(title)",
        "CREATE INDEX IF NOT EXISTS idx_book_category ON books(category_code)",
    ]

    for idx in indexes:
        cursor.execute(idx)

    conn.commit()
    log("Schema created successfully")


def import_pg_catalog(conn):
    """Import data from pg_catalog.csv"""
    log(f"Reading {PG_CATALOG_PATH}...")

    books_count = 0
    errors = 0

    with open(PG_CATALOG_PATH, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        total_rows = sum(1 for _ in open(PG_CATALOG_PATH, "r", encoding="utf-8")) - 1

        cursor = conn.cursor()

        for row_num, row in enumerate(reader, 1):
            try:
                log_progress(row_num, total_rows, "Importing books:")

                book_id = row.get("Text#", "").strip()
                if not book_id:
                    errors += 1
                    continue

                title = row.get("Title", "").strip()
                if not title:
                    errors += 1
                    continue

                # Parse fields
                author_str = row.get("Authors", "")
                bookshelves_str = row.get("Bookshelves", "")

                # Generate epub filename - use original title format like EC2
                # Format: Title.BookId.epub (e.g., "Pride and Prejudice.1342.epub")
                # Only remove path traversal chars, keep spaces/special chars
                safe_title = title.replace("\\", "").replace("/", "")
                epub_filename = f"{safe_title}.{book_id}.epub"

                # Parse multi-value fields
                bookshelves_list = parse_multi_value(bookshelves_str)

                # Determine category (first bookshelf)
                category_code = "uncategorized"
                category_name = "Uncategorized"
                if bookshelves_list:
                    category_name = bookshelves_list[0]
                    # Create safe category code
                    category_code = re.sub(r"[^\w]", "_", category_name.lower())[:50]

                # Parse authors and get first author name
                parsed_authors = parse_authors(author_str)
                author_name = (
                    parsed_authors[0][0] if parsed_authors else "Unknown Author"
                )

                # Insert book
                cursor.execute(
                    """
                    INSERT OR REPLACE INTO books (
                        id, title, author, description, category_code,
                        source_folder, epub_file_name, is_downloaded, reading_status,
                        subjects, external_cover_url
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                    (
                        book_id,
                        title,
                        author_name,
                        None,  # No description in pg_catalog
                        category_code,
                        "",  # Empty source folder
                        epub_filename,
                        0,  # Not downloaded by default
                        "NONE",  # No reading status
                        "[]",  # Empty subjects
                        None,  # No external cover URL
                    ),
                )

                # Handle first bookshelf as category
                if category_code and category_name:
                    try:
                        cursor.execute(
                            """
                            INSERT OR IGNORE INTO categories (code, name, icon, color)
                            VALUES (?, ?, ?, ?)
                        """,
                            (category_code, category_name, "", ""),
                        )
                    except:
                        pass

                books_count += 1

                # Commit every 1000 rows
                if books_count % 1000 == 0:
                    conn.commit()

            except Exception as e:
                errors += 1
                if errors <= 5:
                    print(f"\nError on row {row_num}: {e}")

        conn.commit()

    print()  # New line after progress
    log(f"Imported {books_count:,} books with {errors:,} errors")

    return books_count


def add_catalog_meta(conn, book_count):
    """Add catalog metadata"""
    cursor = conn.cursor()
    now = int(datetime.now().timestamp())

    cursor.execute(
        """
        INSERT INTO catalog_meta (id, catalogHash, bookCount, lastUpdated, bundledAt)
        VALUES (?, ?, ?, ?, ?)
    """,
        (1, "migrated_v1", book_count, now, now),
    )

    conn.commit()


def analyze_and_report(conn):
    """Analyze database and print report"""
    cursor = conn.cursor()

    print("\n" + "=" * 60)
    print("MIGRATION REPORT")
    print("=" * 60)

    # Book counts
    cursor.execute("SELECT COUNT(*) FROM books")
    total_books = cursor.fetchone()[0]
    print(f"\nTotal books: {total_books:,}")

    # Category count
    cursor.execute("SELECT COUNT(*) FROM categories")
    print(f"Total categories: {cursor.fetchone()[0]:,}")

    # Top categories
    print("\nTop 20 categories:")
    cursor.execute("""
        SELECT c.name, COUNT(*) as cnt 
        FROM books b 
        JOIN categories c ON b.category_code = c.code 
        GROUP BY c.code 
        ORDER BY cnt DESC 
        LIMIT 20
    """)
    for row in cursor.fetchall():
        print(f"  {row[0]}: {row[1]:,}")

    print("\n" + "=" * 60)


def main(use_local=True):
    """Main migration function"""
    output_path = LOCAL_TEST_DB if use_local else OUTPUT_DB_PATH

    log(f"Starting migration to {output_path}")

    # Remove existing database
    if os.path.exists(output_path):
        os.remove(output_path)
        log(f"Removed existing database")

    # Create connection
    conn = sqlite3.connect(output_path)

    try:
        # Create schema
        create_schema(conn)

        # Import pg_catalog
        book_count = import_pg_catalog(conn)

        # Add metadata
        add_catalog_meta(conn, book_count)

        # Analyze and report
        analyze_and_report(conn)

        log(f"\nMigration complete! Database saved to {output_path}")

    except Exception as e:
        log(f"Error: {e}")
        import traceback

        traceback.print_exc()
        conn.close()
        raise

    conn.close()


if __name__ == "__main__":
    import sys

    # Check if running on server
    if os.path.exists("/var/www/ebooks"):
        main(use_local=False)
    else:
        # Local test
        main(use_local=True)
