import os
from pathlib import Path
from dotenv import load_dotenv

# Load .env variables
load_dotenv(Path(__file__).resolve().parent / ".env")

import uvicorn
from app.main import app
from pyngrok import ngrok

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))

    # Start ngrok tunnel with the specific domain
    print(f"Opening ngrok tunnel on port {port}...")
    try:
        # Use the provided ngrok domain
        public_url = ngrok.connect(port, domain="untropic-rozanne-noncomprehendingly.ngrok-free.dev").public_url
        print(f" * ngrok tunnel established at: {public_url}")
    except Exception as e:
        print(f"Could not start ngrok: {e}")
        print("Continuing with local server only...")

    print(f"Starting MODULAR Backend on port {port}...")
    uvicorn.run(app, host="0.0.0.0", port=port)
