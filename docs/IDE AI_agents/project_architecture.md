# Project Architecture Guide for AI Agents

This document outlines the structural architecture of the Edu-AI project. AI agents interacting with this repository must familiarize themselves with these guidelines to ensure coherent and effective development.

## Overview

**Edu-AI** is an educational platform designed to assist users in learning and development through AI-powered tools and resources. The project follows a modular architecture, separating concerns into distinct layers: Data, Domain, Presentation, and Core.

## Key Components

### Core Modules

*   **Core Utilities:** Provides fundamental services and extensions used across the application. Includes common extensions, constants, and utility functions.
    *   Key classes/files: `core/utils/*`, `core/extensions/*`
    *   Dependencies: None (provides foundational elements)
*   **AI Engine:** Manages AI model interactions, processing, and response generation.
    *   Key classes/files: `backend/ai_engine.py`, `backend/app/services/ai_service.py`
    *   Dependencies: Core Utilities, specific AI libraries.

### Data Layer

*   **Remote Data Source:** Fetches data from remote sources, primarily through a RESTful API.
    *   API definitions: `app/src/main/java/com/example/edu_ai/data/remote/EduAIApi.kt`
    *   Data mapping: Uses data classes and potentially mappers for transformation.
*   **Local Data Source:** Manages local data persistence using Room.
    *   Database schema: `app/src/main/java/com/example/edu_ai/data/local/EduAIDatabase.kt`
    *   DAO definitions: `app/src/main/java/com/example/edu_ai/data/local/EduAIDao.kt`

### Presentation Layer

*   **UI Components:** Built using Jetpack Compose for a modern, declarative UI.
    *   Screens/Activities: Managed within the `app/src/main/java/com/example/edu_ai/ui` package.
    *   ViewModels/Presenters: Uses `ViewModel` for state management and business logic handling, often exposed as `StateFlow`.
    *   Navigation: Implemented using Jetpack Navigation Component.

## Architectural Principles

*   **Modularity:** The project is divided into modules to promote reusability, maintainability, and independent development.
*   **Separation of Concerns:** Each layer and component has a distinct responsibility, ensuring clean code and easier debugging.
*   **Testability:** Components are designed with testability in mind, promoting the use of dependency injection and clear interfaces.

## AI Agent Interaction Guidelines

*   **Understand Before Acting:** Before making any changes, ensure you understand how they fit into the overall architecture.
*   **Consult Documentation:** Refer to this document and other relevant documentation for guidance.
*   **Prioritize Existing Structure:** Maintain consistency with the established architectural patterns.
