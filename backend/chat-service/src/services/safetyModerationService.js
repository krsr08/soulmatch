const logger = require('../utils/logger');

const DEFAULT_MODEL = 'claude-3-5-haiku-20241022';
const ANTHROPIC_VERSION = '2023-06-01';

function toBool(value, fallback = false) {
  if (value === undefined || value === null || value === '') return fallback;
  return ['1', 'true', 'yes', 'on'].includes(String(value).trim().toLowerCase());
}

function cleanText(value, maxLength = 2000) {
  return String(value || '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, maxLength);
}

function config() {
  return {
    aiEnabled: toBool(process.env.CHAT_MODERATION_AI_ENABLED || process.env.AI_ASSIST_ENABLED, false),
    blockHighRisk: toBool(process.env.CHAT_MODERATION_BLOCK_HIGH_RISK, true),
    provider: String(process.env.AI_PROVIDER || 'anthropic').trim().toLowerCase(),
    model: process.env.ANTHROPIC_MODEL || process.env.AI_MODEL || DEFAULT_MODEL,
    apiKey: process.env.ANTHROPIC_API_KEY || '',
    timeoutMs: Number(process.env.AI_TIMEOUT_MS || 7000)
  };
}

function deterministicFlags(content = '') {
  const text = cleanText(content).toLowerCase();
  const patterns = [
    {
      type: 'off_platform_contact',
      severity: 'medium',
      pattern: /\b(whatsapp|watsapp|mobile number|phone number|call me|telegram|instagram|insta|snapchat)\b/
    },
    {
      type: 'phone_number_shared',
      severity: 'medium',
      pattern: /(?:\+?91[\s-]?)?[6-9]\d{9}\b/
    },
    {
      type: 'financial_request',
      severity: 'high',
      pattern: /\b(send money|loan|bank transfer|account number|upi|paytm|gpay|phonepe|gift card|crypto|forex|investment)\b/
    },
    {
      type: 'coercion_or_threat',
      severity: 'high',
      pattern: /\b(blackmail|threat|force you|leak your|expose you|abuse|harass)\b/
    },
    {
      type: 'sexual_or_private_photo_request',
      severity: 'high',
      pattern: /\b(nude|naked|sex|sexy photo|private photo|intimate photo)\b/
    },
    {
      type: 'suspicious_link',
      severity: 'medium',
      pattern: /\b(https?:\/\/|bit\.ly|tinyurl|t\.me\/|wa\.me\/)\S+/i
    }
  ];
  return patterns
    .filter((item) => item.pattern.test(text))
    .map(({ type, severity }) => ({ type, severity, source: 'rules' }));
}

function extractText(payload) {
  const blocks = Array.isArray(payload?.content) ? payload.content : [];
  return blocks.map((block) => block?.text || '').filter(Boolean).join('\n').trim();
}

function parseJsonObject(text) {
  const raw = String(text || '').trim();
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (_) {
    const match = raw.match(/\{[\s\S]*\}/);
    if (!match) return null;
    try {
      return JSON.parse(match[0]);
    } catch (error) {
      logger.warn(`Chat moderation JSON parse failed: ${error.message}`);
      return null;
    }
  }
}

async function aiModerate(content) {
  const cfg = config();
  if (!cfg.aiEnabled || cfg.provider !== 'anthropic' || !cfg.apiKey || typeof fetch !== 'function') return null;
  try {
    const response = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'x-api-key': cfg.apiKey,
        'anthropic-version': ANTHROPIC_VERSION
      },
      body: JSON.stringify({
        model: cfg.model,
        max_tokens: 300,
        temperature: 0,
        system: 'You are a safety classifier for a matrimony chat app. Classify only. Return compact JSON only.',
        messages: [{
          role: 'user',
          content: JSON.stringify({
            message: cleanText(content),
            categoriesToDetect: [
              'financial request or scam',
              'harassment or threat',
              'sexual/private photo request',
              'off-platform contact pressure',
              'spam or suspicious link'
            ],
            outputSchema: {
              action: 'allow | review | block',
              flags: [{ type: 'string', severity: 'low | medium | high' }],
              reason: 'short string'
            }
          })
        }]
      }),
      signal: AbortSignal.timeout(cfg.timeoutMs)
    });
    if (!response.ok) {
      logger.warn(`Anthropic chat moderation failed with status ${response.status}`);
      return null;
    }
    const parsed = parseJsonObject(extractText(await response.json()));
    if (!parsed) return null;
    return {
      action: ['allow', 'review', 'block'].includes(parsed.action) ? parsed.action : 'review',
      flags: Array.isArray(parsed.flags)
        ? parsed.flags.map((flag) => ({
          type: cleanText(flag?.type, 80) || 'ai_moderation_flag',
          severity: ['low', 'medium', 'high'].includes(flag?.severity) ? flag.severity : 'medium',
          source: 'ai'
        }))
        : [],
      reason: cleanText(parsed.reason, 160),
      provider: 'anthropic',
      model: cfg.model
    };
  } catch (error) {
    logger.warn(`AI chat moderation unavailable: ${error.message}`);
    return null;
  }
}

function strongestSeverity(flags) {
  if (flags.some((flag) => flag.severity === 'high')) return 'high';
  if (flags.some((flag) => flag.severity === 'medium')) return 'medium';
  if (flags.some((flag) => flag.severity === 'low')) return 'low';
  return 'none';
}

exports.detectTextSafety = deterministicFlags;

exports.moderateMessage = async (content = '') => {
  const cfg = config();
  const ruleFlags = deterministicFlags(content);
  const ai = await aiModerate(content);
  const flags = ruleFlags.concat(ai?.flags || []);
  const severity = strongestSeverity(flags);
  const shouldBlock = (severity === 'high' && cfg.blockHighRisk) || ai?.action === 'block';
  return {
    action: shouldBlock ? 'block' : severity === 'none' && ai?.action !== 'review' ? 'allow' : 'review',
    severity,
    flags,
    reason: shouldBlock
      ? ai?.reason || 'This message may violate SoulMatch safety rules.'
      : ai?.reason || '',
    provider: ai?.provider || 'rules',
    model: ai?.model || 'local'
  };
};
