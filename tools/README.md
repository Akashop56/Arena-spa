# Verification helpers

Static gates used when a full Gradle build is unavailable (no JDK / no Maven
access). They complement, and do not replace, `./gradlew assembleDebug`.

- `xref.py`  — every `com.ronin.*` import resolves to a real declaration.
- `vmcheck.py` — every `viewModel.x` / `viewModel::x` reference in a Compose
  screen resolves to a real member of the matching ViewModel.

```bash
python3 tools/xref.py
python3 tools/vmcheck.py
```

- `sqlcheck.py`  — rebuilds the schema from `@Entity` classes and executes every
  `@Query` against a real SQLite engine (approximates Room's compile-time check).
- `hiltcheck.py` — resolves every `@Inject` constructor parameter against
  `@Provides` / `@Binds` / other `@Inject` types (approximates Hilt's check).

`verify/` holds the executable subsystem checks described in
`docs/VALIDATION.md` (secure vault, provider parsing/SSE, memory SQL, settings
serialisation). They run against real Gson / JCE / SQLite, not mocks.
