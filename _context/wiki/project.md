# Project Overview

## What This Project Is About

Keycloak Adaptive Authentication is an extension that provides **risk-based authentication** for Keycloak. It enhances security by evaluating authentication attempts based on various risk factors and adapting the authentication flow accordingly.

## Main Goals and Objectives

The primary objective is to make this project **production-ready and well-documented**. This includes:

1. **Production Readiness**: Ensuring the system is stable, performant, and suitable for real-world deployments
2. **Comprehensive Documentation**: Providing clear documentation for setup, configuration, and usage
3. **Maintainability**: Building a codebase that is easy to understand, extend, and maintain

## Key Stakeholders and Users

- **Security Teams**: Organizations looking to enhance their authentication security with risk-based approaches
- **Keycloak Administrators**: Teams managing Keycloak deployments who need adaptive authentication capabilities
- **Developers**: Contributors extending the system with custom evaluators or integrations

## Important Modules and Workflows

### Core Architecture

The project is built around two key components:

1. **Risk Evaluation Engine**: The central system that orchestrates risk assessment during authentication
2. **Evaluator System**: A flexible, extensible framework for implementing different risk evaluation strategies

### Key Modules

- **`core/`**: Contains the main risk engine, evaluator framework, and authentication flow integration
- **`extensions/`**: Houses optional extensions like:
  - `ip-api/`: Geographic IP resolution for location-based risk assessment
  - `openrouter/`: AI engine integration via OpenRouter
  - `ssf/`: Shared Signals Framework integration
- **`tests/`**: Integration and unit tests for the adaptive authentication system

### Important Workflows

- **Authentication Flow**: Risk evaluation happens during the authentication process, adapting requirements based on calculated risk scores
- **Evaluator Chain**: Multiple evaluators can be chained together to build comprehensive risk profiles
- **AI Integration**: Support for various AI engines to enhance risk assessment capabilities

## Technical Stack

- **Java/Maven**: Core development stack
- **Keycloak SPI**: Extends Keycloak's authentication framework
- **Quarkus**: Used for extension packaging and configuration

## Project Structure

The project follows a modular architecture:
- Core functionality in the `core/` module
- Optional extensions in `extensions/` for additional capabilities
- Comprehensive test suite in `tests/`

---

*For working preferences and standards, see [preferences.md](preferences.md)*
