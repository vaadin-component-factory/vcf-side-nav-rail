# Phase 1 — Build infrastructure

**Prereqs:** Read [`../2026-04-21-side-nav-rail-plan.md`](../2026-04-21-side-nav-rail-plan.md) for cross-phase conventions (license headers, commit style, `./mvnw`, Vaadin version pin).

**Output of this phase:** a multi-module Maven build that compiles cleanly — parent POM, `addon/` module with the package skeleton and Apache 2.0 `LICENSE`, `demo/` module with a Spring Boot entry point.

---

## Task 1: Parent POM and Maven wrapper

**Files:**
- Create: `pom.xml`
- Create: `.mvn/wrapper/maven-wrapper.properties`
- Create: `mvnw`, `mvnw.cmd` (Maven wrapper scripts)
- Update: `.gitignore`

- [ ] **Step 1: Install Maven wrapper**

Run:
```bash
cd /workspace
mvn -N io.takari:maven:0.7.7:wrapper -Dmaven=3.9.9
```

Expected: creates `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/` directory.

- [ ] **Step 2: Write the parent POM**

Create `/workspace/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.vaadin.addons.componentfactory</groupId>
    <artifactId>vcf-side-nav-rail-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>SideNav Rail (reactor)</name>
    <description>Reactor POM for the SideNav Rail workspace — builds addon and demo together.
        The addon module is standalone (does not inherit from this POM) so that the
        published artifact has no parent dependency.</description>

    <licenses>
        <license>
            <name>Apache License 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
    </licenses>

    <properties>
        <maven.compiler.release>25</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <vaadin.version>24.5.0</vaadin.version>
        <spring-boot.version>3.4.0</spring-boot.version>
        <karibu.version>2.2.0</karibu.version>
        <junit.version>5.11.3</junit.version>
    </properties>

    <modules>
        <module>addon</module>
        <module>demo</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.vaadin</groupId>
                <artifactId>vaadin-bom</artifactId>
                <version>${vaadin.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.13.0</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>3.5.2</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <version>3.5.2</version>
                </plugin>
                <plugin>
                    <groupId>com.diffplug.spotless</groupId>
                    <artifactId>spotless-maven-plugin</artifactId>
                    <version>2.43.0</version>
                    <configuration>
                        <java>
                            <googleJavaFormat>
                                <version>1.22.0</version>
                                <style>AOSP</style>
                            </googleJavaFormat>
                            <removeUnusedImports/>
                        </java>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 3: Add .gitignore entries**

Append to `/workspace/.gitignore`:
```
# Maven
target/
*.log

# Node / Playwright
node_modules/
playwright-report/
test-results/
```

- [ ] **Step 4: Verify the parent POM resolves**

Run: `./mvnw -N validate`
Expected: `BUILD SUCCESS`. (`-N` = don't recurse into modules yet; they don't exist.)

- [ ] **Step 5: Commit**

```bash
git init
git add .gitignore pom.xml mvnw mvnw.cmd .mvn/
git commit -m "build: add parent POM and Maven wrapper"
```

---

## Task 2: Addon module skeleton

**Files:**
- Create: `addon/pom.xml`
- Create: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/package-info.java`
- Create: `addon/LICENSE` (Apache 2.0 text)

- [ ] **Step 1: Write the addon POM**

> **Important:** the addon POM **does not** reference the reactor/parent POM. Vaadin Directory addons must be publishable as standalone artifacts — a parent reference would force consumers to also resolve the parent. Everything (groupId, version, properties, dependencyManagement) is therefore declared directly inside `addon/pom.xml`. Some redundancy with the reactor POM is accepted in exchange for a clean published artifact.

Create `/workspace/addon/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- No <parent> — addon is standalone-publishable (see Vaadin Directory publishing rules). -->

    <groupId>org.vaadin.addons.componentfactory</groupId>
    <artifactId>vcf-side-nav-rail</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>SideNav Rail</name>
    <description>A togglable rail-mode SideNav for Vaadin 24.</description>

    <licenses>
        <license>
            <name>Apache License 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
    </licenses>

    <properties>
        <maven.compiler.release>25</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <vaadin.version>24.5.0</vaadin.version>
        <spring-boot.version>3.4.0</spring-boot.version>
        <karibu.version>2.2.0</karibu.version>
        <junit.version>5.11.3</junit.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.vaadin</groupId>
                <artifactId>vaadin-bom</artifactId>
                <version>${vaadin.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>com.vaadin</groupId>
            <artifactId>vaadin-core</artifactId>
        </dependency>

        <!-- Test scope -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.github.mvysny.kaributesting</groupId>
            <artifactId>karibu-testing-v24</artifactId>
            <version>${karibu.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.vaadin</groupId>
            <artifactId>vaadin-spring-boot-starter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.2</version>
                <configuration>
                    <includes>
                        <include>**/unit/*Test.java</include>
                    </includes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>3.5.2</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Write package-info**

Create `/workspace/addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/package-info.java` with the full Apache 2.0 header (see index file) followed by:
```java
/**
 * SideNav Rail — a togglable rail-mode {@link com.vaadin.flow.component.sidenav.SideNav}
 * with a configurable hover popover for items with children.
 *
 * <p>Entry points: {@link org.vaadin.addons.componentfactory.sidenavrail.SideNavRail}
 * and {@link org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem}.
 */
package org.vaadin.addons.componentfactory.sidenavrail;
```

- [ ] **Step 3: Add the Apache 2.0 LICENSE file**

Fetch the canonical text:
```bash
curl -fsSL https://www.apache.org/licenses/LICENSE-2.0.txt -o /workspace/addon/LICENSE
```

- [ ] **Step 4: Verify the module compiles**

Run: `./mvnw -pl addon -am compile`
Expected: `BUILD SUCCESS`. Compiles `package-info.java` (no real sources yet).

- [ ] **Step 5: Commit**

```bash
git add addon/
git commit -m "build: add addon module skeleton"
```

---

## Task 3: Demo module skeleton

**Files:**
- Create: `demo/pom.xml`
- Create: `demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/Application.java`

- [ ] **Step 1: Write the demo POM**

Create `/workspace/demo/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.vaadin.addons.componentfactory</groupId>
        <artifactId>vcf-side-nav-rail-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>vcf-side-nav-rail-demo</artifactId>
    <packaging>jar</packaging>

    <name>SideNav Rail — Demo</name>

    <dependencies>
        <dependency>
            <groupId>org.vaadin.addons.componentfactory</groupId>
            <artifactId>vcf-side-nav-rail</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.vaadin</groupId>
            <artifactId>vaadin-spring-boot-starter</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Write a minimal Spring Boot entry point**

Create `/workspace/demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/Application.java` with the full Apache 2.0 header followed by:
```java
package org.vaadin.addons.componentfactory.sidenavrail.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

- [ ] **Step 3: Verify the multi-module build compiles**

Run: `./mvnw compile`
Expected: both `addon` and `demo` compile, `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add demo/
git commit -m "build: add demo module skeleton"
```

---

## Phase 1 complete when

- `./mvnw compile` succeeds at the workspace root.
- `addon/` and `demo/` both have POMs and at least one source file.
- Three green commits in the log: `build: add parent POM and Maven wrapper`, `build: add addon module skeleton`, `build: add demo module skeleton`.

Next: [Phase 2 — Core API](./02-core-api.md).
