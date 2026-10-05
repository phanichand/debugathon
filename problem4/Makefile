COMPOSE = docker compose -f docker/docker-compose.yml
.PHONY: up down smoke traffic verify logs reset
up:
	$(COMPOSE) up -d --build
smoke:
	./scripts/smoke-test.sh
traffic:
	./scripts/generate-traffic.sh
verify:
	python3 scripts/verify.py
logs:
	$(COMPOSE) logs -f consumer producer
down:
	$(COMPOSE) --profile scale down
reset:
	$(COMPOSE) --profile scale down -v
