import React, { useMemo } from 'react';
import {
  AdminButton,
  Icon,
  ProfileAvatar,
  StatusPill
} from '../../components/AdminPrimitives';

function numberValue(value) {
  const parsed = Number(value || 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function compactNumber(value) {
  const n = numberValue(value);
  if (n >= 10000000) return `${(n / 10000000).toFixed(1)}Cr`;
  if (n >= 100000) return `${(n / 100000).toFixed(1)}L`;
  if (n >= 1000) return `${(n / 1000).toFixed(1)}k`;
  return String(Math.round(n));
}

function agentSearchHaystack(agent) {
  return [
    agent.full_name,
    agent.phone,
    agent.email,
    agent.city,
    agent.state,
    agent.business_name,
    agent.agent_code
  ].filter(Boolean).join(' ').toLowerCase();
}

export function AgentsPanel({ advisors, profiles, search, onOpen, onCreate, onStatus }) {
  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    const filtered = q
      ? advisors.filter((agent) => agentSearchHaystack(agent).includes(q))
      : advisors;

    return filtered.map((agent) => ({
      ...agent,
      profilesAdded: profiles.filter((profile) => profile.created_by_advisor_id === agent.advisor_id).length
    }));
  }, [advisors, profiles, search]);

  const pendingApprovalCount = rows.filter((agent) => agent.kyc_status === 'pending').length;
  const membersAddedCount = rows.reduce((sum, agent) => sum + numberValue(agent.profilesAdded), 0);
  const averageRating = rows.reduce((sum, agent) => sum + numberValue(agent.average_rating), 0) / Math.max(rows.length, 1);

  return (
    <div className="admin-content enterprise-screen agents-page">
      <div className="enterprise-page-head">
        <div>
          <h2>Agent Control</h2>
          <p>{rows.length} agents | KYC, bank, T&C, plans and linked member profiles</p>
        </div>
        <div className="enterprise-actions">
          <AdminButton variant="secondary"><Icon name="export" /> Export</AdminButton>
          <AdminButton variant="primary" onClick={onCreate}><Icon name="plus" /> Invite Agent</AdminButton>
        </div>
      </div>

      <div className="agent-management-layout">
        <div>
          <div className="enterprise-kpi-grid">
            <button type="button" className="enterprise-kpi">
              <Icon name="agent" />
              <span>Total agents</span>
              <strong>{compactNumber(rows.length)}</strong>
              <small>Registered advisors</small>
            </button>
            <button type="button" className="enterprise-kpi warning">
              <Icon name="clock" />
              <span>Pending approval</span>
              <strong>{compactNumber(pendingApprovalCount)}</strong>
              <small>KYC review required</small>
            </button>
            <button type="button" className="enterprise-kpi success">
              <Icon name="users" />
              <span>Members added</span>
              <strong>{compactNumber(membersAddedCount)}</strong>
              <small>Agent-created profiles</small>
            </button>
            <button type="button" className="enterprise-kpi">
              <Icon name="star" />
              <span>Avg rating</span>
              <strong>{averageRating.toFixed(2)}</strong>
              <small>Live advisor quality</small>
            </button>
          </div>

          <div className="enterprise-filter-bar">
            <strong>Agent Queue</strong>
            <span className="mini-token">Approve or reject KYC</span>
            <span className="mini-token">Open 360 view for profile, bank, plans, and members</span>
          </div>

          <div className="enterprise-panel enterprise-table">
            <table>
              <thead>
                <tr>
                  <th>Photo</th>
                  <th>Name / ID</th>
                  <th>City</th>
                  <th>Added</th>
                  <th>KYC Status</th>
                  <th>Bank</th>
                  <th>T&C</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((agent) => (
                  <tr key={agent.advisor_id}>
                    <td><ProfileAvatar profile={agent} /></td>
                    <td><strong>{agent.full_name}</strong><small>{agent.agent_code || agent.advisor_id?.slice(0, 8)}</small></td>
                    <td>{agent.city || '-'}</td>
                    <td>{agent.profilesAdded}</td>
                    <td><StatusPill status={agent.kyc_status}>{agent.kyc_status}</StatusPill></td>
                    <td><StatusPill status={agent.bank_verification_status}>{agent.bank_verification_status || 'not_started'}</StatusPill></td>
                    <td><StatusPill status={agent.terms_accepted_at ? 'approved' : 'pending'}>{agent.terms_accepted_at ? 'Signed' : 'Pending'}</StatusPill></td>
                    <td>
                      <div className="row-actions">
                        <button onClick={() => onOpen(agent)} title="360 view"><Icon name="eye" /></button>
                        <button onClick={() => onStatus(agent, { kycStatus: 'approved', status: 'active' })} title="Approve"><Icon name="check" /></button>
                        <button onClick={() => onStatus(agent, { kycStatus: 'rejected' })} title="Reject"><Icon name="ban" /></button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
