import React, { useMemo, useState } from 'react';
import {
  AdminButton,
  EmptyState,
  Icon,
  ProfileAvatar,
  StatusPill
} from '../../components/AdminPrimitives';

function dateOnly(value) {
  if (!value) return 'Not set';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function fullName(row) {
  return [row?.first_name || row?.firstName, row?.last_name || row?.lastName].filter(Boolean).join(' ') || row?.full_name || 'Unnamed';
}

function formatMemberDisplayId(profileId) {
  const compact = String(profileId || '').replace(/-/g, '').toUpperCase();
  return compact ? `SM-${compact.slice(0, 8)}` : '-';
}

function memberDisplayId(profile) {
  return profile?.profile_display_id || formatMemberDisplayId(profile?.profile_id);
}

function memberSearchHaystack(profile) {
  return [
    memberDisplayId(profile),
    fullName(profile),
    profile.phone,
    profile.email,
    profile.religion,
    profile.caste,
    profile.occupation,
    profile.working_city,
    profile.family_city,
    profile.advisor_name
  ].filter(Boolean).join(' ').toLowerCase();
}

export function MembersPanel({ profiles, search, onOpen, onCreate, onStatus, onBlock }) {
  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return profiles;
    return profiles.filter((profile) => memberSearchHaystack(profile).includes(q));
  }, [profiles, search]);
  const verifiedCount = rows.filter((profile) => String(profile.verification_status || '').toLowerCase() === 'verified').length;
  const pendingCount = rows.filter((profile) => ['pending', 'submitted', 'under_review'].includes(String(profile.verification_status || profile.review_status || '').toLowerCase())).length;
  const hiddenCount = rows.filter((profile) => !profile.is_published).length;
  const paidCount = rows.filter((profile) => profile.plan_id && profile.plan_id !== 'free').length;

  return (
    <div className="admin-content enterprise-screen members-management-page">
      <div className="enterprise-page-head">
        <div>
          <h2>Members Management</h2>
          <p>{rows.length} profiles | full 360-degree member control</p>
        </div>
        <div className="enterprise-actions">
          <AdminButton variant="secondary"><Icon name="export" /> Export</AdminButton>
          <AdminButton variant="primary" onClick={onCreate}><Icon name="plus" /> Add Member</AdminButton>
        </div>
      </div>

      <div className="enterprise-kpi-grid">
        <button type="button" className="enterprise-kpi">
          <Icon name="users" />
          <span>Total members</span>
          <strong>{rows.length}</strong>
          <small>{paidCount} paid | {rows.length - paidCount} free</small>
        </button>
        <button type="button" className="enterprise-kpi warning">
          <Icon name="clock" />
          <span>Pending review</span>
          <strong>{pendingCount}</strong>
          <small>KYC, profile, and document checks</small>
        </button>
        <button type="button" className="enterprise-kpi success">
          <Icon name="check" />
          <span>Verified profiles</span>
          <strong>{verifiedCount}</strong>
          <small>{rows.length ? Math.round((verifiedCount / rows.length) * 100) : 0}% verification coverage</small>
        </button>
        <button type="button" className="enterprise-kpi danger">
          <Icon name="eye" />
          <span>Hidden profiles</span>
          <strong>{hiddenCount}</strong>
          <small>Suspended or unpublished visibility</small>
        </button>
      </div>

      <div className="enterprise-filter-bar">
        <strong>Live Controls</strong>
        <span className="mini-token">Search follows global header</span>
        <span className="mini-token">Verify, hide, block, or open 360 view</span>
      </div>

      <div className="enterprise-panel enterprise-table">
        <table>
          <thead>
            <tr>
              <th>Member</th>
              <th>Gender</th>
              <th>Plan</th>
              <th>Source</th>
              <th>Verification</th>
              <th>Visibility</th>
              <th>Validity</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((profile) => {
              const isVerified = String(profile.verification_status || '').toLowerCase() === 'verified';
              return (
                <tr key={profile.profile_id}>
                  <td>
                    <div className="identity-cell">
                      <ProfileAvatar profile={profile} />
                      <span><strong>{fullName(profile)}</strong><small>{memberDisplayId(profile)} | {profile.phone || profile.email || '-'}</small></span>
                    </div>
                  </td>
                  <td>{profile.gender || '-'}</td>
                  <td><StatusPill status={profile.plan_id === 'free' ? 'neutral' : 'success'}>{profile.plan_id || 'free'}</StatusPill></td>
                  <td>{profile.created_by_advisor_id ? `Agent | ${profile.advisor_name || 'Linked'}` : 'Self'}</td>
                  <td><StatusPill status={profile.verification_status}>{profile.verification_status}</StatusPill></td>
                  <td><StatusPill status={profile.is_published ? 'active' : 'pending'}>{profile.is_published ? 'Visible' : 'Hidden'}</StatusPill></td>
                  <td>{dateOnly(profile.subscription_end_date)}</td>
                  <td>
                    <div className="row-actions">
                      <button onClick={() => onOpen(profile)} title="360 view"><Icon name="eye" /></button>
                      <button
                        onClick={() => onStatus(profile, isVerified ? 'unverify' : 'approve')}
                        title={isVerified ? 'Make unverified' : 'Verify'}
                      >
                        <Icon name={isVerified ? 'close' : 'check'} />
                      </button>
                      <button onClick={() => onStatus(profile, profile.is_published ? 'suspend' : 'restore')} title="Visibility"><Icon name="sliders" /></button>
                      <button onClick={() => onBlock(profile)} title="Block / unblock"><Icon name="ban" /></button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {!rows.length ? <EmptyState title="No members found" body="Try changing the search text in the dashboard header." /> : null}
      </div>
    </div>
  );
}

export function MembersDirectoryPanel({ profiles, search, onOpen, onCreate }) {
  const [viewMode, setViewMode] = useState('cards');
  const [sourceFilter, setSourceFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    return profiles.filter((profile) => {
      const source = profile.created_by_advisor_id ? 'agent' : (profile.profile_created_by === 'admin' ? 'admin' : 'self');
      if (sourceFilter !== 'all' && source !== sourceFilter) return false;
      if (statusFilter !== 'all' && String(profile.verification_status || profile.review_status || '').toLowerCase() !== statusFilter) return false;
      if (!q) return true;
      return memberSearchHaystack(profile).includes(q);
    });
  }, [profiles, search, sourceFilter, statusFilter]);
  const verifiedCount = rows.filter((profile) => String(profile.verification_status || '').toLowerCase() === 'verified').length;
  const pendingCount = rows.filter((profile) => ['pending', 'submitted', 'under_review'].includes(String(profile.verification_status || profile.review_status || '').toLowerCase())).length;
  const activeToday = rows.filter((profile) => {
    const lastSeen = new Date(profile.last_seen_at || profile.updated_at || profile.created_at);
    return !Number.isNaN(lastSeen.getTime()) && Date.now() - lastSeen.getTime() < 24 * 60 * 60 * 1000;
  }).length;

  return (
    <div className="admin-content enterprise-screen members-directory-page">
      <div className="enterprise-page-head">
        <div>
          <h2>Member Directory</h2>
          <p>{rows.length} profiles | open any row or card for complete 360-degree details</p>
        </div>
        <div className="enterprise-actions">
          <AdminButton variant="secondary"><Icon name="export" /> Export</AdminButton>
          <AdminButton variant="primary" onClick={onCreate}><Icon name="plus" /> Add Member</AdminButton>
        </div>
      </div>

      <div className="enterprise-kpi-grid member-directory-kpis">
        <button type="button" className="enterprise-kpi">
          <span>Total Members</span>
          <strong>{rows.length}</strong>
          <small>{profiles.length ? `${Math.round((rows.length / profiles.length) * 100)}% of loaded profiles` : 'No members loaded'}</small>
          <Icon name="users" />
        </button>
        <button type="button" className="enterprise-kpi success">
          <span>Verified</span>
          <strong>{verifiedCount}</strong>
          <small>{rows.length ? `${Math.round((verifiedCount / rows.length) * 100)}% verified` : 'No verified members'}</small>
          <Icon name="check" />
        </button>
        <button type="button" className="enterprise-kpi warning">
          <span>Pending Verification</span>
          <strong>{pendingCount}</strong>
          <small>Identity and profile review queue</small>
          <Icon name="clock" />
        </button>
        <button type="button" className="enterprise-kpi">
          <span>Active Today</span>
          <strong>{activeToday}</strong>
          <small>Based on latest profile activity</small>
          <Icon name="pulse" />
        </button>
      </div>

      <div className="enterprise-filter-bar">
        <strong>Directory Controls</strong>
        <button type="button" className="filter-chip"><Icon name="sliders" /> Advanced Filters</button>
        <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
          <option value="all">All sources</option>
          <option value="self">Self</option>
          <option value="agent">Agent</option>
          <option value="admin">Admin</option>
        </select>
        <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
          <option value="all">All verification</option>
          <option value="verified">Verified</option>
          <option value="pending">Pending</option>
          <option value="rejected">Rejected</option>
        </select>
        <div className="view-switcher">
          <button type="button" className={viewMode === 'list' ? 'active' : ''} onClick={() => setViewMode('list')}>List</button>
          <button type="button" className={viewMode === 'cards' ? 'active' : ''} onClick={() => setViewMode('cards')}>Cards</button>
        </div>
      </div>

      {viewMode === 'cards' ? (
        <div className="member-card-grid">
          {rows.map((profile) => (
            <article key={profile.profile_id} className="member-directory-card" onClick={() => onOpen(profile)}>
              <div className="member-card-top">
                <ProfileAvatar profile={profile} />
                <div>
                  <strong>{fullName(profile)}</strong>
                  <small>{memberDisplayId(profile)} | {profile.gender || '-'} | {profile.plan_id || 'free'}</small>
                </div>
                <StatusPill status={profile.verification_status}>{profile.verification_status || 'pending'}</StatusPill>
              </div>
              <div className="member-card-facts">
                <span>{profile.occupation || 'Occupation not set'}</span>
                <span>{profile.working_city || profile.family_city || 'City not set'}</span>
                <span>{profile.education_level || 'Education not set'}</span>
              </div>
              <div className="member-card-footer">
                <span>{profile.created_by_advisor_id ? `Agent: ${profile.advisor_name || 'Linked'}` : (profile.profile_created_by === 'admin' ? 'Created by Admin' : 'Created by Self')}</span>
                <button type="button" onClick={(event) => { event.stopPropagation(); onOpen(profile); }}>360 View</button>
              </div>
            </article>
          ))}
          {!rows.length ? <EmptyState title="No members found" body="Try changing the search or filters." /> : null}
          {rows.length ? (
            <div className="table-pagination-bar">
              <span>Showing 1 - {rows.length} of {rows.length} members</span>
              <div><button disabled>‹</button><button className="active">1</button><button disabled>›</button></div>
            </div>
          ) : null}
        </div>
      ) : (
        <div className="enterprise-panel enterprise-table">
          <table>
            <thead>
              <tr><th>Profile ID</th><th>Name</th><th>Gender</th><th>Plan</th><th>Source</th><th>Verification</th><th>Created</th><th>Action</th></tr>
            </thead>
            <tbody>
              {rows.map((profile) => (
                <tr key={profile.profile_id} onClick={() => onOpen(profile)}>
                  <td><code>{memberDisplayId(profile)}</code></td>
                  <td><div className="identity-cell"><ProfileAvatar profile={profile} /><span><strong>{fullName(profile)}</strong><small>{profile.phone || profile.email || '-'}</small></span></div></td>
                  <td>{profile.gender || '-'}</td>
                  <td>{profile.plan_id || 'free'}</td>
                  <td>{profile.created_by_advisor_id ? `Agent | ${profile.advisor_name || 'Linked'}` : (profile.profile_created_by === 'admin' ? 'Admin' : 'Self')}</td>
                  <td><StatusPill status={profile.verification_status}>{profile.verification_status || 'pending'}</StatusPill></td>
                  <td>{dateOnly(profile.created_at)}</td>
                  <td><button type="button" onClick={(event) => { event.stopPropagation(); onOpen(profile); }}>360 View</button></td>
                </tr>
              ))}
            </tbody>
          </table>
          {!rows.length ? <EmptyState title="No members found" body="Try changing the search or filters." /> : null}
          {rows.length ? (
            <div className="table-pagination-bar">
              <span>Showing 1 - {rows.length} of {rows.length} members</span>
              <div><button disabled>‹</button><button className="active">1</button><button disabled>›</button></div>
            </div>
          ) : null}
        </div>
      )}
    </div>
  );
}

