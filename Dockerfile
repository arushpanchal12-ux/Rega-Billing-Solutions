FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the jar file
COPY target/rega-billing-solutions-0.0.1-SNAPSHOT.jar app.jar

# Environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=mysql

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
