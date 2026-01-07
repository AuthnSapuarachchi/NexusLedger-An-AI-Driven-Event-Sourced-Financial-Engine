import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { ShieldAlert, CheckCircle, Clock, Send } from 'lucide-react';
import Stomp from 'stompjs';
import SockJS from 'sockjs-client/dist/sockjs';
import '../App.css'

const API_BASE = "http://localhost:8080/api/ledger";


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

      {/* User Info Section */}
      <div className="mb-8 bg-gradient-to-r from-green-900 to-green-800 p-6 rounded-xl border border-green-700">
        <h2 className="text-2xl font-bold mb-4">Welcome, {user.name}</h2>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="text-sm text-green-200">My Account ID</p>
            <p className="text-lg font-mono font-bold">{user.accountId}</p>
          </div>
          <div>
            <p className="text-sm text-green-200">Current Balance</p>
            <p className="text-lg font-bold">${user.balance}</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* FORM SECTION */}
        <section className="bg-slate-800 p-6 rounded-xl border border-slate-700 h-fit">
          <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
            <Send size={20}/> New Transfer
          </h2>
          <form onSubmit={handleTransfer} className="space-x-1 space-y-4">
              {/* FROM ACCOUNT - Auto-filled with logged-in user's account */}
              <input 
                placeholder="From Account UUID" 
                className="w-full bg-slate-900 border border-slate-700 p-3 rounded opacity-75 cursor-not-allowed"
                value={formData.fromId}
                disabled
                readOnly
              />

              {/* TO ACCOUNT - Fixed: Now updates toId and saves lastTo */}
              <input 
                placeholder="To Account UUID" 
                className="w-full bg-slate-900 border border-slate-700 p-3 rounded"
                value={formData.toId} 
                onChange={e => {
                  setFormData({...formData, toId: e.target.value});
                  localStorage.setItem('lastTo', e.target.value);
                }}
                required
              />

              {/* AMOUNT - Fixed: Now updates amount and does not overwrite IDs */}
              <input 
                type="number" 
                placeholder="Amount ($)" 
                className="w-full bg-slate-900 border border-slate-700 p-3 rounded"
                value={formData.amount}
                onChange={e => {
                  setFormData({...formData, amount: e.target.value});
                }}
                required
              />

              <button 
                disabled={submitting}
                className="w-full bg-blue-600 hover:bg-blue-500 py-3 rounded font-bold transition-all disabled:opacity-50"
              >
                {submitting ? "Processing..." : "Execute via Kafka"}
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
                <div key={tx.id} className={`bg-slate-900 p-4 rounded-lg border-l-4 flex justify-between items-center ${
                  tx.status === 'SUCCESS' ? 'border-green-500' : 
                  tx.status === 'FRAUD' ? 'border-red-500' : 'border-blue-500'
                }`}>
                  <div>
                    <p className="font-mono text-xs text-slate-400">ID: {tx.id}</p>
                    <p className="font-semibold">${tx.amount}</p>
                  </div>
                  <div className={`flex items-center gap-2 uppercase text-sm font-bold ${
                    tx.status === 'SUCCESS' ? 'text-green-400' : 
                    tx.status === 'FRAUD' ? 'text-red-400' : 'text-blue-400'
                  }`}>
                    {tx.status === 'SUCCESS' && <CheckCircle size={16}/>}
                    {tx.status === 'FRAUD' && <ShieldAlert size={16}/>}
                    {tx.status === 'QUEUED' && <Clock size={16}/>}
                    {tx.status}
                  </div>
                </div>
              ))}
          </div>
        </section>
      </div>
    </div>
  );
}

export default Dashboard
