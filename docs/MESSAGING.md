# Message sending and history

## REST

- `POST /api/rooms/{roomId}/messages` accepts a client-generated `clientMessageId`, content, type, and optional in-room reply target. Sender identity is always read from the JWT principal.
- `GET /api/rooms/{roomId}/messages?size=50&beforeCreatedAt=<instant>&beforeMessageId=<uuid>` returns a stable descending keyset page. Omit both cursor values to get the newest page; do not provide only one cursor field. Page size is 1-100.

## STOMP

Connect to `/ws` using a STOMP `CONNECT` native header `Authorization: Bearer <access-token>`. Send to `/app/chat.send` with `{ "roomId": "...", "message": { "clientMessageId": "...", "content": "...", "messageType": "TEXT" } }`. Subscribe to `/topic/rooms/{roomId}/messages` (membership is verified at subscription) or `/user/queue/errors` for errors. The service commits the message before the controller broadcasts it to the room topic. The message idempotency key is unique per sender; retries return the already-persisted message.

The current simple in-memory broker is appropriate for a single instance. Production multi-instance fan-out should replace it with an external broker relay or Redis-backed event distribution. Broker enqueue is not a delivery receipt; client delivery/read receipts are a separate operation and must not be inferred from `convertAndSend` success.
