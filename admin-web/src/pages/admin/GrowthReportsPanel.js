import React, { useMemo } from 'react';
import {
  EmptyState,
  Icon,
  SectionHeader,
  StatusPill
} from '../../components/AdminPrimitives';

function formatCurrency(value) {
  const amount = Number(value || 0);
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount);
}

function number(value) {
  return new Intl.NumberFormat('en-IN').format(Number(value || 0));
}

function percent(value) {
  return `${Number(value || 0).toFixed(1)}%`;
}

function dateOnly(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function planFor(profile) {
  return String(profile.plan_id || profile.membership_plan || 'free').toLowerCase();
}

function GrowthMetric({ icon, label, value, sub, tone }) {
  return (
    <article className={`growth-metric ${tone || ''}`}>
      <span><Icon name={icon} /> {label}</span>
      <strong>{value}</strong>
      <small>{sub}</small>
    </article>
  );
}

function BreakdownBar({ label, value, total }) {
  const width = total > 0 ? Math.round((value / total) * 100) : 0;
  return (
    <article className="breakdown-row">
      <div>
        <strong>{label}</strong>
        <span>{number(value)} users</span>
      </div>
      <div className="breakdown-track"><span style={{ width: `${width}%` }} /></div>
      <em>{width}%</em>
    </article>
  );
}

export default function GrowthReportsPanel({ stats, profiles, advisors, payments, reports, funnel, events }) {
  const report = useMemo(() => {
    const profileTotal = profiles.length || Number(stats.totalProfiles || 0);
    const planCounts = profiles.reduce((acc, profile) => {
      const plan = planFor(profile);
      acc[plan] = (acc[plan] || 0) + 1;
      return acc;
    }, {});
    const sourceCounts = profiles.reduce((acc, profile) => {
      const source = profile.created_by_advisor_id ? 'Agent created' : (profile.profile_created_by === 'admin' ? 'Admin created' : 'Self signup');
      acc[source] = (acc[source] || 0) + 1;
      return acc;
    }, {});
    const verificationBacklog = profiles.filter((profile) => String(profile.verification_status || 'pending').toLowerCase() === 'pending').length;
    const hiddenProfiles = profiles.filter((profile) => !profile.is_published).length;
    const openReports = reports.filter((item) => !['resolved', 'closed'].includes(String(item.status || '').toLowerCase()));
    const transactions = Array.isArray(payments.transactions) ? payments.transactions : [];
    const successfulPayments = transactions.filter((item) => ['paid', 'success', 'captured'].includes(String(item.status || '').toLowerCase()));
    const revenue = successfulPayments.reduce((sum, item) => sum + Number(item.amount || item.amount_paid || 0), 0);
    const agentProfiles = profiles.filter((profile) => profile.created_by_advisor_id).length;
    const activeAgents = advisors.filter((advisor) => String(advisor.status || 'active').toLowerCase() === 'active').length;
    return {
      profileTotal,
      planCounts,
      sourceCounts,
      verificationBacklog,
      hiddenProfiles,
      openReports,
      successfulPayments,
      revenue,
      agentProfiles,
      activeAgents
    };
  }, [profiles, advisors, payments, reports, stats.totalProfiles]);

  const funnelRows = Array.isArray(funnel) && funnel.length
    ? funnel
    : [
      { step: 'Signups', value: stats.analytics?.signups || stats.newUsersToday || 0 },
      { step: 'Payment clicks', value: stats.analytics?.paymentClicks || 0 },
      { step: 'Payment success', value: stats.analytics?.paymentSuccesses || stats.premiumUsers || 0 },
      { step: 'Matches made', value: stats.analytics?.matchesMade || 0 }
    ];
  const funnelMax = Math.max(...funnelRows.map((row) => Number(row.value || row.count || 0)), 1);
  const recentEvents = Array.isArray(events) ? events.slice(0, 8) : [];

  return (
    <div className="admin-content growth-reports-page">
      <SectionHeader
        eyebrow="Business Intelligence"
        title="Growth Reports"
        description="Acquisition, revenue, conversion, moderation and operator workload in one place."
      />

      <div className="growth-metrics-grid">
        <GrowthMetric icon="users" label="Members" value={number(report.profileTotal)} sub={`${number(stats.activeProfiles)} active profiles`} tone="ink" />
        <GrowthMetric icon="trend" label="DAU / MAU" value={`${number(stats.dau)} / ${number(stats.mau)}`} sub="Realtime activity snapshot" tone="steel" />
        <GrowthMetric icon="rupee" label="Revenue" value={formatCurrency(stats.revenue30d || report.revenue)} sub={`${number(report.successfulPayments.length)} successful payments`} tone="gold" />
        <GrowthMetric icon="target" label="Conversion" value={percent(stats.conversionRate)} sub={`${percent(stats.matchSuccessRate)} match success`} tone="success" />
        <GrowthMetric icon="flag" label="Risk Queue" value={number(report.openReports.length + report.verificationBacklog)} sub={`${number(report.openReports.length)} reports open`} tone="danger" />
      </div>

      <div className="workspace-columns even growth-grid">
        <section className="admin-card report-card">
          <div className="card-title-row">
            <div>
              <h3>Acquisition Mix</h3>
              <small>Where members are entering the system.</small>
            </div>
          </div>
          <div className="report-breakdowns">
            {Object.entries(report.sourceCounts).map(([label, value]) => (
              <BreakdownBar key={label} label={label} value={value} total={report.profileTotal} />
            ))}
            {!Object.keys(report.sourceCounts).length ? <EmptyState title="No acquisition data" body="Profiles will appear here after signups or admin imports." /> : null}
          </div>
        </section>

        <section className="admin-card report-card">
          <div className="card-title-row">
            <div>
              <h3>Plan Health</h3>
              <small>Free to paid distribution for upgrade strategy.</small>
            </div>
          </div>
          <div className="report-breakdowns">
            {Object.entries(report.planCounts).map(([label, value]) => (
              <BreakdownBar key={label} label={label} value={value} total={report.profileTotal} />
            ))}
            {!Object.keys(report.planCounts).length ? <EmptyState title="No plan data" body="Membership plans will appear as profiles are loaded." /> : null}
          </div>
        </section>

        <section className="admin-card report-card">
          <div className="card-title-row">
            <div>
              <h3>Conversion Funnel</h3>
              <small>Signup to payment to match progress.</small>
            </div>
          </div>
          <div className="funnel-list">
            {funnelRows.map((row, index) => {
              const label = row.step || row.event_name || row.name || `Step ${index + 1}`;
              const value = Number(row.value || row.count || 0);
              return (
                <article key={`${label}-${index}`}>
                  <span>{label}</span>
                  <div><em style={{ width: `${Math.max(6, Math.round((value / funnelMax) * 100))}%` }} /></div>
                  <strong>{number(value)}</strong>
                </article>
              );
            })}
          </div>
        </section>

        <section className="admin-card report-card">
          <div className="card-title-row">
            <div>
              <h3>Operations Load</h3>
              <small>Queues that directly affect trust and revenue.</small>
            </div>
          </div>
          <div className="ops-report-grid">
            <article><strong>{number(report.verificationBacklog)}</strong><span>Pending verifications</span></article>
            <article><strong>{number(report.hiddenProfiles)}</strong><span>Hidden profiles</span></article>
            <article><strong>{number(report.openReports.length)}</strong><span>Open reports</span></article>
            <article><strong>{number(report.agentProfiles)}</strong><span>Agent-created profiles</span></article>
            <article><strong>{number(report.activeAgents)}</strong><span>Active agents</span></article>
          </div>
        </section>
      </div>

      <div className="workspace-columns even growth-grid">
        <section className="admin-card report-card data-table compact-table">
          <div className="card-title-row">
            <div>
              <h3>Recent Payment Signals</h3>
              <small>Latest successful or attempted transactions.</small>
            </div>
          </div>
          <table>
            <thead><tr><th>Date</th><th>User</th><th>Amount</th><th>Status</th></tr></thead>
            <tbody>
              {(payments.transactions || []).slice(0, 8).map((txn, index) => (
                <tr key={txn.payment_id || txn.order_id || index}>
                  <td>{dateOnly(txn.created_at || txn.paid_at)}</td>
                  <td>{txn.user_name || txn.email || txn.user_id || '-'}</td>
                  <td>{formatCurrency(txn.amount || txn.amount_paid)}</td>
                  <td><StatusPill status={txn.status}>{txn.status || 'created'}</StatusPill></td>
                </tr>
              ))}
            </tbody>
          </table>
          {!(payments.transactions || []).length ? <EmptyState title="No payments found" body="Payment activity will appear here after checkout events." /> : null}
        </section>

        <section className="admin-card report-card data-table compact-table">
          <div className="card-title-row">
            <div>
              <h3>Recent Product Events</h3>
              <small>Latest tracked business events from analytics.</small>
            </div>
          </div>
          <table>
            <thead><tr><th>Event</th><th>User</th><th>Date</th><th>Status</th></tr></thead>
            <tbody>
              {recentEvents.map((event, index) => (
                <tr key={event.event_id || index}>
                  <td>{event.event_name || event.name || '-'}</td>
                  <td>{event.user_id || event.profile_id || '-'}</td>
                  <td>{dateOnly(event.created_at || event.event_time)}</td>
                  <td><StatusPill status={event.status || 'active'}>{event.status || 'Tracked'}</StatusPill></td>
                </tr>
              ))}
            </tbody>
          </table>
          {!recentEvents.length ? <EmptyState title="No analytics events" body="Tracked product events will appear after users interact with the app." /> : null}
        </section>
      </div>
    </div>
  );
}
