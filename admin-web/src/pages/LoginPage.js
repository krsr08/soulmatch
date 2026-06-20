import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminLogin } from '../api/adminApi';
import { Icon } from '../components/AdminPrimitives';
import { useRuntimeConfig } from '../context/RuntimeConfigContext';

export default function LoginPage() {
  const { config } = useRuntimeConfig();
  const theme = config.theme || {};
  const branding = config.branding || {};
  const primary = theme.primary || '#6814d9';
  const secondary = theme.secondary || '#8138ee';
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
      gridTemplateColumns: 'minmax(420px, 500px) minmax(420px, 500px)',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 34,
      background: 'radial-gradient(circle at 14% 18%, rgba(104,20,217,0.08), transparent 30%), radial-gradient(circle at 85% 82%, rgba(0,108,159,0.08), transparent 32%), #f7f9fc',
      padding: 32,
      fontFamily: '"Inter", "DM Sans", "Segoe UI", system-ui, sans-serif'
    },
    shell: {
      width: 'min(100%, 480px)',
      justifySelf: 'center'
    },
    logo: {
      width: 64,
      height: 64,
      display: 'grid',
      placeItems: 'center',
      margin: '0 auto 20px',
      border: '1px solid #dce4ef',
      borderRadius: 16,
      background: 'white',
      color: primary,
      boxShadow: '0 10px 24px rgba(15,23,42,0.06)'
    },
    card: {
      background: 'white',
      border: '1px solid #dce4ef',
      borderRadius: 12,
      padding: 34,
      boxShadow: '0 20px 60px rgba(15,23,42,0.08)'
    },
    inputWrap: {
      position: 'relative'
    },
    inputIcon: {
      position: 'absolute',
      left: 14,
      top: 17,
      color: '#6c7280'
    },
    input: {
      width: '100%',
      padding: '13px 14px 13px 44px',
      border: '1px solid #d8e1ee',
      borderRadius: 6,
      fontSize: 14,
      boxSizing: 'border-box',
      marginTop: 6,
      background: '#fbfdff',
      color: '#071426'
    },
    label: {
      fontSize: 12,
      fontWeight: 850,
      color: '#2d2939',
      letterSpacing: '0.04em'
    },
    button: {
      width: '100%',
      padding: 14,
      background: `linear-gradient(135deg, ${primary}, ${secondary})`,
      color: 'white',
      border: 'none',
      borderRadius: 7,
      fontSize: 15,
      fontWeight: 850,
      cursor: loading ? 'wait' : 'pointer',
      marginTop: 12,
      boxShadow: '0 14px 30px rgba(104,20,217,0.22)'
    },
    error: {
      background: '#fee2e2',
      border: '1px solid #ef4444',
      color: '#dc2626',
      padding: '10px 14px',
      borderRadius: 8,
      marginBottom: 16,
      fontSize: 14
    },
    hero: {
      minHeight: 520,
      display: 'grid',
      alignContent: 'end',
      overflow: 'hidden',
      position: 'relative',
      borderRadius: 22,
      background: `radial-gradient(circle at 42% 42%, rgba(255,210,204,0.46) 0 8%, transparent 9% 100%), linear-gradient(145deg, #07111d, #121a21 46%, ${primary})`,
      color: 'white',
      padding: 34,
      boxShadow: '0 28px 70px rgba(15,23,42,0.20)'
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.shell}>
        <div style={styles.logo}><Icon name="heart" /></div>
        <h2 style={{ textAlign: 'center', color: '#071426', margin: '0 0 6px', fontSize: 28, fontWeight: 900 }}>
          {branding.appTitle || 'SoulMatch'} Admin
        </h2>
        <p style={{ textAlign: 'center', color: '#4d5567', margin: '0 0 28px' }}>Command Center Authentication</p>
        <div style={styles.card}>
          {branding.logoUrl ? (
            <img
              src={branding.logoUrl}
              alt={branding.appTitle}
              style={{ width: 54, height: 54, borderRadius: 16, display: 'block', margin: '0 auto 14px', objectFit: 'cover' }}
            />
          ) : null}
          {error ? <div style={styles.error}>{error}</div> : null}
          <form onSubmit={submit}>
            <div>
              <label style={styles.label}>Administrator Email</label>
              <div style={styles.inputWrap}>
                <span style={styles.inputIcon}><Icon name="mail" /></span>
                <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} style={styles.input} required />
              </div>
            </div>
            <div style={{ marginTop: 18 }}>
              <label style={styles.label}>Secure Password</label>
              <div style={styles.inputWrap}>
                <span style={styles.inputIcon}><Icon name="lock" /></span>
                <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} style={styles.input} required />
              </div>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '18px 0 10px', fontSize: 13 }}>
              <label style={{ display: 'inline-flex', alignItems: 'center', gap: 8, color: '#2d2939' }}>
                <input type="checkbox" />
                Remember me
              </label>
              <span style={{ color: primary, fontWeight: 850 }}>Forgot password?</span>
            </div>
            <button type="submit" style={styles.button} disabled={loading}>
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>
          <div style={{ height: 1, background: '#e8eef6', margin: '26px 0 18px' }} />
          <p style={{ textAlign: 'center', fontSize: 11, color: '#8b92a1', margin: 0 }}>Secure environment active</p>
        </div>
        <p style={{ textAlign: 'center', fontSize: 13, color: '#4d5567', marginTop: 24 }}>
          Technical issues? <strong style={{ color: primary }}>Contact System Ops</strong>
        </p>
      </div>

      <section style={styles.hero}>
        <div aria-hidden="true" style={{ position: 'absolute', inset: 0, background: 'radial-gradient(circle at 28% 28%, rgba(255,255,255,0.32), transparent 8%), radial-gradient(circle at 64% 43%, rgba(255,212,200,0.42), transparent 9%), radial-gradient(circle at 82% 30%, rgba(255,255,255,0.2), transparent 5%)' }} />
        <div aria-hidden="true" style={{ position: 'absolute', left: -40, right: -40, bottom: 110, height: 95, background: 'linear-gradient(180deg, transparent, rgba(255,190,150,0.26), rgba(255,255,255,0.08))', transform: 'skewY(-5deg)' }} />
        <div>
          <h1 style={{ position: 'relative', margin: 0, fontSize: 23, lineHeight: 1.15, textShadow: '0 2px 8px rgba(0,0,0,0.32)' }}>Internal Operations Portal</h1>
          <p style={{ position: 'relative', maxWidth: 380, color: 'rgba(255,255,255,0.88)', lineHeight: 1.55, marginBottom: 0 }}>
            Access restricted member matching algorithms and regional moderation controls. Authorized personnel only.
          </p>
        </div>
      </section>
    </div>
  );
}
