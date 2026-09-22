import React, { useState } from 'react';
import {
  Users,
  Send,
  MessageSquare,
  UserCheck,
  Sparkles,
  Award
} from 'lucide-react';
import { User, PeerMessage } from '../types';
import { TraceStore } from '../lib/store';

interface ConnectTabProps {
  user: User;
}

export const ConnectTab: React.FC<ConnectTabProps> = ({ user }) => {
  const peers = [
    { id: '2', name: 'Dr. Neema Ongaga', role: 'Faculty Mentor', status: 'Online' },
    { id: '102', name: 'Grace Naliaka', role: 'Clinical Peer', status: 'Study Mode' },
    { id: '103', name: 'Rayvins Otieno', role: 'Pre-med Peer', status: 'In Assessment' }
  ];

  const [selectedPeer, setSelectedPeer] = useState(peers[0]);
  const [messages, setMessages] = useState<PeerMessage[]>(
    TraceStore.getPeerMessages(user.id, selectedPeer.id)
  );
  const [content, setContent] = useState('');

  const handleSelectPeer = (peer: typeof peers[0]) => {
    setSelectedPeer(peer);
    setMessages(TraceStore.getPeerMessages(user.id, peer.id));
  };

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;

    const newMsg = TraceStore.sendPeerMessage(user.id, user.username, selectedPeer.id, content.trim());
    setMessages((prev) => [...prev, newMsg]);
    setContent('');
  };

  return (
    <div className="h-[calc(100vh-140px)] flex flex-col sm:flex-row gap-4 pb-4 animate-in fade-in duration-200">
      {/* Peers List */}
      <div className="w-full sm:w-72 bg-slate-900/80 border border-slate-800 rounded-3xl p-4 flex flex-col shadow-xl">
        <div className="flex items-center gap-2 px-2 py-3 border-b border-slate-800 mb-3">
          <Users className="w-4 h-4 text-sky-400" />
          <h3 className="text-xs font-bold uppercase tracking-wider text-white">Study Connect & Mentors</h3>
        </div>

        <div className="space-y-1 overflow-y-auto flex-1">
          {peers.map((peer) => (
            <button
              key={peer.id}
              onClick={() => handleSelectPeer(peer)}
              className={`w-full p-3 rounded-2xl text-left flex items-center gap-3 transition-colors ${
                selectedPeer.id === peer.id
                  ? 'bg-sky-500/20 text-white border border-sky-500/30'
                  : 'text-slate-300 hover:bg-slate-800/60'
              }`}
            >
              <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center font-bold text-xs text-sky-400">
                {peer.name.charAt(0)}
              </div>
              <div className="truncate flex-1">
                <p className="text-xs font-bold truncate">{peer.name}</p>
                <p className="text-[10px] text-slate-400">{peer.role} • {peer.status}</p>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Direct Messaging Area */}
      <div className="flex-1 bg-slate-900/80 border border-slate-800 rounded-3xl flex flex-col shadow-xl overflow-hidden backdrop-blur-md">
        <div className="p-4 border-b border-slate-800 bg-slate-950/60 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full bg-sky-500/20 text-sky-300 flex items-center justify-center text-xs font-bold">
              {selectedPeer.name.charAt(0)}
            </div>
            <div>
              <h4 className="text-xs font-bold text-white">{selectedPeer.name}</h4>
              <p className="text-[10px] text-slate-400">{selectedPeer.role}</p>
            </div>
          </div>
          <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
            Encrypted Channel
          </span>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto p-6 space-y-3">
          {messages.map((m) => {
            const isMe = m.senderId === user.id;
            return (
              <div key={m.id} className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[75%] p-3.5 rounded-2xl text-xs leading-relaxed ${
                    isMe
                      ? 'bg-indigo-600 text-white shadow-sm'
                      : 'bg-slate-950 border border-slate-800 text-slate-200'
                  }`}
                >
                  <p>{m.content}</p>
                  <span className="text-[9px] text-slate-400 block text-right mt-1 opacity-75">
                    {new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Send Bar */}
        <form onSubmit={handleSend} className="p-4 border-t border-slate-800 bg-slate-950/60 flex items-center gap-2">
          <input
            type="text"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder={`Message ${selectedPeer.name}...`}
            className="flex-1 bg-slate-900 border border-slate-800 rounded-2xl px-4 py-2.5 text-xs text-white placeholder-slate-500 outline-none focus:border-sky-500 transition-colors"
          />
          <button
            type="submit"
            disabled={!content.trim()}
            className="p-2.5 rounded-2xl bg-sky-600 hover:bg-sky-500 text-white disabled:opacity-40 transition-colors"
          >
            <Send className="w-4 h-4" />
          </button>
        </form>
      </div>
    </div>
  );
};
