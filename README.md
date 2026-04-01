# Jenkins Integration Tests Pipeline

Fully automated Jenkins deployment with Testcontainers for Oracle integration tests.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Host Machine                          │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │               Docker Container                        │  │
│  │                                                       │  │
│  │   ┌────────────────────────────────────────────┐   │  │
│  │   │              Jenkins (:8080)                 │   │  │
│  │   │   - Pipeline plugins pre-installed           │   │  │
│  │   │   - CSRF disabled                           │   │  │
│  │   │                                              │   │  │
│  │   │   Pipeline: Maven + Testcontainers           │   │  │
│  │   │   └── Oracle XE (ephemeral)                │   │  │
│  │   └────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

```bash
# 1. Start Jenkins
docker compose up -d

# 2. Wait for startup
sleep 30

# 3. Access Jenkins (no auth required)
open http://localhost:8080

# 4. Run pipeline
open http://localhost:8080/job/pipeline-integration-tests/
# Click "Build Now"
```

## Files

```
pipeline-integration-tests-jenkins/
├── docker-compose.yml          # Jenkins container
├── Dockerfile.jenkins         # Custom image with plugins
├── jenkins/
│   ├── config.xml           # Pipeline job definition
│   └── plugins.txt          # Plugins list
├── Jenkinsfile               # Pipeline (for reference)
├── pom.xml                  # Maven + Testcontainers
└── README.md
```

## Pipeline Job

- **Name**: `pipeline-integration-tests`
- **Stages**: Build → Unit Tests → Integration Tests → Package
- **Database**: Oracle XE via Testcontainers (auto-provisioned)

## Commands

```bash
# Start
docker compose up -d

# Stop (keep data)
docker compose stop

# Stop and remove
docker compose down -v

# View logs
docker compose logs -f jenkins

# Restart
docker compose restart
```

## Testcontainers

The integration tests automatically:
1. Pull Oracle XE image (first run ~5-10 min)
2. Start container
3. Run tests
4. Stop and remove container

## Troubleshooting

### Jenkins won't start
```bash
docker compose logs jenkins
```

### Pipeline fails
```bash
docker compose logs jenkins | tail -50
```

### Reset everything
```bash
docker compose down -v
docker compose up -d
```

## License

MIT
