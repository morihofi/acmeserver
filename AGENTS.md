# Agents

## Overview

This document outlines the operational standards for autonomous ChatGPT Codex agents within a container bases
development environment.
Agents must adhere to the ACME RFC 8555 (and it extensions) and operate in a secure, self-contained workspace with
internet access in post-setup.
This is a Java application development environment, and agents are expected to follow best practices for Java
development.

## Environment Setup

* **Base Image**: Use the latest Java 21 base image.
* **Containerization**: All development must occur within a Docker container to ensure consistency and isolation.
* **Network Access**: Agents must have internet access for package management and external API calls, but should not
  access the host system directly.
* **File System**: Agents should only write to the `/workspace` directory within the container. This directory is
  mounted to the host system for persistence.
* **Environment Variables**: Use a `.env` file for configuration. Sensitive data should be stored securely and not
  hard-coded in the codebase.
* **Dependencies**: Use a dependency management tool like Maven to handle project dependencies. All dependencies must be
  declared in the project configuration file. You may add dependencies to the parent `pom.xml` file, but ensure they are
  compatible and maintained with Java 21.

## Coding Standards

* **Language Version**: Use Java 21 features and syntax.
* **Code Style**: Follow the Google Java Style Guide for consistent code formatting.
* **Documentation**: All public classes and methods must be documented using Javadoc. Inline comments should be used to
  clarify complex logic.
* **Testing**: Write unit tests for all new features and bug fixes. Use JUnit 5 for testing. Ensure that tests cover
  edge cases and error handling.
* **Error Handling**: Use exceptions for error handling. Custom exceptions should be created for specific error
  scenarios. Avoid using generic exceptions.
* **Logging**: Use SLF4J with Logback for logging. Log at appropriate levels (DEBUG, INFO, WARN, ERROR) and avoid
  logging sensitive information.
* **Security**: Follow secure coding practices. Validate all inputs, sanitize outputs, and avoid hard-coded secrets. Use
  environment variables for sensitive configurations.
* **Lombok**: Use Lombok for boilerplate code reduction, but ensure that it is used judiciously and does not obscure
  code readability. Also Annotate classes with `@Data` and `@Builder` where appropriate to leverage Lombok's
  capabilities.
* **Modularization**: Structure the code into modules where appropriate. Each module should have a clear responsibility
  and should be loosely coupled with other modules.
* **Version Control**: Use Git for version control. All code must be committed to the repository with clear, descriptive
  commit messages. Branches should be used for feature development and bug fixes.

## Commit and Versioning

* Use Conventional Commits:
  ```
  feat(login): add offline auth logic
  fix(utils): correct hashing salt bug
  test(login): add edge case coverage
  ```

## Versioning:

* Use Semantic Versioning (SemVer) for releases.
* Version numbers should be in the format `MAJOR.MINOR.PATCH`.
* Increment the version number according to the changes made:
    - **MAJOR**: for incompatible API changes,
    - **MINOR**: for adding functionality in a backwards-compatible manner,
    - **PATCH**: for backwards-compatible bug fixes.
* Tag releases in the Git repository with the version number.
* Use GitHub Releases to document changes in each version, including new features, bug fixes, and any breaking changes.
* Ensure that the `CHANGELOG.md` is updated with each release, documenting the changes made in a clear and concise
  manner.
* Use Act compatible (like GitHub Actions) AND CircleCI for automated versioning and release management, ensuring that
  the process is consistent and reproducible.
* Ensure that the versioning process is integrated into the CI/CD pipeline, so that every successful build can be tagged
  and released automatically if desired.

## CI/CD Integration

* Use Act compatible CI (like GitHub Actions) and CircleCI for CI/CD
* Ensure that the CI/CD pipeline includes:
    - Build and test stages
    - Code quality checks (e.g., using SpotBugs)
    - Deployment to a staging environment for testing
    - Automated deployment to production upon successful tests and approvals
    - Versioning and tagging of releases

## Security and Compliance

* Ensure that all code adheres to security best practices.
* Conduct regular security audits and code reviews.
* Use tools like OWASP Dependency-Check to identify vulnerabilities in dependencies.
* Ensure compliance with relevant regulations and standards (e.g., GDPR, HIPAA) as applicable to the project.
* Implement secure coding practices, including input validation, output encoding, and proper error handling.
* Use static code analysis tools to identify potential security issues in the codebase.

## Documentation

* Maintain comprehensive documentation for the project, including:
    - Architecture overview
    - API documentation (using Swagger or similar tools) but not the ACME RFC 8555, because it is not a REST API
    - Developer guides and setup instructions
    - User manuals and FAQs
    - Changelog for tracking changes and updates
* Ensure that documentation is kept up-to-date with code changes.
* Use Markdown for documentation files to ensure readability and ease of use.

## Best Practices

* Regularly refactor code to improve readability and maintainability.
* Conduct code reviews for all pull requests to ensure code quality and adherence to standards.
* Use feature flags for new features to allow for gradual rollout and testing in production.
* Implement automated testing for all critical paths in the application.
* Manage dependencies carefully, ensuring that they are up-to-date and secure.
* Keep the codebase clean and organized, with a clear directory structure.
* Use design patterns where appropriate to solve common problems in a consistent manner.
* Encourage collaboration and knowledge sharing among team members.
* Foster a culture of continuous improvement, where team members are encouraged to suggest and implement improvements to
  the development process and codebase.
* Regularly review and update coding standards and best practices to ensure they remain relevant and effective.
* Encourage the use of modern Java features and libraries to improve code quality and developer productivity.

## Conclusion

This document serves as a guideline for the development of autonomous ChatGPT Codex agents within a container-based Java
development environment. Adhering to these standards will ensure a consistent, secure, and efficient development
process, leading to high-quality software that meets the needs of users and stakeholders. Regular reviews and updates to
this document will help maintain its relevance and effectiveness in guiding development practices.
