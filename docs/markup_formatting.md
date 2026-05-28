# Post Formatting Markup Reference

## Overview

This document describes how post formatting buttons work in the post composition form.
Each imageboard (chan) supports a different markup syntax. The app maps these to a set of
`MarkType` constants and provides a uniform button bar that adapts per board.

---

## MarkType Constants

Defined in `BoardModel.java` lines 125-143, stored in `BoardModel.markType`.

| Constant              | Value | Description |
|-----------------------|-------|-------------|
| `MARK_NOMARK`         | 0     | No markup supported |
| `MARK_WAKABAMARK`     | 1     | Legacy WakabaMark — `^H` backspace strike, no underline (iichan, yakuji) |
| `MARK_BBCODE`         | 2     | BBCode (used by vichan-based boards) |
| `MARK_4CHAN`          | 3     | 4chan-style markup |
| `MARK_NULL_CHAN`      | 4     | 0chan.hk style markup |
| `MARK_INFINITY`       | 5     | Infinity/lynxchan style markup |
| `MARK_WAKABAMARK_EXT` | 6     | Extended WakabaMark — `^^strike^^`, `[u]underline[/u]` (014chan, bulochka, 410chan) |

---

## Feature Availability Per MarkType

Defined in `PostFormMarkup.java` method `hasMarkupFeature()` (lines 35-45).

| Feature    | NOMARK | WAKABAMARK | WAKABAMARK_EXT | BBCODE | 4CHAN | NULL_CHAN | INFINITY |
|------------|:------:|:----------:|:--------------:|:------:|:-----:|:---------:|:--------:|
| **Bold**      | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **Italic**    | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **Underline** | ✗ | ✗ | ✓ | ✓ | ✓ | ✗ | ✓ |
| **Strike**    | ✗ | ✓ | ✓ | ✓ | ✗ | ✓ | ✓ |
| **Spoiler**   | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **Quote**     | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **Code**      | ✗ | ✓ | ✓ | ✗ | ✗ | ✓ | ✗ |

Key difference: `WAKABAMARK` uses `^H` backspaces for strike and has no underline;
`WAKABAMARK_EXT` uses `^^text^^` for strike and `[u]` for underline.

---

## Feature Constants

Defined in `PostFormMarkup.java` lines 26-33.

```java
public static final int FEATURE_BOLD      = 1;
public static final int FEATURE_ITALIC    = 2;
public static final int FEATURE_UNDERLINE = 3;
public static final int FEATURE_STRIKE    = 4;
public static final int FEATURE_SPOILER   = 5;
public static final int FEATURE_QUOTE     = 6;
public static final int FEATURE_CODE      = 7;
```

---

## Markup Insertion Reference

What each button actually inserts, per `PostFormMarkup.markup()` (lines 47-218).

Selected text is wrapped with the prefix/suffix. If the selection spans multiple lines,
the markers are applied per line (except for BBCODE-style tags which wrap the whole block).

### MARK_WAKABAMARK (lines 55-86) — legacy

Uses `^H` backspace characters for strikethrough (original WakabaMark spec). No underline.

| Button      | Insert pattern                | Cursor after |
|-------------|-------------------------------|-------------|
| **Bold**    | `**selected**`                | start + 2   |
| *Italic*    | `*selected*`                  | start + 1   |
| <u>Underline</u> | *(not available)*        | —           |
| ~~Strike~~  | `text^H^H^H^H` (backspaces)  | start       |
| Spoiler     | `%%selected%%`                | start + 2   |
| Quote       | `>selected` (per line)        | —           |
| Code        | `` `selected` ``              | start + 1   |

### MARK_WAKABAMARK_EXT (lines 87-116) — extended

Modern variant: `^^text^^` for strikethrough, `[u]text[/u]` for underline.

| Button      | Insert pattern                | Cursor after |
|-------------|-------------------------------|-------------|
| **Bold**    | `**selected**`                | start + 2   |
| *Italic*    | `*selected*`                  | start + 1   |
| <u>Underline</u> | `[u]selected[/u]`       | start + 3   |
| ~~Strike~~  | `^^selected^^`                | start + 2   |
| Spoiler     | `%%selected%%`                | start + 2   |
| Quote       | `>selected` (per line)        | —           |
| Code        | `` `selected` ``              | start + 1   |

### MARK_BBCODE (lines 117-142)

| Button      | Insert pattern                | Cursor after |
|-------------|-------------------------------|-------------|
| **Bold**    | `[b]selected[/b]`             | start + 3   |
| *Italic*    | `[i]selected[/i]`             | start + 3   |
| <u>Underline</u> | `[u]selected[/u]`       | start + 3   |
| ~~Strike~~  | `[s]selected[/s]`             | start + 3   |
| Spoiler     | `[spoiler]selected[/spoiler]` | start + 9   |
| Quote       | `>selected` (per line)        | —           |
| Code        | *(not available)*             | —           |

### MARK_4CHAN (lines 143-164)

| Button      | Insert pattern                | Cursor after |
|-------------|-------------------------------|-------------|
| **Bold**    | `**selected**`                | start + 2   |
| *Italic*    | `*selected*`                  | start + 1   |
| <u>Underline</u> | `__selected__`          | start + 2   |
| ~~Strike~~  | *(not available)*             | —           |
| Spoiler     | `[spoiler]selected[/spoiler]` | start + 9   |
| Quote       | `>selected` (per line)        | —           |
| Code        | *(not available)*             | —           |

### MARK_NULL_CHAN (lines 165-190)

| Button      | Insert pattern                | Cursor after |
|-------------|-------------------------------|-------------|
| **Bold**    | `**selected**`                | start + 2   |
| *Italic*    | `*selected*`                  | start + 1   |
| <u>Underline</u> | *(not available)*       | —           |
| ~~Strike~~  | `-selected-`                  | start + 1   |
| Spoiler     | `%%selected%%`                | start + 2   |
| Quote       | `>selected` (per line)        | —           |
| Code        | `` `selected` ``              | start + 1   |

### MARK_INFINITY (lines 191-217)

| Button      | Insert pattern                | Cursor after |
|-------------|-------------------------------|-------------|
| **Bold**    | `'''selected'''`              | start + 3   |
| *Italic*    | `''selected''`                | start + 2   |
| <u>Underline</u> | `__selected__`          | start + 2   |
| ~~Strike~~  | `~~selected~~`                | start + 2   |
| Spoiler     | `**selected**`                | start + 2   |
| Quote       | `>selected` (per line)        | —           |
| Code        | *(not available)*             | —           |

---

## Per-Chan Markup Support

### Actively supported chans (registered in `MainApplication.java`)

| Module                | Chan               | MarkType         | Source file |
|-----------------------|--------------------|------------------|-------------|
| `CirnoModule`         | iichan.hk          | MARK_WAKABAMARK     | `CirnoBoards.java:131` |
| `CirnoArchiveModule`  | ii.yakuji.moe      | MARK_WAKABAMARK     | `CirnoArchiveBoards.java:138` |
| `Chan410Module`       | 410chan.org        | MARK_WAKABAMARK_EXT | `Chan410Boards.java:107` |
| `ZeroFourModule`      | 014chan.org        | MARK_WAKABAMARK_EXT | `ZeroFourBoards.java:70` |
| `BulochkaModule`      | bulochka.org       | MARK_WAKABAMARK_EXT | `BulochkaBoards.java:70` |

### Other chans

| Module / Reader class        | MarkType         |
|------------------------------|------------------|
| `WakabaFactory`              | MARK_WAKABAMARK  |
| `AbstractKusabaModule`       | MARK_WAKABAMARK  |
| `DobroBoards`                | MARK_WAKABAMARK  |
| `NowereBoards`               | MARK_WAKABAMARK  |
| `AbstractVichanModule`       | MARK_BBCODE      |
| `ChaosBoards`                | MARK_BBCODE      |
| `MakabaJsonMapper`           | MARK_BBCODE      |
| `FourchanJsonMapper`         | MARK_4CHAN       |
| `NewNullchanJsonMapper`      | MARK_NULL_CHAN   |
| `AbstractLynxChanModule`     | MARK_INFINITY    |
| `AnonFmModule`               | MARK_NOMARK      |

See `PostFormActivity.java` lines 550-570 for how `hasMarkupFeature()` controls button
visibility.

---

## Board-Specific Markup Notes

### 014chan.org (`ZeroFourBoards`) — `MARK_WAKABAMARK_EXT`

- **Strikethrough:** `^^text^^`
- **Underline:** `[u]text[/u]`
- **Also accepts BBCode:** `[b]`, `[i]`, `[s]`, `[spoiler]`, `[code]`, `[u]`, `[aa]`, `[nofmt]`
- **Escape character:** `~` before markup suppresses processing (e.g. `~**text**` renders literally)
- **URL handling:** Links inside `%%...%%` get auto-linked; `~` before URL suppresses linking; tilde at end of URL is consumed
- **Lists:** Lines starting with `-`, `*`, `+`, `#` create lists
- **Reference:** https://014chan.org/news.php?p=mark

### bulochka.org (`BulochkaBoards`) — `MARK_WAKABAMARK_EXT`

- **Strikethrough:** `^^text^^`
- **Underline:** `[u]text[/u]`
- *Note: bulochka was forked from an older 410chan; FAQ was never updated. Actual engine supports `^^strike^^` and `[u]underline[/u]`.*
- **Reference:** https://bulochka.org/news.php?p=faq

### 410chan.org (`Chan410Boards`) — `MARK_WAKABAMARK_EXT`

- **Strikethrough:** `^^text^^`
- **Underline:** `[u]text[/u]`
- Same engine family, independently developed since bulochka fork.

### iichan.hk (`CirnoBoards`) / ii.yakuji.moe (`CirnoArchiveBoards`) — `MARK_WAKABAMARK` (legacy)

- **Strikethrough:** `^H` backspace characters (legacy WakabaMark)
- **Underline:** not supported
- These boards use the original WakabaMark implementation.

---

## Button Bar Layout

Defined in two layout XML files, selected by user preference:

- `res/layout/postform_layout.xml` — buttons below attachment area
- `res/layout/postform_layout_pinned_markup.xml` — buttons pinned at top

Both contain 7 `ImageButton`s inside `@id/postform_mark_layout`:

| ID                        | Drawable          | Feature        |
|---------------------------|-------------------|----------------|
| `postform_mark_quote`     | `mark_quote`      | Quote          |
| `postform_mark_bold`      | `mark_bold`       | Bold           |
| `postform_mark_italic`    | `mark_italic`     | Italic         |
| `postform_mark_underline` | `mark_underline`  | Underline      |
| `postform_mark_strike`    | `mark_strike`     | Strikethrough  |
| `postform_mark_spoiler`   | `mark_spoiler`    | Spoiler        |
| `postform_mark_code`      | `mark_code`       | Code           |

Button wiring in `PostFormActivity.java`:
- Click listeners assigned at line 506: all children of `mark_layout` get the Activity as `OnClickListener`
- Dispatch at lines 148-182: each button ID maps to the corresponding `PostFormMarkup.markup()` call
- Visibility at lines 550-570: buttons hidden based on `hasMarkupFeature()` results

---

## Source Files Reference

| File | Purpose |
|------|---------|
| `src/.../api/models/BoardModel.java` (lines 125-141) | `MARK_*` constants, `markType` field |
| `src/.../ui/posting/PostFormMarkup.java` | Feature constants, `hasMarkupFeature()`, `markup()` insert logic |
| `src/.../ui/posting/PostFormActivity.java` (lines 148-182, 506, 550-570) | Button wiring, click dispatch, visibility control |
| `res/layout/postform_layout.xml` | Button bar layout (default) |
| `res/layout/postform_layout_pinned_markup.xml` | Button bar layout (pinned mode) |
| `res/drawable-*/mark_*.png` | Button icons |

---

## How to Add or Change a Markup Syntax

To modify what a button inserts for a given `MarkType`:

1. If adding a **new feature** (not one of the 7 existing), add a `FEATURE_*` constant in
   `PostFormMarkup.java` and a button in the layout XML.
2. Update `hasMarkupFeature()` in `PostFormMarkup.java` to return `true` for your
   feature + mark type combination.
3. Add a `case FEATURE_*:` inside the relevant `markType` block in `markup()`.
4. Update this document.
