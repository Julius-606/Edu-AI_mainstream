# Version Control Guidelines for AI Agents

This document outlines the version control strategy for the Edu-AI project. All AI agents must adhere to these guidelines when modifying files.

## Version Numbering Format

All version numbers must follow the format: `version_number.major_patch.minor_patch`

*   **`version_number`**: Represents the main release version (e.g., 1, 2, 3).
*   **`major_patch`**: Incremented for significant changes that might include new features or substantial refactoring.
*   **`minor_patch`**: Incremented for minor changes, bug fixes, or small improvements.

**Example:** `1.2.3`

*   `1` is the version number.
*   `2` is the major patch.
*   `3` is the minor patch.

## File Touched Versioning

When a file is modified, its version number should be updated according to the following rules:

1.  **Bug Fix:** If the change is a bug fix, increment the `minor_patch` number.
    *   Example: `1.2.3` -> `1.2.4`
2.  **Feature Addition/Refactoring:** If the change introduces a new feature or involves significant refactoring, increment the `major_patch` number and reset `minor_patch` to 0.
    *   Example: `1.2.3` -> `1.3.0`
3.  **Major Release:** For major releases (e.g., V2.0), update the `version_number`, reset `major_patch` and `minor_patch` to 0.
    *   Example: `1.2.3` -> `2.0.0`

**Note:** Ensure that the version number is updated at the beginning of the file or in a clearly designated location (e.g., a constant or a specific comment block).

## Commit Messages

*   All commit messages should be clear and concise, describing the changes made.
*   Prefix commit messages with the type of change (e.g., `feat:`, `fix:`, `refactor:`, `chore:`).
*   Include the updated version number in the commit message if applicable.

## AI Agent Interaction Guidelines

*   **Track Changes:** Always be aware of the files you are modifying and their current version numbers.
*   **Update Versions:** Before committing, ensure the version numbers in modified files are updated according to these guidelines.
*   **Version Control Best Practices:** Follow standard Git practices, such as creating branches for new features and merging them responsibly.
