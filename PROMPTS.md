# TrustVault - AI Usage Record

## Entry 1: Render Deployment

- Date: 2026-10-23
- Problem: I needed to deploy my Spring Boot application so that it could be accessed online.
- AI Prompt: Help me deploy my TrustVault Spring Boot application on Render.
- AI Assistance: I used AI guidance to configure the deployment and Dockerfile.
- My Action: I connected my GitHub repository to Render and deployed the application.
- Result: TrustVault became accessible through a Render live URL. I later identified database persistence as a limitation that needs attention.
- Related Commit: Add Dockerfile for Render deployment
- GitHub Commit URL: https://github.com/prasanthi630/TrustVault/commit/be505e3

## Entry 2: Document Upload, Download and Encryption

- Date: 2026-10-23
- Problem: I needed document upload, download, encryption and deletion to work correctly.
- AI Prompt: Help me troubleshoot document handling and file encryption in TrustVault.
- AI Assistance: I used AI guidance to review document handling and encryption-related code.
- My Action: I updated and tested the document handling features.
- Result: The document operations worked during my testing.
- Related Commit: Fix document upload, download, encryption and delete
- GitHub Commit URL: https://github.com/prasanthi630/TrustVault/commit/e77cc21

## Entry 3: Security Configuration and Login Validation

- Date: 2026-10-24
- Problem: I needed to protect application routes and validate user login correctly.
- AI Prompt: Help me troubleshoot Spring Security configuration and login validation.
- AI Assistance: I used AI guidance to review authentication and access-control rules.
- My Action: I updated the security configuration and tested login behaviour.
- Result: Login and protected application features were tested.
- Related Commit: Updated TrustVault security configuration
- GitHub Commit URL: https://github.com/prasanthi630/TrustVault/commit/517028c

## Entry 4: Document Sharing and Repository Error

- Date: 2026-10-24
- Problem: The document sharing code had a repository method compilation problem.
- AI Prompt: Help me fix the DocumentShareRepository compilation error.
- What Went Wrong: The service called findBySharedByIgnoreCase(), but the repository method was not available in the compiled code.
- My Action: I reviewed the repository methods, added the required method, and checked the application again.
- Result: I continued testing the document sharing feature on the deployed application.
- Related Commit: Improve document handling and login validation
- GitHub Commit URL: https://github.com/prasanthi630/TrustVault/commit/3f0ddb9

## Entry 5: Secure Account Deletion

- Date: 2026-10-25
- Problem: Account deletion needed to verify the user's password and handle related documents and sharing records safely.
- AI Prompt: Help me implement secure account deletion in TrustVault.
- AI Assistance: I used AI guidance to review password verification, related sharing records, and document cleanup.
- My Action: I implemented and tested account deletion with valid and invalid passwords.
- Result: Account deletion worked during my tests, and I checked that unrelated users' documents remained available.
- Security Consideration: Account deletion must not allow one user to delete another user's account or documents.
- Related Commit: Add profile management and secure account deletion
- GitHub Commit URL: https://github.com/prasanthi630/TrustVault/commit/aab4c2b