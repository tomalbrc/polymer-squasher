# Polymer PackSquash Integration

This mod integrates [PackSquash](https://packsquash.dev) with Polymer’s resource pack generation to automatically optimize generated resource packs so you don't have to manually compress after changes.

---

## Setup

- Get the latest [PackSquash release](https://github.com/ComunidadAylas/PackSquash/releases).
- Place the binary here:

  ```
  polymer/packsquash
  ```

- Create or get [an example](https://gist.github.com/Boy0000/92149d2704b6086473fccb4d771c42b4) `packsquash.toml` file.
- Place it in the same directory:

  ```
  polymer/packsquash.toml
  ```

These are the default paths the mod will use. You can adjust them in the configuration file `config/polymer-optimizer.json`:

```json
{
  "enabled": true,
  "log-packsquash": false,
  "packsquash-path": "polymer/packsquash",
  "packsquash-toml-path": "polymer/packsquash.toml"
}
```

---

## How it works

When enabled, the mod will run PackSquash after Polymer finishes generating the resource pack. If the process completes successfully, the optimized version is used instead.

If PackSquash encounters an error, it will print detailed output indicating which file caused the issue and why. If debug logging is enabled, you'll also see information about successfully processed files.
