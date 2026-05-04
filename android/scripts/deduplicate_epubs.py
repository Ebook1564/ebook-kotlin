#!/usr/bin/env python3
"""
EPUB Deduplication Script
Identifies duplicate EPUB files by Gutenberg ID and keeps the largest file in each group.

Usage:
    python deduplicate_epubs.py --preview                    # Preview duplicates (default)
    python deduplicate_epubs.py --execute                   # Actually delete duplicates
    python deduplicate_epubs.py --source-dir /path/to/epubs # Custom directory
"""

import os
import re
import argparse
import sys
from datetime import datetime
from collections import defaultdict

# Configuration
DEFAULT_SOURCE_DIR = "/var/www/ebooks/epubs"
ID_PATTERN = re.compile(r'\.(\d+)\.epub$', re.IGNORECASE)
MIN_FILE_SIZE_BYTES = 1024  # Skip files smaller than 1KB


class Deduplicator:
    def __init__(self, source_dir, verbose=False):
        self.source_dir = source_dir
        self.verbose = verbose
        self.stats = {
            'total_scanned': 0,
            'unique_ids': 0,
            'duplicate_groups': 0,
            'files_to_delete': 0,
            'total_size_to_free': 0,
            'deleted': 0,
            'errors': 0
        }
        self.duplicate_groups = {}

    def extract_gutenberg_id(self, filename):
        """Extract Gutenberg ID from filename like 'Title.12345.epub'"""
        match = ID_PATTERN.search(filename)
        if match:
            return match.group(1)
        return None

    def scan_directory(self):
        """Scan directory and group files by Gutenberg ID"""
        print(f"Scanning directory: {self.source_dir}")
        print("-" * 50)

        if not os.path.exists(self.source_dir):
            print(f"ERROR: Directory not found: {self.source_dir}")
            return False

        # Group files by Gutenberg ID
        id_groups = defaultdict(list)

        files = [f for f in os.listdir(self.source_dir) if f.lower().endswith('.epub')]
        self.stats['total_scanned'] = len(files)

        for filename in files:
            gutenberg_id = self.extract_gutenberg_id(filename)
            if not gutenberg_id:
                continue

            filepath = os.path.join(self.source_dir, filename)
            try:
                file_size = os.path.getsize(filepath)
                if file_size < MIN_FILE_SIZE_BYTES:
                    continue  # Skip likely corrupt files

                id_groups[gutenberg_id].append({
                    'filename': filename,
                    'filepath': filepath,
                    'size': file_size,
                    'size_mb': file_size / (1024 * 1024)
                })
            except OSError as e:
                print(f"WARNING: Could not stat {filename}: {e}")
                self.stats['errors'] += 1

        # Identify duplicate groups (IDs with 2+ files)
        self.stats['unique_ids'] = len(id_groups)

        for gid, files in id_groups.items():
            if len(files) > 1:
                self.duplicate_groups[gid] = sorted(files, key=lambda x: x['size'], reverse=True)
                self.stats['duplicate_groups'] += 1
                self.stats['files_to_delete'] += len(files) - 1

                # Calculate size to free
                for f in files[1:]:  # Skip first (largest)
                    self.stats['total_size_to_free'] += f['size']

        return True

    def generate_report(self):
        """Generate preview report of duplicates"""
        print("\n" + "=" * 50)
        print("EPUB DUPLICATE REPORT")
        print("=" * 50)
        print(f"Directory: {self.source_dir}")
        print(f"Total EPUBs scanned: {self.stats['total_scanned']:,}")
        print(f"Unique IDs found: {self.stats['unique_ids']:,}")
        print(f"Duplicate groups: {self.stats['duplicate_groups']:,}")
        print(f"Files to delete: {self.stats['files_to_delete']:,}")
        print(f"Space to free: {self.stats['total_size_to_free'] / (1024**3):.2f} GB")

        if self.stats['duplicate_groups'] == 0:
            print("\n✓ No duplicates found!")
            return

        print("\n" + "-" * 50)
        print("DUPLICATE GROUPS (keeping largest):")
        print("-" * 50)

        # Show all groups if verbose, otherwise show sample
        groups_to_show = self.duplicate_groups.items()
        if not self.verbose and len(groups_to_show) > 20:
            groups_to_show = list(groups_to_show)[:20]

        for gid, files in groups_to_show:
            print(f"\n[ID: {gid}]")
            for i, f in enumerate(files):
                prefix = "KEEP" if i == 0 else "DELETE"
                print(f"  {prefix}: {f['filename']}")
                print(f"          Size: {f['size_mb']:.2f} MB")

        if not self.verbose and len(self.duplicate_groups) > 20:
            remaining = len(self.duplicate_groups) - 20
            print(f"\n... and {remaining} more groups. Use --verbose to see all.")

    def execute_deletion(self):
        """Actually delete the duplicate files"""
        print("\n" + "=" * 50)
        print("EXECUTING DELETION")
        print("=" * 50)

        if not self.duplicate_groups:
            print("No duplicates to delete.")
            return

        confirm = input(f"\nProceed with deleting {self.stats['files_to_delete']:,} files? (yes/no): ")
        if confirm.lower() != 'yes':
            print("Deletion cancelled.")
            return

        print("\nDeleting files...")
        deleted_count = 0

        for gid, files in self.duplicate_groups.items():
            for f in files[1:]:  # Skip first (largest)
                try:
                    os.remove(f['filepath'])
                    print(f"Deleted: {f['filename']} ({f['size_mb']:.2f} MB)")
                    deleted_count += 1
                except OSError as e:
                    print(f"ERROR: Failed to delete {f['filename']}: {e}")
                    self.stats['errors'] += 1

        print("\n" + "-" * 50)
        print(f"Deletion complete: {deleted_count} files removed")
        print(f"Space freed: {self.stats['total_size_to_free'] / (1024**3):.2f} GB")
        print(f"Errors: {self.stats['errors']}")


def main():
    parser = argparse.ArgumentParser(
        description='Find and remove duplicate EPUB files by Gutenberg ID',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python deduplicate_epubs.py --preview                           # Preview what would be deleted
  python deduplicate_epubs.py --preview --verbose                 # Show all duplicates
  python deduplicate_epubs.py --execute                           # Actually delete duplicates
  python deduplicate_epubs.py --source-dir /custom/path --execute # Custom directory
        """
    )

    parser.add_argument(
        '--source-dir',
        default=DEFAULT_SOURCE_DIR,
        help=f'EPUB directory to scan (default: {DEFAULT_SOURCE_DIR})'
    )

    parser.add_argument(
        '--preview',
        action='store_true',
        default=True,
        help='Preview duplicates without deleting (default)'
    )

    parser.add_argument(
        '--execute',
        action='store_true',
        help='Actually delete duplicate files (default is preview only)'
    )

    parser.add_argument(
        '--verbose',
        action='store_true',
        help='Show all duplicate groups, not just a sample'
    )

    args = parser.parse_args()

    # Initialize deduplicator
    dedup = Deduplicator(args.source_dir, verbose=args.verbose)

    # Scan and analyze
    if not dedup.scan_directory():
        sys.exit(1)

    # Generate report
    dedup.generate_report()

    # Execute if requested
    if args.execute:
        dedup.execute_deletion()
    else:
        print("\n" + "=" * 50)
        print("PREVIEW MODE - No files were deleted")
        print("Run with --execute to delete duplicates")
        print("=" * 50)


if __name__ == '__main__':
    main()