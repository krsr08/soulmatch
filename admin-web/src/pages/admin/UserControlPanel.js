import React, { useMemo, useState } from 'react';
import {
  AdminButton,
  EmptyState,
  Icon,
  ProfileAvatar,
  StatusPill
} from '../../components/AdminPrimitives';

function fullName(row) {
  return [row?.first_name || row?.firstName, row?.last_name || row?.lastName].filter(Boolean).join(' ') || row?.full_name || 'Unnamed';
}

function dateOnly(value) {
  if (!value) return 'Not set';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatMemberDisplayId(profileId) {
  const compact = String(profileId || '').replace(/-/g, '').toUpperCase();
  return compact ? `SM-${compact.slice(0, 8)}` : '-';
}

function memberDisplayId(profile) {
  return profile?.profile_display_id || formatMemberDisplayId(profile?.profile_id);
}

function sourceFor(profile) {
  if (profile.created_by_advisor_id) return 'agent';
  if (profile.profile_created_by === 'admin') return 'admin';
  return 'self';
}

function memberSearchHaystack(profile, user) {
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
    profile.advisor_name,
    user?.phone,
    user?.email
  ].filter(Boolean).join(' ').toLowerCase();
}

function getPlan(profile) {
  return String(profile.plan_id || profile.membership_plan || 'free').toLowerCase();
}

function ControlMetric({ label, value, sub, tone }) {
  return (
    <article className={`control-metric ${tone || ''}`}>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{sub}</small>
    </article>
  );
}

export default function UserControlPanel({
  profiles,
  users,
  reports,
  search,
  onOpen,
  onCreate,
  onStatus,
  onBlock,
  onDelete,
  onBulkStatus,
  onBulkUpdate,
  onBulkDelete
}) {
  const [sourceFilter, setSourceFilter] = useState('all');
  const [planFilter, setPlanFilter] = useState('all');
  const [verificationFilter, setVerificationFilter] = useState('all');
  const [visibilityFilter, setVisibilityFilter] = useState('all');
  const [selected, setSelected] = useState(() => new Set());
  const [bulkField, setBulkField] = useState('adminStatus');
  const [bulkValue, setBulkValue] = useState('active');

  const usersById = useMemo(() => {
    const map = new Map();
    users.forEach((user) => map.set(user.user_id || user.id, user));
    return map;
  }, [users]);

  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    return profiles.filter((profile) => {
      const source = sourceFor(profile);
      const plan = getPlan(profile);
      const verification = String(profile.verification_status || 'pending').toLowerCase();
      const visible = profile.is_published ? 'visible' : 'hidden';
      const user = usersById.get(profile.user_id);
      if (sourceFilter !== 'all' && source !== sourceFilter) return false;
      if (planFilter !== 'all' && plan !== planFilter) return false;
      if (verificationFilter !== 'all' && verification !== verificationFilter) return false;
      if (visibilityFilter !== 'all' && visible !== visibilityFilter) return false;
      if (!q) return true;
      return memberSearchHaystack(profile, user).includes(q);
    });
  }, [profiles, search, sourceFilter, planFilter, verificationFilter, visibilityFilter, usersById]);

  const visibleIds = useMemo(() => rows.map((profile) => profile.profile_id), [rows]);
  const selectedProfiles = useMemo(
    () => rows.filter((profile) => selected.has(profile.profile_id)),
    [rows, selected]
  );

  const metrics = useMemo(() => {
    const visible = profiles.filter((profile) => profile.is_published).length;
    const verified = profiles.filter((profile) => String(profile.verification_status || '').toLowerCase() === 'verified').length;
    const premium = profiles.filter((profile) => getPlan(profile) !== 'free').length;
    const blocked = profiles.filter((profile) => profile.is_banned || String(profile.admin_status || '').toLowerCase() === 'suspended').length;
    const pendingReports = reports.filter((report) => !['resolved', 'closed'].includes(String(report.status || '').toLowerCase())).length;
    return { visible, verified, premium, blocked, pendingReports };
  }, [profiles, reports]);

  const allVisibleSelected = visibleIds.length > 0 && visibleIds.every((id) => selected.has(id));
  const toggleAll = () => {
    setSelected((current) => {
      const next = new Set(current);
      if (allVisibleSelected) {
        visibleIds.forEach((id) => next.delete(id));
      } else {
        visibleIds.forEach((id) => next.add(id));
      }
      return next;
    });
  };
  const toggleOne = (profileId) => {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(profileId)) next.delete(profileId);
      else next.add(profileId);
      return next;
    });
  };
  const afterBulk = async (action) => {
    await action(selectedProfiles);
    setSelected(new Set());
  };
  const bulkOptions = {
    adminStatus: ['active', 'suspended', 'rejected'],
    photoPrivacy: ['all', 'matches_only', 'request_only', 'private'],
    profileVisibility: ['all', 'premium_only', 'matches_only', 'hidden'],
    reviewNotes: ['Reviewed by admin', 'Needs follow-up', 'Escalated for manual review']
  };
  const applyBulkField = async () => {
    await afterBulk((items) => onBulkUpdate(items, bulkField, bulkValue));
  };

  return (
    <div className="admin-content user-control-page">
      <div className="enterprise-page-head">
        <div>
          <h2>User Control</h2>
          <p>{rows.length} of {profiles.length} members | manage profile, verification, visibility, login access and bulk controls.</p>
        </div>
        <div className="enterprise-actions">
          <AdminButton variant="primary" onClick={onCreate}><Icon name="plus" /> Add User</AdminButton>
        </div>
      </div>

      <div className="enterprise-filter-bar user-control-filter-bar">
        <strong>Filters:</strong>
        <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
          <option value="all">All sources</option>
          <option value="self">Self signup</option>
          <option value="agent">Agent created</option>
          <option value="admin">Admin created</option>
        </select>
        <select value={planFilter} onChange={(event) => setPlanFilter(event.target.value)}>
          <option value="all">All plans</option>
          <option value="free">Free</option>
          <option value="bronze">Bronze</option>
          <option value="silver">Silver</option>
          <option value="gold">Gold</option>
          <option value="platinum">Platinum</option>
        </select>
        <select value={verificationFilter} onChange={(event) => setVerificationFilter(event.target.value)}>
          <option value="all">All verification</option>
          <option value="verified">Verified</option>
          <option value="pending">Pending</option>
          <option value="rejected">Rejected</option>
        </select>
        <select value={visibilityFilter} onChange={(event) => setVisibilityFilter(event.target.value)}>
          <option value="all">All visibility</option>
          <option value="visible">Visible</option>
          <option value="hidden">Hidden</option>
        </select>
        <button type="button" className="filter-chip"><Icon name="sliders" /> Advanced Filters</button>
        <button type="button" className="filter-chip" onClick={() => {
          setSourceFilter('all');
          setPlanFilter('all');
          setVerificationFilter('all');
          setVisibilityFilter('all');
        }}><Icon name="clock" /> Reset</button>
      </div>

      <div className="admin-card control-table-card">
        <div className="bulk-action-bar inline">
          <div>
            <strong>{selectedProfiles.length} selected</strong>
            <small>Bulk actions run through the existing secured admin APIs and refresh the dashboard after completion.</small>
          </div>
          <div>
            <AdminButton disabled={!selectedProfiles.length} onClick={() => afterBulk((items) => onBulkStatus(items, 'approve'))}><Icon name="check" /> Verify</AdminButton>
            <AdminButton disabled={!selectedProfiles.length} onClick={() => afterBulk((items) => onBulkStatus(items, 'suspend'))}><Icon name="ban" /> Suspend</AdminButton>
            <AdminButton disabled={!selectedProfiles.length} onClick={() => afterBulk((items) => onBulkStatus(items, 'restore'))}><Icon name="eye" /> Restore</AdminButton>
            <select value={bulkField} onChange={(event) => {
              const nextField = event.target.value;
              setBulkField(nextField);
              setBulkValue(bulkOptions[nextField][0]);
            }} aria-label="Bulk edit field">
              <option value="adminStatus">Admin status</option>
              <option value="photoPrivacy">Photo privacy</option>
              <option value="profileVisibility">Profile visibility</option>
              <option value="reviewNotes">Review note</option>
            </select>
            <select value={bulkValue} onChange={(event) => setBulkValue(event.target.value)} aria-label="Bulk edit value">
              {bulkOptions[bulkField].map((option) => <option key={option} value={option}>{option}</option>)}
            </select>
            <AdminButton disabled={!selectedProfiles.length} onClick={applyBulkField}><Icon name="edit" /> Apply Edit</AdminButton>
            <AdminButton disabled={!selectedProfiles.length} onClick={() => afterBulk(onBulkDelete)}><Icon name="close" /> Delete</AdminButton>
          </div>
        </div>

        <div className="data-table control-table">
          <table>
            <thead>
              <tr>
                <th><input type="checkbox" checked={allVisibleSelected} onChange={toggleAll} aria-label="Select all visible users" /></th>
                <th>User</th>
                <th>Contact</th>
                <th>Source</th>
                <th>Plan</th>
                <th>Verification</th>
                <th>Visibility</th>
                <th>Created</th>
                <th>Controls</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((profile) => {
                const user = usersById.get(profile.user_id);
                const verified = String(profile.verification_status || '').toLowerCase() === 'verified';
                return (
                  <tr key={profile.profile_id}>
                    <td><input type="checkbox" checked={selected.has(profile.profile_id)} onChange={() => toggleOne(profile.profile_id)} aria-label={`Select ${fullName(profile)}`} /></td>
                    <td>
                      <div className="identity-cell">
                        <ProfileAvatar profile={profile} />
                        <span>
                          <strong>{fullName(profile)}</strong>
                          <small>{memberDisplayId(profile)} | {profile.gender || '-'}</small>
                        </span>
                      </div>
                    </td>
                    <td>
                      <strong>{profile.phone || user?.phone || '-'}</strong>
                      <small>{profile.email || user?.email || 'No email'}</small>
                    </td>
                    <td>{sourceFor(profile) === 'agent' ? `Agent: ${profile.advisor_name || 'Linked'}` : sourceFor(profile)}</td>
                    <td><StatusPill status={getPlan(profile) === 'free' ? 'neutral' : 'success'}>{getPlan(profile)}</StatusPill></td>
                    <td><StatusPill status={profile.verification_status}>{profile.verification_status || 'pending'}</StatusPill></td>
                    <td><StatusPill status={profile.is_published ? 'active' : 'pending'}>{profile.is_published ? 'Visible' : 'Hidden'}</StatusPill></td>
                    <td>{dateOnly(profile.created_at)}</td>
                    <td>
                      <div className="row-actions">
                        <button onClick={() => onOpen(profile)} title="View and edit"><Icon name="eye" /></button>
                        <button onClick={() => onStatus(profile, verified ? 'unverify' : 'approve')} title={verified ? 'Unverify' : 'Verify'}><Icon name={verified ? 'close' : 'check'} /></button>
                        <button onClick={() => onStatus(profile, profile.is_published ? 'suspend' : 'restore')} title={profile.is_published ? 'Suspend visibility' : 'Restore visibility'}><Icon name="sliders" /></button>
                        <button onClick={() => onBlock(profile)} title="Block or unblock login"><Icon name="ban" /></button>
                        <button onClick={() => onDelete(profile)} title="Delete member"><Icon name="close" /></button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {!rows.length ? <EmptyState title="No users found" body="Try changing search or filters." /> : null}
        </div>
        {rows.length ? (
          <div className="table-pagination-bar">
            <span>Showing {rows.length} of {profiles.length} members</span>
            <div><button disabled>‹</button><button className="active">1</button><button>2</button><button>3</button><button disabled>›</button></div>
          </div>
        ) : null}
      </div>

      {selectedProfiles.length ? (
        <div className="floating-bulk-actions">
          <span><strong>{selectedProfiles.length}</strong> Selected</span>
          <button type="button" onClick={() => afterBulk((items) => onBulkStatus(items, 'approve'))}><Icon name="check" /> Verify</button>
          <button type="button" onClick={() => afterBulk((items) => onBulkStatus(items, 'suspend'))}><Icon name="ban" /> Suspend</button>
          <button type="button" onClick={() => afterBulk((items) => onBulkStatus(items, 'restore'))}><Icon name="eye" /> Restore</button>
          <button type="button" onClick={() => afterBulk(onBulkDelete)}><Icon name="close" /> Delete</button>
        </div>
      ) : null}
    </div>
  );
}
