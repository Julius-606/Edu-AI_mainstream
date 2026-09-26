"""
Ingestion engine module bridge for the Edu-AI Trace backend.
Supports both root and package-level imports.
"""

import os
import sys
from pathlib import Path

# Add backend root to sys.path if not present
backend_root = str(Path(__file__).resolve().parents[1])
if backend_root not in sys.path:
    sys.path.insert(0, backend_root)

try:
    import ingestion_engine as _root_engine
    parse_syllabus_markdown = _root_engine.parse_syllabus_markdown
    save_syllabus_to_db = _root_engine.save_syllabus_to_db
    get_global_units = _root_engine.get_global_units
    delete_unit = _root_engine.delete_unit
    update_unit = _root_engine.update_unit
    clone_unit_to_user = _root_engine.clone_unit_to_user
except ImportError:
    pass
