---
name: Bzcom Enterprise
colors:
  surface: '#faf8ff'
  surface-dim: '#d9d9e4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3fd'
  surface-container: '#ededf8'
  surface-container-high: '#e7e7f2'
  surface-container-highest: '#e1e2ec'
  on-surface: '#191b23'
  on-surface-variant: '#434654'
  inverse-surface: '#2e3038'
  inverse-on-surface: '#f0f0fb'
  outline: '#737685'
  outline-variant: '#c3c6d6'
  surface-tint: '#0c56d0'
  primary: '#003d9b'
  on-primary: '#ffffff'
  primary-container: '#0052cc'
  on-primary-container: '#c4d2ff'
  inverse-primary: '#b2c5ff'
  secondary: '#535f73'
  on-secondary: '#ffffff'
  secondary-container: '#d4e0f8'
  on-secondary-container: '#576377'
  tertiary: '#7b2600'
  on-tertiary: '#ffffff'
  tertiary-container: '#a33500'
  on-tertiary-container: '#ffc6b2'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae2ff'
  primary-fixed-dim: '#b2c5ff'
  on-primary-fixed: '#001848'
  on-primary-fixed-variant: '#0040a2'
  secondary-fixed: '#d7e3fb'
  secondary-fixed-dim: '#bbc7de'
  on-secondary-fixed: '#101c2d'
  on-secondary-fixed-variant: '#3b475b'
  tertiary-fixed: '#ffdbcf'
  tertiary-fixed-dim: '#ffb59b'
  on-tertiary-fixed: '#380d00'
  on-tertiary-fixed-variant: '#812800'
  background: '#faf8ff'
  on-background: '#191b23'
  surface-variant: '#e1e2ec'
  success-green: '#36B37E'
  warning-orange: '#FFAB00'
  error-red: '#FF5630'
  info-blue: '#00B8D9'
  surface-gray: '#F4F5F7'
  border-subtle: '#DFE1E6'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  code-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 4px
  container-margin: 24px
  gutter: 16px
  card-padding: 20px
  input-height: 40px
---

## Brand & Style

The design system is engineered for a high-performance Enterprise CRM environment where efficiency, reliability, and technical precision are paramount. The brand personality is authoritative yet approachable, positioning itself as a silent, powerful partner in customer relationship management.

### Design Style: Modern Minimalist
This system utilizes a **Minimalist** and **Corporate Modern** aesthetic. It prioritizes data density and readability through:
*   **Structured Information Hierarchy:** Using clear typographic scales and functional color application to direct attention.
*   **Card-Based Architectures:** Segmenting complex data sets into digestible modules.
*   **Functional Utility:** Every visual element serves a purpose—reducing "visual noise" to minimize cognitive load for power users who spend hours within the interface.
*   **Technical Rigor:** Borrowing from developer tool aesthetics (clean lines, subtle borders, and precise alignment) to evoke the reliability of the underlying modular monolith architecture.

## Colors

The palette is anchored by **Bzcom Blue**, a deep, professional blue that signifies stability and trust. 

### Palette Application
*   **Primary:** Used for main actions, active states, and brand identification.
*   **Semantic Colors:** These are strictly mapped to the system's state machine. **Success Green** for `DONE` or HTTP 200/201; **Warning Orange** for `IN_PROGRESS` or high-priority items; **Error Red** for `BUG` categories, high-priority alerts, or HTTP 400/500 errors.
*   **Grayscale:** A "Clean Grayscale" strategy is used. Backgrounds utilize `surface-gray` to provide a subtle contrast against white cards. Borders use `border-subtle` to define structure without creating visual clutter.
*   **Neutral:** A range of slate grays (Secondary) is used for secondary text and icons to maintain a professional, calm environment.

## Typography

The typography system relies on **Inter** for its exceptional legibility at small sizes and its neutral, professional character.

### Functional Roles
*   **Headlines:** Used for page titles and dashboard sections. `headline-xl` is reserved for high-level analytics overviews.
*   **Body:** `body-md` (14px) is the primary workhorse for the CRM, optimized for data tables and multi-step forms to maximize information density without sacrificing readability.
*   **Labels:** All caps with slight letter spacing are used for table headers and form labels to differentiate them from user-generated content.
*   **Technical Data:** For IDs, API paths, and status keys (e.g., `ROLE_ADMIN`), a monospaced font (JetBrains Mono) is used at a small scale to reflect the system's technical foundations.

## Layout & Spacing

This design system uses a **12-column fluid grid** for dashboard views and a **centered fixed-width container** (max 1200px) for settings and form-heavy pages.

### Spacing Philosophy
The system follows a strict 4px baseline grid. 
*   **Dashboards:** Use a 16px gutter between cards to create a tight, efficient feel.
*   **Responsive Behavior:** 
    *   **Desktop:** 12 columns, 24px margins.
    *   **Tablet:** 8 columns, 16px margins. 
    *   **Mobile:** 4 columns, 16px margins. Content reflows vertically, and data tables transition into "List Cards" to maintain usability on small screens.

## Elevation & Depth

To maintain a minimalist enterprise aesthetic, depth is communicated through **Tonal Layering** and **Low-Contrast Outlines** rather than heavy shadows.

### Elevation Levels
1.  **Level 0 (Background):** `surface-gray` (#F4F5F7). The canvas for the application.
2.  **Level 1 (Cards/Surface):** Pure White (#FFFFFF). All primary content containers sit here. They are defined by a 1px `border-subtle` and a very soft, high-diffusion shadow (4px blur, 2% opacity) to provide a "lift" from the background.
3.  **Level 2 (Modals/Overlays):** White (#FFFFFF) with a more pronounced shadow and a 40% opacity black backdrop blur to focus the user's attention during multi-step processes.

## Shapes

The shape language is **Soft** and disciplined. A 4px (0.25rem) standard radius is applied to most UI elements.

*   **Standard (4px):** Applied to buttons, input fields, and status badges. This provides a modern touch while maintaining a serious, structured appearance.
*   **Large (8px):** Applied to cards and containers to create a distinct visual boundary.
*   **Pill:** Exclusively used for status badges (e.g., `DONE`, `BUG`) to make them instantly recognizable as non-interactive status indicators.

## Components

### Data Tables
Tables are the heart of the CRM. Use a "Zebra" row style with a subtle hover state (#F4F5F7). Table headers should use `label-md` and be "sticky" during scroll. Status columns must use the defined semantic badges.

### Multi-step Forms
Forms should be broken into logical sections using "Progress Steppers" at the top. Each step should be housed within a Level 1 Card. Primary actions (Next/Submit) should be right-aligned, while destructive or back actions are left-aligned or ghost-styled.

### Status Badges
High-contrast text on a low-saturation background of the same hue (e.g., Dark Green text on Light Green background) for maximum readability.

### Cards & Analytics
Dashboard cards should have a consistent header style with a 1px bottom border. Sparklines or mini-charts within cards should use the `primary-color` or semantic colors to show trends.

### Inputs & Notifications
Input fields use a 1px `border-subtle`, turning `primary-color` on focus. Notifications (Toasts) should appear in the top-right, color-coded by the semantic system (Success, Error, Info) to provide immediate feedback on API transactions.