function normalizeToken(value) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ');
}

function normalizeList(raw) {
  if (Array.isArray(raw)) {
    return raw.map(normalizeToken).filter(Boolean);
  }
  if (!raw) return [];
  if (typeof raw === 'string') {
    return raw
      .split(',')
      .map(normalizeToken)
      .filter(Boolean);
  }
  return [];
}

function hasOverlap(list, ...values) {
  const normalized = new Set(normalizeList(list));
  return values
    .map(normalizeToken)
    .filter(Boolean)
    .some((value) => normalized.has(value));
}

function isMembershipActive(candidate) {
  if (!candidate.membership_expires_at) return true;
  return new Date(candidate.membership_expires_at).getTime() > Date.now();
}

function scoreAdvisorCandidate(profile, candidate) {
  if (!candidate) return null;
  if (candidate.status !== 'active' || candidate.kyc_status !== 'approved' || !isMembershipActive(candidate)) {
    return null;
  }

  const activeAssignments = Number(candidate.active_assignments || 0);
  const maxAssignments = Number(candidate.max_active_assignments || 0);
  if (maxAssignments > 0 && activeAssignments >= maxAssignments) {
    return null;
  }

  const reasons = [];
  let score = 0;

  const profilePincode = normalizeToken(profile.family_pincode);
  const profileLocality = normalizeToken(profile.family_locality);
  const profileCity = normalizeToken(profile.family_city);
  const profileState = normalizeToken(profile.family_state);

  const advisorPincode = normalizeToken(candidate.pincode);
  const advisorLocality = normalizeToken(candidate.locality);
  const advisorCity = normalizeToken(candidate.city);
  const advisorState = normalizeToken(candidate.state);

  if (profilePincode && advisorPincode && profilePincode === advisorPincode) {
    score += 90;
    reasons.push('Exact pincode coverage');
  } else if (profileLocality && advisorLocality && profileLocality === advisorLocality) {
    score += 60;
    reasons.push('Same locality coverage');
  } else if (profileCity && advisorCity && profileCity === advisorCity) {
    score += 42;
    reasons.push('Same city coverage');
  } else if (profileState && advisorState && profileState === advisorState) {
    score += 20;
    reasons.push('Same state coverage');
  }

  if (hasOverlap(candidate.languages, profile.mother_tongue)) {
    score += 18;
    reasons.push('Language fit');
  }

  if (hasOverlap(candidate.communities, profile.religion, profile.caste)) {
    score += 14;
    reasons.push('Community fit');
  }

  score += Math.min(Number(candidate.success_rate || 0), 100) / 10;
  score += Math.min(Number(candidate.average_rating || 0), 5) * 3;
  score += Math.max(0, 20 - activeAssignments);
  score += Math.min(Number(candidate.priority || 0), 20);
  score -= Math.max(0, Number(candidate.complaint_score || 0)) * 8;

  return {
    score: Number(score.toFixed(2)),
    reasons
  };
}

module.exports = {
  normalizeList,
  scoreAdvisorCandidate
};
