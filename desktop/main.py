#!/usr/bin/env python3
"""
Trace Learning System - Desktop Application Runner
Deployable on Windows and Linux.

Supports:
1. Native Desktop Webview / Windowing (using pywebview, webview, or native web bridge)
2. Embedded or Remote Backend (connects to local Python backend or Hugging Face Space)
3. Offline-first synchronization with Neon DB and local storage
"""

import os
import sys
import time
import socket
import threading
import webbrowser
import argparse

VERSION = "3.1.0"
DEFAULT_HF_URL = "https://huggingface.co/spaces/Agent606/Edu-AI"
LOCAL_WEB_PORT = 3000
LOCAL_API_PORT = 7860

def is_port_in_use(port: int, host: str = "127.0.0.1") -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(0.5)
        return s.connect_ex((host, port)) == 0

def run_native_webview(target_url: str, title: str = "Trace Learning System"):
    """Attempts to launch a native desktop window using pywebview or falls back to system browser."""
    try:
        import webview
        print(f"[*] Initializing native desktop window targeting: {target_url}")
        window = webview.create_window(
            title=title,
            url=target_url,
            width=1280,
            height=850,
            min_size=(900, 600),
            confirm_close=True
        )
        webview.start(debug=False)
        return True
    except ImportError:
        print("[!] 'pywebview' not installed. Falling back to default desktop web app window.")
        webbrowser.open(target_url)
        return False

def main():
    parser = argparse.ArgumentParser(description=f"Trace Desktop Client v{VERSION} (Windows & Linux)")
    parser.add_argument("--mode", choices=["local", "cloud", "hybrid"], default="hybrid",
                        help="Operating mode: 'local' (offline), 'cloud' (Hugging Face + Neon), 'hybrid' (preferred)")
    parser.add_argument("--backend-url", type=str, default="",
                        help="Custom backend URL (defaults to HF Space or local)")
    parser.add_argument("--backend", choices=["cloud", "ngrok", "container"], default="cloud",
                        help="Choose target gateway: 'cloud' (Hugging Face), 'ngrok' (Workstation), 'container' (AI Studio Container)")
    parser.add_argument("--headless", action="store_true",
                        help="Run without opening window")
    args = parser.parse_args()

    print("=" * 60)
    print(f"  TRACE LEARNING SYSTEM - DESKTOP SUITE v{VERSION}")
    print(f"  Platform: {sys.platform.capitalize()} | Python {sys.version.split()[0]}")
    print("=" * 60)

    # Determine target URL
    if args.backend == "cloud":
        target_url = args.backend_url or DEFAULT_HF_URL
    elif args.backend == "ngrok":
        target_url = "https://untropic-rozanne-noncomprehendingly.ngrok-free.dev"
    elif args.backend == "container":
        # Connect directly to the local container background service
        target_url = "http://localhost:8001"
    else:
        if args.mode == "cloud":
            target_url = args.backend_url or DEFAULT_HF_URL
        else:
            # Check if local web client is running
            if is_port_in_use(LOCAL_WEB_PORT):
                target_url = f"http://localhost:{LOCAL_WEB_PORT}"
            elif is_port_in_use(LOCAL_API_PORT):
                target_url = f"http://localhost:{LOCAL_API_PORT}/docs"
            else:
                target_url = args.backend_url or DEFAULT_HF_URL

    print(f"[*] Launching Trace Desktop Interface -> {target_url}")

    if not args.headless:
        run_native_webview(target_url)

if __name__ == "__main__":
    main()
