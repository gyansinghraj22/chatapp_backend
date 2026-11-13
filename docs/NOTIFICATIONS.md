# Notifications and FCM dispatch

In-app notifications are persisted with an outbox row in the same transaction as message processing. Dispatch claims rows using PostgreSQL `FOR UPDATE SKIP LOCKED`; crashed `PROCESSING` claims can be reclaimed after five minutes. Failures use exponential retry backoff and eventually move to `DEAD`. FCM acceptance only means Firebase accepted a send request, not that a device received or displayed it.

Register a device with `POST /api/notifications/devices` and `{ "token": "...", "platform": "ANDROID|IOS|WEB" }`. Raw FCM tokens are never stored. They are AES-256-GCM encrypted with `FCM_TOKEN_ENCRYPTION_KEY` and indexed by SHA-256 hash. Generate and store the encryption key in a secret manager; do not rotate it without a token re-encryption plan.

Preferences are available through `GET/PUT /api/notifications/preferences`; notification history through `GET /api/notifications`; mark read through `PATCH /api/notifications/{id}/read`; unregister through `DELETE /api/notifications/devices?token=...`.

Enable Firebase by setting `FIREBASE_ENABLED=true` and configuring either `FIREBASE_CREDENTIALS_FILE` or Application Default Credentials. `FIREBASE_PROJECT_ID` is optional when present in credentials. Configure `NOTIFICATION_DISPATCH_ENABLED` and `NOTIFICATION_DISPATCH_INTERVAL_MS` as required. Disable Firebase by default for local development; outbox rows remain durable but no external notification is sent when Firebase is disabled.
