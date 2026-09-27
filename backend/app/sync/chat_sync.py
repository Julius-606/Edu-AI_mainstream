import logging
from typing import List, Dict, Any
from sqlalchemy.orm import Session
from app.models import database_models as models

logger = logging.getLogger("edu_ai.sync.chat")

def fetch_user_chats(db: Session, user_id: int) -> List[Dict[str, Any]]:
    """
    Fetch chat consultation sessions and chronological message logs.
    """
    try:
        sessions = db.query(models.ChatSession).filter(models.ChatSession.owner_id == user_id).all()
        sessions_list = []
        for s in sessions:
            try:
                msgs = db.query(models.ChatMessage).filter(
                    models.ChatMessage.session_id == s.id
                ).order_by(models.ChatMessage.id.asc()).all()
                msgs_list = [
                    {
                        "id": m.id,
                        "role": m.role,
                        "content": m.content,
                        "timestamp": m.timestamp
                    }
                    for m in msgs
                ]
            except Exception as msg_err:
                logger.warning(f"Error fetching messages for session {s.id}: {msg_err}")
                msgs_list = []
                
            sessions_list.append({
                "id": s.id,
                "title": s.title,
                "description": s.description,
                "timestamp": s.timestamp,
                "is_archived": s.is_archived,
                "messages": msgs_list
            })
        return sessions_list
    except Exception as err:
        logger.error(f"Error fetching chat sessions for user {user_id}: {err}")
        return []

def sync_user_chats(db: Session, user_id: int, chats_payload: List[Any]) -> Dict[str, Any]:
    """
    Synchronizes chat sessions and messages.
    """
    synced_sessions = 0
    synced_msgs = 0
    
    for s_item in chats_payload:
        try:
            s_title = getattr(s_item, "title", "Clinical Consultation") or "Clinical Consultation"
            s_desc = getattr(s_item, "description", None)
            s_time = getattr(s_item, "timestamp", 0.0) or 0.0
            s_archived = getattr(s_item, "is_archived", False) or False
            messages = getattr(s_item, "messages", []) or []
            
            session = db.query(models.ChatSession).filter(
                models.ChatSession.owner_id == user_id,
                models.ChatSession.title == s_title
            ).first()
            
            if not session:
                session = models.ChatSession(
                    title=s_title,
                    description=s_desc,
                    timestamp=s_time,
                    is_archived=s_archived,
                    owner_id=user_id
                )
                db.add(session)
                db.flush()
                synced_sessions += 1
                
            for m_item in messages:
                m_content = getattr(m_item, "content", "") or ""
                m_role = getattr(m_item, "role", "user") or "user"
                m_time = str(getattr(m_item, "timestamp", ""))
                
                existing_m = db.query(models.ChatMessage).filter(
                    models.ChatMessage.session_id == session.id,
                    models.ChatMessage.content == m_content,
                    models.ChatMessage.timestamp == m_time
                ).first()
                
                if not existing_m:
                    db.add(models.ChatMessage(
                        role=m_role,
                        content=m_content,
                        timestamp=m_time,
                        owner_id=user_id,
                        session_id=session.id
                    ))
                    synced_msgs += 1
        except Exception as err:
            logger.warning(f"Error syncing chat session: {err}")
            
    try:
        db.commit()
    except Exception as err:
        db.rollback()
        logger.error(f"Failed to commit chat sync: {err}")
        
    return {"synced_sessions": synced_sessions, "synced_messages": synced_msgs}
