# SoulMatch AWS EC2 Lift-and-Shift Runbook

This is the safest first migration from the current Azure VM to AWS EC2. It keeps the same Docker Compose architecture and moves the app, databases, uploads, and production environment as-is.

## Target Outcome

- Current Azure VM remains running until AWS is fully verified.
- AWS EC2 runs the same `docker/docker-compose.prod.yml`.
- Postgres, MongoDB, Redis, and profile uploads are restored.
- GitHub Actions deploys to AWS EC2 after the cutover.
- No app rewrite is required.

## Critical Secrets to Preserve

Never paste these in chat or commit them:

- `docker/production.env`
- `JWT_SECRET`
- `REFRESH_TOKEN_SECRET`
- `ADMIN_JWT_SECRET`
- `INTERNAL_SERVICE_SECRET`
- `DOCUMENT_ENCRYPTION_KEY`
- `DOCUMENT_HASH_PEPPER`
- `RAZORPAY_*`
- `FIREBASE_*`
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`
- `TWILIO_*`

`DOCUMENT_ENCRYPTION_KEY` is especially important. If it is lost, encrypted agent KYC documents cannot be decrypted.

## Recommended AWS EC2 Setup

Minimum:

- Ubuntu 22.04 or 24.04 LTS
- Instance: `t3.medium` minimum, `t3.large` preferred
- Disk: 80 GB gp3 minimum
- Security group inbound:
  - `22/tcp` from your IP only
  - `80/tcp` from anywhere
  - `443/tcp` from anywhere, when SSL is configured
- Elastic IP attached to the EC2 instance

Keep Azure running until all validation steps pass.

## 1. Create Backup on Current Azure VM

SSH to the current Azure VM:

```bash
ssh azureuser@20.204.142.19
cd /home/azureuser/soulmatch
```

Create a migration backup folder:

```bash
set -euo pipefail

BACKUP_DIR="$HOME/soulmatch-aws-migration-$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"

cp docker/production.env "$BACKUP_DIR/production.env"
chmod 600 "$BACKUP_DIR/production.env"

set -a
. docker/production.env
set +a

docker compose --env-file docker/production.env -f docker/docker-compose.prod.yml ps > "$BACKUP_DIR/docker-compose-ps.txt"
cat .soulmatch-deployed-version.json > "$BACKUP_DIR/deployed-version.json" 2>/dev/null || true
```

Backup Postgres:

```bash
docker compose --env-file docker/production.env -f docker/docker-compose.prod.yml exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc > "$BACKUP_DIR/postgres.dump"
```

Backup MongoDB chat data:

```bash
docker compose --env-file docker/production.env -f docker/docker-compose.prod.yml exec -T mongodb \
  mongodump --archive --gzip --db soulmatch_chat > "$BACKUP_DIR/mongodb.archive.gz"
```

If `mongodump` is not available inside the Mongo container, use a temporary tools container:

```bash
docker run --rm --network docker_default mongo:7 \
  mongodump --archive --gzip --uri "mongodb://mongodb:27017/soulmatch_chat" > "$BACKUP_DIR/mongodb.archive.gz"
```

Backup uploads volume:

```bash
PROFILE_UPLOADS_VOLUME="$(docker volume ls --format '{{.Name}}' | grep '_profile_uploads$' | head -1)"
test -n "$PROFILE_UPLOADS_VOLUME"

docker run --rm \
  -v "$PROFILE_UPLOADS_VOLUME:/volume:ro" \
  -v "$BACKUP_DIR:/backup" \
  alpine:3.20 \
  tar czf /backup/profile_uploads.tar.gz -C /volume .
```

Optional Redis backup:

```bash
REDIS_VOLUME="$(docker volume ls --format '{{.Name}}' | grep '_redis_data$' | head -1 || true)"
if [ -n "$REDIS_VOLUME" ]; then
  docker run --rm \
    -v "$REDIS_VOLUME:/volume:ro" \
    -v "$BACKUP_DIR:/backup" \
    alpine:3.20 \
    tar czf /backup/redis_data.tar.gz -C /volume .
fi
```

Package the backup:

```bash
tar czf "$HOME/soulmatch-aws-migration.tar.gz" -C "$BACKUP_DIR" .
sha256sum "$HOME/soulmatch-aws-migration.tar.gz" > "$HOME/soulmatch-aws-migration.tar.gz.sha256"
ls -lh "$HOME/soulmatch-aws-migration.tar.gz" "$HOME/soulmatch-aws-migration.tar.gz.sha256"
```

Download the backup to your local machine and keep an offline copy:

```powershell
scp -i C:\Users\ANIRUDH\.ssh\soulmatch_github_deploy `
  azureuser@20.204.142.19:/home/azureuser/soulmatch-aws-migration.tar.gz `
  C:\Users\ANIRUDH\Documents\soulmatch-aws-migration.tar.gz
```

## 2. Prepare AWS EC2

Create EC2, then SSH:

```bash
ssh -i /path/to/aws-key.pem ubuntu@AWS_PUBLIC_IP
```

Install runtime packages:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git nginx rsync unzip

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

. /etc/os-release
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $VERSION_CODENAME stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list >/dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker ubuntu
```

Log out and log back in, then verify:

```bash
docker --version
docker compose version
```

## 3. Copy Code and Backup to AWS

On AWS EC2:

```bash
cd /home/ubuntu
git clone https://github.com/krsr08/soulmatch.git
cd soulmatch
```

Copy the backup archive from local machine to AWS:

```powershell
scp -i C:\path\to\aws-key.pem `
  C:\Users\ANIRUDH\Documents\soulmatch-aws-migration.tar.gz `
  ubuntu@AWS_PUBLIC_IP:/home/ubuntu/soulmatch-aws-migration.tar.gz
```

Extract backup on AWS:

```bash
mkdir -p /home/ubuntu/soulmatch-restore
tar xzf /home/ubuntu/soulmatch-aws-migration.tar.gz -C /home/ubuntu/soulmatch-restore
cp /home/ubuntu/soulmatch-restore/production.env /home/ubuntu/soulmatch/docker/production.env
chmod 600 /home/ubuntu/soulmatch/docker/production.env
```

## 4. Restore Data on AWS

Start only infrastructure services:

```bash
cd /home/ubuntu/soulmatch
docker compose --env-file docker/production.env -f docker/docker-compose.prod.yml up -d postgres mongodb redis
```

Load environment:

```bash
set -a
. docker/production.env
set +a
```

Wait for Postgres:

```bash
POSTGRES_CONTAINER="$(docker compose --env-file docker/production.env -f docker/docker-compose.prod.yml ps -q postgres)"
until docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$POSTGRES_CONTAINER" pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"; do
  sleep 3
done
```

Restore Postgres:

```bash
docker exec -i -e PGPASSWORD="$POSTGRES_PASSWORD" "$POSTGRES_CONTAINER" \
  pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists < /home/ubuntu/soulmatch-restore/postgres.dump
```

Restore MongoDB:

```bash
docker compose --env-file docker/production.env -f docker/docker-compose.prod.yml exec -T mongodb \
  mongorestore --archive --gzip --drop < /home/ubuntu/soulmatch-restore/mongodb.archive.gz
```

Restore profile uploads:

```bash
PROFILE_UPLOADS_VOLUME="$(docker volume ls --format '{{.Name}}' | grep '_profile_uploads$' | head -1)"
test -n "$PROFILE_UPLOADS_VOLUME"

docker run --rm \
  -v "$PROFILE_UPLOADS_VOLUME:/volume" \
  -v /home/ubuntu/soulmatch-restore:/backup \
  alpine:3.20 \
  sh -c "rm -rf /volume/* && tar xzf /backup/profile_uploads.tar.gz -C /volume"
```

Optional Redis restore:

```bash
if [ -f /home/ubuntu/soulmatch-restore/redis_data.tar.gz ]; then
  REDIS_VOLUME="$(docker volume ls --format '{{.Name}}' | grep '_redis_data$' | head -1)"
  docker compose --env-file docker/production.env -f docker/docker-compose.prod.yml stop redis
  docker run --rm \
    -v "$REDIS_VOLUME:/volume" \
    -v /home/ubuntu/soulmatch-restore:/backup \
    alpine:3.20 \
    sh -c "rm -rf /volume/* && tar xzf /backup/redis_data.tar.gz -C /volume"
  docker compose --env-file docker/production.env -f docker/docker-compose.prod.yml up -d redis
fi
```

## 5. Deploy on AWS EC2

```bash
cd /home/ubuntu/soulmatch
DEPLOYED_SOURCE_COMMIT="$(git rev-parse HEAD)" \
DEPLOYED_SOURCE_BRANCH="main" \
APP_DIR="/home/ubuntu/soulmatch" \
bash tools/deploy-production.sh
```

Validate locally on EC2:

```bash
curl -fsS http://127.0.0.1/api/v1/public/config
curl -fsS http://127.0.0.1:3002/health
docker compose --env-file docker/production.env -f docker/docker-compose.prod.yml ps
cat .soulmatch-deployed-version.json
```

Validate from your machine:

```powershell
Invoke-WebRequest -UseBasicParsing http://AWS_PUBLIC_IP/api/v1/public/config
```

## 6. Switch GitHub Actions Deploy to AWS

For the fastest lift-and-shift, reuse the existing secret names even though they say Azure:

- `AZURE_VM_HOST` = AWS EC2 public IP or DNS
- `AZURE_VM_USER` = `ubuntu`
- `AZURE_VM_SSH_KEY` = private key that can SSH to AWS EC2
- `AZURE_VM_PORT` = `22`
- `AZURE_DEPLOY_PATH` = `/home/ubuntu/soulmatch`

Also update:

- `APP_PUBLIC_API_BASE_URL` if the app backend URL changes
- Android/Firebase distribution secrets only if build signing or Google Sign-In changes

Longer term, rename the workflow secrets to AWS names, but do that after the migration is stable.

## 7. DNS Cutover

If using a domain:

1. Create or update DNS A record to point to AWS Elastic IP.
2. Keep TTL low, for example 300 seconds, during cutover.
3. Configure SSL using your chosen method:
   - Nginx + Certbot on EC2, or
   - AWS ALB + ACM certificate.

If using only IP:

1. Update `APP_PUBLIC_API_BASE_URL`.
2. Rebuild/distribute Android APK if the app points to the old Azure IP.

## 8. Final Validation Checklist

- Login via mobile OTP works.
- Google login works.
- Member home, matches, profile, preferences work.
- Agent registration and dashboard work.
- Agent KYC upload works.
- Agent encrypted documents are stored.
- Admin dashboard can approve/reject profiles and agents.
- Payments still create Razorpay orders.
- Chat works.
- Notifications work.
- Public config endpoint works.
- `docker compose ps` shows all services healthy/running.
- DB row counts look sane compared with Azure.

Example DB sanity checks:

```bash
docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$POSTGRES_CONTAINER" \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) FROM users; SELECT COUNT(*) FROM profiles; SELECT COUNT(*) FROM advisors;"
```

## 9. Rollback Plan

Do not delete Azure.

If AWS validation fails:

1. Point DNS/API URL back to Azure.
2. Keep AWS stopped or isolated.
3. Fix restore/deploy issue.
4. Re-run restore from backup.

Only decommission Azure after at least 7 days of stable AWS production traffic and after a fresh AWS backup has been tested.

## 10. Later AWS Improvements

After lift-and-shift is stable:

- Move Postgres to RDS.
- Move Redis to ElastiCache.
- Move uploads/documents to S3 with KMS encryption.
- Move secrets to AWS Secrets Manager.
- Use ALB + ACM SSL.
- Use CloudWatch logs and alarms.
- Replace public SSH deploy with SSM Session Manager or a self-hosted GitHub runner.
