# Rooms and group membership

All endpoints require JWT authentication. Caller identity is obtained from the authenticated principal, never from request JSON.

- `POST /api/rooms/groups` creates a group; creator becomes OWNER and initial invitees become MEMBER.
- `POST /api/rooms/private` creates or returns the unique private room for the caller and exactly one other active user.
- `GET /api/rooms?page=0&size=20`, `GET /api/rooms/{roomId}`, and `GET /api/rooms/{roomId}/participants` list accessible rooms and active members.
- `PATCH /api/rooms/{roomId}` lets owners/admins update group metadata.
- `POST /api/rooms/{roomId}/participants` lets owners/admins add members. Admins may remove regular members; only owners may remove admins.
- `DELETE /api/rooms/{roomId}/participants/{userId}` removes a member or lets a member leave. `POST /api/rooms/{roomId}/leave` leaves a group. Membership rows retain history via `left_at`.
- `PATCH /api/rooms/{roomId}/participants/{userId}/role` assigns MEMBER/ADMIN. `POST /api/rooms/{roomId}/owner/{userId}` transfers ownership. A partial unique index enforces at most one active owner.
- `PATCH /api/rooms/{roomId}/me/settings` updates caller-specific mute/archive settings. Private conversations cannot be left; archive instead.

Private room keys are derived from sorted UUID pairs. Participant/room operations use row locks for concurrent membership and ownership changes. Room-list membership queries batch participant loading for the current page.
