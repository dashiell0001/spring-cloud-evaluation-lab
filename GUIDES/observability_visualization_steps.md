# Observability — How to Visualize **Traces, Logs, and Metrics** (Step‑by‑Step)

This add‑on guide explains **exactly how to see your data** once the stack is up:
- **Traces** in **Jaeger**
- **Logs** in **Grafana → Explore → Loki**
- **Metrics** in **Grafana dashboards** (Prometheus datasource)

> Assumptions
> - You started the compose in `docker-observability` and it includes **Prometheus, Grafana, Loki, Promtail** (and optionally **Jaeger**).
> - Your apps expose `/actuator/prometheus` and write JSON logs to `./logs/<service>.log` (via `logback-spring.xml`).
> - Micrometer Tracing exports to OTLP gRPC at `http://localhost:4317` (if you enabled tracing).

---

## 0) Generate Some Traffic
Run a few requests to produce traces/logs/metrics:
```
# Through API Gateway if you have it (example path)
curl "http://localhost:8081/api/flights/search?origin=MEX&destination=LAX&dateFrom=2025-12-20&dateTo=2025-12-28"

# Direct hit to booking
curl -X POST "http://localhost:8083/api/bookings" \
  -H "Content-Type: application/json" \
  -d '{"flightNumber":"AA123","seats":["12A","12B"],"passengers":[{"type":"ADULT"}]}'
```
Repeat a few times to populate data.

---

## 1) Visualize **Traces** (Jaeger)

> If you included **Jaeger** in compose (ports `16686` UI and `4317` OTLP), follow these steps.

1. Open **Jaeger UI**: `http://localhost:16686`  
2. Top left, select **Service** (e.g., `api-gateway`, `flight-search-service`, `pricing-service`, `booking-service`).  
3. (Optional) Set a time range and click **Find Traces**.  
4. Click a trace → you’ll see the **span waterfall** across services.  
   - Verify **traceId** and **spanId** match what you see in logs.  
   - Expand spans to inspect attributes (HTTP method, URL, status, timings).

**Troubleshooting**  
- No traces? Check your apps:
  - `management.tracing.sampling.probability: 1.0`
  - `otel.exporter.otlp.endpoint: http://localhost:4317`
  - Make a few requests and refresh.

---

## 2) Visualize **Logs** (Grafana → Explore → Loki)

1. Open **Grafana**: `http://localhost:3000` (default `admin/admin`)
2. Add **Loki** as a data source (if not pre‑configured):
   - **Data sources** → **Add data source** → search **Loki**
   - URL: `http://host.docker.internal:3100` (or `http://localhost:3100` depending on your setup)
   - **Save & Test**
3. Go to **Explore** (left nav) → select your **Loki** datasource.
4. In the query bar, start simple:  
   - All logs: `{job="spring-microservices"}`  
   - Filter by service file path: `{filename=~".*flight-search-service.*"}`  
   - Filter by JSON field (logstash encoder): `|= "INFO"` or parse fields:
     - Click **Parser** → **Loki** can parse JSON automatically if your log lines are valid JSON.
     - Once parsed, you can filter: `| json | service_name="flight-search-service"`
5. Click any log line to expand and see full JSON (e.g., `traceId`, `spanId`, `log_message`).

**Create a Logs Panel (optional)**  
1. **Dashboards → New → New dashboard → Add visualization**.  
2. Choose **Loki**, paste your LogQL, e.g.:  
   ```
   {job="spring-microservices"} | json | service_name="flight-search-service"
   ```
3. Save the dashboard (give it a name like “MS Observability”).

**Troubleshooting**  
- No logs? Validate that files exist under `./logs/<service>.log` and that Promtail is mounting the parent folder (e.g., `../services:/var/log/services:ro`).  
- Ensure your `promtail-config.yml` contains a `__path__` that matches your file structure, e.g.:  
  ```yaml
  __path__: /var/log/services/*/logs/*.log
  ```

---

## 3) Visualize **Metrics** (Grafana dashboards via Prometheus)

### 3.1 Add Prometheus as a data source
1. In Grafana, **Data sources → Add data source → Prometheus**.
2. URL: `http://host.docker.internal:9091` (if Prometheus is exposed at 9091 on the host).  
   - If you used 9090 instead: `http://host.docker.internal:9090`
3. **Save & Test**.

### 3.2 Quick Panels for your **custom metrics**
Create a dashboard with two panels (plus some system panels if you want):

**Panel A — Flight searches per minute**  
- Visualization: **Time series**  
- Query (PromQL):
  ```promql
  rate(flight_search_total[1m])
  ```
- Legend: `flight searches / s` (Prometheus computes per‑second rate by default).  
- Min step: `1s` or leave default.  
- Set **Unit**: `ops/s` or `req/s`.

**Panel B — Bookings per minute**  
- Visualization: **Time series**  
- Query:
  ```promql
  rate(booking_created_total[1m])
  ```
- Unit: `ops/s`

**(Optional) Panel C — HTTP request rate**  
- Query (Micrometer default):
  ```promql
  sum(rate(http_server_requests_seconds_count[1m])) by (status)
  ```
- Legend: `{{status}}`

**(Optional) Panel D — HTTP latency (p95)**  
- Query:
  ```promql
  histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))
  ```
- Unit: `s`

### 3.3 Verify that Prometheus is scraping your services
Open Prometheus UI:
- `http://localhost:9091` → **Status → Targets** → Should display targets **UP** for each service.  
- If DOWN → check the hosts/ports in `prometheus.yml` and that the services expose `/actuator/prometheus`.

**Troubleshooting**  
- Custom counters not visible? Hit the endpoints first (metrics only appear after the first increment).  
- Missing `http_server_requests_*`? Ensure you’re using Spring Boot Actuator and that `/actuator/prometheus` is exposed.

---

## 4) Minimal “Smoke Test” Checklist

- **Logs**: In Grafana → Explore → Loki, run `{job="spring-microservices"}` and see lines appear when you hit endpoints.  
- **Metrics**: In Grafana dashboards, both `rate(flight_search_total[1m])` and `rate(booking_created_total[1m])` move after traffic.  
- **Traces**: In Jaeger, you can find traces for `api-gateway` and downstream services; spans show timing and status.

---

## 5) Example PromQL Cheatsheet

- 5xx error rate:
  ```promql
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m]))
  ```

- Average latency:
  ```promql
  sum(rate(http_server_requests_seconds_sum[1m]))
  /
  sum(rate(http_server_requests_seconds_count[1m]))
  ```

- In‑flight requests (if using `tomcat_threads_busy` or similar):
  ```promql
  tomcat_threads_busy
  ```

---

## 6) Common Pitfalls

- **Wrong hostnames for metrics**: If services run on the host, Prometheus must target `host.docker.internal:<port>` from inside the container.  
- **Promtail path mismatch**: Ensure the mounted path matches the `__path__` glob in `promtail-config.yml`.  
- **No traces**: Confirm OTLP exporter endpoint and sampling probability; generate traffic and refresh the timeframe in Jaeger.  
- **No custom metrics**: Ensure your code calls `Counter.increment()` after successful operations.

---

## 7) Quick Reference (URLs)

| Component   | URL                         |
|-------------|-----------------------------|
| Grafana     | http://localhost:3000       |
| Prometheus  | http://localhost:9091       |
| Loki (API)  | http://localhost:3100       |
| Jaeger UI   | http://localhost:16686      |

---

**You’re done!** You now have a repeatable recipe to _see_ what’s happening:  
- **Traces** for request flows across services,  
- **Logs** for structured debugging tied to trace IDs,  
- **Metrics** for dashboards and alerts.
