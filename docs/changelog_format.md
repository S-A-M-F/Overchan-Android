# Changelog Format

Changelog entries are stored as `pref_changelog` string resources in 4 locale files:

- `res/values/strings.xml` — English
- `res/values-ru/strings.xml` — Russian
- `res/values-de/strings.xml` — German
- `res/values-uk/strings.xml` — Ukrainian

## Format

```
<string name="pref_changelog">VERSION: "QUOTE"\n- ITEM\n- ITEM\n- ITEM</string>
```

- **VERSION** — e.g. `1.9.1-samf-1`
- **QUOTE** — a Lovecraft quote in the target language, from the established literature translation.
  Use `"` for English, `«»` for Russian/Ukrainian, `„“` for German.
- **Date** is not included (it was previously but removed for consistency).
- **Items** are prefixed with `\n- ` and listed in plain text (no `### Added/Changed/Fixed` grouping).
- No trailing punctuation on items.

## Quote Sources

| Language | Source |
|----------|--------|
| English | Original Lovecraft, *The Whisperer in Darkness* (1931) |
| Russian | Алякринский / Эрлихман / Шокин / Попов, Flibusta.su |
| German | Rudolf Hermstein, Suhrkamp Taschenbuch 1997 (ISBN 3518392611) |
| Ukrainian | Остап Українець / Катерина Дудка, coollib.cc |

## Example

```xml
<string name="pref_changelog">1.9.1-samf-1: "Some day, the piecing together of dissociated knowledge..."\n- WebP and JFIF support for posting attachments\n- Sage detection on 410chan</string>
```
