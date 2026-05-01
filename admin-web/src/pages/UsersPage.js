import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { banUser, getUsers, unbanUser } from '../api/adminApi';
import { useRuntimeConfig } from '../context/RuntimeConfigContext';

export default function UsersPage() {
  const nav = useNavigate();
  const { config } = useRuntimeConfig();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');

  const load = () => {
    setLoading(true);
    getUsers(page, search)
      .then((response) => setUsers(response.data.data))
      .catch(() => setUsers([]))
      .finally(() => setLoading(false));
  };

  useEffect(load, [page, search]);

  const handleBan = async (id, banned) => {
    try {
      if (banned) await unbanUser(id);
      else await banUser(id);
      load();
    } catch (err) {
      alert(err.response?.data?.error?.message || 'Error');
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--brand-background, #FFF8F4)' }}>
      <div style={{ background: 'white', padding: '18px 28px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16, boxShadow: '0 6px 20px rgba(18, 32, 51, 0.06)', flexWrap: 'wrap' }}>
        <div>
          <h2 style={{ color: config.theme.primary, margin: 0 }}>{config.branding.appTitle} Admin</h2>
          <div style={{ color: '#667085', marginTop: 4 }}>User lifecycle, subscription, and referral attribution at a glance.</div>
        </div>
        <button onClick={() => nav('/dashboard')} style={{ padding: '10px 16px', background: 'white', border: '1px solid #D6DCE5', borderRadius: 999, cursor: 'pointer', fontWeight: 700 }}>Back to control plane</button>
      </div>
      <div style={{ maxWidth: 1400, margin: '0 auto', padding: 28, display: 'grid', gap: 18 }}>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search phone, email, name, or referral code"
            style={{ flex: '1 1 320px', padding: '12px 14px', borderRadius: 14, border: '1px solid #D8DEE8', fontSize: 14 }}
          />
        </div>
        {loading ? <p>Loading users...</p> : (
          <div style={{ background: 'white', borderRadius: 20, boxShadow: '0 18px 50px rgba(18, 32, 51, 0.08)', overflow: 'hidden' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ background: '#FAFAFB' }}>
                  {['Name', 'Phone/Email', 'Plan', 'Attribution', 'Status', 'Action'].map((heading) => (
                    <th key={heading} style={{ padding: '14px 16px', textAlign: 'left', fontSize: 12, fontWeight: 700, color: '#374151', textTransform: 'uppercase' }}>{heading}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {users.map((user, index) => (
                  <tr key={user.user_id} style={{ borderTop: '1px solid #F3F4F6', background: index % 2 === 0 ? 'white' : '#FCFCFD' }}>
                    <td style={{ padding: '14px 16px' }}>{user.first_name ? `${user.first_name} ${user.last_name || ''}`.trim() : 'N/A'}</td>
                    <td style={{ padding: '14px 16px' }}>{user.phone || user.email || 'N/A'}</td>
                    <td style={{ padding: '14px 16px', fontWeight: 700 }}>{(user.plan_id || 'free').toUpperCase()}</td>
                    <td style={{ padding: '14px 16px', color: '#667085' }}>{user.referred_by_code || user.acquisition_source || '-'}</td>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{ padding: '4px 10px', borderRadius: 999, fontSize: 11, fontWeight: 700, background: user.is_banned ? '#FEE2E2' : '#DDF6E7', color: user.is_banned ? '#B42318' : '#166534' }}>
                        {user.is_banned ? 'Banned' : 'Active'}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <button onClick={() => handleBan(user.user_id, user.is_banned)} style={{ padding: '8px 14px', border: 'none', borderRadius: 999, cursor: 'pointer', fontSize: 12, fontWeight: 700, background: user.is_banned ? '#0F8A5F' : '#B42318', color: 'white' }}>
                        {user.is_banned ? 'Restore' : 'Ban user'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div style={{ padding: '16px', display: 'flex', gap: 8, justifyContent: 'flex-end', alignItems: 'center' }}>
              <button onClick={() => setPage((current) => Math.max(1, current - 1))} disabled={page === 1} style={{ padding: '8px 16px', borderRadius: 999, border: '1px solid #E5E7EB', cursor: page === 1 ? 'not-allowed' : 'pointer', background: 'white' }}>Prev</button>
              <span style={{ padding: '8px 16px', color: '#667085' }}>Page {page}</span>
              <button onClick={() => setPage((current) => current + 1)} style={{ padding: '8px 16px', borderRadius: 999, border: '1px solid #E5E7EB', cursor: 'pointer', background: 'white' }}>Next</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
