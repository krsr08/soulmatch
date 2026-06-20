import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Icon } from './AdminPrimitives';

const ROLE_SURFACE_TABS = [];

export default function AdminShell({
  activeTab,
  onTab,
  session,
  search,
  onSearch,
  onHelp,
  menuGroups,
  children
}) {
  const navigate = useNavigate();
  const isRoleSurface = ROLE_SURFACE_TABS.includes(activeTab);
  const handleTab = (id) => {
    if (id === 'logout') {
      localStorage.removeItem('adminToken');
      navigate('/login');
      return;
    }
    onTab(id);
  };

  return (
    <div className="admin-console">
      <aside className="console-sidebar">
        <div className="console-brand">
          <div className="console-brand-mark"><Icon name="heart" /></div>
          <div>
            <h1>SoulMatch</h1>
            <span>Admin Operations</span>
          </div>
        </div>
        <nav className="console-nav">
          {(menuGroups || []).map((group) => (
            <section key={group.label}>
              {group.label !== 'Control' ? <p>{group.label}</p> : null}
              {group.items.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={activeTab === item.id ? 'active' : ''}
                  onClick={() => handleTab(item.id)}
                >
                  <Icon name={item.icon} />
                  <span>{item.label}</span>
                </button>
              ))}
            </section>
          ))}
        </nav>
        <div className="console-user">
          <div className="console-avatar">{(session.email || 'SA').slice(0, 2).toUpperCase()}</div>
          <div>
            <span>Logged in as</span>
            <strong>{session.role === 'super_admin' ? 'Super Admin' : session.role || 'Admin'}</strong>
          </div>
        </div>
      </aside>
      <main className={`console-main ${isRoleSurface ? 'role-surface-main' : ''}`}>
        {!isRoleSurface ? (
          <header className="console-topbar">
            <label className="global-search">
              <Icon name="search" />
              <input value={search} onChange={(event) => onSearch(event.target.value)} placeholder="Search records..." />
            </label>
            <div className="topbar-actions">
              <button type="button" title="Notifications" onClick={() => handleTab('notifications')}><Icon name="bell" /></button>
              <button type="button" title="Help" onClick={onHelp}><Icon name="help" /></button>
              <button type="button" title="Settings" onClick={() => handleTab('settings')}><Icon name="gear" /></button>
              <div className="topbar-admin-copy">
                <strong>{session.email || 'Admin User'}</strong>
                <span>{session.role === 'super_admin' ? 'Super Admin' : session.role || 'Admin'}</span>
              </div>
              <div className="small-avatar">{(session.email || 'A').charAt(0).toUpperCase()}</div>
            </div>
          </header>
        ) : null}
        {children}
      </main>
    </div>
  );
}

