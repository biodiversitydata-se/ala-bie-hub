run:
	./gradlew bootRun

run-docker:
	./gradlew build
	docker compose build --no-cache
	docker compose up --detach

release:
	@./sbdi/make-release.sh