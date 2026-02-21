# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./gradlew clean build          # Full build (default tasks)
./gradlew impl:test            # Run impl module tests
./gradlew impl:test --tests ascelion.merkle.TreeNodeTest        # Single test class
./gradlew impl:test --tests ascelion.merkle.TreeNodeTest.withOne  # Single test method
./gradlew testReport           # Generate HTML test reports (build/reports/tests/index.html)
./gradlew jacocoTestReport     # Generate coverage reports
```

## Architecture

Multi-module Gradle project:
- **`impl/`** — Core Merkle tree library (generic, no external runtime deps)
- **`demo/`** — Standalone REST API application using Jersey + Grizzly + Weld CDI

### Core Library (`impl`)

The tree is generic over hash type `<T>`. Construction requires three functions:
- `hashFn: UnaryOperator<T>` — hash a single value
- `concatFn: BinaryOperator<T>` — combine two hashes
- `zero: Supplier<T>` — identity/filler value for padding

**Key classes:**
- `TreeBuilder<T>` — fluent builder; call `.collect(...)` then `.build()`. Also has static `isValid()` for chain verification.
- `TreeNode` — base node (hash, left, right, parent, height, count)
- `TreeLeaf<S>` — extends TreeNode; holds content and the validation chain (list of hashes from leaf to root)
- `TreeRoot<T>` — interface for the built tree; exposes `hash()`, `height()`, `count()`, `getLeaf(int)`
- `DataSlice` — extends TreeLeaf for byte[] content; static helpers to build trees from `InputStream`/`ByteChannel`

**Tree construction** (`TreeBuilder.doBuild()`): bottom-up, pairs nodes into parents, rounds up to power-of-2 height using filler `Null<T>` nodes. Hash of internal node = `hash(concat(left.hash, right.hash))`.

**Leaf lookup** (`Root.getLeaf(int index)`): uses bit-shifting the index by tree height, traversing left/right based on bit sign — O(height).

**Chain validation** (`TreeBuilder.isValid(List<T> chain, int index, BiPredicate<T,T> eq)`): reconstructs hashes bottom-up using the index's bit pattern to determine concat order, then compares against the chain's root hash.

### Demo Application (`demo`)

Standalone HTTP server that hashes files from one or more directories and exposes them via REST:
- `Main` — sets up Weld CDI + Jersey/Grizzly HTTP server
- `Args` — PicoCLI options: `--size` (slice bytes, default 1024), `--algo` (hash algo, default SHA-256), `--bind`, `--port` (default 8080)
- `FileStoreService` — CDI singleton; walks file trees, builds Merkle trees, manages UUID-keyed containers
- `FilesResource` — JAX-RS endpoints: `GET /containers`, `GET /containers/{uuid}`, `GET /slice/{hash}/{index}`

Demo uses Lombok (`@RequiredArgsConstructor`, `@Getter`, etc.) — see `demo/lombok.config`.

## Versioning

Version is managed by the `axion-release-plugin` from SCM tags. Do not manually edit version numbers.
