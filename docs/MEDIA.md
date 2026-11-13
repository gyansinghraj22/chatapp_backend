# Media upload

`POST /api/media/upload` accepts a multipart part named `file` and requires a valid JWT. The service enforces the configured byte limit, detects content from file bytes (not only the client-supplied MIME type), allows only JPEG/PNG/GIF/WebP/PDF/plain text/CSV, stores an opaque generated key, and records ownership plus metadata. The uploaded filename is display metadata only and never forms a filesystem or object-storage path.

`GET /api/media/{mediaId}` is private and owner-authorized. Files are stored outside PostgreSQL. Local development uses `MEDIA_BACKEND=local` and `MEDIA_LOCAL_ROOT`; production can use `MEDIA_BACKEND=s3` with `MEDIA_S3_BUCKET`, `MEDIA_S3_REGION`, optional `MEDIA_S3_ENDPOINT`, and the AWS default credential chain. S3 objects are uploaded with AES256 server-side encryption. Do not make the bucket public; provision restrictive IAM access and TLS.

The V2 Flyway migration creates `media_uploads` records with a 24-hour staging expiry. A cleanup worker should delete expired unreferenced objects/rows before production rollout; upload-to-message linking should atomically validate owner and status before creating `message_attachments` and marking the upload ATTACHED. The upload endpoint intentionally does not trust a client-supplied `storage_key`.
