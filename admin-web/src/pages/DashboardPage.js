import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { io } from 'socket.io-client';
import {
  ADMIN_SOCKET_URL,
  acknowledgeAlert,
  approveVerification,
  bulkCreateProfiles,
  createCampaign,
  createProfile,
  createRefund,
  deleteProfile,
  getAlerts,
  getAnalyticsEvents,
  getAnalyticsFunnel,
  getAuditLogs,
  getConfig,
  getDashboard,
  getPayments,
  getProfiles,
  getRealtimeSnapshot,
  getReports,
  getRoles,
  getServiceHealth,
  getUsers,
  getVerifications,
  rejectVerification,
  resolveReport,
  updateConfig,
  updateProfile,
  updateProfileStatus
} from '../api/adminApi';
import { useRuntimeConfig } from '../context/RuntimeConfigContext';
import './DashboardPage.css';

const EMPTY_STATS = {
  totalUsers: 0,
  totalProfiles: 0,
  activeUsers: 0,
  activeProfiles: 0,
  pendingApprovals: 0,
  premiumUsers: 0,
  pendingReports: 0,
  totalRevenue: 0,
  revenue30d: 0,
  dau: 0,
  mau: 0,
  conversionRate: 0,
  matchSuccessRate: 0,
  analytics: { signups: 0, paymentClicks: 0, paymentSuccesses: 0, matchesMade: 0 }
};

const DEFAULT_CONFIG = {
  branding: { appTitle: 'SoulMatch', tagline: 'Serious matchmaking for modern families', logoUrl: '', squareLogoUrl: '', previewImageUrl: '', shareBaseUrl: '' },
  theme: {},
  content: { auth: {}, home: {}, phoneEntry: {}, navigation: {}, support: {} },
  legal: { terms: { sections: [] }, privacy: { sections: [] } },
  feature_flags: {},
  matching: { weights: {}, indiaFilters: {} },
  registration: {},
  monetization: { currency: 'INR', plans: [], premiumLimits: {}, upgradePackageGroups: [] },
  notification_templates: {},
  maintenance: {},
  security: {},
  localization: {},
  client_integrations: { googleWebClientId: '', razorpayKeyId: '', supportEmail: 'support@soulmatch.app' }
};

const TAB_ROLES = {
  overview: ['super_admin', 'admin', 'moderator', 'support_agent', 'marketing_manager'],
  profiles: ['super_admin', 'admin', 'moderator', 'support_agent'],
  verification: ['super_admin', 'admin', 'moderator'],
  payments: ['super_admin', 'admin'],
  cms: ['super_admin', 'admin', 'marketing_manager'],
  engagement: ['super_admin', 'admin', 'marketing_manager'],
  moderation: ['super_admin', 'admin', 'moderator'],
  config: ['super_admin', 'admin'],
  rbac: ['super_admin'],
  analytics: ['super_admin', 'admin', 'moderator', 'marketing_manager'],
  health: ['super_admin', 'admin']
};

const TABS = [
  { id: 'overview', label: 'Dashboard', glyph: 'DB' },
  { id: 'profiles', label: 'Profiles', glyph: 'PR' },
  { id: 'verification', label: 'Verification', glyph: 'KY' },
  { id: 'payments', label: 'Payments', glyph: 'IN' },
  { id: 'cms', label: 'CMS', glyph: 'CM' },
  { id: 'engagement', label: 'Engagement', glyph: 'NT' },
  { id: 'moderation', label: 'Moderation', glyph: 'MD' },
  { id: 'config', label: 'Dynamic Config', glyph: 'CF' },
  { id: 'rbac', label: 'RBAC', glyph: 'RB' },
  { id: 'analytics', label: 'Analytics', glyph: 'AN' },
  { id: 'health', label: 'Health', glyph: 'HL' }
];

function compactNumber(value) {
  const n = Number(value || 0);
  if (!Number.isFinite(n)) return '0';
  if (n >= 10000000) return `${(n / 10000000).toFixed(1)}Cr`;
  if (n >= 100000) return `${(n / 100000).toFixed(1)}L`;
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`;
  return String(Math.round(n));
}

function money(value) {
  return `INR ${compactNumber(value)}`;
}

function editableNumber(value) {
  return value === undefined || value === null ? '' : String(value);
}

function parseEditableNumber(value, fallback = 0) {
  if (String(value).trim() === '') return '';
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function normalizeEditableNumber(value, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function formatDateTime(value) {
  if (!value) return 'Waiting for first live update';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
}

function decodeSession() {
  try {
    const token = localStorage.getItem('adminToken');
    if (!token) return { role: 'guest', email: '' };
    const payload = JSON.parse(window.atob(token.split('.')[1]));
    return { token, role: payload.role || 'admin', email: payload.email || '', permissions: payload.permissions || [] };
  } catch (_) {
    return { role: 'guest', email: '' };
  }
}

function normalizeConfig(config) {
  return {
    ...DEFAULT_CONFIG,
    ...(config || {}),
    branding: { ...DEFAULT_CONFIG.branding, ...(config?.branding || {}) },
    theme: { ...DEFAULT_CONFIG.theme, ...(config?.theme || {}) },
    content: { ...DEFAULT_CONFIG.content, ...(config?.content || {}) },
    legal: { ...DEFAULT_CONFIG.legal, ...(config?.legal || {}) },
    monetization: { ...DEFAULT_CONFIG.monetization, ...(config?.monetization || {}) },
    matching: { ...DEFAULT_CONFIG.matching, ...(config?.matching || {}) },
    client_integrations: { ...DEFAULT_CONFIG.client_integrations, ...(config?.client_integrations || {}) }
  };
}

function mergeRealtimeStats(current, snapshot) {
  if (!snapshot) return current;
  return {
    ...current,
    totalUsers: snapshot.totalUsers ?? current.totalUsers,
    totalProfiles: snapshot.totalProfiles ?? current.totalProfiles,
    activeUsers: snapshot.activeUsers ?? snapshot.liveUsers ?? current.activeUsers,
    activeProfiles: snapshot.activeProfiles ?? current.activeProfiles,
    pendingApprovals: snapshot.pendingApprovals ?? current.pendingApprovals,
    premiumUsers: snapshot.premiumUsers ?? current.premiumUsers,
    pendingReports: snapshot.pendingReports ?? current.pendingReports,
    newUsersToday: snapshot.newUsersToday ?? current.newUsersToday,
    totalRevenue: snapshot.totalRevenue ?? current.totalRevenue,
    revenue30d: snapshot.revenue30d ?? current.revenue30d,
    dau: snapshot.dau ?? current.dau,
    mau: snapshot.mau ?? current.mau,
    conversionRate: snapshot.conversionRate ?? current.conversionRate,
    matchSuccessRate: snapshot.matchSuccessRate ?? current.matchSuccessRate,
    analytics: {
      ...(current.analytics || EMPTY_STATS.analytics),
      ...(snapshot.analytics || {})
    }
  };
}

function parseCsv(text) {
  const lines = String(text || '').split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  if (lines.length < 2) return [];
  const headers = lines[0].split(',').map((item) => item.trim());
  return lines.slice(1).map((line) => {
    const values = line.split(',').map((item) => item.trim());
    return headers.reduce((row, header, index) => ({ ...row, [header]: values[index] || '' }), {});
  });
}

function StatusBadge({ tone = 'neutral', children }) {
  return <span className={`status-badge ${tone}`}>{children}</span>;
}

function Field({ label, children, hint }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint ? <small>{hint}</small> : null}
    </label>
  );
}

function TextInput(props) {
  return <input {...props} className="input" />;
}

function TextArea(props) {
  return <textarea {...props} className="textarea" />;
}

function JsonEditor({ value, onValidChange, placeholder = '[]' }) {
  const [text, setText] = useState(JSON.stringify(value || [], null, 2));
  const [valid, setValid] = useState(true);

  useEffect(() => {
    setText(JSON.stringify(value || [], null, 2));
  }, [value]);

  return (
    <>
      <TextArea
        value={text}
        placeholder={placeholder}
        onChange={(event) => {
          const next = event.target.value;
          setText(next);
          try {
            onValidChange(JSON.parse(next));
            setValid(true);
          } catch (_) {
            setValid(false);
          }
        }}
      />
      {!valid ? <small className="field-error">Invalid JSON. Fix it before saving.</small> : null}
    </>
  );
}

function Section({ eyebrow, title, detail, actions, children }) {
  return (
    <section className="panel-section">
      <div className="section-header">
        <div>
          {eyebrow ? <div className="eyebrow">{eyebrow}</div> : null}
          <h2>{title}</h2>
          {detail ? <p>{detail}</p> : null}
        </div>
        {actions ? <div className="section-actions">{actions}</div> : null}
      </div>
      {children}
    </section>
  );
}

function MetricCard({ label, value, sub, tone = 'primary' }) {
  return (
    <div className={`metric-card ${tone}`}>
      <div className="metric-icon">{label.slice(0, 2).toUpperCase()}</div>
      <span>{label}</span>
      <strong>{value}</strong>
      {sub ? <small>{sub}</small> : null}
    </div>
  );
}

function Table({ columns, rows, empty = 'No records found.' }) {
  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>{columns.map((column) => <th key={column.key}>{column.label}</th>)}</tr>
        </thead>
        <tbody>
          {rows.length ? rows.map((row, index) => (
            <tr key={row.id || row.profile_id || row.user_id || row.transaction_id || row.audit_id || index}>
              {columns.map((column) => <td key={column.key}>{column.render ? column.render(row) : row[column.key]}</td>)}
            </tr>
          )) : (
            <tr><td colSpan={columns.length}>{empty}</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function SimpleBarChart({ rows }) {
  const max = Math.max(...rows.map((row) => Number(row.value || 0)), 1);
  return (
    <div className="bar-chart">
      {rows.map((row) => (
        <div className="bar-row" key={row.label}>
          <span>{row.label}</span>
          <div><i style={{ width: `${(Number(row.value || 0) / max) * 100}%` }} /></div>
          <strong>{compactNumber(row.value)}</strong>
        </div>
      ))}
    </div>
  );
}

function SimpleFunnel({ rows }) {
  const first = Number(rows[0]?.value || 0) || 1;
  return (
    <div className="simple-funnel">
      {rows.map((row, index) => {
        const value = Number(row.value || 0);
        const percent = index === 0 ? 100 : Math.round((value / first) * 100);
        return (
          <div className="funnel-row" key={row.label}>
            <div>
              <strong>{row.label}</strong>
              <span>{percent}% of sign ups</span>
            </div>
            <b>{compactNumber(value)}</b>
          </div>
        );
      })}
    </div>
  );
}

function Sidebar({ tabs, activeTab, setActiveTab }) {
  return (
    <aside className="sidebar">
      <div className="suite-title">
        <div>SM</div>
        <span>SoulMatch Control</span>
      </div>
      <nav>
        {tabs.map((tab) => (
          <button className={activeTab === tab.id ? 'active' : ''} key={tab.id} onClick={() => setActiveTab(tab.id)} type="button">
            <b>{tab.glyph}</b>
            {tab.label}
          </button>
        ))}
      </nav>
    </aside>
  );
}

function TopBar({ brand, role, email, live, onSync, onLogout }) {
  return (
    <header className="topbar">
      <div>
        <strong>{brand} Admin</strong>
        <span>Real-time control panel for matrimony operations</span>
      </div>
      <div className="top-actions">
        <StatusBadge tone="ok">{compactNumber(live.liveUsers)} live</StatusBadge>
        <StatusBadge tone={live.fraudAlerts > 0 ? 'danger' : 'neutral'}>{compactNumber(live.fraudAlerts)} alerts</StatusBadge>
        <span className="role-pill">{role.replace('_', ' ')}</span>
        <button className="secondary-btn" onClick={onSync} type="button">Sync</button>
        <button className="avatar-btn" title={email} onClick={onLogout} type="button">{(email || 'A').slice(0, 1).toUpperCase()}</button>
      </div>
    </header>
  );
}

function OverviewPanel({ stats, live, analyticsRows, alerts, setActiveTab }) {
  const pulseRows = [
    { label: 'Live users', value: live.liveUsers || stats.activeUsers || 0 },
    { label: 'Pending KYC', value: live.pendingApprovals || stats.pendingApprovals || 0 },
    { label: 'Payments today', value: live.paymentsToday || 0 },
    { label: 'Matches today', value: live.matchesToday || 0 },
    { label: 'Open reports', value: live.pendingReports || stats.pendingReports || 0 }
  ];
  const funnelRows = [
    { label: 'Sign ups', value: stats.analytics?.signups || 0 },
    { label: 'Payment clicks', value: stats.analytics?.paymentClicks || 0 },
    { label: 'Paid users', value: stats.analytics?.paymentSuccesses || 0 },
    { label: 'Matches made', value: stats.analytics?.matchesMade || 0 }
  ];
  return (
    <>
      <div className="metric-grid">
        <MetricCard label="Profiles" value={compactNumber(stats.totalProfiles || stats.activeProfiles)} sub={`${compactNumber(stats.activeProfiles)} published / ${compactNumber(stats.totalUsers)} members`} />
        <MetricCard label="Live Users" value={compactNumber(live.liveUsers || stats.activeUsers)} sub="Last 15 minutes" tone="green" />
        <MetricCard label="Pending KYC" value={compactNumber(live.pendingApprovals || stats.pendingApprovals)} sub="Approvals queue" tone="amber" />
        <MetricCard label="Revenue" value={money(stats.revenue30d || stats.totalRevenue)} sub="30 day view" />
        <MetricCard label="Matches Today" value={compactNumber(live.matchesToday || stats.analytics?.matchesMade)} sub={`${stats.matchSuccessRate || 0}% success`} tone="green" />
        <MetricCard label="Reports" value={compactNumber(live.pendingReports || stats.pendingReports)} sub="Open abuse queue" tone="danger" />
      </div>
      <div className="two-col">
        <Section eyebrow="Realtime" title="Platform Pulse" detail="Socket updates are pushed from the admin service every few seconds.">
          <div className="live-grid">
            <StatusBadge tone="ok">{compactNumber(live.liveUsers)} users online</StatusBadge>
            <StatusBadge tone="amber">{compactNumber(live.pendingApprovals)} approval queue</StatusBadge>
            <StatusBadge tone="ok">{money(live.revenueToday)} today</StatusBadge>
            <StatusBadge tone={live.pendingReports ? 'danger' : 'neutral'}>{compactNumber(live.pendingReports)} reports</StatusBadge>
          </div>
          <small className="live-timestamp">Last live update: {formatDateTime(live.generatedAt)}</small>
          <SimpleBarChart rows={pulseRows} />
        </Section>
        <Section eyebrow="Alerts" title="Risk Feed" detail="Fraud spikes, abuse reports, and operational warnings.">
          <div className="event-list">
            {alerts.slice(0, 5).map((alert) => (
              <button key={alert.alert_id || alert.title} className="event-card" type="button" onClick={() => setActiveTab('moderation')}>
                <StatusBadge tone={alert.severity === 'critical' || alert.severity === 'high' ? 'danger' : 'amber'}>{alert.severity || 'medium'}</StatusBadge>
                <strong>{alert.title}</strong>
                <span>{alert.body || alert.source || 'Needs review'}</span>
              </button>
            ))}
            {!alerts.length ? <div className="empty-state">No live alerts.</div> : null}
          </div>
        </Section>
      </div>
      <Section eyebrow="Insights" title="Conversion Funnel" detail="Simple 30-day funnel from app analytics events.">
        <div className="insight-grid compact">
          <MetricCard label="DAU" value={compactNumber(stats.dau)} />
          <MetricCard label="MAU" value={compactNumber(stats.mau)} />
          <MetricCard label="Paid Conversion" value={`${stats.conversionRate || 0}%`} />
          <MetricCard label="Recent Events" value={compactNumber(analyticsRows.length)} />
        </div>
        <SimpleFunnel rows={funnelRows} />
      </Section>
    </>
  );
}

function ProfilesPanel({
  profiles,
  profileMeta,
  profileFilters,
  setProfileFilters,
  profileForm,
  setProfileForm,
  photoForm,
  setPhotoForm,
  csvText,
  setCsvText,
  onCreate,
  onAttachPhoto,
  onBulk,
  onStatus,
  onBulkStatus,
  onDelete
}) {
  const columns = [
    { key: 'name', label: 'Profile', render: (p) => (
      <div className="profile-cell">
        <img src={p.primary_photo_url || 'https://placehold.co/96x96/fff0f3/a90f3f?text=SM'} alt="" />
        <div><strong>{`${p.first_name || ''} ${p.last_name || ''}`.trim() || 'Unnamed'}</strong><small>{p.phone || p.email || p.user_id}</small></div>
      </div>
    ) },
    { key: 'community', label: 'Community', render: (p) => `${p.religion || '-'} / ${p.caste || '-'}` },
    { key: 'location', label: 'Location', render: (p) => p.working_city || p.family_city || '-' },
    { key: 'job', label: 'Profession', render: (p) => p.occupation || '-' },
    { key: 'status', label: 'Status', render: (p) => (
      <div className="status-stack">
        <StatusBadge tone={p.is_published ? 'ok' : p.admin_status === 'suspended' ? 'danger' : 'amber'}>{p.admin_status || (p.is_published ? 'published' : 'pending')}</StatusBadge>
        <small>{p.verification_status === 'verified' ? 'Verified' : 'Verification pending'}</small>
      </div>
    ) },
    { key: 'link', label: 'Link', render: (p) => <a href={`${ADMIN_SOCKET_URL}/share/profile/${p.profile_id}`} target="_blank" rel="noreferrer">Open</a> },
    { key: 'actions', label: 'Actions', render: (p) => (
      <div className="inline-actions">
        <button onClick={() => onStatus(p.profile_id, 'approve')} type="button">Approve + verify</button>
        <button onClick={() => onStatus(p.profile_id, 'suspend')} type="button">Suspend</button>
        <button className="danger-link" onClick={() => onDelete(p.profile_id)} type="button">Delete</button>
      </div>
    ) }
  ];
  return (
    <>
      <Section eyebrow="Profile operations" title="Search and Manage Profiles" detail="Admin CRUD, approval, suspension, and India-specific filters.">
        <div className="filter-grid">
          {['search', 'religion', 'caste', 'location', 'profession'].map((key) => (
            <Field key={key} label={key}>
              <TextInput value={profileFilters[key] || ''} onChange={(event) => setProfileFilters({ ...profileFilters, page: 1, [key]: event.target.value })} />
            </Field>
          ))}
          <Field label="status">
            <select className="input" value={profileFilters.status} onChange={(event) => setProfileFilters({ ...profileFilters, page: 1, status: event.target.value })}>
              <option value="all">All</option>
              <option value="published">Published</option>
              <option value="pending">Pending</option>
              <option value="suspended">Suspended</option>
            </select>
          </Field>
          <Field label="rows per page">
            <select className="input" value={profileFilters.limit} onChange={(event) => setProfileFilters({ ...profileFilters, page: 1, limit: Number(event.target.value) })}>
              <option value={25}>25</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </Field>
        </div>
        <Table columns={columns} rows={profiles} />
        <div className="table-footer">
          <div className="inline-actions">
            <button disabled={(profileMeta.page || 1) <= 1} onClick={() => setProfileFilters({ ...profileFilters, page: Math.max(1, Number(profileFilters.page || 1) - 1) })} type="button">Prev</button>
            <span>Page {profileMeta.page || profileFilters.page || 1} of {profileMeta.totalPages || 1}</span>
            <button disabled={(profileMeta.page || 1) >= (profileMeta.totalPages || 1)} onClick={() => setProfileFilters({ ...profileFilters, page: Number(profileFilters.page || 1) + 1 })} type="button">Next</button>
          </div>
          <span className="table-count">Showing {profiles.length} of {compactNumber(profileMeta.total || profiles.length)} profiles</span>
          <div className="inline-actions">
            <button onClick={() => onBulkStatus('approve')} type="button">Approve + verify visible</button>
            <button onClick={() => onBulkStatus('suspend')} type="button">Suspend visible</button>
          </div>
        </div>
      </Section>
      <div className="two-col">
        <Section eyebrow="Manual creation" title="Create Backend Profile" detail="Support staff can create a complete profile without using the mobile app.">
          <div className="form-grid">
            {[
              ['firstName', 'First name'], ['lastName', 'Last name'], ['phone', 'Mobile'], ['email', 'Email'],
              ['gender', 'Gender'], ['religion', 'Religion'], ['caste', 'Caste'], ['motherTongue', 'Mother tongue'],
              ['occupation', 'Profession'], ['workingCity', 'Working city'], ['educationLevel', 'Education'], ['familyCity', 'Family city']
            ].map(([key, label]) => (
              <Field key={key} label={label}>
                <TextInput value={profileForm[key] || ''} onChange={(event) => setProfileForm({ ...profileForm, [key]: event.target.value })} />
              </Field>
            ))}
            <Field label="About" hint="Shown on profile detail.">
              <TextArea value={profileForm.aboutMe || ''} onChange={(event) => setProfileForm({ ...profileForm, aboutMe: event.target.value })} />
            </Field>
          </div>
          <button className="primary-btn" onClick={onCreate} type="button">Create profile</button>
        </Section>
        <Section eyebrow="Bulk upload" title="CSV Profile Import" detail="Paste CSV with headers like firstName,lastName,phone,religion,caste,workingCity.">
          <TextArea value={csvText} onChange={(event) => setCsvText(event.target.value)} placeholder="firstName,lastName,phone,religion,caste,workingCity&#10;Priya,Rao,9876543210,Hindu,Telugu,Hyderabad" />
          <button className="primary-btn" onClick={onBulk} type="button">Import CSV</button>
        </Section>
      </div>
      <Section eyebrow="Photos" title="Upload or Replace Profile Photo" detail="Attach a primary profile image URL for admin-created or corrected profiles. This updates the same image used by the mobile app profile cards.">
        <div className="form-grid">
          <Field label="Profile ID">
            <TextInput value={photoForm.profileId} onChange={(event) => setPhotoForm({ ...photoForm, profileId: event.target.value })} placeholder="UUID from the profiles table" />
          </Field>
          <Field label="Image URL">
            <TextInput type="url" value={photoForm.primaryPhotoUrl} onChange={(event) => setPhotoForm({ ...photoForm, primaryPhotoUrl: event.target.value })} placeholder="https://..." />
          </Field>
        </div>
        <button className="primary-btn" onClick={onAttachPhoto} type="button">Update profile photo</button>
      </Section>
    </>
  );
}

function VerificationPanel({ verifications, onApprove, onReject }) {
  const columns = [
    { key: 'member', label: 'Member', render: (v) => <><strong>{`${v.first_name || ''} ${v.last_name || ''}`.trim() || v.user_id}</strong><small>{v.phone}</small></> },
    { key: 'type', label: 'Type' },
    { key: 'document_url', label: 'Document', render: (v) => v.document_url ? <a href={v.document_url} target="_blank" rel="noreferrer">Open</a> : '-' },
    { key: 'status', label: 'Status', render: (v) => <StatusBadge tone="amber">{v.status}</StatusBadge> },
    { key: 'actions', label: 'Actions', render: (v) => <div className="inline-actions"><button onClick={() => onApprove(v.verification_id)} type="button">Approve</button><button onClick={() => onReject(v.verification_id)} type="button">Reject</button></div> }
  ];
  return (
    <Section eyebrow="KYC workflow" title="Document Review Queue" detail="Approve or reject identity, photo, and document verification requests.">
      <Table columns={columns} rows={verifications} empty="No pending verification requests." />
    </Section>
  );
}

function PaymentsPanel({ payments, config, patchConfig, onSave, onRefund }) {
  const plans = config.monetization?.plans || [];
  const upgradePackageGroups = config.monetization?.upgradePackageGroups || [];
  const transactions = payments.transactions || [];
  return (
    <>
      <Section eyebrow="Plans" title="Subscription and Pricing" detail="Pricing changes are stored in runtime config and reflected without redeploy.">
        <div className="plan-grid">
          {plans.map((plan, index) => (
            <div className="plan-card" key={plan.planId}>
              <Field label="Name"><TextInput value={plan.name || ''} onChange={(event) => {
                const next = [...plans];
                next[index] = { ...plan, name: event.target.value };
                patchConfig('monetization', { ...config.monetization, plans: next });
              }} /></Field>
              <Field label="Price"><TextInput inputMode="numeric" value={editableNumber(plan.price)} onChange={(event) => {
                const next = [...plans];
                next[index] = { ...plan, price: parseEditableNumber(event.target.value, plan.price || 0) };
                patchConfig('monetization', { ...config.monetization, plans: next });
              }} onBlur={() => {
                const next = [...plans];
                next[index] = { ...plan, price: normalizeEditableNumber(plan.price, 0) };
                patchConfig('monetization', { ...config.monetization, plans: next });
              }} /></Field>
              <small>{(plan.features || []).join(', ')}</small>
            </div>
          ))}
        </div>
        <button className="primary-btn" onClick={() => onSave('monetization')} type="button">Save pricing</button>
      </Section>
      <Section eyebrow="App membership" title="Upgrade Packages Used by the Mobile App" detail="This JSON feeds payment/upgrade-packages, so the Android membership screen and admin pricing stay aligned.">
        <JsonEditor value={upgradePackageGroups} onValidChange={(packages) => patchConfig('monetization', { ...config.monetization, upgradePackageGroups: packages })} />
        <button className="primary-btn" onClick={() => onSave('monetization')} type="button">Save app membership packages</button>
      </Section>
      <Section eyebrow="Payments" title="Transactions and Refunds" detail="Track payment status, gateway ids, and refund requests.">
        <Table
          rows={transactions}
          columns={[
            { key: 'member', label: 'Member', render: (t) => `${t.first_name || ''} ${t.last_name || ''}`.trim() || t.phone || t.user_id },
            { key: 'plan_id', label: 'Plan' },
            { key: 'amount', label: 'Amount', render: (t) => money(t.amount) },
            { key: 'status', label: 'Status', render: (t) => <StatusBadge tone={['paid', 'success', 'captured'].includes(t.status) ? 'ok' : 'amber'}>{t.status}</StatusBadge> },
            { key: 'actions', label: 'Actions', render: (t) => <button onClick={() => onRefund(t)} type="button">Refund</button> }
          ]}
        />
      </Section>
    </>
  );
}

function CmsPanel({ config, patchConfig, onSave }) {
  const branding = config.branding || {};
  const content = config.content || {};
  const legal = config.legal || {};
  const updateBranding = (patch) => patchConfig('branding', { ...branding, ...patch });
  const updateContent = (section, patch) => patchConfig('content', { ...content, [section]: { ...(content[section] || {}), ...patch } });
  const updateLegal = (section, key, value) => patchConfig('legal', { ...legal, [section]: { ...(legal[section] || {}), [key]: value } });
  const pageSections = [
    ['auth', 'Login page'],
    ['phoneEntry', 'Mobile number page'],
    ['home', 'Home dashboard'],
    ['navigation', 'Bottom navigation'],
    ['support', 'Support copy']
  ];
  return (
    <>
    <div className="two-col">
      <Section eyebrow="Branding" title="Login Image and App Identity" detail="Control the login hero image and brand copy without a mobile release. Leave preview image URL blank to use the bundled Indian wedding image.">
        <Field label="App title"><TextInput value={branding.appTitle || ''} onChange={(e) => updateBranding({ appTitle: e.target.value })} /></Field>
        <Field label="Tagline"><TextArea value={branding.tagline || ''} onChange={(e) => updateBranding({ tagline: e.target.value })} /></Field>
        <Field label="Login preview image URL"><TextInput value={branding.previewImageUrl || ''} onChange={(e) => updateBranding({ previewImageUrl: e.target.value })} /></Field>
        <Field label="Logo URL"><TextInput value={branding.logoUrl || ''} onChange={(e) => updateBranding({ logoUrl: e.target.value })} /></Field>
        <Field label="Square logo URL"><TextInput value={branding.squareLogoUrl || ''} onChange={(e) => updateBranding({ squareLogoUrl: e.target.value })} /></Field>
        <button className="primary-btn" onClick={() => onSave('branding')} type="button">Save branding</button>
      </Section>
      <Section eyebrow="CMS" title="Mobile App Content" detail="Login, home, phone entry, navigation, and support text.">
        <Field label="Login hero"><TextInput value={content.auth?.heroTitle || ''} onChange={(e) => updateContent('auth', { heroTitle: e.target.value })} /></Field>
        <Field label="Login subtitle"><TextArea value={content.auth?.heroSubtitle || ''} onChange={(e) => updateContent('auth', { heroSubtitle: e.target.value })} /></Field>
        <Field label="Home header"><TextArea value={content.home?.headerSubtitle || ''} onChange={(e) => updateContent('home', { headerSubtitle: e.target.value })} /></Field>
        <Field label="Support email"><TextInput value={content.support?.email || ''} onChange={(e) => updateContent('support', { email: e.target.value })} /></Field>
        <button className="primary-btn" onClick={() => onSave('content')} type="button">Save content</button>
      </Section>
      <Section eyebrow="Static pages" title="Terms and Privacy" detail="Configurable legal content for app links.">
        <Field label="Terms title"><TextInput value={legal.terms?.title || ''} onChange={(e) => updateLegal('terms', 'title', e.target.value)} /></Field>
        <Field label="Privacy title"><TextInput value={legal.privacy?.title || ''} onChange={(e) => updateLegal('privacy', 'title', e.target.value)} /></Field>
        <Field label="Terms sections JSON"><JsonEditor value={legal.terms?.sections || []} onValidChange={(value) => updateLegal('terms', 'sections', value)} /></Field>
        <Field label="Privacy sections JSON"><JsonEditor value={legal.privacy?.sections || []} onValidChange={(value) => updateLegal('privacy', 'sections', value)} /></Field>
        <button className="primary-btn" onClick={() => onSave('legal')} type="button">Save legal pages</button>
      </Section>
    </div>
    <Section eyebrow="CMS" title="Page-wise Static Content" detail="Every static text block used by the mobile app is editable here by page. Changes are exposed through public runtime config without rebuilding the app.">
      <div className="cms-page-grid">
        {pageSections.map(([key, title]) => (
          <div className="template-card" key={key}>
            <strong>{title}</strong>
            <JsonEditor value={content[key] || {}} onValidChange={(value) => patchConfig('content', { ...content, [key]: value })} placeholder="{}" />
          </div>
        ))}
      </div>
      <button className="primary-btn" onClick={() => onSave('content')} type="button">Save page content</button>
    </Section>
    </>
  );
}

function EngagementPanel({ config, patchConfig, onSave, campaign, setCampaign, onCampaign }) {
  const templates = config.notification_templates || {};
  return (
    <div className="two-col">
      <Section eyebrow="Templates" title="Notification Templates" detail="Push, SMS, email, and match alert text.">
        {Object.keys(templates).map((key) => (
          <div className="template-card" key={key}>
            <strong>{key}</strong>
            <TextInput value={templates[key]?.title || ''} onChange={(e) => patchConfig('notification_templates', { ...templates, [key]: { ...templates[key], title: e.target.value } })} />
            <TextArea value={templates[key]?.body || ''} onChange={(e) => patchConfig('notification_templates', { ...templates, [key]: { ...templates[key], body: e.target.value } })} />
          </div>
        ))}
        <button className="primary-btn" onClick={() => onSave('notification_templates')} type="button">Save templates</button>
      </Section>
      <Section eyebrow="Campaigns" title="Create Engagement Campaign" detail="Draft a campaign for match alerts, offers, or winback journeys.">
        <Field label="Name"><TextInput value={campaign.name} onChange={(e) => setCampaign({ ...campaign, name: e.target.value })} /></Field>
        <Field label="Channel"><select className="input" value={campaign.channel} onChange={(e) => setCampaign({ ...campaign, channel: e.target.value })}><option>push</option><option>email</option><option>sms</option></select></Field>
        <Field label="Audience JSON"><TextArea value={campaign.audienceText} onChange={(e) => setCampaign({ ...campaign, audienceText: e.target.value })} /></Field>
        <button className="primary-btn" onClick={onCampaign} type="button">Create campaign draft</button>
      </Section>
    </div>
  );
}

function ModerationPanel({ reports, alerts, onResolve, onAck }) {
  return (
    <div className="two-col">
      <Section eyebrow="Reports" title="Reported Users" detail="Abuse, fake profile, and safety concern queue.">
        <Table
          rows={reports}
          columns={[
            { key: 'reason', label: 'Reason' },
            { key: 'description', label: 'Concern' },
            { key: 'reported_phone', label: 'Reported' },
            { key: 'status', label: 'Status', render: (r) => <StatusBadge tone="amber">{r.status}</StatusBadge> },
            { key: 'actions', label: 'Actions', render: (r) => <button onClick={() => onResolve(r.report_id)} type="button">Resolve</button> }
          ]}
        />
      </Section>
      <Section eyebrow="Security" title="Fraud and Abuse Alerts" detail="Realtime alerts from admin-service and future ML detectors.">
        <div className="event-list">
          {alerts.map((alert) => (
            <button className="event-card" key={alert.alert_id || alert.title} onClick={() => alert.alert_id && onAck(alert.alert_id)} type="button">
              <StatusBadge tone={alert.severity === 'critical' || alert.severity === 'high' ? 'danger' : 'amber'}>{alert.severity}</StatusBadge>
              <strong>{alert.title}</strong>
              <span>{alert.body}</span>
            </button>
          ))}
          {!alerts.length ? <div className="empty-state">No alerts.</div> : null}
        </div>
      </Section>
    </div>
  );
}

function ConfigPanel({ config, patchConfig, onSave }) {
  const flags = config.feature_flags || {};
  const matching = config.matching || { weights: {}, indiaFilters: {} };
  const registration = config.registration || {};
  const security = config.security || {};
  const clientIntegrations = config.client_integrations || {};
  const updateWeight = (key, value) => patchConfig('matching', { ...matching, weights: { ...(matching.weights || {}), [key]: parseEditableNumber(value, matching.weights?.[key] || 0) } });
  const normalizeWeight = (key) => patchConfig('matching', { ...matching, weights: { ...(matching.weights || {}), [key]: normalizeEditableNumber(matching.weights?.[key], 0) } });
  return (
    <>
      <Section eyebrow="Feature flags" title="Dynamic Configuration" detail="These values change app behavior without redeploy.">
        <div className="toggle-grid">
          {Object.keys(flags).map((key) => (
            <label className="toggle-row" key={key}>
              <span><strong>{key}</strong><small>Runtime feature flag</small></span>
              <input type="checkbox" checked={Boolean(flags[key])} onChange={(e) => patchConfig('feature_flags', { ...flags, [key]: e.target.checked })} />
            </label>
          ))}
        </div>
        <button className="primary-btn" onClick={() => onSave('feature_flags')} type="button">Save feature flags</button>
      </Section>
      <Section eyebrow="Client SDK keys" title="Public App Integrations" detail="These are public client identifiers used by the Android app. Keep real secrets like OAuth client secret, Razorpay secret, JWT secret, and Firebase service account keys on the server or in a vault.">
        <div className="form-grid">
          <Field label="Google Web Client ID">
            <TextInput value={clientIntegrations.googleWebClientId || ''} onChange={(e) => patchConfig('client_integrations', { ...clientIntegrations, googleWebClientId: e.target.value })} />
          </Field>
          <Field label="Razorpay Key ID">
            <TextInput value={clientIntegrations.razorpayKeyId || ''} onChange={(e) => patchConfig('client_integrations', { ...clientIntegrations, razorpayKeyId: e.target.value })} />
          </Field>
          <Field label="Support email">
            <TextInput value={clientIntegrations.supportEmail || ''} onChange={(e) => patchConfig('client_integrations', { ...clientIntegrations, supportEmail: e.target.value })} />
          </Field>
        </div>
        <div className="empty-state subtle">
          Public identifiers can change from the control panel. Private provider secrets must never be sent to the mobile app.
        </div>
        <button className="primary-btn" onClick={() => onSave('client_integrations')} type="button">Save app integrations</button>
      </Section>
      <div className="two-col">
        <Section eyebrow="Match engine" title="Algorithm Weights" detail="Tune India-specific match scoring weights.">
          <div className="form-grid">
            {Object.keys(matching.weights || {}).map((key) => (
              <Field key={key} label={key}>
                <TextInput inputMode="numeric" value={editableNumber(matching.weights[key])} onChange={(e) => updateWeight(key, e.target.value)} onBlur={() => normalizeWeight(key)} />
              </Field>
            ))}
          </div>
          <button className="primary-btn" onClick={() => onSave('matching')} type="button">Save matching</button>
        </Section>
        <Section eyebrow="Safety" title="Registration and Security" detail="Rate limits, KYC gates, PII masking, and supported regions.">
          <Field label="OTP limit per phone"><TextInput inputMode="numeric" value={editableNumber(registration.dailyOtpLimitPerPhone)} onChange={(e) => patchConfig('registration', { ...registration, dailyOtpLimitPerPhone: parseEditableNumber(e.target.value, registration.dailyOtpLimitPerPhone || 0) })} onBlur={() => patchConfig('registration', { ...registration, dailyOtpLimitPerPhone: normalizeEditableNumber(registration.dailyOtpLimitPerPhone, 0) })} /></Field>
          <Field label="Minimum age"><TextInput inputMode="numeric" value={editableNumber(registration.minimumAge || 18)} onChange={(e) => patchConfig('registration', { ...registration, minimumAge: parseEditableNumber(e.target.value, registration.minimumAge || 18) })} onBlur={() => patchConfig('registration', { ...registration, minimumAge: normalizeEditableNumber(registration.minimumAge, 18) })} /></Field>
          <Field label="API rate limit/minute"><TextInput inputMode="numeric" value={editableNumber(security.apiRateLimitPerMinute)} onChange={(e) => patchConfig('security', { ...security, apiRateLimitPerMinute: parseEditableNumber(e.target.value, security.apiRateLimitPerMinute || 0) })} onBlur={() => patchConfig('security', { ...security, apiRateLimitPerMinute: normalizeEditableNumber(security.apiRateLimitPerMinute, 0) })} /></Field>
          <button className="primary-btn" onClick={() => Promise.all([onSave('registration'), onSave('security')])} type="button">Save limits</button>
        </Section>
      </div>
    </>
  );
}

function RbacPanel({ roles, session }) {
  return (
    <Section eyebrow="RBAC" title="Role-Based Access Control" detail="UI sections and admin APIs are scoped by admin role.">
      <div className="role-grid">
        {roles.map((role) => (
          <div className="role-card" key={role.role}>
            <strong>{role.label}</strong>
            <StatusBadge tone={session.role === role.role ? 'ok' : 'neutral'}>{role.role}</StatusBadge>
            <small>{(role.permissions || []).join(', ')}</small>
          </div>
        ))}
      </div>
    </Section>
  );
}

function AnalyticsPanel({ funnel, events, auditLogs }) {
  const chartRows = funnel.map((item) => ({ label: item.event_type, value: item.total }));
  return (
    <>
      <Section eyebrow="Funnel" title="Analytics and Insights" detail="DAU/MAU, conversion, match success, and event pipeline.">
        <SimpleBarChart rows={chartRows.length ? chartRows : [{ label: 'No events', value: 0 }]} />
      </Section>
      <Section eyebrow="Audit" title="Admin Action Logs" detail="Every sensitive admin action is retained for compliance.">
        <Table
          rows={auditLogs}
          columns={[
            { key: 'admin_email', label: 'Admin' },
            { key: 'action', label: 'Action' },
            { key: 'entity_type', label: 'Entity' },
            { key: 'created_at', label: 'Time', render: (r) => String(r.created_at || '').replace('T', ' ').slice(0, 19) }
          ]}
        />
      </Section>
      <Section eyebrow="Events" title="Product Events" detail="Latest application and service events.">
        <Table
          rows={events}
          columns={[
            { key: 'event_type', label: 'Event' },
            { key: 'service_name', label: 'Service' },
            { key: 'user_id', label: 'User' },
            { key: 'created_at', label: 'Time', render: (r) => String(r.created_at || '').replace('T', ' ').slice(0, 19) }
          ]}
        />
      </Section>
    </>
  );
}

function HealthPanel({ services }) {
  return (
    <Section eyebrow="Observability" title="Service Health" detail="Each microservice is checked independently so failures are visible without blocking the admin panel.">
      <div className="service-grid">
        {services.map((service) => (
          <div className="service-card" key={service.key || service.label}>
            <StatusBadge tone={service.ok ? 'ok' : 'danger'}>{service.ok ? 'online' : 'offline'}</StatusBadge>
            <strong>{service.label || service.key}</strong>
            <small>{service.url}</small>
            <span>{service.latencyMs || 0}ms {service.status ? `/ HTTP ${service.status}` : ''}</span>
          </div>
        ))}
      </div>
    </Section>
  );
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const { config: runtimeConfig, refresh: refreshRuntime } = useRuntimeConfig();
  const session = useMemo(decodeSession, []);
  const visibleTabs = useMemo(() => TABS.filter((tab) => TAB_ROLES[tab.id]?.includes(session.role)), [session.role]);
  const [activeTab, setActiveTab] = useState(visibleTabs[0]?.id || 'overview');
  const [stats, setStats] = useState(EMPTY_STATS);
  const [live, setLive] = useState({ generatedAt: '', liveUsers: 0, pendingApprovals: 0, paymentsToday: 0, revenueToday: 0, matchesToday: 0, pendingReports: 0, fraudAlerts: 0 });
  const [configState, setConfigState] = useState(DEFAULT_CONFIG);
  const [profiles, setProfiles] = useState([]);
  const [profileMeta, setProfileMeta] = useState({ page: 1, limit: 25, total: 0, totalPages: 1 });
  const [profileFilters, setProfileFilters] = useState({ page: 1, limit: 25, search: '', religion: '', caste: '', location: '', profession: '', status: 'all' });
  const [profileForm, setProfileForm] = useState({ gender: 'female', religion: 'Hindu', maritalStatus: 'never_married' });
  const [photoForm, setPhotoForm] = useState({ profileId: '', primaryPhotoUrl: '' });
  const [csvText, setCsvText] = useState('');
  const [verifications, setVerifications] = useState([]);
  const [payments, setPayments] = useState({ transactions: [], plans: [], coupons: [] });
  const [reports, setReports] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [roles, setRoles] = useState([]);
  const [funnel, setFunnel] = useState([]);
  const [events, setEvents] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [services, setServices] = useState([]);
  const [users, setUsers] = useState([]);
  const [campaign, setCampaign] = useState({ name: '', channel: 'push', audienceText: '{"plan":"free"}' });
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [moduleIssues, setModuleIssues] = useState([]);

  const patchConfig = (key, value) => setConfigState((current) => ({ ...current, [key]: value }));

  const loadProfiles = useCallback(async () => {
    const response = await getProfiles(profileFilters);
    setProfiles(response.data.data || []);
    setProfileMeta(response.data.meta || { page: profileFilters.page, limit: profileFilters.limit, total: 0, totalPages: 1 });
  }, [profileFilters]);

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError('');
    setMessage('');
    setModuleIssues([]);
    const requestDefs = [
      { key: 'dashboard', label: 'Dashboard metrics', critical: true, request: getDashboard() },
      { key: 'realtime', label: 'Realtime metrics', request: getRealtimeSnapshot() },
      { key: 'config', label: 'Runtime config', critical: true, request: getConfig() },
      { key: 'profiles', label: 'Profiles', request: getProfiles(profileFilters) },
      { key: 'verifications', label: 'Verification queue', request: getVerifications() },
      { key: 'payments', label: 'Payments', request: getPayments() },
      { key: 'reports', label: 'Reports', request: getReports() },
      { key: 'alerts', label: 'Alerts', request: getAlerts() },
      { key: 'roles', label: 'Roles', request: getRoles() },
      { key: 'funnel', label: 'Funnel analytics', request: getAnalyticsFunnel() },
      { key: 'events', label: 'Recent events', request: getAnalyticsEvents(100) },
      { key: 'auditLogs', label: 'Audit logs', request: getAuditLogs(100) },
      { key: 'services', label: 'Service health', request: getServiceHealth() },
      { key: 'users', label: 'Users', request: getUsers(1, '') }
    ];
    const requests = await Promise.allSettled(requestDefs.map((item) => item.request));
    const byKey = requestDefs.reduce((acc, item, index) => {
      acc[item.key] = requests[index].status === 'fulfilled' ? requests[index].value : null;
      return acc;
    }, {});
    const failed = requestDefs.filter((item, index) => requests[index].status === 'rejected');
    const criticalFailed = failed.filter((item) => item.critical);
    const optionalFailed = failed.filter((item) => !item.critical);

    const nextStats = { ...EMPTY_STATS, ...(byKey.dashboard?.data?.data || {}) };
    const snapshot = byKey.realtime?.data?.data;
    if (snapshot) setLive(snapshot);
    setStats(snapshot ? mergeRealtimeStats(nextStats, snapshot) : nextStats);
    setConfigState(normalizeConfig(byKey.config?.data?.data?.config || runtimeConfig));
    setProfiles(byKey.profiles?.data?.data || []);
    setProfileMeta(byKey.profiles?.data?.meta || { page: profileFilters.page, limit: profileFilters.limit, total: 0, totalPages: 1 });
    setVerifications(byKey.verifications?.data?.data || []);
    setPayments(byKey.payments?.data?.data || { transactions: [], plans: [], coupons: [] });
    setReports(byKey.reports?.data?.data || []);
    setAlerts(byKey.alerts?.data?.data || []);
    setRoles(byKey.roles?.data?.data || []);
    setFunnel(byKey.funnel?.data?.data || []);
    setEvents(byKey.events?.data?.data || []);
    setAuditLogs(byKey.auditLogs?.data?.data || []);
    setServices(byKey.services?.data?.data || byKey.dashboard?.data?.data?.services || []);
    setUsers(byKey.users?.data?.data || []);
    setModuleIssues(optionalFailed.map((item) => item.label));
    if (criticalFailed.length) {
      setError(`Critical admin data could not be loaded: ${criticalFailed.map((item) => item.label).join(', ')}.`);
    }
    setLoading(false);
  }, [profileFilters, runtimeConfig]);

  useEffect(() => { loadAll(); }, []);
  useEffect(() => { loadProfiles().catch(() => {}); }, [loadProfiles]);

  useEffect(() => {
    if (!session.token) return undefined;
    const socket = io(`${ADMIN_SOCKET_URL}/admin`, {
      path: '/admin-socket',
      auth: { token: session.token },
      transports: ['websocket', 'polling']
    });
    socket.on('admin:snapshot', (snapshot) => {
      setLive(snapshot);
      setStats((current) => mergeRealtimeStats(current, snapshot));
    });
    socket.on('admin:profile_created', loadProfiles);
    socket.on('admin:profile_status', loadProfiles);
    socket.on('admin:config_updated', (event) => {
      if (event.key && event.config) setConfigState((current) => ({ ...current, [event.key]: event.config }));
      refreshRuntime();
    });
    return () => socket.disconnect();
  }, [session.token, loadProfiles, refreshRuntime]);

  useEffect(() => {
    if (!session.token) return undefined;
    const interval = window.setInterval(async () => {
      const [response, healthResponse] = await Promise.all([
        getRealtimeSnapshot().catch(() => null),
        getServiceHealth().catch(() => null)
      ]);
      const snapshot = response?.data?.data;
      if (snapshot) {
        setLive(snapshot);
        setStats((current) => mergeRealtimeStats(current, snapshot));
      }
      if (healthResponse?.data?.data) setServices(healthResponse.data.data);
    }, 10000);
    return () => window.clearInterval(interval);
  }, [session.token]);

  const saveConfig = async (key) => {
    setMessage('');
    setError('');
    try {
      await updateConfig(key, configState[key] || {});
      await refreshRuntime();
      setMessage(`${key.replace('_', ' ')} saved and broadcast.`);
    } catch (err) {
      setError(err.response?.data?.error?.message || err.message || 'Could not save config.');
    }
  };

  const handleCreateProfile = async () => {
    try {
      await createProfile(profileForm);
      setProfileForm({ gender: 'female', religion: 'Hindu', maritalStatus: 'never_married' });
      setMessage('Profile created.');
      await loadProfiles();
    } catch (err) {
      setError(err.response?.data?.error?.message || err.message || 'Could not create profile.');
    }
  };

  const handleBulk = async () => {
    try {
      const rows = parseCsv(csvText);
      await bulkCreateProfiles(rows);
      setCsvText('');
      setMessage(`${rows.length} profile rows submitted.`);
      await loadProfiles();
    } catch (err) {
      setError(err.response?.data?.error?.message || err.message || 'Could not import CSV.');
    }
  };

  const handleStatus = async (id, action) => {
    await updateProfileStatus(id, action);
    await loadProfiles();
  };

  const handleBulkStatus = async (action) => {
    await Promise.all(profiles.map((profile) => updateProfileStatus(profile.profile_id, action)));
    await loadProfiles();
    setMessage(`${profiles.length} visible profiles updated.`);
  };

  const handleDeleteProfile = async (id) => {
    await deleteProfile(id);
    await loadProfiles();
  };

  const handleAttachPhoto = async () => {
    if (!photoForm.profileId || !photoForm.primaryPhotoUrl) {
      setError('Profile ID and photo URL are required.');
      return;
    }
    await updateProfile(photoForm.profileId, { primaryPhotoUrl: photoForm.primaryPhotoUrl });
    setPhotoForm({ profileId: '', primaryPhotoUrl: '' });
    setMessage('Profile photo updated.');
    await loadProfiles();
  };

  const handleCampaign = async () => {
    let audience = {};
    try { audience = JSON.parse(campaign.audienceText || '{}'); } catch (_) { audience = {}; }
    await createCampaign({ name: campaign.name, channel: campaign.channel, audience });
    setCampaign({ name: '', channel: 'push', audienceText: '{"plan":"free"}' });
    setMessage('Campaign draft created.');
  };

  const logout = () => {
    localStorage.removeItem('adminToken');
    navigate('/login');
  };

  const activeLabel = visibleTabs.find((tab) => tab.id === activeTab)?.label || 'Dashboard';

  return (
    <div className="admin-page">
      <TopBar brand={runtimeConfig.branding?.appTitle || 'SoulMatch'} role={session.role} email={session.email} live={live} onSync={loadAll} onLogout={logout} />
      <div className="layout">
        <Sidebar tabs={visibleTabs} activeTab={activeTab} setActiveTab={setActiveTab} />
        <main className="main">
          <div className="main-hero">
            <div>
              <span className="eyebrow">Control plane</span>
              <h1>{activeLabel}</h1>
              <p>Production-grade operations, configuration, moderation, and monitoring for the SoulMatch platform.</p>
            </div>
            <div className="hero-actions">
              <StatusBadge tone={loading ? 'amber' : 'ok'}>{loading ? 'Syncing' : 'Live'}</StatusBadge>
              <button className="secondary-btn" onClick={() => setActiveTab('health')} type="button">System health</button>
            </div>
          </div>
          {message ? <div className="alert success">{message}</div> : null}
          {error ? <div className="alert error">{error}</div> : null}
          {moduleIssues.length && !error ? (
            <div className="alert warning">
              Optional modules unavailable: {moduleIssues.join(', ')}. Core dashboard controls remain usable.
            </div>
          ) : null}

          {activeTab === 'overview' ? <OverviewPanel stats={stats} live={live} analyticsRows={events} alerts={alerts} setActiveTab={setActiveTab} /> : null}
          {activeTab === 'profiles' ? <ProfilesPanel profiles={profiles} profileMeta={profileMeta} profileFilters={profileFilters} setProfileFilters={setProfileFilters} profileForm={profileForm} setProfileForm={setProfileForm} photoForm={photoForm} setPhotoForm={setPhotoForm} csvText={csvText} setCsvText={setCsvText} onCreate={handleCreateProfile} onAttachPhoto={handleAttachPhoto} onBulk={handleBulk} onStatus={handleStatus} onBulkStatus={handleBulkStatus} onDelete={handleDeleteProfile} /> : null}
          {activeTab === 'verification' ? <VerificationPanel verifications={verifications} onApprove={async (id) => { await approveVerification(id); await loadAll(); }} onReject={async (id) => { await rejectVerification(id); await loadAll(); }} /> : null}
          {activeTab === 'payments' ? <PaymentsPanel payments={payments} config={configState} patchConfig={patchConfig} onSave={saveConfig} onRefund={async (t) => { await createRefund({ transactionId: t.transaction_id, amount: t.amount }); setMessage('Refund request queued.'); }} /> : null}
          {activeTab === 'cms' ? <CmsPanel config={configState} patchConfig={patchConfig} onSave={saveConfig} /> : null}
          {activeTab === 'engagement' ? <EngagementPanel config={configState} patchConfig={patchConfig} onSave={saveConfig} campaign={campaign} setCampaign={setCampaign} onCampaign={handleCampaign} /> : null}
          {activeTab === 'moderation' ? <ModerationPanel reports={reports} alerts={alerts} onResolve={async (id) => { await resolveReport(id); await loadAll(); }} onAck={async (id) => { await acknowledgeAlert(id); await loadAll(); }} /> : null}
          {activeTab === 'config' ? <ConfigPanel config={configState} patchConfig={patchConfig} onSave={saveConfig} /> : null}
          {activeTab === 'rbac' ? <RbacPanel roles={roles} session={session} /> : null}
          {activeTab === 'analytics' ? <AnalyticsPanel funnel={funnel} events={events} auditLogs={auditLogs} /> : null}
          {activeTab === 'health' ? <HealthPanel services={services} /> : null}
        </main>
      </div>
    </div>
  );
}
