# Design System Strategy: Clinical Editorial

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Digital Sanctuary."** 

In a medical context, users often feel anxiety or urgency. We move beyond the "utilitarian grid" of standard booking apps to create an experience that feels like a premium, quiet clinic. We achieve this through **Editorial Softness**: a layout style that prioritizes breathing room, high-contrast typographic scales for readability, and intentional asymmetry to guide the eye through complex medical data. This is not just a tool; it is a calming, authoritative presence.

## 2. Color & Atmospheric Depth
We use color not just for branding, but to build a physical sense of space.

*   **The "No-Line" Rule:** 1px solid borders are strictly prohibited for sectioning content. Boundaries must be defined through background color shifts. For example, a `surface-container-low` card sitting on a `surface` background provides all the definition needed without the "noise" of a stroke.
*   **Surface Hierarchy & Nesting:** Treat the UI as stacked sheets of fine, semi-translucent paper. 
    *   **Level 0 (Base):** `surface` (#F8F9FA) for the main background.
    *   **Level 1 (Sections):** `surface-container-low` (#F3F4F5) for grouping content areas.
    *   **Level 2 (Interaction):** `surface-container-lowest` (#FFFFFF) for primary cards and input fields.
*   **The Glass & Gradient Rule:** To signify importance, use **Glassmorphism**. A floating "Book Now" bar should use `surface` with 80% opacity and a 20px backdrop blur. For Hero sections, use a subtle linear gradient from `primary` (#005bbf) to `primary-container` (#1a73e8) at a 135-degree angle to add "soul" and depth.
*   **Signature Tones:**
    *   **Primary (#005bbf):** Use for authoritative actions.
    *   **Secondary (#006b5f):** Reserved for "Wellness" and "Success" states—confirming appointments and health milestones.

## 3. Typography: Editorial Authority
The typography system utilizes two distinct families to balance human warmth with clinical precision.

*   **The Display & Headline (Manrope):** This geometric sans-serif provides a modern, high-end feel. Use `display-lg` (3.5rem) and `headline-md` (1.75rem) with generous leading to create an "Editorial" look for page titles and doctor names.
*   **The Body & Label (Public Sans):** This is our "workhorse." Chosen for its exceptional legibility in Arabic scripts (RTL). 
    *   **Medical Data:** Use `title-md` (1.125rem) for vitals or appointment times to ensure they are the first thing a user sees.
    *   **Hierarchy through Scale:** Use `label-sm` (#414754) in all-caps or high-tracking for metadata (e.g., "SPECIALTY") to contrast against the bold `headline-sm` of the doctor's name.

## 4. Elevation & Depth: Tonal Layering
We reject the heavy drop-shadows of the early web. Depth in this system is organic.

*   **The Layering Principle:** Instead of shadows, use the **Surface Scale**. Place a `surface-container-highest` element behind a `surface-container-lowest` card to create a natural "lift."
*   **Ambient Shadows:** When an element must float (like a Floating Action Button), use a shadow tinted with the `on-surface` color.
    *   *Spec:* `offset: 0, 8px; blur: 24px; color: rgba(25, 28, 29, 0.06);`
*   **The "Ghost Border" Fallback:** If a container sits on an identical color background, use a "Ghost Border": `outline-variant` at 15% opacity.
*   **Glassmorphism:** Navigation bars and top app bars should utilize `surface-bright` with a 70% opacity and `backdrop-filter: blur(16px)`. This keeps the user grounded in the content they just scrolled past.

## 5. Components

### Buttons & Interaction
*   **Primary Action:** `primary` background with `on-primary` text. Use `xl` (3rem) corner radius. These should feel like smooth river stones.
*   **Secondary/Filter:** `secondary-container` background with `on-secondary-container` text. Use `md` (1.5rem) corner radius.
*   **The "Haptic" State:** On press, buttons should scale down slightly (98%) rather than just changing color, providing a premium tactile feel.

### Input Fields & Search
*   **The Floating Input:** Forbid traditional "boxed" inputs. Use `surface-container-lowest` with a `xl` corner radius and a `1.5rem` horizontal padding. The label should use `label-md` and sit 8px above the field, never inside it, to maintain a clean editorial look.

### Medical Cards & Lists
*   **No Dividers:** The use of horizontal 1px lines to separate list items is forbidden.
*   **Spacing as Separator:** Use the `spacing-6` (1.5rem) or `spacing-8` (2rem) tokens to create "Visual Silos." 
*   **Asymmetric Cards:** For doctor profiles, use an `xl` (3rem) radius on the top-right and bottom-left corners, with `DEFAULT` (1rem) on the others. This intentional asymmetry breaks the "template" feel and looks custom-designed.

### Appointment Timeline
*   Use a vertical `primary-fixed` line (2px) with `surface-container-highest` nodes. The "Active" appointment should be a `primary` node with a soft `primary-fixed` outer glow.

## 6. Do’s and Don’ts

### Do
*   **DO** use white space as a functional tool. If a screen feels crowded, increase the spacing to the next token (e.g., move from `8` to `10`).
*   **DO** ensure RTL (Arabic) alignment is perfect. Typography should be right-aligned, but icons (like "Back") must be mirrored for the RTL context.
*   **DO** use `surface-variant` for "Disabled" states to maintain the soft tonal palette.

### Don't
*   **DON'T** use pure black (#000000) for text. Always use `on-surface` (#191c1d) to keep the contrast high but the "vibe" soft.
*   **DON'T** use 100% opaque borders. They create "visual cages" that trap the user's eye.
*   **DON'T** use standard Material 2 "Card Shadows." If the elevation is not achieved through color shifts, the shadow must be almost invisible.