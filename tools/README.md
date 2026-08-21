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
