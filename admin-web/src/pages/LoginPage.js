import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminLogin } from '../api/adminApi';
import { useRuntimeConfig } from '../context/RuntimeConfigContext';

export default function LoginPage() {
  const { config } = useRuntimeConfig();
  const theme = config.theme || {};
  const branding = config.branding || {};
  const primary = theme.primary || '#6f3a2b';
  const secondary = theme.secondary || '#b47a62';
  const [email, setEmail] = useState('admin@soulmatch.app');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const res = await adminLogin({ email, password });
      localStorage.setItem('adminToken', res.data.data.token);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.error?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const styles = {
    page: {
      minHeight: '100vh',
      display: 'grid',
      gridTemplateColumns: 'minmax(320px, 0.9fr) minmax(360px, 1.1fr)',
      alignItems: 'center',
      justifyContent: 'center',
      background: '#fff7f2',
      padding: 28,
      fontFamily: '"DM Sans", "Segoe UI", system-ui, sans-serif'
    },
    hero: {
      minHeight: 560,
      display: 'grid',
      alignContent: 'space-between',
      borderRadius: 24,
      background: `linear-gradient(145deg, ${primary}, #261006)`,
      color: 'white',
      padding: 34,
      boxShadow: '0 24px 70px rgba(44,24,16,0.22)'
    },
    shell: {
      width: 'min(100%, 520px)',
      justifySelf: 'center'
    },
    card: {
      background: 'white',
      border: '1px solid #ead7ce',
      borderRadius: 18,
      padding: 32,
      boxShadow: '0 20px 60px rgba(44,24,16,0.12)'
    },
    input: {
      width: '100%',
      padding: '11px 13px',
      border: '1px solid #e3cec4',
      borderRadius: 10,
      fontSize: 14,
      boxSizing: 'border-box',
      marginTop: 6,
      background: '#fffaf7'
    },
    button: {
      width: '100%',
      padding: 12,
      background: primary,
      color: 'white',
      border: 'none',
      borderRadius: 10,
      fontSize: 14,
      fontWeight: 700,
      cursor: loading ? 'wait' : 'pointer',
      marginTop: 10
    },
    error: {
      background: '#FEE2E2',
      border: '1px solid #EF4444',
      color: '#DC2626',
      padding: '10px 14px',
      borderRadius: 12,
      marginBottom: 16,
      fontSize: 14
    }
  };

  return (
    <div style={styles.page}>
      <section style={styles.hero}>
        <div>
          <div style={{ display: 'inline-grid', placeItems: 'center', width: 48, height: 48, borderRadius: 14, background: 'rgba(255,255,255,0.12)', marginBottom: 22 }}>♡</div>
          <h1 style={{ margin: 0, fontSize: 36, lineHeight: 1.05 }}>{branding.appTitle || 'SoulMatch'} Admin</h1>
          <p style={{ maxWidth: 420, color: 'rgba(255,255,255,0.74)', lineHeight: 1.6 }}>Secure command center for members, agents, revenue, moderation, configuration and business growth.</p>
        </div>
        <div style={{ display: 'grid', gap: 12 }}>
          {['Role-protected access', 'Audit-ready actions', 'Runtime configuration control'].map((item) => (
            <div key={item} style={{ display: 'flex', alignItems: 'center', gap: 10, color: 'rgba(255,255,255,0.86)' }}>
              <span style={{ width: 8, height: 8, borderRadius: 999, background: secondary }} />
              <strong>{item}</strong>
            </div>
          ))}
        </div>
      </section>
      <div style={styles.shell}>
        <div style={styles.card}>
        {branding.logoUrl ? (
          <img
            src={branding.logoUrl}
            alt={branding.appTitle}
            style={{ width: 54, height: 54, borderRadius: 16, display: 'block', margin: '0 auto 14px', objectFit: 'cover' }}
          />
        ) : null}
        <h2 style={{ textAlign: 'center', color: primary, margin: '0 0 6px', fontSize: 24 }}>{branding.appTitle || 'SoulMatch'} Admin</h2>
        <p style={{ textAlign: 'center', color: '#8a6b5e', margin: '0 0 24px' }}>Command Center Authentication</p>
        {error ? <div style={styles.error}>{error}</div> : null}
        <form onSubmit={submit}>
          <div>
            <label style={{ fontSize: 12, fontWeight: 700, color: '#374151' }}>Email</label>
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} style={styles.input} required />
          </div>
          <div style={{ marginTop: 16 }}>
            <label style={{ fontSize: 12, fontWeight: 700, color: '#374151' }}>Password</label>
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} style={styles.input} required />
          </div>
          <button type="submit" style={styles.button} disabled={loading}>
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>
        <p style={{ textAlign: 'center', fontSize: 11, color: '#9CA3AF', marginTop: 16 }}>
          Use the admin credentials configured for this environment.
        </p>
        </div>
      </div>
    </div>
  );
}
