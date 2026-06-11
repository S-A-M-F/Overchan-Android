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

- **VERSION** — `branch.major.minor-samf-build` (e.g. `1.9.3-samf-1`, `1.9.10-samf-1`)
- **Build bumps**: If issues are found post-release, the fix release increments the
  build number (`samf-2`, `samf-3`, …). The previous APK is removed from the
  GitHub Releases page, but the git tag and commit remain in history.
- **All components** (`branch`, `major`, `minor`, `build`) are numeric and not
  restricted to single digits — no implied base-10 carries between them.
- **QUOTE** — a Lovecraft quote in the target language, from the established literature translation.
  Use `"` for English, `«»` for Russian/Ukrainian, `„“` for German.
- **Date** is not included (it was previously but removed for consistency).
- **Items** are prefixed with `\n- ` and listed in plain text (no `### Added/Changed/Fixed` grouping).
- No trailing punctuation on items.

## Scope

The in-app changelog string (`pref_changelog`) only lists changes made in this
release. Full history is maintained in commit messages and version tags.

## Quote Sources

| Language | Source |
|----------|--------|
| English | Original Lovecraft, *The Whisperer in Darkness* (1931) |
| Russian | Алякринский / Эрлихман / Шокин / Попов, Flibusta.su |
| German | Rudolf Hermstein, Suhrkamp Taschenbuch 1997 (ISBN 3518392611) |
| Ukrainian | Остап Українець / Катерина Дудка, coollib.cc |

- Translations should be taken from any available literature source.
  Ebook or scanned versions of physically printed books, magazines, etc. are preferred.
- The table above lists known sources but is not exhaustive — any established
  publication translation is acceptable.

## Example

```xml
<string name="pref_changelog">1.9.1-samf-1: "Some day, the piecing together of dissociated knowledge..."\n- WebP and JFIF support for posting attachments\n- Sage detection on 410chan</string>
```
