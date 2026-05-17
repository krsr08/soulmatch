const { getDB } = require('../config/database');
const crypto = require('crypto');
const { DEFAULT_CONFIG, escapeHtml, getConfigMap, getPublicRuntimeConfig, recordAnalyticsEvent } = require('../../../shared/controlPlane');
const logger = require('../utils/logger');

const ANALYTICS_RATE_BUCKETS = new Map();
const ANALYTICS_EVENT_TYPES = new Set([
  'page_view',
  'click',
  'sign_up',
  'account_type_selected',
  'payment_click',
  'payment_success',
  'match_made',
  'profile_view',
  'contact_unlock',
  'ai_bio_suggestions',
  'ai_icebreakers',
  'chat_report',
  'chat_message_safety',
  'assist_updated',
  'advisor_selected',
  'advisors_selected',
  'waiting_assignment',
  'family_assisted_enabled'
]);
const PUBLIC_ANALYTICS_LIMIT = Number(process.env.PUBLIC_ANALYTICS_RATE_LIMIT || 30);
const PUBLIC_ANALYTICS_WINDOW_MS = Number(process.env.PUBLIC_ANALYTICS_WINDOW_MS || 60 * 1000);
const PUBLIC_ANALYTICS_MAX_PAYLOAD_BYTES = Number(process.env.PUBLIC_ANALYTICS_MAX_PAYLOAD_BYTES || 4096);

async function loadSharedConfig() {
  const db = await getDB();
  const config = await getConfigMap(db, true);
  return { db, config };
}

function renderShell({ title, description, imageUrl, canonicalUrl, accentColor, bodyHtml, twitterHandle }) {
  return `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>${escapeHtml(title)}</title>
    <meta name="description" content="${escapeHtml(description)}" />
    <meta property="og:type" content="website" />
    <meta property="og:title" content="${escapeHtml(title)}" />
    <meta property="og:description" content="${escapeHtml(description)}" />
    <meta property="og:image" content="${escapeHtml(imageUrl)}" />
    <meta property="og:url" content="${escapeHtml(canonicalUrl)}" />
    <meta name="twitter:card" content="summary_large_image" />
    <meta name="twitter:site" content="${escapeHtml(twitterHandle || '@soulmatch')}" />
    <meta name="twitter:title" content="${escapeHtml(title)}" />
    <meta name="twitter:description" content="${escapeHtml(description)}" />
    <meta name="twitter:image" content="${escapeHtml(imageUrl)}" />
    <link rel="canonical" href="${escapeHtml(canonicalUrl)}" />
    <style>
      :root {
        color-scheme: light;
        --accent: ${escapeHtml(accentColor)};
        --text: #102033;
        --muted: #5b6574;
        --surface: #ffffff;
        --background: #fff8f4;
      }
      * { box-sizing: border-box; }
      body {
        margin: 0;
        font-family: Arial, Helvetica, sans-serif;
        background: radial-gradient(circle at top, rgba(212, 40, 90, 0.10), transparent 36%), var(--background);
        color: var(--text);
      }
      main {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 28px;
      }
      .card {
        max-width: 860px;
        width: 100%;
        background: var(--surface);
        border-radius: 28px;
        overflow: hidden;
        box-shadow: 0 24px 80px rgba(16, 32, 51, 0.14);
      }
      .hero {
        padding: 40px;
      }
      .eyebrow {
        display: inline-flex;
        align-items: center;
        gap: 10px;
        font-size: 12px;
        font-weight: 700;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        color: var(--accent);
      }
      h1 {
        font-size: clamp(32px, 5vw, 52px);
        line-height: 1.05;
        margin: 16px 0 12px;
      }
      p {
        margin: 0;
        font-size: 17px;
        line-height: 1.65;
        color: var(--muted);
      }
      .cta {
        display: inline-block;
        margin-top: 24px;
        background: var(--accent);
        color: white;
        padding: 14px 22px;
        border-radius: 999px;
        text-decoration: none;
        font-weight: 700;
      }
      .cover {
        width: 100%;
        height: 360px;
        object-fit: cover;
        display: block;
      }
      .facts {
        display: flex;
        gap: 12px;
        flex-wrap: wrap;
        margin-top: 18px;
      }
      .pill {
        background: rgba(16, 32, 51, 0.06);
        color: var(--text);
        border-radius: 999px;
        padding: 10px 14px;
        font-size: 14px;
        font-weight: 700;
      }
      @media (max-width: 640px) {
        .hero { padding: 26px; }
        .cover { height: 260px; }
      }
    </style>
  </head>
  <body>
    <main>${bodyHtml}</main>
  </body>
</html>`;
}

function sendJsonServerError(res, error, message) {
  logger.error(error.stack || error.message);
  return res.status(500).json({
    success: false,
    error: {
      code: 'INTERNAL_ERROR',
      message
    }
  });
}

function renderShareErrorPage({ appTitle, accentColor, title, message, linkUrl }) {
  return renderShell({
    title: `${title} | ${appTitle}`,
    description: message,
    imageUrl: '',
    canonicalUrl: linkUrl,
    accentColor,
    twitterHandle: '@soulmatch',
    bodyHtml: `<section class="card">
      <div class="hero">
        <div class="eyebrow">${escapeHtml(appTitle)}</div>
        <h1>${escapeHtml(title)}</h1>
        <p>${escapeHtml(message)}</p>
        <a class="cta" href="${escapeHtml(linkUrl)}">Open ${escapeHtml(appTitle)}</a>
      </div>
    </section>`
  });
}

function analyticsClientKey(req) {
  return String(req.headers['x-forwarded-for'] || req.ip || req.socket?.remoteAddress || 'unknown')
    .split(',')[0]
    .trim();
}

function isPublicAnalyticsRateLimited(req) {
  const key = analyticsClientKey(req);
  const now = Date.now();
  const bucket = (ANALYTICS_RATE_BUCKETS.get(key) || []).filter((stamp) => now - stamp < PUBLIC_ANALYTICS_WINDOW_MS);
  if (bucket.length >= PUBLIC_ANALYTICS_LIMIT) {
    ANALYTICS_RATE_BUCKETS.set(key, bucket);
    return true;
  }
  bucket.push(now);
  ANALYTICS_RATE_BUCKETS.set(key, bucket);
  return false;
}

function payloadSizeBytes(value) {
  return Buffer.byteLength(JSON.stringify(value || {}), 'utf8');
}

exports.getRuntimeConfig = async (req, res) => {
  try {
    const { config } = await loadSharedConfig();
    const body = JSON.stringify({ success: true, data: getPublicRuntimeConfig(config) });
    const etag = `"${crypto.createHash('sha256').update(body).digest('hex')}"`;
    res.set('Cache-Control', 'private, max-age=0, must-revalidate');
    res.set('ETag', etag);
    if (req.headers['if-none-match'] === etag) {
      return res.status(304).end();
    }
    res.type('application/json').send(body);
  } catch (error) {
    logger.error(error.stack || error.message);
    const body = JSON.stringify({ success: true, data: getPublicRuntimeConfig(DEFAULT_CONFIG) });
    const etag = `"${crypto.createHash('sha256').update(body).digest('hex')}"`;
    res.set('Cache-Control', 'private, max-age=0, must-revalidate');
    res.set('ETag', etag);
    if (req.headers['if-none-match'] === etag) {
      return res.status(304).end();
    }
    res.type('application/json').send(body);
  }
};

exports.trackAnalyticsEvent = async (req, res) => {
  try {
    if (isPublicAnalyticsRateLimited(req)) {
      return res.status(429).json({
        success: false,
        error: { code: 'RATE_LIMITED', message: 'Too many analytics events. Please try again later.' }
      });
    }
    const db = await getDB();
    const body = req.body || {};
    const eventType = String(body.eventType || body.event_type || '').trim().slice(0, 80);
    if (!eventType || !ANALYTICS_EVENT_TYPES.has(eventType)) {
      return res.status(400).json({
        success: false,
        error: { code: 'VALIDATION_ERROR', message: 'Unsupported analytics eventType.' }
      });
    }
    const payload = body.payload && typeof body.payload === 'object' && !Array.isArray(body.payload) ? body.payload : {};
    if (payloadSizeBytes(payload) > PUBLIC_ANALYTICS_MAX_PAYLOAD_BYTES) {
      return res.status(413).json({
        success: false,
        error: { code: 'PAYLOAD_TOO_LARGE', message: 'Analytics payload is too large.' }
      });
    }
    await recordAnalyticsEvent(db, {
      eventType,
      serviceName: String(body.serviceName || body.service_name || 'android-app').slice(0, 80),
      userId: null,
      sessionId: body.sessionId || body.session_id || null,
      payload: {
        ...payload,
        page: body.page || payload.page || null,
        target: body.target || payload.target || null,
        appVersion: body.appVersion || payload.appVersion || null
      }
    });
    res.json({ success: true, data: { accepted: true } });
  } catch (error) {
    sendJsonServerError(res, error, 'Unable to record analytics right now.');
  }
};

exports._test = {
  ANALYTICS_EVENT_TYPES,
  payloadSizeBytes
};

exports.getPublicProfile = async (req, res) => {
  try {
    const { db, config } = await loadSharedConfig();
    const result = await db.query(
      `SELECT
         p.profile_id,
         p.first_name,
         p.last_name,
         p.primary_photo_url,
         p.religion,
         p.mother_tongue,
         ec.occupation,
         ec.working_city,
         ld.about_me,
         EXTRACT(YEAR FROM AGE(p.dob))::int AS age
       FROM profiles p
       LEFT JOIN education_career ec ON ec.profile_id = p.profile_id
       LEFT JOIN lifestyle_details ld ON ld.profile_id = p.profile_id
       WHERE p.profile_id=$1 AND p.is_published=true
       LIMIT 1`,
      [req.params.profileId]
    );
    const profile = result.rows[0];
    if (!profile) return res.status(404).json({ success: false, error: { message: 'Profile not found' } });
    res.json({
      success: true,
      data: {
        profileId: profile.profile_id,
        title: `${profile.first_name || 'SoulMatch member'}${profile.last_name ? ` ${profile.last_name}` : ''}`,
        description: profile.about_me || config.seo_defaults.defaultDescription,
        imageUrl: profile.primary_photo_url || config.branding.previewImageUrl || config.seo_defaults.defaultImageUrl,
        age: profile.age,
        occupation: profile.occupation,
        workingCity: profile.working_city,
        religion: profile.religion,
        motherTongue: profile.mother_tongue
      }
    });
  } catch (error) {
    sendJsonServerError(res, error, 'Unable to load the public profile right now.');
  }
};

exports.getLandingPage = async (req, res) => {
  try {
    const { db } = await loadSharedConfig();
    const result = await db.query('SELECT * FROM landing_pages WHERE slug=$1 AND is_active=true LIMIT 1', [req.params.slug]);
    const page = result.rows[0];
    if (!page) return res.status(404).json({ success: false, error: { message: 'Landing page not found' } });
    res.json({ success: true, data: page });
  } catch (error) {
    sendJsonServerError(res, error, 'Unable to load the landing page right now.');
  }
};

exports.renderProfileSharePage = async (req, res) => {
  try {
    const { db, config } = await loadSharedConfig();
    const result = await db.query(
      `SELECT
         p.profile_id,
         p.first_name,
         p.last_name,
         p.primary_photo_url,
         p.religion,
         p.mother_tongue,
         ec.occupation,
         ec.working_city,
         ld.about_me,
         EXTRACT(YEAR FROM AGE(p.dob))::int AS age
       FROM profiles p
       LEFT JOIN education_career ec ON ec.profile_id = p.profile_id
       LEFT JOIN lifestyle_details ld ON ld.profile_id = p.profile_id
       WHERE p.profile_id=$1 AND p.is_published=true
       LIMIT 1`,
      [req.params.profileId]
    );
    const profile = result.rows[0];
    if (!profile) {
      return res.status(404).send(
        renderShareErrorPage({
          appTitle: config.branding.appTitle,
          accentColor: config.theme.primary,
          title: 'Profile unavailable',
          message: 'The shared profile could not be found or is no longer available.',
          linkUrl: config.branding.shareBaseUrl
        })
      );
    }

    const fullName = `${profile.first_name || 'SoulMatch member'}${profile.last_name ? ` ${profile.last_name}` : ''}`.trim();
    const title = `${fullName} on ${config.branding.appTitle}`;
    const description = profile.about_me || `${profile.age || 'Verified'} year old ${profile.occupation || 'member'} from ${profile.working_city || 'India'}.`;
    const imageUrl = profile.primary_photo_url || config.branding.previewImageUrl || config.seo_defaults.defaultImageUrl;
    const canonicalUrl = `${config.branding.shareBaseUrl.replace(/\/$/, '')}/share/profile/${profile.profile_id}`;
    const bodyHtml = `<section class="card">
      <img class="cover" src="${escapeHtml(imageUrl)}" alt="${escapeHtml(fullName)}" />
      <div class="hero">
        <div class="eyebrow">${escapeHtml(config.branding.appTitle)}</div>
        <h1>${escapeHtml(fullName)}</h1>
        <p>${escapeHtml(description)}</p>
        <div class="facts">
          ${profile.age ? `<span class="pill">${escapeHtml(profile.age)} years</span>` : ''}
          ${profile.occupation ? `<span class="pill">${escapeHtml(profile.occupation)}</span>` : ''}
          ${profile.working_city ? `<span class="pill">${escapeHtml(profile.working_city)}</span>` : ''}
          ${profile.religion ? `<span class="pill">${escapeHtml(profile.religion)}</span>` : ''}
        </div>
        <a class="cta" href="${escapeHtml(config.branding.shareBaseUrl)}">Continue in ${escapeHtml(config.branding.appTitle)}</a>
      </div>
    </section>`;

    res.send(
      renderShell({
        title,
        description,
        imageUrl,
        canonicalUrl,
        accentColor: config.theme.primary,
        bodyHtml,
        twitterHandle: config.seo_defaults.twitterHandle
      })
    );
  } catch (error) {
    logger.error(error.stack || error.message);
    res.status(500).send(
      renderShareErrorPage({
        appTitle: 'SoulMatch',
        accentColor: '#D4285A',
        title: 'Share unavailable',
        message: 'We could not load this shared page right now. Please try again shortly.',
        linkUrl: 'https://app.soulmatch.app'
      })
    );
  }
};

exports.renderLandingSharePage = async (req, res) => {
  try {
    const { db, config } = await loadSharedConfig();
    const result = await db.query('SELECT * FROM landing_pages WHERE slug=$1 AND is_active=true LIMIT 1', [req.params.slug]);
    const page = result.rows[0];
    if (!page) {
      return res.status(404).send(
        renderShareErrorPage({
          appTitle: config.branding.appTitle,
          accentColor: config.theme.primary,
          title: 'Landing page unavailable',
          message: 'The shared landing page could not be found or is no longer active.',
          linkUrl: config.branding.shareBaseUrl
        })
      );
    }

    const title = page.seo_title || `${page.title} | ${config.branding.appTitle}`;
    const description = page.seo_description || page.description;
    const imageUrl = page.preview_image_url || page.hero_image_url || config.branding.previewImageUrl || config.seo_defaults.defaultImageUrl;
    const canonicalUrl = `${config.branding.shareBaseUrl.replace(/\/$/, '')}/share/landing/${page.slug}`;
    const bodyHtml = `<section class="card">
      ${imageUrl ? `<img class="cover" src="${escapeHtml(imageUrl)}" alt="${escapeHtml(page.title)}" />` : ''}
      <div class="hero">
        <div class="eyebrow">${escapeHtml(config.branding.appTitle)}</div>
        <h1>${escapeHtml(page.title)}</h1>
        ${page.subtitle ? `<p style="font-weight:700;color:var(--text);margin-bottom:12px;">${escapeHtml(page.subtitle)}</p>` : ''}
        <p>${escapeHtml(page.description)}</p>
        <a class="cta" href="${escapeHtml(page.cta_url || config.branding.shareBaseUrl)}">${escapeHtml(page.cta_label || 'Open app')}</a>
      </div>
    </section>`;

    res.send(
      renderShell({
        title,
        description,
        imageUrl,
        canonicalUrl,
        accentColor: config.theme.primary,
        bodyHtml,
        twitterHandle: config.seo_defaults.twitterHandle
      })
    );
  } catch (error) {
    logger.error(error.stack || error.message);
    res.status(500).send(
      renderShareErrorPage({
        appTitle: 'SoulMatch',
        accentColor: '#D4285A',
        title: 'Share unavailable',
        message: 'We could not load this shared page right now. Please try again shortly.',
        linkUrl: 'https://app.soulmatch.app'
      })
    );
  }
};
