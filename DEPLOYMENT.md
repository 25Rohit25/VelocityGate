# VelocityGate Deployment Architecture

This document outlines the deployment strategies for VelocityGate, covering Kubernetes, AWS cloud-native setup, and High Availability configurations.

## 1. Kubernetes Deployment

This diagram illustrates a production-grade Kubernetes cluster setup. The Gateway scales horizontally based on CPU/Memory usage (HPA), with state offloaded to a Redis Cluster.

```mermaid
graph TB
    subgraph "K8s Cluster (Namespace: gateway)"
        Ingress[Ingress Controller / Load Balancer]

        subgraph "Configuration"
            CM[ConfigMap: application.yml]
            Sec[Secret: DB/Redis Creds]
        end

        subgraph "Application Layer"
            Service[Service: api-gateway]

            subgraph "Gateway Deployment (Replicas: 3-10)"
                Pod1[Gateway Pod 1]
                Pod2[Gateway Pod 2]
                Pod3[Gateway Pod 3]
            end
        end

        subgraph "State & Data Layer"
            RedisSvc[Service: redis]

            subgraph "Redis Cluster (StatefulSet)"
                RedisM[Redis Master]
                RedisR1[Redis Replica 1]
                RedisR2[Redis Replica 2]
            end
        end

        subgraph "Observability"
            Prom[Prometheus Pod]
            Graf[Grafana Pod]
        end
    end

    %% Flows
    Ingress -->|Route Traffic| Service
    Service --> Pod1 & Pod2 & Pod3

    %% Config Injection
    CM -.->|Env Vars| Pod1
    Sec -.->|Env Vars| Pod1

    %% Data Access
    Pod1 & Pod2 & Pod3 -->|Read/Write Rate Limits| RedisSvc
    RedisSvc --> RedisM
    RedisM -.->|Async Replication| RedisR1 & RedisR2

    %% Metrics
    Prom -->|Scrape /actuator/prometheus| Service
    Graf -->|Query| Prom

    %% Auto-scaling
    HPA[Horizontal Pod Autoscaler] -.->|Monitor CPU/Mem| Service
    HPA -.->|Scale Replicas| Pod1
```

### Key Components

- **Ingress**: Manages external access (SSL termination, path routing).
- **HPA**: Automatically scales Gateway pods between 3 and 10 replicas based on load.
- **Redis**: Deployed as a StatefulSet with Master-Replica architecture for failover.
- **Secrets**: Sensitive data (passwords, keys) injected securely as environment variables.

---

## 2. AWS Cloud Architecture

This architecture leverages AWS managed services for maximum reliability and minimal operational overhead.

```mermaid
flowchart TD
    User([User Requests]) -->|HTTPS| Route53[Route53 DNS]
    Route53 --> ALB[Application Load Balancer]

    subgraph "VPC (10.0.0.0/16)"
        subgraph "Public Subnets"
            ALB
            NAT[NAT Gateway]
        end

        subgraph "Private Application Subnets"
            subgraph "ECS Cluster / Auto Scaling Group"
                Task1[Gateway Task 1]
                Task2[Gateway Task 2]
                Task3[Gateway Task 3]
            end
        end

        subgraph "Private Data Subnets"
            RedisPrimary[(ElastiCache Redis Primary)]
            RedisReplica[(ElastiCache Redis Replica)]
            RDS[(RDS PostgreSQL)]
        end
    end

    subgraph "AWS Management"
        CW[CloudWatch Logs & Metrics]
        Param[Parameter Store / Secrets Manager]
    end

    %% Traffic Flow
    ALB -->|Target Group| Task1 & Task2 & Task3

    %% Data Flow
    Task1 -->|Rate Limits| RedisPrimary
    RedisPrimary -.->|Replication| RedisReplica
    Task1 -->|Config/Keys| RDS

    %% External Access
    Task1 -->|Outbound Traffic| NAT

    %% Monitoring & Config
    Task1 -.->|Logs/Metrics| CW
    Task1 -.->|Fetch Config| Param

    %% Security Groups (Implicit)
    %% - ALB: Allow 443 from 0.0.0.0/0
    %% - App: Allow 8080 from ALB SG
    %% - Data: Allow 6379/5432 from App SG
```

### Infrastructure details

- **Compute**: ECS Fargate or EC2 Auto Scaling Group for stateless gateway instances.
- **Networking**: Processing and Data layers are isolated in private subnets, accessible only via the ALB.
- **State**: ElastiCache (Redis) handles the high-throughput rate limit counters.
- **Config**: AWS Systems Manager Parameter Store holds configuration and secrets.

---

## 3. High Availability (Multi-Region)

For mission-critical deployments requiring 99.99% uptime and disaster recovery.

```mermaid
flowchart TB
    Client([Global Client]) --> GLB{Global Load Balancer / Route53}

    subgraph "Region A (Active)"
        LB_A[Load Balancer A]
        App_A[VelocityGate A]
        Redis_A[(Redis Primary A)]
    end

    subgraph "Region B (Passive / Warm Standby)"
        LB_B[Load Balancer B]
        App_B[VelocityGate B]
        Redis_B[(Redis Primary B)]
    end

    %% Routing
    GLB -->|Primary Traffic| LB_A
    GLB -.->|Failover| LB_B

    %% Local Flow
    LB_A --> App_A --> Redis_A
    LB_B --> App_B --> Redis_B

    %% Data Sync strategies
    Redis_A <==>|Active-Active (CRDT) or Async Replication| Redis_B

    classDef region fill:#f9f9f9,stroke:#333,stroke-width:2px;
    class Region A,Region B region;
```

### HA Strategy

- **Active-Passive**: Region B is standby. Redis data is asynchronously replicated. In a failover event, users are routed to Region B. Rate limits might reset or be slightly stale depending on replication lag.
- **Active-Active**: Using Redis Enterprise or DynamoDB (Global Tables) allows both regions to accept writes. Note that "Strict" global rate limiting adds significant latency; "Eventually Consistent" limiting is preferred for multi-region performance.
