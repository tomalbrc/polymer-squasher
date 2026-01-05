# Polymer PackSquash

Automatic resource pack optimization for Polymer using [PackSquash](https://packsquash.aylas.org/).

## Features

- Automatically compresses Polymer-generated resource packs
- Smart caching - only reprocesses when files change
- Configurable file hashing and ignore patterns
- Seamless integration with Polymer's build pipeline

## Installation

1. Download [PackSquash](https://github.com/ComunidadAylas/PackSquash/releases) for your platform
2. Place the binary in your server directory:
   - **Linux/Mac**: `polymer/packsquash`
   - **Windows**: `polymer/packsquash.exe`
3. Start your server

The mod will automatically generate `polymer/packsquash.toml` on first run.

## Configuration

Located at `config/polymer-squasher.json`:
```json
{
  "enabled": true,
  "log-packsquash": false,
  "packsquash-path": "polymer/packsquash",
  "packsquash-toml-path": "polymer/packsquash.toml",
  "ignore-hash-paths": [
    "polymer-credits.txt"
  ],
  "force-size-based-hash": false
}
```

### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | boolean | `true` | Enable PackSquash integration |
| `log-packsquash` | boolean | `false` | Output PackSquash logs to console |
| `packsquash-path` | string | `"polymer/packsquash"` | Path to PackSquash binary |
| `packsquash-toml-path` | string | `"polymer/packsquash.toml"` | Path to PackSquash config |
| `ignore-hash-paths` | array | `["polymer-credits.txt"]` | Path prefixes to exclude from hashing |
| `force-size-based-hash` | boolean | `false` | Use file size for change detection (faster, less accurate) |

<details>
<summary>About <code>ignore-hash-paths</code></summary>

Uses prefix matching to exclude files from hash checking:
- `"polymer-credits.txt"` - ignores only this specific file
- `"assets/mymod/"` - ignores all files in this directory  
- `"licenses/"` - ignores all license directories

</details>

## How It Works

The mod maintains a hash cache of your resource pack files in `polymer/hashes.json`.

**On server start:**
1. Checks if resource pack files have changed since last run
2. If unchanged, serves the cached optimized pack
3. If changed, runs PackSquash to create a new optimized version
4. Updates the hash cache

This ensures optimal performance - PackSquash only runs when necessary, while your server always delivers an optimized resource pack.

## Troubleshooting

**PackSquash not running?**
- Verify the binary path in your config
- Ensure the binary is executable (`chmod +x polymer/packsquash` on Linux/Mac)
- Check logs with `"log-packsquash": true`

**Resource pack not updating?**
- Delete `polymer/hashes.json` to force a rebuild
- Check `ignore-hash-paths` isn't excluding changed files