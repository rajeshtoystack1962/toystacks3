# toystacks3

WAR-based private object storage service (S3-like basic behavior) running on Tomcat.

## Configure credentials (.env style values)

1. Create a `.env` file in project root:

```env
STORAGE_ACCESS_KEY=my-access-key-001
STORAGE_SECRET=my-super-secret-001
```

2. Build Docker image:

```bash
docker build -t toystacks3-storage .
```

3. Run container with env values:

```bash
docker run --rm \
  -p 8080:8080 \
  --env STORAGE_ACCESS_KEY=my-access-key-001 \
  --env STORAGE_SECRET=my-super-secret-001 \
  -v "$(pwd)/uploads:/data/uploads" \
  toystacks3-storage
```

4. App URLs:
- UI login: `http://localhost:8080/storage/login`
- API base: `http://localhost:8080/storage/api/storage`

Use the same key/secret for:
- UI login form (`Access Key`, `Secret`)
- API headers (`X-Storage-Key`, `X-Storage-Secret`)

## Kubernetes env configuration

Pass credentials as environment variables in your Deployment:

```yaml
env:
  - name: STORAGE_ACCESS_KEY
    value: "my-access-key-001"
  - name: STORAGE_SECRET
    value: "my-super-secret-001"
```
