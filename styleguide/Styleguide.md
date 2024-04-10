# Code Styleguide for ACME Server Project

This style guide aims to ensure consistency, readability, and maintainability of the codebase for the ACME Server project. The guidelines here are heavily inspired by best practices from the Android platform and Java 8, incorporating a few advancements from Java 17 where appropriate.

## General Principles

1. **Type Clarity:** Avoid the use of `var` for declaring variables. Always use the explicit type to enhance code readability and maintainability.
2. **Multiline Strings:** For multiline strings, utilize the text block feature introduced in Java 13 by enclosing the string in triple-double quotes (`"""`), improving readability.
3. **Functional Programming:** Make use of lambda expressions to write more concise and readable code when it makes sense, especially for single-method interfaces or functional interfaces.
4. **Indentation:** Use tabs for indenting code. This approach helps in keeping the codebase consistent and accessible for developers using different IDEs with varying default space settings for indentation.
5. **Clean Imports:** Avoid dead imports. Always ensure that unused imports are removed to prevent any unnecessary clutter in the codebase.
6. **Modular Code:** Avoid large blocks of code within a single function or method. Break down big chunks of logic into smaller, reusable functions. This not only enhances readability but also facilitates easier testing and maintenance.


## Code Formatting

- **Braces:** Use the "K&R style" for braces, where the opening brace is at the end of the line that begins the block, and the closing brace is aligned with the start of the line that begins the block. Example:
```java
if (x == 5) {
    System.out.println("x is 5");
} else {
    System.out.println("x is not 5");
}

```
- **Naming Conventions:**
  - **Classes and Interfaces:** Start with an uppercase letter, following CamelCase notation. Example: ServerConfiguration.
  - **Methods:** Start with a lowercase letter, also following CamelCase. Example: getConfigValue.
  - **Constants:** Use uppercase letters with underscores to separate words. Example: MAX_USER_COUNT.
  - **Variables:** Like methods, start with a lowercase letter and follow CamelCase. Be descriptive with variable names to ensure code readability.
- **Parameter Passing:** Prefer passing interfaces or superclasses in method parameters instead of concrete classes to facilitate easier unit testing and to adhere to the principle of programming to an interface, not an implementation.

## Best Practices
- **Error Handling:** Prefer custom exceptions over generic ones to provide clear and detailed feedback about issues.
- **Comments:** Use comments judiciously. Prefer code that is self-explanatory and only comment to explain "why" something is done, not "what" is being done.
- **Code Reviews:** Encourage regular code reviews to ensure adherence to these guidelines and to foster a culture of collaboration and continuous improvement.