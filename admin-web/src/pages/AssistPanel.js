import React, { useEffect, useMemo, useState } from 'react';

const EMPTY_ADVISOR_FORM = {
  fullName: '',
  phone: '',
  email: '',
  serviceLabel: 'SoulMatch Advisor',
  bio: '',
  gender: '',
  city: '',
  state: '',
  locality: '',
  pincode: '',
  radiusKm: '15',
  languages: '',
  communities: '',
  maxActiveAssignments: '25',
  membershipPlan: 'starter',
  notes: '',
  extraServiceAreasJson: ''
};

const REQUEST_STATUS_OPTIONS = ['waiting_assignment', 'assigned', 'paused', 'not_requested'];

function compactNumber(value) {
  const n = Number(value || 0);
  if (!Number.isFinite(n)) return '0';
  if (n >= 100000) return `${(n / 100000).toFixed(1)}L`;
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`;
  return String(Math.round(n));
}

function parseList(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function normalizeJsonText(value, fallback = []) {
  const trimmed = String(value || '').trim();
  if (!trimmed) return fallback;
  const parsed = JSON.parse(trimmed);
  return Array.isArray(parsed) ? parsed : fallback;
}

function statusTone(status) {
  if (['active', 'approved', 'assigned'].includes(status)) return 'ok';
  if (['paused', 'waiting_assignment', 'pending'].includes(status)) return 'amber';
  if (['suspended', 'rejected'].includes(status)) return 'danger';
  return 'neutral';
}

function requestStatusLabel(value) {
  return String(value || 'not_requested').replace(/_/g, ' ');
}

function StatusBadge({ tone = 'neutral', children }) {
  return <span className={`status-badge ${tone}`}>{children}</span>;
}

function buildAdvisorPayload(form) {
  const primaryArea = form.city
    ? [{
      city: form.city,
      state: form.state || null,
      locality: form.locality || null,
      pincode: form.pincode || null,
      radiusKm: Number(form.radiusKm || 15) || 15,
      priority: 10,
      isPrimary: true
    }]
    : [];
  const extraAreas = normalizeJsonText(form.extraServiceAreasJson, []).map((item, index) => ({
    city: item.city || form.city || '',
    state: item.state || form.state || null,
    locality: item.locality || null,
    pincode: item.pincode || null,
    radiusKm: Number(item.radiusKm ?? item.radius_km ?? 15) || 15,
    priority: Number(item.priority ?? Math.max(9 - index, 1)) || 1,
    isPrimary: item.isPrimary === true || item.is_primary === true
  })).filter((item) => item.city);

  return {
    fullName: form.fullName.trim(),
    phone: form.phone.trim(),
    email: form.email.trim() || null,
    serviceLabel: form.serviceLabel.trim() || 'SoulMatch Advisor',
    bio: form.bio.trim() || null,
    gender: form.gender || null,
    city: form.city.trim(),
    state: form.state.trim() || null,
    locality: form.locality.trim() || null,
    pincode: form.pincode.trim() || null,
    radiusKm: Number(form.radiusKm || 15) || 15,
    languages: parseList(form.languages),
    communities: parseList(form.communities),
    maxActiveAssignments: Number(form.maxActiveAssignments || 25) || 25,
    membershipPlan: form.membershipPlan.trim() || 'starter',
    notes: form.notes.trim() || null,
    serviceAreas: [...primaryArea, ...extraAreas]
  };
}

function useAssignmentDrafts(assignments) {
  return useMemo(
    () => assignments.reduce((acc, item) => {
      acc[item.assisted_profile_id] = {
        advisorId: item.advisor_id || '',
        requestStatus: item.request_status || 'waiting_assignment',
        notes: item.notes || ''
      };
      return acc;
    }, {}),
    [assignments]
  );
}

export default function AssistPanel({
  advisors,
  assignments,
  assistConfig,
  onSaveConfig,
  onCreateAdvisor,
  onUpdateAdvisor,
  onUpdateAdvisorStatus,
  onUpdateAssignment,
  canManageAdvisors,
  canManageAssignments
}) {
  const [advisorForm, setAdvisorForm] = useState(EMPTY_ADVISOR_FORM);
  const [editingAdvisorId, setEditingAdvisorId] = useState('');
  const [configDraft, setConfigDraft] = useState(assistConfig);
  const initialDrafts = useAssignmentDrafts(assignments);
  const [assignmentDrafts, setAssignmentDrafts] = useState(initialDrafts);

  useEffect(() => {
    setConfigDraft(assistConfig);
  }, [assistConfig]);

  useEffect(() => {
    setAssignmentDrafts(initialDrafts);
  }, [initialDrafts]);

  const activeApprovedAdvisors = useMemo(
    () => advisors.filter((advisor) => advisor.status === 'active' && advisor.kyc_status === 'approved'),
    [advisors]
  );

  const stats = useMemo(() => ({
    activeAdvisors: advisors.filter((advisor) => advisor.status === 'active').length,
    pendingKyc: advisors.filter((advisor) => advisor.kyc_status === 'pending').length,
    waitingAssignments: assignments.filter((item) => item.request_status === 'waiting_assignment').length,
    assignedMembers: assignments.filter((item) => item.request_status === 'assigned').length
  }), [advisors, assignments]);

  const resetAdvisorForm = () => {
    setEditingAdvisorId('');
    setAdvisorForm(EMPTY_ADVISOR_FORM);
  };

  const startEditAdvisor = (advisor) => {
    setEditingAdvisorId(advisor.advisor_id);
    setAdvisorForm({
      fullName: advisor.full_name || '',
      phone: advisor.phone || '',
      email: advisor.email || '',
      serviceLabel: advisor.service_label || 'SoulMatch Advisor',
      bio: advisor.bio || '',
      gender: advisor.gender || '',
      city: advisor.city || '',
      state: advisor.state || '',
      locality: advisor.service_areas?.[0]?.locality || '',
      pincode: advisor.pincode || advisor.service_areas?.[0]?.pincode || '',
      radiusKm: String(advisor.service_areas?.[0]?.radiusKm || advisor.service_areas?.[0]?.radius_km || 15),
      languages: (advisor.languages || []).join(', '),
      communities: (advisor.communities || []).join(', '),
      maxActiveAssignments: String(advisor.max_active_assignments || 25),
      membershipPlan: advisor.membership_plan || 'starter',
      notes: advisor.notes || '',
      extraServiceAreasJson: JSON.stringify((advisor.service_areas || []).slice(1), null, 2)
    });
  };

  const saveAdvisor = async () => {
    try {
      const payload = buildAdvisorPayload(advisorForm);
      if (editingAdvisorId) {
        await onUpdateAdvisor(editingAdvisorId, payload);
      } else {
        await onCreateAdvisor(payload);
      }
      resetAdvisorForm();
    } catch (error) {
      window.alert(error?.message || 'Additional service areas JSON is invalid.');
    }
  };

  const updateAssignmentDraft = (assistedProfileId, patch) => {
    setAssignmentDrafts((current) => ({
      ...current,
      [assistedProfileId]: {
        ...(current[assistedProfileId] || {}),
        ...patch
      }
    }));
  };

  return (
    <div className="admin-content enterprise-screen assist-page">
      <div className="enterprise-page-head">
        <div>
          <h2>Assisted Matchmaking</h2>
          <p>Advisor operations, program settings, trusted local network, and assignment queue.</p>
        </div>
        <div className="enterprise-actions">
          <button className="secondary-btn" type="button">Export Queue</button>
        </div>
      </div>

      <div className="enterprise-kpi-grid">
        <button type="button" className="enterprise-kpi"><span>Active advisors</span><strong>{compactNumber(stats.activeAdvisors)}</strong><small>Currently taking SoulMatch Assist leads</small></button>
        <button type="button" className="enterprise-kpi warning"><span>KYC pending</span><strong>{compactNumber(stats.pendingKyc)}</strong><small>Needs trust approval before assignments</small></button>
        <button type="button" className="enterprise-kpi warning"><span>Waiting assignment</span><strong>{compactNumber(stats.waitingAssignments)}</strong><small>Families opted in but no advisor yet</small></button>
        <button type="button" className="enterprise-kpi success"><span>Assigned members</span><strong>{compactNumber(stats.assignedMembers)}</strong><small>Live assisted matchmaking relationships</small></button>
      </div>

      <div className="two-col">
        <section className="panel-section">
          <div className="section-header">
            <div>
              <div className="eyebrow">Advisor operations</div>
              <h2>{editingAdvisorId ? 'Edit Advisor' : 'Onboard Advisor'}</h2>
              <p>Build a trusted hyperlocal operator network with KYC approval, service areas, and active capacity.</p>
            </div>
            {editingAdvisorId ? (
              <div className="section-actions">
                <button className="secondary-btn" onClick={resetAdvisorForm} type="button">Cancel edit</button>
              </div>
            ) : null}
          </div>
          <div className="form-grid">
            <label className="field"><span>Full name</span><input className="input" value={advisorForm.fullName} onChange={(event) => setAdvisorForm({ ...advisorForm, fullName: event.target.value })} /></label>
            <label className="field"><span>Phone</span><input className="input" value={advisorForm.phone} onChange={(event) => setAdvisorForm({ ...advisorForm, phone: event.target.value })} /></label>
            <label className="field"><span>Email</span><input className="input" value={advisorForm.email} onChange={(event) => setAdvisorForm({ ...advisorForm, email: event.target.value })} /></label>
            <label className="field"><span>Service label</span><input className="input" value={advisorForm.serviceLabel} onChange={(event) => setAdvisorForm({ ...advisorForm, serviceLabel: event.target.value })} /></label>
            <label className="field"><span>City</span><input className="input" value={advisorForm.city} onChange={(event) => setAdvisorForm({ ...advisorForm, city: event.target.value })} /></label>
            <label className="field"><span>State</span><input className="input" value={advisorForm.state} onChange={(event) => setAdvisorForm({ ...advisorForm, state: event.target.value })} /></label>
            <label className="field"><span>Locality</span><input className="input" value={advisorForm.locality} onChange={(event) => setAdvisorForm({ ...advisorForm, locality: event.target.value })} /></label>
            <label className="field"><span>Pincode</span><input className="input" value={advisorForm.pincode} onChange={(event) => setAdvisorForm({ ...advisorForm, pincode: event.target.value })} /></label>
            <label className="field"><span>Radius km</span><input className="input" value={advisorForm.radiusKm} onChange={(event) => setAdvisorForm({ ...advisorForm, radiusKm: event.target.value })} /></label>
            <label className="field"><span>Languages</span><input className="input" value={advisorForm.languages} onChange={(event) => setAdvisorForm({ ...advisorForm, languages: event.target.value })} placeholder="Telugu, English" /></label>
            <label className="field"><span>Communities</span><input className="input" value={advisorForm.communities} onChange={(event) => setAdvisorForm({ ...advisorForm, communities: event.target.value })} placeholder="Hindu, Reddy" /></label>
            <label className="field"><span>Max active assignments</span><input className="input" value={advisorForm.maxActiveAssignments} onChange={(event) => setAdvisorForm({ ...advisorForm, maxActiveAssignments: event.target.value })} /></label>
            <label className="field"><span>Membership plan</span><input className="input" value={advisorForm.membershipPlan} onChange={(event) => setAdvisorForm({ ...advisorForm, membershipPlan: event.target.value })} /></label>
            <label className="field field-full"><span>Bio</span><textarea className="textarea" value={advisorForm.bio} onChange={(event) => setAdvisorForm({ ...advisorForm, bio: event.target.value })} /></label>
            <label className="field field-full"><span>Notes</span><textarea className="textarea" value={advisorForm.notes} onChange={(event) => setAdvisorForm({ ...advisorForm, notes: event.target.value })} /></label>
            <label className="field field-full">
              <span>Additional service areas (JSON optional)</span>
              <textarea
                className="textarea"
                value={advisorForm.extraServiceAreasJson}
                onChange={(event) => setAdvisorForm({ ...advisorForm, extraServiceAreasJson: event.target.value })}
                placeholder='[{"city":"Hyderabad","locality":"Madhapur","pincode":"500081","radiusKm":10}]'
              />
              <small>Use only when the advisor covers multiple cities or pin-code clusters.</small>
            </label>
          </div>
          <button className="primary-btn" disabled={!canManageAdvisors} onClick={saveAdvisor} type="button">
            {editingAdvisorId ? 'Save advisor' : 'Create advisor'}
          </button>
        </section>

        <section className="panel-section">
          <div className="section-header">
            <div>
              <div className="eyebrow">Assist controls</div>
              <h2>Program Settings</h2>
              <p>Decide whether SoulMatch Assist is live, how often requests are reviewed, and what advisor plans are visible to ops.</p>
            </div>
          </div>
          <div className="toggle-row">
            <div>
              <strong>Assist enabled</strong>
              <small>Turn advisor-assisted matchmaking on or off without removing data.</small>
            </div>
            <input
              checked={configDraft?.enabled !== false}
              onChange={(event) => setConfigDraft((current) => ({ ...current, enabled: event.target.checked }))}
              type="checkbox"
            />
          </div>
          <div className="form-grid" style={{ marginTop: 16 }}>
            <label className="field">
              <span>Default review days</span>
              <input
                className="input"
                value={String(configDraft?.defaultReviewDays ?? 7)}
                onChange={(event) => setConfigDraft((current) => ({ ...current, defaultReviewDays: Number(event.target.value || 7) || 7 }))}
              />
            </label>
            <label className="field field-full">
              <span>Member modes</span>
              <input
                className="input"
                value={(configDraft?.memberModes || []).join(', ')}
                onChange={(event) => setConfigDraft((current) => ({ ...current, memberModes: parseList(event.target.value) }))}
              />
            </label>
          </div>
          <div className="plan-grid">
            {(configDraft?.advisorPlans || []).map((plan) => (
              <div className="plan-card" key={plan.planId}>
                <div>
                  <strong>{plan.name}</strong>
                  <small>{plan.planId}</small>
                </div>
                <StatusBadge tone="neutral">INR {plan.monthlyPrice}/month</StatusBadge>
                <small>{plan.maxActiveProfiles} active assisted members</small>
                <ul className="assist-bullet-list">
                  {(plan.features || []).map((feature) => <li key={feature}>{feature}</li>)}
                </ul>
              </div>
            ))}
          </div>
          <button className="primary-btn" disabled={!canManageAdvisors} onClick={() => onSaveConfig(configDraft)} type="button">
            Save assist program settings
          </button>
        </section>
      </div>

      <section className="panel-section">
        <div className="section-header">
          <div>
            <div className="eyebrow">Advisor roster</div>
            <h2>Trusted Local Network</h2>
            <p>Approve trusted advisors, pause low-capacity operators, and keep every service area accountable.</p>
          </div>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Advisor</th>
                <th>Coverage</th>
                <th>Trust</th>
                <th>Capacity</th>
                <th>Plan</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {advisors.length ? advisors.map((advisor) => (
                <tr key={advisor.advisor_id}>
                  <td>
                    <strong>{advisor.full_name}</strong>
                    <small>{advisor.phone}{advisor.email ? ` / ${advisor.email}` : ''}</small>
                    <small>{(advisor.languages || []).join(', ') || 'No language tags yet'}</small>
                  </td>
                  <td>
                    <strong>{advisor.city}{advisor.state ? `, ${advisor.state}` : ''}</strong>
                    <small>{advisor.service_areas?.length || 0} service areas</small>
                    <small>{(advisor.communities || []).join(', ') || 'Open community coverage'}</small>
                  </td>
                  <td>
                    <StatusBadge tone={statusTone(advisor.kyc_status)}>{advisor.kyc_status}</StatusBadge>
                    <small style={{ marginTop: 6 }}><StatusBadge tone={statusTone(advisor.status)}>{advisor.status}</StatusBadge></small>
                    <div style={{ marginTop: 8, display: 'grid', gap: 6 }}>
                      {(advisor.kyc_documents || []).length ? advisor.kyc_documents.map((document) => (
                        <div key={document.advisorKycDocumentId} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                          <small>
                            {String(document.documentType || '').replace(/_/g, ' ')} / {String(document.documentSide || 'single').replace(/_/g, ' ')}
                          </small>
                          <small><StatusBadge tone={statusTone(document.status)}>{document.status}</StatusBadge></small>
                          {document.fileUrl ? (
                            <a href={document.fileUrl} target="_blank" rel="noreferrer">View upload</a>
                          ) : null}
                        </div>
                      )) : <small>No KYC uploads</small>}
                    </div>
                  </td>
                  <td>
                    <strong>{compactNumber(advisor.active_assignments)} / {compactNumber(advisor.max_active_assignments)}</strong>
                    <small>rating {advisor.average_rating || 0} / complaints {advisor.complaint_score || 0}</small>
                  </td>
                  <td>
                    <strong>{advisor.membership_plan || 'starter'}</strong>
                    <small>{advisor.membership_expires_at ? String(advisor.membership_expires_at).slice(0, 10) : 'No expiry set'}</small>
                  </td>
                  <td>
                    <div className="inline-actions">
                      <button onClick={() => startEditAdvisor(advisor)} type="button">Edit</button>
                      <button disabled={!canManageAdvisors || advisor.kyc_status === 'approved'} onClick={() => onUpdateAdvisorStatus(advisor.advisor_id, { kycStatus: 'approved' })} type="button">Approve</button>
                      <button disabled={!canManageAdvisors || advisor.kyc_status === 'pending'} onClick={() => onUpdateAdvisorStatus(advisor.advisor_id, { kycStatus: 'pending' })} type="button">In Progress</button>
                      <button disabled={!canManageAdvisors || advisor.kyc_status === 'rejected'} onClick={() => onUpdateAdvisorStatus(advisor.advisor_id, { kycStatus: 'rejected' })} type="button">Decline</button>
                      <button disabled={!canManageAdvisors || advisor.status === 'active'} onClick={() => onUpdateAdvisorStatus(advisor.advisor_id, { status: 'active' })} type="button">Activate</button>
                      <button disabled={!canManageAdvisors || advisor.status === 'paused'} onClick={() => onUpdateAdvisorStatus(advisor.advisor_id, { status: 'paused' })} type="button">Pause</button>
                    </div>
                  </td>
                </tr>
              )) : (
                <tr>
                  <td colSpan={6}>
                    <div className="empty-state">No advisors onboarded yet.</div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel-section">
        <div className="section-header">
          <div>
            <div className="eyebrow">Assignment queue</div>
            <h2>Families Waiting for Help</h2>
            <p>Allocate profiles by locality and trust readiness, then keep the queue moving with pause and reassignment controls.</p>
          </div>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Member</th>
                <th>Location</th>
                <th>Mode</th>
                <th>Advisor</th>
                <th>Status</th>
                <th>Notes</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {assignments.length ? assignments.map((item) => {
                const draft = assignmentDrafts[item.assisted_profile_id] || {};
                return (
                  <tr key={item.assisted_profile_id}>
                    <td>
                      <strong>{[item.first_name, item.last_name].filter(Boolean).join(' ') || 'Unnamed member'}</strong>
                      <small>{item.religion || '-'} / {item.caste || '-'}</small>
                      <small>{item.family_contact_name || 'No family contact'}{item.family_contact_phone ? ` / ${item.family_contact_phone}` : ''}</small>
                    </td>
                    <td>
                      <strong>{item.family_locality || item.family_city || 'Location pending'}</strong>
                      <small>{[item.family_city, item.family_state].filter(Boolean).join(', ') || 'Family city not shared yet'}</small>
                      <small>{item.family_pincode || 'No pincode'}</small>
                    </td>
                    <td>
                      <StatusBadge tone="neutral">{requestStatusLabel(item.support_level)}</StatusBadge>
                    </td>
                    <td>
                      <select className="input" value={draft.advisorId || ''} onChange={(event) => updateAssignmentDraft(item.assisted_profile_id, { advisorId: event.target.value })}>
                        <option value="">Unassigned</option>
                        {activeApprovedAdvisors.map((advisor) => (
                          <option key={advisor.advisor_id} value={advisor.advisor_id}>
                            {advisor.full_name} - {advisor.city}
                          </option>
                        ))}
                      </select>
                      <small>{item.advisor_name ? `Current: ${item.advisor_name}` : 'No advisor attached yet'}</small>
                    </td>
                    <td>
                      <select className="input" value={draft.requestStatus || 'waiting_assignment'} onChange={(event) => updateAssignmentDraft(item.assisted_profile_id, { requestStatus: event.target.value })}>
                        {REQUEST_STATUS_OPTIONS.map((status) => (
                          <option key={status} value={status}>{requestStatusLabel(status)}</option>
                        ))}
                      </select>
                      <small><StatusBadge tone={statusTone(item.request_status)}>{requestStatusLabel(item.request_status)}</StatusBadge></small>
                    </td>
                    <td>
                      <textarea className="textarea assist-notes-input" value={draft.notes || ''} onChange={(event) => updateAssignmentDraft(item.assisted_profile_id, { notes: event.target.value })} />
                    </td>
                    <td>
                      <button
                        className="primary-btn"
                        disabled={!canManageAssignments}
                        onClick={() => onUpdateAssignment(item.assisted_profile_id, draft)}
                        type="button"
                      >
                        Save
                      </button>
                    </td>
                  </tr>
                );
              }) : (
                <tr>
                  <td colSpan={7}>
                    <div className="empty-state">No assisted members are currently opted in.</div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
