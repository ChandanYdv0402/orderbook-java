# orderbook-java

A single-instrument limit order book and matching engine in Java 21. It will replay real NASDAQ TotalView-ITCH 5.0 data and publish measured p50 / p99 / p99.9 latency per message, with zero allocations on the hot path.

**Status:** Phase 0 — project skeleton. No engine yet.

- Game plan: [docs/GAME_PLAN.md](docs/GAME_PLAN.md)
- Spec: [SPEC.md](SPEC.md)

## Build

Requires Gradle (JDK 21 is downloaded automatically via the Gradle toolchain if missing).

```bash
gradle test
```
