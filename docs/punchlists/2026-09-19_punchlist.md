# Test Drive Punchlist

## Run - Morning reset - Home screen

Create "Scheduled", "Manual", and "Completed" sections. Each displayed routine must fall into one of those.

Scheduled routines that have been completed reside in "Compeleted" until their next schedule trigger. They are then moved to Scheduled.

Manual routines never move.

## Routine Builder - Edit Menu

- Timer input should be a "stopwatch" dial, not an text box or clock.
- Set deadline and Set reminder should appear below the Routine description input. It should never appear for individual steps.
- There is no mechanism for scheduling a routine. This should be under the "Schedule" header. Individual tasks should NEVER has the Schedule toggle. This should be moved under "Schedule" header.
- "Steps" should have a header under the schedule toggle.
- Remove the Stack anchor mechanism - very confusing intention.
- Remove "Stack after first step". Replace this feature with a Drag-and-Drop feature for list items -- allows steps to be rearranged.
- Remove "Update step" button. Changes should be autosaved.
- Remove "Add step" button. Replace feature with a list item always at the bottom of the list. It has a + sign in a circle centered in the item with no other elements. It adds a new step when pressed and a copy replaces it. The tile cannot be drag-and-drop like other items.
- The Save button should navigate back to the Routines tab. It should never switch to another tab.
- Only display "Fix the highlighted fields." if a field is missing. Currently displays if no steps were added -- but all displayed fields are filled.
