# Infrastructure

This directory contains infrastructure configuration for NEXUS CORE development.

## Docker Compose

The `docker-compose.yml` file configures a PostgreSQL database for local development.

### Starting PostgreSQL

```bash
# From the infrastructure directory
docker-compose up -d
```

### Stopping PostgreSQL

```bash
docker-compose down
```

### Viewing Logs

```bash
docker-compose logs -f postgres
```

### Connecting to PostgreSQL

```bash
# Using docker exec
docker exec -it nexus-core-postgres psql -U nexus -d nexus_core

# Or using psql if installed locally
psql -h localhost -p 5432 -U nexus -d nexus_core
```

### Configuration

The PostgreSQL container is configured with:
- **Database:** nexus_core
- **Username:** nexus
- **Password:** nexus_password
- **Port:** 5432 (mapped to host)
- **Volume:** Persistent data in `postgres_data` volume
- **Health check:** Ensures database is ready before accepting connections

### Environment Variables

To use this database with the application, set these environment variables or update `application.yaml`:

```yaml
DATABASE_URL=jdbc:postgresql://localhost:5432/nexus_core
DATABASE_USERNAME=nexus
DATABASE_PASSWORD=nexus_password
```

These are already set as defaults in the application configuration.

### Resetting the Database

To completely reset the database (delete all data):

```bash
docker-compose down -v
docker-compose up -d
```

**Warning:** This will delete all data in the database.
