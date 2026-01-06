# Polymer PackSquash Integration

This mod integrates [PackSquash](https://packsquash.dev) with Polymer’s resource pack generation to automatically optimize generated resource packs so you don't have to manually compress after changes.


## Setup

- Get the latest [PackSquash release](https://github.com/ComunidadAylas/PackSquash/releases).
- Place the binary here:

  ```
  polymer/packsquash
  ```

- The mod will automatically create a `packsquash.toml` in the `polymer` folder.

These are the default paths the mod will use. You can adjust them in the configuration file `config/polymer-squasher.json`

### Default config
```json
{
  "enabled": true,
  "log-packsquash": false,
  "log-hash-mismatch": false,
  "force-size-based-hash": false,
  "cleanup": false,
  "ignore-hash-paths": [
    "polymer-credits.txt"
  ],
  "packsquash-path": "polymer/packsquash",
  "packsquash-toml-path": "polymer/packsquash.toml",
  "hash-file-path": "polymer/hashes.json",
  "resource-pack-directory": "polymer/pack",
  "minified-zip-path": "polymer/resource_pack.min.zip"
}
```

## Config options

- **enabled** — turn the integration on/off.
- **log-packsquash** — print PackSquash output to the log.
- **log-hash-mismatch** — log when a file’s hash changes (useful for debugging).
- **packsquash-path** — path to the PackSquash binary.
- **packsquash-toml-path** — path to the PackSquash TOML config file.
- **ignore-hash-paths** — list of path **prefixes** to ignore hash mismatches when hashing.
- **force-size-based-hash** — if `true`, the mod will use file size instead of a full hash (faster, less precise)
- **cleanup** - cleans up old files in the cached pack folder

---

## How it works

When enabled, the mod will hash the generated resource pack files. The hashes are written to `polymer/hashes.json`.

If nothing has changed since the last run, PackSquash is skipped.

If changes are detected, the mod will run PackSquash after Polymer finishes generating the resource pack.

If the process completes successfully, the optimized version is used instead.
