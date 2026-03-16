import http.server
import socketserver
import os
import urllib.parse
import sys
import re

PORT = 8080

# The master dataset is on E:
DATASET_DIR = r"E:\WSL\all_extracted"

class SmartHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        # 1. Decode requested path
        url_path = urllib.parse.unquote(self.path).lstrip('/')
        requested_filename = os.path.basename(url_path)
        
        if not requested_filename:
            return super().do_GET()

        print("-" * 50)
        print(f"[REQUEST] {requested_filename}")

        search_dirs = [
            DATASET_DIR,
            os.path.join(DATASET_DIR, 'covers'),
            os.getcwd(),
            'all_extracted',
            'law'
        ]
        
        found_path = None
        
        # Try exact match first
        for d in search_dirs:
            if not os.path.exists(d): continue
            test_path = os.path.join(d, requested_filename)
            if os.path.isfile(test_path):
                found_path = test_path
                break
        
        # 3. ADVANCED FUZZY MATCHING (Deep Search)
        if not found_path:
            match = re.search(r'(\d+)', requested_filename)
            if match:
                book_id = match.group(1)
                ext = os.path.splitext(requested_filename)[1].lower()
                
                print(f"[RETRY] Exact match failed. Searching for ID: {book_id} with extension {ext}")
                
                for d in search_dirs:
                    if not os.path.exists(d): continue
                    
                    # Optimization: Get all files in dir once
                    try:
                        files = os.listdir(d)
                    except Exception: continue

                    # Strategy A: Check common image patterns for this ID
                    if ext in ['.png', '.jpg', '.jpeg']:
                        # Patterns to prioritize:
                        # 1. {id}_cover_image.jpg
                        # 2. {id}_img-cover.jpg
                        # 3. {id}_img-front.jpg
                        # 4. {id}_52636-cover.png
                        patterns = [
                            f"{book_id}_cover_image",
                            f"{book_id}_img-cover",
                            f"{book_id}_img-front",
                            f"{book_id}_{book_id}-cover",
                            f"{book_id}_"
                        ]
                        
                        for p in patterns:
                            for f in files:
                                if f.lower().startswith(p.lower()) and f.lower().endswith(ext):
                                    found_path = os.path.join(d, f)
                                    print(f"[FUZZY FOUND] Matched pattern {p} -> {f}")
                                    break
                            if found_path: break
                    
                    # Strategy B: Generic ID match if still not found
                    if not found_path:
                        for f in files:
                            if book_id in f and f.lower().endswith(ext):
                                if f"{book_id}." in f or f"_{book_id}" in f or f".{book_id}" in f:
                                    found_path = os.path.join(d, f)
                                    print(f"[GENERIC FOUND] Matched {f}")
                                    break
                    
                    if found_path: break

        if found_path:
            try:
                self.send_response(200)
                if found_path.lower().endswith('.png'): self.send_header('Content-type', 'image/png')
                elif found_path.lower().endswith('.jpg') or found_path.lower().endswith('.jpeg'): self.send_header('Content-type', 'image/jpeg')
                elif found_path.lower().endswith('.epub'): self.send_header('Content-type', 'application/epub+zip')
                
                stat = os.stat(found_path)
                self.send_header("Content-Length", str(stat.st_size))
                self.end_headers()
                
                with open(found_path, 'rb') as f:
                    self.wfile.write(f.read())
                print(f"[SUCCESS] Served: {found_path}")
                return
            except Exception as e:
                print(f"[ERROR] {e}")
        
        print(f"[404] No match for {requested_filename}")
        self.send_error(404, "File not found")

print(f"Smart Server ACTIVE on port {PORT}")
print(f"Master Dataset: {DATASET_DIR}")

socketserver.TCPServer.allow_reuse_address = True
with socketserver.TCPServer(("", PORT), SmartHandler) as httpd:
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        httpd.shutdown()
