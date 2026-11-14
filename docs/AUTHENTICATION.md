# Authentication and access tokens

- `POST /api/auth/register`, `/login`, `/google`, `/refresh`, `/logout`, `/forgot-password`, `/reset-password`, `/change-password`.
- `GET /api/users/me` reads the authenticated profile; `PATCH /api/users/me` updates username, full name, bio, or profile picture URL.
- Access JWTs are short-lived. Refresh secrets are random, stored only as SHA-256 hashes, rotated on refresh, and revoked on logout or password change.
- Google Sign-In accepts a Google ID token; the backend verifies its signature, issuer, expiration, configured audience, and verified email before linking/creating an account. Configure `GOOGLE_CLIENT_ID`.
- Password reset tokens are random, persisted as hashes, expire in a short configured interval, and are one-use. `POST /forgot-password` always returns a generic response to avoid account enumeration. Email delivery is controlled by `PASSWORD_RESET_EMAIL_ENABLED`; configure SMTP and `MAIL_FROM` before enabling it.
- Configure `JWT_SECRET` as Base64 encoding of a cryptographically random key of at least 32 bytes. Never use an empty/default secret in production. Resetting passwords revokes all refresh-token sessions; already-issued access tokens expire naturally within the configured short lifetime.
