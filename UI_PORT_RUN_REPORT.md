# FollowThru — UI port run report (coral/cream, native Compose)

Branch: `followthru-pergoal-reminders` (this run built on top of the per-goal-reminders work, same branch, clean tree at start except gradle build-cache binaries which were discarded).

Goal: bring the app's UI up to the approved Loveable prototype natively in Jetpack Compose / Material 3, rendered in the brand tokens, adaptive for phone + tablet, with the two deliberate deviations — (1) colours tuned to pass WCAG AA, (2) typography stays Poppins. Privacy unchanged: local-only, **no new permissions**, no network/analytics/location.

## What shipped (per commit)

1. **Foundation: coral/cream brand tokens.** Rewrote `Color.kt`, `Theme.kt`, `AppColors.kt`. Replaced TrackRed-on-white with warm coral on cream. `surfaceVariant == surface` so cards read as white-on-cream (light) / lifted surface (dark); hairline separation carried by `outline` (`AppColors.Border`). `onPrimary` flips per mode (white label in light, dark label in dark) because both directions are AA on coral. New tokens: `CoralTint`, `Gold`/`GoldSurface`, `Border`. `BrandAccentText` is now coral in both modes.
2. **Lucide icons.** 12 stroke-only, round-cap vector drawables at Lucide's 2px weight, opaque so `Icon(tint=)` recolors via SrcIn: flame, check-circle, bell, search, plus, chevron-right, target, edit-3, rotate-ccw, clock, arrow-left, settings. chevron-right / arrow-left auto-mirror for RTL.
3. **Day-circle reminder chips.** Shared `ReminderDayChips` restyled to outlined day circles (coral fill + white/dark label when selected). 48dp touch target around a 42dp circle; responsive single-row / 4+3 wrap retained. Used by Goal Detail, New Goal, Settings.
4. **Onboarding redesign + version bump 95→96.** Cream layout: persistent FollowThru header + Skip, two-segment progress bar, hairline divider. Slide 1 = "Following through is the hard part." + privacy paragraph + App lock (device PIN) card with a coral checkbox. Slide 2 = "How it works" three outlined cards (target / edit-3 / check-circle in pale-coral icon circles) + coral "See an example" expander revealing the health example on a pale-coral card; Back + Get started. Version bumped so already-onboarded installs re-see it once (no data reset).
5. **Home.** Goal cards = white surface + hairline border, coral number badge, goal title, the "When …, I will …" intention as the supporting line (falls back to check-in count when no plan yet), coral bell when a per-goal reminder is set. Summary = bordered card with gold flame + coral check-circle and a chevron into Stats. Search field gains a border + Lucide search glyph. FAB/gear use Lucide plus/settings. Drag reorder + TalkBack arrows preserved. `ListViewModel` derives each goal's intention from its most recent check-in.
6. **Goal Detail.** Single scroller with labelled sections: read-only **YOUR INTENTION** card, two-button **follow-through** pair ("I followed through" coral + "Missed it? Adjust" outlined rotate-ccw that reverses the mark), **REMINDER** card whose toggle reads "Anchor a reminder to this intention", **CHECK-INS** list. `GoalReminderControls` gained a `toggleLabel` parameter.
7. **Stats + Your FollowThrus.** Stats → "Your progress" with back + gear + intro line, the gold **Follow-Through Streak** card (the app's only gold surface), white bordered stat tiles (Check-Ins: Current/Longest/Total; Follow-Through: Total/This week/This month), bordered "Your FollowThrus" chevron row. Your FollowThrus gains the gear; record cards become white-on-cream with a muted goal-name+date row, the moment in bold, the note in italic. Both screens wire `onSettingsClick` → `ROUTE_SETTINGS`.
8. **New Goal + adaptive.** Action-plan step shows the prototype's two labelled inputs ("When … (the moment or situation)" + "I will … (what you'll do)") but stores the single concatenated `implementationIntention` string (round-tripped by `splitIntention`/`joinIntention`); the optional "Remind me for this" reminder stays on this step. Step inputs get persistent visible borders. Adaptive: `MainActivity` caps content at 600dp and centers it on cream, so phones (< 600dp) are unchanged and tablets see the identical single-column design centered — no tablet-specific layout, no two-pane.
9. **Settings + Check-in detail + Customize questions.** Section dividers switched to the `AppColors.Border` hairline (the now-white `surfaceVariant` made them invisible on cream); Settings back/chevrons use Lucide glyphs. Appearance Light/Dark/System selector and coral switches/radios unchanged.

## Brand tokens & accessibility (WCAG AA, both themes)

AA targets: 4.5:1 normal text, 3.0:1 large/bold and UI components. Approximate measured ratios:

| Pair | Light | Dark |
|---|---|---|
| Body text on page (`text` on `bg`) | #2A2622 on #FBF6EF ≈ 16:1 | #F3ECE4 on #1E1B19 ≈ 15:1 |
| Muted text on page | #6E645D on #FBF6EF ≈ 5.2:1 | #B5AAA1 on #1E1B19 ≈ 7.5:1 |
| Muted text on card (white / #2A2622) | ≈ 5.8:1 | ≈ 6.5:1 |
| Coral text/link on page | #B5402C on #FBF6EF ≈ 5.2:1 | #E8775F on #1E1B19 ≈ 5.9:1 |
| Button label on coral fill | white on #B5402C ≈ 5.6:1 | #1E1B19 on #E8775F ≈ 5.9:1 |
| Selected day-circle label on coral | white ≈ 5.6:1 | dark ≈ 5.9:1 |
| Gold streak card text (number) | #2A2622 on #F8EAC7 ≈ 14:1 | #F3ECE4 on #3A3320 ≈ 11:1 |
| Streak flame icon (`goldIcon`) | #9C7A1A on #F8EAC7 ≈ 3.4:1 | #D9B65E on #3A3320 ≈ 6.5:1 |
| Destructive | #C0392B on #FBF6EF ≈ 5.0:1 | #FF8A80 on #2A2622 ≈ 6.6:1 |

- The bright prototype coral `#E26155` (≈ 3.3:1) is **not** used for any text in either theme.
- **Gold flame on the gold streak card** uses a dedicated darker `goldIcon` (#9C7A1A light, GoldDark #D9B65E dark) so it meets the 3:1 UI-component bar in both themes (≈ 3.4:1 / ≈ 6.5:1) — the plain `gold` token (#C9A24A) was only ~2:1 on the pale-gold surface. The streak value is still carried by the large dark number (≈ 14:1) and the card's merged `contentDescription`; the flame is now a real icon, not a decorative exception. `gold`/`goldSurface` and all other gold usage are unchanged.
- ≥48dp targets, TalkBack labels on toggles, day circles, FAB, follow-through buttons, and switches; selected/heading semantics preserved; reduced-motion and font-scaling paths untouched.
- `lintDebug`: **0 error-severity issues**, no Contrast/TouchTarget/ContentDescription findings introduced.

## Verification

- `compileDebugKotlin` ✅ · `assembleDebug` ✅ · `testDebugUnitTest` ✅ · `lintDebug` ✅ (0 errors).
- **v26→v27 migration test** (`Migration26To27Test`) ✅ — re-run with `--rerun-tasks`, BUILD SUCCESSFUL.
- **Privacy grep** ✅ — no INTERNET / ACCESS_*_LOCATION / gms / firebase / analytics / okhttp / Retrofit in `app/src/main`.
- **Merged manifest** ✅ — permissions unchanged from before this run: POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, USE_BIOMETRIC, USE_FINGERPRINT (+ the androidx-injected DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, not from this run). **No new permission gained.**
- Per-goal reminder persistence/scheduling was **not** rebuilt — only the UI was wired to the existing `GoalReminderScheduler` (SharedPreferences keyed by goalId, single intention string, shared exact-alarm + permission gate).

## Still needs a real-device pass

This run verified build/test/lint/migration/privacy on CI only. The following need an on-device check:

- **Visual + dark-mode pass on every screen** (Onboarding, Home, Goal Detail, New Goal, Stats, Your FollowThrus, Settings, Check-in flow/detail) in Light / Dark / System — confirm coral/cream/gold render as intended and the Lucide glyphs match stroke weight/size at real DPI.
- **TalkBack pass** on the new/changed controls: onboarding app-lock card, day circles, Home goal-card bell/number, the two follow-through buttons, the two-field action plan, the gear on Stats/Your FollowThrus.
- **Tablet / large-width pass**: confirm the 600dp centered column reads as the identical phone design with cream gutters.
- **Per-goal reminder set→fires path**: toggle a reminder on a goal (Goal Detail and the New Goal action-plan step), confirm the alarm schedules and the notification fires with the goal's intention text, survives reboot, and is cleared when the goal is deleted.
- **Two-field action-plan round-trip**: create a goal via the two inputs, reopen, confirm the stored sentence splits back into When/I-will correctly and the reminder body matches.

## Deviations from the prototype (intentional)

1. **Colours** tuned to pass AA (coral `#B5402C` light / `#E8775F` dark, etc.) instead of the prototype's bright `#E26155`.
2. **Typography** stays Poppins (brand font), not the prototype's sans.

Everything else aims to be visually indistinguishable from the prototype, rendered natively.
