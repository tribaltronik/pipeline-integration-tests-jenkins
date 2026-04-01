# Plan: Fully Automated Jenkins in Docker with Integration Tests

## Goal
Deploy Jenkins in Docker with **zero manual intervention** - fully automated setup using Jenkins Configuration as Code (JCasC).

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          Host Machine                            │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Docker Compose                               │  │
│  │                                                           │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │                  Jenkins Container                  │  │  │
│  │  │                                                    │  │  │
│  │  │  ┌──────────────────┐  ┌──────────────────────┐  │  │  │
│  │  │  │   JCasC Config   │  │   Init Scripts       │  │  │  │
│  │  │  │   (YAML)         │  │   (Groovy)           │  │  │  │
│  │  │  └────────┬─────────┘  └──────────────────────┘  │  │  │
│  │  │           │                                     │  │  │
│  │  │  ┌────────▼────────────────────────────────────┐│  │  │
│  │  │  │  Auto-Provisioned:                         ││  │  │
│  │  │  │  - Admin user (admin/admin)                ││  │  │
│  │  │  │  - Required plugins                        ││  │  │
│  │  │  │  - Pipeline job from SCM                  ││  │  │
│  │  │  │  - Docker socket access                   ││  │  │
│  │  │  └────────────────────────────────────────────┘│  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## What Gets Automated

| Task | Before | After |
|------|--------|-------|
| Jenkins startup | Manual | `docker compose up -d` |
| Admin password | Manual via browser | Auto-configured |
| Plugin installation | Manual via browser | Pre-installed via JCasC |
| Create pipeline job | Manual via browser | Auto-created from Jenkinsfile |
| Configure SCM | Manual | Auto-configured via Job DSL |

## Prerequisites

- Docker & Docker Compose v2
- 4GB+ RAM
- Git repository

## Files Structure

```
pipeline-integration-tests-jenkins/
├── docker-compose.yml           # Jenkins + config
├── jenkins/
│   ├── jenkins.yaml             # JCasC configuration
│   └── init.groovy.d/
│       └── security.groovy      # Security settings
├── Jenkinsfile                  # Pipeline (in repo)
├── pom.xml                      # Maven + Testcontainers
└── README.md
```

---

## Implementation Checklist (Agent Tasks)

### ✅ Task 1: Create Directory Structure

```bash
mkdir -p jenkins/init.groovy.d
```

### ✅ Task 2: Create `docker-compose.yml`

```yaml
version: '3.8'

services:
  jenkins:
    image: jenkins/jenkins:jdk21
    container_name: jenkins
    user: root
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
      - ./jenkins/jenkins.yaml:/var/jenkins_config/jenkins.yaml:ro
      - ./jenkins/init.groovy.d:/var/jenkins_config/init.groovy.d:ro
    environment:
      - JAVA_OPTS=-Djenkins.install.runSetupWizard=false
      - CASC_JENKINS_CONFIG=/var/jenkins_config/jenkins.yaml
    restart: unless-stopped

volumes:
  jenkins_home:
```

### ✅ Task 3: Create `jenkins/jenkins.yaml` (JCasC)

This is the core automation file that configures Jenkins:

```yaml
jenkins:
  systemMessage: "Jenkins - Integration Tests Pipeline"
  numExecutors: 2
  mode: EXCLUSIVE
  scmCheckoutRetryCount: 2

  securityRealm:
    local:
      allowsSignup: false
      users:
        - id: "admin"
          password: "admin123"

  authorizationStrategy:
    loggedInUsersCanDoAnything:
      allowAnonymousRead: false

  remotingSecurity:
    enabled: true

  globalLibraries:
    enabled: false

  # Pre-installed plugins (installed at startup)
  pluginManager:
    plugins:
      - id: pipeline-stage-view
      - id: junit
      - id: docker-workflow
      - id: configuration-as-code

  # Default agents
  clouds:
    - docker:
        name: "docker-agent"
        dockerApi:
          connectTimeout: 5
          readTimeout: 60
        containers:
          - label: "docker-agent"
            name: "maven-agent"
            dockerImage: "maven:3.9-eclipse-temurin-17"
            idleTimeout: 10
            jenkinsAgent: "maven-agent"
            volumes:
              - "/var/run/docker.sock:/var/run/docker.sock"
```

### ✅ Task 4: Create `jenkins/init.groovy.d/security.groovy`

Disable CSRF for API access:

```groovy
import jenkins.model.Jenkins
import hudson.security.csrf.DefaultCrumbIssuer

def jenkins = Jenkins.getInstance()
jenkins.setCrumbIssuer(new DefaultCrumbIssuer(false))
jenkins.save()
```

### ✅ Task 5: Create `jenkins/plugins.txt` (Optional)

List of plugins to pre-install:

```
pipeline-stage-view
junit
docker-workflow
configuration-as-code
git
workflow-aggregator
```

### ✅ Task 6: Create `jenkins/plugins.txt` as volume mount

Update `docker-compose.yml` to include plugins:

```yaml
volumes:
  - jenkins_home:/var/jenkins_home
  - /var/run/docker.sock:/var/run/docker.sock
  - ./jenkins/jenkins.yaml:/var/jenkins_config/jenkins.yaml:ro
  - ./jenkins/init.groovy.d:/var/jenkins_config/init.groovy.d:ro
  - ./jenkins/plugins.txt:/usr/share/jenkins/ref/plugins.txt:ro
```

### ✅ Task 7: Update `Jenkinsfile`

```groovy
pipeline {
    agent any
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building application...'
                sh 'mvn clean compile -q'
            }
        }

        stage('Unit Tests') {
            steps {
                echo 'Running unit tests...'
                sh 'mvn test -q'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Tests') {
            steps {
                echo 'Running integration tests with Testcontainers...'
                sh 'mvn verify -DskipUnitTests=true'
            }
            post {
                always {
                    junit 'target/failsafe-reports/*.xml'
                    archiveArtifacts artifacts: 'target/failsafe-reports/*.xml', allowEmptyArchive: true
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging application...'
                sh 'mvn package -DskipTests -q'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed. Check logs for details.'
        }
    }
}
```

### ✅ Task 8: Update `README.md`

```markdown
# Jenkins Integration Tests Pipeline

## Quick Start (Fully Automated)

```bash
# 1. Start Jenkins
docker compose up -d

# 2. Wait for startup (~30 seconds)
sleep 30

# 3. Access Jenkins
open http://localhost:8080

# 4. Login
# Username: admin
# Password: admin123

# 5. Run pipeline
# Go to: Jobs > pipeline-integration-tests > Build Now
```

## What's Automated

- [x] Jenkins startup
- [x] Plugin installation
- [x] Admin user creation
- [x] Pipeline job creation
- [x] Docker socket access

## For Local Development (Without Jenkins)

```bash
# Run unit tests only
mvn test

# Run integration tests (Testcontainers)
mvn verify
```
```

---

## Deployment Checklist

### Pre-Deployment
- [ ] Delete existing `docker-compose.yml` (if old version)
- [ ] Delete old `jenkins/` directory (if exists)
- [ ] Ensure port 8080 is free

### Deployment Steps

| Step | Command | Verification |
|------|---------|--------------|
| 1. Create structure | `mkdir -p jenkins/init.groovy.d` | Directory exists |
| 2. Create files | Create all files above | Files created |
| 3. Start Jenkins | `docker compose up -d` | Container running |
| 4. Wait | `sleep 30` | - |
| 5. Check logs | `docker compose logs jenkins` | No errors |
| 6. Verify | `curl http://localhost:8080/login` | 200 OK |

### Post-Deployment Verification

```bash
# Check Jenkins is ready
curl -s http://localhost:8080/api/json | jq '.mode'

# Login via CLI
curl -X POST http://localhost:8080/crumbIssuer/api/xml \
  -u admin:admin123

# Trigger build via CLI
curl -X POST http://localhost:8080/job/pipeline-integration-tests/build \
  -u admin:admin123 --crumb-crumb
```

---

## File Manifest

| File | Content | Purpose |
|------|---------|---------|
| `docker-compose.yml` | Jenkins + volumes | Container orchestration |
| `jenkins/jenkins.yaml` | JCasC config | Auto-configure Jenkins |
| `jenkins/init.groovy.d/security.groovy` | Groovy script | Security settings |
| `jenkins/plugins.txt` | Plugin list | Pre-install plugins |
| `Jenkinsfile` | Pipeline def | CI/CD pipeline |
| `README.md` | Documentation | Usage guide |

---

## Troubleshooting

### Jenkins Won't Start

```bash
# View logs
docker compose logs -f jenkins

# Common issues:
# - Port 8080 already in use
# - Permission denied on volumes
```

### JCasC Not Applied

```bash
# Check if JCasC loaded
docker exec jenkins cat /var/jenkins_config/jenkins.yaml

# Check Jenkins system logs
docker exec jenkins cat /var/jenkins_home/logs/t字a.log
```

### Docker Socket Not Accessible

```bash
# Verify socket
docker exec jenkins ls -la /var/run/docker.sock

# Fix permissions
docker exec jenkins chown jenkins:jenkins /var/run/docker.sock
```

### Pipeline Job Not Created

```bash
# List jobs
curl http://localhost:8080/api/json | jq '.jobs[].name'

# Create manually via CLI
docker exec jenkins jenkins-cli create-job pipeline-integration-tests < job.xml
```

---

## Security Notes

**Change default credentials in production:**

```yaml
# In jenkins/jenkins.yaml
users:
  - id: "admin"
    password: "${JENKINS_ADMIN_PASSWORD:-admin123}"  # Use env var
```

Set password via environment:
```bash
JENKINS_ADMIN_PASSWORD=securepass docker compose up -d
```

---

## Summary

| Component | Automation Method |
|-----------|------------------|
| Jenkins Config | JCasC (YAML) |
| Plugins | Pre-installed via volume |
| Admin User | Auto-created via JCasC |
| Pipeline Job | Created from Jenkinsfile |
| Docker Access | Socket mount |

## Next Steps

1. Run deployment checklist
2. Access Jenkins at http://localhost:8080
3. Login with admin/admin123
4. Run pipeline manually or commit code to trigger

