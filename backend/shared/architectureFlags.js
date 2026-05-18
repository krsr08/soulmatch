const EDGE_WAF_MODES = ['off', 'nginx-basic', 'managed'];
const STORAGE_PROVIDERS = ['local', 's3', 'azure-blob'];
const QUEUE_PROVIDERS = ['none', 'redis-streams', 'bullmq'];
const SEARCH_PROVIDERS = ['postgres', 'opensearch'];
const OBSERVABILITY_PROVIDERS = ['none', 'local-prometheus', 'managed'];

function normalizeChoice(value, allowed, fallback) {
  const normalized = String(value || '').trim().toLowerCase();
  return allowed.includes(normalized) ? normalized : fallback;
}

function parseBoolean(value, fallback = false) {
  if (value === undefined || value === null || value === '') return fallback;
  return ['1', 'true', 'yes', 'on'].includes(String(value).trim().toLowerCase());
}

function parsePositiveInt(value, fallback) {
  const parsed = Number.parseInt(String(value || ''), 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function getArchitectureFlags(env = process.env) {
  const edgeWafMode = normalizeChoice(env.EDGE_WAF_MODE, EDGE_WAF_MODES, 'nginx-basic');
  const mobileBffEnabled = parseBoolean(env.MOBILE_BFF_ENABLED, false);
  const blobStorageProvider = normalizeChoice(env.BLOB_STORAGE_PROVIDER, STORAGE_PROVIDERS, 'local');
  const asyncQueueProvider = normalizeChoice(env.ASYNC_QUEUE_PROVIDER, QUEUE_PROVIDERS, 'none');
  const searchEngineProvider = normalizeChoice(env.SEARCH_ENGINE_PROVIDER, SEARCH_PROVIDERS, 'postgres');
  const observabilityProvider = normalizeChoice(env.OBSERVABILITY_PROVIDER, OBSERVABILITY_PROVIDERS, 'none');
  const maxMonthlyCostInr = parsePositiveInt(env.MAX_MONTHLY_CLOUD_COST_INR, 1000);

  return {
    edge: {
      wafMode: edgeWafMode,
      mobileBffEnabled,
      mobileBffUrl: String(env.MOBILE_BFF_URL || '').trim()
    },
    storage: {
      blobStorageProvider,
      useSignedUrls: parseBoolean(env.MEDIA_SIGNED_URLS_ENABLED, blobStorageProvider !== 'local')
    },
    async: {
      queueProvider: asyncQueueProvider,
      workersEnabled: asyncQueueProvider !== 'none'
    },
    search: {
      engineProvider: searchEngineProvider,
      openSearchUrl: String(env.OPENSEARCH_URL || '').trim()
    },
    observability: {
      provider: observabilityProvider,
      prometheusEnabled: parseBoolean(env.PROMETHEUS_ENABLED, observabilityProvider === 'local-prometheus'),
      grafanaEnabled: parseBoolean(env.GRAFANA_ENABLED, false)
    },
    budget: {
      maxMonthlyCostInr,
      requireApprovalForPaidResources: parseBoolean(env.REQUIRE_APPROVAL_FOR_PAID_RESOURCES, true)
    }
  };
}

module.exports = {
  EDGE_WAF_MODES,
  OBSERVABILITY_PROVIDERS,
  QUEUE_PROVIDERS,
  SEARCH_PROVIDERS,
  STORAGE_PROVIDERS,
  getArchitectureFlags
};
