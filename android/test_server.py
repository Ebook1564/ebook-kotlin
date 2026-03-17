import http.server
import socketserver
import os
import urllib.parse
import sys
import re

PORT = 8080
DATASET_DIR = r"E:\WSL\all_extracted"

class SmartHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        url_path = urllib.parse.unquote(self.path).lstrip('/')
        requested_filename = os.path.basename(url_path)
        
        if not requested_filename:
            return super().do_GET()

        print("-" * 50)
        print(f"[REQUEST] {requested_filename}")

        # 1. DEFINE PRIORITY SEARCH DIRS
        search_dirs = [
            os.path.join(DATASET_DIR, 'covers'), # HIGH PRIORITY
            DATASET_DIR,
            os.getcwd(),
            'all_extracted',
            'law'
        ]
        
        found_path = None
        
        # 2. EXACT MATCH
        for d in search_dirs:
            if not os.path.exists(d): continue
            test_path = os.path.join(d, requested_filename)
            if os.path.isfile(test_path):
                found_path = test_path
                break
        
        # 3. ADVANCED ID-STRICT MATCHING
        if not found_path:
            match = re.search(r'(\d+)', requested_filename)
            if match:
                book_id = match.group(1)
                ext = os.path.splitext(requested_filename)[1].lower()
                if not ext: ext = ".jpg" # Default to jpg for generic ID reqs
                
                print(f"[RETRY] Searching for strict ID: {book_id}")
                
                for d in search_dirs:
                    if not os.path.exists(d): continue
                    try:
                        files = os.listdir(d)
                    except Exception: continue

                    # STAGE A: Priority Patterns (Strict ID prefix)
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
                                break
                        if found_path: break
                    
                    # STAGE B: Exact ID Match (e.g. 52636.jpg)
                    if not found_path:
                        if f"{book_id}{ext}" in files:
                            found_path = os.path.join(d, f"{book_id}{ext}")
                    
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
                print(f"[SUCCESS] Served from {os.path.basename(os.path.dirname(found_path))}: {found_path}")
                return
            except Exception as e:
                print(f"[ERROR] {e}")
        
        print(f"[404] No match for {requested_filename}")
        self.send_error(404, "File not found")

print(f"Ebook Bundle Server ACTIVE on port {PORT}")
socketserver.TCPServer.allow_reuse_address = True
with socketserver.TCPServer(("", PORT), SmartHandler) as httpd:
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        httpd.shutdown()
