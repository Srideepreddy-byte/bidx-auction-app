FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY backend/ ./
RUN javac -cp "lib/*" *.java
EXPOSE 8080
CMD ["java", "-cp", ".:lib/*", "BidXServer"]
