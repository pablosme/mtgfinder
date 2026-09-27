# Project Agent Instructions

These instructions apply to the entire repository.

## Tests

- Do not create unit tests unless the user explicitly requests them.
- Existing tests may be inspected or run when useful for validation, but new unit test files or test cases must not be added without an explicit request.

## Function Design

- Do not fragment a single action into multiple small functions when that would make the project harder to navigate.
- Keep the steps of an action together when they are only used as part of that action.
- Create a new function only when at least one of the following is true:
  - The logic will be reused.
  - The function isolates a fully self-contained responsibility with a clear boundary, such as executing a request, converting data, parsing input, or interacting with an external system.
- Do not extract functions solely to shorten another function or to give every individual step its own name.

## Record Organization

- Keep each standalone record in its own source file.
- A record may remain nested only when it represents a component that belongs exclusively to its enclosing record.
- Do not declare response, result, projection, or other independently meaningful records inside services, controllers, repositories, or unrelated classes.

## Complex Task Memory — Highest Priority

This is the highest-priority project-specific working rule.

- For every complex request, use the repository-root `agent_task/` directory as temporary working memory.
- Before implementing a complex request, create a dedicated Markdown file at `agent_task/<task-name>.md` containing the detailed task instructions and execution plan.
- Break the task into **executions**. Each execution must represent a logical group of related work and contain a checklist of concrete steps.
- Mark each checklist step as it is completed so the file always reflects the current state of the task and clearly identifies the next step.
- Keep the task file updated when discoveries, constraints, decisions, or changes to the plan affect the remaining work.
- At the end of each execution, give the user a compact, short briefing that states:
  - What was completed in the execution.
  - What the next execution will cover.
- After sending the briefing, continue with the next execution unless user input or approval is required.
- Treat files under `agent_task/` as temporary local coordination files. They must not be committed.

A task is complex when it involves multiple logical phases, broad or cross-cutting changes, significant investigation, or enough steps that progress could become difficult to track reliably without a written plan.

Use this structure for task memory files:

```markdown
# Task: <name>

## Instructions

<Detailed task requirements, constraints, and relevant context.>

## Execution 1: <logical group>

- [ ] Step 1
- [ ] Step 2

## Execution 2: <logical group>

- [ ] Step 1
- [ ] Step 2

## Notes and Decisions

- <Important discovery, constraint, or decision.>
```
