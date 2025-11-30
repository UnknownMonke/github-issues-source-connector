# Custom Kafka Connect Source Connector

- Fetches list of issues from any Github repository into a Kafka topic.

## Configuration

- Example file : `config/example-connector.properties`.

- Available options :

    - `name` : Logical name of the connector. Used for identification in configuration and logs.

    - `tasks.max` : Maximum number of parallel tasks the connector may start.

    - `connector.class` :

        - FQCN of implementation class : `org.monke.connector.GithubIssuesSourceConnector`.

    - `topic` : Sink topic name.

    - `connector.plugin.version` : Expected plugin / JAR version for this connector (ex : `1.0.0`).

    - `github.owner` : Owner (organization or user) of repository to monitor.

    - `github.repo` : Name of repository to monitor.

    - `since.timestamp` : ISO 8601 timestamp indicating the start point for retrieving issues (e.g., `2020-01-01T00:00:00Z`). Issues older than this are ignored.

    - `batch.size` : Maximum page size per request when fetching issues.

    - `auth.username` (optional) : Username for authentication if required. Leave commented or empty if not needed.

    - `auth.password` (optional) : Password or token for authentication. Prefer storing this via a secure mechanism rather than in plain text.