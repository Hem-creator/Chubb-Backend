# Documentation index

Project: **Chubb APAC Claims Management – Backend**. The main setup and API reference is the
[root README](../README.md).

| Document | What it covers |
|---|---|
| [requirements.md](requirements.md) | Background, business requirements per role, technology decisions, domain model and API list as specified |
| [api-flow.md](api-flow.md) | Every Swagger endpoint with bodies and status codes, claim lifecycle, end-to-end flow, Kafka events |
| [project-workflow.txt](project-workflow.txt) | Plain-text walkthrough for presenting the project with Swagger: 20-step demo, error cases, Kafka events |
| [architecture.md](architecture.md) | Layers, domain model, lifecycle diagram, event flow, trade-offs, future enhancements |
| [decisions.md](decisions.md) | Log of the main design decisions and why they were made |
| [ai-journal.md](ai-journal.md) | AI usage record: prompts, response, accepted / rejected / modified, reasoning |

Related files outside this folder:

- [`../openapi/claims-api.yaml`](../openapi/claims-api.yaml) – API contract
- [`../postman/claims-management.postman_collection.json`](../postman/claims-management.postman_collection.json) – manual tests
- [`../docker-compose.yml`](../docker-compose.yml) – local Kafka
