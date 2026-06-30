Please run a security review of pending changes on the current branch.

This command provides a portable security review for AI coding tools that lack a built-in security review capability. Claude Code users should use the native `/security-review` skill instead of this command.

## Steps

Follow these steps:

1. **Identify changes to review.** Run `git diff main...HEAD --name-only` to get the list of files modified on the current branch.

2. **Scan for common vulnerability patterns (OWASP Top 10).** For each modified file, check for:
   - **Injection**: SQL injection, command injection, XSS (cross-site scripting)
   - **Broken authentication**: hardcoded credentials, weak session management
   - **Sensitive data exposure**: secrets, API keys, tokens in source code
   - **Security misconfiguration**: overly permissive CORS, debug flags in production
   - **Insecure deserialization**: unsafe parsing of untrusted input
   - **Logging & monitoring**: sensitive data in logs

3. **Check for secrets and credentials.** Scan committed files for:
   - API keys, tokens, passwords in source code or configuration
   - Private keys or certificates
   - Environment-specific secrets that should be in `.env` or secret management

4. **Review input validation and output encoding.** For each modified file:
   - Verify user inputs are validated and sanitised
   - Check output encoding to prevent injection attacks
   - Review boundary checks for arrays and collections

5. **Report findings.** Present results using this format:

   ```
   ## Security Review Results

   ### RESULT: PASS or FAIL

   PASS if no critical or high-severity findings.
   FAIL if any critical or high-severity findings exist.

   ### Findings (if any)
   - Severity: [CRITICAL/HIGH/MEDIUM/LOW]
   - Description: [what the issue is]
   - Location: [file:line]
   - Recommendation: [how to fix]
   ```

<!-- TODO: This is a placeholder implementation. Expand with more comprehensive checks:
     - Dependency vulnerability scanning
     - Static analysis integration
     - Platform-specific security checks (e.g. Android manifest permissions, intent filters)
     - Data storage security (encrypted SharedPreferences, Room encryption)
     - Network security configuration review
-->
