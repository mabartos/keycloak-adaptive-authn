# Working Preferences

## Coding Standards

### Code Quality
- **Prioritize clean, maintainable code**: Write code that is easy to understand, modify, and extend
- Focus on readability and simplicity over cleverness
- Use meaningful variable and method names
- Keep functions and classes focused on single responsibilities

### Git Commit Messages
- **Commits must be signed-off**: Ensure all commits include a sign-off
- Commit messages should be clear and descriptive
- No strict format required, but clarity is important

### Testing
- Maintain comprehensive test coverage
- Write both unit tests and integration tests
- Tests should be clear and document expected behavior

#### Test Suite Requirements
- **No proxies or stubs**: Do not use mocks, proxies, or stubs in the test suite
- **Use RunOnServer**: For server-side testing, use the RunOnServer functionality
- **Cleanup patterns**: Use `cleanup()` methods instead of try-finally blocks for resource cleanup
- Tests should interact with real components whenever possible

### Documentation
- Keep documentation up-to-date with code changes
- Document complex algorithms and architectural decisions
- Provide clear examples for configuration and usage

## Communication Preferences

### Working with AI Assistants

- **Ask before making significant changes**: Always seek approval before:
  - Modifying core architecture or design patterns
  - Making breaking changes to APIs
  - Refactoring large sections of code
  - Changing build configurations or dependencies

- **Be direct and technical**: Focus on technical accuracy and clarity
- **Provide context when needed**: Explain reasoning for architectural decisions
- **Respect existing patterns**: Follow established patterns in the codebase

### GitHub CLI Usage
- **Never use GitHub CLI without explicit consent**: Do not automatically create issues, PRs, or other GitHub resources
- When asked about GitHub issues, provide the content/template only
- Wait for explicit approval before executing any `gh` commands

### Decision Making

- Prefer incremental improvements over large rewrites
- Consider backward compatibility when making changes
- Document trade-offs when making architectural decisions

## Development Workflow

### Before Starting Work
- Review existing code and patterns
- Check for related tests that might need updates
- Consider impact on documentation

### During Development
- Write tests alongside code
- Keep commits focused and atomic
- Update documentation as you go

### After Completing Work
- Verify all tests pass
- Review changes for code quality
- Update relevant documentation

## Project-Specific Notes

### Critical Areas
- **Risk Evaluation Engine**: Core component requiring careful consideration for any changes
- **Evaluator System**: Extension points should maintain backward compatibility
- **AI Integration**: Changes should support multiple AI engine implementations

### Protected Areas
- Do not modify Husky configuration in `/js/.husky/` directory (if present)

---

*For project details, see [project.md](project.md)*
