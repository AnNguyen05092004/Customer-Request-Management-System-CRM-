# Design Specification: Bzcom CRM

## 1. Visual Identity
- **Primary Color:** #0052CC (Bzcom Blue) - Represents trust and technology.
- **Success:** #36B37E | **Warning:** #FFAB00 | **Error:** #FF5630.
- **Backgrounds:** #F4F5F7 (Light Gray) for page backgrounds; #FFFFFF for cards.
- **Typography:** Inter (Sans-serif). 14px for body, 16px-24px for headings.

## 2. Shared Components
- **Sidebar Navigation:** Persistent on the left. Links: Dashboard, Requests, Members, Alerts, Reports.
- **Global Header:** Search bar, AI Assistant trigger, Notifications icon, User Profile.
- **Status Badges:**
    - PENDING: Gray background, dark text.
    - IN_PROGRESS: Blue background, white text.
    - DONE: Green background, white text.
- **Priority Indicators:**
    - HIGH: Red icon/text.
    - MEDIUM: Orange.
    - LOW: Blue.

## 3. Core Screens
### Screen 1: Admin Dashboard (Stats)
- Summary cards (Total, PENDING, DONE).
- Bar chart: Requests by Developer.
- Pie chart: Requests by Category.
- "Recent High Priority" list.

### Screen 2: Request List (Unified)
- Data table with filters (Status, Category, Priority).
- Search input for keywords.
- Actions: View Detail, Quick Assign (Admin only).

### Screen 3: Request Detail & Workflow
- Left column: Title, Description, Attachments.
- Right column: Status control (State machine), Developer assignment, LLM Insights (Summary/Classify).
- Bottom: Timeline (Request History).

### Screen 4: Login & Authentication
- Clean center-aligned card.
- Fields: Email, Password.
- "Forgot password" and "Client Registration" links.

## 4. LLM Integration
- **AI Sidebar/Modal:** Displays "AI Summary" or "Priority Suggestion" results.
- **Inline Badges:** "AI Suggested Category" with a confidence score.

---
*Created for Bzcom CRM - OJT 2026 KITS Hanoi*
