import {
  User,
  Unit,
  QuizQuestion,
  QuizHistoryItem,
  TimetableSlot,
  Bookmark,
  ChatSession,
  PeerMessage,
  StudentProgressSummary
} from '../types';
import { DEFAULT_UNITS } from '../data/defaultCurriculum';
import {
  INITIAL_USERS,
  INITIAL_TIMETABLE,
  INITIAL_QUIZ_BANK,
  INITIAL_QUIZ_HISTORY,
  INITIAL_CHAT_SESSIONS,
  INITIAL_TEACHER_STUDENTS
} from '../data/mockData';

const STORAGE_KEYS = {
  USER: 'trace_active_user',
  ALL_USERS: 'trace_users',
  UNITS: 'trace_units',
  QUIZ_HISTORY: 'trace_quiz_history',
  TIMETABLE: 'trace_timetable',
  BOOKMARKS: 'trace_bookmarks',
  CHAT_SESSIONS: 'trace_chat_sessions',
  PEER_MESSAGES: 'trace_peer_messages',
  TEACHER_STUDENTS: 'trace_teacher_students',
  ZENITH_INSIGHT: 'trace_zenith_insight'
};

function safeGet<T>(key: string, fallback: T): T {
  try {
    const item = localStorage.getItem(key);
    if (!item) return fallback;
    return JSON.parse(item) as T;
  } catch {
    return fallback;
  }
}

function safeSet<T>(key: string, value: T): void {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch (e) {
    console.error(`Failed to save to localStorage for key ${key}:`, e);
  }
}

export class TraceStore {
  // Current user
  static getUser(): User {
    const saved = safeGet<User | null>(STORAGE_KEYS.USER, null);
    if (saved) return saved;
    const initial = INITIAL_USERS[0];
    safeSet(STORAGE_KEYS.USER, initial);
    return initial;
  }

  static setUser(user: User): void {
    safeSet(STORAGE_KEYS.USER, user);
    // Also update in all users list
    const all = this.getAllUsers();
    const idx = all.findIndex((u) => u.id === user.id);
    if (idx >= 0) {
      all[idx] = user;
    } else {
      all.push(user);
    }
    safeSet(STORAGE_KEYS.ALL_USERS, all);
  }

  static getAllUsers(): User[] {
    return safeGet<User[]>(STORAGE_KEYS.ALL_USERS, INITIAL_USERS);
  }

  // Units and curriculum
  static getUnits(): Unit[] {
    return safeGet<Unit[]>(STORAGE_KEYS.UNITS, DEFAULT_UNITS);
  }

  static saveUnits(units: Unit[]): void {
    safeSet(STORAGE_KEYS.UNITS, units);
  }

  static toggleSubtopicCompleted(subtopicId: number): Unit[] {
    const units = this.getUnits();
    for (const unit of units) {
      for (const mod of unit.modules) {
        for (const topic of mod.topics) {
          for (const sub of topic.subtopics) {
            if (sub.id === subtopicId) {
              sub.isCompleted = !sub.isCompleted;
              this.saveUnits(units);
              return [...units];
            }
          }
        }
      }
    }
    return units;
  }

  static toggleUnitActive(unitId: number, isActive: boolean): Unit[] {
    const units = this.getUnits();
    const unit = units.find((u) => u.id === unitId);
    if (unit) {
      unit.isActive = isActive;
      this.saveUnits(units);

      // Sync activeUnits array on user
      const user = this.getUser();
      if (isActive && !user.activeUnits.includes(unit.unitName)) {
        user.activeUnits.push(unit.unitName);
      } else if (!isActive) {
        user.activeUnits = user.activeUnits.filter((n) => n !== unit.unitName);
      }
      this.setUser(user);
    }
    return [...units];
  }

  static enrollUnitByName(unitName: string): Unit[] {
    const units = this.getUnits();
    const unit = units.find((u) => u.unitName.toLowerCase() === unitName.toLowerCase());
    if (unit) {
      unit.isActive = true;
      this.saveUnits(units);

      const user = this.getUser();
      if (!user.activeUnits.includes(unit.unitName)) {
        user.activeUnits.push(unit.unitName);
        this.setUser(user);
      }
    }
    return [...units];
  }

  // Quizzes and assessment
  static getQuizHistory(userId?: string): QuizHistoryItem[] {
    const all = safeGet<QuizHistoryItem[]>(STORAGE_KEYS.QUIZ_HISTORY, INITIAL_QUIZ_HISTORY);
    if (userId) {
      return all.filter((item) => item.userId === userId);
    }
    return all;
  }

  static recordQuizResult(result: Omit<QuizHistoryItem, 'id'>): QuizHistoryItem {
    const history = this.getQuizHistory();
    const newItem: QuizHistoryItem = {
      ...result,
      id: `hist-${Date.now()}`
    };
    history.unshift(newItem);
    safeSet(STORAGE_KEYS.QUIZ_HISTORY, history);
    return newItem;
  }

  static getQuizQuestions(unitName: string, topicName?: string): QuizQuestion[] {
    const bank = INITIAL_QUIZ_BANK[unitName] || [];
    if (topicName) {
      const matched = bank.filter(
        (q) => q.topicName && q.topicName.toLowerCase().includes(topicName.toLowerCase())
      );
      if (matched.length > 0) return matched;
    }
    return bank;
  }

  // Timetable
  static getTimetable(): TimetableSlot[] {
    return safeGet<TimetableSlot[]>(STORAGE_KEYS.TIMETABLE, INITIAL_TIMETABLE);
  }

  static saveTimetable(slots: TimetableSlot[]): void {
    safeSet(STORAGE_KEYS.TIMETABLE, slots);
  }

  // Bookmarks
  static getBookmarks(userId?: string): Bookmark[] {
    const all = safeGet<Bookmark[]>(STORAGE_KEYS.BOOKMARKS, [
      {
        id: 'b-1',
        userId: '1',
        subtopicId: 10001,
        subtopicName: 'Regulation of Phosphofructokinase-1 (PFK-1)',
        objectiveDescription: 'Rate-Limiting Step of Glycolysis',
        excerpt: 'Phosphofructokinase-1 (PFK-1) catalyzes the committed rate-limiting step of glycolysis...',
        timestamp: Date.now() - 86400000,
        notes: 'Review Fructose 2,6-bisphosphate mechanism before clinic exam'
      }
    ]);
    if (userId) {
      return all.filter((b) => b.userId === userId);
    }
    return all;
  }

  static addBookmark(bookmark: Omit<Bookmark, 'id' | 'timestamp'>): Bookmark {
    const bookmarks = this.getBookmarks();
    const newBookmark: Bookmark = {
      ...bookmark,
      id: `bm-${Date.now()}`,
      timestamp: Date.now()
    };
    bookmarks.unshift(newBookmark);
    safeSet(STORAGE_KEYS.BOOKMARKS, bookmarks);
    return newBookmark;
  }

  static deleteBookmark(id: string): Bookmark[] {
    const bookmarks = this.getBookmarks().filter((b) => b.id !== id);
    safeSet(STORAGE_KEYS.BOOKMARKS, bookmarks);
    return bookmarks;
  }

  // Chat sessions
  static getChatSessions(userId?: string): ChatSession[] {
    const all = safeGet<ChatSession[]>(STORAGE_KEYS.CHAT_SESSIONS, INITIAL_CHAT_SESSIONS);
    if (userId) {
      return all.filter((s) => s.userId === userId);
    }
    return all;
  }

  static saveChatSession(session: ChatSession): void {
    const sessions = this.getChatSessions();
    const index = sessions.findIndex((s) => s.id === session.id);
    if (index >= 0) {
      sessions[index] = session;
    } else {
      sessions.unshift(session);
    }
    safeSet(STORAGE_KEYS.CHAT_SESSIONS, sessions);
  }

  static deleteChatSession(id: string): ChatSession[] {
    const sessions = this.getChatSessions().filter((s) => s.id !== id);
    safeSet(STORAGE_KEYS.CHAT_SESSIONS, sessions);
    return sessions;
  }

  // Peer messages
  static getPeerMessages(userId: string, peerId: string): PeerMessage[] {
    const all = safeGet<PeerMessage[]>(STORAGE_KEYS.PEER_MESSAGES, [
      {
        id: 'pm-1',
        senderId: '2',
        senderName: 'Dr. Neema Ongaga',
        recipientId: '1',
        content: 'Hi Alex, excellent work on your ACS ECG differentials. Remember to review right ventricular lead V4R for tomorrow.',
        timestamp: Date.now() - 3600000 * 5
      }
    ]);
    return all.filter(
      (m) =>
        (m.senderId === userId && m.recipientId === peerId) ||
        (m.senderId === peerId && m.recipientId === userId)
    );
  }

  static sendPeerMessage(
    senderId: string,
    senderName: string,
    recipientId: string,
    content: string
  ): PeerMessage {
    const all = safeGet<PeerMessage[]>(STORAGE_KEYS.PEER_MESSAGES, []);
    const msg: PeerMessage = {
      id: `pm-${Date.now()}`,
      senderId,
      senderName,
      recipientId,
      content,
      timestamp: Date.now()
    };
    all.push(msg);
    safeSet(STORAGE_KEYS.PEER_MESSAGES, all);
    return msg;
  }

  // Teacher students
  static getTeacherStudents(): StudentProgressSummary[] {
    return safeGet<StudentProgressSummary[]>(
      STORAGE_KEYS.TEACHER_STUDENTS,
      INITIAL_TEACHER_STUDENTS
    );
  }

  // Zenith Insights
  static getZenithInsight(): string {
    return safeGet<string>(
      STORAGE_KEYS.ZENITH_INSIGHT,
      "**Trajectory Advisory**: You have completed **50% of your foundational biochemistry and acute abdomen modules**. Your latest PnL accuracy on coronary arterial localizations is **100%**. Recommended next focus: complete *Enzyme Kinetics & Lineweaver-Burk transformations* to consolidate metabolic rate-limiting mechanisms before Friday's diagnostic retrieval."
    );
  }

  static setZenithInsight(text: string): void {
    safeSet(STORAGE_KEYS.ZENITH_INSIGHT, text);
  }

  // Restore snapshot or reset demo state
  static restoreSnapshot(): number {
    safeSet(STORAGE_KEYS.UNITS, DEFAULT_UNITS);
    safeSet(STORAGE_KEYS.TIMETABLE, INITIAL_TIMETABLE);
    safeSet(STORAGE_KEYS.QUIZ_HISTORY, INITIAL_QUIZ_HISTORY);
    safeSet(STORAGE_KEYS.CHAT_SESSIONS, INITIAL_CHAT_SESSIONS);
    return DEFAULT_UNITS.length;
  }
}
