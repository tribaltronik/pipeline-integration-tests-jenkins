.PHONY: help up down restart add-pipeline logs clean

help:
	@echo "Available commands:"
	@echo "  make up            - Start Jenkins and Oracle containers"
	@echo "  make down          - Stop and remove containers"
	@echo "  make restart       - Restart containers (with clean volume)"
	@echo "  make add-pipeline  - Add the pipeline to Jenkins"
	@echo "  make logs          - Show logs from Jenkins container"
	@echo "  make clean         - Full cleanup (containers + volumes)"

up:
	docker compose up -d

down:
	docker compose down

restart:
	docker compose down -v
	docker compose up -d

add-pipeline:
	@echo "Adding pipeline to Jenkins..."
	@curl -X POST "http://localhost:8080/createItem?name=integration-tests-pipeline" \
		-H "Content-Type: application/xml" \
		--data-binary @jenkins/job.xml \
		-w "\nHTTP Status: %{http_code}\n" -o /dev/null
	@echo "Pipeline created! Access: http://localhost:8080/job/integration-tests-pipeline/"

logs:
	docker compose logs -f

clean:
	docker compose down -v
