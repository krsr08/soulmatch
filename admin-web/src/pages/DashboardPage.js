import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { io } from 'socket.io-client';
import {
  ADMIN_SOCKET_URL,
  acknowledgeAlert,
  approveVerification,
  banUser,
  createAdvisor,
  createProfile,
  getAdvisors,
  getAlerts,
  getAnalyticsEvents,
  getAnalyticsFunnel,
  getAuditLogs,
  getConfig,
  getConsentEvents,
  getDashboard,
  getPayments,
  getProfiles,
  getRealtimeSnapshot,
  getReports,
  getRoles,
  getServiceHealth,
  getUsers,
  getVerifications,
  rejectVerification,
  resolveReport,
  unbanUser,
  updateAdvisor,
  updateAdvisorStatus,
  updateConfig,
  updateProfile,
  updateProfileStatus
} from '../api/adminApi';
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
    recentAudit: []
  }
};

const DEFAULT_CONFIG = {
  monetization: { currency: 'INR', plans: [], premiumLimits: {}, upgradePackageGroups: [] },
  assisted_matchmaking: { enabled: true, advisorPlans: [], memberModes: [] },
  admin_roles: { roles: [] },
  notification_templates: {},
  legal: {},
  content: {},
  theme: {}
};

const MENU_GROUPS = [
  {
    label: 'Control',
    items: [
      { id: 'dashboard', label: 'Overview', icon: 'grid' },
      { id: 'members', label: 'Members', icon: 'users' },
      { id: 'agents', label: 'Agents', icon: 'agent' },
      { id: 'subscriptions', label: 'Subscriptions', icon: 'tag' },
      { id: 'content', label: 'Content', icon: 'content' },
      { id: 'system', label: 'System', icon: 'gear' }
    ]
  },
  {
    label: 'Members Management',
    items: [
      { id: 'members-all', label: 'All Members', icon: 'users' },
      { id: 'member-signup', label: 'Member Signup', icon: 'plus' },
      { id: 'member-profile', label: 'Manage Profile', icon: 'edit' },
      { id: 'member-password', label: 'Password Details', icon: 'lock' },
      { id: 'member-block', label: 'Block / Unblock', icon: 'ban' },
      { id: 'member-verify', label: 'Verify Member', icon: 'check' },
      { id: 'member-validity', label: 'Validity Extension', icon: 'clock' }
    ]
  },
  {
    label: 'Agents Management',
    items: [
      { id: 'agents-all', label: 'All Agents', icon: 'agent' },
      { id: 'agent-verification', label: 'Agent Verification', icon: 'check' },
      { id: 'agent-ratings', label: 'Agent Ratings', icon: 'star' },
      { id: 'agent-performance', label: 'Agent Performance', icon: 'trend' },
      { id: 'agent-profiles', label: 'Agent Profiles Visibility', icon: 'eye' }
    ]
  },
  {
    label: 'Member Subscriptions',
    items: [
      { id: 'member-plans', label: 'Subscription Plans', icon: 'tag' },
      { id: 'member-upgrades', label: 'Pending Upgrades', icon: 'up' },
      { id: 'member-payments', label: 'Revenue & Payments', icon: 'rupee' },
      { id: 'member-invoices', label: 'Invoices', icon: 'invoice' }
    ]
  },
  {
    label: 'Agent Subscriptions',
    items: [
      { id: 'agent-plans', label: 'Subscription Plans', icon: 'tag' },
      { id: 'agent-upgrades', label: 'Pending Upgrades', icon: 'up' },
      { id: 'agent-payments', label: 'Revenue & Payments', icon: 'rupee' },
      { id: 'agent-invoices', label: 'Invoices', icon: 'invoice' }
    ]
  },
  {
    label: 'Enquiries & Leads',
    items: [
      { id: 'visitor-enquiry', label: 'Visitor Enquiry', icon: 'mail' },
      { id: 'lead-management', label: 'Lead Management', icon: 'target' }
    ]
  },
  {
    label: 'Content',
    items: [
      { id: 'photo-moderation', label: 'Photo Moderation', icon: 'image' },
      { id: 'chat-moderation', label: 'Chat Moderation', icon: 'chat' },
      { id: 'flagged-content', label: 'Flagged Content', icon: 'flag' }
    ]
  },
  {
    label: 'Dynamic Configuration',
    items: [{ id: 'dynamic-config', label: 'Dynamic Configuration', icon: 'sliders' }]
  },
  {
    label: 'System',
    items: [
      { id: 'role-master', label: 'Role Master', icon: 'crown' },
      { id: 'user-master', label: 'User Master', icon: 'person' },
      { id: 'notifications', label: 'Notifications', icon: 'bell' },
      { id: 'data-export', label: 'Data Export', icon: 'export' },
      { id: 'audit-logs', label: 'Audit Logs', icon: 'log' },
      { id: 'service-health', label: 'Service Health', icon: 'pulse' },
      { id: 'cms-management', label: 'CMS Management', icon: 'cms' },
      { id: 'settings', label: 'Settings', icon: 'gear' },
      { id: 'change-password', label: 'Change Password', icon: 'key' },
      { id: 'logout', label: 'Logout', icon: 'exit' }
    ]
  }
];

const MEMBER_PLAN_FALLBACK = [
  { id: 'free', name: 'Free', price: 0, durationDays: 30, contactViews: 0, visibleMatches: 10, profileBoost: false },
  { id: 'silver', name: 'Silver', price: 999, durationDays: 30, contactViews: 20, visibleMatches: 50, profileBoost: true },
  { id: 'gold', name: 'Gold', price: 2499, durationDays: 30, contactViews: 100, visibleMatches: -1, profileBoost: true },
  { id: 'platinum', name: 'Platinum', price: 4999, durationDays: 30, contactViews: -1, visibleMatches: -1, profileBoost: true }
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
  return Array.from(new Set(RBAC_MODULES.flatMap((module) => Object.values(module.permissions))));
}

function normalizeRoleForUi(role) {
  const key = role?.role || String(role?.label || 'custom_role').toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
  const copy = ROLE_COPY[key] || {};
  return {
    role: key,
    label: getRoleLabel({ ...role, role: key }),
    description: role?.description || copy.description || 'Custom admin role configured for the SoulMatch console.',
    scope: role?.scope || copy.scope || 'custom',
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

function toneForStatus(status) {
  const normalized = String(status || '').toLowerCase();
  if (['active', 'verified', 'approved', 'paid', 'success', 'captured'].includes(normalized)) return 'success';
  if (['pending', 'submitted', 'under_review', 'in_progress', 'created'].includes(normalized)) return 'warning';
  if (['rejected', 'suspended', 'blocked', 'failed', 'banned'].includes(normalized)) return 'danger';
  return 'neutral';
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

function Icon({ name }) {
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
    chat: <><path d="M21 11.5a8.4 8.4 0 0 1-9 8.3 9.3 9.3 0 0 1-4.4-1.1L3 20l1.4-4.1A8.3 8.3 0 1 1 21 11.5Z" {...common} /></>,
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

function StatusPill({ status, children }) {
  return <span className={`status-pill ${toneForStatus(status)}`}>{children || status || 'Unknown'}</span>;
}

function AdminButton({ variant = 'secondary', children, className = '', ...props }) {
  return (
    <button {...props} className={`admin-btn ${variant} ${className}`}>
      {children}
    </button>
  );
}

function AdminShell({ activeTab, onTab, session, search, onSearch, onHelp, children }) {
  const navigate = useNavigate();
  const roleSurfaceTabs = ['system', 'role-master', 'user-master', 'settings', 'change-password'];
  const isRoleSurface = roleSurfaceTabs.includes(activeTab);
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
          <h1>SoulMatch</h1>
          <span>Admin Portal</span>
        </div>
        <nav className="console-nav">
          {MENU_GROUPS.map((group) => (
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
              <button type="button" className="settings-link" onClick={() => handleTab('settings')}>Admin Settings</button>
              <div className="small-avatar">{(session.email || 'A').charAt(0).toUpperCase()}</div>
            </div>
          </header>
        ) : null}
        {children}
      </main>
    </div>
  );
}

function SectionHeader({ eyebrow, title, description, actions }) {
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

function StatCard({ tone, label, value, sub, link = 'View Details', onClick }) {
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
    .slice(0, 7);
  const leaderboard = admin.agentLeaderboard?.length
    ? admin.agentLeaderboard
    : advisors.slice(0, 5).map((agent) => ({ ...agent, members_added: agent.active_assignments || 0 }));
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

      <div className="workspace-columns">
        <section className="workspace-left">
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
              <h3>Revenue - Last 6 Months</h3>
              <StatusPill status="approved">Growth {stats.conversionRate || 0}%</StatusPill>
            </div>
            <SimpleLineChart rows={revenueTrend} />
            <div className="mini-stat-row as-tags">
              <span>Highest month <strong>{money(Math.max(...revenueTrend.map((row) => numberValue(row.revenue)), 0))}</strong></span>
              <span>Average <strong>{money(revenueTrend.reduce((sum, row) => sum + numberValue(row.revenue), 0) / Math.max(revenueTrend.length, 1))}</strong></span>
              <span>30d revenue <strong>{money(stats.revenue30d)}</strong></span>
            </div>
          </div>

          <MembersAddedChart profiles={profiles} />
        </section>

        <aside className="workspace-right">
          <div className="admin-card alerts-card">
            <h3>Alerts & Actions Required</h3>
            {[
              { tone: 'danger', count: numberValue(queues.member_kyc || stats.pendingApprovals), title: 'Member KYC pending', action: 'Verify now', tab: 'member-verify' },
              { tone: 'warning', count: numberValue(queues.agent_kyc), title: 'Agent approvals pending', action: 'Review agents', tab: 'agent-verification' },
              { tone: 'danger', count: numberValue(queues.photos), title: 'Photos flagged', action: 'Moderate', tab: 'photo-moderation' },
              { tone: 'warning', count: numberValue(queues.upgrades), title: 'Plan upgrades pending', action: 'Review', tab: 'member-upgrades' },
              { tone: 'info', count: numberValue(queues.alerts || alerts.length), title: 'Open platform alerts', action: 'View alerts', tab: 'notifications' }
            ].map((alert) => (
              <button key={alert.title} className={`alert-row ${alert.tone}`} onClick={() => onTab(alert.tab)}>
                <b>{alert.count}</b>
                <span>{alert.title}</span>
                <strong>{alert.action}</strong>
              </button>
            ))}
          </div>

          <div className="admin-card membership-card">
            <h3>Membership Breakdown</h3>
            <div className="triple-pie-grid">
              <PieChart
                title="Free / Paid Members"
                total={totalMembers}
                segments={[{ label: 'Paid', value: paidMembers }, { label: 'Free', value: freeMembers }]}
              />
              <PieChart
                title="Bride / Groom"
                total={numberValue(members.brides) + numberValue(members.grooms)}
                segments={[{ label: 'Bride', value: numberValue(members.brides) }, { label: 'Groom', value: numberValue(members.grooms) }]}
              />
              <PieChart
                title="Free / Paid Agents"
                total={freeAgentCount + paidAgentCount}
                segments={[{ label: 'Paid', value: paidAgentCount }, { label: 'Free', value: freeAgentCount }]}
              />
            </div>
          </div>

          <div className="admin-card">
            <div className="card-title-row">
              <h3>Top Agents this Month</h3>
              <button type="button" onClick={() => onTab('agents-all')}>View all</button>
            </div>
            <div className="leaderboard">
              {leaderboard.map((agent, index) => (
                <button key={agent.advisor_id || agent.full_name} onClick={() => onAgent(agent)}>
                  <b>{index + 1}</b>
                  <span>{agent.full_name}<small>{agent.city || '-'} | {agent.members_added || agent.active_assignments || 0} members</small></span>
                  <em>{Number(agent.average_rating || 0).toFixed(1)}</em>
                </button>
              ))}
            </div>
          </div>
        </aside>
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

function ProfileAvatar({ profile }) {
  const url = profile.primary_photo_url || profile.profile_photo_url;
  return url
    ? <img className="profile-avatar" src={url} alt="" />
    : <div className="profile-avatar fallback">{fullName(profile).charAt(0).toUpperCase()}</div>;
}

function ManagementToolbar({ title, subtitle, onCreate, createLabel, children }) {
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

function MembersPanel({ profiles, search, onOpen, onCreate, onStatus, onBlock }) {
  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return profiles;
    return profiles.filter((profile) => [
      fullName(profile),
      profile.phone,
      profile.email,
      profile.religion,
      profile.caste,
      profile.occupation,
      profile.working_city,
      profile.family_city,
      profile.advisor_name
    ].filter(Boolean).join(' ').toLowerCase().includes(q));
  }, [profiles, search]);

  return (
    <div className="admin-content">
      <ManagementToolbar
        title="Members Management"
        subtitle={`${rows.length} profiles · full 360-degree member control`}
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
            {rows.map((profile) => (
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
                    <button onClick={() => onStatus(profile, 'approve')} title="Verify"><Icon name="check" /></button>
                    <button onClick={() => onStatus(profile, profile.is_published ? 'suspend' : 'restore')} title="Visibility"><Icon name="sliders" /></button>
                    <button onClick={() => onBlock(profile)} title="Block / unblock"><Icon name="ban" /></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function AgentsPanel({ advisors, profiles, search, onOpen, onCreate, onStatus }) {
  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    const filtered = q
      ? advisors.filter((agent) => [agent.full_name, agent.phone, agent.email, agent.city, agent.state, agent.business_name, agent.agent_code].filter(Boolean).join(' ').toLowerCase().includes(q))
      : advisors;
    return filtered.map((agent) => ({
      ...agent,
      profilesAdded: profiles.filter((profile) => profile.created_by_advisor_id === agent.advisor_id).length
    }));
  }, [advisors, profiles, search]);

  return (
    <div className="admin-content">
      <ManagementToolbar
        title="Agent Management"
        subtitle={`${rows.length} agents · KYC, bank, T&C, plans and linked member profiles`}
        onCreate={onCreate}
        createLabel="Invite Agent"
      >
        <AdminButton variant="secondary"><Icon name="export" /> Export</AdminButton>
      </ManagementToolbar>

      <div className="agent-management-layout">
        <div>
          <div className="stat-grid four compact">
            <StatCard tone="terracotta" label="Total Agents" value={compactNumber(rows.length)} sub="Registered advisors" />
            <StatCard tone="peach" label="Pending Approval" value={compactNumber(rows.filter((a) => a.kyc_status === 'pending').length)} sub="KYC review required" />
            <StatCard tone="mauve" label="Members Added" value={compactNumber(rows.reduce((sum, agent) => sum + numberValue(agent.profilesAdded), 0))} sub="Agent-created profiles" />
            <StatCard tone="sage" label="Avg Rating" value={(rows.reduce((sum, agent) => sum + numberValue(agent.average_rating), 0) / Math.max(rows.length, 1)).toFixed(2)} sub="Live advisor quality" />
          </div>

          <div className="admin-card data-table tall">
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

function VerificationPanel({ verifications, advisors, profiles, onApprove, onReject, onAgentStatus, onProfileStatus }) {
  const pendingProfiles = profiles.filter((profile) =>
    ['pending', 'submitted', 'under_review'].includes(String(profile.verification_status || profile.review_status || '').toLowerCase())
  );
  const pendingAgents = advisors.filter((agent) => agent.kyc_status === 'pending' || ['pending', 'more_info'].includes(agent.onboarding_status));

  return (
    <div className="admin-content">
      <SectionHeader title="Verification Command Center" description="Review member KYC, agent onboarding, documents and profile visibility before they go live." />
      <div className="verification-grid">
        <div className="admin-card">
          <h3>Member Verification Queue</h3>
          <div className="review-list">
            {verifications.map((item) => (
              <article key={item.verification_id}>
                <span><strong>{fullName(item)}</strong><small>{item.type || 'Document'} · Trust {item.trust_score || 0}%</small></span>
                <div>
                  <AdminButton variant="secondary" onClick={() => onReject(item.verification_id)}>Reject</AdminButton>
                  <AdminButton variant="primary" onClick={() => onApprove(item.verification_id)}>Approve</AdminButton>
                </div>
              </article>
            ))}
            {!verifications.length ? <EmptyState title="No verification records" body="There are no member KYC records pending review." /> : null}
          </div>
        </div>
        <div className="admin-card">
          <h3>Agent Verification Queue</h3>
          <div className="review-list">
            {pendingAgents.map((agent) => (
              <article key={agent.advisor_id}>
                <span><strong>{agent.full_name}</strong><small>{agent.city} · Bank {agent.bank_verification_status || 'not started'} · T&C {agent.terms_accepted_at ? 'signed' : 'pending'}</small></span>
                <div>
                  <AdminButton variant="secondary" onClick={() => onAgentStatus(agent, { kycStatus: 'rejected' })}>Reject</AdminButton>
                  <AdminButton variant="primary" onClick={() => onAgentStatus(agent, { kycStatus: 'approved', status: 'active' })}>Approve</AdminButton>
                </div>
              </article>
            ))}
            {!pendingAgents.length ? <EmptyState title="No agent onboarding pending" body="All registered agents are currently reviewed." /> : null}
          </div>
        </div>
        <div className="admin-card full">
          <h3>Agent-created Member Profiles</h3>
          <div className="review-list">
            {pendingProfiles.filter((profile) => profile.created_by_advisor_id).map((profile) => (
              <article key={profile.profile_id}>
                <span><strong>{fullName(profile)}</strong><small>Added by {profile.advisor_name || 'agent'} · {profile.review_status || profile.verification_status}</small></span>
                <div>
                  <AdminButton variant="secondary" onClick={() => onProfileStatus(profile, 'reject')}>Reject</AdminButton>
                  <AdminButton variant="primary" onClick={() => onProfileStatus(profile, 'approve')}>Publish</AdminButton>
                </div>
              </article>
            ))}
            {!pendingProfiles.filter((profile) => profile.created_by_advisor_id).length ? <EmptyState title="No agent-created profiles pending" body="Published profiles will move into managed profiles after verification." /> : null}
          </div>
        </div>
      </div>
    </div>
  );
}

function SubscriptionPanel({ config, payments, type, onSave }) {
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

function ContentPanel({ reports, alerts, consentEvents, onResolve, onAck }) {
  return (
    <div className="admin-content">
      <SectionHeader title="Content & Moderation" description="Review profile reports, flagged content, DPDP consent activity and platform alerts." />
      <div className="workspace-columns even">
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
    .filter((user) => String(user.user_type || '').toLowerCase() === 'admin')
    .forEach((user) => {
      const email = user.email || user.phone || user.user_id;
      map.set(email, {
        id: user.user_id,
        name: fullName(user) === 'Unnamed' ? email : fullName(user),
        email,
        role: user.admin_role || 'admin',
        status: user.is_active === false || user.is_banned ? 'inactive' : 'active',
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
      permissions: ['dashboard:read']
    });
    if (!nextRole.role || draftRoles.some((role) => role.role === nextRole.role)) return;
    setDraftRoles((current) => [...current, nextRole]);
    setSelectedRoleId(nextRole.role);
    setForm({ role: '', label: '', description: '', scope: 'custom' });
    setShowCreate(false);
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
                  {RBAC_MODULES.map((module) => (
                    <tr key={module.key}>
                      <td>{module.label}</td>
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
              {auditLogs.filter((log) => /role|permission|config|user|password/i.test(`${log.action || ''} ${log.entity_type || ''}`)).slice(0, 4).map((log) => (
                <article key={log.audit_id || `${log.created_at}-${log.action}`}>
                  <time>{dateTime(log.created_at)}</time>
                  <span><strong>{log.admin_email || 'System'}</strong> {log.action} <em>{log.entity_type || 'record'}</em></span>
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

function SystemPanel({ roles, users, auditLogs, services, activeTab, search, onSearch, session, onSaveRoles, onTab }) {
  const deploymentLogs = auditLogs
    .filter((log) => /deploy|deployment|version|service health|release/i.test(`${log.action || ''} ${log.entity_type || ''}`))
    .slice(0, 20);
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

function DynamicConfigPanel({ config, onSave }) {
  const [selected, setSelected] = useState('monetization');
  const [json, setJson] = useState(JSON.stringify(config.monetization || {}, null, 2));
  const [error, setError] = useState('');
  const keys = ['monetization', 'assisted_matchmaking', 'admin_roles', 'notification_templates', 'legal', 'content', 'theme'];

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
    <div className="admin-content">
      <SectionHeader title="Dynamic Configuration" description="Runtime configuration without app redeploys." actions={<AdminButton variant="primary" onClick={save}>Save Section</AdminButton>} />
      <div className="config-layout">
        <div className="admin-card config-nav">
          {keys.map((key) => (
            <button key={key} className={selected === key ? 'active' : ''} onClick={() => setSelected(key)}>{key.replaceAll('_', ' ')}</button>
          ))}
        </div>
        <div className="admin-card">
          <h3>{selected.replaceAll('_', ' ')}</h3>
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

function EmptyState({ title, body }) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      <span>{body}</span>
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

function MemberDrawer({ profile, onClose, onSave, onStatus }) {
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
  const [activeTab, setActiveTab] = useState('dashboard');
  const [search, setSearch] = useState('');
  const [stats, setStats] = useState(EMPTY_STATS);
  const [profiles, setProfiles] = useState([]);
  const [advisors, setAdvisors] = useState([]);
  const [verifications, setVerifications] = useState([]);
  const [payments, setPayments] = useState({ transactions: [], plans: [], coupons: [] });
  const [alerts, setAlerts] = useState([]);
  const [reports, setReports] = useState([]);
  const [consentEvents, setConsentEvents] = useState([]);
  const [roles, setRoles] = useState([]);
  const [users, setUsers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [funnel, setFunnel] = useState([]);
  const [events, setEvents] = useState([]);
  const [services, setServices] = useState([]);
  const [config, setConfig] = useState(DEFAULT_CONFIG);
  const [drawer, setDrawer] = useState(null);
  const [notice, setNotice] = useState('');
  const session = useMemo(decodeSession, []);

  const reload = useCallback(async () => {
    const [
      dashboardRes,
      realtimeRes,
      profilesRes,
      advisorsRes,
      verificationsRes,
      paymentsRes,
      alertsRes,
      reportsRes,
      consentRes,
      rolesRes,
      usersRes,
      auditRes,
      funnelRes,
      eventsRes,
      serviceRes,
      configRes
    ] = await Promise.all([
      getDashboard().catch(() => ({ data: { data: EMPTY_STATS } })),
      getRealtimeSnapshot().catch(() => ({ data: { data: null } })),
      getProfiles({ page: 1, limit: 100, search: '' }).catch(() => ({ data: { data: [] } })),
      getAdvisors().catch(() => ({ data: { data: [] } })),
      getVerifications().catch(() => ({ data: { data: [] } })),
      getPayments().catch(() => ({ data: { data: { transactions: [], plans: [], coupons: [] } } })),
      getAlerts().catch(() => ({ data: { data: [] } })),
      getReports().catch(() => ({ data: { data: [] } })),
      getConsentEvents().catch(() => ({ data: { data: [] } })),
      getRoles().catch(() => ({ data: { data: [] } })),
      getUsers(1, '').catch(() => ({ data: { data: [] } })),
      getAuditLogs(200).catch(() => ({ data: { data: [] } })),
      getAnalyticsFunnel().catch(() => ({ data: { data: [] } })),
      getAnalyticsEvents(100).catch(() => ({ data: { data: [] } })),
      getServiceHealth().catch(() => ({ data: { data: [] } })),
      getConfig().catch(() => ({ data: { data: { config: DEFAULT_CONFIG } } }))
    ]);
    const baseStats = { ...EMPTY_STATS, ...(dashboardRes.data?.data || {}) };
    setStats({ ...baseStats, ...(realtimeRes.data?.data || {}) });
    setProfiles(profilesRes.data?.data || []);
    setAdvisors(advisorsRes.data?.data || []);
    setVerifications(verificationsRes.data?.data || []);
    setPayments(paymentsRes.data?.data || { transactions: [], plans: [], coupons: [] });
    setAlerts(alertsRes.data?.data || []);
    setReports(reportsRes.data?.data || []);
    setConsentEvents(consentRes.data?.data || []);
    setRoles(rolesRes.data?.data || []);
    setUsers(usersRes.data?.data || []);
    setAuditLogs(auditRes.data?.data || []);
    setFunnel(funnelRes.data?.data || []);
    setEvents(eventsRes.data?.data || []);
    setServices(serviceRes.data?.data || []);
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
    await withNotice(updateProfileStatus(profile.profile_id, action, `Admin ${action} from console`), `Member ${action} completed.`);
  };

  const handleBlockProfile = async (profile) => {
    if (!profile.user_id) return;
    const blocked = profile.is_banned === true;
    await withNotice(blocked ? unbanUser(profile.user_id) : banUser(profile.user_id), blocked ? 'Member unblocked.' : 'Member blocked.');
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

  const renderContent = () => {
    if (['dashboard', 'overview'].includes(activeTab)) {
      return <DashboardHome stats={stats} profiles={profiles} advisors={advisors} payments={payments} alerts={alerts} auditLogs={auditLogs} search={search} onTab={setActiveTab} onMember={(profile) => setDrawer({ type: 'member', entity: profile })} onAgent={(agent) => setDrawer({ type: 'agent', entity: agent })} onCreateMember={() => setDrawer({ type: 'member', entity: null })} />;
    }
    if (['members', 'members-all', 'member-profile', 'member-signup', 'member-password', 'member-block', 'member-validity'].includes(activeTab)) {
      return <MembersPanel profiles={profiles} search={search} onOpen={(profile) => setDrawer({ type: 'member', entity: profile })} onCreate={() => setDrawer({ type: 'member', entity: null })} onStatus={handleProfileStatus} onBlock={handleBlockProfile} />;
    }
    if (['agents', 'agents-all', 'agent-ratings', 'agent-performance', 'agent-profiles'].includes(activeTab)) {
      return <AgentsPanel advisors={advisors} profiles={profiles} search={search} onOpen={(agent) => setDrawer({ type: 'agent', entity: agent })} onCreate={() => setDrawer({ type: 'agent', entity: null })} onStatus={handleAdvisorStatus} />;
    }
    if (['member-verify', 'agent-verification'].includes(activeTab)) {
      return <VerificationPanel verifications={verifications} advisors={advisors} profiles={profiles} onApprove={(id) => withNotice(approveVerification(id, 'Approved from admin console'), 'Verification approved.')} onReject={(id) => withNotice(rejectVerification(id, 'Rejected from admin console'), 'Verification rejected.')} onAgentStatus={handleAdvisorStatus} onProfileStatus={handleProfileStatus} />;
    }
    if (['subscriptions', 'member-plans', 'member-upgrades', 'member-payments', 'member-invoices'].includes(activeTab)) {
      return <SubscriptionPanel config={config} payments={payments} type="member" onSave={handleConfigSave} />;
    }
    if (['agent-plans', 'agent-upgrades', 'agent-payments', 'agent-invoices'].includes(activeTab)) {
      return <SubscriptionPanel config={config} payments={payments} type="agent" onSave={handleConfigSave} />;
    }
    if (['content', 'photo-moderation', 'chat-moderation', 'flagged-content', 'visitor-enquiry', 'lead-management', 'notifications'].includes(activeTab)) {
      return <ContentPanel reports={reports} alerts={alerts} consentEvents={consentEvents} onResolve={(id) => withNotice(resolveReport(id), 'Report resolved.')} onAck={(id) => withNotice(acknowledgeAlert(id), 'Alert acknowledged.')} />;
    }
    if (activeTab === 'dynamic-config') {
      return <DynamicConfigPanel config={config} onSave={handleConfigSave} />;
    }
    if (['system', 'role-master', 'user-master', 'notifications', 'data-export', 'audit-logs', 'service-health', 'cms-management', 'settings', 'change-password'].includes(activeTab)) {
      return <SystemPanel roles={roles} users={users} auditLogs={auditLogs} services={services} activeTab={activeTab} search={search} onSearch={setSearch} session={session} onSaveRoles={handleSaveRoles} onTab={setActiveTab} funnel={funnel} events={events} />;
    }
    return <DashboardHome stats={stats} profiles={profiles} advisors={advisors} payments={payments} alerts={alerts} auditLogs={auditLogs} search={search} onTab={setActiveTab} onMember={(profile) => setDrawer({ type: 'member', entity: profile })} onAgent={(agent) => setDrawer({ type: 'agent', entity: agent })} onCreateMember={() => setDrawer({ type: 'member', entity: null })} />;
  };

  const linkedProfiles = drawer?.type === 'agent'
    ? profiles.filter((profile) => profile.created_by_advisor_id === drawer.entity?.advisor_id)
    : [];

  return (
    <AdminShell activeTab={activeTab} onTab={setActiveTab} session={session} search={search} onSearch={setSearch} onHelp={() => setDrawer({ type: 'help' })}>
      {notice ? <div className="toast-notice">{notice}<button onClick={() => setNotice('')}>Dismiss</button></div> : null}
      {renderContent()}
      {drawer?.type === 'member' ? (
        <MemberDrawer
          profile={drawer.entity}
          onClose={() => setDrawer(null)}
          onSave={handleSaveProfile}
          onStatus={handleProfileStatus}
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
