# 智能通知系统CI/CD与监控方案

## 📋 项目概述

基于yudao-boot-mini框架的智能通知系统，支持10万+用户高并发推送，99.9%可用性目标。本方案涵盖完整的DevOps流程，从代码提交到生产部署的全自动化流程。

### 技术栈
- **核心框架**: yudao-boot-mini + Spring Boot 3.4.5 + Java 21
- **前端**: Vue3 + Vben Admin + TypeScript
- **数据库**: MySQL 8.0 + Redis 7.0
- **消息队列**: Kafka 3.0 + RabbitMQ
- **容器化**: Docker + Kubernetes
- **CI/CD**: Jenkins + GitLab CI
- **监控**: Prometheus + Grafana + ELK Stack

### 性能目标
- **推送能力**: 单次推送10万+用户
- **响应延迟**: 推送延迟 < 5秒
- **系统可用性**: ≥ 99.9%
- **并发支持**: ≥ 5000并发用户

## 🐳 Docker容器化配置

### 1. 应用服务容器化

#### 后端服务Dockerfile优化

```dockerfile
# 优化的yudao-server Dockerfile
FROM eclipse-temurin:21-jre-alpine

# 创建应用用户（安全最佳实践）
RUN addgroup -g 1001 yudao && \
    adduser -D -s /bin/sh -u 1001 -G yudao yudao

# 创建应用目录
RUN mkdir -p /app && chown -R yudao:yudao /app
WORKDIR /app

# 复制jar包
COPY --chown=yudao:yudao ./target/yudao-server.jar app.jar

# 设置时区和JVM参数
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
               -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/dumps \
               -Djava.security.egd=file:/dev/./urandom" \
    ARGS=""

# 创建必要目录
RUN mkdir -p /app/logs /app/dumps && chown -R yudao:yudao /app

# 切换到非root用户
USER yudao

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:48080/actuator/health || exit 1

# 暴露端口
EXPOSE 48080

# 启动命令
CMD ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar ${ARGS}"]
```

#### 前端Vue3容器化

```dockerfile
# Vue3前端多阶段构建
# Stage 1: Build
FROM node:18-alpine AS builder

WORKDIR /app

# 复制package文件
COPY package*.json ./
RUN npm ci --only=production

# 复制源码并构建
COPY . .
RUN npm run build:prod

# Stage 2: Production
FROM nginx:1.25-alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制nginx配置
COPY nginx.conf /etc/nginx/nginx.conf

# 创建nginx用户目录
RUN chown -R nginx:nginx /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 2. 基础设施容器配置

#### docker-compose.prod.yml

```yaml
version: "3.8"

services:
  # MySQL主库
  mysql-master:
    image: mysql:8.0
    container_name: mysql-master
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_REPLICATION_MODE: master
      MYSQL_REPLICATION_USER: replicator
      MYSQL_REPLICATION_PASSWORD: ${REPLICATION_PASSWORD}
    ports:
      - "3306:3306"
    volumes:
      - mysql_master_data:/var/lib/mysql
      - ./conf/mysql/master.cnf:/etc/mysql/conf.d/master.cnf:ro
      - ./sql/init:/docker-entrypoint-initdb.d:ro
    command: --log-bin=mysql-bin --server-id=1
    networks:
      - backend
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10

  # MySQL从库
  mysql-slave:
    image: mysql:8.0
    container_name: mysql-slave
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_REPLICATION_MODE: slave
      MYSQL_MASTER_HOST: mysql-master
      MYSQL_REPLICATION_USER: replicator
      MYSQL_REPLICATION_PASSWORD: ${REPLICATION_PASSWORD}
    ports:
      - "3307:3306"
    volumes:
      - mysql_slave_data:/var/lib/mysql
      - ./conf/mysql/slave.cnf:/etc/mysql/conf.d/slave.cnf:ro
    depends_on:
      mysql-master:
        condition: service_healthy
    command: --server-id=2
    networks:
      - backend

  # Redis集群
  redis-cluster:
    image: redis:7.0-alpine
    container_name: redis-cluster
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
      - ./conf/redis/redis.conf:/usr/local/etc/redis/redis.conf:ro
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      - backend
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      timeout: 3s
      retries: 5

  # Kafka
  kafka:
    image: confluentinc/cp-kafka:7.4.0
    container_name: kafka
    restart: unless-stopped
    ports:
      - "9092:9092"
      - "9094:9094"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_JMX_PORT: 9999
      KAFKA_JMX_HOSTNAME: localhost
    volumes:
      - kafka_data:/var/lib/kafka/data
    depends_on:
      - zookeeper
    networks:
      - backend

  # Zookeeper
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    container_name: zookeeper
    restart: unless-stopped
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zk_data:/var/lib/zookeeper/data
      - zk_log:/var/lib/zookeeper/log
    networks:
      - backend

  # 应用服务
  notification-app:
    image: yudao/notification:${APP_VERSION}
    container_name: notification-app
    restart: unless-stopped
    ports:
      - "48080:48080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      MYSQL_MASTER_HOST: mysql-master
      MYSQL_SLAVE_HOST: mysql-slave
      REDIS_HOST: redis-cluster
      KAFKA_HOST: kafka
      JVM_OPTS: "-Xms1g -Xmx2g"
    volumes:
      - app_logs:/app/logs
      - app_dumps:/app/dumps
    depends_on:
      mysql-master:
        condition: service_healthy
      redis-cluster:
        condition: service_healthy
      kafka:
        condition: service_started
    networks:
      - backend
      - frontend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:48080/actuator/health"]
      timeout: 10s
      retries: 3
      start_period: 60s

  # 前端服务
  notification-ui:
    image: yudao/notification-ui:${UI_VERSION}
    container_name: notification-ui
    restart: unless-stopped
    ports:
      - "8080:80"
    depends_on:
      - notification-app
    networks:
      - frontend

volumes:
  mysql_master_data:
  mysql_slave_data:
  redis_data:
  kafka_data:
  zk_data:
  zk_log:
  app_logs:
  app_dumps:

networks:
  backend:
    driver: bridge
  frontend:
    driver: bridge
```

## ☸️ Kubernetes部署方案

### 1. 命名空间和基础配置

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: notification-system
  labels:
    name: notification-system
---
# 资源配额限制
apiVersion: v1
kind: ResourceQuota
metadata:
  name: notification-quota
  namespace: notification-system
spec:
  hard:
    requests.cpu: "10"
    requests.memory: 20Gi
    limits.cpu: "20"
    limits.memory: 40Gi
    persistentvolumeclaims: "10"
```

### 2. ConfigMap配置

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: notification-config
  namespace: notification-system
data:
  application-prod.yaml: |
    server:
      port: 48080
      servlet:
        context-path: /api
    
    spring:
      profiles:
        active: prod
      datasource:
        dynamic:
          primary: master
          datasource:
            master:
              url: jdbc:mysql://mysql-service:3306/notification?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
              username: ${MYSQL_USERNAME}
              password: ${MYSQL_PASSWORD}
              driver-class-name: com.mysql.cj.jdbc.Driver
            slave:
              url: jdbc:mysql://mysql-slave-service:3306/notification?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
              username: ${MYSQL_USERNAME}
              password: ${MYSQL_PASSWORD}
              driver-class-name: com.mysql.cj.jdbc.Driver
      
      redis:
        host: redis-service
        port: 6379
        password: ${REDIS_PASSWORD}
        database: 0
        jedis:
          pool:
            max-active: 20
            max-idle: 10
            min-idle: 5
    
    kafka:
      bootstrap-servers: kafka-service:9092
      producer:
        retries: 3
        batch-size: 16384
        buffer-memory: 33554432
      consumer:
        group-id: notification-group
        enable-auto-commit: true
    
    yudao:
      tenant:
        enable: true
      security:
        permit-all-urls:
          - /actuator/health
          - /swagger-ui/**
          - /v3/api-docs/**
```

### 3. MySQL部署

```yaml
# mysql-deployment.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql-master
  namespace: notification-system
spec:
  serviceName: mysql-service
  replicas: 1
  selector:
    matchLabels:
      app: mysql-master
  template:
    metadata:
      labels:
        app: mysql-master
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        ports:
        - containerPort: 3306
        env:
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: root-password
        - name: MYSQL_DATABASE
          value: notification
        volumeMounts:
        - name: mysql-storage
          mountPath: /var/lib/mysql
        - name: mysql-config
          mountPath: /etc/mysql/conf.d
        resources:
          requests:
            memory: "2Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "2"
        livenessProbe:
          exec:
            command:
            - mysqladmin
            - ping
            - -h
            - localhost
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - mysql
            - -h
            - localhost
            - -e
            - "SELECT 1"
          initialDelaySeconds: 5
          periodSeconds: 2
      volumes:
      - name: mysql-config
        configMap:
          name: mysql-config
  volumeClaimTemplates:
  - metadata:
      name: mysql-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "fast-ssd"
      resources:
        requests:
          storage: 100Gi
---
apiVersion: v1
kind: Service
metadata:
  name: mysql-service
  namespace: notification-system
spec:
  selector:
    app: mysql-master
  ports:
  - port: 3306
    targetPort: 3306
  type: ClusterIP
```

### 4. Redis集群部署

```yaml
# redis-deployment.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis
  namespace: notification-system
spec:
  serviceName: redis-service
  replicas: 3
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7.0-alpine
        ports:
        - containerPort: 6379
        command:
        - redis-server
        - /etc/redis/redis.conf
        volumeMounts:
        - name: redis-storage
          mountPath: /data
        - name: redis-config
          mountPath: /etc/redis
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 30
          periodSeconds: 5
      volumes:
      - name: redis-config
        configMap:
          name: redis-config
  volumeClaimTemplates:
  - metadata:
      name: redis-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "fast-ssd"
      resources:
        requests:
          storage: 20Gi
---
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: notification-system
spec:
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379
  type: ClusterIP
```

### 5. 应用服务部署

```yaml
# notification-app-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: notification-app
  namespace: notification-system
  labels:
    app: notification-app
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  selector:
    matchLabels:
      app: notification-app
  template:
    metadata:
      labels:
        app: notification-app
    spec:
      containers:
      - name: notification-app
        image: yudao/notification:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 48080
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: MYSQL_USERNAME
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: username
        - name: MYSQL_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: password
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: redis-secret
              key: password
        volumeMounts:
        - name: app-config
          mountPath: /app/config
        - name: app-logs
          mountPath: /app/logs
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 48080
          initialDelaySeconds: 90
          periodSeconds: 15
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 48080
          initialDelaySeconds: 30
          periodSeconds: 5
      volumes:
      - name: app-config
        configMap:
          name: notification-config
      - name: app-logs
        emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: notification-service
  namespace: notification-system
spec:
  selector:
    app: notification-app
  ports:
  - name: http
    port: 80
    targetPort: 48080
  type: LoadBalancer
---
# HPA自动扩缩容
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: notification-app-hpa
  namespace: notification-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: notification-app
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

## 🔄 Jenkins CI/CD流水线

### 1. Jenkinsfile流水线脚本

```groovy
// Jenkinsfile
pipeline {
    agent any
    
    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'test', 'prod'], description: '部署环境')
        string(name: 'BRANCH', defaultValue: 'main', description: '部署分支')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: '跳过测试')
        booleanParam(name: 'FORCE_DEPLOY', defaultValue: false, description: '强制部署')
    }

    environment {
        // Docker配置
        DOCKER_REGISTRY = 'harbor.company.com'
        DOCKER_NAMESPACE = 'yudao'
        IMAGE_NAME = 'notification'
        
        // Kubernetes配置
        K8S_NAMESPACE = 'notification-system'
        K8S_DEPLOYMENT = 'notification-app'
        
        // 凭证ID
        DOCKER_CREDENTIAL_ID = 'harbor-credential'
        K8S_CREDENTIAL_ID = 'k8s-config'
        SONAR_CREDENTIAL_ID = 'sonarqube-token'
        
        // 版本标签
        BUILD_VERSION = "${BUILD_NUMBER}-${GIT_COMMIT.take(8)}"
        DOCKER_TAG = "${ENVIRONMENT}-${BUILD_VERSION}"
    }

    stages {
        stage('环境初始化') {
            steps {
                script {
                    echo "开始构建环境: ${ENVIRONMENT}"
                    echo "构建分支: ${BRANCH}"
                    echo "构建版本: ${BUILD_VERSION}"
                    
                    // 设置构建描述
                    currentBuild.displayName = "#${BUILD_NUMBER}-${ENVIRONMENT}"
                    currentBuild.description = "Branch: ${BRANCH}, Env: ${ENVIRONMENT}"
                }
            }
        }

        stage('代码检出') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${BRANCH}"]],
                    userRemoteConfigs: [[url: 'https://github.com/company/notification-system.git']]
                ])
                
                script {
                    env.GIT_COMMIT = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                    env.GIT_AUTHOR = sh(returnStdout: true, script: 'git log -1 --pretty=format:"%an"').trim()
                }
            }
        }

        stage('代码质量检查') {
            parallel {
                stage('SonarQube扫描') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                mvn sonar:sonar \
                                  -Dsonar.projectKey=notification-system \
                                  -Dsonar.host.url=${SONAR_HOST_URL} \
                                  -Dsonar.login=${SONAR_AUTH_TOKEN} \
                                  -Dsonar.java.coveragePlugin=jacoco \
                                  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                            '''
                        }
                    }
                }
                
                stage('安全扫描') {
                    steps {
                        sh '''
                            # OWASP依赖检查
                            mvn org.owasp:dependency-check-maven:check
                            
                            # 许可证检查
                            mvn license:check
                        '''
                    }
                }
            }
        }

        stage('质量门禁') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('构建应用') {
            parallel {
                stage('后端构建') {
                    steps {
                        sh '''
                            # Maven构建
                            mvn clean compile -DskipTests=true
                            
                            # 如果不跳过测试，执行测试
                            if [ "${SKIP_TESTS}" = "false" ]; then
                                mvn test
                                mvn jacoco:report
                            fi
                            
                            # 打包
                            mvn package -DskipTests=true -Pproduction
                            
                            # 检查构建产物
                            if [ ! -f "yudao-server/target/yudao-server.jar" ]; then
                                echo "构建失败：找不到jar文件"
                                exit 1
                            fi
                        '''
                    }
                    post {
                        always {
                            // 发布测试报告
                            publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                            publishCoverage adapters: [jacocoAdapter('target/site/jacoco/jacoco.xml')]
                        }
                    }
                }
                
                stage('前端构建') {
                    steps {
                        dir('yudao-ui-admin-vue3') {
                            sh '''
                                # 安装依赖
                                npm ci
                                
                                # 运行代码检查
                                npm run lint:fix
                                npm run type-check
                                
                                # 构建生产版本
                                npm run build:prod
                                
                                # 检查构建产物
                                if [ ! -d "dist" ]; then
                                    echo "前端构建失败：找不到dist目录"
                                    exit 1
                                fi
                            '''
                        }
                    }
                }
            }
        }

        stage('构建Docker镜像') {
            parallel {
                stage('后端镜像') {
                    steps {
                        script {
                            def backendImage = docker.build("${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}:${DOCKER_TAG}", 
                                                           "-f yudao-server/Dockerfile yudao-server/")
                            
                            docker.withRegistry("https://${DOCKER_REGISTRY}", DOCKER_CREDENTIAL_ID) {
                                backendImage.push()
                                backendImage.push("latest")
                            }
                        }
                    }
                }
                
                stage('前端镜像') {
                    steps {
                        script {
                            def frontendImage = docker.build("${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}-ui:${DOCKER_TAG}", 
                                                            "-f yudao-ui-admin-vue3/Dockerfile yudao-ui-admin-vue3/")
                            
                            docker.withRegistry("https://${DOCKER_REGISTRY}", DOCKER_CREDENTIAL_ID) {
                                frontendImage.push()
                                frontendImage.push("latest")
                            }
                        }
                    }
                }
            }
        }

        stage('安全扫描镜像') {
            steps {
                script {
                    sh '''
                        # 使用Trivy扫描镜像漏洞
                        trivy image --exit-code 1 --severity HIGH,CRITICAL ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}:${DOCKER_TAG}
                    '''
                }
            }
        }

        stage('部署到测试环境') {
            when {
                expression { params.ENVIRONMENT == 'test' || params.ENVIRONMENT == 'dev' }
            }
            steps {
                script {
                    kubernetesDeploy(
                        configs: 'k8s/test/*.yaml',
                        kubeconfigId: K8S_CREDENTIAL_ID,
                        enableConfigSubstitution: true
                    )
                    
                    // 等待部署完成
                    sh """
                        kubectl rollout status deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}-${ENVIRONMENT} --timeout=300s
                    """
                }
            }
        }

        stage('集成测试') {
            when {
                expression { params.ENVIRONMENT == 'test' && !params.SKIP_TESTS }
            }
            steps {
                sh '''
                    # 等待服务就绪
                    sleep 30
                    
                    # 运行集成测试
                    mvn test -Dtest=IntegrationTest -Dspring.profiles.active=test
                    
                    # API测试
                    newman run tests/postman/notification-api-tests.json \
                           --environment tests/postman/test-environment.json \
                           --reporters cli,junit
                '''
            }
            post {
                always {
                    publishTestResults testResultsPattern: 'target/newman/*.xml'
                }
            }
        }

        stage('性能测试') {
            when {
                expression { params.ENVIRONMENT == 'test' && !params.SKIP_TESTS }
            }
            steps {
                sh '''
                    # JMeter性能测试
                    jmeter -n -t tests/jmeter/notification-load-test.jmx \
                           -l results/performance-results.jtl \
                           -j results/jmeter.log \
                           -e -o results/html-report
                '''
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'results/html-report',
                        reportFiles: 'index.html',
                        reportName: 'Performance Test Report'
                    ])
                }
            }
        }

        stage('生产部署审批') {
            when {
                expression { params.ENVIRONMENT == 'prod' }
            }
            steps {
                script {
                    def approvers = ['dev-lead', 'ops-lead', 'product-manager']
                    def deployApproval = input(
                        id: 'deployApproval',
                        message: "确认部署到生产环境？",
                        ok: '确认部署',
                        submitterParameter: 'APPROVER',
                        submitter: approvers.join(','),
                        parameters: [
                            text(name: 'DEPLOYMENT_NOTES', defaultValue: '', description: '部署说明')
                        ]
                    )
                    
                    echo "部署已获得 ${deployApproval.APPROVER} 的批准"
                    echo "部署说明: ${deployApproval.DEPLOYMENT_NOTES}"
                }
            }
        }

        stage('蓝绿部署到生产') {
            when {
                expression { params.ENVIRONMENT == 'prod' }
            }
            steps {
                script {
                    // 创建新版本部署
                    sh """
                        # 更新镜像版本
                        sed -i 's|image: .*|image: ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}:${DOCKER_TAG}|g' k8s/prod/deployment.yaml
                        
                        # 部署到绿色环境
                        kubectl apply -f k8s/prod/ -n ${K8S_NAMESPACE}-prod-green
                        
                        # 等待部署完成
                        kubectl rollout status deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}-prod-green --timeout=600s
                    """
                    
                    // 健康检查
                    sh '''
                        # 健康检查
                        for i in {1..10}; do
                            if kubectl get pods -n notification-system-prod-green | grep -q "Running"; then
                                echo "健康检查通过"
                                break
                            fi
                            sleep 30
                        done
                    '''
                    
                    // 切换流量
                    timeout(time: 5, unit: 'MINUTES') {
                        input message: '确认切换生产流量？', ok: '确认切换'
                    }
                    
                    sh '''
                        # 切换服务指向
                        kubectl patch service notification-service -n notification-system-prod \
                                -p '{"spec":{"selector":{"version":"green"}}}'
                        
                        # 验证切换
                        kubectl get service notification-service -n notification-system-prod -o yaml
                    '''
                }
            }
        }

        stage('部署后验证') {
            steps {
                sh '''
                    # 健康检查
                    curl -f http://notification-service.${ENVIRONMENT}.company.com/actuator/health
                    
                    # 业务接口验证
                    curl -f http://notification-service.${ENVIRONMENT}.company.com/api/system/auth/get-permission-info
                    
                    # 性能基准测试
                    ab -n 100 -c 10 http://notification-service.${ENVIRONMENT}.company.com/api/system/auth/get-permission-info
                '''
            }
        }

        stage('监控和告警') {
            steps {
                script {
                    // 更新Grafana Dashboard
                    sh '''
                        # 触发监控数据刷新
                        curl -X POST "http://grafana.company.com/api/dashboards/db" \
                             -H "Authorization: Bearer ${GRAFANA_API_KEY}" \
                             -H "Content-Type: application/json" \
                             -d @monitoring/grafana-dashboard.json
                    '''
                    
                    // 发送部署通知
                    sh '''
                        # 钉钉通知
                        curl -X POST "${DINGTALK_WEBHOOK}" \
                             -H 'Content-Type: application/json' \
                             -d "{
                                \"msgtype\": \"text\",
                                \"text\": {
                                    \"content\": \"🚀 通知系统部署成功\\n环境: ${ENVIRONMENT}\\n版本: ${BUILD_VERSION}\\n分支: ${BRANCH}\\n构建者: ${GIT_AUTHOR}\"
                                }
                             }"
                    '''
                }
            }
        }
    }

    post {
        always {
            // 清理工作空间
            cleanWs()
            
            // 发布构建报告
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'target/site',
                reportFiles: 'index.html',
                reportName: 'Maven Site Report'
            ])
        }
        
        success {
            script {
                // 成功通知
                emailext (
                    subject: "✅ 构建成功: ${currentBuild.displayName}",
                    body: """
                        构建成功！
                        
                        项目: ${env.JOB_NAME}
                        构建号: ${env.BUILD_NUMBER}
                        环境: ${ENVIRONMENT}
                        版本: ${BUILD_VERSION}
                        分支: ${BRANCH}
                        提交者: ${GIT_AUTHOR}
                        
                        构建日志: ${env.BUILD_URL}console
                        部署地址: http://notification-service.${ENVIRONMENT}.company.com
                    """,
                    to: "${GIT_AUTHOR_EMAIL},devops@company.com"
                )
            }
        }
        
        failure {
            script {
                // 失败通知和回滚
                emailext (
                    subject: "❌ 构建失败: ${currentBuild.displayName}",
                    body: """
                        构建失败！
                        
                        项目: ${env.JOB_NAME}
                        构建号: ${env.BUILD_NUMBER}
                        环境: ${ENVIRONMENT}
                        分支: ${BRANCH}
                        失败原因: ${currentBuild.result}
                        
                        构建日志: ${env.BUILD_URL}console
                        
                        请及时查看并修复问题。
                    """,
                    to: "${GIT_AUTHOR_EMAIL},devops@company.com"
                )
                
                // 自动回滚（生产环境）
                if (params.ENVIRONMENT == 'prod') {
                    sh '''
                        echo "开始自动回滚..."
                        kubectl rollout undo deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}-prod
                        kubectl rollout status deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}-prod
                    '''
                }
            }
        }
    }
}
```

### 2. GitLab CI配置

```yaml
# .gitlab-ci.yml
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  DOCKER_REGISTRY: "harbor.company.com"
  DOCKER_NAMESPACE: "yudao"
  IMAGE_NAME: "notification"
  K8S_NAMESPACE: "notification-system"

stages:
  - validate
  - build
  - test
  - security
  - package
  - deploy-dev
  - deploy-test
  - deploy-prod

cache:
  paths:
    - .m2/repository/
    - node_modules/

# 代码质量检查
validate:
  stage: validate
  image: maven:3.8.6-openjdk-21-slim
  script:
    - mvn validate
    - mvn compile -DskipTests=true
  only:
    - merge_requests
    - main
    - develop

# 构建阶段
build-backend:
  stage: build
  image: maven:3.8.6-openjdk-21-slim
  script:
    - mvn clean compile -DskipTests=true
    - mvn package -DskipTests=true -Pproduction
  artifacts:
    paths:
      - yudao-server/target/*.jar
    expire_in: 1 hour
  only:
    - main
    - develop
    - /^release\/.*$/

build-frontend:
  stage: build
  image: node:18-alpine
  script:
    - cd yudao-ui-admin-vue3
    - npm ci
    - npm run build:prod
  artifacts:
    paths:
      - yudao-ui-admin-vue3/dist/
    expire_in: 1 hour
  only:
    - main
    - develop
    - /^release\/.*$/

# 测试阶段
unit-test:
  stage: test
  image: maven:3.8.6-openjdk-21-slim
  services:
    - mysql:8.0
    - redis:7.0-alpine
  variables:
    MYSQL_ROOT_PASSWORD: "123456"
    MYSQL_DATABASE: "notification_test"
  script:
    - mvn test
    - mvn jacoco:report
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml
      coverage_report:
        coverage_format: jacoco
        path: target/site/jacoco/jacoco.xml
  only:
    - merge_requests
    - main
    - develop

integration-test:
  stage: test
  image: maven:3.8.6-openjdk-21-slim
  services:
    - mysql:8.0
    - redis:7.0-alpine
    - kafka:latest
  script:
    - mvn test -Dtest=IntegrationTest -Dspring.profiles.active=test
  dependencies:
    - build-backend
  only:
    - main
    - develop

# 安全扫描
sonarqube-scan:
  stage: security
  image: maven:3.8.6-openjdk-21-slim
  script:
    - mvn sonar:sonar 
      -Dsonar.projectKey=notification-system
      -Dsonar.host.url=$SONAR_HOST_URL
      -Dsonar.login=$SONAR_TOKEN
  dependencies:
    - unit-test
  only:
    - main
    - develop
    - merge_requests

dependency-check:
  stage: security
  image: maven:3.8.6-openjdk-21-slim
  script:
    - mvn org.owasp:dependency-check-maven:check
  artifacts:
    reports:
      junit:
        - target/dependency-check-report.xml
  allow_failure: true
  only:
    - main
    - develop

# 打包Docker镜像
package-backend:
  stage: package
  image: docker:20.10.16
  services:
    - docker:20.10.16-dind
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
  before_script:
    - echo $CI_REGISTRY_PASSWORD | docker login -u $CI_REGISTRY_USER --password-stdin $DOCKER_REGISTRY
  script:
    - cd yudao-server
    - docker build -t $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$IMAGE_NAME:$CI_COMMIT_SHA .
    - docker push $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$IMAGE_NAME:$CI_COMMIT_SHA
    - docker tag $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$IMAGE_NAME:$CI_COMMIT_SHA $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$IMAGE_NAME:latest
    - docker push $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$IMAGE_NAME:latest
  dependencies:
    - build-backend
  only:
    - main
    - develop
    - /^release\/.*$/

package-frontend:
  stage: package
  image: docker:20.10.16
  services:
    - docker:20.10.16-dind
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
  before_script:
    - echo $CI_REGISTRY_PASSWORD | docker login -u $CI_REGISTRY_USER --password-stdin $DOCKER_REGISTRY
  script:
    - cd yudao-ui-admin-vue3
    - docker build -t $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$IMAGE_NAME-ui:$CI_COMMIT_SHA .
    - docker push $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$IMAGE_NAME-ui:$CI_COMMIT_SHA
    - docker tag $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$IMAGE_NAME-ui:$CI_COMMIT_SHA $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$IMAGE_NAME-ui:latest
    - docker push $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$IMAGE_NAME-ui:latest
  dependencies:
    - build-frontend
  only:
    - main
    - develop
    - /^release\/.*$/

# 部署开发环境
deploy-dev:
  stage: deploy-dev
  image: bitnami/kubectl:latest
  environment:
    name: development
    url: http://notification-dev.company.com
  script:
    - sed -i "s|IMAGE_TAG|$CI_COMMIT_SHA|g" k8s/dev/deployment.yaml
    - kubectl apply -f k8s/dev/ -n $K8S_NAMESPACE-dev
    - kubectl rollout status deployment/notification-app -n $K8S_NAMESPACE-dev
  dependencies:
    - package-backend
    - package-frontend
  only:
    - develop

# 部署测试环境
deploy-test:
  stage: deploy-test
  image: bitnami/kubectl:latest
  environment:
    name: testing
    url: http://notification-test.company.com
  script:
    - sed -i "s|IMAGE_TAG|$CI_COMMIT_SHA|g" k8s/test/deployment.yaml
    - kubectl apply -f k8s/test/ -n $K8S_NAMESPACE-test
    - kubectl rollout status deployment/notification-app -n $K8S_NAMESPACE-test
    # 运行冒烟测试
    - sleep 60
    - curl -f http://notification-service.$K8S_NAMESPACE-test:80/actuator/health
  dependencies:
    - package-backend
    - package-frontend
  only:
    - main

# 部署生产环境
deploy-prod:
  stage: deploy-prod
  image: bitnami/kubectl:latest
  environment:
    name: production
    url: http://notification.company.com
  script:
    - sed -i "s|IMAGE_TAG|$CI_COMMIT_SHA|g" k8s/prod/deployment.yaml
    - kubectl apply -f k8s/prod/ -n $K8S_NAMESPACE-prod
    - kubectl rollout status deployment/notification-app -n $K8S_NAMESPACE-prod
  when: manual
  dependencies:
    - package-backend
    - package-frontend
  only:
    - /^release\/.*$/
    - main
```

## 📊 Prometheus监控配置

### 1. Prometheus配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'notification-cluster'
    region: 'beijing'

rule_files:
  - "notification-rules.yml"
  - "infrastructure-rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  # 应用监控
  - job_name: 'notification-app'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - notification-system
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: notification-app
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
    scrape_interval: 30s
    metrics_path: /actuator/prometheus

  # MySQL监控
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-exporter:9104']
    scrape_interval: 30s

  # Redis监控
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
    scrape_interval: 30s

  # Kafka监控
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']
    scrape_interval: 30s

  # Kubernetes监控
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)

  # Node监控
  - job_name: 'node-exporter'
    kubernetes_sd_configs:
      - role: endpoints
    relabel_configs:
      - source_labels: [__meta_kubernetes_endpoints_name]
        action: keep
        regex: node-exporter

  # JVM监控
  - job_name: 'jvm-metrics'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - notification-system
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: notification-app
    metrics_path: /actuator/prometheus
    scrape_interval: 15s
```

### 2. 告警规则配置

```yaml
# notification-rules.yml
groups:
  - name: notification-app-alerts
    rules:
      # 应用可用性告警
      - alert: ApplicationDown
        expr: up{job="notification-app"} == 0
        for: 1m
        labels:
          severity: critical
          service: notification-app
        annotations:
          summary: "通知应用服务不可用"
          description: "应用 {{ $labels.instance }} 已经离线超过1分钟"

      # 内存使用告警
      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes{job="notification-app"} / jvm_memory_max_bytes{job="notification-app"}) * 100 > 85
        for: 5m
        labels:
          severity: warning
          service: notification-app
        annotations:
          summary: "应用内存使用率过高"
          description: "实例 {{ $labels.instance }} 内存使用率已达到 {{ $value }}%"

      # CPU使用告警
      - alert: HighCPUUsage
        expr: rate(process_cpu_seconds_total{job="notification-app"}[5m]) * 100 > 80
        for: 5m
        labels:
          severity: warning
          service: notification-app
        annotations:
          summary: "应用CPU使用率过高"
          description: "实例 {{ $labels.instance }} CPU使用率已达到 {{ $value }}%"

      # GC频繁告警
      - alert: FrequentGC
        expr: rate(jvm_gc_collection_seconds_count{job="notification-app"}[5m]) > 5
        for: 2m
        labels:
          severity: warning
          service: notification-app
        annotations:
          summary: "JVM GC过于频繁"
          description: "实例 {{ $labels.instance }} GC频率为 {{ $value }} 次/秒"

      # 响应时间告警
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="notification-app"}[5m])) > 5
        for: 3m
        labels:
          severity: warning
          service: notification-app
        annotations:
          summary: "接口响应时间过长"
          description: "95%接口响应时间超过5秒，当前值: {{ $value }}s"

      # 错误率告警
      - alert: HighErrorRate
        expr: (rate(http_server_requests_seconds_count{job="notification-app",status=~"5.."}[5m]) / rate(http_server_requests_seconds_count{job="notification-app"}[5m])) * 100 > 5
        for: 2m
        labels:
          severity: critical
          service: notification-app
        annotations:
          summary: "应用错误率过高"
          description: "5xx错误率达到 {{ $value }}%"

      # 数据库连接池告警
      - alert: DatabaseConnectionPoolHigh
        expr: hikaricp_connections_active{job="notification-app"} / hikaricp_connections_max{job="notification-app"} * 100 > 80
        for: 3m
        labels:
          severity: warning
          service: notification-app
        annotations:
          summary: "数据库连接池使用率过高"
          description: "连接池使用率达到 {{ $value }}%"

      # 推送队列积压告警
      - alert: PushQueueBacklog
        expr: kafka_consumer_lag_sum{job="kafka-exporter", topic="notification_push"} > 1000
        for: 2m
        labels:
          severity: warning
          service: notification-push
        annotations:
          summary: "推送队列积压严重"
          description: "推送队列积压消息数: {{ $value }}"

      # 推送成功率告警
      - alert: LowPushSuccessRate
        expr: (rate(notification_push_success_total[5m]) / (rate(notification_push_success_total[5m]) + rate(notification_push_failure_total[5m]))) * 100 < 95
        for: 3m
        labels:
          severity: warning
          service: notification-push
        annotations:
          summary: "推送成功率过低"
          description: "推送成功率仅为 {{ $value }}%"

  - name: infrastructure-alerts
    rules:
      # MySQL告警
      - alert: MySQLDown
        expr: up{job="mysql"} == 0
        for: 1m
        labels:
          severity: critical
          service: mysql
        annotations:
          summary: "MySQL数据库不可用"
          description: "MySQL实例 {{ $labels.instance }} 无法连接"

      - alert: MySQLSlowQueries
        expr: rate(mysql_global_status_slow_queries[5m]) > 0.2
        for: 3m
        labels:
          severity: warning
          service: mysql
        annotations:
          summary: "MySQL慢查询增多"
          description: "慢查询率: {{ $value }} 查询/秒"

      - alert: MySQLConnectionsHigh
        expr: mysql_global_status_threads_connected / mysql_global_variables_max_connections * 100 > 80
        for: 3m
        labels:
          severity: warning
          service: mysql
        annotations:
          summary: "MySQL连接数过高"
          description: "连接使用率达到 {{ $value }}%"

      # Redis告警
      - alert: RedisDown
        expr: up{job="redis"} == 0
        for: 1m
        labels:
          severity: critical
          service: redis
        annotations:
          summary: "Redis缓存不可用"
          description: "Redis实例 {{ $labels.instance }} 无法连接"

      - alert: RedisMemoryHigh
        expr: redis_memory_used_bytes / redis_memory_max_bytes * 100 > 90
        for: 5m
        labels:
          severity: warning
          service: redis
        annotations:
          summary: "Redis内存使用率过高"
          description: "内存使用率达到 {{ $value }}%"

      # Kafka告警
      - alert: KafkaDown
        expr: up{job="kafka"} == 0
        for: 1m
        labels:
          severity: critical
          service: kafka
        annotations:
          summary: "Kafka消息队列不可用"
          description: "Kafka实例 {{ $labels.instance }} 无法连接"

      - alert: KafkaConsumerLag
        expr: kafka_consumer_lag_sum > 5000
        for: 3m
        labels:
          severity: warning
          service: kafka
        annotations:
          summary: "Kafka消费延迟过高"
          description: "消费延迟: {{ $value }} 条消息"
```

### 3. Grafana监控面板

```json
{
  "dashboard": {
    "id": null,
    "title": "智能通知系统监控大屏",
    "tags": ["notification", "yudao"],
    "timezone": "Asia/Shanghai",
    "panels": [
      {
        "id": 1,
        "title": "系统概览",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=\"notification-app\"}",
            "legendFormat": "服务实例数"
          },
          {
            "expr": "sum(rate(http_server_requests_seconds_count{job=\"notification-app\"}[5m]))",
            "legendFormat": "请求QPS"
          },
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job=\"notification-app\"}[5m]))",
            "legendFormat": "P95响应时间"
          },
          {
            "expr": "(1 - sum(rate(http_server_requests_seconds_count{job=\"notification-app\",status=~\"5..\"}[5m])) / sum(rate(http_server_requests_seconds_count{job=\"notification-app\"}[5m]))) * 100",
            "legendFormat": "成功率(%)"
          }
        ],
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "JVM内存使用情况",
        "type": "timeseries",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{job=\"notification-app\", area=\"heap\"}",
            "legendFormat": "堆内存使用-{{instance}}"
          },
          {
            "expr": "jvm_memory_max_bytes{job=\"notification-app\", area=\"heap\"}",
            "legendFormat": "堆内存最大值-{{instance}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 3,
        "title": "GC情况",
        "type": "timeseries",
        "targets": [
          {
            "expr": "rate(jvm_gc_collection_seconds_count{job=\"notification-app\"}[5m])",
            "legendFormat": "GC次数/秒-{{gc}}"
          },
          {
            "expr": "rate(jvm_gc_collection_seconds_sum{job=\"notification-app\"}[5m])",
            "legendFormat": "GC时间/秒-{{gc}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8}
      },
      {
        "id": 4,
        "title": "接口请求统计",
        "type": "timeseries",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{job=\"notification-app\"}[5m])) by (uri)",
            "legendFormat": "{{uri}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 16}
      },
      {
        "id": 5,
        "title": "推送业务监控",
        "type": "timeseries",
        "targets": [
          {
            "expr": "sum(rate(notification_push_total[5m]))",
            "legendFormat": "推送总数/秒"
          },
          {
            "expr": "sum(rate(notification_push_success_total[5m]))",
            "legendFormat": "推送成功/秒"
          },
          {
            "expr": "sum(rate(notification_push_failure_total[5m]))",
            "legendFormat": "推送失败/秒"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 16}
      },
      {
        "id": 6,
        "title": "数据库连接池",
        "type": "timeseries",
        "targets": [
          {
            "expr": "hikaricp_connections_active{job=\"notification-app\"}",
            "legendFormat": "活跃连接数"
          },
          {
            "expr": "hikaricp_connections_idle{job=\"notification-app\"}",
            "legendFormat": "空闲连接数"
          },
          {
            "expr": "hikaricp_connections_max{job=\"notification-app\"}",
            "legendFormat": "最大连接数"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 24}
      },
      {
        "id": 7,
        "title": "Redis性能监控",
        "type": "timeseries",
        "targets": [
          {
            "expr": "rate(redis_commands_processed_total[5m])",
            "legendFormat": "命令执行数/秒"
          },
          {
            "expr": "redis_connected_clients",
            "legendFormat": "连接客户端数"
          },
          {
            "expr": "redis_memory_used_bytes / 1024 / 1024",
            "legendFormat": "内存使用(MB)"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 24}
      },
      {
        "id": 8,
        "title": "Kafka消息队列",
        "type": "timeseries",
        "targets": [
          {
            "expr": "kafka_topic_partition_current_offset",
            "legendFormat": "当前offset-{{topic}}"
          },
          {
            "expr": "kafka_consumer_lag_sum",
            "legendFormat": "消费延迟-{{topic}}"
          }
        ],
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 32}
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "30s"
  }
}
```

## 📋 ELK日志收集配置

### 1. Logstash配置

```ruby
# logstash.conf
input {
  beats {
    port => 5044
  }
}

filter {
  if [fields][service] == "notification-app" {
    # 解析应用日志
    grok {
      match => { 
        "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} \[%{DATA:thread}\] %{DATA:logger} - %{GREEDYDATA:message}"
      }
    }
    
    # 解析JSON格式日志
    if [message] =~ /^\{.*\}$/ {
      json {
        source => "message"
      }
    }
    
    # 添加地理位置信息
    if [client_ip] {
      geoip {
        source => "client_ip"
        target => "geoip"
      }
    }
    
    # 时间解析
    date {
      match => [ "timestamp", "yyyy-MM-dd HH:mm:ss.SSS" ]
      target => "@timestamp"
    }
    
    # 添加标签
    mutate {
      add_tag => [ "notification-app", "spring-boot" ]
      add_field => { "service_name" => "notification-system" }
    }
  }
  
  if [fields][service] == "mysql" {
    # MySQL慢日志解析
    grok {
      match => { 
        "message" => "# Time: %{TIMESTAMP_ISO8601:timestamp}\n# User@Host: %{USER:user}\[%{USER:user_host}\] @ %{IPORHOST:client_host} \[%{IPORHOST:client_ip}\]\n# Query_time: %{NUMBER:query_time:float} Lock_time: %{NUMBER:lock_time:float} Rows_sent: %{NUMBER:rows_sent:int} Rows_examined: %{NUMBER:rows_examined:int}\n%{GREEDYDATA:sql_query}"
      }
    }
    
    if [query_time] and [query_time] > 1 {
      mutate {
        add_tag => [ "slow-query" ]
      }
    }
  }
  
  if [fields][service] == "nginx" {
    # Nginx访问日志解析
    grok {
      match => { 
        "message" => "%{NGINXACCESS}"
      }
    }
    
    # 计算响应时间等级
    if [response_time] {
      if [response_time] > 5 {
        mutate { add_tag => [ "slow-response" ] }
      } else if [response_time] > 1 {
        mutate { add_tag => [ "medium-response" ] }
      } else {
        mutate { add_tag => [ "fast-response" ] }
      }
    }
  }
}

output {
  if "notification-app" in [tags] {
    elasticsearch {
      hosts => ["elasticsearch:9200"]
      index => "notification-app-%{+YYYY.MM.dd}"
      template_name => "notification-app"
    }
  }
  
  if "mysql" in [tags] {
    elasticsearch {
      hosts => ["elasticsearch:9200"]
      index => "mysql-logs-%{+YYYY.MM.dd}"
    }
  }
  
  if "nginx" in [tags] {
    elasticsearch {
      hosts => ["elasticsearch:9200"]
      index => "nginx-access-%{+YYYY.MM.dd}"
    }
  }
  
  # 调试输出
  if [level] == "ERROR" {
    stdout {
      codec => rubydebug
    }
  }
}
```

### 2. Filebeat配置

```yaml
# filebeat.yml
filebeat.inputs:
  # 应用日志收集
  - type: log
    enabled: true
    paths:
      - /app/logs/*.log
    fields:
      service: notification-app
      environment: production
    fields_under_root: true
    multiline.pattern: '^\d{4}-\d{2}-\d{2}'
    multiline.negate: true
    multiline.match: after
    exclude_lines: ['^DBG']
    
  # MySQL日志收集
  - type: log
    enabled: true
    paths:
      - /var/log/mysql/slow.log
      - /var/log/mysql/error.log
    fields:
      service: mysql
      log_type: database
    
  # Nginx日志收集
  - type: log
    enabled: true
    paths:
      - /var/log/nginx/access.log
      - /var/log/nginx/error.log
    fields:
      service: nginx
      log_type: webserver

processors:
  # 添加Docker容器信息
  - add_docker_metadata:
      host: "unix:///var/run/docker.sock"
  
  # 添加Kubernetes元数据
  - add_kubernetes_metadata:
      host: ${NODE_NAME}
      matchers:
      - logs_path:
          logs_path: "/var/log/containers/"
  
  # 删除敏感字段
  - drop_fields:
      fields: ["host.name", "agent.name"]

output.logstash:
  hosts: ["logstash:5044"]
  compression_level: 3
  bulk_max_size: 2048

logging.level: info
logging.to_files: true
logging.files:
  path: /var/log/filebeat
  name: filebeat
  keepfiles: 7
  permissions: 0640

monitoring.enabled: true
```

### 3. Kibana Dashboard配置

```json
{
  "version": "7.15.0",
  "objects": [
    {
      "attributes": {
        "title": "通知系统日志分析",
        "type": "dashboard",
        "description": "智能通知系统综合日志分析面板",
        "panelsJSON": "[{\"version\":\"7.15.0\",\"gridData\":{\"x\":0,\"y\":0,\"w\":24,\"h\":15,\"i\":\"1\"},\"panelIndex\":\"1\",\"embeddableConfig\":{},\"panelRefName\":\"panel_1\"}]",
        "timeRestore": true,
        "timeTo": "now",
        "timeFrom": "now-24h",
        "refreshInterval": {
          "pause": false,
          "value": 30000
        },
        "kibanaSavedObjectMeta": {
          "searchSourceJSON": "{\"query\":{\"match_all\":{}},\"filter\":[]}"
        }
      },
      "references": [
        {
          "name": "panel_1",
          "type": "visualization",
          "id": "log-levels-pie"
        }
      ]
    },
    {
      "attributes": {
        "title": "日志级别分布",
        "visState": "{\"title\":\"日志级别分布\",\"type\":\"pie\",\"params\":{\"addTooltip\":true,\"addLegend\":true,\"legendPosition\":\"right\"},\"aggs\":[{\"id\":\"1\",\"type\":\"count\",\"schema\":\"metric\",\"params\":{}},{\"id\":\"2\",\"type\":\"terms\",\"schema\":\"segment\",\"params\":{\"field\":\"level.keyword\",\"size\":5,\"order\":\"desc\",\"orderBy\":\"1\"}}]}",
        "uiStateJSON": "{}",
        "description": "",
        "version": 1,
        "kibanaSavedObjectMeta": {
          "searchSourceJSON": "{\"index\":\"notification-app-*\",\"query\":{\"match_all\":{}},\"filter\":[]}"
        }
      },
      "id": "log-levels-pie",
      "type": "visualization"
    }
  ]
}
```

## 🚨 告警机制配置

### 1. AlertManager配置

```yaml
# alertmanager.yml
global:
  smtp_smarthost: 'smtp.company.com:587'
  smtp_from: 'alertmanager@company.com'
  smtp_auth_username: 'alertmanager@company.com'
  smtp_auth_password: 'password'

templates:
  - '/etc/alertmanager/templates/*.tmpl'

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'default'
  routes:
    # 紧急告警
    - match:
        severity: critical
      receiver: 'critical-alerts'
      group_wait: 0s
      repeat_interval: 5m
    
    # 警告告警
    - match:
        severity: warning
      receiver: 'warning-alerts'
      group_wait: 30s
      repeat_interval: 30m
    
    # 业务告警
    - match:
        service: notification-push
      receiver: 'business-alerts'

receivers:
  - name: 'default'
    email_configs:
      - to: 'devops@company.com'
        subject: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
        body: |
          {{ range .Alerts }}
          告警名称: {{ .Annotations.summary }}
          告警详情: {{ .Annotations.description }}
          告警时间: {{ .StartsAt }}
          告警级别: {{ .Labels.severity }}
          服务名称: {{ .Labels.service }}
          实例地址: {{ .Labels.instance }}
          {{ end }}

  - name: 'critical-alerts'
    # 邮件通知
    email_configs:
      - to: 'devops@company.com,dev-lead@company.com'
        subject: '🚨 紧急告警: {{ .GroupLabels.alertname }}'
        
    # 钉钉通知
    webhook_configs:
      - url: 'http://webhook-service:8080/dingtalk/critical'
        send_resolved: true
        
    # 短信通知
    webhook_configs:
      - url: 'http://webhook-service:8080/sms/critical'
        send_resolved: false

  - name: 'warning-alerts'
    email_configs:
      - to: 'devops@company.com'
        subject: '⚠️ 警告告警: {{ .GroupLabels.alertname }}'
        
    webhook_configs:
      - url: 'http://webhook-service:8080/dingtalk/warning'
        send_resolved: true

  - name: 'business-alerts'
    webhook_configs:
      - url: 'http://webhook-service:8080/dingtalk/business'
        send_resolved: true
        
inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'instance']
```

### 2. 自动恢复脚本

```bash
#!/bin/bash
# auto-recovery.sh

# 告警处理函数
handle_alert() {
    local alert_name=$1
    local instance=$2
    local severity=$3
    
    echo "处理告警: $alert_name, 实例: $instance, 级别: $severity"
    
    case $alert_name in
        "ApplicationDown")
            restart_application $instance
            ;;
        "HighMemoryUsage")
            cleanup_memory $instance
            ;;
        "DatabaseConnectionPoolHigh")
            restart_database_connection_pool $instance
            ;;
        "KafkaConsumerLag")
            scale_up_consumers
            ;;
        *)
            echo "未知告警类型，仅记录日志"
            ;;
    esac
}

# 重启应用
restart_application() {
    local instance=$1
    echo "重启应用实例: $instance"
    
    # 获取Pod名称
    local pod_name=$(kubectl get pods -n notification-system | grep $instance | awk '{print $1}')
    
    if [ -n "$pod_name" ]; then
        kubectl delete pod $pod_name -n notification-system
        echo "已删除Pod: $pod_name，等待重新创建"
        
        # 等待Pod重新启动
        sleep 30
        kubectl wait --for=condition=Ready pod -l app=notification-app -n notification-system --timeout=300s
        
        # 发送恢复通知
        send_recovery_notification "应用实例 $instance 已自动重启"
    fi
}

# 内存清理
cleanup_memory() {
    local instance=$1
    echo "清理内存: $instance"
    
    # 触发GC
    curl -X POST "http://$instance/actuator/gc"
    
    # 如果内存使用仍然过高，重启应用
    sleep 60
    local memory_usage=$(check_memory_usage $instance)
    if [ $memory_usage -gt 80 ]; then
        restart_application $instance
    fi
}

# 扩展消费者
scale_up_consumers() {
    echo "扩展Kafka消费者"
    
    # 增加消费者副本数
    kubectl scale deployment notification-consumer -n notification-system --replicas=6
    
    # 等待扩容完成
    kubectl rollout status deployment notification-consumer -n notification-system
    
    send_recovery_notification "已自动扩展Kafka消费者至6个副本"
}

# 发送恢复通知
send_recovery_notification() {
    local message=$1
    
    curl -X POST "${DINGTALK_WEBHOOK}" \
         -H 'Content-Type: application/json' \
         -d "{
            \"msgtype\": \"text\",
            \"text\": {
                \"content\": \"🔧 自动恢复通知: $message\"
            }
         }"
}

# 检查内存使用率
check_memory_usage() {
    local instance=$1
    # 通过Prometheus API获取内存使用率
    curl -s "http://prometheus:9090/api/v1/query?query=jvm_memory_used_bytes{instance=\"$instance\"}/jvm_memory_max_bytes{instance=\"$instance\"}*100" | jq -r '.data.result[0].value[1]' | cut -d. -f1
}

# 主函数
main() {
    # 从webhook接收告警信息
    while read line; do
        if echo $line | jq -e .alerts > /dev/null 2>&1; then
            alerts=$(echo $line | jq -r '.alerts[]')
            for alert in $alerts; do
                alert_name=$(echo $alert | jq -r '.labels.alertname')
                instance=$(echo $alert | jq -r '.labels.instance')
                severity=$(echo $alert | jq -r '.labels.severity')
                status=$(echo $alert | jq -r '.status')
                
                if [ "$status" = "firing" ]; then
                    handle_alert $alert_name $instance $severity
                fi
            done
        fi
    done
}

# 启动自动恢复服务
if [ "$1" = "start" ]; then
    echo "启动自动恢复服务"
    nc -l -k -p 9099 | main
else
    echo "用法: $0 start"
fi
```

## 📊 性能监控和优化

### 1. JVM监控配置

```yaml
# jvm-monitoring.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: jvm-monitoring-config
  namespace: notification-system
data:
  jvm-opts: |
    # GC日志配置
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:+UnlockExperimentalVMOptions
    -XX:+UseContainerSupport
    
    # 内存dump配置
    -XX:+HeapDumpOnOutOfMemoryError
    -XX:HeapDumpPath=/app/dumps/
    -XX:+ExitOnOutOfMemoryError
    
    # GC日志
    -Xlog:gc*:logs/gc.log:time,tags
    -Xlog:gc-heap-coops
    
    # JMX监控
    -Dcom.sun.management.jmxremote=true
    -Dcom.sun.management.jmxremote.port=1099
    -Dcom.sun.management.jmxremote.authenticate=false
    -Dcom.sun.management.jmxremote.ssl=false
    
    # APM Agent
    -javaagent:/app/apm/skywalking-agent.jar
    -Dskywalking.agent.service_name=notification-system
    -Dskywalking.collector.backend_service=skywalking-oap:11800
```

### 2. APM配置

```yaml
# skywalking-oap.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: skywalking-oap
  namespace: notification-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: skywalking-oap
  template:
    metadata:
      labels:
        app: skywalking-oap
    spec:
      containers:
      - name: skywalking-oap
        image: apache/skywalking-oap-server:8.9.1
        ports:
        - containerPort: 11800
        - containerPort: 12800
        env:
        - name: SW_STORAGE
          value: elasticsearch
        - name: SW_STORAGE_ES_CLUSTER_NODES
          value: "elasticsearch:9200"
        - name: SW_TELEMETRY
          value: prometheus
        - name: JAVA_OPTS
          value: "-Xms2g -Xmx2g"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1"
          limits:
            memory: "4Gi"
            cpu: "2"

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: skywalking-ui
  namespace: notification-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: skywalking-ui
  template:
    metadata:
      labels:
        app: skywalking-ui
    spec:
      containers:
      - name: skywalking-ui
        image: apache/skywalking-ui:8.9.1
        ports:
        - containerPort: 8080
        env:
        - name: SW_OAP_ADDRESS
          value: "skywalking-oap:12800"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
```

## 📈 容量规划和扩容策略

### 1. 自动扩缩容配置

```yaml
# hpa-vpa.yml
# 水平扩缩容
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: notification-app-hpa
  namespace: notification-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: notification-app
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "1000"
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
      - type: Pods
        value: 2
        periodSeconds: 60

---
# 垂直扩缩容
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: notification-app-vpa
  namespace: notification-system
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: notification-app
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: notification-app
      minAllowed:
        cpu: 100m
        memory: 512Mi
      maxAllowed:
        cpu: 4
        memory: 8Gi
      controlledResources: ["cpu", "memory"]
```

### 2. 集群自动扩容

```yaml
# cluster-autoscaler.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cluster-autoscaler
  namespace: kube-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cluster-autoscaler
  template:
    metadata:
      labels:
        app: cluster-autoscaler
    spec:
      containers:
      - image: k8s.gcr.io/autoscaling/cluster-autoscaler:v1.21.0
        name: cluster-autoscaler
        resources:
          limits:
            cpu: 100m
            memory: 300Mi
          requests:
            cpu: 100m
            memory: 300Mi
        command:
        - ./cluster-autoscaler
        - --v=4
        - --stderrthreshold=info
        - --cloud-provider=aws
        - --skip-nodes-with-local-storage=false
        - --expander=least-waste
        - --node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/notification-cluster
        - --balance-similar-node-groups
        - --skip-nodes-with-system-pods=false
        env:
        - name: AWS_REGION
          value: cn-north-1
        volumeMounts:
        - name: ssl-certs
          mountPath: /etc/ssl/certs/ca-certificates.crt
          readOnly: true
      volumes:
      - name: ssl-certs
        hostPath:
          path: "/etc/ssl/certs/ca-certificates.crt"
```

## 🔒 安全监控配置

### 1. 安全扫描

```yaml
# security-scanning.yml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: security-scan
  namespace: notification-system
spec:
  schedule: "0 2 * * *"  # 每天凌晨2点执行
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: trivy-scanner
            image: aquasec/trivy:latest
            command:
            - /bin/sh
            - -c
            - |
              # 扫描运行中的镜像
              trivy image --format json --output /reports/backend-scan.json harbor.company.com/yudao/notification:latest
              trivy image --format json --output /reports/frontend-scan.json harbor.company.com/yudao/notification-ui:latest
              
              # 扫描文件系统
              trivy fs --format json --output /reports/filesystem-scan.json /app
              
              # 发送报告
              curl -X POST "http://webhook-service:8080/security-report" \
                   -H "Content-Type: application/json" \
                   -d @/reports/backend-scan.json
            volumeMounts:
            - name: scan-reports
              mountPath: /reports
          volumes:
          - name: scan-reports
            emptyDir: {}
          restartPolicy: OnFailure
```

### 2. 入侵检测

```yaml
# falco-rules.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: falco-rules
  namespace: notification-system
data:
  notification_rules.yaml: |
    - rule: Suspicious Network Activity
      desc: Detect suspicious network connections
      condition: >
        spawned_process and container.name startswith "notification-app" and
        (proc.name in (nc, ncat, netcat, socat) or
         (proc.name = "curl" and proc.args contains "shell"))
      output: >
        Suspicious network activity in notification container 
        (user=%user.name command=%proc.cmdline container=%container.name)
      priority: WARNING
      tags: [network, suspicious]
    
    - rule: Unexpected File Access
      desc: Detect unexpected file access in application
      condition: >
        open_read and container.name startswith "notification-app" and
        (fd.name startswith "/etc/passwd" or
         fd.name startswith "/etc/shadow" or
         fd.name startswith "/proc/")
      output: >
        Unexpected file access in notification container 
        (user=%user.name file=%fd.name container=%container.name)
      priority: WARNING
      tags: [filesystem, security]
    
    - rule: Privileged Container Launch
      desc: Detect privileged container launch
      condition: >
        container_started and container.privileged = true and
        container.name contains "notification"
      output: >
        Privileged notification container launched 
        (user=%user.name container=%container.name)
      priority: HIGH
      tags: [container, privileged]
```

## 📋 部署清单和最佳实践

### 1. 环境清单检查

```bash
#!/bin/bash
# deployment-checklist.sh

echo "=== 智能通知系统部署前检查清单 ==="

# 基础设施检查
check_infrastructure() {
    echo "1. 基础设施检查"
    
    # Kubernetes集群
    if kubectl cluster-info > /dev/null 2>&1; then
        echo "✅ Kubernetes集群连接正常"
        kubectl version --short
    else
        echo "❌ Kubernetes集群连接失败"
        return 1
    fi
    
    # Docker Registry
    if docker login $DOCKER_REGISTRY > /dev/null 2>&1; then
        echo "✅ Docker Registry连接正常"
    else
        echo "❌ Docker Registry连接失败"
        return 1
    fi
    
    # 存储检查
    echo "存储类检查:"
    kubectl get storageclass
    
    # 网络检查
    echo "网络策略检查:"
    kubectl get networkpolicy -A
}

# 资源配额检查
check_resources() {
    echo "2. 资源配额检查"
    
    # 节点资源
    echo "节点资源状态:"
    kubectl top nodes
    
    # 命名空间资源配额
    echo "命名空间资源配额:"
    kubectl get resourcequota -n notification-system
    
    # PV状态
    echo "持久卷状态:"
    kubectl get pv | grep Available
}

# 配置检查
check_configurations() {
    echo "3. 配置检查"
    
    # ConfigMaps
    echo "配置映射检查:"
    kubectl get configmap -n notification-system
    
    # Secrets
    echo "密钥检查:"
    kubectl get secret -n notification-system
    
    # 网络服务
    echo "服务检查:"
    kubectl get svc -n notification-system
}

# 监控检查
check_monitoring() {
    echo "4. 监控系统检查"
    
    # Prometheus
    if curl -s http://prometheus:9090/-/healthy > /dev/null; then
        echo "✅ Prometheus运行正常"
    else
        echo "❌ Prometheus连接失败"
    fi
    
    # Grafana
    if curl -s http://grafana:3000/api/health > /dev/null; then
        echo "✅ Grafana运行正常"
    else
        echo "❌ Grafana连接失败"
    fi
    
    # Elasticsearch
    if curl -s http://elasticsearch:9200/_cluster/health > /dev/null; then
        echo "✅ Elasticsearch运行正常"
    else
        echo "❌ Elasticsearch连接失败"
    fi
}

# 安全检查
check_security() {
    echo "5. 安全配置检查"
    
    # RBAC
    echo "RBAC权限检查:"
    kubectl get clusterrolebinding | grep notification
    
    # 网络策略
    echo "网络策略检查:"
    kubectl get networkpolicy -n notification-system
    
    # Pod安全策略
    echo "Pod安全策略检查:"
    kubectl get psp | grep notification
    
    # 镜像安全扫描
    echo "镜像安全扫描:"
    trivy image --severity HIGH,CRITICAL $DOCKER_REGISTRY/yudao/notification:latest
}

# 执行所有检查
main() {
    check_infrastructure
    check_resources  
    check_configurations
    check_monitoring
    check_security
    
    echo ""
    echo "=== 部署前检查完成 ==="
    echo "如所有检查项通过，可以开始部署"
}

main "$@"
```

### 2. 部署最佳实践文档

```markdown
# 智能通知系统部署最佳实践

## 部署前准备

### 1. 环境要求
- Kubernetes 1.21+
- Docker 20.10+
- Helm 3.6+
- kubectl 1.21+

### 2. 资源规划
```yaml
生产环境最小配置:
  节点数量: 5个
  单节点配置: 8C16G
  存储: 500GB SSD
  网络带宽: 10Gbps
```

### 3. 安全配置
- 启用RBAC权限控制
- 配置网络策略隔离
- 启用Pod安全策略
- 配置镜像安全扫描

## 部署流程

### 阶段1: 基础设施部署
1. 创建命名空间
2. 部署存储组件
3. 配置网络策略
4. 部署监控系统

### 阶段2: 数据存储部署
1. 部署MySQL集群
2. 部署Redis集群  
3. 部署Kafka集群
4. 验证存储服务

### 阶段3: 应用服务部署
1. 部署后端服务
2. 部署前端服务
3. 配置负载均衡
4. 健康检查验证

### 阶段4: 监控告警配置
1. 配置Prometheus监控
2. 配置Grafana面板
3. 配置告警规则
4. 测试告警通知

## 运维最佳实践

### 1. 监控指标
- 应用性能指标
- 基础设施指标
- 业务指标监控
- 用户体验指标

### 2. 备份策略
- 数据库每日全量备份
- 配置文件版本控制
- 镜像版本管理
- 灾难恢复预案

### 3. 安全维护
- 定期安全扫描
- 漏洞修复跟踪
- 访问日志审计
- 权限定期review

### 4. 性能优化
- JVM参数调优
- 数据库索引优化
- 缓存策略优化
- 网络性能调优
```

## 🎯 总结

本CI/CD与监控方案基于yudao-boot-mini框架，为智能通知系统提供了完整的DevOps解决方案：

### 核心特性
1. **全自动化CI/CD流水线** - 从代码提交到生产部署的全流程自动化
2. **完善的监控体系** - 覆盖应用、基础设施、业务的全方位监控
3. **智能告警机制** - 多级告警、自动恢复、智能降噪
4. **高可用架构** - 蓝绿部署、自动扩缩容、故障自愈
5. **安全防护体系** - 镜像扫描、运行时防护、访问控制

### 技术亮点
- **容器化部署**: Docker多阶段构建，优化镜像大小
- **Kubernetes编排**: HPA/VPA自动扩缩容，确保服务弹性
- **微服务监控**: APM链路追踪，全链路性能分析
- **日志聚合**: ELK Stack集中式日志管理
- **自动运维**: 智能告警处理，减少人工干预

### 性能保障
通过本方案可以实现：
- **系统可用性**: 99.9%+
- **部署效率**: 代码到生产部署 < 30分钟
- **故障恢复**: 平均故障恢复时间 < 5分钟
- **监控覆盖**: 100%业务指标监控

该方案充分考虑了高并发推送场景的特殊需求，通过完善的监控和自动化运维，确保智能通知系统在复杂业务场景下的稳定运行。