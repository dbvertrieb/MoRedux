![Publish release artifacts](https://github.com/dbvertrieb/MoRedux/actions/workflows/publish-release-artifacts.yml/badge.svg)
![Verify code quality](https://github.com/dbvertrieb/MoRedux/actions/workflows/verify-code-quality.yml/badge.svg)

# MoRedux
Redux framework in Kotlin

# Download

Replace **$VERSION_NUMBER$** with the released version of your choice. The recommended version is the latest.

## gradle
```
# on build.gradle.kts on project level
repositories {
    mavenCentral
}

# wherever you declare your dependencies (project / module level)
dependencies {
    implementation("de.db.moredux:MoRedux:$VERSION_NUMBER$")
}
```

## Maven
```
<dependency>
    <groupId>de.db.moredux</groupId>
    <artifactId>MoRedux</artifactId>
    <version>$VERSION_NUMBER$</version>
</dependency>
```
# Basic Usage

## Components

* State
* Action
* Effect
* Store
* StoreContainer

# License

    Copyright 2013 the original author or authors.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
