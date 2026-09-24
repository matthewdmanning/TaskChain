# Test Drive

## Routine builder

- Remove larger Schedule header.
- Remove "Set deadline" and "Set reminder".
- Change toggleable "Schedule" to "Repeating".
- Change "Once" button to a "Custom" button and move to rightmost position.
- Custom brings up a day of the week selector -- search for an example instead of hand rolling. Multiple days can be selected.
- Time is a separate row. Always present. Centered with just the time -- no label.
- Tapping the time brings up the scrolling selector.

## Home

- Change "Start" button to a Play icon -- right aligned on the same row as the Routine title.
- Routines no longer have descriptions displayed. Keep the field.
- Completed Routines have a Check icon instead of a Play icon.

## Routine Play

- Increase font size of Routine title.
- Remove "New ..." header
- Remove "Pending"
- Time is in center of the screen and text width is 1/3 the smaller dimension.
- Add description above the time -- halfway between top of time and top safe area boundary. Text size is same as "New ..." header was.
- Skip and Back buttons are just above the safe area -- with buffer. Font size same as the desciption.
- Below the timer are icons representing each step.
  - Not completed: Empty with border
  - Completed: Completed semantic color.
  - Skipped: Warning semantic color.
- Bug: Timer continues to run even when step / task is not on the screen. Timer should pause on Skip, Back, or Complete. Timer should continue on Screen Off, Screen Lock, or navigate away from app.
