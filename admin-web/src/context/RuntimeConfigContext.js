import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { getPublicConfig } from '../api/adminApi';

const fallback = {
  branding: {
    appTitle: 'SoulMatch',
    logoUrl: '',
    squareLogoUrl: '',
    previewImageUrl: '',
    shareBaseUrl: ''
  },
  theme: {
    primary: '#D4285A',
    secondary: '#F5A623',
    accent: '#16324F',
    background: '#FFF8F4',
    surface: '#FFFFFF'
  },
  features: {
    chat: true,
    videoCalling: true,
    maintenanceMode: false
  },
  maintenance: {
    enabled: false,
    title: 'Scheduled maintenance',
    message: ''
  },
  content: {
    auth: {
      heroTitle: 'Find Your Perfect Soul Match',
      heroSubtitle: 'Where tradition meets modern connection.',
      trustChips: ['Verified profiles', 'Private photos', 'Family-ready'],
      registerCta: 'Register with mobile',
      googleCta: 'Continue with Google',
      loginCta: 'Log in to existing account',
      termsPrefix: 'By signing up, you agree to our'
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
      bestMatchInsertFrequency: 2,
      showBestMatchInsertCards: true,
      showBestMatchUpgradeCards: true,
      showBestMatchAdCards: true,
      bestMatchAdCards: [
        {
          id: 'wedding-services',
          type: 'marriage',
          title: 'Wedding services nearby',
          body: 'Shortlist decorators, photographers, and venues once both families are ready.',
          cta: 'View ideas',
          destination: 'search'
        },
        {
          id: 'family-horoscope',
          type: 'astrology',
          title: 'Family horoscope support',
          body: 'Add horoscope details and compare compatibility before the first family call.',
          cta: 'Open astrology',
          destination: 'astrology_services'
        },
        {
          id: 'verified-profiles',
          type: 'profiles',
          title: 'Verified profiles first',
          body: 'Focus on members with stronger trust signals and active intent.',
          cta: 'Browse profiles',
          destination: 'search'
        }
      ],
      emptyTitle: 'Your profile needs a little more detail',
      emptyBody: 'Complete career, family, lifestyle, and privacy sections to unlock stronger recommendations.',
      emptyCta: 'Improve my profile',
      searchPlaceholder: 'Search city, community, education, or profession',
      shortlistHint: 'shortlisted from current filters. Saved in Activity > Saved.',
      filterTitle: 'Filters',
      filterSubtitle: 'Default is Any so all suitable matches stay visible'
    },
    navigation: {
      home: 'Home',
      search: 'Search',
      activity: 'Activity',
      chat: 'Messenger',
      profile: 'Profile'
    },
    support: {
      title: 'Need help?',
      body: 'Contact SoulMatch support from settings for account, safety, payment, or privacy help.',
      email: 'support@soulmatch.app'
    },
    notificationPrompt: {
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
    },
    safetyCenter: {
      title: 'Safety Center',
      subtitle: 'Explore practical tools and guidance that help you stay safe while matching.',
      tiles: [],
      resources: [],
      articles: []
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
    plans: [],
    premiumLimits: {},
    upgradePackageGroups: [],
    membershipFeatureMatrix: []
  }
};

const RuntimeConfigContext = createContext({
  config: fallback,
  refresh: () => Promise.resolve()
});

export function RuntimeConfigProvider({ children }) {
  const [config, setConfig] = useState(fallback);

  const refresh = async () => {
    try {
      const response = await getPublicConfig();
      const next = response?.data?.data;
      if (next) setConfig({ ...fallback, ...next });
    } catch (_) {
      return;
    }
  };

  useEffect(() => {
    refresh();
    const id = window.setInterval(refresh, 45000);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    document.title = `${config.branding.appTitle} Control Plane`;
    document.documentElement.style.setProperty('--brand-primary', config.theme.primary);
    document.documentElement.style.setProperty('--brand-secondary', config.theme.secondary);
    document.documentElement.style.setProperty('--brand-accent', config.theme.accent);
    document.documentElement.style.setProperty('--brand-background', config.theme.background);
    document.documentElement.style.setProperty('--brand-surface', config.theme.surface);
  }, [config]);

  const value = useMemo(() => ({ config, refresh }), [config]);

  return <RuntimeConfigContext.Provider value={value}>{children}</RuntimeConfigContext.Provider>;
}

export function useRuntimeConfig() {
  return useContext(RuntimeConfigContext);
}
