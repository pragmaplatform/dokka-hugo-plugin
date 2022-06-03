# Dokka Hugo Plugin

Extends [Dokka](https://github.com/Kotlin/dokka) with a Hugo Plugin that is specific to Pragma Platform's API Docs.

It generates API Docs for elements that are documented, discarding all others.

## Build

``./gradlew build``

## Try Local Build

``./gradlew publishToMavenLocal``


## Example `pom.xml` file in `platform/2-pragma`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>pragma</groupId>
        <artifactId>engine-settings</artifactId>
        <version>${revision}</version>
        <relativePath>../engine-settings.xml</relativePath>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>2-pragma</artifactId>
    <packaging>pom</packaging>

    <modules>
        <module>core</module>
        <module>social-common</module>
        <module>social</module>
        <module>game-common</module>
        <module>game</module>
        <module>server-base</module>
        <module>load-test</module>
    </modules>

    <dependencies>
        <dependency>
            <groupId>pragma</groupId>
            <artifactId>proto-core</artifactId>
            <version>${revision}</version>
        </dependency>
        <dependency>
            <groupId>pragma</groupId>
            <artifactId>proto-defs</artifactId>
            <version>${revision}</version>
        </dependency>
        <dependency>
            <groupId>pragma</groupId>
            <artifactId>proto-exts</artifactId>
            <version>${revision}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-extension-kotlin</artifactId>
        </dependency>

        <!-- TEST DEP BELOW -->
        <dependency>
            <groupId>pragma</groupId>
            <artifactId>proto-defs</artifactId>
            <version>${revision}</version>
            <type>test-jar</type>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <properties>
        <dokka-hugo-plugin.version>3.0</dokka-hugo-plugin.version>
        <dokka.version>1.6.21</dokka.version>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>dokka-maven-plugin</artifactId>
                <version>${dokka.version}</version>
                <executions>
                    <execution>
                        <phase>pre-site</phase>
                        <goals>
                            <goal>dokka</goal>
                        </goals>
                    </execution>
                </executions>

                <configuration>

                    <!-- Set to true to skip dokka task, default: false -->
                    <skip>false</skip>

                    <!-- Default: ${project.artifactId} -->
                    <moduleName>${project.artifactId}</moduleName>

                    <!-- Default: ${project.basedir}/target/dokka -->
                    <outputDir>../../documentation/revamp/content/api</outputDir>

                    <!-- Use default or set to custom path to cache directory to enable package-list caching. -->
                    <!-- When set to default, caches stored in $USER_HOME/.cache/dokka -->
                    <cacheRoot>${project.basedir}/../documentation/temp/package-list</cacheRoot>

                    <!-- Set to true to to prevent resolving package-lists online. -->
                    <!-- When this option is set to true, only local files are resolved, default: false -->
                    <offlineMode>false</offlineMode>

                    <!-- A list of visibility modifiers that should be documented -->
                    <!-- If set by user, overrides includeNonPublic. Default is PUBLIC -->
                    <documentedVisibilities>
                        <visibility>PUBLIC</visibility> <!-- Same for both kotlin and java -->
                        <visibility>PRIVATE</visibility> <!-- Same for both kotlin and java -->
                        <visibility>PROTECTED</visibility> <!-- Same for both kotlin and java -->
                        <visibility>INTERNAL</visibility> <!-- Kotlin-specific internal modifier -->
                        <visibility>PACKAGE</visibility> <!-- Java-specific package-private visibility (default) -->
                    </documentedVisibilities>

                    <!-- Suppress obvious functions like default toString or equals. Defaults to true -->
                    <suppressObviousFunctions>true</suppressObviousFunctions>

                    <!-- Suppress all inherited members that were not overriden in a given class. -->
                    <!-- Eg. using it you can suppress toString or equals functions but you can't suppress componentN or copy on data class. To do that use with suppressObviousFunctions -->
                    <!-- Defaults to false -->
                    <suppressInheritedMembers>true</suppressInheritedMembers>

                    <!-- Do not output deprecated members, applies globally, can be overridden by packageOptions -->
                    <skipDeprecated>true</skipDeprecated>
                    <!-- Emit warnings about not documented members, applies globally, also can be overridden by packageOptions -->
                    <reportUndocumented>true</reportUndocumented>
                    <!-- Do not create index pages for empty packages -->
                    <skipEmptyPackages>true</skipEmptyPackages>

                    <!-- Short form list of sourceRoots, by default, set to ${project.compileSourceRoots} -->
                    <sourceDirectories>
                        <dir>${project.basedir}/src/main/kotlin</dir>
                    </sourceDirectories>

                    <!-- Disable linking to online kotlin-stdlib documentation  -->
                    <noStdlibLink>true</noStdlibLink>

                    <!-- Disable linking to online JDK documentation -->
                    <noJdkLink>true</noJdkLink>

                    <!-- Allows to customize documentation generation options on a per-package basis -->
                    <perPackageOptions>
                        <packageOptions>
                            <!-- Will match kotlin and all sub-packages of it -->
                            <matchingRegex>kotlin($|\.).*</matchingRegex>

                            <!-- All options are optional, default values are below: -->
                            <skipDeprecated>false</skipDeprecated>

                            <!-- Emit warnings about not documented members  -->
                            <reportUndocumented>true</reportUndocumented>

                            <!-- Deprecated. Prefer using documentedVisibilities
                            <includeNonPublic>false</includeNonPublic> -->

                            <!-- A list of visibility modifiers that should be documented -->
                            <!-- If set by user, overrides includeNonPublic. Default is PUBLIC -->
                            <documentedVisibilities>
                                <visibility>PUBLIC</visibility> <!-- Same for both kotlin and java -->
                                <visibility>PRIVATE</visibility> <!-- Same for both kotlin and java -->
                                <visibility>PROTECTED</visibility> <!-- Same for both kotlin and java -->
                                <visibility>INTERNAL</visibility> <!-- Kotlin-specific internal modifier -->
                                <visibility>PACKAGE</visibility> <!-- Java-specific package-private visibility (default) -->
                            </documentedVisibilities>
                        </packageOptions>
                    </perPackageOptions>

                    <!-- Allows to use any dokka plugin, eg. GFM format   -->
                    <dokkaPlugins>
                        <plugin>
                            <groupId>gg.pragma</groupId>
                            <artifactId>dokka-hugo-plugin</artifactId>
                            <version>${dokka-hugo-plugin.version}</version>
                        </plugin>
                    </dokkaPlugins>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```