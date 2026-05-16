const DEFAULT_UPGRADE_PACKAGE_GROUPS = [
  {
    tabKey: 'one_month',
    tabTitle: '1 Month',
    bannerTitle: 'Start with verified discovery',
    bannerText: 'A focused one-month Silver plan for members who want contact access and chat.',
    assistiveContent: 'Useful when you already have a shortlist and want faster introductions.',
    packages: [
      {
        pkgId: 101,
        planId: 'silver',
        pkgName: 'Silver 1 Month',
        pkgActualRate: 299,
        pkgDiscountedRate: 299,
        pkgRate: 299,
        pkgDuration: '30 days',
        pkgDurationDays: 30,
        pkgPhoneCount: 15,
        pkgBenefit: 'Unlock 30 profile views, 15 contact unlocks, shortlist capacity, interests, and chat.',
        buyerChoice: true,
        badge: 'Silver',
        features: ['30 profile views', '15 contact unlocks', 'Chat enabled']
      }
    ]
  },
  {
    tabKey: 'gold',
    tabTitle: 'Gold',
    bannerTitle: 'Most families start here',
    bannerText: 'Gold adds Match Assistance, stronger limits, and monthly spotlight boosts.',
    assistiveContent: 'Best for families comparing matches seriously with guided support.',
    packages: [
      {
        pkgId: 201,
        planId: 'gold',
        pkgName: 'Gold 1 Month',
        pkgActualRate: 599,
        pkgDiscountedRate: 599,
        pkgRate: 599,
        pkgDuration: '30 days',
        pkgDurationDays: 30,
        pkgPhoneCount: 30,
        pkgBenefit: 'Adds 50 profile views, 30 contact unlocks, Match Assistance, and 2 spotlight boosts.',
        buyerChoice: true,
        badge: 'Recommended',
        features: ['Match Assistance', '30 contact unlocks', '2 spotlight boosts']
      }
    ]
  },
  {
    tabKey: 'platinum',
    tabTitle: 'Platinum',
    bannerTitle: 'High-touch assisted matching',
    bannerText: 'Platinum gives the highest monthly limits and stronger discovery reach.',
    assistiveContent: 'Best when the family wants guided support and maximum access.',
    packages: [
      {
        pkgId: 301,
        planId: 'platinum',
        pkgName: 'Platinum 1 Month',
        pkgActualRate: 999,
        pkgDiscountedRate: 999,
        pkgRate: 999,
        pkgDuration: '30 days',
        pkgDurationDays: 30,
        pkgPhoneCount: 80,
        pkgBenefit: 'Includes 80 profile views, 80 contact unlocks, Match Assistance, and 4 spotlight boosts.',
        buyerChoice: false,
        badge: 'Platinum',
        features: ['80 contact unlocks', 'Match Assistance', '4 spotlight boosts']
      }
    ]
  }
];

const DEFAULT_BEST_MATCH_AD_CARDS = [
  { id: 'upgrade-bronze-to-silver', type: 'upgrade', enabled: true, targetPlans: ['free', 'bronze'], minPlan: 'free', maxPlan: 'free', theme: 'rose', badge: 'Starter upgrade', title: 'Move from Bronze to Silver', body: 'Unlock more daily interests, better visibility, and contact access for serious conversations.', bullets: ['20 contact views', 'More visible matches', 'Profile visitor insights', 'Priority support'], cta: 'Upgrade to Silver', discountLabel: 'SILVER BENEFITS', destination: 'membership' },
  { id: 'upgrade-silver-to-gold', type: 'upgrade', enabled: true, targetPlans: ['free', 'bronze', 'silver'], minPlan: 'free', maxPlan: 'silver', theme: 'gold', badge: 'Recommended', title: 'The Gold Tier Experience', body: 'Get stronger reach, unlimited discovery, and richer match actions for your family shortlist.', bullets: ['Priority reach', 'More contact views', 'Profile boost tools', 'Advanced filters'], cta: 'Explore Gold', discountLabel: 'MOST CHOSEN', destination: 'membership' },
  { id: 'upgrade-gold-to-platinum', type: 'upgrade', enabled: true, targetPlans: ['free', 'bronze', 'silver', 'gold'], minPlan: 'free', maxPlan: 'gold', theme: 'dark', badge: 'Elite access', title: 'Platinum for families who want full access', body: 'Unlock unlimited contact discovery, premium visibility, and high-touch assisted benefits.', bullets: ['Unlimited contacts', 'Featured placement', 'Concierge support', 'Deep match insights'], cta: 'Go Platinum', discountLabel: 'PLATINUM', destination: 'membership' },
  { id: 'spotlight-day-pass', type: 'spotlight', enabled: true, targetPlans: ['free', 'bronze', 'silver', 'gold'], minPlan: 'free', maxPlan: 'gold', theme: 'sunrise', badge: 'Spotlight', title: 'Be the first profile others see for an entire day', body: 'Appear on top of recommendations and increase your chances of receiving more interests.', cta: 'Get Spotlight', destination: 'membership' },
  { id: 'contact-unlock', type: 'membership', enabled: true, targetPlans: ['free', 'bronze', 'silver'], minPlan: 'free', maxPlan: 'silver', theme: 'blue', badge: 'Contact access', title: 'Ready to speak with the right family?', body: 'Upgrade to unlock eligible contact views after privacy and trust checks.', bullets: ['Verified phone access', 'Privacy-first contact rules', 'Safer introductions'], cta: 'Unlock contacts', destination: 'membership' },
  { id: 'profile-boost', type: 'membership', enabled: true, targetPlans: ['free', 'bronze', 'silver'], minPlan: 'free', maxPlan: 'silver', theme: 'rose', badge: 'Boost', title: 'Get noticed by more compatible families', body: 'Boosted profiles receive higher placement in compatible recommendations.', bullets: ['Higher listing priority', 'More profile views', 'Better response chances'], cta: 'Boost my profile', destination: 'membership' },
  { id: 'horoscope-family-match', type: 'astrology', enabled: true, targetPlans: ['free', 'bronze', 'silver', 'gold', 'platinum'], theme: 'purple', badge: 'Horoscope', title: 'Add horoscope details for family compatibility', body: 'Help families compare birth details, rashi, nakshatra, and kundli expectations.', bullets: ['Kundli details improve family fit', 'Manglik and rashi checks stay clear', 'Useful before a family call'], cta: 'Open astrology', destination: 'astrology_services' },
  { id: 'verified-trust-profile', type: 'trust', enabled: true, targetPlans: ['free', 'bronze', 'silver', 'gold', 'platinum'], theme: 'green', badge: 'Trust profile', title: 'Verified profiles receive more confident responses', body: 'Complete phone, email, photo, document, education, income, and family trust checks.', bullets: ['Higher trust score', 'More confident family responses', 'Verification status stays visible'], cta: 'Improve trust', destination: 'my_profile' },
  { id: 'enable-notifications', type: 'notification', enabled: true, targetPlans: ['free', 'bronze', 'silver', 'gold', 'platinum'], theme: 'blue', badge: 'Alerts', title: 'Turn on match alerts', body: 'Get notified when a serious profile sends interest, accepts, or messages you.', bullets: ['New interest alerts', 'Acceptance reminders', 'Message notifications'], cta: 'Manage alerts', destination: 'settings' },
  { id: 'private-photo-control', type: 'privacy', enabled: true, targetPlans: ['free', 'bronze', 'silver', 'gold', 'platinum'], theme: 'ivory', badge: 'Privacy', title: 'Keep photos private until you are ready', body: 'Use request-based photo access so families can review visibility safely.', cta: 'Manage photos', destination: 'my_profile' },
  { id: 'assisted-discovery', type: 'assist', enabled: true, targetPlans: ['silver', 'gold', 'platinum'], minPlan: 'silver', theme: 'peach', badge: 'SoulMatch Assist', title: 'Need offline help from a local agent?', body: 'Share your profile with selected registered agents for offline introductions.', cta: 'Open Assist', destination: 'soulmatch_assist' },
  { id: 'wedding-readiness', type: 'marriage', enabled: true, targetPlans: ['free', 'bronze', 'silver', 'gold', 'platinum'], theme: 'maroon', badge: 'Family planning', title: 'Shortlist services after both families connect', body: 'Keep venues, photography, and ceremony planning separate from discovery until you are ready.', cta: 'View ideas', destination: 'search' },
  { id: 'success-stories', type: 'story', enabled: true, targetPlans: ['free', 'bronze', 'silver', 'gold', 'platinum'], theme: 'cream', badge: 'Success stories', title: 'See how families used SoulMatch safely', body: 'Browse real journey patterns and learn what details create better responses.', cta: 'View stories', destination: 'success_stories' }
];

const DEFAULT_SCAM_AWARENESS_CARDS = [
  { id: 'gift-cod', enabled: true, title: 'Never make payments for unsolicited gifts', body: 'Scammers may send gifts by cash-on-delivery and pressure you to pay.' },
  { id: 'import-duty', enabled: true, title: 'Do not pay import duty or custom fees', body: 'Fraudsters may pose as officials and demand fees for gifts or parcels.' },
  { id: 'video-call', enabled: true, title: 'Be cautious during video calls', body: 'Avoid explicit calls and report anyone who blackmails or asks for money.' },
  { id: 'emergency-cash', enabled: true, title: 'Validate emergency cash requests', body: 'Never transfer money because of sudden medical, travel, or family emergencies.' },
  { id: 'advance-fee', enabled: true, title: 'Agents must not demand advance fees', body: 'Use SoulMatch-listed agents carefully and report anyone asking for unofficial payments.' },
  { id: 'bank-transfer', enabled: true, title: 'Avoid direct bank transfers to new contacts', body: 'Do not send money for visas, tickets, gifts, loans, medical stories, or emergencies.' }
];

const DEFAULT_NOTIFICATION_PROMPT_CONTENT = {
  enabled: true,
  title: "Don't miss important match updates",
  subtitle: 'Turn on alerts for interests, acceptances, and family messages.',
  bullets: [
    'Get alerts when a new interest or recommendation arrives',
    'Know when a family accepts your interest',
    'Be notified when a profile sends you a message'
  ],
  allowCta: 'Allow notifications',
  laterCta: 'Maybe later'
};

const DEFAULT_SAFETY_CENTER_CONTENT = {
  title: 'Safety Center',
  subtitle: 'Explore practical tools and guidance that help you stay safe while matching.',
  tiles: [
    { id: 'online_personal_tips', title: 'Online / Personal Tips', subtitle: 'Safe habits before sharing personal details.', icon: 'tips', tone: 'gold', destination: 'article:online_personal_tips' },
    { id: 'privacy_settings', title: 'Privacy Settings', subtitle: 'Control photos, alerts, and visibility.', icon: 'shield', tone: 'green', destination: 'article:privacy_settings' },
    { id: 'report_block', title: 'Report / Block Profile', subtitle: 'Act quickly on suspicious behavior.', icon: 'report', tone: 'rose', destination: 'article:report_block' },
    { id: 'mental_wellbeing', title: 'Mental Wellbeing', subtitle: 'Move at a pace that feels right.', icon: 'heart', tone: 'purple', destination: 'article:mental_wellbeing' }
  ],
  verificationCard: {
    title: 'Help us make SoulMatch safe and authentic',
    body: 'Verified profiles help families move forward with more confidence. Complete your trust details when you are ready.',
    cta: 'Verify yourself',
    destination: 'my_profile'
  },
  resourcesTitle: "We're here for you",
  resources: [
    { id: 'cyber_crime', title: 'Other resources', subtitle: 'Cyber cell contacts to help you take action.', icon: 'help', destination: 'article:cyber_crime' }
  ],
  articles: [
    {
      id: 'online_personal_tips',
      title: 'Online / Personal Tips',
      subtitle: 'Use simple checks before sharing contact details or meeting offline.',
      bullets: [
        'Keep early conversations inside SoulMatch until both families are comfortable.',
        'Do not send money, gifts, documents, or OTPs to a new contact.',
        'Speak with family members before moving to private calls or meetings.'
      ]
    },
    {
      id: 'privacy_settings',
      title: 'Privacy Settings',
      subtitle: 'Choose how much of your profile is visible while you are still deciding.',
      bullets: [
        'Use private photos when you want matches to request access first.',
        'Pause push notifications when you do not want alerts on this device.',
        'Hide or block profiles when you do not want further interaction.'
      ],
      primaryCta: 'Open privacy settings',
      destination: 'settings'
    },
    {
      id: 'report_block',
      title: 'Report / Block Profile',
      subtitle: 'Report suspicious requests, abusive messages, fake profiles, or money demands.',
      bullets: [
        'Block a member immediately when the interaction feels unsafe.',
        'Report profiles that ask for payments, gifts, loans, visas, or emergency help.',
        'SoulMatch reviews safety reports and may restrict accounts.'
      ]
    },
    {
      id: 'mental_wellbeing',
      title: 'Mental Wellbeing',
      subtitle: 'Matchmaking can be emotional. Keep the process steady and family-supported.',
      bullets: [
        'Take breaks from discovery when conversations feel overwhelming.',
        'Avoid pressure to decide quickly or share details before you are ready.',
        'Use trusted family support for important decisions.'
      ]
    },
    {
      id: 'cyber_crime',
      title: 'Take action against cyber crime',
      subtitle: 'Use official channels to report illegal online activity.',
      body: 'If a profile threatens, blackmails, impersonates, or asks for money, preserve screenshots and report through official cyber crime channels.',
      contacts: [
        { label: 'National cyber crime helpline', value: '1930', type: 'phone' },
        { label: 'Cyber crime website', value: 'https://cybercrime.gov.in/', type: 'url' }
      ]
    }
  ]
};

const DEFAULT_MEMBERSHIP_FEATURE_MATRIX = [
  { featureKey: 'visible_matches', label: 'Visible Matches', description: 'Number of recommended profiles visible in discovery.', bronze: 'Access up to 80', silver: 'Access up to 80', gold: 'Access up to 80', platinum: 'Access up to 80' },
  { featureKey: 'profile_views', label: 'Profile Views', description: 'Detailed profile views per 30-day cycle.', bronze: '10', silver: '30', gold: '50', platinum: '80' },
  { featureKey: 'contact_details', label: 'Contact Details', description: 'Eligible verified contact unlocks per 30-day cycle.', bronze: 'Not available', silver: '15', gold: '30', platinum: '80' },
  { featureKey: 'engage_plus', label: 'Engage+', description: 'Engagement and intent insights.', bronze: false, silver: true, gold: true, platinum: true },
  { featureKey: 'daily_interests', label: 'Interests', description: 'Interest requests a member can send every 30-day cycle.', bronze: '5', silver: '20', gold: '40', platinum: '80' },
  { featureKey: 'advanced_filters', label: 'Advanced Filters', description: 'Filter by work, family, activity, horoscope, trust and privacy attributes.', bronze: false, silver: true, gold: true, platinum: true },
  { featureKey: 'chat_after_match', label: 'Chat After Match', description: 'Message after mutual interest acceptance.', bronze: false, silver: true, gold: true, platinum: true },
  { featureKey: 'photo_request', label: 'Private Photo Requests', description: 'Request access to private photos.', bronze: true, silver: true, gold: true, platinum: true },
  { featureKey: 'spotlight', label: 'Spotlight Boosts', description: 'Profile appears higher in compatible recommendations.', bronze: false, silver: false, gold: '2 / month', platinum: '4 / month' },
  { featureKey: 'anonymous_browsing', label: 'Anonymous Browsing', description: 'Browse profiles without showing visitor identity.', bronze: false, silver: false, gold: true, platinum: true },
  { featureKey: 'soulmatch_assist', label: 'SoulMatch Assist', description: 'Share profile with selected registered agents for offline support.', bronze: false, silver: false, gold: true, platinum: true },
  { featureKey: 'priority_listing', label: 'Priority Listing', description: 'Higher visibility in best-match and recently active feeds.', bronze: false, silver: false, gold: true, platinum: true },
  { featureKey: 'concierge_support', label: 'Concierge Support', description: 'Dedicated help for high-intent families.', bronze: false, silver: false, gold: false, platinum: true },
  { featureKey: 'trust_badge_boost', label: 'Trust Badge Boost', description: 'Trust-complete profiles receive visual priority and better response prompts.', bronze: true, silver: true, gold: true, platinum: true }
];

const DEFAULT_MEMBER_PLANS = [
  { planId: 'free', name: 'Bronze', displayName: 'Bronze (Free)', price: 0, duration: 'lifetime', durationDays: 0, tierRank: 0, features: ['Access up to 80 matches', '10 profile views', '5 shortlists', '5 interests'] },
  { planId: 'silver', name: 'Silver', displayName: 'Silver', price: 299, duration: 'monthly', durationDays: 30, tierRank: 1, features: ['Access up to 80 matches', '30 profile views', '15 contact unlocks', 'Chat enabled'] },
  { planId: 'gold', name: 'Gold', displayName: 'Gold', price: 599, duration: 'monthly', durationDays: 30, tierRank: 2, features: ['Access up to 80 matches', '50 profile views', '30 contact unlocks', 'Match assistance', '2 spotlight boosts'] },
  { planId: 'platinum', name: 'Platinum', displayName: 'Platinum', price: 999, duration: 'monthly', durationDays: 30, tierRank: 3, features: ['Access up to 80 matches', '80 profile views', '80 contact unlocks', 'Match assistance', '4 spotlight boosts'] }
];

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
  admin_roles: {
    roles: [
      {
        role: 'super_admin',
        label: 'Super Admin',
        description: 'Full access to every admin console module, finance control, configuration and audit data.',
        scope: 'unrestricted',
        permissions: ['*']
      },
      {
        role: 'admin',
        label: 'Admin',
        description: 'Operational management of members, agents, payments, moderation and configurations.',
        scope: 'partial',
        permissions: ['dashboard:read', 'profiles:write', 'verification:write', 'payments:write', 'config:write', 'cms:write', 'moderation:write', 'analytics:read']
      },
      {
        role: 'moderator',
        label: 'Moderator',
        description: 'Content validation, verification review, profile approval and dispute handling.',
        scope: 'compliance',
        permissions: ['dashboard:read', 'profiles:read', 'verification:write', 'moderation:write', 'analytics:read']
      },
      {
        role: 'support_agent',
        label: 'Support Agent',
        description: 'Member support access with limited profile visibility and moderation read access.',
        scope: 'support',
        permissions: ['dashboard:read', 'profiles:read', 'profiles:support', 'moderation:read']
      },
      {
        role: 'marketing_manager',
        label: 'Marketing Manager',
        description: 'CMS, campaigns, referrals, analytics and audience engagement control.',
        scope: 'marketing',
        permissions: ['dashboard:read', 'cms:write', 'campaigns:write', 'analytics:read']
      }
    ]
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
    googleWebClientId: process.env.GOOGLE_WEB_CLIENT_ID || process.env.GOOGLE_CLIENT_ID || '',
    razorpayKeyId: process.env.RAZORPAY_KEY_ID || '',
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
      bestMatchMinimumProfiles: 5,
      bestMatchHighCompatibilityThreshold: 80,
      bestMatchInsertFrequency: 2,
      showBestMatchInsertCards: true,
      showBestMatchUpgradeCards: true,
      showBestMatchAdCards: true,
      bestMatchAdCards: DEFAULT_BEST_MATCH_AD_CARDS,
      scamAwarenessCards: DEFAULT_SCAM_AWARENESS_CARDS,
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
      chat: 'Messenger',
      profile: 'Profile',
      upgrade: 'Upgrade'
    },
    support: {
      title: 'Need help?',
      body: 'Contact SoulMatch support from settings for account, safety, payment, or privacy help.',
      email: 'support@soulmatch.app'
    },
    notificationPrompt: DEFAULT_NOTIFICATION_PROMPT_CONTENT,
    safetyCenter: DEFAULT_SAFETY_CENTER_CONTENT
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
      updatedAt: '10 May 2026',
      sections: [
        { heading: 'Information we collect', body: 'We collect details you add to your profile, such as name, age, gender, language, community, education, job, family details, photos, preferences, and verification information.' },
        { heading: 'How we use it', body: 'We use your information to create your profile, recommend matches, show compatibility signals, run search filters, verify members, prevent misuse, provide support, and offer optional profile-writing and ice-breaker suggestions.' },
        { heading: 'Who can see your profile', body: 'Your visibility and photo privacy settings decide who can see profile details and photos. You can hide or block members when you do not want further interaction.' },
        { heading: 'Photos and KYC documents', body: 'Photos and identity, education, income, divorce, horoscope, or agent KYC documents are used only for profile display, trust checks, verification review, safety moderation, and support. Document access is limited to authorized review roles.' },
        { heading: 'Agent sharing and SoulMatch Assistance', body: 'SoulMatch Assistance is optional. If you enable it and select agents, your relevant profile and family contact details may be shared with those selected agents for direct offline support. You can turn this off or change selected agents from your profile.' },
        { heading: 'Chats and safety', body: 'Messages, interests, reports, and safety actions may be processed through rule-based and optional AI-assisted checks to protect members, investigate complaints, block risky requests, and improve trust on the platform.' },
        { heading: 'Payments', body: 'Payment details are handled by authorized payment partners. SoulMatch stores payment status, plan details, invoice data, and transaction references needed for service and support.' },
        { heading: 'Sharing with partners', body: 'We share limited information with trusted service providers such as hosting, analytics, payment, notification, support, and configured AI providers only for operating SoulMatch features and safety checks.' },
        { heading: 'Your choices', body: 'You can edit profile details, manage photos, change privacy settings, delete photos, hide members, block members, update SoulMatch Assistance choices, withdraw optional agent sharing, and ask for account support from the app.' },
        { heading: 'Data security', body: 'We use access controls, verification, monitoring, audit logs, and consent records to protect member data. No online service is risk free, so always be careful before sharing personal or financial information.' }
      ]
    }
  },
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
    premiumLimits: {
      dailySwipes: { free: 25, silver: 80, gold: 200, platinum: 500 },
      dailyInterests: { free: 5, silver: 20, gold: 999, platinum: 999 },
      videoCallsPerMonth: { free: 0, silver: 1, gold: 8, platinum: 30 }
    },
    plans: DEFAULT_MEMBER_PLANS,
    membershipFeatureMatrix: DEFAULT_MEMBERSHIP_FEATURE_MATRIX,
    upgradePackageGroups: DEFAULT_UPGRADE_PACKAGE_GROUPS
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
  if (
    String(planId) === String(monetization?.fixedPricePlanId || 'fixed_access') &&
    String(monetization?.accessMode || '').toLowerCase() === 'fixed_price' &&
    monetization?.subscriptionModelEnabled === false
  ) {
    return {
      planId: String(monetization.fixedPricePlanId || 'fixed_access'),
      name: 'SoulMatch Fixed Access',
      price: Number(monetization.fixedPriceAmount || 200),
      duration: 'fixed access',
      durationDays: 30,
      features: ['Full member access at the configured fixed price']
    };
  }
  const directPlan = (monetization?.plans || []).find((plan) => String(plan.planId) === String(planId));
  if (directPlan) return directPlan;
  const upgradePackage = (monetization?.upgradePackageGroups || [])
    .flatMap((group) => group.packages || [])
    .find((pkg) => String(pkg.planId || '') === String(planId) || String(pkg.pkgId) === String(planId));
  if (!upgradePackage) return null;
  return {
    planId: String(upgradePackage.planId || upgradePackage.pkgId),
    name: upgradePackage.pkgName,
    price: upgradePackage.pkgRate || upgradePackage.pkgDiscountedRate || upgradePackage.pkgActualRate || 0,
    duration: upgradePackage.pkgDuration || '',
    durationDays: upgradePackage.pkgDurationDays || 30,
    features: upgradePackage.features || []
  };
}

function getPublicRuntimeConfig(configMap) {
  const clientIntegrations = {
    ...configMap.client_integrations,
    googleWebClientId: configMap.client_integrations?.googleWebClientId || process.env.GOOGLE_WEB_CLIENT_ID || process.env.GOOGLE_CLIENT_ID || '',
    razorpayKeyId: configMap.client_integrations?.razorpayKeyId || process.env.RAZORPAY_KEY_ID || ''
  };
  return {
    branding: configMap.branding,
    theme: configMap.theme,
    features: configMap.feature_flags,
    matching: configMap.matching,
    registration: configMap.registration,
    localization: configMap.localization,
    clientIntegrations,
    maintenance: configMap.maintenance,
    content: configMap.content,
    legal: configMap.legal,
    paymentGateways: configMap.payment_gateways,
    monetization: {
      currency: configMap.monetization.currency,
      accessMode: configMap.monetization.accessMode,
      subscriptionModelEnabled: configMap.monetization.subscriptionModelEnabled,
      fixedPriceAmount: configMap.monetization.fixedPriceAmount,
      fixedPricePlanId: configMap.monetization.fixedPricePlanId,
      fixedPriceLabel: configMap.monetization.fixedPriceLabel,
      freeAccessLabel: configMap.monetization.freeAccessLabel,
      refundGuaranteeEnabled: configMap.monetization.refundGuaranteeEnabled,
      refundGuaranteeTitle: configMap.monetization.refundGuaranteeTitle,
      refundGuaranteeSubtitle: configMap.monetization.refundGuaranteeSubtitle,
      premiumLimits: configMap.monetization.premiumLimits,
      memberPlanEntitlements: configMap.monetization.memberPlanEntitlements || {},
      membershipFeatureMatrix: configMap.monetization.membershipFeatureMatrix || [],
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
