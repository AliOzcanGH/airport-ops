plugins {
	java
	id("org.springframework.boot") version "3.5.15"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.aliozcan.airportops"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(platform("software.amazon.awssdk:bom:2.29.52"))
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.security:spring-security-crypto")
	implementation("org.keycloak:keycloak-admin-client:26.0.10")
	implementation("software.amazon.awssdk:sesv2")
	implementation("dev.samstevens.totp:totp:1.7.1")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty(
		"APP_TOTP_ENCRYPTION_KEY",
		"MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
	)
	systemProperty(
		"IAM_TOKEN_PRIVATE_KEY",
		"MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCG9vn/pSKST+No4hPTjRVoJdTkrOY7ysSrgSu+mFbdZfnyvRBjSWqWVWWP2TdoWDzsRcRgd8dXhqxt0APM1QBAwQ+tNAw/7BZYjZ/6NVO++q39uMDePsQnxBzPTGVGMmmWWEWUXPPI/KMoNbxfUr2vEmm2eukn+cjmrbkNTYt8H4rWM2GiYjhlnFMzJ1RtGM+12OspusiQmdsADpNj4W2To8XafT67tVWKhDMQloE1GBK3z1PWmTLp/2grMsTpPqUyBE373P+ETzcQoo/4rObfKW89VO3TfNAxhxuW/qZaLaQxXjE0pLs2QcKvfBRkBK+MhlFtpBtnZ9qiTDGGwQFZAgMBAAECggEAG5vhV1USJekWxald0exuASCgFOlzra6UndruNm6WcnL+dxQLCqyFk9xDSvJt/bfzOogpHGzUSAjiNBKV/GxkQVrdGJrpmJf0mEfcfoQzLgG83wbQ2YXwfYwGMrfXjc0ZQWqsTSXYrvhTbhHUB5H3AJ3dJzmsgYN9AAdwsBJ5YroLEP9HlKj2cdIRgQEpfipDgYAo8maob18E867isBxkAp7trcG+H31kOcJvZnfO7PSKQZaYw1to1MelZbW3b1ZjrRFkVQabCmwq2mQK/SvPCz8Xk8/3Hln5LyxzUyuylIWSHRlroZICWaumSzBffGDTPOWyo8wAn0k84I30JrCRswKBgQC7UaeRPnDyZ9JD5ZRU2dJrLRK+7XnVh6nZv6/vioVoW3+fiFILXbq4J/IOHJx3if00d1ZXaSg/Yv0e7jk3ocPAYcp2I7llmn7E1CzFp1O7U5iWYmDO88gzJf/WafhsOIHG8+BR1Um+2ESmcEQwsYUJdWtsRpR/bG4+cCZZ43WMDwKBgQC4czEIrLkaUyKWquiQZY5ebaVs7cU3KoRvdKLq3T5rpVQtkGbMkrg8VxxVV5eruXSmW5JPI6ne2mrRyAFBB+89ndKlHCD2/g+M9+aWiXkX22GW/PiNUISvDIwjt/+1+CfMk42cthVMoWUTLgeJeiSIpCS/X2/XV4XN2PAuTHvUFwKBgDaTJbUFcjbN1M7/VmYaSFn9edayrapuqifM1199Bj1PWzGJsA2rszjwOm+uGlAhnenpUviIpLdDCeg3E+iHctbcJvagleVqS8Dg6GJ1y8lqI6y84OOq1ws/6Pf5S+2L0RO7/gEZmTDHJckwME3XiqEk2rVjIuS1HLGIT0QlJxw1AoGATD2I6g4C4Oe2J1LzmCPyKgPmWdUaLIhNf4hVgwD6OSJ4XfEPHMYUkxRqqLfxAFFC7zRwkHesUmoztwyVwByYff6LQCYVViKDqQAa9uRSAlNyzic4UakTfwuPrX/zmXK+eKgQ49K7kRYxIjFneiLbkQNQES5Qm1EZBJIEDQ8eK/cCgYEAiGltwX2cm+d7O/x9ADnxTKyIa7XQpm4LXbVuETz4/0xuAGwYOP8nWM21lcK2vwOvYCL4vz5A7iQ3JMULRGaW2aYXBTdOu9GOOTbKHeh0fhE5cnM+rE3/WiBjvdDf1AETvQQsjH+XzM8yU14GibxSC11tktcyPRCUkSjT+pQZoBI="
	)
}

tasks.named<JavaCompile>("compileTestJava") {
	// Work around local javac classpath directory resolution issues on Windows.
	source(sourceSets.main.get().allJava)
}
