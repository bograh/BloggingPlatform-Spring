# UPDATED ENDPOINTS

This document captures endpoint updates after the async/concurrency feature rollout and the **post creation + image upload integration**.

## Updated Existing Endpoint

### Post Creation Endpoints

#### 1) JSON create post
- **Path:** `/api/posts/old`
- **Content-Type:** `application/json`
- **Body:** `CreatePostDTO`
- **Purpose:** Create post without image

#### 2) Multipart create post with optional image
- **Path:** `/api/posts`
- **Content-Type:** `multipart/form-data`
- **Parts:**
  - `post` (required): JSON object matching `CreatePostDTO`
  - `image` (optional): image file (`image/jpeg`, `image/png`, `image/gif`, `image/webp`)
- **Purpose:** Create post and trigger async image upload in one request
- **Post response update:** `PostResponseDTO` now includes `imageUrls` (list of image CDN URLs)

Security note:
- `POST /api/posts` requires role `AUTHOR` by URL security rule.
- `POST /api/posts/old` falls under default authentication rules (not explicitly `permitAll`).

Example multipart payload:
- `post`: `{ "title": "My title", "body": "My body", "tags": ["spring", "java"] }`
- `image`: binary file

---

## New Feed & Trending Endpoints

### `GET /api/feed`
- Aggregates recent, trending, and popular feeds asynchronously.
- Query params: `limit` (default `10`)
- Request Body: None
- Response Body (200):
```json
{
  "status": "success",
  "message": "Feed aggregated successfully",
  "data": {
    "recentPosts": [
      {
        "postId": 1,
        "title": "Post title",
        "bodyPreview": "Short preview...",
        "author": "authorName",
        "authorId": "user-uuid",
        "tags": ["spring", "java"],
        "postedAt": "2026-02-24T10:00:00",
        "totalComments": 12,
        "trendingScore": 0.0,
        "viewCount": 0,
        "feedSource": "RECENT"
      }
    ],
    "trendingPosts": [],
    "popularPosts": [],
    "recommendedPosts": [],
    "generatedAt": "2026-02-24T10:00:01",
    "generationTimeMs": 34,
    "totalItemsAggregated": 1
  }
}
```

### `GET /api/feed/trending/live`
- Returns live trending scores and movement metadata.
- Query params: `limit` (default `10`)
- Request Body: None
- Response Body (200):
```json
{
  "status": "success",
  "message": "Live trending data retrieved",
  "data": {
    "trendingPosts": [
      {
        "postId": 1,
        "title": "Trending post",
        "currentScore": 420.5,
        "previousScore": 390.2,
        "scoreChange": 30.3,
        "commentCount": 40,
        "viewCount": 0,
        "velocityFactor": 12.1,
        "lastUpdated": "2026-02-24T10:01:00",
        "rank": 1,
        "previousRank": 2,
        "trend": "UP"
      }
    ],
    "snapshotTime": "2026-02-24T10:01:00",
    "calculationTimeMs": 12,
    "totalTrackedPosts": 50,
    "liveUpdate": true
  }
}
```

### `POST /api/feed/trending/refresh`
- Forces immediate refresh of trending score snapshots.
- Request Body: None
- Response Body (200):
```json
{
  "status": "success",
  "message": "Trending scores refreshed",
  "data": "OK"
}
```

---

## New Moderation Endpoints

### `POST /api/moderation/bulk`
- Queues bulk comment moderation task (async).
- Requires admin role.

### `GET /api/moderation/tasks/{taskId}`
- Gets moderation task status/progress.
- Requires admin role.

### `GET /api/moderation/tasks`
- Lists moderator task history.
- Query params: `limit` (default `20`)
- Requires admin role.

---

## New Notification Outbox Endpoints

### `POST /api/notifications`
- Queues notification for async outbox processing.
- Requires admin role.

### `GET /api/notifications/{notificationId}`
- Retrieves notification delivery status.
- Requires admin role.

### `GET /api/notifications/stats`
- Retrieves outbox processing statistics.
- Requires admin role.

---

## New Report Export Endpoints

### `POST /api/reports/export`
- Starts async report generation job.
- Requires admin role.

### `GET /api/reports/download/{reportId}`
- Downloads generated report file.
- Requires admin role.

### `GET /api/reports/{reportId}`
- Gets report generation status and metadata.
- Requires admin role.

### `GET /api/reports`
- Lists report exports for current user.
- Query params: `limit` (default `20`)
- Requires admin role.

---

## Security Alignment (Implementation)

- Public for all methods: `/api/auth/**`, `/oauth2/**`, `/login/oauth2/**`, `/graphql`, `/graphiql`, docs endpoints, `/actuator/health`.
- Public GET only: `/api/posts/**`, `/api/tags/**`.
- Public feed routes: `/api/feed/**` for all methods.
- Admin-protected: `/api/admin/**`, `/api/users/**`, `/api/metrics/performance/**`, `/api/security/audit/**`, `/actuator/**`.

---

## New Image Management Endpoints

### `POST /api/images/upload/{postId}`
- Starts async image upload for existing post.

### `GET /api/images/status/{imageId}`
- Gets image upload status.

### `GET /api/images/post/{postId}`
- Lists all images for a post.

### `GET /api/images/post/{postId}/completed`
- Lists completed images for a post.

### `POST /api/images/retry/{imageId}`
- Retries failed image upload.

### `DELETE /api/images/{imageId}`
- Deletes image and associated stored files.
