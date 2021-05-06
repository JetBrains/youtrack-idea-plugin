job("Run tests against all supported IDE versions") {
    gradlew("openjdk:11", "check")
}