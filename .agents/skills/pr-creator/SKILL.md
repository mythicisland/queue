---
name: pull-request-creator
description: Automatically generate pull request titles and descriptions by comparing the current Git branch against main (or a user-specified base branch). Use this skill whenever the user asks to create a PR, write a PR description, prepare a pull request, draft a PR, or says anything like "make a PR", "open a PR", "PR for this branch", "write the PR description", or "compare my branch". Also trigger when the user mentions pull requests in the context of their current work or branch.
---

# Pull Request Creator

Generate a complete pull request title and description from the Git diff of the current branch.

## Step 1: Get the diff

Determine the base branch to compare against. Default is `main`. If the project uses `master` instead, use that. If the user specifies a different base branch, use that.

```bash
# Fetch the latest base branch so the comparison is accurate
git fetch origin <base-branch>

# Get the diff as GitHub would show it
git diff origin/<base-branch>...HEAD
```

If the diff is very large, also run `git log --oneline origin/<base-branch>..HEAD` to get commit summaries for additional context.

If the diff is empty, tell the user there are no changes to compare and stop.

## Step 2: Analyze the diff

Read through the full diff. Understand:
- What files changed and why
- The intent behind the changes (feature, fix, refactor, docs, infra)
- Whether any database migrations, schema changes, or migration scripts are part of the diff
- Whether there are any breaking changes

## Step 3: Write the PR

Use the template below. Fill in every section. Be concise and specific — do not pad with filler. Write like a developer talking to another developer.

The title should be short and descriptive, not a sentence. Think of it as a subject line.

### Template

```markdown
## Description
<!-- Explain WHY this PR exists. What problem does it solve? What was broken, missing, or insufficient? Why do we need this change? -->

## Changes
<!-- What specifically was changed? Describe the concrete modifications: new endpoints, refactored modules, updated logic, added dependencies, etc. Be specific. -->

## Type of Change
- [ ] New feature / capability
- [ ] Bug fix
- [ ] Refactoring
- [ ] Documentation
- [ ] Infrastructure / CI/CD

## Behavior Change
<!-- How does the system behave differently after this PR? -->
<!-- If there is no behavior change, write "None — internal refactor only." -->

**Before:**

**After:**

## Pre-Deployment Migrations
<!-- Are there any migrations (database, schema, config, data) that must be run BEFORE this code is deployed? -->
<!-- If yes, list them with exact commands or file references. -->
<!-- If no, write: "No migrations required before deploying." -->
<!-- This section must always be filled in. Never leave it blank or remove it. -->

## Screenshots / Examples
<!-- Add screenshots, logs, or example interactions if they help explain the change. -->
<!-- Omit this entire section if there is nothing to show. -->
```

### Rules for filling in the template

- **Description**: This section answers the question "why does this PR exist?" Explain the problem, the motivation, or the context that makes this change necessary. Do not just describe what changed — explain why we need it. This can be longer than the other sections; take as many sentences as needed to make the reasoning clear. Do not list filenames.
- **Changes**: The what. Walk through what was actually modified — new files, updated logic, added or removed dependencies, refactored modules, new endpoints, changed configs. Be specific and concrete. This is where the reviewer understands the scope of the work.
- **Type of Change**: Check exactly one box. If multiple apply, pick the primary one.
- **Behavior Change**: This is the most important section. If the PR changes how anything works from a user or system perspective, show a concrete before/after. If it's purely internal (refactor, dependency bump, CI change), write "None — internal refactor only."
- **Pre-Deployment Migrations**: Always fill this in. Look for migration files, schema changes, SQL scripts, Alembic/Flyway/Knex/Django migration files, or config changes that need to happen before deploy. If there are none, explicitly state "No migrations required before deploying." Never skip or remove this section.
- **Screenshots / Examples**: Only include this heading if there's something visual or interactive to show. Otherwise remove it entirely from the output.

## Step 4: Create the PR

By default, create the pull request using GitHub CLI. If the user explicitly asks to write it to a file instead, save the title and body to a file and skip the `gh` command.

### Default: Create via GitHub CLI

Before creating the PR, make sure the current branch has been pushed to origin:

```bash
# Check if the branch exists on the remote
git ls-remote --heads origin $(git branch --show-current)
```

If the branch has not been pushed, push it first:

```bash
git push -u origin $(git branch --show-current)
```

Then create the PR, passing the body directly:

```bash
gh pr create --base <base-branch> --title "<PR title>" --body "<filled-in template body>"
```

If the user says "draft", "WIP", or "draft PR", add the `--draft` flag:

```bash
gh pr create --draft --base <base-branch> --title "<PR title>" --body "<filled-in template body>"
```

If `gh` is not installed or not authenticated, tell the user and fall back to writing the output to a file.

### Alternative: Write to file

If the user asked to save to a file instead of creating the PR, write both the title and body to the file they specified (or default to `pr_description.md` in the current directory). Format it so the title is on the first line prefixed with `# `, followed by a blank line, followed by the body.