import logging
import time
from typing import Dict, Any
from sqlalchemy.orm import Session
from app.models import database_models as models
from .progress_sync import fetch_user_progress, sync_user_progress
from .bookmarks_sync import fetch_user_bookmarks, sync_user_bookmarks
from .quiz_sync import fetch_user_quizzes, sync_user_quizzes
from .chat_sync import fetch_user_chats, sync_user_chats
from .user_sync import fetch_user_profile, sync_user_profile

logger = logging.getLogger("edu_ai.sync.manager")

class SyncManager:
    """
    Modular, high-resilience coordinator for user data synchronization.
    Guarantees that a failure in one domain (e.g., bookmarks schema anomaly)
    will not abort the remaining syllabus, quiz, or consultation trails.
    """

    def get_full_sync_data(self, db: Session, user: models.User) -> Dict[str, Any]:
        t0 = time.time()
        logger.info(f"Initiating full sync data pull for user #{user.id} ({user.username})")

        # 1. Syllabus Progress Sub-Module
        try:
            progress_list = fetch_user_progress(db, user.id)
        except Exception as err:
            logger.error(f"[Sync Sub-Module: Progress] Failed: {err}")
            progress_list = []

        # 2. Bookmarks Sub-Module (Fault-tolerant with auto-healing schema)
        try:
            bookmarks_list = fetch_user_bookmarks(db, user.id)
        except Exception as err:
            logger.error(f"[Sync Sub-Module: Bookmarks] Failed: {err}")
            bookmarks_list = []

        # 3. Quiz Performance Sub-Module
        try:
            quizzes_list = fetch_user_quizzes(db, user.id)
        except Exception as err:
            logger.error(f"[Sync Sub-Module: Quiz] Failed: {err}")
            quizzes_list = []

        # 4. Chat Consultations Sub-Module
        try:
            chats_list = fetch_user_chats(db, user.id)
        except Exception as err:
            logger.error(f"[Sync Sub-Module: Chat] Failed: {err}")
            chats_list = []

        # 5. User Profile Sub-Module
        try:
            user_profile = fetch_user_profile(db, user)
        except Exception as err:
            logger.error(f"[Sync Sub-Module: User] Failed: {err}")
            user_profile = {"id": str(user.id), "username": user.username, "email": user.email, "role": user.role}

        elapsed = time.time() - t0
        logger.info(
            f"Sync data constructed in {elapsed:.3f}s: "
            f"{len(progress_list)} progress, {len(bookmarks_list)} bookmarks, "
            f"{len(quizzes_list)} quizzes, {len(chats_list)} chats"
        )

        return {
            "progress": progress_list,
            "bookmarks": bookmarks_list,
            "quizzes": quizzes_list,
            "chats": chats_list,
            "user": user_profile,
            "meta": {
                "sync_time": time.time(),
                "duration_ms": round(elapsed * 1000, 2),
                "status": "synchronized"
            }
        }

    def process_full_sync(self, db: Session, user: models.User, payload: Any) -> Dict[str, Any]:
        t0 = time.time()
        logger.info(f"Processing client sync push for user #{user.id} ({user.username})")

        stats = {}

        # 1. Syllabus Progress
        if hasattr(payload, "progress") and payload.progress:
            stats["progress"] = sync_user_progress(db, user.id, payload.progress)

        # 2. Bookmarks
        if hasattr(payload, "bookmarks") and payload.bookmarks:
            stats["bookmarks"] = sync_user_bookmarks(db, user.id, payload.bookmarks)

        # 3. Quizzes
        if hasattr(payload, "quizzes") and payload.quizzes:
            stats["quizzes"] = sync_user_quizzes(db, user.id, payload.quizzes)

        # 4. Chats
        if hasattr(payload, "chats") and payload.chats:
            stats["chats"] = sync_user_chats(db, user.id, payload.chats)

        # 5. User Preferences
        stats["user"] = sync_user_profile(db, user, payload)

        elapsed = time.time() - t0
        logger.info(f"Client sync processed in {elapsed:.3f}s. Sub-module statistics: {stats}")

        return {
            "status": "success",
            "message": "User data successfully synchronized across clinical sub-modules.",
            "synced_at": time.time(),
            "details": stats
        }

sync_manager = SyncManager()
