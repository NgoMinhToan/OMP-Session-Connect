# Releasing

Publishing is fully automated: **push to `main` and a release appears.**
No manual tagging, version bumping, or asset uploads.

## What the workflow does

`.github/workflows/release.yml` runs on:

- every push to `main` (`v1.0.<commit count>`),
- every `v*` tag (uses the tag as the version),
- manual dispatch (optional version input).

Steps: unit tests → `assembleRelease` + `assembleReleaseNonMinify` → rename APKs
→ `SHA256SUMS.txt` → GitHub Release (`draft: false`, `make_latest: true`).

## Versioning

| Field | Source |
| --- | --- |
| `versionCode` | `git rev-list --count HEAD` — increases on every commit, so APK updates always install over the previous build |
| `versionName` | `1.0.<commit count>`, the pushed tag, or the dispatch input |
| Release tag | `v<versionName>` |

Locally, `gradle.properties` supplies `omp.versionName` / `omp.versionCode`
(defaults `1.0.0` / `1`). CI overrides both:

```sh
./gradlew :app:assembleRelease -Pomp.versionName=1.0.42 -Pomp.versionCode=42
```

### Cutting a semantic version

```sh
git tag v1.2.0
git push origin v1.2.0
```

The workflow then publishes `OMP-Session-Connect-1.2.0-*.apk`. `versionCode`
still comes from the commit count, so it keeps rising even when `versionName`
moves backwards (a manual `1.0.30` after a `1.2.0` tag would still install).

### Re-running a release

Re-run the job for the same commit: the version is derived from the commit, so
the workflow updates the existing release and replaces its assets instead of
creating a second tag.

## Assets per release

Ten APKs — two variants × five "architectures":

| Variant | Gradle task | Flags |
| --- | --- | --- |
| `release` | `assembleRelease` | R8, resource shrinking, signed |
| `releaseNonMinify` | `assembleReleaseNonMinify` | no R8, no shrinking, signed |

ABIs: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`, `universal`. Names follow
`OMP-Session-Connect-<version>-<variant>-<abi>.apk`; `dist/SHA256SUMS.txt` in the
same release carries the checksums.

## In-app updates

The app reads
`https://api.github.com/repos/NgoMinhToan/OMP-Session-Connect/releases/latest`
on launch (no token, 60 requests/hour per IP) and in Settings → **Updates**.
It compares `versionName` numerically against the installed build, then picks the
`release` asset (never `release-nonminify`) for the device ABI, falling back to
`universal`.

The updater depends on the release assets keeping this shape:

```
OMP-Session-Connect-<versionName>-release-<abi>.apk
abi ∈ armeabi-v7a | arm64-v8a | x86 | x86_64 | universal
```

Downloads run in a `dataSync` foreground service, resume from a partial
`.part` file, are written under `filesDir/updates/`, and are exposed to the
system installer through a `FileProvider`. Installing requires the
`REQUEST_INSTALL_PACKAGES` grant ("install unknown apps" for this app); the
update flow opens that settings page when it is missing. A release must keep the
same signing key, otherwise Android rejects the install.

## Signing key

`keystore/omp-release.jks` and `keystore/keystore.properties` are committed so
the workflow can sign builds without repository secrets. Latest certificate:

```
SHA-256 3B:7A:A9:07:9A:5E:DA:C5:1C:C4:77:D7:8A:68:8B:A3:79:A1:70:60:33:D0:72:B2:6B:30:73:57:1D:D9:CB:BA
```

Consequences: a fork can sign an APK with this package name, and the key cannot
be rotated without breaking updates for installed users (Android then refuses
the install unless the old app is uninstalled). Only install builds downloaded
from this repository and check `SHA256SUMS.txt`.

To use different credentials in CI, set repository secrets `OMP_KEYSTORE_PASSWORD`,
`OMP_KEY_ALIAS`, `OMP_KEY_PASSWORD`, plus `keystore/omp-release.jks` in the
branch. The build reads `keystore.properties` first, then those env vars, then
the committed defaults. If the keystore file is missing, release APKs are
produced unsigned (`assembleRelease` still succeeds, files are not installable).

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| `Expected 10 APKs` in CI | A variant did not produce all ABIs; check the Gradle log above it |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | APK signed with a different key; uninstall first |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | Older APK than the installed one; install the newest release |
| `v*` tag release fails | Tag does not match `vMAJOR.MINOR.PATCH` |
