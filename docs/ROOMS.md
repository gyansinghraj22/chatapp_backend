# Rooms and group membership

All endpoints require JWT authentication. Caller identity is derived from the authenticated principal.

- `POST /api/rooms/groups` creates a group; caller becomes OWNER and initial users are MEMBERS.
- `POST /api/rooms/private` creates or returns the unique private room with exactly one other active user.
- `GET /api/rooms?page=0&size=20`, `GET /api/rooms/{roomId}`, and `GET /api/rooms/{roomId}/participants` list accessible rooms and active members.
- `PATCH /api/rooms/{roomId}` lets owners/admins update group metadata.
- `POST /api/rooms/{roomId}/participants` adds members for owners/admins. Admins may remove regular members; only an owner can remove admins.
- `DELETE /api/rooms/{roomId}/participants/{userId}` removes a member or lets a member leave. `POST /api/rooms/{roomId}/leave` leaves a group. Membership rows are retained with `left_at`.
- `PATCH /api/rooms/{roomId}/participants/{userId}/role` assigns MEMBER/ADMIN. `POST /api/rooms/{roomId}/owner/{userId}` transfers ownership. The database enforces at most one active owner, and service locking protects concurrent role/member operations.
- `PATCH /api/rooms/{roomId}/me/settings` changes caller-specific mute/archive settings. Private conversations cannot be left; archive them instead.

Private room keys are derived from the sorted pair of UUIDs and protected by a partial unique database index. A room-list query batches participants for the selected room page to avoid an N+1 query pattern.
