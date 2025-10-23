# Java 21 Upgrade

This project now targets Java 21 (LTS).

Prerequisites
- Install JDK 21 (Temurin, Zulu, or Corretto)
- Ensure `JAVA_HOME` points to your JDK 21 installation
- Confirm with `java -version` and `javac -version`

Maven Build
- Local: `mvn -B -DskipTests package`
- The `pom.xml` sets compiler source/target to 21

CI/CD
- For GitHub Actions, use:
  - `uses: actions/setup-java@v3`
  - `with: java-version: '21', distribution: 'temurin'`

Notes
- Spring Boot 3.3.x supports Java 21 and is already configured
- If using toolchains, ensure your `~/.m2/toolchains.xml` points to JDK 21
