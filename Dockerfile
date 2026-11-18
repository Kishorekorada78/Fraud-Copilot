# Stage 1: build the jar

# Start from an image that has JDK 21
FROM eclipse-temurin:21-jdk AS build	

# Work inside the folder /app in the image.
WORKDIR /app

# Copy your whole project (minus .dockerignore items) into /app.
COPY . .

# Make the Maven wrapper executable and build the jar. -DskipTests skips tests, and -q reduces log noise.
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

# Stage 2: run it

# Start a fresh, smaller image with only the Java runtime.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy only the finished jar from stage 1
COPY --from=build /app/target/*.jar app.jar

# Documents the port. It doesn’t open it, because -p does that later.
EXPOSE 8080

# The command that runs when the container starts.
ENTRYPOINT ["java", "-jar", "app.jar"]


#   docker compose up -d db          # database (port 5433)
# run the app in STS, or:
#docker compose up --build        # app + db in containers
# open fraud-ui/index.html with Live Server