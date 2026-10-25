
# TrustVault - Threat Model

## 1. Purpose

This document identifies security threats to TrustVault,
a Spring Boot application used to upload, encrypt, store,
view, download, and share documents.

The purpose is to identify risks, describe existing controls,
and record tests and improvements needed.

## 2. Important Assets

- User accounts and passwords
- Uploaded documents and their contents
- AES encryption key
- Document sharing permissions
- Database records
- Audit logs
- User sessions

## 3. Threats and Mitigations

### Threat 1: Unauthorized Document Access

**Risk:** A user may try to access another user's document
by changing the document ID in a URL.

Existing control: Document ownership checks are implemented
in the document service and controller.

Test: Log in as User A and try to access User B's document.

Expected result: Access should be denied.

Status: Test with two separate accounts on the live website.

### Threat 2: Unauthorized Shared Document Download

Risk: A recipient with VIEW permission may try to download
a document without DOWNLOAD permission.

Existing control: The shared document download service
checks the sharing permission.

Test: Share a document with VIEW permission and try its
download endpoint as the recipient.

Expected result: The request should be denied.

Status: Verify the result on the deployed application.

### Threat 3: Unauthorized Sharing or Revocation

Risk: A user may try to share a document they do not own
or revoke another user's sharing permission.

Existing control: Server-side ownership and share-owner
checks are implemented in the service layer.

Test: Use a second account to attempt sharing another
user's document and revoking that user's share.

Expected result: Both unauthorized operations should fail.

Status: Test and record the actual results.

### Threat 4: Loss of the Encryption Key

Risk: If the AES encryption key is lost, encrypted documents
may no longer be decryptable.

Existing control: Documents are encrypted using
AES/GCM/NoPadding, and the key is stored separately as
trustvault.key.

Limitation: The key must be securely backed up and kept
available across deployments and restarts.

Improvement: Use secure persistent secret storage and
restrict access to the key.

Status: Persistent key storage needs verification.

### Threat 5: Database or File Loss After Restart

Risk: Accounts, sharing records, or uploaded files may
disappear if the hosting environment loses local filesystem data.

Existing limitation: The application uses an H2 file database
and stores encrypted uploads on the filesystem.

Improvement: Use a persistent managed database and
persistent file storage.

Status: Persistence on the Render Free deployment
needs to be addressed.

### Threat 6: Malicious or Oversized File Uploads

Risk: An attacker may upload unsupported, misleading,
or excessively large files.

Existing control: The application restricts allowed file
extensions and limits uploads to 10 MB.

Limitation: Extension checks alone do not prove that a file's
contents are safe or match the claimed file type.

Improvement: Validate file content and MIME type on the server,
and keep strict size limits.

Status: Test rejected extensions and oversized files.

### Threat 7: Unauthorized Login Attempts

Risk: An attacker may attempt to guess passwords or
reuse another user's credentials.

Existing control: Passwords are stored using BCrypt hashing,
and Spring Security protects authenticated routes.

Improvement: Consider login rate limiting, stronger password
policies, and monitoring repeated failed login attempts.

Status: Verify authentication behaviour and document
any controls that are not yet implemented.

### Threat 8: Missing or Incorrect Audit Records

Risk: Important actions may not be recorded, making it
difficult to investigate unauthorized document access.

Existing control: The application records actions such as
document upload, view, download, and deletion.

Test: Perform each action and verify the corresponding
audit record is created for the correct user.

Improvement: Protect audit records against unauthorized
modification and avoid storing sensitive document contents
in logs.

Status: Verify audit coverage and access controls.

## 4. Testing Principles

- Use only test accounts and test documents.
- Test access control using two separate user accounts.
- Verify permissions on the server, not only in the frontend.
- Check that failed operations do not expose document contents.
- Record actual results and unresolved issues honestly.

## 5. Known Limitations

- H2 file database persistence on Render Free needs improvement.
- Uploaded-file and encryption-key persistence need verification.
- Additional upload validation and login abuse protection
  may be required.
- Security tests must be completed and documented before
  claiming that a control has been verified.
