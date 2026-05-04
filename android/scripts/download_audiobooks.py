#!/usr/bin/env python3
"""
Audiobook Download Script
Downloads human-read M4B audiobooks from Project Gutenberg that match EPUB files on local disk.

Usage:
    python3 download_audiobooks.py --help
    python3 download_audiobooks.py --epub-dir /var/www/ebooks/epubs --output-dir /path/to/audiobooks

Requirements:
    pip install requests beautifulsoup4 tqdm lxml
"""

import os
import re
import json
import argparse
import time
from urllib.parse import urljoin
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests
from bs4 import BeautifulSoup
from tqdm import tqdm

# Configuration
GUTENBERG_AUDIOBOOK_URL = "https://www.gutenberg.org/browse/categories/1"
GUTENBERG_AUDIOBOOK_SHELF_URL = "https://www.gutenberg.org/ebooks/bookshelf/70"
GUTENBERG_BASE_URL = "https://www.gutenberg.org"
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

# Default concurrent workers
DEFAULT_WORKERS = 10

# Regex to extract Gutenberg ID from EPUB filename
# Examples: "Some Book.12345.epub", "Title - Author.12345.epub"
EPUB_ID_PATTERN = re.compile(r'\.(\d+)\.epub$', re.IGNORECASE)


class AudiobookDownloader:
    def __init__(self, output_dir, epub_dir=None, workers=DEFAULT_WORKERS, retry_count=3):
        self.output_dir = Path(output_dir)
        self.epub_dir = Path(epub_dir) if epub_dir else None
        self.workers = workers
        self.retry_count = retry_count
        self.session = requests.Session()
        self.session.headers.update({'User-Agent': USER_AGENT})

        # Stats
        self.stats = {
            'epubs_found': 0,
            'audiobooks_matched': 0,
            'chapters_found': 0,
            'chapters_downloaded': 0,
            'chapters_failed': 0,
            'total_size_mb': 0
        }

        # Cache for discovered audiobook IDs
        self.audiobook_ids_cache = None
        self.epub_ids = set()

    def scan_epub_directory(self):
        """Scan local EPUB directory and extract Gutenberg IDs"""
        if not self.epub_dir or not self.epub_dir.exists():
            print(f"Error: EPUB directory not found: {self.epub_dir}")
            return set()

        print(f"Scanning EPUB directory: {self.epub_dir}")

        epub_files = list(self.epub_dir.glob('*.epub'))
        self.stats['epubs_found'] = len(epub_files)

        ids = set()
        for epub_file in epub_files:
            match = EPUB_ID_PATTERN.search(epub_file.name)
            if match:
                ids.add(match.group(1))

        print(f"Found {len(ids)} unique Gutenberg IDs from {len(epub_files)} EPUB files")
        self.epub_ids = ids
        return ids

    def fetch_audiobook_ids(self):
        """Fetch all Gutenberg IDs that have human-read audiobooks from Gutenberg"""
        print(f"Fetching audiobook list from {GUTENBERG_AUDIOBOOK_URL}...")

        if self.audiobook_ids_cache:
            return self.audiobook_ids_cache

        audiobook_ids = set()

        try:
            # Main audiobook category page
            response = self.session.get(GUTENBERG_AUDIOBOOK_URL, timeout=60)
            response.raise_for_status()
            soup = BeautifulSoup(response.text, 'lxml')

            # Extract book IDs from links
            for link in soup.find_all('a', href=True):
                href = link['href']
                match = re.search(r'/ebooks/(\d+)', href)
                if match:
                    audiobook_ids.add(match.group(1))

            # Also check the audiobook bookshelf
            try:
                response2 = self.session.get(GUTENBERG_AUDIOBOOK_SHELF_URL, timeout=30)
                if response2.status_code == 200:
                    soup2 = BeautifulSoup(response2.text, 'lxml')
                    for link in soup2.find_all('a', href=True):
                        href = link['href']
                        match = re.search(r'/ebooks/(\d+)', href)
                        if match:
                            audiobook_ids.add(match.group(1))
            except Exception as e:
                print(f"Warning: Could not fetch audiobook bookshelf: {e}")

            print(f"Found {len(audiobook_ids)} audiobook IDs from Gutenberg")
            self.audiobook_ids_cache = audiobook_ids
            return audiobook_ids

        except requests.RequestException as e:
            print(f"Error fetching audiobook list: {e}")
            return set()

    def check_audiobook_exists(self, book_id):
        """Check if a specific Gutenberg ID has M4B audiobooks and return chapter URLs"""
        url = f"{GUTENBERG_BASE_URL}/files/{book_id}/"
        try:
            # First try the main files page
            response = self.session.get(url, timeout=15)
            if response.status_code != 200:
                return None

            soup = BeautifulSoup(response.text, 'lxml')

            # Look for M4B links - they appear in tables with format info
            m4b_links = []

            # Check for direct M4B folder listing
            m4b_url = f"{GUTENBERG_BASE_URL}/files/{book_id}/m4b/"
            try:
                m4b_response = self.session.get(m4b_url, timeout=10)
                if m4b_response.status_code == 200:
                    m4b_soup = BeautifulSoup(m4b_response.text, 'lxml')
                    for link in m4b_soup.find_all('a', href=True):
                        href = link['href']
                        if href.endswith('.m4b'):
                            m4b_links.append(href)
            except:
                pass

            # Also check the main page for M4B links
            if not m4b_links:
                for link in soup.find_all('a', href=True):
                    href = link['href']
                    if 'm4b' in href.lower() and href.endswith('.m4b'):
                        m4b_links.append(href)

            if not m4b_links:
                return None

            # Extract chapter numbers and sort
            chapter_pattern = re.compile(rf'{book_id}-(\d+)\.m4b')
            chapters = []
            for link in m4b_links:
                full_url = urljoin(GUTENBERG_BASE_URL, link)
                match = chapter_pattern.search(link)
                if match:
                    chapter_num = int(match.group(1))
                    chapters.append((chapter_num, full_url))

            if not chapters:
                return None

            chapters.sort(key=lambda x: x[0])
            return [url for _, url in chapters]

        except requests.RequestException:
            return None

    def download_file(self, url, filepath, pbar=None):
        """Download a single file with retry logic"""
        for attempt in range(self.retry_count):
            try:
                response = self.session.get(url, timeout=120, stream=True)
                response.raise_for_status()

                total_size = int(response.headers.get('content-length', 0))

                with open(filepath, 'wb') as f:
                    for chunk in response.iter_content(chunk_size=8192):
                        if chunk:
                            f.write(chunk)
                            if pbar and total_size:
                                pbar.update(len(chunk))

                return True

            except requests.RequestException as e:
                if attempt < self.retry_count - 1:
                    wait_time = 2 ** attempt
                    print(f"\n  Retry {attempt + 1}/{self.retry_count} after {wait_time}s...")
                    time.sleep(wait_time)
                else:
                    print(f"\n  Failed: {e}")
                    return False

        return False

    def process_book(self, book_id):
        """Process a single book - check for audiobooks and download"""
        chapters = self.check_audiobook_exists(book_id)
        if not chapters:
            return None

        self.stats['audiobooks_matched'] += 1
        self.stats['chapters_found'] += len(chapters)

        # Create directory for this book
        book_dir = self.output_dir / book_id
        book_dir.mkdir(parents=True, exist_ok=True)

        # Download each chapter
        downloaded = []
        for i, chapter_url in enumerate(chapters, 1):
            filename = f"{book_id}-{i:02d}.m4b"
            filepath = book_dir / filename

            if filepath.exists():
                downloaded.append(filepath)
                continue

            pbar = tqdm(
                total=None,
                desc=f"  {book_id}/{i:02d}",
                unit='B',
                unit_scale=True,
                leave=False
            )

            success = self.download_file(chapter_url, filepath, pbar)
            pbar.close()

            if success:
                downloaded.append(filepath)
                self.stats['chapters_downloaded'] += 1
                self.stats['total_size_mb'] += filepath.stat().st_size / (1024 * 1024)
            else:
                self.stats['chapters_failed'] += 1
                if filepath.exists():
                    filepath.unlink()

        if downloaded:
            return {
                'id': book_id,
                'chapters': len(downloaded),
                'path': str(book_dir)
            }
        return None

    def run(self):
        """Main execution loop"""
        print("\n" + "=" * 60)
        print("GUTENBERG AUDIOBOOK DOWNLOADER")
        print("=" * 60)

        # Ensure output directory exists
        self.output_dir.mkdir(parents=True, exist_ok=True)

        # Step 1: Scan local EPUB directory
        epub_ids = self.scan_epub_directory()

        # Step 2: Fetch audiobook IDs from Gutenberg
        gutenberg_audiobook_ids = self.fetch_audiobook_ids()

        # Step 3: Find intersection
        matched_ids = sorted(epub_ids & gutenberg_audiobook_ids)

        print(f"\nMatched {len(matched_ids)} books that have both EPUBs and audiobooks")

        if not matched_ids:
            print("\nNo matching audiobooks found.")
            return []

        # Step 4: Download audiobooks
        matched_books = []

        print(f"\nDownloading audiobooks...")
        print(f"Using {self.workers} concurrent workers\n")

        with ThreadPoolExecutor(max_workers=self.workers) as executor:
            futures = {
                executor.submit(self.process_book, book_id): book_id
                for book_id in matched_ids
            }

            for future in tqdm(as_completed(futures), total=len(futures), desc="Downloading"):
                result = future.result()
                if result:
                    matched_books.append(result)
                    print(f"  ✓ {result['id']}: {result['chapters']} chapters")

        # Summary
        print("\n" + "=" * 60)
        print("DOWNLOAD COMPLETE")
        print("=" * 60)
        print(f"EPUBs on disk: {self.stats['epubs_found']}")
        print(f"Audiobooks available on Gutenberg: {len(gutenberg_audiobook_ids)}")
        print(f"Matched books with audiobooks: {len(matched_books)}")
        print(f"Chapters found: {self.stats['chapters_found']}")
        print(f"Chapters downloaded: {self.stats['chapters_downloaded']}")
        print(f"Chapters failed: {self.stats['chapters_failed']}")
        print(f"Total size: {self.stats['total_size_mb']:.2f} MB")
        print(f"Output directory: {self.output_dir}")

        # Save summary
        summary_file = self.output_dir / "download_summary.json"
        with open(summary_file, 'w') as f:
            json.dump({
                'stats': self.stats,
                'matched_books': matched_books
            }, f, indent=2)
        print(f"\nSummary saved to: {summary_file}")

        return matched_books


def main():
    parser = argparse.ArgumentParser(
        description='Download human-read M4B audiobooks from Project Gutenberg',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Run on EC2 with EPUBs in default location:
  python3 download_audiobooks.py --output-dir /var/www/audiobooks

  # With custom EPUB directory:
  python3 download_audiobooks.py --epub-dir /var/www/ebooks/epubs --output-dir ./audiobooks

  # Dry run - just show what would be downloaded:
  python3 download_audiobooks.py --output-dir ./audiobooks --dry-run
        """
    )

    parser.add_argument(
        '--epub-dir',
        default='/var/www/ebooks/epubs',
        help='Directory containing EPUB files (default: /var/www/ebooks/epubs)'
    )

    parser.add_argument(
        '--output-dir',
        required=True,
        help='Directory to save downloaded audiobooks'
    )

    parser.add_argument(
        '--workers',
        type=int,
        default=DEFAULT_WORKERS,
        help=f'Number of concurrent download workers (default: {DEFAULT_WORKERS})'
    )

    parser.add_argument(
        '--retry-count',
        type=int,
        default=3,
        help='Number of retry attempts for failed downloads (default: 3)'
    )

    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Only show matches without downloading'
    )

    args = parser.parse_args()

    # Initialize downloader
    downloader = AudiobookDownloader(
        output_dir=args.output_dir,
        epub_dir=args.epub_dir,
        workers=args.workers,
        retry_count=args.retry_count
    )

    # Run
    if args.dry_run:
        # Just show what would be downloaded
        epub_ids = downloader.scan_epub_directory()
        gutenberg_ids = downloader.fetch_audiobook_ids()
        matched = sorted(epub_ids & gutenberg_ids)

        print("\n" + "=" * 60)
        print("DRY RUN - Audiobooks that would be downloaded:")
        print("=" * 60)
        print(f"EPUBs found: {len(epub_ids)}")
        print(f"Audiobooks on Gutenberg: {len(gutenberg_ids)}")
        print(f"Matched: {len(matched)}\n")

        for book_id in matched[:30]:
            print(f"  - {book_id}")
        if len(matched) > 30:
            print(f"  ... and {len(matched) - 30} more")

        # Save full list
        with open(f"{args.output_dir}/matched_ids.txt", 'w') as f:
            for book_id in matched:
                f.write(f"{book_id}\n")
        print(f"\nFull list saved to: {args.output_dir}/matched_ids.txt")
    else:
        downloader.run()


if __name__ == '__main__':
    main()