## What does this PR do?

<!-- Brief description of the change. Link to an issue if applicable: Fixes #123 -->

## Type of change

- [ ] Bug fix
- [ ] New rule type
- [ ] New feature
- [ ] Documentation
- [ ] Refactoring / chore

## Checklist

- [ ] Unit tests added / updated
- [ ] `RuleConfigValidator` updated if new `ruleType` added
- [ ] `RuleApplierFactory` registration added if new `ruleType` added
- [ ] `RuleType` enum updated in `StandardizationRule.java` if new `ruleType` added
- [ ] README rule types table updated if new `ruleType` added
- [ ] No raw exceptions — only `RuleApplicationException` thrown from `apply()`
- [ ] `mvn test` passes locally

## Screenshots / Logs (if applicable)

<!-- Paste any relevant screenshots or log output here -->

