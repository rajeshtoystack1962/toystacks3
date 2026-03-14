# File Storage API – Correct Endpoints

**Base URL:** `https://toystack-tomcats3.toystack.dev/file-api-1.0.0`

---

## CREATE FOLDER

**Ex: Create a new folder**
```bash
curl -X POST "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/folder?folder=my-folder"
```

**Ex: Create nested folder**
```bash
curl -X POST "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/folder?folder=my-folder/sub-folder"
```

**Ex: Create same folder again (idempotent – no replace)**
```bash
curl -X POST "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/folder?folder=my-folder"
```

---

## UPLOAD FILE

*Use `POST /api/storage` (no `/upload` in path).*

**Ex: Upload file**
```bash
curl -F "folder=my-folder" -F "file=@/path/to/your/document.pdf" "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage"
```

**Ex: Upload another file in the same folder**
```bash
curl -F "folder=my-folder" -F "file=@/path/to/another.txt" "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage"
```

**Ex: Upload file with same name in same folder (duplicate – must fail)**
```bash
curl -F "folder=my-folder" -F "file=@/path/to/document.pdf" "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage"
```
*Expected response (409):* `"error": "File already exists"`, `"message": "A file named 'document.pdf' already exists in this folder."`

**Ex: Upload to nested folder**
```bash
curl -F "folder=my-folder/sub-folder" -F "file=@/path/to/image.png" "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage"
```

**Ex: Upload file too large (> 50MB)**  
*Expected response (413):*
```json
"error": "File too large",
"message": "File size exceeds 50MB limit"
```

---

## VIEW FILE

*Use `GET /api/storage/<folder>/<fileName>` (no `/view` in path).*

**Ex: View file**
```bash
curl -i "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/document.pdf"
```

**Ex: View file in nested folder**
```bash
curl -i "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/sub-folder/image.png"
```

**Ex: View non-existent file**
```bash
curl -i "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/does-not-exist.pdf"
```
*Expected response (404):* `"error": "File not found"`, `"message": "File does not exist at: ..."`, `"resolvedPath": "..."`

---

## DOWNLOAD FILE

*Use same path as view, with `?download=true` (no `/download` in path).*

**Ex: Download file**
```bash
curl -OJ "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/document.pdf?download=true"
```

**Ex: Download file in nested folder**
```bash
curl -OJ "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/sub-folder/image.png?download=true"
```

---

## DELETE FILE

*Use `DELETE /api/storage/<folder>/<fileName>` (same path as view/download, no `/delete` in path).*

**Ex: Delete file**
```bash
curl -X DELETE "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/document.pdf"
```

**Ex: Delete file in nested folder**
```bash
curl -X DELETE "https://toystack-tomcats3.toystack.dev/file-api-1.0.0/api/storage/my-folder/sub-folder/image.png"
```

---

## SUMMARY – Correct paths (single route for upload / view / download / delete)

| Action        | Method | Correct endpoint |
|---------------|--------|------------------|
| Create folder | POST   | `/api/storage/folder?folder=<path>` |
| Upload file   | POST   | `/api/storage` (form: `folder`, `file`) |
| View file     | GET    | `/api/storage/<folder>/<fileName>` |
| Download file | GET    | `/api/storage/<folder>/<fileName>?download=true` |
| Delete file   | DELETE | `/api/storage/<folder>/<fileName>` |

**Single route:** Same path `/api/storage/<folder>/<fileName>` for:
- **GET** → view (inline)
- **GET** `?download=true` → download (attachment)
- **DELETE** → delete file
