import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { io } from 'socket.io-client';
import {
  ADMIN_SOCKET_URL,
  acknowledgeAlert,
  addProfilePhoto,
  approveProfileDocument,
  approveVerification,
  banUser,
  createAdminUser,
  createAdvisor,
  createProfile,
  deleteProfile,
  deleteProfilePhoto,
  deleteAdminUser,
  getAdvisors,
  getAdminUsers,
  getAlerts,
  getAnalyticsEvents,
  getAnalyticsFunnel,
  getAssistedAssignments,
  getAuditLogs,
  getConfig,
  getConsentEvents,
  getDashboard,
  getModerationInbox,
  getPayments,
  getProfiles,
  getProfileDocuments,
  getRealtimeSnapshot,
  getReports,
  getRoles,
  getServiceHealth,
  getSystemInventory,
  getUsers,
  getVerifications,
  rejectProfileDocument,
  rejectVerification,
  resolveReport,
  unbanUser,
  updateAdminUser,
  updateAdvisor,
  updateAdvisorStatus,
  updateAssistedAssignment,
  updateConfig,
  updateProfile,
  updateProfilePhoto,
  updateProfileStatus
} from '../api/adminApi';
import {
  AdminButton,
  EmptyState,
  Icon,
  ManagementToolbar,
  ProfileAvatar,
  SectionHeader,
  StatCard,
  StatusPill
} from '../components/AdminPrimitives';
import AdminShell from '../components/AdminShell';
import { AgentsPanel } from './admin/AgentsPanels';
import GrowthReportsPanel from './admin/GrowthReportsPanel';
import { MembersDirectoryPanel, MembersPanel } from './admin/MembersPanels';
import UserControlPanel from './admin/UserControlPanel';
import { VerificationPanel } from './admin/VerificationPanels';
import AssistPanel from './AssistPanel';
import './DashboardPage.css';

const EMPTY_STATS = {
  totalUsers: 0,
  totalProfiles: 0,
  activeUsers: 0,
  activeProfiles: 0,
  pendingApprovals: 0,
  premiumUsers: 0,
  pendingReports: 0,
  newUsersToday: 0,
  totalRevenue: 0,
  revenue30d: 0,
  dau: 0,
  mau: 0,
  conversionRate: 0,
  matchSuccessRate: 0,
  analytics: { signups: 0, paymentClicks: 0, paymentSuccesses: 0, matchesMade: 0 },
  adminConsole: {
    members: {},
    agents: {},
    revenueTrend: [],
    queues: {},
    recentMembers: [],
    membershipBreakdown: [],
    agentLeaderboard: [],
    recentAgents: [],
    recentAudit: []
  }
};

const DEFAULT_CONFIG = {
  monetization: {
    currency: 'INR',
    accessMode: 'subscription',
    subscriptionModelEnabled: true,
    fixedPriceAmount: 200,
    fixedPricePlanId: 'fixed_access',
    fixedPriceLabel: '₹200',
    freeAccessLabel: 'Account',
    refundGuaranteeEnabled: true,
    refundGuaranteeTitle: '30-day full refund guarantee*',
    refundGuaranteeSubtitle: '*Conditions apply',
    plans: [],
    premiumLimits: {},
    upgradePackageGroups: [],
    membershipFeatureMatrix: []
  },
  assisted_matchmaking: { enabled: true, advisorPlans: [], memberModes: [] },
  admin_roles: { roles: [] },
  notification_templates: {},
  legal: {},
  branding: {},
  feature_flags: {},
  navigation: {},
  maintenance: {},
  client_integrations: {},
  experiments: {},
  payment_gateways: {},
  seo_defaults: {},
  analytics: {},
  content: {},
  theme: {}
};

const MENU_GROUPS = [
  {
    label: 'Command Center',
    items: [
      { id: 'dashboard', label: 'Overview', icon: 'grid' },
      { id: 'user-control', label: 'User Control', icon: 'users' },
      { id: 'agents', label: 'Agent Control', icon: 'agent' },
      { id: 'subscriptions', label: 'Plans & Revenue', icon: 'rupee' },
      { id: 'growth-reports', label: 'Growth Reports', icon: 'trend' },
      { id: 'analytics', label: 'Analytics', icon: 'pulse' }
    ]
  },
  {
    label: 'Operations',
    items: [
      { id: 'member-verify', label: 'Verify Member', icon: 'check' },
      { id: 'agent-verification', label: 'Agent Verification', icon: 'check' },
      { id: 'content', label: 'Reports & Moderation', icon: 'flag' },
      { id: 'assist', label: 'Assisted Matchmaking', icon: 'target' },
      { id: 'cms-management', label: 'CMS & Content', icon: 'cms' }
    ]
  },
  {
    label: 'System Control',
    items: [
      { id: 'dynamic-config', label: 'Configuration', icon: 'sliders' },
      { id: 'role-master', label: 'Role Master', icon: 'crown' },
      { id: 'user-master', label: 'User Master', icon: 'person' },
      { id: 'audit-logs', label: 'Audit Logs', icon: 'log' },
      { id: 'service-health', label: 'Service Health', icon: 'pulse' },
      { id: 'settings', label: 'Settings', icon: 'gear' },
      { id: 'change-password', label: 'Change Password', icon: 'key' }
    ]
  }
];

const ROUTE_TO_TAB = {
  overview: 'dashboard',
  dashboard: 'dashboard',
  'user-control': 'user-control',
  users: 'user-control',
  members: 'members',
  agents: 'agents',
  verification: 'member-verify',
  payments: 'member-payments',
  subscriptions: 'subscriptions',
  moderation: 'content',
  cms: 'cms-management',
  analytics: 'analytics',
  reports: 'growth-reports',
  'growth-reports': 'growth-reports',
  system: 'system',
  assist: 'assist'
};

const TAB_TO_ROUTE = {
  dashboard: 'overview',
  overview: 'overview',
  'user-control': 'user-control',
  members: 'members',
  'members-all': 'members',
  'member-profile': 'members',
  'member-signup': 'members',
  'member-password': 'members',
  'member-block': 'members',
  'member-verify': 'verification',
  'member-validity': 'members',
  agents: 'agents',
  'agents-all': 'agents',
  'agent-verification': 'verification',
  'agent-ratings': 'agents',
  'agent-performance': 'agents',
  'agent-profiles': 'agents',
  subscriptions: 'subscriptions',
  'member-plans': 'subscriptions',
  'agent-plans': 'subscriptions',
  'member-upgrades': 'payments',
  'member-payments': 'payments',
  'member-invoices': 'payments',
  'agent-upgrades': 'payments',
  'agent-payments': 'payments',
  'agent-invoices': 'payments',
  content: 'moderation',
  'photo-moderation': 'moderation',
  'chat-moderation': 'moderation',
  'flagged-content': 'moderation',
  'visitor-enquiry': 'moderation',
  'lead-management': 'moderation',
  notifications: 'system',
  'dynamic-config': 'cms',
  'cms-management': 'cms',
  analytics: 'analytics',
  'growth-reports': 'reports',
  system: 'system',
  'role-master': 'system',
  'user-master': 'system',
  'data-export': 'system',
  'audit-logs': 'system',
  'service-health': 'system',
  settings: 'system',
  'change-password': 'system',
  assist: 'assist'
};

const MEMBER_PLAN_FALLBACK = [
  { planId: 'bronze', id: 'bronze', name: 'Bronze', displayName: 'Bronze (Free)', price: 0, durationDays: 30, contactViews: 5, visibleMatches: 80, profileViews: 10, shortlistLimit: 5, interestLimit: 5, chat: false, engagePlus: false, matchAssistance: false, spotlightBoosts: 0, profileBoost: false },
  { planId: 'silver', id: 'silver', name: 'Silver', displayName: 'Silver', price: 299, durationDays: 30, contactViews: 15, visibleMatches: 80, profileViews: 30, shortlistLimit: 20, interestLimit: 20, chat: true, engagePlus: true, matchAssistance: false, spotlightBoosts: 0, profileBoost: true },
  { planId: 'gold', id: 'gold', name: 'Gold', displayName: 'Gold', price: 599, durationDays: 30, contactViews: 30, visibleMatches: 80, profileViews: 50, shortlistLimit: 40, interestLimit: 40, chat: true, engagePlus: true, matchAssistance: true, spotlightBoosts: 2, profileBoost: true },
  { planId: 'platinum', id: 'platinum', name: 'Platinum', displayName: 'Platinum', price: 999, durationDays: 30, contactViews: 80, visibleMatches: 80, profileViews: 80, shortlistLimit: 80, interestLimit: 80, chat: true, engagePlus: true, matchAssistance: true, spotlightBoosts: 4, profileBoost: true }
];

const AGENT_PLAN_FALLBACK = [
  { id: 'free', name: 'Free', price: 0, profilesAllowed: 5, visibleMatches: 10, contactViews: 0, analytics: false },
  { id: 'silver', name: 'Silver', price: 999, profilesAllowed: 25, visibleMatches: 50, contactViews: 20, analytics: false },
  { id: 'gold', name: 'Gold', price: 2499, profilesAllowed: 100, visibleMatches: -1, contactViews: 100, analytics: true },
  { id: 'platinum', name: 'Platinum', price: 4999, profilesAllowed: -1, visibleMatches: -1, contactViews: -1, analytics: true }
];

const RBAC_MODULES = [
  {
    key: 'profiles',
    label: 'Member Management',
    permissions: { view: 'profiles:read', add: 'profiles:add', edit: 'profiles:write', delete: 'profiles:delete', export: 'profiles:export' }
  },
  {
    key: 'advisors',
    label: 'Agent & Matchmakers',
    permissions: { view: 'advisors:read', add: 'advisors:add', edit: 'advisors:write', delete: 'advisors:delete', export: 'advisors:export' }
  },
  {
    key: 'payments',
    label: 'Revenue & Analytics',
    permissions: { view: 'analytics:read', add: 'payments:add', edit: 'payments:write', delete: 'payments:delete', export: 'analytics:export' }
  },
  {
    key: 'config',
    label: 'System Configuration',
    permissions: { view: 'config:read', add: 'config:add', edit: 'config:write', delete: 'config:delete', export: 'config:export' }
  },
  {
    key: 'moderation',
    label: 'Content & Moderation',
    permissions: { view: 'moderation:read', add: 'moderation:add', edit: 'moderation:write', delete: 'moderation:delete', export: 'moderation:export' }
  },
  {
    key: 'campaigns',
    label: 'Notifications & Campaigns',
    permissions: { view: 'campaigns:read', add: 'campaigns:write', edit: 'campaigns:write', delete: 'campaigns:delete', export: 'campaigns:export' }
  }
];

const RBAC_ACTIONS = ['view', 'add', 'edit', 'delete', 'export'];

function buildRbacModules() {
  const modules = [];
  MENU_GROUPS.forEach((group) => {
    group.items.forEach((item) => {
      if (item.id === 'logout') return;
      const key = item.id;
      modules.push({
        key,
        label: `${group.label}: ${item.label}`,
        section: group.label,
        permissions: RBAC_ACTIONS.reduce((acc, action) => {
          acc[action] = `${key}:${action}`;
          return acc;
        }, {})
      });
    });
  });
  return modules;
}

const MEMBER_TIER_COLUMNS = ['bronze', 'silver', 'gold', 'platinum'];

const ROLE_COPY = {
  super_admin: { description: 'Full access to every admin console module, finance control, configuration and audit data.', scope: 'unrestricted' },
  admin: { description: 'Operational management of members, agents, payments, moderation and configurations.', scope: 'partial' },
  moderator: { description: 'Content validation, verification review, profile approval and dispute handling.', scope: 'compliance' },
  support_agent: { description: 'Member support access with limited profile visibility and moderation read access.', scope: 'support' },
  marketing_manager: { description: 'CMS, campaigns, referrals, analytics and audience engagement control.', scope: 'marketing' }
};

function decodeSession() {
  try {
    const token = localStorage.getItem('adminToken');
    if (!token) return { role: 'guest', email: '' };
    const payload = JSON.parse(window.atob(token.split('.')[1]));
    return {
      role: payload.role || 'admin',
      email: payload.email || '',
      permissions: payload.permissions || []
    };
  } catch (_) {
    return { role: 'guest', email: '' };
  }
}

function normalizeConfig(config) {
  return {
    ...DEFAULT_CONFIG,
    ...(config || {}),
    monetization: { ...DEFAULT_CONFIG.monetization, ...(config?.monetization || {}) },
    assisted_matchmaking: { ...DEFAULT_CONFIG.assisted_matchmaking, ...(config?.assisted_matchmaking || {}) },
    admin_roles: { ...DEFAULT_CONFIG.admin_roles, ...(config?.admin_roles || {}) }
  };
}

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

function money(value) {
  return `₹${new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 }).format(numberValue(value))}`;
}

function dateOnly(value) {
  if (!value) return 'Not set';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function dateTime(value) {
  if (!value) return 'Waiting';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function todayLong() {
  return new Date().toLocaleDateString('en-IN', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  });
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

function titleFromKey(value) {
  return String(value || '')
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function getRoleLabel(role) {
  return role?.label || titleFromKey(role?.role || 'Role');
}

function allPermissionTokens() {
  return Array.from(new Set(buildRbacModules().flatMap((module) => Object.values(module.permissions))));
}

function normalizeRoleForUi(role) {
  const key = role?.role || String(role?.label || 'custom_role').toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
  const copy = ROLE_COPY[key] || {};
  return {
    role: key,
    label: getRoleLabel({ ...role, role: key }),
    description: role?.description || copy.description || 'Custom admin role configured for the SoulMatch console.',
    scope: role?.scope || copy.scope || 'custom',
    status: role?.status || 'active',
    permissions: Array.isArray(role?.permissions) ? role.permissions : []
  };
}

function roleHasPermission(role, token) {
  const permissions = Array.isArray(role?.permissions) ? role.permissions : [];
  if (permissions.includes('*')) return true;
  if (permissions.includes(token)) return true;
  if (token.endsWith(':read')) return permissions.some((permission) => permission === token.replace(':read', ':write'));
  return false;
}

function toggleRolePermission(role, token, enabled) {
  const expanded = role.permissions.includes('*') ? allPermissionTokens() : role.permissions;
  const next = new Set(expanded);
  if (enabled) next.add(token);
  else next.delete(token);
  return { ...role, permissions: Array.from(next).sort() };
}

function metricPercent(numerator, denominator) {
  const total = numberValue(denominator);
  if (!total) return '0%';
  return `${((numberValue(numerator) / total) * 100).toFixed(1)}%`;
}

function makeProfilePayload(form) {
  return {
    firstName: form.first_name,
    lastName: form.last_name,
    phone: form.phone,
    email: form.email,
    dob: form.dob,
    gender: form.gender,
    religion: form.religion,
    caste: form.caste,
    motherTongue: form.mother_tongue,
    maritalStatus: form.marital_status,
    isPublished: form.is_published,
    profileStatus: form.profile_status,
    profileCreatedBy: form.profile_created_by,
    verificationStatus: form.verification_status,
    adminStatus: form.admin_status,
    primaryPhotoUrl: form.primary_photo_url,
    photoPrivacy: form.photo_privacy,
    profileVisibility: form.profile_visibility,
    hideLastSeen: form.hide_last_seen,
    reviewStatus: form.review_status,
    reviewNotes: form.review_notes,
    rejectionReason: form.rejection_reason,
    heightCm: form.height_cm,
    weightKg: form.weight_kg,
    complexion: form.complexion,
    bodyType: form.body_type,
    bloodGroup: form.blood_group,
    educationLevel: form.education_level,
    isEmployed: form.is_employed,
    occupation: form.occupation,
    annualIncome: form.annual_income,
    workingCity: form.working_city,
    workingState: form.working_state,
    workingPincode: form.working_pincode,
    fatherOccupation: form.father_occupation,
    motherOccupation: form.mother_occupation,
    numBrothers: form.num_brothers,
    numSisters: form.num_sisters,
    familyType: form.family_type,
    familyCity: form.family_city,
    familyState: form.family_state,
    familyLocality: form.family_locality,
    familyPincode: form.family_pincode,
    rashi: form.rashi,
    nakshatra: form.nakshatra,
    isManglik: form.is_manglik,
    birthCity: form.birth_city,
    gotra: form.gotra,
    diet: form.diet,
    smoking: form.smoking,
    drinking: form.drinking,
    aboutMe: form.about_me,
    ageMin: form.age_min,
    ageMax: form.age_max,
    preferenceReligion: form.preference_religion,
    manglikPref: form.manglik_pref,
    educationLevels: form.education_levels,
    occupations: form.occupations,
    annualIncomeMin: form.annual_income_min,
    annualIncomeMax: form.annual_income_max,
    heightMinCm: form.height_min_cm,
    heightMaxCm: form.height_max_cm,
    locations: form.locations,
    timeline: form.timeline
  };
}

function makeAdvisorPayload(form) {
  return {
    fullName: form.full_name,
    phone: form.phone,
    email: form.email,
    serviceLabel: form.service_label,
    businessName: form.business_name,
    bio: form.bio,
    gender: form.gender,
    city: form.city,
    state: form.state,
    pincode: form.pincode,
    languages: form.languages,
    communities: form.communities,
    maxActiveAssignments: form.max_active_assignments,
    successRate: form.success_rate,
    complaintScore: form.complaint_score,
    averageRating: form.average_rating,
    kycStatus: form.kyc_status,
    status: form.status,
    membershipPlan: form.membership_plan,
    membershipExpiresAt: form.membership_expires_at,
    notes: form.notes,
    yearsExperience: form.years_experience
  };
}

function SimpleLineChart({ rows, ariaLabel = 'Revenue trend chart' }) {
  const values = (rows || []).map((row) => numberValue(row.revenue || row.total || row.value));
  const labels = (rows || []).map((row) => row.label || row.month || '');
  const max = Math.max(...values, 1);
  const points = values.map((value, index) => {
    const x = 20 + index * (260 / Math.max(values.length - 1, 1));
    const y = 150 - (value / max) * 120;
    return `${x},${y}`;
  }).join(' ');
  const area = points ? `20,160 ${points} 280,160` : '';
  return (
    <div className="chart-card">
      <svg viewBox="0 0 300 180" role="img" aria-label={ariaLabel}>
        {[40, 80, 120, 160].map((y) => <line key={y} x1="18" x2="282" y1={y} y2={y} className="chart-grid-line" />)}
        <polyline points={area} className="chart-area" />
        <polyline points={points} className="chart-line" />
        {values.map((value, index) => {
          const x = 20 + index * (260 / Math.max(values.length - 1, 1));
          const y = 150 - (value / max) * 120;
          return <circle key={labels[index] || index} cx={x} cy={y} r="4" className="chart-dot" />;
        })}
      </svg>
      <div className="chart-labels">
        {labels.map((label, index) => <span key={`${label}-${index}`}>{label}</span>)}
      </div>
    </div>
  );
}

function PieChart({ title, total, segments, centerLabel = 'Total' }) {
  const primary = numberValue(segments[0]?.value);
  const percent = numberValue(total) ? (primary / numberValue(total)) * 100 : 0;
  return (
    <div className="donut-wrap multi">
      <h4>{title}</h4>
      <div className="donut" style={{ '--paid': `${percent}%` }}>
        <strong>{compactNumber(total)}</strong>
        <span>{centerLabel}</span>
      </div>
      <div className="donut-legend">
        {segments.map((segment, index) => (
          <p key={segment.label}>
            <b className={`dot ${index === 0 ? 'paid' : 'free'}`} />
            {segment.label}
            <strong>{segment.value}</strong>
            <span>{metricPercent(segment.value, total)}</span>
          </p>
        ))}
      </div>
    </div>
  );
}

function createMonthlyTrend(rows, dateKey = 'created_at') {
  const buckets = new Map();
  const now = new Date();
  for (let index = 5; index >= 0; index -= 1) {
    const date = new Date(now.getFullYear(), now.getMonth() - index, 1);
    const key = `${date.getFullYear()}-${date.getMonth()}`;
    buckets.set(key, {
      label: date.toLocaleDateString('en-IN', { month: 'short' }),
      value: 0
    });
  }
  rows.forEach((row) => {
    const date = new Date(row[dateKey]);
    if (Number.isNaN(date.getTime())) return;
    const key = `${date.getFullYear()}-${date.getMonth()}`;
    if (buckets.has(key)) buckets.get(key).value += 1;
  });
  return Array.from(buckets.values());
}

function MembersAddedChart({ profiles }) {
  const [range, setRange] = useState('90');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const filtered = useMemo(() => {
    const now = new Date();
    const start = from
      ? new Date(from)
      : new Date(now.getTime() - Number(range) * 24 * 60 * 60 * 1000);
    const end = to ? new Date(to) : now;
    return profiles.filter((profile) => {
      const created = new Date(profile.created_at);
      return !Number.isNaN(created.getTime()) && created >= start && created <= end;
    });
  }, [from, profiles, range, to]);
  const trend = createMonthlyTrend(filtered).map((row) => ({ ...row, revenue: row.value }));
  return (
    <div className="admin-card">
      <div className="card-title-row chart-title-row">
        <h3>Members Added</h3>
        <div className="chart-controls">
          <select value={range} onChange={(event) => setRange(event.target.value)}>
            <option value="7">Last 7 days</option>
            <option value="30">Last 30 days</option>
            <option value="90">Last 90 days</option>
            <option value="180">Last 180 days</option>
          </select>
          <input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
          <input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
        </div>
      </div>
      <SimpleLineChart rows={trend} ariaLabel="Members added trend chart" />
      <div className="mini-stat-row as-tags">
        <span>Selected range <strong>{filtered.length}</strong></span>
        <span>Total profiles <strong>{profiles.length}</strong></span>
      </div>
    </div>
  );
}

function DashboardHome({ stats, profiles, advisors, payments, alerts, auditLogs, search, onTab, onMember, onAgent, onCreateMember }) {
  const admin = stats.adminConsole || EMPTY_STATS.adminConsole;
  const [memberFilter, setMemberFilter] = useState('all');
  const [memberSort, setMemberSort] = useState('newest');
  const members = admin.members || {};
  const agents = admin.agents || {};
  const queues = admin.queues || {};
  const totalMembers = numberValue(members.total || stats.totalProfiles || profiles.length);
  const paidMembers = numberValue(members.paid || stats.premiumUsers);
  const freeMembers = Math.max(numberValue(members.free || totalMembers - paidMembers), 0);
  const totalRevenue = numberValue(stats.totalRevenue);
  const monthlyRevenue = numberValue(stats.revenue30d);
  const pendingRevenue = payments.transactions
    .filter((tx) => ['pending', 'created', 'attempted'].includes(String(tx.status || '').toLowerCase()))
    .reduce((sum, tx) => sum + numberValue(tx.amount), 0);
  const baseRecentMembers = (admin.recentMembers?.length ? admin.recentMembers : profiles.slice(0, 12))
    .map((profile) => ({ ...(profiles.find((item) => item.profile_id === profile.profile_id) || {}), ...profile }));
  const memberSource = (profile) => {
    if (profile.created_by_advisor_id) return 'Agent';
    if (profile.profile_created_by === 'admin' || profile.acquisition_source === 'admin_manual') return 'Admin';
    return 'Self';
  };
  const recentMembers = baseRecentMembers
    .filter((profile) => {
      const source = memberSource(profile).toLowerCase();
      if (memberFilter !== 'all' && source !== memberFilter) return false;
      const q = search.trim().toLowerCase();
      if (!q) return true;
      return [
        profile.profile_id,
        profile.user_id,
        fullName(profile),
        profile.phone,
        profile.email,
        profile.gender,
        profile.plan_id,
        source
      ].filter(Boolean).join(' ').toLowerCase().includes(q);
    })
    .sort((a, b) => {
      if (memberSort === 'oldest') return new Date(a.created_at) - new Date(b.created_at);
      if (memberSort === 'name') return fullName(a).localeCompare(fullName(b));
      return new Date(b.created_at) - new Date(a.created_at);
    })
    .slice(0, 12);
  const leaderboard = admin.agentLeaderboard?.length
    ? admin.agentLeaderboard
    : advisors.slice(0, 5).map((agent) => ({ ...agent, members_added: agent.active_assignments || 0 }));
  const newlyRegisteredAgents = (admin.recentAgents?.length ? admin.recentAgents : advisors)
    .filter((agent) => {
      const q = search.trim().toLowerCase();
      if (!q) return true;
      return [agent.full_name, agent.agent_code, agent.phone, agent.email, agent.business_name, agent.city, agent.state]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(q);
    })
    .sort((a, b) => new Date(b.created_at || 0) - new Date(a.created_at || 0))
    .slice(0, 10);
  const quickPending = numberValue(queues.member_kyc || stats.pendingApprovals);
  const activeAgents = numberValue(agents.active || advisors.filter((agent) => agent.status === 'active').length);
  const paidAgentCount = advisors.filter((agent) => !['', 'free', 'starter'].includes(String(agent.membership_plan || '').toLowerCase())).length;
  const freeAgentCount = Math.max(numberValue(agents.total || advisors.length) - paidAgentCount, 0);
  const dashboardAudit = (admin.recentAudit?.length ? admin.recentAudit : auditLogs)
    .filter((log) => !/deploy|deployment|version|service health|release/i.test(`${log.action || ''} ${log.entity_type || ''}`))
    .slice(0, 8);
  const revenueTrend = admin.revenueTrend || [];

  return (
    <div className="admin-content dashboard-grid">
      <SectionHeader
        title="Good morning, Admin"
        description={`${todayLong()} | Here's your platform overview`}
        actions={(
          <>
            <AdminButton variant="secondary" onClick={onCreateMember}><Icon name="plus" /> Add Member</AdminButton>
            <AdminButton variant="primary" onClick={() => onTab('member-verify')}><Icon name="check" /> Verify Pending ({quickPending})</AdminButton>
            <AdminButton variant="secondary" onClick={() => onTab('data-export')}><Icon name="export" /> Export Report</AdminButton>
          </>
        )}
      />

      <div className="stat-grid five">
        <StatCard tone="terracotta" label="Total Members" value={compactNumber(totalMembers)} sub={`Paid: ${paidMembers} | Free: ${freeMembers} | New Today: ${numberValue(members.new_today || stats.newUsersToday)}`} onClick={() => onTab('members-all')} />
        <StatCard tone="peach" label="Grooms Registered" value={compactNumber(members.grooms)} sub={`Paid: ${Math.round(paidMembers * 0.62)} | Free: ${Math.max(numberValue(members.grooms) - Math.round(paidMembers * 0.62), 0)} | New Today: ${numberValue(members.grooms_today || 0)}`} onClick={() => onTab('members-all')} />
        <StatCard tone="mauve" label="Brides Registered" value={compactNumber(members.brides)} sub={`Paid: ${Math.max(paidMembers - Math.round(paidMembers * 0.62), 0)} | Free: ${Math.max(numberValue(members.brides) - Math.max(paidMembers - Math.round(paidMembers * 0.62), 0), 0)} | New Today: ${numberValue(members.brides_today || 0)}`} onClick={() => onTab('members-all')} />
        <StatCard tone="sage" label="Total Revenue" value={money(totalRevenue)} sub={`This Month: ${money(monthlyRevenue)} | Pending: ${money(pendingRevenue)} | New Today: ${money(stats.revenueToday || 0)}`} onClick={() => onTab('member-payments')} />
        <StatCard tone="steel" label="Active Agents" value={compactNumber(activeAgents)} sub={`Verified: ${numberValue(agents.verified)} | Pending: ${numberValue(agents.pending)} | Suspended: ${numberValue(agents.suspended)}`} onClick={() => onTab('agents-all')} />
      </div>

      <div className="workspace-columns members-first">
        <section className="workspace-left wide">
          <div className="admin-card">
            <div className="card-title-row">
              <h3>New Registered Members</h3>
              <div className="table-controls">
                <select value={memberFilter} onChange={(event) => setMemberFilter(event.target.value)}>
                  <option value="all">All sources</option>
                  <option value="self">Created by Self</option>
                  <option value="agent">Created by Agent</option>
                  <option value="admin">Created by Admin</option>
                </select>
                <select value={memberSort} onChange={(event) => setMemberSort(event.target.value)}>
                  <option value="newest">Newest first</option>
                  <option value="oldest">Oldest first</option>
                  <option value="name">Name A-Z</option>
                </select>
              </div>
            </div>
            <div className="data-table">
              <table>
                <thead>
                  <tr>
                    <th>Profile ID</th>
                    <th>Photo</th>
                    <th>Name / ID</th>
                    <th>Gender</th>
                    <th>Joining Date</th>
                    <th>Type</th>
                    <th>Created By</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {recentMembers.map((profile) => {
                    const source = memberSource(profile);
                    return (
                      <tr key={profile.profile_id || profile.user_id}>
                        <td><code>{memberDisplayId(profile)}</code></td>
                        <td><ProfileAvatar profile={profile} /></td>
                        <td><strong>{fullName(profile)}</strong><small>{profile.phone || profile.email || profile.user_id?.slice(0, 8)}</small></td>
                        <td><StatusPill status="neutral">{profile.gender || '-'}</StatusPill></td>
                        <td>{dateOnly(profile.created_at)}</td>
                        <td><StatusPill status={profile.plan_id === 'free' ? 'neutral' : 'approved'}>{profile.plan_id || 'free'}</StatusPill></td>
                        <td>{source}</td>
                        <td>
                          <div className="row-actions">
                            <button title="View" onClick={() => onMember(profile)}><Icon name="eye" /></button>
                            <button title="Edit" onClick={() => onMember(profile)}><Icon name="edit" /></button>
                            <button title="Verify" onClick={() => onTab('member-verify')}><Icon name="check" /></button>
                            <button title="Block" onClick={() => onTab('member-block')}><Icon name="ban" /></button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <div className="table-footer">Showing {recentMembers.length} of {totalMembers} members <button type="button" onClick={() => onTab('members-all')}>View all members</button></div>
          </div>

          <div className="admin-card">
            <div className="card-title-row">
              <h3>Newly Registered Agents</h3>
              <button type="button" onClick={() => onTab('agents-all')}>View all agents</button>
            </div>
            <div className="data-table">
              <table>
                <thead>
                  <tr>
                    <th>Agent</th>
                    <th>Business</th>
                    <th>City</th>
                    <th>KYC</th>
                    <th>Plan</th>
                    <th>Joined</th>
                    <th>Profiles</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {newlyRegisteredAgents.map((agent) => (
                    <tr key={agent.advisor_id || agent.agent_code}>
                      <td><strong>{agent.full_name}</strong><small>{agent.agent_code || agent.phone || agent.email}</small></td>
                      <td>{agent.business_name || '-'}</td>
                      <td>{[agent.city, agent.state].filter(Boolean).join(', ') || '-'}</td>
                      <td><StatusPill status={agent.kyc_status}>{agent.kyc_status || 'pending'}</StatusPill></td>
                      <td>{agent.membership_plan || 'free'}</td>
                      <td>{dateOnly(agent.created_at)}</td>
                      <td>{agent.profiles_added || agent.active_assignments || 0}</td>
                      <td><button type="button" onClick={() => onAgent(agent)}>View</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!newlyRegisteredAgents.length ? <EmptyState title="No agents yet" body="New agent registrations will appear here." /> : null}
            </div>
          </div>
        </section>
      </div>

      <div className="admin-card full">
        <div className="card-title-row">
          <h3>Dashboard Activity Audit</h3>
          <button type="button" onClick={() => onTab('audit-logs')}>View full audit log</button>
        </div>
        <AuditLogTable logs={dashboardAudit} />
      </div>
    </div>
  );
}

const REVENUE_TABS = [
  { id: 'subscriptions', label: 'Member Plans' },
  { id: 'member-upgrades', label: 'Member Upgrades' },
  { id: 'member-payments', label: 'Member Payments' },
  { id: 'member-invoices', label: 'Member Invoices' },
  { id: 'agent-plans', label: 'Agent Plans' },
  { id: 'agent-upgrades', label: 'Agent Upgrades' },
  { id: 'agent-payments', label: 'Agent Payments' },
  { id: 'agent-invoices', label: 'Agent Invoices' }
];

function RevenueTabs({ active, onTab }) {
  if (!onTab) return null;
  return (
    <div className="admin-card revenue-tabs" role="tablist" aria-label="Revenue sections">
      {REVENUE_TABS.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          className={active === tab.id || (active === 'member-plans' && tab.id === 'subscriptions') ? 'active' : ''}
          onClick={() => onTab(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}

function SubscriptionPanel({ config, payments, type, onSave, activeTab, onTab }) {
  const isAgent = type === 'agent';
  const sectionKey = isAgent ? 'assisted_matchmaking' : 'monetization';
  const plansKey = isAgent ? 'advisorPlans' : 'plans';
  const fallback = isAgent ? AGENT_PLAN_FALLBACK : MEMBER_PLAN_FALLBACK;
  const section = config[sectionKey] || {};
  const initialPlans = Array.isArray(section[plansKey]) && section[plansKey].length ? section[plansKey] : fallback;
  const [plansText, setPlansText] = useState(JSON.stringify(initialPlans, null, 2));
  const [error, setError] = useState('');

  useEffect(() => {
    const plans = Array.isArray(section[plansKey]) && section[plansKey].length ? section[plansKey] : fallback;
    setPlansText(JSON.stringify(plans, null, 2));
  }, [fallback, plansKey, section]);

  const save = () => {
    try {
      const parsed = JSON.parse(plansText);
      if (!Array.isArray(parsed)) throw new Error('Plan config must be an array.');
      setError('');
      onSave(sectionKey, { ...section, [plansKey]: parsed });
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="admin-content">
      <SectionHeader
        title={`${isAgent ? 'Agent' : 'Member'} Subscription Management`}
        description="Plans, limits, pricing, payment history and upgrade rules are configurable from here."
        actions={<AdminButton variant="primary" onClick={save}>Save Plan Configuration</AdminButton>}
      />
      <RevenueTabs active={activeTab} onTab={onTab} />
      <div className="subscription-grid">
        <div className="admin-card">
          <h3>{isAgent ? 'Agent Plans JSON' : 'Member Plans JSON'}</h3>
          <p className="muted">Edit every plan field without a code change. Keep valid JSON array format.</p>
          <textarea className="json-editor" value={plansText} onChange={(event) => setPlansText(event.target.value)} />
          {error ? <p className="form-error">{error}</p> : null}
        </div>
        <div className="admin-card">
          <h3>Live Plan Preview</h3>
          <div className="plan-preview">
            {(JSON.parse(JSON.stringify(initialPlans)) || []).map((plan) => (
              <article key={plan.id || plan.planId || plan.name}>
                <span>{plan.name || plan.planId || plan.id}</span>
                <strong>{money(plan.price || plan.monthlyPrice || 0)}</strong>
                <small>{isAgent ? `${plan.profilesAllowed ?? '-'} profiles · ${plan.contactViews ?? '-'} contacts` : `${plan.contactViews ?? '-'} contacts · ${plan.visibleMatches ?? '-'} matches`}</small>
              </article>
            ))}
          </div>
        </div>
        <div className="admin-card full">
          <h3>Payments & Invoices</h3>
          <PaymentsTable payments={payments} />
        </div>
      </div>
    </div>
  );
}

function PendingUpgradesPanel({ payments, type, activeTab, onTab }) {
  const rows = (payments.pendingOrders || []).filter((order) => (type === 'agent') === (order.owner_type === 'agent'));
  return (
    <div className="admin-content">
      <SectionHeader title={`${titleFromKey(type)} Pending Upgrades`} description="Members or agents who selected a plan but did not complete payment." />
      <RevenueTabs active={activeTab} onTab={onTab} />
      <div className="admin-card data-table tall">
        <table>
          <thead><tr><th>Profile ID</th><th>Name</th><th>Email</th><th>Phone Number</th><th>Issue Details</th><th>Issue Timestamp</th><th>Support Contacted</th><th>Support Comments</th></tr></thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.payment_order_id}>
                <td><code>{row.profile_display_id || row.agent_code || row.user_id?.slice(0, 8)}</code></td>
                <td>{row.display_name || '-'}</td>
                <td>{row.email || '-'}</td>
                <td>{row.phone || '-'}</td>
                <td>{row.status} | {row.plan_id} | {money(row.amount)}</td>
                <td>{dateTime(row.updated_at || row.created_at)}</td>
                <td><StatusPill status={row.support_contacted ? 'active' : 'pending'}>{row.support_contacted ? 'True' : 'False'}</StatusPill></td>
                <td>{row.support_comments || 'No comments added'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!rows.length ? <EmptyState title="No pending upgrades" body="Failed or abandoned payment attempts will appear here." /> : null}
      </div>
    </div>
  );
}

function RevenuePaymentsPanel({ payments, type, activeTab, onTab }) {
  const [planFilter, setPlanFilter] = useState('all');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const rows = (payments.transactions || []).filter((tx) => {
    if ((type === 'agent') !== (tx.owner_type === 'agent')) return false;
    if (planFilter !== 'all' && tx.plan_id !== planFilter) return false;
    const created = new Date(tx.created_at);
    if (from && created < new Date(from)) return false;
    if (to && created > new Date(to)) return false;
    return ['paid', 'success', 'captured'].includes(String(tx.status || '').toLowerCase());
  });
  const planOptions = Array.from(new Set((payments.transactions || []).map((tx) => tx.plan_id).filter(Boolean)));
  const total = rows.reduce((sum, tx) => sum + numberValue(tx.amount), 0);
  return (
    <div className="admin-content">
      <SectionHeader title={`${titleFromKey(type)} Revenue & Payments`} description="Subscription payments with sales team follow-up fields." />
      <RevenueTabs active={activeTab} onTab={onTab} />
      <div className="admin-card revenue-filter-card">
        <div className="mini-stat-row as-tags">
          <span>Revenue <strong>{money(total)}</strong></span>
          <span>Transactions <strong>{rows.length}</strong></span>
          <span>Average <strong>{money(total / Math.max(rows.length, 1))}</strong></span>
        </div>
        <div className="table-controls">
          <select value={planFilter} onChange={(event) => setPlanFilter(event.target.value)}><option value="all">All plans</option>{planOptions.map((plan) => <option key={plan} value={plan}>{plan}</option>)}</select>
          <input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
          <input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
        </div>
      </div>
      <div className="admin-card data-table tall">
        <table>
          <thead><tr><th>Profile ID</th><th>Name</th><th>Email</th><th>Phone Number</th><th>Subscription</th><th>Paid Amount</th><th>Status</th><th>Transaction Number</th><th>Timestamp</th><th>Sales Connected</th><th>Sales Comments</th><th>Contacted Date</th></tr></thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.transaction_id}>
                <td><code>{row.profile_display_id || row.agent_code || row.user_id?.slice(0, 8)}</code></td>
                <td>{row.display_name || fullName(row)}</td>
                <td>{row.email || '-'}</td>
                <td>{row.phone || '-'}</td>
                <td>{row.plan_id || '-'}</td>
                <td>{money(row.amount)}</td>
                <td><StatusPill status={row.status}>{row.status}</StatusPill></td>
                <td><code>{row.razorpay_payment_id || row.razorpay_order_id || row.transaction_id?.slice(0, 10)}</code></td>
                <td>{dateTime(row.created_at)}</td>
                <td>False</td>
                <td>Not updated</td>
                <td>-</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!rows.length ? <EmptyState title="No paid subscriptions" body="Successful subscription payments will appear here." /> : null}
      </div>
    </div>
  );
}

function InvoicesPanel({ payments, type, activeTab, onTab }) {
  const rows = (payments.invoices || []).filter((invoice) => (type === 'agent') === (invoice.owner_type === 'agent'));
  return (
    <div className="admin-content">
      <SectionHeader title={`${titleFromKey(type)} Invoices`} description="Invoices are attached to paid member and agent profiles for each subscription cycle." />
      <RevenueTabs active={activeTab} onTab={onTab} />
      <div className="invoice-grid">
        {rows.map((invoice) => (
          <article className="invoice-card" key={invoice.invoice_id}>
            <div><strong>{invoice.invoice_number}</strong><StatusPill status={invoice.status}>{invoice.status}</StatusPill></div>
            <h3>{invoice.display_name || '-'}</h3>
            <p>{invoice.plan_id || 'Subscription'} | {money(invoice.amount)} | {invoice.payment_method || 'payment gateway'}</p>
            <dl>
              <dt>Transaction</dt><dd>{invoice.transaction_ref || '-'}</dd>
              <dt>Paid on</dt><dd>{dateTime(invoice.issued_at)}</dd>
              <dt>Validity</dt><dd>{dateOnly(invoice.valid_from)} to {dateOnly(invoice.valid_to)}</dd>
            </dl>
          </article>
        ))}
        {!rows.length ? <EmptyState title="No invoices" body="Invoices will be generated from successful subscription transactions." /> : null}
      </div>
    </div>
  );
}

function tryParseJson(value, fallback) {
  try {
    return JSON.parse(value);
  } catch (_) {
    return fallback;
  }
}

function CmsManagementPanel({ config, onSave }) {
  const content = config.content || {};
  const home = content.home || {};
  const monetization = config.monetization || {};
  const [cardsText, setCardsText] = useState(JSON.stringify(home.bestMatchAdCards || [], null, 2));
  const [scamText, setScamText] = useState(JSON.stringify(home.scamAwarenessCards || [], null, 2));
  const [promptText, setPromptText] = useState(JSON.stringify(content.notificationPrompt || {}, null, 2));
  const [safetyText, setSafetyText] = useState(JSON.stringify(content.safetyCenter || {}, null, 2));
  const [matrixText, setMatrixText] = useState(JSON.stringify(monetization.membershipFeatureMatrix || [], null, 2));
  const [packageGroupsText, setPackageGroupsText] = useState(JSON.stringify(monetization.upgradePackageGroups || [], null, 2));
  const [accessMode, setAccessMode] = useState(monetization.accessMode || 'subscription');
  const [fixedPriceAmount, setFixedPriceAmount] = useState(String(monetization.fixedPriceAmount ?? 200));
  const [fixedPricePlanId, setFixedPricePlanId] = useState(monetization.fixedPricePlanId || 'fixed_access');
  const [error, setError] = useState('');

  useEffect(() => {
    setCardsText(JSON.stringify(home.bestMatchAdCards || [], null, 2));
    setScamText(JSON.stringify(home.scamAwarenessCards || [], null, 2));
    setPromptText(JSON.stringify(content.notificationPrompt || {}, null, 2));
    setSafetyText(JSON.stringify(content.safetyCenter || {}, null, 2));
    setMatrixText(JSON.stringify(monetization.membershipFeatureMatrix || [], null, 2));
    setPackageGroupsText(JSON.stringify(monetization.upgradePackageGroups || [], null, 2));
    setAccessMode(monetization.accessMode || 'subscription');
    setFixedPriceAmount(String(monetization.fixedPriceAmount ?? 200));
    setFixedPricePlanId(monetization.fixedPricePlanId || 'fixed_access');
  }, [home.bestMatchAdCards, home.scamAwarenessCards, content.notificationPrompt, content.safetyCenter, monetization.membershipFeatureMatrix, monetization.upgradePackageGroups, monetization.accessMode, monetization.fixedPriceAmount, monetization.fixedPricePlanId]);

  const adCards = tryParseJson(cardsText, []);
  const scamCards = tryParseJson(scamText, []);
  const notificationPrompt = tryParseJson(promptText, {});
  const safetyCenter = tryParseJson(safetyText, {});
  const featureMatrix = tryParseJson(matrixText, []);
  const packageGroups = tryParseJson(packageGroupsText, []);

  const saveHomeCms = () => {
    try {
      const parsedCards = JSON.parse(cardsText);
      const parsedScam = JSON.parse(scamText);
      const parsedPrompt = JSON.parse(promptText);
      const parsedSafety = JSON.parse(safetyText);
      if (!Array.isArray(parsedCards) || !Array.isArray(parsedScam)) {
        throw new Error('Best match cards and scam awareness cards must be JSON arrays.');
      }
      if (!parsedPrompt || Array.isArray(parsedPrompt) || typeof parsedPrompt !== 'object') {
        throw new Error('Notification prompt must be a JSON object.');
      }
      if (!parsedSafety || Array.isArray(parsedSafety) || typeof parsedSafety !== 'object') {
        throw new Error('Safety Center must be a JSON object.');
      }
      setError('');
      onSave('content', {
        ...content,
        home: {
          ...home,
          bestMatchAdCards: parsedCards,
          scamAwarenessCards: parsedScam,
          showBestMatchInsertCards: true,
          showBestMatchAdCards: true
        },
        notificationPrompt: parsedPrompt,
        safetyCenter: parsedSafety
      });
    } catch (err) {
      setError(err.message);
    }
  };

  const saveFeatureMatrix = () => {
    try {
      const parsed = JSON.parse(matrixText);
      if (!Array.isArray(parsed)) throw new Error('Membership feature matrix must be a JSON array.');
      setError('');
      onSave('monetization', { ...monetization, membershipFeatureMatrix: parsed });
    } catch (err) {
      setError(err.message);
    }
  };

  const saveAccessMode = () => {
    const normalizedMode = accessMode || 'subscription';
    const amount = Number(fixedPriceAmount || 0);
    if (normalizedMode === 'fixed_price' && (!Number.isFinite(amount) || amount <= 0)) {
      setError('Fixed price amount must be greater than zero.');
      return;
    }
    setError('');
    onSave('monetization', {
      ...monetization,
      accessMode: normalizedMode,
      subscriptionModelEnabled: normalizedMode === 'subscription',
      fixedPriceAmount: amount || 200,
      fixedPricePlanId: fixedPricePlanId || 'fixed_access',
      fixedPriceLabel: `₹${amount || 200}`,
      freeAccessLabel: 'Account'
    });
  };

  const savePackageGroups = () => {
    try {
      const parsed = JSON.parse(packageGroupsText);
      if (!Array.isArray(parsed)) throw new Error('Upgrade package groups must be a JSON array.');
      setError('');
      onSave('monetization', { ...monetization, upgradePackageGroups: parsed });
    } catch (err) {
      setError(err.message);
    }
  };

  const updateFeatureMatrixCell = (rowIndex, key, value) => {
    const source = Array.isArray(featureMatrix) ? featureMatrix : [];
    const next = source.map((feature, index) => (
      index === rowIndex ? { ...feature, [key]: MEMBER_TIER_COLUMNS.includes(key) ? parseFeatureMatrixValue(value) : value } : feature
    ));
    setMatrixText(JSON.stringify(next, null, 2));
  };

  const addFeatureMatrixRow = () => {
    const source = Array.isArray(featureMatrix) ? featureMatrix : [];
    const next = [
      ...source,
      {
        featureKey: `new_feature_${Date.now()}`,
        label: 'New feature',
        description: 'Describe this membership capability.',
        bronze: false,
        silver: false,
        gold: true,
        platinum: true
      }
    ];
    setMatrixText(JSON.stringify(next, null, 2));
  };

  const removeFeatureMatrixRow = (rowIndex) => {
    const source = Array.isArray(featureMatrix) ? featureMatrix : [];
    setMatrixText(JSON.stringify(source.filter((_, index) => index !== rowIndex), null, 2));
  };

  return (
    <div className="admin-content cms-management-page">
      <SectionHeader
        eyebrow="CMS Management"
        title="Best Matches Merchandising"
        description="Configure upgrade cards, service cards, scam-awareness carousel content, and tier-based feature gates without an app release."
        actions={<AdminButton variant="primary" onClick={saveHomeCms}>Save CMS Cards</AdminButton>}
      />
      {error ? <p className="form-error cms-error">{error}</p> : null}
      <div className="cms-grid">
        <section className="admin-card cms-editor-card">
          <div className="card-title-row">
            <h3>Access & Pricing Mode</h3>
            <StatusPill status={accessMode === 'subscription' ? 'active' : 'warning'}>{accessMode}</StatusPill>
          </div>
          <p className="muted">Controls whether members see subscription plans, all-free access, or a single fixed access price.</p>
          <div className="config-form compact">
            <label>Access mode
              <select value={accessMode} onChange={(event) => setAccessMode(event.target.value)}>
                <option value="subscription">Subscription model</option>
                <option value="free">Allow all users for free</option>
                <option value="fixed_price">Fixed price access</option>
              </select>
            </label>
            <label>Fixed price amount
              <input type="number" min="1" value={fixedPriceAmount} onChange={(event) => setFixedPriceAmount(event.target.value)} />
            </label>
            <label>Fixed price plan ID
              <input value={fixedPricePlanId} onChange={(event) => setFixedPricePlanId(event.target.value)} />
            </label>
          </div>
          <AdminButton variant="primary" onClick={saveAccessMode}>Save Access Mode</AdminButton>
        </section>
        <section className="admin-card cms-preview-card">
          <div className="card-title-row">
            <h3>Runtime Behavior</h3>
            <StatusPill status="neutral">Mobile menu</StatusPill>
          </div>
          <div className="cms-card-list">
            <article className="cms-preview-item">
              <div>
                <strong>{accessMode === 'free' ? '4th tab: Account' : accessMode === 'fixed_price' ? `4th tab: ₹${fixedPriceAmount || 200}` : '4th tab: Upgrade'}</strong>
                <span>{accessMode === 'subscription' ? 'Plans and recurring checkout enabled' : accessMode === 'fixed_price' ? 'Single payment checkout enabled' : 'Paid upgrade hidden'}</span>
                <small>These labels are read from public runtime configuration by the Android app.</small>
              </div>
              <StatusPill status={accessMode === 'free' ? 'active' : 'pending'}>{accessMode === 'free' ? 'Free' : 'Paid'}</StatusPill>
            </article>
          </div>
        </section>
        <section className="admin-card cms-editor-card">
          <div className="card-title-row">
            <h3>Best Matches Insert Cards</h3>
            <StatusPill status="neutral">{Array.isArray(adCards) ? adCards.length : 0} cards</StatusPill>
          </div>
          <p className="muted">Use <code>enabled</code>, <code>targetPlans</code>, <code>minPlan</code>, and <code>maxPlan</code> to control which membership tier sees each card.</p>
          <textarea className="json-editor large cms-json" value={cardsText} onChange={(event) => setCardsText(event.target.value)} />
        </section>
        <section className="admin-card cms-preview-card">
          <div className="card-title-row">
            <h3>Card Preview</h3>
            <StatusPill status="active">Runtime</StatusPill>
          </div>
          <div className="cms-card-list">
            {(Array.isArray(adCards) ? adCards : []).slice(0, 12).map((card) => (
              <article key={card.id || card.title} className={`cms-preview-item ${card.enabled === false ? 'disabled' : ''}`}>
                <div>
                  <strong>{card.title || card.id}</strong>
                  <span>{card.badge || card.type || 'card'} | {(card.targetPlans || []).join(', ') || 'all tiers'}</span>
                  <small>{card.body || 'No body configured'}</small>
                </div>
                <StatusPill status={card.enabled === false ? 'inactive' : 'active'}>{card.enabled === false ? 'Hidden' : 'Live'}</StatusPill>
              </article>
            ))}
          </div>
        </section>
        <section className="admin-card cms-editor-card">
          <div className="card-title-row">
            <h3>Scam Awareness Carousel</h3>
            <StatusPill status="warning">{Array.isArray(scamCards) ? scamCards.filter((item) => item.enabled !== false).length : 0} live</StatusPill>
          </div>
          <p className="muted">These safety cards are inserted between best matches and can be updated as fraud patterns change.</p>
          <textarea className="json-editor large cms-json" value={scamText} onChange={(event) => setScamText(event.target.value)} />
        </section>
        <section className="admin-card cms-preview-card">
          <div className="card-title-row">
            <h3>Safety Card Preview</h3>
            <StatusPill status="neutral">Carousel</StatusPill>
          </div>
          <div className="cms-safety-strip">
            {(Array.isArray(scamCards) ? scamCards : []).map((card) => (
              <article key={card.id || card.title}>
                <span>{card.enabled === false ? 'Hidden' : 'Live'}</span>
                <strong>{card.title || card.id}</strong>
                <small>{card.body || 'No body configured'}</small>
              </article>
            ))}
          </div>
        </section>
        <section className="admin-card cms-editor-card">
          <div className="card-title-row">
            <h3>Notification Opt-In Prompt</h3>
            <StatusPill status={notificationPrompt.enabled === false ? 'inactive' : 'active'}>
              {notificationPrompt.enabled === false ? 'Disabled' : 'Enabled'}
            </StatusPill>
          </div>
          <p className="muted">Shown once after login/app launch when a member has push notifications turned off.</p>
          <textarea className="json-editor large cms-json" value={promptText} onChange={(event) => setPromptText(event.target.value)} />
        </section>
        <section className="admin-card cms-preview-card">
          <div className="card-title-row">
            <h3>Prompt Preview</h3>
            <StatusPill status="neutral">{Array.isArray(notificationPrompt.bullets) ? notificationPrompt.bullets.length : 0} bullets</StatusPill>
          </div>
          <div className="cms-prompt-preview">
            <span>{notificationPrompt.enabled === false ? 'Hidden' : 'Live prompt'}</span>
            <strong>{notificationPrompt.title || 'Notification prompt title'}</strong>
            <small>{notificationPrompt.subtitle || 'Prompt subtitle'}</small>
            <ul>
              {(Array.isArray(notificationPrompt.bullets) ? notificationPrompt.bullets : []).slice(0, 4).map((item) => <li key={item}>{item}</li>)}
            </ul>
            <button type="button" disabled>{notificationPrompt.allowCta || 'Allow notifications'}</button>
          </div>
        </section>
        <section className="admin-card cms-editor-card">
          <div className="card-title-row">
            <h3>Safety Center Content</h3>
            <StatusPill status="active">{Array.isArray(safetyCenter.tiles) ? safetyCenter.tiles.length : 0} tiles</StatusPill>
          </div>
          <p className="muted">Controls Safety Center title, four topic cards, verification card, resource rows, and article pages.</p>
          <textarea className="json-editor large cms-json" value={safetyText} onChange={(event) => setSafetyText(event.target.value)} />
        </section>
        <section className="admin-card cms-preview-card">
          <div className="card-title-row">
            <h3>Safety Center Preview</h3>
            <StatusPill status="neutral">{Array.isArray(safetyCenter.articles) ? safetyCenter.articles.length : 0} articles</StatusPill>
          </div>
          <div className="cms-card-list">
            <article className="cms-preview-item">
              <div>
                <strong>{safetyCenter.title || 'Safety Center'}</strong>
                <span>{safetyCenter.resourcesTitle || 'Resources'}</span>
                <small>{safetyCenter.subtitle || 'No subtitle configured'}</small>
              </div>
              <StatusPill status="active">Mobile</StatusPill>
            </article>
            {(Array.isArray(safetyCenter.tiles) ? safetyCenter.tiles : []).slice(0, 4).map((tile) => (
              <article key={tile.id || tile.title} className="cms-preview-item">
                <div>
                  <strong>{tile.title || tile.id}</strong>
                  <span>{tile.icon || 'icon'} | {tile.destination || 'no destination'}</span>
                  <small>{tile.subtitle || 'No subtitle configured'}</small>
                </div>
                <StatusPill status="active">{tile.tone || 'tone'}</StatusPill>
              </article>
            ))}
          </div>
        </section>
      </div>
      <SectionHeader
        eyebrow="Membership Control"
        title="Feature Access Matrix"
        description="Every current or future paid capability should be registered here so Bronze, Silver, Gold and Platinum access stays auditable."
        actions={<AdminButton variant="primary" onClick={saveFeatureMatrix}>Save Feature Matrix</AdminButton>}
      />
      <div className="cms-grid matrix-grid">
        <section className="admin-card cms-editor-card">
          <div className="card-title-row">
            <h3>Upgrade Packages JSON</h3>
            <StatusPill status="neutral">{Array.isArray(packageGroups) ? packageGroups.length : 0} groups</StatusPill>
          </div>
          <p className="muted">Controls membership packages, prices, durations, plan IDs, benefits, badges and checkout targets.</p>
          <textarea className="json-editor large cms-json" value={packageGroupsText} onChange={(event) => setPackageGroupsText(event.target.value)} />
          <AdminButton variant="primary" onClick={savePackageGroups}>Save Upgrade Packages</AdminButton>
        </section>
        <section className="admin-card cms-editor-card">
          <h3>Feature Matrix JSON</h3>
          <textarea className="json-editor large cms-json" value={matrixText} onChange={(event) => setMatrixText(event.target.value)} />
        </section>
        <section className="admin-card feature-matrix-card">
          <div className="card-title-row">
            <h3>Tier Controls</h3>
            <AdminButton variant="secondary" onClick={addFeatureMatrixRow}>Add Feature</AdminButton>
          </div>
          <div className="feature-matrix-table">
            <table>
              <thead>
                <tr>
                  <th>Key</th>
                  <th>Feature</th>
                  {MEMBER_TIER_COLUMNS.map((tier) => <th key={tier}>{tier}</th>)}
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {(Array.isArray(featureMatrix) ? featureMatrix : []).map((feature, index) => (
                  <tr key={`${feature.featureKey || feature.label}-${index}`}>
                    <td>
                      <input
                        className="matrix-key-input"
                        value={feature.featureKey || ''}
                        onChange={(event) => updateFeatureMatrixCell(index, 'featureKey', event.target.value)}
                      />
                    </td>
                    <td>
                      <input
                        className="matrix-label-input"
                        value={feature.label || ''}
                        onChange={(event) => updateFeatureMatrixCell(index, 'label', event.target.value)}
                      />
                      <input
                        className="matrix-description-input"
                        value={feature.description || ''}
                        onChange={(event) => updateFeatureMatrixCell(index, 'description', event.target.value)}
                      />
                    </td>
                    {MEMBER_TIER_COLUMNS.map((tier) => (
                      <td key={tier}>
                        <input
                          className="matrix-value-input"
                          value={formatFeatureValue(feature[tier])}
                          onChange={(event) => updateFeatureMatrixCell(index, tier, event.target.value)}
                        />
                      </td>
                    ))}
                    <td>
                      <button className="icon-action danger" type="button" onClick={() => removeFeatureMatrixRow(index)}>Remove</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}

function parseFeatureMatrixValue(value) {
  const trimmed = String(value ?? '').trim();
  if (/^(yes|true|enabled)$/i.test(trimmed)) return true;
  if (/^(no|false|disabled)$/i.test(trimmed)) return false;
  return trimmed;
}

function formatFeatureValue(value) {
  if (value === true) return 'Yes';
  if (value === false) return 'No';
  if (value === null || value === undefined || value === '') return '-';
  return String(value);
}

function PaymentsTable({ payments }) {
  const rows = payments.transactions || [];
  return (
    <div className="data-table compact-table">
      <table>
        <thead><tr><th>Member</th><th>Plan</th><th>Amount</th><th>Status</th><th>Gateway</th><th>Date</th></tr></thead>
        <tbody>
          {rows.slice(0, 20).map((tx) => (
            <tr key={tx.transaction_id}>
              <td>{fullName(tx)}<small>{tx.phone}</small></td>
              <td>{tx.plan_id || '-'}</td>
              <td>{money(tx.amount)}</td>
              <td><StatusPill status={tx.status}>{tx.status}</StatusPill></td>
              <td>{tx.gateway || 'razorpay'}</td>
              <td>{dateOnly(tx.created_at)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!rows.length ? <EmptyState title="No payment records" body="Payment records will appear here after transactions are captured." /> : null}
    </div>
  );
}

function AnalyticsPanel({ stats, funnel, events, payments }) {
  const paidTransactions = payments.transactions || [];
  return (
    <div className="admin-content analytics-page">
      <SectionHeader title="Analytics" description="Real funnel, payment and product event telemetry from production tables." />
      <div className="metric-grid">
        <StatCard tone="terracotta" label="Signups" value={compactNumber(stats.analytics?.signups || stats.newUsersToday || 0)} sub="Auth milestone events" />
        <StatCard tone="gold" label="Published Profiles" value={compactNumber(stats.totalProfiles || 0)} sub="Profiles eligible for discovery" />
        <StatCard tone="sage" label="Payments" value={compactNumber(paidTransactions.length)} sub={`Revenue ${money(stats.revenue30d || stats.totalRevenue || 0)}`} />
        <StatCard tone="mauve" label="Conversion" value={`${numberValue(stats.conversionRate || 0).toFixed(1)}%`} sub="Signup to paid journey" />
      </div>
      <div className="workspace-columns even">
        <section className="admin-card">
          <h3>Signup to Payment Funnel</h3>
          <div className="review-list">
            {(funnel || []).map((step, index) => (
              <article key={step.event_type || step.label || index}>
                <span><strong>{titleFromKey(step.event_type || step.label || `Step ${index + 1}`)}</strong><small>{compactNumber(step.count || step.total)} users</small></span>
                <StatusPill status="neutral">{index + 1}</StatusPill>
              </article>
            ))}
            {!funnel?.length ? <EmptyState title="No funnel events" body="Server-signed milestone events will populate the funnel here." /> : null}
          </div>
        </section>
        <section className="admin-card">
          <h3>Latest Product Events</h3>
          <div className="review-list">
            {(events || []).slice(0, 15).map((event, index) => (
              <article key={event.event_id || `${event.event_type}-${index}`}>
                <span><strong>{titleFromKey(event.event_type || 'event')}</strong><small>{dateTime(event.created_at)} | {event.source || 'server'}</small></span>
                <StatusPill status={event.is_server_signed ? 'active' : 'pending'}>{event.is_server_signed ? 'signed' : 'client'}</StatusPill>
              </article>
            ))}
            {!events?.length ? <EmptyState title="No analytics events" body="Events appear once Android/backend batching is active." /> : null}
          </div>
        </section>
      </div>
    </div>
  );
}

function ContentPanel({ inbox = [], reports, alerts, consentEvents, onResolve, onAck }) {
  const sortedInbox = [...(inbox || [])].sort((a, b) => {
    const severity = numberValue(b.severity_score) - numberValue(a.severity_score);
    if (severity !== 0) return severity;
    return new Date(a.created_at || 0) - new Date(b.created_at || 0);
  });
  return (
    <div className="admin-content">
      <SectionHeader title="Content & Moderation" description="Review profile reports, flagged content, DPDP consent activity and platform alerts." />
      <div className="workspace-columns even">
        <div className="admin-card full">
          <div className="card-title-row">
            <h3>Unified Moderation Inbox</h3>
            <StatusPill status="warning">{sortedInbox.length} open</StatusPill>
          </div>
          <div className="review-list moderation-inbox">
            {sortedInbox.slice(0, 40).map((item) => (
              <article key={`${item.item_type}-${item.item_id}`}>
                <span>
                  <strong>{item.title}</strong>
                  <small>{titleFromKey(item.item_type)} | {item.body || 'Review required'} | {dateTime(item.created_at)}</small>
                </span>
                <StatusPill status={numberValue(item.severity_score) >= 85 ? 'rejected' : 'pending'}>{item.severity_score}</StatusPill>
              </article>
            ))}
            {!sortedInbox.length ? <EmptyState title="Moderation queue clear" body="Reports, photo reviews, chat flags and verifications will appear here by severity and age." /> : null}
          </div>
        </div>
        <div className="admin-card">
          <h3>Flagged Content</h3>
          <div className="review-list">
            {reports.map((report) => (
              <article key={report.report_id}>
                <span><strong>{report.reason || 'Report'}</strong><small>{report.description || report.reporter_phone}</small></span>
                <AdminButton variant="primary" onClick={() => onResolve(report.report_id)}>Resolve</AdminButton>
              </article>
            ))}
            {!reports.length ? <EmptyState title="No open reports" body="User reports and flagged content are clear." /> : null}
          </div>
        </div>
        <div className="admin-card">
          <h3>Platform Alerts</h3>
          <div className="review-list">
            {alerts.map((alert) => (
              <article key={alert.alert_id}>
                <span><strong>{alert.title}</strong><small>{alert.body || alert.severity}</small></span>
                <AdminButton variant="secondary" onClick={() => onAck(alert.alert_id)}>Ack</AdminButton>
              </article>
            ))}
            {!alerts.length ? <EmptyState title="No open alerts" body="System monitoring has no unacknowledged alerts." /> : null}
          </div>
        </div>
        <div className="admin-card full">
          <h3>Consent Ledger</h3>
          <div className="data-table compact-table">
            <table>
              <thead><tr><th>User</th><th>Consent</th><th>Status</th><th>Purpose</th><th>Date</th></tr></thead>
              <tbody>
                {consentEvents.slice(0, 25).map((event) => (
                  <tr key={event.consent_event_id}>
                    <td>{fullName(event)}<small>{event.phone || event.email}</small></td>
                    <td>{event.consent_type}</td>
                    <td><StatusPill status={event.status}>{event.status}</StatusPill></td>
                    <td>{event.purpose}</td>
                    <td>{dateOnly(event.created_at)}</td>
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

function getAssignedAdminUsers(users, auditLogs, session) {
  const map = new Map();
  users
    .filter((user) => user.admin_user_id || String(user.user_type || '').toLowerCase() === 'admin')
    .forEach((user) => {
      const email = user.email || user.phone || user.user_id;
      map.set(email, {
        id: user.admin_user_id || user.user_id,
        name: user.display_name || (fullName(user) === 'Unnamed' ? email : fullName(user)),
        email,
        role: user.role || user.admin_role || 'admin',
        status: user.status || (user.is_active === false || user.is_banned ? 'inactive' : 'active'),
        joinedAt: user.created_at,
        lastActive: user.last_login || user.created_at,
        avatar: user.primary_photo_url
      });
    });
  auditLogs.forEach((log) => {
    const email = log.admin_email;
    if (!email) return;
    const existing = map.get(email);
    map.set(email, {
      id: existing?.id || email,
      name: existing?.name || titleFromKey(email.split('@')[0]),
      email,
      role: log.admin_role || existing?.role || 'admin',
      status: existing?.status || 'active',
      joinedAt: existing?.joinedAt || log.created_at,
      lastActive: log.created_at || existing?.lastActive,
      avatar: existing?.avatar
    });
  });
  if (session?.email && !map.has(session.email)) {
    map.set(session.email, {
      id: session.email,
      name: titleFromKey(session.email.split('@')[0]),
      email: session.email,
      role: session.role || 'admin',
      status: 'active',
      joinedAt: null,
      lastActive: new Date().toISOString()
    });
  }
  return Array.from(map.values()).sort((a, b) => new Date(b.lastActive || 0) - new Date(a.lastActive || 0));
}

function RoleMasterView({ roles, users, auditLogs, search, onSearch, session, onSaveRoles, onTab }) {
  const normalizedRoles = useMemo(() => (roles.length ? roles : DEFAULT_CONFIG.admin_roles.roles).map(normalizeRoleForUi), [roles]);
  const [draftRoles, setDraftRoles] = useState(normalizedRoles);
  const [selectedRoleId, setSelectedRoleId] = useState(normalizedRoles[0]?.role || 'super_admin');
  const [propagate, setPropagate] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ role: '', label: '', description: '', scope: 'custom' });

  useEffect(() => {
    setDraftRoles(normalizedRoles);
    if (!normalizedRoles.some((role) => role.role === selectedRoleId)) {
      setSelectedRoleId(normalizedRoles[0]?.role || 'super_admin');
    }
  }, [normalizedRoles, selectedRoleId]);

  const q = search.trim().toLowerCase();
  const selectedRole = draftRoles.find((role) => role.role === selectedRoleId) || draftRoles[0] || normalizeRoleForUi({ role: 'admin', label: 'Admin', permissions: [] });
  const permissionModules = useMemo(buildRbacModules, []);
  const assignedUsers = useMemo(() => getAssignedAdminUsers(users, auditLogs, session), [auditLogs, session, users]);
  const filteredRoles = draftRoles.filter((role) => {
    if (!q) return true;
    return [role.role, role.label, role.description, role.scope].filter(Boolean).join(' ').toLowerCase().includes(q);
  });
  const filteredUsers = assignedUsers.filter((user) => {
    const matchesRole = user.role === selectedRole.role || selectedRole.permissions.includes('*');
    const matchesSearch = !q || [user.name, user.email, user.role, user.status].filter(Boolean).join(' ').toLowerCase().includes(q);
    return matchesRole && matchesSearch;
  });
  const roleUserCount = (roleId) => assignedUsers.filter((user) => user.role === roleId).length;

  const updatePermission = (token, enabled) => {
    setDraftRoles((current) => current.map((role) => (
      role.role === selectedRole.role ? toggleRolePermission(role, token, enabled) : role
    )));
  };

  const createRole = (event) => {
    event.preventDefault();
    const nextRole = normalizeRoleForUi({
      ...form,
      role: form.role || form.label,
      permissions: ['dashboard:view']
    });
    if (!nextRole.role || draftRoles.some((role) => role.role === nextRole.role)) return;
    setDraftRoles((current) => [...current, nextRole]);
    setSelectedRoleId(nextRole.role);
    setForm({ role: '', label: '', description: '', scope: 'custom' });
    setShowCreate(false);
  };

  const deleteRole = () => {
    if (['super_admin', 'admin'].includes(selectedRole.role)) return;
    setDraftRoles((current) => current.filter((role) => role.role !== selectedRole.role));
    setSelectedRoleId(draftRoles.find((role) => role.role !== selectedRole.role)?.role || 'super_admin');
  };

  const toggleSuspendRole = () => {
    if (selectedRole.role === 'super_admin') return;
    setDraftRoles((current) => current.map((role) => (
      role.role === selectedRole.role ? { ...role, status: role.status === 'suspended' ? 'active' : 'suspended' } : role
    )));
  };

  const saveRoles = async () => {
    setSaving(true);
    try {
      await onSaveRoles({ roles: draftRoles });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="admin-content role-master-page">
      <div className="role-master-header">
        <div>
          <h2>Role Master</h2>
          <p>{draftRoles.length} roles | {assignedUsers.length} admin identities from users and audit logs</p>
        </div>
        <label className="role-master-search">
          <Icon name="search" />
          <input value={search} onChange={(event) => onSearch(event.target.value)} placeholder="Search roles or users..." />
        </label>
        <div className="role-master-actions">
          <button type="button" title="Notifications" onClick={() => onTab('notifications')}><Icon name="bell" /></button>
          <button type="button" title="Help"><Icon name="help" /></button>
          <AdminButton variant="secondary" onClick={() => setShowCreate((value) => !value)}><Icon name="plus" /> Create New Role</AdminButton>
        </div>
      </div>

      <div className="role-master-grid">
        <section className="role-list-panel">
          <div className="role-section-title">
            <span>Admin Roles</span>
            <StatusPill status="neutral">{filteredRoles.length} defined</StatusPill>
          </div>
          {showCreate ? (
            <form className="role-create-card" onSubmit={createRole}>
              <Input value={form.label} onChange={(value) => setForm((current) => ({ ...current, label: value, role: value.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '') }))} placeholder="Role name" required />
              <Input value={form.description} onChange={(value) => setForm((current) => ({ ...current, description: value }))} placeholder="Short role description" required />
              <Input value={form.scope} onChange={(value) => setForm((current) => ({ ...current, scope: value }))} placeholder="Scope label" />
              <AdminButton variant="primary" type="submit">Add Role</AdminButton>
            </form>
          ) : null}
          <div className="role-card-list">
            {filteredRoles.map((role) => (
              <button
                type="button"
                key={role.role}
                className={`role-card ${role.role === selectedRole.role ? 'active' : ''}`}
                onClick={() => setSelectedRoleId(role.role)}
              >
                <span className="role-card-top">
                  <strong>{role.label}</strong>
                  <Icon name={role.permissions.includes('*') ? 'crown' : role.role.includes('moderator') ? 'flag' : 'lock'} />
                </span>
                <p>{role.description}</p>
                <span className="role-card-meta">
                  <small><Icon name="users" /> {roleUserCount(role.role)} users</small>
                  <small className={`scope-${role.scope}`}>{titleFromKey(role.scope)}</small>
                  <small>{titleFromKey(role.status)}</small>
                </span>
              </button>
            ))}
          </div>
        </section>

        <section className="role-detail-panel">
          <div className="admin-card permission-card">
            <div className="permission-card-head">
              <h3><Icon name="lock" /> Permissions Matrix: {selectedRole.label}</h3>
              <label className="inline-switch">
                <span>Propagate changes to assigned users?</span>
                <input type="checkbox" checked={propagate} onChange={(event) => setPropagate(event.target.checked)} />
              </label>
            </div>
            <div className="permission-table">
              <table>
                <thead>
                  <tr>
                    <th>Module Name</th>
                    <th>View</th>
                    <th>Add</th>
                    <th>Edit</th>
                    <th>Delete</th>
                    <th>Export</th>
                  </tr>
                </thead>
                <tbody>
                  {permissionModules.map((module) => (
                    <tr key={module.key}>
                      <td><strong>{module.label}</strong><small>{module.section}</small></td>
                      {['view', 'add', 'edit', 'delete', 'export'].map((action) => {
                        const token = module.permissions[action];
                        return (
                          <td key={action}>
                            <input
                              type="checkbox"
                              checked={roleHasPermission(selectedRole, token)}
                              onChange={(event) => updatePermission(token, event.target.checked)}
                              aria-label={`${selectedRole.label} ${action} ${module.label}`}
                            />
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="permission-footer">
              <span>{propagate ? 'Changes apply to this role the next time assigned admins sign in.' : 'Role definition will be saved without propagation note.'}</span>
              <div>
                <AdminButton variant="secondary" onClick={toggleSuspendRole} disabled={selectedRole.role === 'super_admin'}>{selectedRole.status === 'suspended' ? 'Reactivate' : 'Suspend'} Role</AdminButton>
                <AdminButton variant="secondary" onClick={deleteRole} disabled={['super_admin', 'admin'].includes(selectedRole.role)}>Delete Role</AdminButton>
                <AdminButton variant="secondary" onClick={() => setDraftRoles(normalizedRoles)}>Reset</AdminButton>
                <AdminButton variant="primary" onClick={saveRoles} disabled={saving}>{saving ? 'Saving...' : 'Save Role Changes'}</AdminButton>
              </div>
            </div>
          </div>

          <div className="admin-card assigned-users-card">
            <div className="permission-card-head">
              <h3><Icon name="users" /> Assigned Users</h3>
              <AdminButton variant="secondary" onClick={() => onTab('user-master')}><Icon name="plus" /> Assign New User</AdminButton>
            </div>
            <div className="data-table compact-table">
              <table>
                <thead>
                  <tr>
                    <th>Staff Member</th>
                    <th>Email Address</th>
                    <th>Last Active</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.map((user) => (
                    <tr key={user.id || user.email}>
                      <td>
                        <div className="staff-cell">
                          {user.avatar ? <img src={user.avatar} alt="" /> : <b>{String(user.name || user.email).charAt(0).toUpperCase()}</b>}
                          <span><strong>{user.name}</strong><small>Joined {dateOnly(user.joinedAt)}</small></span>
                        </div>
                      </td>
                      <td>{user.email}</td>
                      <td>{dateTime(user.lastActive)}</td>
                      <td><StatusPill status={user.status}>{user.status}</StatusPill></td>
                      <td>
                        <div className="row-actions">
                          <button title="Edit user" onClick={() => onTab('user-master')}><Icon name="edit" /></button>
                          <button title="Reset password" onClick={() => onTab('change-password')}><Icon name="key" /></button>
                          <button title="Disable user" onClick={() => onTab('user-master')}><Icon name="ban" /></button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!filteredUsers.length ? <EmptyState title="No assigned users" body="Admin identities appear here after sign-in or audited actions." /> : null}
            </div>
          </div>

          <div className="admin-card permission-audit-card">
            <div className="card-title-row">
              <h3>Permission Audit Log</h3>
              <button type="button" onClick={() => onTab('audit-logs')}>View Full Logs</button>
            </div>
            <div className="permission-audit-list">
              {auditLogs.filter((log) => /role|permission|config|user|password/i.test(`${log.action || ''} ${log.entity_type || ''} ${log.change_description || ''}`)).slice(0, 4).map((log) => (
                <article key={log.audit_id || log.role_change_log_id || `${log.created_at}-${log.action || log.change_description}`}>
                  <time>{dateTime(log.created_at)}</time>
                  <span><strong>{log.admin_email || 'System'}</strong> {log.change_description || log.action} <em>{log.entity_type || 'role master'}</em></span>
                </article>
              ))}
              {!auditLogs.length ? <EmptyState title="No permission audit logs" body="Role and user changes will appear here after admins make updates." /> : null}
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

function ChangePasswordPanel({ adminUsers, session, onSaveAdminUser, onTab }) {
  const currentAdmin = adminUsers.find((user) => String(user.email || '').toLowerCase() === String(session.email || '').toLowerCase());
  const [form, setForm] = useState({ password: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    if (!currentAdmin?.admin_user_id) {
      setError('Create this admin in User Master before changing the database-backed password.');
      return;
    }
    if (form.password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    setSaving(true);
    try {
      setError('');
      await onSaveAdminUser({
        admin_user_id: currentAdmin.admin_user_id,
        email: currentAdmin.email,
        displayName: currentAdmin.display_name,
        role: currentAdmin.role,
        status: currentAdmin.status,
        password: form.password
      });
      setForm({ password: '', confirmPassword: '' });
    } finally {
      setSaving(false);
    }
  };
  return (
    <div className="admin-content settings-page">
      <SectionHeader
        title="Change Administrator Password"
        description="Update the signed-in admin account password through the secured admin-user endpoint."
        actions={<AdminButton variant="secondary" onClick={() => onTab('user-master')}><Icon name="person" /> User Master</AdminButton>}
      />
      <div className="workspace-columns even">
        <form className="admin-card admin-user-form" onSubmit={submit}>
          <h3>Password Reset</h3>
          <p className="muted">{currentAdmin ? `Changing password for ${currentAdmin.email}.` : 'Current fallback admin is not yet present in User Master.'}</p>
          <Field label="New password">
            <Input type="password" value={form.password} onChange={(value) => setForm((current) => ({ ...current, password: value }))} minLength={8} required />
          </Field>
          <Field label="Confirm password">
            <Input type="password" value={form.confirmPassword} onChange={(value) => setForm((current) => ({ ...current, confirmPassword: value }))} minLength={8} required />
          </Field>
          {error ? <p className="form-error">{error}</p> : null}
          <AdminButton variant="primary" type="submit" disabled={saving}>{saving ? 'Saving...' : 'Update Password'}</AdminButton>
        </form>
        <div className="admin-card settings-guidance">
          <h3>Secure Password Rules</h3>
          <div className="review-list">
            <article><span><strong>Minimum length</strong><small>Use at least 8 characters for admin-user passwords.</small></span><StatusPill status="active">Required</StatusPill></article>
            <article><span><strong>Fallback admin</strong><small>If using .env fallback login, rotate ADMIN_PASSWORD_HASH outside the dashboard.</small></span><StatusPill status="warning">Local only</StatusPill></article>
            <article><span><strong>Audit trail</strong><small>Password changes through User Master are protected by admin auth and role gates.</small></span><StatusPill status="active">Protected</StatusPill></article>
          </div>
        </div>
      </div>
    </div>
  );
}

function SystemPanel({ roles, users, adminUsers, auditLogs, services, activeTab, search, onSearch, session, onSaveRoles, onTab, systemInventory, onSaveAdminUser, onDeleteAdminUser }) {
  const deploymentLogs = auditLogs
    .filter((log) => /deploy|deployment|version|service health|release/i.test(`${log.action || ''} ${log.entity_type || ''}`))
    .slice(0, 20);
  const deploymentAudit = systemInventory?.deploymentAudit?.length ? systemInventory.deploymentAudit : deploymentLogs;
  if (activeTab === 'role-master') {
    return <RoleMasterView roles={roles} users={adminUsers?.length ? adminUsers : users} auditLogs={[...(systemInventory?.roleChangeLogs || []), ...auditLogs]} search={search} onSearch={onSearch} session={session} onSaveRoles={onSaveRoles} onTab={onTab} />;
  }
  if (activeTab === 'user-master') {
    return <AdminUsersPanel adminUsers={adminUsers || []} roles={roles} search={search} onSave={onSaveAdminUser} onDelete={onDeleteAdminUser} />;
  }
  if (activeTab === 'change-password') {
    return <ChangePasswordPanel adminUsers={adminUsers || []} session={session} onSaveAdminUser={onSaveAdminUser} onTab={onTab} />;
  }
  if (['system', 'data-export', 'settings'].includes(activeTab)) {
    return <SystemOverviewPanel services={services} inventory={systemInventory} deploymentAudit={deploymentAudit} onTab={onTab} />;
  }
  if (activeTab === 'service-health') {
    return (
      <div className="admin-content">
        <SectionHeader title="Service Health" description="Live status for backend services used by production." />
        <div className="service-grid">
          {services.map((service) => (
            <div className="admin-card service-card" key={service.name || service.service}>
              <StatusPill status={service.status || service.ok ? 'active' : 'failed'}>{service.status || (service.ok ? 'healthy' : 'down')}</StatusPill>
              <h3>{service.name || service.service}</h3>
              <p>{service.url || service.message || 'No endpoint reported'}</p>
            </div>
          ))}
        </div>
        <div className="admin-card full">
          <div className="card-title-row">
            <h3>Deployment / Version Audit</h3>
            <StatusPill status="neutral">{deploymentLogs.length} records</StatusPill>
          </div>
          <AuditLogTable logs={deploymentLogs} />
        </div>
      </div>
    );
  }
  if (activeTab === 'audit-logs') {
    return (
      <div className="admin-content">
        <SectionHeader title="Audit Logs" description="Every admin action should leave a recovery-grade audit trail." />
        <div className="admin-card"><AuditLogTable logs={auditLogs} /></div>
      </div>
    );
  }
  if (activeTab !== 'service-health' && activeTab !== 'audit-logs') {
    return <RoleMasterView roles={roles} users={users} auditLogs={auditLogs} search={search} onSearch={onSearch} session={session} onSaveRoles={onSaveRoles} onTab={onTab} />;
  }
  return (
    <div className="admin-content">
      <SectionHeader title="System Management" description="Roles, admin users, notifications, exports, settings and operational controls." />
      <div className="workspace-columns even">
        <div className="admin-card">
          <h3>Role Master</h3>
          <div className="role-list">
            {roles.map((role) => (
              <article key={role.role}>
                <strong>{role.label || role.role}</strong>
                <small>{Array.isArray(role.permissions) ? role.permissions.join(', ') : 'Configured'}</small>
              </article>
            ))}
          </div>
        </div>
        <div className="admin-card">
          <h3>User Master</h3>
          <div className="role-list">
            {users.slice(0, 12).map((user) => (
              <article key={user.user_id}>
                <strong>{fullName(user) || user.email || user.phone}</strong>
                <small>{user.email || user.phone} · {user.is_active ? 'active' : 'inactive'}</small>
              </article>
            ))}
          </div>
        </div>
        <div className="admin-card full">
          <h3>Operational Notes</h3>
          <p className="muted">Password reset, admin-user creation, granular RBAC persistence and one-click exports need dedicated backend endpoints before they can be made destructive. The redesigned console exposes the control surface and keeps existing live APIs wired safely.</p>
        </div>
      </div>
    </div>
  );
}

function DeploymentAuditTable({ rows }) {
  return (
    <div className="data-table compact-table">
      <table>
        <thead><tr><th>Timestamp</th><th>Admin/Agent</th><th>Release Description</th><th>Details of Changes</th><th>Release Version</th><th>Status</th><th>Change Type</th></tr></thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={row.deployment_audit_id || row.audit_id || index}>
              <td><code>{dateTime(row.timestamp || row.created_at)}</code></td>
              <td>{row.admin_actor || row.admin_email || 'system'}</td>
              <td>{row.release_description || row.action || 'Deployment activity'}</td>
              <td>{row.change_details || row.entity_type || row.source_commit || '-'}</td>
              <td>{row.release_version || row.source_commit?.slice(0, 8) || '-'}</td>
              <td><StatusPill status={row.deployment_status || 'neutral'}>{row.deployment_status || 'recorded'}</StatusPill></td>
              <td>{row.change_type || 'Both'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!rows.length ? <EmptyState title="No deployment audit records" body="New production deploys will write release/version records here." /> : null}
    </div>
  );
}

function SystemOverviewPanel({ services, inventory, deploymentAudit, compact = false, onTab }) {
  const exportSystemReport = () => window.print();
  return (
    <div className="admin-content system-overview-page">
      <SectionHeader
        title="System Management"
        description="Service health, deployment/version audit, configuration inventory and secure admin controls."
        actions={<AdminButton variant="secondary" onClick={exportSystemReport}><Icon name="export" /> Export PDF</AdminButton>}
      />
      <div className="admin-card full">
        <div className="card-title-row">
          <h3>Service Health</h3>
          <StatusPill status="neutral">{services.length} services</StatusPill>
        </div>
        <div className="service-grid embedded">
          {services.map((service) => (
            <div className="service-card compact" key={service.name || service.service}>
              <StatusPill status={service.status || service.ok ? 'active' : 'failed'}>{service.status || (service.ok ? 'healthy' : 'down')}</StatusPill>
              <h3>{service.name || service.service}</h3>
              <p>{service.url || service.message || 'No endpoint reported'}</p>
            </div>
          ))}
          {!services.length ? <EmptyState title="No service telemetry" body="Service health appears when the monitor endpoint is reachable." /> : null}
        </div>
      </div>
      <div className="admin-card full">
        <div className="card-title-row">
          <h3>Deployment / Version Audit Details</h3>
          <StatusPill status="neutral">{deploymentAudit.length} records</StatusPill>
        </div>
        <DeploymentAuditTable rows={deploymentAudit} />
      </div>
      {!compact ? (
        <div className="admin-card full">
          <div className="card-title-row">
            <h3>Systems Configuration & Secrets Inventory</h3>
            {onTab ? <button type="button" onClick={() => onTab('dynamic-config')}>Open Dynamic Configuration</button> : null}
          </div>
          <div className="inventory-grid">
            {(inventory?.inventory || []).map((section) => (
              <section key={section.section} className="inventory-section">
                <h4>{section.section}</h4>
                <p>{section.description}</p>
                <div className="inventory-list">
                  {section.items.map((item) => (
                    <article key={`${section.section}-${item.name}`}>
                      <span><strong>{item.name}</strong><small>{item.purpose}</small></span>
                      <StatusPill status={item.configured ? 'active' : 'pending'}>{item.configured ? 'Configured' : 'Missing'}</StatusPill>
                      <button type="button" title={`${item.howGenerated} ${item.rotationProcedure}`}><Icon name="help" /></button>
                    </article>
                  ))}
                </div>
              </section>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}

function AdminUsersPanel({ adminUsers, roles, search, onSave, onDelete }) {
  const [form, setForm] = useState({ email: '', password: '', displayName: '', role: roles[0]?.role || 'admin', status: 'active' });
  const [editingId, setEditingId] = useState(null);
  const q = search.trim().toLowerCase();
  const rows = adminUsers.filter((user) => !q || [user.email, user.display_name, user.role, user.status].filter(Boolean).join(' ').toLowerCase().includes(q));
  const save = async (event) => {
    event.preventDefault();
    await onSave({ ...form, admin_user_id: editingId || undefined });
    setEditingId(null);
    setForm({ email: '', password: '', displayName: '', role: roles[0]?.role || 'admin', status: 'active' });
  };
  const edit = (user) => {
    setEditingId(user.admin_user_id);
    setForm({ email: user.email, password: '', displayName: user.display_name || '', role: user.role || 'admin', status: user.status || 'active' });
  };
  return (
    <div className="admin-content admin-users-page">
      <SectionHeader title="User Master" description="Create dashboard login emails, assign RBAC roles, suspend access, or reset passwords." />
      <div className="workspace-columns even">
        <form className="admin-card admin-user-form" onSubmit={save}>
          <h3>{editingId ? 'Edit Admin Login' : 'Create Admin Login'}</h3>
          <Field label="Email"><Input type="email" value={form.email} onChange={(value) => setForm((current) => ({ ...current, email: value }))} required /></Field>
          <Field label={editingId ? 'New password (optional)' : 'Password'}><Input type="password" value={form.password} onChange={(value) => setForm((current) => ({ ...current, password: value }))} required={!editingId} minLength={8} /></Field>
          <Field label="Display name"><Input value={form.displayName} onChange={(value) => setForm((current) => ({ ...current, displayName: value }))} /></Field>
          <Field label="Role">
            <Select value={form.role} onChange={(value) => setForm((current) => ({ ...current, role: value }))}>
              {(roles.length ? roles : DEFAULT_CONFIG.admin_roles.roles).map((role) => <option key={role.role} value={role.role}>{role.label || titleFromKey(role.role)}</option>)}
            </Select>
          </Field>
          <Field label="Status"><Select value={form.status} onChange={(value) => setForm((current) => ({ ...current, status: value }))}><option>active</option><option>suspended</option></Select></Field>
          <AdminButton variant="primary" type="submit">{editingId ? 'Save Admin User' : 'Create Admin User'}</AdminButton>
        </form>
        <div className="admin-card data-table">
          <table>
            <thead><tr><th>Email</th><th>Role</th><th>Status</th><th>Last login</th><th>Actions</th></tr></thead>
            <tbody>
              {rows.map((user) => (
                <tr key={user.admin_user_id}>
                  <td><strong>{user.display_name || user.email}</strong><small>{user.email}</small></td>
                  <td>{titleFromKey(user.role)}</td>
                  <td><StatusPill status={user.status}>{user.status}</StatusPill></td>
                  <td>{dateTime(user.last_login)}</td>
                  <td><div className="row-actions"><button onClick={() => edit(user)}><Icon name="edit" /></button><button onClick={() => onDelete(user)}><Icon name="ban" /></button></div></td>
                </tr>
              ))}
            </tbody>
          </table>
          {!rows.length ? <EmptyState title="No admin users" body="Create an admin login to manage RBAC assignments from the console." /> : null}
        </div>
      </div>
    </div>
  );
}

function DynamicConfigPanel({ config, onSave }) {
  const [selected, setSelected] = useState('monetization');
  const [json, setJson] = useState(JSON.stringify(config.monetization || {}, null, 2));
  const [error, setError] = useState('');
  const configSections = [
    { key: 'monetization', label: 'Pricing Constants', icon: 'rupee', purpose: 'Plans, fixed pricing, premium limits and upgrade packages.' },
    { key: 'assisted_matchmaking', label: 'Assisted Matchmaking', icon: 'target', purpose: 'Advisor plans, member modes and assisted matching controls.' },
    { key: 'feature_flags', label: 'Operational Flags', icon: 'sliders', purpose: 'Feature rollout, experiments and app behavior toggles.' },
    { key: 'theme', label: 'Theme Tokens', icon: 'content', purpose: 'Runtime color, spacing and visual theme values.' },
    { key: 'branding', label: 'Branding', icon: 'star', purpose: 'Brand name, assets, labels and public identity.' },
    { key: 'navigation', label: 'Navigation', icon: 'grid', purpose: 'Mobile/web navigation and feature entry points.' },
    { key: 'maintenance', label: 'Maintenance', icon: 'gear', purpose: 'Maintenance mode, notices and outage controls.' },
    { key: 'content', label: 'Content', icon: 'cms', purpose: 'CMS-driven home, safety and prompt content.' },
    { key: 'legal', label: 'Legal', icon: 'lock', purpose: 'Policy links, consent copy and compliance text.' },
    { key: 'notification_templates', label: 'Notifications', icon: 'bell', purpose: 'Push/SMS/email template configuration.' },
    { key: 'client_integrations', label: 'Integration Keys', icon: 'key', purpose: 'Client-safe integration flags and endpoint labels.' },
    { key: 'payment_gateways', label: 'Payment Gateways', icon: 'invoice', purpose: 'Gateway settings and checkout display rules.' },
    { key: 'experiments', label: 'Experiments', icon: 'trend', purpose: 'A/B tests and growth experiments.' },
    { key: 'analytics', label: 'Analytics', icon: 'pulse', purpose: 'Tracking and reporting configuration.' },
    { key: 'seo_defaults', label: 'SEO Defaults', icon: 'search', purpose: 'Public SEO defaults and metadata.' },
    { key: 'admin_roles', label: 'Admin Roles Config', icon: 'crown', purpose: 'Role configuration mirrored by Role Master.' }
  ];
  const selectedSection = configSections.find((section) => section.key === selected) || configSections[0];

  useEffect(() => {
    setJson(JSON.stringify(config[selected] || {}, null, 2));
  }, [config, selected]);

  const save = () => {
    try {
      const parsed = JSON.parse(json);
      setError('');
      onSave(selected, parsed);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="admin-content configuration-page">
      <SectionHeader title="System Configuration" description="Control app variables, algorithm-facing weights, pricing constants, operational flags and integration settings without a mobile redeploy." actions={<AdminButton variant="primary" onClick={save}>Save Section</AdminButton>} />
      <div className="config-summary-grid">
        {configSections.slice(0, 4).map((section) => (
          <button type="button" key={section.key} className={`config-summary-card ${selected === section.key ? 'active' : ''}`} onClick={() => setSelected(section.key)}>
            <Icon name={section.icon} />
            <span>{section.label}</span>
            <small>{Object.keys(config[section.key] || {}).length} variables</small>
          </button>
        ))}
      </div>
      <div className="config-layout">
        <div className="admin-card config-nav">
          {configSections.map((section) => (
            <button key={section.key} className={selected === section.key ? 'active' : ''} onClick={() => setSelected(section.key)}>
              <Icon name={section.icon} />
              <span>{section.label}</span>
            </button>
          ))}
        </div>
        <div className="admin-card config-editor-card">
          <div className="card-title-row">
            <div>
              <h3>{selectedSection.label}</h3>
              <small>{selectedSection.purpose}</small>
            </div>
            <StatusPill status="neutral">{selected}</StatusPill>
          </div>
          <textarea className="json-editor large" value={json} onChange={(event) => setJson(event.target.value)} />
          {error ? <p className="form-error">{error}</p> : null}
        </div>
      </div>
    </div>
  );
}

function AuditLogTable({ logs }) {
  return (
    <div className="data-table compact-table">
      <table>
        <thead><tr><th>Timestamp</th><th>Admin/Agent</th><th>Action</th><th>Target</th><th>IP Address</th></tr></thead>
        <tbody>
          {logs.map((log, index) => (
            <tr key={`${log.created_at}-${index}`}>
              <td><code>{dateTime(log.created_at)}</code></td>
              <td>{log.admin_email || log.admin_role || 'system'}</td>
              <td><code>{log.action}</code></td>
              <td>{log.entity_type || '-'} {log.entity_id ? String(log.entity_id).slice(0, 8) : ''}</td>
              <td><code>{log.ip_address || '-'}</code></td>
            </tr>
          ))}
        </tbody>
      </table>
      {!logs.length ? <EmptyState title="No audit logs" body="Audit records will appear after admin actions are performed." /> : null}
    </div>
  );
}

function DrawerShell({ title, subtitle, onClose, children, footer }) {
  return (
    <div className="drawer-backdrop" role="presentation">
      <aside className="entity-drawer" role="dialog" aria-modal="true">
        <header>
          <div>
            <h3>{title}</h3>
            <p>{subtitle}</p>
          </div>
          <button onClick={onClose} aria-label="Close"><Icon name="close" /></button>
        </header>
        <div className="drawer-body">{children}</div>
        {footer ? <footer>{footer}</footer> : null}
      </aside>
    </div>
  );
}

function Field({ label, children }) {
  return <label className="admin-field"><span>{label}</span>{children}</label>;
}

function Input({ value, onChange, ...props }) {
  return <input {...props} value={value ?? ''} onChange={(event) => onChange(event.target.value)} />;
}

function Select({ value, onChange, children }) {
  return <select value={value ?? ''} onChange={(event) => onChange(event.target.value)}>{children}</select>;
}

function BoolSelect({ value, onChange }) {
  return (
    <select value={value ? 'true' : 'false'} onChange={(event) => onChange(event.target.value === 'true')}>
      <option value="true">Enabled</option>
      <option value="false">Disabled</option>
    </select>
  );
}

function MemberDrawer({ profile, onClose, onSave, onStatus, onPhotoAdd, onPhotoUpdate, onPhotoDelete }) {
  const [form, setForm] = useState(() => ({
    first_name: '',
    last_name: '',
    phone: '',
    email: '',
    dob: '',
    gender: '',
    religion: '',
    caste: '',
    mother_tongue: '',
    marital_status: 'never_married',
    verification_status: 'pending',
    admin_status: 'active',
    profile_status: 'active',
    profile_created_by: 'self',
    is_published: false,
    photo_privacy: 'all',
    profile_visibility: 'all',
    hide_last_seen: false,
    ...profile,
    dob: profile?.dob ? String(profile.dob).slice(0, 10) : ''
  }));
  const [newPhotoUrl, setNewPhotoUrl] = useState('');
  const [newPhotoUpload, setNewPhotoUpload] = useState(null);
  const [saving, setSaving] = useState(false);
  const set = (key, value) => setForm((current) => ({ ...current, [key]: value }));
  const submit = async () => {
    setSaving(true);
    try {
      await onSave(form);
    } finally {
      setSaving(false);
    }
  };
  const addPhoto = async () => {
    if (!form.profile_id || (!newPhotoUrl.trim() && !newPhotoUpload?.dataUrl)) return;
    const photo = await onPhotoAdd(form.profile_id, {
      photoUrl: newPhotoUrl.trim(),
      photoDataUrl: newPhotoUpload?.dataUrl || undefined,
      fileName: newPhotoUpload?.fileName || undefined,
      isPrimary: !Array.isArray(form.photos) || form.photos.length === 0
    });
    if (photo) {
      setForm((current) => ({
        ...current,
        primary_photo_url: photo.is_primary ? photo.photo_url : current.primary_photo_url,
        photos: [photo, ...(Array.isArray(current.photos) ? current.photos : [])]
      }));
      setNewPhotoUrl('');
      setNewPhotoUpload(null);
    }
  };
  const handlePhotoFile = (file) => {
    if (!file) {
      setNewPhotoUpload(null);
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      setNewPhotoUpload({ fileName: file.name, dataUrl: String(reader.result || '') });
    };
    reader.readAsDataURL(file);
  };
  const makePrimaryPhoto = async (photo) => {
    if (!form.profile_id || !photo?.photo_id) return;
    const updated = await onPhotoUpdate(form.profile_id, photo.photo_id, { isPrimary: true });
    if (updated) {
      setForm((current) => ({
        ...current,
        primary_photo_url: updated.photo_url,
        photos: (Array.isArray(current.photos) ? current.photos : []).map((item) => ({
          ...item,
          is_primary: item.photo_id === updated.photo_id
        }))
      }));
    }
  };
  const deletePhoto = async (photo) => {
    if (!form.profile_id || !photo?.photo_id) return;
    const ok = await onPhotoDelete(form.profile_id, photo.photo_id);
    if (ok) {
      setForm((current) => {
        const remaining = (Array.isArray(current.photos) ? current.photos : []).filter((item) => item.photo_id !== photo.photo_id);
        const primary = remaining.find((item) => item.is_primary) || remaining[0];
        return { ...current, photos: remaining, primary_photo_url: primary?.photo_url || '' };
      });
    }
  };

  return (
    <DrawerShell
      title={profile?.profile_id ? 'Member 360 View' : 'Create Member'}
      subtitle={profile?.profile_id ? `${fullName(form)} | ${memberDisplayId(form)}` : 'Admin-created member profile'}
      onClose={onClose}
      footer={(
        <>
          {profile?.profile_id ? <AdminButton variant="secondary" onClick={() => onStatus(form, 'reject')}>Reject</AdminButton> : null}
          {profile?.profile_id ? <AdminButton variant="secondary" onClick={() => onStatus(form, form.is_published ? 'suspend' : 'restore')}>{form.is_published ? 'Hide' : 'Show'} Profile</AdminButton> : null}
          <AdminButton variant="primary" onClick={submit} disabled={saving}>{saving ? 'Saving...' : 'Save Member'}</AdminButton>
        </>
      )}
    >
      <div className="drawer-section">
        <h4>Identity & Access</h4>
        <div className="form-grid two">
          <Field label="First name"><Input value={form.first_name} onChange={(v) => set('first_name', v)} /></Field>
          <Field label="Last name"><Input value={form.last_name} onChange={(v) => set('last_name', v)} /></Field>
          <Field label="Mobile"><Input value={form.phone} onChange={(v) => set('phone', v)} /></Field>
          <Field label="Email"><Input value={form.email} onChange={(v) => set('email', v)} /></Field>
          <Field label="DOB"><Input type="date" value={form.dob} onChange={(v) => set('dob', v)} /></Field>
          <Field label="Gender"><Select value={form.gender} onChange={(v) => set('gender', v)}><option value="">Select</option><option>male</option><option>female</option><option>other</option></Select></Field>
          <Field label="Verification"><Select value={form.verification_status} onChange={(v) => set('verification_status', v)}><option>pending</option><option>verified</option><option>rejected</option></Select></Field>
          <Field label="Admin status"><Select value={form.admin_status} onChange={(v) => set('admin_status', v)}><option>active</option><option>suspended</option><option>rejected</option></Select></Field>
        </div>
      </div>
      <div className="drawer-section">
        <h4>Profile Details</h4>
        <div className="form-grid two">
          <Field label="Religion"><Input value={form.religion} onChange={(v) => set('religion', v)} /></Field>
          <Field label="Caste"><Input value={form.caste} onChange={(v) => set('caste', v)} /></Field>
          <Field label="Mother tongue"><Input value={form.mother_tongue} onChange={(v) => set('mother_tongue', v)} /></Field>
          <Field label="Marital status"><Input value={form.marital_status} onChange={(v) => set('marital_status', v)} /></Field>
          <Field label="Height cm"><Input type="number" value={form.height_cm} onChange={(v) => set('height_cm', v)} /></Field>
          <Field label="Weight kg"><Input type="number" value={form.weight_kg} onChange={(v) => set('weight_kg', v)} /></Field>
          <Field label="Complexion"><Input value={form.complexion} onChange={(v) => set('complexion', v)} /></Field>
          <Field label="Body type"><Input value={form.body_type} onChange={(v) => set('body_type', v)} /></Field>
        </div>
      </div>
      <div className="drawer-section">
        <h4>Education, Work & Family</h4>
        <div className="form-grid two">
          <Field label="Education"><Input value={form.education_level} onChange={(v) => set('education_level', v)} /></Field>
          <Field label="Currently employed"><BoolSelect value={form.is_employed} onChange={(v) => set('is_employed', v)} /></Field>
          <Field label="Occupation"><Input value={form.occupation} onChange={(v) => set('occupation', v)} /></Field>
          <Field label="Annual income"><Input value={form.annual_income} onChange={(v) => set('annual_income', v)} /></Field>
          <Field label="Working city"><Input value={form.working_city} onChange={(v) => set('working_city', v)} /></Field>
          <Field label="Working state"><Input value={form.working_state} onChange={(v) => set('working_state', v)} /></Field>
          <Field label="Family city"><Input value={form.family_city} onChange={(v) => set('family_city', v)} /></Field>
          <Field label="Family type"><Input value={form.family_type} onChange={(v) => set('family_type', v)} /></Field>
          <Field label="Father occupation"><Input value={form.father_occupation} onChange={(v) => set('father_occupation', v)} /></Field>
          <Field label="Mother occupation"><Input value={form.mother_occupation} onChange={(v) => set('mother_occupation', v)} /></Field>
        </div>
      </div>
      <div className="drawer-section">
        <h4>Visibility, Preferences & Notes</h4>
        <div className="form-grid two">
          <Field label="Published"><BoolSelect value={form.is_published} onChange={(v) => set('is_published', v)} /></Field>
          <Field label="Photo privacy"><Select value={form.photo_privacy} onChange={(v) => set('photo_privacy', v)}><option>all</option><option>matches_only</option><option>request_only</option><option>private</option></Select></Field>
          <Field label="Age min"><Input type="number" value={form.age_min} onChange={(v) => set('age_min', v)} /></Field>
          <Field label="Age max"><Input type="number" value={form.age_max} onChange={(v) => set('age_max', v)} /></Field>
          <Field label="Preference religion"><Input value={form.preference_religion} onChange={(v) => set('preference_religion', v)} /></Field>
          <Field label="Preference locations"><Input value={Array.isArray(form.locations) ? form.locations.join(', ') : form.locations} onChange={(v) => set('locations', v)} /></Field>
        </div>
        <Field label="About me"><textarea value={form.about_me || ''} onChange={(event) => set('about_me', event.target.value)} /></Field>
        <Field label="Admin review notes"><textarea value={form.review_notes || ''} onChange={(event) => set('review_notes', event.target.value)} /></Field>
      </div>
      <div className="drawer-section">
        <h4>Photos, Documents & Actions</h4>
        <div className="profile-360-grid">
          <div>
            <strong>Photos</strong>
            {form.profile_id ? (
              <div className="photo-admin-toolbar">
                <input
                  value={newPhotoUrl}
                  onChange={(event) => setNewPhotoUrl(event.target.value)}
                  placeholder="Paste HTTPS image URL or /uploads path"
                />
                <label className="photo-admin-file">
                  <Icon name="image" />
                  <span>{newPhotoUpload?.fileName || 'Choose file'}</span>
                  <input type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => handlePhotoFile(event.target.files?.[0])} />
                </label>
                <button type="button" onClick={addPhoto} disabled={!newPhotoUrl.trim() && !newPhotoUpload?.dataUrl}>
                  <Icon name="plus" /> Add / upload
                </button>
              </div>
            ) : (
              <Field label="Primary photo URL"><Input value={form.primary_photo_url} onChange={(v) => set('primary_photo_url', v)} /></Field>
            )}
            <div className="document-list">
              {(Array.isArray(form.photos) ? form.photos : []).map((photo) => (
                <article key={photo.photo_id || photo.photo_url} className="photo-admin-row">
                  <img src={photo.photo_url} alt="" />
                  <span>{photo.is_primary ? 'Primary photo' : 'Gallery photo'}<small>{photo.is_approved ? 'Approved' : 'Needs moderation'} | {dateOnly(photo.uploaded_at)}</small></span>
                  <div className="photo-admin-actions">
                    <a href={photo.photo_url} target="_blank" rel="noreferrer" title="Open image"><Icon name="eye" /></a>
                    {!photo.is_primary ? <button type="button" onClick={() => makePrimaryPhoto(photo)} title="Make primary"><Icon name="star" /></button> : null}
                    <button type="button" onClick={() => deletePhoto(photo)} title="Delete photo"><Icon name="close" /></button>
                  </div>
                </article>
              ))}
              {!Array.isArray(form.photos) || !form.photos.length ? <small className="muted">No photos uploaded.</small> : null}
            </div>
          </div>
          <div>
            <strong>Documents</strong>
            <div className="document-list">
              {(Array.isArray(form.documents) ? form.documents : []).map((document) => (
                <article key={document.profile_document_id || document.file_url}>
                  <span>{titleFromKey(document.document_type)}<small>{document.status} | {document.review_comment || 'No admin comment'}</small></span>
                  <a href={document.file_url} target="_blank" rel="noreferrer">Open</a>
                </article>
              ))}
              {!Array.isArray(form.documents) || !form.documents.length ? <small className="muted">No documents uploaded.</small> : null}
            </div>
          </div>
          <div>
            <strong>Interest activity</strong>
            <div className="mini-stat-row as-tags">
              <span>Sent <strong>{form.interests_sent_count || 0}</strong></span>
              <span>Received <strong>{form.interests_received_count || 0}</strong></span>
              <span>Shortlists <strong>{form.shortlist_count || 0}</strong></span>
              <span>Views <strong>{form.view_count || 0}</strong></span>
              <span>Reports <strong>{form.report_count || 0}</strong></span>
            </div>
          </div>
          <div>
            <strong>Recent interactions</strong>
            <div className="document-list">
              {(Array.isArray(form.interests) ? form.interests : []).slice(0, 5).map((interest) => (
                <article key={interest.interest_id}>
                  <span>{titleFromKey(interest.direction)} interest<small>{interest.status} | {dateOnly(interest.sent_at)}</small></span>
                  <code>{String(interest.other_profile_id || '').slice(0, 8)}</code>
                </article>
              ))}
              {!Array.isArray(form.interests) || !form.interests.length ? <small className="muted">No interests recorded yet.</small> : null}
            </div>
          </div>
        </div>
      </div>
    </DrawerShell>
  );
}

function AgentDrawer({ agent, linkedProfiles, onClose, onSave, onStatus }) {
  const [form, setForm] = useState(() => ({
    full_name: '',
    phone: '',
    email: '',
    business_name: '',
    city: '',
    state: '',
    pincode: '',
    languages: [],
    communities: [],
    years_experience: '',
    service_label: 'SoulMatch Advisor',
    kyc_status: 'pending',
    status: 'active',
    membership_plan: 'free',
    ...agent
  }));
  const [saving, setSaving] = useState(false);
  const set = (key, value) => setForm((current) => ({ ...current, [key]: value }));
  const submit = async () => {
    setSaving(true);
    try {
      await onSave(form);
    } finally {
      setSaving(false);
    }
  };
  const docs = Array.isArray(form.kyc_documents) ? form.kyc_documents : [];

  return (
    <DrawerShell
      title={agent?.advisor_id ? 'Agent 360 View' : 'Create Agent'}
      subtitle={agent?.advisor_id ? `${form.full_name} · ${form.agent_code || form.advisor_id}` : 'Admin-created agent account'}
      onClose={onClose}
      footer={(
        <>
          {agent?.advisor_id ? <AdminButton variant="secondary" onClick={() => onStatus(form, { kycStatus: 'rejected' })}>Reject KYC</AdminButton> : null}
          {agent?.advisor_id ? <AdminButton variant="secondary" onClick={() => onStatus(form, { status: form.status === 'active' ? 'suspended' : 'active' })}>{form.status === 'active' ? 'Suspend' : 'Activate'}</AdminButton> : null}
          <AdminButton variant="primary" onClick={submit} disabled={saving}>{saving ? 'Saving...' : 'Save Agent'}</AdminButton>
        </>
      )}
    >
      <div className="drawer-section">
        <h4>Business Profile</h4>
        <div className="form-grid two">
          <Field label="Full name"><Input value={form.full_name} onChange={(v) => set('full_name', v)} /></Field>
          <Field label="Business name"><Input value={form.business_name} onChange={(v) => set('business_name', v)} /></Field>
          <Field label="Phone"><Input value={form.phone} onChange={(v) => set('phone', v)} /></Field>
          <Field label="Email"><Input value={form.email} onChange={(v) => set('email', v)} /></Field>
          <Field label="City"><Input value={form.city} onChange={(v) => set('city', v)} /></Field>
          <Field label="State"><Input value={form.state} onChange={(v) => set('state', v)} /></Field>
          <Field label="Experience"><Input type="number" value={form.years_experience} onChange={(v) => set('years_experience', v)} /></Field>
          <Field label="Languages"><Input value={Array.isArray(form.languages) ? form.languages.join(', ') : form.languages} onChange={(v) => set('languages', v)} /></Field>
        </div>
        <Field label="Bio"><textarea value={form.bio || ''} onChange={(event) => set('bio', event.target.value)} /></Field>
      </div>
      <div className="drawer-section">
        <h4>Fraud Controls & KYC</h4>
        <div className="status-grid">
          <StatusTile label="Aadhaar" value={form.aadhaar_verification_status || 'not_started'} />
          <StatusTile label="PAN" value={form.pan_verification_status || 'not_started'} />
          <StatusTile label="Bank" value={form.bank_verification_status || 'not_started'} />
          <StatusTile label="Penny Drop" value={form.penny_drop_status || 'not_started'} />
          <StatusTile label="Terms" value={form.terms_accepted_at ? 'signed' : 'pending'} />
          <StatusTile label="Fraud Review" value={form.fraud_review_status || 'pending'} />
        </div>
        <div className="form-grid two">
          <Field label="KYC status"><Select value={form.kyc_status} onChange={(v) => set('kyc_status', v)}><option>pending</option><option>approved</option><option>rejected</option></Select></Field>
          <Field label="Agent status"><Select value={form.status} onChange={(v) => set('status', v)}><option>active</option><option>paused</option><option>suspended</option></Select></Field>
          <Field label="Membership plan"><Input value={form.membership_plan} onChange={(v) => set('membership_plan', v)} /></Field>
          <Field label="Membership expiry"><Input type="date" value={form.membership_expires_at ? String(form.membership_expires_at).slice(0, 10) : ''} onChange={(v) => set('membership_expires_at', v)} /></Field>
        </div>
        <div className="document-list">
          {docs.map((doc) => (
            <article key={doc.advisorKycDocumentId || doc.fileUrl}>
              <span>{doc.documentType || doc.document_type}<small>{doc.documentSide || doc.document_side}</small></span>
              <StatusPill status={doc.status}>{doc.status}</StatusPill>
              {doc.fileUrl ? <a href={doc.fileUrl} target="_blank" rel="noreferrer">View</a> : null}
            </article>
          ))}
          {!docs.length ? <EmptyState title="No KYC documents" body="Uploaded Aadhaar, PAN, voter ID or cheque documents will appear here." /> : null}
        </div>
      </div>
      <div className="drawer-section">
        <h4>Linked Member Profiles</h4>
        <div className="document-list">
          {linkedProfiles.map((profile) => (
            <article key={profile.profile_id}>
              <span>{fullName(profile)}<small>{profile.review_status || profile.verification_status}</small></span>
              <StatusPill status={profile.is_published ? 'active' : 'pending'}>{profile.is_published ? 'Visible' : 'Pending'}</StatusPill>
            </article>
          ))}
          {!linkedProfiles.length ? <EmptyState title="No member profiles linked" body="Agent-created profiles will be linked here automatically." /> : null}
        </div>
        <Field label="Internal notes"><textarea value={form.notes || ''} onChange={(event) => set('notes', event.target.value)} /></Field>
      </div>
    </DrawerShell>
  );
}

function HelpDrawer({ onClose }) {
  return (
    <DrawerShell title="Admin Help" subtitle="Quick actions and dashboard navigation" onClose={onClose}>
      <div className="drawer-section help-list">
        <h4>Dashboard controls</h4>
        <article>
          <Icon name="search" />
          <span>Use the top search box to filter dashboard member rows and management tables by profile ID, name, phone, email, plan or source.</span>
        </article>
        <article>
          <Icon name="bell" />
          <span>Notifications opens engagement and system notices that need admin attention.</span>
        </article>
        <article>
          <Icon name="gear" />
          <span>Admin Settings opens console configuration, account options, CMS and system preferences.</span>
        </article>
        <article>
          <Icon name="plus" />
          <span>Add Member opens the admin 360-degree member creation drawer without leaving the dashboard.</span>
        </article>
      </div>
    </DrawerShell>
  );
}

function StatusTile({ label, value }) {
  return (
    <div className="status-tile">
      <span>{label}</span>
      <StatusPill status={value}>{value}</StatusPill>
    </div>
  );
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const { section } = useParams();
  const routeTab = ROUTE_TO_TAB[section || 'overview'] || 'dashboard';
  const [activeTab, setActiveTab] = useState(routeTab);
  const [search, setSearch] = useState('');
  const [stats, setStats] = useState(EMPTY_STATS);
  const [profiles, setProfiles] = useState([]);
  const [advisors, setAdvisors] = useState([]);
  const [assistAssignments, setAssistAssignments] = useState([]);
  const [verifications, setVerifications] = useState([]);
  const [profileDocuments, setProfileDocuments] = useState([]);
  const [payments, setPayments] = useState({ transactions: [], plans: [], coupons: [], pendingOrders: [], invoices: [], revenueSummary: [] });
  const [alerts, setAlerts] = useState([]);
  const [reports, setReports] = useState([]);
  const [moderationInbox, setModerationInbox] = useState([]);
  const [consentEvents, setConsentEvents] = useState([]);
  const [roles, setRoles] = useState([]);
  const [users, setUsers] = useState([]);
  const [adminUsers, setAdminUsers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [funnel, setFunnel] = useState([]);
  const [events, setEvents] = useState([]);
  const [services, setServices] = useState([]);
  const [systemInventory, setSystemInventory] = useState({ inventory: [], deploymentAudit: [], roleChangeLogs: [] });
  const [config, setConfig] = useState(DEFAULT_CONFIG);
  const [drawer, setDrawer] = useState(null);
  const [notice, setNotice] = useState('');
  const session = useMemo(decodeSession, []);

  useEffect(() => {
    setActiveTab(routeTab);
  }, [routeTab]);

  const navigateTab = useCallback((tabId) => {
    setActiveTab(tabId);
    const route = TAB_TO_ROUTE[tabId] || 'overview';
    navigate(`/dashboard/${route}`);
  }, [navigate]);

  useEffect(() => {
    if (!notice) return undefined;
    const timer = window.setTimeout(() => setNotice(''), 5000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const reload = useCallback(async () => {
    const [
      dashboardRes,
      realtimeRes,
      profilesRes,
      advisorsRes,
      assignmentsRes,
      verificationsRes,
      profileDocumentsRes,
      paymentsRes,
      alertsRes,
      reportsRes,
      moderationRes,
      consentRes,
      rolesRes,
      adminUsersRes,
      usersRes,
      auditRes,
      funnelRes,
      eventsRes,
      serviceRes,
      inventoryRes,
      configRes
    ] = await Promise.all([
      getDashboard().catch(() => ({ data: { data: EMPTY_STATS } })),
      getRealtimeSnapshot().catch(() => ({ data: { data: null } })),
      getProfiles({ page: 1, limit: 100, search: '' }).catch(() => ({ data: { data: [] } })),
      getAdvisors().catch(() => ({ data: { data: [] } })),
      getAssistedAssignments().catch(() => ({ data: { data: [] } })),
      getVerifications().catch(() => ({ data: { data: [] } })),
      getProfileDocuments().catch(() => ({ data: { data: [] } })),
      getPayments().catch(() => ({ data: { data: { transactions: [], plans: [], coupons: [], pendingOrders: [], invoices: [], revenueSummary: [] } } })),
      getAlerts().catch(() => ({ data: { data: [] } })),
      getReports().catch(() => ({ data: { data: [] } })),
      getModerationInbox().catch(() => ({ data: { data: [] } })),
      getConsentEvents().catch(() => ({ data: { data: [] } })),
      getRoles().catch(() => ({ data: { data: [] } })),
      getAdminUsers().catch(() => ({ data: { data: [] } })),
      getUsers(1, '').catch(() => ({ data: { data: [] } })),
      getAuditLogs(200).catch(() => ({ data: { data: [] } })),
      getAnalyticsFunnel().catch(() => ({ data: { data: [] } })),
      getAnalyticsEvents(100).catch(() => ({ data: { data: [] } })),
      getServiceHealth().catch(() => ({ data: { data: [] } })),
      getSystemInventory().catch(() => ({ data: { data: { inventory: [], deploymentAudit: [], roleChangeLogs: [] } } })),
      getConfig().catch(() => ({ data: { data: { config: DEFAULT_CONFIG } } }))
    ]);
    const baseStats = { ...EMPTY_STATS, ...(dashboardRes.data?.data || {}) };
    setStats({ ...baseStats, ...(realtimeRes.data?.data || {}) });
    setProfiles(profilesRes.data?.data || []);
    setAdvisors(advisorsRes.data?.data || []);
    setAssistAssignments(assignmentsRes.data?.data || []);
    setVerifications(verificationsRes.data?.data || []);
    setProfileDocuments(profileDocumentsRes.data?.data || []);
    setPayments(paymentsRes.data?.data || { transactions: [], plans: [], coupons: [], pendingOrders: [], invoices: [], revenueSummary: [] });
    setAlerts(alertsRes.data?.data || []);
    setReports(reportsRes.data?.data || []);
    setModerationInbox(moderationRes.data?.data || []);
    setConsentEvents(consentRes.data?.data || []);
    setRoles(rolesRes.data?.data || []);
    setAdminUsers(adminUsersRes.data?.data || []);
    setUsers(usersRes.data?.data || []);
    setAuditLogs(auditRes.data?.data || []);
    setFunnel(funnelRes.data?.data || []);
    setEvents(eventsRes.data?.data || []);
    setServices(serviceRes.data?.data || []);
    setSystemInventory(inventoryRes.data?.data || { inventory: [], deploymentAudit: [], roleChangeLogs: [] });
    setConfig(normalizeConfig(configRes.data?.data?.config || DEFAULT_CONFIG));
  }, []);

  useEffect(() => {
    reload().catch((err) => setNotice(err.message));
  }, [reload]);

  useEffect(() => {
    const socket = io(ADMIN_SOCKET_URL, {
      auth: { token: localStorage.getItem('adminToken') },
      transports: ['websocket', 'polling']
    });
    socket.on('admin:dashboard_metrics', (payload) => setStats((current) => ({ ...current, ...(payload || {}) })));
    socket.on('admin:profile_created', reload);
    socket.on('admin:profile_updated', reload);
    socket.on('admin:profile_status', reload);
    socket.on('admin:config_updated', reload);
    return () => socket.disconnect();
  }, [reload]);

  const withNotice = async (promise, message) => {
    try {
      await promise;
      setNotice(message);
      await reload();
    } catch (err) {
      setNotice(err.response?.data?.error?.message || err.message);
    }
  };

  const handleSaveProfile = async (form) => {
    const payload = makeProfilePayload(form);
    if (form.profile_id) {
      await withNotice(updateProfile(form.profile_id, payload), 'Member profile updated.');
    } else {
      await withNotice(createProfile(payload), 'Member profile created.');
    }
    setDrawer(null);
  };

  const handleProfileStatus = async (profile, action) => {
    const labels = {
      approve: 'Member verified and published.',
      unverify: 'Member marked unverified.',
      reject: 'Member rejected.',
      suspend: 'Member hidden from discovery.',
      restore: 'Member visibility restored.'
    };
    await withNotice(updateProfileStatus(profile.profile_id, action, `Admin ${action} from console`), labels[action] || `Member ${action} completed.`);
  };

  const handleAddProfilePhoto = async (profileId, payload) => {
    try {
      const response = await addProfilePhoto(profileId, payload);
      setNotice('Profile photo added.');
      await reload();
      return response.data?.data;
    } catch (err) {
      setNotice(err.response?.data?.error?.message || err.message);
      return null;
    }
  };

  const handleUpdateProfilePhoto = async (profileId, photoId, payload) => {
    try {
      const response = await updateProfilePhoto(profileId, photoId, payload);
      setNotice('Profile photo updated.');
      await reload();
      return response.data?.data;
    } catch (err) {
      setNotice(err.response?.data?.error?.message || err.message);
      return null;
    }
  };

  const handleDeleteProfilePhoto = async (profileId, photoId) => {
    try {
      await deleteProfilePhoto(profileId, photoId);
      setNotice('Profile photo deleted.');
      await reload();
      return true;
    } catch (err) {
      setNotice(err.response?.data?.error?.message || err.message);
      return false;
    }
  };

  const handleBlockProfile = async (profile) => {
    if (!profile.user_id) return;
    const blocked = profile.is_banned === true;
    await withNotice(blocked ? unbanUser(profile.user_id) : banUser(profile.user_id), blocked ? 'Member unblocked.' : 'Member blocked.');
  };

  const handleDeleteProfile = async (profile) => {
    if (!profile?.profile_id) return;
    const label = profile.first_name || profile.profile_display_id || profile.profile_id;
    if (!window.confirm(`Delete ${label}? This removes the member profile from admin records.`)) return;
    await withNotice(deleteProfile(profile.profile_id), 'Member profile deleted.');
    setDrawer(null);
  };

  const handleBulkProfileStatus = async (items, action) => {
    if (!items.length) return;
    await withNotice(
      Promise.all(items.map((profile) => updateProfileStatus(profile.profile_id, action, `Admin bulk ${action} from console`))),
      `${items.length} member profiles updated.`
    );
  };

  const handleBulkProfileUpdate = async (items, field, value) => {
    if (!items.length) return;
    const allowedFields = {
      adminStatus: 'adminStatus',
      photoPrivacy: 'photoPrivacy',
      profileVisibility: 'profileVisibility',
      reviewNotes: 'reviewNotes'
    };
    const payloadKey = allowedFields[field];
    if (!payloadKey) return;
    await withNotice(
      Promise.all(items.map((profile) => updateProfile(profile.profile_id, { ...makeProfilePayload(profile), [payloadKey]: value }))),
      `${items.length} member profiles bulk edited.`
    );
  };

  const handleBulkDeleteProfiles = async (items) => {
    if (!items.length) return;
    if (!window.confirm(`Delete ${items.length} selected member profiles? This cannot be undone from the dashboard.`)) return;
    await withNotice(
      Promise.all(items.map((profile) => deleteProfile(profile.profile_id))),
      `${items.length} member profiles deleted.`
    );
  };

  const handleSaveAdvisor = async (form) => {
    const payload = makeAdvisorPayload(form);
    if (form.advisor_id) {
      await withNotice(updateAdvisor(form.advisor_id, payload), 'Agent profile updated.');
    } else {
      await withNotice(createAdvisor(payload), 'Agent profile created.');
    }
    setDrawer(null);
  };

  const handleAdvisorStatus = async (agent, payload) => {
    await withNotice(updateAdvisorStatus(agent.advisor_id, payload), 'Agent status updated.');
  };

  const handleConfigSave = async (key, payload) => {
    await withNotice(updateConfig(key, payload), 'Configuration saved.');
  };

  const handleSaveRoles = async (payload) => {
    await withNotice(updateConfig('admin_roles', payload), 'Role permissions saved.');
  };

  const handleUpdateAssignment = async (id, payload) => {
    await withNotice(updateAssistedAssignment(id, payload), 'Assist assignment updated.');
  };

  const handleSaveAdminUser = async (payload) => {
    if (payload.admin_user_id) {
      const { admin_user_id: id, ...rest } = payload;
      await withNotice(updateAdminUser(id, rest), 'Admin user updated.');
    } else {
      await withNotice(createAdminUser(payload), 'Admin user created.');
    }
  };

  const handleDeleteAdminUser = async (user) => {
    if (!user?.admin_user_id) return;
    await withNotice(deleteAdminUser(user.admin_user_id), 'Admin user removed.');
  };

  const renderContent = () => {
    if (['dashboard', 'overview'].includes(activeTab)) {
      return <DashboardHome stats={stats} profiles={profiles} advisors={advisors} payments={payments} alerts={alerts} auditLogs={auditLogs} search={search} onTab={navigateTab} onMember={(profile) => setDrawer({ type: 'member', entity: profile })} onAgent={(agent) => setDrawer({ type: 'agent', entity: agent })} onCreateMember={() => setDrawer({ type: 'member', entity: null })} />;
    }
    if (activeTab === 'members') {
      return <MembersDirectoryPanel profiles={profiles} search={search} onOpen={(profile) => setDrawer({ type: 'member', entity: profile })} onCreate={() => setDrawer({ type: 'member', entity: null })} />;
    }
    if (activeTab === 'user-control') {
      return (
        <UserControlPanel
          profiles={profiles}
          users={users}
          reports={reports}
          search={search}
          onOpen={(profile) => setDrawer({ type: 'member', entity: profile })}
          onCreate={() => setDrawer({ type: 'member', entity: null })}
          onStatus={handleProfileStatus}
          onBlock={handleBlockProfile}
          onDelete={handleDeleteProfile}
          onBulkStatus={handleBulkProfileStatus}
          onBulkUpdate={handleBulkProfileUpdate}
          onBulkDelete={handleBulkDeleteProfiles}
        />
      );
    }
    if (['members-all', 'member-profile', 'member-signup', 'member-password', 'member-block', 'member-validity'].includes(activeTab)) {
      return <MembersPanel profiles={profiles} search={search} onOpen={(profile) => setDrawer({ type: 'member', entity: profile })} onCreate={() => setDrawer({ type: 'member', entity: null })} onStatus={handleProfileStatus} onBlock={handleBlockProfile} />;
    }
    if (['agents', 'agents-all', 'agent-ratings', 'agent-performance', 'agent-profiles'].includes(activeTab)) {
      return <AgentsPanel advisors={advisors} profiles={profiles} search={search} onOpen={(agent) => setDrawer({ type: 'agent', entity: agent })} onCreate={() => setDrawer({ type: 'agent', entity: null })} onStatus={handleAdvisorStatus} />;
    }
    if (['member-verify', 'agent-verification'].includes(activeTab)) {
      return <VerificationPanel verifications={verifications} profileDocuments={profileDocuments} advisors={advisors} profiles={profiles} onApprove={(id) => withNotice(approveVerification(id, 'Approved from admin console'), 'Verification approved.')} onReject={(id) => withNotice(rejectVerification(id, 'Rejected from admin console'), 'Verification rejected.')} onApproveDocument={(id) => withNotice(approveProfileDocument(id, 'Approved from admin console'), 'Document approved.')} onRejectDocument={(id) => withNotice(rejectProfileDocument(id, 'Please upload a clearer/correct document.'), 'Document re-requested.')} onAgentStatus={handleAdvisorStatus} onProfileStatus={handleProfileStatus} />;
    }
    if (activeTab === 'member-upgrades') {
      return <PendingUpgradesPanel payments={payments} type="member" activeTab={activeTab} onTab={navigateTab} />;
    }
    if (activeTab === 'member-payments') {
      return <RevenuePaymentsPanel payments={payments} type="member" activeTab={activeTab} onTab={navigateTab} />;
    }
    if (activeTab === 'member-invoices') {
      return <InvoicesPanel payments={payments} type="member" activeTab={activeTab} onTab={navigateTab} />;
    }
    if (['subscriptions', 'member-plans'].includes(activeTab)) {
      return <SubscriptionPanel config={config} payments={payments} type="member" activeTab={activeTab} onTab={navigateTab} onSave={handleConfigSave} />;
    }
    if (activeTab === 'agent-upgrades') {
      return <PendingUpgradesPanel payments={payments} type="agent" activeTab={activeTab} onTab={navigateTab} />;
    }
    if (activeTab === 'agent-payments') {
      return <RevenuePaymentsPanel payments={payments} type="agent" activeTab={activeTab} onTab={navigateTab} />;
    }
    if (activeTab === 'agent-invoices') {
      return <InvoicesPanel payments={payments} type="agent" activeTab={activeTab} onTab={navigateTab} />;
    }
    if (activeTab === 'agent-plans') {
      return <SubscriptionPanel config={config} payments={payments} type="agent" activeTab={activeTab} onTab={navigateTab} onSave={handleConfigSave} />;
    }
    if (['content', 'photo-moderation', 'chat-moderation', 'flagged-content', 'visitor-enquiry', 'lead-management', 'notifications'].includes(activeTab)) {
      return <ContentPanel inbox={moderationInbox} reports={reports} alerts={alerts} consentEvents={consentEvents} onResolve={(id) => withNotice(resolveReport(id), 'Report resolved.')} onAck={(id) => withNotice(acknowledgeAlert(id), 'Alert acknowledged.')} />;
    }
    if (activeTab === 'assist') {
      return (
        <AssistPanel
          advisors={advisors}
          assignments={assistAssignments}
          assistConfig={config.assisted_matchmaking || {}}
          onSaveConfig={(payload) => handleConfigSave('assisted_matchmaking', payload)}
          onCreateAdvisor={(payload) => withNotice(createAdvisor(payload), 'Agent advisor created.')}
          onUpdateAdvisor={(id, payload) => withNotice(updateAdvisor(id, payload), 'Agent advisor updated.')}
          onUpdateAdvisorStatus={handleAdvisorStatus}
          onUpdateAssignment={handleUpdateAssignment}
          canManageAdvisors
          canManageAssignments
        />
      );
    }
    if (activeTab === 'dynamic-config') {
      return <DynamicConfigPanel config={config} onSave={handleConfigSave} />;
    }
    if (activeTab === 'cms-management') {
      return <CmsManagementPanel config={config} onSave={handleConfigSave} />;
    }
    if (activeTab === 'analytics') {
      return <AnalyticsPanel stats={stats} funnel={funnel} events={events} payments={payments} />;
    }
    if (activeTab === 'growth-reports') {
      return <GrowthReportsPanel stats={stats} profiles={profiles} advisors={advisors} payments={payments} reports={reports} funnel={funnel} events={events} />;
    }
    if (['system', 'role-master', 'user-master', 'notifications', 'data-export', 'audit-logs', 'service-health', 'settings', 'change-password'].includes(activeTab)) {
      return <SystemPanel roles={roles} users={users} adminUsers={adminUsers} auditLogs={auditLogs} services={services} activeTab={activeTab} search={search} onSearch={setSearch} session={session} onSaveRoles={handleSaveRoles} onTab={navigateTab} funnel={funnel} events={events} systemInventory={systemInventory} onSaveAdminUser={handleSaveAdminUser} onDeleteAdminUser={handleDeleteAdminUser} />;
    }
    return <DashboardHome stats={stats} profiles={profiles} advisors={advisors} payments={payments} alerts={alerts} auditLogs={auditLogs} search={search} onTab={navigateTab} onMember={(profile) => setDrawer({ type: 'member', entity: profile })} onAgent={(agent) => setDrawer({ type: 'agent', entity: agent })} onCreateMember={() => setDrawer({ type: 'member', entity: null })} />;
  };

  const linkedProfiles = drawer?.type === 'agent'
    ? profiles.filter((profile) => profile.created_by_advisor_id === drawer.entity?.advisor_id)
    : [];

  return (
    <AdminShell activeTab={activeTab} onTab={navigateTab} session={session} search={search} onSearch={setSearch} onHelp={() => setDrawer({ type: 'help' })} menuGroups={MENU_GROUPS}>
      {notice ? <div className="toast-notice" role="status" aria-live="polite"><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Dismiss notification"><Icon name="close" /></button></div> : null}
      {renderContent()}
      {drawer?.type === 'member' ? (
        <MemberDrawer
          profile={drawer.entity}
          onClose={() => setDrawer(null)}
          onSave={handleSaveProfile}
          onStatus={handleProfileStatus}
          onPhotoAdd={handleAddProfilePhoto}
          onPhotoUpdate={handleUpdateProfilePhoto}
          onPhotoDelete={handleDeleteProfilePhoto}
        />
      ) : null}
      {drawer?.type === 'agent' ? (
        <AgentDrawer
          agent={drawer.entity}
          linkedProfiles={linkedProfiles}
          onClose={() => setDrawer(null)}
          onSave={handleSaveAdvisor}
          onStatus={handleAdvisorStatus}
        />
      ) : null}
      {drawer?.type === 'help' ? <HelpDrawer onClose={() => setDrawer(null)} /> : null}
    </AdminShell>
  );
}
