import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { ShieldAlert, CheckCircle, Clock, Send } from 'lucide-react';

const API_BASE = "http://localhost:8080/api/ledger";

function App() {

  const [formData, setFormData] = useState({ fromId: '', toId: '', amount: '' });
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleTransfer = async (e) => {
    e.preventDefault();
    setLoading(true);
    // Generate a unique idempotency key for this click
    const idKey = `web-tx-${Date.now()}`;

    try {
      const res = await axios.post(`${API_BASE}/transfer`, formData, {
        headers: { 'X-Idempotency-Key': idKey }
      });
      
      // Add to local list immediately (Optimistic UI)
      const newTx = { id: idKey, ...formData, status: 'QUEUED', message: res.data.message };
      setTransactions([newTx, ...transactions]);
      alert("Transaction Sent to Kafka!");
    } catch (err) {
      alert("Error: " + err.response?.data?.error || "Server Down");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-white p-8 font-sans">
      <header className="mb-10 flex justify-between items-center border-b border-slate-700 pb-5">
        <h1 className="text-3xl font-bold tracking-tight text-blue-400">NEXUS <span className="text-white">LEDGER</span></h1>
        <div className="flex gap-4">
          <div className="bg-slate-800 p-3 rounded-lg border border-slate-700">
            <p className="text-xs text-slate-400">System Status</p>
            <p className="text-green-400 font-mono">● KAFKA_ONLINE</p>
          </div>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* FORM SECTION */}
        <section className="bg-slate-800 p-6 rounded-xl border border-slate-700 h-fit">
          <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
            <Send size={20}/> New Transfer
          </h2>
          <form onSubmit={handleTransfer} className="space-x-1 space-y-4">
            <input 
              placeholder="From Account UUID" 
              className="w-full bg-slate-900 border border-slate-700 p-3 rounded"
              onChange={e => setFormData({...formData, fromId: e.target.value})}
              required
            />
            <input 
              placeholder="To Account UUID" 
              className="w-full bg-slate-900 border border-slate-700 p-3 rounded"
              onChange={e => setFormData({...formData, toId: e.target.value})}
              required
            />
            <input 
              type="number" 
              placeholder="Amount ($)" 
              className="w-full bg-slate-900 border border-slate-700 p-3 rounded"
              onChange={e => setFormData({...formData, amount: e.target.value})}
              required
            />
            <button 
              disabled={loading}
              className="w-full bg-blue-600 hover:bg-blue-500 py-3 rounded font-bold transition-all disabled:opacity-50"
            >
              {loading ? "Processing..." : "Execute via Kafka"}
            </button>
          </form>
        </section>

        {/* AUDIT LOG SECTION */}
        <section className="lg:col-span-2 bg-slate-800 p-6 rounded-xl border border-slate-700">
          <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
            <Clock size={20}/> Live Transaction Audit (Kafka Streams)
          </h2>
          <div className="space-y-4">
            {transactions.length === 0 && <p className="text-slate-500 italic text-center py-10">No transactions recorded in this session.</p>}
            {transactions.map(tx => (
              <div key={tx.id} className="bg-slate-900 p-4 rounded-lg border-l-4 border-blue-500 flex justify-between items-center">
                <div>
                  <p className="font-mono text-xs text-slate-400">ID: {tx.id}</p>
                  <p className="font-semibold">${tx.amount} <span className="text-slate-400 text-sm">from {tx.fromId.substring(0,8)}...</span></p>
                </div>
                <div className="flex items-center gap-2 text-blue-400 uppercase text-sm font-bold">
                  <Clock size={16}/> Queued
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}

export default App
