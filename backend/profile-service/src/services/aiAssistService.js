const logger = require('../utils/logger');

const DEFAULT_MODEL = 'claude-3-5-haiku-20241022';
const ANTHROPIC_VERSION = '2023-06-01';

function toBool(value, fallback = false) {
  if (value === undefined || value === null || value === '') return fallback;
  return ['1', 'true', 'yes', 'on'].includes(String(value).trim().toLowerCase());
}

function cleanText(value, maxLength = 240) {
  return String(value || '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, maxLength);
}

function label(value, fallback = 'Not specified') {
  return cleanText(value, 80) || fallback;
}

function compactProfile(profile = {}) {
  return {
    name: cleanText([profile.first_name, profile.last_name].filter(Boolean).join(' '), 80) || 'Member',
    age: profile.age || null,
    gender: label(profile.gender),
    religion: label(profile.religion),
    community: label(profile.caste),
    motherTongue: label(profile.mother_tongue),
    maritalStatus: label(profile.marital_status),
    education: label(profile.education_level),
    occupation: label(profile.occupation),
    city: label(profile.working_city || profile.family_city),
    diet: label(profile.diet),
    familyType: label(profile.family_type),
    aboutMe: cleanText(profile.about_me, 700)
  };
}

function providerConfig() {
  return {
    enabled: toBool(process.env.AI_ASSIST_ENABLED, false),
    provider: String(process.env.AI_PROVIDER || 'anthropic').trim().toLowerCase(),
    model: process.env.ANTHROPIC_MODEL || process.env.AI_MODEL || DEFAULT_MODEL,
    apiKey: process.env.ANTHROPIC_API_KEY || '',
    timeoutMs: Number(process.env.AI_TIMEOUT_MS || 8000)
  };
}

function extractText(payload) {
  if (!payload) return '';
  if (typeof payload === 'string') return payload;
  const blocks = Array.isArray(payload.content) ? payload.content : [];
  return blocks
    .map((block) => block?.text || '')
    .filter(Boolean)
    .join('\n')
    .trim();
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
      logger.warn(`AI JSON parse failed: ${error.message}`);
      return null;
    }
  }
}

async function callAnthropic({ system, prompt, maxTokens = 700 }) {
  const config = providerConfig();
  if (!config.enabled || config.provider !== 'anthropic' || !config.apiKey || typeof fetch !== 'function') {
    return null;
  }
  try {
    const response = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'x-api-key': config.apiKey,
        'anthropic-version': ANTHROPIC_VERSION
      },
      body: JSON.stringify({
        model: config.model,
        max_tokens: maxTokens,
        temperature: 0.4,
        system,
        messages: [{ role: 'user', content: prompt }]
      }),
      signal: AbortSignal.timeout(config.timeoutMs)
    });
    if (!response.ok) {
      logger.warn(`Anthropic AI assist failed with status ${response.status}`);
      return null;
    }
    const payload = await response.json();
    return {
      text: extractText(payload),
      provider: 'anthropic',
      model: config.model
    };
  } catch (error) {
    logger.warn(`Anthropic AI assist unavailable: ${error.message}`);
    return null;
  }
}

function fallbackBioSuggestions(profile, currentBio) {
  const p = compactProfile(profile);
  const existing = cleanText(currentBio || p.aboutMe, 700);
  const anchor = existing || `${p.name} is a ${p.education} professional working as ${p.occupation} in ${p.city}.`;
  return [
    `${anchor} I value respectful communication, family bonds, and a partnership where both people support each other's growth.`,
    `${p.name} comes from a ${p.motherTongue}-speaking ${p.familyType.toLowerCase()} family and is looking for a grounded marriage built on trust, warmth, and shared responsibility.`,
    `I am serious about finding a compatible life partner. My priorities are family respect, emotional maturity, career stability, and a home where both families feel welcomed.`
  ].map((item) => cleanText(item, 520));
}

function fallbackIcebreakers(sourceProfile, targetProfile) {
  const source = compactProfile(sourceProfile);
  const target = compactProfile(targetProfile);
  const targetCity = target.city === 'Not specified' ? 'your city' : target.city;
  return [
    `Hi ${target.name}, I liked the way your profile reflects both career focus and family values. What kind of partnership feels most important to you?`,
    `Hello ${target.name}, your ${target.education} and ${target.occupation} background stood out to me. Would you be open to a respectful conversation to understand compatibility?`,
    `Hi ${target.name}, I noticed you are based in ${targetCity}. How do you usually prefer families to take the first introduction forward?`
  ].map((item) => cleanText(item, 320));
}

function normalizeSuggestionList(value, fallback) {
  const items = Array.isArray(value) ? value : [];
  const normalized = items
    .map((item) => typeof item === 'string' ? item : item?.text || item?.bio || item?.message || '')
    .map((item) => cleanText(item, 560))
    .filter((item) => item.length >= 20)
    .slice(0, 5);
  return normalized.length ? normalized : fallback;
}

exports.suggestBio = async ({ profile, currentBio }) => {
  const fallback = fallbackBioSuggestions(profile, currentBio);
  const p = compactProfile(profile);
  const ai = await callAnthropic({
    system: 'You improve Indian matrimony profile introductions. Be respectful, specific, family-safe, and truthful. Do not invent salary, contact details, caste claims, or verification claims. Return JSON only.',
    prompt: JSON.stringify({
      task: 'Write 3 improved About Me options for this SoulMatch member. Each option should be 45-75 words, warm, mature, and suitable for a matrimony profile.',
      profile: p,
      currentBio: cleanText(currentBio || p.aboutMe, 700),
      outputSchema: { suggestions: ['string'] }
    }),
    maxTokens: 900
  });
  if (!ai?.text) {
    return { source: 'rules', provider: 'local', model: 'fallback', suggestions: fallback };
  }
  const parsed = parseJsonObject(ai.text);
  return {
    source: 'ai',
    provider: ai.provider,
    model: ai.model,
    suggestions: normalizeSuggestionList(parsed?.suggestions, fallback)
  };
};

exports.generateIcebreakers = async ({ sourceProfile, targetProfile }) => {
  const fallback = fallbackIcebreakers(sourceProfile, targetProfile);
  const source = compactProfile(sourceProfile);
  const target = compactProfile(targetProfile);
  const ai = await callAnthropic({
    system: 'You write safe, respectful matrimony conversation starters. Avoid pressure, flirting, phone-number requests, financial topics, and private contact asks. Return JSON only.',
    prompt: JSON.stringify({
      task: 'Write 4 opening messages for a SoulMatch member to send after reviewing a profile. Keep each under 34 words and make them profile-aware.',
      sourceProfile: source,
      targetProfile: target,
      outputSchema: { suggestions: ['string'] }
    }),
    maxTokens: 700
  });
  if (!ai?.text) {
    return { source: 'rules', provider: 'local', model: 'fallback', suggestions: fallback };
  }
  const parsed = parseJsonObject(ai.text);
  return {
    source: 'ai',
    provider: ai.provider,
    model: ai.model,
    suggestions: normalizeSuggestionList(parsed?.suggestions, fallback)
  };
};
