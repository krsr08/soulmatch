import React, { useMemo, useState } from 'react';
import {
  EmptyState,
  Icon,
  ManagementToolbar,
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

  return (
    <div className="admin-content">
      <ManagementToolbar
        title="Members Management"
        subtitle={`${rows.length} profiles | full 360-degree member control`}
        onCreate={onCreate}
        createLabel="Add Member"
      />
      <div className="admin-card data-table tall">
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

  return (
    <div className="admin-content members-directory-page">
      <ManagementToolbar
        title="Members Directory"
        subtitle={`${rows.length} profiles | open any row or card for complete 360-degree details`}
        onCreate={onCreate}
        createLabel="Add Member"
      >
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
      </ManagementToolbar>

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
        </div>
      ) : (
        <div className="admin-card data-table tall">
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
        </div>
      )}
    </div>
  );
}

