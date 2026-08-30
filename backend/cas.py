# IDENTITY: backend/cas.py
# VERSION: 1.0.0
# ⚙️ CAS (Content-Addressable Storage) & Delta Engine for FastAPI Backend

import hashlib
import gzip
import json
from typing import List, Dict, Any

def sha256(text: str) -> str:
    """Calculates SHA-256 hash string for text."""
    return hashlib.sha256(text.encode('utf-8')).hexdigest()

def compress(text: str) -> bytes:
    """Compresses text to GZIP bytes."""
    return gzip.compress(text.encode('utf-8'))

def decompress(data: bytes) -> str:
    """Decompresses GZIP bytes back to string."""
    if not data:
        return ""
    return gzip.decompress(data).decode('utf-8')

def create_diff_patch(old_text: str, new_text: str) -> str:
    """Generates a line-by-line diff patch JSON string."""
    old_lines = old_text.splitlines() if old_text else []
    new_lines = new_text.splitlines() if new_text else []
    
    old_set = set(old_lines)
    new_set = set(new_lines)
    
    ops = []
    for line in old_lines:
        if line not in new_set:
            ops.append({"type": "DELETE", "line": line})
        else:
            ops.append({"type": "KEEP", "line": line})

    for line in new_lines:
        if line not in old_set:
            ops.append({"type": "ADD", "line": line})

    return json.dumps(ops)

def apply_diff_patch(base_text: str, patch_json: str) -> str:
    """Applies a diff patch JSON string to base text."""
    if not patch_json:
        return base_text
    try:
        ops = json.loads(patch_json)
    except Exception:
        return base_text

    base_lines = base_text.splitlines() if base_text else []
    
    for op in ops:
        op_type = op.get("type")
        line = op.get("line")
        if op_type == "DELETE" and line in base_lines:
            base_lines.remove(line)
        elif op_type == "ADD" and line not in base_lines:
            base_lines.append(line)

    return "\n".join(base_lines)
