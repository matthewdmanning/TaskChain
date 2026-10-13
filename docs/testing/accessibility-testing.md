# Accessibility testing

Use this file for accessibility checks on a physical device.
Follow the safety rules and device setup in [device-testing.md](device-testing.md).

## Manual checks

Automation cannot cover these. Record each result as device evidence, apart from automated results.

- Drag reorder with TalkBack on.
- Swipe through each screen with TalkBack on. Confirm the reading order is logical.
- Confirm TalkBack announces runner state changes: task advance, overtime and completion.
- Confirm every icon-only control has a spoken label.
- Navigate the runner and builder with Switch Access or a keyboard. Confirm the focus order is logical.
- Confirm no state relies on color alone. Check overtime and behind-schedule states.
- Turn on the reduced-motion setting. Confirm the runner and builder remain usable without motion.

## Screenshot settings

- Capture the main screens at font scale 1.5.
- Use the screenshot tooling in [automated-physical-device-tests.md](automated-physical-device-tests.md#screenshot-tests).
- Record the font scale with each reference image.
