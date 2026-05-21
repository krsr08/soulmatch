import React from 'react';

export function toneForStatus(status) {
  const normalized = String(status || '').toLowerCase();
  if (['active', 'verified', 'approved', 'paid', 'success', 'captured'].includes(normalized)) return 'success';
  if (['pending', 'submitted', 'under_review', 'in_progress', 'created'].includes(normalized)) return 'warning';
  if (['rejected', 'suspended', 'blocked', 'failed', 'banned'].includes(normalized)) return 'danger';
  return 'neutral';
}

export function Icon({ name }) {
  const common = {
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round',
    strokeLinejoin: 'round'
  };
  const icons = {
    grid: (
      <>
        <rect x="3" y="3" width="7" height="7" rx="1.5" {...common} />
        <rect x="14" y="3" width="7" height="7" rx="1.5" {...common} />
        <rect x="3" y="14" width="7" height="7" rx="1.5" {...common} />
        <rect x="14" y="14" width="7" height="7" rx="1.5" {...common} />
      </>
    ),
    users: (
      <>
        <circle cx="9" cy="7" r="4" {...common} />
        <path d="M3 21v-2a6 6 0 0 1 12 0v2" {...common} />
        <path d="M16 3.5a4 4 0 0 1 0 7" {...common} />
        <path d="M21 21v-2a5 5 0 0 0-4-4.9" {...common} />
      </>
    ),
    agent: (
      <>
        <path d="M4 18v-5a8 8 0 0 1 16 0v5" {...common} />
        <path d="M4 14h3v5H5a1 1 0 0 1-1-1v-4Z" {...common} />
        <path d="M20 14h-3v5h2a1 1 0 0 0 1-1v-4Z" {...common} />
        <path d="M13 21h-2a2 2 0 0 1-2-2" {...common} />
      </>
    ),
    tag: (
      <>
        <path d="M20 12 12 20 4 12V4h8l8 8Z" {...common} />
        <circle cx="8.5" cy="8.5" r="1.5" {...common} />
      </>
    ),
    content: (
      <>
        <rect x="4" y="3" width="16" height="18" rx="2" {...common} />
        <path d="M8 8h8M8 12h8M8 16h5" {...common} />
      </>
    ),
    gear: (
      <>
        <circle cx="12" cy="12" r="3" {...common} />
        <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.04.04a2 2 0 0 1-2.83 2.83l-.04-.04A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 1.56V21a2 2 0 0 1-4 0v-.04a1.7 1.7 0 0 0-1-1.56 1.7 1.7 0 0 0-1.88.34l-.04.04a2 2 0 0 1-2.83-2.83l.04-.04A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-1.56-1H3a2 2 0 0 1 0-4h.04A1.7 1.7 0 0 0 4.6 9a1.7 1.7 0 0 0-.34-1.88l-.04-.04a2 2 0 0 1 2.83-2.83l.04.04A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-1.56V3a2 2 0 0 1 4 0v.04a1.7 1.7 0 0 0 1 1.56 1.7 1.7 0 0 0 1.88-.34l.04-.04a2 2 0 0 1 2.83 2.83l-.04.04A1.7 1.7 0 0 0 19.4 9c.2.6.78 1 1.56 1H21a2 2 0 0 1 0 4h-.04A1.7 1.7 0 0 0 19.4 15Z" {...common} />
      </>
    ),
    plus: <path d="M12 5v14M5 12h14" {...common} />,
    check: <path d="m5 12 4 4L19 6" {...common} />,
    close: <path d="M6 6l12 12M18 6 6 18" {...common} />,
    export: <><path d="M12 3v12" {...common} /><path d="m7 10 5 5 5-5" {...common} /><path d="M5 21h14" {...common} /></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" {...common} /><path d="M10 21h4" {...common} /></>,
    help: <><circle cx="12" cy="12" r="9" {...common} /><path d="M9.5 9a2.7 2.7 0 0 1 5.2 1c0 2-2.7 2.2-2.7 4" {...common} /><path d="M12 18h.01" {...common} /></>,
    search: <><circle cx="11" cy="11" r="7" {...common} /><path d="m20 20-3.6-3.6" {...common} /></>,
    eye: <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" {...common} /><circle cx="12" cy="12" r="3" {...common} /></>,
    edit: <><path d="M12 20h9" {...common} /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4 11.5-11.5Z" {...common} /></>,
    ban: <><circle cx="12" cy="12" r="9" {...common} /><path d="M5.7 5.7 18.3 18.3" {...common} /></>,
    trend: <><path d="M3 17 9 11l4 4 8-9" {...common} /><path d="M15 6h6v6" {...common} /></>,
    sliders: <><path d="M4 7h10M18 7h2M4 17h2M10 17h10" {...common} /><circle cx="16" cy="7" r="2" {...common} /><circle cx="8" cy="17" r="2" {...common} /></>,
    lock: <><rect x="5" y="10" width="14" height="11" rx="2" {...common} /><path d="M8 10V7a4 4 0 0 1 8 0v3" {...common} /></>,
    clock: <><circle cx="12" cy="12" r="9" {...common} /><path d="M12 7v5l3 2" {...common} /></>,
    star: <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.2 6.4 20.2 7.5 14 3 9.6l6.2-.9L12 3Z" {...common} />,
    up: <><path d="M12 19V5" {...common} /><path d="m5 12 7-7 7 7" {...common} /></>,
    rupee: <><path d="M7 5h10M7 9h10M7 5c6 0 6 8 0 8l8 8" {...common} /></>,
    invoice: <><path d="M6 2h12v20l-3-2-3 2-3-2-3 2V2Z" {...common} /><path d="M9 8h6M9 12h6M9 16h4" {...common} /></>,
    mail: <><rect x="3" y="5" width="18" height="14" rx="2" {...common} /><path d="m3 7 9 6 9-6" {...common} /></>,
    target: <><circle cx="12" cy="12" r="9" {...common} /><circle cx="12" cy="12" r="5" {...common} /><circle cx="12" cy="12" r="1" {...common} /></>,
    image: <><rect x="3" y="5" width="18" height="14" rx="2" {...common} /><circle cx="8" cy="10" r="2" {...common} /><path d="m21 16-5-5L5 19" {...common} /></>,
    chat: <path d="M21 11.5a8.4 8.4 0 0 1-9 8.3 9.3 9.3 0 0 1-4.4-1.1L3 20l1.4-4.1A8.3 8.3 0 1 1 21 11.5Z" {...common} />,
    flag: <><path d="M5 21V4" {...common} /><path d="M5 4h12l-2 4 2 4H5" {...common} /></>,
    crown: <><path d="m3 8 4 3 5-7 5 7 4-3-2 10H5L3 8Z" {...common} /><path d="M5 21h14" {...common} /></>,
    person: <><circle cx="12" cy="7" r="4" {...common} /><path d="M5 21a7 7 0 0 1 14 0" {...common} /></>,
    log: <><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" {...common} /><path d="M16 17l5-5-5-5" {...common} /><path d="M21 12H9" {...common} /></>,
    pulse: <path d="M3 12h4l2-6 4 12 2-6h6" {...common} />,
    cms: <><rect x="3" y="4" width="18" height="16" rx="2" {...common} /><path d="M3 9h18M8 14h8" {...common} /></>,
    key: <><circle cx="7.5" cy="14.5" r="4.5" {...common} /><path d="M11 11 21 1M16 6l2 2M14 8l2 2" {...common} /></>,
    exit: <><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" {...common} /><path d="M16 17l5-5-5-5M21 12H9" {...common} /></>
  };
  return (
    <span className={`admin-icon admin-icon-${name}`} aria-hidden="true">
      <svg viewBox="0 0 24 24">{icons[name] || icons.grid}</svg>
    </span>
  );
}

export function StatusPill({ status, children }) {
  return <span className={`status-pill ${toneForStatus(status)}`}>{children || status || 'Unknown'}</span>;
}

export function AdminButton({ variant = 'secondary', children, className = '', ...props }) {
  return (
    <button {...props} className={`admin-btn ${variant} ${className}`}>
      {children}
    </button>
  );
}

export function SectionHeader({ eyebrow, title, description, actions }) {
  return (
    <div className="section-heading">
      <div>
        {eyebrow ? <span>{eyebrow}</span> : null}
        <h2>{title}</h2>
        {description ? <p>{description}</p> : null}
      </div>
      {actions ? <div className="heading-actions">{actions}</div> : null}
    </div>
  );
}

export function EmptyState({ title, body }) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      <span>{body}</span>
    </div>
  );
}

function displayName(row) {
  return [row?.first_name || row?.firstName, row?.last_name || row?.lastName].filter(Boolean).join(' ') || row?.full_name || 'Unnamed';
}

export function ProfileAvatar({ profile }) {
  const url = profile?.primary_photo_url || profile?.profile_photo_url;
  return url
    ? <img className="profile-avatar" src={url} alt="" />
    : <div className="profile-avatar fallback">{displayName(profile).charAt(0).toUpperCase()}</div>;
}

export function ManagementToolbar({ title, subtitle, onCreate, createLabel, children }) {
  return (
    <div className="management-toolbar">
      <div>
        <h3>{title}</h3>
        <p>{subtitle}</p>
      </div>
      <div>
        {children}
        {onCreate ? <AdminButton variant="primary" onClick={onCreate}><Icon name="plus" /> {createLabel}</AdminButton> : null}
      </div>
    </div>
  );
}

export function StatCard({ tone, label, value, sub, link = 'View Details', onClick }) {
  return (
    <button type="button" className={`stat-tile ${tone || ''}`} onClick={onClick}>
      <div className="stat-icon"><Icon name="trend" /></div>
      <strong>{value}</strong>
      <span>{label}</span>
      <small>{sub}</small>
      <em>{link}</em>
    </button>
  );
}
