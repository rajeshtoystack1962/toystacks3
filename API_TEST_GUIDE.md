# File Storage API – Test Guide for Developers

**Base URL:** `https://toystack-tomcats3.toystack.dev/file-api-1.0.0`

All endpoints are under: `{BASE_URL}/api/storage/...`

---

## 1. CREATE FOLDER

### 1.1 Create a new folder (first time)
**Endpoint:** `POST /api/storage/folder?folder=<folderPath>`

```bash
curl -X POST "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/folder?folder=my-folder"
```

**Expected response (201):**
```json
{
  "message": "Folder created",
  "path": "/api/storage/my-folder",
  "folder": "my-folder"
}
```

---

### 1.2 Create nested folder (e.g. parent/child)
**Endpoint:** `POST /api/storage/folder?folder=<folderPath>`

```bash
curl -X POST "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/folder?folder=my-folder/sub-folder"
```

**Expected response (201):**
```json
{
  "message": "Folder created",
  "path": "/api/storage/my-folder/sub-folder",
  "folder": "my-folder/sub-folder"
}
```

---

### 1.3 Create same folder again (idempotent – no replace)
**Endpoint:** `POST /api/storage/folder?folder=<folderPath>`

```bash
curl -X POST "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/folder?folder=my-folder"
```

**Expected response (200):**
```json
{
  "message": "Folder already exists",
  "path": "/api/storage/my-folder",
  "folder": "my-folder"
}
```
*Existing folder is not renamed or replaced; same path is reused.*

---

### 1.4 Invalid: empty folder path
```bash
curl -X POST "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/folder?folder="
```
**Expected:** 400 Bad Request – "Folder path cannot be empty" or similar.

---

## 2. UPLOAD FILE (same route as view/download: `/api/storage`)

**Single route:** Upload = `POST /api/storage`. View and download = `GET /api/storage/<path>` (see section 3).

### 2.1 Upload file to a folder (first time)
**Endpoint:** `POST /api/storage`  
**Body:** `multipart/form-data` with `folder` and `file`

```bash
curl -F "folder=my-folder" -F "file=@/path/to/your/document.pdf" "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage"
```

**Expected response (200):**
```json
{
  "fileName": "document.pdf",
  "folder": "my-folder",
  "url": "/api/storage/my-folder/document.pdf",
  "viewUrl": "/api/storage/my-folder/document.pdf",
  "downloadUrl": "/api/storage/my-folder/document.pdf?download=true"
}
```

---

### 2.2 Upload another file in the same folder (different filename)
**Endpoint:** Same as 2.1.

```bash
curl -F "folder=my-folder" -F "file=@/path/to/another.txt" "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage"
```

**Expected response (200):** Success; new file is added to the same folder.

---

### 2.3 Upload file with same name in same folder (duplicate – must fail)
**Endpoint:** Same as 2.1. Use same `folder` and same `file` name as an existing file.

```bash
curl -F "folder=my-folder" -F "file=@/path/to/document.pdf" "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage"
```
*(Assume document.pdf already exists in my-folder.)*

**Expected response (409 Conflict):**
```json
{
  "error": "File already exists",
  "message": "A file named 'document.pdf' already exists in this folder."
}
```

---

### 2.4 Upload to nested folder
**Endpoint:** Same as 2.1; use nested path in `folder`.

```bash
curl -F "folder=my-folder/sub-folder" -F "file=@/path/to/image.png" "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage"
```

**Expected response (200):** Success; file is stored under `my-folder/sub-folder/`.

---

### 2.5 Upload file too large (> 50MB)
**Expected response (413 Payload Too Large):**
```json
{
  "error": "File too large",
  "message": "File size exceeds 50MB limit"
}
```

---

## 3. VIEW FILE and DOWNLOAD FILE (single GET route: `/api/storage/<path>`)

**Single route:** `GET /api/storage/<folder>/<fileName>`  
- **View (inline):** no query param, or open URL in browser.  
- **Download (attachment):** add `?download=true`.

**Full URL pattern:**  
- View: `{BASE_URL}/api/storage/<folder>/<fileName>`  
- Download: `{BASE_URL}/api/storage/<folder>/<fileName>?download=true`

### 3.1 View file in browser or via GET (inline)
**Endpoint:** `GET /api/storage/<folder>/<fileName>`

**Example – view a file:**
```bash
curl -i "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/document.pdf"
```

**Browser:** Open the same URL. Any file type can be viewed (inline); browser may display or prompt depending on type.

---

### 3.2 View file in nested folder
**Endpoint:** `GET /api/storage/<folderPath>/<fileName>`

**Example:**
```bash
curl -i "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/sub-folder/image.png"
```

**Expected:** 200 with file content and appropriate `Content-Type`.

---

### 3.3 Download file (attachment – same route with `?download=true`)
**Endpoint:** `GET /api/storage/<folder>/<fileName>?download=true`

**Example – save file with server filename:**
```bash
curl -OJ "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/document.pdf?download=true"
```

**Example – nested path:**
```bash
curl -OJ "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/sub-folder/image.png?download=true"
```

**Expected:** 200 with file content; `Content-Disposition: attachment` so the file is downloaded.

---

### 3.4 View non-existent file
**Example:**
```bash
curl -i "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/does-not-exist.pdf"
```

**Expected response (404):**
```json
{
  "error": "File not found",
  "message": "File does not exist at: ...",
  "resolvedPath": "..."
}
```

---

## 5. DELETE FILE

### 5.1 Delete a file
**Endpoint:** `DELETE /api/storage/delete/<folder>/<fileName>`

**Example:**
```bash
curl -X DELETE "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/delete/my-folder/document.pdf"
```

**Expected response (200):**
```json
{
  "message": "File deleted",
  "path": "document.pdf"
}
```

---

### 5.2 Delete file in nested folder
**Example:**
```bash
curl -X DELETE "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/delete/my-folder/sub-folder/image.png"
```

**Expected response (200):** Same structure as 5.1.

---

### 5.3 Delete non-existent file
**Example:**
```bash
curl -X DELETE "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/delete/my-folder/does-not-exist.pdf"
```

**Expected response (404):** File not found.

---

### 5.4 Delete with directory path (must fail)
**Endpoint:** Only file paths are supported; folder paths must be rejected.

**Expected response (400):** "Cannot delete directory" or similar – only files can be deleted.

---

## 6. ENDPOINT SUMMARY

| Action        | Method | Endpoint |
|---------------|--------|----------|
| Create folder | POST   | `/api/storage/folder?folder=<path>` |
| Upload file   | POST   | `/api/storage` (form: `folder`, `file`) |
| View file     | GET    | `/api/storage/<folder>/<fileName>` |
| Download file | GET    | `/api/storage/<folder>/<fileName>?download=true` |
| Delete file   | DELETE | `/api/storage/delete/<folder>/<fileName>` |

**Single route for upload / view / download:**  
- **POST** `/api/storage` → upload (body: `folder` + `file`).  
- **GET** `/api/storage/<path>` → view (inline).  
- **GET** `/api/storage/<path>?download=true` → download (attachment).

**Base URL:** `https://toystack-tomcats3.toystack.dev/file-api-1.0.0`

Replace `<folder>` with the folder path (e.g. `my-folder` or `my-folder/sub-folder`). Replace `<fileName>` with the actual file name. Use the full URL: `{BASE_URL}/api/storage/...` for all requests.

---

## 7. SUPPORTED FILES

- **Upload:** Any file type; max **50 MB** per file.
- **View:** Any file type (images, PDF, docs, etc.) – no extension blocklist.
