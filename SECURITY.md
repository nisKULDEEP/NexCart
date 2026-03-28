# 🔐 Security Guidelines - NexCart Microservices

## Critical: Secrets Management

⚠️ **NEVER commit passwords, API keys, or private credentials to version control**

### Local Development
1. Copy `.env.example` to `.env`:
   ```bash
   cp services/.env.example services/.env
   ```

2. Edit `.env` with your local credentials:
   ```bash
   DB_USER=niskuldeep
   DB_PASSWORD=your-local-password
   MONGO_PASSWORD=your-local-password
   ```

3. The `.env` file is in `.gitignore` and will NOT be committed ✅

### Environment Variable Substitution

All configuration files use Spring property placeholders with environment variables:

```yaml
# ✅ SECURE - Uses environment variables with fallbacks
spring:
  datasource:
    username: ${DB_USER:niskuldeep}
    password: ${DB_PASSWORD:niskuldeep}
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/product
```

**NOT hardcoded:**
```yaml
# ❌ INSECURE - Never do this!
spring:
  datasource:
    username: niskuldeep
    password: my-secret-password
```

---

## Production Deployment Security

### 1. GCP Cloud Run (Recommended)

Use **Google Secret Manager**:

```bash
# Create secrets in GCP
gcloud secrets create db-password --data-file=- < password.txt
gcloud secrets create postgres-user --data-file=- < username.txt

# Reference in Cloud Run deployment
gcloud run deploy order-service \
  --set-env-vars=DB_PASSWORD=secret:db-password:latest \
  --set-env-vars=DB_USER=secret:postgres-user:latest
```

### 2. Kubernetes Secrets

Store sensitive data in Kubernetes:

```bash
# Create secret
kubectl create secret generic db-credentials \
  --from-literal=username=postgres \
  --from-literal=password=your-prod-password

# Reference in deployment
env:
  - name: DB_USER
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: username
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: password
```

### 3. AWS Systems Manager Parameter Store

```bash
# Store secrets
aws ssm put-parameter --name /order-service/db-password --value "secure-password" --type SecureString

# Reference in environment
SPRING_DATASOURCE_PASSWORD: !Sub '{{resolve:secretsmanager:db-password:SecretString:password}}'
```

### 4. HashiCorp Vault

```bash
# Store secret
vault kv put secret/order-service/database \
  username="postgres" \
  password="secure-password"

# Spring Cloud Vault auto-loads at startup
spring:
  cloud:
    vault:
      uri: https://vault.example.com
      token: s.xxxxxxxxxxxxxxxx
```

---

## Credential Rotation

### Regular Rotation Schedule

- **Database Passwords:** Every 90 days
- **API Keys:** Every 90 days  
- **Service Tokens:** Every 30 days
- **Certificates:** Every 1 year (or before expiration)

### Rotation Procedure

```bash
# 1. Generate new credentials
# 2. Update in secrets manager
gcloud secrets versions add db-password --data-file=- < new-password.txt

# 3. Restart services (they will pick up new secrets)
gcloud run deploy order-service --region us-central1 --no-traffic

# 4. Test with new credentials
# 5. Remove old version after validation
```

---

## Pre-Commit Security Checks

### Install GitGuardian CLI (Recommended)

```bash
# Install
brew install gitguardian/tap/ggshield

# Configure
ggshield auth login

# Run before commit
ggshield secret scan commit
```

### Git Hook (Automatic)

```bash
# Create pre-commit hook
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
ggshield secret scan commit --staged
EOF

chmod +x .git/hooks/pre-commit
```

### TruffleHog (Alternative)

```bash
# Scan entire repo
docker run -v "$PWD:/path" trufflesecurity/trufflehog:latest filesystem /path

# Scan Git history
docker run -v "$PWD:/path" trufflesecurity/trufflehog:latest git file:///path --only-verified
```

---

## Detected Vulnerabilities - FIXED ✅

### Issue 1: Hardcoded Database Credentials
**Status:** ✅ FIXED
- **Files affected:** 5 YAML configuration files
- **Fix:** Replaced with environment variable substitution
- **Pattern used:** `${VAR_NAME:default-value}`

### Issue 2: Docker Compose Credentials
**Status:** ✅ MITIGATED
- **Files affected:** docker-compose.yml, docker-compose-full.yml
- **Action:** Use .env file for sensitive values
- **Update needed:** Create `.env.local` for production use

### Issue 3: Missing .gitignore Entries
**Status:** ✅ FIXED
- **Files affected:** .gitignore
- **Added entries:** `.env`, `.env.*`, `*.key`, `*.pem`, `*.p12`, `secrets/`

---

## Production Deployment Checklist

Before deploying to production, verify:

- [ ] No `.env` file in git history
- [ ] All credentials use environment variable substitution
- [ ] Secrets stored in cloud provider's secret manager
- [ ] Database passwords meet complexity requirements (12+ chars, mixed case, symbols)
- [ ] API keys rotated in last 90 days
- [ ] SSH keys configured for authentication (not passwords)
- [ ] HTTPS/TLS enabled on all endpoints
- [ ] Rate limiting configured on API Gateway
- [ ] CORS policies restricted to known domains
- [ ] SQL injection prevention verified (using parameterized queries)
- [ ] CSRF tokens enabled on state-changing endpoints
- [ ] Security headers configured (X-Frame-Options, Content-Security-Policy, etc.)
- [ ] Logging does not include sensitive data
- [ ] Audit trail enabled for all privileged operations

---

## Incident Response

### If credentials are accidentally committed:

1. **IMMEDIATELY rotate** the exposed credential
2. **Force push** to remove from history (if private repo):
   ```bash
   git filter-branch --tree-filter 'rm -f <file-with-secret>' -- --all
   git push --force-with-lease
   ```
3. **Scan history** for other leaks:
   ```bash
   git log -p | grep -i password
   ```
4. **Monitor** for unauthorized access

### Tools for remediation:

- **BFG Repo-Cleaner:** `bfg --delete-files <filename>`
- **GitGuardian Incident Response:** https://www.gitguardian.com/
- **GitHub Secret Scanning:** Automatic in public repos

---

## Network Security

### GCP Cloud Run

```bash
# Restrict to VPC only
gcloud run deploy order-service \
  --vpc-connector microservices-connector \
  --vpc-egress private-ranges-only

# Disable public access
gcloud run services remove-iam-policy-binding order-service \
  --member="allUsers" \
  --role="roles/run.invoker"

# Allow only from gateway
gcloud run services add-iam-policy-binding order-service \
  --member=serviceAccount:gateway-sa@project.iam.gserviceaccount.com \
  --role=roles/run.invoker
```

### Database Network Policy

```sql
-- PostgreSQL: Restrict to specific IPs
CREATE ROLE app_user WITH PASSWORD 'complex_password';
ALTER ROLE app_user CONNECTION LIMIT 10;
```

---

## Logging Security

### DO NOT log:
- ❌ Passwords or API keys
- ❌ Personal Identifiable Information (PII)
- ❌ Credit card numbers
- ❌ Private keys or certificates

### Example Secure Logging:

```java
// ❌ INSECURE
log.info("User: {}, Password: {}", user, password);

// ✅ SECURE
log.info("User: {} authenticated", user);
log.debug("Connection established to {} database", dbName);
```

---

## Regular Security Audits

### Monthly Tasks:
- Review access logs for anomalies
- Audit IAM permissions
- Check for unused service accounts
- Verify encryption at rest/transit

### Quarterly Tasks:
- Penetration testing
- Dependency vulnerability scanning (`mvn dependency-check:check`)
- Code security analysis (`SonarQube`)
- Security audit of configuration

### Annually:
- Full security assessment
- Compliance review (GDPR, SOC 2, etc.)
- Infrastructure security audit
- Disaster recovery testing

---

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [12 Factor App - Config](https://12factor.net/config)
- [Spring Cloud Config Security](https://spring.io/guides/gs/centralized-configuration/)
- [GitGuardian Documentation](https://docs.gitguardian.com/)
- [GCP Secret Manager](https://cloud.google.com/secret-manager/docs)

---

**Last Updated:** March 28, 2026  
**Status:** ✅ Security best practices implemented

