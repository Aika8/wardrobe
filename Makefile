.PHONY: test test-backlog test-server test-client dev stop ps

PORT ?= 8080

# Every test in the repository: backlog rules, server (Gradle: domain + app), client (node --test + type check).
# Works in a clean clone: client dependencies are installed with `npm ci` first. CI runs exactly this.
test: test-backlog test-server test-client

test-backlog:
	python3 -m unittest discover -s scripts/backlog -p 'test_*.py'
	python3 scripts/backlog/backlog.py lint

test-server:
	./gradlew test

# `npm ci` runs when node_modules is missing or older than package-lock.json (always in a clean clone).
client/node_modules/.ci-stamp: client/package-lock.json
	cd client && npm ci
	touch $@

test-client: client/node_modules/.ci-stamp
	cd client && npm test

# Server on PORT (agents: another port than 8080, and `make stop PORT=...` before finishing).
dev:
	PORT=$(PORT) ./gradlew :app:bootRun

stop:
	@pid=$$(lsof -ti tcp:$(PORT) -sTCP:LISTEN 2>/dev/null); if [ -n "$$pid" ]; then kill $$pid && echo "stopped $$pid on $(PORT)"; else echo "nothing on $(PORT)"; fi

ps:
	@lsof -nP -iTCP -sTCP:LISTEN 2>/dev/null | grep -E 'java|node' || echo "no java/node servers listening"
