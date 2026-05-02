# SoulMatch Monitoring and Alerting

SoulMatch production monitoring is handled by `tools/monitor-production-health.sh`.

## What It Checks

- Docker Compose services are present and running.
- Docker healthchecks are not unhealthy.
- Local HTTP health endpoints respond for all backend services.
- Admin web and Nginx respond locally.
- Root disk usage is below 85%.
- Memory usage is below 90%.
- Local backup is newer than 36 hours.
- Latest local backup files match their `manifest.json` checksums.
- Latest local backup has uploaded to Azure Blob when Azure backup settings are configured.

## Manual Run

Run on the Azure VM:

```bash
cd /home/azureuser/soulmatch
bash tools/monitor-production-health.sh
```

Latest JSON report:

```bash
cat /home/azureuser/soulmatch-ops/monitor-latest.json
```

## Install Schedule

Run on the Azure VM:

```bash
cd /home/azureuser/soulmatch
bash tools/install-monitor-cron.sh
```

Default schedule:

```bash
*/5 * * * *
```

Check logs:

```bash
tail -n 100 /home/azureuser/soulmatch-ops/monitor.log
```

## Optional Webhook Alerts

Set `ALERT_WEBHOOK_URL` in the monitor runtime environment to send alerts on status changes.

The webhook receives JSON:

```json
{
  "text": "SoulMatch production monitor: FAIL\nHost: soulmatch-vm\n..."
}
```

Until a webhook is configured, the monitor writes logs and the latest JSON report locally.
