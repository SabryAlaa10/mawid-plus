# Design System Strategy: The Clinical Precision Framework

## 1. Overview & Creative North Star
**Creative North Star: "The Curated Diagnostic"**

In the medical space, "trustworthy" often defaults to "boring." This design system rejects the clinical sterility of the past in favor of a **High-End Editorial** approach. We are moving away from the "boxed-in" SaaS template and toward an experience that feels like a premium medical journal. 

We achieve this through **Intentional Asymmetry** and **Tonal Depth**. By utilizing wide margins, varying column widths, and overlapping header elements, we break the rigid grid. The goal is to make the "Mawid+ Doctor Portal" feel less like a database and more like a sophisticated workspace that anticipates a physician's needs.

---

## 2. Colors & Surface Philosophy

Our palette transitions from the authoritative Navy of the clinic to the airy, high-contrast whites of a modern lab.

### Tonal Hierarchy
*   **Primary:** `#005bbf` (Core Brand) & `#1a73e8` (Action/Interaction).
*   **Surface Foundation:** The `background` (`#f6fafe`) is our canvas. 
*   **The "No-Line" Rule:** **1px solid borders are strictly prohibited for sectioning.** We define boundaries through background shifts. A `surface-container-low` section sitting on a `surface` background creates a natural edge without visual clutter.
*   **Surface Hierarchy & Nesting:** Treat the UI as physical layers of "Fine Paper."
    *   **Level 0 (Base):** `surface` (`#f6fafe`)
    *   **Level 1 (Sections):** `surface-container-low` (`#f0f4f8`)
    *   **Level 2 (Active Cards):** `surface-container-lowest` (`#ffffff`)
*   **The "Glass & Gradient" Rule:** For floating modals or "Today's Schedule" overlays, use Glassmorphism. Apply `surface` with 80% opacity and a `backdrop-blur` of 12px.
*   **Signature Textures:** Use a subtle linear gradient (Top-Left to Bottom-Right) from `primary` to `primary-container` for high-value CTAs (e.g., "Start Consultation"). This adds "soul" and depth that flat hex codes lack.

---

## 3. Typography: The Editorial Scale

We use a dual-typeface system to balance clinical authority with high-density legibility.

*   **The Display Choice (Manrope):** Used for `display` and `headline` scales. Its geometric but warm nature provides a modern, "Editorial" feel to patient names and dashboard stats.
*   **The Utility Choice (Inter):** Used for `title`, `body`, and `label`. Inter’s tall x-height ensures that complex medical data remains legible even at `label-sm` (0.6875rem).

**Creative Direction:** Use high contrast in scale. A `display-md` patient count next to a `label-md` descriptive tag creates a visual rhythm that guides the eye better than uniform sizing.

---

## 4. Elevation & Depth: Tonal Layering

We do not use shadows to create "pop"; we use them to create "atmosphere."

*   **The Layering Principle:** Stack `surface-container-lowest` cards on top of `surface-container-high` backgrounds. The contrast in lightness provides all the "lift" required.
*   **Ambient Shadows:** For floating elements (e.g., dropdowns), use a custom shadow: 
    *   `box-shadow: 0 10px 30px -5px rgba(23, 28, 31, 0.05);` 
    *   Note: The shadow is tinted with the `on-surface` color, not pure black.
*   **The Ghost Border Fallback:** If a patient table requires a separator, use the `outline-variant` token at **15% opacity**. It should be felt, not seen.

---

## 5. Components

### Buttons
*   **Primary:** Gradient fill (`primary` to `primary-container`), `rounded-DEFAULT` (8px). 
*   **Secondary:** `surface-container-high` background with `on-secondary-container` text. No border.
*   **Tertiary:** Pure ghost style. `on-surface-variant` text.

### High-Density Data Tables
*   **Forbid Dividers:** Use `0.5rem` of vertical white space between rows. 
*   **Zebra-Stripe Alternative:** Use a `surface-container-low` background on every second row to maintain the "No-Line" rule.
*   **Status Badges:** Use `error_container`, `secondary_container`, and a custom success-mint. Keep text in the "On-Container" variant for maximum accessibility.

### Cards & Containers
*   **Styling:** No borders. Use `rounded-lg` (1rem) for main dashboard widgets.
*   **Content:** Group related data using the Spacing Scale (e.g., `spacing-4` between label and value).

### Input Fields
*   **State:** Default state uses `surface-container-highest` as a subtle fill. On focus, transition to an `outline` of `primary` with a 2px "Soft Glow" (a spread shadow using the primary color at 10% opacity).

---

## 6. Do’s and Don'ts

### Do:
*   **DO** use whitespace as a functional tool. If the data is dense, increase the page margins to `spacing-12` or `spacing-16` to let the UI breathe.
*   **DO** use "surface-tint" subtly on large icons to tie them into the brand color without making them interactive.
*   **DO** use `tertiary` (Orange/Warm) for life-critical alerts; it contrasts sharply against the medical Blue/Navy.

### Don't:
*   **DON'T** use pure black (#000000) for text. Always use `on-surface` (#171C1F) for a softer, high-end feel.
*   **DON'T** use 1px borders to separate the sidebar from the main content. Use the color shift from `inverse-surface` (Navy) to `background` (Light Grey/Blue).
*   **DON'T** crowd the "Status Badges." Give them `rounded-full` corners and generous horizontal padding (`spacing-3`) to make them feel like "pills" rather than "tags."