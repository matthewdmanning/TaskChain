# Designing Shapes with augmented-ui-generator

Here are practical rules-of-thumb for designing complex, cut-based UI components using subtractive operations (such as chamfers, notches, cutouts, and sectors) rather than arbitrary vector paths.

These rules represent standard, conventional designs. You deviate if requested by the user.

### **1. Chamfers & Angled Corners**

- **Proportional Depth:** Set chamfer depth to **15% to 30%** of the container's shorter side. This keeps the cut distinct without swallowing interior content space.
- **Angle Standardization:** Restrict corner cuts to a strict **45-degree angle** to ensure clean rendering across standard clip-path properties and screen resolutions.
- **Symmetric Pairing:** If chamfering one corner, mirror it diagonally or across the primary axis to maintain visual balance.

### **2. Perimeter Notches & Cutouts**

- **Grid Alignment:** Make notch widths and depths multiples of your baseline grid (e.g., **8px** or **12px**) to prevent sub-pixel blurring.
- **Depth Limit:** Never let a perimeter cutout exceed **25%** of the total edge length; otherwise, the component loses its structural container feel.
- **Safe Zone Padding:** Enforce an internal padding zone equal to **1.5x the notch depth** so text and interactive elements never drift into clipped areas.

### **3. Sectors & Radial Arrangements**

- **Angular Distribution:** For circular sectors or radial cutouts, divide the full circle by your item count ($360^\circ / N$).
- **Gap Spacing:** Always introduce a minimum gap of **4dp** between adjacent sectors to prevent anti-aliasing bleeding and visual merging.
- **Radial Shape Definition:** A radial control is an **annulus sector system**, not a circle. Render an outer rim with an inner radius, divide the rim into equal angular sections, and cut the gaps between sections.
- **Equal Sections:** For $N$ rim sections, each section spans $360^\circ / N$ before applying the gap. Keep every section's inner radius, outer radius, thickness, and angular gap equal.
- **Radial Acceptance Check:** Reject the render if it shows one filled disk, a single circle, or a continuous rim without visible section gaps.

### **4. Agent Geometry Contract**

- **Symmetry Modes:** Every generated shape must declare exactly one symmetry mode: `x`, `y`, or `all`.
- **X Symmetry:** Mirror the complete cut specification across the vertical axis. Left and right corners, edge notches, chamfers, and inlays must have matching geometry.
- **Y Symmetry:** Mirror the complete cut specification across the horizontal axis. Top and bottom corners, edge notches, chamfers, and inlays must have matching geometry.
- **All Symmetry:** Apply both mirrors. The four corresponding corners and four corresponding edge positions must match.
- **Symmetry Scope:** Mirror geometry, not only color or shading. The preview must show matching cut vertices, notch depths, chamfer lengths, and inlay boundaries.
- **Asymmetry Is Allowed:** Do not reject asymmetric geometry by default. Use asymmetry when it creates a deliberate visual hierarchy, directional motion, control affordance, threat or impact language, hardware-like construction, or another strong stylistic effect.
- **Asymmetry Rationale:** Every asymmetric shape must declare its stylistic reason in the specification and preview label. State which side is dominant and how the geometry supports that intent.
- **Asymmetry Standard:** The rationale must be specific and visible in the render. Do not use asymmetry only to add random variation, decoration, or a minor offset.
- **Symmetry Acceptance Check:** For `x`, `y`, and `all`, overlay the shape with its declared mirror and reject unintended mismatches. For asymmetric shapes, verify that every mismatch is intentional, documented, and visually legible.
- **Named Cut Requirement:** Every variation must use at least one named grammar primitive: `notch`, `chamfer`, `scoop`, `rect`, `step`, or `sector`.
- **Variation Requirement:** Labels such as `deep cut`, `offset`, or `radial` do not count as geometry. Each variation must change a measurable primitive property such as cut type, depth, width, position, count, or gap.
- **Deep Cut Definition:** A `deep cut` must visibly increase the penetration depth of a notch, chamfer, scoop, rect, or step. If the silhouette is unchanged, the iteration is invalid.
- **Preview Labels:** Label every render with its type, iteration, symmetry mode, and active primitive so an agent can verify the output without inference.

### **5. Tessellated & Repeating Patterns**

- **Row/Column Offsets:** Offset alternate rows or columns by **50%** of the element's height or width to create seamless interlocking meshes.
- **Overlap Management:** When stacking cut components, overlap boundaries by exactly **1px** to avoid sub-pixel background leaks between adjacent cuts.

### **6. Dynamic Clamping**

- **Responsive Scaling:** Constrain cut and chamfer sizes using hybrid scales (e.g., `clamp(8px, 2vw, 24px)`) so geometric cutouts scale gracefully on mobile without collapsing on smaller viewports.

### **7. Preview Before Large Shape Sets**

- **HTML Preview Offer:** Before rendering a complex shape or a large collection of shapes, offer a quick HTML preview to inspect the visual rhythm, cut depth, and overlap behavior in a browser first.
- **Low-Noise Validation:** Use the preview to confirm proportions, notches, and spacing before committing to the final Compose or native rendering pass.
- **Batch Safety Check:** For grouped or repeated shapes, preview one representative sample and then a small batch to catch drift, anti-aliasing, or spacing issues before scaling the design across the full set.

### **8. Family Parameter Sets**

- **Complete Sets:** Each family must contain four complete parameter sets. A set changes the coordinated geometry together: primitive size, notch width, notch depth, border, and inlay where present. Do not vary one scalar while leaving the rest of the family frozen.
- **Notch Proportion:** Keep notch width and depth visually related. Use a width-to-depth ratio of roughly **4:1 to 6:1** for rectangular controls; reject a notch that reads as a slit or a deep bite.
- **Side-Length Scaling:** Size edge cuts against the edge they occupy. On a nominal `180dp × 72dp` card, horizontal `t`/`b` notches should use widths around **36dp to 54dp** and depths around **6dp to 10dp**. Vertical `l`/`r` notches should use widths around **16dp to 26dp** and the same depth range.
- **Orientation Swap:** In browser or CSS representations, horizontal `t`/`b` notch dimensions map as `width = along-edge span` and `height = inward depth`; vertical `l`/`r` notches swap those dimensions so the same motif rotates with the side.
- **Directional Pairing:** A directional component must be made from a subtraction on one side and an equal, same-sized counterpart on the opposite side. The pair must share primitive, size, and depth; direction comes from the paired placement, not arbitrary parameter drift.

### **9. Radial Components**

- **Separate Components:** A circular-chevron family is a set of independent annulus sectors, not one annulus path with decorative lines. Render each sector as its own component or subpath with a real gap between neighbors.
- **Gap Acceptance:** Keep no less than **4dp** gap at the outer rim and verify that the center does not read as one continuous ring.
- **Chevron Acceptance:** Each sector must retain a measurable chevron point or bite. An uninterrupted annulus, filled disk, or ring with only painted separators is invalid.

### **10. Current Iteration Corrections**

- **Border Presence:** The border must read as structural hardware, not hairline decoration. Use a visible **2.5dp to 4dp** border in the nominal gallery and keep it legible against the dark surface.
- **Radial Visibility:** Every annular sector must render visibly in both browser and Compose previews. Prefer independent sector elements or independent draw operations; do not rely on a disjoint clipping path that can disappear as one filled contour.
- **Rectangle Variation:** Rectangular primitives must not repeat as four identical corner blocks. Alter at least two measurable properties per set—such as corner orientation, paired edge notches, step depth, or placement—while preserving declared symmetry and directional pairing.
