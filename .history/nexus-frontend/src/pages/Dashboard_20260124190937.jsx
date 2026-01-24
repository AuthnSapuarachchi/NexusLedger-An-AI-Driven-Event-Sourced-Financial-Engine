import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { ShieldAlert, CheckCircle, Clock, Send, LogOut, Plus, Wallet } from 'lucide-react';
import Stomp from 'stompjs';
import SockJS from 'sockjs-client/dist/sockjs';
import '../App.css'

const API_BASE = "http://localhost:8080/api/ledger";

const Dashboard = () => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [formData, setFormData] = useState({
    fromId: '',
    toId: localStorage.getItem('lastTo') || '',
    amount: ''
  });
  const [transactions, setTransactions] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [historyError, setHistoryError] = useState(null);
  const [newVaultName, setNewVaultName] = useState('');
  const [creatingVault, setCreatingVault] = useState(false);

  // --- 1. FUNCTION: Fetch History from Database ---
  const fetchHistory = async () => {
    try {
      console.log("Fetching transaction history...");
      const res = await axios.get(`${API_BASE}/history`, { withCredentials: true });
      console.log("Raw history response:", res.data);
      
      if (!res.data || res.data.length === 0) {
        console.log("No transaction history found");
        setTransactions([]);
        return;
      }
      
      const history = res.data.map(record => {
        console.log("Processing record:", record);
        return {
          id: record.idempotencyKey || record.id,
          status: record.statusCode === 200 ? 'SUCCESS' : 
                  record.statusCode === 403 ? 'FRAUD' : 'ERROR',
          amount: record.amount,
          fromId: record.fromId,
          toId: record.toId
        };
      });
      
      console.log("Processed history:", history);
      setTransactions(history);
      setHistoryError(null);
    } catch (err) {
      console.error("Could not load history", err);
      console.error("Error details:", err.response?.data);
      setHistoryError(err.response?.data?.message || "Failed to load transaction history");
    }
  };

  // --- 2. EFFECT: Initial Login & User Load ---
  useEffect(() => {
    axios.get('http://localhost:8080/api/user/me', { withCredentials: true })
      .then(response => {
        console.log("User data received:", response.data);
        const userData = response.data;
        
        // Set user state with proper data structure
        setUser({
          name: userData.name,
          email: userData.email,
          accountId: userData.accountId,
          balance: userData.balance,
          accounts: userData.accounts
        });
        
        // Set fromId in form
        if (userData.accountId) {
          setFormData(prev => ({ ...prev, fromId: userData.accountId }));
        }
        
        setLoading(false);
      })
      .catch(err => {
        console.log("Not logged in, redirecting...");
        window.location.href = "http://localhost:8080/oauth2/authorization/github";
      });
  }, []);

  // --- 3. EFFECT: WebSocket & History Load ---
  useEffect(() => {
    if (!user || !user.accountId) return;

    // Load history once user is confirmed
    fetchHistory();

    // Setup WebSocket
    const socket = new SockJS('http://localhost:8080/ws-ledger');
    const client = Stomp.over(socket);
    client.debug = () => {}; // Silence logs

    client.connect({}, () => {
      console.log("Connected to WebSocket");

      // SUBSCRIBE: Balance Updates (Match the type from your Java Consumer)
      client.subscribe(`/topic/updates/${user.accountId}`, (message) => {
        const data = JSON.parse(message.body);
        console.log("Received WS Update:", data);

        // Update Balance
        if (data.newBalance !== null && data.newBalance !== undefined) {
          setUser(prev => ({ ...prev, balance: data.newBalance }));
        }
        
        // Refresh the table to show the new SUCCESS/FRAUD record
        fetchHistory();
      });
    }, (err) => {
      console.error("STOMP error", err);
    });

    return () => {
      if (client.connected) client.disconnect();
    };
  }, [user?.accountId]); // Only re-run if accountId changes

  // --- 4. FUNCTION: Handle Transfer ---
  const handleTransfer = async (e) => {
    e.preventDefault();
    setSubmitting(true);

    const idKey = `web-tx-${Date.now()}`;
    const amt = parseFloat(formData.amount);

    if (isNaN(amt) || amt <= 0) {
      alert("Please enter a valid amount");
      setSubmitting(false);
      return;
    }

    const payload = {
      fromId: String(formData.fromId).trim(),
      toId: String(formData.toId).trim(),
      amount: amt
    };

    try {
      const res = await axios.post(`${API_BASE}/transfer`, payload, {
        headers: { 'X-Idempotency-Key': idKey },
        withCredentials: true
      });

      // Optimistic Update: Add to UI as QUEUED
      const newTx = { 
        id: idKey, 
        ...payload, 
        status: 'QUEUED' 
      };
      setTransactions(prev => [newTx, ...prev]);
      alert("Transaction Sent to Kafka!");
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.response?.data?.error || "Connection Error";
      alert("Error: " + errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  // --- 5. FUNCTION: Handle Create Vault ---
  const handleCreateVault = async (e) => {
    e.preventDefault();
    
    if (!newVaultName.trim()) {
      alert("Please enter a vault name");
      return;
    }

    setCreatingVault(true);
    
    try {
      await axios.post(`${API_BASE}/accounts/create?vaultName=${encodeURIComponent(newVaultName)}`, {}, {
        withCredentials: true
      });
      
      alert(`Vault "${newVaultName}" created successfully!`);
      setNewVaultName('');
      
      // Refresh user data to get updated accounts list
      const response = await axios.get('http://localhost:8080/api/user/me', { withCredentials: true });
      const userData = response.data;
      setUser({
        name: userData.name,
        email: userData.email,
        accountId: userData.accountId,
        balance: userData.balance,
        accounts: userData.accounts
      });
      
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.response?.data || "Failed to create vault";
      alert("Error: " + errorMessage);
    } finally {
      setCreatingVault(false);
    }
  };

  if (loading) return (
    <div className="min-h-screen bg-slate-900 text-white flex items-center justify-center">
      <div className="text-xl">Securing Session...</div>
    </div>
  );

  const handleLogout = async () => {
    try {
        console.log("Logging out...");
        // Call backend logout with credentials
        await axios.post('http://localhost:8080/api/user/logout', {}, { 
          withCredentials: true 
        });
        
        // Clear local storage
        localStorage.removeItem('lastTo');
        
        // Redirect to OAuth login
        window.location.href = "http://localhost:8080/oauth2/authorization/github";
    } catch (err) {
        console.error("Logout failed", err);
        // Force clear and redirect anyway
        localStorage.clear();
        window.location.href = "http://localhost:8080/oauth2/authorization/github";
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-white p-8 font-sans">
      <header className="mb-10 flex justify-between items-center border-b border-slate-700 pb-5">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-blue-400">
            NEXUS <span className="text-white">LEDGER</span>
          </h1>
          <p className="text-slate-400 mt-1">Welcome back, {user.name}</p>
        </div>
        
        <div className="flex gap-4 items-center">
          <div className="bg-slate-800 p-3 rounded-lg border border-slate-700">
            <p className="text-xs text-slate-400">System Status</p>
            <p className="text-green-400 font-mono">● KAFKA_ONLINE</p>
          </div>
          
          <button 
            onClick={handleLogout}
            className="bg-red-600/20 hover:bg-red-600 text-red-500 hover:text-white px-4 py-2 rounded-lg border border-red-600/50 transition-all flex items-center gap-2"
          >
            <LogOut size={18} /> Logout
          </button>
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
          
          {historyError && (
            <div className="bg-red-900/30 border border-red-500 p-4 rounded-lg mb-4">
              <p className="text-red-400">⚠️ {historyError}</p>
            </div>
          )}
          
          <div className="space-y-4">
            {transactions.length === 0 && !historyError && (
              <p className="text-slate-500 italic text-center py-10">
                No transactions recorded in this session.
              </p>
            )}
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
