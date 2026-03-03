# Documentation Index

Use this index to navigate implementation-aligned docs by area.

## Core

- [Database Schema](DATABASE_SCHEMA.md)
- [Database Quick Reference](DATABASE_QUICK_REFERENCE.md)
- [ER Diagram](ER_DIAGRAM.md)
- [Lab Review Guide](LAB_REVIEW_GUIDE.md)

## API

- [OpenAPI Documentation Guide](api/OPENAPI_DOCUMENTATION_GUIDE.md)
- [Updated Endpoints](api/UPDATED_ENDPOINTS.md)

## Security

- [Security Architecture](security/security-architecture.md)
- [JWT Flow](security/jwt-flow.md)
- [OAuth2 Flow](security/oauth2-flow.md)
- [RBAC](security/rbac.md)
- [CORS vs CSRF](security/cors-vs-csrf.md)
- [Unauthenticated GET Endpoints](security/UNAUTHENTICATED_GET_ENDPOINTS.md)

## GraphQL

- [GraphQL Quick Start](graphql/README_GRAPHQL.md)
- [GraphQL Guide](graphql/GRAPHQL_GUIDE.md)
- [GraphQL Test Queries](graphql/GRAPHQL_TEST_QUERIES.md)
- [GraphQL Implementation Summary](graphql/GRAPHQL_IMPLEMENTATION_SUMMARY.md)

## Performance & Cache

- [Performance Metrics Guide](aop/PERFORMANCE_METRICS_GUIDE.md)
- [Performance Metrics Quick Reference](aop/PERFORMANCE_METRICS_QUICK_REFERENCE.md)
- [Cache Monitoring Guide](aop/CACHE_MONITORING_GUIDE.md)
- [Cache Performance Comparison](cache/CACHE_PERFORMANCE_COMPARISON.md)
- [Cache Simulation API](cache/CACHE_SIMULATION_API.md)
- [Final Optimization Report](performance/FINAL_OPTIMIZATION_REPORT.md)

## AOP & Logging

- [AOP Implementation Guide](aop/AOP_IMPLEMENTATION_GUIDE.md)
- [AOP Quick Reference](aop/AOP_QUICK_REFERENCE.md)
- [Request Masking Examples](aop/REQUEST_MASKING_EXAMPLES.md)
- [Sensitive Data Masking](aop/SENSITIVE_DATA_MASKING.md)

## Optimization & Profiling
- [Optimization](optimization/OPTIMIZATION.md)

## Notes

- Source of truth for endpoint behavior is controller mappings in `src/main/java/.../controllers`.
- Source of truth for access rules is `SecurityConfig`.
- Source of truth for GraphQL operations is `src/main/resources/graphql/schema.graphqls`.
