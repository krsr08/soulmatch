import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminLogin } from '../api/adminApi';
import { useRuntimeConfig } from '../context/RuntimeConfigContext';

export default function LoginPage() {
  const { config } = useRuntimeConfig();
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
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: `linear-gradient(135deg, ${config.theme.primary}, ${config.theme.secondary})`,
      padding: 24
    },
    card: {
      background: 'white',
      borderRadius: 24,
      padding: 40,
      width: 420,
      boxShadow: '0 20px 60px rgba(0,0,0,0.2)'
    },
    input: {
      width: '100%',
      padding: '12px 14px',
      border: '1px solid #E5E7EB',
      borderRadius: 12,
      fontSize: 14,
      boxSizing: 'border-box',
      marginTop: 6
    },
    button: {
      width: '100%',
      padding: 12,
      background: config.theme.primary,
      color: 'white',
      border: 'none',
      borderRadius: 12,
      fontSize: 16,
      fontWeight: 700,
      cursor: loading ? 'wait' : 'pointer',
      marginTop: 8
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
      <div style={styles.card}>
        {config.branding.logoUrl ? (
          <img
            src={config.branding.logoUrl}
            alt={config.branding.appTitle}
            style={{ width: 64, height: 64, borderRadius: 20, display: 'block', margin: '0 auto 14px', objectFit: 'cover' }}
          />
        ) : null}
        <h2 style={{ textAlign: 'center', color: config.theme.primary, marginBottom: 8 }}>{config.branding.appTitle} Admin</h2>
        <p style={{ textAlign: 'center', color: '#6B7280', marginBottom: 24 }}>Owner control plane</p>
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
  );
}
