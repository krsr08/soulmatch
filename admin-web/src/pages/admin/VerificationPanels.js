import React from 'react';
import {
  AdminButton,
  EmptyState,
  Icon
} from '../../components/AdminPrimitives';

function fullName(row) {
  return [row?.first_name || row?.firstName, row?.last_name || row?.lastName].filter(Boolean).join(' ') || row?.full_name || 'Unnamed';
}

function titleFromKey(value) {
  return String(value || '')
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function VerificationPanel({
  verifications,
  profileDocuments = [],
  advisors,
  profiles,
  onApprove,
  onReject,
  onApproveDocument,
  onRejectDocument,
  onAgentStatus,
  onProfileStatus
}) {
  const pendingProfiles = profiles.filter((profile) =>
    ['pending', 'submitted', 'under_review'].includes(String(profile.verification_status || profile.review_status || '').toLowerCase())
  );
  const pendingAgents = advisors.filter((agent) => agent.kyc_status === 'pending' || ['pending', 'more_info'].includes(agent.onboarding_status));
  const pendingAgentCreatedProfiles = pendingProfiles.filter((profile) => profile.created_by_advisor_id);

  return (
    <div className="admin-content enterprise-screen verification-page">
      <div className="enterprise-page-head">
        <div>
          <h2>Verification Command Center</h2>
          <p>Review member KYC, agent onboarding, documents and profile visibility before they go live.</p>
        </div>
        <div className="enterprise-actions">
          <AdminButton variant="secondary"><Icon name="export" /> Export Queue</AdminButton>
        </div>
      </div>

      <div className="enterprise-kpi-grid">
        <button type="button" className="enterprise-kpi warning">
          <Icon name="person" />
          <span>Member KYC</span>
          <strong>{verifications.length}</strong>
          <small>Trust records waiting for review</small>
        </button>
        <button type="button" className="enterprise-kpi">
          <Icon name="image" />
          <span>Uploaded docs</span>
          <strong>{profileDocuments.length}</strong>
          <small>PAN, Aadhaar, education and profile documents</small>
        </button>
        <button type="button" className="enterprise-kpi warning">
          <Icon name="agent" />
          <span>Agent KYC</span>
          <strong>{pendingAgents.length}</strong>
          <small>Bank, onboarding and terms checks</small>
        </button>
        <button type="button" className="enterprise-kpi success">
          <Icon name="users" />
          <span>Agent profiles</span>
          <strong>{pendingAgentCreatedProfiles.length}</strong>
          <small>Agent-created profiles awaiting publish</small>
        </button>
      </div>

      <div className="verification-grid">
        <div className="enterprise-panel verification-section">
          <div className="enterprise-panel-head">
            <div>
              <h3>Member Verification Queue</h3>
              <p>KYC and trust records waiting for admin review.</p>
            </div>
          </div>
          <div className="review-list">
            {verifications.map((item) => (
              <article key={item.verification_id}>
                <span><strong>{fullName(item)}</strong><small>{item.type || 'Document'} | Trust {item.trust_score || 0}%</small></span>
                <div>
                  <AdminButton variant="secondary" onClick={() => onReject(item.verification_id)}>Reject</AdminButton>
                  <AdminButton variant="primary" onClick={() => onApprove(item.verification_id)}>Approve</AdminButton>
                </div>
              </article>
            ))}
            {!verifications.length ? <EmptyState title="No verification records" body="There are no member KYC records pending review." /> : null}
          </div>
        </div>
        <div className="enterprise-panel verification-section">
          <div className="enterprise-panel-head">
            <div>
              <h3>Member Uploaded Documents</h3>
              <p>Approve the document or re-request a corrected copy with a notification.</p>
            </div>
          </div>
          <div className="review-list">
            {profileDocuments.map((document) => (
              <article key={document.profile_document_id}>
                <span><strong>{fullName(document)}</strong><small>{titleFromKey(document.document_type)} | {document.status} | {document.review_comment || 'No comment'}</small></span>
                <div>
                  <a href={document.file_url} target="_blank" rel="noreferrer">View</a>
                  <AdminButton variant="secondary" onClick={() => onRejectDocument(document.profile_document_id)}>Re-request</AdminButton>
                  <AdminButton variant="primary" onClick={() => onApproveDocument(document.profile_document_id)}>Approve</AdminButton>
                </div>
              </article>
            ))}
            {!profileDocuments.length ? <EmptyState title="No member documents" body="Uploaded PAN, Aadhaar, education and other profile documents appear here for review." /> : null}
          </div>
        </div>
        <div className="enterprise-panel verification-section">
          <div className="enterprise-panel-head">
            <div>
              <h3>Agent Verification Queue</h3>
              <p>Onboarding, bank checks, and signed terms.</p>
            </div>
          </div>
          <div className="review-list">
            {pendingAgents.map((agent) => (
              <article key={agent.advisor_id}>
                <span><strong>{agent.full_name}</strong><small>{agent.city} | Bank {agent.bank_verification_status || 'not started'} | T&C {agent.terms_accepted_at ? 'signed' : 'pending'}</small></span>
                <div>
                  <AdminButton variant="secondary" onClick={() => onAgentStatus(agent, { kycStatus: 'rejected' })}>Reject</AdminButton>
                  <AdminButton variant="primary" onClick={() => onAgentStatus(agent, { kycStatus: 'approved', status: 'active' })}>Approve</AdminButton>
                </div>
              </article>
            ))}
            {!pendingAgents.length ? <EmptyState title="No agent onboarding pending" body="All registered agents are currently reviewed." /> : null}
          </div>
        </div>
        <div className="enterprise-panel verification-section full">
          <div className="enterprise-panel-head">
            <div>
              <h3>Agent-created Member Profiles</h3>
              <p>Profiles submitted by agents before they become visible to members.</p>
            </div>
          </div>
          <div className="review-list">
            {pendingAgentCreatedProfiles.map((profile) => (
              <article key={profile.profile_id}>
                <span><strong>{fullName(profile)}</strong><small>Added by {profile.advisor_name || 'agent'} | {profile.review_status || profile.verification_status}</small></span>
                <div>
                  <AdminButton variant="secondary" onClick={() => onProfileStatus(profile, 'reject')}>Reject</AdminButton>
                  <AdminButton variant="primary" onClick={() => onProfileStatus(profile, 'approve')}>Publish</AdminButton>
                </div>
              </article>
            ))}
            {!pendingAgentCreatedProfiles.length ? <EmptyState title="No agent-created profiles pending" body="Published profiles will move into managed profiles after verification." /> : null}
          </div>
        </div>
      </div>
    </div>
  );
}
