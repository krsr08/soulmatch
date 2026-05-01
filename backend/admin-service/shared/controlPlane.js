const DEFAULT_CONFIG = {
  branding: {
    appTitle: 'SoulMatch',
    tagline: 'Serious matchmaking for modern families',
    logoUrl: '',
    squareLogoUrl: '',
    previewImageUrl: '',
    shareBaseUrl: 'https://app.soulmatch.app'
  },
  theme: {
    primary: '#D4285A',
    secondary: '#F5A623',
    accent: '#16324F',
    background: '#FFF8F4',
    surface: '#FFFFFF'
  },
  feature_flags: {
    chat: true,
    videoCalling: true,
    maintenanceMode: false,
    botFlow: true,
    profileBoosts: true,
    aiModeration: true
  },
  matching: {
    algorithmVersion: 'v1.0',
    weights: {
      age: 20,
      location: 15,
      religion: 15,
      caste: 12,
      education: 10,
      profession: 8,
      lifestyle: 10,
      activity: 10
    },
    indiaFilters: {
      caste: true,
      gotra: true,
      motherTongue: true,
      community: true
    },
    minimumCompatibilityForBestMatch: 70
  },
  registration: {
    dailyOtpLimitPerPhone: 5,
    maxProfilesPerDevice: 2,
    minimumAge: 18,
    requireKycForContactView: false,
    allowedCountries: ['IN']
  },
  security: {
    jwtExpiryHours: 8,
    apiRateLimitPerMinute: 120,
    piiMasking: true,
    auditRetentionDays: 365
  },
  localization: {
    defaultLanguage: 'en-IN',
    supportedLanguages: ['en-IN', 'hi-IN', 'ta-IN', 'te-IN', 'ml-IN', 'kn-IN', 'mr-IN', 'gu-IN', 'or-IN', 'pa-IN']
  },
  client_integrations: {
    googleWebClientId: '',
    razorpayKeyId: '',
    supportEmail: 'support@soulmatch.app'
  },
  payment_gateways: {
    razorpay: { enabled: true, label: 'Razorpay' },
    stripe: { enabled: false, label: 'Stripe' }
  },
  maintenance: {
    enabled: false,
    title: 'Scheduled maintenance',
    message: 'We are tuning SoulMatch for a smoother experience. Please check back shortly.',
    startsAt: null,
    endsAt: null
  },
  content: {
    auth: {
      heroTitle: 'Serious matchmaking for modern families',
      heroSubtitle: 'Serious matchmaking for modern families',
      trustChips: ['Verified profiles', 'Private photos', 'Family-ready'],
      registerCta: 'Register with mobile',
      googleCta: 'Continue with Google',
      loginCta: 'Log in to existing account',
      termsPrefix: 'By continuing, you agree to our'
    },
    phoneEntry: {
      topBarTitle: 'Mobile verification',
      title: 'Enter your mobile number',
      subtitle: 'We use this number for OTP login, account recovery, and important match alerts.',
      trustLines: ['No password needed', 'Private by default'],
      fieldLabel: '10 digit mobile number',
      helperText: 'Use your active Indian mobile number.',
      privacyTitle: 'Your number stays protected',
      privacyBody: 'Members see contact details only when your privacy and plan rules allow it.',
      submitCta: 'Send OTP'
    },
    home: {
      eyebrow: 'SoulMatch',
      headerSubtitle: 'Premium matrimonial matches ranked by family fit, intent, activity, and compatibility.',
      upgradeTitle: 'Unlock contact details and premium visibility',
      upgradeDetail: 'Upgrade to view verified phone numbers, get more profile reach, and continue high-intent conversations.',
      bestMatchesTitle: 'Best matches',
      bestMatchesSubtitle: 'High-signal cards with interest, shortlist, profile, and more actions',
      emptyTitle: 'Your profile needs a little more detail',
      emptyBody: 'Complete career, family, lifestyle, and privacy sections to unlock stronger recommendations.',
      emptyCta: 'Improve my profile',
      searchPlaceholder: 'Search city, community, education, or profession',
      shortlistHint: 'shortlisted from current filters. Saved in Activity > Saved.',
      filterTitle: 'Filters',
      filterSubtitle: 'Any age, any location, any community'
    },
    navigation: {
      home: 'Home',
      search: 'Search',
      activity: 'Activity',
      chat: 'Chat',
      profile: 'Profile'
    },
    support: {
      title: 'Need help?',
      body: 'Contact SoulMatch support from settings for account, safety, payment, or privacy help.',
      email: 'support@soulmatch.app'
    }
  },
  legal: {
    terms: {
      title: 'Terms of Service',
      subtitle: 'Simple rules for using SoulMatch safely and respectfully.',
      updatedAt: '29 Apr 2026',
      sections: [
        { heading: 'Who can use SoulMatch', body: 'SoulMatch is for lawful matrimonial discovery. You must be legally eligible to marry in India and use your own real identity, phone number, and profile details.' },
        { heading: 'Profile information', body: 'Add correct details about age, education, job, community, family, photos, and preferences. Do not create fake profiles, impersonate another person, or upload misleading photos.' },
        { heading: 'Respectful communication', body: 'Use chats, interests, calls, and profile actions respectfully. Harassment, abusive messages, spam, fraud, money requests, and pressure tactics are not allowed.' },
        { heading: 'Safety actions', body: 'You can hide, block, or report a member at any time. SoulMatch may review reports, restrict accounts, remove content, or contact users when safety checks are needed.' },
        { heading: 'Membership and payments', body: 'Paid plans unlock features such as contact views, visibility, and premium profile actions. Plan benefits, price, validity, taxes, and refund rules are shown before payment.' },
        { heading: 'Account responsibility', body: 'Keep your phone, OTP, and account access private. You are responsible for activity from your account unless you report unauthorized access promptly.' },
        { heading: 'Service changes', body: 'SoulMatch may improve, pause, or change features to keep the service reliable and safe. Important changes will be shown in the app or through official communication.' },
        { heading: 'Need help', body: 'For account, payment, privacy, or safety questions, contact SoulMatch support from the app settings page.' }
      ]
    },
    privacy: {
      title: 'Privacy Policy',
      subtitle: 'How SoulMatch uses your information to help you find suitable matches.',
      updatedAt: '29 Apr 2026',
      sections: [
        { heading: 'Information we collect', body: 'We collect details you add to your profile, such as name, age, gender, language, community, education, job, family details, photos, preferences, and verification information.' },
        { heading: 'How we use it', body: 'We use your information to create your profile, recommend matches, show compatibility signals, run search filters, verify members, prevent misuse, and provide support.' },
        { heading: 'Who can see your profile', body: 'Your visibility and photo privacy settings decide who can see profile details and photos. You can hide or block members when you do not want further interaction.' },
        { heading: 'Chats and safety', body: 'Messages, interests, reports, and safety actions may be processed to protect members, investigate complaints, and improve trust on the platform.' },
        { heading: 'Payments', body: 'Payment details are handled by authorized payment partners. SoulMatch stores payment status, plan details, invoice data, and transaction references needed for service and support.' },
        { heading: 'Sharing with partners', body: 'We share limited information with trusted service providers such as hosting, analytics, payment, notification, and support partners only for operating SoulMatch.' },
        { heading: 'Your choices', body: 'You can edit profile details, manage photos, change privacy settings, delete photos, hide members, block members, and ask for account support from the app.' },
        { heading: 'Data security', body: 'We use access controls, verification, and monitoring to protect member data. No online service is risk free, so always be careful before sharing personal or financial information.' }
      ]
    }
  },
  monetization: {
    currency: 'INR',
    premiumLimits: {
      dailySwipes: { free: 25, silver: 80, gold: 200, platinum: 500 },
      dailyInterests: { free: 5, silver: 20, gold: 999, platinum: 999 },
      videoCallsPerMonth: { free: 0, silver: 1, gold: 8, platinum: 30 }
    },
    plans: [
      { planId: 'free', name: 'Free', price: 0, duration: 'lifetime', durationDays: 0, features: ['5 interests/day', 'Basic search', 'View profiles'] },
      { planId: 'silver', name: 'Silver', price: 499, duration: 'monthly', durationDays: 30, features: ['20 interests/day', 'Advanced search', 'See viewers'] },
      { planId: 'gold', name: 'Gold', price: 999, duration: 'quarterly', durationDays: 90, features: ['Unlimited interests', 'Video calling', 'Priority search'] },
      { planId: 'platinum', name: 'Platinum', price: 1499, duration: 'yearly', durationDays: 365, features: ['All Gold features', 'Anonymous browsing', 'Unlimited boosts'] }
    ],
    upgradePackageGroups: []
  },
  notification_templates: {
    someone_liked_you: {
      title: 'Someone liked you',
      body: '{{name}} liked your profile. Open SoulMatch to respond.'
    },
    match_made: {
      title: "It's a match",
      body: '{{name}} accepted your interest. Say hello on SoulMatch.'
    },
    interest_declined: {
      title: 'Interest update',
      body: '{{name}} declined your interest. Keep exploring compatible matches.'
    },
    payment_success: {
      title: 'Membership activated',
      body: 'Your {{planName}} membership is now active.'
    }
  },
  seo_defaults: {
    baseUrl: 'https://app.soulmatch.app',
    defaultTitle: 'SoulMatch | Premium matchmaking for modern families',
    defaultDescription: 'Find meaningful matrimonial matches with verified profiles, premium discovery, and family-friendly trust signals.',
    defaultImageUrl: '',
    twitterHandle: '@soulmatch'
  },
  analytics: {
    enabled: true,
    retentionDays: 180,
    dashboardLookbackDays: 30
  }
};

const CONFIG_KEYS = Object.freeze(Object.keys(DEFAULT_CONFIG));

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value);
}

function deepMerge(base, override) {
  if (Array.isArray(base)) return Array.isArray(override) ? override : base;
  if (!isPlainObject(base)) return override === undefined ? base : override;
  const result = { ...base };
  const source = isPlainObject(override) ? override : {};
  for (const key of Object.keys(source)) {
    result[key] = key in base ? deepMerge(base[key], source[key]) : source[key];
  }
  return result;
}

async function getConfigMap(db, includePrivate = true) {
  const result = await db.query(
    'SELECT config_key, config_value, is_public, updated_at FROM app_config WHERE config_key = ANY($1::text[])',
    [CONFIG_KEYS]
  );
  const rowMap = new Map(result.rows.map((row) => [row.config_key, row]));
  const merged = {};
  for (const key of CONFIG_KEYS) {
    const row = rowMap.get(key);
    if (!includePrivate && row && !row.is_public) continue;
    const base = DEFAULT_CONFIG[key];
    merged[key] = deepMerge(base, row?.config_value || {});
  }
  return merged;
}

async function getConfigSection(db, key) {
  const result = await db.query('SELECT config_value FROM app_config WHERE config_key=$1 LIMIT 1', [key]);
  const current = result.rows[0]?.config_value || {};
  return deepMerge(DEFAULT_CONFIG[key] || {}, current);
}

async function upsertConfigSection(db, key, value, updatedBy = 'system', isPublicOverride = null) {
  const isPublic = isPublicOverride === null ? ['branding', 'theme', 'feature_flags', 'payment_gateways', 'maintenance', 'monetization', 'legal', 'content', 'matching', 'registration', 'localization', 'client_integrations'].includes(key) : isPublicOverride;
  const merged = deepMerge(DEFAULT_CONFIG[key] || {}, value);
  await db.query(
    `INSERT INTO app_config (config_key, config_value, is_public, updated_at, updated_by)
     VALUES ($1, $2::jsonb, $3, NOW(), $4)
     ON CONFLICT (config_key)
     DO UPDATE SET config_value = EXCLUDED.config_value, is_public = EXCLUDED.is_public, updated_at = NOW(), updated_by = EXCLUDED.updated_by`,
    [key, JSON.stringify(merged), isPublic, updatedBy]
  );
  return merged;
}

async function recordAnalyticsEvent(db, { eventType, serviceName, userId = null, sessionId = null, payload = {} }) {
  await db.query(
    `INSERT INTO analytics_events (event_type, service_name, user_id, session_id, payload)
     VALUES ($1, $2, $3, $4, $5::jsonb)`,
    [eventType, serviceName, userId, sessionId, JSON.stringify(payload || {})]
  );
}

function renderTemplate(template, variables = {}) {
  return String(template || '').replace(/\{\{\s*(\w+)\s*\}\}/g, (_, key) => {
    const value = variables[key];
    return value === undefined || value === null ? '' : String(value);
  });
}

function getPlanById(monetization, planId) {
  const directPlan = (monetization?.plans || []).find((plan) => String(plan.planId) === String(planId));
  if (directPlan) return directPlan;
  const upgradePackage = (monetization?.upgradePackageGroups || [])
    .flatMap((group) => group.packages || [])
    .find((pkg) => String(pkg.pkgId) === String(planId));
  if (!upgradePackage) return null;
  return {
    planId: String(upgradePackage.pkgId),
    name: upgradePackage.pkgName,
    price: upgradePackage.pkgRate || upgradePackage.pkgDiscountedRate || upgradePackage.pkgActualRate || 0,
    duration: upgradePackage.pkgDuration || '',
    durationDays: upgradePackage.pkgDurationDays || 30,
    features: upgradePackage.features || []
  };
}

function getPublicRuntimeConfig(configMap) {
  return {
    branding: configMap.branding,
    theme: configMap.theme,
    features: configMap.feature_flags,
    matching: configMap.matching,
    registration: configMap.registration,
    localization: configMap.localization,
    clientIntegrations: configMap.client_integrations,
    maintenance: configMap.maintenance,
    content: configMap.content,
    legal: configMap.legal,
    paymentGateways: configMap.payment_gateways,
    monetization: {
      currency: configMap.monetization.currency,
      premiumLimits: configMap.monetization.premiumLimits,
      upgradePackageGroups: configMap.monetization.upgradePackageGroups || [],
      plans: configMap.monetization.plans.map((plan) => ({
        planId: plan.planId,
        name: plan.name,
        price: plan.price,
        duration: plan.duration,
        durationDays: plan.durationDays,
        features: plan.features
      }))
    }
  };
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

module.exports = {
  CONFIG_KEYS,
  DEFAULT_CONFIG,
  deepMerge,
  escapeHtml,
  getConfigMap,
  getConfigSection,
  getPlanById,
  getPublicRuntimeConfig,
  recordAnalyticsEvent,
  renderTemplate,
  upsertConfigSection
};
