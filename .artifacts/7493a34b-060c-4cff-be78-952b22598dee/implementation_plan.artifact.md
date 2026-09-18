# Backend Fixes

This plan addresses the two backend issues reported:
1.  Failing to enter the cursor for "New unit group name" field.
2.  Ingestion engine not adding saved units to the selected Unit Group.

## Proposed Changes

### [MODIFY] ingestion.html (C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/templates/curriculum/ingestion.html)

The `new-unit` input field is hidden by default and its visibility is not correctly managed by the JavaScript. The `updateUnits` function unconditionally adds the `hidden` class to `newUnit`, preventing it from ever being shown when a new unit group is intended to be created.

**Changes:**
- Remove the line `newUnit.classList.add('hidden');` from the `updateUnits` JavaScript function.
- Add a call to `updateUnitInput();` at the end of the `updateUnits` function to ensure the correct initial state of the `newUnit` field.

This will allow the `updateUnitInput` function, which is triggered by changes to `unitSelect`, to correctly control the visibility of the "New unit group name" input field.

### [INVESTIGATE] Ingestion engine not adding saved units to the selected Unit Group.

This issue requires further investigation. I will need to examine the backend code responsible for handling the unit ingestion process, specifically where new units are associated with unit groups. This will likely involve looking at the Python/Flask code handling the form submission from `ingestion.html`.

## Verification Plan

### Manual Verification (for "New unit group name" field)

1.  Navigate to the curriculum ingestion page.
2.  Interact with the "Field" and "Course" dropdowns.
3.  Select "+ Create new unit group" from the "Unit group" dropdown.
4.  Verify that the "New unit group name (optional)" input field becomes visible and allows text input.

### Investigation (for "Ingestion engine not adding saved units")

1.  Identify the backend endpoint and function responsible for handling the submission of the ingestion form.
2.  Examine the logic within that function to understand how unit groups are processed and how new units are associated with them.
3.  Look for any errors in the server logs during the ingestion process.
