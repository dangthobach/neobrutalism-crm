# Implementation Spec: Automated Dependency Updates

**Feature ID:** #7
**Theme:** Developer Velocity
**Priority:** Quick Win - Week 1
**Effort:** 1 sprint (1 week)
**Risk Level:** Zero (non-invasive)
**Status:** Ready to implement

---

## 🎯 Overview

Automate dependency updates using Renovate Bot to eliminate manual version checking, reduce security vulnerability windows, and maintain up-to-date dependencies across both frontend (Node.js/npm) and backend (Java/Maven) ecosystems.

### Business Value
- **Security:** Automated security patch deployment within hours instead of weeks
- **Velocity:** Eliminate manual "check for updates" tasks (saves ~4 hours/month)
- **Quality:** Catch breaking changes early with small, incremental updates
- **Compliance:** Always-current dependencies for audit requirements

### Success Metrics
- ✅ 5+ automated PRs created in first week
- ✅ At least 2 dependency updates merged
- ✅ Zero manual intervention for patch updates
- ✅ Security vulnerabilities patched within 24 hours

---

## 📋 Current State Analysis

### Existing Dependencies
**Frontend (package.json):**
- Next.js 16.0.4
- React 19.0.0
- TypeScript 5.1.6
- 50+ npm packages total

**Backend (pom.xml):**
- Spring Boot 3.5.7
- Java 21
- 30+ Maven dependencies

### Current Process
- ❌ Manual checking of dependency versions
- ❌ Irregular update schedule (quarterly at best)
- ❌ No automated security vulnerability detection
- ❌ Batch updates causing large, risky PRs

### Problems to Solve
1. Security vulnerabilities sit unpatched for weeks
2. Major version updates are scary (too many changes at once)
3. No visibility into which dependencies are outdated
4. Breaking changes discovered too late

---

## 🎯 Solution Design

### Tool Selection: Renovate Bot

**Why Renovate over Dependabot?**
- ✅ Better grouping options (group minor/patch updates)
- ✅ Supports both npm AND Maven in single config
- ✅ More flexible scheduling
- ✅ Better monorepo support for future growth
- ✅ Free for public and private repos

### Configuration Strategy

**Update Frequency:**
- **Security patches:** Immediately (any time)
- **Patch updates:** Weekly (Monday 2-3 AM)
- **Minor updates:** Weekly (Monday 2-3 AM)
- **Major updates:** Monthly (first Monday)

**Grouping Strategy:**
- Group all patch + minor updates into single PR (easier to review)
- Separate major updates (need careful testing)
- Separate security updates (immediate attention)

**Auto-merge Strategy:**
- Auto-merge: Patch updates for stable packages (version >= 1.0.0)
- Manual review: Minor updates, major updates, pre-release packages
- Immediate alert: Security vulnerabilities (critical/high)

---

## 🛠️ Implementation Plan

### Phase 1: Basic Setup (Day 1)

**Step 1: Install Renovate App (5 minutes)**

1. Go to: https://github.com/apps/renovate
2. Click "Install"
3. Select repository: `neobrutalism-crm`
4. Grant permissions: Read/Write to code, PRs

**Step 2: Create Renovate Config (15 minutes)**

Create file: `renovate.json` in project root

```json
{
  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
  "extends": [
    "config:recommended"
  ],
  "timezone": "Asia/Ho_Chi_Minh",
  "schedule": [
    "before 3am on Monday"
  ],
  "labels": [
    "dependencies",
    "automated"
  ],
  "assignees": ["@your-github-username"],
  "reviewers": ["@team-lead-username"],

  "packageRules": [
    {
      "description": "Group all non-major updates together",
      "matchUpdateTypes": ["minor", "patch"],
      "groupName": "all non-major dependencies",
      "groupSlug": "all-minor-patch"
    },
    {
      "description": "Auto-merge patch updates for stable packages",
      "matchUpdateTypes": ["patch"],
      "matchCurrentVersion": "!/^0/",
      "automerge": true,
      "automergeType": "pr",
      "automergeStrategy": "squash"
    },
    {
      "description": "Separate major updates for careful review",
      "matchUpdateTypes": ["major"],
      "groupName": "major dependencies",
      "groupSlug": "major"
    },
    {
      "description": "Security updates - immediate priority",
      "matchDatasources": ["npm", "maven"],
      "matchUpdateTypes": ["patch"],
      "vulnerabilityAlerts": {
        "labels": ["security"],
        "assignees": ["@security-team-username"]
      }
    }
  ],

  "npm": {
    "fileMatch": ["^package\\.json$"],
    "rangeStrategy": "bump"
  },

  "maven": {
    "fileMatch": ["^pom\\.xml$"]
  },

  "ignoreDeps": [
    "java"
  ],

  "lockFileMaintenance": {
    "enabled": true,
    "schedule": ["before 3am on Monday"]
  },

  "prConcurrentLimit": 5,
  "prHourlyLimit": 2,

  "commitMessagePrefix": "chore(deps):",
  "semanticCommits": "enabled"
}
```

**Step 3: Commit and Push (5 minutes)**

```bash
git checkout -b chore/setup-renovate
git add renovate.json
git commit -m "chore: Setup Renovate for automated dependency updates"
git push origin chore/setup-renovate
```

**Step 4: Create PR and Merge (10 minutes)**

- Create PR for renovate.json
- Review config
- Merge to main branch
- Renovate will activate automatically

---

### Phase 2: Initial Onboarding (Day 1-2)

**What Happens Next:**

1. **Onboarding PR Created (within 1 hour)**
   - Renovate creates initial PR
   - Shows all available updates
   - Explains configuration
   - **Action:** Review and merge onboarding PR

2. **First Update PRs (within 2 hours)**
   - Renovate scans dependencies
   - Creates grouped PRs for updates
   - Typically 3-5 PRs in first run
   - **Action:** Review, test, merge

**Expected First PRs:**
- PR #1: All minor and patch updates (grouped)
- PR #2: Major version updates (if any)
- PR #3: Lock file maintenance
- PR #4-5: Individual major updates requiring attention

---

### Phase 3: Testing & Validation (Day 2-3)

**Test Plan for First Update PRs:**

**For Frontend Updates (package.json):**
```bash
# Pull PR branch
git fetch origin
git checkout renovate/all-minor-patch

# Install dependencies
npm install

# Run tests
npm run test

# Run build
npm run build

# Start dev server and smoke test
npm run dev
# Manually test: Login, navigate key pages, check console for errors

# If all pass → Approve PR
```

**For Backend Updates (pom.xml):**
```bash
# Pull PR branch
git checkout renovate/all-minor-patch

# Clean install
mvn clean install

# Run tests
mvn test

# Run integration tests
mvn verify

# Start application and smoke test
mvn spring-boot:run
# Manually test: API endpoints, database connections, security

# If all pass → Approve PR
```

**Validation Checklist:**
- [ ] All unit tests pass
- [ ] Build succeeds without errors
- [ ] Application starts without errors
- [ ] Key user flows work (login, CRUD operations)
- [ ] No console errors
- [ ] No breaking changes in logs

---

### Phase 4: Monitoring & Tuning (Day 4-7)

**Monitor These Metrics:**

1. **PR Success Rate**
   - Target: >80% of PRs auto-mergeable
   - If lower: Adjust grouping or auto-merge rules

2. **Review Time**
   - Target: <30 minutes per PR
   - If higher: Groups are too large, split them

3. **Breaking Changes**
   - Target: 0 breaking changes in patch updates
   - If any: Add to ignoreDeps, investigate package

4. **Security Updates**
   - Target: Critical CVEs patched within 24 hours
   - If slower: Enable immediate notifications

**Tuning Config:**

If PRs are overwhelming:
```json
{
  "prConcurrentLimit": 3,  // Reduce from 5
  "prCreation": "not-pending"  // Wait for CI before creating more
}
```

If updates are too frequent:
```json
{
  "schedule": ["before 3am on first Monday of month"]  // Monthly instead of weekly
}
```

If specific packages cause issues:
```json
{
  "packageRules": [
    {
      "matchPackageNames": ["problematic-package"],
      "enabled": false  // Temporarily disable
    }
  ]
}
```

---

## 📁 File Changes

### New Files
- ✅ `renovate.json` (root directory)

### Modified Files
- ❌ None (zero code changes!)

### No Changes Required To
- Existing CI/CD pipelines (Renovate uses your existing workflows)
- Existing test suites (no changes needed)
- Existing build scripts (no changes needed)

---

## 🔒 Security Considerations

### Permissions Required
- **GitHub App Permissions:**
  - Read access to code
  - Write access to pull requests
  - Read access to checks

### Security Benefits
- ✅ Automated security vulnerability patching
- ✅ CVE notifications via PR labels
- ✅ Dependency audit trail in PRs
- ✅ No manual credentials needed

### Security Best Practices
1. **Review all major updates manually** (configured in renovate.json)
2. **Test security patches in staging first** (if critical system)
3. **Enable branch protection** (require CI to pass before merge)
4. **Set up alerts** (configure GitHub notifications)

---

## 🧪 Testing Strategy

### Automated Testing
- ✅ Existing CI/CD runs on all Renovate PRs automatically
- ✅ All tests must pass before merge
- ✅ Build must succeed before merge

### Manual Testing
**For Low-Risk Updates (patch):**
- Quick smoke test only (5 minutes)
- Check key user flows work

**For Medium-Risk Updates (minor):**
- Full smoke test (15 minutes)
- Check all major features work
- Review changelogs for breaking changes

**For High-Risk Updates (major):**
- Comprehensive testing (30+ minutes)
- Read migration guides
- Test all impacted features
- Stage deployment first

---

## 📊 Success Criteria

### Week 1 Goals
- [x] Renovate installed and configured
- [x] Onboarding PR merged
- [x] First 2+ update PRs merged successfully
- [x] Zero breaking changes introduced
- [x] Team familiar with PR review process

### Month 1 Goals
- [x] 20+ dependencies updated automatically
- [x] At least 1 security vulnerability patched
- [x] Auto-merge working for patch updates
- [x] <30 minute average PR review time

### Quarter 1 Goals
- [x] 100% of security patches applied within 48 hours
- [x] Zero manual dependency update tasks
- [x] All dependencies within 1 major version of latest
- [x] Team confidence in automated updates

---

## 🚀 Rollout Plan

### Day 1: Setup (Monday)
**Time Required:** 2 hours
- Morning: Install Renovate, create config, submit PR
- Afternoon: Merge config PR, review onboarding PR

### Day 2: First Updates (Tuesday)
**Time Required:** 2 hours
- Morning: Review and test first update PRs
- Afternoon: Merge successful PRs, investigate any failures

### Day 3: Stabilization (Wednesday)
**Time Required:** 1 hour
- Review any remaining PRs
- Tune configuration if needed
- Document any issues

### Day 4-5: Monitoring (Thursday-Friday)
**Time Required:** 30 minutes/day
- Check for new PRs
- Monitor for breaking changes
- Adjust configuration as needed

### Week 2+: Steady State
**Time Required:** 15 minutes/week
- Review weekly grouped PR (Monday morning)
- Quick test and merge
- Handle any security alerts immediately

---

## 🎓 Team Training

### For Developers

**What You'll See:**
- New PRs labeled "dependencies" and "automated"
- PR titles like: "chore(deps): update all non-major dependencies"
- Clear changelogs in PR description

**What You Should Do:**
1. **For Patch Updates (Auto-merged):**
   - Review the PR after merge
   - If issues found, revert and add to ignoreDeps

2. **For Minor Updates (Manual review):**
   - Check CI status (must be green)
   - Quick smoke test key features
   - Approve and merge if all good

3. **For Major Updates (Careful review):**
   - Read changelog for breaking changes
   - Full testing in local environment
   - Stage deployment if critical package
   - Merge only when confident

**When to Ask for Help:**
- CI failing on dependency update PR
- Breaking change suspected
- Security vulnerability needs immediate attention
- Unsure if update is safe

---

## 🐛 Troubleshooting

### Issue: Renovate Not Creating PRs

**Diagnosis:**
```bash
# Check Renovate app status
# Go to: https://github.com/yourorg/yourrepo/settings/installations
# Verify Renovate is installed and active
```

**Solution:**
1. Check renovate.json syntax (must be valid JSON)
2. Check schedule configuration (might be outside window)
3. Check logs: https://developer.mend.io/github/yourorg/yourrepo
4. Trigger manually via issue: "renovate/renovate-me"

### Issue: Too Many PRs at Once

**Solution:**
```json
{
  "prConcurrentLimit": 2,  // Reduce concurrent PRs
  "prCreation": "not-pending"  // Wait for CI before creating more
}
```

### Issue: PR Always Fails CI

**Diagnosis:**
- Check if it's a legitimate breaking change
- Check if tests need updating
- Check if configuration conflict

**Solution:**
```json
{
  "packageRules": [
    {
      "matchPackageNames": ["problematic-package"],
      "enabled": false  // Disable temporarily
    }
  ]
}
```

### Issue: Security Vulnerability Not Fixed

**Solution:**
- Check if fix version available
- Manual update if Renovate missed it
- Add to security alert tracking

---

## 📚 Resources

### Documentation
- Renovate Docs: https://docs.renovatebot.com/
- Configuration Reference: https://docs.renovatebot.com/configuration-options/
- Preset Configs: https://docs.renovatebot.com/presets-config/

### Support Channels
- GitHub Issues: https://github.com/renovatebot/renovate/issues
- Discord: https://discord.gg/renovate
- Stack Overflow: Tag `renovate`

### Team Knowledge Base
- Internal Wiki: (Add link to your team's documentation)
- Slack Channel: #dependencies (Create if not exists)
- Point of Contact: (Assign team member as Renovate champion)

---

## 🎯 Next Steps After Week 1

Once automated dependency updates are working smoothly, consider:

1. **Week 2-6:** Implement #6 Command Palette + #3 Transaction Integrity (parallel)
2. **Month 2-3:** Implement #1 Granular Authorization (security foundation)
3. **Month 4:** Implement #4 Policy Conflict Detection (security intelligence)

This Week 1 quick win establishes good development practices and reduces tech debt accumulation for all future work.

---

## ✅ Implementation Checklist

### Pre-Implementation
- [ ] Read this spec completely
- [ ] Assign implementation owner
- [ ] Schedule 2-hour setup window
- [ ] Notify team of upcoming change

### Implementation
- [ ] Install Renovate GitHub App
- [ ] Create renovate.json with provided config
- [ ] Create PR for renovate.json
- [ ] Merge renovate.json PR
- [ ] Review and merge Onboarding PR
- [ ] Test first update PR
- [ ] Merge first update PR

### Post-Implementation
- [ ] Monitor for issues (first week)
- [ ] Tune configuration as needed
- [ ] Document any team-specific procedures
- [ ] Train team on PR review process
- [ ] Celebrate first automated update! 🎉

---

**Status:** ✅ Ready for immediate implementation
**Owner:** (Assign)
**Target Start Date:** Monday of next sprint
**Target Completion:** End of week 1

---

*Generated from Brainstorming Session 2025-12-07*
*Part of 30-week roadmap for Neobrutalism CRM enhancement*
