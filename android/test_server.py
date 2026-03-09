import http.server
import socketserver
import os
import urllib.parse
import sys

PORT = 8080

class DiagnosticHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        # Decode the path to see what the app is actually asking for
        decoded_path = urllib.parse.unquote(self.path).lstrip('/')
        
        print("-" * 50)
        print(f"[REQUEST] Raw Path: {self.path}")
        print(f"[REQUEST] Decoded:  {decoded_path}")
        
        # Check if file exists relative to where the server is running
        full_path = os.path.join(os.getcwd(), decoded_path)
        if os.path.exists(decoded_path) and os.path.isfile(decoded_path):
            print(f"[FOUND] File exists: {os.path.abspath(decoded_path)}")
            print(f"[SIZE]  {os.path.getsize(decoded_path)} bytes")
        else:
            print(f"[NOT FOUND] Checked: {full_path}")
            # Help diagnose folder mismatch
            if os.path.exists(os.path.basename(decoded_path)):
                print(f"[HINT] File found in CURRENT dir but request had subfolders: {os.path.basename(decoded_path)}")
            
        sys.stdout.flush() # Force print to terminal immediately
        return super().do_GET()

print(f"Starting Diagnostic Server on port {PORT}...")
print(f"Current Working Directory: {os.getcwd()}")
print("Point your Android app to this server and check the output below.")
print("-" * 50)

# Allow port reuse to avoid "Address already in use" errors
socketserver.TCPServer.allow_reuse_address = True
with socketserver.TCPServer(("", PORT), DiagnosticHandler) as httpd:
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping server...")
        httpd.shutdown()
